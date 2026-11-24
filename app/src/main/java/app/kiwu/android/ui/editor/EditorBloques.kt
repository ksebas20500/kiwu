package app.kiwu.android.ui.editor

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiwu.android.domain.Documento
import app.kiwu.android.domain.TiposBloque
import app.kiwu.android.domain.VistaPrevia
import app.kiwu.android.ui.ColoresNota
import kotlinx.coroutines.delay
import app.kiwu.android.R
import androidx.compose.ui.res.stringResource

/** Lo que el editor necesita del sistema: selectores de archivos, abrir enlaces y descargar vistas previas. */
class OrigenAdjuntos(
    val elegirImagen: ((Uri) -> Unit) -> Unit,
    val elegirArchivo: ((Uri) -> Unit) -> Unit,
    val abrirArchivo: (String, String?) -> Unit,
    val abrirUrl: (String) -> Unit,
    val vistaPrevia: suspend (String) -> VistaPrevia?,
)

/** Crea el estado del editor y guarda con retraso (400 ms tras el último cambio) y al salir de la pantalla. */
@Composable
fun rememberEstadoEditor(inicial: Documento, alCambiar: (Documento) -> Unit): EstadoEditor {
    val estado = remember { EstadoEditor(inicial) }
    val alCambiarActual by rememberUpdatedState(alCambiar)
    val guardada = remember { mutableIntStateOf(0) }
    LaunchedEffect(estado.version) {
        if (estado.version != guardada.intValue) {
            delay(400)
            guardada.intValue = estado.version
            alCambiarActual(estado.documento())
        }
    }
    DisposableEffect(estado) {
        onDispose {
            if (estado.version != guardada.intValue) {
                guardada.intValue = estado.version
                alCambiarActual(estado.documento())
            }
        }
    }
    return estado
}

