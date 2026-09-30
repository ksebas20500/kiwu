import AppKit
import Foundation
import UserNotifications

struct ElementoCarpeta: Identifiable, Hashable {
    let url: URL
    var id: URL { url }
    let nombre: String
    let esCarpeta: Bool
    let tamano: Int64?
    let modificado: Date?
}

struct FinderService {
    func makeBookmark(for url: URL) throws -> Data {
        try url.bookmarkData(options: [.withSecurityScope, .securityScopeAllowOnlyReadAccess], includingResourceValuesForKeys: nil, relativeTo: nil)
    }

    func resolveBookmark(_ data: Data) throws -> (url: URL, isStale: Bool) {
        var isStale = false
        let url = try URL(resolvingBookmarkData: data, options: [.withSecurityScope], relativeTo: nil, bookmarkDataIsStale: &isStale)
        return (url, isStale)
    }

    /// Abre el selector de macOS para elegir una carpeta.
    func elegirCarpeta(mensaje: String) -> URL? {
        let panel = NSOpenPanel()
        panel.canChooseFiles = false
        panel.canChooseDirectories = true
        panel.allowsMultipleSelection = false
        panel.prompt = "Vincular"
        panel.message = mensaje
        return panel.runModal() == .OK ? panel.url : nil
    }

    /// Contenido de una carpeta (carpetas primero, luego por nombre). Requiere acceso activo a la carpeta vinculada.
    func listar(_ carpeta: URL) throws -> [ElementoCarpeta] {
        let claves: [URLResourceKey] = [.isDirectoryKey, .isPackageKey, .fileSizeKey, .contentModificationDateKey]
        let urls = try FileManager.default.contentsOfDirectory(at: carpeta, includingPropertiesForKeys: claves, options: [.skipsHiddenFiles])
        return urls
            .map { url -> ElementoCarpeta in
                let valores = try? url.resourceValues(forKeys: Set(claves))
                // Los paquetes (.pages, .app…) se tratan como archivos, no como carpetas.
                let esCarpeta = (valores?.isDirectory ?? false) && !(valores?.isPackage ?? false)
                return ElementoCarpeta(
                    url: url,
                    nombre: url.lastPathComponent,
                    esCarpeta: esCarpeta,
                    tamano: valores?.fileSize.map { Int64($0) },
                    modificado: valores?.contentModificationDate
                )
            }
            .sorted {
                if $0.esCarpeta != $1.esCarpeta { return $0.esCarpeta }
                return $0.nombre.localizedStandardCompare($1.nombre) == .orderedAscending
            }
    }

    func open(_ url: URL) {
        NSWorkspace.shared.open(url, configuration: NSWorkspace.OpenConfiguration()) { _, error in
            if let error { NSLog("No se pudo abrir \(url.lastPathComponent): \(error.localizedDescription)") }
        }
    }
}

@MainActor
final class ReminderService {
    func requestAuthorization() async throws -> Bool {
        try await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge])
    }

    /// Cancela el aviso anterior y, si el deber sigue pendiente, programa uno nuevo.
    func reprogramar(_ deber: Deber) async {
        cancel(for: deber)
        guard !deber.completado, deber.fechaEntrega != nil, deber.recordatorioAntesEnHoras > 0 else { return }
        guard (try? await requestAuthorization()) == true else { return }
        try? await schedule(for: deber)
    }

    func schedule(for deber: Deber) async throws {
        let horas = Int(deber.recordatorioAntesEnHoras)
        guard let fechaEntrega = deber.fechaEntrega, horas > 0 else { return }

        let reminderDate = fechaEntrega.addingTimeInterval(-TimeInterval(horas * 60 * 60))
        guard reminderDate > .now else { return }

        let content = UNMutableNotificationContent()
        content.title = "Recordatorio de deber"
        content.body = deber.titulo
        content.sound = .default

        let components = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: reminderDate)
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        let request = UNNotificationRequest(identifier: deber.id.uuidString, content: content, trigger: trigger)
        try await UNUserNotificationCenter.current().add(request)
    }

    func cancel(for deber: Deber) {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [deber.id.uuidString])
    }
}
