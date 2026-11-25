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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.semantics.Role
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
import app.kiwu.android.ui.Acentos
import app.kiwu.android.ui.KiwuTheme
import kotlinx.coroutines.launch
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
    private var guardando by mutableStateOf(false)
    private var error by mutableStateOf(false)

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
        setContent {
            val ajustes by app.ajustes.flujo.collectAsStateWithLifecycle(AjustesUi())
            val oscuro = when (ajustes.tema) {
                "claro" -> false
                "oscuro", Acentos.NEGRO -> true
                else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            }
            KiwuTheme(oscuro, ajustes.acento, amoled = false) {
                Scaffold { relleno ->
                    PantallaConfiguracion(
                        app = app,
                        tipo = tipo,
                        appWidgetId = appWidgetId,
                        modifier = Modifier.padding(relleno),
                        alCancelar = { finish() },
                        alGuardar = ::guardar,
                        guardando = guardando,
                        error = error,
                    )
                }
            }
        }
    }

    private val appWidgetManager by lazy { AppWidgetManager.getInstance(this) }

    private fun guardar(valor: String) {
        if (guardando) return
        guardando = true
        error = false
        lifecycleScope.launch {
            try {
                guardarConfiguracion(valor)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                error = true
            } finally {
                guardando = false
            }
        }
    }

    private suspend fun guardarConfiguracion(valor: String) {
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

@Composable
private fun PantallaConfiguracion(
    app: KiwuApp,
    tipo: TipoWidget,
    appWidgetId: Int,
    modifier: Modifier = Modifier,
    alCancelar: () -> Unit,
    alGuardar: (String) -> Unit,
    guardando: Boolean,
    error: Boolean,
) {
    val materias by app.repo.materias.collectAsStateWithLifecycle(emptyList())
    val grupos by app.repo.grupos.collectAsStateWithLifecycle(emptyList())
    val horarios by app.repo.horarios.collectAsStateWithLifecycle(emptyList())
    val cuadernos by app.repo.cuadernos.collectAsStateWithLifecycle(emptyList())
    // Al volver a configurar un widget ya puesto, se marca lo que tenía guardado.
    val contexto = LocalContext.current
    var guardada by remember(appWidgetId) { mutableStateOf<String?>(null) }
    var errorLectura by remember(appWidgetId) { mutableStateOf(false) }
    var intento by remember(appWidgetId) { mutableStateOf(0) }
    LaunchedEffect(appWidgetId, intento) {
        errorLectura = false
        try {
            val id = GlanceAppWidgetManager(contexto).getGlanceIdBy(appWidgetId)
            val prefs: Preferences = getAppWidgetState(contexto, PreferencesGlanceStateDefinition, id)
            guardada = prefs[ClaveConfig] ?: ConfigWidget.TODAS
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            errorLectura = true
        }
    }
    var eleccion by rememberSaveable { mutableStateOf<String?>(null) }

    val ubicaciones = remember(materias, grupos) { ubicacionesDeListas(materias, grupos) }
    val opciones = when (tipo) {
        TipoWidget.DEBERES -> buildList {
            add(ConfigWidget.TODAS to stringResource(R.string.todas_tareas))
            add(ConfigWidget.HOY to stringResource(R.string.hoy))
            add(ConfigWidget.PROXIMOS to stringResource(R.string.proximos_7))
            if (grupos.isNotEmpty()) add("encabezado:carpetas" to stringResource(R.string.w_carpetas))
            grupos.forEach { add(ConfigWidget.grupo(it.id) to "${it.icono} ${it.nombre}") }
            val listas = materias.filter { it.id != ID_EVENTOS }
            if (listas.isNotEmpty()) add("encabezado:listas" to stringResource(R.string.w_listas))
            listas.forEach { add(ConfigWidget.lista(it.id) to "${it.emoji ?: "📋"} ${ubicaciones[it.id]?.etiqueta ?: it.nombre}") }
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

    val seleccion = (eleccion ?: guardada)?.takeIf { valor -> opciones.any { it.first == valor } } ?: ConfigWidget.TODAS

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
            if (guardada != null) items(opciones, key = { it.first }) { (valor, etiqueta) ->
                if (valor.startsWith("encabezado:")) {
                    Text(etiqueta, Modifier.padding(top = 12.dp, bottom = 4.dp), style = MaterialTheme.typography.labelLarge)
                } else Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .selectable(selected = seleccion == valor, enabled = !guardando, role = Role.RadioButton, onClick = { eleccion = valor })
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = seleccion == valor, onClick = null)
                    Text(etiqueta, Modifier.padding(start = 8.dp))
                }
            }
        }
        if (guardada == null && !errorLectura || guardando) {
            androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        if (errorLectura || error) {
            Text(stringResource(R.string.w_error_config), color = MaterialTheme.colorScheme.error)
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = alCancelar, enabled = !guardando, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cancelar)) }
            Button(
                onClick = { if (errorLectura) intento++ else alGuardar(seleccion) },
                enabled = !guardando && (errorLectura || guardada != null),
                modifier = Modifier.padding(start = 8.dp).heightIn(min = 48.dp),
            ) { Text(stringResource(if (errorLectura) R.string.w_reintentar else R.string.guardar)) }
        }
    }
}
