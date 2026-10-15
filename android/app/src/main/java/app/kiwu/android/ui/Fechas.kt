package app.kiwu.android.ui

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Textos de fechas en español. */
object Fechas {
    private val es = Locale("es", "ES")
    private val hora = DateTimeFormatter.ofPattern("HH:mm", es)
    private val diaMes = DateTimeFormatter.ofPattern("EEE d MMM", es)
    private val diaMesAnio = DateTimeFormatter.ofPattern("EEE d MMM yyyy", es)
    private val completa = DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", es)
    private val mesAnio = DateTimeFormatter.ofPattern("LLLL yyyy", es)

    private fun local(ms: Long, zona: ZoneId): LocalDateTime = Instant.ofEpochMilli(ms).atZone(zona).toLocalDateTime()

    /** «Hoy 18:00», «Mañana 09:30», «mié 30 sep» (en la lista de tareas). */
    fun corta(ms: Long, ahora: Long, zona: ZoneId = ZoneId.systemDefault()): String {
        val f = local(ms, zona)
        val hoy = local(ahora, zona).toLocalDate()
        val conHora = f.hour != 0 || f.minute != 0
        val sufijo = if (conHora) " " + hora.format(f) else ""
        return when (f.toLocalDate()) {
            hoy -> "Hoy$sufijo"
            hoy.plusDays(1) -> "Mañana$sufijo"
            hoy.minusDays(1) -> "Ayer$sufijo"
            else -> (if (f.year == hoy.year) diaMes else diaMesAnio).format(f) + sufijo
        }
    }

    fun completa(ms: Long, zona: ZoneId = ZoneId.systemDefault()): String = completa.format(local(ms, zona))

    fun mes(mes: LocalDate): String = mesAnio.format(mes).replaceFirstChar { it.uppercase(es) }

    fun hora(ms: Long, zona: ZoneId = ZoneId.systemDefault()): String = hora.format(local(ms, zona))
}
