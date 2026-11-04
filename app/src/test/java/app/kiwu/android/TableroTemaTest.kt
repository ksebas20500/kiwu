package app.kiwu.android

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import app.kiwu.android.domain.Columna
import app.kiwu.android.ui.Acentos
import app.kiwu.android.ui.contraste
import app.kiwu.android.ui.esquemaDe
import app.kiwu.android.ui.minutoAjustado
import app.kiwu.android.ui.tareas.columnaEn
import app.kiwu.android.ui.tareas.velocidadDeDesplazamiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TableroTemaTest {
    private val limites = mapOf(
        Columna.POR_HACER to Rect(0f, 0f, 100f, 500f),
        Columna.EN_CURSO to Rect(108f, 0f, 208f, 500f),
        Columna.HECHO to Rect(216f, 0f, 316f, 500f),
    )

    @Test
    fun laColumnaDestinoSeDeduceDeLaPosicionDelDedo() {
        assertEquals(Columna.POR_HACER, columnaEn(Offset(50f, 100f), limites))
        assertEquals(Columna.EN_CURSO, columnaEn(Offset(150f, 499f), limites))
        assertEquals(Columna.HECHO, columnaEn(Offset(300f, 1f), limites))
    }

    @Test
    fun soltarFueraDeLasColumnasNoDaDestino() {
        assertNull(columnaEn(Offset(104f, 100f), limites)) // hueco entre columnas
        assertNull(columnaEn(Offset(50f, 600f), limites))
        assertNull(columnaEn(Offset(50f, 100f), emptyMap()))
    }

    @Test
    fun elDesplazamientoAutomaticoSoloActuaCercaDelBorde() {
        assertEquals(0f, velocidadDeDesplazamiento(500f, 0f, 1000f, 100f, 20f), 0f)
        assertEquals(-20f, velocidadDeDesplazamiento(0f, 0f, 1000f, 100f, 20f), 0f)
        assertEquals(20f, velocidadDeDesplazamiento(1000f, 0f, 1000f, 100f, 20f), 0f)
        assertEquals(-10f, velocidadDeDesplazamiento(50f, 0f, 1000f, 100f, 20f), 0.001f)
        assertEquals(10f, velocidadDeDesplazamiento(950f, 0f, 1000f, 100f, 20f), 0.001f)
        assertEquals(0f, velocidadDeDesplazamiento(10f, 0f, 100f, 100f, 20f), 0f) // zona demasiado pequeña
    }

    @Test
    fun cadaAcentoPredefinidoTieneContrasteLegibleEnClaroYOscuro() {
        for (a in Acentos.predefinidos) {
            for (oscuro in listOf(false, true)) {
                val e = esquemaDe(a.id, oscuro)
                val nombre = "${a.id} ${if (oscuro) "oscuro" else "claro"}"
                assertTrue("$nombre: texto sobre primario", contraste(e.onPrimary, e.primary) >= 4.5f)
                assertTrue("$nombre: texto sobre contenedor", contraste(e.onPrimaryContainer, e.primaryContainer) >= 4.5f)
                assertTrue("$nombre: texto sobre fondo", contraste(e.onBackground, e.background) >= 7f)
                assertTrue("$nombre: texto secundario", contraste(e.onSurfaceVariant, e.surfaceVariant) >= 4.5f)
                assertTrue("$nombre: primario sobre fondo", contraste(e.primary, e.background) >= 3f)
            }
        }
    }

    @Test
    fun cualquierColorPersonalizadoSigueSiendoLegible() {
        val semillas = listOf("#FFFF00", "#00FFFF", "#FFFFFF", "#000000", "#808080", "#FF00FF", "#7FFF00", "#123456")
        for (hex in semillas) {
            for (oscuro in listOf(false, true)) {
                val e = esquemaDe(hex, oscuro)
                assertTrue("$hex $oscuro primario", contraste(e.onPrimary, e.primary) >= 4.5f)
                assertTrue("$hex $oscuro contenedor", contraste(e.onPrimaryContainer, e.primaryContainer) >= 4.5f)
                assertTrue("$hex $oscuro fondo", contraste(e.onBackground, e.background) >= 7f)
            }
        }
    }

    @Test
    fun elCoralOriginalNoCambia() {
        assertEquals(Color(0xFFC44B35), esquemaDe("coral", false).primary)
        assertEquals(Color(0xFFFFB4A5), esquemaDe("coral", true).primary)
    }

    @Test
    fun losColoresPersonalizadosSeGuardanComoTexto() {
        val c = Acentos.personalizado("#2F6FD0")!!
        assertEquals("#2F6FD0", Acentos.aTexto(c))
        assertNull(Acentos.personalizado("azul"))
        assertNull(Acentos.personalizado("#GGGGGG"))
        assertEquals("sistema", Acentos.SISTEMA)
    }

    @Test
    fun losMinutosDeLaRuedaSeAjustanAlPasoMasCercano() {
        assertEquals(0, minutoAjustado(0, 5))
        assertEquals(5, minutoAjustado(7, 5))
        assertEquals(5, minutoAjustado(3, 5))
        assertEquals(0, minutoAjustado(2, 5))
        assertEquals(30, minutoAjustado(30, 5))
        assertEquals(0, minutoAjustado(58, 5)) // 60 vuelve a 0
        assertEquals(55, minutoAjustado(55, 5))
        assertEquals(59, minutoAjustado(59, 1))
        assertEquals(0, minutoAjustado(0, 1))
    }
}
