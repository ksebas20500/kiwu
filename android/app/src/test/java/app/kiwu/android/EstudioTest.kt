package app.kiwu.android

import app.kiwu.android.domain.Calificacion
import app.kiwu.android.domain.Calificaciones
import app.kiwu.android.domain.Examenes
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
}
