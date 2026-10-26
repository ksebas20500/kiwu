package app.kiwu.android

import app.kiwu.android.domain.Bloque
import app.kiwu.android.domain.Campo
import app.kiwu.android.domain.Documento
import app.kiwu.android.domain.FusionSync
import app.kiwu.android.domain.Marca
import app.kiwu.android.domain.RelojHibrido
import app.kiwu.android.domain.TiposBloque
import app.kiwu.android.domain.paraSincronizar
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SincronizacionTest {
    @Test
    fun lasMarcasSeOrdenanPorHoraContadorYDispositivo() {
        assertTrue(Marca(2, 0, "a") > Marca(1, 9, "z"))
        assertTrue(Marca(1, 1, "a") > Marca(1, 0, "z"))
        assertTrue(Marca(1, 1, "b") > Marca(1, 1, "a"))
    }

    @Test
    fun laMarcaSeEscribeYSeLeeConUnUuidDeDispositivo() {
        val m = Marca(1_700_000_000_000, 3, "1F2E-33AA-9")
        assertEquals(m, Marca.deTexto(m.toString()))
        assertNull(Marca.deTexto("basura"))
        assertNull(Marca.deTexto("1-2-"))
    }

    @Test
    fun elRelojNuncaRetrocedeNiRepiteMarca() {
        val reloj = RelojHibrido("a")
        val primera = reloj.nueva(1000)
        val mismoMs = reloj.nueva(1000)
        val relojAtrasado = reloj.nueva(500)
        assertTrue(mismoMs > primera)
        assertTrue(relojAtrasado > mismoMs)
        assertTrue(reloj.nueva(2000) > relojAtrasado)
    }

    @Test
    fun unaMarcaRecibidaAdelantaElRelojPeroUnaDeMasDeUnDiaSeIgnora() {
        val reloj = RelojHibrido("a")
        reloj.ver(Marca(5000, 4, "b"), ahora = 4000)
        assertTrue(reloj.nueva(4000) > Marca(5000, 4, "b"))
        val otro = RelojHibrido("a")
        otro.ver(Marca(4000 + 2L * RelojHibrido.MAX_ADELANTO_MS, 0, "b"), ahora = 4000)
        assertEquals(4000L, otro.nueva(4000).ms)
    }

    private fun c(valor: String, ms: Long, d: String = "a") = Campo(JsonPrimitive(valor), Marca(ms, 0, d))

    @Test
    fun camposDistintosSeConservanLosDos() {
        val movil = mapOf("titulo" to c("Mate", 10), "fecha" to c("lunes", 1))
        val pc = mapOf("titulo" to c("Viejo", 1), "fecha" to c("martes", 10, "b"))
        val r = FusionSync.fusionar(movil, pc)
        assertEquals(JsonPrimitive("Mate"), r["titulo"]!!.valor)
        assertEquals(JsonPrimitive("martes"), r["fecha"]!!.valor)
    }

    @Test
    fun elBorradoMasRecienteGanaALaEdicionYViceversa() {
        val borrado = mapOf("eliminada" to Campo(JsonPrimitive(true), Marca(10, 0, "a")))
        val edicionAntes = mapOf("eliminada" to Campo(JsonPrimitive(false), Marca(5, 0, "b")))
        val edicionDespues = mapOf("eliminada" to Campo(JsonPrimitive(false), Marca(20, 0, "b")))
        assertEquals(JsonPrimitive(true), FusionSync.fusionar(borrado, edicionAntes)["eliminada"]!!.valor)
        assertEquals(JsonPrimitive(false), FusionSync.fusionar(borrado, edicionDespues)["eliminada"]!!.valor)
    }

    @Test
    fun fusionarEsConmutativaEIdempotente() {
        val a = mapOf("x" to c("1", 3), "y" to c("1", 1))
        val b = mapOf("x" to c("2", 2), "y" to c("2", 9, "b"), "z" to c("2", 1, "b"))
        assertEquals(FusionSync.fusionar(a, b), FusionSync.fusionar(b, a))
        assertEquals(FusionSync.fusionar(a, b), FusionSync.fusionar(FusionSync.fusionar(a, b), b))
    }

    @Test
    fun laNotaSincronizadaNoLlevaImagenesNiRutas() {
        val nota = Documento(
            listOf(
                Bloque(text = "hola"),
                Bloque(kind = TiposBloque.IMAGEN, imageData = "AAAA", path = "content://x"),
                Bloque(kind = TiposBloque.ARCHIVO, fileName = "apuntes.pdf", path = "content://y", bookmark = "bk"),
                Bloque(kind = TiposBloque.ENLACE, url = "https://a.b", preview = buildJsonObject { put("titulo", "T"); put("imagen", "AAAA") }),
            ),
        )
        val r = nota.paraSincronizar().bloques
        assertEquals(listOf(TiposBloque.PARRAFO, TiposBloque.ARCHIVO, TiposBloque.ENLACE), r.map { it.kind })
        assertEquals("apuntes.pdf", r[1].fileName)
        assertNull(r[1].path)
        assertNull(r[1].bookmark)
        assertEquals(JsonObject(mapOf("titulo" to JsonPrimitive("T"))), r[2].preview)
    }

    @Test
    fun siSoloHabiaImagenesQuedaUnBloqueVacio() {
        val r = Documento(listOf(Bloque(kind = TiposBloque.IMAGEN, imageData = "AAAA"))).paraSincronizar()
        assertEquals(1, r.bloques.size)
        assertEquals(TiposBloque.PARRAFO, r.bloques[0].kind)
    }
}
