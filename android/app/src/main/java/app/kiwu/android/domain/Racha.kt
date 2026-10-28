package app.kiwu.android.domain

import androidx.room.ColumnInfo
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * La racha «al día»: cuántos días exigentes seguidos se cerraron sin nada vencido. [dias] son los días cumplidos (para la
 * cuadrícula). Hoy aún no se ha cerrado: si quedan tareas por entregar ([pendientes]) la racha sigue viva pero en riesgo.
 */
data class Racha(
    val actual: Int = 0,
    val mejor: Int = 0,
    val hoyCumplido: Boolean = false,
    val dias: Set<LocalDate> = emptySet(),
    /** Cuántas tareas se han completado en total. */
    val tareas: Int = 0,
    /** Tareas con entrega hoy o antes que siguen sin resolver. */
    val pendientes: Int = 0,
) {
    /** La racha se pierde esta noche si siguen quedando tareas por entregar. */
    val enRiesgo: Boolean get() = actual > 0 && pendientes > 0
}

/** De una tarea solo lo que cuenta para la racha. Incluye las borradas (`eliminada`); los eventos no se piden. */
data class TareaRacha(
    @ColumnInfo(name = "fecha_entrega") val entrega: Long?,
    val completado: Boolean,
    @ColumnInfo(name = "fecha_completado") val completada: Long?,
    val eliminada: Long?,
    val creada: Long,
)

/** Lo que necesita el cálculo: las tareas y los días de la semana (1 = lunes … 7) que tienen clase. */
data class EntradaRacha(
    val tareas: List<TareaRacha> = emptyList(),
    val diasConClase: Set<Int> = emptySet(),
    /** Días que ya contaron como cumplidos (ver [Rachas.calcular]): desmarcar una tarea vieja no los quita. */
    val banco: Set<LocalDate> = emptySet(),
)

/**
 * Cálculo de la racha «al día». Un día **se rompe** si al terminarlo queda una tarea con entrega ese día o antes sin
 * resolver (completada o borrada); es **exigente** si hay una entrega ese día o si hay clase ese día de la semana **y**
 * hubo actividad (se completó o se añadió una tarea); el resto son días **neutros**: no suman ni rompen. Así crear un
 * horario no cumple ni acredita días por sí solo. Las tareas sin fecha y los eventos no cuentan para vencer. No se
 * guarda nada aparte: sale del historial, salvo el [EntradaRacha.banco]: un día que ya contó como cumplido no se pierde
 * porque luego se desmarque una tarea suya o se complete pasada la medianoche. La racha solo baja por lo de hoy.
 *
 * shortcut: los días con clase salen del horario actual (el modelo no guarda desde cuándo vale cada clase) y basta una
 * tarea trivial, completada o añadida, para que un día de clase cuente. Endurecer (p. ej. solo «completada») si se abusa.
 */
object Rachas {
    /** Hora del aviso «No pierdas tu racha». */
    val HORA_AVISO: LocalTime = LocalTime.of(20, 0)

