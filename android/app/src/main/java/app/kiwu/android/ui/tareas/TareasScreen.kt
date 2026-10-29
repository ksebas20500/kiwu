package app.kiwu.android.ui.tareas

import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.School
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

import app.kiwu.android.ui.calendario.CalendarioScreen
import kotlinx.coroutines.launch
import app.kiwu.android.R
import androidx.compose.ui.res.stringResource

@Composable
private fun tituloDe(e: Elemento, materia: Materia?, grupo: app.kiwu.android.domain.GrupoLista?): String = when (e) {
    Elemento.Hoy -> stringResource(R.string.hoy)
    Elemento.Proximos -> stringResource(R.string.proximos_7)
    Elemento.Calendario -> stringResource(R.string.calendario)
    Elemento.Todos -> stringResource(R.string.todas_tareas)
    Elemento.Completadas -> stringResource(R.string.completadas)
    is Elemento.DeMateria -> materia?.nombre ?: stringResource(R.string.lista)
    is Elemento.DeGrupo -> grupo?.nombre ?: stringResource(R.string.carpeta)
}

/** Módulo de tareas: lista, tablero o calendario (+ archivos de la lista). El menú lateral vive en la pantalla principal. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TareasScreen(
    vm: TareasViewModel,
    alAbrirTarea: (String) -> Unit,
    alAbrirMenu: () -> Unit,
    archivos: @Composable (Materia) -> Unit,
    insigniaRacha: @Composable () -> Unit = {},
    accionesExtra: @Composable () -> Unit = {},
    alCalificaciones: (String) -> Unit = {},
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    // El tablero solo tiene sentido en pantallas anchas: en el teléfono siempre se ve la lista.
    val anchaParaTablero = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    val verTablero = anchaParaTablero && estado.vista == "tablero" && !estado.enEventos
    var dialogoNuevaLista by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tituloDe(estado.elemento, estado.materiaActual, estado.grupoActual)) },
                navigationIcon = {
                    androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        IconButton(onClick = alAbrirMenu) { Icon(Icons.Filled.Menu, stringResource(R.string.abrir_menu)) }
                        insigniaRacha()
                    }
                },
                actions = {
                    accionesExtra()
                    estado.materiaActual?.takeIf { !estado.enEventos }?.let { m ->
                        IconButton(onClick = { alCalificaciones(m.id) }) { Icon(Icons.Filled.School, stringResource(R.string.calificaciones)) }
                    }
                    if (estado.elemento != Elemento.Calendario && anchaParaTablero && !estado.enEventos) {
                        val tablero = verTablero
                        IconButton(onClick = { vm.cambiarVista(if (tablero) "lista" else "tablero") }) {
                            Icon(
                                if (tablero) Icons.Filled.ViewList else Icons.Filled.ViewKanban,
                                stringResource(if (tablero) R.string.ver_como_lista else R.string.ver_como_tablero),
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
            if (materia != null && !estado.enEventos) {
                TabRow(
                    selectedTabIndex = if (estado.subvista == "archivos") 1 else 0,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Tab(selected = estado.subvista != "archivos", onClick = { vm.cambiarSubvista("tareas") }, text = { Text(stringResource(R.string.tareas)) })
                    Tab(selected = estado.subvista == "archivos", onClick = { vm.cambiarSubvista("archivos") }, text = { Text(stringResource(R.string.archivos)) })
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                // Fundido suave al cambiar de sección desde el menú (Calendario ↔ listas ↔ archivos). Solo cambia la clave
                // al cambiar de tipo de vista, no al pasar de una lista a otra.
                val tipo = when {
                    estado.elemento == Elemento.Calendario -> "calendario"
                    materia != null && estado.subvista == "archivos" -> "archivos"
                    verTablero -> "tablero"
                    else -> "lista"
                }
                androidx.compose.animation.AnimatedContent(
                    targetState = tipo,
                    transitionSpec = {
                        (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220, 40)) +
                            androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(260), initialScale = 0.97f)) togetherWith
                            androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120))
                    },
                    label = "seccionTareas",
                ) { t ->
                    when (t) {
                        "calendario" -> CalendarioScreen(vm, estado, alAbrirTarea)
                        "archivos" -> materia?.let { archivos(it) }
                        "tablero" -> Tablero(vm, estado, alAbrirTarea) { dialogoNuevaLista = true }
                        else -> ListaTareas(vm, estado, alAbrirTarea) { dialogoNuevaLista = true }
                    }
                }
            }
        }
    }

    if (dialogoNuevaLista) {
        // Si se está viendo una carpeta, la lista nace dentro de ella.
        val carpeta = (estado.elemento as? Elemento.DeGrupo)?.id
        DialogoNuevoElemento(
            titulo = if (carpeta != null) stringResource(R.string.lista_nueva_en, estado.grupoActual?.nombre ?: stringResource(R.string.la_carpeta)) else stringResource(R.string.lista_nueva),
            etiqueta = stringResource(R.string.lista_nombre_materia), accion = stringResource(R.string.crear), alCerrar = { dialogoNuevaLista = false },
        ) { nombre, emoji -> vm.crearLista(nombre, emoji, carpeta) }
    }
}

/** Cabecera de los menús laterales: la marca y qué módulo es. */
@Composable
fun CabeceraMenu(titulo: String, subtitulo: String) {
    Row(Modifier.padding(start = 28.dp, end = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(48.dp).background(
                // Mismo degradado morado y azul del icono de la app.
                androidx.compose.ui.graphics.Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color(0xFF8D51EF), androidx.compose.ui.graphics.Color(0xFF6E62F4), androidx.compose.ui.graphics.Color(0xFF3693F3))),
                androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            ),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(
                androidx.compose.ui.res.painterResource(app.kiwu.android.R.drawable.ic_gato), null,
                Modifier.size(38.dp),
            )
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleLarge)
            Text(subtitulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
