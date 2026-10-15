package app.kiwu.android.ui.horario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kiwu.android.data.Repositorio
import app.kiwu.android.domain.Clase
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
) {
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

    val estado: StateFlow<HorarioEstado> = combine(repo.horarios, repo.clases, repo.materias, seleccion, reloj) { horarios, clases, materias, sel, ahora ->
        // Si el horario elegido ya no existe, se vuelve a ver todos.
        val actual = sel?.takeIf { id -> horarios.any { it.id == id } }
        HorarioEstado(horarios, clases, materias, actual, ahora, cargado = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HorarioEstado())

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
        inicioMin: Int, finMin: Int, aula: String, notas: String, avisoMin: Int?,
    ) = viewModelScope.launch {
        repo.guardarClase(horarioId, grupo, titulo, materiaId, dias, inicioMin, finMin, aula, notas, avisoMin)
    }

    fun eliminarClase(grupo: String) = viewModelScope.launch { repo.eliminarGrupo(grupo) }

    /** Choques de una clase (nueva o editada) con las ya existentes en cualquier horario. */
    fun choques(estado: HorarioEstado, grupo: String?, dias: Set<Int>, inicioMin: Int, finMin: Int): List<Clase> =
        if (!ReglasHorario.valida(inicioMin, finMin)) emptyList() else ReglasHorario.choques(estado.todas, grupo, dias, inicioMin, finMin)
}
