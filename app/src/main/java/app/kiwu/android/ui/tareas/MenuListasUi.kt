package app.kiwu.android.ui.tareas

import app.kiwu.android.domain.Cuaderno
import app.kiwu.android.domain.Horario
import app.kiwu.android.KiwuApp
import app.kiwu.android.data.AjustesUi
import app.kiwu.android.domain.ReglasHorario
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.IconButtonDefaults
import app.kiwu.android.ui.notas.DialogoCuaderno
import app.kiwu.android.ui.horario.DialogoHorario
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.domain.Elemento
import app.kiwu.android.domain.GrupoLista
import app.kiwu.android.domain.ID_EVENTOS
import androidx.compose.material.icons.filled.Event
import app.kiwu.android.domain.Materia
import app.kiwu.android.domain.NodoMenu
import app.kiwu.android.ui.DialogoConfirmar
import app.kiwu.android.ui.Pasteles
import app.kiwu.android.ui.horario.SelectorColorClase
import app.kiwu.android.ui.notas.SelectorEmoji
import app.kiwu.android.R
import androidx.compose.ui.res.stringResource

/** Emojis para ponerle a una lista o a una carpeta. */
val EmojisListas = listOf(
    "📁", "📚", "🎓", "💼", "🧪", "📐", "💻", "🎨", "🎵", "🌍", "🏋️", "🏠", "🛒", "✈️", "💡", "📝", "⭐", "❤️", "🔥", "🌱", "🧠", "⚽", "🎮", "🍽️",
)

/** Dónde cae una lista al soltarla: dentro de una carpeta o suelta a la raíz. */
private sealed interface DestinoDrag {
    data class Carpeta(val grupoId: String) : DestinoDrag
    object Raiz : DestinoDrag
}

/** Pide un nombre y, si se quiere, un emoji. Sirve para crear o editar una lista o una carpeta. */
@Composable
fun DialogoNuevoElemento(
    titulo: String,
    etiqueta: String,
    accion: String = stringResource(R.string.crear),
    inicial: String = "",
    emojiInicial: String? = null,
    /** Color de la lista (`#RRGGBB`); solo se muestra el selector si [conColor]. */
    colorInicial: String? = null,
    conColor: Boolean = false,
    grupos: List<GrupoLista> = emptyList(),
    grupoIdInicial: String? = null,
    conSelectorCarpeta: Boolean = false,
    alCerrar: () -> Unit,
    alConfirmar: (nombre: String, emoji: String?, color: String?, grupoId: String?) -> Unit,
) {
    var texto by remember { mutableStateOf(inicial) }
    var emoji by remember { mutableStateOf(emojiInicial) }
    var color by remember { mutableStateOf(colorInicial) }
    var grupoElegidoId by remember { mutableStateOf(grupoIdInicial) }
    var menuCarpetas by remember { mutableStateOf(false) }
    var elegirEmoji by remember { mutableStateOf(false) }
    val enviar = { if (texto.isNotBlank()) { alConfirmar(texto.trim(), emoji, color, grupoElegidoId); alCerrar() } }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(titulo, maxLines = 2) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = texto, onValueChange = { texto = it }, label = { Text(etiqueta) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { enviar() }),
                )

                if (conSelectorCarpeta && grupos.isNotEmpty()) {
                    val nombreGrupoActual = grupos.firstOrNull { it.id == grupoElegidoId }?.let { "${it.icono} ${it.nombre}" }
                        ?: stringResource(R.string.sin_carpeta)
                    Text(stringResource(R.string.carpeta), style = MaterialTheme.typography.labelLarge)
                    Box {
                        OutlinedButton(
                            onClick = { menuCarpetas = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(nombreGrupoActual, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Icon(Icons.Filled.ExpandMore, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = menuCarpetas, onDismissRequest = { menuCarpetas = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sin_carpeta)) },
                                onClick = {
                                    grupoElegidoId = null
                                    menuCarpetas = false
                                },
                            )
                            for (g in grupos) {
                                DropdownMenuItem(
                                    text = { Text("${g.icono} ${g.nombre}") },
                                    onClick = {
                                        grupoElegidoId = g.id
                                        menuCarpetas = false
                                    },
                                )
                            }
                        }
                    }
                }

                // Emoji opcional: un solo toque abre la cuadrícula; no hace falta editar después.
                Row(Modifier.fillMaxWidth().clickable { elegirEmoji = !elegirEmoji }, verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text(emoji ?: "＋", fontSize = if (emoji != null) 24.sp else 20.sp) }
                    Text(stringResource(if (emoji == null) R.string.emoji_opcional else R.string.emoji_cambiar), Modifier.weight(1f).padding(start = 12.dp))
                    if (emoji != null) TextButton(onClick = { emoji = null }) { Text(stringResource(R.string.ajustes_quitar)) }
                }
                if (elegirEmoji) SelectorEmoji(emoji ?: "", EmojisListas) { emoji = it; elegirEmoji = false }
                if (conColor) {
                    Text(stringResource(R.string.lista_color), style = MaterialTheme.typography.labelLarge)
                    SelectorColorClase(color, alElegir = { color = it })
                }
            }
        },
        confirmButton = { TextButton(onClick = enviar, enabled = texto.isNotBlank()) { Text(accion) } },
        dismissButton = { TextButton(onClick = alCerrar) { Text(stringResource(R.string.cancelar)) } },
    )
}

