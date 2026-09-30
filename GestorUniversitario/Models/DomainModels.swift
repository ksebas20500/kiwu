import Foundation
import CoreData

enum NoteBlockKind: String, Codable, Sendable, CaseIterable, Identifiable {
    var id: String { rawValue }
    case paragraph
    case heading
    case subheading
    case bulletedList
    case numberedList
    case checklist
    case quote
    case callout
    case code
    case image
    case file
    case divider
}

struct NoteBlock: Codable, Identifiable, Sendable {
    var id: UUID = UUID()
    var kind: NoteBlockKind = .paragraph
    var text: String = ""
    var isChecked: Bool = false
    var imageData: Data?
    /// Referencia (security-scoped bookmark) a un archivo del Finder; no se copia el archivo.
    var bookmark: Data?
    var fileName: String?
}

struct NoteDocument: Codable, Sendable {
    var blocks: [NoteBlock] = [NoteBlock()]

    static let empty = NoteDocument()

    var data: Data {
        (try? JSONEncoder().encode(self)) ?? Data()
    }

    /// Texto legible de toda la nota, para búsquedas y vistas previas.
    var textoPlano: String {
        blocks
            .map { $0.kind == .file ? ($0.fileName ?? "") : $0.text }
            .filter { !$0.isEmpty }
            .joined(separator: "\n")
    }
}

@objc(Materia)
final class Materia: NSManagedObject {
    @NSManaged var id: UUID
    @NSManaged var nombre: String
    @NSManaged var icono: String
    @NSManaged var orden: Int16
    @NSManaged var bookmarkCarpeta: Data?
    @NSManaged var carpetaDisponible: Bool
    @NSManaged var fechaCreacion: Date
    @NSManaged var fechaActualizacion: Date
    @NSManaged var deberes: Set<Deber>
}

@objc(Deber)
final class Deber: NSManagedObject {
    @NSManaged var id: UUID
    @NSManaged var titulo: String
    @NSManaged var completado: Bool
    @NSManaged var fechaEntrega: Date?
    @NSManaged var recordatorioAntesEnHoras: Int16
    @NSManaged var fechaCompletado: Date?
    @NSManaged var contenidoNota: Data
    @NSManaged var fechaCreacion: Date
    @NSManaged var fechaActualizacion: Date
    @NSManaged var materia: Materia
}

extension Materia {
    static func fetchRequest() -> NSFetchRequest<Materia> {
        NSFetchRequest<Materia>(entityName: "Materia")
    }

    convenience init(context: NSManagedObjectContext, nombre: String, orden: Int16 = 0) {
        self.init(context: context)
        id = UUID()
        self.nombre = nombre
        icono = "book.closed"
        self.orden = orden
        bookmarkCarpeta = nil
        carpetaDisponible = false
        fechaCreacion = .now
        fechaActualizacion = .now
    }
}

extension Deber {
    static func fetchRequest() -> NSFetchRequest<Deber> {
        NSFetchRequest<Deber>(entityName: "Deber")
    }

    convenience init(context: NSManagedObjectContext, titulo: String, materia: Materia) {
        self.init(context: context)
        id = UUID()
        self.titulo = titulo
        completado = false
        fechaEntrega = nil
        recordatorioAntesEnHoras = 24
        fechaCompletado = nil
        contenidoNota = NoteDocument.empty.data
        fechaCreacion = .now
        fechaActualizacion = .now
        self.materia = materia
    }
}

extension Materia: Identifiable {}
extension Deber: Identifiable {}

@objc(Cuaderno)
final class Cuaderno: NSManagedObject {
    @NSManaged var id: UUID
    @NSManaged var nombre: String
    @NSManaged var icono: String
    @NSManaged var orden: Int16
    @NSManaged var fechaCreacion: Date
    @NSManaged var fechaActualizacion: Date
    @NSManaged var paginas: Set<Pagina>
}

@objc(Pagina)
final class Pagina: NSManagedObject {
    @NSManaged var id: UUID
    @NSManaged var titulo: String
    @NSManaged var icono: String
    @NSManaged var contenido: Data
    @NSManaged var textoPlano: String
    @NSManaged var favorita: Bool
    @NSManaged var fechaCreacion: Date
    @NSManaged var fechaActualizacion: Date
    @NSManaged var cuaderno: Cuaderno
}

extension Cuaderno: Identifiable {
    convenience init(context: NSManagedObjectContext, nombre: String, icono: String, orden: Int16) {
        self.init(context: context)
        id = UUID()
        self.nombre = nombre
        self.icono = icono
        self.orden = orden
        fechaCreacion = .now
        fechaActualizacion = .now
    }
}

extension Pagina: Identifiable {
    convenience init(context: NSManagedObjectContext, cuaderno: Cuaderno) {
        self.init(context: context)
        id = UUID()
        titulo = ""
        icono = ""
        contenido = NoteDocument.empty.data
        textoPlano = ""
        favorita = false
        fechaCreacion = .now
        fechaActualizacion = .now
        self.cuaderno = cuaderno
    }
}
