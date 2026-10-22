package app.kiwu.android.ui.tareas

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.kiwu.android.domain.Columna
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.Elemento
import app.kiwu.android.ui.Chip
import app.kiwu.android.ui.ColorPastel
import app.kiwu.android.ui.EstadoVacio
import app.kiwu.android.ui.Pasteles
import kotlinx.coroutines.delay
import app.kiwu.android.domain.esEvento
import kotlin.math.roundToInt

/** Tarea que se está arrastrando: dónde está el dedo, cuánto mide la tarjeta y en qué punto de ella se agarró. */
private class Arrastre(val tarea: Deber, val color: ColorPastel?, val tamano: IntSize, val agarre: Offset, var dedo: Offset)

/** Tablero: Por hacer · En curso · Hecho. La tarea se mueve arrastrándola (pulsación larga) o con su menú. */
@Composable
fun Tablero(vm: TareasViewModel, estado: TareasEstado, alAbrir: (String) -> Unit, alCrearLista: () -> Unit) {
    // Los eventos no se completan: no van en el tablero.
    val todas = (estado.pendientes + estado.completados).filter { !it.esEvento }
    val colores = remember(todas.map { it.id }) { Pasteles.asignar(todas.map { it.id }) }
    val puedeCrear = estado.elemento != Elemento.Completadas

    var arrastre by remember { mutableStateOf<Arrastre?>(null) }
    var dedo by remember { mutableStateOf(Offset.Zero) }
    var raiz by remember { mutableStateOf(Offset.Zero) }
    var zonaColumnas by remember { mutableStateOf(Rect.Zero) }
    val limites = remember { mutableStateMapOf<Columna, Rect>() }
    val listas = Columna.entries.associateWith { rememberLazyListState() }
    val scrollLateral = rememberScrollState()
    val tactil = LocalHapticFeedback.current
    val densidad = LocalDensity.current

    val destino = arrastre?.let { columnaEn(dedo, limites) }

    // Desplazamiento automático al acercarse a un borde mientras se arrastra.
    LaunchedEffect(arrastre != null) {
        if (arrastre == null) return@LaunchedEffect
        val borde = with(densidad) { 56.dp.toPx() }
        val maxima = with(densidad) { 14.dp.toPx() }
        while (true) {
            delay(16)
            val lateral = velocidadDeDesplazamiento(dedo.x, zonaColumnas.left, zonaColumnas.right, borde, maxima)
            if (lateral != 0f) scrollLateral.dispatchRawDelta(lateral)
            val columna = columnaEn(dedo, limites)
            val rect = columna?.let { limites[it] }
            if (columna != null && rect != null) {
                val vertical = velocidadDeDesplazamiento(dedo.y, rect.top, rect.bottom, borde, maxima)
                if (vertical != 0f) listas.getValue(columna).dispatchRawDelta(vertical)
            }
        }
    }

    Box(Modifier.fillMaxSize().onGloballyPositioned { raiz = it.positionInRoot() }) {
        Column(Modifier.fillMaxSize()) {
            if (estado.cargado && estado.materiasDelContexto.isEmpty()) {
                EstadoVacio(Icons.Filled.CheckCircle, "Crea una lista para empezar", Modifier.weight(1f)) {
                    Button(onClick = alCrearLista, modifier = Modifier.padding(top = 16.dp)) { Text("Crear lista") }
                }
            } else {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().onGloballyPositioned { zonaColumnas = it.boundsInRoot() }) {
                    // En pantallas anchas las tres columnas caben; en el teléfono se desliza de lado.
                    val ancha = maxWidth >= 840.dp
                    val anchoColumna: Dp = if (ancha) (maxWidth - 40.dp) / 3 else 300.dp
                    Row(
                        Modifier.fillMaxSize().then(if (ancha) Modifier else Modifier.horizontalScroll(scrollLateral))
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (columna in Columna.entries) {
                            val tareas = todas.filter { vm.reglas.columnaDe(it) == columna }
                            ColumnaTablero(
                                columna, tareas, estado, colores, vm, alAbrir,
                                modifier = Modifier.width(anchoColumna).fillMaxHeight().onGloballyPositioned { limites[columna] = it.boundsInRoot() },
                                lista = listas.getValue(columna),
                                resaltada = arrastre != null && destino == columna && destino != vm.reglas.columnaDe(arrastre!!.tarea),
                                arrastrando = arrastre?.tarea?.id,
                                alEmpezar = { d, color, tamano, agarre, punto ->
                                    tactil.performHapticFeedback(HapticFeedbackType.LongPress)
                                    dedo = punto
                                    arrastre = Arrastre(d, color, tamano, agarre, punto)
                                },
                                alArrastrar = { delta -> dedo += delta },
                                alSoltar = {
                                    val a = arrastre
                                    arrastre = null
                                    val dest = columnaEn(dedo, limites)
                                    if (a != null && dest != null && dest != vm.reglas.columnaDe(a.tarea)) {
                                        tactil.performHapticFeedback(HapticFeedbackType.LongPress)
                                        vm.mover(a.tarea.id, dest)
                                    }
                                },
                                alCancelar = { arrastre = null },
                            )
                        }
                    }
                }
            }
            if (puedeCrear) CampoNuevaTarea(estado.materiasDelContexto, estado.materiaActual, alCrear = { t, m, f -> vm.crearTarea(t, m, f) })
        }

        // La tarjeta que flota bajo el dedo.
        arrastre?.let { a ->
            val posicion = dedo - a.agarre - raiz
            Box(
                Modifier.offset { IntOffset(posicion.x.roundToInt(), posicion.y.roundToInt()) }
                    .width(with(densidad) { a.tamano.width.toDp() })
                    .shadow(8.dp, RoundedCornerShape(12.dp)),
            ) { TarjetaBase(a.tarea, estado, a.color, vm, conMenu = false) }
        }
    }
}

