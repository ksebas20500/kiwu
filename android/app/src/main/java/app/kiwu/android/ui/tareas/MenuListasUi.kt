package app.kiwu.android.ui.tareas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.domain.Elemento
import app.kiwu.android.domain.GrupoLista
import app.kiwu.android.domain.Materia
import app.kiwu.android.domain.NodoMenu
import app.kiwu.android.domain.ReglasHorario
import app.kiwu.android.ui.DialogoConfirmar
import app.kiwu.android.ui.DialogoTexto
import app.kiwu.android.ui.Pasteles
import app.kiwu.android.ui.ajustes.DialogoColor
import app.kiwu.android.ui.horario.SelectorColorClase
import app.kiwu.android.ui.notas.SelectorEmoji

/** Emojis para ponerle a una lista o a una carpeta. */
val EmojisListas = listOf(
    "📁", "📚", "🎓", "💼", "🧪", "📐", "💻", "🎨", "🎵", "🌍", "🏋️", "🏠", "🛒", "✈️", "💡", "📝", "⭐", "❤️", "🔥", "🌱", "🧠", "⚽", "🎮", "🍽️",
)

/** Qué se está editando con el cuadro de emoji y color: una lista o una carpeta. */
private sealed interface Aspecto {
    data class DeLista(val materia: Materia) : Aspecto
    data class DeCarpeta(val grupo: GrupoLista) : Aspecto
}

/** Menú lateral de Tareas. Se dibuja sobre toda la pantalla (también sobre la barra inferior). */
@Composable
fun MenuTareas(vm: TareasViewModel, cerrarYLuego: (() -> Unit) -> Unit, alAjustes: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var nuevaLista by remember { mutableStateOf(false) }
    var nuevaCarpeta by remember { mutableStateOf(false) }
    var renombrarLista by remember { mutableStateOf<Materia?>(null) }
    var eliminarLista by remember { mutableStateOf<Materia?>(null) }
    var renombrarCarpeta by remember { mutableStateOf<GrupoLista?>(null) }
    var eliminarCarpeta by remember { mutableStateOf<GrupoLista?>(null) }
    var aspecto by remember { mutableStateOf<Aspecto?>(null) }
    var moverLista by remember { mutableStateOf<Materia?>(null) }

    PanelLateral(
        estado = estado,
        alElegir = { e -> cerrarYLuego { vm.ir(e) } },
        alNuevaLista = { nuevaLista = true }, alNuevaCarpeta = { nuevaCarpeta = true },
        alPlegar = { g, plegado -> vm.plegarGrupo(g.id, plegado) },
        alRenombrarLista = { renombrarLista = it }, alEliminarLista = { eliminarLista = it },
        alAspectoLista = { aspecto = Aspecto.DeLista(it) }, alMoverLista = { moverLista = it },
        alSacarLista = { vm.moverAGrupo(it.id, null) }, alMoverOrdenLista = { m, d -> vm.moverOrdenLista(m.id, d) },
        alRenombrarCarpeta = { renombrarCarpeta = it }, alAspectoCarpeta = { aspecto = Aspecto.DeCarpeta(it) },
        alEliminarCarpeta = { eliminarCarpeta = it }, alMoverOrdenCarpeta = { g, d -> vm.moverOrdenGrupo(g.id, d) },
        alAjustes = { cerrarYLuego(alAjustes) },
    )

    if (nuevaLista) DialogoTexto("Nueva lista", "Nombre de la materia", accion = "Crear", alCerrar = { nuevaLista = false }) { vm.crearLista(it) }
    if (nuevaCarpeta) DialogoTexto("Nueva carpeta", "Nombre de la carpeta", accion = "Crear", alCerrar = { nuevaCarpeta = false }) { vm.crearGrupo(it) }
    renombrarLista?.let { m -> DialogoTexto("Renombrar lista", "Nombre", inicial = m.nombre, alCerrar = { renombrarLista = null }) { vm.renombrarLista(m.id, it) } }
    renombrarCarpeta?.let { g -> DialogoTexto("Renombrar carpeta", "Nombre", inicial = g.nombre, alCerrar = { renombrarCarpeta = null }) { vm.editarGrupo(g.id, it, g.icono, g.color) } }
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
    aspecto?.let { a ->
        val (titulo, emoji, color) = when (a) {
            is Aspecto.DeLista -> Triple("Emoji y color de ${a.materia.nombre}", a.materia.emoji, a.materia.color)
            is Aspecto.DeCarpeta -> Triple("Emoji y color de ${a.grupo.nombre}", a.grupo.icono, a.grupo.color)
        }
        DialogoAspecto(titulo, emoji, color, alCerrar = { aspecto = null }) { nuevoEmoji, nuevoColor ->
            when (a) {
                is Aspecto.DeLista -> vm.editarAspectoLista(a.materia.id, nuevoEmoji, nuevoColor)
                is Aspecto.DeCarpeta -> vm.editarGrupo(a.grupo.id, a.grupo.nombre, nuevoEmoji ?: "📁", nuevoColor)
            }
        }
    }
    moverLista?.let { m ->
        DialogoMoverLista(m, estado.grupos, alCerrar = { moverLista = null }, alElegir = { vm.moverAGrupo(m.id, it) }, alNuevaCarpeta = { nombre -> vm.crearGrupoYMover(nombre, m.id) })
    }
}

