package app.kiwu.android.ui.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import app.kiwu.android.domain.Bloque
import app.kiwu.android.domain.Documento
import app.kiwu.android.domain.Enlaces
import app.kiwu.android.domain.TiposBloque
import kotlinx.serialization.json.JsonObject

/**
 * Cada campo de texto empieza con un carácter invisible (espacio de ancho cero). Así, cuando la persona pulsa
 * «borrar» con el cursor al inicio, el teclado borra ese carácter y nos enteramos (de otro modo no llega ningún
 * evento). El modelo nunca lo contiene: se quita y se vuelve a poner.
 */
const val ZW = "​"

private val LISTAS = setOf(TiposBloque.VINETAS, TiposBloque.NUMERADA, TiposBloque.TAREAS)

/** Texto de un bloque y su formato, siempre juntos para que nunca queden desalineados. */
class Contenido(val campo: TextFieldValue, val fmt: List<Fmt>)

/** Un bloque en edición. Los campos que la interfaz dibuja son estado observable. */
@Stable
class BloqueEd(base: Bloque) {
    val id: String = base.id
    var kind by mutableStateOf(base.kind)
    var indent by mutableStateOf(base.indent)
    var isChecked by mutableStateOf(base.isChecked)
    var language by mutableStateOf(base.language)
    var url by mutableStateOf(base.url)
    var preview by mutableStateOf(base.preview)
    var imageData by mutableStateOf(base.imageData)
    var fileName by mutableStateOf(base.fileName)
    var path by mutableStateOf(base.path)
    var cargandoVistaPrevia by mutableStateOf(false)
    var vistaPrevioFallida by mutableStateOf(false)
    private val bookmark = base.bookmark
    private val extra: JsonObject = base.extra

    var contenido by mutableStateOf(armar(base.text, Formato.desdeRuns(base.text, base.runs), null))

    /** Formato elegido antes de escribir (cursor sin selección): se aplica a lo que se teclee a continuación. */
    var pendiente by mutableStateOf<Fmt?>(null)

    /** Se descartó el menú «/» con Esc/toque fuera para este bloque. */
    var slashDescartado by mutableStateOf(false)

    val texto: String get() = contenido.campo.text.removePrefix(ZW)
    val fmt: List<Fmt> get() = contenido.fmt

    /** Selección en coordenadas del modelo (sin el carácter invisible). */
    val seleccion: TextRange
        get() {
            val s = contenido.campo.selection
            return TextRange((s.start - 1).coerceIn(0, texto.length), (s.end - 1).coerceIn(0, texto.length))
        }

    fun poner(texto: String, fmt: List<Fmt>, cursor: Int = texto.length) {
        contenido = armar(texto, fmt, TextRange(cursor.coerceIn(0, texto.length)))
    }

    fun aBloque(): Bloque {
        val esTexto = kind in TiposBloque.deTexto
        return Bloque(
            id = id, kind = kind, text = if (kind in TiposBloque.deTexto || kind == TiposBloque.CODIGO) texto else "",
            runs = if (esTexto) Formato.aRuns(texto, fmt) else null, indent = indent, language = language, url = url,
            preview = preview, isChecked = isChecked, imageData = imageData, bookmark = bookmark, fileName = fileName,
            path = path, extra = extra,
        )
    }

    private fun armar(texto: String, fmt: List<Fmt>, cursor: TextRange?): Contenido {
        val sel = cursor ?: TextRange(texto.length)
        return Contenido(TextFieldValue(ZW + texto, TextRange(sel.start + 1, sel.end + 1)), fmt)
    }
}

/** Minúsculas y sin tildes, para que «cod» encuentre «Código». */
internal fun sinTildes(texto: String): String =
    java.text.Normalizer.normalize(texto.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("""\p{Mn}+"""), "")

class PeticionFoco(val id: String, val alFinal: Boolean, val posicion: Int? = null)

/** Acción que necesita al sistema (elegir una imagen o un archivo) y que resuelve `EditorBloques`. */
class AccionPendiente(val tipo: String, val tras: BloqueEd?)

/**
 * Estado y reglas del editor por bloques (equivalente a `editor.py` en Linux y `NoteEditorView` en macOS):
 * Intro, borrar al inicio, sangría, conversión de bloques, menú «/», formato en línea…
 */
@Stable
class EstadoEditor(inicial: Documento) {
    val bloques = mutableStateListOf<BloqueEd>()

