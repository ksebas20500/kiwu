package app.kiwu.android

import app.kiwu.android.domain.Clase
import app.kiwu.android.domain.ReglasHorario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

class HorarioReglasTest {
    private val zona = ZoneId.of("America/Bogota")

    private fun clase(
        dia: Int, inicio: String, fin: String, titulo: String = "Clase", grupo: String = titulo + dia, aviso: Int? = null,
    ): Clase {
        fun min(h: String) = h.substringBefore(":").toInt() * 60 + h.substringAfter(":").toInt()
        return Clase(horarioId = "H", grupo = grupo, titulo = titulo, diaSemana = dia, inicioMin = min(inicio), finMin = min(fin), avisoMin = aviso, creada = 0, actualizada = 0)
    }

    private fun ms(fecha: LocalDateTime) = fecha.atZone(zona).toInstant().toEpochMilli()

    @Test
    fun horasValidas() {
        assertTrue(ReglasHorario.valida(8 * 60, 10 * 60))
        assertTrue(ReglasHorario.valida(23 * 60, 24 * 60))
        assertFalse(ReglasHorario.valida(10 * 60, 10 * 60))
        assertFalse(ReglasHorario.valida(10 * 60, 9 * 60))
        assertFalse(ReglasHorario.valida(-1, 60))
        assertFalse(ReglasHorario.valida(24 * 60, 24 * 60 + 30))
    }

    @Test
    fun textosDeHora() {
        assertEquals("08:05", ReglasHorario.hora(8 * 60 + 5))
        assertEquals("24:00", ReglasHorario.hora(24 * 60))
        assertEquals("08:00 – 10:30", ReglasHorario.rango(clase(1, "08:00", "10:30")))
        assertEquals("Miércoles", ReglasHorario.nombreDia(3))
    }

    @Test
    fun deDiaOrdenaPorHora() {
        val c = listOf(clase(1, "10:00", "12:00", "B"), clase(1, "08:00", "09:00", "A"), clase(2, "07:00", "08:00", "C"))
        assertEquals(listOf("A", "B"), ReglasHorario.deDia(c, 1).map { it.titulo })
    }

    @Test
    fun soloChocanLasQueSeSolapanDeVerdad() {
        val existentes = listOf(clase(1, "08:00", "10:00", "Cálculo"), clase(3, "08:00", "10:00", "Física"))
        // empieza justo cuando termina la otra: no choca
        assertTrue(ReglasHorario.choques(existentes, null, setOf(1), 10 * 60, 11 * 60).isEmpty())
        // se mete a la mitad
        assertEquals(listOf("Cálculo"), ReglasHorario.choques(existentes, null, setOf(1), 9 * 60, 11 * 60).map { it.titulo })
        // otro día
        assertTrue(ReglasHorario.choques(existentes, null, setOf(2), 8 * 60, 10 * 60).isEmpty())
        // varios días a la vez
        assertEquals(2, ReglasHorario.choques(existentes, null, setOf(1, 3), 9 * 60, 9 * 60 + 30).size)
    }

    @Test
    fun alEditarUnGrupoNoChocaConsigoMismo() {
        val existentes = listOf(clase(1, "08:00", "10:00", "Cálculo", grupo = "G"), clase(3, "08:00", "10:00", "Cálculo", grupo = "G"))
        assertTrue(ReglasHorario.choques(existentes, "G", setOf(1, 3), 8 * 60, 10 * 60).isEmpty())
    }

