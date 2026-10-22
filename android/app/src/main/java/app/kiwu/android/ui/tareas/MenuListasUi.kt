package app.kiwu.android.ui.tareas

import androidx.compose.foundation.background
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
import app.kiwu.android.ui.notas.SelectorEmoji

/** Emojis para ponerle a una lista o a una carpeta. */
val EmojisListas = listOf(
    "📁", "📚", "🎓", "💼", "🧪", "📐", "💻", "🎨", "🎵", "🌍", "🏋️", "🏠", "🛒", "✈️", "💡", "📝", "⭐", "❤️", "🔥", "🌱", "🧠", "⚽", "🎮", "🍽️",
)

/** Dónde cae una lista al soltarla: dentro de una carpeta o suelta (grupoId = null). */
private data class Destino(val grupoId: String?)

/** Las acciones de arrastrar una lista con el dedo (mantener pulsada y mover). */
private class ArrastreLista(val inicio: (Offset) -> Unit, val mover: (Offset) -> Unit, val soltar: () -> Unit, val cancelar: () -> Unit)

/** Pide un nombre y, si se quiere, un emoji. Sirve para crear o editar una lista o una carpeta. */
@Composable
fun DialogoNuevoElemento(
    titulo: String,
    etiqueta: String,
    accion: String = "Crear",
    inicial: String = "",
    emojiInicial: String? = null,
    alCerrar: () -> Unit,
    alConfirmar: (nombre: String, emoji: String?) -> Unit,
) {
    var texto by remember { mutableStateOf(inicial) }
    var emoji by remember { mutableStateOf(emojiInicial) }
    var elegirEmoji by remember { mutableStateOf(false) }
    val enviar = { if (texto.isNotBlank()) { alConfirmar(texto.trim(), emoji); alCerrar() } }
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
                // Emoji opcional: un solo toque abre la cuadrícula; no hace falta editar después.
                Row(Modifier.fillMaxWidth().clickable { elegirEmoji = !elegirEmoji }, verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text(emoji ?: "＋", fontSize = if (emoji != null) 24.sp else 20.sp) }
                    Text(if (emoji == null) "Emoji (opcional)" else "Emoji · toca para cambiarlo", Modifier.weight(1f).padding(start = 12.dp))
                    if (emoji != null) TextButton(onClick = { emoji = null }) { Text("Quitar") }
                }
                if (elegirEmoji) SelectorEmoji(emoji ?: "", EmojisListas) { emoji = it; elegirEmoji = false }
            }
        },
        confirmButton = { TextButton(onClick = enviar, enabled = texto.isNotBlank()) { Text(accion) } },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )
}

/** Qué cuadro de nombre y emoji está abierto. */
private sealed interface Dialogo {
    data class NuevaLista(val grupoId: String?, val nombreCarpeta: String?) : Dialogo
    data object NuevaCarpeta : Dialogo
    data class EditarLista(val materia: Materia) : Dialogo
    data class EditarCarpeta(val grupo: GrupoLista) : Dialogo
}

