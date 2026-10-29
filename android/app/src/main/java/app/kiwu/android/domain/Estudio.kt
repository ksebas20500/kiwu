package app.kiwu.android.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Una nota de una materia (lista): lo que sacaste y cuánto pesa en el promedio. La escala (5, 10, 100…) la pones tú. */
data class Calificacion(val nombre: String, val nota: Double, val peso: Double = 1.0)

object Calificaciones {
    fun leer(json: String): List<Calificacion> = try {
        if (json.isBlank()) emptyList()
        else Json.parseToJsonElement(json).jsonArray.map {
            val o = it.jsonObject
            Calificacion(o.getValue("n").jsonPrimitive.content, o.getValue("v").jsonPrimitive.double, o["p"]?.jsonPrimitive?.double ?: 1.0)
        }
    } catch (e: Exception) {
        emptyList() // un texto dañado (p. ej. de otro dispositivo) no debe romper la pantalla
    }

    fun escribir(notas: List<Calificacion>): String =
        if (notas.isEmpty()) "" else JsonArray(notas.map { JsonObject(mapOf("n" to JsonPrimitive(it.nombre), "v" to JsonPrimitive(it.nota), "p" to JsonPrimitive(it.peso))) }).toString()

    /** Promedio ponderado por peso; null si no hay notas o los pesos suman cero. */
    fun promedio(notas: List<Calificacion>): Double? {
        val pesos = notas.sumOf { it.peso }
        return if (notas.isEmpty() || pesos <= 0.0) null else notas.sumOf { it.nota * it.peso } / pesos
    }
}

/** Una tarjeta de repaso: pregunta y respuesta. */
data class Tarjeta(val pregunta: String, val respuesta: String)

object Tarjetas {
    fun leer(json: String): List<Tarjeta> = try {
        if (json.isBlank()) emptyList()
        else Json.parseToJsonElement(json).jsonArray.map { Tarjeta(it.jsonObject.getValue("p").jsonPrimitive.content, it.jsonObject.getValue("r").jsonPrimitive.content) }
    } catch (e: Exception) {
        emptyList() // un texto dañado (p. ej. de otro dispositivo) no debe romper la pantalla
    }

    fun escribir(tarjetas: List<Tarjeta>): String =
        if (tarjetas.isEmpty()) "" else JsonArray(tarjetas.map { JsonObject(mapOf("p" to JsonPrimitive(it.pregunta), "r" to JsonPrimitive(it.respuesta))) }).toString()
}

/** Propone tarjetas a partir de cómo está escrito el apunte, sin IA. Quien repasa elige cuáles guardar. */
object GeneradorTarjetas {
    private const val MAX_TERMINO = 60
    private const val MAX_RESPUESTA = 400
    private const val HUECO = "_____"
    private val TITULOS = setOf(TiposBloque.TITULO, TiposBloque.SUBTITULO)
    private val SEPARADORES = listOf(": ", " — ", " – ", " - ")

    /** Tarjetas escritas como bloque «Tarjeta» dentro del apunte (las que tienen pregunta y respuesta). */
    fun delApunte(doc: Documento): List<Tarjeta> =
        doc.bloques.filter { it.kind == TiposBloque.TARJETA && it.text.isNotBlank() && !it.answer.isNullOrBlank() }
            .map { Tarjeta(it.text.trim(), it.answer!!.trim()) }

    /**
     * Reglas, en este orden: un título con su explicación debajo; una pregunta (termina en «?») con el párrafo siguiente;
     * «Término: definición» (también con guion largo); y cada palabra en **negrita** como hueco a rellenar.
     * No repite preguntas de [yaHay] ni las tarjetas que ya están escritas en el apunte.
     */
    fun proponer(doc: Documento, yaHay: List<Tarjeta> = emptyList()): List<Tarjeta> {
        val vistas = (yaHay + delApunte(doc)).mapTo(HashSet()) { clave(it.pregunta) }
        val salida = ArrayList<Tarjeta>()
        fun anadir(p: String, r: String) {
            val pregunta = p.trim()
            val respuesta = recortar(r.trim())
            if (pregunta.isNotEmpty() && respuesta.isNotEmpty() && vistas.add(clave(pregunta))) salida += Tarjeta(pregunta, respuesta)
        }
        val bloques = doc.bloques
        // Los bloques usados como respuesta de una pregunta no generan otra tarjeta.
        val usados = HashSet<Int>()
        for ((i, b) in bloques.withIndex()) {
            if (i in usados) continue
            val texto = b.text.trim()
            when {
                b.kind in TITULOS -> {
                    val cuerpo = bloques.drop(i + 1).takeWhile { it.kind !in TITULOS }
                        .filter { it.kind in TiposBloque.deTexto }.map { it.text.trim() }.filter { it.isNotEmpty() }
                    if (texto.isNotEmpty() && cuerpo.isNotEmpty()) anadir(texto, cuerpo.joinToString("\n"))
                }
                b.kind !in TiposBloque.deTexto -> Unit
                texto.endsWith("?") -> {
                    val j = i + 1
                    val siguiente = bloques.getOrNull(j)
                    if (siguiente != null && siguiente.kind in TiposBloque.deTexto && siguiente.kind !in TITULOS && siguiente.text.isNotBlank()) {
                        anadir(texto, siguiente.text)
                        usados += j
                    }
                }
                else -> {
                    val par = terminoYDefinicion(texto)
                    if (par != null) anadir(par.first, par.second)
                    else for (n in negritas(b)) anadir(texto.replace(n, HUECO), n)
                }
            }
        }
        return salida
    }

    /** «Término: definición» en una línea, con el término corto (hasta 6 palabras). */
    internal fun terminoYDefinicion(texto: String): Pair<String, String>? {
        if (texto.contains('\n')) return null
        for (sep in SEPARADORES) {
            val i = texto.indexOf(sep)
            if (i <= 0) continue
            val termino = texto.substring(0, i).trim()
            val definicion = texto.substring(i + sep.length).trim()
            if (termino.length in 2..MAX_TERMINO && termino.split(' ').size <= 6 && definicion.length >= 3) return termino to definicion
        }
        return null
    }

    /** Fragmentos en negrita de un bloque (sin contar el bloque entero en negrita: ahí no hay contexto). */
    private fun negritas(b: Bloque): List<String> =
        b.runs.orEmpty().filter { it.bold }.map { it.text.trim() }
            .filter { it.length in 2..MAX_TERMINO && it != b.text.trim() }.distinct()

    private fun recortar(texto: String): String {
        if (texto.length <= MAX_RESPUESTA) return texto
        val corte = texto.lastIndexOf(' ', MAX_RESPUESTA).takeIf { it > MAX_RESPUESTA / 2 } ?: MAX_RESPUESTA
        return texto.take(corte).trimEnd() + "…"
    }

    private fun clave(texto: String) =
        java.text.Normalizer.normalize(texto.lowercase().trim(), java.text.Normalizer.Form.NFD).replace(Regex("""\p{Mn}+"""), "")
}

object Examenes {
    /** Días naturales que faltan para la fecha de [entrega]: 0 = hoy, negativo = ya pasó. */
    fun diasRestantes(entrega: Long, ahora: Long, zona: ZoneId = ZoneId.systemDefault()): Long =
        ChronoUnit.DAYS.between(Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate(), Instant.ofEpochMilli(entrega).atZone(zona).toLocalDate())
}
