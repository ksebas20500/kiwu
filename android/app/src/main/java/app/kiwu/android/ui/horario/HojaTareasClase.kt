package app.kiwu.android.ui.horario

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiwu.android.domain.Clase
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.ReglasHorario
import app.kiwu.android.domain.SeccionTarea
import app.kiwu.android.domain.TareasDeClase
import app.kiwu.android.ui.DialogoConfirmar
import app.kiwu.android.ui.Fechas
import app.kiwu.android.ui.fechaCorta
import app.kiwu.android.ui.tareas.CampoNuevaTarea
import java.time.LocalDate
import app.kiwu.android.R
import androidx.compose.ui.res.stringResource

/**
 * Hoja de tareas de una clase: se abre al tocarla en el horario. Muestra las tareas de la lista vinculada (pendientes y hechas),
 * deja añadir una y completarlas; editar la clase está en el menú de tres puntitos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HojaTareasClase(
    clase: Clase,
    estado: HorarioEstado,
    deberes: List<Deber>,
    alCerrar: () -> Unit,
    alEditar: () -> Unit,
    alEliminar: () -> Unit,
    alAbrirTarea: (String) -> Unit,
    alAbrirLista: (String) -> Unit,
    marcar: (String, Boolean) -> Unit,
    crearTarea: (titulo: String, materiaId: String, fecha: Long?) -> Unit,
    vincular: (materiaId: String?) -> Unit,
    crearYVincular: (nombre: String) -> Unit,
    marcarFalta: (LocalDate) -> Unit,
    quitarFalta: (String) -> Unit,
    justificarFalta: (String, Boolean) -> Unit,
    ponerMaximoFaltas: (Int?) -> Unit,
) {
    val color = ColoresClase.de(clase)
    val materia = estado.materias.firstOrNull { it.id == clase.materiaId }
    var menu by remember { mutableStateOf(false) }
    var elegirLista by remember { mutableStateOf(false) }
    var confirmarEliminar by remember { mutableStateOf(false) }
    // Qué se ve debajo de la cabecera: las tareas pendientes, las hechas o las faltas.
    var vista by remember { mutableStateOf(VistaHoja.Pendientes) }
    val contador = estado.contadores[clase.grupo]

    ModalBottomSheet(onDismissRequest = alCerrar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            // Cabecera: franja del color de la clase, datos y menú.
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(6.dp).height(46.dp).background(color.fondo, RoundedCornerShape(3.dp)))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(clase.titulo, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val detalle = listOfNotNull(
                        "${Fechas.nombreDia(clase.diaSemana)} ${ReglasHorario.rango(clase)}",
                        clase.aula.takeIf { it.isNotBlank() },
                        estado.nombres[clase.horarioId]?.takeIf { estado.horarios.size > 1 },
                    ).joinToString(" · ")
                    Text(detalle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.clase_opciones)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.clase_editar)) }, onClick = { menu = false; alEditar() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.clase_cambiar_lista)) }, onClick = { menu = false; elegirLista = true })
                        DropdownMenuItem(text = { Text(stringResource(R.string.clase_eliminar), color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; confirmarEliminar = true })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // Pestañas: Pendientes y Hechas (si hay lista) y siempre Faltas.
            val pendientes = materia?.let { TareasDeClase.pendientes(deberes, it.id) }.orEmpty()
            val hechas = materia?.let { TareasDeClase.hechas(deberes, it.id) }.orEmpty()
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (materia != null) {
                    FilterChip(vista == VistaHoja.Pendientes, { vista = VistaHoja.Pendientes }, { Text(stringResource(R.string.hoja_pendientes, pendientes.size)) })
                    FilterChip(vista == VistaHoja.Hechas, { vista = VistaHoja.Hechas }, { Text(stringResource(R.string.hoja_hechas, hechas.size)) })
                } else {
                    FilterChip(vista != VistaHoja.Faltas, { vista = VistaHoja.Pendientes }, { Text(stringResource(R.string.hoja_tareas)) })
                }
                ChipFaltas(contador, vista == VistaHoja.Faltas) { vista = VistaHoja.Faltas }
            }

            if (vista == VistaHoja.Faltas) {
                SeccionFaltas(
                    clase = clase, clasesDelGrupo = estado.todas.filter { it.grupo == clase.grupo }, faltas = estado.faltasDe(clase.grupo),
                    contador = contador, marcar = marcarFalta, quitar = quitarFalta, justificar = justificarFalta, ponerMaximo = ponerMaximoFaltas,
                )
            } else if (materia == null) {
                // Clase sin lista de tareas.
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.hoja_sin_lista), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.hoja_sin_lista_texto), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { crearYVincular(clase.titulo) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.hoja_crear_lista, clase.titulo)) }
                    if (estado.materias.isNotEmpty()) OutlinedButton(onClick = { elegirLista = true }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.hoja_vincular)) }
                }
            } else {
                Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    if (vista == VistaHoja.Pendientes) {
                        if (pendientes.isEmpty()) Text(stringResource(R.string.hoja_nada_pendiente), Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        for ((seccion, filas) in TareasDeClase.agrupadas(pendientes, estado.ahora)) {
                            Text(
                                stringResource(seccion.titulo), Modifier.padding(start = 8.dp, top = 10.dp, bottom = 2.dp), style = MaterialTheme.typography.labelMedium,
                                color = if (seccion == SeccionTarea.Vencidas) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                            for (d in filas) FilaTarea(d, estado.ahora, false, marcar, alAbrirTarea)
                        }
                    } else {
                        if (hechas.isEmpty()) Text(stringResource(R.string.hoja_nada_hecho), Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        for (d in hechas.sortedByDescending { it.fechaCompletado ?: 0L }) FilaTarea(d, estado.ahora, true, marcar, alAbrirTarea)
                    }
                }
                CampoNuevaTarea(
                    materias = estado.materias, fija = materia,
                    alCrear = { titulo, materiaId, fecha -> crearTarea(titulo, materiaId, fecha) },
                    marcador = stringResource(R.string.hoja_anadir_a, materia.nombre),
                )
                TextButton(onClick = { alAbrirLista(materia.id) }, Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.hoja_abrir_lista))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (elegirLista) {
        AlertDialog(
            onDismissRequest = { elegirLista = false },
            title = { Text(stringResource(R.string.hoja_lista_titulo)) },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    OpcionLista(stringResource(R.string.hoja_sin_lista_opcion), clase.materiaId == null) { vincular(null); elegirLista = false }
                    for (m in estado.materias) OpcionLista(m.nombre, clase.materiaId == m.id) { vincular(m.id); elegirLista = false }
                }
            },
            confirmButton = { TextButton(onClick = { elegirLista = false }) { Text(stringResource(R.string.cerrar)) } },
        )
    }
    if (confirmarEliminar) {
        DialogoConfirmar(
            stringResource(R.string.hoja_eliminar_titulo, clase.titulo), stringResource(R.string.hoja_eliminar_texto), stringResource(R.string.clase_eliminar),
            alCerrar = { confirmarEliminar = false },
        ) { alEliminar() }
    }
}

private enum class VistaHoja { Pendientes, Hechas, Faltas }

@Composable
private fun OpcionLista(texto: String, marcada: Boolean, alPulsar: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = alPulsar).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(marcada, onClick = alPulsar)
        Text(texto, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FilaTarea(d: Deber, ahora: Long, hecha: Boolean, marcar: (String, Boolean) -> Unit, alAbrir: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(hecha, onCheckedChange = { marcar(d.id, it) })
        Column(Modifier.weight(1f).clickable { alAbrir(d.id) }.padding(vertical = 6.dp)) {
            Text(
                d.titulo, maxLines = 2, overflow = TextOverflow.Ellipsis,
                textDecoration = if (hecha) TextDecoration.LineThrough else null,
                color = if (hecha) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            d.fechaEntrega?.let { f ->
                val vencida = !hecha && f < ahora
                Text(fechaCorta(f, ahora), fontSize = 12.sp, color = if (vencida) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
