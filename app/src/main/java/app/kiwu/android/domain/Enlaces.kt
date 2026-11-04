package app.kiwu.android.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.net.URI
import java.net.URLEncoder
import java.util.Base64

/** Datos de la vista previa de un enlace; mismo formato que `LinkPreview` en macOS. */
data class VistaPrevia(
    val titulo: String? = null,
    val descripcion: String? = null,
    val sitio: String? = null,
    /** Imagen reducida en base64. */
    val imagen: String? = null,
    /** «youtube» o «vimeo» si es un vídeo reproducible. */
    val proveedor: String? = null,
    val videoID: String? = null,
) {
    fun aJson(): JsonObject = buildJsonObject {
        titulo?.let { put("titulo", it) }
        descripcion?.let { put("descripcion", it) }
        sitio?.let { put("sitio", it) }
        imagen?.let { put("imagen", it) }
        proveedor?.let { put("proveedor", it) }
        videoID?.let { put("videoID", it) }
    }
}

/**
 * Vista previa de enlaces (páginas web y vídeos de YouTube/Vimeo). Solo hay conexión a Internet cuando la persona
 * añade un enlace a una nota; `descargar` es lo único que toca la red y se puede sustituir en las pruebas.
 */
object Enlaces {
    private val idYoutube = Regex("^[A-Za-z0-9_-]{6,15}$")

    /** Normaliza lo escrito a una URL http(s); null si no parece una dirección. */
    fun urlDesdeTexto(texto: String?): String? {
        var t = (texto ?: "").trim()
        if (t.isEmpty() || t.any { it.isWhitespace() }) return null
        if ("://" !in t) t = "https://$t"
        val uri = try { URI(t) } catch (e: Exception) { return null }
        val host = uri.host ?: return null
        if (uri.scheme != "http" && uri.scheme != "https") return null
        return if ('.' in host) t else null
    }

    /** Un párrafo que es solo una dirección web (se convierte en tarjeta al pulsar Intro). */
    fun pareceEnlaceSuelto(texto: String?): Boolean {
        val t = (texto ?: "").trim()
        return (t.startsWith("http://") || t.startsWith("https://") || t.startsWith("www.")) && urlDesdeTexto(t) != null
    }

    private fun consulta(uri: URI): Map<String, String> =
        (uri.rawQuery ?: "").split("&").filter { "=" in it }.associate {
            it.substringBefore("=") to it.substringAfter("=")
        }

    fun videoYoutube(url: String): String? {
        val uri = try { URI(url) } catch (e: Exception) { return null }
        val host = (uri.host ?: "").lowercase().removePrefix("www.").removePrefix("m.")
        val partes = (uri.path ?: "").split("/").filter { it.isNotEmpty() }
        val id = when {
            host == "youtu.be" -> partes.firstOrNull()
            host.endsWith("youtube.com") || host.endsWith("youtube-nocookie.com") ->
                consulta(uri)["v"] ?: if (partes.size >= 2 && partes[0] in setOf("shorts", "embed", "live", "v")) partes[1] else null
            else -> null
        }
        return id?.takeIf { idYoutube.matches(it) }
    }

    fun videoVimeo(url: String): String? {
        val uri = try { URI(url) } catch (e: Exception) { return null }
        if (!(uri.host ?: "").lowercase().endsWith("vimeo.com")) return null
        return (uri.path ?: "").split("/").filter { it.isNotEmpty() && it.all(Char::isDigit) }.lastOrNull()
    }

    /** Dirección del reproductor incrustado. */
    fun urlIncrustada(proveedor: String, id: String): String =
        if (proveedor == "youtube") "https://www.youtube.com/embed/$id?autoplay=1&rel=0&playsinline=1"
        else "https://player.vimeo.com/video/$id?autoplay=1"

    // --- análisis de HTML ----------------------------------------------------------------------

    class InfoHtml(val titulo: String?, val descripcion: String?, val sitio: String?, val imagenUrl: String?)

    private val etiquetaMeta = Regex("<meta\\s+([^>]*)>", RegexOption.IGNORE_CASE)
    private val atributo = Regex("([a-zA-Z:_-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')")
    private val etiquetaTitulo = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val entidad = Regex("&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);")

