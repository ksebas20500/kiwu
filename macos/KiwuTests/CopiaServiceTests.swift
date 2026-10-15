import XCTest
import CoreData
@testable import Kiwu

final class CopiaServiceTests: XCTestCase {
    var persistencia: PersistenceController!
    var contexto: NSManagedObjectContext { persistencia.container.viewContext }

    override func setUp() { persistencia = PersistenceController(inMemory: true) }

    private func vacia() -> NSManagedObjectContext { PersistenceController(inMemory: true).container.viewContext }

    /// Copia de ejemplo del repositorio (formato de Android y Linux) con filas huérfanas y una clase inválida.
    private func ejemplo() throws -> Data {
        let raiz = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
        return try Data(contentsOf: raiz.appendingPathComponent("docs/contrato/copia/ejemplo.kiwu.json"))
    }

    private func conteos(_ c: NSManagedObjectContext) throws -> [Int] {
        [try c.count(for: Materia.fetchRequestTodas()), try c.count(for: Deber.fetchRequestTodas()),
         try c.count(for: Cuaderno.fetchRequestTodas()), try c.count(for: Pagina.fetchRequestTodas()),
         try c.count(for: Horario.fetchRequestTodas()), try c.count(for: Clase.fetchRequestTodas())]
    }

    func testImportaCopiaDeAndroidOLinuxDescartandoHuerfanasEInvalidas() throws {
        let resumen = try CopiaService.importar(try ejemplo(), en: contexto)
        XCTAssertEqual(resumen, ResumenImportacion(listas: 2, tareas: 1, cuadernos: 1, notas: 1, horarios: 1, clases: 2))
        XCTAssertEqual(try conteos(contexto), [2, 1, 1, 1, 1, 2])

        let tarea = try XCTUnwrap(contexto.fetch(Deber.fetchRequest()).first)
        XCTAssertEqual(tarea.titulo, "Taller de límites")
        XCTAssertEqual(tarea.estado, 1)
        XCTAssertEqual(NotaService.leer(tarea.contenidoNota).textoPlano, "Hacer los ejercicios 1 al 10")
        XCTAssertEqual(tarea.fechaEntrega, CopiaService.desdeIso("2026-10-20T23:00:00Z"))
        // El contenido que llegó como texto con JSON queda normalizado.
        let nota = try XCTUnwrap(contexto.fetch(Pagina.fetchRequest()).first)
        XCTAssertTrue(nota.favorita)
        XCTAssertEqual(nota.textoPlano, "Texto con tildes: canción")
        let clases = try contexto.fetch(Clase.fetchRequest())
        XCTAssertEqual(Set(clases.map(\.grupo)).count, 1)
        XCTAssertEqual(clases.first { $0.diaSemana == 1 }?.aviso, 10)
        XCTAssertNil(clases.first { $0.diaSemana == 3 }?.aviso)
    }

    func testImportarDosVecesNoDuplica() throws {
        try CopiaService.importar(try ejemplo(), en: contexto)
        let segunda = try CopiaService.importar(try ejemplo(), en: contexto)
        XCTAssertEqual(segunda.total, 0)
        XCTAssertEqual(try conteos(contexto), [2, 1, 1, 1, 1, 2])
    }

    func testNoResucitaLoEliminado() throws {
        try CopiaService.importar(try ejemplo(), en: contexto)
        let avisos = ReminderService()
        let materias = MateriaService(persistence: persistencia, reminders: avisos)
        let cuadernos = CuadernoService(persistence: persistencia)
        for m in try contexto.fetch(Materia.fetchRequest()) { materias.eliminar(m) }
        for c in try contexto.fetch(Cuaderno.fetchRequest()) { cuadernos.eliminar(c) }

        XCTAssertEqual(try CopiaService.importar(try ejemplo(), en: contexto).total, 0)
        XCTAssertEqual(try contexto.count(for: Materia.fetchRequest()), 0)
        XCTAssertEqual(try contexto.count(for: Deber.fetchRequest()), 0)
        XCTAssertEqual(try contexto.count(for: Pagina.fetchRequest()), 0)
    }

