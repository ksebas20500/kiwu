import XCTest
@testable import Kiwu

final class ColorAcentoTests: XCTestCase {
    func testPredefinidosYHexPersonalizado() {
        XCTAssertEqual(ColorAcento.hex(de: "coral"), "#E8705A")
        XCTAssertEqual(ColorAcento.hex(de: "#2f6fd0"), "#2F6FD0")
        XCTAssertNil(ColorAcento.hex(de: "sistema"))
        XCTAssertNil(ColorAcento.hex(de: "#XYZ123"))
    }

    func testTodosLosColoresLlegaAContraste() {
        for oscuro in [false, true] {
            let fondo = oscuro ? ColorAcento.luminancia(r: 0.12, g: 0.12, b: 0.12) : 1
            for p in ColorAcento.predefinidos + [("x", "x", "#FFFF00"), ("y", "y", "#101010")] {
                let h = ColorAcento.ajustado(hex: p.hex, oscuro: oscuro)
                let c = ColorAcento.rgb(hex: h)!
                XCTAssertGreaterThanOrEqual(ColorAcento.contraste(ColorAcento.luminancia(r: c.r, g: c.g, b: c.b), fondo), 4.5, "\(p.hex) \(oscuro)")
            }
        }
    }

    func testUnColorQueYaContrastaNoCambia() {
        XCTAssertEqual(ColorAcento.ajustado(hex: "#2F6FD0", oscuro: false), "#2F6FD0")
    }
}
