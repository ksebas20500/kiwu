package app.kiwu.android

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.kiwu.android.data.Copia
import app.kiwu.android.data.KiwuDb
import app.kiwu.android.data.Repositorio
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Horarios y clases con Room (base en memoria): grupos por días, borrado lógico en cascada y copia de seguridad. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(application = android.app.Application::class)
class HorarioDatosTest {
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

    private suspend fun clase(h: String, titulo: String, dias: Set<Int>, grupo: String? = null, inicio: Int = 480, fin: Int = 600) =
        repo.guardarClase(h, grupo, titulo, null, dias, inicio, fin, "", "", null)

    @Test
    fun variosHorariosIndependientes() = runBlocking {
        val uni = repo.crearHorario("Universidad", "🎓")
        val ocio = repo.crearHorario("Ocio", "🌴")
        clase(uni.id, "Cálculo", setOf(1))
        clase(ocio.id, "Gimnasio", setOf(1), inicio = 1080, fin = 1140)
        assertEquals(listOf("Universidad", "Ocio"), repo.horarios.first().map { it.nombre })
        assertEquals(2, repo.clases.first().size)
        assertEquals(listOf("Cálculo"), repo.clases.first().filter { it.horarioId == uni.id }.map { it.titulo })
    }

    @Test
    fun unaClaseEnVariosDiasEsUnGrupo() = runBlocking {
        val h = repo.crearHorario("Universidad")
        val g = clase(h.id, "Cálculo", setOf(1, 3))
        val filas = repo.clasesDeGrupo(g)
        assertEquals(setOf(1, 3), filas.map { it.diaSemana }.toSet())
        assertEquals(1, filas.map { it.grupo }.toSet().size)
    }

    @Test
    fun editarUnGrupoActualizaAnadeYQuitaDias() = runBlocking {
        val h = repo.crearHorario("Universidad")
        val g = clase(h.id, "Cálculo", setOf(1, 3))
        val miercoles = repo.clasesDeGrupo(g).first { it.diaSemana == 3 }
        // cambia el título y las horas, quita el miércoles y añade el viernes
        repo.guardarClase(h.id, g, "Cálculo II", null, setOf(1, 5), 540, 660, "Aula 5", "", 15)
        val ahora = repo.clasesDeGrupo(g)
        assertEquals(setOf(1, 5), ahora.map { it.diaSemana }.toSet())
        assertTrue(ahora.all { it.titulo == "Cálculo II" && it.inicioMin == 540 && it.finMin == 660 && it.aula == "Aula 5" && it.avisoMin == 15 })
        assertNotNull("El miércoles quitado queda como borrado lógico", eliminadaDe("clase", miercoles.id))
        assertEquals(2, repo.clases.first().size)
    }

    @Test
    fun guardarNoPierdeLaFechaDeCreacion() = runBlocking {
        val h = repo.crearHorario("Universidad")
        val g = clase(h.id, "Cálculo", setOf(1))
        val creada = repo.clasesDeGrupo(g).single().creada
        repo.guardarClase(h.id, g, "Cálculo", null, setOf(1), 540, 600, "", "", null)
        assertEquals(creada, repo.clasesDeGrupo(g).single().creada)
    }

    @Test
    fun datosInvalidosSeRechazan() = runBlocking {
        val h = repo.crearHorario("Universidad")
        for ((dias, ini, fin) in listOf(Triple(emptySet<Int>(), 480, 600), Triple(setOf(8), 480, 600), Triple(setOf(1), 600, 600), Triple(setOf(1), 700, 600))) {
            try {
                repo.guardarClase(h.id, null, "X", null, dias, ini, fin, "", "", null)
                fail("Debía rechazar $dias $ini-$fin")
            } catch (e: IllegalArgumentException) {
                // esperado
            }
        }
        assertTrue(repo.clases.first().isEmpty())
    }

    @Test
    fun eliminarUnGrupoLoMarcaEnTodosSusDias() = runBlocking {
        val h = repo.crearHorario("Universidad")
        val g = clase(h.id, "Cálculo", setOf(1, 3))
        val otro = clase(h.id, "Física", setOf(2))
        repo.eliminarGrupo(g)
        assertEquals(listOf("Física"), repo.clases.first().map { it.titulo })
        assertTrue(repo.clasesDeGrupo(g).isEmpty())
        assertEquals(1, repo.clasesDeGrupo(otro).size)
    }

    @Test
    fun eliminarUnHorarioEliminaSusClasesPeroConservaLasFilas() = runBlocking {
        val uni = repo.crearHorario("Universidad")
        val ocio = repo.crearHorario("Ocio")
        val g = clase(uni.id, "Cálculo", setOf(1, 3))
        clase(ocio.id, "Gimnasio", setOf(2))
        val filaClase = repo.clasesDeGrupo(g).first()
        repo.eliminarHorario(uni.id)
        assertEquals(listOf("Ocio"), repo.horarios.first().map { it.nombre })
        assertEquals(listOf("Gimnasio"), repo.clases.first().map { it.titulo })
        assertNotNull(eliminadaDe("horario", uni.id))
        assertNotNull(eliminadaDe("clase", filaClase.id))
    }

