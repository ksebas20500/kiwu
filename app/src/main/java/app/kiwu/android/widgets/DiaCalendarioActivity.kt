package app.kiwu.android.widgets

import app.kiwu.android.ui.Acentos
import app.kiwu.android.ui.casillaDe
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import app.kiwu.android.ui.tareas.AzulEventoTexto
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.KiwuApp
import app.kiwu.android.R
import app.kiwu.android.data.AjustesUi
import app.kiwu.android.data.CalendarioTelefono
import app.kiwu.android.data.TareasGoogle
import app.kiwu.android.domain.EventoExterno
import app.kiwu.android.domain.ID_EVENTOS
import app.kiwu.android.domain.Deber
import app.kiwu.android.ui.tareas.ListaEventos
import app.kiwu.android.domain.esEvento
import app.kiwu.android.ui.Fechas
import app.kiwu.android.ui.KiwuTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Al tocar un día del widget de calendario: una hoja sobre el escritorio con lo pendiente de ese día y el campo para
 * añadir una tarea, sin abrir Kiwu. Se puede completar una tarea desde aquí o tocarla para abrirla en la app.
 */
class DiaCalendarioActivity : ComponentActivity() {
    override fun attachBaseContext(base: android.content.Context) = super.attachBaseContext(app.kiwu.android.Idioma.contexto(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as KiwuApp
        val abrirCaptura = intent?.getBooleanExtra(EXTRA_CREAR, false) == true
        val dia = LocalDate.ofEpochDay(intent?.getLongExtra(EXTRA_DIA, LocalDate.now().toEpochDay()) ?: LocalDate.now().toEpochDay())
        setContent {
            val ajustes by app.ajustes.flujo.collectAsStateWithLifecycle(AjustesUi())
            val oscuro = when (ajustes.tema) {
                "claro" -> false
                "oscuro", Acentos.NEGRO -> true
                else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            }
            KiwuTheme(oscuro, ajustes.acento, amoled = false) {
                val materias by app.repo.materias.collectAsStateWithLifecycle(emptyList())
                val grupos by app.repo.grupos.collectAsStateWithLifecycle(emptyList())
                val deberes by app.repo.deberes.collectAsStateWithLifecycle(emptyList())
                val ubicaciones = remember(materias, grupos) { ubicacionesDeListas(materias, grupos) }
                val zona = ZoneId.systemDefault()
                fun externosDe(): List<EventoExterno> =
                    (if (ajustes.calendarioGoogle) CalendarioTelefono.eventos(this@DiaCalendarioActivity, dia, dia, zona) else emptyList()) +
                        (if (ajustes.tareasGoogle) TareasGoogle.leer(this@DiaCalendarioActivity).filter { it.dia(zona) == dia } else emptyList())
                var externos by remember(dia, ajustes.calendarioGoogle, ajustes.tareasGoogle) {
                    mutableStateOf<List<EventoExterno>?>(null)
                }
                // Las tareas de Google salen de la copia local del widget (máximo 5 minutos de edad): al abrir la
                // hoja se le pide la lista fresca, para que lo que se ve aquí esté tan al día como se pueda.
                LaunchedEffect(dia, ajustes.calendarioGoogle, ajustes.tareasGoogle) {
                    externos = withContext(Dispatchers.IO) { externosDe() }
                    if (ajustes.tareasGoogle) {
                        val nueva = withContext(Dispatchers.IO) { TareasGoogle.actualizar(this@DiaCalendarioActivity) }
                        if (nueva) externos = withContext(Dispatchers.IO) { externosDe() }
                    }
                }
                val delDia = remember(deberes, dia) {
                    deberes.filter { d ->
                        d.fechaEntrega != null && (d.esEvento || !d.completado) &&
                            Instant.ofEpochMilli(d.fechaEntrega).atZone(zona).toLocalDate() == dia
                    }.sortedBy { it.fechaEntrega }
                }
                var creando by rememberSaveable { mutableStateOf(if (abrirCaptura) "tarea" else null) }
                var cerrarSolicitado by rememberSaveable { mutableStateOf(0) }
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (creando == null) finish() else cerrarSolicitado++ },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    // Tocar la hoja no la cierra: solo el fondo oscuro.
                    Surface(
                        Modifier.statusBarsPadding()
                            .then(if (creando != null) Modifier.padding(horizontal = 12.dp, vertical = 8.dp) else Modifier)
                            .pointerInput(Unit) { detectTapGestures { } },
                        shape = if (creando == null) RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp) else RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        val captura = creando
                        if (captura != null) {
                            Box(Modifier.navigationBarsPadding().imePadding()) {
                                CapturaTareaWidget(
                                    listas = if (captura == ID_EVENTOS) listOf(ListaEventos) else materias.filter { it.id != ID_EVENTOS },
                                    ubicaciones = ubicaciones,
                                    tituloFormulario = stringResource(if (captura == ID_EVENTOS) R.string.anadir_evento else R.string.anadir_tarea),
                                    fechaInicial = entregaWidget(dia),
                                    diaFijo = dia,
                                    cerrarSolicitado = cerrarSolicitado,
                                    cerrarDirectamente = abrirCaptura,
                                    alCerrar = { if (abrirCaptura) finish() else creando = null },
                                    guardar = { titulo, lista, fecha, prioridad, descripcion ->
                                        app.repo.crearDeber(titulo, lista, fecha, prioridad, descripcion)
                                    },
                                )
                            }
                        } else Column(Modifier.navigationBarsPadding().imePadding()) {
                            Text(Fechas.diaLargo(dia), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp))
                            if (externos == null && delDia.isEmpty()) {
                                LinearProgressIndicator(Modifier.fillMaxWidth())
                            } else if (delDia.isEmpty() && externos.isNullOrEmpty()) {
                                Text(
                                    stringResource(R.string.dia_sin_pendientes), color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                )
                            } else {
                                LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 240.dp)) {
                                    items(externos.orEmpty(), key = { "g:${it.id}:${it.inicio}" }) { e -> FilaExterna(e, zona) }
                                    items(delDia, key = { it.id }) { d ->
                                        val ubicacion = ubicaciones[d.materiaId]
                                        FilaDia(
                                            d, ubicacion?.lista, ubicacion?.carpeta, zona,
                                            alCompletar = { app.alcance.launch { app.repo.marcar(d.id, true) } },
                                            alAbrir = { startActivity(intencionDeApertura(this@DiaCalendarioActivity, tarea = d.id)); finish() },
                                        )
                                    }
                                }
                            }
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            ) {
                                TextButton(
                                    onClick = { creando = ID_EVENTOS },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) { Text(stringResource(R.string.anadir_evento)) }
                                Button(
                                    onClick = { creando = "tarea" },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) { Text(stringResource(R.string.anadir_tarea)) }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        /** El día que se abre, como `LocalDate.toEpochDay()`. */
        const val EXTRA_DIA = "dia"
        const val EXTRA_CREAR = "crear"
    }
}

