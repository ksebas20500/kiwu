package app.kiwu.android.ui.guia

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import app.kiwu.android.R
import androidx.compose.ui.res.stringResource

private data class PaginaGuia(val emoji: String, @androidx.annotation.StringRes val titulo: Int, @androidx.annotation.StringRes val texto: Int)

private val paginas = listOf(
    PaginaGuia("👋", R.string.guia_1_titulo, R.string.guia_1_texto),
    PaginaGuia("✅", R.string.guia_2_titulo, R.string.guia_2_texto),
    PaginaGuia("📅", R.string.guia_3_titulo, R.string.guia_3_texto),
    PaginaGuia("📝", R.string.guia_4_titulo, R.string.guia_4_texto),
    PaginaGuia("🔥", R.string.guia_5_titulo, R.string.guia_5_texto),
)

/** Guía de inicio: unas pocas pantallas que se pasan deslizando. «Saltar» y «Empezar» llaman a [alTerminar]. */
@Composable
fun GuiaScreen(alTerminar: () -> Unit) {
    val estado = rememberPagerState { paginas.size }
    val alcance = rememberCoroutineScope()
    val ultima = estado.currentPage == paginas.lastIndex
    val descripcionPagina = stringResource(R.string.pagina_n_de, estado.currentPage + 1, paginas.size)
    androidx.activity.compose.BackHandler(enabled = estado.currentPage > 0) { alcance.launch { estado.animateScrollToPage(estado.currentPage - 1) } }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (!ultima) TextButton(onClick = alTerminar) { Text(stringResource(R.string.saltar)) }
        }
        HorizontalPager(estado, Modifier.weight(1f).fillMaxWidth()) { i ->
            val p = paginas[i]
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(Modifier.widthIn(max = 420.dp).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(p.emoji, fontSize = 72.sp)
                    // Sin Surface que fije el color del contenido, el título salía negro: invisible sobre el fondo del modo oscuro.
                    Text(stringResource(p.titulo), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
                    Text(stringResource(p.texto), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 16.dp).semantics { contentDescription = descripcionPagina },
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            repeat(paginas.size) { i ->
                val color by animateColorAsState(
                    if (i == estado.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "punto",
                )
                Box(Modifier.size(8.dp).background(color, CircleShape))
            }
        }
        Button(
            onClick = { if (ultima) alTerminar() else alcance.launch { estado.animateScrollToPage(estado.currentPage + 1) } },
            Modifier.widthIn(max = 420.dp).fillMaxWidth().align(Alignment.CenterHorizontally),
        ) { Text(stringResource(if (ultima) R.string.empezar else R.string.siguiente)) }
    }
}
