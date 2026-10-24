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
    if (tarea != null) putExtra(MainActivity.EXTRA_TAREA, tarea)
    if (nota != null) putExtra(MainActivity.EXTRA_NOTA, nota)
    if (nuevaNotaEnCuaderno != null) putExtra(MainActivity.EXTRA_NUEVA_NOTA, nuevaNotaEnCuaderno)
    if (seccion != null) putExtra(MainActivity.EXTRA_SECCION, seccion)
}

/**
 * Refresco de los widgets: cuando cambian los datos de la app (cualquier lista, tarea, nota, clase o ajuste) y cuando
 * toca por una alarma (cambio de clase o medianoche).
 */
object Widgets {
    /** Vuelve a dibujar los tres widgets a partir de la base de datos. */
    suspend fun refrescar(contexto: Context) {
        DeberesWidget().updateAll(contexto)
        HorarioWidget().updateAll(contexto)
        NotasWidget().updateAll(contexto)
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
