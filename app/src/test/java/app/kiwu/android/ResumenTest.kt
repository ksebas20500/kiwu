package app.kiwu.android

import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.Resumenes
import app.kiwu.android.domain.ResumenSemanal
import app.kiwu.android.domain.SesionEstudio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ResumenTest {
    private val zona = ZoneId.of("America/Bogota")

    /** miércoles 30 de septiembre de 2026; lunes de esa semana = 28, domingo = 4 de octubre. */
    private fun ms(dia: Int, hora: Int, minuto: Int = 0): Long =
        LocalDateTime.of(2026, 9, dia, hora, minuto).atZone(zona).toInstant().toEpochMilli()

    private val lunes = LocalDate.of(2026, 9, 28)
    private val domingo = LocalDate.of(2026, 10, 4)

    private fun tarea(
        entrega: Long? = null,
        completado: Boolean = false,
        fechaCompletado: Long? = null,
        evento: Boolean = false,
        eliminada: Long? = null,
    ) = Deber(
        materiaId = if (evento) app.kiwu.android.domain.ID_EVENTOS else "m",
        titulo = "t",
        completado = completado,
        fechaEntrega = entrega,
        fechaCompletado = fechaCompletado,
        eliminada = eliminada,
        creada = 0,
        actualizada = 0,
    )

    private fun sesion(dia: LocalDate, minutos: Int) = SesionEstudio(materiaId = null, dia = dia.toEpochDay(), minutos = minutos)

    @Test
    fun cuentaSoloLasTareasCompletadasDentroDeLaSemana() {
        val tareas = listOf(
            tarea(completado = true, fechaCompletado = ms(28, 10)), // lunes: cuenta
            tarea(completado = true, fechaCompletado = ms(30, 22)), // miércoles: cuenta
            tarea(completado = true, fechaCompletado = ms(27, 18)), // domingo anterior: no
            tarea(completado = true, fechaCompletado = ms(5, 9)), // lunes siguiente: no
            tarea(completado = true, fechaCompletado = null), // marcada sin momento: no
            tarea(), // pendiente: no
        )
        val r = Resumenes.de(tareas, emptyList(), 0, lunes, domingo, zona)
        assertEquals(2, r.tareasCompletadas)
    }

    @Test
    fun losEventosYLasEliminadasNoCuentanComoTareas() {
        val tareas = listOf(
            tarea(completado = true, fechaCompletado = ms(28, 10), evento = true),
            tarea(completado = true, fechaCompletado = ms(28, 11), eliminada = ms(29, 8)),
        )
        assertEquals(0, Resumenes.de(tareas, emptyList(), 0, lunes, domingo, zona).tareasCompletadas)
    }

    @Test
    fun losMinutosDeEstudioSeSumanPorDiaNatural() {
        val sesiones = listOf(
            sesion(lunes, 25),
            sesion(lunes, 35), // dos sesiones el mismo día se suman
            sesion(domingo, 20),
            sesion(lunes.minusDays(1), 99), // domingo anterior: no
            sesion(domingo.plusDays(1), 99), // lunes siguiente: no
        )
        assertEquals(80, Resumenes.de(emptyList(), sesiones, 0, lunes, domingo, zona).minutosEstudio)
    }

    @Test
    fun elAvisoDelResumenCaeElDomingoALasSieteDeLaTarde() {
        // Miércoles 30 a las 15:00 → este domingo 4 a las 19:00.
        assertEquals(LocalDateTime.of(2026, 10, 4, 19, 0).atZone(zona).toInstant().toEpochMilli(), Resumenes.proximoAviso(ms(30, 15), zona))
        // El mismo domingo a las 19:30 ya pasó: toca el siguiente (11 de octubre).
        assertEquals(LocalDateTime.of(2026, 10, 11, 19, 0).atZone(zona).toInstant().toEpochMilli(), Resumenes.proximoAviso(LocalDateTime.of(2026, 10, 4, 19, 30).atZone(zona).toInstant().toEpochMilli(), zona))
        // Justo a las 19:00 la hora ya se alcanzó: toca el domingo siguiente (misma convención que la racha).
        assertEquals(LocalDateTime.of(2026, 10, 11, 19, 0).atZone(zona).toInstant().toEpochMilli(), Resumenes.proximoAviso(LocalDateTime.of(2026, 10, 4, 19, 0).atZone(zona).toInstant().toEpochMilli(), zona))
        // Un minuto antes de las 19:00 sí cae este domingo.
        assertEquals(LocalDateTime.of(2026, 10, 4, 19, 0).atZone(zona).toInstant().toEpochMilli(), Resumenes.proximoAviso(LocalDateTime.of(2026, 10, 4, 18, 59).atZone(zona).toInstant().toEpochMilli(), zona))
    }

    @Test
    fun elProximoAvisoEsSiempreUnDomingo() {
        val base = LocalDate.of(2026, 9, 28) // lunes
        for (dia in 0..6) {
            val momento = base.plusDays(dia.toLong()).atTime(12, 0).atZone(zona).toInstant().toEpochMilli()
            assertEquals(DayOfWeek.SUNDAY, Instant.ofEpochMilli(Resumenes.proximoAviso(momento, zona)).atZone(zona).dayOfWeek)
        }
    }

    @Test
    fun unaSemanaSinNadaNoSeCuenta() {
        assertFalse(Resumenes.hayQueContar(ResumenSemanal(0, 0, 0)))
        assertFalse(Resumenes.hayQueContar(ResumenSemanal(0, 0, 5))) // solo racha no basta: la racha ya se avisó a diario
        assertTrue(Resumenes.hayQueContar(ResumenSemanal(3, 0, 0)))
        assertTrue(Resumenes.hayQueContar(ResumenSemanal(0, 45, 0)))
    }
}
