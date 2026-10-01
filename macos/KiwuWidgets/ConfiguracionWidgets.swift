import AppIntents
import WidgetKit

// MARK: Lista (materia) elegible en el widget de deberes

struct ListaEntity: AppEntity {
    static let typeDisplayRepresentation: TypeDisplayRepresentation = "Lista"
    static let defaultQuery = ListaQuery()

    let id: UUID
    let nombre: String

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(nombre)")
    }
}

struct ListaQuery: EntityQuery {
    private func todas() -> [ListaEntity] {
        (AlmacenWidget.leer()?.listas ?? []).map { ListaEntity(id: $0.id, nombre: $0.nombre) }
    }

    func entities(for identifiers: [UUID]) async throws -> [ListaEntity] {
        todas().filter { identifiers.contains($0.id) }
    }

    func suggestedEntities() async throws -> [ListaEntity] {
        todas()
    }
}

struct SeleccionListaIntent: WidgetConfigurationIntent {
    static let title: LocalizedStringResource = "Lista de deberes"
    static let description = IntentDescription("Elige de qué lista quieres ver los deberes. Si no eliges ninguna, se muestran todas.")

    @Parameter(title: "Lista")
    var lista: ListaEntity?

    init() {}
}

// MARK: Cuaderno elegible en el widget de notas

struct CuadernoEntity: AppEntity {
    static let typeDisplayRepresentation: TypeDisplayRepresentation = "Cuaderno"
    static let defaultQuery = CuadernoQuery()

    let id: UUID
    let nombre: String
    let icono: String

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(icono) \(nombre)")
    }
}

struct CuadernoQuery: EntityQuery {
    private func todos() -> [CuadernoEntity] {
        (AlmacenWidget.leer()?.cuadernos ?? []).map { CuadernoEntity(id: $0.id, nombre: $0.nombre, icono: $0.icono) }
    }

    func entities(for identifiers: [UUID]) async throws -> [CuadernoEntity] {
        todos().filter { identifiers.contains($0.id) }
    }

    func suggestedEntities() async throws -> [CuadernoEntity] {
        todos()
    }
}

struct SeleccionCuadernoIntent: WidgetConfigurationIntent {
    static let title: LocalizedStringResource = "Cuaderno de notas"
    static let description = IntentDescription("Elige el cuaderno donde crear las notas rápidas. Si no eliges ninguno, se usa el primero.")

    @Parameter(title: "Cuaderno")
    var cuaderno: CuadernoEntity?

    init() {}
}
