package app.kiwu.android

import app.kiwu.android.domain.Rachas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class RachaTest {
    private val zona = ZoneId.of("America/Bogota")

    // miércoles 30 de septiembre de 2026
    private fun en(dias: Long, hora: Int = 12, minuto: Int = 0): Long =
        LocalDateTime.of(2026, 9, 30, hora, minuto).plusDays(dias).atZone(zona).toInstant().toEpochMilli()

    private val ahora = en(0, 15)

    @Test
    fun sinTareasCompletadasNoHayRacha() {
        val r = Rachas.calcular(emptyList(), ahora, zona)
        assertEquals(0, r.actual)
        assertEquals(0, r.mejor)
        assertFalse(r.hoyCumplido)
        assertFalse(r.enRiesgo)
    }

    @Test
    fun diasSeguidosHastaHoySumanLaRacha() {
        val r = Rachas.calcular(listOf(en(-2), en(-1), en(0), en(0, 9)), ahora, zona)
        assertEquals(3, r.actual)
        assertTrue(r.hoyCumplido)
        assertFalse(r.enRiesgo)
    }

    @Test
    fun siHoyAunNoSeCompletoNadaLaRachaDeAyerSigueVivaPeroEnRiesgo() {
        val r = Rachas.calcular(listOf(en(-3), en(-2), en(-1)), ahora, zona)
        assertEquals(3, r.actual)
        assertFalse(r.hoyCumplido)
        assertTrue(r.enRiesgo)
    }

    @Test
    fun unDiaSinCompletarCortaLaRachaPeroSeRecuerdaLaMejor() {
        // Cuatro días seguidos, un hueco anteayer y luego solo ayer.
        val r = Rachas.calcular(listOf(en(-7), en(-6), en(-5), en(-4), en(-1)), ahora, zona)
        assertEquals(1, r.actual)
        assertEquals(4, r.mejor)
    }

    @Test
    fun siAyerTampocoSeCompletoLaRachaEsCero() {
        val r = Rachas.calcular(listOf(en(-3), en(-2)), ahora, zona)
        assertEquals(0, r.actual)
        assertEquals(2, r.mejor)
        assertFalse(r.enRiesgo)
    }

    @Test
    fun losDiasSeCuentanConLaZonaHorariaLocal() {
        // 23:30 de ayer y 00:15 de hoy en Bogotá son días distintos (en UTC ambos caen en el mismo día).
        val r = Rachas.calcular(listOf(en(-1, 23, 30), en(0, 0, 15)), ahora, zona)
        assertEquals(2, r.actual)
        assertEquals(setOf(LocalDate.of(2026, 9, 29), LocalDate.of(2026, 9, 30)), r.dias)
    }

    @Test
    fun unDiaDelFuturoNoCuentaParaLaMejor() {
        // Con el reloj mal puesto puede haber una tarea completada «mañana»: no alarga la racha.
        val r = Rachas.calcular(listOf(en(0), en(1), en(2)), ahora, zona)
        assertEquals(1, r.actual)
        assertEquals(1, r.mejor)
    }

    @Test
    fun elAvisoEsHoyALasOchoSiNoHaPasadoYSiNoMañana() {
        assertEquals(en(0, 20), Rachas.proximoAviso(en(0, 15), zona))
        assertEquals(en(1, 20), Rachas.proximoAviso(en(0, 20), zona))
        assertEquals(en(1, 20), Rachas.proximoAviso(en(0, 22), zona))
    }

    @Test
    fun laCuadriculaVaDeLunesADomingoYTerminaEnLaSemanaDeHoy() {
        val hoy = LocalDate.of(2026, 9, 30)
        val semanas = Rachas.semanas(hoy, 8)
        assertEquals(8, semanas.size)
        assertTrue(semanas.all { it.size == 7 && it.first().dayOfWeek.value == 1 })
        assertTrue(hoy in semanas.last())
        assertEquals(LocalDate.of(2026, 8, 10), semanas.first().first())
    }

    @Test
    fun laSiguienteMetaEsLaPrimeraPorEncimaDeLaRacha() {
        assertEquals(3, Rachas.siguienteMeta(0))
        assertEquals(7, Rachas.siguienteMeta(3))
        assertEquals(14, Rachas.siguienteMeta(8))
        assertEquals(400, Rachas.siguienteMeta(365))
        assertEquals(500, Rachas.siguienteMeta(420))
    }

    @Test
    fun seCelebraUnaVezAlDiaYSoloSiHoyCuenta() {
        val hoy = LocalDate.of(2026, 9, 30)
        val cumplida = Rachas.calcular(listOf(en(-1), en(0)), ahora, zona)
        val pendiente = Rachas.calcular(listOf(en(-1)), ahora, zona)
        assertTrue(Rachas.debeCelebrar(cumplida, null, hoy))
        assertTrue(Rachas.debeCelebrar(cumplida, "2026-09-29", hoy))
        assertFalse(Rachas.debeCelebrar(cumplida, "2026-09-30", hoy))
        assertFalse(Rachas.debeCelebrar(pendiente, null, hoy))
    }

    @Test
    fun elMesSeReparteEnFilasDeLunesADomingo() {
        // Octubre de 2026 empieza en jueves y tiene 31 días.
        val filas = Rachas.mes(java.time.YearMonth.of(2026, 10))
        assertEquals(5, filas.size)
        assertTrue(filas.all { it.size == 7 })
        assertEquals(listOf(null, null, null), filas.first().take(3))
        assertEquals(LocalDate.of(2026, 10, 1), filas.first()[3])
        assertEquals(LocalDate.of(2026, 10, 31), filas.last()[5])
        assertEquals(null, filas.last()[6])
        assertEquals(31, filas.flatten().count { it != null })
    }

    @Test
    fun laRachaCuentaLasTareasCompletadas() {
        assertEquals(4, Rachas.calcular(listOf(en(-1), en(0), en(0, 9), en(0, 10)), ahora, zona).tareas)
    }

    @Test
    fun textos() {
        assertEquals("1 día", Rachas.texto(1))
        assertEquals("5 días", Rachas.texto(5))
    }
}
