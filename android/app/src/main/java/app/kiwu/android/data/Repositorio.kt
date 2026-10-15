package app.kiwu.android.data

import app.kiwu.android.domain.Clase
import app.kiwu.android.domain.Columna
import app.kiwu.android.domain.Cuaderno
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.Documento
import app.kiwu.android.domain.Horario
import app.kiwu.android.domain.nuevoId
import app.kiwu.android.domain.Materia
import app.kiwu.android.domain.Pagina
import app.kiwu.android.domain.Reglas
import app.kiwu.android.domain.ReglasHorario
import kotlinx.coroutines.flow.Flow

/**
 * Punto único de acceso a los datos para la interfaz: aplica las reglas de `domain` y guarda con el DAO.
 * `reloj` se puede sustituir en las pruebas.
 */
class Repositorio(
    private val dao: KiwuDao,
    val reglas: Reglas = Reglas(),
    private val reloj: () -> Long = System::currentTimeMillis,
) {
    val materias: Flow<List<Materia>> = dao.materias()
    val deberes: Flow<List<Deber>> = dao.deberes()
    val cuadernos: Flow<List<Cuaderno>> = dao.cuadernos()
    val paginas: Flow<List<ResumenPagina>> = dao.paginas()
    val horarios: Flow<List<Horario>> = dao.horarios()
    val clases: Flow<List<Clase>> = dao.clases()

    fun ahora(): Long = reloj()

    // --- listas (materias) ----------------------------------------------------------------------

    suspend fun crearMateria(nombre: String): Materia {
        val t = reloj()
        val m = Materia(nombre = nombre.trim(), orden = dao.siguienteOrdenMateria(), creada = t, actualizada = t)
        dao.guardarMateria(m)
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

    suspend fun eliminarMateria(id: String) = dao.eliminarMateria(id, reloj())

    // --- tareas ---------------------------------------------------------------------------------

    suspend fun crearDeber(titulo: String, materiaId: String, fecha: Long? = null): Deber {
        val t = reloj()
        val d = Deber(materiaId = materiaId, titulo = titulo.trim(), fechaEntrega = fecha, creada = t, actualizada = t)
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
                aula = aula.trim(), notas = notas.trim(), avisoMin = avisoMin, actualizada = t,
            )
        }
        dao.guardarClases(filas)
        val quitadas = actuales.filterKeys { it !in dias }.values.map { it.id }
        if (quitadas.isNotEmpty()) dao.eliminarClases(quitadas, t)
        return g
    }

    suspend fun eliminarGrupo(grupo: String) = dao.eliminarGrupo(grupo, reloj())
}
