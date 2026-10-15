package app.kiwu.android.ui.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.kiwu.android.domain.Enlaces
import app.kiwu.android.domain.Lenguajes
import app.kiwu.android.domain.Resaltador
import app.kiwu.android.domain.TiposBloque
import app.kiwu.android.ui.cargarAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.Base64

/** Menú de un bloque: mover, duplicar, eliminar y lo propio de cada tipo. */
@Composable
internal fun MenuBloque(ed: EstadoEditor, b: BloqueEd, extra: @Composable (cerrar: () -> Unit) -> Unit = {}) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.MoreVert, "Opciones del bloque") }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            extra { abierto = false }
            DropdownMenuItem(text = { Text("Mover arriba") }, onClick = { abierto = false; ed.mover(b, -1) })
            DropdownMenuItem(text = { Text("Mover abajo") }, onClick = { abierto = false; ed.mover(b, 1) })
            DropdownMenuItem(text = { Text("Duplicar") }, onClick = { abierto = false; ed.duplicar(b) })
            DropdownMenuItem(
                text = { Text("Eliminar bloque", color = MaterialTheme.colorScheme.error) },
                onClick = { abierto = false; ed.eliminar(b) },
            )
        }
    }
}

// --- divisor -----------------------------------------------------------------------------------

@Composable
internal fun BloqueDivisorUi(ed: EstadoEditor, b: BloqueEd) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f))
        MenuBloque(ed, b)
    }
}

// --- código ------------------------------------------------------------------------------------

private fun colorDe(token: Resaltador.Token?, oscuro: Boolean): Color {
    fun c(claro: Long, os: Long) = Color(if (oscuro) os else claro)
    return when (token) {
        null -> c(0xFF1F1F1F, 0xFFD4D4D4)
        Resaltador.Token.PALABRA -> c(0xFF0000FF, 0xFF569CD6)
        Resaltador.Token.CONTROL -> c(0xFFAF00DB, 0xFFC586C0)
        Resaltador.Token.CADENA -> c(0xFFA31515, 0xFFCE9178)
        Resaltador.Token.COMENTARIO -> c(0xFF008000, 0xFF6A9955)
        Resaltador.Token.NUMERO -> c(0xFF098658, 0xFFB5CEA8)
        Resaltador.Token.TIPO -> c(0xFF267F99, 0xFF4EC9B0)
        Resaltador.Token.FUNCION -> c(0xFF795E26, 0xFFDCDCAA)
        Resaltador.Token.VARIABLE -> c(0xFF001080, 0xFF9CDCFE)
        Resaltador.Token.ETIQUETA -> c(0xFF800000, 0xFF569CD6)
        Resaltador.Token.ATRIBUTO -> c(0xFFE50000, 0xFF9CDCFE)
        Resaltador.Token.PROPIEDAD -> c(0xFF0451A5, 0xFF9CDCFE)
        Resaltador.Token.DECORADOR -> c(0xFF795E26, 0xFFDCDCAA)
    }
}