@Composable
fun esOscuro(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** Editor por bloques. La barra de formato (`BarraFormato`) va aparte, sobre el teclado. */
@Composable
fun EditorBloques(estado: EstadoEditor, adjuntos: OrigenAdjuntos, modifier: Modifier = Modifier) {
    val numeros = estado.numeros()
    ManejarAcciones(estado, adjuntos)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (b in estado.bloques) {
            key(b.id) { BloqueUi(estado, b, numeros[b.id], adjuntos) }
        }
        Spacer(
            Modifier.fillMaxWidth().height(220.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { estado.alFinal() },
        )
    }
}

@Composable
private fun BloqueUi(ed: EstadoEditor, b: BloqueEd, numero: Int?, adjuntos: OrigenAdjuntos) {
    when (b.kind) {
        TiposBloque.CODIGO -> BloqueCodigoUi(ed, b)
        TiposBloque.DIVISOR -> BloqueDivisorUi(ed, b)
        TiposBloque.IMAGEN -> BloqueImagenUi(ed, b)
        TiposBloque.ARCHIVO -> BloqueArchivoUi(ed, b, adjuntos)
        TiposBloque.ENLACE -> BloqueEnlaceUi(ed, b, adjuntos)
        TiposBloque.TARJETA -> BloqueTarjetaUi(ed, b)
        else -> BloqueTextoUi(ed, b, numero)
    }
}

internal fun estiloDe(f: Fmt, oscuro: Boolean, colorEnlace: Color, fondoCodigo: Color): SpanStyle? {
    if (f == Fmt.NINGUNO) return null
    val decoraciones = buildList {
        if (f.underline || f.link != null) add(TextDecoration.Underline)
        if (f.strike) add(TextDecoration.LineThrough)
    }
    return SpanStyle(
        fontWeight = if (f.bold) FontWeight.Bold else null,
        fontStyle = if (f.italic) FontStyle.Italic else null,
        fontFamily = if (f.code) FontFamily.Monospace else null,
        textDecoration = if (decoraciones.isEmpty()) null else TextDecoration.combine(decoraciones),
        color = if (f.link != null) colorEnlace else ColoresNota.texto(f.color, oscuro) ?: Color.Unspecified,
        background = ColoresNota.resaltado(f.highlight, oscuro) ?: if (f.code) fondoCodigo else Color.Unspecified,
    )
}

/** Dibuja el formato por carácter sobre el texto (que lleva delante el carácter invisible `ZW`). */
internal fun anotar(visible: String, fmt: List<Fmt>, oscuro: Boolean, colorEnlace: Color, fondoCodigo: Color): AnnotatedString {
    if (fmt.isEmpty() || fmt.none { it.tieneEstilo }) return AnnotatedString(visible)
    return buildAnnotatedString {
        append(visible)
        if (fmt.size != visible.length - 1) return@buildAnnotatedString
        var i = 0
        while (i < fmt.size) {
            var j = i + 1
            while (j < fmt.size && fmt[j] == fmt[i]) j++
            estiloDe(fmt[i], oscuro, colorEnlace, fondoCodigo)?.let { addStyle(it, i + 1, j + 1) }
            i = j
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BloqueTextoUi(ed: EstadoEditor, b: BloqueEd, numero: Int?) {
    val oscuro = esOscuro()
    val colorTexto = MaterialTheme.colorScheme.onSurface
    val colorEnlace = MaterialTheme.colorScheme.primary
    val fondoCodigo = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val foco = remember { FocusRequester() }
    var enfocado by remember { androidx.compose.runtime.mutableStateOf(false) }

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val density = LocalDensity.current

    var campoLocal by remember(b.id) { androidx.compose.runtime.mutableStateOf(b.contenido.campo) }

    LaunchedEffect(b.contenido.campo) {
        val campoModelo = b.contenido.campo
        val campoLimpio = if (b.kind != TiposBloque.CODIGO && campoModelo.text.contains('\n')) {
            val sinEnter = campoModelo.text.replace("\n", "")
            campoModelo.copy(text = sinEnter, selection = TextRange(campoModelo.selection.start.coerceAtMost(sinEnter.length)))
        } else {
            campoModelo
        }
        if (campoLocal != campoLimpio) {
            campoLocal = campoLimpio
        }
    }

    LaunchedEffect(campoLocal) {
        if (campoLocal != b.contenido.campo) {
            kotlinx.coroutines.delay(350)
            escribir(ed, b, campoLocal)
        }
    }

    LaunchedEffect(campoLocal.selection.end, layoutResult?.lineCount, enfocado) {
        val layout = layoutResult ?: return@LaunchedEffect
        if (!enfocado) return@LaunchedEffect
        val text = campoLocal.text
        val cursorOffset = campoLocal.selection.end.coerceIn(0, text.length)
        val cursorRect = layout.getCursorRect(cursorOffset)
        val extraBottomPx = with(density) { 80.dp.toPx() }
        runCatching {
            bringIntoViewRequester.bringIntoView(cursorRect.copy(bottom = cursorRect.bottom + extraBottomPx))
        }
    }

    DisposableEffect(b.id) {
        onDispose {
            if (campoLocal != b.contenido.campo) {
                escribir(ed, b, campoLocal)
            }
        }
    }

    PedirFoco(ed, b, foco)

    val base = when (b.kind) {
        TiposBloque.TITULO -> TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp)
        TiposBloque.SUBTITULO -> TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
        TiposBloque.CITA -> TextStyle(fontSize = 17.sp, fontStyle = FontStyle.Italic, lineHeight = 24.sp)
        else -> TextStyle(fontSize = 16.sp, lineHeight = 23.sp)
    }
    val hecho = b.kind == TiposBloque.TAREAS && b.isChecked
    val estilo = base.copy(
        color = if (hecho) colorTexto.copy(alpha = 0.5f) else colorTexto,
        textDecoration = if (hecho) TextDecoration.LineThrough else null,
    )
    val tieneFormato = remember(b.fmt) { b.fmt.any { it.tieneEstilo } }
    val transformacion = if (!tieneFormato) {
        VisualTransformation.None
    } else {
        remember(b.fmt, oscuro, colorEnlace) {
            VisualTransformation { t -> TransformedText(anotar(t.text, b.fmt, oscuro, colorEnlace, fondoCodigo), OffsetMapping.Identity) }
        }
    }
    val sangria = (b.indent * 24).dp

    val contenido: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            when (b.kind) {
                TiposBloque.VINETAS -> Text(
                    listOf("•", "◦", "▪", "•", "◦")[b.indent.coerceIn(0, 4)],
                    Modifier.width(24.dp).padding(top = 1.dp), color = colorTexto, fontSize = 18.sp,
                )
                TiposBloque.NUMERADA -> Text("${numero ?: 1}.", Modifier.width(30.dp).padding(top = 1.dp), color = colorTexto.copy(alpha = 0.7f), fontSize = 16.sp)
                TiposBloque.TAREAS -> Checkbox(
                    checked = b.isChecked,
                    onCheckedChange = { b.isChecked = it; ed.marcarCambio() },
                    modifier = Modifier.padding(end = 4.dp).height(24.dp),
                    colors = CheckboxDefaults.colors(),
                )
                TiposBloque.DESTACADO -> Icon(Icons.Filled.Lightbulb, null, Modifier.padding(end = 10.dp, top = 2.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Box(Modifier.weight(1f)) {
                val textoLimpio = campoLocal.text.replace("\u200B", "")
                if (textoLimpio.isEmpty()) {
                    val pista = when {
                        b.kind == TiposBloque.TITULO -> stringResource(R.string.bloque_titulo1)
                        b.kind == TiposBloque.SUBTITULO -> stringResource(R.string.bloque_titulo2)
                        b.kind == TiposBloque.PARRAFO && (enfocado || (ed.enfocado == null && ed.bloques.lastOrNull()?.id == b.id)) -> stringResource(R.string.pista_parrafo)
                        b.kind == TiposBloque.CITA -> stringResource(R.string.bloque_cita)
                        b.kind == TiposBloque.DESTACADO -> stringResource(R.string.bloque_destacado)
                        else -> ""
                    }
                    if (pista.isNotEmpty()) Text(pista, style = estilo, color = colorTexto.copy(alpha = 0.6f), modifier = Modifier.padding(start = 0.dp))
                }
                BasicTextField(
                    value = campoLocal,
                    onValueChange = { newValue ->
                        // 1. Ignorar si el texto no cambió (solo selección/cursor/composición de Gboard)
                        if (newValue.text == campoLocal.text) {
                            campoLocal = newValue
                            return@BasicTextField
                        }

                        // 2. Interceptar \n ANTES de guardarlo en campoLocal (para bloques de no-Código)
                        if (b.kind != TiposBloque.CODIGO && newValue.text.contains('\n')) {
                            val rawText = newValue.text.replace("\u200B", "")
                            val idx = rawText.indexOf('\n')
                            val antes = if (idx >= 0) rawText.substring(0, idx) else rawText
                            val nuevoCampo = TextFieldValue(text = ZW + antes, selection = TextRange(antes.length + 1))
                            campoLocal = nuevoCampo
                            escribir(ed, b, newValue)
                            return@BasicTextField
                        }

                        // Ruta normal de escritura
                        campoLocal = newValue
                        val text = newValue.text.replace("\u200B", "")
                        if (text.startsWith("/") || text.contains(" /") || text.isEmpty()) {
                            escribir(ed, b, newValue)
                        }
                    },
                    onTextLayout = { layoutResult = it },
                    textStyle = estilo,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    visualTransformation = transformacion,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
                    modifier = Modifier.fillMaxWidth()
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .focusRequester(foco)
                        .onFocusChanged {
                            enfocado = it.isFocused
                            if (it.isFocused) ed.enfocado = b.id else if (ed.enfocado == b.id) ed.enfocado = null
                        }
                        .onPreviewKeyEvent { evento -> atajos(ed, b, evento) },
                )
            }
            if (enfocado) {
                MenuBloque(ed, b)
            }
        }
    }

    Column(Modifier.fillMaxWidth().padding(start = sangria)) {
        when (b.kind) {
            TiposBloque.CITA -> Row(Modifier.height(IntrinsicSize.Min)) {
                Box(Modifier.width(3.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)))
                Box(Modifier.padding(start = 10.dp)) { contenido() }
            }
            TiposBloque.DESTACADO -> Surface(
                Modifier.padding(vertical = 4.dp), shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            ) { Box(Modifier.padding(10.dp)) { contenido() } }
            else -> contenido()
        }
    }
}

/** Pide el foco cuando el estado lo ordena (bloque nuevo, tras Intro…) y coloca el cursor. */
@Composable
internal fun PedirFoco(ed: EstadoEditor, b: BloqueEd, foco: FocusRequester) {
    androidx.compose.runtime.DisposableEffect(b.id) {
        ed.focos[b.id] = foco
        onDispose { if (ed.focos[b.id] === foco) ed.focos.remove(b.id) }
    }
    val peticion = ed.peticionFoco
    LaunchedEffect(peticion) {
        if (peticion == null || peticion.id != b.id) return@LaunchedEffect
        androidx.compose.runtime.withFrameNanos { }
        runCatching { foco.requestFocus() }
        val pos = if (peticion.alFinal) b.texto.length else (peticion.posicion ?: 0).coerceIn(0, b.texto.length)
        b.contenido = Contenido(b.contenido.campo.copy(selection = TextRange(pos + 1)), b.fmt)
        if (ed.peticionFoco === peticion) ed.peticionFoco = null
    }
}

/**
 * Si borrar quitó el bloque que tenía el foco, el foco pasa ya al bloque destino: si se espera a que el campo
 * borrado desaparezca, Android cierra el teclado y lo vuelve a abrir (se «refresca»).
 */
internal fun escribir(ed: EstadoEditor, b: BloqueEd, v: androidx.compose.ui.text.input.TextFieldValue) {
    ed.alEscribir(b, v)
    val destino = ed.peticionFoco?.id ?: return
    if (ed.bloques.none { it.id == b.id }) ed.focos[destino]?.let { runCatching { it.requestFocus() } }
}

/** Atajos de teclado físico (también valen con un teclado Bluetooth). */
private fun atajos(ed: EstadoEditor, b: BloqueEd, evento: androidx.compose.ui.input.key.KeyEvent): Boolean {
    if (evento.type != KeyEventType.KeyDown) return false
    if (evento.isCtrlPressed) {
        val mayus = evento.isShiftPressed
        when {
            evento.key == Key.B && !mayus -> ed.alternarFormato("b")
            evento.key == Key.I && !mayus -> ed.alternarFormato("i")
            evento.key == Key.U && !mayus -> ed.alternarFormato("u")
            evento.key == Key.E && !mayus -> ed.alternarFormato("code")
            evento.key == Key.X && mayus -> ed.alternarFormato("s")
            else -> return false
        }
        return true
    }
    if (evento.key == Key.Tab) {
        ed.cambiarSangria(b, if (evento.isShiftPressed) -1 else 1)
        return true
    }
    return false
}

private fun descripcionBloque(tipo: String): String = when (tipo) {
    TiposBloque.TITULO -> "Título principal de tamaño grande"
    TiposBloque.SUBTITULO -> "Sección o subtítulo secundario"
    TiposBloque.PARRAFO -> "Texto normal en párrafo"
    TiposBloque.VINETAS -> "Lista simple con viñetas"
    TiposBloque.NUMERADA -> "Lista ordenada con números"
    TiposBloque.TAREAS -> "Lista de tareas con casilla"
    TiposBloque.CITA -> "Cita o texto con barra lateral"
    TiposBloque.DESTACADO -> "Recuadro de aviso o idea clave"
    TiposBloque.CODIGO -> "Bloque de código con sintaxis"
    TiposBloque.TARJETA -> "Tarjeta de repaso flashcard"
    TiposBloque.IMAGEN -> "Insertar una imagen adjunta"
    TiposBloque.ARCHIVO -> "Adjuntar un documento o archivo"
    TiposBloque.DIVISOR -> "Línea horizontal separadora"
    ACCION_REPASAR -> "Repasar las tarjetas de esta nota"
    ACCION_TEXTO_FOTO -> "Escanear texto desde una foto"
    else -> "Insertar bloque"
}

@Composable
internal fun iconoBloque(tipo: String): androidx.compose.ui.graphics.vector.ImageVector = when (tipo) {
    TiposBloque.TITULO -> Icons.Filled.Title
    TiposBloque.SUBTITULO -> Icons.Filled.FormatSize
    TiposBloque.PARRAFO -> Icons.AutoMirrored.Filled.ShortText
    TiposBloque.VINETAS -> Icons.AutoMirrored.Filled.FormatListBulleted
    TiposBloque.NUMERADA -> Icons.Filled.FormatListNumbered
    TiposBloque.TAREAS -> Icons.Filled.Checklist
    TiposBloque.CITA -> Icons.Filled.FormatQuote
    TiposBloque.DESTACADO -> Icons.Filled.Lightbulb
    TiposBloque.CODIGO -> Icons.Filled.Code
    TiposBloque.TARJETA -> Icons.Filled.Style
    TiposBloque.IMAGEN -> Icons.Filled.Image
    TiposBloque.ARCHIVO -> Icons.Filled.AttachFile
    TiposBloque.DIVISOR -> Icons.Filled.HorizontalRule
    ACCION_REPASAR -> Icons.Filled.School
    ACCION_TEXTO_FOTO -> Icons.Filled.CameraAlt
    else -> Icons.Filled.Description
}

/** Aplica la opción elegida en el menú «/». Las que necesitan el sistema se resuelven en `AccionesEditor`. */
fun EstadoEditor.elegirSlash(b: BloqueEd, tipo: String) {
    val t = b.texto.replace("\u200B", "")
    val slashIndex = when {
        t.startsWith("/") -> 0
        t.contains(" /") -> t.lastIndexOf(" /") + 1
        else -> -1
    }
    val textoRestante = if (slashIndex >= 0) t.substring(0, slashIndex).trimEnd() else ""
    b.poner(textoRestante, List(textoRestante.length) { Fmt.NINGUNO })
    b.slashDescartado = false

    when (tipo) {
        TiposBloque.IMAGEN, TiposBloque.ARCHIVO, ACCION_TEXTO_FOTO -> accionPendiente = AccionPendiente(tipo, b)
        ACCION_REPASAR -> { marcarCambio(); accionPendiente = AccionPendiente(tipo, b) }
        TiposBloque.DIVISOR -> insertarTras(b, listOf(app.kiwu.android.domain.Bloque(kind = TiposBloque.DIVISOR)))
        else -> convertir(b, tipo, textoRestante)
    }
    marcarCambio()
}

/** Nombre de cada tipo de bloque en el idioma de la app (menú «/», «Convertir en…»). */
@Composable
internal fun titulosBloques(): Map<String, String> {
    val t1 = stringResource(R.string.bloque_titulo1)
    val t2 = stringResource(R.string.bloque_titulo2)
    val cita = stringResource(R.string.bloque_cita)
    val des = stringResource(R.string.bloque_destacado)
    val vin = stringResource(R.string.bloque_vinetas)
    val num = stringResource(R.string.bloque_numerada)
    val tar = stringResource(R.string.bloque_tareas)
    val cod = stringResource(R.string.bloque_codigo)
    val flash = stringResource(R.string.bloque_tarjeta)
    val img = stringResource(R.string.bloque_imagen)
    val arch = stringResource(R.string.bloque_archivo)
    val div = stringResource(R.string.bloque_divisor)
    val rep = stringResource(R.string.slash_repasar)
    val foto = stringResource(R.string.slash_texto_foto)
    return remember(t1, t2, cita, des, vin, num, tar, cod, flash, img, arch, div, rep, foto) {
        mapOf(
            TiposBloque.TITULO to t1,
            TiposBloque.SUBTITULO to t2,
            TiposBloque.CITA to cita,
            TiposBloque.DESTACADO to des,
            TiposBloque.VINETAS to vin,
            TiposBloque.NUMERADA to num,
            TiposBloque.TAREAS to tar,
            TiposBloque.CODIGO to cod,
            TiposBloque.TARJETA to flash,
            TiposBloque.IMAGEN to img,
            TiposBloque.ARCHIVO to arch,
            TiposBloque.DIVISOR to div,
            ACCION_REPASAR to rep,
            ACCION_TEXTO_FOTO to foto,
        )
    }
}
