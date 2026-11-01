package app.kiwu.android

import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.ID_EVENTOS
import app.kiwu.android.domain.Recordatorios
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * El modelo A de los avisos: las tareas con fecha pero sin hora se avisan el mismo día a la hora elegida en Ajustes,
 * sin aplicar el adelanto del chip («1 h antes», «1 día antes»). Todo con zona explícita para no depender del equipo.
 */
class AvisoSinHoraTest {
    private val zona = ZoneId.of("America/Bogota")
    private val dia = LocalDate.of(2026, 10, 30)
    private val hora = 3_600_000L

    private fun ms(h: Int, m: Int = 0, d: LocalDate = dia): Long =
        d.atTime(LocalTime.of(h, m)).atZone(zona).toInstant().toEpochMilli()

    private fun tarea(fecha: Long?, horas: Int = 24, materia: String = "M") = Deber(
        materiaId = materia, titulo = "x", fechaEntrega = fecha, recordatorioHoras = horas, creada = 0, actualizada = 0,
    )

    // 18:00 es lo que guarda la app cuando la tarea no tiene hora; 00:00, cuando es «todo el día».
    private val sinHoraTarde = tarea(ms(18))
    private val sinHoraTodoElDia = tarea(ms(0))

    @Test
    fun reconoceLasTareasSinHora() {
        assertTrue(Recordatorios.sinHora(sinHoraTarde, zona))
        assertTrue(Recordatorios.sinHora(sinHoraTodoElDia, zona))
        assertFalse(Recordatorios.sinHora(tarea(ms(8)), zona))
        assertFalse(Recordatorios.sinHora(tarea(ms(18, 30)), zona))
        assertFalse(Recordatorios.sinHora(tarea(null), zona))
        // Un evento «todo el día» también se guarda a las 00:00, pero tiene su propio horario de referencia (9:00).
        assertFalse(Recordatorios.sinHora(tarea(ms(0), materia = ID_EVENTOS), zona))
    }

    @Test
    fun sinHoraAvisaEseDiaALaHoraElegida() {
        assertEquals(ms(8), Recordatorios.momentoDeAviso(sinHoraTarde, 8 * 60, zona))
        assertEquals(ms(8), Recordatorios.momentoDeAviso(sinHoraTodoElDia, 8 * 60, zona))
        assertEquals(ms(21, 30), Recordatorios.momentoDeAviso(sinHoraTarde, 21 * 60 + 30, zona))
        // La hora por defecto son las 8:00.
        assertEquals(ms(8), Recordatorios.momentoDeAviso(sinHoraTarde, Recordatorios.HORA_SIN_HORA_POR_DEFECTO, zona))
    }

    @Test
    fun conHoraSeRespetaElAdelantoDelChip() {
        val d = tarea(ms(8))
        assertEquals(ms(8, d = dia.minusDays(1)), Recordatorios.momentoDeAviso(d, 8 * 60, zona))
        assertEquals(ms(8, d = dia.minusDays(7)), Recordatorios.momentoDeAviso(d.copy(recordatorioHoras = 168), 8 * 60, zona))
        assertEquals(ms(7), Recordatorios.momentoDeAviso(d.copy(recordatorioHoras = 1), 8 * 60, zona))
    }

    @Test
    fun losEventosDeTodoElDiaSiguenAvisandoseALasNueve() {
        // recordatorio de 1 h por defecto en los eventos: 9:00 − 1 h.
        assertEquals(ms(8), Recordatorios.momentoDeAviso(tarea(ms(0), horas = 1, materia = ID_EVENTOS), 8 * 60, zona))
    }

    @Test
    fun completadaOSinAvisoNoSuena() {
        assertNull(Recordatorios.momentoDeAviso(sinHoraTarde.copy(completado = true), 8 * 60, zona))
        assertNull(Recordatorios.momentoDeAviso(sinHoraTarde.copy(recordatorioHoras = 0), 8 * 60, zona))
        assertNull(Recordatorios.momentoDeAviso(tarea(null), 8 * 60, zona))
    }

    @Test
    fun elAvisoSuenaEnLaVentanaDelDia() {
        val d = sinHoraTarde
        assertTrue(Recordatorios.pendientesDeAviso(listOf(d), ms(7, 59), 8 * 60, zona).isEmpty())
        assertEquals(1, Recordatorios.pendientesDeAviso(listOf(d), ms(8), 8 * 60, zona).size)
        // A las 18:00 (la hora que se guarda) el aviso sigue teniendo sentido: la tarea es de hoy.
        assertEquals(1, Recordatorios.pendientesDeAviso(listOf(d), ms(18), 8 * 60, zona).size)
        assertEquals(ms(8), Recordatorios.momentoDeDisparo(d, ms(6), 8 * 60, zona))
        // Si la alarma se retrasa, sale enseguida y no al día siguiente.
        assertEquals(ms(18) + 2_000, Recordatorios.momentoDeDisparo(d, ms(18), 8 * 60, zona))
        // Ya avisada, no se repite; y al día siguiente ya no toca.
        assertNull(Recordatorios.momentoDeDisparo(d.copy(recordado = ms(8)), ms(18), 8 * 60, zona))
        assertNull(Recordatorios.momentoDeDisparo(d, ms(0, d = dia.plusDays(1)), 8 * 60, zona))
    }

    @Test
    fun unaHoraTardiaTambienSuenaEseDia() {
        val d = sinHoraTarde
        val elegida = 23 * 60 + 30
        assertEquals(ms(23, 30), Recordatorios.momentoDeAviso(d, elegida, zona))
        assertEquals(1, Recordatorios.pendientesDeAviso(listOf(d), ms(23, 45), elegida, zona).size)
    }

    @Test
    fun laTareaConHoraConservaLaVentanaDeSiempre() {
        val d = tarea(ms(8))
        assertEquals(1, Recordatorios.pendientesDeAviso(listOf(d), ms(8) - hora, 8 * 60, zona).size)
        assertTrue(Recordatorios.pendientesDeAviso(listOf(d), ms(8) + 2 * hora, 8 * 60, zona).isEmpty())
    }
}
