import XCTest
import CoreData
@testable import Kiwu

/// Pruebas del borrado lógico (campo `eliminada`) sobre una base en memoria.
final class BorradoLogicoTests: XCTestCase {
    var persistencia: PersistenceController!
    var contexto: NSManagedObjectContext { persistencia.container.viewContext }
    var materias: MateriaService!
    var deberes: DeberService!
    var cuadernos: CuadernoService!

    override func setUp() {
        persistencia = PersistenceController(inMemory: true)
        let avisos = ReminderService()
        materias = MateriaService(persistence: persistencia, reminders: avisos)
        deberes = DeberService(persistence: persistencia, reminders: avisos)
        cuadernos = CuadernoService(persistence: persistencia)
    }

    func testEliminarMateriaMarcaMateriaYTareas() throws {
        let m = materias.crear(nombre: "Cálculo")
        let a = deberes.crear(titulo: "A", en: m)
        let b = deberes.crear(titulo: "B", en: m)
        materias.eliminar(m)

        XCTAssertNotNil(m.eliminada)
        XCTAssertNotNil(a.eliminada)
        XCTAssertNotNil(b.eliminada)
        // La fila sigue en la base (borrado lógico), pero las lecturas no la ven.
        XCTAssertEqual(try contexto.fetch(Materia.fetchRequestTodas()).count, 1)
        XCTAssertEqual(try contexto.fetch(Materia.fetchRequest()).count, 0)
        XCTAssertEqual(try contexto.fetch(Deber.fetchRequestTodas()).count, 2)
        XCTAssertEqual(try contexto.fetch(Deber.fetchRequest()).count, 0)
        XCTAssertTrue(m.deberesVivos.isEmpty)
    }

    func testEliminarTareaNoAfectaALasDemas() throws {
        let m = materias.crear(nombre: "Física")
        let a = deberes.crear(titulo: "A", en: m)
        _ = deberes.crear(titulo: "B", en: m)
        deberes.eliminar(a)
        XCTAssertEqual(try contexto.fetch(Deber.fetchRequest()).map(\.titulo), ["B"])
        XCTAssertEqual(m.deberesVivos.count, 1)
    }

    func testEliminarCuadernoMarcaCuadernoYPaginas() throws {
        let c = cuadernos.crearCuaderno(nombre: "Apuntes", icono: "📓", orden: 0)
        let p1 = cuadernos.crearPagina(en: c)
        let p2 = cuadernos.crearPagina(en: c)
        cuadernos.eliminar(c)

        XCTAssertNotNil(c.eliminada)
        XCTAssertNotNil(p1.eliminada)
        XCTAssertNotNil(p2.eliminada)
        XCTAssertEqual(try contexto.fetch(Cuaderno.fetchRequest()).count, 0)
        XCTAssertEqual(try contexto.fetch(Pagina.fetchRequest()).count, 0)
        XCTAssertEqual(try contexto.fetch(Pagina.fetchRequestTodas()).count, 2)
    }

    func testEliminarPaginaLaOcultaDeLasLecturas() throws {
        let c = cuadernos.crearCuaderno(nombre: "Apuntes", icono: "📓", orden: 0)
        let p = cuadernos.crearPagina(en: c)
        cuadernos.eliminar(p)
        XCTAssertTrue(p.estaEliminado)
        XCTAssertEqual(try contexto.fetch(Pagina.fetchRequest()).count, 0)
        XCTAssertEqual(c.paginasVivas.count, 0)
        // Un cuaderno vivo sin páginas vivas no se confunde con una página en blanco eliminada.
        XCTAssertNil(cuadernos.paginaEnBlanco(en: c))
    }

    func testPrimerCuadernoIgnoraLosEliminados() {
        let c = cuadernos.crearCuaderno(nombre: "Viejo", icono: "📓", orden: 0)
        cuadernos.eliminar(c)
        let nuevo = cuadernos.primerCuaderno()
        XCTAssertNotEqual(nuevo.id, c.id)
        XCTAssertNil(nuevo.eliminada)
        XCTAssertNil(cuadernos.cuaderno(id: c.id))
    }

