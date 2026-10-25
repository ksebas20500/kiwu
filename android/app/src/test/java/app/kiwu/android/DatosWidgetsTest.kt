package app.kiwu.android

import androidx.compose.ui.graphics.Color
import app.kiwu.android.data.ResumenPagina
import app.kiwu.android.domain.Clase
import app.kiwu.android.domain.Cuaderno
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.GrupoLista
import app.kiwu.android.domain.Horario
import app.kiwu.android.domain.ID_EVENTOS
import app.kiwu.android.domain.Materia
import app.kiwu.android.domain.UrgenciaTarea
import app.kiwu.android.widgets.ConfigWidget
import app.kiwu.android.widgets.DatosWidgets
import app.kiwu.android.widgets.TextosWidget
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lo que muestran los widgets, con datos de ejemplo y una zona horaria fija (sin emulador). */
class DatosWidgetsTest {
    private val zona: ZoneId = ZoneId.of("Europe/Madrid")
    private val textos = TextosWidget("Hoy", "Mañana", "Próximos 7 días", "Deberes", "Sin fecha", "Vencida", "Sin título")
    private val antes = java.util.Locale.getDefault()

    @org.junit.Before
    fun enEspanol() = java.util.Locale.setDefault(java.util.Locale.forLanguageTag("es"))

    @org.junit.After
    fun restaurar() = java.util.Locale.setDefault(antes)

    /** Lunes 19 de octubre de 2026, 12:00. */
    private val lunes: LocalDate = LocalDate.of(2026, 10, 19)
    private val ahora: Long = ms(lunes, 12, 0)

    private fun ms(fecha: LocalDate, hora: Int = 12, minuto: Int = 0): Long =
        LocalDateTime.of(fecha, java.time.LocalTime.of(hora, minuto)).atZone(zona).toInstant().toEpochMilli()

    private fun materia(id: String, nombre: String, grupo: String? = null) =
        Materia(id = id, nombre = nombre, grupoId = grupo, creada = 0, actualizada = 0)

    private fun deber(id: String, materiaId: String, titulo: String, fecha: Long? = null, completado: Boolean = false) =
        Deber(id = id, materiaId = materiaId, titulo = titulo, fechaEntrega = fecha, completado = completado, creada = 0, actualizada = 0)

    private fun horario(id: String, nombre: String) = Horario(id = id, nombre = nombre, creada = 0, actualizada = 0)

    private fun clase(
        id: String, horarioId: String, titulo: String, dia: Int = 1, inicio: Int = 480, fin: Int = 600,
        aula: String = "", materiaId: String? = null, color: String? = null,
    ) = Clase(
        id = id, horarioId = horarioId, titulo = titulo, diaSemana = dia, inicioMin = inicio, finMin = fin,
        aula = aula, materiaId = materiaId, color = color, creada = 0, actualizada = 0,
    )

    private fun cuaderno(id: String, nombre: String) = Cuaderno(id = id, nombre = nombre, creada = 0, actualizada = 0)

    private fun pagina(id: String, cuadernoId: String, titulo: String, texto: String, actualizada: Long) =
        ResumenPagina(id = id, cuadernoId = cuadernoId, titulo = titulo, icono = "📄", favorita = false, textoPlano = texto, creada = 0, actualizada = actualizada)

    // --- deberes ---------------------------------------------------------------------------------

    @Test
    fun deberesOrdenaPorFechaYDejaLasVencidasPrimero() {
        val listas = listOf(materia("mate", "Cálculo"))
        val deberes = listOf(
            deber("sin", "mate", "Sin fecha"),
            deber("tarde", "mate", "La semana que viene", ms(lunes.plusDays(6))),
            deber("manana", "mate", "Mañana", ms(lunes.plusDays(1))),
            deber("hecha", "mate", "Ya hecha", ms(lunes), completado = true),
        )
        val datos = DatosWidgets.deberes(listas, emptyList(), deberes, null, ahora, textos, zona)
        assertEquals(listOf("manana", "tarde", "sin"), datos.pendientes.map { it.id })
        assertEquals(3, datos.total)
        assertTrue(datos.hayListas)
        assertEquals("Cálculo", datos.pendientes.first().lista)
        assertEquals("Mañana", datos.pendientes.first().fecha)
    }

    @Test
    fun deberesNoMuestranEventos() {
        val listas = listOf(materia("mate", "Cálculo"))
        val deberes = listOf(
            deber("evento", ID_EVENTOS, "Cumpleaños", ms(lunes)),
            deber("tarea", "mate", "Informe", ms(lunes.plusDays(2))),
        )
        val datos = DatosWidgets.deberes(listas, emptyList(), deberes, null, ahora, textos, zona)
        assertEquals(listOf("tarea"), datos.pendientes.map { it.id })
    }

