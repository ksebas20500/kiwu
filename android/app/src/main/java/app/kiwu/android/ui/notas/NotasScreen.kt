package app.kiwu.android.ui.notas

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.data.ResumenPagina
import app.kiwu.android.domain.Cuaderno
import app.kiwu.android.ui.DialogoConfirmar
import app.kiwu.android.ui.EstadoVacio
import app.kiwu.android.ui.Fechas
import app.kiwu.android.ui.fechaCorta
import kotlinx.coroutines.launch
import app.kiwu.android.R
import androidx.compose.ui.res.stringResource

val EmojisCuaderno = listOf("📓", "📝", "📚", "📖", "🎓", "💡", "🧪", "🧮", "💻", "🎨", "🎵", "🌱", "🗂️", "⭐", "🔥", "✅", "📌", "🧠", "🏃", "🍎")

/** Módulo de notas: cuadernos (filtros), búsqueda y lista de páginas. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotasScreen(vm: NotasViewModel, alAbrirPagina: (String) -> Unit, alAbrirMenu: () -> Unit, insigniaRacha: @Composable () -> Unit = {}) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val alcance = rememberCoroutineScope()
    var buscando by remember { mutableStateOf(false) }
    var dialogoCuaderno by remember { mutableStateOf<Cuaderno?>(null) }
    var nuevoCuaderno by remember { mutableStateOf(false) }
    var eliminarCuaderno by remember { mutableStateOf<Cuaderno?>(null) }
    var menuCuaderno by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (buscando) {
                        OutlinedTextField(
                            value = estado.busqueda, onValueChange = vm::buscar, singleLine = true, modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.notas_buscar_en)) },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Search),
                        )
                    } else {
                        Text(
                            when (val f = estado.filtro) {
                                FiltroNotas.Todas -> stringResource(R.string.notas)
                                FiltroNotas.Favoritas -> stringResource(R.string.favoritas)
                                is FiltroNotas.DeCuaderno -> estado.cuadernoActual?.let { "${it.icono} ${it.nombre}" } ?: stringResource(R.string.notas)
                            },
                        )
                    }
                },
                navigationIcon = {
                    if (buscando) {
                        IconButton(onClick = { buscando = false; vm.buscar("") }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cerrar_busqueda)) }
                    } else {
                        IconButton(onClick = alAbrirMenu) { Icon(Icons.Filled.Menu, stringResource(R.string.abrir_menu)) }
                    }
                },
                actions = {
                    // Al buscar, el campo de búsqueda necesita el sitio: la racha se esconde.
                    if (!buscando) insigniaRacha()
                    if (!buscando) IconButton(onClick = { buscando = true }) { Icon(Icons.Filled.Search, stringResource(R.string.buscar)) }
                    if (estado.cuadernoActual != null && !buscando) {
                        Box {
                            IconButton(onClick = { menuCuaderno = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.cuaderno_opciones)) }
                            DropdownMenu(expanded = menuCuaderno, onDismissRequest = { menuCuaderno = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.cuaderno_editar)) }, onClick = { menuCuaderno = false; dialogoCuaderno = estado.cuadernoActual })
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.cuaderno_eliminar), color = MaterialTheme.colorScheme.error) },
                                    onClick = { menuCuaderno = false; eliminarCuaderno = estado.cuadernoActual },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { alcance.launch { alAbrirPagina(vm.nuevaPagina()) } }) { Icon(Icons.Filled.Add, stringResource(R.string.nota_nueva)) }
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { relleno ->
        Column(Modifier.padding(relleno).consumeWindowInsets(relleno).fillMaxSize()) {
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(estado.filtro == FiltroNotas.Todas, { vm.filtrar(FiltroNotas.Todas) }, { Text(stringResource(R.string.todas)) }) }
                item { FilterChip(estado.filtro == FiltroNotas.Favoritas, { vm.filtrar(FiltroNotas.Favoritas) }, { Text(stringResource(R.string.favoritas_estrella)) }) }
                items(estado.cuadernos, key = { it.id }) { c ->
                    FilterChip(estado.filtro == FiltroNotas.DeCuaderno(c.id), { vm.filtrar(FiltroNotas.DeCuaderno(c.id)) }, { Text("${c.icono} ${c.nombre}") })
                }
                item { TextButton(onClick = { nuevoCuaderno = true }) { Text(stringResource(R.string.cuaderno_mas)) } }
            }
            if (estado.cargado && estado.paginas.isEmpty()) {
                EstadoVacio(
                    Icons.Filled.Description,
                    stringResource(if (estado.busqueda.isNotBlank()) R.string.sin_resultados else R.string.sin_notas),
                )
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(estado.paginas, key = { it.id }) { p ->
                        FilaPagina(p, estado.nombresCuadernos[p.cuadernoId], estado.cuadernos, vm, alAbrirPagina)
                    }
                }
            }
        }
    }

    if (nuevoCuaderno) {
        DialogoCuaderno(null, alCerrar = { nuevoCuaderno = false }) { nombre, icono -> vm.crearCuaderno(nombre, icono) }
    }
    dialogoCuaderno?.let { c ->
        DialogoCuaderno(c, alCerrar = { dialogoCuaderno = null }) { nombre, icono -> vm.editarCuaderno(c.id, nombre, icono) }
    }
    eliminarCuaderno?.let { c ->
        DialogoConfirmar(
            stringResource(R.string.cuaderno_eliminar_titulo, c.nombre), stringResource(R.string.cuaderno_eliminar_texto), stringResource(R.string.cuaderno_eliminar_boton),
            alCerrar = { eliminarCuaderno = null },
        ) { vm.eliminarCuaderno(c.id) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FilaPagina(p: ResumenPagina, cuaderno: String?, cuadernos: List<Cuaderno>, vm: NotasViewModel, alAbrir: (String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var mover by remember { mutableStateOf(false) }
    var eliminar by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = { alAbrir(p.id) }, onLongClick = { menu = true })
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(p.icono.ifBlank { "📄" }, fontSize = 24.sp, modifier = Modifier.padding(end = 14.dp))
        Column(Modifier.weight(1f)) {
            Text(p.titulo.ifBlank { stringResource(R.string.sin_titulo) }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val resumen = p.textoPlano.replace('\n', ' ').take(90)
            if (resumen.isNotBlank()) Text(resumen, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text(
                listOfNotNull(cuaderno, fechaCorta(p.actualizada, System.currentTimeMillis())).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
        IconButton(onClick = { vm.ponerFavorita(p.id, !p.favorita) }) {
            Icon(if (p.favorita) Icons.Filled.Star else Icons.Filled.StarBorder, stringResource(if (p.favorita) R.string.quitar_favoritas else R.string.anadir_favoritas),
                tint = if (p.favorita) Color(0xFFE0A100) else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.nota_opciones)) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(if (p.favorita) R.string.quitar_favoritas else R.string.anadir_favoritas)) }, onClick = { menu = false; vm.ponerFavorita(p.id, !p.favorita) })
                if (cuadernos.size > 1) DropdownMenuItem(text = { Text(stringResource(R.string.mover_otro_cuaderno)) }, onClick = { menu = false; mover = true })
                DropdownMenuItem(text = { Text(stringResource(R.string.nota_eliminar), color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; eliminar = true })
            }
        }
    }
    if (mover) {
        AlertDialog(
            onDismissRequest = { mover = false }, title = { Text(stringResource(R.string.mover_a)) },
            text = {
                Column {
                    for (c in cuadernos.filter { it.id != p.cuadernoId }) {
                        TextButton(onClick = { mover = false; vm.moverPagina(p.id, c.id) }) { Text("${c.icono} ${c.nombre}") }
                    }
                }
            },
            confirmButton = {}, dismissButton = { TextButton(onClick = { mover = false }) { Text(stringResource(R.string.cancelar)) } },
        )
    }
    if (eliminar) {
        DialogoConfirmar(stringResource(R.string.nota_eliminar_titulo), stringResource(R.string.no_se_recupera), stringResource(R.string.eliminar), alCerrar = { eliminar = false }) { vm.eliminarPagina(p.id) }
    }
}

/** Crear o editar un cuaderno: nombre e icono (emoji). */
@Composable
fun DialogoCuaderno(cuaderno: Cuaderno?, alCerrar: () -> Unit, alConfirmar: (String, String) -> Unit) {
    var nombre by remember { mutableStateOf(cuaderno?.nombre ?: "") }
    var icono by remember { mutableStateOf(cuaderno?.icono ?: "📓") }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(if (cuaderno == null) R.string.cuaderno_nuevo else R.string.cuaderno_editar)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    nombre, { nombre = it }, label = { Text(stringResource(R.string.nombre)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Text(stringResource(R.string.horario_icono), style = MaterialTheme.typography.labelMedium)
                SelectorEmoji(icono) { icono = it }
            }
        },
        confirmButton = { TextButton(onClick = { alConfirmar(nombre.trim(), icono); alCerrar() }, enabled = nombre.isNotBlank()) { Text(stringResource(R.string.guardar)) } },
        dismissButton = { TextButton(onClick = alCerrar) { Text(stringResource(R.string.cancelar)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SelectorEmoji(actual: String, emojis: List<String> = EmojisCuaderno, alElegir: (String) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (e in emojis) {
            Surface(
                onClick = { alElegir(e) }, shape = RoundedCornerShape(8.dp),
                color = if (e == actual) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            ) { Text(e, fontSize = 22.sp, modifier = Modifier.padding(6.dp)) }
        }
    }
}

/** Menú lateral de Notas: filtros, cuadernos (con su menú) y ajustes. */
@Composable
private fun PanelNotas(
    estado: NotasEstado,
    alElegir: (FiltroNotas) -> Unit,
    alNuevoCuaderno: () -> Unit,
    alEditar: (Cuaderno) -> Unit,
    alEliminar: (Cuaderno) -> Unit,
    alAjustes: () -> Unit,
) {
    androidx.compose.material3.ModalDrawerSheet(drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)) {
        androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 20.dp, bottom = 16.dp)) {
            item { app.kiwu.android.ui.tareas.CabeceraMenu(stringResource(R.string.notas), stringResource(R.string.notas_menu_sub)) }
            item { ItemPanel(stringResource(R.string.todas_notas), Icons.Filled.Description, estado.filtro == FiltroNotas.Todas) { alElegir(FiltroNotas.Todas) } }
            item { ItemPanel(stringResource(R.string.favoritas), Icons.Filled.Star, estado.filtro == FiltroNotas.Favoritas) { alElegir(FiltroNotas.Favoritas) } }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 28.dp, end = 12.dp, top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.cuadernos), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick = alNuevoCuaderno) { Icon(Icons.Filled.Add, stringResource(R.string.cuaderno_nuevo)) }
                }
            }
            items(estado.cuadernos, key = { it.id }) { c ->
                var menu by remember { mutableStateOf(false) }
                androidx.compose.material3.NavigationDrawerItem(
                    label = { Text("${c.icono} ${c.nombre}", maxLines = 1) },
                    selected = estado.filtro == FiltroNotas.DeCuaderno(c.id),
                    onClick = { alElegir(FiltroNotas.DeCuaderno(c.id)) },
                    badge = {
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.opciones_de, c.nombre)) }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.cuaderno_editar)) }, onClick = { menu = false; alEditar(c) })
                                DropdownMenuItem(text = { Text(stringResource(R.string.cuaderno_eliminar), color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; alEliminar(c) })
                            }
                        }
                    },
                    modifier = Modifier.padding(androidx.compose.material3.NavigationDrawerItemDefaults.ItemPadding),
                )
            }
            item { androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 28.dp)) }
            item {
                androidx.compose.material3.NavigationDrawerItem(
                    label = { Text(stringResource(R.string.ajustes)) }, icon = { Icon(Icons.Filled.Settings, null) }, selected = false, onClick = alAjustes,
                    modifier = Modifier.padding(androidx.compose.material3.NavigationDrawerItemDefaults.ItemPadding),
                )
            }
        }
    }
}

