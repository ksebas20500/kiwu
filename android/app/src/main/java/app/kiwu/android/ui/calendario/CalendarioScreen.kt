package app.kiwu.android.ui.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiwu.android.ui.Fechas
import app.kiwu.android.ui.Pasteles
import app.kiwu.android.ui.tareas.CampoNuevaTarea
import app.kiwu.android.ui.tareas.FilaTarea
import app.kiwu.android.ui.tareas.TareasEstado
import app.kiwu.android.ui.tareas.TareasViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val DiasSemana = listOf("L", "M", "X", "J", "V", "S", "D")
private const val MAX_PUNTOS = 4
private val ALTO_FILA = 54.dp

/** Primer día que se dibuja en la rejilla de un mes: el lunes de la semana del día 1. */
internal fun inicioDeRejilla(mes: LocalDate): LocalDate {
    val primero = mes.withDayOfMonth(1)
    return primero.minusDays((primero.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
}

/** Calendario mensual con las entregas en colores pastel y el detalle del día elegido. */
@Composable
fun CalendarioScreen(vm: TareasViewModel, estado: TareasEstado, alAbrir: (String) -> Unit) {
    val zona = ZoneId.systemDefault()
    val hoy = remember(estado.ahora) { Instant.ofEpochMilli(estado.ahora).atZone(zona).toLocalDate() }
    var mes by rememberSaveable { mutableStateOf(hoy.withDayOfMonth(1).toString()) }
    var seleccionado by rememberSaveable { mutableStateOf(hoy.toString()) }
    val primeroDelMes = LocalDate.parse(mes)
    val diaSel = LocalDate.parse(seleccionado)

    val porDia = remember(estado.deberes) {
        estado.deberes.filter { it.fechaEntrega != null }
            .groupBy { Instant.ofEpochMilli(it.fechaEntrega!!).atZone(zona).toLocalDate() }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(Fechas.mes(primeroDelMes), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(start = 8.dp))
            IconButton(onClick = { mes = primeroDelMes.minusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior") }
            TextButton(onClick = { mes = hoy.withDayOfMonth(1).toString(); seleccionado = hoy.toString() }) { Text("Hoy") }
            IconButton(onClick = { mes = primeroDelMes.plusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mes siguiente") }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            for (d in DiasSemana) {
                Text(d, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val inicio = inicioDeRejilla(primeroDelMes)
        val filas = (primeroDelMes.dayOfWeek.value - 1 + primeroDelMes.lengthOfMonth() + 6) / 7
        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            for (fila in 0 until filas) {
                Row(Modifier.height(ALTO_FILA).fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val dia = inicio.plusDays((fila * 7 + col).toLong())
                        CeldaDia(
                            dia = dia, delMes = dia.month == primeroDelMes.month, esHoy = dia == hoy, seleccionada = dia == diaSel,
                            tareas = porDia[dia].orEmpty().sortedBy { it.fechaEntrega },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            alElegir = {
                                seleccionado = dia.toString()
                                if (dia.month != primeroDelMes.month) mes = dia.withDayOfMonth(1).toString()
                            },
                        )
                    }
                }
            }
        }
        // --- día elegido ---
        val delDia = remember(estado.deberes, diaSel) { vm.reglas.delDia(estado.deberes, diaSel) }
        androidx.compose.material3.HorizontalDivider(Modifier.padding(top = 6.dp))
        Column(Modifier.fillMaxWidth().weight(1f)) {
            Text(
                Fechas.corta(diaSel.atStartOfDay(zona).toInstant().toEpochMilli(), estado.ahora, zona)
                    .replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            LazyColumn(Modifier.weight(1f)) {
                if (delDia.isEmpty()) {
                    item { Text("Sin entregas este día", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(delDia, key = { it.id }) { d ->
                    FilaTarea(
                        d, estado.nombres[d.materiaId], estado.ahora, vm.reglas,
                        alAbrir = { alAbrir(d.id) }, alMarcar = { vm.marcar(d.id, it) },
                        alMover = { vm.mover(d.id, it) }, alEliminar = { vm.eliminarTarea(d.id) },
                    )
                }
            }
            CampoNuevaTarea(estado.materias, null, alCrear = { t, m, f -> vm.crearTareaEnDia(t, m, diaSel, f) }, marcador = "Añadir tarea a este día", diaFijo = diaSel)
        }
    }
}

@Composable
private fun CeldaDia(
    dia: LocalDate,
    delMes: Boolean,
    esHoy: Boolean,
    seleccionada: Boolean,
    tareas: List<app.kiwu.android.domain.Deber>,
    modifier: Modifier,
    alElegir: () -> Unit,
) {
    val colores = remember(tareas.map { it.id }) { Pasteles.asignar(tareas.map { it.id }) }
    val contorno = if (seleccionada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    Column(
        modifier.padding(1.dp).clip(RoundedCornerShape(6.dp))
            .border(if (seleccionada) 2.dp else 0.5.dp, contorno, RoundedCornerShape(6.dp))
            .clickable(onClick = alElegir).padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).then(if (esHoy) Modifier.background(MaterialTheme.colorScheme.primary) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                dia.dayOfMonth.toString(), fontSize = 12.sp, fontWeight = if (esHoy) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    esHoy -> MaterialTheme.colorScheme.onPrimary
                    delMes -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                },
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 3.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            for (t in tareas.take(MAX_PUNTOS)) {
                val c = colores[t.id]
                Box(
                    Modifier.padding(horizontal = 1.5.dp).size(7.dp).clip(CircleShape)
                        .background(c?.texto ?: MaterialTheme.colorScheme.primary),
                )
            }
            if (tareas.size > MAX_PUNTOS) {
                Text("+", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 1.dp))
            }
        }
    }
}