    @Test
    fun claseEnCursoYSiguiente() {
        val c = listOf(clase(1, "08:00", "10:00", "A"), clase(1, "14:00", "16:00", "B"), clase(3, "09:00", "10:00", "C"))
        assertEquals("A", ReglasHorario.enCurso(c, 1, 9 * 60)?.titulo)
        assertNull("El fin no cuenta como en curso", ReglasHorario.enCurso(c, 1, 10 * 60))
        assertNull(ReglasHorario.enCurso(c, 2, 9 * 60))

        val (hoy, dias) = ReglasHorario.siguiente(c, 1, 9 * 60)!!
        assertEquals("B", hoy.titulo); assertEquals(0, dias)
        val (miercoles, d2) = ReglasHorario.siguiente(c, 1, 17 * 60)!!
        assertEquals("C", miercoles.titulo); assertEquals(2, d2)
        // después de la última clase de la semana vuelve al lunes siguiente
        val (lunes, d3) = ReglasHorario.siguiente(c, 3, 11 * 60)!!
        assertEquals("A", lunes.titulo); assertEquals(5, d3)
        assertNull(ReglasHorario.siguiente(emptyList(), 1, 0))
    }

    @Test
    fun cuandoEnPalabras() {
        assertEquals("hoy", ReglasHorario.cuando(0, 3))
        assertEquals("mañana", ReglasHorario.cuando(1, 4))
        assertEquals("lunes", ReglasHorario.cuando(5, 1))
    }

    @Test
    fun proximoInicioDeUnaClaseSemanal() {
        val lunes = clase(1, "08:00", "10:00")
        // miércoles 30 sep 2026 → lunes 5 oct
        val inicio = ReglasHorario.proximoInicio(lunes, LocalDateTime.of(2026, 9, 30, 12, 0))
        assertEquals(LocalDateTime.of(2026, 10, 5, 8, 0), inicio)
        assertEquals(DayOfWeek.MONDAY, inicio.dayOfWeek)
        // el mismo lunes antes de la hora: hoy; después de la hora: la semana siguiente
        assertEquals(LocalDateTime.of(2026, 10, 5, 8, 0), ReglasHorario.proximoInicio(lunes, LocalDateTime.of(2026, 10, 5, 7, 0)))
        assertEquals(LocalDateTime.of(2026, 10, 12, 8, 0), ReglasHorario.proximoInicio(lunes, LocalDateTime.of(2026, 10, 5, 8, 1)))
    }

    @Test
    fun momentoDelAviso() {
        val c = clase(1, "08:00", "10:00", aviso = 10)
        // desde el miércoles 30 sep: el lunes 5 oct a las 07:50
        assertEquals(ms(LocalDateTime.of(2026, 10, 5, 7, 50)), ReglasHorario.momentoDeAviso(c, ms(LocalDateTime.of(2026, 9, 30, 12, 0)), zona))
        // ya pasó el aviso de hoy (07:55 > 07:50): se pasa a la semana siguiente
        assertEquals(ms(LocalDateTime.of(2026, 10, 12, 7, 50)), ReglasHorario.momentoDeAviso(c, ms(LocalDateTime.of(2026, 10, 5, 7, 55)), zona))
        // aún no llega el aviso de hoy
        assertEquals(ms(LocalDateTime.of(2026, 10, 5, 7, 50)), ReglasHorario.momentoDeAviso(c, ms(LocalDateTime.of(2026, 10, 5, 7, 40)), zona))
        // sin aviso no hay alarma
        assertNull(ReglasHorario.momentoDeAviso(clase(1, "08:00", "10:00"), 0, zona))
    }

    @Test
    fun elAvisoDeUnaClaseTempranaPuedeCaerElDiaAnterior() {
        // clase a las 00:05 con aviso de 10 min: el aviso es a las 23:55 del domingo
        val c = clase(1, "00:05", "01:00", aviso = 10)
        val cuando = ReglasHorario.momentoDeAviso(c, ms(LocalDateTime.of(2026, 10, 1, 9, 0)), zona)
        assertNotNull(cuando)
        assertEquals(ms(LocalDateTime.of(2026, 10, 4, 23, 55)), cuando)
    }

    @Test
    fun textoDelAviso() {
        assertEquals("Empieza a las 08:00", ReglasHorario.textoAviso(clase(1, "08:00", "09:00")))
        assertEquals("Empieza a las 08:00 · Aula 204", ReglasHorario.textoAviso(clase(1, "08:00", "09:00").copy(aula = "Aula 204")))
    }
}
