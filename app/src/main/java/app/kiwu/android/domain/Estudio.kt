package app.kiwu.android.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Una nota de una materia (lista): lo que sacaste y cuánto pesa en el promedio. La escala (5, 10, 100…) la pones tú. */
data class Calificacion(val nombre: String, val nota: Double, val peso: Double = 1.0)

/** Configuración de calificaciones de una materia: la nota mínima para aprobar y la lista de notas. */
data class ConfigCalificaciones(
    val minimaAprobacion: Double = 3.0,
    val notas: List<Calificacion> = emptyList(),
)

object Calificaciones {
    fun leer(json: String): List<Calificacion> = try {
        if (json.isBlank()) emptyList()
        else {
            val element = Json.parseToJsonElement(json)
            val array = if (element is JsonArray) element else element.jsonObject["notas"]?.jsonArray
            array?.map {
                val o = it.jsonObject
                Calificacion(o.getValue("n").jsonPrimitive.content, o.getValue("v").jsonPrimitive.double, o["p"]?.jsonPrimitive?.double ?: 1.0)
            } ?: emptyList()
        }
    } catch (e: Exception) {
        emptyList() // un texto dañado (p. ej. de otro dispositivo) no debe romper la pantalla
    }

    fun leerConfig(json: String): ConfigCalificaciones = try {
        if (json.isBlank()) ConfigCalificaciones()
        else {
            val element = Json.parseToJsonElement(json)
            if (element is JsonObject) {
                val min = element["minima"]?.jsonPrimitive?.double ?: 3.0
                ConfigCalificaciones(minimaAprobacion = min, notas = leer(json))
            } else {
                ConfigCalificaciones(minimaAprobacion = 3.0, notas = leer(json))
            }
        }
    } catch (e: Exception) {
        ConfigCalificaciones(minimaAprobacion = 3.0, notas = leer(json))
    }

    fun escribir(notas: List<Calificacion>): String =
        if (notas.isEmpty()) "" else JsonArray(notas.map { JsonObject(mapOf("n" to JsonPrimitive(it.nombre), "v" to JsonPrimitive(it.nota), "p" to JsonPrimitive(it.peso))) }).toString()

    fun escribirConfig(config: ConfigCalificaciones): String =
        JsonObject(mapOf(
            "minima" to JsonPrimitive(config.minimaAprobacion),
            "notas" to Json.parseToJsonElement(escribir(config.notas)),
        )).toString()

    fun sumaPesos(notas: List<Calificacion>): Double = notas.sumOf { it.peso }

    fun puntosAcumulados(notas: List<Calificacion>): Double =
        notas.sumOf { it.nota * (it.peso / 100.0) }

    /** Promedio ponderado por peso; null si no hay notas o los pesos suman cero. */
    fun promedio(notas: List<Calificacion>): Double? {
        val pesos = sumaPesos(notas)
        return if (notas.isEmpty() || pesos <= 0.0) null else notas.sumOf { it.nota * it.peso } / pesos
    }

    /** Promedio acumulado considerando el porcentaje evaluado hasta ahora. */
    fun promedioAcumulado(notas: List<Calificacion>): Double? {
        val pesos = sumaPesos(notas)
        return if (notas.isEmpty() || pesos <= 0.0) null else puntosAcumulados(notas) / (pesos / 100.0)
    }

    /** Nota que hace falta en lo que queda (con peso [pesoRestante]) para terminar con promedio [meta]; null si no queda peso. */
    fun necesaria(notas: List<Calificacion>, meta: Double, pesoRestante: Double): Double? {
        if (pesoRestante <= 0.0) return null
        return (meta * (notas.sumOf { it.peso } + pesoRestante) - notas.sumOf { it.nota * it.peso }) / pesoRestante
    }

    /** Nota que hace falta en el % restante para aprobar con la nota [minimaAprobacion]. */
    fun necesariaParaAprobar(notas: List<Calificacion>, minimaAprobacion: Double = 3.0): Double? {
        val pesoRestante = (100.0 - sumaPesos(notas)).coerceAtLeast(0.0)
        if (pesoRestante <= 0.0) return null
        val acumulado = puntosAcumulados(notas)
        val faltaPuntos = minimaAprobacion - acumulado
        if (faltaPuntos <= 0.0) return 0.0
        return faltaPuntos / (pesoRestante / 100.0)
    }
}

/**
 * Una tarjeta de repaso: pregunta y respuesta. `caja` y `vence` guardan el repaso espaciado (ver [Repaso]);
 * una tarjeta sin repasar todavía tiene `vence = 0` y las versiones anteriores la leen igual.
 */
data class Tarjeta(val pregunta: String, val respuesta: String, val caja: Int = 0, val vence: Long = 0)

