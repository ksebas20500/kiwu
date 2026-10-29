package app.kiwu.android.domain

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import android.content.res.Resources
import app.kiwu.android.R

/** Recordatorios: qué tareas toca avisar y con qué texto. Las notificaciones viven en el paquete `notify`. */
object Recordatorios {
    private const val HORA_MS = 3_600_000L

    /** No se avisa de entregas que vencieron hace más de este tiempo (p. ej. al abrir la app tras días). */
    const val GRACIA_MS = HORA_MS

    /** Un evento de todo el día (00:00) se avisa respecto de las 9:00, no de la medianoche. */
    private const val HORA_EVENTO_TODO_EL_DIA_MS = 9 * HORA_MS

    /** El instante de referencia de una tarea o evento: su entrega, o las 9:00 si es un evento de todo el día. */
    fun referencia(d: Deber, zona: ZoneId = ZoneId.systemDefault()): Long? {
        val entrega = d.fechaEntrega ?: return null
        if (!d.esEvento) return entrega
        val hora = Instant.ofEpochMilli(entrega).atZone(zona).toLocalTime()
        return if (hora == LocalTime.MIDNIGHT) entrega + HORA_EVENTO_TODO_EL_DIA_MS else entrega
    }

    /** Momento en que debe sonar el aviso de una tarea; null si no lleva aviso. */
    fun momentoDeAviso(d: Deber): Long? {
        val entrega = referencia(d) ?: return null
        if (d.completado || d.recordatorioHoras <= 0) return null
        return entrega - d.recordatorioHoras * HORA_MS
    }

    /**
     * Cuándo hay que programar la alarma de una tarea, o null si no toca avisar (sin fecha, completada, ya avisada
     * o vencida hace mucho). Si el aviso ya pasó pero aún está en plazo, sale enseguida.
     */
    fun momentoDeDisparo(d: Deber, ahora: Long): Long? {
        val aviso = momentoDeAviso(d) ?: return null
        val entrega = referencia(d) ?: return null
        if (ahora > entrega + GRACIA_MS) return null
        if (d.recordado != null && d.recordado >= aviso) return null
        return maxOf(aviso, ahora + 2_000)
    }

    /** A qué hora suena un aviso pospuesto «a mañana»: la mañana siguiente, cuando la persona ya está despierta. */
    val HORA_MANANA: LocalTime = LocalTime.of(8, 0)

    /**
     * Cuándo vuelve a sonar un aviso pospuesto a mañana: mañana a las 8:00, o 30 minutos antes de la entrega si esta cae
     * mañana antes de esa hora (así no llega tarde). Una entrega de hoy o de mañana a medianoche usa las 8:00 de mañana.
     */
    fun mananaPospuesto(d: Deber, ahora: Long, zona: ZoneId = ZoneId.systemDefault()): Long {
        val manana = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate().plusDays(1)
        val aLas8 = manana.atTime(HORA_MANANA).atZone(zona).toInstant().toEpochMilli()
        val inicioManana = manana.atStartOfDay(zona).toInstant().toEpochMilli()
        val entrega = referencia(d, zona)
        val antes = entrega?.minus(30 * 60_000L)
        return if (entrega != null && antes != null && entrega < aLas8 && antes >= inicioManana) antes else aLas8
    }

    /** Tareas cuyo aviso ya llegó y todavía no se ha notificado. */
    fun pendientesDeAviso(deberes: List<Deber>, ahora: Long): List<Deber> =
        deberes.filter { d ->
            val aviso = momentoDeAviso(d) ?: return@filter false
            val entrega = referencia(d) ?: return@filter false
            aviso <= ahora && ahora <= entrega + GRACIA_MS && (d.recordado == null || d.recordado < aviso)
        }.sortedBy { it.fechaEntrega }

    /** «Vence en 5 h», «Vence mañana», «Venció hace 10 min»… (en un evento: «Empieza en 5 h», «Empezó hace 10 min»). */
    fun textoAviso(d: Deber, ahora: Long, r: Resources): String {
        val entrega = referencia(d) ?: return ""
        val ev = d.esEvento
        val resto = entrega - ahora
        if (resto < 0) {
            val minutos = (-resto / 60_000).toInt()
            if (minutos < 1) return r.getString(if (ev) R.string.aviso_evento_ya else R.string.aviso_tarea_ya)
            if (minutos < 60) return r.getString(if (ev) R.string.aviso_evento_hace else R.string.aviso_tarea_hace, minutos)
            val horas = minutos / 60
            if (horas >= 24) return r.getQuantityString(if (ev) R.plurals.aviso_evento_hace_d else R.plurals.aviso_tarea_hace_d, horas / 24, horas / 24)
            return r.getQuantityString(if (ev) R.plurals.aviso_evento_hace_h else R.plurals.aviso_tarea_hace_h, horas, horas)
        }
        val horas = resto.toDouble() / HORA_MS
        if (horas < 1) return r.getString(if (ev) R.string.aviso_evento_min else R.string.aviso_tarea_min, maxOf(1, (resto / 60_000).toInt()))
        if (horas < 24) return r.getString(if (ev) R.string.aviso_evento_h else R.string.aviso_tarea_h, horas.toInt())
        val dias = (horas / 24).toInt()
        return if (dias == 1) r.getString(if (ev) R.string.aviso_evento_manana else R.string.aviso_tarea_manana)
        else r.getString(if (ev) R.string.aviso_evento_dias else R.string.aviso_tarea_dias, dias)
    }
}
