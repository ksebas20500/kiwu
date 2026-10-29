package app.kiwu.android.ui.estudio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.KiwuApp
import app.kiwu.android.R
import app.kiwu.android.domain.Calificacion
import app.kiwu.android.domain.Calificaciones
import app.kiwu.android.domain.Tarjeta
import app.kiwu.android.domain.Tarjetas
import app.kiwu.android.notify.Temporizador
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Pantalla(titulo: String, alVolver: () -> Unit, contenido: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = { IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
        content = contenido,
    )
}

/** Temporizador de estudio: 25, 50, 5 o 10 minutos; avisa al terminar aunque cierres la app. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemporizadorScreen(alVolver: () -> Unit) {
    val c = LocalContext.current
    var fin by remember { mutableStateOf(Temporizador.fin(c)) }
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(fin) {
        while (fin != null) {
            ahora = System.currentTimeMillis()
            if (ahora >= fin!!) fin = null
            delay(500)
        }
    }
    val restante = ((fin ?: 0L) - ahora).coerceAtLeast(0L) / 1000
    Pantalla(stringResource(R.string.temporizador), alVolver) { relleno ->
        Column(Modifier.padding(relleno).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                if (fin != null) "%02d:%02d".format(restante / 60, restante % 60) else "%02d:00".format(Temporizador.minutos(c)),
                fontSize = 72.sp, style = MaterialTheme.typography.displayLarge,
            )
            if (fin != null) {
                OutlinedButton(onClick = { Temporizador.cancelar(c); fin = null }) { Text(stringResource(R.string.temporizador_parar)) }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (m in listOf(25, 50, 5, 10)) {
                        Button(onClick = { Temporizador.iniciar(c, m); fin = Temporizador.fin(c) }) { Text(stringResource(R.string.duracion_min, m)) }
                    }
                }
                Text(stringResource(R.string.temporizador_ayuda), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Notas de una materia (lista) con su promedio ponderado. */
@Composable
fun CalificacionesScreen(app: KiwuApp, materiaId: String, alVolver: () -> Unit) {
    val materias by app.repo.materias.collectAsStateWithLifecycle(emptyList())
    val m = materias.firstOrNull { it.id == materiaId }
    val notas = Calificaciones.leer(m?.calificaciones.orEmpty())
    val alcance = rememberCoroutineScope()
    var nombre by remember { mutableStateOf("") }
    var valor by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }
    fun guardar(l: List<Calificacion>) { alcance.launch { app.repo.cambiarCalificaciones(materiaId, l) } }
    Pantalla(stringResource(R.string.calificaciones) + (m?.let { " · ${it.nombre}" } ?: ""), alVolver) { relleno ->
        Column(Modifier.padding(relleno).fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Calificaciones.promedio(notas)?.let {
                Text(stringResource(R.string.promedio_n, "%.2f".format(it)), style = MaterialTheme.typography.headlineSmall)
            } ?: Text(stringResource(R.string.sin_calificaciones), color = MaterialTheme.colorScheme.onSurfaceVariant)
            for ((i, n) in notas.withIndex()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(n.nombre, Modifier.weight(1f))
                    Text("%.2f  ×%s".format(n.nota, if (n.peso % 1.0 == 0.0) n.peso.toInt().toString() else n.peso.toString()))
                    IconButton(onClick = { guardar(notas.toMutableList().also { it.removeAt(i) }) }) { Icon(Icons.Filled.Close, stringResource(R.string.calificacion_quitar)) }
                }
            }
            OutlinedTextField(nombre, { nombre = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.nombre)) })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(valor, { valor = it }, Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.nota)) }, supportingText = { Text(stringResource(R.string.nota_ayuda)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(peso, { peso = it }, Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.peso)) }, supportingText = { Text(stringResource(R.string.peso_ayuda)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            val v = valor.replace(',', '.').toDoubleOrNull()
            val p = if (peso.isBlank()) 1.0 else peso.replace(',', '.').toDoubleOrNull()
            Button(
                onClick = { guardar(notas + Calificacion(nombre.trim(), v!!, p!!)); nombre = ""; valor = ""; peso = "" },
                enabled = nombre.isNotBlank() && v != null && p != null && p > 0,
            ) { Text(stringResource(R.string.anadir)) }
        }
    }
}

/** Tarjetas de repaso de una nota: las creas con una pregunta y su respuesta, y las repasas una a una. */
@Composable
fun TarjetasScreen(app: KiwuApp, paginaId: String, alVolver: () -> Unit) {
    val alcance = rememberCoroutineScope()
    var tarjetas by remember { mutableStateOf<List<Tarjeta>?>(null) }
    var cola by remember { mutableStateOf<List<Tarjeta>?>(null) } // null = no se está repasando
    var vista by remember { mutableStateOf(false) }
    var pregunta by remember { mutableStateOf("") }
    var respuesta by remember { mutableStateOf("") }
    LaunchedEffect(paginaId) { tarjetas = Tarjetas.leer(app.repo.pagina(paginaId)?.tarjetas.orEmpty()) }
    fun guardar(l: List<Tarjeta>) { tarjetas = l; alcance.launch { app.repo.cambiarTarjetas(paginaId, l) } }
    val lista = tarjetas.orEmpty()
    Pantalla(stringResource(R.string.tarjetas), { if (cola != null) cola = null else alVolver() }) { relleno ->
        val actual = cola?.firstOrNull()
        Column(Modifier.padding(relleno).fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                cola != null && actual == null -> {
                    Text(stringResource(R.string.tarjetas_terminado, lista.size), style = MaterialTheme.typography.titleLarge)
                    Button(onClick = { cola = null }) { Text(stringResource(R.string.tarjetas_volver)) }
                }
                cola != null && actual != null -> {
                    Text(stringResource(R.string.tarjetas_quedan, cola!!.size), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(actual.pregunta, style = MaterialTheme.typography.headlineSmall)
                    if (vista) {
                        Text(actual.respuesta, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { cola = cola!!.drop(1) + actual; vista = false }) { Text(stringResource(R.string.tarjeta_repasar)) }
                            Button(onClick = { cola = cola!!.drop(1); vista = false }) { Text(stringResource(R.string.tarjeta_sabida)) }
                        }
                    } else {
                        Button(onClick = { vista = true }) { Text(stringResource(R.string.tarjeta_ver)) }
                    }
                }
                else -> {
                    Text(stringResource(R.string.tarjetas_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { cola = lista; vista = false }, enabled = lista.isNotEmpty()) { Text(stringResource(R.string.tarjetas_empezar, lista.size)) }
                    for ((i, t) in lista.withIndex()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(t.pregunta, style = MaterialTheme.typography.bodyLarge)
                                Text(t.respuesta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { guardar(lista.toMutableList().also { it.removeAt(i) }) }) { Icon(Icons.Filled.Close, stringResource(R.string.tarjeta_quitar)) }
                        }
                    }
                    OutlinedTextField(pregunta, { pregunta = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.pregunta)) })
                    OutlinedTextField(respuesta, { respuesta = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.respuesta)) })
                    Button(
                        onClick = { guardar(lista + Tarjeta(pregunta.trim(), respuesta.trim())); pregunta = ""; respuesta = "" },
                        enabled = pregunta.isNotBlank() && respuesta.isNotBlank(),
                    ) { Text(stringResource(R.string.tarjeta_anadir)) }
                }
            }
        }
    }
}
