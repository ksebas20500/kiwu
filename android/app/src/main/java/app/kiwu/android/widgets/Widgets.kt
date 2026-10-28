package app.kiwu.android.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.updateAll
import androidx.glance.unit.ColorProvider
import app.kiwu.android.KiwuApp
import app.kiwu.android.MainActivity
import app.kiwu.android.ui.Acentos
import app.kiwu.android.ui.ColorPastel
import app.kiwu.android.ui.esquemaDe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Clave del estado de Glance donde se guarda la configuración de cada widget. */
val ClaveConfig = stringPreferencesKey("config")

/** Id de la tarea que se pulsa en el widget de deberes. */
val ClaveDeberWidget = ActionParameters.Key<String>("deber")

/** Colores con los que se dibuja un widget: los del tema y el color de la app en ese momento. */
class ColoresWidget(
    val fondo: ColorProvider,
    val texto: ColorProvider,
    val secundario: ColorProvider,
    val acento: ColorProvider,
    val sobreAcento: ColorProvider,
    val rojo: ColorProvider,
    /** Baldosa de cada día del calendario: un tono sobre el fondo para separar los días. */
    val celda: ColorProvider,
)

/** Lee el tema de la app (claro, oscuro o el del sistema) para que el widget se vea como la app. */
suspend fun coloresDelWidget(contexto: Context): ColoresWidget {
    val app = contexto.applicationContext as KiwuApp
    val ajustes = app.ajustes.flujo.first()
    val sistemaOscuro = (contexto.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val oscuro = when (ajustes.tema) {
        "claro" -> false
        "oscuro" -> true
        else -> sistemaOscuro
    }
    val esquema = if (ajustes.acento == Acentos.SISTEMA && Build.VERSION.SDK_INT >= 31) {
        if (oscuro) dynamicDarkColorScheme(contexto) else dynamicLightColorScheme(contexto)
    } else {
        esquemaDe(ajustes.acento, oscuro)
    }
    return ColoresWidget(
        fondo = ColorProvider(esquema.background),
        texto = ColorProvider(esquema.onSurface),
        secundario = ColorProvider(esquema.onSurfaceVariant),
        acento = ColorProvider(esquema.primary),
        sobreAcento = ColorProvider(esquema.onPrimary),
        rojo = ColorProvider(if (oscuro) Color(0xFFFF8A80) else Color(0xFFD32F2F)),
        celda = ColorProvider(esquema.onSurface.copy(alpha = if (oscuro) 0.10f else 0.05f)),
    )
}

/** Color de una clase (el elegido o el automático) para el widget. */
fun colorDeClase(color: ColorPastel): ColorProvider = ColorProvider(color.fondo)

/** Intención para abrir la app, opcionalmente en una tarea, una nota, una nota nueva o una sección. */
fun intencionDeApertura(
    contexto: Context,
    tarea: String? = null,
    nota: String? = null,
    nuevaNotaEnCuaderno: String? = null,
    seccion: String? = null,
): Intent = Intent(contexto, MainActivity::class.java).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    // Los PendingIntent ignoran los extras al compararse: sin un `data` distinto, un widget pisaría el destino de otro.
    data = android.net.Uri.parse("kiwu://widget/${seccion.orEmpty()}/${tarea.orEmpty()}/${nota.orEmpty()}/${nuevaNotaEnCuaderno ?: "-"}")
    if (tarea != null) putExtra(MainActivity.EXTRA_TAREA, tarea)
    if (nota != null) putExtra(MainActivity.EXTRA_NOTA, nota)
    if (nuevaNotaEnCuaderno != null) putExtra(MainActivity.EXTRA_NUEVA_NOTA, nuevaNotaEnCuaderno)
    if (seccion != null) putExtra(MainActivity.EXTRA_SECCION, seccion)
}

/**
 * Intención para añadir una tarea desde el widget de deberes sin abrir la app. [lista] es la lista fija del widget (null =
 * elegirla) y [hoy] hace que la tarea venza hoy, para que aparezca en un widget que solo muestra «Hoy».
 */
fun intencionNuevaTarea(contexto: Context, lista: String?, hoy: Boolean): Intent =
    Intent(contexto, NuevaTareaActivity::class.java).apply {
        // Un `data` distinto por destino, por lo mismo que en `intencionDeApertura`.
        data = android.net.Uri.parse("kiwu://widget/nueva-tarea/${lista.orEmpty()}/$hoy")
        if (lista != null) putExtra(NuevaTareaActivity.EXTRA_LISTA, lista)
        putExtra(NuevaTareaActivity.EXTRA_HOY, hoy)
    }

