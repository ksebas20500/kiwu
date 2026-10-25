package app.kiwu.android.data

import app.kiwu.android.domain.Clase
import app.kiwu.android.domain.Columna
import app.kiwu.android.domain.Cuaderno
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.Documento
import app.kiwu.android.domain.GrupoLista
import app.kiwu.android.domain.Guia
import app.kiwu.android.domain.Falta
import app.kiwu.android.domain.Horario
import app.kiwu.android.domain.nuevoId
import app.kiwu.android.domain.Materia
import app.kiwu.android.domain.Pagina
import app.kiwu.android.domain.Reglas
import app.kiwu.android.domain.ReglasHorario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.kiwu.android.domain.ID_EVENTOS
import java.time.LocalDate

/**
 * Punto único de acceso a los datos para la interfaz: aplica las reglas de `domain` y guarda con el DAO.
 * `reloj` se puede sustituir en las pruebas.
 */
class Repositorio(
    private val dao: KiwuDao,
    val reglas: Reglas = Reglas(),
    private val reloj: () -> Long = System::currentTimeMillis,
) {
    /** Las listas que se ven y se eligen; la lista especial de eventos queda aparte. */
    val materias: Flow<List<Materia>> = dao.materias().map { l -> l.filter { it.id != ID_EVENTOS } }
    val deberes: Flow<List<Deber>> = dao.deberes()
    val cuadernos: Flow<List<Cuaderno>> = dao.cuadernos()
    val paginas: Flow<List<ResumenPagina>> = dao.paginas()
    val horarios: Flow<List<Horario>> = dao.horarios()
    val clases: Flow<List<Clase>> = dao.clases()
    val grupos: Flow<List<GrupoLista>> = dao.grupos()
    val faltas: Flow<List<Falta>> = dao.faltas()

    /** Cuándo se completó cada tarea (también las borradas; sin eventos): de aquí sale la racha. */
    val fechasCompletado: Flow<List<Long>> = dao.fechasCompletado()

    fun ahora(): Long = reloj()

    suspend fun fechasCompletadoAhora(): List<Long> = dao.fechasCompletadoAhora()

    suspend fun hayDatos(): Boolean = dao.hayDatos()

    /** Crea la lista de ejemplo de la guía de inicio con sus tareas. Si ya existe (aunque se borrara), no hace nada. */
    suspend fun crearListaDeEjemplo(nombre: String, tareas: List<String>): Boolean {
        val t = reloj()
        val m = Materia(id = Guia.ID_LISTA, nombre = nombre, emoji = Guia.EMOJI_LISTA, orden = dao.siguienteOrdenMateria(), creada = t, actualizada = t)
        if (dao.insertarMaterias(listOf(m)).first() == -1L) return false // ya existía (borrada): no se resucita
        dao.insertarDeberes(tareas.mapIndexed { i, titulo -> Deber(materiaId = m.id, titulo = titulo, creada = t + i, actualizada = t + i) })
        return true
    }

    // --- listas (materias) ----------------------------------------------------------------------

    /** Crea una lista; con [grupoId] nace ya dentro de esa carpeta y con [emoji] (opcional) como icono. */
    suspend fun crearMateria(nombre: String, emoji: String? = null, grupoId: String? = null): Materia {
        val t = reloj()
        val grupo = grupoId?.takeIf { dao.grupo(it) != null }
        val m = Materia(
            nombre = nombre.trim(), orden = dao.siguienteOrdenMateria(), grupoId = grupo,
            emoji = emoji?.trim()?.takeIf { it.isNotEmpty() }, creada = t, actualizada = t,
        )
        dao.guardarMateria(m)
        // Si la carpeta estaba plegada se despliega, para que se vea la lista recién creada.
        if (grupo != null) plegarGrupo(grupo, false)
        return m
    }

    suspend fun renombrarMateria(id: String, nombre: String) {
        val m = dao.materia(id) ?: return
        dao.guardarMateria(m.copy(nombre = nombre.trim(), actualizada = reloj()))
    }

    suspend fun vincularCarpeta(id: String, carpeta: String?) {
        val m = dao.materia(id) ?: return
        dao.guardarMateria(m.copy(carpeta = carpeta, actualizada = reloj()))
    }

    suspend fun materia(id: String): Materia? = dao.materia(id)

    /** Cambia el nombre y el emoji de una lista (con emoji null o vacío vuelve el icono de siempre). */
    suspend fun editarMateria(id: String, nombre: String, emoji: String?) {
        val m = dao.materia(id) ?: return
        dao.guardarMateria(m.copy(nombre = nombre.trim().ifEmpty { m.nombre }, emoji = emoji?.trim()?.takeIf { it.isNotEmpty() }, actualizada = reloj()))
    }

    /** Mete una lista en una carpeta o la deja suelta (con null). Si la carpeta no existe, queda suelta. */
    suspend fun moverMateriaAGrupo(materiaId: String, grupoId: String?) {
        val m = dao.materia(materiaId) ?: return
        val destino = grupoId?.takeIf { dao.grupo(it) != null }
        if (m.grupoId == destino) return
        dao.guardarMateria(m.copy(grupoId = destino, actualizada = reloj()))
        // La carpeta que recibe la lista se despliega, para que se vea dónde ha quedado.
        if (destino != null) plegarGrupo(destino, false)
    }

    /** Sube (-1) o baja (+1) una lista un puesto entre las de su misma carpeta (o entre las sueltas). */
    suspend fun moverOrdenMateria(id: String, delta: Int) {
        val m = dao.materia(id) ?: return
        val hermanas = dao.materias().first().filter { it.grupoId == m.grupoId }.sortedWith(compareBy<Materia> { it.orden }.thenBy { it.nombre.lowercase() })
        val i = hermanas.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in hermanas.indices) return
        val nuevas = hermanas.toMutableList().apply { add(j, removeAt(i)) }
        val t = reloj()
        dao.guardarMaterias(nuevas.mapIndexed { n, x -> x.copy(orden = n, actualizada = t) })
    }

    // --- carpetas de listas -----------------------------------------------------------------------

    suspend fun crearGrupo(nombre: String, icono: String? = null): GrupoLista {
        val t = reloj()
        val g = GrupoLista(nombre = nombre.trim(), icono = icono?.trim()?.takeIf { it.isNotEmpty() } ?: "📁", orden = dao.siguienteOrdenGrupo(), creada = t, actualizada = t)
        dao.guardarGrupo(g)
        return g
    }

    suspend fun editarGrupo(id: String, nombre: String, icono: String?) {
        val g = dao.grupo(id) ?: return
        dao.guardarGrupo(g.copy(nombre = nombre.trim().ifEmpty { g.nombre }, icono = icono?.trim()?.takeIf { it.isNotEmpty() } ?: "📁", actualizada = reloj()))
    }

    suspend fun plegarGrupo(id: String, plegado: Boolean) {
        val g = dao.grupo(id) ?: return
        if (g.plegado != plegado) dao.guardarGrupo(g.copy(plegado = plegado, actualizada = reloj()))
    }

    /** Sube (-1) o baja (+1) una carpeta un puesto. */
    suspend fun moverOrdenGrupo(id: String, delta: Int) {
        val todas = dao.gruposAhora()
        val i = todas.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in todas.indices) return
        val nuevas = todas.toMutableList().apply { add(j, removeAt(i)) }
        val t = reloj()
        dao.guardarGrupos(nuevas.mapIndexed { n, x -> x.copy(orden = n, actualizada = t) })
    }

    /** Elimina la carpeta; sus listas (y las tareas de ellas) se conservan como listas sueltas. */
    suspend fun eliminarGrupoListas(id: String) = dao.eliminarGrupoListas(id, reloj())

    suspend fun eliminarMateria(id: String) = dao.eliminarMateria(id, reloj())

    // --- tareas ---------------------------------------------------------------------------------

    /** Crea una tarea; si [materiaId] es [ID_EVENTOS] es un evento (la lista especial se crea la primera vez). */
    suspend fun crearDeber(titulo: String, materiaId: String, fecha: Long? = null): Deber {
        val t = reloj()
        val evento = materiaId == ID_EVENTOS
        if (evento && dao.materia(ID_EVENTOS) == null) {
            dao.guardarMateria(Materia(id = ID_EVENTOS, nombre = app.kiwu.android.Textos.eventos, emoji = "📅", orden = -1, creada = t, actualizada = t))
        }
        // Un evento avisa 1 hora antes por defecto (las tareas, 1 día antes).
        val d = Deber(
            materiaId = materiaId, titulo = titulo.trim(), fechaEntrega = fecha, creada = t, actualizada = t,
            recordatorioHoras = if (evento) 1 else 24,
        )
        dao.guardarDeber(d)
        return d
    }

    suspend fun deber(id: String): Deber? = dao.deber(id)

    /** Guarda cambios de una tarea que sigue existiendo (no resucita las eliminadas). */
    suspend fun actualizarDeber(d: Deber): Deber? {
        dao.deber(d.id) ?: return null
        val nuevo = d.copy(actualizada = reloj(), eliminada = null)
        dao.guardarDeber(nuevo)
        return nuevo
    }

    /** Cambios parciales: leen la fila actual y modifican solo un campo, para no pisar otros cambios en curso. */
    suspend fun cambiarTitulo(id: String, titulo: String) {
        val d = dao.deber(id) ?: return
        val limpio = titulo.trim()
        if (limpio.isNotEmpty() && limpio != d.titulo) dao.guardarDeber(d.copy(titulo = limpio, actualizada = reloj()))
    }

    suspend fun cambiarNota(id: String, nota: String) {
        val d = dao.deber(id) ?: return
        if (nota != d.nota) dao.guardarDeber(d.copy(nota = nota, actualizada = reloj()))
    }

    /** Horas de antelación del aviso (0 = sin aviso). Cambiarlo rearma el aviso. */
    suspend fun cambiarRecordatorio(id: String, horas: Int): Deber? {
        val d = dao.deber(id) ?: return null
        return d.copy(recordatorioHoras = horas, recordado = null, actualizada = reloj()).also { dao.guardarDeber(it) }
    }

    suspend fun marcar(id: String, completado: Boolean): Deber? {
        val d = dao.deber(id) ?: return null
        return reglas.marcar(d, completado, reloj()).also { dao.guardarDeber(it) }
    }

    suspend fun mover(id: String, columna: Columna): Deber? {
        val d = dao.deber(id) ?: return null
        return reglas.mover(d, columna, reloj()).also { dao.guardarDeber(it) }
    }

    suspend fun ponerFecha(id: String, fecha: Long?): Deber? {
        val d = dao.deber(id) ?: return null
        return reglas.ponerFecha(d, fecha, reloj()).also { dao.guardarDeber(it) }
    }

    suspend fun moverAMateria(id: String, materiaId: String): Deber? {
        val d = dao.deber(id) ?: return null
        return d.copy(materiaId = materiaId, actualizada = reloj()).also { dao.guardarDeber(it) }
    }

    suspend fun eliminarDeber(id: String) = dao.eliminarDeber(id, reloj())

    suspend fun deberesAhora(): List<Deber> = dao.deberesAhora()

    suspend fun marcarAvisado(id: String, cuando: Long = reloj()) = dao.marcarAvisado(id, cuando)

    // --- cuadernos y páginas --------------------------------------------------------------------

    suspend fun crearCuaderno(nombre: String, icono: String = "📓"): Cuaderno {
        val t = reloj()
        val c = Cuaderno(nombre = nombre.trim(), icono = icono, orden = dao.siguienteOrdenCuaderno(), creada = t, actualizada = t)
        dao.guardarCuaderno(c)
        return c
    }

    suspend fun editarCuaderno(id: String, nombre: String, icono: String) {
        val c = dao.cuaderno(id) ?: return
        dao.guardarCuaderno(c.copy(nombre = nombre.trim(), icono = icono, actualizada = reloj()))
    }

    suspend fun eliminarCuaderno(id: String) = dao.eliminarCuaderno(id, reloj())

    suspend fun crearPagina(cuadernoId: String): Pagina {
        val t = reloj()
        val p = Pagina(cuadernoId = cuadernoId, creada = t, actualizada = t)
        dao.guardarPagina(p)
        return p
    }

    /**
     * Crea una nota desde el widget: en el cuaderno indicado si existe, si no en el primero y, si todavía no hay
     * ninguno, en uno nuevo llamado «Notas rápidas» (el mismo que usa la pantalla de Notas).
     */
    suspend fun crearPaginaRapida(cuadernoId: String?): Pagina {
        val cuaderno = cuadernoId?.takeIf { it.isNotBlank() }?.let { dao.cuaderno(it) }
            ?: dao.cuadernos().first().firstOrNull()
            ?: crearCuaderno(app.kiwu.android.Textos.notasRapidas, "📝")
        return crearPagina(cuaderno.id)
    }

    suspend fun pagina(id: String): Pagina? = dao.pagina(id)

    /** Guarda el contenido de una página y recalcula su texto plano (para búsquedas y vistas previas). */
    suspend fun guardarContenidoPagina(id: String, titulo: String, icono: String, doc: Documento) {
        val p = dao.pagina(id) ?: return
        dao.guardarPagina(
            p.copy(titulo = titulo, icono = icono, contenido = doc.aJson(), textoPlano = doc.textoPlano(), actualizada = reloj()),
        )
    }

    suspend fun ponerFavorita(id: String, favorita: Boolean) {
        val p = dao.pagina(id) ?: return
        dao.guardarPagina(p.copy(favorita = favorita, actualizada = reloj()))
    }

    suspend fun moverPagina(id: String, cuadernoId: String) {
        val p = dao.pagina(id) ?: return
        dao.guardarPagina(p.copy(cuadernoId = cuadernoId, actualizada = reloj()))
    }

    suspend fun eliminarPagina(id: String) = dao.eliminarPagina(id, reloj())

    // --- horarios -------------------------------------------------------------------------------

    suspend fun crearHorario(nombre: String, icono: String = "🎓"): Horario {
        val t = reloj()
        val h = Horario(nombre = nombre.trim(), icono = icono, orden = dao.siguienteOrdenHorario(), creada = t, actualizada = t)
        dao.guardarHorario(h)
        return h
    }

    suspend fun editarHorario(id: String, nombre: String, icono: String) {
        val h = dao.horario(id) ?: return
        dao.guardarHorario(h.copy(nombre = nombre.trim(), icono = icono, actualizada = reloj()))
    }

    suspend fun eliminarHorario(id: String) = dao.eliminarHorario(id, reloj())

    suspend fun clase(id: String): Clase? = dao.clase(id)

    suspend fun horarioNombre(id: String): String? = dao.horario(id)?.nombre

    suspend fun clasesAhora(): List<Clase> = dao.clasesAhora()

    /** Clases (una por día) que comparten un grupo: son las de una misma clase repetida varios días. */
    suspend fun clasesDeGrupo(grupo: String): List<Clase> = dao.clasesDeGrupo(grupo)

    /**
     * Crea o edita una clase en varios días a la vez. Con [grupo] existente se actualizan sus días, se añaden los
     * nuevos y se eliminan (borrado lógico) los que se quitaron. Devuelve el grupo.
     */
    suspend fun guardarClase(
        horarioId: String,
        grupo: String?,
        titulo: String,
        materiaId: String?,
        dias: Set<Int>,
        inicioMin: Int,
        finMin: Int,
        aula: String,
        notas: String,
        avisoMin: Int?,
        color: String? = null,
        maxFaltas: Int? = null,
    ): String {
        require(dias.isNotEmpty() && dias.all { it in 1..7 }) { "Elige al menos un día de la semana" }
        require(ReglasHorario.valida(inicioMin, finMin)) { "La hora de fin debe ser posterior a la de inicio" }
        val t = reloj()
        val g = grupo ?: nuevoId()
        val actuales = dao.clasesDeGrupo(g).associateBy { it.diaSemana }
        val filas = dias.sorted().map { dia ->
            val previa = actuales[dia]
            (previa ?: Clase(horarioId = horarioId, grupo = g, titulo = titulo, diaSemana = dia, inicioMin = inicioMin, finMin = finMin, creada = t, actualizada = t)).copy(
                horarioId = horarioId, materiaId = materiaId, titulo = titulo.trim(), inicioMin = inicioMin, finMin = finMin,
                aula = aula.trim(), notas = notas.trim(), avisoMin = avisoMin, color = ReglasHorario.colorValido(color),
                maxFaltas = maxFaltas?.takeIf { it > 0 }, actualizada = t,
            )
        }
        dao.guardarClases(filas)
        val quitadas = actuales.filterKeys { it !in dias }.values.map { it.id }
        if (quitadas.isNotEmpty()) dao.eliminarClases(quitadas, t)
        return g
    }

    /** Vincula (o desvincula, con null) a una lista de tareas todas las filas de una clase. */
    suspend fun vincularGrupo(grupo: String, materiaId: String?) {
        val t = reloj()
        dao.guardarClases(dao.clasesDeGrupo(grupo).map { it.copy(materiaId = materiaId, actualizada = t) })
    }

    suspend fun eliminarGrupo(grupo: String) = dao.eliminarGrupo(grupo, reloj())

    // --- faltas de clase ------------------------------------------------------------------------

    suspend fun faltasAhora(): List<Falta> = dao.faltasAhora()

    /** Marca una falta de la materia ([grupo]) el día [dia] (por defecto, hoy). Un mismo día solo cuenta una vez. */
    suspend fun marcarFalta(grupo: String, horarioId: String, dia: LocalDate = LocalDate.now(), justificada: Boolean = false): Falta {
        dao.faltasAhora().firstOrNull { it.grupo == grupo && it.dia == dia.toEpochDay() }?.let { return it }
        val f = Falta(grupo = grupo, horarioId = horarioId, dia = dia.toEpochDay(), justificada = justificada, creada = reloj())
        dao.guardarFalta(f)
        return f
    }

    suspend fun quitarFalta(id: String) = dao.quitarFalta(id)

    suspend fun justificarFalta(id: String, justificada: Boolean) = dao.justificarFalta(id, justificada)

    /** Máximo de faltas de la materia, en todas sus filas; null o 0 = sin límite. */
    suspend fun ponerMaximoFaltas(grupo: String, maximo: Int?) = dao.ponerMaximoFaltas(grupo, maximo?.takeIf { it > 0 }, reloj())

    /** Borra todas las faltas de un horario (al empezar un semestre nuevo). */
    suspend fun vaciarFaltas(horarioId: String) = dao.vaciarFaltas(horarioId)
}
