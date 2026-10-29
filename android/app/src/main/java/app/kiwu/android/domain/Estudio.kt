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

object Examenes {
    /** Días naturales que faltan para la fecha de [entrega]: 0 = hoy, negativo = ya pasó. */
    fun diasRestantes(entrega: Long, ahora: Long, zona: ZoneId = ZoneId.systemDefault()): Long =
        ChronoUnit.DAYS.between(Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate(), Instant.ofEpochMilli(entrega).atZone(zona).toLocalDate())
}
