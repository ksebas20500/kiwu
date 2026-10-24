package app.kiwu.android

import app.kiwu.android.data.AjustesUi
import app.kiwu.android.ui.ajustes.ResumenAjustes
import app.kiwu.android.ui.ajustes.SeccionAjustes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResumenAjustesTest {
    @Test
    fun aparienciaResumeTemaColorYFondo() {
        assertEquals("Del sistema · color coral", ResumenAjustes.apariencia(AjustesUi()))
        assertEquals("Oscuro · color azul · con fondo", ResumenAjustes.apariencia(AjustesUi(tema = "oscuro", acento = "azul", fondoUri = "x")))
        assertEquals("Claro · color propio", ResumenAjustes.apariencia(AjustesUi(tema = "claro", acento = "#FF8800")))
        assertEquals("Claro · color del sistema", ResumenAjustes.apariencia(AjustesUi(tema = "claro", acento = "sistema")))
    }

    @Test
    fun menuResumeAspectoOrdenYContadores() {
        assertEquals("Plegable · orden manual", ResumenAjustes.menu(AjustesUi()))
        assertEquals(
            "Compacto · orden alfabético · sin contadores",
            ResumenAjustes.menu(AjustesUi(menuAspecto = "compacto", menuOrden = "alfabetico", menuContadores = false)),
        )
        assertEquals("Con color · por tareas pendientes", ResumenAjustes.menu(AjustesUi(menuAspecto = "color", menuOrden = "pendientes")))
    }

    @Test
    fun avisosResumeElEstadoYElSegundoAviso() {
        assertEquals("Notificaciones desactivadas", ResumenAjustes.avisos(AjustesUi(segundoAvisoMin = 10), false))
        assertEquals("Activados", ResumenAjustes.avisos(AjustesUi(), true))
        assertEquals("Activados · segundo aviso 10 min antes", ResumenAjustes.avisos(AjustesUi(segundoAvisoMin = 10), true))
        assertEquals("Activados · segundo aviso 1 h antes", ResumenAjustes.avisos(AjustesUi(segundoAvisoMin = 60), true))
    }

    @Test
    fun laRachaSoloSeMencionaCuandoEstaApagada() {
        assertEquals("Plegable · orden manual · sin racha", ResumenAjustes.menu(AjustesUi(rachaVisible = false)))
        assertEquals("Activados · sin aviso de racha", ResumenAjustes.avisos(AjustesUi(rachaAviso = false), true))
        assertEquals(
            "Activados · segundo aviso 5 min antes · sin aviso de racha",
            ResumenAjustes.avisos(AjustesUi(segundoAvisoMin = 5, rachaAviso = false), true),
        )
    }

    @Test
    fun lasSeccionesSeRecuperanPorSuId() {
        for (s in SeccionAjustes.entries) assertEquals(s, SeccionAjustes.deId(s.id))
        assertNull(SeccionAjustes.deId(null))
        assertNull(SeccionAjustes.deId("no-existe"))
    }
}
