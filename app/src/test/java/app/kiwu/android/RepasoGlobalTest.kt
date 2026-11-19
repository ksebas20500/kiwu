package app.kiwu.android

import app.kiwu.android.domain.Cuaderno
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.Materia
import app.kiwu.android.domain.PaginaTarjetas
import app.kiwu.android.domain.RepasoGlobal
import app.kiwu.android.domain.Tarjeta
import app.kiwu.android.domain.Tarjetas
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cola global de repaso: tarjetas vencidas, prioridad por examen y hora del aviso diario. */
class RepasoGlobalTest {
    private val zona: ZoneId = ZoneOffset.UTC
    private val ahora = Instant.parse("2026-01-05T10:00:00Z").toEpochMilli()

    private fun ms(dia: Int, hora: Int = 9): Long =
        LocalDateTime.of(2026, 1, 5, 0, 0).plusDays(dia.toLong()).withHour(hora).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun nota(id: String, cuaderno: String, titulo: String, vararg tarjetas: Tarjeta) =
        PaginaTarjetas(id, cuaderno, titulo, Tarjetas.escribir(tarjetas.toList()))

    private fun cuaderno(id: String, nombre: String) =
        Cuaderno(id = id, nombre = nombre, creada = 0, actualizada = 0)

    private fun materia(id: String, nombre: String) =
        Materia(id = id, nombre = nombre, creada = 0, actualizada = 0)

    private fun examen(titulo: String, materiaId: String, fecha: Long? = ms(2), completado: Boolean = false) =
        Deber(
            materiaId = materiaId, titulo = titulo, examen = true, fechaEntrega = fecha,
            completado = completado, creada = 0, actualizada = 0,
        )

    @Test
    fun juntaLasVencidasDeTodasLasNotasYDejaFueraLasQueAunNoTocan() {
        val cola = RepasoGlobal.cola(
            listOf(
                nota("P1", "C1", "Álgebra", Tarjeta("vieja", "r", vence = 500), Tarjeta("futura", "r", vence = ahora + 5_000)),
                nota("P2", "C2", "Historia", Tarjeta("más vieja", "r", vence = 100)),
            ),
            listOf(cuaderno("C1", "Álgebra"), cuaderno("C2", "Historia")),
            emptyList(), emptyList(), ahora, zona,
        )
        assertEquals(listOf("más vieja", "vieja"), cola.map { it.tarjeta.pregunta })
        assertEquals(listOf("P2", "P1"), cola.map { it.paginaId })
        assertTrue(cola.all { it.prioridad == 0 })
    }

    @Test
    fun unExamenCercanoAdelantaLasTarjetasDeSuMateria() {
        val cola = RepasoGlobal.cola(
            listOf(
                nota("P1", "C1", "Cálculo", Tarjeta("derivadas", "r", vence = ahora - 1_000)),
                nota("P2", "C2", "Historia", Tarjeta("revolución", "r", vence = 0)),
            ),
            listOf(cuaderno("C1", "Cálculo"), cuaderno("C2", "Historia")),
            listOf(examen("Parcial", "M1", fecha = ms(2))),
            listOf(materia("M1", "Cálculo")),
            ahora, zona,
        )
        assertEquals("derivadas", cola.first().tarjeta.pregunta)
        assertEquals(2, cola.first().prioridad)
        assertEquals("Parcial", cola.first().examen)
        assertEquals(2, cola.first().diasExamen)
        assertEquals(0, cola.last().prioridad)
        assertNull(cola.last().examen)
    }

    @Test
    fun laPrioridadSubeAlAcercarseElExamen() {
        assertEquals(1, RepasoGlobal.prioridad(7))
        assertEquals(1, RepasoGlobal.prioridad(4))
        assertEquals(2, RepasoGlobal.prioridad(3))
        assertEquals(2, RepasoGlobal.prioridad(2))
        assertEquals(3, RepasoGlobal.prioridad(1))
        assertEquals(3, RepasoGlobal.prioridad(0))
        assertEquals(0, RepasoGlobal.prioridad(8))
    }

    @Test
    fun unExamenLejanoCompletadoOPasadoNoDaPrioridad() {
        val paginas = listOf(nota("P1", "C1", "Cálculo", Tarjeta("derive", "r", vence = 0)))
        val cuadernos = listOf(cuaderno("C1", "Cálculo"))
        val materias = listOf(materia("M1", "Cálculo"))
        for (d in listOf(
            examen("Lejano", "M1", fecha = ms(10)),
            examen("Completado", "M1", fecha = ms(2), completado = true),
            examen("Pasado", "M1", fecha = ms(-1)),
        )) {
            val cola = RepasoGlobal.cola(paginas, cuadernos, listOf(d), materias, ahora, zona)
            assertEquals(d.titulo, 0, cola.single().prioridad)
            assertNull(cola.single().examen)
        }
    }

    @Test
    fun elTituloDeLaNotaTambienAsociaElExamen() {
        val cola = RepasoGlobal.cola(
            listOf(nota("P1", "C1", "Revolución francesa", Tarjeta("1789", "r", vence = 0))),
            listOf(cuaderno("C1", "Apuntes sueltos")),
            listOf(examen("Examen de revolución francesa", "M1", fecha = ms(1))),
            listOf(materia("M1", "Historia")),
            ahora, zona,
        )
        assertEquals(3, cola.single().prioridad)
        assertEquals(1, cola.single().diasExamen)
    }

    @Test
    fun losNombresSeComparanSinAcentosNiMayusculas() {
        assertTrue(RepasoGlobal.seParecen("Cálculo I", "calculo"))
        assertTrue(RepasoGlobal.seParecen("HISTORIA", "História"))
        assertTrue(RepasoGlobal.seParecen("Matemáticas", "Matematicas"))
        assertTrue(!RepasoGlobal.seParecen("Física", "Química"))
        assertTrue(!RepasoGlobal.seParecen("", "Cálculo"))
        assertTrue(!RepasoGlobal.seParecen("ab", "abc"))
    }

    @Test
    fun elAvisoDiarioEsHoySiAunNoSonLasSeisYMananaSiYaPasaron() {
        fun momento(dia: Int, hora: Int, minuto: Int) =
            LocalDate.of(2026, 1, 5).plusDays(dia.toLong()).atTime(hora, minuto).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals(momento(0, 18, 0), RepasoGlobal.proximoAviso(momento(0, 9, 0), zona))
        assertEquals(momento(0, 18, 0), RepasoGlobal.proximoAviso(momento(0, 17, 59), zona))
        assertEquals(momento(1, 18, 0), RepasoGlobal.proximoAviso(momento(0, 18, 0), zona))
        assertEquals(momento(1, 18, 0), RepasoGlobal.proximoAviso(momento(0, 23, 30), zona))
    }
}