    func testPurgarSoloBorraLoAntiguo() throws {
        let m = materias.crear(nombre: "Vieja")
        _ = deberes.crear(titulo: "X", en: m)
        let viva = materias.crear(nombre: "Viva")
        materias.eliminar(m)

        XCTAssertEqual(persistencia.purgar(antesDe: Date.now.addingTimeInterval(-3600)), 0)
        XCTAssertEqual(try contexto.fetch(Materia.fetchRequestTodas()).count, 2)

        XCTAssertEqual(persistencia.purgar(antesDe: Date.now.addingTimeInterval(3600)), 2)
        XCTAssertEqual(try contexto.fetch(Materia.fetchRequestTodas()).map(\.id), [viva.id])
        XCTAssertEqual(try contexto.fetch(Deber.fetchRequestTodas()).count, 0)
    }

    func testOrdenDeTareaNuevaEsMaximoMasUno() throws {
        let m1 = materias.crear(nombre: "A")
        let m2 = materias.crear(nombre: "B")
        let m3 = materias.crear(nombre: "C")
        materias.eliminar(m2)
        let m4 = materias.crear(nombre: "D")
        XCTAssertEqual([m1.orden, m3.orden, m4.orden], [0, 2, 3])
    }
}

/// Servicio de horarios: grupos de clases y borrado lógico en cascada.
final class HorarioServiceTests: XCTestCase {
    var persistencia: PersistenceController!
    var servicio: HorarioService!
    var contexto: NSManagedObjectContext { persistencia.container.viewContext }

    override func setUp() {
        persistencia = PersistenceController(inMemory: true)
        servicio = HorarioService(persistence: persistencia)
    }

    func testClaseEnVariosDiasEsUnGrupoYSeEditaJunta() throws {
        let h = servicio.crearHorario(nombre: "Universidad")
        let grupo = servicio.guardarClase(BorradorClase(horario: h, titulo: "Cálculo", dias: [1, 3], inicioMin: 600, finMin: 720))!
        var filas = try contexto.fetch(Clase.fetchRequest())
        XCTAssertEqual(filas.count, 2)
        XCTAssertEqual(Set(filas.map(\.grupo)), [grupo])
        let creada = filas.first { $0.diaSemana == 1 }!.fechaCreacion

        // Editar: quita el miércoles, añade el viernes y cambia la hora. El lunes conserva su fecha de creación.
        servicio.guardarClase(BorradorClase(grupo: grupo, horario: h, titulo: "Cálculo II", dias: [1, 5], inicioMin: 480, finMin: 540))
        filas = try contexto.fetch(Clase.fetchRequest())
        XCTAssertEqual(filas.map { Int($0.diaSemana) }.sorted(), [1, 5])
        XCTAssertTrue(filas.allSatisfy { $0.titulo == "Cálculo II" && $0.inicioMin == 480 })
        XCTAssertEqual(filas.first { $0.diaSemana == 1 }!.fechaCreacion, creada)
        XCTAssertEqual(try contexto.fetch(Clase.fetchRequestTodas()).count, 3) // el miércoles sigue como eliminado
    }

    func testHorasInvalidasNoSeGuardan() throws {
        let h = servicio.crearHorario(nombre: "Trabajo")
        XCTAssertNil(servicio.guardarClase(BorradorClase(horario: h, titulo: "X", dias: [1], inicioMin: 600, finMin: 600)))
        XCTAssertEqual(try contexto.fetch(Clase.fetchRequestTodas()).count, 0)
    }

    func testEliminarGrupoYHorario() throws {
        let h = servicio.crearHorario(nombre: "Ocio", icono: "🌴")
        let g = servicio.guardarClase(BorradorClase(horario: h, titulo: "Yoga", dias: [2, 4], inicioMin: 1080, finMin: 1140))!
        servicio.guardarClase(BorradorClase(horario: h, titulo: "Cine", dias: [6], inicioMin: 1200, finMin: 1320))
        servicio.eliminarGrupo(g)
        XCTAssertEqual(try contexto.fetch(Clase.fetchRequest()).map(\.titulo), ["Cine"])
        servicio.eliminar(h)
        XCTAssertEqual(try contexto.fetch(Horario.fetchRequest()).count, 0)
        XCTAssertEqual(try contexto.fetch(Clase.fetchRequest()).count, 0)
        XCTAssertNotNil(h.eliminada)
    }

    func testClaseAdmiteTerminarAMedianoche() throws {
        let h = servicio.crearHorario(nombre: "Noche")
        XCTAssertNotNil(servicio.guardarClase(BorradorClase(horario: h, titulo: "Turno", dias: [1], inicioMin: 1320, finMin: 1440)))
    }
}
