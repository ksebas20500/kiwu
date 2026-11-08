package app.kiwu.android.ui.horario

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.AlertDialog
import app.kiwu.android.ui.diaEnFrase
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.R
import app.kiwu.android.domain.Clase
import app.kiwu.android.domain.ContadorFaltas
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.Horario
import app.kiwu.android.domain.PuntoTarea
import app.kiwu.android.domain.ReglasFaltas
import app.kiwu.android.domain.ReglasHorario
import app.kiwu.android.domain.TareasDeClase
import app.kiwu.android.domain.UrgenciaTarea
import app.kiwu.android.ui.ColorPastel
import app.kiwu.android.ui.DialogoConfirmar
import app.kiwu.android.ui.EstadoVacio
import app.kiwu.android.ui.Fechas
import app.kiwu.android.ui.SelectorHora
import app.kiwu.android.ui.ajustes.DialogoColor
import app.kiwu.android.ui.notas.SelectorEmoji
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min

val EmojisHorario = listOf("🎓", "💼", "🌴", "🏋️", "🎮", "🎵", "🧘", "🏠", "🚌", "🍽️", "📚", "🎨", "⚽", "🧪", "💻", "❤️", "🛒", "✈️", "🐶", "⭐")

private class Edicion(val grupo: String?, val dia: Int)

private fun colorDe(c: Clase): ColorPastel = ColoresClase.de(c)

