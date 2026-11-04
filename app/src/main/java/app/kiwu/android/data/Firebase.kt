package app.kiwu.android.data

import android.content.Context
import app.kiwu.android.domain.Campo
import app.kiwu.android.domain.CuotaAgotada
import app.kiwu.android.domain.FilaSync
import app.kiwu.android.domain.LoteSync
import app.kiwu.android.domain.Marca
import app.kiwu.android.domain.Nube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** La cuenta o la red fallaron; [codigo] es el de Firebase («EMAIL_EXISTS»…) o `null` si fue la red. */
class ErrorCuenta(val codigo: String?, mensaje: String) : IOException(mensaje)

/** Quién tiene la sesión abierta. El token de acceso dura una hora; el de renovación, hasta que se cierre la sesión. */
data class Sesion(val uid: String, val correo: String, val idToken: String, val renovacion: String, val venceMs: Long, val google: Boolean = false)

/** Guarda la sesión en el almacenamiento privado de la app (ninguna otra app lo lee). */
class AlmacenSesion(contexto: Context) {
    private val prefs = contexto.applicationContext.getSharedPreferences("cuenta", Context.MODE_PRIVATE)

    fun leer(): Sesion? {
        val uid = prefs.getString("uid", null) ?: return null
        return Sesion(
            uid, prefs.getString("correo", "").orEmpty(), prefs.getString("idToken", "").orEmpty(),
            prefs.getString("renovacion", null) ?: return null, prefs.getLong("vence", 0), prefs.getBoolean("google", false),
        )
    }

    fun guardar(s: Sesion?) {
        prefs.edit().apply {
            if (s == null) clear() else {
                putString("uid", s.uid); putString("correo", s.correo); putString("idToken", s.idToken)
                putString("renovacion", s.renovacion); putLong("vence", s.venceMs); putBoolean("google", s.google)
            }
        }.apply()
    }
}

internal object Http {
    suspend fun enviar(
        url: String, cuerpo: String?, token: String? = null, tipo: String = "application/json",
        metodo: String = "POST", cabeceras: Map<String, String> = emptyMap(),
    ): Pair<Int, String> =
        withContext(Dispatchers.IO) {
            val c = URL(url).openConnection() as HttpURLConnection
            try {
                c.requestMethod = metodo
                c.connectTimeout = 15_000
                c.readTimeout = 30_000
                c.setRequestProperty("Content-Type", tipo)
                if (token != null) c.setRequestProperty("Authorization", "Bearer $token")
                cabeceras.forEach { (k, v) -> c.setRequestProperty(k, v) }
                if (cuerpo != null) {
                    c.doOutput = true
                    c.outputStream.use { it.write(cuerpo.toByteArray()) }
                }
                val flujo = if (c.responseCode in 200..299) c.inputStream else c.errorStream
                c.responseCode to (flujo?.bufferedReader()?.use { it.readText() }.orEmpty())
            } catch (e: IOException) {
                throw ErrorCuenta(null, e.message ?: "Sin conexión")
            } finally {
                c.disconnect()
            }
        }
}

/** Inicio de sesión con correo y contraseña por la API REST de Firebase (la misma en Android, macOS y Linux). */
class CuentaFirebase(private val apiKey: String, private val almacen: AlmacenSesion) {
    private val json = Json { ignoreUnknownKeys = true }
    val disponible: Boolean get() = apiKey.isNotEmpty()

    var sesion: Sesion? = almacen.leer()
        private set

    private fun url(metodo: String) = "https://identitytoolkit.googleapis.com/v1/accounts:$metodo?key=$apiKey"