object Tarjetas {
    fun leer(json: String): List<Tarjeta> {
        if (json.isBlank()) return emptyList()
        return try {
            val elemento = Json.parseToJsonElement(json)
            // Formato nuevo: {"fecha":"...","items":[...]}  |  Formato antiguo: [...]
            val array = if (elemento is JsonArray) elemento else elemento.jsonObject["items"]?.jsonArray
                ?: return emptyList()
            array.map {
                val o = it.jsonObject
                Tarjeta(
                    o.getValue("p").jsonPrimitive.content,
                    o.getValue("r").jsonPrimitive.content,
                    o["c"]?.jsonPrimitive?.intOrNull ?: 0,
                    o["v"]?.jsonPrimitive?.longOrNull ?: 0L,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Lee la fecha de examen local guardada en la nota (ISO «yyyy-MM-dd»), si existe. */
    fun leerFechaExamen(json: String): String? = try {
        if (json.isBlank()) null
        else {
            val e = Json.parseToJsonElement(json)
            if (e is JsonArray) null else e.jsonObject["fecha"]?.jsonPrimitive?.content
        }
    } catch (_: Exception) { null }

    fun escribir(tarjetas: List<Tarjeta>, fechaExamen: String? = null): String {
        if (tarjetas.isEmpty() && fechaExamen == null) return ""
        val items = JsonArray(
            tarjetas.map { t ->
                JsonObject(buildMap {
                    put("p", JsonPrimitive(t.pregunta))
                    put("r", JsonPrimitive(t.respuesta))
                    if (t.caja != 0) put("c", JsonPrimitive(t.caja))
                    if (t.vence != 0L) put("v", JsonPrimitive(t.vence))
                })
            },
        )
        // Si hay fecha de examen usamos el wrapper objeto; si no, el array directo (compatible con versiones anteriores).
        return if (fechaExamen != null) {
            JsonObject(mapOf("fecha" to JsonPrimitive(fechaExamen), "items" to items)).toString()
        } else {
            items.toString()
        }
    }
}

/**
 * Repaso espaciado con cajas de Leitner: cada tarjeta acertada sube de caja y tarda el doble en volver;
 * las falladas vuelven a empezar y siguen pendientes. Las cajas solo aplican a las tarjetas guardadas
 * (las escritas como bloque en el apunte no tienen dónde recordar su caja).
 */
object Repaso {
    /** Cuántas cajas hay (0 = nueva o fallada; las otras 5 esperan 1, 2, 4, 8 y 16 días). */
    const val CAJAS = 6

    /** Días que espera una tarjeta acertada según su caja (el índice es la caja; la 0 no se usa). */
    val DIAS = longArrayOf(0, 1, 2, 4, 8, 16)

    /** Las tarjetas cuya hora de repaso ya llegó: las nuevas (vence = 0) primero, luego las más atrasadas. */
    fun vencen(tarjetas: List<Tarjeta>, ahora: Long): List<Tarjeta> =
        tarjetas.filter { it.vence <= ahora }.sortedBy { it.vence }

    /** La tarjeta se supo: sube de caja y vuelve dentro de los días que le tocan. */
    fun acertada(t: Tarjeta, ahora: Long): Tarjeta {
        val caja = (t.caja + 1).coerceAtMost(CAJAS - 1)
        return t.copy(caja = caja, vence = ahora + DIAS[caja] * 24 * 60 * 60 * 1000L)
    }

    /** La tarjeta no se supo: vuelve a empezar y queda pendiente (reaparece en la misma sesión). */
    fun fallada(t: Tarjeta, ahora: Long): Tarjeta = t.copy(caja = 0, vence = ahora)
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

    /** Repasos antes de un examen: 7, 3 y 1 días antes a las 18:00, sin los que ya pasaron. */
    fun sesionesRepaso(entrega: Long, ahora: Long, zona: ZoneId = ZoneId.systemDefault()): List<Long> {
        val examen = Instant.ofEpochMilli(entrega).atZone(zona).toLocalDate()
        return listOf(7L, 3L, 1L).map { examen.minusDays(it).atTime(18, 0).atZone(zona).toInstant().toEpochMilli() }.filter { it > ahora }
    }
}

/**
 * Una sesión del temporizador: la lista (materia) en la que se estudió, el día (`LocalDate.toEpochDay`) y los minutos.
 * Vive en Room (tabla `sesion`): así viaja en el sync y entra al respaldo; hasta 0.8.2 vivía en preferencias
 * (el arranque la importa una vez, ver `KiwuApp`).
 */
@Entity(tableName = "sesion", indices = [Index("dia")])
data class SesionEstudio(
    @PrimaryKey val id: String = nuevoId(),
    @ColumnInfo(name = "materia_id") val materiaId: String?,
    /** El día de la sesión como `LocalDate.toEpochDay()`. */
    val dia: Long,
    val minutos: Int,
    val creada: Long = 0,
    val actualizada: Long = 0,
    val eliminada: Long? = null,
)

object TiempoEstudio {
    /** Las sesiones que guardaban en preferencias las versiones anteriores (JSON `{m, d, n}`). Solo para la importación. */
    fun leer(json: String): List<SesionEstudio> = try {
        if (json.isBlank()) {
            emptyList()
        } else {
            Json.parseToJsonElement(json).jsonArray.map {
                val o = it.jsonObject
                SesionEstudio(materiaId = o["m"]?.jsonPrimitive?.content, dia = o.getValue("d").jsonPrimitive.content.toLong(), minutos = o.getValue("n").jsonPrimitive.content.toInt())
            }
        }
    } catch (e: Exception) {
        emptyList()
    }

    /**
     * Minutos efectivamente estudiados de una sesión que quedó con [restanteMs] sin terminar (0 si llegó al final).
     * El −1 redondea por lo bajo: un «40 min estudiados» de la pantalla cuenta la marca del minuto incompleto.
     */
    fun minutosHechos(minutos: Int, restanteMs: Long): Int =
        (minutos - (restanteMs / 60_000).toInt() - 1).coerceAtLeast(0)

    /** Minutos por lista en la semana (de lunes a domingo) que contiene [hoy], de más a menos. */
    fun semana(s: List<SesionEstudio>, hoy: java.time.LocalDate): List<Pair<String?, Int>> {
        val lunes = hoy.with(java.time.DayOfWeek.MONDAY).toEpochDay()
        return s.filter { it.eliminada == null && it.dia in lunes until lunes + 7 }.groupBy { it.materiaId }
            .map { (m, l) -> m to l.sumOf { it.minutos } }.sortedByDescending { it.second }
    }
}
