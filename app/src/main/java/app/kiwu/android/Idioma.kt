package app.kiwu.android

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.util.Locale

/**
 * Idioma de la app: español (base), inglés y portugués.
 *
 * - **Android 13 o superior:** lo decide Android. Por defecto es el del teléfono y se puede elegir otro solo para Kiwu en
 *   el ajuste del sistema «Idioma de la aplicación» (`res/xml/locales_config.xml`), que se abre desde Ajustes.
 * - **Android 8 a 12:** no existe ese ajuste, así que Kiwu tiene su propio selector. La elección se guarda en
 *   `SharedPreferences` (se lee sin esperar, antes del primer fotograma) y se aplica envolviendo el contexto con [contexto].
 *
 * Las notificaciones y los widgets no pasan por una pantalla: construyen sus textos con [contexto] para usar el mismo idioma.
 */
object Idioma {
    /** Idiomas con traducción, en el orden del selector. «» = el del sistema. */
    val etiquetas = listOf("", "es", "en", "pt")

    private const val PREFS = "kiwu"
    private const val CLAVE = "idioma"

    /** En Android 13+ el idioma de la app lo gestiona el sistema. */
    val delSistema: Boolean get() = Build.VERSION.SDK_INT >= 33

    /** El idioma elegido en el selector propio (Android 8–12), o «» para seguir al teléfono. */
    fun elegido(c: Context): String =
        if (delSistema) "" else c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLAVE, "").orEmpty()

    fun guardar(c: Context, etiqueta: String) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CLAVE, etiqueta).commit()
    }

    /** El contexto con el idioma elegido (o el mismo si sigue al teléfono, o en Android 13+, donde ya lo aplica el sistema). */
    fun contexto(base: Context): Context {
        val etiqueta = elegido(base)
        if (etiqueta.isEmpty()) {
            // Si antes se eligió otro idioma en este mismo proceso, las fechas vuelven al del teléfono.
            if (!delSistema) Locale.setDefault(android.content.res.Resources.getSystem().configuration.locales[0])
            return base
        }
        val locale = Locale.forLanguageTag(etiqueta)
        // Las fechas (`DateTimeFormatter`, `String.format`) usan el locale por defecto del proceso.
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    /** El idioma con el que se ven ahora los textos. */
    fun actual(c: Context): Locale = c.resources.configuration.locales[0]

    /** Abre el ajuste de Android para elegir el idioma de Kiwu (solo Android 13+). */
    fun abrirAjusteDelSistema(c: Context) {
        val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", c.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            c.startActivity(intent)
        } catch (e: Exception) {
            c.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", c.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

/**
 * Nombres fijos que también viven en los datos (la lista «Eventos», el cuaderno «Notas rápidas»), en el idioma actual. Los
 * ViewModels y el repositorio no tienen contexto: [actualizar] se llama al crear la app, la pantalla y los widgets.
 */
object Textos {
    @Volatile var eventos: String = "Eventos"
        private set
    @Volatile var notasRapidas: String = "Notas rápidas"
        private set

    fun actualizar(c: Context) {
        eventos = c.getString(R.string.eventos)
        notasRapidas = c.getString(R.string.notas_rapidas)
    }
}