@Composable
private fun PanelLateral(
    estado: TareasEstado,
    alElegir: (Elemento) -> Unit,
    alNuevaLista: () -> Unit,
    alNuevaCarpeta: () -> Unit,
    alPlegar: (GrupoLista, Boolean) -> Unit,
    alRenombrarLista: (Materia) -> Unit,
    alEliminarLista: (Materia) -> Unit,
    alAspectoLista: (Materia) -> Unit,
    alMoverLista: (Materia) -> Unit,
    alSacarLista: (Materia) -> Unit,
    alMoverOrdenLista: (Materia, Int) -> Unit,
    alRenombrarCarpeta: (GrupoLista) -> Unit,
    alAspectoCarpeta: (GrupoLista) -> Unit,
    alEliminarCarpeta: (GrupoLista) -> Unit,
    alMoverOrdenCarpeta: (GrupoLista, Int) -> Unit,
    alAjustes: () -> Unit,
) {
    val compacto = estado.aspecto == "compacto"
    val mostrarCuenta = estado.contadores && !compacto
    var menuNueva by remember { mutableStateOf(false) }

    ModalDrawerSheet(drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 20.dp, bottom = 16.dp)) {
            item { CabeceraMenu("Kiwu", "Tareas y listas") }
            val fijos = listOf(
                Triple(Elemento.Hoy, "Hoy", Icons.Filled.Today),
                Triple(Elemento.Proximos, "Próximos 7 días", Icons.Filled.DateRange),
                Triple(Elemento.Calendario, "Calendario", Icons.Filled.CalendarMonth),
                Triple(Elemento.Todos, "Todas las tareas", Icons.Filled.ViewList),
            )
            items(fijos) { (e, titulo, icono) -> FilaMenu(titulo, icono, estado.elemento == e, estado.cuentas[e] ?: 0) { alElegir(e) } }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 28.dp, end = 12.dp, top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Listas", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                titulo = g.nombre, compacta = compacto, sangria = 0.dp,
                                seleccionado = estado.elemento == Elemento.DeGrupo(g.id),
                                cuenta = nodo.pendientes, mostrarCuenta = mostrarCuenta, alClic = { alElegir(Elemento.DeGrupo(g.id)) },
                                inicio = {
                                    IconButton(onClick = { alPlegar(g, !g.plegado) }, Modifier.size(36.dp)) {
                                        Icon(if (g.plegado) Icons.Filled.ChevronRight else Icons.Filled.ExpandMore, if (g.plegado) "Desplegar ${g.nombre}" else "Plegar ${g.nombre}")
                                    }
                                },
                                icono = { Text(g.icono, fontSize = 20.sp, color = colorDe(g.color, Color.Unspecified)) },
                                menu = { cerrar ->
                                    DropdownMenuItem(text = { Text("Renombrar carpeta") }, onClick = { cerrar(); alRenombrarCarpeta(g) })
                                    DropdownMenuItem(text = { Text("Emoji y color") }, onClick = { cerrar(); alAspectoCarpeta(g) })
                                    DropdownMenuItem(text = { Text("Subir") }, onClick = { cerrar(); alMoverOrdenCarpeta(g, -1) })
                                    DropdownMenuItem(text = { Text("Bajar") }, onClick = { cerrar(); alMoverOrdenCarpeta(g, 1) })
                                    DropdownMenuItem(text = { Text("Eliminar carpeta", color = MaterialTheme.colorScheme.error) }, onClick = { cerrar(); alEliminarCarpeta(g) })
                                },
                            )
                        }
                        if (!g.plegado) {
                            items(nodo.listas, key = { "l-${it.materia.id}" }) { e ->
                                FilaDeLista(e.materia, e.pendientes, estado, mostrarCuenta, compacto, sangria = 20.dp, alElegir, alRenombrarLista, alEliminarLista, alAspectoLista, alMoverLista, alSacarLista, alMoverOrdenLista)
                            }
                        }
                    }
                    is NodoMenu.Lista -> item(key = "l-${nodo.entrada.materia.id}") {
                        FilaDeLista(nodo.entrada.materia, nodo.entrada.pendientes, estado, mostrarCuenta, compacto, sangria = 0.dp, alElegir, alRenombrarLista, alEliminarLista, alAspectoLista, alMoverLista, alSacarLista, alMoverOrdenLista)
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
    }
}

