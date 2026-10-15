import XCTest
@testable import Kiwu

final class BusquedaTextoTests: XCTestCase {
    func testIgnoraMayusculasYTildes() {
        XCTAssertTrue(BusquedaTexto.contiene("Código fuente", "cod"))
        XCTAssertTrue(BusquedaTexto.contiene("codigo", "CÓDIGO"))
        XCTAssertTrue(BusquedaTexto.contiene("Año nuevo", "ano"))
        XCTAssertTrue(BusquedaTexto.contiene("Física", "fisica"))
        XCTAssertTrue(BusquedaTexto.contiene("Cálculo", ""))
        XCTAssertFalse(BusquedaTexto.contiene("Cálculo", "algebra"))
    }
}
