package app.kiwu.android

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import app.kiwu.android.domain.Bloque
import app.kiwu.android.domain.Documento
import app.kiwu.android.domain.TiposBloque
import app.kiwu.android.ui.editor.BloqueEd
import app.kiwu.android.ui.editor.EstadoEditor
import app.kiwu.android.ui.editor.ZW
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mantener pulsado «borrar»: llegan muchos eventos seguidos, también a bloques que ya se eliminaron. */
class BorradoTest {
    private fun editor(vararg bloques: Bloque) = EstadoEditor(Documento(bloques.toList()))

    /** Lo que manda el teclado al borrar el carácter invisible de un bloque vacío. */
    private fun borrar(ed: EstadoEditor, b: BloqueEd) = ed.alEscribir(b, TextFieldValue(b.texto, TextRange(0)))

    @Test
    fun borrarSinSoltarSobreUnParrafoVacioNoFalla() {
        val ed = editor(Bloque(text = "uno"), Bloque(text = ""), Bloque(text = ""), Bloque(text = ""))
        val fijos = ed.bloques.toList()
        // Cada bloque recibe muchos eventos aunque ya no esté en la lista.
        repeat(30) { fijos.forEach { borrar(ed, it) } }
        assertTrue(ed.bloques.isNotEmpty())
        assertEquals(TiposBloque.PARRAFO, ed.bloques.last().kind)
    }

    @Test
    fun unSoloParrafoVacioAguantaBorrarSinParar() {
        val ed = editor(Bloque())
        val b = ed.bloques[0]
        repeat(100) { borrar(ed, b) }
        assertEquals(1, ed.bloques.size)
        assertTrue(ed.bloques[0].contenido.campo.text.startsWith(ZW))
    }

    @Test
    fun unBloqueEliminadoIgnoraTodasLasOperaciones() {
        val ed = editor(Bloque(text = "uno"), Bloque(text = ""))
        val viejo = ed.bloques[1]
        borrar(ed, viejo) // lo elimina
        assertEquals(listOf("uno"), ed.bloques.filter { it.kind == TiposBloque.PARRAFO && it.texto.isNotEmpty() }.map { it.texto })
        val antes = ed.documento()
        ed.alBorrarAlInicio(viejo)
        ed.borrarVacio(viejo)
        ed.convertir(viejo, TiposBloque.TITULO)
        ed.cambiarSangria(viejo, 1)
        ed.duplicar(viejo)
        ed.mover(viejo, 1)
        ed.eliminar(viejo)
        ed.alEscribir(viejo, TextFieldValue(ZW + "texto", TextRange(6)))
        assertEquals(antes, ed.documento())
    }

    @Test
    fun borrarSinSoltarEnListasYCodigoNoFalla() {
        val ed = editor(
            Bloque(kind = TiposBloque.VINETAS, text = "", indent = 2),
            Bloque(kind = TiposBloque.TAREAS, text = ""),
            Bloque(kind = TiposBloque.CODIGO, text = ""),
            Bloque(kind = TiposBloque.DIVISOR),
            Bloque(kind = TiposBloque.TITULO, text = ""),
            Bloque(text = ""),
        )
        val fijos = ed.bloques.toList()
        repeat(40) { fijos.forEach { borrar(ed, it) } }
        assertEquals(TiposBloque.PARRAFO, ed.bloques.last().kind)
    }
}
