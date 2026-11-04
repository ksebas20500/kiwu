package app.kiwu.android.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URLEncoder

/**
 * La copia de seguridad `.kiwu.json` en el Drive de la persona (no en Firebase): un solo archivo visible en «Mi unidad»
 * que se sobrescribe en cada copia. Drive guarda las versiones anteriores del archivo por su cuenta.
 */
class RespaldoDrive(private val token: String) {
    private val json = Json { ignoreUnknownKeys = true }
    private val api = "https://www.googleapis.com/drive/v3/files"

    private suspend fun pedir(url: String, metodo: String, cuerpo: String? = null, tipo: String = "application/json", cabeceras: Map<String, String> = emptyMap()): String {
        val (codigo, texto) = Http.enviar(url, cuerpo, token, tipo, metodo, cabeceras)
        if (codigo == 401) throw ErrorCuenta("DRIVE_AUTORIZACION", "Drive 401")
        if (codigo == 403 && texto.contains("accessNotConfigured")) throw ErrorCuenta("DRIVE_NO_ACTIVADA", "Drive API desactivada")
        if (codigo == 403 && texto.contains("storageQuotaExceeded")) throw ErrorCuenta("DRIVE_LLENO", "Drive lleno")
        if (codigo !in 200..299) throw ErrorCuenta("DRIVE_ERROR", "Drive $codigo: ${texto.take(200)}")
        return texto
    }

    /** El id de la copia más reciente, o `null` si todavía no hay ninguna. */
    private suspend fun buscar(): String? {
        val q = URLEncoder.encode("name = '$NOMBRE' and trashed = false", "UTF-8")
        val r = json.parseToJsonElement(pedir("$api?q=$q&orderBy=modifiedTime%20desc&pageSize=1&fields=files(id)", "GET")).jsonObject
        return r["files"]?.jsonArray?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
    }

    suspend fun subir(copia: String) {
        val id = buscar()
        val metadatos = buildJsonObject { if (id == null) put("name", NOMBRE); put("mimeType", "application/json") }.toString()
        val url = "https://www.googleapis.com/upload/drive/v3/files" + (if (id != null) "/$id" else "") + "?uploadType=multipart"
        // HttpURLConnection no sabe hacer PATCH: Google acepta POST con esta cabecera.
        val cabeceras = if (id != null) mapOf("X-HTTP-Method-Override" to "PATCH") else emptyMap()
        pedir(url, "POST", cuerpoMultipart(metadatos, copia), "multipart/related; boundary=$LIMITE", cabeceras)
    }

    /** El texto de la copia más reciente, o `null` si no hay ninguna. */
    suspend fun bajar(): String? = buscar()?.let { pedir("$api/$it?alt=media", "GET") }

    companion object {
        const val NOMBRE = "Kiwu - copia de seguridad.kiwu.json"
        private const val LIMITE = "kiwu_limite_7f3a"

        internal fun cuerpoMultipart(metadatos: String, contenido: String) =
            "--$LIMITE\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadatos\r\n" +
                "--$LIMITE\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$contenido\r\n--$LIMITE--"
    }
}
