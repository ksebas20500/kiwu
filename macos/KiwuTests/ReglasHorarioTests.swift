import XCTest
@testable import Kiwu

/// Vectores de prueba copiados de `HorarioReglasTest.kt` (Android). Zona horaria: America/Bogota.
final class ReglasHorarioTests: XCTestCase {
    private var cal: Calendar = {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = TimeZone(identifier: "America/Bogota")!
        return c
    }()

    private func clase(_ titulo: String, dia: Int, _ ini: Int, _ fin: Int, grupo: UUID = UUID(), aula: String = "", aviso: Int? = nil) -> ClaseHorario {
        ClaseHorario(grupo: grupo, titulo: titulo, diaSemana: dia, inicioMin: ini * 60, finMin: fin * 60, aula: aula, avisoMin: aviso)
    }

    private func fecha(_ d: Int, _ mes: Int, _ h: Int, _ m: Int = 0) -> Date {
        cal.date(from: DateComponents(year: 2026, month: mes, day: d, hour: h, minute: m))!
    }

    func testValida() {
        XCTAssertTrue(ReglasHorario.valida(inicio: 480, fin: 600))
        XCTAssertTrue(ReglasHorario.valida(inicio: 1380, fin: 1440))
        XCTAssertFalse(ReglasHorario.valida(inicio: 600, fin: 600))
        XCTAssertFalse(ReglasHorario.valida(inicio: 600, fin: 540))
        XCTAssertFalse(ReglasHorario.valida(inicio: -1, fin: 60))
        XCTAssertFalse(ReglasHorario.valida(inicio: 1440, fin: 1470))
    }

    func testTextos() {
        XCTAssertEqual(ReglasHorario.hora(0), "00:00")
        XCTAssertEqual(ReglasHorario.hora(1440), "24:00")
        XCTAssertEqual(ReglasHorario.rango(clase("A", dia: 1, 8, 10)), "08:00 – 10:00")
        XCTAssertEqual(ReglasHorario.nombreDia(1), "Lunes")
        XCTAssertEqual(ReglasHorario.nombreDia(7), "Domingo")
        XCTAssertEqual(ReglasHorario.textoAviso(clase("A", dia: 1, 8, 10)), "Empieza a las 08:00")
        XCTAssertEqual(ReglasHorario.textoAviso(clase("A", dia: 1, 8, 10, aula: "204")), "Empieza a las 08:00 · 204")
        XCTAssertEqual(ReglasHorario.cuando(dias: 0, dia: 1), "hoy")
        XCTAssertEqual(ReglasHorario.cuando(dias: 1, dia: 2), "mañana")
        XCTAssertEqual(ReglasHorario.cuando(dias: 3, dia: 4), "jueves")
    }

    func testDiaIso() {
        XCTAssertEqual(ReglasHorario.diaDe(fecha(5, 10, 12), calendario: cal), 1)   // lunes 5 oct 2026
        XCTAssertEqual(ReglasHorario.diaDe(fecha(11, 10, 12), calendario: cal), 7)  // domingo
    }

    func testChoques() {
        let calculo = clase("Cálculo", dia: 1, 8, 10), fisica = clase("Física", dia: 3, 8, 10)
        let existentes = [calculo, fisica]
        XCTAssertTrue(ReglasHorario.choques(existentes: existentes, grupo: nil, dias: [1], inicio: 600, fin: 660).isEmpty)
        XCTAssertEqual(ReglasHorario.choques(existentes: existentes, grupo: nil, dias: [1], inicio: 540, fin: 660).map(\.titulo), ["Cálculo"])
        XCTAssertTrue(ReglasHorario.choques(existentes: existentes, grupo: nil, dias: [2], inicio: 480, fin: 600).isEmpty)
        XCTAssertEqual(ReglasHorario.choques(existentes: existentes, grupo: nil, dias: [1, 3], inicio: 540, fin: 570).count, 2)
        // Editar su propio grupo no choca consigo mismo.
        XCTAssertTrue(ReglasHorario.choques(existentes: existentes, grupo: calculo.grupo, dias: [1], inicio: 540, fin: 660).isEmpty)
    }

