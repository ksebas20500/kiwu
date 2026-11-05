package app.kiwu.android.ui.estudio

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.KiwuApp
import app.kiwu.android.R
import app.kiwu.android.domain.Calificacion
import app.kiwu.android.domain.Calificaciones
import app.kiwu.android.domain.ConfigCalificaciones
import app.kiwu.android.domain.Documento
import app.kiwu.android.domain.GeneradorTarjetas
import app.kiwu.android.domain.Tarjeta
import app.kiwu.android.domain.Tarjetas
import app.kiwu.android.domain.TiempoEstudio
import app.kiwu.android.notify.Temporizador
import app.kiwu.android.ui.Acentos
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Pantalla(
    titulo: String,
    alVolver: () -> Unit,
    acciones: @Composable (RowScope.() -> Unit) = {},
    contenido: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.atras)) } },
                actions = acciones,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        content = contenido,
    )
}

/** Temporizador de estudio: 25, 50, 5 o 10 minutos; avisa al terminar aunque cierres la app. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemporizadorScreen(app: KiwuApp, alVolver: () -> Unit) {
    val c = LocalContext.current
    val materias by app.repo.materias.collectAsStateWithLifecycle(emptyList())
    var materia by remember { mutableStateOf(Temporizador.materia(c)) }
    var fin by remember { mutableStateOf(Temporizador.fin(c)) }
    var pausa by remember { mutableLongStateOf(Temporizador.pausaRestante(c)) }
    var noMolestar by remember { mutableStateOf(Temporizador.quiereNoMolestar(c)) }
    var colorNum by remember { mutableStateOf(Temporizador.color(c)) }
    val semana = remember(fin, pausa) { TiempoEstudio.semana(Temporizador.sesiones(c), java.time.LocalDate.now()) }
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(fin) {
        while (fin != null) {
            ahora = System.currentTimeMillis()
            if (Temporizador.fin(c) == null) fin = null
            else if (ahora >= fin!!) fin = null
            delay(500)
        }
    }
    val colorNumero = if (colorNum == "auto") null else Acentos.muestra(colorNum)
    Pantalla(stringResource(R.string.temporizador), alVolver) { relleno ->
        Column(
            Modifier.padding(relleno).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val quedanMs = if (fin != null) (fin ?: 0L) - ahora else pausa
            Text(
                if (fin != null || pausa > 0) "%02d:%02d".format(quedanMs / 60_000, quedanMs / 1000 % 60)
                else "%02d:00".format(Temporizador.minutos(c)),
                fontSize = 72.sp, style = MaterialTheme.typography.displayLarge,
                color = colorNumero ?: Color.Unspecified,
            )
            when {
                fin != null -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { Temporizador.pausar(c); fin = null; pausa = Temporizador.pausaRestante(c) }) { Text(stringResource(R.string.temporizador_pausar)) }
                    OutlinedButton(onClick = { Temporizador.cancelar(c); fin = null; pausa = 0 }) { Text(stringResource(R.string.temporizador_parar)) }
                }
                pausa > 0 -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { Temporizador.reanudar(c); fin = Temporizador.fin(c); pausa = 0 }) { Text(stringResource(R.string.temporizador_reanudar)) }
                    OutlinedButton(onClick = { Temporizador.cancelar(c); pausa = 0 }) { Text(stringResource(R.string.temporizador_parar)) }
                }
                else -> {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (m in listOf(25, 50, 5, 10)) {
                            Button(onClick = { Temporizador.iniciar(c, m); fin = Temporizador.fin(c) }) { Text(stringResource(R.string.duracion_min, m)) }
                        }
                        OutlinedButton(onClick = { Temporizador.iniciarDescanso(c, 5); fin = Temporizador.fin(c) }) { Text(stringResource(R.string.temporizador_descanso)) }
                    }
                    Text(stringResource(R.string.temporizador_ayuda), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    if (materias.isNotEmpty()) {
                        Text(stringResource(R.string.temporizador_lista), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall, modifier = Modifier.fillMaxWidth())
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (m in materias) {
                                FilterChip(
                                    selected = m.id == materia,
                                    onClick = { materia = if (m.id == materia) null else m.id; Temporizador.elegirMateria(c, materia) },
                                    label = { Text(listOfNotNull(m.emoji, m.nombre).joinToString(" ")) },
                                )
                            }
                        }
                    }
                    Text(stringResource(R.string.temporizador_color), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall, modifier = Modifier.fillMaxWidth())
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = colorNum == "auto",
                            onClick = { colorNum = "auto"; Temporizador.elegirColor(c, "auto") },
                            label = { Text(stringResource(R.string.temporizador_auto)) },
                        )
                        for (a in Acentos.predefinidos) {
                            FilterChip(
                                selected = colorNum == a.id,
                                onClick = { colorNum = a.id; Temporizador.elegirColor(c, a.id) },
                                label = { Box(Modifier.padding(4.dp).size(18.dp).clip(CircleShape).background(a.muestra)) },
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.temporizador_no_molestar), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.temporizador_no_molestar_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = noMolestar,
                            onCheckedChange = { quiero ->
                                if (quiero && !Temporizador.puedeNoMolestar(c)) c.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                                else {
                                    noMolestar = quiero
                                    Temporizador.elegirNoMolestar(c, quiero)
                                }
                            },
                        )
                    }
                }
            }
            if (semana.isNotEmpty()) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.estudio_semana), style = MaterialTheme.typography.titleSmall)
                    for ((id, min) in semana) {
                        Row {
                            Text(materias.firstOrNull { it.id == id }?.nombre ?: stringResource(R.string.estudio_sin_lista), Modifier.weight(1f))
                            Text(if (min >= 60) stringResource(R.string.duracion_h_min, min / 60, min % 60) else stringResource(R.string.duracion_min, min))
                        }
                    }
                }
            }
        }
    }
}

/** Notas de una materia (lista) con su nota acumulada, promedio de lo evaluado, nota mínima para aprobar y desglose. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalificacionesScreen(app: KiwuApp, materiaId: String, alVolver: () -> Unit) {
    val materias by app.repo.materias.collectAsStateWithLifecycle(emptyList())
    val m = materias.firstOrNull { it.id == materiaId }
    val alcance = rememberCoroutineScope()

    var config by remember { mutableStateOf<ConfigCalificaciones?>(null) }
    LaunchedEffect(m?.calificaciones) {
        if (m != null && config == null) {
            config = Calificaciones.leerConfig(m.calificaciones)
        }
    }

    val cfg = config ?: remember(m?.calificaciones) { Calificaciones.leerConfig(m?.calificaciones.orEmpty()) }
    var notas by remember(cfg.notas) { mutableStateOf(cfg.notas) }
    var minima by remember(cfg.minimaAprobacion) { mutableStateOf(cfg.minimaAprobacion) }

    fun guardar(nuevasNotas: List<Calificacion> = notas, nuevaMinima: Double = minima) {
        notas = nuevasNotas
        minima = nuevaMinima
        val nCfg = ConfigCalificaciones(nuevaMinima, nuevasNotas)
        config = nCfg
        alcance.launch {
            app.repo.cambiarCalificacionesConfig(materiaId, nCfg)
        }
    }

    var mostrarModalNota by remember { mutableStateOf(false) }
    var notaAEditarIndex by remember { mutableStateOf<Int?>(null) }
    var mostrarModalAjustes by remember { mutableStateOf(false) }

    val sumaPesos = remember(notas) { Calificaciones.sumaPesos(notas) }
    val acumulado = remember(notas) { Calificaciones.puntosAcumulados(notas) }
    val promedioEvaluado = remember(notas) { Calificaciones.promedioAcumulado(notas) }
    val notaNecesaria = remember(notas, minima) { Calificaciones.necesariaParaAprobar(notas, minima) }
    val pesoRestante = (100.0 - sumaPesos).coerceAtLeast(0.0)

    val tagParcial = stringResource(R.string.tag_parcial)
    val tagQuizzesTalleres = stringResource(R.string.tag_quizzes_talleres)

    Pantalla(
        titulo = stringResource(R.string.calificaciones) + (m?.let { " · ${it.nombre}" } ?: ""),
        alVolver = alVolver,
        acciones = {
            IconButton(onClick = { mostrarModalAjustes = true }) {
                Icon(Icons.Filled.Settings, stringResource(R.string.configuracion_calificaciones))
            }
        }
    ) { relleno ->
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        notaAEditarIndex = null
                        mostrarModalNota = true
                    },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text(stringResource(R.string.agregar_nota)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        ) { paddingInterno ->
            Column(
                Modifier.padding(relleno).padding(paddingInterno).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Card: Dashboard de Estado y Proyección
                OutlinedCard(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Métricas Principales en dos columnas
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(
                                    "%.2f".format(acumulado),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    stringResource(R.string.aporte_nota_final),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    promedioEvaluado?.let { "%.2f".format(it) } ?: "0.00",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    stringResource(R.string.promedio_calificado),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Barra de Progreso Segmentada
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    stringResource(R.string.evaluado_formato, sumaPesos),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    stringResource(R.string.restante_formato, pesoRestante),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (sumaPesos / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                        }

                        // Banner de Predicción / Meta
                        val (bannerColor, textColor) = when {
                            acumulado >= minima -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                            notaNecesaria != null && notaNecesaria > 5.0 -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = bannerColor,
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                when {
                                    acumulado >= minima -> {
                                        Text(
                                            stringResource(R.string.materia_aprobada_garantizada, minima),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                    }
                                    notaNecesaria != null && notaNecesaria > 5.0 -> {
                                        Text(
                                            stringResource(R.string.meta_inalcanzable, minima),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                    }
                                    notaNecesaria != null -> {
                                        Text(
                                            stringResource(R.string.necesitaras_n_para_aprobar, notaNecesaria, pesoRestante, minima),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor
                                        )
                                        if (pesoRestante > 0.0) {
                                            Text(
                                                stringResource(R.string.desglose_ejemplo, pesoRestante, notaNecesaria),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = textColor.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Lista de Calificaciones Registradas
                Text(
                    stringResource(R.string.calificaciones_registradas, notas.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )

                if (notas.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.School, null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            stringResource(R.string.sin_notas_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((i, n) in notas.withIndex()) {
                            OutlinedCard(
                                onClick = {
                                    notaAEditarIndex = i
                                    mostrarModalNota = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(n.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("%.2f".format(n.nota), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                "%.0f%%".format(n.peso),
                                                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        IconButton(onClick = {
                                            val list = notas.toMutableList().also { it.removeAt(i) }
                                            guardar(nuevasNotas = list)
                                        }) {
                                            Icon(Icons.Filled.Close, stringResource(R.string.calificacion_quitar))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet para Nueva / Editar Nota
    if (mostrarModalNota) {
        val notaExistente = notaAEditarIndex?.let { notas.getOrNull(it) }
        ModalNotaSheet(
            notaInicial = notaExistente,
            tagParcial = tagParcial,
            tagQuizzesTalleres = tagQuizzesTalleres,
            alCerrar = { mostrarModalNota = false },
            alGuardar = { nombreIngresado, notaIngresada, pesoIngresado ->
                val list = notas.toMutableList()
                val pesoFinal = pesoIngresado ?: (if (list.isEmpty()) 20.0 else (100.0 - sumaPesos).coerceAtLeast(10.0))
                val nuevaNota = Calificacion(nombreIngresado, notaIngresada, pesoFinal)
                if (notaAEditarIndex != null && notaAEditarIndex!! in list.indices) {
                    list[notaAEditarIndex!!] = nuevaNota
                } else {
                    list.add(nuevaNota)
                }
                guardar(nuevasNotas = list)
                mostrarModalNota = false
            }
        )
    }

    // Modal de Configuración / Ajustes
    if (mostrarModalAjustes) {
        ModalAjustesDialog(
            minimaInicial = minima,
            alCerrar = { mostrarModalAjustes = false },
            alGuardar = { nuevaMin ->
                guardar(nuevaMinima = nuevaMin)
                mostrarModalAjustes = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalNotaSheet(
    notaInicial: Calificacion?,
    tagParcial: String,
    tagQuizzesTalleres: String,
    alCerrar: () -> Unit,
    alGuardar: (String, Double, Double?) -> Unit
) {
    var nombre by remember { mutableStateOf(notaInicial?.nombre ?: "") }
    var notaStr by remember { mutableStateOf(notaInicial?.nota?.toString() ?: "") }
    var pesoStr by remember { mutableStateOf(notaInicial?.peso?.toString() ?: "") }

    ModalBottomSheet(
        onDismissRequest = alCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 12.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                if (notaInicial != null) stringResource(R.string.editar_calificacion) else stringResource(R.string.nueva_calificacion),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Chips de sugerencia rápida
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (tag in listOf(tagParcial, tagQuizzesTalleres)) {
                    FilterChip(
                        selected = nombre == tag,
                        onClick = { nombre = tag },
                        label = { Text(tag) }
                    )
                }
            }

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.nombre)) },
                placeholder = { Text("Ej. Parcial 1") }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = notaStr,
                    onValueChange = { notaStr = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.nota)) },
                    placeholder = { Text("0.0 a 5.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = pesoStr,
                    onValueChange = { pesoStr = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.peso)) },
                    trailingIcon = { Text("%", Modifier.padding(end = 12.dp), fontWeight = FontWeight.Bold) },
                    supportingText = { Text(stringResource(R.string.dejar_vacio_peso)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            val v = notaStr.replace(',', '.').toDoubleOrNull()
            val p = pesoStr.replace(',', '.').toDoubleOrNull()

            Button(
                onClick = {
                    val nNombre = nombre.trim().ifBlank { tagParcial }
                    alGuardar(nNombre, v!!, p)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = v != null && v in 0.0..5.0
            ) {
                Text(stringResource(R.string.guardar_calificacion))
            }
        }
    }
}

@Composable
private fun ModalAjustesDialog(
    minimaInicial: Double,
    alCerrar: () -> Unit,
    alGuardar: (Double) -> Unit
) {
    var minStr by remember { mutableStateOf(minimaInicial.toString()) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(R.string.configuracion_calificaciones), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = minStr,
                    onValueChange = { minStr = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.nota_minima_aprobar)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val d = minStr.replace(',', '.').toDoubleOrNull()
                    if (d != null && d > 0.0) alGuardar(d)
                }
            ) {
                Text(stringResource(R.string.ajustes_guardar))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = alCerrar) {
                Text(stringResource(R.string.cancelar))
            }
        }
    )
}

/**
 * Tarjetas de repaso de una nota: las escritas como bloque «Tarjeta» en el apunte más las guardadas aquí (a mano o
 * generadas desde el apunte), y el repaso una a una.
 */
