package app.kiwu.android

import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.ReglasHorario
import app.kiwu.android.domain.SeccionTarea
import app.kiwu.android.domain.TareasDeClase
import app.kiwu.android.domain.UrgenciaTarea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Puntos y grupos de tareas de una clase del horario. */
class TareasDeClaseTest {
    private val dia = 24L * 60 * 60 * 1000
    private val ahora = 1_000_000_000_000L

    private fun deber(id: String, materia: String = "M", fecha: Long? = null, hecho: Boolean = false) =
        Deber(id = id, materiaId = materia, titulo = id, completado = hecho, fechaEntrega = fecha, creada = 1, actualizada = 1)

    @Test
    fun soloCuentanLasPendientesConFechaCercanaOVencidas() {
        val tareas = listOf(
            deber("vencida", fecha = ahora - dia),
            deber("pronto", fecha = ahora + dia),
            deber("normal", fecha = ahora + 5 * dia),
            deber("lejana", fecha = ahora + 30 * dia),
            deber("sinFecha"),
            deber("hecha", fecha = ahora + dia, hecho = true),
            deber("otraMateria", materia = "X", fecha = ahora + dia),
        )
        val puntos = TareasDeClase.puntos(tareas, "M", ahora)
        assertEquals(listOf("vencida", "pronto", "normal"), puntos.map { it.id })
        assertEquals(listOf(UrgenciaTarea.Vencida, UrgenciaTarea.Pronto, UrgenciaTarea.Normal), puntos.map { it.urgencia })
    }

    @Test
    fun sinListaVinculadaNoHayPuntosNiTareas() {
        assertEquals(emptyList<Any>(), TareasDeClase.puntos(listOf(deber("a", fecha = ahora + dia)), null, ahora))
        assertEquals(emptyList<Deber>(), TareasDeClase.pendientes(listOf(deber("a")), null))
        assertNull(TareasDeClase.masUrgente(emptyList()))
    }

    @Test
    fun laUrgenciaMasAltaManda() {
        val puntos = TareasDeClase.puntos(listOf(deber("a", fecha = ahora + 3 * dia), deber("b", fecha = ahora - 1)), "M", ahora)
        assertEquals(UrgenciaTarea.Vencida, TareasDeClase.masUrgente(puntos))
    }

    @Test
    fun lasTareasSeAgrupanPorSeccionYFecha() {
        val tareas = listOf(
            deber("sin"), deber("lejos", fecha = ahora + 20 * dia), deber("semana2", fecha = ahora + 3 * dia),
            deber("semana1", fecha = ahora + dia), deber("vencida", fecha = ahora - 5),
        )
        val grupos = TareasDeClase.agrupadas(tareas, ahora)
        assertEquals(listOf(SeccionTarea.Vencidas, SeccionTarea.EstaSemana, SeccionTarea.Proximas, SeccionTarea.SinFecha), grupos.map { it.first })
        assertEquals(listOf("semana1", "semana2"), grupos[1].second.map { it.id })
    }

    @Test
    fun soloSeAceptanColoresHexadecimalesValidos() {
        assertEquals("#E53935", ReglasHorario.colorValido("#e53935"))
        assertEquals("#E53935", ReglasHorario.colorValido(" #E53935 "))
        assertNull(ReglasHorario.colorValido("rojo"))
        assertNull(ReglasHorario.colorValido("#12345"))
        assertNull(ReglasHorario.colorValido("#GGGGGG"))
        assertNull(ReglasHorario.colorValido(null))
    }
}
