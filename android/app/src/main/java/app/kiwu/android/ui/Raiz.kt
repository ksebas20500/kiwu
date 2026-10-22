package app.kiwu.android.ui

import androidx.compose.animation.togetherWith
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.kiwu.android.KiwuApp
import app.kiwu.android.data.AjustesUi
import app.kiwu.android.domain.Elemento
import app.kiwu.android.ui.editor.rememberOrigenAdjuntos
import app.kiwu.android.ui.ajustes.AjustesScreen
import app.kiwu.android.ui.archivos.ArchivosScreen
import app.kiwu.android.ui.horario.HorarioScreen
import app.kiwu.android.ui.horario.MenuHorario
import app.kiwu.android.ui.horario.HorarioViewModel
import app.kiwu.android.ui.notas.MenuNotas
import app.kiwu.android.ui.notas.NotasScreen
import app.kiwu.android.ui.notas.NotasViewModel
import app.kiwu.android.ui.notas.PaginaScreen
import app.kiwu.android.ui.notas.PaginaViewModel
import app.kiwu.android.ui.tareas.DetalleTareaScreen
import app.kiwu.android.ui.tareas.MenuTareas
import app.kiwu.android.ui.tareas.DetalleTareaViewModel
import app.kiwu.android.ui.tareas.TareasScreen
import app.kiwu.android.ui.tareas.TareasViewModel

/** Pantalla raíz: tema, navegación entre Tareas/Notas y las pantallas de detalle. */
@Composable
fun Raiz(app: KiwuApp, abrirTarea: String? = null, inicial: AjustesUi = AjustesUi()) {
    val ajustes by app.ajustes.flujo.collectAsStateWithLifecycle(inicial)
    val oscuro = when (ajustes.tema) {
        "claro" -> false
        "oscuro" -> true
        else -> isSystemInDarkTheme()
    }
    PedirPermisoDeAvisos(app)
    IconosDeBarras(oscuro)
    KiwuTheme(oscuro, ajustes.acento) {
        FondoDeVentana()
        FondoKiwu(ajustes) {
            val nav = rememberNavController()
            LaunchedEffect(abrirTarea) { if (abrirTarea != null) nav.navigate("tarea/$abrirTarea") }
            val adjuntos = rememberOrigenAdjuntos()
            NavHost(nav, startDestination = "principal") {
                composable("principal") {
                    Principal(app, alAbrirTarea = { nav.navigate("tarea/$it") }, alAbrirPagina = { nav.navigate("pagina/$it") }, alAjustes = { nav.navigate("ajustes") })
                }
                composable("tarea/{id}") { entrada ->
                    val id = entrada.arguments?.getString("id") ?: return@composable
                    val vm: DetalleTareaViewModel = viewModel(
                        key = "tarea-$id",
                        factory = viewModelFactory { initializer { DetalleTareaViewModel(app.repo, app.alcance, id) } },
                    )
                    DetalleTareaScreen(vm, adjuntos, alVolver = { nav.volver() })
                }
                composable("ajustes") { AjustesScreen(app, alVolver = { nav.volver() }) }
                composable("pagina/{id}") { entrada ->
                    val id = entrada.arguments?.getString("id") ?: return@composable
                    val vm: PaginaViewModel = viewModel(
                        key = "pagina-$id",
                        factory = viewModelFactory { initializer { PaginaViewModel(app.repo, app.alcance, id) } },
                    )
                    PaginaScreen(vm, adjuntos, alVolver = { nav.volver() })
                }
            }
        }
    }
}

/**
 * Vuelve a la pantalla anterior **una sola vez**. Mientras una pantalla se cierra (animación de salida) sigue a la vista, y un
 * segundo toque sobre su flecha (que está justo donde luego está el botón del menú) la volvería a cerrar: se quedaba la
 * navegación sin ninguna pantalla y solo se veía el fondo. Solo se vuelve si la pantalla está activa del todo.
 */
private fun androidx.navigation.NavController.volver() {
    if (currentBackStackEntry?.lifecycle?.currentState == androidx.lifecycle.Lifecycle.State.RESUMED && previousBackStackEntry != null) popBackStack()
}

