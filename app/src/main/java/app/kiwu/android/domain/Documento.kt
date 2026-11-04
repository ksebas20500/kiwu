package app.kiwu.android.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import java.util.UUID
import app.kiwu.android.R

/**
 * Formato de las notas. Es el mismo JSON que usan Kiwu para macOS y Linux (`NoteDocument`, `NoteBlock`, `TextRun`):
 * una nota creada en un sistema se lee en los otros. Los lectores son tolerantes: aceptan campos ausentes
 * y conservan los desconocidos. Las fixtures de `docs/contrato/` comprueban que los tres clientes coinciden.
 */
object TiposBloque {
    const val PARRAFO = "paragraph"
    const val TITULO = "heading"
    const val SUBTITULO = "subheading"
    const val VINETAS = "bulletedList"
    const val NUMERADA = "numberedList"
    const val TAREAS = "checklist"
    const val CITA = "quote"
    const val DESTACADO = "callout"
    const val CODIGO = "code"
    const val IMAGEN = "image"
    const val ARCHIVO = "file"
    const val DIVISOR = "divider"
    const val ENLACE = "embed"

    /** Tarjeta de repaso: `text` es la pregunta y `answer` la respuesta. */
    const val TARJETA = "flashcard"

    val todos = listOf(
        PARRAFO, TITULO, SUBTITULO, VINETAS, NUMERADA, TAREAS, CITA, DESTACADO, CODIGO, IMAGEN, ARCHIVO, DIVISOR, ENLACE, TARJETA,
    )

    /** Nombre de cada tipo en el idioma de la app (recursos de texto). */
    val titulos = mapOf(
        PARRAFO to R.string.bloque_texto,
        TITULO to R.string.bloque_titulo1,
        SUBTITULO to R.string.bloque_titulo2,
        VINETAS to R.string.bloque_vinetas,
        NUMERADA to R.string.bloque_numerada,
        TAREAS to R.string.bloque_tareas,
        CITA to R.string.bloque_cita,
        DESTACADO to R.string.bloque_destacado,
        CODIGO to R.string.bloque_codigo,
        IMAGEN to R.string.bloque_imagen,
        ARCHIVO to R.string.bloque_archivo,
        DIVISOR to R.string.bloque_divisor,
        ENLACE to R.string.bloque_enlace,
        TARJETA to R.string.bloque_tarjeta,
    )

    /** Bloques cuyo contenido es texto editable con formato. */
    val deTexto = setOf(PARRAFO, TITULO, SUBTITULO, VINETAS, NUMERADA, TAREAS, CITA, DESTACADO)
}

fun nuevoId(): String = UUID.randomUUID().toString().uppercase()

private fun JsonObject.texto(clave: String): String? {
    val p = this[clave] as? JsonPrimitive ?: return null
    return if (p is JsonNull) null else p.content
}

private fun JsonObject.logico(clave: String): Boolean = (this[clave] as? JsonPrimitive)?.booleanOrNull ?: false

private fun JsonObject.entero(clave: String): Int = (this[clave] as? JsonPrimitive)?.intOrNull ?: 0

data class TextRun(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strike: Boolean = false,
    val code: Boolean = false,
    /** Nombre de un color de texto (ver `ColoresNota`). */
    val color: String? = null,
    val highlight: String? = null,
    val link: String? = null,
) {
    fun mismoFormato(otro: TextRun): Boolean =
        bold == otro.bold && italic == otro.italic && underline == otro.underline && strike == otro.strike &&
            code == otro.code && color == otro.color && highlight == otro.highlight && link == otro.link

    val esPlano: Boolean
        get() = !bold && !italic && !underline && !strike && !code && color == null && highlight == null && link == null

    fun aJson(): JsonObject = buildJsonObject {
        // macOS exige todas las claves booleanas; las opcionales se omiten si no hay valor.
        put("text", text)
        put("bold", bold)
        put("italic", italic)
        put("underline", underline)
        put("strike", strike)
        put("code", code)
        color?.let { put("color", it) }
        highlight?.let { put("highlight", it) }
        link?.let { put("link", it) }
    }

    companion object {
        fun deJson(o: JsonObject) = TextRun(
            text = o.texto("text") ?: "",
            bold = o.logico("bold"),
            italic = o.logico("italic"),
            underline = o.logico("underline"),
            strike = o.logico("strike"),
            code = o.logico("code"),
            color = o.texto("color"),
            highlight = o.texto("highlight"),
            link = o.texto("link"),
        )
    }
}

/** Une tramos contiguos con el mismo formato y descarta los vacíos. */
fun fusionarRuns(runs: List<TextRun>): List<TextRun> {
    val salida = ArrayList<TextRun>()
    for (run in runs) {
        if (run.text.isEmpty()) continue
        val ultimo = salida.lastOrNull()
        if (ultimo != null && ultimo.mismoFormato(run)) {
            salida[salida.size - 1] = ultimo.copy(text = ultimo.text + run.text)
        } else {
            salida.add(run)
        }
    }
    return salida
}