    internal fun decodificarEntidades(texto: String): String = entidad.replace(texto) { m ->
        val e = m.groupValues[1]
        when {
            e.startsWith("#x") -> e.drop(2).toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: m.value
            e.startsWith("#") -> e.drop(1).toIntOrNull()?.let { String(Character.toChars(it)) } ?: m.value
            else -> when (e) {
                "amp" -> "&"; "lt" -> "<"; "gt" -> ">"; "quot" -> "\""; "apos" -> "'"; "nbsp" -> " "
                else -> m.value
            }
        }
    }

    /** Extrae título, descripción, sitio e imagen (Open Graph / Twitter / etiquetas básicas). */
    fun analizarHtml(html: String, urlBase: String): InfoHtml {
        val trozo = html.take(400_000)
        val meta = LinkedHashMap<String, String>()
        for (m in etiquetaMeta.findAll(trozo)) {
            val a = HashMap<String, String>()
            for (p in atributo.findAll(m.groupValues[1])) {
                a[p.groupValues[1].lowercase()] = p.groupValues[2].ifEmpty { p.groupValues[3] }
            }
            val clave = (a["property"]?.ifEmpty { null } ?: a["name"] ?: "").lowercase()
            val contenido = a["content"]
            if (clave.isNotEmpty() && !contenido.isNullOrEmpty() && clave !in meta) {
                meta[clave] = decodificarEntidades(contenido).trim()
            }
        }
        val tituloHtml = etiquetaTitulo.find(trozo)?.groupValues?.get(1)?.let { decodificarEntidades(it).trim() }
        val titulo = meta["og:title"] ?: meta["twitter:title"] ?: tituloHtml?.ifEmpty { null }
        val descripcion = meta["og:description"] ?: meta["twitter:description"] ?: meta["description"]
        val hostBase = try { URI(urlBase).host } catch (e: Exception) { null }
        val sitio = meta["og:site_name"] ?: hostBase?.removePrefix("www.")?.ifEmpty { null }
        val imagen = (meta["og:image"] ?: meta["twitter:image"])?.let {
            try { URI(urlBase).resolve(it).toString() } catch (e: Exception) { it }
        }
        return InfoHtml(titulo, descripcion, sitio, imagen)
    }

    // --- obtención ----------------------------------------------------------------------------

    /**
     * Devuelve la vista previa o null. `reducir` recibe los bytes de la imagen y devuelve una versión pequeña
     * (en la app se usa `Bitmap`); `descargar(url, maxBytes)` devuelve null si falla.
     */
    fun obtener(
        url: String,
        descargar: (String, Int) -> ByteArray?,
        reducir: ((ByteArray) -> ByteArray?)? = null,
    ): VistaPrevia? {
        fun imagenDe(direccion: String?): String? {
            if (direccion.isNullOrEmpty()) return null
            var datos = descargar(direccion, 5_000_000) ?: return null
            if (reducir != null) datos = reducir(datos) ?: return null
            return Base64.getEncoder().encodeToString(datos)
        }

        fun oembed(direccion: String): JsonObject? =
            descargar(direccion, 1_000_000)?.let {
                try { Json.parseToJsonElement(String(it, Charsets.UTF_8)).jsonObject } catch (e: Exception) { null }
            }

        fun JsonObject.cadena(clave: String): String? = (this[clave] as? JsonPrimitive)?.takeIf { it.isString }?.content

        val codificada = URLEncoder.encode(url, "UTF-8")

        videoYoutube(url)?.let { yt ->
            val j = oembed("https://www.youtube.com/oembed?format=json&url=$codificada")
            val imagen = listOf("maxresdefault", "hqdefault").firstNotNullOfOrNull {
                imagenDe("https://img.youtube.com/vi/$yt/$it.jpg")
            }
            return VistaPrevia(
                titulo = j?.cadena("title"), descripcion = j?.cadena("author_name"), sitio = "YouTube",
                imagen = imagen, proveedor = "youtube", videoID = yt,
            )
        }

        videoVimeo(url)?.let { vm ->
            val j = oembed("https://vimeo.com/api/oembed.json?url=$codificada")
            return VistaPrevia(
                titulo = j?.cadena("title"), descripcion = j?.cadena("author_name"), sitio = "Vimeo",
                imagen = imagenDe(j?.cadena("thumbnail_url")), proveedor = "vimeo", videoID = vm,
            )
        }

        val crudo = descargar(url, 1_500_000) ?: return null
        val info = analizarHtml(String(crudo, Charsets.UTF_8), url)
        if (info.titulo == null && info.descripcion == null) return null
        return VistaPrevia(info.titulo, info.descripcion, info.sitio, imagenDe(info.imagenUrl))
    }
}
