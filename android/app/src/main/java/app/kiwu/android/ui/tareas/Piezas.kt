package app.kiwu.android.ui.tareas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import app.kiwu.android.ui.SelectorHora
import app.kiwu.android.ui.SelectorFechaHora
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kiwu.android.domain.Columna
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.esEvento
import androidx.compose.foundation.layout.size
import app.kiwu.android.domain.Materia
import app.kiwu.android.domain.Reglas
import app.kiwu.android.ui.Chip
import app.kiwu.android.ui.Fechas
import app.kiwu.android.ui.fechaCorta
import app.kiwu.android.R
import androidx.compose.ui.res.stringResource

private val VerdeHoy = Color(0xFF2E7D32)

/** Los eventos van siempre de azul claro pastel (acuarela suave), con su texto azul grisáceo, en la app y en el widget. */
val AzulEvento = Color(0xFFCFE5F7)
val AzulEventoTexto = Color(0xFF3B6486)

/** Chip con la fecha de entrega: rojo si venció, verde si es hoy. */
@Composable
fun ChipFecha(d: Deber, ahora: Long, reglas: Reglas) {
    val entrega = d.fechaEntrega ?: return
    val vencida = reglas.estaVencido(d, ahora)
    val hoy = !d.completado && !vencida && reglas.inicioDelDia(entrega) == reglas.inicioDelDia(ahora)
    val color = when {
        vencida -> MaterialTheme.colorScheme.error
        hoy -> VerdeHoy
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Chip(fechaCorta(entrega, ahora, completo = true), fondo = color.copy(alpha = 0.12f), color = color)
}

/** Menú de una tarea: solo eliminar (mover de columna se hace arrastrando en el tablero o desde el detalle). */
@Composable
fun MenuTarea(
    abierto: Boolean,
    alCerrar: () -> Unit,
    alEliminar: () -> Unit,
    esEvento: Boolean = false,
) {
    DropdownMenu(expanded = abierto, onDismissRequest = alCerrar) {
        DropdownMenuItem(
            text = { Text(stringResource(if (esEvento) R.string.evento_eliminar else R.string.tarea_eliminar), color = MaterialTheme.colorScheme.error) },
            onClick = { alCerrar(); alEliminar() },
        )
    }
}

/** Una fila de la lista de tareas. */
@Composable
fun FilaTarea(
    d: Deber,
    nombreLista: String?,
    ahora: Long,
    reglas: Reglas,
    alAbrir: () -> Unit,
    alMarcar: (Boolean) -> Unit,
        alEliminar: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val columna = reglas.columnaDe(d)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = alAbrir).padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (d.esEvento) {
            // Un evento no se completa: en lugar del círculo lleva el icono de calendario.
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Event, stringResource(R.string.evento), tint = AzulEventoTexto)
            }
        } else {
            Checkbox(checked = d.completado, onCheckedChange = alMarcar)
        }
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(
                d.titulo,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (d.completado) TextDecoration.LineThrough else null,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (d.completado) 0.5f else 1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            val hayExtras = d.fechaEntrega != null || nombreLista != null || columna == Columna.EN_CURSO
            if (hayExtras) {
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (columna == Columna.EN_CURSO) {
                        Chip(stringResource(R.string.en_curso), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    ChipFecha(d, ahora, reglas)
                    if (nombreLista != null) {
                        Text(nombreLista, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.tarea_opciones)) }
            MenuTarea(menu, { menu = false }, alEliminar, esEvento = d.esEvento)
        }
    }
}

/** Campo «Añadir tarea» con selector de lista (si no hay una lista fija). */
@Composable
fun CampoNuevaTarea(
    materias: List<Materia>,
    fija: Materia?,
    alCrear: (titulo: String, materiaId: String, fecha: Long?) -> Unit,
    modifier: Modifier = Modifier,
    marcador: String = stringResource(R.string.anadir_tarea),
    /** El calendario fija el día que se está viendo: entonces el botón solo elige la hora de entrega. */
    diaFijo: java.time.LocalDate? = null,
    /** Pone el cursor en el campo al abrirse (la pantalla de añadir desde el widget). */
    enfocar: Boolean = false,
) {
    val foco = remember { androidx.compose.ui.focus.FocusRequester() }
    // El campo está desactivado hasta que llegan las listas: se enfoca cuando ya se puede escribir.
    LaunchedEffect(materias.isNotEmpty()) { if (enfocar && materias.isNotEmpty()) foco.requestFocus() }
    var texto by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf<Long?>(null) }
    var hora by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var selector by remember { mutableStateOf(false) }
    var destino by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    val elegida = fija ?: materias.firstOrNull { it.id == destino } ?: materias.firstOrNull()
    val enviar = {
        if (texto.isNotBlank() && elegida != null) {
            val entrega = if (diaFijo != null) hora?.let { (h, m) -> diaFijo.atTime(h, m).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() } else fecha
            alCrear(texto.trim(), elegida.id, entrega)
            texto = ""
            fecha = null
            hora = null
        }
    }
    Surface(
        modifier = modifier.fillMaxWidth().imePadding().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    ) {
        Column {
        // La fecha elegida va en su propia línea: en una fila con el campo le quitaba el ancho y el recuadro crecía a lo alto.
        if (diaFijo != null) {
            hora?.let { (h, m) -> Box(Modifier.padding(start = 12.dp, top = 8.dp).clickable { hora = null }) { Chip("%02d:%02d  ✕".format(h, m)) } }
        } else {
            fecha?.let { f -> Box(Modifier.padding(start = 12.dp, top = 8.dp).clickable { fecha = null }) { Chip(fechaCorta(f, System.currentTimeMillis(), completo = true) + "  ✕") } }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val puesta = if (diaFijo != null) hora != null else fecha != null
            IconButton(onClick = { selector = true }) {
                Icon(
                    if (diaFijo != null) Icons.Filled.Schedule else Icons.Filled.Event,
                    stringResource(if (diaFijo != null) R.string.elegir_hora_entrega else R.string.elegir_fecha_entrega),
                    tint = if (puesta) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextField(
                value = texto,
                onValueChange = { texto = it },
                modifier = Modifier.weight(1f).widthIn(min = 96.dp).focusRequester(foco),
                placeholder = { Text(if (materias.isEmpty()) stringResource(R.string.crea_lista_primero) else marcador) },
                enabled = materias.isNotEmpty(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { enviar() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )
            if (fija == null && materias.isNotEmpty() && elegida != null) {
                Box {
                    TextButton(onClick = { menu = true }) {
                        Text(elegida.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(end = 2.dp))
                        Icon(Icons.Filled.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        for (m in materias) DropdownMenuItem(text = { Text(m.nombre) }, onClick = { destino = m.id; menu = false })
                    }
                }
            }
            IconButton(onClick = enviar, enabled = texto.isNotBlank() && elegida != null) {
                Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.anadir))
            }
        }
        }
    }
    if (selector) {
        if (diaFijo != null) SelectorHora(hora?.first ?: 18, hora?.second ?: 0, alCerrar = { selector = false }) { h, m -> hora = h to m }
        else SelectorFechaHora(fecha, alCerrar = { selector = false }, alElegir = { fecha = it })
    }
}