/** Menú lateral de Tareas. Se dibuja sobre toda la pantalla (también sobre la barra inferior). */
@Composable
fun MenuTareas(vm: TareasViewModel, cerrarYLuego: (() -> Unit) -> Unit, alAjustes: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var dialogo by remember { mutableStateOf<Dialogo?>(null) }
    var eliminarLista by remember { mutableStateOf<Materia?>(null) }
    var eliminarCarpeta by remember { mutableStateOf<GrupoLista?>(null) }
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
        alAjustes = { cerrarYLuego(alAjustes) },
    )

    when (val d = dialogo) {
        is Dialogo.NuevaLista -> DialogoNuevoElemento(
            titulo = if (d.nombreCarpeta != null) "Nueva lista en ${d.nombreCarpeta}" else "Nueva lista",
            etiqueta = "Nombre de la materia", alCerrar = { dialogo = null },
        ) { nombre, emoji -> cerrarYLuego { vm.crearLista(nombre, emoji, d.grupoId) } }
        Dialogo.NuevaCarpeta -> DialogoNuevoElemento("Nueva carpeta", "Nombre de la carpeta", alCerrar = { dialogo = null }) { nombre, emoji ->
            cerrarYLuego { vm.crearGrupo(nombre, emoji) }
        }
        is Dialogo.EditarLista -> DialogoNuevoElemento(
            "Editar lista", "Nombre", accion = "Guardar", inicial = d.materia.nombre, emojiInicial = d.materia.emoji, alCerrar = { dialogo = null },
        ) { nombre, emoji -> vm.editarLista(d.materia.id, nombre, emoji) }
        is Dialogo.EditarCarpeta -> DialogoNuevoElemento(
            "Editar carpeta", "Nombre", accion = "Guardar", inicial = d.grupo.nombre,
            emojiInicial = d.grupo.icono.takeIf { it != "📁" }, alCerrar = { dialogo = null },
        ) { nombre, emoji -> vm.editarGrupo(d.grupo.id, nombre, emoji) }
        null -> Unit
    }
    eliminarLista?.let { m ->
        DialogoConfirmar(
            titulo = "¿Eliminar ${m.nombre}?",
            mensaje = "Se eliminarán también todas sus tareas. Los archivos de la carpeta vinculada no se tocan.",
            accion = "Eliminar lista y sus tareas",
            alCerrar = { eliminarLista = null },
        ) { vm.eliminarLista(m.id) }
    }
    eliminarCarpeta?.let { g ->
        DialogoConfirmar(
            titulo = "¿Eliminar la carpeta ${g.nombre}?",
            mensaje = "Sus listas y sus tareas no se borran: pasan a estar sueltas.",
            accion = "Eliminar carpeta",
            alCerrar = { eliminarCarpeta = null },
        ) { vm.eliminarGrupo(g.id) }
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
    alAjustes: () -> Unit,
) {
    val compacto = estado.aspecto == "compacto"
    // Los contadores los manda solo su interruptor en Ajustes; «Compacto» solo hace las filas más bajas.
    val mostrarCuenta = estado.contadores
    var menuNueva by remember { mutableStateOf(false) }

    // Arrastrar y soltar: cada fila que puede recibir una lista apunta aquí dónde está en pantalla y a qué carpeta pertenece.
    val limites = remember { mutableMapOf<String, Pair<Rect, Destino>>() }
    var arrastrando by remember { mutableStateOf<Materia?>(null) }
    var dedo by remember { mutableStateOf(Offset.Zero) }
    var raiz by remember { mutableStateOf(Offset.Zero) }
    fun destinoEn(p: Offset): Destino? = limites.values.firstOrNull { it.first.contains(p) }?.second
    val sobre = if (arrastrando != null) destinoEn(dedo) else null
    val arrastre = ArrastreLista(
        inicio = { p -> dedo = p },
        mover = { d -> dedo += d },
        soltar = {
            val m = arrastrando
            val d = destinoEn(dedo)
            arrastrando = null
            if (m != null && d != null && d.grupoId != m.grupoId) alMoverAGrupo(m.id, d.grupoId)
        },
        cancelar = { arrastrando = null },
    )

    ModalDrawerSheet(drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)) {
        Box(Modifier.fillMaxSize().onGloballyPositioned { raiz = it.positionInRoot() }) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 20.dp, bottom = 16.dp)) {
                item { CabeceraMenu("Kiwu", "Tareas y listas") }
                val fijos = listOf(
                    Triple(Elemento.Hoy, "Hoy", Icons.Filled.Today),
                    Triple(Elemento.Proximos, "Próximos 7 días", Icons.Filled.DateRange),
                    Triple(Elemento.Calendario, "Calendario", Icons.Filled.CalendarMonth),
                    Triple(Elemento.Todos, "Todas las tareas", Icons.Filled.ViewList),
                    // Los eventos viven en su propia lista fija, fuera de las carpetas.
                    Triple(Elemento.DeMateria(ID_EVENTOS), "Eventos", Icons.Filled.Event),
                )
                items(fijos) { (e, titulo, icono) -> FilaMenu(titulo, icono, estado.elemento == e, estado.cuentas[e] ?: 0) { alElegir(e) } }

                item {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 28.dp, end = 12.dp, top = 14.dp).registrar("cabecera", Destino(null), limites),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val sacando = arrastrando?.grupoId != null
                        Text(
                            if (sacando) "Suelta aquí para sacarla de la carpeta" else "Listas", Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (sacando && sobre == Destino(null)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Box {
                            IconButton(onClick = { menuNueva = true }) { Icon(Icons.Filled.Add, "Nueva lista o carpeta") }
                            DropdownMenu(expanded = menuNueva, onDismissRequest = { menuNueva = false }) {
                                DropdownMenuItem(text = { Text("Nueva lista") }, onClick = { menuNueva = false; alNuevaLista() })
                                DropdownMenuItem(text = { Text("Nueva carpeta") }, onClick = { menuNueva = false; alNuevaCarpeta() })
                            }
                        }
                    }
                }

                for (nodo in estado.menu) {
                    when (nodo) {
                        is NodoMenu.Carpeta -> {
                            val g = nodo.grupo
                            item(key = "g-${g.id}") {
                                FilaLista(
                                    clave = "g-${g.id}", destino = Destino(g.id), limites = limites,
                                    titulo = g.nombre, compacta = compacto, sangria = 0.dp,
                                    seleccionado = estado.elemento == Elemento.DeGrupo(g.id), resaltada = arrastrando != null && sobre == Destino(g.id),
                                    cuenta = nodo.pendientes, mostrarCuenta = mostrarCuenta, alClic = { alElegir(Elemento.DeGrupo(g.id)) },
                                    inicio = {
                                        IconButton(onClick = { alPlegar(g, !g.plegado) }, Modifier.size(36.dp)) {
                                            Icon(if (g.plegado) Icons.Filled.ChevronRight else Icons.Filled.ExpandMore, if (g.plegado) "Desplegar ${g.nombre}" else "Plegar ${g.nombre}")
                                        }
                                    },
                                    icono = { Text(g.icono, fontSize = 20.sp) },
                                    arrastre = null, // las carpetas no se arrastran: así nunca queda una carpeta dentro de otra
                                    menu = { cerrar ->
                                        DropdownMenuItem(text = { Text("Nueva lista aquí") }, onClick = { cerrar(); alNuevaListaEn(g) })
                                        DropdownMenuItem(text = { Text("Editar (nombre y emoji)") }, onClick = { cerrar(); alEditarCarpeta(g) })
                                        DropdownMenuItem(text = { Text("Subir") }, onClick = { cerrar(); alMoverOrdenCarpeta(g, -1) })
                                        DropdownMenuItem(text = { Text("Bajar") }, onClick = { cerrar(); alMoverOrdenCarpeta(g, 1) })
                                        DropdownMenuItem(text = { Text("Eliminar carpeta", color = MaterialTheme.colorScheme.error) }, onClick = { cerrar(); alEliminarCarpeta(g) })
                                    },
                                )
                            }
                            if (!g.plegado) {
                                if (nodo.listas.isEmpty()) {
                                    // Una carpeta vacía se ve y se puede llenar: crear una lista aquí o arrastrar una hasta ella.
                                    item(key = "v-${g.id}") {
                                        Row(
                                            Modifier.fillMaxWidth().padding(start = 60.dp, end = 16.dp).registrar("v-${g.id}", Destino(g.id), limites),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Carpeta vacía", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                            TextButton(onClick = { alNuevaListaEn(g) }) { Text("＋ Nueva lista") }
                                        }
                                    }
                                }
                                items(nodo.listas, key = { "l-${it.materia.id}" }) { e ->
                                    FilaDeLista(
                                        e.materia, e.pendientes, estado, mostrarCuenta, compacto, 20.dp, Destino(g.id), limites,
                                        arrastrando?.id == e.materia.id, alElegir, alEditarLista, alEliminarLista, alMoverLista, alSacarLista, alMoverOrdenLista,
                                        ArrastreLista(
                                            inicio = { p -> arrastrando = e.materia; arrastre.inicio(p) }, mover = arrastre.mover, soltar = arrastre.soltar, cancelar = arrastre.cancelar,
                                        ),
                                    )
                                }
                            }
                        }
                        is NodoMenu.Lista -> item(key = "l-${nodo.entrada.materia.id}") {
                            val m = nodo.entrada.materia
                            FilaDeLista(
                                m, nodo.entrada.pendientes, estado, mostrarCuenta, compacto, 0.dp, Destino(null), limites,
                                arrastrando?.id == m.id, alElegir, alEditarLista, alEliminarLista, alMoverLista, alSacarLista, alMoverOrdenLista,
                                ArrastreLista(
                                    inicio = { p -> arrastrando = m; arrastre.inicio(p) }, mover = arrastre.mover, soltar = arrastre.soltar, cancelar = arrastre.cancelar,
                                ),
                            )
                        }
                    }
                }

                item { HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 28.dp)) }
                item {
                    FilaMenu("Completadas", Icons.Filled.CheckCircle, estado.elemento == Elemento.Completadas, 0) { alElegir(Elemento.Completadas) }
                }
                item {
                    NavigationDrawerItem(
                        label = { Text("Ajustes") }, icon = { Icon(Icons.Filled.Settings, null) }, selected = false, onClick = alAjustes,
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }

            // La lista que se lleva con el dedo.
            arrastrando?.let { m ->
                val px = LocalDensity.current
                Surface(
                    shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 8.dp,
                    modifier = Modifier.offset { IntOffset((dedo.x - raiz.x - 60 * px.density).toInt(), (dedo.y - raiz.y - 24 * px.density).toInt()) },
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(m.emoji ?: "📋", fontSize = 18.sp)
                        Text(m.nombre, Modifier.padding(start = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/** Apunta dónde está en pantalla una fila y a qué carpeta pertenece, para saber sobre qué se suelta una lista. */
@Composable
private fun Modifier.registrar(clave: String, destino: Destino, limites: MutableMap<String, Pair<Rect, Destino>>): Modifier {
    DisposableEffect(clave) { onDispose { limites.remove(clave) } }
    return onGloballyPositioned { limites[clave] = it.boundsInRoot() to destino }
}

/** Una lista dentro del menú (suelta o dentro de una carpeta). */
@Composable
private fun FilaDeLista(
    m: Materia, pendientes: Int, estado: TareasEstado, mostrarCuenta: Boolean, compacta: Boolean, sangria: Dp,
    destino: Destino, limites: MutableMap<String, Pair<Rect, Destino>>, arrastrada: Boolean,
    alElegir: (Elemento) -> Unit, alEditar: (Materia) -> Unit, alEliminar: (Materia) -> Unit,
    alMover: (Materia) -> Unit, alSacar: (Materia) -> Unit, alMoverOrden: (Materia, Int) -> Unit, arrastre: ArrastreLista,
) {
    val e = Elemento.DeMateria(m.id)
    FilaLista(
        clave = "l-${m.id}", destino = destino, limites = limites,
        titulo = m.nombre, compacta = compacta, sangria = sangria, seleccionado = estado.elemento == e, resaltada = false, atenuada = arrastrada,
        cuenta = pendientes, mostrarCuenta = mostrarCuenta, alClic = { alElegir(e) }, inicio = null,
        icono = { IconoLista(m, estado.aspecto) }, arrastre = arrastre,
        menu = { cerrar ->
            DropdownMenuItem(text = { Text("Editar (nombre y emoji)") }, onClick = { cerrar(); alEditar(m) })
            DropdownMenuItem(text = { Text("Mover a una carpeta…") }, onClick = { cerrar(); alMover(m) })
            if (m.grupoId != null) DropdownMenuItem(text = { Text("Sacar de la carpeta") }, onClick = { cerrar(); alSacar(m) })
            DropdownMenuItem(text = { Text("Subir") }, onClick = { cerrar(); alMoverOrden(m, -1) })
            DropdownMenuItem(text = { Text("Bajar") }, onClick = { cerrar(); alMoverOrden(m, 1) })
            DropdownMenuItem(text = { Text("Eliminar lista", color = MaterialTheme.colorScheme.error) }, onClick = { cerrar(); alEliminar(m) })
        },
    )
}

/**
 * El icono de una lista: su emoji si tiene; si no, según el aspecto del menú, un punto de color (aspecto «color») o la
 * carpeta de siempre.
 */
@Composable
private fun IconoLista(m: Materia, aspecto: String) {
    when {
        m.emoji != null -> Text(m.emoji, fontSize = 20.sp)
        aspecto == "color" -> Box(Modifier.size(14.dp).clip(CircleShape).background(Pasteles.colores[Pasteles.indicePreferido(m.id)].fondo))
        else -> Icon(Icons.Filled.Folder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Fila del menú de listas y carpetas. En el aspecto compacto es más baja; [inicio] es el botón de plegar de las carpetas y
 * [sangria] mete las listas que están dentro de una carpeta. Si tiene [arrastre], mantenerla pulsada permite moverla.
 */
@Composable
private fun FilaLista(
    clave: String, destino: Destino, limites: MutableMap<String, Pair<Rect, Destino>>,
    titulo: String, compacta: Boolean, sangria: Dp, seleccionado: Boolean, cuenta: Int, mostrarCuenta: Boolean,
    alClic: () -> Unit, inicio: (@Composable () -> Unit)?, icono: @Composable () -> Unit, arrastre: ArrastreLista?,
    menu: @Composable (cerrar: () -> Unit) -> Unit,
    resaltada: Boolean = false, atenuada: Boolean = false,
) {
    var abierto by remember { mutableStateOf(false) }
    var origen by remember { mutableStateOf(Offset.Zero) }
    val accion = rememberUpdatedState(arrastre)
    val relleno = NavigationDrawerItemDefaults.ItemPadding
    val fondo = when {
        resaltada -> MaterialTheme.colorScheme.primaryContainer
        seleccionado -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = fondo,
        modifier = Modifier.padding(relleno).padding(start = sangria).fillMaxWidth().height(if (compacta) 40.dp else 52.dp)
            .registrar(clave, destino, limites)
            .onGloballyPositioned { origen = it.positionInRoot() }
            .clip(RoundedCornerShape(28.dp)).clickable(onClick = alClic)
            .then(
                if (arrastre == null) Modifier else Modifier.pointerInput(clave) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { p -> accion.value?.inicio?.invoke(origen + p) },
                        onDrag = { cambio, d -> cambio.consume(); accion.value?.mover?.invoke(d) },
                        onDragEnd = { accion.value?.soltar?.invoke() },
                        onDragCancel = { accion.value?.cancelar?.invoke() },
                    )
                },
            ),
    ) {
        Row(
            Modifier.padding(start = if (inicio == null) 16.dp else 4.dp, end = 2.dp).then(if (atenuada) Modifier.background(Color.Transparent) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            inicio?.invoke()
            Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) { icono() }
            Spacer(Modifier.width(10.dp))
            Text(
                titulo, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = if (compacta) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelLarge,
                color = if (atenuada) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurface,
            )
            if (mostrarCuenta && cuenta > 0) Text(cuenta.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box {
                IconButton(onClick = { abierto = true }, Modifier.size(36.dp)) { Icon(Icons.Filled.MoreVert, "Opciones de $titulo") }
                DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) { menu { abierto = false } }
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
        title = { Text("Mover «${m.nombre}»", maxLines = 2) },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                OpcionCarpeta("Sin carpeta", m.grupoId == null) { alElegir(null); alCerrar() }
                for (g in grupos) OpcionCarpeta("${g.icono} ${g.nombre}", m.grupoId == g.id) { alElegir(g.id); alCerrar() }
                TextButton(onClick = { nueva = true }) { Text("＋ Nueva carpeta…") }
            }
        },
        confirmButton = { TextButton(onClick = alCerrar) { Text("Cerrar") } },
    )
    if (nueva) DialogoNuevoElemento("Nueva carpeta", "Nombre de la carpeta", accion = "Crear y mover", alCerrar = { nueva = false }) { nombre, emoji ->
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