@Composable
private fun ItemPanel(titulo: String, icono: androidx.compose.ui.graphics.vector.ImageVector, seleccionado: Boolean, alClic: () -> Unit) {
    androidx.compose.material3.NavigationDrawerItem(
        label = { Text(titulo, maxLines = 1) }, icon = { Icon(icono, null) }, selected = seleccionado, onClick = alClic,
        modifier = Modifier.padding(androidx.compose.material3.NavigationDrawerItemDefaults.ItemPadding),
    )
}

/** Menú lateral de Notas. Se dibuja sobre toda la pantalla (también sobre la barra inferior). */
@Composable
fun MenuNotas(vm: NotasViewModel, cerrarYLuego: (() -> Unit) -> Unit, alAjustes: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var nuevo by remember { mutableStateOf(false) }
    var editar by remember { mutableStateOf<Cuaderno?>(null) }
    var eliminar by remember { mutableStateOf<Cuaderno?>(null) }
    PanelNotas(
        estado = estado,
        alElegir = { f -> cerrarYLuego { vm.filtrar(f) } },
        alNuevoCuaderno = { nuevo = true },
        alEditar = { editar = it },
        alEliminar = { eliminar = it },
        alAjustes = { cerrarYLuego(alAjustes) },
    )
    if (nuevo) DialogoCuaderno(null, alCerrar = { nuevo = false }) { nombre, icono -> vm.crearCuaderno(nombre, icono) }
    editar?.let { c -> DialogoCuaderno(c, alCerrar = { editar = null }) { nombre, icono -> vm.editarCuaderno(c.id, nombre, icono) } }
    eliminar?.let { c ->
        DialogoConfirmar(
            stringResource(R.string.cuaderno_eliminar_titulo, c.nombre), stringResource(R.string.cuaderno_eliminar_texto), stringResource(R.string.cuaderno_eliminar_boton),
            alCerrar = { eliminar = null },
        ) { vm.eliminarCuaderno(c.id) }
    }
}