@androidx.compose.runtime.Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FilaDia(d: Deber, lista: String?, carpeta: String?, zona: ZoneId, alCompletar: () -> Unit, alAbrir: () -> Unit) {
    val hora = d.fechaEntrega?.let { Instant.ofEpochMilli(it).atZone(zona).toLocalTime() }?.takeIf { it != LocalTime.MIDNIGHT }
    val horaTexto = hora?.let { Fechas.hora(d.fechaEntrega!!, zona) }
    val detalle = listOfNotNull(lista, horaTexto).joinToString(" · ")
    Row(Modifier.fillMaxWidth().clickable(onClick = alAbrir).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        // Los eventos no se completan: en su lugar va un hueco del mismo ancho para que el texto quede alineado.
        if (d.esEvento) Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Event, null, tint = AzulEventoTexto) } else Checkbox(checked = false, onCheckedChange = { alCompletar() }, modifier = Modifier.casillaDe(d.titulo))
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(d.titulo, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (carpeta != null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Text(carpeta, Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                    }
                    if (lista != null) Text(lista, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (horaTexto != null) Text(horaTexto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (detalle.isNotBlank()) {
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Un evento de Google Calendar: solo se ve (se edita en Google Calendar). */
@androidx.compose.runtime.Composable
private fun FilaExterna(e: EventoExterno, zona: ZoneId) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Icon(if (e.tarea) Icons.Filled.CheckCircle else Icons.Filled.Event, null, tint = AzulEventoTexto) }
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(e.titulo, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(stringResource(if (e.tarea) R.string.google_tareas else R.string.google_calendar), if (e.todoElDia) null else Fechas.hora(e.inicio, zona)).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
            )
        }
    }
}