    /** Bloque de texto que tiene el foco ahora (para la barra de formato). */
    var enfocado by mutableStateOf<String?>(null)
    var peticionFoco by mutableStateOf<PeticionFoco?>(null)
    var accionPendiente by mutableStateOf<AccionPendiente?>(null)

    /** Sube con cada cambio; la interfaz lo usa para guardar con retraso. */
    var version by mutableIntStateOf(0)
        private set

    init {
        cargar(inicial)
    }

    fun cargar(doc: Documento) {
        bloques.clear()
        doc.bloques.forEach { bloques.add(BloqueEd(it)) }
        asegurarFinal()
    }

    fun documento(): Documento = Documento(bloques.map { it.aBloque() })

    private fun cambio() {
        version++
    }

    private fun indiceDe(b: BloqueEd) = bloques.indexOfFirst { it.id == b.id }

    /** Un bloque ya eliminado puede seguir recibiendo eventos del teclado (p. ej. al mantener «borrar»): se ignoran. */
    private fun esta(b: BloqueEd) = indiceDe(b) >= 0

    private fun asegurarFinal() {
        if (bloques.isEmpty() || bloques.last().kind != TiposBloque.PARRAFO) bloques.add(BloqueEd(Bloque()))
    }

    fun bloqueEnfocado(): BloqueEd? = bloques.firstOrNull { it.id == enfocado }

    fun enfocar(b: BloqueEd, alFinal: Boolean = true, posicion: Int? = null) {
        peticionFoco = PeticionFoco(b.id, alFinal, posicion)
    }

    /** Número de cada elemento de lista numerada (un contador por nivel de sangría). */
    fun numeros(): Map<String, Int> {
        val contadores = IntArray(5)
        val salida = HashMap<String, Int>()
        for (b in bloques) {
            val nivel = b.indent.coerceIn(0, 4)
            if (b.kind == TiposBloque.NUMERADA) {
                contadores[nivel]++
                for (mayor in nivel + 1..4) contadores[mayor] = 0
                salida[b.id] = maxOf(contadores[nivel], 1)
            } else {
                contadores.fill(0)
            }
        }
        return salida
    }

    // --- escritura ---------------------------------------------------------------------------------

    /** Cambio de texto o de cursor en un bloque de texto o de código. */
    fun alEscribir(b: BloqueEd, v: TextFieldValue) {
        if (!esta(b)) return
        val viejoTexto = b.texto
        val bruto = v.text
        val tieneInicio = bruto.startsWith(ZW) && bruto.indexOf(ZW, 1) < 0
        val modelo: String
        val cursorModelo: TextRange
        if (tieneInicio) {
            modelo = bruto.substring(1)
            cursorModelo = TextRange((v.selection.start - 1).coerceAtLeast(0), (v.selection.end - 1).coerceAtLeast(0))
        } else if (!bruto.contains(ZW) && bruto == viejoTexto) {
            // Se borró solo el carácter invisible: «borrar» con el cursor al inicio.
            alBorrarAlInicio(b)
            return
        } else {
            // Se escribió antes del carácter invisible, o se reemplazó todo el texto.
            modelo = bruto.replace(ZW, "")
            val antes = bruto.take(v.selection.start.coerceAtMost(bruto.length)).count { it == ZW[0] }
            val c = (v.selection.start - antes).coerceIn(0, modelo.length)
            cursorModelo = TextRange(c)
        }

        if (modelo == viejoTexto) {
            // Solo cambió el cursor, la selección o la composición del teclado.
            val sigue = if (tieneInicio) v else TextFieldValue(ZW + modelo, TextRange(cursorModelo.start + 1, cursorModelo.end + 1))
            b.contenido = Contenido(sigue, b.fmt)
            if (cursorModelo.start != b.seleccion.start) b.pendiente = null
            return
        }

        val nuevoFmt = Formato.alCambiarTexto(viejoTexto, b.fmt, modelo, b.pendiente)
        b.pendiente = null

        if (b.kind != TiposBloque.CODIGO && modelo.contains('\n')) {
            dividir(b, modelo, nuevoFmt, cursorModelo.start)
            return
        }

        val valor = if (tieneInicio) v else TextFieldValue(ZW + modelo, TextRange(cursorModelo.start + 1, cursorModelo.end + 1))
        b.contenido = Contenido(valor, nuevoFmt)
        if (b.kind == TiposBloque.PARRAFO) b.slashDescartado = b.slashDescartado && modelo.startsWith("/")
        cambio()
    }

