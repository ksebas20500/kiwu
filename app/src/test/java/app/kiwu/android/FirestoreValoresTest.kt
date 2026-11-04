package app.kiwu.android

import app.kiwu.android.data.ValoresFirestore
import app.kiwu.android.domain.Campo
import app.kiwu.android.domain.FilaSync
import app.kiwu.android.domain.Marca
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirestoreValoresTest {
    private val marca = Marca(1_800_000_000_000, 0, "tablet")
    private val fila = FilaSync(
        "deber", "T-1",
        mapOf(
            "titulo" to Campo(JsonPrimitive("Taller"), marca),
            "completado" to Campo(JsonPrimitive(1L), marca),
            "fecha_entrega" to Campo(JsonNull, marca),
            "actualizada" to Campo(JsonPrimitive(1_800_000_000_000), marca),
        ),
    )

    @Test
    fun cadaTipoViajaConSuEtiquetaDeFirestore() {
        assertEquals("""{"stringValue":"a"}""", ValoresFirestore.aValor(JsonPrimitive("a")).toString())
        assertEquals("""{"integerValue":"7"}""", ValoresFirestore.aValor(JsonPrimitive(7L)).toString())
        assertEquals("""{"nullValue":null}""", ValoresFirestore.aValor(JsonNull).toString())
        assertEquals(JsonPrimitive(7L), ValoresFirestore.deValor(ValoresFirestore.aValor(JsonPrimitive(7L)).jsonObject))
        assertEquals(JsonPrimitive("a"), ValoresFirestore.deValor(ValoresFirestore.aValor(JsonPrimitive("a")).jsonObject))
        assertEquals(JsonNull, ValoresFirestore.deValor(ValoresFirestore.aValor(JsonNull).jsonObject))
    }

    @Test
    fun unaFilaEscritaSeLeeIgualYConSuCursor() {
        val w = ValoresFirestore.escritura("projects/p/databases/(default)/documents", "UID", fila)
        val update = w["update"]!!.jsonObject
        assertEquals("projects/p/databases/(default)/documents/usuarios/UID/filas/deber_T-1", update["name"]!!.jsonPrimitive.content)
        assertEquals("servidor", w["updateTransforms"]!!.jsonArray[0].jsonObject["fieldPath"]!!.jsonPrimitive.content)

        // Lo que devolvería el servidor: el mismo documento con su hora.
        val devuelto = buildJsonObject {
            put("name", update["name"]!!.jsonPrimitive.content)
            put(
                "fields",
                JsonObject(update["fields"]!!.jsonObject + ("servidor" to buildJsonObject { put("timestampValue", "2026-10-25T20:00:00.123456Z") })),
            )
        }
        val (leida, cursor) = ValoresFirestore.fila(devuelto)!!
        assertEquals("deber", leida.tabla)
        assertEquals("T-1", leida.id)
        assertEquals(fila.campos.mapValues { it.value.valor }, leida.campos.mapValues { it.value.valor })
        assertEquals(1_800_000_000_000, leida.campos["actualizada"]!!.marca.ms)
        assertEquals("2026-10-25T20:00:00.123456Z|projects/p/databases/(default)/documents/usuarios/UID/filas/deber_T-1", cursor)
    }

    @Test
    fun laConsultaPideLoPosteriorAlCursorEnOrdenEstable() {
        val primera = ValoresFirestore.consulta(null, 200)["structuredQuery"]!!.jsonObject
        assertNull(primera["startAt"])
        assertEquals(200, primera["limit"]!!.jsonPrimitive.content.toInt())

        val siguiente = ValoresFirestore.consulta("2026-10-25T20:00:00Z|projects/p/x/filas/a", 50)["structuredQuery"]!!.jsonObject
        val inicio = siguiente["startAt"]!!.jsonObject
        assertEquals("false", inicio["before"]!!.jsonPrimitive.content) // «después de», no «desde»: no repite el último documento
        assertEquals("2026-10-25T20:00:00Z", inicio["values"]!!.jsonArray[0].jsonObject["timestampValue"]!!.jsonPrimitive.content)
        assertEquals("projects/p/x/filas/a", inicio["values"]!!.jsonArray[1].jsonObject["referenceValue"]!!.jsonPrimitive.content)
    }

    @Test
    fun unDocumentoQueNoEsDeKiwuSeIgnora() {
        assertNull(ValoresFirestore.fila(buildJsonObject { putJsonObject("fields") { } }))
    }
}