/** Intención para abrir la hoja de un día del widget de calendario (lo pendiente y añadir una tarea), sin abrir la app. */
fun intencionDiaCalendario(contexto: Context, dia: java.time.LocalDate): Intent =
    Intent(contexto, DiaCalendarioActivity::class.java).apply {
        data = android.net.Uri.parse("kiwu://widget/dia/${dia.toEpochDay()}")
        putExtra(DiaCalendarioActivity.EXTRA_DIA, dia.toEpochDay())
    }

/**
 * Refresco de los widgets: cuando cambian los datos de la app (cualquier lista, tarea, nota, clase o ajuste) y cuando
 * toca por una alarma (cambio de clase o medianoche).
 */
object Widgets {
    /** Vuelve a dibujar los widgets a partir de la base de datos. */
    suspend fun refrescar(contexto: Context) {
        DeberesWidget().updateAll(contexto)
        HorarioWidget().updateAll(contexto)
        NotasWidget().updateAll(contexto)
        CalendarioWidget().updateAll(contexto)
    }

    /**
     * Programa el próximo refresco: el cambio de clase más cercano (para «Ahora / Siguiente») o la medianoche (para
     * «Hoy», «Mañana» y el día del horario). Una alarma inexacta basta: no necesita el permiso de alarmas exactas.
     */
    suspend fun programar(contexto: Context) {
        val app = contexto.applicationContext as KiwuApp
        val ahora = System.currentTimeMillis()
        val cuando = DatosWidgets.proximoRefresco(app.repo.clasesAhora(), ahora).coerceAtLeast(ahora + 60_000L)
        val alarma = contexto.getSystemService(AlarmManager::class.java) ?: return
        val pendiente = PendingIntent.getBroadcast(
            contexto, PETICION_ALARMA,
            Intent(contexto, WidgetAlarmaReceiver::class.java).setAction(ACCION_ALARMA),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarma.set(AlarmManager.RTC, cuando, pendiente)
    }

    private const val PETICION_ALARMA = 7042
    private const val ACCION_ALARMA = "app.kiwu.android.widgets.REFRESCAR"
}

/** Suena en el próximo cambio de clase o a medianoche: refresca los widgets y programa el siguiente. */
class WidgetAlarmaReceiver : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        val pendiente = goAsync()
        val app = contexto.applicationContext as KiwuApp
        app.alcance.launch {
            try {
                Widgets.refrescar(contexto)
                Widgets.programar(contexto)
            } finally {
                pendiente.finish()
            }
        }
    }
}

/** Tocar el círculo de una tarea la completa sin abrir la app; los avisos se cancelan solos. */
class CompletarDeber : androidx.glance.appwidget.action.ActionCallback {
    override suspend fun onAction(contexto: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[ClaveDeberWidget] ?: return
        val app = contexto.applicationContext as KiwuApp
        app.repo.marcar(id, true)
        // Cambia el contador y los puntos de urgencia del horario: se refrescan los tres.
        Widgets.refrescar(contexto)
    }
}

/** Relee la configuración de un widget (la que guarda `ConfiguracionWidgetsActivity`). */
fun Preferences.configWidget(): String? = this[ClaveConfig]

/** Un texto del widget en el idioma de Kiwu (Glance no pasa por `attachBaseContext`, así que se envuelve el contexto aquí). */
@androidx.compose.runtime.Composable
internal fun textoWidget(@androidx.annotation.StringRes id: Int, vararg datos: Any): String =
    app.kiwu.android.Idioma.contexto(androidx.glance.LocalContext.current).getString(id, *datos)

/** Palabras que `DatosWidgets` pone en los widgets, ya en el idioma de Kiwu (así sus funciones siguen siendo puras). */
data class TextosWidget(
    val hoy: String, val manana: String, val proximos: String, val deberes: String,
    val sinFecha: String, val vencida: String, val sinTitulo: String,
) {
    companion object {
        fun de(contexto: android.content.Context): TextosWidget {
            val t = app.kiwu.android.Idioma.contexto(contexto)
            app.kiwu.android.Textos.actualizar(t)
            return TextosWidget(
                hoy = t.getString(app.kiwu.android.R.string.hoy), manana = t.getString(app.kiwu.android.R.string.manana),
                proximos = t.getString(app.kiwu.android.R.string.proximos_7), deberes = t.getString(app.kiwu.android.R.string.deberes),
                sinFecha = t.getString(app.kiwu.android.R.string.seccion_sin_fecha), vencida = t.getString(app.kiwu.android.R.string.vencida),
                sinTitulo = t.getString(app.kiwu.android.R.string.sin_titulo),
            )
        }
    }
}
