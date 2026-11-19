package app.kiwu.android

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.kiwu.android.data.Copia
import app.kiwu.android.data.KiwuDb
import app.kiwu.android.data.Repositorio
import app.kiwu.android.domain.SesionEstudio
import app.kiwu.android.domain.TiempoEstudio
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/** Sesiones de estudio: registro con poda, importación de las preferencias viejas y copia de seguridad. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(application = android.app.Application::class)
class SesionesTest {
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
    fun registrarSesionLaGuardaConMarcaYPodaLasViejas() = runBlocking {
        val lista = repo.crearMateria("Mate")
        val hoy = LocalDate.of(2026, 11, 13)
        repo.registrarSesion(lista.id, 25, dia = hoy.minusDays(60)) // vieja: se poda al registrar la nueva
        repo.registrarSesion(lista.id, 50, dia = hoy)
        val sesiones = repo.sesionesAhora()
        assertEquals(1, sesiones.size)
        assertEquals(50, sesiones[0].minutos)
        assertEquals(hoy.toEpochDay(), sesiones[0].dia)
        // La podada se queda en la base con `eliminada` (para que el sync la borre en los demás dispositivos).
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM sesion").use { it.moveToFirst(); assertEquals(2, it.getInt(0)) }
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM sesion WHERE eliminada IS NULL").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
        // Menos de un minuto no se registra.
        repo.registrarSesion(lista.id, 0)
        assertEquals(1, repo.sesionesAhora().size)
    }

    @Test
    fun lasSesionesViejasDePreferenciasSeImportanUnaVez() = runBlocking {
        val hoy = LocalDate.of(2026, 11, 13).toEpochDay()
        val viejas = TiempoEstudio.leer("""[{"m":"mate","d":"$hoy","n":"25"},{"d":"$hoy","n":"10"}]""")
        assertEquals(2, viejas.size)
        repo.guardarSesionesImportadas(viejas)
        val sesiones = repo.sesionesAhora()
        assertEquals(2, sesiones.size)
        // Conservan su día y la materia vinculada (aunque la lista no exista ya; sin id no se vincula).
        assertEquals(25, sesiones.first { it.materiaId == "mate" }.minutos)
        assertNull(sesiones.first { it.materiaId == null }.materiaId)
    }

    @Test
    fun copiaIdaYVueltaConSesiones() = runBlocking {
        val lista = repo.crearMateria("Física")
        repo.registrarSesion(lista.id, 25, dia = LocalDate.of(2026, 11, 12))
        repo.registrarSesion(null, 10, dia = LocalDate.of(2026, 11, 12))
        val texto = Copia.exportar(db.dao())
        assertTrue(texto.contains("\"sesiones\"") || texto.contains("sesiones"))

        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val otra = Room.inMemoryDatabaseBuilder(contexto, KiwuDb::class.java).allowMainThreadQueries().build()
        try {
            val r = Copia.importar(otra.dao(), texto)
            assertEquals(2, r.sesiones)
            val sesiones = otra.dao().sesionesAhora()
            assertEquals(2, sesiones.size)
            // La sesión sin lista sigue sin lista; la de Física conserva la suya (con el id de la copia).
            assertEquals(lista.id, sesiones.first { it.minutos == 25 }.materiaId)
            assertNull(sesiones.first { it.minutos == 10 }.materiaId)
            // Importar otra vez no duplica.
            assertEquals(0, Copia.importar(otra.dao(), texto).sesiones)
        } finally {
            otra.close()
        }
    }
}
