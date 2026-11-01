package app.kiwu.android

import app.kiwu.android.domain.Calificacion
import app.kiwu.android.domain.Calificaciones
import app.kiwu.android.domain.Examenes
import app.kiwu.android.domain.SesionEstudio
import app.kiwu.android.domain.TiempoEstudio
import app.kiwu.android.domain.Tarjeta
import app.kiwu.android.domain.Tarjetas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class EstudioTest {
    private val zona = ZoneId.of("America/Bogota")

    @Test
    fun elPromedioEsPonderadoPorPeso() {
        val notas = listOf(Calificacion("Parcial", 4.0, 3.0), Calificacion("Quiz", 2.0, 1.0))
        assertEquals(3.5, Calificaciones.promedio(notas)!!, 1e-9)
        assertNull(Calificaciones.promedio(emptyList()))
    }

    @Test
    fun lasNotasSeGuardanYSeLeenIgual() {
        val notas = listOf(Calificacion("Parcial \"1\"", 4.5, 2.0), Calificacion("Taller", 5.0))
        assertEquals(notas, Calificaciones.leer(Calificaciones.escribir(notas)))
        assertEquals(emptyList<Calificacion>(), Calificaciones.leer("{dañado"))
    }

    @Test
    fun lasTarjetasSeGuardanYSeLeenIgual() {
        val tarjetas = listOf(Tarjeta("Capital de \"Perú\"", "Lima"), Tarjeta("H2O", "Agua, líquida"))
        assertEquals(tarjetas, Tarjetas.leer(Tarjetas.escribir(tarjetas)))
        assertEquals("", Tarjetas.escribir(emptyList()))
        assertEquals(emptyList<Tarjeta>(), Tarjetas.leer("{dañado"))
    }

    @Test
    fun losDiasDeUnExamenSeCuentanPorDiaNaturalNoPorHoras() {
        val ahora = LocalDateTime.of(2026, 10, 28, 23, 0).atZone(zona).toInstant().toEpochMilli()
        val manana = LocalDateTime.of(2026, 10, 29, 7, 0).atZone(zona).toInstant().toEpochMilli()
        val en5 = LocalDateTime.of(2026, 11, 2, 9, 0).atZone(zona).toInstant().toEpochMilli()
        assertEquals(1L, Examenes.diasRestantes(manana, ahora, zona))
        assertEquals(5L, Examenes.diasRestantes(en5, ahora, zona))
        assertEquals(0L, Examenes.diasRestantes(ahora, ahora, zona))
    }

    @Test
    fun laNotaNecesariaCompletaElPromedioConLoQueFalta() {
        val notas = listOf(Calificacion("Parcial", 3.0, 1.0), Calificacion("Quiz", 4.0, 1.0))
        // (3 + 4 + x·2) / 4 = 4  →  x = 4.5
        assertEquals(4.5, Calificaciones.necesaria(notas, 4.0, 2.0)!!, 1e-9)
        assertEquals(4.0, Calificaciones.necesaria(emptyList(), 4.0, 1.0)!!, 1e-9)
        assertNull(Calificaciones.necesaria(notas, 4.0, 0.0))
    }

    private fun ms(d: Int, h: Int) = LocalDateTime.of(2026, 11, d, h, 0).atZone(zona).toInstant().toEpochMilli()

    @Test
    fun elPlanDeRepasoVaA7_3Y1DiasSinLosQueYaPasaron() {
        val examen = ms(20, 8)
        assertEquals(listOf(ms(13, 18), ms(17, 18), ms(19, 18)), Examenes.sesionesRepaso(examen, ms(1, 9), zona))
        // A 5 días del examen ya no cabe el de 7 días; el de hoy a las 18:00 sí, si aún no son las 18:00.
        assertEquals(listOf(ms(17, 18), ms(19, 18)), Examenes.sesionesRepaso(examen, ms(15, 9), zona))
        assertEquals(listOf(ms(19, 18)), Examenes.sesionesRepaso(examen, ms(17, 19), zona))
    }

    @Test
    fun elTiempoDeEstudioSeSumaPorListaEnLaSemana() {
        val miercoles = java.time.LocalDate.of(2026, 10, 28)
        val lunes = miercoles.minusDays(2).toEpochDay()
        val s = listOf(
            SesionEstudio("mate", lunes, 25), SesionEstudio("mate", lunes + 2, 50), SesionEstudio(null, lunes + 6, 25),
            SesionEstudio("mate", lunes - 1, 25), // domingo anterior: otra semana
            SesionEstudio("fisica", lunes + 1, 10),
        )
        assertEquals(listOf("mate" to 75, null to 25, "fisica" to 10), TiempoEstudio.semana(s, miercoles))
        assertEquals(s, TiempoEstudio.leer(TiempoEstudio.escribir(s)))
        assertEquals(listOf(SesionEstudio("x", lunes, 5)), TiempoEstudio.anadir(listOf(SesionEstudio("x", lunes - 60, 5)), SesionEstudio("x", lunes, 5)))
    }
}
