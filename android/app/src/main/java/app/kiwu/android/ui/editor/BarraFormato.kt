package app.kiwu.android.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatIndentDecrease
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.kiwu.android.domain.Bloque
import app.kiwu.android.domain.Enlaces
import app.kiwu.android.domain.TiposBloque
import app.kiwu.android.ui.ColoresNota
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import app.kiwu.android.R
import androidx.compose.ui.res.stringResource

/** Resuelve las acciones que necesitan al sistema (elegir imagen o archivo) pedidas por el menú «/» o la barra. */
@Composable
internal fun ManejarAcciones(ed: EstadoEditor, adjuntos: OrigenAdjuntos) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val accion = ed.accionPendiente
    LaunchedEffect(accion) {
        if (accion == null) return@LaunchedEffect
        ed.accionPendiente = null
        when (accion.tipo) {
            TiposBloque.IMAGEN -> adjuntos.elegirImagen { uri ->
                alcance.launch {
                    val datos = withContext(Dispatchers.IO) { imagenParaNota(contexto, uri) }
                    if (datos != null) ed.insertarTras(accion.tras, listOf(Bloque(kind = TiposBloque.IMAGEN, imageData = datos)))
                }
            }
            TiposBloque.ARCHIVO -> adjuntos.elegirArchivo { uri ->
                val nombre = nombreDeArchivo(contexto, uri)
                ed.insertarTras(accion.tras, listOf(Bloque(kind = TiposBloque.ARCHIVO, fileName = nombre, path = uri.toString())))
            }
        }
    }
}

@Composable
private fun Boton(icono: ImageVector, descripcion: String, activo: Boolean = false, alClic: () -> Unit) {
    IconButton(
        onClick = alClic,
        modifier = Modifier.size(42.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (activo) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
            contentColor = if (activo) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        ),
    ) { Icon(icono, descripcion) }
}

@Composable
private fun MenuColores(icono: ImageVector, descripcion: String, resaltado: Boolean, ed: EstadoEditor) {
    var abierto by remember { mutableStateOf(false) }
    val oscuro = esOscuro()
    Box {
        Boton(icono, descripcion) { abierto = true }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            DropdownMenuItem(text = { Text(stringResource(if (resaltado) R.string.sin_resaltado else R.string.sin_color)) }, onClick = { abierto = false; ed.fijarColor(null, resaltado) })
            for ((nombre, par) in ColoresNota.colores) {
                DropdownMenuItem(
                    text = { Text(ColoresNota.titulos[nombre]?.let { stringResource(it) } ?: nombre) },
                    leadingIcon = { Box(Modifier.size(18.dp).background(if (oscuro) par.oscuro else par.claro, CircleShape)) },
                    onClick = { abierto = false; ed.fijarColor(nombre, resaltado) },
                )
            }
        }
    }
}

