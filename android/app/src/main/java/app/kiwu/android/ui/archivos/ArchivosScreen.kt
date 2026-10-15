package app.kiwu.android.ui.archivos

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import app.kiwu.android.domain.Materia
import app.kiwu.android.ui.EstadoVacio
import app.kiwu.android.ui.Fechas
import app.kiwu.android.ui.cargarAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator

private class Elemento(val doc: DocumentFile, val nombre: String, val esCarpeta: Boolean, val tamano: Long, val modificado: Long)

/** Lee una carpeta: carpetas primero y luego por nombre. null si no se puede leer. */
private fun listar(carpeta: DocumentFile): List<Elemento>? {
    if (!carpeta.exists() || !carpeta.canRead()) return null
    val coleccion = Collator.getInstance(java.util.Locale("es"))
    return try {
        carpeta.listFiles()
            .map { Elemento(it, it.name ?: "(sin nombre)", it.isDirectory, it.length(), it.lastModified()) }
            .filter { !it.nombre.startsWith(".") }
            .sortedWith(compareBy<Elemento> { !it.esCarpeta }.thenComparator { a, b -> coleccion.compare(a.nombre, b.nombre) })
    } catch (e: Exception) {
        null
    }
}

/** Explorador de la carpeta del dispositivo vinculada a una lista. Los archivos no se copian. */
@Composable
fun ArchivosScreen(materia: Materia, alVincular: (String?) -> Unit) {
    val contexto = LocalContext.current
    val elegir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                contexto.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) { /* sigue valiendo mientras dure el permiso */ }
            alVincular(uri.toString())
        }
    }

    val raiz = materia.carpeta?.let { DocumentFile.fromTreeUri(contexto, Uri.parse(it)) }
    if (materia.carpeta == null) {
        EstadoVacio(Icons.Filled.FolderOpen, "Vincula una carpeta de tu dispositivo con los archivos de esta lista") {
            Button(onClick = { elegir.launch(null) }, modifier = Modifier.padding(top = 16.dp)) { Text("Elegir carpeta") }
        }
        return
    }

    // Camino recorrido dentro de la carpeta (la última es la que se ve)
    val camino = remember(materia.carpeta) { mutableStateListOf<DocumentFile>() }
    val actual = camino.lastOrNull() ?: raiz
    val elementos by cargarAsync(actual?.uri) {
        actual?.let { withContext(Dispatchers.IO) { listar(it) } }
    }
    var menu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                (listOf(raiz?.name ?: "Carpeta") + camino.map { it.name ?: "" }).joinToString(" › "),
                Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Opciones de la carpeta") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Cambiar carpeta") }, onClick = { menu = false; elegir.launch(null) })
                    DropdownMenuItem(text = { Text("Desvincular (no borra nada)") }, onClick = { menu = false; alVincular(null) })
                }
            }
        }
        val lista = elementos
        when {
            lista == null && actual != null && !(actual.exists() && actual.canRead()) -> EstadoVacio(Icons.Filled.Warning, "La carpeta vinculada no está disponible") {
                Button(onClick = { elegir.launch(null) }, modifier = Modifier.padding(top = 16.dp)) { Text("Elegir de nuevo") }
            }
            lista == null -> Box(Modifier.fillMaxSize())
            else -> LazyColumn(Modifier.fillMaxSize()) {
                if (camino.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth().clickable { camino.removeAt(camino.lastIndex) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.FolderOpen, null, tint = MaterialTheme.colorScheme.primary)
                            Text("Subir un nivel", Modifier.padding(start = 16.dp))
                        }
                    }
                }
                if (lista.isEmpty()) item { Text("La carpeta está vacía", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(lista, key = { it.doc.uri.toString() }) { e ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (e.esCarpeta) camino.add(e.doc) else abrir(contexto, e.doc)
                        }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (e.esCarpeta) Icons.Filled.Folder else Icons.AutoMirrored.Filled.InsertDriveFile, null,
                            tint = if (e.esCarpeta) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(e.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (!e.esCarpeta) {
                                Text(
                                    listOfNotNull(Formatter.formatShortFileSize(contexto, e.tamano), if (e.modificado > 0) Fechas.completa(e.modificado) else null).joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun abrir(contexto: android.content.Context, doc: DocumentFile) {
    try {
        contexto.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(doc.uri, doc.type ?: "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(contexto, "No hay una app para abrir este archivo", Toast.LENGTH_SHORT).show()
    }
}