    /** El texto tenía saltos de línea (Intro o pegado): se reparte en bloques. */
    private fun dividir(b: BloqueEd, modelo: String, fmt: List<Fmt>, cursor: Int) {
        val lineas = ArrayList<Pair<String, List<Fmt>>>()
        var inicio = 0
        while (true) {
            val salto = modelo.indexOf('\n', inicio)
            val fin = if (salto < 0) modelo.length else salto
            lineas.add(modelo.substring(inicio, fin) to fmt.subList(inicio, fin).toList())
            if (salto < 0) break
            inicio = salto + 1
        }

        // Intro en un elemento de lista vacío: baja de nivel o se convierte en párrafo.
        if (b.kind in LISTAS && lineas.size == 2 && lineas.all { it.first.isEmpty() }) {
            b.poner("", emptyList())
            if (b.indent > 0) cambiarSangria(b, -1) else convertir(b, TiposBloque.PARRAFO, "")
            return
        }
        // Una dirección web sola en un párrafo se convierte en tarjeta con vista previa.
        if (b.kind == TiposBloque.PARRAFO && b.indent == 0 && lineas.size == 2 && lineas[1].first.isEmpty() &&
            Enlaces.pareceEnlaceSuelto(lineas[0].first)
        ) {
            b.poner(lineas[0].first, lineas[0].second)
            convertir(b, TiposBloque.ENLACE)
            val i = indiceDe(b)
            if (i + 1 < bloques.size) enfocar(bloques[i + 1], alFinal = false)
            return
        }

        b.poner(lineas[0].first, lineas[0].second, cursor = lineas[0].first.length)
        var anterior = b
        var i = indiceDe(b)
        val nuevos = ArrayList<BloqueEd>()
        for (k in 1 until lineas.size) {
            val esLista = b.kind in LISTAS
            val nuevo = BloqueEd(
                Bloque(kind = if (esLista) b.kind else TiposBloque.PARRAFO, indent = if (esLista) b.indent else 0),
            )
            nuevo.poner(lineas[k].first, lineas[k].second, cursor = 0)
            bloques.add(++i, nuevo)
            nuevos.add(nuevo)
            anterior = nuevo
        }
        // El cursor queda donde estaba dentro del texto repartido.
        var acumulado = 0
        var destino = anterior
        var posicion = 0
        val todos = listOf(b) + nuevos
        for ((k, linea) in lineas.withIndex()) {
            if (cursor <= acumulado + linea.first.length || k == lineas.lastIndex) {
                destino = todos[k]
                posicion = (cursor - acumulado).coerceIn(0, linea.first.length)
                break
            }
            acumulado += linea.first.length + 1
        }
        enfocar(destino, alFinal = false, posicion = posicion)
        cambio()
    }

    /** «Borrar» con el cursor al inicio del bloque (mismas reglas que Linux, más unir con el bloque anterior). */
    fun alBorrarAlInicio(b: BloqueEd) {
        if (!esta(b)) return
        val vacio = b.texto.isEmpty()
        when {
            vacio -> borrarVacio(b)
            b.indent > 0 -> cambiarSangria(b, -1)
            b.kind != TiposBloque.PARRAFO && b.kind != TiposBloque.CODIGO -> convertir(b, TiposBloque.PARRAFO, b.texto)
            b.kind == TiposBloque.PARRAFO -> unirConAnterior(b)
        }
        // Lo que escribió el teclado (borrar el carácter invisible) se descarta: se vuelve a poner.
        bloques.firstOrNull { it.id == b.id }?.let { it.poner(it.texto, it.fmt, it.seleccion.start) }
    }

    private fun unirConAnterior(b: BloqueEd) {
        val i = indiceDe(b)
        if (i <= 0) return
        val previo = bloques[i - 1]
        if (previo.kind !in TiposBloque.deTexto) return
        val union = previo.texto.length
        previo.poner(previo.texto + b.texto, previo.fmt + b.fmt, cursor = union)
        bloques.removeAt(i)
        enfocar(previo, alFinal = false, posicion = union)
        asegurarFinal()
        cambio()
    }

    fun borrarVacio(b: BloqueEd) {
        if (!esta(b)) return
        if (b.indent > 0) return cambiarSangria(b, -1)
        if (b.kind != TiposBloque.PARRAFO) return convertir(b, TiposBloque.PARRAFO, "")
        if (bloques.size <= 1) return
        val i = indiceDe(b)
        bloques.removeAt(i)
        val previo = bloques[(i - 1).coerceIn(0, bloques.lastIndex)]
        if (previo.kind in TiposBloque.deTexto || previo.kind == TiposBloque.CODIGO) enfocar(previo)
        asegurarFinal()
        cambio()
    }

