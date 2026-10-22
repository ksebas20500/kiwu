package app.kiwu.android.ui.tareas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kiwu.android.data.AjustesStore
import app.kiwu.android.data.Repositorio
import app.kiwu.android.domain.Columna
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.Elemento
import app.kiwu.android.domain.GrupoLista
import app.kiwu.android.domain.MenuListas
import app.kiwu.android.domain.NodoMenu
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
    /** Carpetas de listas y el menú ya armado (carpetas con sus listas, y las listas sueltas). */
    val grupos: List<GrupoLista> = emptyList(),
    val menu: List<NodoMenu> = emptyList(),
    /** Ajustes del menú: aspecto (plegable | compacto | color) y si se ven los contadores. */
    val aspecto: String = "plegable",
    val contadores: Boolean = true,
) {
    val grupoActual: GrupoLista? get() = (elemento as? Elemento.DeGrupo)?.let { e -> grupos.firstOrNull { it.id == e.id } }
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

    private val datos = combine(repo.materias, repo.grupos, repo.deberes) { materias, grupos, deberes -> Triple(materias, grupos, deberes) }

    val estado: StateFlow<TareasEstado> = combine(
        datos, seleccion, ajustes.flujo, reloj,
    ) { (materias, grupos, deberes), (elemento, sub), aj, ahora ->
        // Si la lista o la carpeta elegida ya no existe, se vuelve a «Todas las tareas».
        val actual = when {
            elemento is Elemento.DeMateria && materias.none { it.id == elemento.id } -> Elemento.Todos
            elemento is Elemento.DeGrupo && grupos.none { it.id == elemento.id } -> Elemento.Todos
            else -> elemento
        }
        // Las tareas de una carpeta son las de sus listas: se filtran y se tratan como «todas».
        val deLaCarpeta = (actual as? Elemento.DeGrupo)?.let { MenuListas.listasDe(materias, it.id) }
        val base = if (deLaCarpeta != null) deberes.filter { it.materiaId in deLaCarpeta } else deberes
        val efectivo = if (deLaCarpeta != null) Elemento.Todos else actual
        val items = listOf(Elemento.Hoy, Elemento.Proximos, Elemento.Todos) + materias.map { Elemento.DeMateria(it.id) }
        val cuentas = items.associateWith { repo.reglas.conteo(deberes, it, ahora) }
        TareasEstado(
            materias = materias, deberes = deberes, elemento = actual, subvista = sub, vista = aj.vistaTareas,
            ahora = ahora,
            pendientes = repo.reglas.pendientes(base, efectivo, ahora),
            completados = repo.reglas.completados(base, efectivo, ahora),
            cuentas = cuentas,
            cargado = true,
            grupos = grupos,
            menu = MenuListas.construir(materias, grupos, materias.associate { it.id to (cuentas[Elemento.DeMateria(it.id)] ?: 0) }, aj.menuOrden),
            aspecto = aj.menuAspecto, contadores = aj.menuContadores,
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

    // --- carpetas de listas ---------------------------------------------------------------------

    fun crearGrupo(nombre: String) = viewModelScope.launch { repo.crearGrupo(nombre) }

    /** Crea una carpeta y mete en ella una lista. */
    fun crearGrupoYMover(nombre: String, materiaId: String) = viewModelScope.launch {
        val g = repo.crearGrupo(nombre)
        repo.moverMateriaAGrupo(materiaId, g.id)
    }

    fun editarGrupo(id: String, nombre: String, icono: String, color: String?) = viewModelScope.launch { repo.editarGrupo(id, nombre, icono, color) }

    fun plegarGrupo(id: String, plegado: Boolean) = viewModelScope.launch { repo.plegarGrupo(id, plegado) }

    fun eliminarGrupo(id: String) = viewModelScope.launch {
        if ((seleccion.value.first as? Elemento.DeGrupo)?.id == id) ir(Elemento.Todos)
        repo.eliminarGrupoListas(id)
    }

    fun moverAGrupo(materiaId: String, grupoId: String?) = viewModelScope.launch { repo.moverMateriaAGrupo(materiaId, grupoId) }

    fun moverOrdenLista(id: String, delta: Int) = viewModelScope.launch { repo.moverOrdenMateria(id, delta) }

    fun moverOrdenGrupo(id: String, delta: Int) = viewModelScope.launch { repo.moverOrdenGrupo(id, delta) }

    fun editarAspectoLista(id: String, emoji: String?, color: String?) = viewModelScope.launch { repo.editarAspectoMateria(id, emoji, color) }

    fun vincularCarpeta(id: String, carpeta: String?) = viewModelScope.launch { repo.vincularCarpeta(id, carpeta) }
}