    func testExportarEImportarEnBaseVaciaConservaTodo() throws {
        try CopiaService.importar(try ejemplo(), en: contexto)
        let archivo = try CopiaService.exportar(en: contexto, ahora: CopiaService.desdeIso("2026-10-15T18:00:00Z")!)

        let texto = String(decoding: archivo, as: UTF8.self)
        XCTAssertTrue(texto.contains("\"formato\" : 1"))
        XCTAssertTrue(texto.contains("\"app\" : \"kiwu\""))
        XCTAssertTrue(texto.contains("2026-10-15T18:00:00Z"))
        XCTAssertTrue(texto.contains("11111111-1111-1111-1111-111111111111"), "los UUID van en mayúsculas")

        let destino = vacia()
        let resumen = try CopiaService.importar(archivo, en: destino)
        XCTAssertEqual(resumen, ResumenImportacion(listas: 2, tareas: 1, cuadernos: 1, notas: 1, horarios: 1, clases: 2))
        XCTAssertEqual(try conteos(destino), [2, 1, 1, 1, 1, 2])
        let original = try XCTUnwrap(contexto.fetch(Deber.fetchRequest()).first)
        let copia = try XCTUnwrap(destino.fetch(Deber.fetchRequest()).first)
        XCTAssertEqual(copia.id, original.id)
        XCTAssertEqual(copia.fechaActualizacion, original.fechaActualizacion)
        XCTAssertEqual(NotaService.leer(copia.contenidoNota).textoPlano, NotaService.leer(original.contenidoNota).textoPlano)
    }

    func testExportarNoIncluyeLoEliminadoNiCarpetas() throws {
        let m = MateriaService(persistence: persistencia, reminders: ReminderService()).crear(nombre: "Viva")
        m.bookmarkCarpeta = Data([1, 2, 3])
        let borrada = MateriaService(persistence: persistencia, reminders: ReminderService()).crear(nombre: "Borrada")
        MateriaService(persistence: persistencia, reminders: ReminderService()).eliminar(borrada)

        let json = try XCTUnwrap(JSONSerialization.jsonObject(with: try CopiaService.exportar(en: contexto)) as? [String: Any])
        let materias = try XCTUnwrap(json["materias"] as? [[String: Any]])
        XCTAssertEqual(materias.map { $0["nombre"] as? String }, ["Viva"])
        XCTAssertTrue(materias[0]["carpeta"] is NSNull)
    }

    func testCopiaAntiguaSinHorariosSeImporta() throws {
        let json = #"{"formato":1,"app":"kiwu","materias":[{"id":"11111111-1111-1111-1111-111111111111","nombre":"Álgebra"}]}"#
        let resumen = try CopiaService.importar(Data(json.utf8), en: contexto)
        XCTAssertEqual(resumen, ResumenImportacion(listas: 1))
    }

    func testRechazaFormatoNuevoYOtrasApps() {
        XCTAssertThrowsError(try CopiaService.importar(Data(#"{"formato":2,"app":"kiwu"}"#.utf8), en: contexto)) {
            XCTAssertEqual(($0 as? ErrorCopia)?.mensaje, "La copia es de una versión más nueva de Kiwu")
        }
        XCTAssertThrowsError(try CopiaService.importar(Data(#"{"formato":1,"app":"otra"}"#.utf8), en: contexto)) {
            XCTAssertEqual(($0 as? ErrorCopia)?.mensaje, "El archivo no es una copia de seguridad de Kiwu")
        }
        XCTAssertThrowsError(try CopiaService.importar(Data("no es json".utf8), en: contexto))
    }

    func testFechasIso() {
        let fecha = Date(timeIntervalSince1970: 1_781_535_845.9)
        XCTAssertFalse((CopiaService.iso(fecha) as? String ?? "").contains("."))
        XCTAssertEqual(CopiaService.desdeIso("2026-10-13T15:04:05Z")?.timeIntervalSince1970, 1_791_903_845)
        XCTAssertNotNil(CopiaService.desdeIso("2026-10-13T15:04:05.250Z"))
        XCTAssertNil(CopiaService.desdeIso("ayer"))
        XCTAssertEqual(CopiaService.nombreDeArchivo(CopiaService.desdeIso("2026-10-15T18:00:00Z")!, calendario: {
            var c = Calendar(identifier: .gregorian); c.timeZone = TimeZone(identifier: "UTC")!; return c }()), "kiwu-2026-10-15.kiwu.json")
    }
}
