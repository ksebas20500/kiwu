import CoreData

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

    /// Carpeta fija de los datos (no depende del nombre del ejecutable). Si solo existe la carpeta
    /// antigua `GestorUniversitario` (versiones anteriores al cambio de nombre), se copian sus bases
    /// a la nueva sin borrar nada.
    private static func directorioDeDatos() -> URL {
        let fm = FileManager.default
        let base = fm.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        let nuevo = base.appendingPathComponent("TaskFlow", isDirectory: true)
        let antiguo = base.appendingPathComponent("GestorUniversitario", isDirectory: true)
        try? fm.createDirectory(at: nuevo, withIntermediateDirectories: true)

        let tareasNuevas = nuevo.appendingPathComponent("GestorUniversitario.sqlite")
        let tareasAntiguas = antiguo.appendingPathComponent("GestorUniversitario.sqlite")
        if !fm.fileExists(atPath: tareasNuevas.path), fm.fileExists(atPath: tareasAntiguas.path) {
            for nombre in ["GestorUniversitario.sqlite", "GestorUniversitario.sqlite-wal", "GestorUniversitario.sqlite-shm",
                           "Notas.sqlite", "Notas.sqlite-wal", "Notas.sqlite-shm"] {
                let origen = antiguo.appendingPathComponent(nombre)
                if fm.fileExists(atPath: origen.path) {
                    try? fm.copyItem(at: origen, to: nuevo.appendingPathComponent(nombre))
                }
            }
        }
        return nuevo
    }

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

    private static func attribute(_ name: String, _ type: NSAttributeType, optional: Bool) -> NSAttributeDescription {
        let attribute = NSAttributeDescription()
        attribute.name = name
        attribute.attributeType = type
        attribute.isOptional = optional
        return attribute
    }
}