    @Test
    fun deberesPorListaCarpetaHoyYProximos() {
        val listas = listOf(
            materia("mate", "Cálculo", grupo = "uni"),
            materia("fis", "Física", grupo = "uni"),
            materia("ocio", "Ocio"),
        )
        val grupos = listOf(GrupoLista(id = "uni", nombre = "Universidad", creada = 0, actualizada = 0))
        val deberes = listOf(
            deber("m1", "mate", "Derivadas", ms(lunes, 18, 0)),
            deber("f1", "fis", "Laboratorio", ms(lunes.plusDays(3))),
            deber("o1", "ocio", "Película", ms(lunes)),
            deber("m2", "mate", "Examen", ms(lunes.plusDays(30))),
        )

        val una = DatosWidgets.deberes(listas, grupos, deberes, ConfigWidget.lista("mate"), ahora, textos, zona)
        assertEquals("Cálculo", una.titulo)
        assertEquals(listOf("m1", "m2"), una.pendientes.map { it.id })

        val carpeta = DatosWidgets.deberes(listas, grupos, deberes, ConfigWidget.grupo("uni"), ahora, textos, zona)
        assertEquals("Universidad", carpeta.titulo)
        assertEquals(listOf("m1", "f1", "m2"), carpeta.pendientes.map { it.id })

        val hoy = DatosWidgets.deberes(listas, grupos, deberes, ConfigWidget.HOY, ahora, textos, zona)
        assertEquals("Hoy", hoy.titulo)
        assertEquals(listOf("o1", "m1"), hoy.pendientes.map { it.id })

        val proximos = DatosWidgets.deberes(listas, grupos, deberes, ConfigWidget.PROXIMOS, ahora, textos, zona)
        assertEquals(listOf("o1", "m1", "f1"), proximos.pendientes.map { it.id })
    }

    @Test
    fun fechaEnPalabras() {
        assertEquals("Sin fecha", DatosWidgets.textoFecha(null, ahora, textos, zona))
        assertEquals("Vencida", DatosWidgets.textoFecha(ms(lunes.minusDays(1)), ahora, textos, zona))
        assertEquals("Hoy", DatosWidgets.textoFecha(ms(lunes), ahora, textos, zona))
        assertEquals("Mañana", DatosWidgets.textoFecha(ms(lunes.plusDays(1)), ahora, textos, zona))
        assertEquals("Jue", DatosWidgets.textoFecha(ms(lunes.plusDays(3)), ahora, textos, zona))
        assertEquals("3 nov", DatosWidgets.textoFecha(ms(LocalDate.of(2026, 11, 3)), ahora, textos, zona))
    }

    @Test
    fun sinListasElWidgetInvitaACrear() {
        val datos = DatosWidgets.deberes(emptyList(), emptyList(), emptyList(), null, ahora, textos, zona)
        assertEquals(false, datos.hayListas)
        assertTrue(datos.pendientes.isEmpty())
    }

    // --- horario ---------------------------------------------------------------------------------

    @Test
    fun horarioMuestraLaClaseEnCursoYLaSiguiente() {
        val h = horario("uni", "Universidad")
        val clases = listOf(
            clase("calculo", h.id, "Cálculo", inicio = 480, fin = 600, aula = "Aula 3", materiaId = "mate"),
            clase("fisica", h.id, "Física", inicio = 600, fin = 720, aula = "Lab"),
        )
        val datos = DatosWidgets.horario(listOf(h), clases, emptyList(), null, ms(lunes, 9, 0), zona)
        assertEquals("Cálculo", datos.enCurso?.titulo)
        assertEquals("Física", datos.siguiente?.titulo)
        assertEquals(0, datos.diasSiguiente)
        assertEquals(listOf("Cálculo", "Física"), datos.hoy.map { it.titulo })
        assertEquals("Universidad", datos.hoy.first().horario)
        assertTrue(datos.hayHorarios)
    }

    @Test
    fun horarioSinClaseEnCursoPasaALaSiguiente() {
        val h = horario("uni", "Universidad")
        val clases = listOf(clase("calculo", h.id, "Cálculo", inicio = 480, fin = 600))
        val tarde = ms(lunes, 14, 0)
        val datos = DatosWidgets.horario(listOf(h), clases, emptyList(), null, tarde, zona)
        assertNull(datos.enCurso)
        assertEquals("Cálculo", datos.siguiente?.titulo)
        // La siguiente es la misma clase de la semana que viene.
        assertEquals(7, datos.diasSiguiente)
    }

    @Test
    fun horarioFiltraPorHorarioElegido() {
        val uni = horario("uni", "Universidad")
        val ocio = horario("ocio", "Ocio")
        val clases = listOf(
            clase("calculo", uni.id, "Cálculo", inicio = 480, fin = 600),
            clase("gym", ocio.id, "Gimnasio", inicio = 1080, fin = 1140),
        )
        val datos = DatosWidgets.horario(listOf(uni, ocio), clases, emptyList(), ConfigWidget.horario(ocio.id), ahora, zona)
        assertEquals("Ocio", datos.nombre)
        assertEquals(listOf("Gimnasio"), datos.hoy.map { it.titulo })
        assertNull(datos.hoy.first().horario)
    }

