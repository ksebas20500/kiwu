package app.kiwu.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.min
import app.kiwu.android.R
import androidx.compose.ui.res.stringResource

private val ALTO_FILA = 52.dp
private const val FILAS_VISIBLES = 5
private const val VUELTAS = 400

/** Rueda de números que gira sin fin y se detiene en uno (como el selector de la alarma del reloj). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Rueda(
    cantidad: Int, valor: Int, descripcion: String, modifier: Modifier = Modifier,
    formato: (Int) -> String = { "%02d".format(it) }, alCambiar: (Int) -> Unit,
) {
    val inicio = VUELTAS / 2 * cantidad + valor.coerceIn(0, cantidad - 1) - FILAS_VISIBLES / 2
    val lista = rememberLazyListState(initialFirstVisibleItemIndex = inicio)
    val altoPx = with(LocalDensity.current) { ALTO_FILA.toPx() }
    // El número del centro: el primero visible más dos filas, redondeando hacia la fila más cercana.
    val centro by remember(lista) {
        derivedStateOf {
            val desplazadas = lista.firstVisibleItemIndex + if (lista.firstVisibleItemScrollOffset > altoPx / 2) 1 else 0
            (desplazadas + FILAS_VISIBLES / 2) % cantidad
        }
    }
    // Se avisa de cada cambio del número del centro, también mientras gira: el último aviso es siempre el valor en el que
    // la rueda se detiene (avisar solo al parar podía dejar un valor anterior, como 11:59 en vez de 12:00).
    LaunchedEffect(lista) {
        snapshotFlow { centro }.collect { alCambiar(it) }
    }
    Box(modifier.height(ALTO_FILA * FILAS_VISIBLES).semantics { contentDescription = descripcion }, contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxWidth().height(ALTO_FILA).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
        )
        LazyColumn(state = lista, flingBehavior = rememberSnapFlingBehavior(lista)) {
            items(VUELTAS * cantidad) { i ->
                val n = i % cantidad
                // Distancia (en filas) al número del centro, mirando por el camino corto de la rueda.
                val d = abs(n - centro).let { min(it, cantidad - it) }
                Box(Modifier.height(ALTO_FILA).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        formato(n),
                        Modifier.alpha(if (d == 0) 1f else if (d == 1) 0.55f else 0.28f),
                        fontSize = if (d == 0) 30.sp else 24.sp,
                        fontWeight = if (d == 0) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** Minuto al múltiplo de [paso] más cercano; si redondea a 60 vuelve a 0 (por ejemplo, 58 con paso 5 → 0). */
internal fun minutoAjustado(minuto: Int, paso: Int): Int = Math.round(minuto / paso.toFloat()) % (60 / paso) * paso

/** Dos ruedas, horas (00–23) y minutos (de [paso] en [paso]: 1 = todos, 5 = 00, 05, 10…), separadas por dos puntos. */
@Composable
fun RuedaHora(hora: Int, minuto: Int, paso: Int = 1, alCambiar: (Int, Int) -> Unit) {
    val minutos = 60 / paso
    var h by remember { mutableIntStateOf(hora.coerceIn(0, 23)) }
    var m by remember { mutableIntStateOf(minutoAjustado(minuto, paso)) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Rueda(24, h, stringResource(R.string.horas), Modifier.width(88.dp)) { h = it; alCambiar(h, m) }
        Text(":", Modifier.width(20.dp), fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Rueda(minutos, m / paso, stringResource(R.string.minutos), Modifier.width(88.dp), formato = { "%02d".format(it * paso) }) { m = it * paso; alCambiar(h, m) }
    }
}
