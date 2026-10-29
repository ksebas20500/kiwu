package app.kiwu.android.ui.tareas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.kiwu.android.data.Repositorio
import app.kiwu.android.domain.Columna
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.Documento
import app.kiwu.android.domain.Materia
import app.kiwu.android.ui.Fechas
import app.kiwu.android.ui.SelectorFechaHora
import app.kiwu.android.domain.Repeticion
import app.kiwu.android.domain.esEvento
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Repeat
import app.kiwu.android.ui.editor.BarraFormato
import app.kiwu.android.ui.editor.EditorBloques
import app.kiwu.android.ui.editor.rememberEstadoEditor
import app.kiwu.android.ui.editor.OrigenAdjuntos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import app.kiwu.android.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

class DetalleTareaViewModel(private val repo: Repositorio, private val alcanceApp: CoroutineScope, val id: String) : ViewModel() {
    val reglas get() = repo.reglas
    val deber = repo.deberes.map { lista -> lista.firstOrNull { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val materias: Flow<List<Materia>> = repo.materias

    private val notaPendiente = MutableStateFlow<String?>(null)

    @OptIn(FlowPreview::class)
    private val guardadoDeNota = viewModelScope.launch {
        notaPendiente.debounce(500).distinctUntilChanged().collect { json -> if (json != null) repo.cambiarNota(id, json) }
    }

    fun editarNota(doc: Documento) {
        notaPendiente.value = doc.aJson()
    }

    /** Al salir de la pantalla se guarda ya lo que quedara pendiente (sobrevive a la pantalla). */
    fun guardarAhora() {
        val json = notaPendiente.value ?: return
        alcanceApp.launch { repo.cambiarNota(id, json) }
    }

    fun cambiarTitulo(titulo: String) = viewModelScope.launch { repo.cambiarTitulo(id, titulo) }
    fun guardarTituloAhora(titulo: String) { alcanceApp.launch { repo.cambiarTitulo(id, titulo) } }
    fun marcar(c: Boolean) = viewModelScope.launch { repo.marcar(id, c) }
    fun mover(c: Columna) = viewModelScope.launch { repo.mover(id, c) }
    fun ponerFecha(f: Long?) = viewModelScope.launch { repo.ponerFecha(id, f) }
    fun cambiarRecordatorio(h: Int) = viewModelScope.launch { repo.cambiarRecordatorio(id, h) }
    fun cambiarRepeticion(m: Int) = viewModelScope.launch { repo.cambiarRepeticion(id, m) }
    fun cambiarExamen(e: Boolean) = viewModelScope.launch { repo.cambiarExamen(id, e) }
    fun cambiarPrioridad(p: Int) = viewModelScope.launch { repo.cambiarPrioridad(id, p) }
    fun cambiarEtiquetas(e: List<String>) = viewModelScope.launch { repo.cambiarEtiquetas(id, e) }
    fun cambiarPasos(p: List<app.kiwu.android.domain.Paso>) = viewModelScope.launch { repo.cambiarPasos(id, p) }
    fun moverAMateria(m: String) = viewModelScope.launch { repo.moverAMateria(id, m) }
    fun eliminar() = alcanceApp.launch { repo.eliminarDeber(id) }
}

private val AvisosDisponibles = listOf(0, 1, 2, 6, 12, 24, 48, 168)

/** «Sin aviso», «2 horas antes», «1 día antes», «1 semana antes». */
@Composable
private fun textoAvisoTarea(horas: Int): String = when {
    horas <= 0 -> stringResource(R.string.sin_aviso)
    horas % 168 == 0 -> pluralStringResource(R.plurals.aviso_semanas_antes, horas / 168, horas / 168)
    horas % 24 == 0 -> pluralStringResource(R.plurals.aviso_dias_antes, horas / 24, horas / 24)
    else -> pluralStringResource(R.plurals.aviso_horas_antes, horas, horas)
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DetalleTareaScreen(vm: DetalleTareaViewModel, adjuntos: OrigenAdjuntos, alVolver: () -> Unit) {
    val d by vm.deber.collectAsStateWithLifecycle()
    val materias by vm.materias.collectAsStateWithLifecycle(emptyList())
    var seleccionarFecha by remember { mutableStateOf(false) }
    var menuLista by remember { mutableStateOf(false) }
    var menuAviso by remember { mutableStateOf(false) }
    var menuRepite by remember { mutableStateOf(false) }
    var menuFecha by remember { mutableStateOf(false) }
    var seleccionarHora by remember { mutableStateOf(false) }
    val zona = ZoneId.systemDefault()

    val actual = d
    // Si la tarea desapareció (se eliminó), se vuelve atrás.
    var existio by remember { mutableStateOf(false) }
    LaunchedEffect(actual) {
        if (actual != null) existio = true else if (existio) alVolver()
    }
    if (actual == null) return

    var titulo by remember(actual.id) { mutableStateOf(actual.titulo) }
    LaunchedEffect(titulo) {
        kotlinx.coroutines.delay(500)
        vm.cambiarTitulo(titulo)
    }
    val tituloActual by androidx.compose.runtime.rememberUpdatedState(titulo)
    DisposableEffect(vm) {
        onDispose {
            vm.guardarAhora()
            vm.guardarTituloAhora(tituloActual)
        }
    }

    val documentoInicial = remember(actual.id) { Documento.deJson(actual.nota) }
    val editor = rememberEstadoEditor(documentoInicial, vm::editarNota)
    val columna = vm.reglas.columnaDe(actual)
    val ahora = System.currentTimeMillis()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
                actions = {
                    if (!actual.esEvento) Checkbox(checked = actual.completado, onCheckedChange = vm::marcar)
                    IconButton(onClick = { vm.eliminar(); alVolver() }) { Icon(Icons.Filled.Delete, stringResource(if (actual.esEvento) R.string.evento_eliminar else R.string.tarea_eliminar)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = { BarraFormato(editor, adjuntos) },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { relleno ->
        Column(Modifier.padding(relleno).consumeWindowInsets(relleno).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            TextField(
                value = titulo, onValueChange = { titulo = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.titulo), fontSize = 24.sp) },
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                ),
            )

            fun conDia(dia: LocalDate) {
                val base = actual.fechaEntrega?.let { java.time.Instant.ofEpochMilli(it).atZone(zona).toLocalTime() } ?: java.time.LocalTime.of(18, 0)
                vm.ponerFecha(dia.atTime(base).atZone(zona).toInstant().toEpochMilli())
            }
            fun conHora(h: Int, m: Int) {
                val dia = actual.fechaEntrega?.let { java.time.Instant.ofEpochMilli(it).atZone(zona).toLocalDate() } ?: LocalDate.now(zona)
                vm.ponerFecha(dia.atTime(h, m).atZone(zona).toInstant().toEpochMilli())
            }
            val neutro = coloresChipNeutro()

            // Fila 1 · cuándo: fecha y hora juntas en un solo chip, el aviso y la repetición.
            androidx.compose.foundation.layout.FlowRow(
                Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val entrega = actual.fechaEntrega
                Box {
                    // Las 00:00 significan «todo el día» (la lista de tareas tampoco muestra hora en ese caso).
                    val todoElDia = entrega?.let { java.time.Instant.ofEpochMilli(it).atZone(zona).toLocalTime() }?.let { it.hour == 0 && it.minute == 0 } ?: false
                    AssistChip(
                        onClick = { menuFecha = true }, colors = neutro,
                        label = {
                            Text(
                                if (entrega == null) stringResource(if (actual.esEvento) R.string.fecha_evento else R.string.fecha_entrega)
                                else Fechas.dia(entrega, zona) + " · " + if (todoElDia) stringResource(R.string.todo_el_dia) else Fechas.hora(entrega, zona),
                            )
                        },
                        leadingIcon = { Icon(Icons.Filled.CalendarMonth, null) },
                    )
                    DropdownMenu(expanded = menuFecha, onDismissRequest = { menuFecha = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.hoy)) }, onClick = { menuFecha = false; conDia(LocalDate.now(zona)) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.manana)) }, onClick = { menuFecha = false; conDia(LocalDate.now(zona).plusDays(1)) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.el_lunes)) }, onClick = { menuFecha = false; conDia(LocalDate.now(zona).with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY))) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.en_una_semana)) }, onClick = { menuFecha = false; conDia(LocalDate.now(zona).plusDays(7)) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.elegir_dia)) }, onClick = { menuFecha = false; seleccionarFecha = true })
                        if (entrega != null) {
                            androidx.compose.material3.HorizontalDivider()
                            DropdownMenuItem(text = { Text(stringResource(R.string.elegir_hora)) }, onClick = { menuFecha = false; seleccionarHora = true })
                            DropdownMenuItem(text = { Text(stringResource(R.string.todo_el_dia_sin_hora)) }, onClick = { menuFecha = false; conHora(0, 0) })
                            androidx.compose.material3.HorizontalDivider()
                            DropdownMenuItem(text = { Text(stringResource(R.string.quitar_fecha)) }, onClick = { menuFecha = false; vm.ponerFecha(null) })
                        }
                    }
                }
                if (entrega != null) Box {
                    AssistChip(
                        onClick = { menuAviso = true }, colors = neutro,
                        label = { Text(textoAvisoTarea(actual.recordatorioHoras)) },
                        leadingIcon = { Icon(Icons.Filled.Notifications, null) },
                    )
                    DropdownMenu(expanded = menuAviso, onDismissRequest = { menuAviso = false }) {
                        for (horas in AvisosDisponibles) {
                            DropdownMenuItem(text = { Text(textoAvisoTarea(horas)) }, onClick = { menuAviso = false; vm.cambiarRecordatorio(horas) })
                        }
                    }
                }
                if (!actual.esEvento) Box {
                    AssistChip(
                        onClick = { menuRepite = true }, colors = neutro,
                        label = { Text(if (actual.repite == 0) stringResource(R.string.repetir) else textoRepeticion(actual.repite)) },
                        leadingIcon = { Icon(Icons.Filled.Repeat, null) },
                    )
                    DropdownMenu(expanded = menuRepite, onDismissRequest = { menuRepite = false }) {
                        for (m in listOf(Repeticion.NINGUNA, Repeticion.TODOS_LOS_DIAS, Repeticion.LUNES_A_VIERNES, Repeticion.FINES_DE_SEMANA)) {
                            DropdownMenuItem(text = { Text(textoRepeticion(m)) }, onClick = { menuRepite = false; vm.cambiarRepeticion(m) })
                        }
                    }
                }
            }

            // Fila 2 · contexto: estado, lista, prioridad, examen y etiquetas (las categorías van con fondo suave, sin borde).
            if (!actual.esEvento) androidx.compose.foundation.layout.FlowRow(
                Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box {
                    var menuEstado by remember { mutableStateOf(false) }
                    AssistChip(
                        onClick = { menuEstado = true }, colors = neutro,
                        label = { Text(stringResource(columna.titulo)) },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
                    )
                    DropdownMenu(expanded = menuEstado, onDismissRequest = { menuEstado = false }) {
                        for (c in Columna.entries) DropdownMenuItem(text = { Text(stringResource(c.titulo)) }, onClick = { menuEstado = false; vm.mover(c) })
                    }
                }
                Box {
                    ChipCategoria(materias.firstOrNull { it.id == actual.materiaId }?.nombre ?: stringResource(R.string.lista), onClick = { menuLista = true })
                    DropdownMenu(expanded = menuLista, onDismissRequest = { menuLista = false }) {
                        for (m in materias) DropdownMenuItem(text = { Text(m.nombre) }, onClick = { menuLista = false; vm.moverAMateria(m.id) })
                    }
                }
                ChipsOrganizar(actual, vm::cambiarPrioridad, vm::cambiarEtiquetas, vm::cambiarExamen)
            }

            // Fila 3 · pasos
            if (!actual.esEvento) PasosTarea(actual, vm::cambiarPasos)

            EditorBloques(editor, adjuntos, Modifier.fillMaxWidth().padding(top = 20.dp))

            Text(
                stringResource(R.string.editado_n, Fechas.completa(actual.actualizada, zona)), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }

    if (seleccionarFecha) {
        val inicial = actual.fechaEntrega?.let { java.time.Instant.ofEpochMilli(it).atZone(zona).toLocalDate() } ?: LocalDate.now(zona)
        app.kiwu.android.ui.SelectorDia(inicial, alCerrar = { seleccionarFecha = false }) { dia ->
            val base = actual.fechaEntrega?.let { java.time.Instant.ofEpochMilli(it).atZone(zona).toLocalTime() } ?: java.time.LocalTime.of(18, 0)
            vm.ponerFecha(dia.atTime(base).atZone(zona).toInstant().toEpochMilli())
        }
    }
    if (seleccionarHora) {
        val h = actual.fechaEntrega?.let { java.time.Instant.ofEpochMilli(it).atZone(zona).toLocalTime() } ?: java.time.LocalTime.of(18, 0)
        app.kiwu.android.ui.SelectorHora(h.hour, h.minute, alCerrar = { seleccionarHora = false }) { hh, mm ->
            val dia = actual.fechaEntrega?.let { java.time.Instant.ofEpochMilli(it).atZone(zona).toLocalDate() } ?: LocalDate.now(zona)
            vm.ponerFecha(dia.atTime(hh, mm).atZone(zona).toInstant().toEpochMilli())
        }
    }
}

/** «No se repite», «Todos los días»… para el chip de repetición. */
@androidx.compose.runtime.Composable
fun textoRepeticion(mascara: Int): String = androidx.compose.ui.res.stringResource(
    when (mascara) {
        Repeticion.NINGUNA -> R.string.no_se_repite
        Repeticion.TODOS_LOS_DIAS -> R.string.todos_los_dias
        Repeticion.LUNES_A_VIERNES -> R.string.lunes_a_viernes
        Repeticion.FINES_DE_SEMANA -> R.string.fines_de_semana
        else -> R.string.se_repite
    },
)
