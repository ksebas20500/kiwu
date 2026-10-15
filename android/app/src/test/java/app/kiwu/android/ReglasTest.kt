package app.kiwu.android

import app.kiwu.android.domain.Columna
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.Elemento
import app.kiwu.android.domain.Recordatorios
import app.kiwu.android.domain.Reglas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ReglasTest {
    private val zona = ZoneId.of("America/Bogota")
    private val reglas = Reglas(zona)
    private val uni = "M-UNI"
    private val casa = "M-CASA"

    // miércoles 30 de septiembre de 2026, 12:00 locales
    private val ahora = LocalDateTime.of(2026, 9, 30, 12, 0).atZone(zona).toInstant().toEpochMilli()

    private fun en(dias: Long = 0, horas: Long = 0): Long =
        LocalDateTime.of(2026, 9, 30, 12, 0).plusDays(dias).plusHours(horas).atZone(zona).toInstant().toEpochMilli()

    private var contador = 0L
    private fun tarea(titulo: String, materia: String = uni, fecha: Long? = null) =
        Deber(id = titulo, materiaId = materia, titulo = titulo, fechaEntrega = fecha, creada = ++contador, actualizada = contador)

    @Test
    fun hoyIncluyeVencidasYHoyPeroNoMananaNiSinFecha() {
        val lista = listOf(tarea("vencida", fecha = en(-3)), tarea("hoy", fecha = en(0, 5)), tarea("mañana", fecha = en(1)), tarea("sin fecha"))
        assertEquals(setOf("vencida", "hoy"), reglas.pendientes(lista, Elemento.Hoy, ahora).map { it.titulo }.toSet())
    }

    @Test
    fun proximosSieteDias() {
        val lista = listOf(
            tarea("vencida", fecha = en(-1)), tarea("hoy", fecha = en(0)),
            tarea("dia6", fecha = en(6, 3)), tarea("dia7", fecha = en(7)),
        )
        assertEquals(setOf("hoy", "dia6"), reglas.pendientes(lista, Elemento.Proximos, ahora).map { it.titulo }.toSet())
    }

    @Test
    fun ordenPorFechaYSinFechaAlFinal() {
        val lista = listOf(tarea("c"), tarea("b", fecha = en(2)), tarea("a", fecha = en(1)))
        assertEquals(listOf("a", "b", "c"), reglas.pendientes(lista, Elemento.Todos, ahora).map { it.titulo })
    }

    @Test
    fun flujoDeColumnas() {
        val d = tarea("x")
        assertEquals(Columna.POR_HACER, reglas.columnaDe(d))
        val enCurso = reglas.mover(d, Columna.EN_CURSO, ahora)
        assertEquals(Columna.EN_CURSO, reglas.columnaDe(enCurso))
        val hecho = reglas.mover(enCurso, Columna.HECHO, ahora)
        assertEquals(Columna.HECHO, reglas.columnaDe(hecho))
        assertEquals(ahora, hecho.fechaCompletado)
        val deVuelta = reglas.mover(hecho, Columna.POR_HACER, ahora)
        assertEquals(Columna.POR_HACER, reglas.columnaDe(deVuelta))
        assertNull(deVuelta.fechaCompletado)
    }

    @Test
    fun marcarYDesmarcarConservaEnCurso() {
        val enCurso = reglas.mover(tarea("x"), Columna.EN_CURSO, ahora)
        val hecha = reglas.marcar(enCurso, true, ahora)
        assertEquals(Columna.HECHO, reglas.columnaDe(hecha))
        assertEquals(Columna.EN_CURSO, reglas.columnaDe(reglas.marcar(hecha, false, ahora)))
    }

    @Test
    fun completadasYHoy() {
        val a = reglas.marcar(tarea("a"), true, ahora)
        val lista = listOf(a)
        assertEquals(1, reglas.completados(lista, Elemento.Completadas, ahora).size)
        assertEquals(1, reglas.completados(lista, Elemento.Hoy, ahora).size)
        assertEquals(0, reglas.completados(lista, Elemento.Proximos, ahora).size)
        assertEquals(0, reglas.conteo(lista, Elemento.Completadas, ahora))
    }

    @Test
    fun porMateriaYConteo() {
        val lista = listOf(tarea("a"), tarea("b", materia = casa))
        assertEquals(1, reglas.conteo(lista, Elemento.DeMateria(uni), ahora))
        assertEquals(2, reglas.conteo(lista, Elemento.Todos, ahora))
    }

    @Test
    fun delDia() {
        val lista = listOf(tarea("a", fecha = en(0, 3)), tarea("b", fecha = en(1)))
        assertEquals(listOf("a"), reglas.delDia(lista, LocalDate.of(2026, 9, 30)).map { it.titulo })
    }

    @Test
    fun finDelDia() {
        val fin = java.time.Instant.ofEpochMilli(reglas.finDelDia(ahora)).atZone(zona)
        assertEquals("23:59:59", "%02d:%02d:%02d".format(fin.hour, fin.minute, fin.second))
        val inicio = java.time.Instant.ofEpochMilli(reglas.inicioDelDia(ahora)).atZone(zona)
        assertEquals(0, inicio.hour)
        assertEquals(0, inicio.minute)
    }

    @Test
    fun cambiarLaFechaRearmaElAviso() {
        val t0 = 1_800_000_000_000L
        val d = tarea("x", fecha = t0 + 3_600_000).copy(recordado = t0)
        val nueva = reglas.ponerFecha(d, t0 + 5 * 3_600_000, t0)
        assertNull(nueva.recordado)
        assertEquals(1, Recordatorios.pendientesDeAviso(listOf(nueva), t0 + 60_000).size)
    }

    // --- recordatorios ---------------------------------------------------------------------------

    private val t0 = 1_800_000_000_000L
    private val hora = 3_600_000L

    @Test
    fun avisaEnLaVentanaCorrectaYSoloUnaVez() {
        val d = tarea("entrega", fecha = t0 + 30 * hora)    // vence en 30 h, aviso 24 h antes
        assertTrue(Recordatorios.pendientesDeAviso(listOf(d), t0).isEmpty())
        assertEquals(1, Recordatorios.pendientesDeAviso(listOf(d), t0 + 7 * hora).size)
        val avisada = d.copy(recordado = t0 + 7 * hora)
        assertTrue(Recordatorios.pendientesDeAviso(listOf(avisada), t0 + 8 * hora).isEmpty())
    }

    @Test
    fun noAvisaDeCompletadasNiMuyVencidas() {
        val d = tarea("vieja", fecha = t0)
        assertTrue(Recordatorios.pendientesDeAviso(listOf(d), t0 + 3 * 24 * hora).isEmpty())
        assertTrue(Recordatorios.pendientesDeAviso(listOf(reglas.marcar(d, true, t0)), t0 - hora).isEmpty())
    }

    @Test
    fun textoDelAviso() {
        val d = tarea("x", fecha = t0 + 5 * hora)
        assertEquals("Vence en 5 h", Recordatorios.textoAviso(d, t0))
        assertEquals("Vence en 30 min", Recordatorios.textoAviso(d, t0 + (4.5 * hora).toLong()))
        assertEquals("Venció hace 10 min", Recordatorios.textoAviso(d, t0 + 5 * hora + 600_000))
        assertEquals("Vence mañana", Recordatorios.textoAviso(d.copy(fechaEntrega = t0 + 30 * hora), t0))
    }

    @Test
    fun momentoDeDisparo() {
        val d = tarea("x", fecha = t0 + 30 * hora)            // aviso a las 24 h antes = t0 + 6 h
        assertEquals(t0 + 6 * hora, Recordatorios.momentoDeDisparo(d, t0))
        assertEquals(t0 + 8 * hora + 2_000, Recordatorios.momentoDeDisparo(d, t0 + 8 * hora))      // ya pasó: sale enseguida
        assertNull(Recordatorios.momentoDeDisparo(d.copy(recordado = t0 + 7 * hora), t0 + 8 * hora)) // ya avisada
        assertNull(Recordatorios.momentoDeDisparo(reglas.marcar(d, true, t0), t0))                  // completada
        assertNull(Recordatorios.momentoDeDisparo(d.copy(recordatorioHoras = 0), t0))               // sin aviso
        assertNull(Recordatorios.momentoDeDisparo(tarea("s"), t0))                                   // sin fecha
        assertNull(Recordatorios.momentoDeDisparo(d, t0 + 40 * hora))                                // vencida hace mucho
    }
}
