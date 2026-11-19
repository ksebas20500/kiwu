package app.kiwu.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.KiwuApp
import app.kiwu.android.R
import app.kiwu.android.data.AjustesUi
import app.kiwu.android.domain.Clase
import app.kiwu.android.domain.Cuaderno
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.ReglasHorario
import app.kiwu.android.ui.Fechas
import app.kiwu.android.ui.horario.HorarioEstado
import app.kiwu.android.ui.horario.HorarioViewModel
import app.kiwu.android.ui.horario.InsigniaFaltas
import app.kiwu.android.ui.notas.NotasEstado
import app.kiwu.android.ui.notas.NotasViewModel
import app.kiwu.android.ui.tareas.InsigniaRacha
import app.kiwu.android.ui.tareas.TareasEstado
import app.kiwu.android.ui.tareas.TareasViewModel
import coil.compose.AsyncImage
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private fun hexAColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    app: KiwuApp,
    tareasVm: TareasViewModel,
    notasVm: NotasViewModel,
    horarioVm: HorarioViewModel,
    alAbrirMenu: () -> Unit,
    alAbrirTarea: (String) -> Unit,
    alAbrirCuaderno: (String) -> Unit,
    alTareas: () -> Unit,
    alNotas: () -> Unit,
    alHorario: () -> Unit,
    alRacha: () -> Unit,
    alTemporizador: () -> Unit,
    alRepasoHoy: () -> Unit,
) {
    val ajustes by app.ajustes.flujo.collectAsStateWithLifecycle(AjustesUi())

    val estadoTareas by tareasVm.estado.collectAsStateWithLifecycle()
    val estadoNotas by notasVm.estado.collectAsStateWithLifecycle()
    val estadoHorario by horarioVm.estado.collectAsStateWithLifecycle()

    val horaActual = remember { LocalTime.now().hour }
    val saludo = when (horaActual) {
        in 5..11 -> stringResource(R.string.saludo_manana)
        in 12..18 -> stringResource(R.string.saludo_tarde)
        else -> stringResource(R.string.saludo_noche)
    }

    val hoyDate = remember { LocalDate.now() }
    val fechaFormateada = remember(hoyDate) {
        val diaSemana = Fechas.nombreDia(ReglasHorario.diaDe(hoyDate)).replaceFirstChar { it.uppercase() }
        "$diaSemana, ${hoyDate.dayOfMonth} ${Fechas.mes(hoyDate.withDayOfMonth(1)).take(3)}"
    }

    Box(Modifier.fillMaxSize()) {
        if (ajustes.fondoUri != null) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f)))
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(saludo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(fechaFormateada, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = alAbrirMenu) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.abrir_menu))
                        }
                    },
                    actions = {
                        InsigniaRacha(app = app, alClic = alRacha)
                        IconButton(onClick = alTemporizador) {
                            Icon(Icons.Filled.Timer, contentDescription = stringResource(R.string.temporizador))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) { relleno ->
            LazyColumn(
                modifier = Modifier.padding(relleno).consumeWindowInsets(relleno).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            ) {
                // Bloque 2: Siguiente Clase
                item {
                    BloqueSiguienteClase(estadoHorario, alHorario)
                }

                // Bloque 3: Para Hoy (Tareas + Banner de Repaso)
                item {
                    BloqueParaHoy(estadoTareas, estadoNotas, tareasVm, alTareas, alAbrirTarea, alRepasoHoy)
                }

                // Bloque 4: Cuadernos Recientes
                item {
                    BloqueCuadernosRecientes(estadoNotas, alNotas, alAbrirCuaderno)
                }
            }
        }
    }
}

