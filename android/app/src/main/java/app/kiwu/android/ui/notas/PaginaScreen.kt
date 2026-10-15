package app.kiwu.android.ui.notas

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.domain.Documento
import app.kiwu.android.ui.Fechas
import app.kiwu.android.ui.editor.BarraFormato
import app.kiwu.android.ui.editor.EditorBloques
import app.kiwu.android.ui.editor.OrigenAdjuntos
import app.kiwu.android.ui.editor.rememberEstadoEditor

/** Una página de nota: icono, título y editor por bloques. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaginaScreen(vm: PaginaViewModel, adjuntos: OrigenAdjuntos, alVolver: () -> Unit) {
    val pagina by vm.pagina.collectAsStateWithLifecycle()
    val titulo by vm.titulo.collectAsStateWithLifecycle()
    val icono by vm.icono.collectAsStateWithLifecycle()
    val cuadernos by vm.cuadernos.collectAsStateWithLifecycle(emptyList())
    var menu by remember { mutableStateOf(false) }
    var mover by remember { mutableStateOf(false) }
    var elegirIcono by remember { mutableStateOf(false) }

    DisposableEffect(vm) { onDispose { vm.alSalir() } }

    val p = pagina ?: run { Box(Modifier.fillMaxSize()); return }
    val editor = rememberEstadoEditor(remember(p.id) { Documento.deJson(p.contenido) }, vm::cambioDocumento)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                actions = {
                    IconButton(onClick = { vm.favorita(!p.favorita) }) {
                        Icon(
                            if (p.favorita) Icons.Filled.Star else Icons.Filled.StarBorder, if (p.favorita) "Quitar de favoritas" else "Añadir a favoritas",
                            tint = if (p.favorita) Color(0xFFE0A100) else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Más opciones") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            if (cuadernos.size > 1) DropdownMenuItem(text = { Text("Mover a otro cuaderno…") }, onClick = { menu = false; mover = true })
                            DropdownMenuItem(
                                text = { Text("Eliminar nota", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; vm.eliminar(); alVolver() },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = { BarraFormato(editor, adjuntos) },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { relleno ->
        Column(Modifier.padding(relleno).consumeWindowInsets(relleno).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { elegirIcono = true }) { Text(icono.ifBlank { "📄" }, fontSize = 30.sp) }
                TextField(
                    value = titulo, onValueChange = vm::cambioTitulo, modifier = Modifier.weight(1f),
                    placeholder = { Text("Sin título", fontSize = 28.sp) },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
            }
            Text(
                "Editado " + Fechas.completa(p.actualizada), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
            )
            EditorBloques(editor, adjuntos, Modifier.fillMaxWidth())
        }
    }

    if (elegirIcono) {
        AlertDialog(
            onDismissRequest = { elegirIcono = false }, title = { Text("Icono de la nota") },
            text = { SelectorEmoji(icono) { vm.cambioIcono(it); elegirIcono = false } },
            confirmButton = { TextButton(onClick = { vm.cambioIcono(""); elegirIcono = false }) { Text("Quitar icono") } },
            dismissButton = { TextButton(onClick = { elegirIcono = false }) { Text("Cancelar") } },
        )
    }
    if (mover) {
        AlertDialog(
            onDismissRequest = { mover = false }, title = { Text("Mover a…") },
            text = {
                Column {
                    for (c in cuadernos.filter { it.id != p.cuadernoId }) {
                        TextButton(onClick = { mover = false; vm.mover(c.id) }) { Text("${c.icono} ${c.nombre}") }
                    }
                }
            },
            confirmButton = {}, dismissButton = { TextButton(onClick = { mover = false }) { Text("Cancelar") } },
        )
    }
}