    // --- estructura --------------------------------------------------------------------------------

    fun convertir(b: BloqueEd, tipo: String, texto: String? = null) {
        if (!esta(b) || (tipo == b.kind && texto == null)) return
        val textoActual = texto ?: b.texto
        val fmtActual = if (texto != null) emptyList() else b.fmt
        val eraCodigo = b.kind == TiposBloque.CODIGO
        b.kind = tipo
        when (tipo) {
            TiposBloque.ENLACE -> {
                b.url = Enlaces.urlDesdeTexto(textoActual)
                b.preview = null
                b.vistaPrevioFallida = false
                b.poner("", emptyList())
            }
            TiposBloque.DIVISOR, TiposBloque.IMAGEN, TiposBloque.ARCHIVO -> b.poner("", emptyList())
            else -> {
                val limpio = if (eraCodigo || tipo == TiposBloque.CODIGO) List(textoActual.length) { Fmt.NINGUNO } else fmtActual
                b.poner(textoActual, if (limpio.size == textoActual.length) limpio else List(textoActual.length) { Fmt.NINGUNO })
            }
        }
        if (tipo != TiposBloque.TAREAS) b.isChecked = false
        asegurarFinal()
        cambio()
        if (tipo != TiposBloque.ENLACE && tipo != TiposBloque.DIVISOR && tipo != TiposBloque.IMAGEN && tipo != TiposBloque.ARCHIVO) enfocar(b)
    }

    fun cambiarSangria(b: BloqueEd, delta: Int) {
        if (!esta(b)) return
        val nueva = (b.indent + delta).coerceIn(0, 4)
        if (nueva == b.indent) return
        b.indent = nueva
        cambio()
    }

    fun mover(b: BloqueEd, delta: Int) {
        val i = indiceDe(b)
        val j = i + delta
        if (i < 0 || j !in bloques.indices) return
        bloques.removeAt(i)
        bloques.add(j, b)
        asegurarFinal()
        cambio()
    }

    fun duplicar(b: BloqueEd) {
        if (!esta(b)) return
        val copia = BloqueEd(b.aBloque().copy(id = app.kiwu.android.domain.nuevoId()))
        bloques.add(indiceDe(b) + 1, copia)
        cambio()
    }

    fun eliminar(b: BloqueEd) {
        val i = indiceDe(b)
        if (i < 0) return
        if (bloques.size == 1) {
            bloques[0] = BloqueEd(Bloque())
        } else {
            bloques.removeAt(i)
            val vecino = bloques[(i - 1).coerceIn(0, bloques.lastIndex)]
            if (vecino.kind in TiposBloque.deTexto) enfocar(vecino)
        }
        asegurarFinal()
        cambio()
    }

    /** Inserta bloques tras [b] (sustituyéndolo si es un párrafo vacío); sin [b], al final. */
    fun insertarTras(b: BloqueEd?, nuevos: List<Bloque>) {
        var i: Int
        if (b != null && indiceDe(b) >= 0) {
            i = indiceDe(b)
            if (b.kind == TiposBloque.PARRAFO && b.texto.isEmpty()) bloques.removeAt(i) else i++
        } else {
            i = bloques.size
            if (bloques.isNotEmpty() && bloques.last().kind == TiposBloque.PARRAFO && bloques.last().texto.isEmpty()) i--
        }
        nuevos.forEachIndexed { k, datos -> bloques.add((i + k).coerceAtMost(bloques.size), BloqueEd(datos)) }
        asegurarFinal()
        cambio()
    }

    /** Pulsar bajo el último bloque: enfoca el párrafo final (o crea uno). */
    fun alFinal() {
        val ultimo = bloques.lastOrNull()
        if (ultimo != null && ultimo.kind == TiposBloque.PARRAFO && ultimo.texto.isEmpty()) {
            enfocar(ultimo)
        } else {
            val nuevo = BloqueEd(Bloque())
            bloques.add(nuevo)
            cambio()
            enfocar(nuevo)
        }
    }

    fun enlaceATexto(b: BloqueEd) {
        val direccion = b.url ?: ""
        b.kind = TiposBloque.PARRAFO
        b.url = null
        b.preview = null
        b.poner(direccion, List(direccion.length) { Fmt.NINGUNO })
        cambio()
        enfocar(b)
    }

    fun marcarCambio() = cambio()

    // --- menú «/» ----------------------------------------------------------------------------------

