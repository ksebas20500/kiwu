package app.kiwu.android.widgets

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kiwu.android.R
import app.kiwu.android.domain.Materia
import app.kiwu.android.domain.ID_EVENTOS
import app.kiwu.android.domain.Prioridad
import app.kiwu.android.ui.tareas.colorPrioridad
import app.kiwu.android.ui.tareas.textoPrioridad
import app.kiwu.android.ui.Fechas
import app.kiwu.android.ui.SelectorFechaHora
import app.kiwu.android.ui.SelectorHora
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
internal fun CapturaTareaWidget(
    listas: List<Materia>,
    ubicaciones: Map<String, UbicacionLista>,
    tituloFormulario: String,
    alCerrar: () -> Unit,
    guardar: suspend (String, String, Long?, Int, String) -> Unit,
    fechaInicial: Long? = null,
    diaFijo: LocalDate? = null,
    cerrarSolicitado: Int = 0,
    cerrarDirectamente: Boolean = false,
) {
    var texto by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var prioridad by rememberSaveable { mutableStateOf(Prioridad.NINGUNA) }
    var menuPrioridad by remember { mutableStateOf(false) }
    var destino by rememberSaveable { mutableStateOf<String?>(null) }
    var fecha by rememberSaveable { mutableStateOf(fechaInicial) }
    var confirmar by rememberSaveable { mutableStateOf(false) }
    var solicitudAtendida by rememberSaveable { mutableStateOf(cerrarSolicitado) }
    var mostrandoListas by rememberSaveable { mutableStateOf(false) }
    var enfocado by rememberSaveable { mutableStateOf(false) }
    var selector by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf(false) }

    val lista = if (destino == null) listas.firstOrNull() else listas.firstOrNull { it.id == destino }
    val teclado = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val foco = remember { FocusRequester() }
    val alcance = rememberCoroutineScope()
    val zona = ZoneId.systemDefault()

    fun solicitarCerrar() {
        if (guardando) return
        teclado?.hide()
        if (!cerrarDirectamente && (texto.isNotBlank() || descripcion.isNotBlank())) confirmar = true else alCerrar()
    }

    BackHandler(enabled = mostrandoListas || cerrarDirectamente || !imeVisible || guardando) {
        if (mostrandoListas) mostrandoListas = false else solicitarCerrar()
    }
    LaunchedEffect(cerrarSolicitado) {
        if (solicitudAtendida != cerrarSolicitado) {
            solicitudAtendida = cerrarSolicitado
            solicitarCerrar()
        }
    }
    val hayListas = listas.isNotEmpty()

    val etiquetaFecha = fecha?.let {
        val hora = Fechas.hora(it, zona)
        if (diaFijo != null) hora
        else "${Fechas.diaLargo(Instant.ofEpochMilli(it).atZone(zona).toLocalDate())} · $hora"
    }

    AnimatedContent(targetState = mostrandoListas, label = "selector de listas") { elegirLista ->
        if (elegirLista) {
            SelectorListaCaptura(
                listas, ubicaciones, lista?.id,
                alVolver = { mostrandoListas = false },
                alElegir = {
                    destino = it
                    mostrandoListas = false
                },
            )
        } else {
            LaunchedEffect(hayListas) {
                if (hayListas && !confirmar && !enfocado && !mostrandoListas) {
                    foco.requestFocus()
                    enfocado = true
                }
            }
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp)) {
                if (diaFijo != null) {
                    Text(
                        Fechas.diaLargo(diaFijo),
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val coloresCampo = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                )
                TextField(
                    value = texto,
                    onValueChange = { texto = it },
                    placeholder = { Text(if (hayListas) tituloFormulario else stringResource(R.string.crea_lista_primero)) },
                    textStyle = MaterialTheme.typography.titleMedium,
                    enabled = hayListas && !guardando,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(foco),
                    colors = coloresCampo,
                )
                TextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    placeholder = { Text(stringResource(R.string.w_descripcion_tarea)) },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    enabled = hayListas && !guardando,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    colors = coloresCampo,
                )

                if (etiquetaFecha != null) {
                    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(etiquetaFecha, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (fechaInicial == null) {
                            IconButton(onClick = { fecha = null }, enabled = !guardando) {
                                Icon(Icons.Filled.Close, stringResource(R.string.quitar_fecha), Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        enabled = !guardando,
                        onClick = {
                            teclado?.hide()
                            selector = true
                        },
                    ) {
                        Icon(
                            Icons.Filled.Schedule,
                            stringResource(if (diaFijo == null) R.string.elegir_fecha_entrega else R.string.elegir_hora_entrega),
                            tint = if (fecha != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (lista?.id != ID_EVENTOS) {
                        Box {
                            IconButton(enabled = hayListas && !guardando, onClick = {
                                teclado?.hide()
                                menuPrioridad = true
                            }) {
                                Icon(Icons.Filled.Flag, textoPrioridad(prioridad), tint = colorPrioridad(prioridad))
                            }
                            DropdownMenu(expanded = menuPrioridad, onDismissRequest = { menuPrioridad = false }) {
                                for (opcion in Prioridad.NINGUNA..Prioridad.ALTA) {
                                    DropdownMenuItem(text = { Text(textoPrioridad(opcion)) }, onClick = {
                                        prioridad = opcion
                                        menuPrioridad = false
                                    })
                                }
                            }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        val etiqueta = lista?.let { ubicaciones[it.id]?.etiqueta ?: it.nombre }
                        if (listas.size == 1 && etiqueta != null) {
                            Text(etiqueta, Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        } else if (hayListas) {
                            TextButton(
                                enabled = !guardando,
                                onClick = {
                                    teclado?.hide()
                                    mostrandoListas = true
                                },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(etiqueta ?: stringResource(R.string.lista), Modifier.weight(1f, fill = false),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Icon(Icons.Filled.ArrowDropDown, null)
                            }
                        }

                    }
                    IconButton(
                        enabled = !guardando && texto.isNotBlank() && lista != null,
                        onClick = {
                            val id = lista?.id ?: return@IconButton
                            guardando = true
                            error = false
                            teclado?.hide()
                            alcance.launch {
                                try {
                                    guardar(texto.trim(), id, fecha, prioridad, descripcion)
                                    alCerrar()
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    error = true
                                } finally {
                                    guardando = false
                                }
                            }
                        },
                    ) {
                        if (guardando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.guardar))
                    }
                }
                if (guardando || error) {
                    Text(
                        stringResource(if (error) R.string.w_error_captura else R.string.w_guardando_captura),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    if (selector) {
        if (diaFijo == null) {
            SelectorFechaHora(
                fecha,
                alCerrar = { selector = false },
                alElegir = { fecha = it },
            )
        } else {
            val hora = fecha?.let { Instant.ofEpochMilli(it).atZone(zona).toLocalTime() }
            SelectorHora(hora?.hour ?: 18, hora?.minute ?: 0, alCerrar = { selector = false }) { h, m ->
                fecha = diaFijo.atTime(h, m).atZone(zona).toInstant().toEpochMilli()
            }
        }
    }
    if (confirmar) AlertDialog(
        onDismissRequest = { confirmar = false },
        title = { Text(stringResource(R.string.w_descartar_captura)) },
        confirmButton = {
            TextButton(onClick = alCerrar) { Text(stringResource(R.string.w_descartar)) }
        },
        dismissButton = {
            TextButton(onClick = { confirmar = false }) { Text(stringResource(R.string.cancelar)) }
        },
    )
}

@Composable
private fun SelectorListaCaptura(
    listas: List<Materia>,
    ubicaciones: Map<String, UbicacionLista>,
    seleccion: String?,
    alVolver: () -> Unit,
    alElegir: (String) -> Unit,
) {
    val grupos = remember(listas, ubicaciones) {
        listas.groupBy { lista -> lista.grupoId?.takeIf { ubicaciones[lista.id]?.carpeta != null } }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = alVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.atras))
            }
            Text(stringResource(R.string.w_elegir_lista), style = MaterialTheme.typography.titleMedium)
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
            grupos.forEach { (grupo, susListas) ->
                item(key = "carpeta:${grupo.orEmpty()}") {
                    Text(
                        ubicaciones[susListas.first().id]?.carpeta ?: stringResource(R.string.sin_carpeta),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(susListas, key = { "lista:${it.id}" }) { opcion ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 44.dp)
                            .clickable { alElegir(opcion.id) }.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(opcion.emoji ?: "📋", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            opcion.nombre, Modifier.weight(1f).padding(horizontal = 12.dp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        if (opcion.id == seleccion) {
                            Icon(Icons.Rounded.Check, stringResource(R.string.w_lista_seleccionada),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

internal fun entregaWidget(dia: LocalDate): Long =
    dia.atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
