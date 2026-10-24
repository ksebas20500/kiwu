package app.kiwu.android.ui.tareas

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.KiwuApp
import app.kiwu.android.domain.Racha
import app.kiwu.android.domain.Rachas
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/** Colores de la racha: el naranja del fuego no sigue el color de la app, para que la racha se reconozca siempre. */
private val Fuego = Color(0xFFFF6B1A)
private val FuegoClaro = Color(0xFFFFB347)
private val es = Locale.forLanguageTag("es")
private val iniciales = listOf("LU", "MA", "MI", "JU", "VI", "SA", "DO")

/** Fila de la racha arriba del menú de Tareas: la llama se apaga mientras hoy todavía no cuenta. */
@Composable
internal fun FilaRacha(r: Racha, alClic: () -> Unit) {
    val descripcion = when {
        r.hoyCumplido -> "Racha de ${Rachas.texto(r.actual)}, hoy ya cuenta"
        r.actual > 0 -> "Racha de ${Rachas.texto(r.actual)}, completa una tarea hoy para mantenerla"
        else -> "Sin racha todavía"
    }
    NavigationDrawerItem(
        label = { Text(if (r.actual > 0) Rachas.texto(r.actual) else "Empieza tu racha", maxLines = 1, fontWeight = FontWeight.Medium) },
        icon = { Llama(24.dp, encendida = r.hoyCumplido) },
        selected = false,
        onClick = alClic,
        badge = { if (r.enRiesgo) Text("Hoy falta", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).semantics { contentDescription = descripcion },
    )
}

/**
 * Insignia de la racha para la barra superior de Tareas y Notas: la llama y el número de días. Lee la racha por su cuenta y
 * no se muestra si se ocultó en Ajustes. Con la racha en riesgo, la llama está apagada y lleva el «!».
 */
@Composable
fun InsigniaRacha(app: KiwuApp, alClic: () -> Unit) {
    val visible by remember(app) { app.ajustes.flujo.map { it.rachaVisible } }.collectAsStateWithLifecycle(false)
    val fechas by app.repo.fechasCompletado.collectAsStateWithLifecycle(null)
    val f = fechas
    if (!visible || f == null) return
    val r = remember(f) { Rachas.calcular(f, System.currentTimeMillis()) }
    InsigniaRacha(r, alClic)
}

@Composable
internal fun InsigniaRacha(r: Racha, alClic: () -> Unit) {
    val descripcion = when {
        r.hoyCumplido -> "Racha de ${Rachas.texto(r.actual)}, hoy ya cuenta. Abrir la racha"
        r.enRiesgo -> "Racha de ${Rachas.texto(r.actual)} en riesgo. Abrir la racha"
        else -> "Sin racha todavía. Abrir la racha"
    }
    Surface(
        onClick = alClic,
        shape = RoundedCornerShape(50),
        color = if (r.hoyCumplido) Fuego.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.padding(end = 4.dp).semantics(mergeDescendants = true) { contentDescription = descripcion },
    ) {
        Row(Modifier.padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Llama(24.dp, encendida = r.hoyCumplido, alerta = r.enRiesgo)
            Spacer(Modifier.size(4.dp))
            Text(
                r.actual.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = if (r.hoyCumplido) Fuego else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** La llama: con degradado de fuego si está encendida, gris si no; con [alerta], un «!» naranja en la esquina. */
@Composable
internal fun Llama(tamano: Dp, encendida: Boolean, alerta: Boolean = false, modifier: Modifier = Modifier) {
    val apagada = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
    Box(modifier.size(tamano)) {
        Icon(
            Icons.Filled.LocalFireDepartment, null,
            Modifier.fillMaxSize().then(
                if (encendida) Modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithCache {
                    val degradado = Brush.verticalGradient(listOf(Color(0xFFFF4D1A), Fuego, FuegoClaro))
                    onDrawWithContent { drawContent(); drawRect(degradado, blendMode = BlendMode.SrcIn) }
                } else Modifier,
            ),
            tint = if (encendida) Fuego else apagada,
        )
        if (alerta) {
            Box(
                Modifier.align(Alignment.BottomEnd).size(tamano * 0.36f).background(Fuego, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = (tamano.value * 0.22f).sp) }
        }
    }
}

/**
 * La semana actual (lunes a domingo) con los días cumplidos unidos por una línea. Con [animarHoy] el círculo de hoy se
 * rellena con un rebote (la celebración).
 */
@Composable
internal fun SemanaRacha(r: Racha, hoy: LocalDate, animarHoy: Boolean = false, modifier: Modifier = Modifier) {
    val semana = Rachas.semanas(hoy, 1).first()
    val relleno = remember { Animatable(if (animarHoy) 0f else 1f) }
    LaunchedEffect(animarHoy) {
        if (animarHoy) { delay(700); relleno.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
    }
    val linea = MaterialTheme.colorScheme.outlineVariant
    Row(modifier.fillMaxWidth()) {
        semana.forEachIndexed { i, dia ->
            val hecho = dia in r.dias
            val anteriorHecho = i > 0 && semana[i - 1] in r.dias
            val siguienteHecho = i < 6 && semana[i + 1] in r.dias
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    iniciales[i], style = MaterialTheme.typography.labelMedium,
                    color = if (dia == hoy) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (dia == hoy) FontWeight.Bold else FontWeight.Normal,
                )
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                    // Media línea a cada lado: naranja hacia los días cumplidos vecinos (juntas forman la cadena), gris si no.
                    Canvas(Modifier.fillMaxSize()) {
                        val y = size.height / 2
                        val mitad = size.width / 2
                        if (i > 0) {
                            if (hecho && anteriorHecho) drawLine(Fuego, Offset(0f, y), Offset(mitad, y), 3.dp.toPx())
                            else drawLine(linea, Offset(0f, y), Offset(mitad, y), 1.dp.toPx())
                        }
                        if (i < 6) {
                            if (hecho && siguienteHecho) drawLine(Fuego, Offset(mitad, y), Offset(size.width, y), 3.dp.toPx())
                            else drawLine(linea, Offset(mitad, y), Offset(size.width, y), 1.dp.toPx())
                        }
                    }
                    CirculoDia(dia, hoy, hecho, if (dia == hoy) relleno.value else 1f)
                }
            }
        }
    }
}

@Composable
private fun CirculoDia(dia: LocalDate, hoy: LocalDate, hecho: Boolean, relleno: Float) {
    val esHoy = dia == hoy
    val borde = when {
        esHoy -> MaterialTheme.colorScheme.onSurface
        dia.isAfter(hoy) -> MaterialTheme.colorScheme.outlineVariant
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    Box(
        Modifier.size(38.dp).background(MaterialTheme.colorScheme.surface, CircleShape)
            .border(if (esHoy) 2.dp else 1.dp, borde, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (hecho) {
            Box(Modifier.size(38.dp).scale(relleno).background(Fuego, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, null, Modifier.size(22.dp).alpha(relleno.coerceIn(0f, 1f)), tint = Color.White)
            }
        } else if (esHoy) {
            Llama(20.dp, encendida = false)
        }
    }
}

/** Pantalla de la racha: número grande, semana, cifras, la siguiente meta y el calendario del mes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RachaScreen(app: KiwuApp, alVolver: () -> Unit) {
    val fechas by app.repo.fechasCompletado.collectAsStateWithLifecycle(emptyList())
    val ahora = remember(fechas) { System.currentTimeMillis() }
    val r = remember(fechas, ahora) { Rachas.calcular(fechas, ahora) }
    val hoy = remember(ahora) { LocalDate.now() }
    var ayuda by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                actions = { IconButton(onClick = { ayuda = true }) { Icon(Icons.AutoMirrored.Outlined.HelpOutline, "Cómo funciona la racha") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { relleno ->
        Column(
            Modifier.padding(relleno).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(r.actual.toString(), fontSize = 88.sp, fontWeight = FontWeight.Bold, lineHeight = 88.sp)
                    Text(if (r.actual == 1) "Día de racha" else "Días de racha", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }
                Llama(112.dp, encendida = r.hoyCumplido, alerta = r.enRiesgo)
            }
            SemanaRacha(r, hoy)
            AvisoEstado(r)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Cifra(Modifier.weight(1f), { Llama(22.dp, encendida = true) }, r.mejor.toString(), "Mejor racha")
                Cifra(Modifier.weight(1f), { Icon(Icons.Filled.Today, null, Modifier.size(22.dp), tint = Fuego) }, r.dias.count { !it.isAfter(hoy) }.toString(), "Días cumplidos")
                Cifra(Modifier.weight(1f), { Icon(Icons.Filled.TaskAlt, null, Modifier.size(22.dp), tint = Fuego) }, r.tareas.toString(), "Tareas hechas")
            }
            SiguienteMeta(r)
            CalendarioRacha(r, hoy)
        }
    }
    if (ayuda) AlertDialog(
        onDismissRequest = { ayuda = false },
        title = { Text("Cómo funciona la racha") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("• Un día cuenta si completas al menos una tarea.")
                Text("• Si hoy aún no has completado nada, la racha sigue viva hasta medianoche: la llama se ve gris con un «!».")
                Text("• Los eventos no cuentan, y desmarcar la única tarea del día quita ese día.")
                Text("• A las 20:00 te avisamos si la racha está a punto de perderse (se cambia en Ajustes → Avisos).")
            }
        },
        confirmButton = { TextButton(onClick = { ayuda = false }) { Text("Entendido") } },
    )
}

@Composable
private fun AvisoEstado(r: Racha) {
    val (texto, destacado) = when {
        r.hoyCumplido -> "¡Hoy ya cuenta! Vuelve mañana para seguir sumando." to false
        r.enRiesgo -> "Tu racha está en riesgo: completa una tarea antes de medianoche." to true
        else -> "Completa una tarea hoy para empezar una racha." to false
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (destacado) Fuego.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(texto, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = if (destacado) Fuego else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Cifra(modifier: Modifier, icono: @Composable () -> Unit, valor: String, etiqueta: String) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                icono()
                Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(etiqueta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun SiguienteMeta(r: Racha) {
    val meta = Rachas.siguienteMeta(r.actual)
    val anterior = Rachas.METAS.lastOrNull { it <= r.actual } ?: 0
    val progreso = ((r.actual - anterior).toFloat() / (meta - anterior)).coerceIn(0f, 1f)
    val faltan = meta - r.actual
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.EmojiEvents, null, tint = FuegoClaro)
                Text("Siguiente meta: ${Rachas.texto(meta)}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text("${r.actual}/$meta", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(
                progress = { progreso }, modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Fuego, trackColor = MaterialTheme.colorScheme.surfaceVariant, strokeCap = StrokeCap.Round,
                gapSize = 0.dp, drawStopIndicator = {},
            )
            Text(
                if (faltan == 1) "¡Solo te falta 1 día!" else "Te faltan $faltan días seguidos.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Calendario del mes: los días cumplidos seguidos se unen en una píldora; se puede ir a meses anteriores. */
@Composable
private fun CalendarioRacha(r: Racha, hoy: LocalDate) {
    val actual = YearMonth.from(hoy)
    val primero = r.dias.minOrNull()?.let { YearMonth.from(it) }?.coerceAtMost(actual) ?: actual
    var mes by rememberSaveable { mutableStateOf(actual.toString()) }
    val ym = YearMonth.parse(mes)
    val titulo = ym.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", es)).replaceFirstChar { it.titlecase(es) }
    val formato = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", es)
    fun cuenta(d: LocalDate?) = d != null && d in r.dias && !d.isAfter(hoy)
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { mes = ym.minusMonths(1).toString() }, enabled = ym > primero) { Icon(Icons.Filled.ChevronLeft, "Mes anterior") }
                Text(titulo, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                IconButton(onClick = { mes = ym.plusMonths(1).toString() }, enabled = ym < actual) { Icon(Icons.Filled.ChevronRight, "Mes siguiente") }
            }
            Row {
                for (d in listOf("L", "M", "X", "J", "V", "S", "D")) {
                    Text(d, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
            for (fila in Rachas.mes(ym)) {
                Row(Modifier.padding(vertical = 3.dp)) {
                    fila.forEachIndexed { i, dia ->
                        val hecho = cuenta(dia)
                        val izq = hecho && i > 0 && cuenta(fila[i - 1])
                        val der = hecho && i < 6 && cuenta(fila[i + 1])
                        // La píldora: fondo tenue de lado a lado, redondeado solo en los extremos del tramo.
                        val forma = RoundedCornerShape(
                            topStartPercent = if (izq) 0 else 50, bottomStartPercent = if (izq) 0 else 50,
                            topEndPercent = if (der) 0 else 50, bottomEndPercent = if (der) 0 else 50,
                        )
                        Box(
                            Modifier.weight(1f).height(42.dp).then(if (hecho) Modifier.background(Fuego.copy(alpha = 0.18f), forma) else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (dia != null) DiaCalendario(dia, hoy, hecho, Modifier.semantics { contentDescription = formato.format(dia) + if (hecho) ", cumplido" else "" })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaCalendario(dia: LocalDate, hoy: LocalDate, hecho: Boolean, modifier: Modifier) {
    val esHoy = dia == hoy
    Box(
        modifier.size(36.dp)
            .then(if (hecho) Modifier.background(Fuego, CircleShape) else Modifier)
            .then(if (esHoy) Modifier.border(2.dp, if (hecho) Color.White else MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            dia.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (hecho || esHoy) FontWeight.Bold else FontWeight.Normal,
            color = when {
                hecho -> Color.White
                dia.isAfter(hoy) -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                esHoy -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            },
        )
    }
}

/**
 * Celebración al completar la primera tarea del día (al estilo de Duolingo): la llama entra con un rebote y chispas, el
 * número sube de ayer a hoy y el círculo de hoy se rellena en la semana. Cubre toda la pantalla; «Continuar» o atrás la cierran.
 */
@Composable
fun CelebracionRacha(r: Racha, alCerrar: () -> Unit, modifier: Modifier = Modifier) {
    val hoy = remember { LocalDate.now() }
    val tactil = LocalHapticFeedback.current
    val entrada = remember { Animatable(0f) }
    val chispas = remember { Animatable(0f) }
    var numero by remember { mutableStateOf((r.actual - 1).coerceAtLeast(0)) }
    var mostrarBoton by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        launch { entrada.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 220f)) }
        delay(250)
        launch { chispas.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        delay(350)
        numero = r.actual
        tactil.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(900)
        mostrarBoton = true
    }
    androidx.activity.compose.BackHandler(onBack = alCerrar)
    val latido = rememberInfiniteTransition(label = "latido")
    val brillo by latido.animateFloat(0.85f, 1.1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "brillo")
    val balanceo by latido.animateFloat(-4f, 4f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "balanceo")

    Box(
        modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = entrada.value.coerceIn(0f, 1f)))
            // Se traga los toques para que no lleguen a la pantalla de debajo.
            .clickable(remember { MutableInteractionSource() }, indication = null) {}
            .safeDrawingPadding(),
    ) {
        Column(
            Modifier.align(Alignment.Center).widthIn(max = 420.dp).fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                // Resplandor que late detrás de la llama.
                Canvas(Modifier.fillMaxSize().scale(brillo * entrada.value)) {
                    drawCircle(Brush.radialGradient(listOf(Fuego.copy(alpha = 0.35f), Color.Transparent)))
                }
                // Chispas que salen en círculo y se apagan.
                Canvas(Modifier.fillMaxSize()) {
                    val p = chispas.value
                    if (p > 0f && p < 1f) {
                        val centro = Offset(size.width / 2, size.height / 2)
                        for (i in 0 until 14) {
                            val angulo = Math.toRadians(i * 360.0 / 14 + 10)
                            val distancia = size.minDimension * (0.22f + 0.30f * p) * (if (i % 2 == 0) 1f else 0.8f)
                            val punto = centro + Offset((cos(angulo) * distancia).toFloat(), (sin(angulo) * distancia).toFloat())
                            drawCircle(if (i % 3 == 0) FuegoClaro else Fuego, radius = 5.dp.toPx() * (1f - p * 0.6f), center = punto, alpha = 1f - p)
                        }
                        drawCircle(Fuego.copy(alpha = 0.4f * (1f - p)), radius = size.minDimension * 0.45f * p, style = Stroke(3.dp.toPx()))
                    }
                }
                Llama(140.dp, encendida = true, modifier = Modifier.scale(entrada.value).graphicsLayer { rotationZ = balanceo })
            }
            AnimatedContent(
                targetState = numero,
                transitionSpec = {
                    (slideInVertically(spring(dampingRatio = 0.6f)) { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                },
                label = "numero",
            ) { n -> Text(n.toString(), fontSize = 96.sp, fontWeight = FontWeight.Black, color = Fuego, lineHeight = 96.sp) }
            Text(
                if (r.actual == 1) "¡Empezaste una racha!" else "¡${r.actual} días de racha!",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    r.actual in Rachas.METAS -> "¡Alcanzaste la meta de ${Rachas.texto(r.actual)}!"
                    r.actual == 1 -> "Completa una tarea mañana para llegar a 2 días."
                    else -> "Sigue así: mañana llegas a ${r.actual + 1} días."
                },
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            SemanaRacha(r, hoy, animarHoy = true)
            Spacer(Modifier.height(36.dp))
            // «Continuar» aparece al final, subiendo un poco (ocupa su sitio desde el principio para que nada salte).
            val boton by androidx.compose.animation.core.animateFloatAsState(if (mostrarBoton) 1f else 0f, tween(300), label = "boton")
            Button(
                onClick = alCerrar, enabled = mostrarBoton,
                modifier = Modifier.fillMaxWidth().height(52.dp).graphicsLayer { alpha = boton; translationY = (1f - boton) * 24.dp.toPx() },
                colors = ButtonDefaults.buttonColors(containerColor = Fuego, contentColor = Color.White, disabledContainerColor = Fuego, disabledContentColor = Color.White),
            ) { Text("Continuar", fontWeight = FontWeight.Bold) }
        }
    }
}