@Composable
internal fun BloqueCodigoUi(ed: EstadoEditor, b: BloqueEd) {
    val oscuro = esOscuro()
    val foco = remember { FocusRequester() }
    val portapapeles = LocalClipboardManager.current
    var menuLenguaje by remember { mutableStateOf(false) }
    PedirFoco(ed, b, foco)

    val automatico = b.language == null
    val lenguaje = b.language ?: Lenguajes.detectar(b.texto)
    val transformacion = remember(lenguaje, oscuro) {
        VisualTransformation { t ->
            val visible = t.text
            val anotado = buildAnnotatedString {
                append(visible)
                // El primer carácter es el invisible: se desplazan los tramos una posición.
                for (tramo in Resaltador.tramos(visible.removePrefix(ZW), lenguaje)) {
                    addStyle(SpanStyle(color = colorDe(tramo.token, oscuro)), tramo.inicio + 1, tramo.fin + 1)
                }
            }
            TransformedText(anotado, OffsetMapping.Identity)
        }
    }
    Surface(
        Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(10.dp),
        color = if (oscuro) Color(0xFF1E1E1E) else Color(0xFFF5F5F5),
    ) {
        Column {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    TextButton(onClick = { menuLenguaje = true }) {
                        Text(if (automatico) "Automático · ${Lenguajes.nombre(lenguaje)}" else Lenguajes.nombre(lenguaje), fontSize = 12.sp)
                        Icon(Icons.Filled.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = menuLenguaje, onDismissRequest = { menuLenguaje = false }) {
                        DropdownMenuItem(text = { Text("Automático") }, onClick = { menuLenguaje = false; b.language = null; ed.marcarCambio() })
                        for ((id, nombre) in Lenguajes.nombres) {
                            DropdownMenuItem(text = { Text(nombre) }, onClick = { menuLenguaje = false; b.language = id; ed.marcarCambio() })
                        }
                    }
                }
                Box(Modifier.weight(1f))
                IconButton(onClick = { portapapeles.setText(AnnotatedString(b.texto)) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.ContentCopy, "Copiar código", Modifier.size(18.dp))
                }
                MenuBloque(ed, b)
            }
            Box(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                BasicTextField(
                    value = b.contenido.campo,
                    onValueChange = { ed.alEscribir(b, it) },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp, color = colorDe(null, oscuro)),
                    cursorBrush = SolidColor(colorDe(null, oscuro)),
                    visualTransformation = transformacion,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Default),
                    modifier = Modifier.focusRequester(foco).onFocusChanged {
                        if (it.isFocused) ed.enfocado = b.id else if (ed.enfocado == b.id) ed.enfocado = null
                    },
                )
            }
        }
    }
}

// --- imagen ------------------------------------------------------------------------------------

