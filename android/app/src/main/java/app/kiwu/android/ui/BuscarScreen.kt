package app.kiwu.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.KiwuApp
import app.kiwu.android.R

/** Búsqueda en todo Kiwu: tareas (y eventos) por título, y notas por título o texto. */
@Composable
fun BuscarScreen(app: KiwuApp, alVolver: () -> Unit, alAbrirTarea: (String) -> Unit, alAbrirPagina: (String) -> Unit) {
    val deberes by app.repo.deberes.collectAsStateWithLifecycle(emptyList())
    val paginas by app.repo.paginas.collectAsStateWithLifecycle(emptyList())
    var q by rememberSaveable { mutableStateOf("") }
    val t = q.trim()
    val tareas = remember(deberes, t) { if (t.isEmpty()) emptyList() else deberes.filter { it.titulo.contains(t, true) || it.etiquetas.contains(t.removePrefix("#"), true) }.take(50) }
    val notas = remember(paginas, t) { if (t.isEmpty()) emptyList() else paginas.filter { it.titulo.contains(t, true) || it.textoPlano.contains(t, true) }.take(50) }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cerrar_busqueda)) }
                TextField(
                    q, { q = it }, Modifier.weight(1f).padding(end = 12.dp), singleLine = true,
                    placeholder = { Text(stringResource(R.string.buscar_todo)) },
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                )
            }
        },
    ) { relleno ->
        if (t.isNotEmpty() && tareas.isEmpty() && notas.isEmpty()) {
            Text(stringResource(R.string.sin_resultados), Modifier.padding(relleno).padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(Modifier.padding(relleno).fillMaxSize()) {
            if (tareas.isNotEmpty()) item { Encabezado(stringResource(R.string.tareas)) }
            items(tareas, key = { "t" + it.id }) { Fila(it.titulo) { alAbrirTarea(it.id) } }
            if (notas.isNotEmpty()) item { Encabezado(stringResource(R.string.notas)) }
            items(notas, key = { "n" + it.id }) { Fila(it.titulo.ifBlank { stringResource(R.string.sin_titulo) }) { alAbrirPagina(it.id) } }
        }
    }
}

@Composable
private fun Encabezado(texto: String) =
    Text(texto, Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

@Composable
private fun Fila(texto: String, alTocar: () -> Unit) =
    Column(Modifier.fillMaxWidth().clickable(onClick = alTocar).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(texto, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
