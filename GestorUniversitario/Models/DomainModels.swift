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
    case embed
}

/// Fragmento de texto con formato. `NoteBlock.text` guarda siempre el texto plano (búsquedas, widgets);
/// `runs` solo existe cuando el bloque tiene algún formato.
struct TextRun: Codable, Sendable, Equatable {
    var text: String
    var bold = false
    var italic = false
    var underline = false
    var strike = false
    var code = false
    /// Nombre de un color de `NotaColor` (texto) y su versión de resaltado.
    var color: String?
    var highlight: String?
    var link: String?
}

/// Datos de la vista previa de un enlace (título, descripción e imagen reducida).
struct LinkPreview: Codable, Sendable, Equatable {
    var titulo: String?
    var descripcion: String?
    var sitio: String?
    var imagen: Data?
    /// "youtube" o "vimeo" si es un vídeo reproducible.
    var proveedor: String?
    var videoID: String?
}

struct NoteBlock: Codable, Identifiable, Sendable {
    var id: UUID = UUID()
    var kind: NoteBlockKind = .paragraph
    var text: String = ""
    var runs: [TextRun]?
    /// Niveles de sangría (0...4).
    var indent: Int = 0
    /// Lenguaje del bloque de código (nil = detección automática).
    var language: String?
    /// Dirección del bloque de enlace.
    var url: String?
    var preview: LinkPreview?
    var isChecked: Bool = false
    var imageData: Data?
    /// Referencia (security-scoped bookmark) a un archivo del Finder; no se copia el archivo.
    var bookmark: Data?
    var fileName: String?

    init() {}

    /// Decodificación tolerante: las notas guardadas por versiones anteriores no tienen los campos nuevos.
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(UUID.self, forKey: .id) ?? UUID()
        kind = (try? c.decodeIfPresent(NoteBlockKind.self, forKey: .kind)) ?? .paragraph
        text = try c.decodeIfPresent(String.self, forKey: .text) ?? ""
        runs = try c.decodeIfPresent([TextRun].self, forKey: .runs)
        indent = try c.decodeIfPresent(Int.self, forKey: .indent) ?? 0
        language = try c.decodeIfPresent(String.self, forKey: .language)
        url = try c.decodeIfPresent(String.self, forKey: .url)
        preview = try c.decodeIfPresent(LinkPreview.self, forKey: .preview)
        isChecked = try c.decodeIfPresent(Bool.self, forKey: .isChecked) ?? false
        imageData = try c.decodeIfPresent(Data.self, forKey: .imageData)
        bookmark = try c.decodeIfPresent(Data.self, forKey: .bookmark)
        fileName = try c.decodeIfPresent(String.self, forKey: .fileName)
    }
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
            .map { $0.kind == .file ? ($0.fileName ?? "") : ($0.kind == .embed ? ($0.preview?.titulo ?? $0.url ?? "") : $0.text) }
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
    /// Columna del tablero: 0 por hacer, 1 en curso. «Hecho» se deduce de `completado`.
    @NSManaged var estado: Int16
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
        estado = 0
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
