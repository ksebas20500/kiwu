import AppIntents
import WidgetKit

/// Flechas del widget de calendario: mes anterior (-1), siguiente (+1) o volver al actual (0).
struct CambiarMesIntent: AppIntent {
    static let title: LocalizedStringResource = "Cambiar mes del calendario"
    static let isDiscoverable = false

    @Parameter(title: "Cambio")
    var delta: Int

    init() {}

    init(delta: Int) {
        self.delta = delta
    }

    func perform() async throws -> some IntentResult {
        AlmacenWidget.cambiarMes(delta)
        WidgetCenter.shared.reloadTimelines(ofKind: "CalendarioDeberes")
        return .result()
    }
}