    @Test
    fun horarioMuestraElPuntoMasUrgenteDeLaMateria() {
        val h = horario("uni", "Universidad")
        val clases = listOf(clase("calculo", h.id, "Cálculo", materiaId = "mate"))
        val deberes = listOf(
            deber("vencida", "mate", "Entrega atrasada", ms(lunes.minusDays(2))),
            deber("pronto", "mate", "Trabajo", ms(lunes.plusDays(1))),
            deber("otra", "fis", "No cuenta", ms(lunes.plusDays(1))),
        )
        val datos = DatosWidgets.horario(listOf(h), clases, deberes, null, ahora, zona)
        assertEquals(UrgenciaTarea.Vencida, datos.hoy.first().urgencia)
    }

    @Test
    fun horarioMuestraLasFaltasDeLaMateriaSoloSiHayAlgoQueContar() {
        val h = horario("uni", "Universidad")
        val clases = listOf(
            clase("calculo", h.id, "Cálculo").copy(grupo = "G1", maxFaltas = 4),
            clase("fisica", h.id, "Física", inicio = 600, fin = 700).copy(grupo = "G2"),
        )
        val faltas = listOf(
            app.kiwu.android.domain.Falta(grupo = "G1", horarioId = h.id, dia = 0, creada = 0),
            app.kiwu.android.domain.Falta(grupo = "G1", horarioId = h.id, dia = 1, justificada = true, creada = 0),
        )
        val datos = DatosWidgets.horario(listOf(h), clases, emptyList(), null, ahora, zona, faltas)
        val calculo = datos.hoy.first { it.id == "calculo" }.faltas!!
        assertEquals(1, calculo.cuentan)
        assertEquals(3, calculo.quedan)
        assertEquals(null, datos.hoy.first { it.id == "fisica" }.faltas)
    }

    @Test
    fun horarioUsaElColorElegidoDeLaClase() {
        val h = horario("uni", "Universidad")
        val clases = listOf(clase("calculo", h.id, "Cálculo", color = "#1E88E5"))
        val datos = DatosWidgets.horario(listOf(h), clases, emptyList(), null, ahora, zona)
        assertEquals(Color(0xFF1E88E5), datos.hoy.first().color.fondo)
    }

    @Test
    fun proximoRefrescoEsElCambioDeClaseOMedianoche() {
        val clases = listOf(
            clase("calculo", "uni", "Cálculo", inicio = 480, fin = 600),
            clase("fisica", "uni", "Física", inicio = 600, fin = 720),
        )
        // A las 9:00, lo próximo es el fin de Cálculo (10:00).
        assertEquals(ms(lunes, 10, 0), DatosWidgets.proximoRefresco(clases, ms(lunes, 9, 0), zona))
        // A las 14:00 ya no queda nada hoy: toca la medianoche.
        assertEquals(ms(lunes.plusDays(1), 0, 0), DatosWidgets.proximoRefresco(clases, ms(lunes, 14, 0), zona))
    }

    // --- notas -----------------------------------------------------------------------------------

    @Test
    fun notasFiltraPorCuadernoYOrdenaPorFecha() {
        val cuadernos = listOf(cuaderno("ideas", "Ideas"), cuaderno("clase", "Clase"))
        val paginas = listOf(
            pagina("a", "ideas", "Proyecto", "  Ideas\n\npara   el proyecto ", 100),
            pagina("b", "ideas", "Otra", "Más ideas", 300),
            pagina("c", "clase", "Apuntes", "Tema 1", 200),
        )
        val datos = DatosWidgets.notas(cuadernos, paginas, ConfigWidget.cuaderno("ideas"), textos)
        assertEquals("Ideas", datos.cuaderno?.nombre)
        assertEquals(listOf("b", "a"), datos.notas.map { it.id })
        assertEquals("Ideas para el proyecto", datos.notas.last().resumen)
        assertEquals("ideas", datos.cuadernoNuevo)
    }

    @Test
    fun notasSinCuadernoElegidoMuestranTodasYElResumenSeRecorta() {
        val cuadernos = listOf(cuaderno("ideas", "Ideas"))
        val texto = "Una nota muy larga ".repeat(20)
        val paginas = listOf(pagina("a", "ideas", "", texto, 100))
        val datos = DatosWidgets.notas(cuadernos, paginas, null, textos)
        assertNull(datos.cuaderno)
        assertEquals("Sin título", datos.notas.first().titulo)
        assertTrue(datos.notas.first().resumen.endsWith("…"))
        assertTrue(datos.notas.first().resumen.length <= 91)
    }

    @Test
    fun notasSinCuadernos() {
        val datos = DatosWidgets.notas(emptyList(), emptyList(), null, textos)
        assertEquals(false, datos.hayCuadernos)
        assertTrue(datos.notas.isEmpty())
    }
}