@Composable
private fun Principal(app: KiwuApp, alAbrirTarea: (String) -> Unit, alAbrirPagina: (String) -> Unit, alAjustes: () -> Unit) {
    var modulo by rememberSaveable { mutableStateOf("tareas") }
    // Facturación de Google Play: se conecta pasado el arranque para no retrasarlo.
    LaunchedEffect(Unit) { app.compras.iniciar() }
    val tareasVm: TareasViewModel = viewModel(factory = viewModelFactory { initializer { TareasViewModel(app.repo, app.ajustes) } })
    val notasVm: NotasViewModel = viewModel(factory = viewModelFactory { initializer { NotasViewModel(app.repo) } })
    val horarioVm: HorarioViewModel = viewModel(factory = viewModelFactory { initializer { HorarioViewModel(app.repo) } })
    val drawer = androidx.compose.material3.rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val alcance = androidx.compose.runtime.rememberCoroutineScope()
    val cerrarMenu: () -> Unit = { alcance.launch { drawer.close() } }
    // Primero se cierra el menú (se ve su animación) y después se cambia de pantalla, que es lo que más trabajo cuesta.
    val cerrarYLuego: (() -> Unit) -> Unit = { accion -> alcance.launch { drawer.close(); accion() } }
    val abrirMenu: () -> Unit = { alcance.launch { drawer.open() } }
    // Con el menú abierto, «atrás» lo cierra en vez de salir de la app.
    androidx.activity.compose.BackHandler(enabled = drawer.isOpen) { cerrarMenu() }
    // El menú lateral cubre toda la pantalla, también la barra inferior. Cada módulo tiene el suyo.
    androidx.compose.material3.ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            when (modulo) {
                "tareas" -> MenuTareas(tareasVm, cerrarYLuego, alAjustes)
                "notas" -> MenuNotas(notasVm, cerrarYLuego, alAjustes)
                else -> MenuHorario(horarioVm, cerrarYLuego, alAjustes)
            }
        },
    ) {
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        // Las pantallas de dentro ya gestionan la barra de estado; la barra inferior gestiona la de navegación.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            androidx.compose.foundation.layout.Column {
            // Banner pequeño solo en las listas de Tareas y de Notas (no en Horario, Calendario, editor ni detalle).
            val tareasEstado by tareasVm.estado.collectAsStateWithLifecycle()
            val verBanner = modulo == "notas" || (modulo == "tareas" && tareasEstado.elemento != Elemento.Calendario)
            if (verBanner) BannerAnuncio()
            NavigationBar {
                NavigationBarItem(selected = modulo == "tareas", onClick = { modulo = "tareas" }, icon = { Icon(Icons.Filled.CheckCircle, null) }, label = { Text("Tareas") })
                NavigationBarItem(selected = modulo == "notas", onClick = { modulo = "notas" }, icon = { Icon(Icons.Filled.Description, null) }, label = { Text("Notas") })
                NavigationBarItem(selected = modulo == "horario", onClick = { modulo = "horario" }, icon = { Icon(Icons.Filled.Schedule, null) }, label = { Text("Horario") })
            }
            }
        },
    ) { relleno ->
        Box(Modifier.padding(relleno).consumeWindowInsets(relleno).fillMaxSize()) {
            // Transición corta entre pestañas: fundido con un deslizamiento pequeño hacia el lado al que se avanza.
            val orden = listOf("tareas", "notas", "horario")
            androidx.compose.animation.AnimatedContent(
                targetState = modulo,
                transitionSpec = {
                    val dir = if (orden.indexOf(targetState) > orden.indexOf(initialState)) 1 else -1
                    (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220, 40)) +
                        androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(260)) { dir * it / 10 }) togetherWith
                        (androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)) +
                            androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(260)) { -dir * it / 10 })
                },
                label = "modulo",
            ) { destino ->
                when (destino) {
                    "notas" -> NotasScreen(notasVm, alAbrirPagina, abrirMenu)
                    "horario" -> HorarioScreen(
                        horarioVm, abrirMenu, alAbrirTarea,
                        // «Abrir la lista completa»: cambia a la pestaña Tareas, ya en esa lista.
                        alAbrirLista = { id -> tareasVm.ir(Elemento.DeMateria(id)); modulo = "tareas" },
                    )
                    else -> TareasScreen(tareasVm, alAbrirTarea, abrirMenu, archivos = { m -> ArchivosScreen(m) { carpeta -> tareasVm.vincularCarpeta(m.id, carpeta) } })
                }
            }
        }
    }
    }
}

/** Pide el permiso de notificaciones (Android 13+) la primera vez que alguna tarea tiene fecha, no al abrir la app. */
@Composable
private fun PedirPermisoDeAvisos(app: KiwuApp) {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val lanzador = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }
    val deberes by app.repo.deberes.collectAsStateWithLifecycle(emptyList())
    val hayFechas = deberes.any { it.fechaEntrega != null }
    LaunchedEffect(hayFechas) {
        if (!hayFechas || android.os.Build.VERSION.SDK_INT < 33 || app.avisosPermitidos()) return@LaunchedEffect
        val prefs = contexto.getSharedPreferences("kiwu", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("pidio_avisos", false)) {
            prefs.edit().putBoolean("pidio_avisos", true).apply()
            lanzador.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/** La ventana toma el color de fondo del tema: así no asoma otro color al girar la pantalla ni al abrir teclados. */
@Composable
private fun FondoDeVentana() {
    var contexto = androidx.compose.ui.platform.LocalContext.current
    while (contexto is android.content.ContextWrapper && contexto !is androidx.activity.ComponentActivity) contexto = contexto.baseContext
    val actividad = contexto as? androidx.activity.ComponentActivity ?: return
    val fondo = MaterialTheme.colorScheme.background
    androidx.compose.runtime.SideEffect {
        actividad.window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(fondo.toArgb()))
    }
}

/** Los iconos de las barras del sistema siguen el tema de la app (no solo el del sistema). */
@Composable
private fun IconosDeBarras(oscuro: Boolean) {
    var contexto = androidx.compose.ui.platform.LocalContext.current
    while (contexto is android.content.ContextWrapper && contexto !is androidx.activity.ComponentActivity) contexto = contexto.baseContext
    val actividad = contexto as? androidx.activity.ComponentActivity ?: return
    androidx.compose.runtime.SideEffect {
        val transparente = android.graphics.Color.TRANSPARENT
        val estilo = if (oscuro) androidx.activity.SystemBarStyle.dark(transparente) else androidx.activity.SystemBarStyle.light(transparente, transparente)
        actividad.enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
    }
}
