package app.kiwu.android.ui.horario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kiwu.android.data.Repositorio
import app.kiwu.android.domain.Clase
import app.kiwu.android.domain.ContadorFaltas
import app.kiwu.android.domain.EstadoFaltas
import app.kiwu.android.domain.Falta
import app.kiwu.android.domain.ReglasFaltas
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.Horario
import app.kiwu.android.domain.Materia
import app.kiwu.android.domain.ReglasHorario
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HorarioEstado(
    val horarios: List<Horario> = emptyList(),
    /** Todas las clases de todos los horarios (para detectar choques). */
    val todas: List<Clase> = emptyList(),
    val materias: List<Materia> = emptyList(),
    /** Id del horario que se ve, o null para ver todos juntos. */
    val seleccion: String? = null,
    val ahora: Long = 0L,
    val cargado: Boolean = false,
    /** Todas las faltas (de todos los horarios), agrupadas luego por materia (`grupo`). */
    val faltas: List<Falta> = emptyList(),
) {
    /** Contador de faltas de cada materia, por grupo. */
    val contadores: Map<String, ContadorFaltas> by lazy { ReglasFaltas.contadores(todas, faltas) }

    fun faltasDe(grupo: String): List<Falta> = ReglasFaltas.ordenadas(faltas.filter { it.grupo == grupo })

    /** Materias del horario que se ve que ya llegaron al máximo de faltas (una por grupo). */
    val enRiesgo: List<Clase> get() = clases.distinctBy { it.grupo }.filter { contadores[it.grupo]?.estado == EstadoFaltas.Superado }.sortedBy { it.titulo }

    /** Las clases del horario elegido (o de todos). */
    val clases: List<Clase> get() = if (seleccion == null) todas else todas.filter { it.horarioId == seleccion }
    val horarioActual: Horario? get() = horarios.firstOrNull { it.id == seleccion }
    val nombres: Map<String, String> get() = horarios.associate { it.id to it.nombre }

    /** Día de la semana (1 = lunes) y minuto del día ahora mismo. */
    fun diaYMinuto(zona: ZoneId = ZoneId.systemDefault()): Pair<Int, Int> {
        val f = Instant.ofEpochMilli(ahora).atZone(zona)
        return f.dayOfWeek.value to f.hour * 60 + f.minute
    }
}

class HorarioViewModel(private val repo: Repositorio) : ViewModel() {
    private val seleccion = MutableStateFlow<String?>(null)
    private val reloj = flow {
        while (true) {
            emit(repo.ahora())
            delay(30_000)
        }
    }

    val estado: StateFlow<HorarioEstado> = combine(
        combine(repo.horarios, repo.clases, repo.materias, ::Triple), seleccion, reloj, repo.faltas,
    ) { (horarios, clases, materias), sel, ahora, faltas ->
        // Si el horario elegido ya no existe, se vuelve a ver todos.
        val actual = sel?.takeIf { id -> horarios.any { it.id == id } }
        HorarioEstado(horarios, clases, materias, actual, ahora, cargado = true, faltas = faltas)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HorarioEstado())

    /** Todas las tareas (para los puntos de las clases y la hoja de tareas). */
    val deberes: StateFlow<List<Deber>> = repo.deberes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun marcarTarea(id: String, hecha: Boolean) = viewModelScope.launch { repo.marcar(id, hecha) }

    fun crearTarea(titulo: String, materiaId: String, fecha: Long?) = viewModelScope.launch { repo.crearDeber(titulo, materiaId, fecha) }

    fun vincular(grupo: String, materiaId: String?) = viewModelScope.launch { repo.vincularGrupo(grupo, materiaId) }

    /** Crea una lista de tareas con ese nombre y la vincula a la clase. */
    fun crearListaYVincular(grupo: String, nombre: String) = viewModelScope.launch {
        val m = repo.crearMateria(nombre.trim())
        repo.vincularGrupo(grupo, m.id)
    }

    fun elegir(id: String?) {
        seleccion.value = id
    }

    fun crearHorario(nombre: String, icono: String) = viewModelScope.launch {
        val h = repo.crearHorario(nombre, icono)
        seleccion.value = h.id
    }

    fun editarHorario(id: String, nombre: String, icono: String) = viewModelScope.launch { repo.editarHorario(id, nombre, icono) }

    fun eliminarHorario(id: String) = viewModelScope.launch { repo.eliminarHorario(id) }

    fun guardarClase(
        horarioId: String, grupo: String?, titulo: String, materiaId: String?, dias: Set<Int>,
        inicioMin: Int, finMin: Int, aula: String, notas: String, avisoMin: Int?, color: String? = null, maxFaltas: Int? = null,
    ) = viewModelScope.launch {
        repo.guardarClase(horarioId, grupo, titulo, materiaId, dias, inicioMin, finMin, aula, notas, avisoMin, color, maxFaltas)
    }

    fun marcarFalta(c: Clase, dia: LocalDate) = viewModelScope.launch { repo.marcarFalta(c.grupo, c.horarioId, dia) }

    fun quitarFalta(id: String) = viewModelScope.launch { repo.quitarFalta(id) }

    fun justificarFalta(id: String, justificada: Boolean) = viewModelScope.launch { repo.justificarFalta(id, justificada) }

    fun ponerMaximoFaltas(grupo: String, maximo: Int?) = viewModelScope.launch { repo.ponerMaximoFaltas(grupo, maximo) }

    fun vaciarFaltas(horarioId: String) = viewModelScope.launch { repo.vaciarFaltas(horarioId) }

    fun eliminarClase(grupo: String) = viewModelScope.launch { repo.eliminarGrupo(grupo) }

    /** Choques de una clase (nueva o editada) con las ya existentes en cualquier horario. */
    fun choques(estado: HorarioEstado, grupo: String?, dias: Set<Int>, inicioMin: Int, finMin: Int): List<Clase> =
        if (!ReglasHorario.valida(inicioMin, finMin)) emptyList() else ReglasHorario.choques(estado.todas, grupo, dias, inicioMin, finMin)
}
