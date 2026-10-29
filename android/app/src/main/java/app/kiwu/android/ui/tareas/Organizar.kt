package app.kiwu.android.ui.tareas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.kiwu.android.R
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.Etiquetas
import app.kiwu.android.domain.Paso
import app.kiwu.android.domain.Pasos
import app.kiwu.android.domain.Prioridad

private val AmbarMedia = Color(0xFFB26A00)

@Composable
fun colorPrioridad(p: Int): Color = when (p) {
    Prioridad.ALTA -> MaterialTheme.colorScheme.error
    Prioridad.MEDIA -> AmbarMedia
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun textoPrioridad(p: Int): String = stringResource(
    when (p) {
        Prioridad.BAJA -> R.string.prioridad_baja
        Prioridad.MEDIA -> R.string.prioridad_media
        Prioridad.ALTA -> R.string.prioridad_alta
        else -> R.string.sin_prioridad
    },
)

/** Chips de propiedades (fecha, aviso, repetición, prioridad…): borde fino y iconos grises, nunca del color de acento. */
@Composable
fun coloresChipNeutro() = AssistChipDefaults.assistChipColors(
    leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    trailingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

/** Una categoría (la lista, una etiqueta): fondo suave y sin borde, para distinguirla de las propiedades. */
@Composable
fun ChipCategoria(texto: String, onClick: () -> Unit, trailingIcon: (@Composable () -> Unit)? = null) {
    AssistChip(
        onClick = onClick, label = { Text(texto) }, trailingIcon = trailingIcon, border = null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            trailingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    )
}

/** Una acción, no un dato: borde punteado y texto gris («+ Etiqueta»). */
@Composable
fun ChipPunteado(texto: String, onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val forma = RoundedCornerShape(8.dp)
    // Mismo alto táctil (48 dp) que los chips de Material para que la fila quede alineada.
    Box(Modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
    Box(
        Modifier.height(32.dp).clip(forma).clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    color, cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                )
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Add, null, Modifier.size(16.dp), tint = color)
            Text(texto, Modifier.padding(start = 4.dp), color = color, style = MaterialTheme.typography.labelLarge)
        }
    }
    }
}

/**
 * Los chips de prioridad, examen y etiquetas: van dentro de la misma fila que la lista. Las etiquetas se añaden con un
 * botón «+ Etiqueta» que abre un cuadro pequeño.
 */
@Composable
fun ChipsOrganizar(
    d: Deber,
    alCambiarPrioridad: (Int) -> Unit,
    alCambiarEtiquetas: (List<String>) -> Unit,
    alCambiarExamen: (Boolean) -> Unit,
) {
    val etiquetas = Etiquetas.leer(d.etiquetas)
    var menu by remember { mutableStateOf(false) }
    var dialogo by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { menu = true }, colors = coloresChipNeutro(),
            label = { Text(textoPrioridad(d.prioridad)) },
            leadingIcon = { Icon(Icons.Filled.Flag, null, tint = colorPrioridad(d.prioridad)) },
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            for (p in listOf(Prioridad.NINGUNA, Prioridad.BAJA, Prioridad.MEDIA, Prioridad.ALTA)) {
                DropdownMenuItem(text = { Text(textoPrioridad(p)) }, onClick = { menu = false; alCambiarPrioridad(p) })
            }
        }
    }
    FilterChip(
        selected = d.examen, onClick = { alCambiarExamen(!d.examen) }, label = { Text(stringResource(R.string.examen)) }, border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    )
    for (e in etiquetas) {
        ChipCategoria("#$e", onClick = { alCambiarEtiquetas(etiquetas - e) }, trailingIcon = { Icon(Icons.Filled.Close, stringResource(R.string.quitar_etiqueta)) })
    }
    ChipPunteado(stringResource(R.string.etiqueta)) { dialogo = true }
    if (dialogo) {
        var texto by remember { mutableStateOf("") }
        val foco = remember { FocusRequester() }
        val cerrar = { dialogo = false }
        val aceptar = { if (texto.isNotBlank()) alCambiarEtiquetas(etiquetas + texto.removePrefix("#")); cerrar() }
        AlertDialog(
            onDismissRequest = cerrar,
            title = { Text(stringResource(R.string.etiqueta_nueva)) },
            text = {
                OutlinedTextField(
                    texto, { texto = it }, Modifier.focusRequester(foco), singleLine = true,
                    placeholder = { Text(stringResource(R.string.etiqueta_ejemplo)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { aceptar() }),
                )
                androidx.compose.runtime.LaunchedEffect(Unit) { foco.requestFocus() }
            },
            confirmButton = { TextButton(onClick = { aceptar() }, enabled = texto.isNotBlank()) { Text(stringResource(R.string.anadir)) } },
            dismissButton = { TextButton(onClick = cerrar) { Text(stringResource(R.string.cancelar)) } },
        )
    }
}

/** Los pasos de una tarea: la lista y un botón «+ Añadir paso» que muestra el campo solo cuando lo necesitas. */
@Composable
fun PasosTarea(d: Deber, alCambiarPasos: (List<Paso>) -> Unit) {
    val pasos = Pasos.leer(d.pasos)
    var escribiendo by remember { mutableStateOf(false) }
    var nuevo by remember { mutableStateOf("") }
    val foco = remember { FocusRequester() }
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        for ((i, p) in pasos.withIndex()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = p.hecho, onCheckedChange = { alCambiarPasos(pasos.toMutableList().also { l -> l[i] = p.copy(hecho = it) }) })
                Text(
                    p.texto, Modifier.weight(1f),
                    textDecoration = if (p.hecho) TextDecoration.LineThrough else null,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (p.hecho) 0.5f else 1f),
                )
                IconButton(onClick = { alCambiarPasos(pasos.toMutableList().also { l -> l.removeAt(i) }) }) {
                    Icon(Icons.Filled.Close, stringResource(R.string.paso_eliminar))
                }
            }
        }
        if (escribiendo) {
            OutlinedTextField(
                nuevo, { nuevo = it }, Modifier.fillMaxWidth().focusRequester(foco), singleLine = true,
                placeholder = { Text(stringResource(R.string.paso_ejemplo)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (nuevo.isNotBlank()) alCambiarPasos(pasos + Paso(nuevo.trim()))
                    nuevo = ""; escribiendo = false
                }),
            )
            androidx.compose.runtime.LaunchedEffect(Unit) { foco.requestFocus() }
        } else {
            TextButton(onClick = { escribiendo = true }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
                Icon(Icons.Filled.Add, null)
                Text(stringResource(R.string.paso_anadir), Modifier.padding(start = 4.dp))
            }
        }
    }
}
