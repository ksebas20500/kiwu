package app.kiwu.android.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.kiwu.android.KiwuApp
import app.kiwu.android.data.AjustesUi
import app.kiwu.android.domain.ID_EVENTOS
import app.kiwu.android.ui.KiwuTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import app.kiwu.android.R
import androidx.compose.ui.res.stringResource

/** Qué widget se está configurando. */
private enum class TipoWidget { DEBERES, HORARIO, NOTAS }

/**
 * Pantalla que sale al añadir un widget (los tres usan la misma): elige qué debe mostrar y lo guarda en el estado de
 * Glance de ese widget, sin tocar los demás.
 */
class ConfiguracionWidgetsActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var tipo = TipoWidget.DEBERES

    override fun attachBaseContext(base: android.content.Context) = super.attachBaseContext(app.kiwu.android.Idioma.contexto(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Si la persona cancela o cierra con «atrás», el widget no se añade.
        setResult(RESULT_CANCELED)
        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val proveedor = appWidgetManager.getAppWidgetInfo(appWidgetId)?.provider?.className.orEmpty()
        tipo = when {
            proveedor.contains("Deberes") -> TipoWidget.DEBERES
            proveedor.contains("Horario") -> TipoWidget.HORARIO
            else -> TipoWidget.NOTAS
        }
        val app = application as KiwuApp
        val ajustes = runBlocking { withTimeoutOrNull(1500) { app.ajustes.flujo.first() } } ?: AjustesUi()
        val oscuro = when (ajustes.tema) {
            "claro" -> false
            "oscuro" -> true
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
        setContent {
            KiwuTheme(oscuro, ajustes.acento) {
                Scaffold { relleno ->
                    PantallaConfiguracion(
                        app = app,
                        tipo = tipo,
                        appWidgetId = appWidgetId,
                        modifier = Modifier.padding(relleno),
                        alCancelar = { finish() },
                        alGuardar = ::guardar,
                    )
                }
            }
        }
    }

    private val appWidgetManager by lazy { AppWidgetManager.getInstance(this) }

    private fun guardar(valor: String) {
        lifecycleScope.launch {
            val id = GlanceAppWidgetManager(this@ConfiguracionWidgetsActivity).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(this@ConfiguracionWidgetsActivity, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply { this[ClaveConfig] = valor }
            }
            val contexto = this@ConfiguracionWidgetsActivity
            when (tipo) {
                TipoWidget.DEBERES -> DeberesWidget().update(contexto, id)
                TipoWidget.HORARIO -> HorarioWidget().update(contexto, id)
                TipoWidget.NOTAS -> NotasWidget().update(contexto, id)
            }
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }
}

@Composable
private fun PantallaConfiguracion(
    app: KiwuApp,
    tipo: TipoWidget,
    appWidgetId: Int,
    modifier: Modifier = Modifier,
    alCancelar: () -> Unit,
    alGuardar: (String) -> Unit,
) {
    val materias by app.repo.materias.collectAsStateWithLifecycle(emptyList())
    val grupos by app.repo.grupos.collectAsStateWithLifecycle(emptyList())
    val horarios by app.repo.horarios.collectAsStateWithLifecycle(emptyList())
    val cuadernos by app.repo.cuadernos.collectAsStateWithLifecycle(emptyList())
    // Al volver a configurar un widget ya puesto, se marca lo que tenía guardado.
    val contexto = LocalContext.current
    var guardada by remember(appWidgetId) { mutableStateOf<String?>(null) }
    LaunchedEffect(appWidgetId) {
        val id = GlanceAppWidgetManager(contexto).getGlanceIdBy(appWidgetId)
        val prefs: Preferences = getAppWidgetState(contexto, PreferencesGlanceStateDefinition, id)
        guardada = prefs[ClaveConfig]
    }
    var eleccion by rememberSaveable { mutableStateOf<String?>(null) }
    val seleccion = eleccion ?: guardada ?: ConfigWidget.TODAS

    val opciones = when (tipo) {
        TipoWidget.DEBERES -> buildList {
            add(ConfigWidget.TODAS to stringResource(R.string.todas_tareas))
            add(ConfigWidget.HOY to stringResource(R.string.hoy))
            add(ConfigWidget.PROXIMOS to stringResource(R.string.proximos_7))
            materias.filter { it.id != ID_EVENTOS }.forEach { add(ConfigWidget.lista(it.id) to "${it.emoji ?: "📋"} ${it.nombre}") }
            grupos.forEach { add(ConfigWidget.grupo(it.id) to "${it.icono} ${it.nombre}") }
        }
        TipoWidget.HORARIO -> buildList {
            add(ConfigWidget.TODAS to stringResource(R.string.w_todos_horarios))
            horarios.forEach { add(ConfigWidget.horario(it.id) to "${it.icono} ${it.nombre}") }
        }
        TipoWidget.NOTAS -> buildList {
            add(ConfigWidget.TODAS to stringResource(R.string.todas_notas))
            cuadernos.forEach { add(ConfigWidget.cuaderno(it.id) to "${it.icono} ${it.nombre}") }
        }
    }

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text(
            when (tipo) {
                TipoWidget.DEBERES -> stringResource(R.string.w_titulo_deberes)
                TipoWidget.HORARIO -> stringResource(R.string.w_titulo_horario)
                TipoWidget.NOTAS -> stringResource(R.string.w_titulo_notas)
            },
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )
        Text(
            when (tipo) {
                TipoWidget.DEBERES -> stringResource(R.string.w_ayuda_deberes)
                TipoWidget.HORARIO -> stringResource(R.string.w_ayuda_horario)
                TipoWidget.NOTAS -> stringResource(R.string.w_ayuda_notas)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(opciones, key = { it.first }) { (valor, etiqueta) ->
                Row(
                    Modifier.fillMaxWidth()
                        .selectable(selected = seleccion == valor, onClick = { eleccion = valor })
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = seleccion == valor, onClick = { eleccion = valor })
                    Text(etiqueta, Modifier.padding(start = 8.dp))
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = alCancelar) { Text(stringResource(R.string.cancelar)) }
            Button(onClick = { alGuardar(seleccion) }, modifier = Modifier.padding(start = 8.dp)) { Text(stringResource(R.string.guardar)) }
        }
    }
}
