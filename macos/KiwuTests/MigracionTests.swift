import XCTest
import CoreData
@testable import Kiwu

/// La migración ligera debe conservar lo que ya hay guardado cuando el modelo gana `eliminada`.
final class MigracionTests: XCTestCase {
    private func modeloAntiguo() -> NSManagedObjectModel {
        let modelo = PersistenceController.makeModel().copy() as! NSManagedObjectModel
        for entidad in modelo.entities {
            entidad.properties = entidad.properties.filter { $0.name != "eliminada" }
        }
        return modelo
    }

    func testAlmacenAntiguoSeMigraSinPerderDatos() throws {
        let carpeta = FileManager.default.temporaryDirectory.appendingPathComponent("kiwu-migracion-\(UUID().uuidString)")
        try FileManager.default.createDirectory(at: carpeta, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: carpeta) }
        let tareas = carpeta.appendingPathComponent("Tareas.sqlite")
        let notas = carpeta.appendingPathComponent("Notas.sqlite")

        // 1. Se crean datos con el modelo antiguo (sin `eliminada`).
        let viejo = modeloAntiguo()
        XCTAssertNil(viejo.entitiesByName["Materia"]?.attributesByName["eliminada"])
        let coordinadorViejo = NSPersistentStoreCoordinator(managedObjectModel: viejo)
        try coordinadorViejo.addPersistentStore(ofType: NSSQLiteStoreType, configurationName: "Tareas", at: tareas)
        try coordinadorViejo.addPersistentStore(ofType: NSSQLiteStoreType, configurationName: "Notas", at: notas)
        let ctxViejo = NSManagedObjectContext(concurrencyType: .mainQueueConcurrencyType)
        ctxViejo.persistentStoreCoordinator = coordinadorViejo
        let idMateria = UUID(), idDeber = UUID(), idCuaderno = UUID(), idPagina = UUID()
        let materia = NSEntityDescription.insertNewObject(forEntityName: "Materia", into: ctxViejo)
        materia.setValuesForKeys(["id": idMateria, "nombre": "Cálculo", "icono": "book", "orden": 0, "carpetaDisponible": false,
                                  "fechaCreacion": Date(), "fechaActualizacion": Date()])
        let deber = NSEntityDescription.insertNewObject(forEntityName: "Deber", into: ctxViejo)
        deber.setValuesForKeys(["id": idDeber, "titulo": "Taller", "completado": false, "recordatorioAntesEnHoras": 24,
                                "contenidoNota": Data(), "estado": 0, "fechaCreacion": Date(), "fechaActualizacion": Date(), "materia": materia])
        let cuaderno = NSEntityDescription.insertNewObject(forEntityName: "Cuaderno", into: ctxViejo)
        cuaderno.setValuesForKeys(["id": idCuaderno, "nombre": "Apuntes", "icono": "📓", "orden": 0,
                                   "fechaCreacion": Date(), "fechaActualizacion": Date()])
        let pagina = NSEntityDescription.insertNewObject(forEntityName: "Pagina", into: ctxViejo)
        pagina.setValuesForKeys(["id": idPagina, "titulo": "Tema 1", "icono": "", "contenido": Data(), "textoPlano": "hola",
                                 "favorita": false, "fechaCreacion": Date(), "fechaActualizacion": Date(), "cuaderno": cuaderno])
        // Las dos tiendas son de configuraciones distintas: cada objeto va a la suya.
        ctxViejo.assign(materia, to: coordinadorViejo.persistentStores[0]); ctxViejo.assign(deber, to: coordinadorViejo.persistentStores[0])
        ctxViejo.assign(cuaderno, to: coordinadorViejo.persistentStores[1]); ctxViejo.assign(pagina, to: coordinadorViejo.persistentStores[1])
        try ctxViejo.save()
        for almacen in coordinadorViejo.persistentStores { try coordinadorViejo.remove(almacen) }

        // 2. Se abren con el modelo nuevo y la migración automática.
        let opciones = [NSMigratePersistentStoresAutomaticallyOption: true, NSInferMappingModelAutomaticallyOption: true]
        let coordinador = NSPersistentStoreCoordinator(managedObjectModel: PersistenceController.makeModel())
        try coordinador.addPersistentStore(ofType: NSSQLiteStoreType, configurationName: "Tareas", at: tareas, options: opciones)
        try coordinador.addPersistentStore(ofType: NSSQLiteStoreType, configurationName: "Notas", at: notas, options: opciones)
        let ctx = NSManagedObjectContext(concurrencyType: .mainQueueConcurrencyType)
        ctx.persistentStoreCoordinator = coordinador

        let materias = try ctx.fetch(Materia.fetchRequest())
        XCTAssertEqual(materias.map(\.id), [idMateria])
        XCTAssertEqual(materias.first?.nombre, "Cálculo")
        XCTAssertNil(materias.first?.eliminada)
        let deberes = try ctx.fetch(Deber.fetchRequest())
        XCTAssertEqual(deberes.map(\.id), [idDeber])
        XCTAssertEqual(try ctx.fetch(Cuaderno.fetchRequest()).map(\.id), [idCuaderno])
        XCTAssertEqual(try ctx.fetch(Pagina.fetchRequest()).map(\.id), [idPagina])
    }
}
