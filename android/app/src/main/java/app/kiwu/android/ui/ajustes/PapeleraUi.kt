package app.kiwu.android.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.KiwuApp
import app.kiwu.android.R
import kotlinx.coroutines.launch

/** Lo último que se borró (tareas, eventos y notas, hasta 100 de cada) para devolverlo a su lista o cuaderno. */
@Composable
fun SeccionPapelera(app: KiwuApp) {
    val deberes by app.repo.deberesEliminados.collectAsStateWithLifecycle(emptyList())
    val paginas by app.repo.paginasEliminadas.collectAsStateWithLifecycle(emptyList())
    val alcance = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (deberes.isEmpty() && paginas.isEmpty()) Text(stringResource(R.string.papelera_vacia), color = MaterialTheme.colorScheme.onSurfaceVariant)
        for (d in deberes) Fila(d.titulo) { alcance.launch { app.repo.restaurarDeber(d.id) } }
        for (p in paginas) Fila(p.titulo.ifBlank { stringResource(R.string.sin_titulo) }) { alcance.launch { app.repo.restaurarPagina(p.id) } }
    }
}

@Composable
private fun Fila(titulo: String, restaurar: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(titulo, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        TextButton(onClick = restaurar) { Text(stringResource(R.string.restaurar)) }
    }
}
