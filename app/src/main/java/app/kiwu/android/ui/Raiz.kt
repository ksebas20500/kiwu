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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import app.kiwu.android.ui.home.HomeScreen
import app.kiwu.android.ui.estudio.CalificacionesScreen
import app.kiwu.android.ui.estudio.RepasoHoyScreen
import app.kiwu.android.ui.estudio.RepasoHoyViewModel
import app.kiwu.android.ui.estudio.TarjetasScreen
import app.kiwu.android.ui.estudio.TemporizadorScreen
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
import androidx.compose.runtime.remember
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
import app.kiwu.android.R
import app.kiwu.android.KiwuApp
import app.kiwu.android.data.AjustesUi
import app.kiwu.android.domain.Elemento
import app.kiwu.android.domain.Guia
import app.kiwu.android.domain.Rachas
import app.kiwu.android.ui.tareas.CelebracionRacha
import app.kiwu.android.ui.tareas.InsigniaRacha
import app.kiwu.android.ui.tareas.RachaScreen
import app.kiwu.android.ui.guia.GuiaScreen
import app.kiwu.android.ui.editor.rememberOrigenAdjuntos
import app.kiwu.android.ui.ajustes.AjustesScreen
import app.kiwu.android.ui.archivos.ArchivosScreen
import app.kiwu.android.ui.horario.HorarioScreen
import app.kiwu.android.ui.horario.MenuHorario
import app.kiwu.android.ui.horario.HorarioViewModel
import app.kiwu.android.ui.notas.FiltroNotas
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

/**
 * Pantalla raíz: tema, navegación entre Tareas/Notas y las pantallas de detalle. Con [guia] (primera vez, app vacía) se
 * empieza por la guía de inicio.
 */
