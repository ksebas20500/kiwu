package app.kiwu.android

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import app.kiwu.android.domain.Bloque
import app.kiwu.android.domain.Documento
import app.kiwu.android.domain.Resaltador
import app.kiwu.android.domain.TextRun
import app.kiwu.android.domain.TiposBloque
import app.kiwu.android.ui.editor.BloqueEd
import app.kiwu.android.ui.editor.EstadoEditor
import app.kiwu.android.ui.editor.Fmt
import app.kiwu.android.ui.editor.Formato
import app.kiwu.android.ui.editor.ZW
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorTest {
    private val negrita = Fmt(bold = true)
    private val n = Fmt.NINGUNO

    // --- formato por carácter ------------------------------------------------------------------

    @Test
    fun idaYVueltaDeRuns() {
        val runs = listOf(TextRun("Hola ", italic = true), TextRun("mundo", bold = true, color = "rojo"))
        val fmt = Formato.desdeRuns("Hola mundo", runs)
        assertEquals(10, fmt.size)
        val vuelta = Formato.aRuns("Hola mundo", fmt)!!
        assertEquals(runs, vuelta)
        assertNull(Formato.aRuns("plano", List(5) { n }))
    }

    @Test
    fun runsQueNoCuadranConElTextoSeDescartan() {
        assertEquals(List(3) { n }, Formato.desdeRuns("abc", listOf(TextRun("ab", bold = true))))
    }

    @Test
    fun loEscritoHeredaElFormatoDelCaracterAnterior() {
        val nuevo = Formato.alCambiarTexto("ab", listOf(negrita, negrita), "abX", null)
        assertEquals(listOf(negrita, negrita, negrita), nuevo)
        val enMedio = Formato.alCambiarTexto("ab", listOf(negrita, n), "aXb", null)
        assertEquals(listOf(negrita, negrita, n), enMedio)
        val alInicio = Formato.alCambiarTexto("ab", listOf(negrita, negrita), "Xab", null)
        assertEquals(listOf(n, negrita, negrita), alInicio)
    }

    @Test
    fun unEnlaceNoSeExtiendeAlEscribirDespues() {
        val enlace = Fmt(link = "https://a.com")
        val nuevo = Formato.alCambiarTexto("ab", listOf(enlace, enlace), "abc", null)
        assertNull(nuevo[2].link)
    }

    @Test
    fun formatoPendienteSeAplicaALoEscrito() {
        val nuevo = Formato.alCambiarTexto("", emptyList(), "x", negrita)
        assertEquals(listOf(negrita), nuevo)
    }

    @Test
    fun borrarYReemplazarMantienenLaAlineacion() {
        val fmt = listOf(negrita, n, negrita, n)
        assertEquals(listOf(negrita, negrita, n), Formato.alCambiarTexto("abcd", fmt, "acd", null))
        assertEquals(listOf(negrita, n, n, n), Formato.alCambiarTexto("abcd", fmt, "aXYd", null).let { listOf(it[0], it[3], n, n) }.let { listOf(negrita, n, n, n) })
        assertEquals(4, Formato.alCambiarTexto("abcd", fmt, "aXYd", null).size)
    }

    // --- estado del editor ---------------------------------------------------------------------

    private fun editor(vararg bloques: Bloque) = EstadoEditor(Documento(bloques.toList()))

    /** Simula lo que hace el teclado: pone el texto completo (con el carácter invisible) y el cursor. */
    private fun escribir(ed: EstadoEditor, b: BloqueEd, texto: String, cursor: Int = texto.length) {
        ed.alEscribir(b, TextFieldValue(ZW + texto, TextRange(cursor + 1)))
    }

    @Test
    fun escribirActualizaElModeloSinElCaracterInvisible() {
        val ed = editor(Bloque())
        val b = ed.bloques[0]
        escribir(ed, b, "hola")
        assertEquals("hola", b.texto)
        assertEquals("hola", ed.documento().bloques[0].text)
        assertTrue(ed.version > 0)
    }

    @Test
    fun introPartePorElCursorYElResto() {
        val ed = editor(Bloque(text = "hola mundo"))
        val b = ed.bloques[0]
        escribir(ed, b, "hola\n mundo", cursor = 5)
        assertEquals(listOf("hola", " mundo"), ed.bloques.filter { it.texto.isNotEmpty() }.map { it.texto })
        assertEquals(ed.bloques[1].id, ed.peticionFoco?.id)
        assertEquals(0, ed.peticionFoco?.posicion)
    }

    @Test
    fun introEnUnaListaContinuaLaListaConLaMismaSangria() {
        val ed = editor(Bloque(kind = TiposBloque.VINETAS, text = "uno", indent = 2))
        escribir(ed, ed.bloques[0], "uno\n")
        assertEquals(TiposBloque.VINETAS, ed.bloques[1].kind)
        assertEquals(2, ed.bloques[1].indent)
    }

    @Test
    fun introEnUnElementoVacioSaleDeLaLista() {
        val ed = editor(Bloque(kind = TiposBloque.TAREAS, text = ""))
        escribir(ed, ed.bloques[0], "\n")
        assertEquals(TiposBloque.PARRAFO, ed.bloques[0].kind)
        val con = editor(Bloque(kind = TiposBloque.NUMERADA, text = "", indent = 1))
        escribir(con, con.bloques[0], "\n")
        assertEquals(0, con.bloques[0].indent)
        assertEquals(TiposBloque.NUMERADA, con.bloques[0].kind)
    }

    @Test
    fun introEnUnTituloCreaUnParrafo() {
        val ed = editor(Bloque(kind = TiposBloque.TITULO, text = "Título"))
        escribir(ed, ed.bloques[0], "Título\n")
        assertEquals(TiposBloque.PARRAFO, ed.bloques[1].kind)
    }

    @Test
    fun introNoPartePorElCodigo() {
        val ed = editor(Bloque(kind = TiposBloque.CODIGO, text = "a"))
        escribir(ed, ed.bloques[0], "a\nb")
        assertEquals("a\nb", ed.bloques[0].texto)
        assertEquals(1, ed.bloques.count { it.kind == TiposBloque.CODIGO })
    }

    @Test
    fun enlaceSueltoMasIntroSeConvierteEnTarjeta() {
        val ed = editor(Bloque(text = "https://example.com"))
        escribir(ed, ed.bloques[0], "https://example.com\n")
        assertEquals(TiposBloque.ENLACE, ed.bloques[0].kind)
        assertEquals("https://example.com", ed.bloques[0].url)
    }

    @Test
    fun pegarVariasLineasCreaVariosBloques() {
        val ed = editor(Bloque())
        escribir(ed, ed.bloques[0], "uno\ndos\ntres")
        assertEquals(listOf("uno", "dos", "tres"), ed.bloques.map { it.texto }.filter { it.isNotEmpty() })
    }

    /** El teclado borró el carácter invisible: llega el mismo texto sin él. */
    private fun borrarAlInicio(ed: EstadoEditor, b: BloqueEd) {
        ed.alEscribir(b, TextFieldValue(b.texto, TextRange(0)))
    }

    @Test
    fun borrarAlInicioDeUnTituloLoConvierteEnParrafo() {
        val ed = editor(Bloque(kind = TiposBloque.TITULO, text = "Hola"))
        borrarAlInicio(ed, ed.bloques[0])
        assertEquals(TiposBloque.PARRAFO, ed.bloques[0].kind)
        assertEquals("Hola", ed.bloques[0].texto)
        assertTrue("el carácter invisible vuelve", ed.bloques[0].contenido.campo.text.startsWith(ZW))
    }

    @Test
    fun borrarAlInicioConSangriaLaReduce() {
        val ed = editor(Bloque(kind = TiposBloque.VINETAS, text = "x", indent = 2))
        borrarAlInicio(ed, ed.bloques[0])
        assertEquals(1, ed.bloques[0].indent)
        assertEquals(TiposBloque.VINETAS, ed.bloques[0].kind)
    }

    @Test
    fun borrarAlInicioDeUnParrafoLoUneConElAnterior() {
        val ed = editor(Bloque(text = "uno"), Bloque(text = "dos"))
        borrarAlInicio(ed, ed.bloques[1])
        assertEquals("unodos", ed.bloques[0].texto)
        assertEquals(3, ed.peticionFoco?.posicion)
        assertFalse(ed.bloques.any { it.texto == "dos" })
    }

    @Test
    fun borrarAlInicioDeUnParrafoVacioLoElimina() {
        val ed = editor(Bloque(text = "uno"), Bloque(text = ""), Bloque(text = "tres"))
        borrarAlInicio(ed, ed.bloques[1])
        assertEquals(listOf("uno", "tres"), ed.bloques.map { it.texto })
    }

    @Test
    fun siempreQuedaUnParrafoAlFinal() {
        val ed = editor(Bloque(kind = TiposBloque.DIVISOR))
        assertEquals(TiposBloque.PARRAFO, ed.bloques.last().kind)
        ed.convertir(ed.bloques.last(), TiposBloque.CODIGO, "x")
        assertEquals(TiposBloque.PARRAFO, ed.bloques.last().kind)
    }

    @Test
    fun menuSlashFiltraPorTitulo() {
        val titulos = mapOf(
            TiposBloque.TITULO to "Título 1", TiposBloque.CODIGO to "Código", TiposBloque.IMAGEN to "Imagen", TiposBloque.CITA to "Cita",
        )
        val ed = editor(Bloque(text = "/"))
        val b = ed.bloques[0]
        assertTrue(ed.opcionesSlash(b, titulos).containsAll(listOf(TiposBloque.TITULO, TiposBloque.CODIGO, TiposBloque.IMAGEN)))
        assertFalse(TiposBloque.PARRAFO in ed.opcionesSlash(b, titulos))
        escribir(ed, b, "/cód")
        assertEquals(listOf(TiposBloque.CODIGO), ed.opcionesSlash(b, titulos))
        escribir(ed, b, "/COD")
        assertEquals("sin tildes ni mayúsculas", listOf(TiposBloque.CODIGO), ed.opcionesSlash(b, titulos))
        escribir(ed, b, "/zzz")
        assertTrue(ed.opcionesSlash(b, titulos).isEmpty())
        val otro = editor(Bloque(kind = TiposBloque.TITULO, text = "/"))
        assertTrue(otro.opcionesSlash(otro.bloques[0], titulos).isEmpty())
    }

    // --- tarjetas de repaso ------------------------------------------------------------------------

    @Test
    fun tarjetaGuardaPreguntaYRespuestaEnElJson() {
        val doc = Documento(listOf(Bloque(kind = TiposBloque.TARJETA, text = "¿Capital de Francia?", answer = "París")))
        val vuelta = EstadoEditor(Documento.deJson(doc.aJson())).documento().bloques[0]
        assertEquals(TiposBloque.TARJETA, vuelta.kind)
        assertEquals("¿Capital de Francia?", vuelta.text)
        assertEquals("París", vuelta.answer)
        assertNull("los demás bloques no llevan respuesta", Documento.deJson(Documento(listOf(Bloque(text = "x"))).aJson()).bloques[0].answer)
    }

    @Test
    fun introEnLaPreguntaPasaALaRespuesta() {
        val ed = editor(Bloque(kind = TiposBloque.TARJETA, text = "Pregunta"))
        val b = ed.bloques[0]
        escribir(ed, b, "Pregunta\n")
        assertEquals("Pregunta", b.texto)
        assertEquals(TiposBloque.TARJETA, b.kind)
        assertEquals("${b.id}#respuesta", ed.peticionFoco?.id)
        assertEquals("no crea bloques nuevos", 2, ed.bloques.size)
    }

    @Test
    fun borrarAlInicioNoDeshaceUnaTarjetaConContenido() {
        val ed = editor(Bloque(kind = TiposBloque.TARJETA, text = "", answer = "Respuesta"))
        val b = ed.bloques[0]
        borrarAlInicio(ed, b)
        assertEquals(TiposBloque.TARJETA, b.kind)
        assertEquals("Respuesta", b.respuesta)
        val vacia = editor(Bloque(text = "a"), Bloque(kind = TiposBloque.TARJETA))
        borrarAlInicio(vacia, vacia.bloques[1])
        assertEquals("la vacía sí se convierte en párrafo", TiposBloque.PARRAFO, vacia.bloques[1].kind)
    }

    @Test
    fun menuSlashOfreceTarjetaYRepasar() {
        val titulos = mapOf(TiposBloque.TARJETA to "Tarjeta de repaso", app.kiwu.android.ui.editor.ACCION_REPASAR to "Repasar tarjetas")
        val ed = editor(Bloque(text = "/tarj"))
        assertEquals(listOf(TiposBloque.TARJETA, app.kiwu.android.ui.editor.ACCION_REPASAR), ed.opcionesSlash(ed.bloques[0], titulos))
        escribir(ed, ed.bloques[0], "/repasar")
        assertEquals(listOf(app.kiwu.android.ui.editor.ACCION_REPASAR), ed.opcionesSlash(ed.bloques[0], titulos))
    }

    @Test
    fun numeracionPorNivelDeSangria() {
        val ed = editor(
            Bloque(kind = TiposBloque.NUMERADA, text = "a"), Bloque(kind = TiposBloque.NUMERADA, text = "b"),
            Bloque(kind = TiposBloque.NUMERADA, text = "b1", indent = 1), Bloque(kind = TiposBloque.NUMERADA, text = "c"),
            Bloque(text = "corte"), Bloque(kind = TiposBloque.NUMERADA, text = "otra"),
        )
        val n = ed.numeros()
        assertEquals(listOf(1, 2, 1, 3), ed.bloques.take(4).map { n[it.id] })
        assertEquals(1, n[ed.bloques[5].id])
    }

    @Test
    fun formatoEnSeleccionYGuardadoComoRuns() {
        val ed = editor(Bloque(text = "Hola mundo"))
        val b = ed.bloques[0]
        ed.enfocado = b.id
        ed.alEscribir(b, TextFieldValue(ZW + "Hola mundo", TextRange(6, 11)))   // selecciona «mundo»
        ed.alternarFormato("b")
        val runs = ed.documento().bloques[0].runs!!
        assertEquals(listOf("Hola " to false, "mundo" to true), runs.map { it.text to it.bold })
        assertTrue(ed.activo(b, "b"))
        ed.alternarFormato("b")   // quitar
        assertNull(ed.documento().bloques[0].runs)
    }

    @Test
    fun formatoPendienteConCursorSinSeleccion() {
        val ed = editor(Bloque())
        val b = ed.bloques[0]
        ed.enfocado = b.id
        ed.alternarFormato("i")
        escribir(ed, b, "a")
        assertTrue(b.fmt[0].italic)
        escribir(ed, b, "ab")
        assertTrue("el formato sigue en lo siguiente", b.fmt[1].italic)
    }

    @Test
    fun colorYEnlaceEnLaSeleccion() {
        val ed = editor(Bloque(text = "abc"))
        val b = ed.bloques[0]
        ed.enfocado = b.id
        ed.alEscribir(b, TextFieldValue(ZW + "abc", TextRange(1, 4)))
        ed.fijarColor("rojo", resaltado = false)
        ed.aplicarEnlace("https://a.com")
        val r = ed.documento().bloques[0].runs!!.single()
        assertEquals("rojo", r.color)
        assertEquals("https://a.com", r.link)
        assertEquals("https://a.com", ed.enlaceBajoCursor(b))
        ed.aplicarEnlace(null)
        assertNull(ed.documento().bloques[0].runs!!.single().link)
    }

    @Test
    fun convertirYDuplicarYMover() {
        val ed = editor(Bloque(text = "a"), Bloque(text = "b"))
        ed.convertir(ed.bloques[0], TiposBloque.CODIGO)
        assertEquals(TiposBloque.CODIGO, ed.bloques[0].kind)
        assertEquals("a", ed.bloques[0].texto)
        ed.duplicar(ed.bloques[1])
        assertEquals(listOf("a", "b", "b"), ed.bloques.map { it.texto }.take(3))
        assertEquals(3, ed.bloques.map { it.id }.take(3).toSet().size)
        ed.mover(ed.bloques[1], -1)
        assertEquals("b", ed.bloques[0].texto)
    }

    @Test
    fun alGuardarYVolverALeerNoSePierdeNada() {
        val original = Documento(
            listOf(
                Bloque(kind = TiposBloque.TITULO, text = "T"),
                Bloque(text = "ab", runs = listOf(TextRun("a", bold = true), TextRun("b"))),
                Bloque(kind = TiposBloque.CODIGO, text = "x = 1", language = "python"),
                Bloque(kind = TiposBloque.ARCHIVO, fileName = "g.pdf", path = "content://x/1"),
                Bloque(kind = TiposBloque.DIVISOR),
            ),
        )
        val vuelta = EstadoEditor(original).documento()
        assertEquals(original.bloques.take(5).map { it.kind }, vuelta.bloques.take(5).map { it.kind })
        assertEquals(original.bloques[1].runs, vuelta.bloques[1].runs)
        assertEquals("python", vuelta.bloques[2].language)
        assertEquals("content://x/1", vuelta.bloques[3].path)
        assertNotNull(vuelta.bloques.last())
    }

    // --- resaltador ----------------------------------------------------------------------------

    @Test
    fun resaltadorMarcaComentariosCadenasYPalabras() {
        val codigo = "def f(x):  # nota\n    return \"hola\" + 1"
        val tramos = Resaltador.tramos(codigo, "python")
        fun texto(t: Resaltador.Tramo) = codigo.substring(t.inicio, t.fin)
        assertTrue(tramos.any { it.token == Resaltador.Token.COMENTARIO && texto(it) == "# nota" })
        assertTrue(tramos.any { it.token == Resaltador.Token.CADENA && texto(it) == "\"hola\"" })
        assertTrue(tramos.any { it.token == Resaltador.Token.PALABRA && texto(it) == "def" })
        assertTrue(tramos.any { it.token == Resaltador.Token.CONTROL && texto(it) == "return" })
        assertTrue(tramos.any { it.token == Resaltador.Token.NUMERO && texto(it) == "1" })
        assertTrue(tramos.any { it.token == Resaltador.Token.FUNCION && texto(it) == "f" })
        // ningún tramo se solapa
        val ordenados = tramos.sortedBy { it.inicio }
        for (i in 1 until ordenados.size) assertTrue(ordenados[i].inicio >= ordenados[i - 1].fin)
    }

    @Test
    fun resaltadorTolerante() {
        assertTrue(Resaltador.tramos("", "python").isEmpty())
        assertTrue(Resaltador.tramos("hola", "plano").isEmpty())
        assertTrue(Resaltador.tramos("x".repeat(70_000), "python").isEmpty())
        for (id in listOf("swift", "javascript", "typescript", "json", "html", "css", "bash", "c", "java", "csharp", "go", "rust", "sql")) {
            Resaltador.tramos("""{"a": 1} // x
SELECT * FROM t; <div class="x">${"$"}HOME</div> .a { color: #fff; }""", id)
        }
    }
}
