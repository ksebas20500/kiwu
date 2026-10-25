package app.kiwu.android.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * La racha: cuántos días seguidos se ha completado al menos una tarea. [dias] son los días cumplidos (para la cuadrícula).
 * Si hoy todavía no se ha completado nada pero ayer sí, la racha sigue viva ([hoyCumplido] = false): se pierde al acabar el día.
 */
data class Racha(
    val actual: Int = 0,
    val mejor: Int = 0,
    val hoyCumplido: Boolean = false,
    val dias: Set<LocalDate> = emptySet(),
    /** Cuántas tareas se han completado en total (las que cuentan para la racha). */
    val tareas: Int = 0,
) {
    /** La racha se pierde esta noche si no se completa nada hoy. */
    val enRiesgo: Boolean get() = actual > 0 && !hoyCumplido
}

/**
 * Cálculo de la racha a partir de cuándo se completó cada tarea (`fecha_completado`). No se guarda nada aparte: desmarcar la
 * única tarea de un día quita ese día. Los eventos no cuentan (no se completan).
 */
object Rachas {
    /** Hora del aviso «No pierdas tu racha». */
    val HORA_AVISO: LocalTime = LocalTime.of(20, 0)

    fun calcular(completados: List<Long>, ahora: Long, zona: ZoneId = ZoneId.systemDefault()): Racha {
        val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
        val dias = completados.mapTo(HashSet()) { Instant.ofEpochMilli(it).atZone(zona).toLocalDate() }
        val hoyCumplido = hoy in dias
        // Se cuenta hacia atrás desde hoy (si ya cumplió) o desde ayer (si hoy aún está a tiempo).
        var dia = if (hoyCumplido) hoy else hoy.minusDays(1)
        var actual = 0
        while (dia in dias) { actual++; dia = dia.minusDays(1) }
        var mejor = 0
        var tramo = 0
        var anterior: LocalDate? = null
        for (d in dias.filter { !it.isAfter(hoy) }.sorted()) {
            tramo = if (anterior != null && d == anterior.plusDays(1)) tramo + 1 else 1
            mejor = maxOf(mejor, tramo)
            anterior = d
        }
        return Racha(actual = actual, mejor = maxOf(mejor, actual), hoyCumplido = hoyCumplido, dias = dias, tareas = completados.size)
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
