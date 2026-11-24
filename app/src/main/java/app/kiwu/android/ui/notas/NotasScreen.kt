package app.kiwu.android.ui.notas

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.KiwuApp
import app.kiwu.android.R
import app.kiwu.android.data.AjustesUi
import app.kiwu.android.data.NotebookCoverStorage
import app.kiwu.android.data.ResumenPagina
import app.kiwu.android.domain.Cuaderno
import app.kiwu.android.ui.DialogoConfirmar
import app.kiwu.android.ui.EstadoVacio
import app.kiwu.android.ui.fechaCorta
import coil.compose.AsyncImage
import java.io.File
import kotlinx.coroutines.launch

val EmojisCuaderno = listOf("📓", "📝", "📚", "📖", "🎓", "💡", "🧪", "🧮", "💻", "🎨", "🎵", "🌱", "🗂️", "⭐", "🔥", "✅", "📌", "🧠", "🏃", "🍎")
val ColoresPortada = listOf("#E57373", "#81C784", "#64B5F6", "#FFD54F", "#BA68C8", "#FF8A65", "#4DB6AC", "#A1887F")

private fun hexAColor(hex: String?): Color? {
    if (hex.isNull_or_blank()) return null
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        null
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()

/** Módulo de notas: estantería de cuadernos con portadas visuales, búsqueda y lista de páginas. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotasScreen(
    vm: NotasViewModel,
    alAbrirPagina: (String) -> Unit,
    alAbrirMenu: () -> Unit,
    alRepasoHoy: () -> Unit = {},
    insigniaRacha: @Composable () -> Unit = {},
    accionesExtra: @Composable () -> Unit = {},
) {
    val contexto = LocalContext.current
    val estado by vm.estado.collectAsStateWithLifecycle()
    val alcance = rememberCoroutineScope()
    var buscando by remember { mutableStateOf(false) }
    var dialogoCuaderno by remember { mutableStateOf<Cuaderno?>(null) }
    var nuevoCuaderno by remember { mutableStateOf(false) }
    var eliminarCuaderno by remember { mutableStateOf<Cuaderno?>(null) }
    var menuCuaderno by remember { mutableStateOf(false) }

    val esNivelPrincipal = estado.filtro == FiltroNotas.Todas && !buscando
    val cuadernoActual = estado.cuadernoActual

    BackHandler(enabled = !esNivelPrincipal) {
        if (buscando) {
            buscando = false
            vm.buscar("")
        } else {
            vm.filtrar(FiltroNotas.Todas)
        }
    }

    val app = contexto.applicationContext as? KiwuApp
    val ajustes by (app?.ajustes?.flujo?.collectAsStateWithLifecycle(AjustesUi()) ?: remember { mutableStateOf(AjustesUi()) })

    Box(Modifier.fillMaxSize()) {
        if (ajustes.fondoUri != null) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.18f)))
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (buscando) {
                            OutlinedTextField(
                                value = estado.busqueda,
                                onValueChange = vm::buscar,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(stringResource(R.string.notas_buscar_en)) },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Search),
                            )
                        } else {
                            Column {
                                Text(
                                    when (val f = estado.filtro) {
                                        FiltroNotas.Todas -> stringResource(R.string.notas)
                                        FiltroNotas.Favoritas -> stringResource(R.string.favoritas)
                                        is FiltroNotas.DeCuaderno -> cuadernoActual?.nombre ?: stringResource(R.string.notas)
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (!esNivelPrincipal) {
                                    val cant = estado.paginas.size
                                    Text(
                                        if (cant == 1) "1 nota" else "$cant notas",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        if (buscando || !esNivelPrincipal) {
                            IconButton(onClick = {
                                if (buscando) {
                                    buscando = false
                                    vm.buscar("")
                                } else {
                                    vm.filtrar(FiltroNotas.Todas)
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver))
                            }
                        } else {
                            IconButton(onClick = alAbrirMenu) {
                                Icon(Icons.Filled.Menu, stringResource(R.string.abrir_menu))
                            }
                        }
                    },
                    actions = {
                        if (!buscando) {
                            IconButton(onClick = { buscando = true }) {
                                Icon(Icons.Filled.Search, stringResource(R.string.buscar))
                            }

                            if (!esNivelPrincipal && estado.filtro is FiltroNotas.DeCuaderno) {
                                Box {
                                    IconButton(onClick = { menuCuaderno = true }) {
                                        Icon(Icons.Filled.MoreVert, stringResource(R.string.cuaderno_opciones))
                                    }
                                    DropdownMenu(expanded = menuCuaderno, onDismissRequest = { menuCuaderno = false }) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.cuaderno_editar)) },
                                            onClick = { menuCuaderno = false; dialogoCuaderno = cuadernoActual },
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.cuaderno_eliminar), color = MaterialTheme.colorScheme.error) },
                                            onClick = { menuCuaderno = false; eliminarCuaderno = cuadernoActual },
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    alcance.launch {
                        val destino = (estado.filtro as? FiltroNotas.DeCuaderno)?.id
                        alAbrirPagina(vm.nuevaPagina(destino))
                    }
                }) {
                    Icon(Icons.Filled.Add, stringResource(R.string.nota_nueva))
                }
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) { relleno ->
            Column(Modifier.padding(relleno).consumeWindowInsets(relleno).fillMaxSize()) {
                if (buscando) {
                    BuscadorUnificado(estado, vm, alAbrirPagina) { buscando = false }
                } else if (esNivelPrincipal) {
                    // --- NIVEL 1: ESTANTERÍA DE CUADERNOS ---
                    // Fila de accesos rápidos
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.76f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
                            modifier = Modifier.weight(1f).clickable { vm.filtrar(FiltroNotas.Favoritas) },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFE0A100), modifier = Modifier.size(20.dp))
                                Text(
                                    "${stringResource(R.string.favoritas)} (${estado.favoritasCount})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        val tienePendientesSRS = estado.tarjetasPendientes > 0
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (tienePendientesSRS) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
                            modifier = Modifier.weight(1f).clickable { alRepasoHoy() },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Style,
                                    contentDescription = null,
                                    tint = if (tienePendientesSRS) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    stringResource(R.string.repaso_hoy),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (tienePendientesSRS) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    // Cuadrícula de Cuadernos
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 156.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        // Primer ítem: + Nuevo cuaderno
                        item {
                            TarjetaNuevoCuaderno { nuevoCuaderno = true }
                        }

                        // Lista de cuadernos
                        items(estado.cuadernos, key = { it.id }) { cuaderno ->
                            NotebookCard(
                                cuaderno = cuaderno,
                                cantNotas = estado.cantidadNotas(cuaderno.id),
                                alPulsar = { vm.filtrar(FiltroNotas.DeCuaderno(cuaderno.id)) },
                                alEditar = { dialogoCuaderno = cuaderno },
                                alEliminar = { eliminarCuaderno = cuaderno },
                            )
                        }
                    }
                } else {
                    // --- NIVEL 2: DENTRO DE UN CUADERNO / FAVORITAS ---
                    if (estado.cargado && estado.paginas.isEmpty()) {
                        EstadoVacio(
                            Icons.Filled.Description,
                            stringResource(R.string.sin_notas),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(estado.paginas, key = { it.id }) { p ->
                                TarjetaNotaIndividual(p, estado.nombresCuadernos[p.cuadernoId], vm, alAbrirPagina)
                            }
                        }
                    }
                }
            }
        }
    }

    if (nuevoCuaderno) {
        DialogoCuaderno(null, alCerrar = { nuevoCuaderno = false }) { nombre, icono, foto, colorHex ->
            vm.crearCuaderno(nombre, icono, foto, colorHex)
        }
    }
    dialogoCuaderno?.let { c ->
        DialogoCuaderno(c, alCerrar = { dialogoCuaderno = null }) { nombre, icono, foto, colorHex ->
            vm.editarCuaderno(c.id, nombre, icono, foto, colorHex)
        }
    }
    eliminarCuaderno?.let { c ->
        DialogoConfirmar(
            stringResource(R.string.cuaderno_eliminar_titulo, c.nombre),
            stringResource(R.string.cuaderno_eliminar_texto),
            stringResource(R.string.cuaderno_eliminar_boton),
            alCerrar = { eliminarCuaderno = null },
        ) { vm.eliminarCuaderno(c.id, contexto) }
    }
}

/** Tarjeta con forma de libro para la estantería de cuadernos. */
@Composable
private fun NotebookCard(
    cuaderno: Cuaderno,
    cantNotas: Int,
    alPulsar: () -> Unit,
    alEditar: () -> Unit,
    alEliminar: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val shapeLibro = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp)
    val colorRespaldo = hexAColor(cuaderno.coverColorHex) ?: MaterialTheme.colorScheme.primaryContainer
    val tieneImagen = cuaderno.coverImagePath != null && File(cuaderno.coverImagePath).exists()

    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = alPulsar),
    ) {
        // Portada del libro
        Surface(
            shape = shapeLibro,
            color = colorRespaldo,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.75f),
        ) {
            Box(Modifier.fillMaxSize()) {
                if (tieneImagen && cuaderno.coverImagePath != null) {
                    AsyncImage(
                        model = File(cuaderno.coverImagePath),
                        contentDescription = cuaderno.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(colorRespaldo, colorRespaldo.copy(alpha = 0.75f))
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(cuaderno.icono.ifBlank { "📓" }, fontSize = 42.sp)
                    }
                }

                // Lomo del libro (franja vertical a la izquierda)
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.14f))
                        .align(Alignment.CenterStart)
                )
            }
        }

        // Pie del cuaderno sobre superficie legible
        Surface(
            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.76f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) {
            Row(
                modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        cuaderno.nombre,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (cantNotas == 1) "1 nota" else "$cantNotas notas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.cuaderno_editar)) },
                            onClick = { menu = false; alEditar() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.cuaderno_eliminar), color = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; alEliminar() },
                        )
                    }
                }
            }
        }
    }
}