@Composable
fun TarjetasScreen(app: KiwuApp, paginaId: String, alVolver: () -> Unit) {
    val alcance = rememberCoroutineScope()
    var tarjetas by remember { mutableStateOf<List<Tarjeta>?>(null) }
    var apunte by remember { mutableStateOf<Documento?>(null) }
    var cola by remember { mutableStateOf<List<Tarjeta>?>(null) }
    var propuestas by remember { mutableStateOf<List<Tarjeta>?>(null) }
    var elegidas by remember { mutableStateOf(emptySet<Int>()) }
    var vista by remember { mutableStateOf(false) }
    var pregunta by remember { mutableStateOf("") }
    var respuesta by remember { mutableStateOf("") }
    LaunchedEffect(paginaId) {
        val p = app.repo.pagina(paginaId)
        tarjetas = Tarjetas.leer(p?.tarjetas.orEmpty())
        apunte = Documento.deJson(p?.contenido)
    }
    fun guardar(l: List<Tarjeta>) { tarjetas = l; alcance.launch { app.repo.cambiarTarjetas(paginaId, l) } }
    val lista = tarjetas.orEmpty()
    val delApunte = remember(apunte) { apunte?.let(GeneradorTarjetas::delApunte).orEmpty() }
    val todas = delApunte + lista
    Pantalla(stringResource(R.string.tarjetas), { if (cola != null) cola = null else if (propuestas != null) propuestas = null else alVolver() }) { relleno ->
        val actual = cola?.firstOrNull()
        Column(
            Modifier.padding(relleno).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                propuestas != null -> {
                    val ps = propuestas!!
                    if (ps.isEmpty()) {
                        Text(stringResource(R.string.tarjetas_generar_nada), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { propuestas = null }) { Text(stringResource(R.string.tarjetas_volver)) }
                    } else {
                        Text(androidx.compose.ui.res.pluralStringResource(R.plurals.tarjetas_propuestas, ps.size, ps.size),style = MaterialTheme.typography.titleMedium)
                        for ((i, t) in ps.withIndex()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = i in elegidas, onCheckedChange = { elegidas = if (it) elegidas + i else elegidas - i })
                                Column(Modifier.weight(1f)) {
                                    Text(t.pregunta, style = MaterialTheme.typography.bodyLarge)
                                    Text(t.respuesta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { propuestas = null }) { Text(stringResource(R.string.cancelar)) }
                            Button(
                                onClick = { guardar(lista + ps.filterIndexed { i, _ -> i in elegidas }); propuestas = null },
                                enabled = elegidas.isNotEmpty(),
                            ) { Text(stringResource(R.string.tarjetas_anadir_n, elegidas.size)) }
                        }
                    }
                }
                cola != null && actual == null -> {
                    Text(stringResource(R.string.tarjetas_terminado, todas.size), style = MaterialTheme.typography.titleLarge)
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
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { cola = todas; vista = false }, enabled = todas.isNotEmpty()) { Text(stringResource(R.string.tarjetas_empezar, todas.size)) }
                        OutlinedButton(
                            onClick = {
                                val ps = GeneradorTarjetas.proponer(apunte ?: Documento.vacio(), lista)
                                propuestas = ps
                                elegidas = ps.indices.toSet()
                            },
                            enabled = apunte != null,
                        ) { Text(stringResource(R.string.tarjetas_generar)) }
                    }
                    for (t in delApunte) {
                        Column {
                            Text(stringResource(R.string.tarjeta_en_apunte), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(t.pregunta, style = MaterialTheme.typography.bodyLarge)
                            Text(t.respuesta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
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