    func testEnCursoYSiguiente() {
        let a = clase("A", dia: 1, 8, 10), b = clase("B", dia: 1, 14, 16), c = clase("C", dia: 3, 9, 10)
        let todas = [c, b, a]
        XCTAssertEqual(ReglasHorario.enCurso(todas, dia: 1, minuto: 540)?.titulo, "A")
        XCTAssertNil(ReglasHorario.enCurso(todas, dia: 1, minuto: 600))
        let s1 = ReglasHorario.siguiente(todas, dia: 1, minuto: 540)
        XCTAssertEqual(s1?.clase.titulo, "B"); XCTAssertEqual(s1?.dias, 0)
        let s2 = ReglasHorario.siguiente(todas, dia: 1, minuto: 17 * 60)
        XCTAssertEqual(s2?.clase.titulo, "C"); XCTAssertEqual(s2?.dias, 2)
        let s3 = ReglasHorario.siguiente(todas, dia: 3, minuto: 11 * 60)
        XCTAssertEqual(s3?.clase.titulo, "A"); XCTAssertEqual(s3?.dias, 5)
        XCTAssertNil(ReglasHorario.siguiente([], dia: 1, minuto: 0))
    }

    func testProximoInicio() {
        let lunes = clase("A", dia: 1, 8, 10)
        XCTAssertEqual(ReglasHorario.proximoInicio(lunes, desde: fecha(30, 9, 12), calendario: cal), fecha(5, 10, 8))
        XCTAssertEqual(ReglasHorario.proximoInicio(lunes, desde: fecha(5, 10, 7), calendario: cal), fecha(5, 10, 8))
        XCTAssertEqual(ReglasHorario.proximoInicio(lunes, desde: fecha(5, 10, 8, 1), calendario: cal), fecha(12, 10, 8))
    }

    func testMomentoDeAviso() {
        let lunes = clase("A", dia: 1, 8, 10, aviso: 10)
        XCTAssertEqual(ReglasHorario.momentoDeAviso(lunes, desde: fecha(30, 9, 12), calendario: cal), fecha(5, 10, 7, 50))
        XCTAssertEqual(ReglasHorario.momentoDeAviso(lunes, desde: fecha(5, 10, 7, 55), calendario: cal), fecha(12, 10, 7, 50))
        XCTAssertEqual(ReglasHorario.momentoDeAviso(lunes, desde: fecha(5, 10, 7, 40), calendario: cal), fecha(5, 10, 7, 50))
        XCTAssertNil(ReglasHorario.momentoDeAviso(clase("B", dia: 1, 8, 10), desde: fecha(30, 9, 12), calendario: cal))
    }

    func testAvisoDeMedianocheCaeElDiaAnterior() {
        var c = clase("Guardia", dia: 2, 0, 1, aviso: 10)
        c.inicioMin = 5
        // Martes 00:05 con aviso de 10 min → lunes 23:55.
        XCTAssertEqual(ReglasHorario.momentoDeAviso(c, desde: fecha(5, 10, 12), calendario: cal), fecha(5, 10, 23, 55))
    }
}

final class SelectorHoraTests: XCTestCase {
    func testConversionDeMinutosYHora() {
        for m in [0, 7, 60, 607, 720, 779, 1439] {
            XCTAssertEqual(ReglasHorario.minutos(de: ReglasHorario.fecha(deMinutos: m), esFin: false), m)
        }
        XCTAssertEqual(ReglasHorario.minutos(de: ReglasHorario.fecha(deMinutos: 600), esFin: true), 600)
    }

    func testMedianocheAlFinalEsMilCuatrocientos() {
        XCTAssertEqual(ReglasHorario.minutos(de: ReglasHorario.fecha(deMinutos: 1440), esFin: true), 1440)
        XCTAssertEqual(ReglasHorario.minutos(de: ReglasHorario.fecha(deMinutos: 0), esFin: false), 0)
        XCTAssertTrue(ReglasHorario.valida(inicio: 1380, fin: ReglasHorario.minutos(de: ReglasHorario.fecha(deMinutos: 1440), esFin: true)))
    }
}