@Composable
fun Raiz(
    app: KiwuApp,
    abrirTarea: String? = null,
    abrirPagina: String? = null,
    nuevaNota: String? = null,
    seccion: String? = null,
    abrirRepaso: Boolean = false,
    inicial: AjustesUi = AjustesUi(),
    fondoInicial: android.graphics.Bitmap? = null,
    guia: Boolean = false,
    alAtender: () -> Unit = {},
) {
    val ajustes by app.ajustes.flujo.collectAsStateWithLifecycle(inicial)
    val oscuro = when (ajustes.tema) {
        "claro" -> false
        "oscuro", Acentos.NEGRO -> true
        else -> isSystemInDarkTheme()
    }
    PedirPermisoDeAvisos(app)
    IconosDeBarras(oscuro)
    KiwuTheme(oscuro, ajustes.acento, false) {
        FondoDeVentana()
        FondoKiwu(ajustes, fondoInicial) {
            val nav = rememberNavController()
            LaunchedEffect(abrirTarea) {
                if (abrirTarea != null) { nav.navigate("tarea/$abrirTarea"); alAtender() }
            }
            LaunchedEffect(abrirPagina) {
                if (abrirPagina != null) { nav.navigate("pagina/$abrirPagina"); alAtender() }
            }
            // Nota rápida desde el widget: se crea la página y se abre directamente en el editor.
            LaunchedEffect(nuevaNota) {
                if (nuevaNota != null) {
                    val pagina = app.repo.crearPaginaRapida(nuevaNota)
                    nav.navigate("pagina/${pagina.id}")
                    alAtender()
                }
            }
            // Aviso diario de repaso: abre la cola global.
            LaunchedEffect(abrirRepaso) {
                if (abrirRepaso) { nav.navigate("repaso-hoy"); alAtender() }
            }
            val adjuntos = rememberOrigenAdjuntos()
            val alcance = androidx.compose.runtime.rememberCoroutineScope()
            // Lista que se abre al entrar en Tareas (la de ejemplo, al terminar la guía).
            var abrirLista by rememberSaveable { mutableStateOf<String?>(null) }
            NavHost(nav, startDestination = if (guia) "guia" else "principal") {
                composable("guia") {
                    var terminando by androidx.compose.runtime.remember { mutableStateOf(false) }
                    GuiaScreen(alTerminar = {
                        if (!terminando) alcance.launch {
                            terminando = true
                            app.ajustes.cambiar { it.copy(guiaVista = true) }
                            nav.navigate("principal") { popUpTo("guia") { inclusive = true } }
                        }
                    })
                }
                // Repaso desde Ajustes: solo las pantallas, sin crear la lista de ejemplo.
                composable("repaso") { GuiaScreen(alTerminar = { nav.volver() }) }
                composable("principal") {
                    Principal(
                        app, alAbrirTarea = { nav.navigate("tarea/$it") }, alAbrirPagina = { nav.navigate("pagina/$it") }, alAjustes = { nav.navigate("ajustes") },
                        alRacha = { nav.navigate("racha") }, alBuscar = { nav.navigate("buscar") },
                        alTemporizador = { nav.navigate("temporizador") }, alCalificaciones = { nav.navigate("calificaciones/$it") },
                        alRepasoHoy = { nav.navigate("repaso-hoy") }, alPapelera = { nav.navigate("ajustes/papelera") },
                        abrirLista = abrirLista, alAbrirLista = { abrirLista = null },
                        seccionInicial = seccion, alAtenderSeccion = alAtender,
                    )
                }
                composable("tarea/{id}") { entrada ->
                    val id = entrada.arguments?.getString("id") ?: return@composable
                    val vm: DetalleTareaViewModel = viewModel(
                        key = "tarea-$id",
                        factory = viewModelFactory { initializer { DetalleTareaViewModel(app.repo, app.alcance, id) } },
                    )
                    DetalleTareaScreen(vm, adjuntos, alVolver = { nav.volver() })
                }
                composable("ajustes") { AjustesScreen(app, alVolver = { nav.volver() }, alVerGuia = { nav.navigate("repaso") }) }
                composable("ajustes/{seccion}") { entrada ->
                    val seccionStr = entrada.arguments?.getString("seccion")
                    AjustesScreen(app, seccionInicial = seccionStr, alVolver = { nav.volver() }, alVerGuia = { nav.navigate("repaso") })
                }
                composable("pagina/{id}") { entrada ->
                    val id = entrada.arguments?.getString("id") ?: return@composable
                    val vm: PaginaViewModel = viewModel(
                        key = "pagina-$id",
                        factory = viewModelFactory { initializer { PaginaViewModel(app.repo, app.alcance, id) } },
                    )
                    PaginaScreen(vm, adjuntos, alVolver = { nav.volver() }, alTarjetas = { nav.navigate("tarjetas/$id") })
                }
                composable("buscar") { BuscarScreen(app, alVolver = { nav.volver() }, alAbrirTarea = { nav.navigate("tarea/$it") }, alAbrirPagina = { nav.navigate("pagina/$it") }) }
                composable("temporizador") { TemporizadorScreen(app, alVolver = { nav.volver() }) }
                composable("calificaciones/{id}") { entrada ->
                    val id = entrada.arguments?.getString("id") ?: return@composable
                    CalificacionesScreen(app, id, alVolver = { nav.volver() })
                }
                composable("tarjetas/{id}") { entrada ->
                    val id = entrada.arguments?.getString("id") ?: return@composable
                    TarjetasScreen(app, id, alVolver = { nav.volver() })
                }
                composable("repaso-hoy") {
                    val vm: RepasoHoyViewModel = viewModel(
                        factory = viewModelFactory { initializer { RepasoHoyViewModel(app.repo) } },
                    )
                    RepasoHoyScreen(vm, alVolver = { nav.volver() })
                }
                composable("racha") { RachaScreen(app, alVolver = { nav.volver() }) }
            }
            CelebrarRacha(app, ajustes)
            AvisoActualizacion(app)
        }
    }
}

/**
 * Al completar la primera tarea del día salta la celebración de la racha, por encima de cualquier pantalla. Sale una vez al
 * día (se guarda el día celebrado) y solo si la racha está visible.
 */
