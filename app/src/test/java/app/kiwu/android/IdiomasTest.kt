package app.kiwu.android

import app.kiwu.android.ui.Fechas
import app.kiwu.android.ui.diaEnFrase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

/** Los textos de fechas y días siguen el idioma de la app (el locale por defecto). */
class IdiomasTest {
    private val antes = Locale.getDefault()

    @After
    fun restaurar() = Locale.setDefault(antes)

    @Test
    fun nombresDeLosDiasEnLosTresIdiomas() {
        Locale.setDefault(Locale.forLanguageTag("es"))
        assertEquals("Miércoles", Fechas.nombreDia(3))
        assertEquals("X", Fechas.letraDia(3))
        assertEquals("lunes", Fechas.diaEnFrase(1))
        Locale.setDefault(Locale.forLanguageTag("en"))
        assertEquals("Wednesday", Fechas.nombreDia(3))
        assertEquals("Wed", Fechas.diaCorto(3))
        assertEquals("W", Fechas.letraDia(3))
        assertEquals("Monday", Fechas.diaEnFrase(1))
        Locale.setDefault(Locale.forLanguageTag("pt"))
        assertEquals("Segunda-feira", Fechas.nombreDia(1))
        assertEquals("segunda-feira", Fechas.diaEnFrase(1))
    }

    @Test
    fun fechaCortaUsaLasPalabrasQueRecibe() {
        Locale.setDefault(Locale.forLanguageTag("en"))
        val zona = java.time.ZoneId.of("UTC")
        val ahora = LocalDate.of(2026, 10, 21).atStartOfDay(zona).toInstant().toEpochMilli()
        val manana = ahora + 86_400_000L + 9 * 3_600_000L
        assertEquals("Tomorrow, 09:00", Fechas.corta(manana, ahora, zona, "Today", "Tomorrow", "Yesterday"))
        assertEquals("Wednesday 21 Oct 2026", Fechas.diaLargo(LocalDate.of(2026, 10, 21)))
    }
}