/** Qué cuadro de nombre y emoji está abierto. */
private sealed interface Dialogo {
    data class NuevaLista(val grupoId: String?, val nombreCarpeta: String?) : Dialogo
    data object NuevaCarpeta : Dialogo
    data class EditarLista(val materia: Materia) : Dialogo
    data class EditarCarpeta(val grupo: GrupoLista) : Dialogo
    data class EditarCuaderno(val cuaderno: Cuaderno) : Dialogo
    data class EditarHorario(val horario: Horario) : Dialogo
}

/** Menú lateral de Tareas. Se dibuja sobre toda la pantalla (también sobre la barra inferior). */
@Composable
fun MenuTareas(
    vm: TareasViewModel,
    cerrarYLuego: (() -> Unit) -> Unit,
    alAjustes: () -> Unit,
    alRacha: () -> Unit = {},
    cuadernos: List<Cuaderno> = emptyList(),
    horarios: List<Horario> = emptyList(),
    flashcardsPendientes: Int = 0,
    alElegirCuaderno: ((String) -> Unit)? = null,
    alElegirHorario: ((String) -> Unit)? = null,
    alRepasoHoy: () -> Unit = {},
    alNuevoCuaderno: () -> Unit = {},
    alNuevoHorario: () -> Unit = {},
    alPapelera: () -> Unit = {},
    app: KiwuApp? = null,
    ajustes: AjustesUi = AjustesUi(),
    alCambiarArrastre: (Boolean) -> Unit = {},
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val alcance = rememberCoroutineScope()
    var dialogo by remember { mutableStateOf<Dialogo?>(null) }
    var eliminarLista by remember { mutableStateOf<Materia?>(null) }
    var eliminarCarpeta by remember { mutableStateOf<GrupoLista?>(null) }
    var eliminarCuaderno by remember { mutableStateOf<Cuaderno?>(null) }
    var eliminarHorario by remember { mutableStateOf<Horario?>(null) }
    var moverLista by remember { mutableStateOf<Materia?>(null) }

    PanelLateral(
        estado = estado,
        alElegir = { e -> cerrarYLuego { vm.ir(e) } },
        alNuevaLista = { dialogo = Dialogo.NuevaLista(null, null) },
        alNuevaCarpeta = { dialogo = Dialogo.NuevaCarpeta },
        alNuevaListaEn = { g -> dialogo = Dialogo.NuevaLista(g.id, g.nombre) },
        alPlegar = { g, plegado -> vm.plegarGrupo(g.id, plegado) },
        alEditarLista = { dialogo = Dialogo.EditarLista(it) }, alEliminarLista = { eliminarLista = it },
        alMoverLista = { moverLista = it }, alSacarLista = { vm.moverAGrupo(it.id, null) },
        alMoverAGrupo = { m, g -> vm.moverAGrupo(m, g) }, alMoverOrdenLista = { m, d -> vm.moverOrdenLista(m.id, d) },
        alEditarCarpeta = { dialogo = Dialogo.EditarCarpeta(it) },
        alEliminarCarpeta = { eliminarCarpeta = it }, alMoverOrdenCarpeta = { g, d -> vm.moverOrdenGrupo(g.id, d) },
        alEditarCuaderno = { dialogo = Dialogo.EditarCuaderno(it) },
        alEliminarCuaderno = { eliminarCuaderno = it },
        alEditarHorario = { dialogo = Dialogo.EditarHorario(it) },
        alEliminarHorario = { eliminarHorario = it },
        alAjustes = { cerrarYLuego(alAjustes) },
        alRacha = { cerrarYLuego(alRacha) },
        cuadernos = cuadernos,
        horarios = horarios,
        flashcardsPendientes = flashcardsPendientes,
        alElegirCuaderno = { id -> cerrarYLuego { alElegirCuaderno?.invoke(id) } },
        alElegirHorario = { id -> cerrarYLuego { alElegirHorario?.invoke(id) } },
        alRepasoHoy = { cerrarYLuego(alRepasoHoy) },
        alNuevoCuaderno = { cerrarYLuego(alNuevoCuaderno) },
        alNuevoHorario = { cerrarYLuego(alNuevoHorario) },
        alPapelera = { cerrarYLuego(alPapelera) },
        app = app,
        ajustes = ajustes,
        alCambiarArrastre = alCambiarArrastre,
    )

    when (val d = dialogo) {
        is Dialogo.NuevaLista -> DialogoNuevoElemento(
            titulo = if (d.nombreCarpeta != null) stringResource(R.string.lista_nueva_en, d.nombreCarpeta) else stringResource(R.string.lista_nueva),
            etiqueta = stringResource(R.string.lista_nombre_materia), conColor = true,
            grupos = estado.grupos, grupoIdInicial = d.grupoId, conSelectorCarpeta = true,
            alCerrar = { dialogo = null },
        ) { nombre, emoji, color, grupoId ->
            cerrarYLuego {
                vm.crearLista(nombre, emoji, grupoId, color)
                if (grupoId != null) {
                    vm.plegarGrupo(grupoId, false)
                }
            }
        }
        Dialogo.NuevaCarpeta -> DialogoNuevoElemento(stringResource(R.string.carpeta_nueva), stringResource(R.string.carpeta_nombre), alCerrar = { dialogo = null }) { nombre, emoji, _, _ ->
            cerrarYLuego { vm.crearGrupo(nombre, emoji) }
        }
        is Dialogo.EditarLista -> DialogoNuevoElemento(
            stringResource(R.string.lista_editar), stringResource(R.string.nombre), accion = stringResource(R.string.guardar), inicial = d.materia.nombre,
            emojiInicial = d.materia.emoji, colorInicial = d.materia.color, conColor = true, alCerrar = { dialogo = null },
        ) { nombre, emoji, color, _ -> vm.editarLista(d.materia.id, nombre, emoji, color) }
        is Dialogo.EditarCarpeta -> DialogoNuevoElemento(
            stringResource(R.string.carpeta_editar), stringResource(R.string.nombre), accion = stringResource(R.string.guardar), inicial = d.grupo.nombre,
            emojiInicial = d.grupo.icono.takeIf { it != "📁" }, alCerrar = { dialogo = null },
        ) { nombre, emoji, _, _ -> vm.editarGrupo(d.grupo.id, nombre, emoji) }
        is Dialogo.EditarCuaderno -> DialogoCuaderno(
            cuaderno = d.cuaderno,
            alCerrar = { dialogo = null },
        ) { nombre, icono, photoPath, colorHex ->
            app?.let { kApp ->
                alcance.launch { kApp.repo.editarCuaderno(d.cuaderno.id, nombre, icono, photoPath, colorHex) }
            }
        }
        is Dialogo.EditarHorario -> DialogoHorario(
            horario = d.horario,
            alCerrar = { dialogo = null },
        ) { nombre, icono ->
            app?.let { kApp ->
                alcance.launch { kApp.repo.editarHorario(d.horario.id, nombre, icono) }
            }
        }
        null -> Unit
    }
    eliminarLista?.let { m ->
        DialogoConfirmar(
            titulo = stringResource(R.string.lista_eliminar_titulo, m.nombre),
            mensaje = stringResource(R.string.lista_eliminar_texto),
            accion = stringResource(R.string.lista_eliminar_boton),
            alCerrar = { eliminarLista = null },
        ) { vm.eliminarLista(m.id) }
    }
    eliminarCarpeta?.let { g ->
        DialogoConfirmar(
            titulo = stringResource(R.string.carpeta_eliminar_titulo, g.nombre),
            mensaje = stringResource(R.string.carpeta_eliminar_texto),
            accion = stringResource(R.string.carpeta_eliminar),
            alCerrar = { eliminarCarpeta = null },
        ) { vm.eliminarGrupo(g.id) }
    }
    eliminarCuaderno?.let { c ->
        DialogoConfirmar(
            titulo = stringResource(R.string.cuaderno_eliminar_titulo, c.nombre),
            mensaje = stringResource(R.string.cuaderno_eliminar_texto),
            accion = stringResource(R.string.cuaderno_eliminar_boton),
            alCerrar = { eliminarCuaderno = null },
        ) {
            app?.let { kApp ->
                alcance.launch { kApp.repo.eliminarCuaderno(c.id) }
            }
        }
    }
    eliminarHorario?.let { h ->
        DialogoConfirmar(
            titulo = stringResource(R.string.horario_eliminar_titulo, h.nombre),
            mensaje = stringResource(R.string.horario_eliminar_texto),
            accion = stringResource(R.string.horario_eliminar),
            alCerrar = { eliminarHorario = null },
        ) {
            app?.let { kApp ->
                alcance.launch { kApp.repo.eliminarHorario(h.id) }
            }
        }
    }
    moverLista?.let { m ->
        DialogoMoverLista(m, estado.grupos, alCerrar = { moverLista = null }, alElegir = { vm.moverAGrupo(m.id, it) }, alNuevaCarpeta = { nombre, emoji -> vm.crearGrupoYMover(nombre, emoji, m.id) })
    }
}

