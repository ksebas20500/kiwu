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

private data class PaginaGuia(val emoji: String, val titulo: String, val texto: String)

private val paginas = listOf(
    PaginaGuia("👋", "Te damos la bienvenida a Kiwu", "Tus tareas, tus apuntes y tu horario de clases en un solo sitio. Todo se guarda en tu teléfono."),
    PaginaGuia("✅", "Tareas en listas y carpetas", "Crea una lista por materia y agrupa las listas en carpetas. Abre el menú ☰ para verlas y toca la casilla de una tarea para completarla."),
    PaginaGuia("📅", "Fechas, avisos y eventos", "Ponle fecha a una tarea y Kiwu te avisa antes. Los eventos (cumpleaños, charlas) salen en el calendario y no hay que completarlos."),
    PaginaGuia("📝", "Notas y horario", "En Notas escribes apuntes con títulos, listas, código e imágenes. En Horario añades tus clases y ves las tareas de cada una."),
    PaginaGuia("🔥", "Tu racha", "Completa al menos una tarea al día para mantener tu racha. Por la tarde te avisamos si está a punto de perderse."),
)

/** Guía de inicio: unas pocas pantallas que se pasan deslizando. «Saltar» y «Empezar» llaman a [alTerminar]. */
@Composable
fun GuiaScreen(alTerminar: () -> Unit) {
    val estado = rememberPagerState { paginas.size }
    val alcance = rememberCoroutineScope()
    val ultima = estado.currentPage == paginas.lastIndex
    androidx.activity.compose.BackHandler(enabled = estado.currentPage > 0) { alcance.launch { estado.animateScrollToPage(estado.currentPage - 1) } }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (!ultima) TextButton(onClick = alTerminar) { Text("Saltar") }
        }
        HorizontalPager(estado, Modifier.weight(1f).fillMaxWidth()) { i ->
            val p = paginas[i]
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(Modifier.widthIn(max = 420.dp).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(p.emoji, fontSize = 72.sp)
                    Text(p.titulo, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Text(p.texto, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 16.dp).semantics { contentDescription = "Página ${estado.currentPage + 1} de ${paginas.size}" },
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
        ) { Text(if (ultima) "Empezar" else "Siguiente") }
    }
}
