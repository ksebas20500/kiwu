package app.kiwu.android

import app.kiwu.android.domain.Bloque
import app.kiwu.android.domain.Documento
import app.kiwu.android.domain.TextRun
import app.kiwu.android.domain.fusionarRuns
import app.kiwu.android.domain.runsONada
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DocumentoTest {
    private val notaMacos =
        """{"blocks":[{"id":"6F1E6C0A-1111-4222-8333-444455556666","kind":"paragraph","text":"Hola mundo",""" +
            """"indent":0,"isChecked":false,"runs":[{"text":"Hola ","bold":false,"italic":false,"underline":false,""" +
            """"strike":false,"code":false},{"text":"mundo","bold":true,"italic":false,"underline":false,"strike":false,""" +
            """"code":false,"color":"rojo"}]}]}"""

    @Test
    fun notaAntiguaSinCamposNuevos() {
        val viejo = """{"blocks":[{"id":"6F1E6C0A-1111-4222-8333-444455556666","kind":"paragraph","text":"hola","isChecked":false}]}"""
        val doc = Documento.deJson(viejo)
        assertEquals("hola", doc.bloques[0].text)
        assertEquals(0, doc.bloques[0].indent)
    }

    @Test
    fun idaYVueltaConservaFormatoYCamposDesconocidos() {
        val original = Json.parseToJsonElement(notaMacos).jsonObject
        val bloque = original["blocks"]!!.jsonArray[0].jsonObject
        val conExtra = JsonObject(
            mapOf("blocks" to JsonArray(listOf(JsonObject(bloque + ("campoFuturo" to JsonObject(mapOf("x" to JsonPrimitive(1)))))))),
        )
        val vuelta = Json.parseToJsonElement(Documento.deJson(conExtra.toString()).aJson()).jsonObject
        val b = vuelta["blocks"]!!.jsonArray[0].jsonObject
        assertEquals(1, b["campoFuturo"]!!.jsonObject["x"]!!.jsonPrimitive.int)
        val runs = b["runs"]!!.jsonArray
        assertTrue(runs[1].jsonObject["bold"]!!.jsonPrimitive.boolean)
        assertEquals("rojo", runs[1].jsonObject["color"]!!.jsonPrimitive.content)
    }

    @Test
    fun jsonInvalidoDaDocumentoVacio() {
        for (malo in listOf("", "no es json", "[1,2]", "null", "{}", """{"blocks":5}""")) {
            assertEquals(malo, 1, Documento.deJson(malo).bloques.size)
        }
        assertEquals(1, Documento.deJson(null).bloques.size)
    }

    @Test
    fun runsSeFusionanYSinFormatoEsNull() {
        assertNull(runsONada(listOf(TextRun("a"), TextRun("b"))))
        val runs = runsONada(listOf(TextRun("a", bold = true), TextRun("b", bold = true), TextRun("c")))!!
        assertEquals(listOf("ab" to true, "c" to false), runs.map { it.text to it.bold })
        assertEquals(1, fusionarRuns(listOf(TextRun(""), TextRun("x"))).size)
    }

    @Test
    fun todasLasClavesBooleanasDeTextRunEstanPresentes() {
        // Swift exige estas claves al decodificar TextRun
        val d = TextRun("x").aJson()
        for (clave in listOf("text", "bold", "italic", "underline", "strike", "code")) {
            assertTrue(clave, clave in d)
        }
    }

    @Test
    fun sangriaTipoDesconocidoYTextoPlano() {
        val b = Bloque.deJson(Json.parseToJsonElement("""{"kind":"raro","indent":99,"text":"t"}""").jsonObject)
        assertEquals("paragraph", b.kind)
        assertEquals(4, b.indent)
        val doc = Documento(
            listOf(
                Bloque(kind = "file", fileName = "guia.pdf"),
                Bloque(kind = "embed", url = "https://a.com"),
                Bloque(kind = "paragraph", text = "x"),
            ),
        )
        assertEquals("guia.pdf\nhttps://a.com\nx", doc.textoPlano())
    }

    // --- contrato compartido con macOS y Linux ----------------------------------------------------

    private fun carpetaContrato(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val candidata = File(dir, "docs/contrato/notas")
            if (candidata.isDirectory) return candidata
            dir = dir.parentFile
        }
        error("No se encontró docs/contrato/notas")
    }

    private fun resumen(doc: Documento) = Triple(doc.textoPlano(), doc.bloques.map { it.kind }, doc.bloques.map { it.indent })

    @Test
    fun cadaFixtureSeLeeComoSeEspera() {
        val archivos = carpetaContrato().listFiles { f -> f.name.endsWith(".json") && !f.name.endsWith(".esperado.json") }!!
            .sortedBy { it.name }
        assertTrue("No hay fixtures", archivos.isNotEmpty())
        for (archivo in archivos) {
            val esperado = Json.parseToJsonElement(File(archivo.path.removeSuffix(".json") + ".esperado.json").readText()).jsonObject
            val doc = Documento.deJson(archivo.readText())
            assertEquals(archivo.name, esperado["texto_plano"]!!.jsonPrimitive.content, doc.textoPlano())
            assertEquals(archivo.name, esperado["tipos"]!!.jsonArray.map { it.jsonPrimitive.content }, doc.bloques.map { it.kind })
            assertEquals(archivo.name, esperado["sangrias"]!!.jsonArray.map { it.jsonPrimitive.int }, doc.bloques.map { it.indent })
            // escribir y volver a leer no cambia nada
            assertEquals(archivo.name, resumen(doc), resumen(Documento.deJson(doc.aJson())))
        }
    }
}
