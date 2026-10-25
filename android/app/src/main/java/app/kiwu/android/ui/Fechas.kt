package app.kiwu.android.ui

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Textos de fechas en el idioma de la app (el locale por defecto: lo fija el sistema o `Idioma`). */
object Fechas {
    private val locale: Locale get() = Locale.getDefault()
    private val hora get() = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val diaMes get() = DateTimeFormatter.ofPattern("EEE d MMM", locale)
    private val diaMesAnio get() = DateTimeFormatter.ofPattern("EEE d MMM yyyy", locale)
    private val completa get() = DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", locale)
    private val mesAnio get() = DateTimeFormatter.ofPattern("LLLL yyyy", locale)
    private val diaLargo get() = DateTimeFormatter.ofPattern("EEEE d MMM yyyy", locale)

    private fun local(ms: Long, zona: ZoneId): LocalDateTime = Instant.ofEpochMilli(ms).atZone(zona).toLocalDateTime()

    /** «Hoy 18:00», «Mañana 09:30», «mié 30 sep» (en la lista de tareas). Las palabras llegan ya traducidas. */
    fun corta(ms: Long, ahora: Long, zona: ZoneId = ZoneId.systemDefault(), hoyTexto: String, mananaTexto: String, ayerTexto: String): String {
        val f = local(ms, zona)
        val hoy = local(ahora, zona).toLocalDate()
        val conHora = f.hour != 0 || f.minute != 0
        val sufijo = if (conHora) " " + hora.format(f) else ""
        return when (f.toLocalDate()) {
            hoy -> "$hoyTexto$sufijo"
            hoy.plusDays(1) -> "$mananaTexto$sufijo"
            hoy.minusDays(1) -> "$ayerTexto$sufijo"
            else -> (if (f.year == hoy.year) diaMes else diaMesAnio).format(f) + sufijo
        }
    }

    fun completa(ms: Long, zona: ZoneId = ZoneId.systemDefault()): String = completa.format(local(ms, zona))

    /** Solo el día: «mié 21 oct 2026». */
    fun dia(ms: Long, zona: ZoneId = ZoneId.systemDefault()): String = diaMesAnio.format(local(ms, zona))

    /** El día con su nombre completo: «Martes 20 oct 2026». */
    fun diaLargo(fecha: LocalDate): String = diaLargo.format(fecha).replaceFirstChar { it.uppercase(locale) }

    fun mes(mes: LocalDate): String = mesAnio.format(mes).replaceFirstChar { it.uppercase(locale) }

    fun hora(ms: Long, zona: ZoneId = ZoneId.systemDefault()): String = hora.format(local(ms, zona))

    /** Nombre del día de la semana (1 = lunes): «Lunes», «Lun» o «L». */
    fun nombreDia(dia: Int, estilo: TextStyle = TextStyle.FULL): String =
        DayOfWeek.of(dia.coerceIn(1, 7)).getDisplayName(estilo, locale).replaceFirstChar { it.uppercase(locale) }.removeSuffix(".")

    fun diaCorto(dia: Int): String = nombreDia(dia, TextStyle.SHORT)

    /** Letra del día para las pestañas (en español, «X» para el miércoles, como siempre). */
    fun letraDia(dia: Int): String =
        if (locale.language == "es") listOf("L", "M", "X", "J", "V", "S", "D")[(dia - 1).coerceIn(0, 6)] else nombreDia(dia, TextStyle.NARROW)
}

/** [Fechas.corta] con «Hoy / Mañana / Ayer» en el idioma de la app. */
@androidx.compose.runtime.Composable
fun fechaCorta(ms: Long, ahora: Long, zona: ZoneId = ZoneId.systemDefault()): String = Fechas.corta(
    ms, ahora, zona,
    androidx.compose.ui.res.stringResource(app.kiwu.android.R.string.fecha_hoy),
    androidx.compose.ui.res.stringResource(app.kiwu.android.R.string.fecha_manana),
    androidx.compose.ui.res.stringResource(app.kiwu.android.R.string.fecha_ayer),
)

/** El día de la semana dentro de una frase: «el lunes» → «lunes» en español y portugués, «Monday» en inglés. */
fun Fechas.diaEnFrase(dia: Int): String {
    val nombre = nombreDia(dia)
    return if (java.util.Locale.getDefault().language == "en") nombre else nombre.lowercase(java.util.Locale.getDefault())
}
