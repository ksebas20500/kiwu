import CoreData
import SQLite3

final class PersistenceController {
    static let shared = PersistenceController()

    let container: NSPersistentContainer

    init(inMemory: Bool = false) {
        container = NSPersistentContainer(name: "GestorUniversitario", managedObjectModel: Self.makeModel())

        // Tareas y notas viven en almacenes separados: así añadir el módulo de notas
        // no altera la base de datos de tareas que ya existe.
        let directorio = Self.directorioDeDatos()
        let tareas = NSPersistentStoreDescription(url: directorio.appendingPathComponent("GestorUniversitario.sqlite"))
        tareas.configuration = "Tareas"
        let notas = NSPersistentStoreDescription(url: directorio.appendingPathComponent("Notas.sqlite"))
        notas.configuration = "Notas"
        if inMemory {
            tareas.type = NSInMemoryStoreType
            notas.type = NSInMemoryStoreType
        }
        container.persistentStoreDescriptions = [tareas, notas]

        container.loadPersistentStores { _, error in
            if let error {
                fatalError("No se pudo cargar la base local: \(error)")
            }
        }
        container.viewContext.automaticallyMergesChangesFromParent = true
        container.viewContext.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy
    }

    private static func directorioDeDatos() -> URL { CarpetaDatos.url }

    func save() {
        let context = container.viewContext
        guard context.hasChanges else { return }
        do {
            try context.save()
            WidgetDataService.programarActualizacion(context)
        } catch {
            assertionFailure("No se pudo guardar la base local: \(error)")
        }
    }

    private static func makeModel() -> NSManagedObjectModel {
        let model = NSManagedObjectModel()

        let materia = NSEntityDescription()
        materia.name = "Materia"
        materia.managedObjectClassName = "Materia"
        materia.properties = [
            attribute("id", .UUIDAttributeType, optional: false),
            attribute("nombre", .stringAttributeType, optional: false),
            attribute("icono", .stringAttributeType, optional: false),
            attribute("orden", .integer16AttributeType, optional: false),
            attribute("bookmarkCarpeta", .binaryDataAttributeType, optional: true),
            attribute("carpetaDisponible", .booleanAttributeType, optional: false),
            attribute("fechaCreacion", .dateAttributeType, optional: false),
            attribute("fechaActualizacion", .dateAttributeType, optional: false)
        ]

        let deber = NSEntityDescription()
        deber.name = "Deber"
        deber.managedObjectClassName = "Deber"
        deber.properties = [
            attribute("id", .UUIDAttributeType, optional: false),
            attribute("titulo", .stringAttributeType, optional: false),
            attribute("completado", .booleanAttributeType, optional: false),
            attribute("fechaEntrega", .dateAttributeType, optional: true),
            attribute("recordatorioAntesEnHoras", .integer16AttributeType, optional: false),
            attribute("fechaCompletado", .dateAttributeType, optional: true),
            attribute("contenidoNota", .binaryDataAttributeType, optional: false),
            attribute("estado", .integer16AttributeType, optional: false, defaultValue: 0),
            attribute("fechaCreacion", .dateAttributeType, optional: false),
            attribute("fechaActualizacion", .dateAttributeType, optional: false)
        ]

        let deberes = NSRelationshipDescription()
        deberes.name = "deberes"
        deberes.destinationEntity = deber
        deberes.minCount = 0
        deberes.maxCount = 0
        deberes.deleteRule = .cascadeDeleteRule

        let materiaDeber = NSRelationshipDescription()
        materiaDeber.name = "materia"
        materiaDeber.destinationEntity = materia
        materiaDeber.minCount = 1
        materiaDeber.maxCount = 1
        materiaDeber.deleteRule = .nullifyDeleteRule

        deberes.inverseRelationship = materiaDeber
        materiaDeber.inverseRelationship = deberes
        materia.properties.append(deberes)
        deber.properties.append(materiaDeber)

        let cuaderno = NSEntityDescription()
        cuaderno.name = "Cuaderno"
        cuaderno.managedObjectClassName = "Cuaderno"
        cuaderno.properties = [
            attribute("id", .UUIDAttributeType, optional: false),
            attribute("nombre", .stringAttributeType, optional: false),
            attribute("icono", .stringAttributeType, optional: false),
            attribute("orden", .integer16AttributeType, optional: false),
            attribute("fechaCreacion", .dateAttributeType, optional: false),
            attribute("fechaActualizacion", .dateAttributeType, optional: false)
        ]

        let pagina = NSEntityDescription()
        pagina.name = "Pagina"
        pagina.managedObjectClassName = "Pagina"
        pagina.properties = [
            attribute("id", .UUIDAttributeType, optional: false),
            attribute("titulo", .stringAttributeType, optional: false),
            attribute("icono", .stringAttributeType, optional: false),
            attribute("contenido", .binaryDataAttributeType, optional: false),
            attribute("textoPlano", .stringAttributeType, optional: false),
            attribute("favorita", .booleanAttributeType, optional: false),
            attribute("fechaCreacion", .dateAttributeType, optional: false),
            attribute("fechaActualizacion", .dateAttributeType, optional: false)
        ]

        let paginas = NSRelationshipDescription()
        paginas.name = "paginas"
        paginas.destinationEntity = pagina
        paginas.minCount = 0
        paginas.maxCount = 0
        paginas.deleteRule = .cascadeDeleteRule

        let paginaCuaderno = NSRelationshipDescription()
        paginaCuaderno.name = "cuaderno"
        paginaCuaderno.destinationEntity = cuaderno
        paginaCuaderno.minCount = 1
        paginaCuaderno.maxCount = 1
        paginaCuaderno.deleteRule = .nullifyDeleteRule

        paginas.inverseRelationship = paginaCuaderno
        paginaCuaderno.inverseRelationship = paginas
        cuaderno.properties.append(paginas)
        pagina.properties.append(paginaCuaderno)

        model.entities = [materia, deber, cuaderno, pagina]
        model.setEntities([materia, deber], forConfigurationName: "Tareas")
        model.setEntities([cuaderno, pagina], forConfigurationName: "Notas")
        return model
    }

