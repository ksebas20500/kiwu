package app.kiwu.android

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.kiwu.android.data.Copia
import app.kiwu.android.data.KiwuDb
import app.kiwu.android.data.Repositorio
import app.kiwu.android.domain.Clase
import app.kiwu.android.domain.EstadoFaltas
import app.kiwu.android.domain.Falta
import app.kiwu.android.domain.ReglasFaltas
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/** Faltas de clase: reglas puras, guardado por materia (grupo) y copia de seguridad. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(application = android.app.Application::class)
class FaltasTest {
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

    private fun falta(grupo: String, dia: Long = 0, justificada: Boolean = false) =
        Falta(grupo = grupo, horarioId = "H", dia = dia, justificada = justificada, creada = 0)

    private fun clase(grupo: String, dia: Int, max: Int? = null) =
        Clase(horarioId = "H", grupo = grupo, titulo = "Cálculo", diaSemana = dia, inicioMin = 480, finMin = 600, maxFaltas = max, creada = 0, actualizada = 0)

    @Test
    fun lasJustificadasNoCuentan() {
        assertEquals(2, ReglasFaltas.contar(listOf(falta("G"), falta("G"), falta("G", justificada = true))))
    }

    @Test
    fun estadosYUmbrales() {
        assertEquals(EstadoFaltas.SinLimite, ReglasFaltas.estado(3, null))
        assertEquals(EstadoFaltas.SinLimite, ReglasFaltas.estado(3, 0))
        assertEquals(EstadoFaltas.Tranquilo, ReglasFaltas.estado(0, 10))
        assertEquals(EstadoFaltas.Tranquilo, ReglasFaltas.estado(5, 10))
        assertEquals(EstadoFaltas.Aviso, ReglasFaltas.estado(6, 10))
        assertEquals(EstadoFaltas.Aviso, ReglasFaltas.estado(9, 10))
        assertEquals(EstadoFaltas.Superado, ReglasFaltas.estado(10, 10))
        assertEquals(EstadoFaltas.Superado, ReglasFaltas.estado(12, 10))
    }

    @Test
    fun quedanNuncaEsNegativo() {
        assertEquals(3, ReglasFaltas.quedan(2, 5))
        assertEquals(0, ReglasFaltas.quedan(7, 5))
    }

    @Test
    fun elContadorEsPorMateriaAunqueSeDeVariosDias() {
        val clases = listOf(clase("G", 1, 5), clase("G", 3, 5), clase("O", 2))
        val c = ReglasFaltas.contadores(clases, listOf(falta("G"), falta("G", justificada = true), falta("X")))
        assertEquals(1, c.getValue("G").cuentan)
        assertEquals(2, c.getValue("G").total)
        assertEquals(4, c.getValue("G").quedan)
        assertNull(c.getValue("O").maximo)
        assertFalse(ReglasFaltas.seMuestra(c["O"]))
        assertTrue(ReglasFaltas.seMuestra(c["G"]))
    }

    @Test
    fun tocaClaseSegunElDiaDeLaSemana() {
        val lunes = LocalDate.of(2026, 10, 26)
        assertTrue(ReglasFaltas.tocaClase(lunes, listOf(clase("G", 1))))
        assertFalse(ReglasFaltas.tocaClase(lunes.plusDays(1), listOf(clase("G", 1))))
    }

    @Test
    fun marcarJustificarYQuitar() = runBlocking {
        val h = repo.crearHorario("Uni")
        val g = repo.guardarClase(h.id, null, "Cálculo", null, setOf(1, 3), 480, 600, "", "", null, maxFaltas = 4)
        val dia = LocalDate.of(2026, 10, 26)
        val f = repo.marcarFalta(g, h.id, dia)
        repo.marcarFalta(g, h.id, dia.plusDays(2))
        repo.justificarFalta(f.id, true)
        val faltas = repo.faltas.first()
        assertEquals(2, faltas.size)
        assertEquals(dia.toEpochDay(), faltas.first { it.id == f.id }.dia)
        val c = ReglasFaltas.contadores(repo.clasesAhora(), faltas).getValue(g)
        assertEquals(1, c.cuentan)
        assertEquals(3, c.quedan)
        repo.quitarFalta(f.id)
        assertEquals(1, repo.faltas.first().size)
    }

    @Test
    fun unMismoDiaSoloCuentaUnaVez() = runBlocking {
        val h = repo.crearHorario("Uni")
        val g = repo.guardarClase(h.id, null, "Cálculo", null, setOf(1), 480, 600, "", "", null)
        val dia = LocalDate.of(2026, 10, 26)
        val primera = repo.marcarFalta(g, h.id, dia)
        assertEquals(primera.id, repo.marcarFalta(g, h.id, dia).id)
        assertEquals(1, repo.faltas.first().size)
    }

    @Test
    fun renombrarLaMateriaConservaSusFaltasYElMaximo() = runBlocking {
        val h = repo.crearHorario("Uni")
        val g = repo.guardarClase(h.id, null, "Cálculo", null, setOf(1), 480, 600, "", "", null, maxFaltas = 3)
        repo.marcarFalta(g, h.id)
        repo.guardarClase(h.id, g, "Cálculo I", null, setOf(1, 4), 480, 600, "", "", null, maxFaltas = 3)
        val c = ReglasFaltas.contadores(repo.clasesAhora(), repo.faltas.first()).getValue(g)
        assertEquals(1, c.cuentan)
        assertEquals(3, c.maximo)
    }

    @Test
    fun ponerMaximoLlegaATodasLasFilasYVaciarBorraElHorario() = runBlocking {
        val uni = repo.crearHorario("Uni")
        val otro = repo.crearHorario("Trabajo")
        val g = repo.guardarClase(uni.id, null, "Cálculo", null, setOf(1, 3), 480, 600, "", "", null)
        val g2 = repo.guardarClase(otro.id, null, "Inglés", null, setOf(2), 480, 600, "", "", null)
        repo.ponerMaximoFaltas(g, 6)
        assertTrue(repo.clasesDeGrupo(g).all { it.maxFaltas == 6 })
        repo.ponerMaximoFaltas(g, 0)
        assertTrue(repo.clasesDeGrupo(g).all { it.maxFaltas == null })
        repo.marcarFalta(g, uni.id); repo.marcarFalta(g2, otro.id)
        repo.vaciarFaltas(uni.id)
        assertEquals(listOf(g2), repo.faltas.first().map { it.grupo })
    }

    @Test
    fun copiaIdaYVueltaConFaltas() = runBlocking {
        val h = repo.crearHorario("Uni")
        val g = repo.guardarClase(h.id, null, "Cálculo", null, setOf(1), 480, 600, "", "", null, maxFaltas = 5)
        repo.marcarFalta(g, h.id, LocalDate.of(2026, 10, 26), justificada = true)
        val texto = Copia.exportar(db.dao())
        assertTrue(texto.contains("\"dia\": \"2026-10-26\"") || texto.contains("\"dia\":\"2026-10-26\""))

        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val otra = Room.inMemoryDatabaseBuilder(contexto, KiwuDb::class.java).allowMainThreadQueries().build()
        try {
            val r = Copia.importar(otra.dao(), texto)
            assertEquals(1, r.faltas)
            val f = otra.dao().faltasAhora().single()
            assertEquals(LocalDate.of(2026, 10, 26).toEpochDay(), f.dia)
            assertTrue(f.justificada)
            assertEquals(5, otra.dao().clasesAhora().single().maxFaltas)
            // Importar otra vez no duplica.
            assertEquals(0, Copia.importar(otra.dao(), texto).faltas)
        } finally {
            otra.close()
        }
    }

    @Test
    fun alImportarSeDescartanLasFaltasDeMateriasQueNoExisten() = runBlocking {
        val copia = """{"app":"kiwu","formato":1,"faltas":[{"id":"F1","grupo":"NO","horario":"H","dia":"2026-10-26","justificada":false}]}"""
        assertEquals(0, Copia.importar(db.dao(), copia).faltas)
        assertTrue(db.dao().faltasAhora().isEmpty())
    }
}