@Composable
private fun CelebrarRacha(app: KiwuApp, ajustes: AjustesUi) {
    val fechas by app.repo.entradaRacha.collectAsStateWithLifecycle(null)
    var celebrando by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(fechas, ajustes.rachaCelebrada, ajustes.rachaVisible) {
        val f = fechas ?: return@LaunchedEffect
        if (!ajustes.rachaVisible || celebrando) return@LaunchedEffect
        val hoy = java.time.LocalDate.now()
        if (Rachas.debeCelebrar(Rachas.calcular(f, System.currentTimeMillis()), ajustes.rachaCelebrada, hoy)) {
            kotlinx.coroutines.delay(450) // que primero se vea la casilla marcada
            app.ajustes.cambiar { it.copy(rachaCelebrada = hoy.toString()) }
            celebrando = true
        }
    }
    val f = fechas
    if (celebrando && f != null) {
        val racha = androidx.compose.runtime.remember(f) { Rachas.calcular(f, System.currentTimeMillis()) }
        CelebracionRacha(racha, alCerrar = { celebrando = false })
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
private fun Principal(
    app: KiwuApp, alAbrirTarea: (String) -> Unit, alAbrirPagina: (String) -> Unit, alAjustes: () -> Unit,
    alRacha: () -> Unit = {}, alBuscar: () -> Unit = {}, alTemporizador: () -> Unit = {}, alCalificaciones: (String) -> Unit = {},
    alRepasoHoy: () -> Unit = {}, alPapelera: () -> Unit = {},
    abrirLista: String? = null, alAbrirLista: () -> Unit = {},
    seccionInicial: String? = null, alAtenderSeccion: () -> Unit = {},
) {
    var modulo by rememberSaveable { mutableStateOf("inicio") }
    // Abrir desde el widget de horario (o de notas): se cambia de pestaña una sola vez.
    LaunchedEffect(seccionInicial) {
        if (seccionInicial != null) { modulo = seccionInicial; alAtenderSeccion() }
    }
    val tareasVm: TareasViewModel = viewModel(factory = viewModelFactory { initializer { TareasViewModel(app, app.repo, app.ajustes) } })
    LaunchedEffect(abrirLista) {
        if (abrirLista != null) { tareasVm.ir(Elemento.DeMateria(abrirLista)); modulo = "tareas"; alAbrirLista() }
    }
    val notasVm: NotasViewModel = viewModel(factory = viewModelFactory { initializer { NotasViewModel(app.repo) } })
    val horarioVm: HorarioViewModel = viewModel(factory = viewModelFactory { initializer { HorarioViewModel(app.repo) } })
    val ajustes by app.ajustes.flujo.collectAsStateWithLifecycle(AjustesUi())
    val drawer = androidx.compose.material3.rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val alcance = androidx.compose.runtime.rememberCoroutineScope()
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    LaunchedEffect(drawer.targetValue, drawer.isOpen) {
        if (drawer.targetValue == androidx.compose.material3.DrawerValue.Open || drawer.isOpen) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    val cerrarMenu: () -> Unit = { alcance.launch { drawer.close() } }
    val cerrarYLuego: (() -> Unit) -> Unit = { accion ->
        alcance.launch {
            drawer.close()
            modulo = "tareas"
            accion()
        }
    }
    val abrirMenu: () -> Unit = { alcance.launch { drawer.open() } }
    androidx.activity.compose.BackHandler(enabled = drawer.isOpen) { cerrarMenu() }
    val estadoNotas by notasVm.estado.collectAsStateWithLifecycle()
    val estadoHorario by horarioVm.estado.collectAsStateWithLifecycle()
    var estaArrastrandoLista by remember { mutableStateOf(false) }

    androidx.compose.material3.ModalNavigationDrawer(
        drawerState = drawer,
        gesturesEnabled = drawer.isOpen && !estaArrastrandoLista,
        drawerContent = {
            MenuTareas(
                vm = tareasVm,
                cerrarYLuego = cerrarYLuego,
                alAjustes = alAjustes,
                alRacha = alRacha,
                cuadernos = estadoNotas.cuadernos,
                horarios = estadoHorario.horarios,
                flashcardsPendientes = estadoNotas.tarjetasPendientes,
                alElegirCuaderno = { id ->
                    notasVm.filtrar(FiltroNotas.DeCuaderno(id))
                    modulo = "notas"
                },
                alElegirHorario = { id ->
                    horarioVm.elegir(id)
                    modulo = "horario"
                },
                alRepasoHoy = alRepasoHoy,
                alPapelera = alPapelera,
                app = app,
                ajustes = ajustes,
                alCambiarArrastre = { esta -> estaArrastrandoLista = esta },
            )
        },
    ) {
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            androidx.compose.foundation.layout.Column {
                AdBanner(app)
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    val colores = androidx.compose.material3.NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    )
                    NavigationBarItem(colors = colores, selected = modulo == "inicio", onClick = { modulo = "inicio" }, icon = { Icon(Icons.Filled.Home, null) }, label = { Text(androidx.compose.ui.res.stringResource(R.string.inicio)) })
                    NavigationBarItem(colors = colores, selected = modulo == "tareas", onClick = { modulo = "tareas" }, icon = { Icon(Icons.Filled.CheckCircle, null) }, label = { Text(androidx.compose.ui.res.stringResource(R.string.tareas)) })
                    NavigationBarItem(colors = colores, selected = modulo == "notas", onClick = { modulo = "notas" }, icon = { Icon(Icons.Filled.Description, null) }, label = { Text(androidx.compose.ui.res.stringResource(R.string.notas)) })
                    NavigationBarItem(colors = colores, selected = modulo == "horario", onClick = { modulo = "horario" }, icon = { Icon(Icons.Filled.Schedule, null) }, label = { Text(androidx.compose.ui.res.stringResource(R.string.horario)) })
                }
            }
        },
    ) { relleno ->
        Box(Modifier.padding(relleno).consumeWindowInsets(relleno).fillMaxSize()) {
            val orden = listOf("inicio", "tareas", "notas", "horario")
            androidx.compose.animation.AnimatedContent(
                targetState = modulo,
                transitionSpec = {
                    val dir = if (orden.indexOf(targetState) > orden.indexOf(initialState)) 1 else -1
                    (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(160, 20)) +
                        androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(180)) { dir * it / 20 }) togetherWith
                        (androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)) +
                            androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(160)) { -dir * it / 20 })
                },
                label = "modulo",
            ) { destino ->
                when (destino) {
                    "inicio" -> HomeScreen(
                        app = app,
                        tareasVm = tareasVm,
                        notasVm = notasVm,
                        horarioVm = horarioVm,
                        alAbrirMenu = abrirMenu,
                        alAbrirTarea = alAbrirTarea,
                        alAbrirCuaderno = { id ->
                            notasVm.filtrar(FiltroNotas.DeCuaderno(id))
                            modulo = "notas"
                        },
                        alTareas = { modulo = "tareas" },
                        alNotas = { modulo = "notas" },
                        alHorario = { modulo = "horario" },
                        alRacha = alRacha,
                        alTemporizador = alTemporizador,
                        alRepasoHoy = alRepasoHoy,
                    )
                    "notas" -> NotasScreen(
                        notasVm, alAbrirPagina, abrirMenu, alRepasoHoy = alRepasoHoy,
                    )
                    "horario" -> HorarioScreen(
                        horarioVm, abrirMenu, alAbrirTarea,
                        alAbrirLista = { id -> tareasVm.ir(Elemento.DeMateria(id)); modulo = "tareas" },
                    )
                    else -> TareasScreen(
                        tareasVm, alAbrirTarea, abrirMenu, archivos = { m -> ArchivosScreen(m) { carpeta -> tareasVm.vincularCarpeta(m.id, carpeta) } },
                        accionesExtra = { IconoBuscar(alBuscar) },
                        alCalificaciones = alCalificaciones,
                    )
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

@Composable
private fun IconoTemporizador(alTemporizador: () -> Unit) =
    androidx.compose.material3.IconButton(onClick = alTemporizador) { Icon(Icons.Filled.Timer, androidx.compose.ui.res.stringResource(R.string.temporizador)) }

@Composable
private fun IconoBuscar(alBuscar: () -> Unit) =
    androidx.compose.material3.IconButton(onClick = alBuscar) { Icon(Icons.Filled.Search, androidx.compose.ui.res.stringResource(R.string.buscar)) }

@Composable
private fun IconoRepasoHoy(alAbrir: () -> Unit) =
    androidx.compose.material3.IconButton(onClick = alAbrir) { Icon(Icons.Filled.Style, androidx.compose.ui.res.stringResource(R.string.repaso_hoy)) }
