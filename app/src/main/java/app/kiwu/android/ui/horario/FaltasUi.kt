package app.kiwu.android.ui.horario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiwu.android.domain.Clase
import app.kiwu.android.domain.ContadorFaltas
import app.kiwu.android.domain.EstadoFaltas
import app.kiwu.android.domain.Falta
import app.kiwu.android.domain.ReglasFaltas
import app.kiwu.android.ui.SelectorDia
import java.time.LocalDate
import app.kiwu.android.ui.Fechas
import android.content.res.Resources
import androidx.compose.ui.platform.LocalContext
import app.kiwu.android.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/** Color del contador de faltas: verde, ámbar o rojo según lo cerca del máximo; neutro sin límite. */
internal fun colorFaltas(e: EstadoFaltas, neutro: Color): Color = when (e) {
    EstadoFaltas.Tranquilo -> Color(0xFF2E7D32)
    EstadoFaltas.Aviso -> Color(0xFFD97706)
    EstadoFaltas.Superado -> Color(0xFFD32F2F)
    EstadoFaltas.SinLimite -> neutro
}

/** Texto del chip de la hoja: «Faltas: 2» o, con máximo, «Faltas: 2/5». */
@Composable
internal fun textoChipFaltas(c: ContadorFaltas?): String =
    if (c?.maximo != null) stringResource(R.string.faltas_chip_max, c.cuentan, c.maximo) else stringResource(R.string.faltas_chip, c?.cuentan ?: 0)

/** Texto corto de la tarjeta de la clase (y del widget): cuántas quedan o, sin máximo, cuántas lleva. */
internal fun textoCortoFaltas(c: ContadorFaltas, r: Resources): String {
    val quedan = c.quedan
    return when (quedan) {
        null -> r.getQuantityString(R.plurals.faltas_n, c.cuentan, c.cuentan)
        0 -> r.getString(R.string.faltas_al_limite)
        else -> r.getQuantityString(R.plurals.faltas_quedan, quedan, quedan)
    }
}

/**
 * Pastilla del contador de faltas en la tarjeta de una clase del horario. Es clara con el texto del color del estado (en tono
 * oscuro): así se lee sobre cualquier color de clase, también sobre una clase verde o roja.
 */