    fun calcular(entrada: EntradaRacha, ahora: Long, zona: ZoneId = ZoneId.systemDefault()): Racha {
        fun dia(ms: Long) = Instant.ofEpochMilli(ms).atZone(zona).toLocalDate()
        val hoy = dia(ahora)
        val rotos = HashSet<LocalDate>()
        val entregas = HashSet<LocalDate>()
        val actividad = HashSet<LocalDate>()
        var pendientes = 0
        var completadas = 0
        var inicio = minOf(hoy, entrada.banco.minOrNull() ?: hoy)
        for (t in entrada.tareas) {
            if (t.completado && t.completada != null) completadas++
            inicio = minOf(inicio, dia(t.creada))
            actividad += dia(t.creada)
            if (t.completado && t.completada != null) actividad += dia(t.completada)
            val entrega = t.entrega?.let(::dia) ?: continue
            if (entrega.isAfter(hoy)) continue
            if (entrega.isBefore(hoy) && entrega in entrada.banco) continue // día ya cumplido: tocar la tarea ahora no lo deshace
            val borrada = t.eliminada?.let(::dia)
            if (borrada != null && !borrada.isAfter(entrega)) continue // borrada a tiempo: nunca se exigió
            // Cuándo dejó de estar pendiente; una tarea marcada sin fecha de completado se da por entregada a tiempo.
            val hecha = if (t.completado) (t.completada?.let(::dia) ?: entrega) else null
            val resuelta = listOfNotNull(hecha, borrada).minOrNull()
            entregas += entrega
            if (resuelta == null || resuelta.isAfter(hoy)) pendientes++
            var d = entrega
            val ultimo = if (resuelta == null) hoy else minOf(hoy, resuelta.minusDays(1))
            while (!d.isAfter(ultimo)) { rotos += d; d = d.plusDays(1) }
        }
        val dias = HashSet<LocalDate>()
        var mejor = 0
        var tramo = 0
        var d = inicio
        while (d.isBefore(hoy)) { // los días ya cerrados
            if (d !in entrada.banco && d in rotos) tramo = 0
            else if (d in entrada.banco || d in entregas || (d.dayOfWeek.value in entrada.diasConClase && d in actividad)) {
                tramo++
                mejor = maxOf(mejor, tramo)
                dias += d
            }
            d = d.plusDays(1)
        }
        val hoyCumplido = pendientes == 0 &&
            (hoy in entregas || (hoy.dayOfWeek.value in entrada.diasConClase && hoy in actividad))
        if (hoyCumplido) {
            tramo++
            mejor = maxOf(mejor, tramo)
            dias += hoy
        } // si no, hoy todavía puede cumplirse: la racha se cuenta hasta ayer
        return Racha(actual = tramo, mejor = mejor, hoyCumplido = hoyCumplido, dias = dias, tareas = completadas, pendientes = pendientes)
    }

    /** El próximo momento del aviso (hoy a las 20:00 si aún no ha pasado; si no, mañana). */
    fun proximoAviso(ahora: Long, zona: ZoneId = ZoneId.systemDefault()): Long {
        val momento = Instant.ofEpochMilli(ahora).atZone(zona)
        val hoy = momento.toLocalDate().atTime(HORA_AVISO).atZone(zona)
        return (if (momento.isBefore(hoy)) hoy else hoy.plusDays(1)).toInstant().toEpochMilli()
    }

    /** Metas de la racha, como insignias: la siguiente se muestra con lo que falta. */
    val METAS = listOf(3, 7, 14, 30, 50, 100, 200, 365)

    /** La próxima meta por encima de [actual] (o el siguiente centenar si ya pasó todas). */
    fun siguienteMeta(actual: Int): Int = METAS.firstOrNull { it > actual } ?: ((actual / 100) + 1) * 100

    /**
     * Si hay que celebrar la racha: hoy ya cuenta y todavía no se celebró hoy ([celebrada] es el último día celebrado,
     * en formato ISO). Así se celebra una vez al día, aunque se desmarque y se vuelva a marcar.
     */
    fun debeCelebrar(r: Racha, celebrada: String?, hoy: LocalDate): Boolean = r.hoyCumplido && celebrada != hoy.toString()

    /** Las filas del calendario de un mes (lunes a domingo); null = hueco antes del día 1 o después del último. */
    fun mes(mes: java.time.YearMonth): List<List<LocalDate?>> {
        val primero = mes.atDay(1)
        val huecos = primero.dayOfWeek.value - 1
        val celdas = List(huecos) { null } + (1..mes.lengthOfMonth()).map { mes.atDay(it) }
        return celdas.chunked(7).map { fila -> fila + List(7 - fila.size) { null } }
    }

    /** «1 día», «5 días». */

    /** Las semanas de la cuadrícula (lunes a domingo), de la más antigua a la actual. */
    fun semanas(hoy: LocalDate, cuantas: Int = 8): List<List<LocalDate>> {
        val lunes = hoy.minusDays((hoy.dayOfWeek.value - 1).toLong())
        return (cuantas - 1 downTo 0).map { s -> (0L..6L).map { lunes.minusWeeks(s.toLong()).plusDays(it) } }
    }
}