/** Módulo de horarios: varios horarios semanales, vista por día o por semana. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorarioScreen(vm: HorarioViewModel, alAbrirMenu: () -> Unit, alAbrirTarea: (String) -> Unit = {}, alAbrirLista: (String) -> Unit = {}) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val deberes by vm.deberes.collectAsStateWithLifecycle()
    var hojaId by rememberSaveable { mutableStateOf<String?>(null) }
    val hoy = remember(estado.ahora) { ReglasHorario.diaDe(LocalDate.now(ZoneId.systemDefault())) }
    var diaSel by rememberSaveable { mutableIntStateOf(hoy) }
    var vistaElegida by rememberSaveable { mutableStateOf<String?>(null) }
    val ancha = LocalConfiguration.current.screenWidthDp >= 600
    val vista = vistaElegida ?: if (ancha) "semana" else "dia"
    var edicion by remember { mutableStateOf<Edicion?>(null) }
    var nuevoHorario by remember { mutableStateOf(false) }
    var editarHorario by remember { mutableStateOf<Horario?>(null) }
    var eliminarHorario by remember { mutableStateOf<Horario?>(null) }
    var menuHorario by remember { mutableStateOf(false) }
    var vaciarFaltas by remember { mutableStateOf<Horario?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(estado.horarioActual?.let { "${it.icono} ${it.nombre}" } ?: stringResource(R.string.horario)) },
                navigationIcon = { IconButton(onClick = alAbrirMenu) { Icon(Icons.Filled.Menu, stringResource(R.string.abrir_menu)) } },
                actions = {
                    if (estado.horarios.isNotEmpty()) {
                        IconButton(onClick = { vistaElegida = if (vista == "semana") "dia" else "semana" }) {
                            Icon(
                                if (vista == "semana") Icons.Filled.ViewDay else Icons.Filled.ViewWeek,
                                stringResource(if (vista == "semana") R.string.ver_por_dia else R.string.ver_semana),
                            )
                        }
                    }
                    estado.horarioActual?.let { h ->
                        Box {
                            IconButton(onClick = { menuHorario = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.horario_opciones)) }
                            DropdownMenu(expanded = menuHorario, onDismissRequest = { menuHorario = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.horario_editar)) }, onClick = { menuHorario = false; editarHorario = h })
                                DropdownMenuItem(text = { Text(stringResource(R.string.horario_vaciar_faltas)) }, onClick = { menuHorario = false; vaciarFaltas = h })
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.horario_eliminar), color = MaterialTheme.colorScheme.error) },
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
                FloatingActionButton(onClick = { edicion = Edicion(null, diaSel) }) { Icon(Icons.Filled.Add, stringResource(R.string.horario_anadir_clase)) }
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
                item { FilterChip(estado.seleccion == null, { vm.elegir(null) }, { Text(stringResource(R.string.horario_todos)) }) }
                items(estado.horarios, key = { it.id }) { h ->
                    FilterChip(estado.seleccion == h.id, { vm.elegir(h.id) }, { Text("${h.icono} ${h.nombre}") })
                }
                item { TextButton(onClick = { nuevoHorario = true }) { Text(stringResource(R.string.horario_mas)) } }
            }
            TarjetaAhora(estado)
            AvisoFaltas(estado.enRiesgo) { hojaId = it.id }
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
            marcarFalta = { vm.marcarFalta(claseHoja, it) },
            quitarFalta = { vm.quitarFalta(it) },
            justificarFalta = { id, si -> vm.justificarFalta(id, si) },
            ponerMaximoFaltas = { vm.ponerMaximoFaltas(claseHoja.grupo, it) },
        )
    }
    if (nuevoHorario) DialogoHorario(null, alCerrar = { nuevoHorario = false }) { nombre, icono -> vm.crearHorario(nombre, icono) }
    editarHorario?.let { h -> DialogoHorario(h, alCerrar = { editarHorario = null }) { nombre, icono -> vm.editarHorario(h.id, nombre, icono) } }
    vaciarFaltas?.let { h ->
        DialogoConfirmar(
            stringResource(R.string.horario_vaciar_titulo, h.nombre), stringResource(R.string.horario_vaciar_texto),
            stringResource(R.string.horario_vaciar_boton), alCerrar = { vaciarFaltas = null },
        ) { vm.vaciarFaltas(h.id) }
    }
    eliminarHorario?.let { h ->
        DialogoConfirmar(
            stringResource(R.string.horario_eliminar_titulo, h.nombre), stringResource(R.string.horario_eliminar_texto), stringResource(R.string.horario_eliminar_boton),
            alCerrar = { eliminarHorario = null },
        ) { vm.eliminarHorario(h.id) }
    }
}

@Composable
private fun SinHorarios(vm: HorarioViewModel, alOtro: () -> Unit) {
    val universidad = stringResource(R.string.horario_universidad)
    val trabajo = stringResource(R.string.horario_trabajo)
    val ocio = stringResource(R.string.horario_ocio)
    EstadoVacio(Icons.Filled.Schedule, stringResource(R.string.horario_vacio_titulo)) {
        Text(
            stringResource(R.string.horario_vacio_texto),
            Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), fontSize = 14.sp,
        )
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { vm.crearHorario(universidad, "🎓") }) { Text("🎓 $universidad") }
            FilledTonalButton(onClick = { vm.crearHorario(trabajo, "💼") }) { Text("💼 $trabajo") }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { vm.crearHorario(ocio, "🌴") }) { Text("🌴 $ocio") }
            OutlinedButton(onClick = alOtro) { Text(stringResource(R.string.horario_otro)) }
        }
    }
}

@Composable
private fun TarjetaAhora(estado: HorarioEstado) {
    val (dia, minuto) = estado.diaYMinuto()
    val clases = estado.clases
    if (clases.isEmpty()) return
    val encurso = ReglasHorario.enCurso(clases, dia, minuto)
    val siguiente = ReglasHorario.siguiente(clases, dia, minuto)
    val ahora = encurso?.let { stringResource(R.string.horario_ahora_hasta, it.titulo, ReglasHorario.hora(it.finMin)) }
    val luego = siguiente?.let { (c, dias) ->
        val cuando = when (dias) {
            0 -> stringResource(R.string.horario_hoy)
            1 -> stringResource(R.string.horario_manana)
            else -> Fechas.diaEnFrase(c.diaSemana)
        }
        stringResource(R.string.horario_siguiente, c.titulo, cuando, ReglasHorario.hora(c.inicioMin))
    }
    val texto = listOfNotNull(ahora, luego).joinToString("\n")
    if (texto.isEmpty()) return
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) { Text(texto, Modifier.padding(horizontal = 14.dp, vertical = 10.dp), style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun VistaDia(estado: HorarioEstado, hoy: Int, diaSel: Int, deberes: List<Deber>, alElegirDia: (Int) -> Unit, alAbrir: (Clase) -> Unit, alEditar: (Clase) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (d in 1..7) {
            val sel = d == diaSel
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { alElegirDia(d) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    Fechas.letraDia(d),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (sel || d == hoy) FontWeight.Bold else FontWeight.Normal,
                    color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    val delDia = remember(estado.clases, diaSel) { ReglasHorario.deDia(estado.clases, diaSel) }
    val (dia, minuto) = estado.diaYMinuto()
    Text(
        Fechas.nombreDia(diaSel), style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (delDia.isEmpty()) {
            item { Text(stringResource(R.string.horario_sin_clases_dia), Modifier.padding(4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(delDia, key = { it.id }) { c ->
            TarjetaClase(
                c, nombreHorario = if (estado.seleccion == null && estado.horarios.size > 1) estado.nombres[c.horarioId] else null,
                enCurso = c.diaSemana == dia && minuto >= c.inicioMin && minuto < c.finMin, alClic = { alAbrir(c) }, alLargo = { alEditar(c) },
                puntos = TareasDeClase.puntos(deberes, c.materiaId, estado.ahora),
                faltas = estado.contadores[c.grupo]?.takeIf { ReglasFaltas.seMuestra(it) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TarjetaClase(c: Clase, nombreHorario: String?, enCurso: Boolean, alClic: () -> Unit, alLargo: () -> Unit, puntos: List<PuntoTarea>, faltas: ContadorFaltas? = null) {
    val color = colorDe(c)
    Surface(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).combinedClickable(onClick = alClic, onLongClick = alLargo),
        shape = RoundedCornerShape(12.dp), color = color.fondo, contentColor = color.texto,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(64.dp)) {
                Text(ReglasHorario.hora(c.inicioMin), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(ReglasHorario.hora(c.finMin), fontSize = 12.sp, color = color.texto.copy(alpha = 0.7f))
            }
            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(c.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val detalle = listOfNotNull(c.aula.takeIf { it.isNotBlank() }, nombreHorario).joinToString(" · ")
                if (detalle.isNotBlank()) {
                    Text(detalle, fontSize = 13.sp, color = color.texto.copy(alpha = 0.8f))
                }
            }
            faltas?.let { InsigniaFaltas(it, color.texto, Modifier.padding(start = 4.dp)) }
            if (puntos.isNotEmpty()) PuntosTareas(puntos, color.texto)
            if (enCurso) Text(stringResource(R.string.horario_ahora), Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

private fun colorUrgencia(u: UrgenciaTarea, neutro: Color): Color = when (u) {
    UrgenciaTarea.Vencida -> Color(0xFFD32F2F)
    UrgenciaTarea.Pronto -> Color(0xFFF9A825)
    UrgenciaTarea.Normal -> neutro.copy(alpha = 0.6f)
}

@Composable
private fun PuntosTareas(puntos: List<PuntoTarea>, neutro: Color) {
    Row(Modifier.padding(start = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        for (p in puntos.take(3)) {
            Box(Modifier.size(10.dp).background(colorUrgencia(p.urgencia, neutro), CircleShape).border(1.dp, neutro.copy(alpha = 0.5f), CircleShape))
        }
        if (puntos.size > 3) Text("+${puntos.size - 3}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

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
                Fechas.diaCorto(d), Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                        Text(c.titulo, fontSize = 9.sp, lineHeight = 10.sp, color = color.texto, maxLines = 5, overflow = TextOverflow.Ellipsis)
                        val puntos = TareasDeClase.puntos(deberes, c.materiaId, estado.ahora)
                        TareasDeClase.masUrgente(puntos)?.let { u ->
                            Box(
                                Modifier.align(Alignment.TopEnd).size(12.dp).background(colorUrgencia(u, color.texto), CircleShape).border(1.dp, color.texto.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Text("${puntos.size}", fontSize = 8.sp, lineHeight = 8.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                        }
                        estado.contadores[c.grupo]?.takeIf { ReglasFaltas.seMuestra(it) }?.let { f ->
                            val fondo = colorFaltas(f.estado, color.texto.copy(alpha = 0.55f))
                            Box(Modifier.align(Alignment.BottomStart).background(fondo, RoundedCornerShape(4.dp)).padding(horizontal = 3.dp)) {
                                Text("${f.quedan ?: f.cuentan}", fontSize = 8.sp, lineHeight = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DialogoHorario(horario: Horario?, alCerrar: () -> Unit, alConfirmar: (String, String) -> Unit) {
    var nombre by remember { mutableStateOf(horario?.nombre ?: "") }
    var icono by remember { mutableStateOf(horario?.icono ?: "🎓") }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(if (horario == null) R.string.horario_nuevo else R.string.horario_editar)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    nombre, { nombre = it }, label = { Text(stringResource(R.string.horario_nombre)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Text(stringResource(R.string.horario_icono), style = MaterialTheme.typography.labelMedium)
                SelectorEmoji(icono, EmojisHorario) { icono = it }
            }
        },
        confirmButton = { TextButton(onClick = { alConfirmar(nombre.trim(), icono); alCerrar() }, enabled = nombre.isNotBlank()) { Text(stringResource(R.string.guardar)) } },
        dismissButton = { TextButton(onClick = alCerrar) { Text(stringResource(R.string.cancelar)) } },
    )
}

/** Formulario de clase a pantalla completa: autocompletado de materia, días, rango de horas, aula, aviso y faltas stepper. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
    var maxFaltas by remember { mutableStateOf(ReglasFaltas.maximo(previas)?.toString() ?: "") }
    var otroColor by remember { mutableStateOf(false) }
    var elegirHora by remember { mutableStateOf<String?>(null) }
    var confirmarEliminar by remember { mutableStateOf(false) }
    var menuHorario by remember { mutableStateOf(false) }
    var menuAviso by remember { mutableStateOf(false) }
    var expandirMaterias by remember { mutableStateOf(false) }

    val horaValida = ReglasHorario.valida(inicio, fin)
    val choques = vm.choques(estado, edicion.grupo, dias, inicio, fin)
    val puedeGuardar = titulo.isNotBlank() && dias.isNotEmpty() && horaValida

    Dialog(onDismissRequest = alCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().imePadding(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = alCerrar) { Icon(Icons.Filled.Close, stringResource(R.string.cerrar)) }
                    Text(stringResource(if (edicion.grupo == null) R.string.clase_nueva else R.string.clase_editar), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    TextButton(
                        enabled = puedeGuardar,
                        onClick = {
                            vm.guardarClase(horarioId, edicion.grupo, titulo.trim(), materiaId, dias, inicio, fin, aula, notas, aviso, color, maxFaltas.toIntOrNull())
                            alCerrar()
                        },
                    ) { Text(stringResource(R.string.guardar)) }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Campo unificado autocompletado de materia
                    ExposedDropdownMenuBox(expanded = expandirMaterias, onExpandedChange = { expandirMaterias = it }) {
                        OutlinedTextField(
                            value = titulo,
                            onValueChange = {
                                titulo = it
                                expandirMaterias = true
                                val coincide = estado.materias.firstOrNull { m -> m.nombre.trim().equals(it.trim(), ignoreCase = true) }
                                if (coincide != null) materiaId = coincide.id else if (materiaId != null) materiaId = null
                            },
                            label = { Text(stringResource(R.string.clase_materia)) },
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandirMaterias) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        )
                        val filtradas = remember(titulo, estado.materias) {
                            if (titulo.isBlank()) estado.materias
                            else estado.materias.filter { it.nombre.contains(titulo, ignoreCase = true) }
                        }
                        if (filtradas.isNotEmpty()) {
                            ExposedDropdownMenu(expanded = expandirMaterias, onDismissRequest = { expandirMaterias = false }) {
                                for (m in filtradas) {
                                    DropdownMenuItem(
                                        text = { Text(m.nombre) },
                                        onClick = {
                                            titulo = m.nombre
                                            materiaId = m.id
                                            expandirMaterias = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (estado.horarios.size > 1) {
                        Text(stringResource(R.string.horario), style = MaterialTheme.typography.labelMedium)
                        Box {
                            val actual = estado.horarios.firstOrNull { it.id == horarioId }
                            OutlinedButton(onClick = { menuHorario = true }) { Text(actual?.let { "${it.icono} ${it.nombre}" } ?: stringResource(R.string.clase_elegir_horario)) }
                            DropdownMenu(expanded = menuHorario, onDismissRequest = { menuHorario = false }) {
                                for (h in estado.horarios) DropdownMenuItem(text = { Text("${h.icono} ${h.nombre}") }, onClick = { horarioId = h.id; menuHorario = false })
                            }
                        }
                    }

                    // Botones circulares/píldoras L M X J V S D
                    Text(stringResource(R.string.clase_dias), style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        for (d in 1..7) {
                            val sel = d in dias
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { dias = if (sel) dias - d else dias + d },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    Fechas.letraDia(d),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Selector de horas Inicio / Fin
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { elegirHora = "inicio" }, Modifier.weight(1f)) { Text("Inicio  ${ReglasHorario.hora(inicio)}") }
                        OutlinedButton(onClick = { elegirHora = "fin" }, Modifier.weight(1f)) { Text("Fin  ${ReglasHorario.hora(fin)}") }
                    }
                    if (!horaValida) Text(stringResource(R.string.clase_hora_invalida), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    if (choques.isNotEmpty()) {
                        val c = choques.first()
                        val dondeEs = estado.nombres[c.horarioId]?.let { " ($it)" } ?: ""
                        Text(
                            "⚠️ Cruce con ${c.titulo} (${Fechas.diaCorto(c.diaSemana)} ${ReglasHorario.rango(c)}$dondeEs)",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    OutlinedTextField(aula, { aula = it }, label = { Text(stringResource(R.string.clase_aula)) }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Text(stringResource(R.string.clase_color), style = MaterialTheme.typography.labelMedium)
                    SelectorColorClase(color, alElegir = { color = it }, alOtro = { otroColor = true })

                    Text(stringResource(R.string.clase_aviso), style = MaterialTheme.typography.labelMedium)
                    Box {
                        OutlinedButton(onClick = { menuAviso = true }) { Text(aviso?.let { textoAviso(it) } ?: stringResource(R.string.clase_sin_aviso)) }
                        DropdownMenu(expanded = menuAviso, onDismissRequest = { menuAviso = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.clase_sin_aviso)) }, onClick = { aviso = null; menuAviso = false })
                            for (m in ReglasHorario.avisos) DropdownMenuItem(text = { Text(textoAviso(m)) }, onClick = { aviso = m; menuAviso = false })
                        }
                    }

                    // Stepper para Máximo de faltas [-] N [+]
                    Text("Máximo de faltas", style = MaterialTheme.typography.labelMedium)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val valorActual = maxFaltas.toIntOrNull() ?: 0
                        OutlinedButton(
                            onClick = {
                                val nuevo = max(0, valorActual - 1)
                                maxFaltas = if (nuevo == 0) "" else nuevo.toString()
                            },
                            enabled = valorActual > 0
                        ) { Text("–", fontWeight = FontWeight.Bold, fontSize = 16.sp) }

                        Text(
                            if (valorActual == 0) "Sin límite" else "$valorActual faltas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        OutlinedButton(
                            onClick = {
                                val nuevo = valorActual + 1
                                maxFaltas = nuevo.toString()
                            }
                        ) { Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                    }
                    Text(
                        "Límite de inasistencias permitidas en el semestre/periodo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (edicion.grupo != null) {
                        TextButton(onClick = { confirmarEliminar = true }) { Text(stringResource(R.string.clase_eliminar), color = MaterialTheme.colorScheme.error) }
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
        "inicio" -> SelectorHora(inicio / 60, inicio % 60, alCerrar = { elegirHora = null }, titulo = stringResource(R.string.clase_hora_inicio), paso = 5) { h, m ->
            inicio = h * 60 + m
            if (fin <= inicio) fin = min(inicio + 60, ReglasHorario.MINUTOS_DIA)
        }
        "fin" -> SelectorHora(fin / 60 % 24, fin % 60, alCerrar = { elegirHora = null }, titulo = stringResource(R.string.clase_hora_fin), paso = 5) { h, m -> fin = if (h == 0 && m == 0) ReglasHorario.MINUTOS_DIA else h * 60 + m }
    }
    if (confirmarEliminar && edicion.grupo != null) {
        DialogoConfirmar(
            stringResource(R.string.clase_eliminar_titulo), stringResource(R.string.clase_eliminar_texto), stringResource(R.string.eliminar),
            alCerrar = { confirmarEliminar = false },
        ) { vm.eliminarClase(edicion.grupo); alCerrar() }
    }
}

@Composable
private fun textoAviso(min: Int): String =
    if (min >= 60) stringResource(R.string.clase_aviso_horas, min / 60) else stringResource(R.string.clase_aviso_min, min)

/** Menú lateral de Horario: los horarios (con su menú), crear uno nuevo y ajustes. */
@Composable
fun MenuHorario(vm: HorarioViewModel, cerrarYLuego: (() -> Unit) -> Unit, alAjustes: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var nuevo by remember { mutableStateOf(false) }
    var editar by remember { mutableStateOf<Horario?>(null) }
    var eliminar by remember { mutableStateOf<Horario?>(null) }
    androidx.compose.material3.ModalDrawerSheet(drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 20.dp, bottom = 16.dp)) {
            item { app.kiwu.android.ui.tareas.CabeceraMenu(stringResource(R.string.horario), stringResource(R.string.horario_menu_sub)) }
            item {
                androidx.compose.material3.NavigationDrawerItem(
                    label = { Text(stringResource(R.string.horario_todos_los)) }, icon = { Icon(Icons.Filled.Schedule, null) }, selected = estado.seleccion == null,
                    onClick = { cerrarYLuego { vm.elegir(null) } },
                    modifier = Modifier.padding(androidx.compose.material3.NavigationDrawerItemDefaults.ItemPadding),
                )
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 28.dp, end = 12.dp, top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.horario_mis), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick = { nuevo = true }) { Icon(Icons.Filled.Add, stringResource(R.string.horario_nuevo)) }
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
                            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.opciones_de, h.nombre)) }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.horario_editar)) }, onClick = { menu = false; editar = h })
                                DropdownMenuItem(text = { Text(stringResource(R.string.horario_eliminar), color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; eliminar = h })
                            }
                        }
                    },
                    modifier = Modifier.padding(androidx.compose.material3.NavigationDrawerItemDefaults.ItemPadding),
                )
            }
            item { androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 28.dp)) }
            item {
                androidx.compose.material3.NavigationDrawerItem(
                    label = { Text(stringResource(R.string.ajustes)) }, icon = { Icon(Icons.Filled.Settings, null) }, selected = false,
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
            stringResource(R.string.horario_eliminar_titulo, h.nombre), stringResource(R.string.horario_eliminar_texto), stringResource(R.string.horario_eliminar_boton),
            alCerrar = { eliminar = null },
        ) { vm.eliminarHorario(h.id) }
    }
}
