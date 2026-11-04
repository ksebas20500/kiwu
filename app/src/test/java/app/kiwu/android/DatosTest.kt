package app.kiwu.android

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.kiwu.android.data.Copia
import app.kiwu.android.data.ErrorCopia
import app.kiwu.android.data.KiwuDb
import app.kiwu.android.data.Repositorio
import app.kiwu.android.domain.Bloque
import app.kiwu.android.domain.Columna
import app.kiwu.android.domain.Documento
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Room contra una base en memoria (Robolectric): borrado lógico, repositorio y copia de seguridad. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(application = android.app.Application::class)
class DatosTest {
    private lateinit var db: KiwuDb
    private lateinit var repo: Repositorio
    private var reloj = 1_800_000_000_000L

    @Before
    fun abrir() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(contexto, KiwuDb::class.java).allowMainThreadQueries().build()
        repo = Repositorio(db.dao(), reloj = { reloj++ })
    }

    @After
    fun cerrar() = db.close()

    private fun eliminadaDe(tabla: String, id: String): Long? =
        db.openHelper.readableDatabase.query("SELECT eliminada FROM $tabla WHERE id = ?", arrayOf(id)).use {
            assertTrue("La fila de $tabla debe seguir existiendo", it.moveToFirst())
            if (it.isNull(0)) null else it.getLong(0)
        }

    @Test
    fun crearYListar() = runBlocking {
        val m = repo.crearMateria("Universidad")
        val d = repo.crearDeber("Estudiar", m.id)
        assertEquals(listOf("Estudiar"), repo.deberes.first().map { it.titulo })
        assertEquals(Columna.POR_HACER, repo.reglas.columnaDe(d))
    }

    @Test
    fun ordenDeListasCreceAlFinal() = runBlocking {
        repo.crearMateria("A")
        repo.crearMateria("B")
        val c = repo.crearMateria("C")
        assertEquals(2, c.orden)
        repo.eliminarMateria(c.id)
        assertEquals(2, repo.crearMateria("D").orden)   // lo eliminado no cuenta
    }

    @Test
    fun borradoLogicoDejaLaFilaMarcada() = runBlocking {
        val m = repo.crearMateria("U")
        val d = repo.crearDeber("A", m.id)
        repo.eliminarDeber(d.id)
        assertNull(repo.deber(d.id))
        assertNotNull(eliminadaDe("deber", d.id))
    }

    @Test
    fun borrarMateriaMarcaTambienSusTareas() = runBlocking {
        val m = repo.crearMateria("U")
        val d = repo.crearDeber("A", m.id)
        repo.eliminarMateria(m.id)
        assertTrue(repo.materias.first().isEmpty())
        assertTrue(repo.deberes.first().isEmpty())
        assertNotNull(eliminadaDe("deber", d.id))
    }

    @Test
    fun borrarCuadernoMarcaTambienSusPaginas() = runBlocking {
        val c = repo.crearCuaderno("N")
        val p = repo.crearPagina(c.id)
        repo.eliminarCuaderno(c.id)
        assertTrue(repo.cuadernos.first().isEmpty())
        assertNull(repo.pagina(p.id))
        assertNotNull(eliminadaDe("pagina", p.id))
    }

    @Test
    fun loEliminadoNoSePuedeEditarNiResucitar() = runBlocking {
        val m = repo.crearMateria("U")
        val d = repo.crearDeber("A", m.id)
        repo.eliminarDeber(d.id)
        assertNull(repo.actualizarDeber(d.copy(titulo = "resucitada")))
        assertNull(repo.marcar(d.id, true))
        assertTrue(repo.deberes.first().isEmpty())
    }

    @Test
    fun accionesDelTablero() = runBlocking {
        val m = repo.crearMateria("U")
        val d = repo.crearDeber("A", m.id)
        assertEquals(Columna.EN_CURSO, repo.reglas.columnaDe(repo.mover(d.id, Columna.EN_CURSO)!!))
        assertEquals(Columna.HECHO, repo.reglas.columnaDe(repo.marcar(d.id, true)!!))
        assertEquals(Columna.EN_CURSO, repo.reglas.columnaDe(repo.marcar(d.id, false)!!))
    }

    @Test
    fun guardarPaginaCalculaElTextoPlano() = runBlocking {
        val c = repo.crearCuaderno("N")
        val p = repo.crearPagina(c.id)
        repo.guardarContenidoPagina(p.id, "Título", "📝", Documento(listOf(Bloque(text = "uno"), Bloque(text = "dos"))))
        val guardada = repo.pagina(p.id)!!
        assertEquals("uno\ndos", guardada.textoPlano)
        assertEquals("Título", repo.paginas.first().single().titulo)
        repo.ponerFavorita(p.id, true)
        assertTrue(repo.paginas.first().single().favorita)
    }

    // --- copia de seguridad --------------------------------------------------------------------

    @Test
    fun copiaIdaYVuelta() = runBlocking {
        val m = repo.crearMateria("Uni")
        val d = repo.crearDeber("Quiz", m.id, 1_800_100_000_000L)
        repo.marcar(d.id, true)
        val c = repo.crearCuaderno("Notas")
        val p = repo.crearPagina(c.id)
        repo.guardarContenidoPagina(p.id, "Hola", "", Documento(listOf(Bloque(text = "texto"))))
        val texto = Copia.exportar(db.dao())

        db.clearAllTables()
        val resumen = Copia.importar(db.dao(), texto)
        assertEquals(listOf(1, 1, 1, 1), listOf(resumen.materias, resumen.deberes, resumen.cuadernos, resumen.paginas))
        assertEquals("Quiz", repo.deber(d.id)!!.titulo)
        assertTrue(repo.deber(d.id)!!.completado)
        assertEquals("texto", repo.pagina(p.id)!!.textoPlano)
        // importar de nuevo no duplica nada
        assertEquals(0, Copia.importar(db.dao(), texto).deberes)
    }

    @Test
    fun importarNoResucitaLoEliminado() = runBlocking {
        val m = repo.crearMateria("U")
        val d = repo.crearDeber("A", m.id)
        val texto = Copia.exportar(db.dao())
        repo.eliminarDeber(d.id)
        assertEquals(0, Copia.importar(db.dao(), texto).deberes)
        assertNull(repo.deber(d.id))
    }

    @Test
    fun leeUnaCopiaExportadaPorLinux() = runBlocking {
        val deLinux = """
        {"formato":1,"app":"kiwu","exportado":"2026-10-01T10:00:00Z",
         "materias":[{"id":"M1","nombre":"Álgebra","orden":0,"carpeta":"/home/a","creada":"2026-09-01T10:00:00Z","actualizada":"2026-09-01T10:00:00Z"}],
         "deberes":[{"id":"D1","materia":"M1","titulo":"Taller","completado":false,"estado":1,"fechaEntrega":"2026-10-03T23:00:00Z",
                     "recordatorioHoras":24,"fechaCompletado":null,
                     "nota":{"blocks":[{"id":"B1","kind":"paragraph","text":"hola","indent":0,"isChecked":false}]},
                     "creada":"2026-09-02T10:00:00Z","actualizada":"2026-09-02T10:00:00Z"}],
         "cuadernos":[],"paginas":[]}
        """.trimIndent()
        val r = Copia.importar(db.dao(), deLinux)
        assertEquals(1, r.materias)
        assertEquals(1, r.deberes)
        val d = repo.deber("D1")!!
        assertEquals(Columna.EN_CURSO, repo.reglas.columnaDe(d))
        assertEquals("hola", Documento.deJson(d.nota).textoPlano())
        assertNull(repo.materia("M1")!!.carpeta)   // las carpetas no viajan entre dispositivos
    }

    @Test
    fun rechazaArchivosQueNoSonDeKiwu() = runBlocking {
        for (malo in listOf("no es json", """{"app":"otra"}""", """{"app":"kiwu","formato":99}""")) {
            try {
                Copia.importar(db.dao(), malo)
                error("Debía fallar: $malo")
            } catch (e: ErrorCopia) {
                // esperado
            }
        }
    }
}
