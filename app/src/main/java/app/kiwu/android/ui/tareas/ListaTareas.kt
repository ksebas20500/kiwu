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
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.kiwu.android.R
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.Elemento
import app.kiwu.android.domain.esEvento
import app.kiwu.android.ui.EstadoVacio
import app.kiwu.android.ui.fechaCorta

private data class GrupoDiaTareas(
    val clave: String,
    val tituloMs: Long?,
    val tituloStr: String?,
    val items: List<Deber>,
)

/** Vista de lista: pendientes agrupados por día, y una sección plegable con las completadas. */
@Composable
fun ListaTareas(vm: TareasViewModel, estado: TareasEstado, alCrearLista: () -> Unit, alAbrir: (String) -> Unit) {
    val enCarpeta = estado.elemento is Elemento.DeGrupo
    var completadasVisibles by rememberSaveable { mutableStateOf(false) }
    val mostrarLista = estado.materiaActual == null
    val puedeCrear = estado.elemento != Elemento.Completadas
    val agruparPorDia = estado.elemento == Elemento.Proximos || estado.elemento == Elemento.Hoy

    val resHoy = stringResource(R.string.hoy)
    val resManana = stringResource(R.string.manana)
    val resVencidas = stringResource(R.string.vencidas)
    val resSinFecha = stringResource(R.string.seccion_sin_fecha)

    val gruposPorDia = remember(estado.pendientes, estado.ahora, agruparPorDia) {
        if (!agruparPorDia) emptyList()
        else {
            val mapa = LinkedHashMap<String, MutableList<Deber>>()
            val inicioHoy = vm.reglas.inicioDelDia(estado.ahora)
            val inicioManana = inicioHoy + 24 * 3600 * 1000L

            for (d in estado.pendientes) {
                val entrega = d.fechaEntrega
                val clave = when {
                    entrega == null -> "sin_fecha"
                    vm.reglas.estaVencido(d, estado.ahora) -> "vencidas"
                    vm.reglas.inicioDelDia(entrega) == inicioHoy -> "hoy"
                    vm.reglas.inicioDelDia(entrega) == inicioManana -> "manana"
                    else -> "fecha_" + vm.reglas.inicioDelDia(entrega)
                }
                mapa.getOrPut(clave) { mutableListOf() }.add(d)
            }

            val res = mutableListOf<GrupoDiaTareas>()
            mapa["vencidas"]?.let { res.add(GrupoDiaTareas("vencidas", null, resVencidas, it)) }
            mapa["hoy"]?.let { res.add(GrupoDiaTareas("hoy", null, resHoy, it)) }
            mapa["manana"]?.let { res.add(GrupoDiaTareas("manana", null, resManana, it)) }
            for ((k, v) in mapa) {
                if (k.startsWith("fecha_")) {
                    val ms = k.removePrefix("fecha_").toLongOrNull()
                    res.add(GrupoDiaTareas(k, ms, null, v))
                }
            }
            mapa["sin_fecha"]?.let { res.add(GrupoDiaTareas("sin_fecha", null, resSinFecha, it)) }
            res
        }
    }

    Column(Modifier.fillMaxSize()) {
        when {
            estado.cargado && estado.materiasDelContexto.isEmpty() && !estado.enEventos -> EstadoVacio(
                Icons.Filled.CheckCircle, stringResource(if (enCarpeta) R.string.carpeta_esta_vacia else R.string.crea_lista_empezar), Modifier.weight(1f),
            ) {
                Button(onClick = alCrearLista, modifier = Modifier.padding(top = 16.dp)) { Text(stringResource(if (enCarpeta) R.string.crear_lista_aqui else R.string.crear_lista)) }
            }
            estado.pendientes.isEmpty() && estado.completados.isEmpty() ->
                EstadoVacio(if (estado.enEventos) Icons.Filled.Event else Icons.Filled.CheckCircle, stringResource(if (estado.enEventos) R.string.sin_eventos else R.string.sin_tareas), Modifier.weight(1f))
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                if (agruparPorDia) {
                    for (g in gruposPorDia) {
                        item(key = "hdr-" + g.clave) {
                            val titulo = g.tituloStr ?: (g.tituloMs?.let { fechaCorta(it, estado.ahora, completo = true) } ?: g.clave)
                            Text(
                                titulo,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp)
                            )
                        }
                        items(g.items, key = { it.id }) { d ->
                            if (d.esEvento) {
                                TarjetaEventoAgenda(d, if (mostrarLista) estado.nombres[d.materiaId] else null, alAbrir = { alAbrir(d.id) })
                            } else {
                                FilaTarea(
                                    d, if (mostrarLista) estado.nombres[d.materiaId] else null, estado.ahora, vm.reglas,
                                    alAbrir = { alAbrir(d.id) }, alMarcar = { vm.marcar(d.id, it) },
                                    alEliminar = { vm.eliminarTarea(d.id) },
                                )
                            }
                        }
                    }
                } else {
                    items(estado.pendientes, key = { it.id }) { d ->
                        if (d.esEvento) {
                            TarjetaEventoAgenda(d, if (mostrarLista) estado.nombres[d.materiaId] else null, alAbrir = { alAbrir(d.id) })
                        } else {
                            FilaTarea(
                                d, if (mostrarLista) estado.nombres[d.materiaId] else null, estado.ahora, vm.reglas,
                                alAbrir = { alAbrir(d.id) }, alMarcar = { vm.marcar(d.id, it) },
                                alEliminar = { vm.eliminarTarea(d.id) },
                            )
                        }
                    }
                }

                if (estado.completados.isNotEmpty()) {
                    item(key = "cabecera-completadas") {
                        Row(
                            Modifier.fillMaxWidth().clickable { completadasVisibles = !completadasVisibles }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(if (completadasVisibles) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight, null)
                            Text(
                                stringResource(R.string.completadas),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 6.dp)
                            )
                            Text(
                                estado.completados.size.toString(), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                    if (completadasVisibles) {
                        items(estado.completados, key = { "c-" + it.id }) { d ->
                            FilaTarea(
                                d, if (mostrarLista) estado.nombres[d.materiaId] else null, estado.ahora, vm.reglas,
                                alAbrir = { alAbrir(d.id) }, alMarcar = { vm.marcar(d.id, it) },
                                alEliminar = { vm.eliminarTarea(d.id) },
                            )
                        }
                    }
                }
            }
        }
        if (puedeCrear) {
            val matActual = estado.materiaActual
            val marcadorTexto = if (matActual != null) {
                stringResource(R.string.anadir_tarea_a, matActual.nombre)
            } else {
                stringResource(if (estado.enEventos) R.string.anadir_evento else R.string.anadir_tarea)
            }
            CampoNuevaTarea(
                materias = estado.materiasDelContexto + if (enCarpeta) emptyList() else listOf(ListaEventos),
                fija = matActual,
                alCrear = { t, m, f -> vm.crearTarea(t, m, f) },
                marcador = marcadorTexto,
            )
        }
    }
}
