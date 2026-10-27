package app.kiwu.android.widgets

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.esEvento
import app.kiwu.android.ui.Fechas
import app.kiwu.android.ui.KiwuTheme
import app.kiwu.android.ui.tareas.CampoNuevaTarea
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
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
        val dia = LocalDate.ofEpochDay(intent?.getLongExtra(EXTRA_DIA, LocalDate.now().toEpochDay()) ?: LocalDate.now().toEpochDay())
        val ajustes = runBlocking { withTimeoutOrNull(1500) { app.ajustes.flujo.first() } } ?: AjustesUi()
        val oscuro = when (ajustes.tema) {
            "claro" -> false
            "oscuro" -> true
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
        setContent {
            KiwuTheme(oscuro, ajustes.acento) {
                val materias by app.repo.materias.collectAsStateWithLifecycle(emptyList())
                val deberes by app.repo.deberes.collectAsStateWithLifecycle(emptyList())
                val zona = ZoneId.systemDefault()
                val delDia = remember(deberes, dia) {
                    deberes.filter { d ->
                        d.fechaEntrega != null && (d.esEvento || !d.completado) &&
                            Instant.ofEpochMilli(d.fechaEntrega).atZone(zona).toLocalDate() == dia
                    }.sortedBy { it.fechaEntrega }
                }
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { finish() },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    // Tocar la hoja no la cierra: solo el fondo oscuro.
                    Surface(
                        Modifier.pointerInput(Unit) { detectTapGestures { } },
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Column {
                            Text(Fechas.diaLargo(dia), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp))
                            if (delDia.isEmpty()) {
                                Text(
                                    stringResource(R.string.dia_sin_pendientes), color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                )
                            } else {
                                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                                    items(delDia, key = { it.id }) { d ->
                                        val lista = materias.firstOrNull { it.id == d.materiaId }?.nombre
                                        FilaDia(
                                            d, lista, zona,
                                            alCompletar = { app.alcance.launch { app.repo.marcar(d.id, true) } },
                                            alAbrir = { startActivity(intencionDeApertura(this@DiaCalendarioActivity, tarea = d.id)); finish() },
                                        )
                                    }
                                }
                            }
                            CampoNuevaTarea(
                                materias = materias, fija = null, diaFijo = dia,
                                alCrear = { titulo, materiaId, fecha ->
                                    // Sin hora elegida, la tarea vence ese día a las 18:00, como en el calendario de la app.
                                    val entrega = fecha ?: dia.atTime(LocalTime.of(18, 0)).atZone(zona).toInstant().toEpochMilli()
                                    app.alcance.launch { app.repo.crearDeber(titulo, materiaId, entrega) }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        /** El día que se abre, como `LocalDate.toEpochDay()`. */
        const val EXTRA_DIA = "dia"
    }
}

@androidx.compose.runtime.Composable
private fun FilaDia(d: Deber, lista: String?, zona: ZoneId, alCompletar: () -> Unit, alAbrir: () -> Unit) {
    val hora = d.fechaEntrega?.let { Instant.ofEpochMilli(it).atZone(zona).toLocalTime() }?.takeIf { it != LocalTime.MIDNIGHT }
    val detalle = listOfNotNull(lista, hora?.let { Fechas.hora(d.fechaEntrega!!, zona) }).joinToString(" · ")
    Row(Modifier.fillMaxWidth().clickable(onClick = alAbrir).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        // Los eventos no se completan: en su lugar va un hueco del mismo ancho para que el texto quede alineado.
        if (d.esEvento) Spacer(Modifier.size(48.dp)) else Checkbox(checked = false, onCheckedChange = { alCompletar() })
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(d.titulo, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detalle.isNotBlank()) Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
