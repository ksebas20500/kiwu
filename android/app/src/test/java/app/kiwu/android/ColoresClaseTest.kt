package app.kiwu.android

import androidx.compose.ui.graphics.Color
import app.kiwu.android.domain.Clase
import app.kiwu.android.ui.horario.ColoresClase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** El color automático de las clases: la misma materia repite color en todos sus días. */
class ColoresClaseTest {

    private fun clase(titulo: String, grupo: String, dia: Int = 2, materiaId: String? = null, color: String? = null) =
        Clase(
            horarioId = "h1", materiaId = materiaId, grupo = grupo, titulo = titulo, diaSemana = dia,
            inicioMin = 480, finMin = 600, creada = 0, actualizada = 0, color = color,
        )

    private fun fondo(c: Clase) = ColoresClase.de(c).fondo

    @Test
    fun laMismaMateriaEnDiasDistintosRepiteColor() {
        // Dos entradas separadas (grupos distintos) del mismo título, con días y horas distintas.
        val martes = clase("Algebra", grupo = "AAA1", dia = 2)
        val jueves = clase("Algebra", grupo = "BBB2", dia = 4)
        assertEquals(ColoresClase.de(martes).fondo, ColoresClase.de(jueves).fondo)
        assertEquals(ColoresClase.de(martes).texto, ColoresClase.de(jueves).texto)
    }

    @Test
    fun elTituloSeComparaNormalizado() {
        val a = fondo(clase("Algebra", grupo = "AAA1"))
        val b = fondo(clase("  algebra ", grupo = "BBB2"))
        assertEquals(a, b)
    }

    @Test
    fun titulosDiferentesTienenColorAutomaticoDistinto() {
        // Índices verificados contra «Pasteles»: «clase:algebra» → 4 y «clase:quimica» → 6.
        assertNotEquals(fondo(clase("Algebra", grupo = "AAA1")), fondo(clase("Quimica", grupo = "BBB2")))
    }

    @Test
    fun conListaVinculadaElColorMandaLaLista() {
        val deLaLista = fondo(clase("Nombre libre", grupo = "AAA1", materiaId = "M1"))
        assertEquals(deLaLista, fondo(clase("Otro nombre", grupo = "BBB2", materiaId = "M1")))
        // una clase sin lista cuyo título es «M1» no comparte la clave de la lista M1:
        // «clase:m1» → 5 y la lista «M1» → 6. (Índices verificados contra «Pasteles».)
        assertNotEquals(deLaLista, fondo(clase("M1", grupo = "CCC3")))
    }

    @Test
    fun elColorElegidoGanaSobreElAutomatico() {
        val rojo = Color(0xFFE53935)
        val elegido = ColoresClase.de(clase("Algebra", grupo = "AAA1", color = "#e53935"))
        val automatico = clase("Algebra", grupo = "BBB2")
        assertTrue(elegido.fondo == rojo)
        assertTrue(elegido.texto == ColoresClase.contraste(rojo))
        assertNotEquals(elegido.fondo, fondo(automatico))
    }
}
