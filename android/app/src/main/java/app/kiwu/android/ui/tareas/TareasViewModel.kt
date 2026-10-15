package app.kiwu.android.ui.tareas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kiwu.android.data.AjustesStore
import app.kiwu.android.data.Repositorio
import app.kiwu.android.domain.Columna
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.Elemento
import app.kiwu.android.domain.Materia
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class TareasEstado(
    val materias: List<Materia> = emptyList(),
    val deberes: List<Deber> = emptyList(),
    val elemento: Elemento = Elemento.Hoy,
    val subvista: String = "tareas",
    val vista: String = "lista",
    val ahora: Long = 0L,
    val pendientes: List<Deber> = emptyList(),
    val completados: List<Deber> = emptyList(),
    val cuentas: Map<Elemento, Int> = emptyMap(),
    val cargado: Boolean = false,
) {
    val materiaActual: Materia? get() = (elemento as? Elemento.DeMateria)?.let { e -> materias.firstOrNull { it.id == e.id } }
    val nombres: Map<String, String> get() = materias.associate { it.id to it.nombre }
}

class TareasViewModel(private val repo: Repositorio, private val ajustes: AjustesStore) : ViewModel() {
    /** Lo que se está mirando: el elemento del menú y, dentro de una lista, la pestaña (tareas | archivos). */
    private val seleccion = MutableStateFlow<Pair<Elemento, String>>(Elemento.Hoy to "tareas")
    private val reloj = flow {
        while (true) {
            emit(repo.ahora())
            delay(60_000)
        }
    }
    val reglas get() = repo.reglas

    val estado: StateFlow<TareasEstado> = combine(
        repo.materias, repo.deberes, seleccion, ajustes.flujo, reloj,
    ) { materias, deberes, (elemento, sub), aj, ahora ->
        // Si la lista elegida ya no existe, se vuelve a «Todas las tareas».
        val actual = if (elemento is Elemento.DeMateria && materias.none { it.id == elemento.id }) Elemento.Todos else elemento
        val items = listOf(Elemento.Hoy, Elemento.Proximos, Elemento.Todos) + materias.map { Elemento.DeMateria(it.id) }
        TareasEstado(
            materias = materias, deberes = deberes, elemento = actual, subvista = sub, vista = aj.vistaTareas,
            ahora = ahora,
            pendientes = repo.reglas.pendientes(deberes, actual, ahora),
            completados = repo.reglas.completados(deberes, actual, ahora),
            cuentas = items.associateWith { repo.reglas.conteo(deberes, it, ahora) },
            cargado = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TareasEstado())

    fun ir(e: Elemento) {
        seleccion.value = e to "tareas"
    }

    fun cambiarSubvista(s: String) {
        seleccion.update { it.first to s }
    }

    fun cambiarVista(vista: String) = viewModelScope.launch { ajustes.cambiar { it.copy(vistaTareas = vista) } }

    /** En «Hoy» las tareas nuevas vencen hoy a las 18:00, como en las versiones de escritorio. */
    fun crearTarea(titulo: String, materiaId: String, fechaElegida: Long? = null) = viewModelScope.launch {
        val fecha = if (fechaElegida != null) fechaElegida else if (seleccion.value.first == Elemento.Hoy) {
            LocalDate.now().atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else null
        repo.crearDeber(titulo, materiaId, fecha)
    }

    fun crearTareaEnDia(titulo: String, materiaId: String, dia: LocalDate, fechaElegida: Long? = null) = viewModelScope.launch {
        val fecha = fechaElegida ?: dia.atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        repo.crearDeber(titulo, materiaId, fecha)
    }

    fun marcar(id: String, completado: Boolean) = viewModelScope.launch { repo.marcar(id, completado) }

    fun mover(id: String, columna: Columna) = viewModelScope.launch { repo.mover(id, columna) }

    fun moverAMateria(id: String, materiaId: String) = viewModelScope.launch { repo.moverAMateria(id, materiaId) }

    fun eliminarTarea(id: String) = viewModelScope.launch { repo.eliminarDeber(id) }

    fun crearLista(nombre: String) = viewModelScope.launch {
        val m = repo.crearMateria(nombre)
        ir(Elemento.DeMateria(m.id))
    }

    fun renombrarLista(id: String, nombre: String) = viewModelScope.launch { repo.renombrarMateria(id, nombre) }

    fun eliminarLista(id: String) = viewModelScope.launch {
        if ((seleccion.value.first as? Elemento.DeMateria)?.id == id) ir(Elemento.Todos)
        repo.eliminarMateria(id)
    }

    fun vincularCarpeta(id: String, carpeta: String?) = viewModelScope.launch { repo.vincularCarpeta(id, carpeta) }
}