    private suspend fun identidad(metodo: String, cuerpo: JsonObject): JsonObject {
        val (codigo, texto) = Http.enviar(url(metodo), cuerpo.toString())
        val respuesta = runCatching { json.parseToJsonElement(texto).jsonObject }.getOrDefault(JsonObject(emptyMap()))
        if (codigo !in 200..299) {
            val mensaje = respuesta["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull ?: "ERROR"
            throw ErrorCuenta(mensaje.substringBefore(' '), mensaje)
        }
        return respuesta
    }

    private fun abrir(r: JsonObject, correo: String, google: Boolean = false): Sesion {
        val s = Sesion(
            r.getValue("localId").jsonPrimitive.content, correo, r.getValue("idToken").jsonPrimitive.content,
            r.getValue("refreshToken").jsonPrimitive.content,
            System.currentTimeMillis() + (r["expiresIn"]?.jsonPrimitive?.longOrNull ?: 3600L) * 1000, google,
        )
        almacen.guardar(s)
        sesion = s
        return s
    }

    private fun credenciales(correo: String, clave: String) = buildJsonObject {
        put("email", correo.trim()); put("password", clave); put("returnSecureToken", true)
    }

    suspend fun registrar(correo: String, clave: String): Sesion = abrir(identidad("signUp", credenciales(correo, clave)), correo.trim())

    suspend fun entrar(correo: String, clave: String): Sesion = abrir(identidad("signInWithPassword", credenciales(correo, clave)), correo.trim())

    /** Entra (o crea la cuenta) con el token que Google dio en el teléfono; Firebase lo comprueba con Google. */
    suspend fun entrarConGoogle(tokenGoogle: String): Sesion {
        val r = identidad("signInWithIdp", buildJsonObject {
            put("postBody", "id_token=" + URLEncoder.encode(tokenGoogle, "UTF-8") + "&providerId=google.com")
            put("requestUri", "http://localhost"); put("returnSecureToken", true); put("returnIdpCredential", true)
        })
        return abrir(r, r["email"]?.jsonPrimitive?.contentOrNull.orEmpty(), google = true)
    }

    suspend fun recuperar(correo: String) {
        identidad("sendOobCode", buildJsonObject { put("requestType", "PASSWORD_RESET"); put("email", correo.trim()) })
    }

    fun salir() {
        almacen.guardar(null)
        sesion = null
    }

    /** Borra la cuenta de Firebase (los datos de Firestore se borran antes, con [NubeFirestore.borrarTodo]). */
    suspend fun borrar() {
        if (sesion == null) return
        identidad("delete", buildJsonObject { put("idToken", token()) })
        salir()
    }

    /** Un token de acceso vigente; si está por caducar se renueva. Si la renovación falla porque la cuenta ya no existe, se cierra la sesión. */
    suspend fun token(forzar: Boolean = false): String {
        val s = sesion ?: throw ErrorCuenta("SIN_SESION", "No hay sesión")
        if (!forzar && System.currentTimeMillis() < s.venceMs - 60_000) return s.idToken
        val cuerpo = "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(s.renovacion, "UTF-8")
        val (codigo, texto) = Http.enviar("https://securetoken.googleapis.com/v1/token?key=$apiKey", cuerpo, tipo = "application/x-www-form-urlencoded")
        val r = runCatching { json.parseToJsonElement(texto).jsonObject }.getOrDefault(JsonObject(emptyMap()))
        if (codigo !in 200..299) {
            val mensaje = r["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull ?: "ERROR"
            if (mensaje.startsWith("TOKEN_EXPIRED") || mensaje.startsWith("USER_NOT_FOUND") || mensaje.startsWith("USER_DISABLED")) salir()
            throw ErrorCuenta(mensaje.substringBefore(' '), mensaje)
        }
        val nueva = s.copy(
            idToken = r.getValue("id_token").jsonPrimitive.content,
            renovacion = r["refresh_token"]?.jsonPrimitive?.contentOrNull ?: s.renovacion,
            venceMs = System.currentTimeMillis() + (r["expires_in"]?.jsonPrimitive?.longOrNull ?: 3600L) * 1000,
        )
        almacen.guardar(nueva)
        sesion = nueva
        return nueva.idToken
    }
}

/** Traducción entre el JSON de la base local y los «valores» tipados de Firestore. Puro: se prueba sin red. */
object ValoresFirestore {
    fun aValor(v: JsonElement): JsonObject = buildJsonObject {
        val p = v as? JsonPrimitive
        when {
            p == null || p is JsonNull -> put("nullValue", JsonNull)
            p.isString -> put("stringValue", p.content)
            p.booleanOrNull != null -> put("booleanValue", p.booleanOrNull!!)
            p.longOrNull != null -> put("integerValue", p.content)
            else -> put("doubleValue", p.doubleOrNull ?: 0.0)
        }
    }

    fun deValor(o: JsonObject): JsonElement = when {
        "stringValue" in o -> JsonPrimitive(o.getValue("stringValue").jsonPrimitive.content)
        "integerValue" in o -> JsonPrimitive(o.getValue("integerValue").jsonPrimitive.content.toLong())
        "doubleValue" in o -> JsonPrimitive(o.getValue("doubleValue").jsonPrimitive.double())
        "booleanValue" in o -> JsonPrimitive(o.getValue("booleanValue").jsonPrimitive.booleanOrNull ?: false)
        else -> JsonNull
    }

    private fun JsonPrimitive.double(): Double = content.toDouble()

    fun idDocumento(f: FilaSync) = "${f.tabla}_${f.id}"

    /** Un `write` de Firestore que reemplaza el documento de la fila y le pone la hora del servidor en `servidor`. */
    fun escritura(raiz: String, uid: String, f: FilaSync): JsonObject = buildJsonObject {
        putJsonObject("update") {
            put("name", "$raiz/usuarios/$uid/filas/${idDocumento(f)}")
            putJsonObject("fields") {
                putJsonObject("tabla") { put("stringValue", f.tabla) }
                putJsonObject("id") { put("stringValue", f.id) }
                putJsonObject("actualizada") { put("integerValue", (f.campos["actualizada"]?.marca?.ms ?: 0L).toString()) }
                putJsonObject("dispositivo") { put("stringValue", f.campos.values.firstOrNull()?.marca?.dispositivo.orEmpty()) }
                putJsonObject("datos") {
                    putJsonObject("mapValue") { putJsonObject("fields") { for ((k, c) in f.campos) put(k, aValor(c.valor)) } }
                }
            }
        }
        putJsonArray("updateTransforms") {
            add(buildJsonObject { put("fieldPath", "servidor"); put("setToServerValue", "REQUEST_TIME") })
        }
    }

    /** Un documento de Firestore → la fila y su posición (para el cursor). `null` si no es una fila de Kiwu. */
    fun fila(documento: JsonObject): Pair<FilaSync, String>? {
        val campos = documento["fields"]?.jsonObject ?: return null
        val tabla = campos["tabla"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.contentOrNull ?: return null
        val id = campos["id"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.contentOrNull ?: return null
        val actualizada = campos["actualizada"]?.jsonObject?.get("integerValue")?.jsonPrimitive?.longOrNull ?: 0L
        val dispositivo = campos["dispositivo"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.contentOrNull.orEmpty()
        val datos = campos["datos"]?.jsonObject?.get("mapValue")?.jsonObject?.get("fields")?.jsonObject ?: return null
        val marca = Marca(actualizada, 0, dispositivo)
        val servidor = campos["servidor"]?.jsonObject?.get("timestampValue")?.jsonPrimitive?.contentOrNull ?: return null
        val nombre = documento["name"]?.jsonPrimitive?.contentOrNull ?: return null
        return FilaSync(tabla, id, datos.mapValues { Campo(deValor(it.value.jsonObject), marca) }) to "$servidor|$nombre"
    }

    /** La consulta «lo que cambió después del cursor», en orden y por lotes. */
    fun consulta(cursor: String?, limite: Int): JsonObject = buildJsonObject {
        putJsonObject("structuredQuery") {
            putJsonArray("from") { add(buildJsonObject { put("collectionId", "filas") }) }
            putJsonArray("orderBy") {
                add(buildJsonObject { putJsonObject("field") { put("fieldPath", "servidor") }; put("direction", "ASCENDING") })
                add(buildJsonObject { putJsonObject("field") { put("fieldPath", "__name__") }; put("direction", "ASCENDING") })
            }
            if (cursor != null) {
                val (marca, nombre) = cursor.split("|", limit = 2)
                putJsonObject("startAt") {
                    put("before", false)
                    putJsonArray("values") {
                        add(buildJsonObject { put("timestampValue", marca) })
                        add(buildJsonObject { put("referenceValue", nombre) })
                    }
                }
            }
            put("limit", limite)
        }
    }
}

/** Firestore por REST, en la rama `usuarios/{uid}/filas` de la persona con la sesión abierta. */
class NubeFirestore(private val proyecto: String, private val cuenta: CuentaFirebase) : Nube {
    private val json = Json { ignoreUnknownKeys = true }
    private val raiz = "projects/$proyecto/databases/(default)/documents"
    private val base = "https://firestore.googleapis.com/v1"

    private suspend fun llamar(url: String, cuerpo: JsonElement): JsonElement {
        var (codigo, texto) = Http.enviar(url, cuerpo.toString(), cuenta.token())
        if (codigo == 401) { // el token caducó justo ahora: se renueva y se repite una vez
            val otra = Http.enviar(url, cuerpo.toString(), cuenta.token(forzar = true))
            codigo = otra.first
            texto = otra.second
        }
        if (codigo == 429 || texto.contains("RESOURCE_EXHAUSTED")) throw CuotaAgotada()
        if (codigo !in 200..299) throw ErrorCuenta(null, "Firestore $codigo: ${texto.take(200)}")
        return json.parseToJsonElement(texto)
    }

    private fun uid() = cuenta.sesion?.uid ?: throw ErrorCuenta("SIN_SESION", "No hay sesión")

    override suspend fun subir(filas: List<FilaSync>) {
        if (filas.isEmpty()) return
        val escrituras = buildJsonArray { filas.forEach { add(ValoresFirestore.escritura(raiz, uid(), it)) } }
        llamar("$base/$raiz:commit", buildJsonObject { put("writes", escrituras) })
    }

    override suspend fun bajar(desde: String?, limite: Int): LoteSync {
        val respuesta = llamar("$base/$raiz/usuarios/${uid()}:runQuery", ValoresFirestore.consulta(desde, limite)) as? JsonArray
            ?: return LoteSync(emptyList(), desde)
        val filas = respuesta.mapNotNull { (it as? JsonObject)?.get("document")?.jsonObject?.let(ValoresFirestore::fila) }
        return LoteSync(filas.map { it.first }, filas.lastOrNull()?.second ?: desde)
    }

    /** Borra todos los documentos de la persona (antes de borrar la cuenta). Devuelve cuántos. */
    suspend fun borrarTodo(): Int {
        var total = 0
        while (true) {
            val consulta = buildJsonObject {
                putJsonObject("structuredQuery") {
                    putJsonArray("from") { add(buildJsonObject { put("collectionId", "filas") }) }
                    putJsonObject("select") { putJsonArray("fields") { add(buildJsonObject { put("fieldPath", "__name__") }) } }
                    put("limit", 200)
                }
            }
            val nombres = (llamar("$base/$raiz/usuarios/${uid()}:runQuery", consulta) as JsonArray)
                .mapNotNull { (it as? JsonObject)?.get("document")?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull }
            if (nombres.isEmpty()) return total
            val borrados = buildJsonArray { nombres.forEach { n -> add(buildJsonObject { put("delete", n) }) } }
            llamar("$base/$raiz:commit", buildJsonObject { put("writes", borrados) })
            total += nombres.size
        }
    }
}
