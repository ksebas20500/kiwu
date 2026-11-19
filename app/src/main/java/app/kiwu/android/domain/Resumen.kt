package app.kiwu.android.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Lo que se logró en una semana natural (lunes a domingo): tareas, estudio y racha. */
data class ResumenSemanal(val tareasCompletadas: Int, val minutosEstudio: Int, val racha: Int)

object Resumenes {
    /** HORA del aviso del domingo (por la tarde, con la semana ya cerrada). */
    val HORA_AVISO: LocalTime = LocalTime.of(19, 0)

    /**
     * [tareas]: todas las de la base (aquí se quedan solo las completadas en [desde]..[hasta], ambos incluidos);
     * [sesiones]: lo que guardó el temporizador Pomodoro; [racha]: la racha que va ahora.
     */
    fun de(
        tareas: List<Deber>,
        sesiones: List<SesionEstudio>,
        racha: Int,
        desde: LocalDate,
        hasta: LocalDate,
        zona: ZoneId = ZoneId.systemDefault(),
    ): ResumenSemanal {
        val inicio = desde.atStartOfDay(zona).toInstant().toEpochMilli()
        val fin = hasta.plusDays(1).atStartOfDay(zona).toInstant().toEpochMilli()
        val hechas = tareas.count { !it.esEvento && it.eliminada == null && it.completado && it.fechaCompletado != null && it.fechaCompletado in inicio until fin }
        val minutos = sesiones.filter { it.dia in desde.toEpochDay()..hasta.toEpochDay() }.sumOf { it.minutos.toLong() }.toInt()
        return ResumenSemanal(hechas, minutos, racha)
    }

    /** El próximo momento del aviso (este domingo a las 19:00 si aún no ha pasado; si no, el domingo siguiente). */
    fun proximoAviso(ahora: Long, zona: ZoneId = ZoneId.systemDefault()): Long {
        val momento = Instant.ofEpochMilli(ahora).atZone(zona)
        val domingo = momento.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).atTime(HORA_AVISO).atZone(zona)
        return (if (momento.isBefore(domingo)) domingo else domingo.plusWeeks(1)).toInstant().toEpochMilli()
    }

    /** El aviso solo tiene sentido si algo pasó: sin tareas ni estudio, no hay nada que contar. */
    fun hayQueContar(r: ResumenSemanal): Boolean = r.tareasCompletadas > 0 || r.minutosEstudio > 0
}
