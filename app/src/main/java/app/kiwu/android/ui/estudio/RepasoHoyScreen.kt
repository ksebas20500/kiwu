package app.kiwu.android.ui.estudio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Style
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.R
import app.kiwu.android.domain.TarjetaDeNota

/**
 * «Repasar hoy»: todas las tarjetas vencidas de todas las notas en una sola cola. Las notas cuya materia tiene un
 * examen a 7, 3 o 1 días van primero, con su etiqueta.
 */
@Composable
fun RepasoHoyScreen(vm: RepasoHoyViewModel, alVolver: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var sesion by remember { mutableStateOf<List<TarjetaDeNota>?>(null) }
    var total by remember { mutableIntStateOf(0) }
    var vista by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.refrescar() }

    val volver = {
        if (sesion != null) {
            sesion = null
            vista = false
        } else {
            alVolver()
        }
    }
    Pantalla(stringResource(R.string.repaso_hoy), volver) { relleno ->
        Column(
            Modifier.padding(relleno).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val cola = sesion
            val actual = cola?.firstOrNull()
            when {
                cola == null -> Resumen(estado) {
                    sesion = estado.cola
                    total = estado.cola.size
                    vista = false
                }
                actual == null -> {
                    Text(stringResource(R.string.tarjetas_terminado, total), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.tarjetas_al_dia), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { sesion = null }) { Text(stringResource(R.string.tarjetas_volver)) }
                }
                else -> {
                    Text(stringResource(R.string.tarjetas_quedan, cola.size), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${actual.paginaTitulo.ifBlank { stringResource(R.string.sin_titulo) }} · ${actual.cuaderno}",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (actual.examen != null) {
                        androidx.compose.material3.Surface(
                            color = if ((actual.diasExamen ?: 99) <= 3) androidx.compose.ui.graphics.Color(0xFFE65100) else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                stringResource(R.string.repaso_examen_chip, actual.examen, actual.diasExamen ?: 0),
                                style = MaterialTheme.typography.labelMedium,
                                color = if ((actual.diasExamen ?: 99) <= 3) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(actual.tarjeta.pregunta, style = MaterialTheme.typography.headlineSmall)
                    if (vista) {
                        Text(actual.tarjeta.respuesta, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = {
                                vm.contestar(actual, sabida = false)
                                sesion = cola.drop(1) + actual
                                vista = false
                            }) { Text(stringResource(R.string.tarjeta_repasar)) }
                            Button(onClick = {
                                vm.contestar(actual, sabida = true)
                                sesion = cola.drop(1)
                                vista = false
                            }) { Text(stringResource(R.string.tarjeta_sabida)) }
                        }
                    } else {
                        Button(onClick = { vista = true }) { Text(stringResource(R.string.tarjeta_ver)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Resumen(estado: RepasoHoyEstado, alEmpezar: () -> Unit) {
    if (!estado.cargado) return
    Text(stringResource(R.string.repaso_hoy_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (estado.cola.isEmpty()) {
        Text(stringResource(R.string.tarjetas_al_dia), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Button(onClick = alEmpezar, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.tarjetas_hoy, estado.cola.size))
    }
    Text(
        pluralStringResource(R.plurals.repaso_hoy_notas, estado.notas, estado.notas),
        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    for (grupo in estado.cola.groupBy { it.paginaId }.values) {
        val primera = grupo.first()
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(primera.paginaTitulo.ifBlank { stringResource(R.string.sin_titulo) }, style = MaterialTheme.typography.bodyLarge)
                        Text(primera.cuaderno, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${grupo.size}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 4.dp))
                    androidx.compose.material3.Icon(Icons.Filled.Style, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 4.dp).size(16.dp))
                    androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (primera.examen != null) {
                    Surface(
                        color = if ((primera.diasExamen ?: 99) <= 3) androidx.compose.ui.graphics.Color(0xFFE65100).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            stringResource(R.string.repaso_examen_chip, primera.examen, primera.diasExamen ?: 0),
                            style = MaterialTheme.typography.labelSmall, 
                            color = if ((primera.diasExamen ?: 99) <= 3) androidx.compose.ui.graphics.Color(0xFFE65100) else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
