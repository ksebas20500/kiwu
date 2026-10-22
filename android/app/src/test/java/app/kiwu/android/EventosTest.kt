package app.kiwu.android

import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.Elemento
import app.kiwu.android.domain.ID_EVENTOS
import app.kiwu.android.domain.Recordatorios
import app.kiwu.android.domain.Reglas
import app.kiwu.android.domain.esEvento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class EventosTest {
    private val zona = ZoneId.of("America/Bogota")
    private val reglas = Reglas(zona)

    // miércoles 30 de septiembre de 2026, 12:00 locales
    private val ahora = LocalDateTime.of(2026, 9, 30, 12, 0).atZone(zona).toInstant().toEpochMilli()

    private fun en(dias: Long, hora: Int = 0): Long =
        LocalDateTime.of(2026, 9, 30, hora, 0).plusDays(dias).atZone(zona).toInstant().toEpochMilli()

    private fun deber(titulo: String, materia: String, fecha: Long?) =
        Deber(id = titulo, materiaId = materia, titulo = titulo, fechaEntrega = fecha, creada = 1, actualizada = 1)

    private fun evento(titulo: String, fecha: Long?) = deber(titulo, ID_EVENTOS, fecha)
    private fun tarea(titulo: String, fecha: Long?) = deber(titulo, "M-UNI", fecha)

    @Test
    fun unDeberEsEventoSoloSiViveEnLaListaDeEventos() {
        assertTrue(evento("cumple", en(2)).esEvento)
        assertFalse(tarea("informe", en(2)).esEvento)
    }

    @Test
    fun unEventoPasadoNoVenceNiSaleEnHoyNiEnTodas() {
        val ayer = evento("charla", en(-1, 18))
        assertFalse(reglas.estaVencido(ayer, ahora))
        assertTrue(reglas.pendientes(listOf(ayer), Elemento.Hoy, ahora).isEmpty())
        assertTrue(reglas.pendientes(listOf(ayer), Elemento.Todos, ahora).isEmpty())
        // pero sigue en su lista
        assertEquals(1, reglas.pendientes(listOf(ayer), Elemento.DeMateria(ID_EVENTOS), ahora).size)
    }

    @Test
    fun unEventoDeHoyDeTodoElDiaSaleEnHoyAunqueYaPasaraLaMedianoche() {
        val hoy = evento("cumple", en(0, 0))
        assertEquals(1, reglas.pendientes(listOf(hoy), Elemento.Hoy, ahora).size)
        assertEquals(1, reglas.pendientes(listOf(hoy), Elemento.Todos, ahora).size)
    }

    @Test
    fun losEventosNoCuentanComoPendientesNiSonCompletadas() {
        val lista = listOf(evento("cumple", en(0, 0)), evento("charla", en(2, 18)), tarea("informe", en(0, 18)))
        assertEquals(1, reglas.conteo(lista, Elemento.Hoy, ahora))
        assertEquals(1, reglas.conteo(lista, Elemento.Todos, ahora))
        assertTrue(reglas.completados(lista, Elemento.Completadas, ahora).isEmpty())
    }

    @Test
    fun losEventosFuturosSalenEnProximosYEnElDiaDelCalendario() {
        val lista = listOf(evento("charla", en(3, 18)))
        assertEquals(1, reglas.pendientes(lista, Elemento.Proximos, ahora).size)
        assertEquals(1, reglas.delDia(lista, LocalDateTime.of(2026, 10, 3, 0, 0).toLocalDate()).size)
    }

    @Test
    fun unEventoDeTodoElDiaAvisaRespectoDeLasNueveDeLaManana() {
        val dia = evento("cumple", en(1, 0))                 // 1 oct, 00:00
        val nueve = en(1, 9)
        assertEquals(nueve, Recordatorios.referencia(dia, zona))
        // una tarea de todo el día no cambia: sigue siendo la medianoche
        assertEquals(en(1, 0), Recordatorios.referencia(tarea("informe", en(1, 0)), zona))
        // con hora concreta, la referencia es esa hora
        assertEquals(en(1, 15), Recordatorios.referencia(evento("charla", en(1, 15)), zona))
    }

    @Test
    fun elTextoDelAvisoDiceEmpiezaEnVezDeVence() {
        // 7 minutos de más: así nunca cae en la medianoche de la zona del equipo que corre la prueba.
        val inicio = ahora + 5 * 3_600_000L + 7 * 60_000L
        val e = evento("charla", inicio)
        assertEquals("Empieza en 5 h", Recordatorios.textoAviso(e, ahora))
        assertEquals("Empezó hace 10 min", Recordatorios.textoAviso(e, inicio + 600_000))
    }
}
