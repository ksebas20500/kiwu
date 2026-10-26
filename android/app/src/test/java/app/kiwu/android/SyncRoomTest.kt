package app.kiwu.android

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.kiwu.android.data.AlmacenRoom
import app.kiwu.android.data.KiwuDb
import app.kiwu.android.data.Repositorio
import app.kiwu.android.data.SyncSql
import app.kiwu.android.domain.Bloque
import app.kiwu.android.domain.Documento
import app.kiwu.android.domain.FilaSync
import app.kiwu.android.domain.LoteSync
import app.kiwu.android.domain.MotorSync
import app.kiwu.android.domain.Nube
import app.kiwu.android.domain.RelojHibrido
import app.kiwu.android.domain.TiposBloque
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Un servidor en memoria con la misma regla que Firestore: cada documento se reemplaza entero y lleva un número de orden. */
private class NubeMemoria : Nube {
    private val docs = linkedMapOf<String, Pair<Int, FilaSync>>()
    private var seq = 0

    override suspend fun subir(filas: List<FilaSync>) {
        for (f in filas) docs["${f.tabla}/${f.id}"] = ++seq to f
    }

    override suspend fun bajar(desde: String?, limite: Int): LoteSync {
        val minimo = desde?.toInt() ?: 0
        val lote = docs.values.filter { it.first > minimo }.sortedBy { it.first }.take(limite)
        return LoteSync(lote.map { it.second }, lote.lastOrNull()?.first?.toString())
    }
}