@Composable
private fun ColumnaTablero(
    columna: Columna,
    tareas: List<Deber>,
    estado: TareasEstado,
    colores: Map<String, ColorPastel>,
    vm: TareasViewModel,
    alAbrir: (String) -> Unit,
    modifier: Modifier,
    lista: LazyListState,
    resaltada: Boolean,
    arrastrando: String?,
    alEmpezar: (Deber, ColorPastel?, IntSize, Offset, Offset) -> Unit,
    alArrastrar: (Offset) -> Unit,
    alSoltar: () -> Unit,
    alCancelar: () -> Unit,
) {
    val forma = RoundedCornerShape(14.dp)
    Surface(
        modifier.then(if (resaltada) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, forma) else Modifier),
        shape = forma,
        color = if (resaltada) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(Modifier.padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(columna.titulo, style = MaterialTheme.typography.titleSmall)
                Text(
                    tareas.size.toString(), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp),
                )
            }
            LazyColumn(state = lista, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tareas, key = { it.id }) { d ->
                    TarjetaTarea(d, estado, colores[d.id], vm, alAbrir, d.id == arrastrando, alEmpezar, alArrastrar, alSoltar, alCancelar)
                }
            }
        }
    }
}

/** Tarjeta con sus gestos: toque abre la tarea, pulsación larga y arrastre la mueve de columna. */
@Composable
private fun TarjetaTarea(
    d: Deber,
    estado: TareasEstado,
    color: ColorPastel?,
    vm: TareasViewModel,
    alAbrir: (String) -> Unit,
    oculta: Boolean,
    alEmpezar: (Deber, ColorPastel?, IntSize, Offset, Offset) -> Unit,
    alArrastrar: (Offset) -> Unit,
    alSoltar: () -> Unit,
    alCancelar: () -> Unit,
) {
    var origen by remember { mutableStateOf(Offset.Zero) }
    var tamano by remember { mutableStateOf(IntSize.Zero) }
    TarjetaBase(
        d, estado, color, vm, conMenu = true,
        modifier = Modifier.alpha(if (oculta) 0.25f else 1f)
            .onGloballyPositioned { origen = it.positionInRoot(); tamano = it.size }
            .clickable { alAbrir(d.id) }
            .pointerInput(d.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { local -> alEmpezar(d, color, tamano, local, origen + local) },
                    onDrag = { cambio, delta -> cambio.consume(); alArrastrar(delta) },
                    onDragEnd = alSoltar,
                    onDragCancel = alCancelar,
                )
            },
    )
}

/** Aspecto de una tarjeta de tarea (la misma que se ve en la columna y flotando mientras se arrastra). */
@Composable
private fun TarjetaBase(d: Deber, estado: TareasEstado, color: ColorPastel?, vm: TareasViewModel, conMenu: Boolean, modifier: Modifier = Modifier) {
    var menu by remember { mutableStateOf(false) }
    val fondo = color?.fondo ?: MaterialTheme.colorScheme.surface
    val texto = color?.texto ?: MaterialTheme.colorScheme.onSurface
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = fondo, contentColor = texto) {
        Row(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    d.titulo, style = MaterialTheme.typography.bodyLarge, maxLines = 3,
                    textDecoration = if (d.completado) TextDecoration.LineThrough else null,
                )
                if (d.fechaEntrega != null || estado.materiaActual == null) {
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        ChipFecha(d, estado.ahora, vm.reglas)
                        if (estado.materiaActual == null) estado.nombres[d.materiaId]?.let { Chip(it) }
                    }
                }
            }
            if (conMenu) {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Opciones de la tarea") }
                    MenuTarea(menu, { menu = false }, vm.reglas.columnaDe(d), { vm.mover(d.id, it) }, { vm.eliminarTarea(d.id) })
                }
            }
        }
    }
}
