package app.kiwu.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import app.kiwu.android.R
import androidx.compose.ui.res.stringResource

/** Cuadro para pedir un texto (nombre de lista, de cuaderno…). */
@Composable
fun DialogoTexto(
    titulo: String,
    etiqueta: String,
    inicial: String = "",
    accion: String = stringResource(R.string.guardar),
    alCerrar: () -> Unit,
    alConfirmar: (String) -> Unit,
) {
    var texto by remember { mutableStateOf(inicial) }
    val enviar = { if (texto.isNotBlank()) { alConfirmar(texto.trim()); alCerrar() } }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(titulo) },
        text = {
            OutlinedTextField(
                value = texto, onValueChange = { texto = it }, label = { Text(etiqueta) }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { enviar() }),
            )
        },
        confirmButton = { TextButton(onClick = enviar, enabled = texto.isNotBlank()) { Text(accion) } },
        dismissButton = { TextButton(onClick = alCerrar) { Text(stringResource(R.string.cancelar)) } },
    )
}

/** Confirmación antes de una acción que borra datos. */
@Composable
fun DialogoConfirmar(titulo: String, mensaje: String, accion: String, alCerrar: () -> Unit, alConfirmar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(onClick = { alConfirmar(); alCerrar() }) { Text(accion, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text(stringResource(R.string.cancelar)) } },
    )
}

/** Etiqueta pequeña redondeada (estado, fecha, lista…). */
@Composable
fun Chip(texto: String, fondo: Color = MaterialTheme.colorScheme.surfaceVariant, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Surface(color = fondo, contentColor = color, shape = RoundedCornerShape(99.dp)) {
        Text(texto, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
fun EstadoVacio(icono: ImageVector, texto: String, modifier: Modifier = Modifier, contenido: @Composable () -> Unit = {}) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icono, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
        Text(texto, modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), fontSize = 17.sp)
        contenido()
    }
}

/** Elige día y hora (dos cuadros seguidos). Devuelve milisegundos locales. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFechaHora(inicial: Long?, alCerrar: () -> Unit, alElegir: (Long) -> Unit) {
    val zona = ZoneId.systemDefault()
    val base: LocalDateTime = inicial?.let { Instant.ofEpochMilli(it).atZone(zona).toLocalDateTime() }
        ?: LocalDate.now(zona).atTime(18, 0)
    var dia by remember { mutableStateOf<LocalDate?>(null) }

    if (dia == null) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = base.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = alCerrar,
            confirmButton = {
                // La hora es opcional: «Aceptar» deja solo el día (todo el día) y «Añadir hora» pasa al segundo cuadro.
                androidx.compose.foundation.layout.Row {
                    TextButton(onClick = {
                        estado.selectedDateMillis?.let { dia = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    }) { Text(stringResource(R.string.anadir_hora)) }
                    TextButton(onClick = {
                        estado.selectedDateMillis?.let {
                            val d = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                            alElegir(d.atStartOfDay(zona).toInstant().toEpochMilli())
                            alCerrar()
                        }
                    }) { Text(stringResource(R.string.aceptar)) }
                }
            },
            dismissButton = { TextButton(onClick = alCerrar) { Text(stringResource(R.string.cancelar)) } },
        ) { DatePicker(state = estado) }
    } else {
        var h by remember { mutableStateOf(base.hour) }
        var m by remember { mutableStateOf(base.minute) }
        // Las 00:00 significan «todo el día»: la hora no se muestra ni se avisa a una hora concreta.
        var todoElDia by remember { mutableStateOf(inicial != null && base.hour == 0 && base.minute == 0) }
        AlertDialog(
            onDismissRequest = alCerrar,
            title = { Text(stringResource(R.string.hora_entrega)) },
            text = {
                Column {
                    androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(stringResource(R.string.todo_el_dia))
                        androidx.compose.material3.Switch(checked = todoElDia, onCheckedChange = { todoElDia = it })
                    }
                    if (todoElDia) {
                        Text(
                            stringResource(R.string.sin_hora), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp),
                        )
                    } else {
                        RuedaHora(h, m) { nh, nm -> h = nh; m = nm }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val hora = if (todoElDia) LocalTime.MIDNIGHT else LocalTime.of(h, m)
                    val elegido = LocalDateTime.of(dia!!, hora).atZone(zona).toInstant().toEpochMilli()
                    alElegir(elegido)
                    alCerrar()
                }) { Text(stringResource(R.string.aceptar)) }
            },
            dismissButton = { TextButton(onClick = { dia = null }) { Text(stringResource(R.string.atras)) } },
        )
    }
}

/** Elige solo el día (calendario), sin pedir la hora. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorDia(inicial: LocalDate, alCerrar: () -> Unit, alElegir: (LocalDate) -> Unit) {
    val estado = rememberDatePickerState(initialSelectedDateMillis = inicial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = alCerrar,
        confirmButton = {
            TextButton(onClick = {
                estado.selectedDateMillis?.let { alElegir(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                alCerrar()
            }) { Text(stringResource(R.string.aceptar)) }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text(stringResource(R.string.cancelar)) } },
    ) { DatePicker(state = estado) }
}

/**
 * Elige una hora con dos ruedas que se desplazan (como la alarma del reloj). La hora llega en 0..24 (24:00 = medianoche
 * al final del día) y se muestra como 00:00; las ruedas solo conocen 00–23.
 */
@Composable
fun SelectorHora(hora: Int, minuto: Int, alCerrar: () -> Unit, titulo: String = stringResource(R.string.hora_entrega), paso: Int = 1, alElegir: (Int, Int) -> Unit) {
    var h by remember { mutableStateOf(hora % 24) }
    var m by remember { mutableStateOf(minutoAjustado(minuto, paso)) }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(titulo) },
        text = { RuedaHora(h, m, paso) { nh, nm -> h = nh; m = nm } },
        confirmButton = { TextButton(onClick = { alElegir(h, m); alCerrar() }) { Text(stringResource(R.string.aceptar)) } },
        dismissButton = { TextButton(onClick = alCerrar) { Text(stringResource(R.string.cancelar)) } },
    )
}

/** Carga algo pesado fuera del hilo principal cuando cambian las [claves]; mientras tanto devuelve el valor anterior (o null). */
@Composable
fun <T> cargarAsync(vararg claves: Any?, cargar: suspend () -> T?): androidx.compose.runtime.State<T?> {
    val estado = remember { mutableStateOf<T?>(null) }
    androidx.compose.runtime.LaunchedEffect(*claves) { estado.value = cargar() }
    return estado
}