/** null si el texto no tiene ningún formato (como hacen las versiones de macOS y Linux). */
fun runsONada(runs: List<TextRun>): List<TextRun>? {
    val unidos = fusionarRuns(runs)
    return if (unidos.all { it.esPlano }) null else unidos
}

data class Bloque(
    val id: String = nuevoId(),
    val kind: String = TiposBloque.PARRAFO,
    val text: String = "",
    val runs: List<TextRun>? = null,
    /** Niveles de sangría (0..4). */
    val indent: Int = 0,
    val language: String? = null,
    val url: String? = null,
    /** Vista previa de un enlace (`titulo`, `descripcion`, `sitio`, `imagen` en base64, `proveedor`, `videoID`). */
    val preview: JsonObject? = null,
    val isChecked: Boolean = false,
    /** Imagen reducida en base64, igual que `Data` en el JSON de Swift. */
    val imageData: String? = null,
    /** Marcador de macOS: opaco, se conserva al reexportar. */
    val bookmark: String? = null,
    val fileName: String? = null,
    /** Dirección del archivo o la imagen adjunta en este dispositivo (en Android, un `content://`). */
    val path: String? = null,
    /** Respuesta de una tarjeta de repaso. */
    val answer: String? = null,
    /** Campos desconocidos, para no perderlos al reescribir. */
    val extra: JsonObject = JsonObject(emptyMap()),
) {
    fun aJson(): JsonObject = buildJsonObject {
        for ((clave, valor) in extra) put(clave, valor)
        put("id", id)
        put("kind", kind)
        put("text", text)
        put("indent", indent)
        put("isChecked", isChecked)
        if (!runs.isNullOrEmpty()) put("runs", buildJsonArray { runs.forEach { add(it.aJson()) } })
        language?.let { put("language", it) }
        url?.let { put("url", it) }
        preview?.let { put("preview", it) }
        imageData?.let { put("imageData", it) }
        bookmark?.let { put("bookmark", it) }
        fileName?.let { put("fileName", it) }
        path?.let { put("path", it) }
        answer?.let { put("answer", it) }
    }

    fun textoPlano(): String = when (kind) {
        TiposBloque.ARCHIVO -> fileName ?: ""
        TiposBloque.TARJETA -> listOf(text, answer.orEmpty()).filter { it.isNotEmpty() }.joinToString("\n")
        TiposBloque.ENLACE -> preview?.texto("titulo")?.takeIf { it.isNotEmpty() } ?: url ?: ""
        else -> text
    }

    companion object {
        private val conocidas = setOf(
            "id", "kind", "text", "runs", "indent", "language", "url", "preview", "isChecked",
            "imageData", "bookmark", "fileName", "path", "answer",
        )

        fun deJson(o: JsonObject): Bloque {
            val tipo = o.texto("kind") ?: TiposBloque.PARRAFO
            val runs = (o["runs"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.let(TextRun::deJson) }
            return Bloque(
                id = o.texto("id")?.takeIf { it.isNotEmpty() } ?: nuevoId(),
                kind = if (tipo in TiposBloque.todos) tipo else TiposBloque.PARRAFO,
                text = o.texto("text") ?: "",
                runs = runs,
                indent = o.entero("indent").coerceIn(0, 4),
                language = o.texto("language"),
                url = o.texto("url"),
                preview = o["preview"] as? JsonObject,
                isChecked = o.logico("isChecked"),
                imageData = o.texto("imageData"),
                bookmark = o.texto("bookmark"),
                fileName = o.texto("fileName"),
                path = o.texto("path"),
                answer = o.texto("answer"),
                extra = JsonObject(o.filterKeys { it !in conocidas }),
            )
        }
    }
}

data class Documento(val bloques: List<Bloque> = listOf(Bloque())) {
    fun aJson(): String = Json.encodeToString(
        JsonElement.serializer(),
        buildJsonObject { put("blocks", buildJsonArray { bloques.forEach { add(it.aJson()) } }) },
    )

    fun textoPlano(): String = bloques.map { it.textoPlano() }.filter { it.isNotEmpty() }.joinToString("\n")

    companion object {
        fun vacio() = Documento(listOf(Bloque()))

        /** Lee un documento. Si el contenido no es válido devuelve uno vacío (nunca falla). */
        fun deJson(contenido: String?): Documento {
            if (contenido.isNullOrBlank()) return vacio()
            val raiz = try {
                Json.parseToJsonElement(contenido)
            } catch (e: Exception) {
                return vacio()
            }
            val bloques = ((raiz as? JsonObject)?.get("blocks") as? JsonArray)
                ?.mapNotNull { (it as? JsonObject)?.let(Bloque::deJson) }
                ?: return vacio()
            return Documento(bloques.ifEmpty { listOf(Bloque()) })
        }
    }
}

/** Contenido vacío de una nota nueva (se guarda tal cual en la base de datos). */
const val DOCUMENTO_VACIO = """{"blocks":[]}"""