/** Dos dispositivos de verdad (dos bases Room) sincronizados a través de un servidor en memoria. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(application = android.app.Application::class)
class SyncRoomTest {
    private class Dispositivo(contexto: Context, val nombre: String, reloj: () -> Long) {
        val db: KiwuDb = Room.inMemoryDatabaseBuilder(contexto, KiwuDb::class.java).allowMainThreadQueries().build()
        val repo = Repositorio(db.dao(), reloj = reloj)
        val almacen: AlmacenRoom

        init {
            SyncSql.instalar(db.openHelper.writableDatabase)
            almacen = AlmacenRoom(db)
        }

        fun sincronizar(nube: Nube) = runBlocking { MotorSync(nube, almacen, RelojHibrido(nombre)).sincronizar() }

        fun valor(sql: String): String? = db.openHelper.readableDatabase.query(sql).use { if (it.moveToFirst()) it.getString(0) else null }
    }

    private var hora = 1_800_000_000_000L
    private val nube = NubeMemoria()
    private lateinit var movil: Dispositivo
    private lateinit var tablet: Dispositivo

    @Before
    fun abrir() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        movil = Dispositivo(contexto, "movil") { hora++ }
        tablet = Dispositivo(contexto, "tablet") { hora++ }
    }

    @After
    fun cerrar() {
        movil.db.close()
        tablet.db.close()
    }

    @Test
    fun unaTareaCreadaEnUnDispositivoLlegaAlOtro() = runBlocking {
        val lista = movil.repo.crearMateria("Cálculo")
        val tarea = movil.repo.crearDeber("Taller 3", lista.id)
        assertEquals(2, movil.almacen.pendientesTotal())

        movil.sincronizar(nube)
        assertEquals(0, movil.almacen.pendientesTotal())
        tablet.sincronizar(nube)

        val deberes = tablet.repo.deberes.first()
        assertEquals(listOf("Taller 3"), deberes.map { it.titulo })
        assertEquals(tarea.id, deberes[0].id)
        assertEquals(listOf("Cálculo"), tablet.repo.materias.first().map { it.nombre })
        // Lo que se recibe no vuelve a la cola de salida.
        assertEquals(0, tablet.almacen.pendientesTotal())
    }

    @Test
    fun completarYBorrarSePropaganDeVueltaYDeIda() = runBlocking {
        val lista = movil.repo.crearMateria("Física")
        val a = movil.repo.crearDeber("A", lista.id)
        val b = movil.repo.crearDeber("B", lista.id)
        movil.sincronizar(nube)
        tablet.sincronizar(nube)

        tablet.repo.marcar(a.id, true)
        tablet.repo.eliminarDeber(b.id)
        tablet.sincronizar(nube)
        movil.sincronizar(nube)

        val deberes = movil.repo.deberes.first()
        assertEquals(listOf("A"), deberes.map { it.titulo })
        assertTrue(deberes[0].completado)
    }

    @Test
    fun laEdicionMasRecienteGanaAunqueLlegueDespues() = runBlocking {
        val lista = movil.repo.crearMateria("Química")
        movil.sincronizar(nube)
        tablet.sincronizar(nube)

        tablet.repo.renombrarMateria(lista.id, "Química orgánica") // edición más reciente
        movil.repo.renombrarMateria(lista.id, "Quimica") // la hizo antes... pero con el reloj de pruebas es posterior
        val ganadora = movil.valor("SELECT nombre FROM materia WHERE id = '${lista.id}'")
        movil.sincronizar(nube)
        tablet.sincronizar(nube)
        movil.sincronizar(nube)
        assertEquals(ganadora, movil.valor("SELECT nombre FROM materia WHERE id = '${lista.id}'"))
        assertEquals(ganadora, tablet.valor("SELECT nombre FROM materia WHERE id = '${lista.id}'"))
    }

    @Test
    fun lasColumnasDelDispositivoNoViajan() = runBlocking {
        val lista = movil.repo.crearMateria("Inglés")
        val tarea = movil.repo.crearDeber("Leer", lista.id)
        movil.db.openHelper.writableDatabase.execSQL("UPDATE materia SET carpeta = 'content://carpeta' WHERE id = '${lista.id}'")
        movil.repo.marcarAvisado(tarea.id, 1234L)
        // Marcar un aviso no es una edición: no vuelve a la cola.
        movil.sincronizar(nube)
        assertEquals(0, movil.almacen.pendientesTotal())
        tablet.sincronizar(nube)
        assertNull(tablet.valor("SELECT carpeta FROM materia WHERE id = '${lista.id}'"))
        assertNull(tablet.valor("SELECT recordado FROM deber WHERE id = '${tarea.id}'"))
        assertEquals("content://carpeta", movil.valor("SELECT carpeta FROM materia WHERE id = '${lista.id}'"))
    }

    @Test
    fun unaNotaLlegaSinImagenesYElDispositivoConservaLasSuyas() = runBlocking {
        val cuaderno = movil.repo.crearCuaderno("Clases")
        val pagina = movil.repo.crearPagina(cuaderno.id)
        val texto = Bloque(text = "hola")
        val imagen = Bloque(kind = TiposBloque.IMAGEN, imageData = "AAAA", path = "content://foto")
        movil.repo.guardarContenidoPagina(pagina.id, "Apuntes", "", Documento(listOf(texto, imagen)))
        movil.sincronizar(nube)
        tablet.sincronizar(nube)

        val enLaTablet = Documento.deJson(tablet.valor("SELECT contenido FROM pagina WHERE id = '${pagina.id}'"))
        assertEquals(listOf("hola"), enLaTablet.bloques.map { it.text })
        assertFalse(enLaTablet.bloques.any { it.kind == TiposBloque.IMAGEN })

        // La tablet edita; el móvil recibe el texto nuevo y no pierde su foto.
        tablet.repo.guardarContenidoPagina(pagina.id, "Apuntes", "", Documento(listOf(texto.copy(text = "hola mundo"))))
        tablet.sincronizar(nube)
        movil.sincronizar(nube)
        val enElMovil = Documento.deJson(movil.valor("SELECT contenido FROM pagina WHERE id = '${pagina.id}'"))
        assertEquals("hola mundo", enElMovil.bloques.first().text)
        assertNotNull(enElMovil.bloques.firstOrNull { it.kind == TiposBloque.IMAGEN }?.imageData)
    }

    @Test
    fun alIniciarSesionPorPrimeraVezSeSubeTodoLoQueYaHabia() = runBlocking {
        val lista = movil.repo.crearMateria("Historia")
        movil.repo.crearDeber("Ensayo", lista.id)
        movil.sincronizar(nube) // ya sincronizado antes de que existiera la cola de otro usuario
        movil.almacen.reiniciar()
        assertTrue(movil.almacen.pendientesTotal() >= 2)
        assertNull(movil.almacen.cursor)
    }
}
