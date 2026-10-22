package app.kiwu.android.ui.horario

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import app.kiwu.android.ui.ajustes.DialogoColor
import app.kiwu.android.domain.UrgenciaTarea
import app.kiwu.android.domain.TareasDeClase
import app.kiwu.android.domain.PuntoTarea
import app.kiwu.android.domain.Deber
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardOptions
import app.kiwu.android.domain.Clase
import app.kiwu.android.domain.Horario
import app.kiwu.android.domain.ReglasHorario
import app.kiwu.android.ui.ColorPastel
import app.kiwu.android.ui.DialogoConfirmar
import app.kiwu.android.ui.EstadoVacio
import app.kiwu.android.ui.Pasteles
import app.kiwu.android.ui.SelectorHora
import app.kiwu.android.ui.notas.SelectorEmoji
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.AlertDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min

val EmojisHorario = listOf("🎓", "💼", "🌴", "🏋️", "🎮", "🎵", "🧘", "🏠", "🚌", "🍽️", "📚", "🎨", "⚽", "🧪", "💻", "❤️", "🛒", "✈️", "🐶", "⭐")

/** Qué se está editando en el formulario: una clase existente (por su grupo) o una nueva para un día. */
private class Edicion(val grupo: String?, val dia: Int)

/** Color de una clase: el mismo para todas las clases de una misma materia o grupo. */
private fun colorDe(c: Clase): ColorPastel = ColoresClase.de(c)

