package app.kiwu.android

import app.kiwu.android.domain.Bloque
import app.kiwu.android.domain.Documento
import app.kiwu.android.domain.GeneradorTarjetas
import app.kiwu.android.domain.Tarjeta
import app.kiwu.android.domain.TextRun
import app.kiwu.android.domain.TiposBloque
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneradorTarjetasTest {
    private fun doc(vararg b: Bloque) = Documento(b.toList())

    @Test
    fun tituloConSuExplicacion() {
        val d = doc(
            Bloque(kind = TiposBloque.TITULO, text = "¿Qué es Javascript?"),
            Bloque(text = "Un lenguaje interpretado."),
            Bloque(text = "Base de la web."),
            Bloque(kind = TiposBloque.SUBTITULO, text = "Sin cuerpo"),
        )
        assertEquals(listOf(Tarjeta("¿Qué es Javascript?", "Un lenguaje interpretado.\nBase de la web.")), GeneradorTarjetas.proponer(d))
    }

    @Test
    fun preguntaConElParrafoSiguiente() {
        val d = doc(Bloque(text = "¿Cuánto es 2+2?"), Bloque(text = "Cuatro"), Bloque(text = "Otra cosa"))
        assertEquals(listOf(Tarjeta("¿Cuánto es 2+2?", "Cuatro")), GeneradorTarjetas.proponer(d))
    }

    @Test
    fun terminoYDefinicion() {
        val d = doc(
            Bloque(kind = TiposBloque.VINETAS, text = "Mitocondria: produce la energía de la célula"),
            Bloque(text = "Ribosoma — fabrica proteínas"),
            Bloque(text = "Hoy a las 10:30 hay clase"),
            Bloque(text = "Esta frase es demasiado larga para ser un término de verdad: no cuenta"),
        )
        assertEquals(
            listOf(Tarjeta("Mitocondria", "produce la energía de la célula"), Tarjeta("Ribosoma", "fabrica proteínas")),
            GeneradorTarjetas.proponer(d),
        )
    }

    @Test
    fun negritaComoHueco() {
        val texto = "La capital de Francia es París."
        val d = doc(Bloque(text = texto, runs = listOf(TextRun("La capital de Francia es "), TextRun("París", bold = true), TextRun("."))))
        assertEquals(listOf(Tarjeta("La capital de Francia es _____.", "París")), GeneradorTarjetas.proponer(d))
        val todoNegrita = doc(Bloque(text = "Importante", runs = listOf(TextRun("Importante", bold = true))))
        assertTrue("sin contexto no hay hueco", GeneradorTarjetas.proponer(todoNegrita).isEmpty())
    }

    @Test
    fun noRepiteLasQueYaHay() {
        val d = doc(
            Bloque(text = "Átomo: unidad de la materia"),
            Bloque(kind = TiposBloque.TARJETA, text = "Electrón", answer = "Partícula negativa"),
            Bloque(text = "Electrón: partícula con carga negativa"),
        )
        assertTrue("sin tildes ni mayúsculas", GeneradorTarjetas.proponer(d, listOf(Tarjeta("atomo", "x"))).isEmpty())
        assertEquals(listOf(Tarjeta("Electrón", "Partícula negativa")), GeneradorTarjetas.delApunte(d))
    }

    @Test
    fun respuestasLargasSeRecortan() {
        val largo = (1..200).joinToString(" ") { "palabra" }
        val r = GeneradorTarjetas.proponer(doc(Bloque(text = "¿Qué?"), Bloque(text = largo)))[0].respuesta
        assertTrue(r.length <= 401 && r.endsWith("…"))
    }

    @Test
    fun separadorNecesitaTerminoYDefinicion() {
        assertNull(GeneradorTarjetas.terminoYDefinicion(": sin término"))
        assertNull(GeneradorTarjetas.terminoYDefinicion("Término: ab"))
    }
}
