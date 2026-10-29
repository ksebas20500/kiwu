package app.kiwu.android.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Un paso de una tarea (subtarea). Se guardan como texto JSON en la columna `pasos` de la tarea. */
data class Paso(val texto: String, val hecho: Boolean = false)

object Pasos {
    fun leer(json: String): List<Paso> = try {
        if (json.isBlank()) emptyList()
        else Json.parseToJsonElement(json).jsonArray.map { Paso(it.jsonObject.getValue("t").jsonPrimitive.content, it.jsonObject["h"]?.jsonPrimitive?.boolean ?: false) }
    } catch (e: Exception) {
        emptyList() // un texto dañado (p. ej. de otro dispositivo) no debe romper la pantalla
    }

    fun escribir(pasos: List<Paso>): String =
        if (pasos.isEmpty()) "" else JsonArray(pasos.map { JsonObject(mapOf("t" to JsonPrimitive(it.texto), "h" to JsonPrimitive(it.hecho))) }).toString()

    /** «2/5» si hay pasos; si no, null. */
    fun progreso(json: String): String? = leer(json).takeIf { it.isNotEmpty() }?.let { "${it.count { p -> p.hecho }}/${it.size}" }
}

/** Prioridad de una tarea: 0 = ninguna, 1 = baja, 2 = media, 3 = alta. */
object Prioridad {
    const val NINGUNA = 0
    const val BAJA = 1
    const val MEDIA = 2
    const val ALTA = 3
}

/** Las etiquetas de una tarea van en una sola columna, separadas por coma (una etiqueta no puede llevar comas). */
object Etiquetas {
    fun leer(texto: String): List<String> = texto.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    fun escribir(etiquetas: List<String>): String = etiquetas.map { it.replace(",", " ").trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.joinToString(",")
}