    @Test
    fun elOrdenDeLosHorariosCreceAlFinal() = runBlocking {
        repo.crearHorario("A"); repo.crearHorario("B"); repo.crearHorario("C")
        assertEquals(listOf(0, 1, 2), repo.horarios.first().map { it.orden })
    }

    @Test
    fun copiaDeSeguridadConHorarios() = runBlocking {
        val uni = repo.crearHorario("Universidad", "🎓")
        val m = repo.crearMateria("Cálculo")
        val g = repo.guardarClase(uni.id, null, "Cálculo", m.id, setOf(1, 3), 480, 600, "Aula 204", "Traer calculadora", 10)
        val texto = Copia.exportar(db.dao())

        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val otra = Room.inMemoryDatabaseBuilder(contexto, KiwuDb::class.java).allowMainThreadQueries().build()
        try {
            val r = Copia.importar(otra.dao(), texto)
            assertEquals(1, r.horarios)
            assertEquals(2, r.clases)
            val clases = otra.dao().clasesAhora().sortedBy { it.diaSemana }
            assertEquals(listOf(1, 3), clases.map { it.diaSemana })
            assertTrue(clases.all { it.grupo == g && it.titulo == "Cálculo" && it.aula == "Aula 204" && it.notas == "Traer calculadora" && it.avisoMin == 10 && it.materiaId == m.id })
            assertEquals("🎓", otra.dao().horarios().first().single().icono)
            // importar otra vez no duplica nada
            val de_nuevo = Copia.importar(otra.dao(), texto)
            assertEquals(0, de_nuevo.horarios); assertEquals(0, de_nuevo.clases)
        } finally {
            otra.close()
        }
    }

    @Test
    fun laCopiaIgnoraClasesInvalidasOSinHorario() = runBlocking {
        val texto = """{"formato":1,"app":"kiwu","horarios":[{"id":"H1","nombre":"Uni"}],
            "clases":[
              {"id":"C1","horario":"H1","titulo":"Buena","diaSemana":2,"inicioMin":480,"finMin":540},
              {"id":"C2","horario":"H1","titulo":"Dia malo","diaSemana":9,"inicioMin":480,"finMin":540},
              {"id":"C3","horario":"H1","titulo":"Horas malas","diaSemana":2,"inicioMin":600,"finMin":540},
              {"id":"C4","horario":"NO","titulo":"Sin horario","diaSemana":2,"inicioMin":480,"finMin":540}
            ]}"""
        val r = Copia.importar(db.dao(), texto)
        assertEquals(1, r.horarios)
        assertEquals(1, r.clases)
        assertEquals("Buena", db.dao().clasesAhora().single().titulo)
        assertNull(db.dao().clase("C2"))
    }

    @Test
    fun unaCopiaAntiguaSinHorariosSigueImportandose() = runBlocking {
        val r = Copia.importar(db.dao(), """{"formato":1,"app":"kiwu","materias":[{"id":"M1","nombre":"Uni"}]}""")
        assertEquals(1, r.materias)
        assertEquals(0, r.horarios)
        assertEquals(0, r.clases)
    }

    @Test
    fun elColorDeLaClaseSeGuardaYViajaEnLaCopia() = runBlocking {
        val uni = repo.crearHorario("Universidad", "🎓")
        val g = repo.guardarClase(uni.id, null, "Física", null, setOf(2, 4), 480, 600, "", "", null, color = "#e53935")
        assertTrue(db.dao().clasesDeGrupo(g).all { it.color == "#E53935" })
        val texto = Copia.exportar(db.dao())
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val otra = Room.inMemoryDatabaseBuilder(contexto, KiwuDb::class.java).allowMainThreadQueries().build()
        try {
            Copia.importar(otra.dao(), texto)
            assertTrue(otra.dao().clasesAhora().all { it.color == "#E53935" })
        } finally {
            otra.close()
        }
    }

    @Test
    fun unColorInvalidoSeIgnoraYUnaCopiaAntiguaSinColorSeImporta() = runBlocking {
        val texto = """{"formato":1,"app":"kiwu","horarios":[{"id":"H1","nombre":"Uni"}],
            "clases":[
              {"id":"C1","horario":"H1","titulo":"Sin color","diaSemana":2,"inicioMin":480,"finMin":540},
              {"id":"C2","horario":"H1","titulo":"Color roto","color":"rojo","diaSemana":3,"inicioMin":480,"finMin":540},
              {"id":"C3","horario":"H1","titulo":"Color bueno","color":"#112233","diaSemana":4,"inicioMin":480,"finMin":540}
            ]}"""
        assertEquals(3, Copia.importar(db.dao(), texto).clases)
        assertNull(db.dao().clase("C1")!!.color)
        assertNull(db.dao().clase("C2")!!.color)
        assertEquals("#112233", db.dao().clase("C3")!!.color)
    }
}