/** Tarjeta "+ Nuevo cuaderno" con borde punteado/tonal. */
@Composable
private fun TarjetaNuevoCuaderno(alPulsar: () -> Unit) {
    val shapeLibro = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp)
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = alPulsar),
    ) {
        Surface(
            shape = shapeLibro,
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth().aspectRatio(0.75f),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Text(
                    stringResource(R.string.cuaderno_nuevo),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

/** Tarjeta de nota individual dentro de un cuaderno. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TarjetaNotaIndividual(
    p: ResumenPagina,
    nombreCuaderno: String?,
    vm: NotasViewModel,
    alAbrir: (String) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var eliminar by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { alAbrir(p.id) }, onLongClick = { menu = true }),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(p.icono.ifBlank { "📄" }, fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    p.titulo.ifBlank { stringResource(R.string.sin_titulo) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val extracto = p.textoPlano.replace('\n', ' ').trim()
                if (extracto.isNotBlank()) {
                    Text(
                        extracto,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (nombreCuaderno != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        ) {
                            Text(
                                nombreCuaderno,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        fechaCorta(p.actualizada, System.currentTimeMillis()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    )
                }
            }

            IconButton(onClick = { vm.ponerFavorita(p.id, !p.favorita) }) {
                Icon(
                    if (p.favorita) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = null,
                    tint = if (p.favorita) Color(0xFFE0A100) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(if (p.favorita) R.string.quitar_favoritas else R.string.anadir_favoritas)) },
                        onClick = { menu = false; vm.ponerFavorita(p.id, !p.favorita) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.nota_eliminar), color = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; eliminar = true },
                    )
                }
            }
        }
    }

    if (eliminar) {
        DialogoConfirmar(
            stringResource(R.string.nota_eliminar_titulo),
            stringResource(R.string.no_se_recupera),
            stringResource(R.string.eliminar),
            alCerrar = { eliminar = false },
        ) { vm.eliminarPagina(p.id) }
    }
}

/** Buscador unificado: cuadernos en miniatura + notas que coinciden. */
@Composable
private fun BuscadorUnificado(
    estado: NotasEstado,
    vm: NotasViewModel,
    alAbrirPagina: (String) -> Unit,
    alCerrarBusqueda: () -> Unit,
) {
    val cuadernosCoincidentes = remember(estado.cuadernos, estado.busqueda) {
        if (estado.busqueda.isBlank()) emptyList()
        else estado.cuadernos.filter { estado.busqueda in it.nombre.lowercase() }
    }

    Column(Modifier.fillMaxSize()) {
        if (cuadernosCoincidentes.isNotEmpty()) {
            Text(
                stringResource(R.string.cuadernos),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(cuadernosCoincidentes, key = { "b-c-" + it.id }) { c ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
                        modifier = Modifier.clickable {
                            alCerrarBusqueda()
                            vm.filtrar(FiltroNotas.DeCuaderno(c.id))
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val colorRespaldo = hexAColor(c.coverColorHex) ?: MaterialTheme.colorScheme.primaryContainer
                            val tieneImagen = c.coverImagePath != null && File(c.coverImagePath).exists()
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = colorRespaldo,
                                modifier = Modifier.size(36.dp, 48.dp),
                            ) {
                                if (tieneImagen && c.coverImagePath != null) {
                                    AsyncImage(
                                        model = File(c.coverImagePath),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(c.icono.ifBlank { "📓" }, fontSize = 18.sp)
                                    }
                                }
                            }
                            Text(c.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (estado.paginas.isEmpty() && cuadernosCoincidentes.isEmpty()) {
            EstadoVacio(Icons.Filled.Description, stringResource(R.string.sin_resultados))
        } else {
            Text(
                stringResource(R.string.notas),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(estado.paginas, key = { "b-p-" + it.id }) { p ->
                    TarjetaNotaIndividual(p, estado.nombresCuadernos[p.cuadernoId], vm, alAbrirPagina)
                }
            }
        }
    }
}

/** Diálogo / BottomSheet para crear o editar un cuaderno con selector de foto y color. */
@Composable
fun DialogoCuaderno(
    cuaderno: Cuaderno?,
    alCerrar: () -> Unit,
    alConfirmar: (nombre: String, icono: String, coverImagePath: String?, coverColorHex: String?) -> Unit,
) {
    val contexto = LocalContext.current
    var nombre by remember { mutableStateOf(cuaderno?.nombre ?: "") }
    var icono by remember { mutableStateOf(cuaderno?.icono ?: "📓") }
    var coverImagePath by remember { mutableStateOf(cuaderno?.coverImagePath) }
    var coverColorHex by remember { mutableStateOf(cuaderno?.coverColorHex) }

    val lanzadorFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val notebookId = cuaderno?.id ?: app.kiwu.android.domain.nuevoId()
            val ruta = NotebookCoverStorage.guardarPortada(contexto, notebookId, uri)
            if (ruta != null) {
                coverImagePath = ruta
            }
        }
    }

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(if (cuaderno == null) R.string.cuaderno_nuevo else R.string.cuaderno_editar)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(R.string.nombre)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Foto de portada", style = MaterialTheme.typography.labelMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val tieneFoto = coverImagePath != null && File(coverImagePath!!).exists()
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(48.dp, 64.dp),
                    ) {
                        if (tieneFoto && coverImagePath != null) {
                            AsyncImage(
                                model = File(coverImagePath!!),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = {
                                lanzadorFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        ) {
                            Text(if (tieneFoto) "Cambiar foto" else "Elegir foto")
                        }
                        if (tieneFoto) {
                            TextButton(onClick = { coverImagePath = null }) {
                                Text("Quitar foto", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text("Color de fondo e ícono", style = MaterialTheme.typography.labelMedium)
                // Colores predefinidos
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ColoresPortada) { hex ->
                        val color = hexAColor(hex) ?: Color.Gray
                        val seleccionado = coverColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (seleccionado) 3.dp else 1.dp,
                                    if (seleccionado) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    CircleShape,
                                )
                                .clickable { coverColorHex = hex },
                        )
                    }
                }

                SelectorEmoji(icono) { icono = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    alConfirmar(nombre.trim(), icono, coverImagePath, coverColorHex)
                    alCerrar()
                },
                enabled = nombre.isNotBlank(),
            ) { Text(stringResource(R.string.guardar)) }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text(stringResource(R.string.cancelar)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SelectorEmoji(actual: String, emojis: List<String> = EmojisCuaderno, alElegir: (String) -> Unit) {
    var textoLibre by remember { mutableStateOf("") }

    val categorias = listOf(
        "Estudio" to listOf("📚", "📖", "📝", "📓", "🎓", "🧠", "🎒", "🏫", "✏️", "📋"),
        "Ciencias y Tech" to listOf("🧪", "🔬", "🔭", "🧮", "💻", "⌨️", "📱", "📐", "🌐", "🔋"),
        "Estados y Prioridad" to listOf("⭐", "🔥", "✅", "📌", "🚨", "🎯", "💡", "🛑", "⚠️", "⏳"),
        "Rutina" to listOf("🏃", "🍎", "☕", "🛌", "🧘", "🍽️", "🏋️", "🚿", "🌅", "🌙"),
        "Símbolos" to listOf("❤️", "✨", "🎵", "🎨", "🌱", "🌍", "✈️", "💰", "🎉", "🏆")
    )

    Column(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = textoLibre,
                onValueChange = { textoLibre = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("O escribe un emoji...") },
                singleLine = true
            )
            TextButton(
                onClick = {
                    val trim = textoLibre.trim()
                    if (trim.isNotEmpty()) {
                        val iter = java.text.BreakIterator.getCharacterInstance()
                        iter.setText(trim)
                        val end = iter.next()
                        val primerGrafema = if (end != java.text.BreakIterator.DONE) trim.substring(0, end) else trim
                        alElegir(primerGrafema)
                    }
                },
                enabled = textoLibre.isNotBlank()
            ) { Text(stringResource(R.string.guardar)) }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            for ((cat, lista) in categorias) {
                Text(cat, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (e in lista) {
                        Surface(
                            onClick = { alElegir(e) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (e == actual) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        ) { Text(e, fontSize = 22.sp, modifier = Modifier.padding(6.dp)) }
                    }
                }
            }
        }
    }
}

/** Menú lateral de Notas. Se dibuja sobre toda la pantalla. */
@Composable
fun MenuNotas(vm: NotasViewModel, cerrarYLuego: (() -> Unit) -> Unit, alAjustes: () -> Unit) {
    val contexto = LocalContext.current
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
    if (nuevo) DialogoCuaderno(null, alCerrar = { nuevo = false }) { nombre, icono, foto, color -> vm.crearCuaderno(nombre, icono, foto, color) }
    editar?.let { c -> DialogoCuaderno(c, alCerrar = { editar = null }) { nombre, icono, foto, color -> vm.editarCuaderno(c.id, nombre, icono, foto, color) } }
    eliminar?.let { c ->
        DialogoConfirmar(
            stringResource(R.string.cuaderno_eliminar_titulo, c.nombre), stringResource(R.string.cuaderno_eliminar_texto), stringResource(R.string.cuaderno_eliminar_boton),
            alCerrar = { eliminar = null },
        ) { vm.eliminarCuaderno(c.id, contexto) }
    }
}

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