@Composable
internal fun BloqueImagenUi(ed: EstadoEditor, b: BloqueEd) {
    val bitmap by cargarAsync(b.imageData) {
        b.imageData?.let { datos ->
            withContext(Dispatchers.Default) {
                try {
                    val bytes = Base64.getDecoder().decode(datos)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        val img = bitmap
        if (img != null) {
            Image(img.asImageBitmap(), "Imagen de la nota", Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.FillWidth)
        } else {
            Text("No se puede mostrar la imagen", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.align(Alignment.TopEnd)) { MenuBloque(ed, b) }
    }
}

// --- archivo -----------------------------------------------------------------------------------

internal fun mimeDe(nombre: String?): String? = when (nombre?.substringAfterLast('.', "")?.lowercase()) {
    "pdf" -> "application/pdf"
    "doc" -> "application/msword"
    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    "odt" -> "application/vnd.oasis.opendocument.text"
    "txt" -> "text/plain"
    else -> null
}

@Composable
internal fun BloqueArchivoUi(ed: EstadoEditor, b: BloqueEd, adjuntos: OrigenAdjuntos) {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val vinculado = b.path != null
    Surface(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
            val ruta = b.path
            if (ruta != null) adjuntos.abrirArchivo(ruta, mimeDe(b.fileName)) else adjuntos.elegirArchivo { uri ->
                b.path = uri.toString()
                if (b.fileName.isNullOrEmpty()) b.fileName = nombreDeArchivo(contexto, uri)
                ed.marcarCambio()
            }
        },
        shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Description, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(b.fileName ?: "Archivo", maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (vinculado) "Toca para abrir" else "Sin vincular: toca para elegir el archivo en este dispositivo",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { ed.eliminar(b) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Close, "Quitar adjunto (no borra el archivo)") }
        }
    }
}

// --- enlace con vista previa -------------------------------------------------------------------

private fun JsonObjectTexto(b: BloqueEd, clave: String): String? = (b.preview?.get(clave) as? JsonPrimitive)?.contentOrNull

@Composable
internal fun BloqueEnlaceUi(ed: EstadoEditor, b: BloqueEd, adjuntos: OrigenAdjuntos) {
    val alcance = rememberCoroutineScope()
    val direccion = b.url
    var reproduciendo by remember { mutableStateOf(false) }

    suspend fun cargar() {
        val url = b.url ?: return
        b.cargandoVistaPrevia = true
        b.vistaPrevioFallida = false
        val vp = adjuntos.vistaPrevia(url)
        b.cargandoVistaPrevia = false
        if (vp != null) {
            b.preview = vp.aJson()
            ed.marcarCambio()
        } else {
            b.vistaPrevioFallida = true
        }
    }
    LaunchedEffect(direccion) {
        if (direccion != null && b.preview == null && !b.vistaPrevioFallida) cargar()
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (direccion == null) {
            var entrada by remember { mutableStateOf("") }
            var error by remember { mutableStateOf(false) }
            val foco = remember { FocusRequester() }
            PedirFoco(ed, b, foco)
            val confirmar = {
                val url = Enlaces.urlDesdeTexto(entrada)
                if (url == null) error = true else { b.url = url; b.vistaPrevioFallida = false; ed.marcarCambio() }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.OutlinedTextField(
                    value = entrada, onValueChange = { entrada = it; error = false },
                    modifier = Modifier.weight(1f).focusRequester(foco),
                    label = { Text("Pega un enlace (web, YouTube o Vimeo)") }, singleLine = true, isError = error,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Go, keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri),
                    keyboardActions = KeyboardActions(onGo = { confirmar() }),
                )
                TextButton(onClick = { confirmar() }) { Text("Añadir") }
                MenuBloque(ed, b)
            }
        } else {
            val titulo = JsonObjectTexto(b, "titulo")
            val descripcion = JsonObjectTexto(b, "descripcion")
            val sitio = JsonObjectTexto(b, "sitio") ?: direccion
            val proveedor = JsonObjectTexto(b, "proveedor")
            val videoId = JsonObjectTexto(b, "videoID")
            val esVideo = proveedor != null && videoId != null
            val imagen by cargarAsync(b.preview) {
                JsonObjectTexto(b, "imagen")?.let { datos ->
                    withContext(Dispatchers.Default) {
                        try {
                            val bytes = Base64.getDecoder().decode(datos)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (e: Exception) { null }
                    }
                }
            }
            Surface(
                Modifier.fillMaxWidth().clickable { if (esVideo) reproduciendo = true else adjuntos.abrirUrl(direccion) },
                shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            ) {
                Column {
                    imagen?.let { img ->
                        Box {
                            Image(img.asImageBitmap(), null, Modifier.fillMaxWidth().aspectRatio(1.91f), contentScale = ContentScale.Crop)
                            if (esVideo) {
                                Surface(Modifier.align(Alignment.Center).size(52.dp), shape = RoundedCornerShape(99.dp), color = Color.Black.copy(alpha = 0.6f)) {
                                    Icon(Icons.Filled.PlayArrow, "Reproducir", Modifier.padding(10.dp), tint = Color.White)
                                }
                            }
                        }
                    }
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(sitio, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            Text(
                                titulo ?: if (b.cargandoVistaPrevia) "Cargando vista previa…" else direccion,
                                style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                            if (!descripcion.isNullOrEmpty()) {
                                Text(descripcion, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            if (b.vistaPrevioFallida && titulo == null) {
                                Text("No se pudo cargar la vista previa", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        MenuBloque(ed, b) { cerrar ->
                            DropdownMenuItem(text = { Text("Abrir en el navegador") }, onClick = { cerrar(); adjuntos.abrirUrl(direccion) })
                            DropdownMenuItem(text = { Text("Actualizar vista previa") }, onClick = {
                                cerrar(); b.preview = null; alcance.launch { cargar() }
                            })
                            DropdownMenuItem(text = { Text("Convertir en texto") }, onClick = { cerrar(); ed.enlaceATexto(b) })
                        }
                    }
                }
            }
            if (reproduciendo && proveedor != null && videoId != null) {
                ReproductorDialog(Enlaces.urlIncrustada(proveedor, videoId)) { reproduciendo = false }
            }
        }
    }
}

@Composable
private fun ReproductorDialog(url: String, alCerrar: () -> Unit) {
    Dialog(onDismissRequest = alCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(8.dp), shape = RoundedCornerShape(12.dp), color = Color.Black) {
            Box {
                var web by remember { mutableStateOf<WebView?>(null) }
                AndroidView(
                    factory = { contexto ->
                        WebView(contexto).apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            settings.javaScriptEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            webViewClient = WebViewClient()
                            loadUrl(url)
                            web = this
                        }
                    },
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                )
                DisposableEffect(Unit) { onDispose { web?.destroy() } }
                IconButton(onClick = alCerrar, Modifier.align(Alignment.TopEnd)) { Icon(Icons.Filled.Close, "Cerrar", tint = Color.White) }
            }
        }
    }
}
