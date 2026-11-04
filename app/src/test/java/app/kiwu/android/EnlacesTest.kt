package app.kiwu.android

import app.kiwu.android.domain.Enlaces
import app.kiwu.android.domain.Lenguajes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnlacesTest {
    @Test
    fun urlDesdeTexto() {
        assertEquals("https://example.com/a", Enlaces.urlDesdeTexto("example.com/a"))
        assertNull(Enlaces.urlDesdeTexto("hola mundo"))
        assertNull(Enlaces.urlDesdeTexto("ftp://example.com"))
        assertNull(Enlaces.urlDesdeTexto("localhost"))
    }

    @Test
    fun enlaceSuelto() {
        assertTrue(Enlaces.pareceEnlaceSuelto("https://example.com"))
        assertTrue(Enlaces.pareceEnlaceSuelto("www.example.com"))
        assertFalse(Enlaces.pareceEnlaceSuelto("hola.mundo"))
    }

    @Test
    fun videos() {
        for (url in listOf(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ", "https://youtu.be/dQw4w9WgXcQ?t=3",
            "https://www.youtube.com/shorts/dQw4w9WgXcQ", "https://m.youtube.com/embed/dQw4w9WgXcQ",
        )) {
            assertEquals(url, "dQw4w9WgXcQ", Enlaces.videoYoutube(url))
        }
        assertNull(Enlaces.videoYoutube("https://example.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("76979871", Enlaces.videoVimeo("https://vimeo.com/76979871"))
    }

    @Test
    fun analizarHtml() {
        val html = """<html><head><title>Título viejo</title><meta property="og:title" content="Título OG &amp; más">""" +
            """<meta name="description" content="Descripción"><meta property="og:image" content="/img/a.png"></head></html>"""
        val info = Enlaces.analizarHtml(html, "https://www.sitio.com/pagina")
        assertEquals("Título OG & más", info.titulo)
        assertEquals("Descripción", info.descripcion)
        assertEquals("sitio.com", info.sitio)
        assertEquals("https://www.sitio.com/img/a.png", info.imagenUrl)
    }

    @Test
    fun obtenerWebConDescargaSimulada() {
        val paginas = mapOf(
            "https://sitio.com/" to """<title>Hola</title><meta property="og:image" content="https://sitio.com/i.png">""".toByteArray(),
            "https://sitio.com/i.png" to "PNGDATA".toByteArray(),
        )
        val vp = Enlaces.obtener("https://sitio.com/", { u, _ -> paginas[u] }, { it.copyOf(3) })!!
        assertEquals("Hola", vp.titulo)
        assertEquals("UE5H", vp.imagen)   // base64 de «PNG»
    }

    @Test
    fun obtenerYoutubeConDescargaSimulada() {
        val falsa = { url: String, _: Int ->
            when {
                "oembed" in url -> """{"title":"Un vídeo","author_name":"Canal"}""".toByteArray()
                "maxresdefault" in url -> null            // no existe: se usa hqdefault
                else -> "IMG".toByteArray()
            }
        }
        val vp = Enlaces.obtener("https://youtu.be/dQw4w9WgXcQ", falsa)!!
        assertEquals(Triple("youtube", "dQw4w9WgXcQ", "Un vídeo"), Triple(vp.proveedor, vp.videoID, vp.titulo))
        assertNotNull(vp.imagen)
    }

    @Test
    fun paginaSinDatosDevuelveNull() {
        assertNull(Enlaces.obtener("https://sitio.com/", { _, _ -> "<html></html>".toByteArray() }))
        assertNull(Enlaces.obtener("https://sitio.com/", { _, _ -> null }))
    }

    @Test
    fun deteccionDeLenguaje() {
        val casos = mapOf(
            "import SwiftUI\nstruct A: View {}" to "swift", "def f(x):\n    return x" to "python", """{"a": 1}""" to "json",
            "SELECT * FROM t WHERE a=1" to "sql", "const a = () => 1" to "javascript", "#include <stdio.h>" to "c",
            "<div>hola</div>" to "html", "texto cualquiera" to "plano", "" to "plano",
            "sudo pacman -Syu" to "bash", "fn main() { let mut x = 1; }" to "rust",
        )
        for ((codigo, esperado) in casos) assertEquals(codigo, esperado, Lenguajes.detectar(codigo))
    }
}