/** Bloque 2: Siguiente clase (Conectado a Horario). */
@Composable
private fun BloqueSiguienteClase(estadoHorario: HorarioEstado, alHorario: () -> Unit) {
    val (diaActual, minutoActual) = remember(estadoHorario.ahora) { estadoHorario.diaYMinuto() }
    val claseEnCurso = remember(estadoHorario.clases, diaActual, minutoActual) {
        ReglasHorario.enCurso(estadoHorario.clases, diaActual, minutoActual)
    }
    val siguienteClaseInfo = remember(estadoHorario.clases, diaActual, minutoActual) {
        ReglasHorario.siguiente(estadoHorario.clases, diaActual, minutoActual)
    }

    val claseMostrar = claseEnCurso ?: siguienteClaseInfo?.first
    val esEnCurso = claseEnCurso != null

    if (claseMostrar != null) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
            modifier = Modifier.fillMaxWidth().clickable { alHorario() },
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier.size(10.dp).clip(CircleShape)
                                .background(if (esEnCurso) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            if (esEnCurso) "En curso ahora" else if (siguienteClaseInfo?.second == 0) stringResource(R.string.proxima_clase) else "Siguiente clase",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    val contador = estadoHorario.contadores[claseMostrar.grupo]
                    if (contador != null) {
                        InsigniaFaltas(c = contador, neutro = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text(
                    claseMostrar.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            ReglasHorario.rango(claseMostrar),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (claseMostrar.aula.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        ) {
                            Text(
                                "Aula ${claseMostrar.aula}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    } else {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
            modifier = Modifier.fillMaxWidth().clickable { alHorario() },
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Column {
                        Text(stringResource(R.string.sin_clases_hoy), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.configura_horario), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Bloque 3: Para hoy (Tareas + Repaso de Flashcards). */
@Composable
private fun BloqueParaHoy(
    estadoTareas: TareasEstado,
    estadoNotas: NotasEstado,
    tareasVm: TareasViewModel,
    alTareas: () -> Unit,
    alAbrirTarea: (String) -> Unit,
    alRepasoHoy: () -> Unit,
) {
    val tareasParaHoy = remember(estadoTareas.deberes, estadoTareas.ahora) {
        val hoy = LocalDate.now(ZoneId.systemDefault())
        estadoTareas.deberes.filter { d ->
            !d.completado && d.fechaEntrega != null && (
                Instant.ofEpochMilli(d.fechaEntrega).atZone(ZoneId.systemDefault()).toLocalDate() <= hoy
            )
        }.sortedBy { it.fechaEntrega ?: Long.MAX_VALUE }.take(3)
    }

    val flashcardsDueCount = remember(estadoNotas.todasLasPaginas) {
        estadoNotas.todasLasPaginas.size
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Encabezado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.para_hoy), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = alTareas) {
                    Text(stringResource(R.string.ver_todas_flecha), style = MaterialTheme.typography.labelMedium)
                }
            }

            // Subsección Tareas de hoy (hasta max 3)
            if (tareasParaHoy.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.todo_al_dia_hoy), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (d in tareasParaHoy) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { alAbrirTarea(d.id) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = d.completado,
                                onCheckedChange = { tareasVm.marcar(d.id, it) },
                            )
                            Text(
                                d.titulo,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(start = 4.dp),
                            )
                        }
                    }
                }
            }

            // Banner integrado "Repasar hoy" (Condicional)
            if (flashcardsDueCount > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth().clickable { alRepasoHoy() },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                Icons.Filled.Style,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                if (flashcardsDueCount == 1) "Tienes 1 tarjeta para repasar hoy" else stringResource(R.string.tarjetas_para_repasar_n, flashcardsDueCount),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Button(
                            onClick = alRepasoHoy,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text(stringResource(R.string.empezar), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

/** Bloque 4: Carrusel Cuadernos recientes (Conectado a Notas). */
@Composable
private fun BloqueCuadernosRecientes(
    estadoNotas: NotasEstado,
    alNotas: () -> Unit,
    alAbrirCuaderno: (String) -> Unit,
) {
    val cuadernosRecientes = remember(estadoNotas.cuadernos) {
        estadoNotas.cuadernos.sortedByDescending { it.actualizada }.take(4)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.tus_cuadernos), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = alNotas) {
                    Text(stringResource(R.string.ver_todos_flecha), style = MaterialTheme.typography.labelMedium)
                }
            }

            if (cuadernosRecientes.isEmpty()) {
                Text(
                    stringResource(R.string.sin_notas),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(cuadernosRecientes, key = { it.id }) { c ->
                        MiniNotebookCard(c) { alAbrirCuaderno(c.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniNotebookCard(cuaderno: Cuaderno, alPulsar: () -> Unit) {
    val shapeLibro = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 10.dp, bottomEnd = 10.dp)
    val colorRespaldo = hexAColor(cuaderno.coverColorHex) ?: MaterialTheme.colorScheme.primaryContainer
    val tieneImagen = cuaderno.coverImagePath != null && File(cuaderno.coverImagePath).exists()

    Column(
        modifier = Modifier.width(104.dp).clickable(onClick = alPulsar),
    ) {
        Surface(
            shape = shapeLibro,
            color = colorRespaldo,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.75f),
        ) {
            Box(Modifier.fillMaxSize()) {
                if (tieneImagen && cuaderno.coverImagePath != null) {
                    AsyncImage(
                        model = File(cuaderno.coverImagePath),
                        contentDescription = cuaderno.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(colorRespaldo, colorRespaldo.copy(alpha = 0.75f))
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(cuaderno.icono.ifBlank { "📓" }, fontSize = 28.sp)
                    }
                }

                // Lomo vertical
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.14f))
                        .align(Alignment.CenterStart)
                )
            }
        }
        Text(
            cuaderno.nombre,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