/** Una lista dentro del menú (suelta o dentro de una carpeta). */
@Composable
private fun FilaDeLista(
    m: Materia, pendientes: Int, estado: TareasEstado, mostrarCuenta: Boolean, compacta: Boolean, sangria: Dp,
    alElegir: (Elemento) -> Unit, alRenombrar: (Materia) -> Unit, alEliminar: (Materia) -> Unit, alAspecto: (Materia) -> Unit,
    alMover: (Materia) -> Unit, alSacar: (Materia) -> Unit, alMoverOrden: (Materia, Int) -> Unit,
) {
    val e = Elemento.DeMateria(m.id)
    FilaLista(
        titulo = m.nombre, compacta = compacta, sangria = sangria, seleccionado = estado.elemento == e,
        cuenta = pendientes, mostrarCuenta = mostrarCuenta, alClic = { alElegir(e) }, inicio = null,
        icono = { IconoLista(m, estado.aspecto) },
        menu = { cerrar ->
            DropdownMenuItem(text = { Text("Renombrar lista") }, onClick = { cerrar(); alRenombrar(m) })
            DropdownMenuItem(text = { Text("Emoji y color") }, onClick = { cerrar(); alAspecto(m) })
            DropdownMenuItem(text = { Text("Mover a una carpeta…") }, onClick = { cerrar(); alMover(m) })
            if (m.grupoId != null) DropdownMenuItem(text = { Text("Sacar de la carpeta") }, onClick = { cerrar(); alSacar(m) })
            DropdownMenuItem(text = { Text("Subir") }, onClick = { cerrar(); alMoverOrden(m, -1) })
            DropdownMenuItem(text = { Text("Bajar") }, onClick = { cerrar(); alMoverOrden(m, 1) })
            DropdownMenuItem(text = { Text("Eliminar lista", color = MaterialTheme.colorScheme.error) }, onClick = { cerrar(); alEliminar(m) })
        },
    )
}

private fun colorDe(hex: String?, porDefecto: Color): Color =
    ReglasHorario.colorValido(hex)?.let { Color(android.graphics.Color.parseColor(it)) } ?: porDefecto