    private static func attribute(_ name: String, _ type: NSAttributeType, optional: Bool, defaultValue: Any? = nil) -> NSAttributeDescription {
        let attribute = NSAttributeDescription()
        attribute.defaultValue = defaultValue
        attribute.name = name
        attribute.attributeType = type
        attribute.isOptional = optional
        return attribute
    }
}

/// Carpeta fija de los datos de la app (`Application Support/Kiwu`).
///
/// Al abrir Kiwu se copian, sin borrar nada, los datos de las versiones anteriores: primero de `TaskFlow`
/// (el nombre anterior de la app) y, si no existe, de `GestorUniversitario`. Se copia la carpeta entera:
/// bases de tareas y notas, y la imagen de fondo si la había. Si Kiwu ya tenía bases pero están vacías
/// (por ejemplo, de una primera apertura que no encontró los datos), se apartan y se copian las antiguas.
enum CarpetaDatos {
    static let url: URL = preparar(base: FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0])

    private static let baseDeTareas = "GestorUniversitario.sqlite"

    static func preparar(base: URL) -> URL {
        let fm = FileManager.default
        let nuevo = base.appendingPathComponent("Kiwu", isDirectory: true)
        try? fm.createDirectory(at: nuevo, withIntermediateDirectories: true)

        let tareasNuevas = nuevo.appendingPathComponent(baseDeTareas)
        if fm.fileExists(atPath: tareasNuevas.path), tieneDatos(nuevo) { return nuevo }

        for anterior in ["TaskFlow", "GestorUniversitario"] {
            let origen = base.appendingPathComponent(anterior, isDirectory: true)
            guard fm.fileExists(atPath: origen.appendingPathComponent(baseDeTareas).path), tieneDatos(origen) else { continue }

            // Aparta las bases vacías de Kiwu (no se borran) para que no se mezclen con las copiadas.
            let apartadas = nuevo.appendingPathComponent("vacias-\(Int(Date().timeIntervalSince1970))", isDirectory: true)
            for nombre in (try? fm.contentsOfDirectory(atPath: nuevo.path)) ?? [] where nombre.contains(".sqlite") {
                try? fm.createDirectory(at: apartadas, withIntermediateDirectories: true)
                try? fm.moveItem(at: nuevo.appendingPathComponent(nombre), to: apartadas.appendingPathComponent(nombre))
            }
            for nombre in (try? fm.contentsOfDirectory(atPath: origen.path)) ?? [] where !nombre.hasPrefix(".") {
                let destino = nuevo.appendingPathComponent(nombre)
                if !fm.fileExists(atPath: destino.path) {
                    try? fm.copyItem(at: origen.appendingPathComponent(nombre), to: destino)
                }
            }
            break
        }
        return nuevo
    }

    /// true si la carpeta tiene alguna lista, tarea, nota o página guardada.
    private static func tieneDatos(_ carpeta: URL) -> Bool {
        let consultas = [(baseDeTareas, "SELECT COUNT(*) FROM ZMATERIA"),
                         (baseDeTareas, "SELECT COUNT(*) FROM ZDEBER"),
                         ("Notas.sqlite", "SELECT COUNT(*) FROM ZPAGINA")]
        return consultas.contains { contar(carpeta.appendingPathComponent($0.0), $0.1) > 0 }
    }

    private static func contar(_ archivo: URL, _ sql: String) -> Int {
        var db: OpaquePointer?
        guard sqlite3_open_v2(archivo.path, &db, SQLITE_OPEN_READONLY, nil) == SQLITE_OK else { sqlite3_close(db); return 0 }
        defer { sqlite3_close(db) }
        var consulta: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &consulta, nil) == SQLITE_OK else { return 0 }
        defer { sqlite3_finalize(consulta) }
        return sqlite3_step(consulta) == SQLITE_ROW ? Int(sqlite3_column_int(consulta, 0)) : 0
    }
}