    /** Tipos que ofrece el menú «/» para este bloque, filtrados por su nombre en [titulos]; vacío si no hay que mostrarlo. */
    fun opcionesSlash(b: BloqueEd, titulos: Map<String, String>): List<String> {
        val t = b.texto
        if (b.kind != TiposBloque.PARRAFO || !t.startsWith("/") || t.contains('\n') || b.slashDescartado) return emptyList()
        val filtro = sinTildes(t.drop(1))
        return TiposBloque.todos.filter {
            it != TiposBloque.PARRAFO && (filtro.isEmpty() || filtro in sinTildes(titulos[it] ?: ""))
        }
    }

    // --- formato en línea --------------------------------------------------------------------------

    /** Formato del carácter junto al cursor (o de la selección si todo coincide). */
    fun formatoActual(b: BloqueEd): Fmt {
        b.pendiente?.let { return it }
        val s = b.seleccion
        return when {
            b.fmt.isEmpty() -> Fmt.NINGUNO
            !s.collapsed -> b.fmt[s.min.coerceIn(0, b.fmt.lastIndex)]
            s.start > 0 -> b.fmt[(s.start - 1).coerceAtMost(b.fmt.lastIndex)]
            else -> b.fmt[0]
        }
    }

    fun activo(b: BloqueEd, nombre: String): Boolean {
        val s = b.seleccion
        if (s.collapsed) return formatoActual(b).tiene(nombre)
        return Formato.todosCumplen(b.fmt, s.min, s.max) { it.tiene(nombre) }
    }

    fun alternarFormato(nombre: String) {
        val b = bloqueEnfocado() ?: return
        if (b.kind !in TiposBloque.deTexto) return
        val s = b.seleccion
        if (s.collapsed) {
            b.pendiente = formatoActual(b).alternar(nombre)
        } else {
            val todos = Formato.todosCumplen(b.fmt, s.min, s.max) { it.tiene(nombre) }
            b.contenido = Contenido(b.contenido.campo, Formato.aplicar(b.fmt, s.min, s.max) { it.conValor(nombre, !todos) })
            cambio()
        }
    }

    fun fijarColor(nombre: String?, resaltado: Boolean) {
        val b = bloqueEnfocado() ?: return
        if (b.kind !in TiposBloque.deTexto) return
        val s = b.seleccion
        val cambio: (Fmt) -> Fmt = { if (resaltado) it.copy(highlight = nombre) else it.copy(color = nombre) }
        if (s.collapsed) {
            b.pendiente = cambio(formatoActual(b))
        } else {
            b.contenido = Contenido(b.contenido.campo, Formato.aplicar(b.fmt, s.min, s.max, cambio))
            cambio()
        }
    }

    /** Pone (o quita, con null) un enlace en la selección. Sin selección, inserta la dirección como texto con enlace. */
    fun aplicarEnlace(direccion: String?) {
        val b = bloqueEnfocado() ?: return
        if (b.kind !in TiposBloque.deTexto) return
        val s = b.seleccion
        if (s.collapsed) {
            if (direccion == null) {
                // Quitar el enlace de la palabra bajo el cursor
                val i = s.start
                var desde = i
                var hasta = i
                val enlaceAqui = b.fmt.getOrNull(i - 1)?.link ?: b.fmt.getOrNull(i)?.link ?: return
                while (desde > 0 && b.fmt[desde - 1].link == enlaceAqui) desde--
                while (hasta < b.fmt.size && b.fmt[hasta].link == enlaceAqui) hasta++
                b.contenido = Contenido(b.contenido.campo, Formato.aplicar(b.fmt, desde, hasta) { it.copy(link = null) })
                cambio()
            } else {
                val nuevoTexto = b.texto.substring(0, s.start) + direccion + b.texto.substring(s.start)
                val f = formatoActual(b).copy(link = direccion)
                val nuevoFmt = b.fmt.take(s.start) + List(direccion.length) { f } + b.fmt.drop(s.start)
                b.poner(nuevoTexto, nuevoFmt, cursor = s.start + direccion.length)
                cambio()
            }
        } else {
            b.contenido = Contenido(b.contenido.campo, Formato.aplicar(b.fmt, s.min, s.max) { it.copy(link = direccion) })
            cambio()
        }
    }

    /** Enlace bajo el cursor, si lo hay. */
    fun enlaceBajoCursor(b: BloqueEd): String? {
        val s = b.seleccion
        return b.fmt.getOrNull(s.start - 1)?.link ?: b.fmt.getOrNull(s.start)?.link
    }
}