/** Barra de formato: va sobre el teclado y solo aparece cuando hay un bloque de texto o de código enfocado. */
@Composable
fun BarraFormato(ed: EstadoEditor, adjuntos: OrigenAdjuntos, modifier: Modifier = Modifier) {
    val b = ed.bloqueEnfocado() ?: return
    var dialogoEnlace by remember { mutableStateOf(false) }
    var menuInsertar by remember { mutableStateOf(false) }
    var menuMas by remember { mutableStateOf(false) }
    var menuConvertir by remember { mutableStateOf(false) }
    val esTexto = b.kind in TiposBloque.deTexto

    Surface(modifier.fillMaxWidth().imePadding().navigationBarsPadding(), tonalElevation = 3.dp, shadowElevation = 6.dp) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (esTexto) {
                Boton(Icons.Filled.FormatBold, stringResource(R.string.negrita), ed.activo(b, "b")) { ed.alternarFormato("b") }
                Boton(Icons.Filled.FormatItalic, stringResource(R.string.cursiva), ed.activo(b, "i")) { ed.alternarFormato("i") }
                Boton(Icons.Filled.FormatUnderlined, stringResource(R.string.subrayado), ed.activo(b, "u")) { ed.alternarFormato("u") }
                Boton(Icons.Filled.FormatStrikethrough, stringResource(R.string.tachado), ed.activo(b, "s")) { ed.alternarFormato("s") }
                Boton(Icons.Filled.Code, stringResource(R.string.codigo_en_linea), ed.activo(b, "code")) { ed.alternarFormato("code") }
                MenuColores(Icons.Filled.FormatColorText, stringResource(R.string.color_texto), false, ed)
                MenuColores(Icons.Filled.Highlight, stringResource(R.string.resaltado), true, ed)
                Boton(Icons.Filled.Link, stringResource(R.string.enlace), ed.enlaceBajoCursor(b) != null) { dialogoEnlace = true }
                ed.enlaceBajoCursor(b)?.let { url -> TextButton(onClick = { adjuntos.abrirUrl(url) }) { Text(stringResource(R.string.abrir)) } }
                Boton(Icons.Filled.FormatIndentDecrease, stringResource(R.string.menos_sangria)) { ed.cambiarSangria(b, -1) }
                Boton(Icons.Filled.FormatIndentIncrease, stringResource(R.string.mas_sangria)) { ed.cambiarSangria(b, 1) }
            }
            Box {
                Boton(Icons.Filled.Add, stringResource(R.string.insertar)) { menuInsertar = true }
                DropdownMenu(expanded = menuInsertar, onDismissRequest = { menuInsertar = false }) {
                    for ((tipo, titulo) in listOf(
                        TiposBloque.IMAGEN to stringResource(R.string.bloque_imagen),
                        TiposBloque.ARCHIVO to stringResource(R.string.bloque_archivo),
                        TiposBloque.ENLACE to stringResource(R.string.bloque_enlace_corto),
                        TiposBloque.CODIGO to stringResource(R.string.bloque_codigo_largo),
                        TiposBloque.DIVISOR to stringResource(R.string.bloque_divisor),
                    )) {
                        DropdownMenuItem(text = { Text(titulo) }, onClick = {
                            menuInsertar = false
                            when (tipo) {
                                TiposBloque.IMAGEN, TiposBloque.ARCHIVO -> ed.accionPendiente = AccionPendiente(tipo, b)
                                TiposBloque.DIVISOR -> ed.insertarTras(b, listOf(Bloque(kind = TiposBloque.DIVISOR)))
                                TiposBloque.ENLACE -> ed.insertarTras(b, listOf(Bloque(kind = TiposBloque.ENLACE)))
                                else -> ed.insertarTras(b, listOf(Bloque(kind = tipo)))
                            }
                        })
                    }
                }
            }
            Box {
                Boton(Icons.Filled.MoreVert, stringResource(R.string.bloque_mas_opciones)) { menuMas = true }
                DropdownMenu(expanded = menuMas, onDismissRequest = { menuMas = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.convertir_en)) }, onClick = { menuMas = false; menuConvertir = true })
                    DropdownMenuItem(text = { Text(stringResource(R.string.mover_arriba)) }, onClick = { menuMas = false; ed.mover(b, -1) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.mover_abajo)) }, onClick = { menuMas = false; ed.mover(b, 1) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.duplicar)) }, onClick = { menuMas = false; ed.duplicar(b) })
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.bloque_eliminar), color = MaterialTheme.colorScheme.error) },
                        onClick = { menuMas = false; ed.eliminar(b) },
                    )
                }
                DropdownMenu(expanded = menuConvertir, onDismissRequest = { menuConvertir = false }) {
                    for (tipo in TiposBloque.deTexto + TiposBloque.CODIGO) {
                        DropdownMenuItem(text = { Text(TiposBloque.titulos[tipo]?.let { stringResource(it) } ?: tipo) }, onClick = { menuConvertir = false; ed.convertir(b, tipo) })
                    }
                }
            }
        }
    }

    if (dialogoEnlace) DialogoEnlace(ed, b, adjuntos) { dialogoEnlace = false }
}

@Composable
private fun DialogoEnlace(ed: EstadoEditor, b: BloqueEd, adjuntos: OrigenAdjuntos, alCerrar: () -> Unit) {
    val existente = ed.enlaceBajoCursor(b)
    var direccion by remember { mutableStateOf(existente ?: "") }
    val aplicar = {
        val limpia = direccion.trim()
        if (limpia.isNotEmpty()) ed.aplicarEnlace(Enlaces.urlDesdeTexto(limpia) ?: limpia)
        alCerrar()
    }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(R.string.enlace)) },
        text = {
            OutlinedTextField(
                value = direccion, onValueChange = { direccion = it }, label = { Text(stringResource(R.string.direccion_web)) }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { aplicar() }),
            )
        },
        confirmButton = { TextButton(onClick = aplicar, enabled = direccion.isNotBlank()) { Text(stringResource(R.string.aplicar)) } },
        dismissButton = {
            Row {
                if (existente != null) TextButton(onClick = { ed.aplicarEnlace(null); alCerrar() }) { Text(stringResource(R.string.quitar_enlace)) }
                TextButton(onClick = alCerrar) { Text(stringResource(R.string.cancelar)) }
            }
        },
    )
}
