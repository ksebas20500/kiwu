package app.kiwu.android

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.kiwu.android.data.KiwuDb
import app.kiwu.android.data.Repositorio
import app.kiwu.android.domain.Guia
import app.kiwu.android.domain.ID_EVENTOS
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Guía de inicio (cuándo sale y la lista de ejemplo) y los datos de la racha, con Room en memoria. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(application = android.app.Application::class)
class GuiaTest {
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
    fun laGuiaSoloSaleLaPrimeraVezConLaAppVacia() {
        assertTrue(Guia.debeMostrarse(guiaVista = false, hayDatos = false))
        assertFalse(Guia.debeMostrarse(guiaVista = true, hayDatos = false))
        assertFalse(Guia.debeMostrarse(guiaVista = false, hayDatos = true))
    }

    @Test
    fun unaAppConDatosNoEsUnaInstalacionNueva() = runBlocking {
        assertFalse(repo.hayDatos())
        repo.crearCuaderno("Apuntes")
        assertTrue(repo.hayDatos())
    }

    @Test
    fun laListaDeEjemploTraeSusTareasEnOrden() = runBlocking {
        val r = ApplicationProvider.getApplicationContext<Context>().resources
        val nombres = Guia.tareas.map { r.getString(it) }
        assertTrue(repo.crearListaDeEjemplo(r.getString(Guia.NOMBRE_LISTA), nombres))
        val lista = repo.materia(Guia.ID_LISTA)!!
        assertEquals(r.getString(Guia.NOMBRE_LISTA), lista.nombre)
        assertEquals(Guia.EMOJI_LISTA, lista.emoji)
        val tareas = repo.deberesAhora().filter { it.materiaId == Guia.ID_LISTA }.sortedBy { it.creada }
        assertEquals(nombres, tareas.map { it.titulo })
        // Sin fecha: no disparan avisos ni salen vencidas.
        assertTrue(tareas.all { it.fechaEntrega == null && !it.completado })
    }

    @Test
    fun laListaDeEjemploNoSeDuplicaNiResucitaSiSeBorro() = runBlocking {
        assertTrue(repo.crearListaDeEjemplo("Empieza aquí", listOf("Una tarea")))
        assertFalse(repo.crearListaDeEjemplo("Empieza aquí", listOf("Una tarea")))
        repo.eliminarMateria(Guia.ID_LISTA)
        assertFalse(repo.crearListaDeEjemplo("Empieza aquí", listOf("Una tarea")))
        assertEquals(null, repo.materia(Guia.ID_LISTA))
    }

    @Test
    fun lasFechasDeLaRachaIncluyenTareasBorradasPeroNoEventosNiDesmarcadas() = runBlocking {
        val lista = repo.crearMateria("Química")
        val hecha = repo.crearDeber("Informe", lista.id)
        val borrada = repo.crearDeber("Quiz", lista.id)
        val desmarcada = repo.crearDeber("Taller", lista.id)
        val evento = repo.crearDeber("Charla", ID_EVENTOS, reloj)
        repo.marcar(hecha.id, true)
        repo.marcar(borrada.id, true)
        repo.eliminarDeber(borrada.id)
        repo.marcar(desmarcada.id, true)
        repo.marcar(desmarcada.id, false)
        repo.marcar(evento.id, true)
        assertEquals(2, repo.fechasCompletado.first().size)
        assertEquals(2, repo.fechasCompletadoAhora().size)
    }
}