@Composable
private fun PanelLateral(
    estado: TareasEstado,
    alElegir: (Elemento) -> Unit,
    alNuevaLista: () -> Unit,
    alNuevaCarpeta: () -> Unit,
    alNuevaListaEn: (GrupoLista) -> Unit,
    alPlegar: (GrupoLista, Boolean) -> Unit,
    alEditarLista: (Materia) -> Unit,
    alEliminarLista: (Materia) -> Unit,
    alMoverLista: (Materia) -> Unit,
    alSacarLista: (Materia) -> Unit,
    alMoverAGrupo: (String, String?) -> Unit,
    alMoverOrdenLista: (Materia, Int) -> Unit,
    alEditarCarpeta: (GrupoLista) -> Unit,
    alEliminarCarpeta: (GrupoLista) -> Unit,
    alMoverOrdenCarpeta: (GrupoLista, Int) -> Unit,
    alEditarCuaderno: (Cuaderno) -> Unit = {},
    alEliminarCuaderno: (Cuaderno) -> Unit = {},
    alEditarHorario: (Horario) -> Unit = {},
    alEliminarHorario: (Horario) -> Unit = {},
    alAjustes: () -> Unit,
    alRacha: () -> Unit,
    cuadernos: List<Cuaderno> = emptyList(),
    horarios: List<Horario> = emptyList(),
    flashcardsPendientes: Int = 0,
    alElegirCuaderno: ((String) -> Unit)? = null,
    alElegirHorario: ((String) -> Unit)? = null,
    alRepasoHoy: () -> Unit = {},
    alNuevoCuaderno: () -> Unit = {},
    alNuevoHorario: () -> Unit = {},
    alPapelera: () -> Unit = {},
    app: KiwuApp? = null,
    ajustes: AjustesUi = AjustesUi(),
    alCambiarArrastre: (Boolean) -> Unit = {},
) {
    var menuNueva by remember { mutableStateOf(false) }
    var listasPlegadas by rememberSaveable { mutableStateOf(false) }
    var cuadernosPlegados by rememberSaveable { mutableStateOf(false) }
    var horariosPlegados by rememberSaveable { mutableStateOf(true) }

    val alcance = rememberCoroutineScope()

    ModalDrawerSheet(
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier.width(310.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- ZONA 1: CABECERA COMPACTA ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.kiwu_icon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                    Text(
                        "Kiwu",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Racha
                val streakCount = estado.racha?.actual ?: 0
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFFFF3E0),
                    modifier = Modifier.clickable { alRacha() },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            if (streakCount == 1) "1 día" else "$streakCount días",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100),
                        )
                    }
                }
            }

            // --- ZONA 2: CUERPO CON SCROLL VERTICAL ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                // A. Cuadrícula 2x2 de Accesos Rápidos
                val hoyCuenta = estado.cuentas[Elemento.Hoy] ?: 0
                val proxCuenta = estado.cuentas[Elemento.Proximos] ?: 0
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TarjetaAccesoRapido(
                            titulo = stringResource(R.string.hoy),
                            icono = Icons.Filled.WbSunny,
                            cuenta = hoyCuenta,
                            seleccionado = estado.elemento == Elemento.Hoy,
                            modifier = Modifier.weight(1f),
                            onClick = { alElegir(Elemento.Hoy) },
                        )
                        TarjetaAccesoRapido(
                            titulo = "7 días",
                            icono = Icons.Filled.DateRange,
                            cuenta = proxCuenta,
                            seleccionado = estado.elemento == Elemento.Proximos,
                            modifier = Modifier.weight(1f),
                            onClick = { alElegir(Elemento.Proximos) },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TarjetaAccesoRapido(
                            titulo = stringResource(R.string.calendario),
                            icono = Icons.Filled.CalendarMonth,
                            cuenta = 0,
                            seleccionado = estado.elemento == Elemento.Calendario,
                            modifier = Modifier.weight(1f),
                            onClick = { alElegir(Elemento.Calendario) },
                        )
                        TarjetaAccesoRapido(
                            titulo = stringResource(R.string.eventos),
                            icono = Icons.Filled.AutoAwesome,
                            cuenta = 0,
                            seleccionado = estado.elemento == Elemento.DeMateria(ID_EVENTOS),
                            modifier = Modifier.weight(1f),
                            onClick = { alElegir(Elemento.DeMateria(ID_EVENTOS)) },
                        )
                    }
                }

                // B. Fila Dividida Intermedia (Todas las tareas | Repasar hoy)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Izquierda: Todas las tareas
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { alElegir(Elemento.Todos) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Inbox, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    "Todas",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            val cuentaTodos = estado.cuentas[Elemento.Todos] ?: 0
                            if (cuentaTodos > 0) {
                                Text("$cuentaTodos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        VerticalDivider(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Derecha: Repasar
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { alRepasoHoy() }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f, fill = false),
                            ) {
                                Icon(Icons.Filled.Style, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    "Repasar",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (flashcardsPendientes > 0) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                ) {
                                    Text(
                                        "$flashcardsPendientes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // C. Sección TUS ESPACIOS
                Text(
                    "TUS ESPACIOS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                )

                // 1. Listas de Tareas Accordion
                Box {
                    EncabezamientoAcordeon(
                        titulo = stringResource(R.string.listas),
                        expandido = !listasPlegadas,
                        alToggle = { listasPlegadas = !listasPlegadas },
                        alAdd = { menuNueva = true },
                    )
                    DropdownMenu(expanded = menuNueva, onDismissRequest = { menuNueva = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.lista_nueva)) }, onClick = { menuNueva = false; alNuevaLista() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.carpeta_nueva)) }, onClick = { menuNueva = false; alNuevaCarpeta() })
                    }
                }

                AnimatedVisibility(visible = !listasPlegadas) {
                    var arrastrandoListaId by remember { mutableStateOf<String?>(null) }
                    var dragOffsetY by remember { mutableFloatStateOf(0f) }
                    var targetActual by remember { mutableStateOf<DestinoDrag?>(null) }
                    var itemBoundsWindow by remember { mutableStateOf<Map<String, Rect>>(emptyMap()) }
                    var targetBoundsWindow by remember { mutableStateOf<Map<DestinoDrag, Rect>>(emptyMap()) }
                    var startPointerWindowPos by remember { mutableStateOf(Offset.Zero) }
                    var currentPointerWindowPos by remember { mutableStateOf(Offset.Zero) }

                    val haptic = LocalHapticFeedback.current

                    LaunchedEffect(arrastrandoListaId) {
                        alCambiarArrastre(arrastrandoListaId != null)
                    }

                    Column {
                        val todasLasMaterias = remember(estado.menu) {
                            estado.menu.flatMap {
                                when (it) {
                                    is NodoMenu.Carpeta -> it.listas.map { l -> l.materia }
                                    is NodoMenu.Lista -> listOf(it.entrada.materia)
                                }
                            }
                        }
                        val listaEnArrastre = todasLasMaterias.firstOrNull { it.id == arrastrandoListaId }
                        val esDeCarpeta = listaEnArrastre?.grupoId != null

                        if (arrastrandoListaId != null && esDeCarpeta) {
                            val resaltadoRaiz = targetActual == DestinoDrag.Raiz
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (resaltadoRaiz) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.5.dp, if (resaltadoRaiz) MaterialTheme.colorScheme.primary else Color.Transparent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .onGloballyPositioned { coords ->
                                        targetBoundsWindow = targetBoundsWindow + (DestinoDrag.Raiz to coords.boundsInWindow())
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text(
                                        stringResource(R.string.sacar_de_carpeta),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        for (nodo in estado.menu) {
                            when (nodo) {
                                is NodoMenu.Carpeta -> {
                                    val g = nodo.grupo
                                    val isTarget = targetActual == DestinoDrag.Carpeta(g.id) && arrastrandoListaId != null && listaEnArrastre?.grupoId != g.id
                                    FilaCarpetaDrawer(
                                        grupo = g,
                                        pendientes = nodo.pendientes,
                                        seleccionado = estado.elemento == Elemento.DeGrupo(g.id),
                                        resaltada = isTarget,
                                        onPositioned = { bounds ->
                                            targetBoundsWindow = targetBoundsWindow + (DestinoDrag.Carpeta(g.id) to bounds)
                                        },
                                        alClic = { alElegir(Elemento.DeGrupo(g.id)) },
                                        alPlegar = { alPlegar(g, !g.plegado) },
                                        alNuevaListaEn = { alNuevaListaEn(g) },
                                        alEditar = { alEditarCarpeta(g) },
                                        alEliminar = { alEliminarCarpeta(g) },
                                    )
                                    if (!g.plegado) {
                                        for (e in nodo.listas) {
                                            val m = e.materia
                                            val isBeingDragged = arrastrandoListaId == m.id
                                            val dragModifier = Modifier.pointerInput(m.id) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { startOffset ->
                                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                        arrastrandoListaId = m.id
                                                        dragOffsetY = 0f
                                                        val itemRect = itemBoundsWindow[m.id] ?: Rect.Zero
                                                        startPointerWindowPos = itemRect.topLeft + startOffset
                                                        currentPointerWindowPos = startPointerWindowPos
                                                        targetActual = null
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetY += dragAmount.y
                                                        currentPointerWindowPos += dragAmount
                                                        targetActual = targetBoundsWindow.entries.firstOrNull { (_, rect) ->
                                                            rect.contains(currentPointerWindowPos)
                                                        }?.key
                                                    },
                                                    onDragEnd = {
                                                        val finalTarget = targetActual
                                                        val draggedId = m.id
                                                        if (finalTarget != null) {
                                                            when (finalTarget) {
                                                                is DestinoDrag.Carpeta -> {
                                                                    if (m.grupoId != finalTarget.grupoId) {
                                                                        alMoverAGrupo(draggedId, finalTarget.grupoId)
                                                                        val targetGrupo = estado.grupos.firstOrNull { it.id == finalTarget.grupoId }
                                                                        if (targetGrupo != null && targetGrupo.plegado) {
                                                                            alPlegar(targetGrupo, false)
                                                                        }
                                                                    }
                                                                }
                                                                DestinoDrag.Raiz -> {
                                                                    if (m.grupoId != null) {
                                                                        alMoverAGrupo(draggedId, null)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        arrastrandoListaId = null
                                                        dragOffsetY = 0f
                                                        targetActual = null
                                                    },
                                                    onDragCancel = {
                                                        arrastrandoListaId = null
                                                        dragOffsetY = 0f
                                                        targetActual = null
                                                    }
                                                )
                                            }
                                            FilaListaDrawer(
                                                materia = m,
                                                cuenta = e.pendientes,
                                                seleccionado = estado.elemento == Elemento.DeMateria(m.id),
                                                sangria = 16.dp,
                                                isBeingDragged = isBeingDragged,
                                                dragOffsetY = dragOffsetY,
                                                dragModifier = dragModifier,
                                                onPositioned = { bounds ->
                                                    itemBoundsWindow = itemBoundsWindow + (m.id to bounds)
                                                },
                                                alClic = { alElegir(Elemento.DeMateria(m.id)) },
                                                alEditar = { alEditarLista(m) },
                                                alMover = { alMoverLista(m) },
                                                alSacar = { alSacarLista(m) },
                                                alEliminar = { alEliminarLista(m) },
                                            )
                                        }
                                    }
                                }
                                is NodoMenu.Lista -> {
                                    val m = nodo.entrada.materia
                                    val isBeingDragged = arrastrandoListaId == m.id
                                    val dragModifier = Modifier.pointerInput(m.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { startOffset ->
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                arrastrandoListaId = m.id
                                                dragOffsetY = 0f
                                                val itemRect = itemBoundsWindow[m.id] ?: Rect.Zero
                                                startPointerWindowPos = itemRect.topLeft + startOffset
                                                currentPointerWindowPos = startPointerWindowPos
                                                targetActual = null
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount.y
                                                currentPointerWindowPos += dragAmount
                                                targetActual = targetBoundsWindow.entries.firstOrNull { (_, rect) ->
                                                    rect.contains(currentPointerWindowPos)
                                                }?.key
                                            },
                                            onDragEnd = {
                                                val finalTarget = targetActual
                                                val draggedId = m.id
                                                if (finalTarget != null) {
                                                    when (finalTarget) {
                                                        is DestinoDrag.Carpeta -> {
                                                            if (m.grupoId != finalTarget.grupoId) {
                                                                alMoverAGrupo(draggedId, finalTarget.grupoId)
                                                                val targetGrupo = estado.grupos.firstOrNull { it.id == finalTarget.grupoId }
                                                                if (targetGrupo != null && targetGrupo.plegado) {
                                                                    alPlegar(targetGrupo, false)
                                                                }
                                                            }
                                                        }
                                                        DestinoDrag.Raiz -> {
                                                            if (m.grupoId != null) {
                                                                alMoverAGrupo(draggedId, null)
                                                            }
                                                        }
                                                    }
                                                }
                                                arrastrandoListaId = null
                                                dragOffsetY = 0f
                                                targetActual = null
                                            },
                                            onDragCancel = {
                                                arrastrandoListaId = null
                                                dragOffsetY = 0f
                                                targetActual = null
                                            }
                                        )
                                    }
                                    FilaListaDrawer(
                                        materia = m,
                                        cuenta = nodo.entrada.pendientes,
                                        seleccionado = estado.elemento == Elemento.DeMateria(m.id),
                                        sangria = 0.dp,
                                        isBeingDragged = isBeingDragged,
                                        dragOffsetY = dragOffsetY,
                                        dragModifier = dragModifier,
                                        onPositioned = { bounds ->
                                            itemBoundsWindow = itemBoundsWindow + (m.id to bounds)
                                        },
                                        alClic = { alElegir(Elemento.DeMateria(m.id)) },
                                        alEditar = { alEditarLista(m) },
                                        alMover = { alMoverLista(m) },
                                        alSacar = { alSacarLista(m) },
                                        alEliminar = { alEliminarLista(m) },
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Cuadernos Accordion
                EncabezamientoAcordeon(
                    titulo = stringResource(R.string.cuadernos),
                    expandido = !cuadernosPlegados,
                    alToggle = { cuadernosPlegados = !cuadernosPlegados },
                    alAdd = { alNuevoCuaderno() },
                )
                AnimatedVisibility(visible = !cuadernosPlegados) {
                    Column {
                        for (c in cuadernos) {
                            FilaCuadernoDrawer(
                                cuaderno = c,
                                seleccionado = false,
                                alClic = { alElegirCuaderno?.invoke(c.id) },
                                alLongClick = { alEditarCuaderno(c) },
                                alEditar = { alEditarCuaderno(c) },
                                alEliminar = { alEliminarCuaderno(c) },
                            )
                        }
                    }
                }

                // 3. Mis Horarios Accordion
                EncabezamientoAcordeon(
                    titulo = stringResource(R.string.horario_mis),
                    expandido = !horariosPlegados,
                    alToggle = { horariosPlegados = !horariosPlegados },
                    alAdd = { alNuevoHorario() },
                )
                AnimatedVisibility(visible = !horariosPlegados) {
                    Column {
                        for (h in horarios) {
                            FilaHorarioDrawer(
                                horario = h,
                                seleccionado = false,
                                alClic = { alElegirHorario?.invoke(h.id) },
                                alLongClick = { alEditarHorario(h) },
                                alEditar = { alEditarHorario(h) },
                                alEliminar = { alEliminarHorario(h) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // --- ZONA 3: PIE FIJO ANCLADO ABAJO (Sticky Footer) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Fila superior: Completadas | Papelera
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { alElegir(Elemento.Completadas) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.completadas), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    VerticalDivider(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { alPapelera() }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.papelera), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Fila inferior: Ajustes + Selector de Tema (Pill)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                        .heightIn(min = 52.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { alAjustes() },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            Text(
                                stringResource(R.string.ajustes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        // Theme selector pill (Sol / Luna)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Row(
                                modifier = Modifier.padding(2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val esOscuroActual = ajustes.tema == "oscuro"
                                IconButton(
                                    onClick = {
                                        app?.let { kApp ->
                                            alcance.launch {
                                                kApp.ajustes.cambiar { it.copy(tema = "claro") }
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = if (!esOscuroActual) MaterialTheme.colorScheme.surface else Color.Transparent,
                                        contentColor = if (!esOscuroActual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                ) {
                                    Icon(Icons.Filled.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        app?.let { kApp ->
                                            alcance.launch {
                                                kApp.ajustes.cambiar { it.copy(tema = "oscuro") }
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = if (esOscuroActual) MaterialTheme.colorScheme.surface else Color.Transparent,
                                        contentColor = if (esOscuroActual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                ) {
                                    Icon(Icons.Filled.NightsStay, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaAccesoRapido(
    titulo: String,
    icono: ImageVector,
    cuenta: Int,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (seleccionado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (seleccionado) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    icono,
                    contentDescription = null,
                    tint = if (seleccionado) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    titulo,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (seleccionado) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (cuenta > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (seleccionado) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        "$cuenta",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EncabezamientoAcordeon(
    titulo: String,
    expandido: Boolean,
    alToggle: () -> Unit,
    alAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = alToggle)
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                if (expandido) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Text(
                titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(
            onClick = alAdd,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = stringResource(R.string.lista_o_carpeta_nueva),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun FilaCarpetaDrawer(
    grupo: GrupoLista,
    pendientes: Int,
    seleccionado: Boolean,
    resaltada: Boolean = false,
    onPositioned: ((Rect) -> Unit)? = null,
    alClic: () -> Unit,
    alPlegar: () -> Unit,
    alNuevaListaEn: () -> Unit,
    alEditar: () -> Unit,
    alEliminar: () -> Unit,
) {
    var menuAbierto by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            resaltada -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            seleccionado -> MaterialTheme.colorScheme.primaryContainer
            else -> Color.Transparent
        },
        border = if (resaltada) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .heightIn(min = 46.dp)
            .then(if (onPositioned != null) Modifier.onGloballyPositioned { onPositioned(it.boundsInWindow()) } else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = alClic),
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                IconButton(onClick = alPlegar, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (grupo.plegado) Icons.Filled.ChevronRight else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(grupo.icono.ifBlank { "📁" }, fontSize = 18.sp)
                Text(
                    grupo.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pendientes > 0) {
                    Text(
                        "$pendientes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Box {
                    IconButton(
                        onClick = { menuAbierto = true },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.opciones_de, grupo.nombre),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lista_nueva_aqui)) },
                            onClick = { menuAbierto = false; alNuevaListaEn() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.carpeta_editar)) },
                            onClick = { menuAbierto = false; alEditar() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.carpeta_eliminar), color = MaterialTheme.colorScheme.error) },
                            onClick = { menuAbierto = false; alEliminar() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaListaDrawer(
    materia: Materia,
    cuenta: Int,
    seleccionado: Boolean,
    sangria: androidx.compose.ui.unit.Dp = 0.dp,
    isBeingDragged: Boolean = false,
    dragOffsetY: Float = 0f,
    dragModifier: Modifier = Modifier,
    onPositioned: ((Rect) -> Unit)? = null,
    alClic: () -> Unit,
    alEditar: () -> Unit,
    alMover: () -> Unit,
    alSacar: () -> Unit,
    alEliminar: () -> Unit,
) {
    var menuAbierto by remember { mutableStateOf(false) }

    val colorMateria = remember(materia.color) {
        val valido = ReglasHorario.colorValido(materia.color)
        if (valido != null) {
            try { Color(android.graphics.Color.parseColor(valido)) } catch (_: Exception) { Color(0xFF6750A4) }
        } else Color(0xFF6750A4)
    }

    val dragVisualModifier = if (isBeingDragged) {
        Modifier
            .zIndex(10f)
            .graphicsLayer {
                translationY = dragOffsetY
                scaleX = 1.03f
                scaleY = 1.03f
                shadowElevation = 12f
                alpha = 0.92f
            }
    } else {
        Modifier
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            seleccionado -> MaterialTheme.colorScheme.primaryContainer
            else -> Color.Transparent
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp + sangria, end = 12.dp, top = 2.dp, bottom = 2.dp)
            .heightIn(min = 46.dp)
            .then(if (onPositioned != null) Modifier.onGloballyPositioned { onPositioned(it.boundsInWindow()) } else Modifier)
            .then(dragModifier)
            .then(dragVisualModifier)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = alClic),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorMateria.copy(alpha = 0.25f),
                    modifier = Modifier.size(28.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(materia.emoji ?: "📋", fontSize = 16.sp)
                    }
                }
                Text(
                    materia.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (cuenta > 0) {
                    Text(
                        "$cuenta",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Box {
                    IconButton(
                        onClick = { menuAbierto = true },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.opciones_de, materia.nombre),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lista_editar)) },
                            onClick = { menuAbierto = false; alEditar() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mover_a_carpeta)) },
                            onClick = { menuAbierto = false; alMover() },
                        )
                        if (materia.grupoId != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sacar_de_carpeta)) },
                                onClick = { menuAbierto = false; alSacar() },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lista_eliminar), color = MaterialTheme.colorScheme.error) },
                            onClick = { menuAbierto = false; alEliminar() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaCuadernoDrawer(
    cuaderno: Cuaderno,
    seleccionado: Boolean,
    alClic: () -> Unit,
    alLongClick: () -> Unit,
    alEditar: () -> Unit,
    alEliminar: () -> Unit,
) {
    var menuAbierto by remember { mutableStateOf(false) }

    val colorBook = remember(cuaderno.coverColorHex) {
        val hex = cuaderno.coverColorHex
        if (!hex.isNullOrBlank()) {
            try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color(0xFF6750A4) }
        } else Color(0xFF6750A4)
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (seleccionado) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .heightIn(min = 46.dp)
            .clickable(onClick = alClic),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 5.dp, bottomEnd = 5.dp),
                    color = colorBook,
                    modifier = Modifier.size(width = 20.dp, height = 26.dp),
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .width(2.5.dp)
                                .fillMaxHeight()
                                .background(Color.Black.copy(alpha = 0.15f))
                                .align(Alignment.CenterStart)
                        )
                    }
                }
                Text(
                    cuaderno.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            Box {
                IconButton(
                    onClick = { menuAbierto = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.opciones_de, cuaderno.nombre),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.cuaderno_editar)) },
                        onClick = { menuAbierto = false; alEditar() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.cuaderno_eliminar), color = MaterialTheme.colorScheme.error) },
                        onClick = { menuAbierto = false; alEliminar() },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaHorarioDrawer(
    horario: Horario,
    seleccionado: Boolean,
    alClic: () -> Unit,
    alLongClick: () -> Unit,
    alEditar: () -> Unit,
    alEliminar: () -> Unit,
) {
    var menuAbierto by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (seleccionado) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .heightIn(min = 46.dp)
            .clickable(onClick = alClic),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(horario.icono.ifBlank { "🎓" }, fontSize = 20.sp)
                Text(
                    horario.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            Box {
                IconButton(
                    onClick = { menuAbierto = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.opciones_de, horario.nombre),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.horario_editar)) },
                        onClick = { menuAbierto = false; alEditar() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.horario_eliminar), color = MaterialTheme.colorScheme.error) },
                        onClick = { menuAbierto = false; alEliminar() },
                    )
                }
            }
        }
    }
}

/** Fila fija del menú (Hoy, Próximos 7 días…). */
@Composable
internal fun FilaMenu(
    titulo: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    seleccionado: Boolean,
    cuenta: Int,
    alClic: () -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(titulo, maxLines = 1) },
        icon = { Icon(icono, null) },
        selected = seleccionado,
        onClick = alClic,
        badge = { if (cuenta > 0) Text(cuenta.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}

/** Cuadro para mover una lista a una carpeta (o dejarla suelta, o crear una carpeta nueva con ella). */
@Composable
private fun DialogoMoverLista(
    m: Materia, grupos: List<GrupoLista>, alCerrar: () -> Unit, alElegir: (String?) -> Unit, alNuevaCarpeta: (String, String?) -> Unit,
) {
    var nueva by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(R.string.mover_n, m.nombre), maxLines = 2) },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                OpcionCarpeta(stringResource(R.string.sin_carpeta), m.grupoId == null) { alElegir(null); alCerrar() }
                for (g in grupos) OpcionCarpeta("${g.icono} ${g.nombre}", m.grupoId == g.id) { alElegir(g.id); alCerrar() }
                TextButton(onClick = { nueva = true }) { Text(stringResource(R.string.carpeta_nueva_mas)) }
            }
        },
        confirmButton = { TextButton(onClick = alCerrar) { Text(stringResource(R.string.cerrar)) } },
    )
    if (nueva) DialogoNuevoElemento(stringResource(R.string.carpeta_nueva), stringResource(R.string.carpeta_nombre), accion = stringResource(R.string.crear_y_mover), alCerrar = { nueva = false }) { nombre, emoji, _, _ ->
        alNuevaCarpeta(nombre, emoji); alCerrar()
    }
}

@Composable
private fun OpcionCarpeta(texto: String, marcada: Boolean, alPulsar: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = alPulsar), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(marcada, onClick = alPulsar)
        Text(texto, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
