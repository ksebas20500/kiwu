package app.kiwu.android.ui.tareas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.kiwu.android.domain.Elemento
import app.kiwu.android.ui.EstadoVacio

/** Vista de lista: pendientes, y una sección plegable con las completadas. */
@Composable
fun ListaTareas(vm: TareasViewModel, estado: TareasEstado, alAbrir: (String) -> Unit, alCrearLista: () -> Unit) {
    var completadasVisibles by rememberSaveable { mutableStateOf(true) }
    val mostrarLista = estado.materiaActual == null
    val puedeCrear = estado.elemento != Elemento.Completadas

    Column(Modifier.fillMaxSize()) {
        when {
            estado.cargado && estado.materias.isEmpty() -> EstadoVacio(Icons.Filled.CheckCircle, "Crea una lista para empezar", Modifier.weight(1f)) {
                Button(onClick = alCrearLista, modifier = Modifier.padding(top = 16.dp)) { Text("Crear lista") }
            }
            estado.pendientes.isEmpty() && estado.completados.isEmpty() ->
                EstadoVacio(Icons.Filled.CheckCircle, "Sin tareas", Modifier.weight(1f))
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                items(estado.pendientes, key = { it.id }) { d ->
                    FilaTarea(
                        d, if (mostrarLista) estado.nombres[d.materiaId] else null, estado.ahora, vm.reglas,
                        alAbrir = { alAbrir(d.id) }, alMarcar = { vm.marcar(d.id, it) },
                        alMover = { vm.mover(d.id, it) }, alEliminar = { vm.eliminarTarea(d.id) },
                    )
                }
                if (estado.completados.isNotEmpty()) {
                    item(key = "cabecera-completadas") {
                        Row(
                            Modifier.fillMaxWidth().clickable { completadasVisibles = !completadasVisibles }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(if (completadasVisibles) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight, null)
                            Text("Completadas", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 6.dp))
                            Text(
                                estado.completados.size.toString(), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                    if (completadasVisibles) {
                        items(estado.completados, key = { "c-" + it.id }) { d ->
                            FilaTarea(
                                d, if (mostrarLista) estado.nombres[d.materiaId] else null, estado.ahora, vm.reglas,
                                alAbrir = { alAbrir(d.id) }, alMarcar = { vm.marcar(d.id, it) },
                                alMover = { vm.mover(d.id, it) }, alEliminar = { vm.eliminarTarea(d.id) },
                            )
                        }
                    }
                }
            }
        }
        if (puedeCrear) {
            CampoNuevaTarea(estado.materias, estado.materiaActual, alCrear = { t, m, f -> vm.crearTarea(t, m, f) })
        }
    }
}
