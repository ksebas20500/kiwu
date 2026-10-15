package app.kiwu.android.ui.tareas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import app.kiwu.android.domain.Materia
import app.kiwu.android.domain.Reglas
import app.kiwu.android.ui.Chip
import app.kiwu.android.ui.Fechas

private val VerdeHoy = Color(0xFF2E7D32)

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
    Chip(Fechas.corta(entrega, ahora), fondo = color.copy(alpha = 0.12f), color = color)
}

/** Menú de una tarea: mover entre columnas del tablero y eliminar. */
@Composable
fun MenuTarea(
    abierto: Boolean,
    alCerrar: () -> Unit,
    columnaActual: Columna,
    alMover: (Columna) -> Unit,
    alEliminar: () -> Unit,
) {
    DropdownMenu(expanded = abierto, onDismissRequest = alCerrar) {
        for (c in Columna.entries) {
            if (c != columnaActual) {
                DropdownMenuItem(text = { Text("Mover a «${c.titulo}»") }, onClick = { alCerrar(); alMover(c) })
            }
        }
        DropdownMenuItem(
            text = { Text("Eliminar tarea", color = MaterialTheme.colorScheme.error) },
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
    alMover: (Columna) -> Unit,
    alEliminar: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val columna = reglas.columnaDe(d)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = alAbrir).padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = d.completado, onCheckedChange = alMarcar)
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
                        Chip("En curso", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    ChipFecha(d, ahora, reglas)
                    if (nombreLista != null) {
                        Text(nombreLista, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Opciones de la tarea") }
            MenuTarea(menu, { menu = false }, columna, alMover, alEliminar)
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
    marcador: String = "Añadir tarea",
    /** El calendario fija el día que se está viendo: entonces el botón solo elige la hora de entrega. */
    diaFijo: java.time.LocalDate? = null,
) {
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            val puesta = if (diaFijo != null) hora != null else fecha != null
            IconButton(onClick = { selector = true }) {
                Icon(
                    if (diaFijo != null) Icons.Filled.Schedule else Icons.Filled.Event,
                    if (diaFijo != null) "Elegir la hora de entrega" else "Elegir fecha de entrega",
                    tint = if (puesta) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (diaFijo != null) {
                hora?.let { (h, m) -> Box(Modifier.clickable { hora = null }) { Chip("%02d:%02d  ✕".format(h, m)) } }
            } else {
                fecha?.let { f -> Box(Modifier.clickable { fecha = null }) { Chip(Fechas.corta(f, System.currentTimeMillis()) + "  ✕") } }
            }
            TextField(
                value = texto,
                onValueChange = { texto = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (materias.isEmpty()) "Crea una lista primero" else marcador) },
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
                Icon(Icons.AutoMirrored.Filled.Send, "Añadir")
            }
        }
    }
    if (selector) {
        if (diaFijo != null) SelectorHora(hora?.first ?: 18, hora?.second ?: 0, alCerrar = { selector = false }) { h, m -> hora = h to m }
        else SelectorFechaHora(fecha, alCerrar = { selector = false }, alElegir = { fecha = it })
    }
}
