import AppIntents
import WidgetKit

/// Botón de la casilla del widget: marca el deber como completado.
/// La extensión no toca la base de datos; deja el aviso en la cola y la app lo aplica.
struct CompletarDeberIntent: AppIntent {
    static let title: LocalizedStringResource = "Completar deber"
    static let isDiscoverable = false

    @Parameter(title: "Identificador")
    var id: String

    init() {}

    init(id: String) {
        self.id = id
    }

    func perform() async throws -> some IntentResult {
        if let uuid = UUID(uuidString: id) {
            AlmacenWidget.marcarCompletado(uuid)
        }
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}
