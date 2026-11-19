package app.kiwu.android

import app.kiwu.android.domain.Bloque
import app.kiwu.android.domain.TiposBloque
import app.kiwu.android.ui.notas.PdfNota
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exportación a PDF: el nombre del archivo y la numeración de las listas. El dibujo en sí se prueba a mano en el
 * teléfono (Robolectric no trae `PdfDocument`).
 */
class PdfNotaTest {

    @Test
    fun elNombreDelArchivoEsSeguroYReconocible() {
        assertEquals("calculo-integral.pdf", PdfNota.nombreArchivo("Cálculo Integral"))
        assertEquals("nota.pdf", PdfNota.nombreArchivo("   "))
        assertEquals("a-b-c.pdf", PdfNota.nombreArchivo("¿A/B? C!"))
        assertEquals("nino.pdf", PdfNota.nombreArchivo("Niño"))
        assertTrue(PdfNota.nombreArchivo("x".repeat(200)).length <= 52)
    }

    @Test
    fun lasListasNumeradasSiguenSuCuentaYReinicianAlCambiarDeBloque() {
        val bloques = listOf(
            Bloque(kind = TiposBloque.PARRAFO, text = "intro"),
            Bloque(kind = TiposBloque.NUMERADA, text = "uno"),
            Bloque(kind = TiposBloque.NUMERADA, text = "dos"),
            Bloque(kind = TiposBloque.NUMERADA, text = "dos bis", indent = 1),
            Bloque(kind = TiposBloque.VINETAS, text = "aparte"),
            Bloque(kind = TiposBloque.NUMERADA, text = "otra vez uno"),
            Bloque(kind = TiposBloque.NUMERADA, text = "otra vez dos"),
        )
        assertEquals(listOf(0, 1, 2, 3, 0, 1, 2), PdfNota.numeros(bloques))
    }

    @Test
    fun unaListaNumeradaEmpiezaEnUno() {
        val bloques = listOf(Bloque(kind = TiposBloque.NUMERADA, text = "primero"))
        assertEquals(listOf(1), PdfNota.numeros(bloques))
    }
}
