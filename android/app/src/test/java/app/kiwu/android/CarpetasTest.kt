package app.kiwu.android

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.kiwu.android.data.Copia
import app.kiwu.android.data.KiwuDb
import app.kiwu.android.data.Repositorio
import app.kiwu.android.domain.GrupoLista
import app.kiwu.android.domain.Materia
import app.kiwu.android.domain.MenuListas
import app.kiwu.android.domain.NodoMenu
import app.kiwu.android.domain.OrdenMenu
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

/** Carpetas de listas: datos (Room en memoria), copia de seguridad y el árbol del menú. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(application = android.app.Application::class)
class CarpetasTest {
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

    @Test
    fun unaListaSePuedeMeterEnUnaCarpetaYSacarla() = runBlocking {
        val g = repo.crearGrupo("Semestre 3")
        val m = repo.crearMateria("Cálculo")
        assertNull(repo.materia(m.id)!!.grupoId)
        repo.moverMateriaAGrupo(m.id, g.id)
        assertEquals(g.id, repo.materia(m.id)!!.grupoId)
        repo.moverMateriaAGrupo(m.id, null)
        assertNull(repo.materia(m.id)!!.grupoId)
    }

    @Test
    fun moverAUnaCarpetaQueNoExisteDejaLaListaSuelta() = runBlocking {
        val m = repo.crearMateria("Cálculo")
        repo.moverMateriaAGrupo(m.id, "no-existe")
        assertNull(repo.materia(m.id)!!.grupoId)
    }

    @Test
    fun borrarUnaCarpetaConservaSusListasYSusTareas() = runBlocking {
        val g = repo.crearGrupo("Trabajo")
        val m = repo.crearMateria("Informes")
        repo.moverMateriaAGrupo(m.id, g.id)
        repo.crearDeber("Entregar", m.id)
        repo.eliminarGrupoListas(g.id)
        assertTrue(repo.grupos.first().isEmpty())
        assertNull(repo.materia(m.id)!!.grupoId)
        assertEquals(1, repo.deberes.first().size)
    }

    @Test
    fun laCarpetaSePliegaYSeEdita() = runBlocking {
        val g = repo.crearGrupo("Idiomas", "🌍", "#43A047")
        repo.plegarGrupo(g.id, true)
        repo.editarGrupo(g.id, "Idiomas y más", "📚", "no-es-un-color")
        val leida = repo.grupos.first().single()
        assertTrue(leida.plegado)
        assertEquals("Idiomas y más", leida.nombre)
        assertEquals("📚", leida.icono)
        assertNull(leida.color) // un color inválido se ignora
    }

    @Test
    fun subirYBajarCambiaElOrdenSoloEntreHermanas() = runBlocking {
        val g = repo.crearGrupo("A")
        val a = repo.crearMateria("Uno"); val b = repo.crearMateria("Dos"); val c = repo.crearMateria("Tres")
        repo.moverMateriaAGrupo(a.id, g.id); repo.moverMateriaAGrupo(b.id, g.id)
        repo.moverOrdenMateria(b.id, -1)
        val dentro = repo.materias.first().filter { it.grupoId == g.id }.sortedBy { it.orden }.map { it.nombre }
        assertEquals(listOf("Dos", "Uno"), dentro)
        repo.moverOrdenMateria(b.id, -1) // ya es la primera: no pasa nada
        assertEquals(listOf("Dos", "Uno"), repo.materias.first().filter { it.grupoId == g.id }.sortedBy { it.orden }.map { it.nombre })
        assertNotNull(repo.materia(c.id)) // la suelta sigue intacta
    }

    @Test
    fun subirYBajarCarpetas() = runBlocking {
        val a = repo.crearGrupo("A"); val b = repo.crearGrupo("B")
        repo.moverOrdenGrupo(b.id, -1)
        assertEquals(listOf("B", "A"), repo.grupos.first().map { it.nombre })
        repo.moverOrdenGrupo(b.id, 1)
        assertEquals(listOf("A", "B"), repo.grupos.first().map { it.nombre })
    }

    @Test
    fun elAspectoDeLaListaSeGuardaYUnColorMaloSeIgnora() = runBlocking {
        val m = repo.crearMateria("Química")
        repo.editarAspectoMateria(m.id, "🧪", "#e53935")
        assertEquals("🧪", repo.materia(m.id)!!.emoji); assertEquals("#E53935", repo.materia(m.id)!!.color)
        repo.editarAspectoMateria(m.id, " ", "rojo")
        assertNull(repo.materia(m.id)!!.emoji); assertNull(repo.materia(m.id)!!.color)
    }

    @Test
    fun lasCarpetasViajanEnLaCopiaDeSeguridad() = runBlocking {
        val g = repo.crearGrupo("Semestre 3", "🎓", "#1E88E5")
        repo.plegarGrupo(g.id, true)
        val m = repo.crearMateria("Cálculo")
        repo.moverMateriaAGrupo(m.id, g.id)
        repo.editarAspectoMateria(m.id, "📐", "#FB8C00")
        val texto = Copia.exportar(db.dao())

        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val otra = Room.inMemoryDatabaseBuilder(contexto, KiwuDb::class.java).allowMainThreadQueries().build()
        try {
            val r = Copia.importar(otra.dao(), texto)
            assertEquals(1, r.grupos); assertEquals(1, r.materias)
            val grupo = otra.dao().gruposAhora().single()
            assertEquals("Semestre 3", grupo.nombre); assertEquals("🎓", grupo.icono); assertEquals("#1E88E5", grupo.color); assertTrue(grupo.plegado)
            val lista = otra.dao().materia(m.id)!!
            assertEquals(g.id, lista.grupoId); assertEquals("📐", lista.emoji); assertEquals("#FB8C00", lista.color)
            // importar otra vez no duplica nada
            assertEquals(0, Copia.importar(otra.dao(), texto).grupos)
        } finally {
            otra.close()
        }
    }

    @Test
    fun unaCopiaAntiguaSinCarpetasSeImportaConLasListasSueltas() = runBlocking {
        val r = Copia.importar(db.dao(), """{"formato":1,"app":"kiwu","materias":[{"id":"M1","nombre":"Uni"}]}""")
        assertEquals(1, r.materias); assertEquals(0, r.grupos)
        val m = db.dao().materia("M1")!!
        assertNull(m.grupoId); assertNull(m.color); assertNull(m.emoji)
    }

    @Test
    fun unaListaQueApuntaAUnaCarpetaInexistenteSeImportaSuelta() = runBlocking {
        val texto = """{"formato":1,"app":"kiwu","grupos":[{"id":"G1","nombre":"Real"}],
            "materias":[{"id":"M1","nombre":"Buena","grupo":"G1"},{"id":"M2","nombre":"Huérfana","grupo":"NO","color":"verde"}]}"""
        Copia.importar(db.dao(), texto)
        assertEquals("G1", db.dao().materia("M1")!!.grupoId)
        assertNull(db.dao().materia("M2")!!.grupoId)
        assertNull(db.dao().materia("M2")!!.color)
    }

    // --- árbol del menú (sin base de datos) ----------------------------------------------------------

    private fun materia(id: String, nombre: String, orden: Int = 0, grupo: String? = null) =
        Materia(id = id, nombre = nombre, orden = orden, grupoId = grupo, creada = 1, actualizada = 1)

    private fun grupo(id: String, nombre: String, orden: Int = 0) = GrupoLista(id = id, nombre = nombre, orden = orden, creada = 1, actualizada = 1)

    @Test
    fun elMenuPoneLasCarpetasPrimeroYLasSueltasDespues() {
        val nodos = MenuListas.construir(
            materias = listOf(materia("a", "Suelta"), materia("b", "Dentro", grupo = "G")),
            grupos = listOf(grupo("G", "Carpeta")),
            pendientes = mapOf("a" to 1, "b" to 3), orden = OrdenMenu.MANUAL,
        )
        assertEquals(2, nodos.size)
        val carpeta = nodos[0] as NodoMenu.Carpeta
        assertEquals(3, carpeta.pendientes)
        assertEquals(listOf("Dentro"), carpeta.listas.map { it.materia.nombre })
        assertEquals("Suelta", (nodos[1] as NodoMenu.Lista).entrada.materia.nombre)
    }

    @Test
    fun laListaConCarpetaInexistenteCuentaComoSuelta() {
        val nodos = MenuListas.construir(listOf(materia("a", "Huérfana", grupo = "NO")), emptyList(), emptyMap(), OrdenMenu.MANUAL)
        assertTrue(nodos.single() is NodoMenu.Lista)
    }

    @Test
    fun losTresOrdenesDelMenu() {
        val ms = listOf(materia("1", "beta", orden = 0), materia("2", "Alfa", orden = 1), materia("3", "gamma", orden = 2))
        val pend = mapOf("1" to 1, "2" to 0, "3" to 5)
        fun nombres(o: String) = MenuListas.construir(ms, emptyList(), pend, o).map { (it as NodoMenu.Lista).entrada.materia.nombre }
        assertEquals(listOf("beta", "Alfa", "gamma"), nombres(OrdenMenu.MANUAL))
        assertEquals(listOf("Alfa", "beta", "gamma"), nombres(OrdenMenu.ALFABETICO))
        assertEquals(listOf("gamma", "beta", "Alfa"), nombres(OrdenMenu.PENDIENTES))
    }

    @Test
    fun lasListasDeUnaCarpeta() {
        val ms = listOf(materia("a", "A", grupo = "G"), materia("b", "B"), materia("c", "C", grupo = "G"))
        assertEquals(setOf("a", "c"), MenuListas.listasDe(ms, "G"))
    }
}