/**
 * El icono de una lista: su emoji si tiene; si no, según el aspecto del menú, un punto de color (aspecto «color», con el color
 * elegido o uno automático) o la carpeta de siempre (teñida si tiene color).
 */
@Composable
private fun IconoLista(m: Materia, aspecto: String) {
    when {
        m.emoji != null -> Text(m.emoji, fontSize = 20.sp)
        aspecto == "color" -> {
            val automatico = Pasteles.colores[Pasteles.indicePreferido(m.id)].fondo
            Box(Modifier.size(14.dp).clip(CircleShape).background(colorDe(m.color, automatico)))
        }
        else -> Icon(Icons.Filled.Folder, null, tint = colorDe(m.color, MaterialTheme.colorScheme.onSurfaceVariant))
    }
}

/**
 * Fila del menú de listas y carpetas. En el aspecto compacto es más baja; [inicio] es el botón de plegar de las carpetas y
 * [sangria] mete las listas que están dentro de una carpeta.
 */
@Composable
private fun FilaLista(
    titulo: String, compacta: Boolean, sangria: Dp, seleccionado: Boolean, cuenta: Int, mostrarCuenta: Boolean,
    alClic: () -> Unit, inicio: (@Composable () -> Unit)?, icono: @Composable () -> Unit,
    menu: @Composable (cerrar: () -> Unit) -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }
    val relleno = NavigationDrawerItemDefaults.ItemPadding
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = if (seleccionado) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        modifier = Modifier.padding(relleno).padding(start = sangria).fillMaxWidth().height(if (compacta) 40.dp else 52.dp).clip(RoundedCornerShape(28.dp)).clickable(onClick = alClic),
    ) {
        Row(Modifier.padding(start = if (inicio == null) 16.dp else 4.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            inicio?.invoke()
            Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) { icono() }
            Spacer(Modifier.width(10.dp))
            Text(titulo, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = if (compacta) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelLarge)
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

/** Cuadro para elegir el emoji y el color de una lista o carpeta. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoAspecto(titulo: String, emoji: String?, color: String?, alCerrar: () -> Unit, alGuardar: (String?, String?) -> Unit) {
    var emojiElegido by remember { mutableStateOf(emoji) }
    var colorElegido by remember { mutableStateOf(ReglasHorario.colorValido(color)) }
    var otroColor by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(titulo, maxLines = 2) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Emoji", style = MaterialTheme.typography.labelMedium)
                SelectorEmoji(emojiElegido ?: "", EmojisListas) { emojiElegido = it }
                TextButton(onClick = { emojiElegido = null }) { Text("Sin emoji (icono de siempre)") }
                Text("Color", style = MaterialTheme.typography.labelMedium)
                SelectorColorClase(colorElegido, alElegir = { colorElegido = it }, alOtro = { otroColor = true }, textoAutomatico = "Automático")
            }
        },
        confirmButton = { TextButton(onClick = { alGuardar(emojiElegido, colorElegido); alCerrar() }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )
    if (otroColor) {
        DialogoColor(
            inicial = colorElegido?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color(0xFF8E24AA),
            alCerrar = { otroColor = false },
            alElegir = { c -> colorElegido = "#%06X".format(c.toArgb() and 0xFFFFFF) },
        )
    }
}

/** Cuadro para mover una lista a una carpeta (o dejarla suelta, o crear una carpeta nueva con ella). */
@Composable
private fun DialogoMoverLista(
    m: Materia, grupos: List<GrupoLista>, alCerrar: () -> Unit, alElegir: (String?) -> Unit, alNuevaCarpeta: (String) -> Unit,
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
    if (nueva) DialogoTexto("Nueva carpeta", "Nombre de la carpeta", accion = "Crear y mover", alCerrar = { nueva = false }) { alNuevaCarpeta(it); alCerrar() }
}

@Composable
private fun OpcionCarpeta(texto: String, marcada: Boolean, alPulsar: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = alPulsar), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(marcada, onClick = alPulsar)
        Text(texto, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