@Composable
internal fun InsigniaFaltas(c: ContadorFaltas, @Suppress("UNUSED_PARAMETER") neutro: Color, modifier: Modifier = Modifier) {
    val texto = when (c.estado) {
        EstadoFaltas.Tranquilo -> Color(0xFF1B5E20)
        EstadoFaltas.Aviso -> Color(0xFFB45309)
        EstadoFaltas.Superado -> Color(0xFFC62828)
        EstadoFaltas.SinLimite -> Color(0xFF424242)
    }
    Surface(modifier, shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.92f), contentColor = texto) {
        Text(textoCortoFaltas(c, LocalContext.current.resources), Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** El chip «Faltas: N» de la hoja de la clase, con el color del estado. */
@Composable
internal fun ChipFaltas(c: ContadorFaltas?, elegido: Boolean, alPulsar: () -> Unit) {
    val estado = c?.estado ?: EstadoFaltas.SinLimite
    val color = colorFaltas(estado, MaterialTheme.colorScheme.onSurface)
    FilterChip(
        elegido, alPulsar, { Text(textoChipFaltas(c), fontWeight = if (estado == EstadoFaltas.SinLimite) null else FontWeight.SemiBold) },
        colors = if (estado == EstadoFaltas.SinLimite) FilterChipDefaults.filterChipColors()
        else FilterChipDefaults.filterChipColors(labelColor = color, selectedLabelColor = color),
    )
}

/** Sección de faltas de la hoja de una clase: contador, marcar (hoy u otro día) y la lista con «Justificada» y «Quitar». */
@Composable
internal fun SeccionFaltas(
    clase: Clase,
    clasesDelGrupo: List<Clase>,
    faltas: List<Falta>,
    contador: ContadorFaltas?,
    marcar: (LocalDate) -> Unit,
    quitar: (String) -> Unit,
    justificar: (String, Boolean) -> Unit,
    ponerMaximo: (Int?) -> Unit,
) {
    var otroDia by remember { mutableStateOf(false) }
    var sinClase by remember { mutableStateOf<LocalDate?>(null) }
    var editarMaximo by remember { mutableStateOf(false) }
    val c = contador ?: ReglasFaltas.contador(faltas, ReglasFaltas.maximo(clasesDelGrupo))
    val hoy = LocalDate.now()

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Contador grande.
        Surface(shape = RoundedCornerShape(14.dp), color = colorFaltas(c.estado, MaterialTheme.colorScheme.primary).copy(alpha = 0.14f)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (c.maximo != null) "${c.cuentan}/${c.maximo}" else "${c.cuentan}",
                    fontSize = 30.sp, fontWeight = FontWeight.Bold, color = colorFaltas(c.estado, MaterialTheme.colorScheme.onSurface),
                )
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(
                        when (c.estado) {
                            EstadoFaltas.SinLimite -> pluralStringResource(R.plurals.faltas_n, c.cuentan, c.cuentan)
                            EstadoFaltas.Superado -> stringResource(R.string.faltas_superado)
                            else -> (c.quedan ?: 0).let { pluralStringResource(R.plurals.faltas_te_quedan, it, it) }
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                    val justificadas = c.total - c.cuentan
                    val detalle = listOfNotNull(
                        if (c.maximo == null) stringResource(R.string.faltas_sin_limite) else stringResource(R.string.faltas_maximo_n, c.maximo),
                        if (justificadas > 0) pluralStringResource(R.plurals.faltas_justificadas, justificadas, justificadas) else null,
                    ).joinToString(" · ")
                    Text(detalle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { editarMaximo = true }) { Text(stringResource(if (c.maximo == null) R.string.faltas_poner_maximo else R.string.faltas_cambiar)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val hoyMarcada = faltas.any { it.dia == hoy.toEpochDay() }
            Button(
                onClick = { if (ReglasFaltas.tocaClase(hoy, clasesDelGrupo)) marcar(hoy) else sinClase = hoy },
                Modifier.weight(1f), enabled = !hoyMarcada,
            ) { Text(stringResource(if (hoyMarcada) R.string.faltas_hoy_marcada else R.string.faltas_marcar_hoy)) }
            OutlinedButton(onClick = { otroDia = true }) { Text(stringResource(R.string.faltas_otro_dia)) }
        }
        Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
            if (faltas.isEmpty()) Text(stringResource(R.string.faltas_ninguna, clase.titulo), Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            for (f in faltas) FilaFalta(f, quitar, justificar)
        }
    }

    if (otroDia) {
        SelectorDia(hoy, alCerrar = { otroDia = false }) { dia -> if (ReglasFaltas.tocaClase(dia, clasesDelGrupo)) marcar(dia) else sinClase = dia }
    }
    sinClase?.let { dia ->
        AlertDialog(
            onDismissRequest = { sinClase = null },
            title = { Text(stringResource(R.string.faltas_sin_clase_titulo)) },
            text = { Text(stringResource(R.string.faltas_sin_clase_texto, clase.titulo, Fechas.diaLargo(dia))) },
            confirmButton = { TextButton(onClick = { marcar(dia); sinClase = null }) { Text(stringResource(R.string.faltas_marcar)) } },
            dismissButton = { TextButton(onClick = { sinClase = null }) { Text(stringResource(R.string.cancelar)) } },
        )
    }
    if (editarMaximo) DialogoMaximoFaltas(c.maximo, alCerrar = { editarMaximo = false }, alGuardar = ponerMaximo)
}

@Composable
private fun FilaFalta(f: Falta, quitar: (String) -> Unit, justificar: (String, Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(Fechas.diaLargo(LocalDate.ofEpochDay(f.dia)))
            if (f.justificada) Text(stringResource(R.string.faltas_justificada_no_cuenta), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(stringResource(R.string.faltas_justificada), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Switch(f.justificada, { justificar(f.id, it) }, Modifier.padding(horizontal = 6.dp))
        IconButton(onClick = { quitar(f.id) }) { Icon(Icons.Filled.Close, stringResource(R.string.faltas_quitar)) }
    }
}

/** Pide el máximo de faltas de la materia; vacío = sin límite. */
@Composable
internal fun DialogoMaximoFaltas(actual: Int?, alCerrar: () -> Unit, alGuardar: (Int?) -> Unit) {
    var texto by remember { mutableStateOf(actual?.toString() ?: "") }
    val valor = texto.trim().toIntOrNull()
    val valido = texto.isBlank() || (valor != null && valor in 1..999)
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(R.string.faltas_maximo_titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.faltas_maximo_texto), color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    texto, { t -> texto = t.filter { it.isDigit() }.take(3) }, singleLine = true, isError = !valido,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(enabled = valido, onClick = { alGuardar(valor); alCerrar() }) { Text(stringResource(R.string.guardar)) } },
        dismissButton = { TextButton(onClick = alCerrar) { Text(stringResource(R.string.cancelar)) } },
    )
}

/** Franja de aviso arriba del horario cuando alguna materia ya llegó al máximo de faltas. */
@Composable
internal fun AvisoFaltas(enRiesgo: List<Clase>, alAbrir: (Clase) -> Unit) {
    if (enRiesgo.isEmpty()) return
    Surface(
        onClick = { alAbrir(enRiesgo.first()) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(stringResource(R.string.faltas_superado), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(enRiesgo.joinToString(", ") { it.titulo }, style = MaterialTheme.typography.bodySmall)
        }
    }
}
