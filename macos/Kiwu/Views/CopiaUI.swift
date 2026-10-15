import SwiftUI
import AppKit
import CoreData
import UniformTypeIdentifiers

/// Diálogos de exportar e importar la copia de seguridad (menú Archivo y Ajustes).
@MainActor
enum CopiaUI {
    private static var contexto: NSManagedObjectContext { PersistenceController.shared.container.viewContext }

    static func exportar() {
        let panel = NSSavePanel()
        panel.nameFieldStringValue = CopiaService.nombreDeArchivo()
        panel.allowedContentTypes = [.json]
        panel.message = "Guarda una copia de seguridad de tus listas, tareas, notas y horarios"
        guard panel.runModal() == .OK, let url = panel.url else { return }
        do {
            try CopiaService.exportar(en: contexto).write(to: url, options: .atomic)
        } catch {
            alerta("No se pudo exportar la copia", error.localizedDescription)
        }
    }

    static func importar() {
        let panel = NSOpenPanel()
        panel.allowedContentTypes = [.json]
        panel.allowsMultipleSelection = false
        panel.canChooseDirectories = false
        panel.message = "Elige un archivo .kiwu.json (de este Mac, Linux o Android)"
        guard panel.runModal() == .OK, let url = panel.url else { return }
        do {
            let resumen = try CopiaService.importar(Data(contentsOf: url), en: contexto)
            alerta("Copia importada", resumen.total == 0 ? "No había nada nuevo que añadir." : resumen.texto)
        } catch {
            alerta("No se pudo importar la copia", error.localizedDescription)
        }
    }

    private static func alerta(_ titulo: String, _ texto: String) {
        let alerta = NSAlert()
        alerta.messageText = titulo
        alerta.informativeText = texto
        alerta.runModal()
    }
}
