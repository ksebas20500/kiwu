package app.kiwu.android.ui.tareas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.domain.Elemento
import app.kiwu.android.domain.Materia
import app.kiwu.android.ui.DialogoConfirmar
import app.kiwu.android.ui.DialogoTexto
import app.kiwu.android.ui.calendario.CalendarioScreen
import kotlinx.coroutines.launch

private fun tituloDe(e: Elemento, materia: Materia?): String = when (e) {
    Elemento.Hoy -> "Hoy"
    Elemento.Proximos -> "Próximos 7 días"
    Elemento.Calendario -> "Calendario"
    Elemento.Todos -> "Todas las tareas"
    Elemento.Completadas -> "Completadas"
    is Elemento.DeMateria -> materia?.nombre ?: "Lista"
}

/** Módulo de tareas: lista, tablero o calendario (+ archivos de la lista). El menú lateral vive en la pantalla principal. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TareasScreen(
    vm: TareasViewModel,
    alAbrirTarea: (String) -> Unit,
    alAbrirMenu: () -> Unit,
    archivos: @Composable (Materia) -> Unit,
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    // El tablero solo tiene sentido en pantallas anchas: en el teléfono siempre se ve la lista.
    val anchaParaTablero = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    val verTablero = anchaParaTablero && estado.vista == "tablero"
    var dialogoNuevaLista by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tituloDe(estado.elemento, estado.materiaActual)) },
                navigationIcon = { IconButton(onClick = alAbrirMenu) { Icon(Icons.Filled.Menu, "Abrir menú") } },
                actions = {
                    if (estado.elemento != Elemento.Calendario && anchaParaTablero) {
                        val tablero = verTablero
                        IconButton(onClick = { vm.cambiarVista(if (tablero) "lista" else "tablero") }) {
                            Icon(
                                if (tablero) Icons.Filled.ViewList else Icons.Filled.ViewKanban,
                                if (tablero) "Ver como lista" else "Ver como tablero",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f)),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { relleno ->
        Column(Modifier.padding(relleno).consumeWindowInsets(relleno).fillMaxSize()) {
            val materia = estado.materiaActual
            if (materia != null) {
                TabRow(
                    selectedTabIndex = if (estado.subvista == "archivos") 1 else 0,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Tab(selected = estado.subvista != "archivos", onClick = { vm.cambiarSubvista("tareas") }, text = { Text("Tareas") })
                    Tab(selected = estado.subvista == "archivos", onClick = { vm.cambiarSubvista("archivos") }, text = { Text("Archivos") })
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    estado.elemento == Elemento.Calendario -> CalendarioScreen(vm, estado, alAbrirTarea)
                    materia != null && estado.subvista == "archivos" -> archivos(materia)
                    verTablero -> Tablero(vm, estado, alAbrirTarea) { dialogoNuevaLista = true }
                    else -> ListaTareas(vm, estado, alAbrirTarea) { dialogoNuevaLista = true }
                }
            }
        }
    }

    if (dialogoNuevaLista) {
        DialogoTexto("Nueva lista", "Nombre de la materia", accion = "Crear", alCerrar = { dialogoNuevaLista = false }) { vm.crearLista(it) }
    }
}

/** Menú lateral de Tareas. Se dibuja sobre toda la pantalla (también sobre la barra inferior). */
@Composable
fun MenuTareas(vm: TareasViewModel, cerrarYLuego: (() -> Unit) -> Unit, alAjustes: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var nuevaLista by remember { mutableStateOf(false) }
    var renombrar by remember { mutableStateOf<Materia?>(null) }
    var eliminar by remember { mutableStateOf<Materia?>(null) }

    PanelLateral(
        estado = estado,
        alElegir = { e -> cerrarYLuego { vm.ir(e) } },
        alNuevaLista = { nuevaLista = true },
        alRenombrar = { renombrar = it },
        alEliminar = { eliminar = it },
        alAjustes = { cerrarYLuego(alAjustes) },
    )
    if (nuevaLista) {
        DialogoTexto("Nueva lista", "Nombre de la materia", accion = "Crear", alCerrar = { nuevaLista = false }) { vm.crearLista(it) }
    }
    renombrar?.let { m ->
        DialogoTexto("Renombrar lista", "Nombre", inicial = m.nombre, alCerrar = { renombrar = null }) { vm.renombrarLista(m.id, it) }
    }
    eliminar?.let { m ->
        DialogoConfirmar(
            titulo = "¿Eliminar ${m.nombre}?",
            mensaje = "Se eliminarán también todas sus tareas. Los archivos de la carpeta vinculada no se tocan.",
            accion = "Eliminar lista y sus tareas",
            alCerrar = { eliminar = null },
        ) { vm.eliminarLista(m.id) }
    }
}

@Composable
private fun PanelLateral(
    estado: TareasEstado,
    alElegir: (Elemento) -> Unit,
    alNuevaLista: () -> Unit,
    alRenombrar: (Materia) -> Unit,
    alEliminar: (Materia) -> Unit,
    alAjustes: () -> Unit,
) {
    ModalDrawerSheet(drawerShape = androidx.compose.foundation.shape.RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 20.dp, bottom = 16.dp)) {
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
                    IconButton(onClick = alNuevaLista) { Icon(Icons.Filled.Add, "Nueva lista") }
                }
            }
            items(estado.materias, key = { it.id }) { m ->
                val e = Elemento.DeMateria(m.id)
                FilaMenu(
                    titulo = m.nombre, icono = Icons.Filled.Folder, seleccionado = estado.elemento == e,
                    cuenta = estado.cuentas[e] ?: 0, alClic = { alElegir(e) },
                    menu = { cerrar ->
                        DropdownMenuItem(text = { Text("Renombrar lista") }, onClick = { cerrar(); alRenombrar(m) })
                        DropdownMenuItem(text = { Text("Eliminar lista") }, onClick = { cerrar(); alEliminar(m) })
                    },
                )
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

@Composable
private fun FilaMenu(
    titulo: String,
    icono: ImageVector,
    seleccionado: Boolean,
    cuenta: Int,
    menu: (@Composable (cerrar: () -> Unit) -> Unit)? = null,
    alClic: () -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }
    NavigationDrawerItem(
        label = { Text(titulo, maxLines = 1) },
        icon = { Icon(icono, null) },
        selected = seleccionado,
        onClick = alClic,
        badge = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (cuenta > 0) Text(cuenta.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (menu != null) {
                    Box {
                        IconButton(onClick = { abierto = true }) { Icon(Icons.Filled.MoreVert, "Opciones de $titulo") }
                        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) { menu { abierto = false } }
                    }
                }
            }
        },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}

/** Cabecera de los menús laterales: la marca y qué módulo es. */
@Composable
fun CabeceraMenu(titulo: String, subtitulo: String) {
    Row(Modifier.padding(start = 28.dp, end = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.onPrimary) }
        Column(Modifier.padding(start = 14.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleLarge)
            Text(subtitulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
