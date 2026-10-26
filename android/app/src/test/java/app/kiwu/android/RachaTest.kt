package app.kiwu.android

import app.kiwu.android.domain.EntradaRacha
import app.kiwu.android.domain.Rachas
import app.kiwu.android.domain.TareaRacha
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

    // Las tareas se describen en días relativos a hoy: entrega, cuándo se completó y cuándo se borró.
    private fun tarea(entrega: Long?, hecha: Long? = null, borrada: Long? = null, creada: Long = -10) = TareaRacha(
        entrega = entrega?.let { en(it) }, completado = hecha != null, completada = hecha?.let { en(it) },
        eliminada = borrada?.let { en(it) }, creada = en(creada),
    )

    private val martes = setOf(2) // ayer (-1) y hace ocho días (-8) fueron martes; hoy es miércoles
    private val todosLosDias = (1..7).toSet()

    // Una tarea sin fecha creada ese día: da «actividad» a un día de clase sin añadir entregas.
    private fun actividad(vararg dias: Long) = dias.map { tarea(entrega = null, creada = it) }.toTypedArray()

    private fun racha(vararg tareas: TareaRacha, clases: Set<Int> = emptySet()) =
        Rachas.calcular(EntradaRacha(tareas.toList(), clases), ahora, zona)

    @Test
    fun sinTareasNiClasesNoHayRacha() {
        val r = racha()
        assertEquals(0, r.actual)
        assertEquals(0, r.mejor)
        assertFalse(r.hoyCumplido)
        assertFalse(r.enRiesgo)
    }

    @Test
    fun unaEntregaHechaATiempoCumpleEseDia() {
        val r = racha(tarea(entrega = -1, hecha = -2))
        assertEquals(1, r.actual)
        assertEquals(setOf(LocalDate.of(2026, 9, 29)), r.dias)
        assertFalse(r.enRiesgo)
    }

    @Test
    fun unaTareaVencidaSinHacerRompeLaRachaYSeCuentaComoPendiente() {
        val r = racha(tarea(entrega = -1), *actividad(-8), clases = martes)
        assertEquals(0, r.actual)
        assertEquals(1, r.mejor) // el martes de hace ocho días sí estaba al día
        assertEquals(1, r.pendientes)
        assertFalse(r.hoyCumplido)
    }

    @Test
    fun desmarcarUnaTareaLaVuelveAPendiente() {
        // Misma tarea: hecha ayer (cumple) y luego desmarcada (completado = false, sin fecha).
        assertEquals(1, racha(tarea(entrega = -1, hecha = -1)).actual)
        assertEquals(0, racha(tarea(entrega = -1)).actual)
    }

    @Test
    fun unaVencidaHechaTardeRompeSoloLosDiasEnQueEstuvoPendiente() {
        // Vencía hace 4 días y se hizo ayer: rotos -4, -3 y -2. Ayer tenía otra entrega a tiempo: cumple.
        val r = racha(tarea(entrega = -4, hecha = -1), tarea(entrega = -1, hecha = -1), clases = emptySet())
        assertEquals(1, r.actual)
        assertEquals(setOf(LocalDate.of(2026, 9, 29)), r.dias)
    }

    @Test
    fun borrarLaTareaVencidaDejaDeRomper() {
        val r = racha(tarea(entrega = -3, borrada = -1), *actividad(-8, -1), clases = martes)
        assertEquals(1, r.actual) // -3 y -2 rotos; ayer (martes) ya está al día
        assertEquals(1, r.mejor)
        assertEquals(0, r.pendientes)
    }

    @Test
    fun unaTareaBorradaAntesDeSuEntregaNuncaSeExigio() {
        val r = racha(tarea(entrega = -3, borrada = -5))
        assertEquals(0, r.actual)
        assertTrue(r.dias.isEmpty())
    }

    @Test
    fun lasTareasSinFechaYLasQueVencenEnElFuturoNoCuentan() {
        val r = racha(tarea(entrega = null), tarea(entrega = 2))
        assertEquals(0, r.pendientes)
        assertEquals(0, r.actual)
        assertFalse(r.hoyCumplido)
    }

    @Test
    fun losDiasSinClasesNiEntregasNoSumanNiRompen() {
        // Solo hay clase los martes y hubo actividad esos dos: los días entre medias son neutros.
        val r = racha(*actividad(-8, -1), clases = martes)
        assertEquals(2, r.actual)
        assertEquals(2, r.mejor)
        assertFalse(r.hoyCumplido) // hoy es miércoles: neutro
    }

    @Test
    fun unHorarioSinActividadNoCumpleNiHoyNiLosDiasPasados() {
        // El caso de crear el horario: hay clases todos los martes y hoy, pero no se completó ni añadió nada esos días.
        val sinHoy = racha(tarea(entrega = null), clases = martes)
        assertEquals(0, sinHoy.actual)
        assertTrue(sinHoy.dias.isEmpty())
        val hoyConClase = racha(tarea(entrega = null), clases = setOf(3))
        assertFalse(hoyConClase.hoyCumplido)
        assertEquals(0, hoyConClase.actual)
    }

    @Test
    fun unDiaDeClaseCumpleSiEseDiaSeCompletaOSeAnadeUnaTarea() {
        val completada = racha(tarea(entrega = null, hecha = 0), clases = setOf(3))
        assertTrue(completada.hoyCumplido)
        assertEquals(1, completada.actual)
        val anadida = racha(*actividad(-1), clases = martes)
        assertEquals(setOf(LocalDate.of(2026, 9, 29)), anadida.dias)
        assertEquals(1, anadida.actual)
    }

    @Test
    fun hoyConEntregasSinResolverEstaEnRiesgo() {
        val r = racha(tarea(entrega = 0), *actividad(-8, -1), clases = martes)
        assertEquals(2, r.actual) // se cuenta hasta ayer
        assertEquals(1, r.pendientes)
        assertFalse(r.hoyCumplido)
        assertTrue(r.enRiesgo)
    }

    @Test
    fun hoyCumpleAlResolverLasEntregasDeHoy() {
        val r = racha(tarea(entrega = 0, hecha = 0), *actividad(-8, -1), clases = martes)
        assertEquals(3, r.actual)
        assertTrue(r.hoyCumplido)
        assertFalse(r.enRiesgo)
        assertTrue(LocalDate.of(2026, 9, 30) in r.dias)
    }

    @Test
    fun losDiasSeCuentanConLaZonaHorariaLocal() {
        // Vencía ayer a las 23:30 y se hizo a las 00:15 de hoy en Bogotá: ayer quedó pendiente (en UTC ambas caen el mismo día).
        val r = Rachas.calcular(
            EntradaRacha(listOf(TareaRacha(en(-1, 23, 30), true, en(0, 0, 15), null, en(-10)))), ahora, zona,
        )
        assertFalse(LocalDate.of(2026, 9, 29) in r.dias)
        assertEquals(0, r.actual)
        assertEquals(0, r.pendientes)
    }

    @Test
    fun unaRotaEnMedioCortaLaRachaPeroSeRecuerdaLaMejor() {
        // Todos los días con clase: cumplen -9..-4 (6 días), se rompe -3, y cumplen -2, -1 y hoy.
        val r = racha(tarea(entrega = -3, hecha = -2, creada = -9), *actividad(-8, -7, -6, -5, -4, -1, 0), clases = todosLosDias)
        assertEquals(3, r.actual)
        assertEquals(6, r.mejor)
    }

    @Test
    fun laRachaCuentaLasTareasCompletadas() {
        assertEquals(2, racha(tarea(entrega = -1, hecha = -1), tarea(entrega = null, hecha = 0), tarea(entrega = 1)).tareas)
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
        val cumplida = racha(tarea(entrega = 0, hecha = 0))
        val pendiente = racha(tarea(entrega = 0))
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
}
