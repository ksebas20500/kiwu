package app.kiwu.android

import app.kiwu.android.domain.AlmacenSync
import app.kiwu.android.domain.Campo
import app.kiwu.android.domain.CuotaAgotada
import app.kiwu.android.domain.FilaSync
import app.kiwu.android.domain.FusionSync
import app.kiwu.android.domain.LoteSync
import app.kiwu.android.domain.Marca
import app.kiwu.android.domain.MotorSync
import app.kiwu.android.domain.Nube
import app.kiwu.android.domain.RelojHibrido
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** El servidor de mentira: fusiona como Firestore y numera cada cambio para el cursor. */
private class NubeFalsa : Nube {
    val filas = linkedMapOf<String, Pair<Int, FilaSync>>()
    private var seq = 0
    var subidasRecibidas = 0
    var cuotaAgotada = false

    override suspend fun subir(filas: List<FilaSync>) {
        if (cuotaAgotada) throw CuotaAgotada()
        for (f in filas) {
            subidasRecibidas++
            val clave = "${f.tabla}/${f.id}"
            val previa = this.filas[clave]?.second
            val fusion = if (previa == null) f else f.copy(campos = FusionSync.fusionar(previa.campos, f.campos))
            this.filas[clave] = ++seq to fusion
        }
    }

    override suspend fun bajar(desde: String?, limite: Int): LoteSync {
        if (cuotaAgotada) throw CuotaAgotada()
        val minimo = desde?.toInt() ?: 0
        val lote = filas.values.filter { it.first > minimo }.sortedBy { it.first }.take(limite)
        return LoteSync(lote.map { it.second }, lote.lastOrNull()?.first?.toString())
    }
}

private class AlmacenFalso : AlmacenSync {
    val datos = linkedMapOf<String, FilaSync>()
    private val sucias = linkedSetOf<String>()
    override var cursor: String? = null

    fun editar(tabla: String, id: String, campo: String, valor: String, marca: Marca) {
        val clave = "$tabla/$id"
        val previa = datos[clave]?.campos ?: emptyMap()
        datos[clave] = FilaSync(tabla, id, FusionSync.fusionar(previa, mapOf(campo to Campo(JsonPrimitive(valor), marca))))
        sucias += clave
    }

    fun valor(tabla: String, id: String, campo: String) = (datos["$tabla/$id"]?.campos?.get(campo)?.valor as? JsonPrimitive)?.content

    override suspend fun pendientes(limite: Int) = sucias.take(limite).mapNotNull { datos[it] }

    override suspend fun marcarSubidas(enviadas: List<FilaSync>) {
        for (f in enviadas) if (datos["${f.tabla}/${f.id}"] == f) sucias -= "${f.tabla}/${f.id}"
    }

    override suspend fun aplicar(filas: List<FilaSync>) {
        for (f in filas) {
            val clave = "${f.tabla}/${f.id}"
            datos[clave] = f.copy(campos = FusionSync.fusionar(datos[clave]?.campos ?: emptyMap(), f.campos))
        }
    }
}

class MotorSyncTest {
    private fun motor(nube: Nube, almacen: AlmacenSync, dispositivo: String, lote: Int = 200) =
        MotorSync(nube, almacen, RelojHibrido(dispositivo), ahora = { 1_000 }, lote = lote)

    @Test
    fun dosDispositivosConvergenYCadaUnoConservaSuCampo() = runBlocking {
        val nube = NubeFalsa()
        val movil = AlmacenFalso()
        val pc = AlmacenFalso()
        movil.editar("deber", "T1", "titulo", "Mate", Marca(10, 0, "movil"))
        pc.editar("deber", "T1", "fecha", "martes", Marca(11, 0, "pc"))
        motor(nube, movil, "movil").sincronizar()
        motor(nube, pc, "pc").sincronizar()
        motor(nube, movil, "movil").sincronizar()
        for (a in listOf(movil, pc)) {
            assertEquals("Mate", a.valor("deber", "T1", "titulo"))
            assertEquals("martes", a.valor("deber", "T1", "fecha"))
        }
    }

    @Test
    fun elBorradoMasRecienteSeAplicaEnElOtroDispositivo() = runBlocking {
        val nube = NubeFalsa()
        val movil = AlmacenFalso()
        val pc = AlmacenFalso()
        movil.editar("deber", "T1", "eliminada", "false", Marca(1, 0, "movil"))
        motor(nube, movil, "movil").sincronizar()
        motor(nube, pc, "pc").sincronizar()
        pc.editar("deber", "T1", "eliminada", "true", Marca(5, 0, "pc"))
        motor(nube, pc, "pc").sincronizar()
        motor(nube, movil, "movil").sincronizar()
        assertEquals("true", movil.valor("deber", "T1", "eliminada"))
    }

    @Test
    fun sincronizarDosVecesNoReenviaNiDuplica() = runBlocking {
        val nube = NubeFalsa()
        val movil = AlmacenFalso()
        movil.editar("materia", "M1", "nombre", "Cálculo", Marca(1, 0, "movil"))
        val primera = motor(nube, movil, "movil").sincronizar()
        val segunda = motor(nube, movil, "movil").sincronizar()
        assertEquals(1, primera.subidas)
        assertEquals(0, segunda.subidas)
        assertEquals(1, segunda.bajadas) // el servidor devuelve la propia escritura; aplicarla no cambia nada
        assertEquals(1, nube.subidasRecibidas)
    }

    @Test
    fun trabajaPorLotesYElCursorAvanza() = runBlocking {
        val nube = NubeFalsa()
        val movil = AlmacenFalso()
        for (i in 1..5) movil.editar("deber", "T$i", "titulo", "t$i", Marca(i.toLong(), 0, "movil"))
        val pc = AlmacenFalso()
        motor(nube, movil, "movil", lote = 2).sincronizar()
        val r = motor(nube, pc, "pc", lote = 2).sincronizar()
        assertEquals(5, r.bajadas)
        assertEquals(5, pc.datos.size)
        assertEquals("5", pc.cursor)
    }

    @Test
    fun siEditasMientrasSeSubeNoSePierdeElCambio() = runBlocking {
        val nube = NubeFalsa()
        val movil = AlmacenFalso()
        movil.editar("deber", "T1", "titulo", "A", Marca(1, 0, "movil"))
        val enviada = movil.pendientes(10)
        movil.editar("deber", "T1", "titulo", "B", Marca(2, 0, "movil"))
        nube.subir(enviada)
        movil.marcarSubidas(enviada)
        assertEquals(1, movil.pendientes(10).size)
    }

    @Test
    fun sinCuotaSeDetieneSinFallarYSigueTodoLocal() = runBlocking {
        val nube = NubeFalsa().apply { cuotaAgotada = true }
        val movil = AlmacenFalso()
        movil.editar("deber", "T1", "titulo", "A", Marca(1, 0, "movil"))
        val r = motor(nube, movil, "movil").sincronizar()
        assertTrue(r.limiteDiario)
        assertEquals(1, movil.pendientes(10).size)
        nube.cuotaAgotada = false
        assertFalse(motor(nube, movil, "movil").sincronizar().limiteDiario)
        assertEquals(0, movil.pendientes(10).size)
    }
}