/** Módulo de horarios: varios horarios semanales (Universidad, Trabajo, Ocio…), vista por día o por semana. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorarioScreen(vm: HorarioViewModel, alAbrirMenu: () -> Unit, alAbrirTarea: (String) -> Unit = {}, alAbrirLista: (String) -> Unit = {}) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val deberes by vm.deberes.collectAsStateWithLifecycle()
    // Clase cuya hoja de tareas está abierta (se guarda su id: si la clase se edita o se borra, la hoja se actualiza o se cierra).
    var hojaId by rememberSaveable { mutableStateOf<String?>(null) }
    val hoy = remember(estado.ahora) { ReglasHorario.diaDe(LocalDate.now(ZoneId.systemDefault())) }
    var diaSel by rememberSaveable { mutableIntStateOf(hoy) }
    // null = automática: semana en pantallas anchas, un día en el teléfono.
    var vistaElegida by rememberSaveable { mutableStateOf<String?>(null) }
    val ancha = LocalConfiguration.current.screenWidthDp >= 600
    val vista = vistaElegida ?: if (ancha) "semana" else "dia"
    var edicion by remember { mutableStateOf<Edicion?>(null) }
    var nuevoHorario by remember { mutableStateOf(false) }
    var editarHorario by remember { mutableStateOf<Horario?>(null) }
    var eliminarHorario by remember { mutableStateOf<Horario?>(null) }
    var menuHorario by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(estado.horarioActual?.let { "${it.icono} ${it.nombre}" } ?: "Horario") },
                navigationIcon = { IconButton(onClick = alAbrirMenu) { Icon(Icons.Filled.Menu, "Abrir menú") } },
                actions = {
                    if (estado.horarios.isNotEmpty()) {
                        IconButton(onClick = { vistaElegida = if (vista == "semana") "dia" else "semana" }) {
                            Icon(
                                if (vista == "semana") Icons.Filled.ViewDay else Icons.Filled.ViewWeek,
                                if (vista == "semana") "Ver por día" else "Ver la semana",
                            )
                        }
                    }
                    estado.horarioActual?.let { h ->
                        Box {
                            IconButton(onClick = { menuHorario = true }) { Icon(Icons.Filled.MoreVert, "Opciones del horario") }
                            DropdownMenu(expanded = menuHorario, onDismissRequest = { menuHorario = false }) {
                                DropdownMenuItem(text = { Text("Editar horario") }, onClick = { menuHorario = false; editarHorario = h })
                                DropdownMenuItem(
                                    text = { Text("Eliminar horario", color = MaterialTheme.colorScheme.error) },
                                    onClick = { menuHorario = false; eliminarHorario = h },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            if (estado.horarios.isNotEmpty()) {
                FloatingActionButton(onClick = { edicion = Edicion(null, diaSel) }) { Icon(Icons.Filled.Add, "Añadir clase") }
            }
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { relleno ->
        Column(Modifier.padding(relleno).consumeWindowInsets(relleno).fillMaxSize()) {
            if (!estado.cargado) return@Column
            if (estado.horarios.isEmpty()) {
                SinHorarios(vm) { nuevoHorario = true }
                return@Column
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(estado.seleccion == null, { vm.elegir(null) }, { Text("Todos") }) }
                items(estado.horarios, key = { it.id }) { h ->
                    FilterChip(estado.seleccion == h.id, { vm.elegir(h.id) }, { Text("${h.icono} ${h.nombre}") })
                }
                item { TextButton(onClick = { nuevoHorario = true }) { Text("+ Horario") } }
            }
            TarjetaAhora(estado)
            // Al pasar de día a semana el contenido entra desde la derecha; al volver, desde la izquierda.
            androidx.compose.animation.AnimatedContent(
                targetState = vista,
                transitionSpec = {
                    val dir = if (targetState == "semana") 1 else -1
                    (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220, 40)) +
                        androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(260)) { dir * it / 8 }) togetherWith
                        (androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)) +
                            androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(260)) { -dir * it / 8 })
                },
                label = "vistaHorario",
            ) { v ->
                // Cada vista tiene varias piezas seguidas (pestañas, título, lista): sin una columna se apilarían unas sobre otras.
                Column(Modifier.fillMaxSize()) {
                    if (v == "semana") {
                        VistaSemana(estado, hoy, deberes, alAbrir = { hojaId = it.id }, alEditar = { edicion = Edicion(it.grupo, it.diaSemana) })
                    } else {
                        VistaDia(estado, hoy, diaSel, deberes, alElegirDia = { diaSel = it }, alAbrir = { hojaId = it.id }, alEditar = { edicion = Edicion(it.grupo, it.diaSemana) })
                    }
                }
            }
        }
    }

    edicion?.let { e -> FormularioClase(vm, estado, e, alCerrar = { edicion = null }) }
    val claseHoja = hojaId?.let { id -> estado.todas.firstOrNull { it.id == id } }
    if (claseHoja != null) {
        HojaTareasClase(
            clase = claseHoja, estado = estado, deberes = deberes,
            alCerrar = { hojaId = null },
            alEditar = { hojaId = null; edicion = Edicion(claseHoja.grupo, claseHoja.diaSemana) },
            alEliminar = { vm.eliminarClase(claseHoja.grupo); hojaId = null },
            alAbrirTarea = { hojaId = null; alAbrirTarea(it) },
            alAbrirLista = { hojaId = null; alAbrirLista(it) },
            marcar = { id, hecha -> vm.marcarTarea(id, hecha) },
            crearTarea = { titulo, materiaId, fecha -> vm.crearTarea(titulo, materiaId, fecha) },
            vincular = { vm.vincular(claseHoja.grupo, it) },
            crearYVincular = { vm.crearListaYVincular(claseHoja.grupo, it) },
        )
    }
    if (nuevoHorario) DialogoHorario(null, alCerrar = { nuevoHorario = false }) { nombre, icono -> vm.crearHorario(nombre, icono) }
    editarHorario?.let { h -> DialogoHorario(h, alCerrar = { editarHorario = null }) { nombre, icono -> vm.editarHorario(h.id, nombre, icono) } }
    eliminarHorario?.let { h ->
        DialogoConfirmar(
            "¿Eliminar ${h.nombre}?", "Se eliminarán también todas sus clases.", "Eliminar horario y sus clases",
            alCerrar = { eliminarHorario = null },
        ) { vm.eliminarHorario(h.id) }
    }
}

@Composable
private fun SinHorarios(vm: HorarioViewModel, alOtro: () -> Unit) {
    EstadoVacio(Icons.Filled.Schedule, "Crea tu primer horario") {
        Text(
            "Puedes tener varios: uno para la universidad, otro para el trabajo, otro para tu tiempo libre…",
            Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), fontSize = 14.sp,
        )
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { vm.crearHorario("Universidad", "🎓") }) { Text("🎓 Universidad") }
            FilledTonalButton(onClick = { vm.crearHorario("Trabajo", "💼") }) { Text("💼 Trabajo") }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { vm.crearHorario("Ocio", "🌴") }) { Text("🌴 Ocio") }
            OutlinedButton(onClick = alOtro) { Text("Otro…") }
        }
    }
}

/** «Ahora» y «Siguiente»: lo que toca en este momento y lo próximo. */
@Composable
private fun TarjetaAhora(estado: HorarioEstado) {
    val (dia, minuto) = estado.diaYMinuto()
    val clases = estado.clases
    if (clases.isEmpty()) return
    val encurso = ReglasHorario.enCurso(clases, dia, minuto)
    val siguiente = ReglasHorario.siguiente(clases, dia, minuto)
    val texto = buildString {
        if (encurso != null) append("Ahora: ${encurso.titulo} · hasta ${ReglasHorario.hora(encurso.finMin)}")
        if (siguiente != null) {
            val (c, dias) = siguiente
            if (isNotEmpty()) append("\n")
            append("Siguiente: ${c.titulo} · ${ReglasHorario.cuando(dias, c.diaSemana)} ${ReglasHorario.hora(c.inicioMin)}")
        }
    }
    if (texto.isEmpty()) return
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) { Text(texto, Modifier.padding(horizontal = 14.dp, vertical = 10.dp), style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun VistaDia(estado: HorarioEstado, hoy: Int, diaSel: Int, deberes: List<Deber>, alElegirDia: (Int) -> Unit, alAbrir: (Clase) -> Unit, alEditar: (Clase) -> Unit) {
    TabRow(
        selectedTabIndex = diaSel - 1,
        containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        for (d in 1..7) {
            Tab(
                selected = d == diaSel, onClick = { alElegirDia(d) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(ReglasHorario.diasLetra[d - 1], fontWeight = if (d == hoy) FontWeight.ExtraBold else FontWeight.Normal)
                        if (d == hoy) Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                    }
                },
            )
        }
    }
    val delDia = remember(estado.clases, diaSel) { ReglasHorario.deDia(estado.clases, diaSel) }
    val (dia, minuto) = estado.diaYMinuto()
    Text(
        ReglasHorario.nombreDia(diaSel), style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (delDia.isEmpty()) {
            item { Text("Sin clases este día", Modifier.padding(4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(delDia, key = { it.id }) { c ->
            TarjetaClase(
                c, nombreHorario = if (estado.seleccion == null && estado.horarios.size > 1) estado.nombres[c.horarioId] else null,
                enCurso = c.diaSemana == dia && minuto >= c.inicioMin && minuto < c.finMin, alClic = { alAbrir(c) }, alLargo = { alEditar(c) },
                puntos = TareasDeClase.puntos(deberes, c.materiaId, estado.ahora),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TarjetaClase(c: Clase, nombreHorario: String?, enCurso: Boolean, alClic: () -> Unit, alLargo: () -> Unit, puntos: List<PuntoTarea>) {
    val color = colorDe(c)
    Surface(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).combinedClickable(onClick = alClic, onLongClick = alLargo),
        shape = RoundedCornerShape(12.dp), color = color.fondo, contentColor = color.texto,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(64.dp)) {
                Text(ReglasHorario.hora(c.inicioMin), fontWeight = FontWeight.Bold)
                Text(ReglasHorario.hora(c.finMin), fontSize = 13.sp, color = color.texto.copy(alpha = 0.75f))
            }
            Column(Modifier.weight(1f)) {
                Text(c.titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val detalle = listOfNotNull(c.aula.takeIf { it.isNotBlank() }, nombreHorario).joinToString(" · ")
                if (detalle.isNotEmpty()) Text(detalle, fontSize = 13.sp, color = color.texto.copy(alpha = 0.8f))
            }
            if (puntos.isNotEmpty()) PuntosTareas(puntos, color.texto)
            if (enCurso) Text("Ahora", Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

/** Color del punto de una tarea: rojo si está vencida, ámbar si vence pronto y neutro si no hay prisa. */
private fun colorUrgencia(u: UrgenciaTarea, neutro: Color): Color = when (u) {
    UrgenciaTarea.Vencida -> Color(0xFFD32F2F)
    UrgenciaTarea.Pronto -> Color(0xFFF9A825)
    UrgenciaTarea.Normal -> neutro.copy(alpha = 0.6f)
}

/** Hasta tres puntos (uno por tarea pendiente, la más urgente primero) y «+N» si hay más. */
@Composable
private fun PuntosTareas(puntos: List<PuntoTarea>, neutro: Color) {
    Row(Modifier.padding(start = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        for (p in puntos.take(3)) {
            Box(Modifier.size(10.dp).background(colorUrgencia(p.urgencia, neutro), CircleShape).border(1.dp, neutro.copy(alpha = 0.5f), CircleShape))
        }
        if (puntos.size > 3) Text("+${puntos.size - 3}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** Vista de la semana: siete columnas con un eje de horas; las clases que coinciden se reparten el ancho. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VistaSemana(estado: HorarioEstado, hoy: Int, deberes: List<Deber>, alAbrir: (Clase) -> Unit, alEditar: (Clase) -> Unit) {
    val clases = estado.clases
    val primera = if (clases.isEmpty()) 7 else min(7, clases.minOf { it.inicioMin } / 60)
    val ultima = if (clases.isEmpty()) 19 else min(24, max(19, (clases.maxOf { it.finMin } + 59) / 60))
    val altoHora = 52.dp
    val alto = altoHora * (ultima - primera)
    val linea = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Row(Modifier.fillMaxWidth().padding(start = 40.dp, end = 4.dp)) {
        for (d in 1..7) {
            Text(
                ReglasHorario.diasCortos[d - 1], Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 12.sp, fontWeight = if (d == hoy) FontWeight.ExtraBold else FontWeight.Normal,
                color = if (d == hoy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 4.dp, end = 4.dp, bottom = 96.dp)) {
        Box(Modifier.width(36.dp).height(alto)) {
            for (h in primera..ultima) {
                Text("%02d".format(h % 24), Modifier.offset(y = altoHora * (h - primera) - 8.dp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        for (d in 1..7) {
            BoxWithConstraints(Modifier.weight(1f).height(alto)) {
                val ancho = maxWidth
                for (h in primera..ultima) {
                    Box(Modifier.offset(y = altoHora * (h - primera)).fillMaxWidth().height(0.5.dp).background(linea))
                }
                val delDia = ReglasHorario.deDia(clases, d)
                // Reparto en carriles: cada clase va al primer carril libre.
                val finDeCarril = mutableListOf<Int>()
                val carril = delDia.associate { c ->
                    var i = finDeCarril.indexOfFirst { it <= c.inicioMin }
                    if (i < 0) { finDeCarril += c.finMin; i = finDeCarril.lastIndex } else finDeCarril[i] = c.finMin
                    c.id to i
                }
                val carriles = max(1, finDeCarril.size)
                for (c in delDia) {
                    val color = colorDe(c)
                    val y = altoHora * ((c.inicioMin - primera * 60) / 60f)
                    val h = altoHora * ((c.finMin - c.inicioMin) / 60f)
                    Box(
                        Modifier.offset(x = ancho * (carril.getValue(c.id)) / carriles, y = y).width(ancho / carriles).height(h).padding(1.dp)
                            .clip(RoundedCornerShape(5.dp)).background(color.fondo).combinedClickable(onClick = { alAbrir(c) }, onLongClick = { alEditar(c) }).padding(horizontal = 1.dp, vertical = 2.dp),
                    ) {
                        // Las columnas de la semana son estrechas en el teléfono: letra pequeña para que las palabras no se partan.
                        Text(c.titulo, fontSize = 9.sp, lineHeight = 10.sp, color = color.texto, maxLines = 5, overflow = TextOverflow.Ellipsis)
                        // Un solo punto con el número de tareas con fecha cercana (color de la más urgente).
                        val puntos = TareasDeClase.puntos(deberes, c.materiaId, estado.ahora)
                        TareasDeClase.masUrgente(puntos)?.let { u ->
                            Box(
                                Modifier.align(Alignment.TopEnd).size(12.dp).background(colorUrgencia(u, color.texto), CircleShape).border(1.dp, color.texto.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Text("${puntos.size}", fontSize = 8.sp, lineHeight = 8.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                        }
                    }
                }
            }
        }
    }
}

// --- diálogos -------------------------------------------------------------------------------------------

/** Crear o editar un horario: nombre e icono. */
@Composable
internal fun DialogoHorario(horario: Horario?, alCerrar: () -> Unit, alConfirmar: (String, String) -> Unit) {
    var nombre by remember { mutableStateOf(horario?.nombre ?: "") }
    var icono by remember { mutableStateOf(horario?.icono ?: "🎓") }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(if (horario == null) "Nuevo horario" else "Editar horario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    nombre, { nombre = it }, label = { Text("Nombre (Universidad, Trabajo, Ocio…)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Text("Icono", style = MaterialTheme.typography.labelMedium)
                SelectorEmoji(icono, EmojisHorario) { icono = it }
            }
        },
        confirmButton = { TextButton(onClick = { alConfirmar(nombre.trim(), icono); alCerrar() }, enabled = nombre.isNotBlank()) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )
}

/** Formulario de clase a pantalla completa: materia, horario, días, horas, aula, aviso y notas. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormularioClase(vm: HorarioViewModel, estado: HorarioEstado, edicion: Edicion, alCerrar: () -> Unit) {
    val previas = remember(edicion) { edicion.grupo?.let { g -> estado.todas.filter { it.grupo == g } }.orEmpty() }
    val base = previas.firstOrNull()
    var titulo by remember { mutableStateOf(base?.titulo ?: "") }
    var materiaId by remember { mutableStateOf(base?.materiaId) }
    var horarioId by remember { mutableStateOf(base?.horarioId ?: estado.seleccion ?: estado.horarios.first().id) }
    var dias by remember { mutableStateOf(if (previas.isEmpty()) setOf(edicion.dia) else previas.map { it.diaSemana }.toSet()) }
    var inicio by remember { mutableIntStateOf(base?.inicioMin ?: 8 * 60) }
    var fin by remember { mutableIntStateOf(base?.finMin ?: 10 * 60) }
    var aula by remember { mutableStateOf(base?.aula ?: "") }
    var notas by remember { mutableStateOf(base?.notas ?: "") }
    var aviso by remember { mutableStateOf(base?.avisoMin) }
    var color by remember { mutableStateOf(ReglasHorario.colorValido(base?.color)) }
    var otroColor by remember { mutableStateOf(false) }
    var elegirHora by remember { mutableStateOf<String?>(null) }
    var confirmarEliminar by remember { mutableStateOf(false) }
    var menuHorario by remember { mutableStateOf(false) }
    var menuAviso by remember { mutableStateOf(false) }

    val horaValida = ReglasHorario.valida(inicio, fin)
    val choques = vm.choques(estado, edicion.grupo, dias, inicio, fin)
    val puedeGuardar = titulo.isNotBlank() && dias.isNotEmpty() && horaValida

    Dialog(onDismissRequest = alCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = alCerrar) { Icon(Icons.Filled.Close, "Cerrar") }
                    Text(if (edicion.grupo == null) "Nueva clase" else "Editar clase", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    TextButton(
                        enabled = puedeGuardar,
                        onClick = {
                            vm.guardarClase(horarioId, edicion.grupo, titulo.trim(), materiaId, dias, inicio, fin, aula, notas, aviso, color)
                            alCerrar()
                        },
                    ) { Text("Guardar") }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        titulo, {
                            titulo = it
                            // Si el nombre coincide con una de tus listas se vincula sola; si ya no coincide, se desvincula.
                            val coincide = estado.materias.firstOrNull { m -> m.nombre.trim().equals(it.trim(), ignoreCase = true) }
                            if (coincide != null) materiaId = coincide.id
                            else if (materiaId != null) materiaId = null
                        },
                        label = { Text("Materia o actividad") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    )
                    if (estado.materias.isNotEmpty()) {
                        Text("O elige una de tus listas de tareas", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (m in estado.materias.take(12)) {
                                FilterChip(materiaId == m.id, { titulo = m.nombre; materiaId = m.id }, { Text(m.nombre, maxLines = 1) })
                            }
                        }
                    }
                    if (estado.horarios.size > 1) {
                        Text("Horario", style = MaterialTheme.typography.labelMedium)
                        Box {
                            val actual = estado.horarios.firstOrNull { it.id == horarioId }
                            OutlinedButton(onClick = { menuHorario = true }) { Text(actual?.let { "${it.icono} ${it.nombre}" } ?: "Elegir horario") }
                            DropdownMenu(expanded = menuHorario, onDismissRequest = { menuHorario = false }) {
                                for (h in estado.horarios) DropdownMenuItem(text = { Text("${h.icono} ${h.nombre}") }, onClick = { horarioId = h.id; menuHorario = false })
                            }
                        }
                    }
                    Text("Días", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (d in 1..7) {
                            FilterChip(d in dias, { dias = if (d in dias) dias - d else dias + d }, { Text(ReglasHorario.diasCortos[d - 1]) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { elegirHora = "inicio" }, Modifier.weight(1f)) { Text("Inicio  ${ReglasHorario.hora(inicio)}") }
                        OutlinedButton(onClick = { elegirHora = "fin" }, Modifier.weight(1f)) { Text("Fin  ${ReglasHorario.hora(fin)}") }
                    }
                    if (!horaValida) Text("La hora de fin debe ser posterior a la de inicio", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    if (choques.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text("Coincide con otra clase", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                for (c in choques.take(4)) {
                                    val dondeEs = estado.nombres[c.horarioId]?.let { " ($it)" } ?: ""
                                    Text("${c.titulo} · ${ReglasHorario.diasCortos[c.diaSemana - 1]} ${ReglasHorario.rango(c)}$dondeEs", fontSize = 13.sp)
                                }
                                if (choques.size > 4) Text("y ${choques.size - 4} más", fontSize = 13.sp)
                            }
                        }
                    }
                    OutlinedTextField(aula, { aula = it }, label = { Text("Aula o lugar (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Color de la clase", style = MaterialTheme.typography.labelMedium)
                    SelectorColorClase(color, alElegir = { color = it }, alOtro = { otroColor = true })
                    Text("Aviso", style = MaterialTheme.typography.labelMedium)
                    Box {
                        OutlinedButton(onClick = { menuAviso = true }) { Text(aviso?.let { textoAviso(it) } ?: "Sin aviso") }
                        DropdownMenu(expanded = menuAviso, onDismissRequest = { menuAviso = false }) {
                            DropdownMenuItem(text = { Text("Sin aviso") }, onClick = { aviso = null; menuAviso = false })
                            for (m in ReglasHorario.avisos) DropdownMenuItem(text = { Text(textoAviso(m)) }, onClick = { aviso = m; menuAviso = false })
                        }
                    }
                    OutlinedTextField(
                        notas, { notas = it }, label = { Text("Notas (opcional)") }, minLines = 2, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )
                    if (edicion.grupo != null) {
                        TextButton(onClick = { confirmarEliminar = true }) { Text("Eliminar clase", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }

    if (otroColor) {
        DialogoColor(
            inicial = color?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color(0xFF8E24AA),
            alCerrar = { otroColor = false },
            alElegir = { color = ColoresClase.hex(it) },
        )
    }
    when (elegirHora) {
        "inicio" -> SelectorHora(inicio / 60, inicio % 60, alCerrar = { elegirHora = null }, titulo = "Hora de inicio", paso = 5) { h, m ->
            inicio = h * 60 + m
            if (fin <= inicio) fin = min(inicio + 60, ReglasHorario.MINUTOS_DIA)
        }
        "fin" -> SelectorHora(fin / 60 % 24, fin % 60, alCerrar = { elegirHora = null }, titulo = "Hora de fin", paso = 5) { h, m -> fin = if (h == 0 && m == 0) ReglasHorario.MINUTOS_DIA else h * 60 + m }
    }
    if (confirmarEliminar && edicion.grupo != null) {
        DialogoConfirmar(
            "¿Eliminar esta clase?", "Se eliminará en todos los días en que se repite.", "Eliminar",
            alCerrar = { confirmarEliminar = false },
        ) { vm.eliminarClase(edicion.grupo); alCerrar() }
    }
}

private fun textoAviso(min: Int): String = if (min >= 60) "${min / 60} h antes" else "$min min antes"

/** Menú lateral de Horario: los horarios (con su menú), crear uno nuevo y ajustes. */
@Composable
fun MenuHorario(vm: HorarioViewModel, cerrarYLuego: (() -> Unit) -> Unit, alAjustes: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var nuevo by remember { mutableStateOf(false) }
    var editar by remember { mutableStateOf<Horario?>(null) }
    var eliminar by remember { mutableStateOf<Horario?>(null) }
    androidx.compose.material3.ModalDrawerSheet(drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 20.dp, bottom = 16.dp)) {
            item { app.kiwu.android.ui.tareas.CabeceraMenu("Horario", "Clases y actividades") }
            item {
                androidx.compose.material3.NavigationDrawerItem(
                    label = { Text("Todos los horarios") }, icon = { Icon(Icons.Filled.Schedule, null) }, selected = estado.seleccion == null,
                    onClick = { cerrarYLuego { vm.elegir(null) } },
                    modifier = Modifier.padding(androidx.compose.material3.NavigationDrawerItemDefaults.ItemPadding),
                )
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 28.dp, end = 12.dp, top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Mis horarios", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick = { nuevo = true }) { Icon(Icons.Filled.Add, "Nuevo horario") }
                }
            }
            items(estado.horarios, key = { it.id }) { h ->
                var menu by remember { mutableStateOf(false) }
                androidx.compose.material3.NavigationDrawerItem(
                    label = { Text("${h.icono} ${h.nombre}", maxLines = 1) },
                    selected = estado.seleccion == h.id,
                    onClick = { cerrarYLuego { vm.elegir(h.id) } },
                    badge = {
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Opciones de ${h.nombre}") }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text("Editar horario") }, onClick = { menu = false; editar = h })
                                DropdownMenuItem(text = { Text("Eliminar horario", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; eliminar = h })
                            }
                        }
                    },
                    modifier = Modifier.padding(androidx.compose.material3.NavigationDrawerItemDefaults.ItemPadding),
                )
            }
            item { androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 28.dp)) }
            item {
                androidx.compose.material3.NavigationDrawerItem(
                    label = { Text("Ajustes") }, icon = { Icon(Icons.Filled.Settings, null) }, selected = false,
                    onClick = { cerrarYLuego(alAjustes) },
                    modifier = Modifier.padding(androidx.compose.material3.NavigationDrawerItemDefaults.ItemPadding),
                )
            }
        }
    }
    if (nuevo) DialogoHorario(null, alCerrar = { nuevo = false }) { nombre, icono -> vm.crearHorario(nombre, icono) }
    editar?.let { h -> DialogoHorario(h, alCerrar = { editar = null }) { nombre, icono -> vm.editarHorario(h.id, nombre, icono) } }
    eliminar?.let { h ->
        DialogoConfirmar(
            "¿Eliminar ${h.nombre}?", "Se eliminarán también todas sus clases.", "Eliminar horario y sus clases",
            alCerrar = { eliminar = null },
        ) { vm.eliminarHorario(h.id) }
    }
}
