package app.kiwu.android

import app.kiwu.android.domain.EventoExterno
import app.kiwu.android.domain.EventosExternos
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Copias del mismo evento: el teléfono devuelve una fila por calendario y cada cuenta de Google trae su propio
 * calendario de festivos, así que con cuatro cuentas el mismo festivo llega cuatro veces con ids distintos.
 */
class EventosExternosTest {
    private val base = 1_796_000_000_000L // un día cualquiera de 2026
    private val unDia = 86_400_000L

    private fun evento(
        id: Long,
        titulo: String = "Independencia de Cartagena",
        inicio: Long = base,
        fin: Long? = base + unDia,
        todoElDia: Boolean = true,
        tarea: Boolean = false,
    ) = EventoExterno(id, titulo, inicio, todoElDia, fin, tarea)

    @Test
    fun elMismoFestivoEnCuatroCuentasQuedaUnaVez() {
        // Cuatro calendarios de festivos (uno por cuenta): mismo título, misma hora, distinto id.
        val copias = listOf(evento(11), evento(12), evento(13), evento(14))
        val unicos = EventosExternos.deduplicar(copias)
        assertEquals(1, unicos.size)
        assertEquals(11L, unicos.single().id) // se queda la primera
    }

    @Test
    fun dosEventosConElMismoTituloPeroOtraHoraSiguenSiendoDos() {
        val lista = listOf(evento(1), evento(2, inicio = base + 3_600_000, fin = base + 7_200_000))
        assertEquals(2, EventosExternos.deduplicar(lista).size)
    }

    @Test
    fun dosEventosConElMismoTituloYHoraPeroOtroFinSiguenSiendoDos() {
        val lista = listOf(evento(1, fin = base + 3_600_000), evento(2, fin = base + 7_200_000))
        assertEquals(2, EventosExternos.deduplicar(lista).size)
    }

    @Test
    fun unoDeTodoElDiaYOtroConHoraNoSeMezclan() {
        val lista = listOf(evento(1, todoElDia = true), evento(2, todoElDia = false))
        assertEquals(2, EventosExternos.deduplicar(lista).size)
    }

    @Test
    fun unaTareaDeGoogleTasksNoSeMezclaConUnEvento() {
        val lista = listOf(evento(1), evento(2, tarea = true))
        assertEquals(2, EventosExternos.deduplicar(lista).size)
    }

    @Test
    fun losEspaciosDelTituloNoHacenOtroEvento() {
        val lista = listOf(evento(1, titulo = "Parcial"), evento(2, titulo = "  Parcial "))
        assertEquals(1, EventosExternos.deduplicar(lista).size)
    }

    @Test
    fun seConservaElOrdenDeEntrada() {
        val temprano = evento(1, titulo = "A")
        val tarde = evento(2, titulo = "B", inicio = base + unDia, fin = base + 2 * unDia)
        assertEquals(listOf(1L, 2L), EventosExternos.deduplicar(listOf(temprano, tarde)).map { it.id })
        assertEquals(listOf(2L, 1L), EventosExternos.deduplicar(listOf(tarde, temprano)).map { it.id })
    }
}
