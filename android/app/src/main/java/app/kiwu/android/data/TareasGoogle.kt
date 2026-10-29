package app.kiwu.android.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import app.kiwu.android.domain.EventoExterno
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant

/**
 * Las tareas con fecha de Google Tasks (las «Tareas» de Google Calendar), solo lectura. No salen del calendario del
 * teléfono: hace falta el permiso `tasks.readonly` de Google, que se pide una vez en Ajustes. La lista se guarda en el
 * teléfono (el widget no puede enseñar la pantalla de permiso) y se renueva sola cada 15 minutos si el widget se redibuja.
 *
 * shortcut: solo cuenta la cuenta de Google que dio el permiso y las tareas con fecha de entrega; las completadas se
 * ocultan. Añadir varias cuentas o las tareas sin fecha si alguien lo pide.
 */
object TareasGoogle {
    /** Apagado en las versiones publicadas: falta activar la API de Google Tasks y la verificación de Google. */
    const val DISPONIBLE = false

    const val ALCANCE = "https://www.googleapis.com/auth/tasks.readonly"
    private const val API = "https://tasks.googleapis.com/tasks/v1"
    private const val PREFS = "tareas_google"
    private const val VIGENCIA_MS = 15 * 60_000L
    private val json = Json { ignoreUnknownKeys = true }

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** O ya hay token, o hay que pedirle el consentimiento a la persona (una sola vez). */
    suspend fun pedir(c: Context): PermisoDrive {
        val r = try {
            Identity.getAuthorizationClient(c)
                .authorize(AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(ALCANCE))).build()).esperar()
        } catch (e: Exception) {
            throw ErrorCuenta("GOOGLE_ERROR", e.message ?: "Google")
        }
        val intencion = r.pendingIntent
        return if (r.hasResolution() && intencion != null) PermisoDrive.Pedir(intencion)
        else PermisoDrive.Listo(r.accessToken ?: throw ErrorCuenta("GOOGLE_ERROR", "Sin token"))
    }

    /** El token que sale de la pantalla de consentimiento. */
    fun token(actividad: Activity, datos: Intent?): String? =
        try { Identity.getAuthorizationClient(actividad).getAuthorizationResultFromIntent(datos).accessToken } catch (e: Exception) { null }

    /** Vuelve a leer las tareas de Google y las guarda. `false` si falta el permiso o falla la red. */
    suspend fun actualizar(c: Context, token: String? = null): Boolean = try {
        val t = token ?: (pedir(c) as? PermisoDrive.Listo)?.token ?: return false
        val listas = obtener("$API/users/@me/lists?maxResults=100", t).getValue("items").jsonArray.map { it.jsonObject.getValue("id").jsonPrimitive.content }
        val tareas = buildJsonArray {
            for (lista in listas) {
                val items = obtener("$API/lists/$lista/tasks?showCompleted=false&showHidden=false&maxResults=100", t)["items"]?.jsonArray.orEmpty()
                for (it in items) {
                    val o = it.jsonObject
                    val due = o["due"]?.jsonPrimitive?.contentOrNull ?: continue
                    val titulo = o["title"]?.jsonPrimitive?.contentOrNull?.takeIf { s -> s.isNotBlank() } ?: continue
                    add(buildJsonObject { put("i", o.getValue("id").jsonPrimitive.content); put("t", titulo); put("d", Instant.parse(due).toEpochMilli()) })
                }
            }
        }
        prefs(c).edit().putString("datos", tareas.toString()).putLong("hora", System.currentTimeMillis()).apply()
        true
    } catch (e: Exception) {
        false // sin red, sin permiso o con la API de Tasks apagada: se queda lo último que se guardó
    }

    private suspend fun obtener(url: String, token: String) =
        Http.enviar(url, null, token, metodo = "GET").let { (codigo, texto) ->
            if (codigo !in 200..299) throw ErrorCuenta(null, "Tasks $codigo: ${texto.take(200)}")
            json.parseToJsonElement(texto).jsonObject
        }

    fun vieja(c: Context): Boolean = System.currentTimeMillis() - prefs(c).getLong("hora", 0) > VIGENCIA_MS

    /** Lo último guardado, como eventos de todo el día (la fecha de Google Tasks va en UTC, sin hora). */
    fun leer(c: Context): List<EventoExterno> = try {
        json.parseToJsonElement(prefs(c).getString("datos", "[]").orEmpty()).jsonArray.map {
            val o = it.jsonObject
            EventoExterno(o.getValue("i").jsonPrimitive.content.hashCode().toLong(), o.getValue("t").jsonPrimitive.content, o.getValue("d").jsonPrimitive.content.toLong(), true, tarea = true)
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun borrar(c: Context) = prefs(c).edit().clear().apply()
}
