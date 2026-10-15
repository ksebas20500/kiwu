import CoreData
import UserNotifications

/// Avisos locales de las clases: una notificación por clase con aviso, para su **próxima** ocurrencia.
/// Se reprograman al guardar, al abrir la app y cada pocos minutos (así, tras sonar uno, queda el de la semana siguiente).
@MainActor
enum AvisosHorario {
    private static let prefijo = "clase-"

    static func reprogramar(en contexto: NSManagedObjectContext, ahora: Date = .now) async {
        let clases = ((try? contexto.fetch(Clase.fetchRequest())) ?? []).filter { $0.aviso != nil }
        let centro = UNUserNotificationCenter.current()

        let pendientes = await centro.pendingNotificationRequests()
        centro.removePendingNotificationRequests(withIdentifiers: pendientes.map(\.identifier).filter { $0.hasPrefix(prefijo) })
        guard !clases.isEmpty else { return }
        guard (try? await centro.requestAuthorization(options: [.alert, .sound, .badge])) == true else { return }

        for clase in clases {
            guard let momento = ReglasHorario.momentoDeAviso(clase.datos, desde: ahora) else { continue }
            let contenido = UNMutableNotificationContent()
            contenido.title = clase.titulo
            contenido.body = [ReglasHorario.textoAviso(clase.datos), clase.horario.nombre].joined(separator: " · ")
            contenido.sound = .default
            let componentes = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: momento)
            let disparador = UNCalendarNotificationTrigger(dateMatching: componentes, repeats: false)
            let peticion = UNNotificationRequest(identifier: prefijo + clase.id.uuidString, content: contenido, trigger: disparador)
            try? await centro.add(peticion)
        }
    }
}
