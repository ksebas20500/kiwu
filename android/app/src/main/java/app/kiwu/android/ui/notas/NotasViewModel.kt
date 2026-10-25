package app.kiwu.android.ui.notas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kiwu.android.data.Repositorio
import app.kiwu.android.data.ResumenPagina
import app.kiwu.android.domain.Cuaderno
import app.kiwu.android.domain.Documento
import app.kiwu.android.domain.Pagina
import app.kiwu.android.ui.editor.sinTildes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Qué notas se ven: todas, solo las favoritas o las de un cuaderno. */
sealed interface FiltroNotas {
    data object Todas : FiltroNotas
    data object Favoritas : FiltroNotas
    data class DeCuaderno(val id: String) : FiltroNotas
}

data class NotasEstado(
    val cuadernos: List<Cuaderno> = emptyList(),
    val filtro: FiltroNotas = FiltroNotas.Todas,
    val busqueda: String = "",
    val paginas: List<ResumenPagina> = emptyList(),
    val cargado: Boolean = false,
) {
    val cuadernoActual: Cuaderno? get() = (filtro as? FiltroNotas.DeCuaderno)?.let { f -> cuadernos.firstOrNull { it.id == f.id } }
    val nombresCuadernos: Map<String, String> get() = cuadernos.associate { it.id to it.nombre }
}

class NotasViewModel(private val repo: Repositorio) : ViewModel() {
    private val filtro = MutableStateFlow<FiltroNotas>(FiltroNotas.Todas)
    private val busqueda = MutableStateFlow("")

    val estado: StateFlow<NotasEstado> = combine(repo.cuadernos, repo.paginas, filtro, busqueda) { cuadernos, paginas, f, q ->
        val actual = if (f is FiltroNotas.DeCuaderno && cuadernos.none { it.id == f.id }) FiltroNotas.Todas else f
        val consulta = sinTildes(q.trim())
        val visibles = paginas.filter { p ->
            val dentro = when (actual) {
                FiltroNotas.Todas -> true
                FiltroNotas.Favoritas -> p.favorita
                is FiltroNotas.DeCuaderno -> p.cuadernoId == actual.id
            }
            dentro && (consulta.isEmpty() || consulta in sinTildes(p.titulo) || consulta in sinTildes(p.textoPlano))
        }
        NotasEstado(cuadernos, actual, q, visibles, cargado = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotasEstado())

    fun filtrar(f: FiltroNotas) { filtro.value = f }
    fun buscar(texto: String) { busqueda.value = texto }

    fun crearCuaderno(nombre: String, icono: String) = viewModelScope.launch {
        val c = repo.crearCuaderno(nombre, icono.ifBlank { "📓" })
        filtro.value = FiltroNotas.DeCuaderno(c.id)
    }

    fun editarCuaderno(id: String, nombre: String, icono: String) = viewModelScope.launch { repo.editarCuaderno(id, nombre, icono.ifBlank { "📓" }) }

    fun eliminarCuaderno(id: String) = viewModelScope.launch {
        if ((filtro.value as? FiltroNotas.DeCuaderno)?.id == id) filtro.value = FiltroNotas.Todas
        repo.eliminarCuaderno(id)
    }

    /** Crea una página en el cuaderno elegido (o en «Notas rápidas», que se crea si no hay ninguno) y devuelve su id. */
    suspend fun nuevaPagina(): String {
        val e = estado.value
        val cuaderno = e.cuadernoActual ?: e.cuadernos.firstOrNull() ?: repo.crearCuaderno(app.kiwu.android.Textos.notasRapidas, "📝")
        return repo.crearPagina(cuaderno.id).id
    }

    fun ponerFavorita(id: String, favorita: Boolean) = viewModelScope.launch { repo.ponerFavorita(id, favorita) }
    fun moverPagina(id: String, cuadernoId: String) = viewModelScope.launch { repo.moverPagina(id, cuadernoId) }
    fun eliminarPagina(id: String) = viewModelScope.launch { repo.eliminarPagina(id) }
}

/** Estado de la pantalla de una página: se carga una vez y guarda con retraso. */
class PaginaViewModel(private val repo: Repositorio, private val alcanceApp: CoroutineScope, val id: String) : ViewModel() {
    val pagina = MutableStateFlow<Pagina?>(null)
    val cuadernos = repo.cuadernos
    val titulo = MutableStateFlow("")
    val icono = MutableStateFlow("")
    private var documento: Documento? = null
    private val sucio = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            val p = repo.pagina(id)
            if (p != null) {
                titulo.value = p.titulo
                icono.value = p.icono
                documento = Documento.deJson(p.contenido)
            }
            pagina.value = p
        }
    }

    @OptIn(FlowPreview::class)
    private val guardado = viewModelScope.launch {
        sucio.debounce(500).collect { if (it > 0) guardarAhoraEnVm() }
    }

    fun cambioTitulo(t: String) { titulo.value = t; sucio.update { it + 1 } }
    fun cambioIcono(i: String) { icono.value = i; sucio.update { it + 1 } }
    fun cambioDocumento(d: Documento) { documento = d; sucio.update { it + 1 } }

    private suspend fun guardarAhoraEnVm() {
        val d = documento ?: return
        repo.guardarContenidoPagina(id, titulo.value, icono.value, d)
    }

    /** Al salir de la pantalla: guarda lo pendiente (o elimina la página si quedó vacía y sin título). */
    fun alSalir() {
        val d = documento
        val t = titulo.value
        val vacia = t.isBlank() && (d == null || d.textoPlano().isBlank() && d.bloques.all { it.kind == "paragraph" })
        alcanceApp.launch {
            if (vacia) repo.eliminarPagina(id) else if (d != null && sucio.value > 0) repo.guardarContenidoPagina(id, t, icono.value, d)
        }
    }

    fun favorita(f: Boolean) = viewModelScope.launch {
        repo.ponerFavorita(id, f)
        pagina.update { it?.copy(favorita = f) }
    }

    fun mover(cuadernoId: String) = viewModelScope.launch {
        repo.moverPagina(id, cuadernoId)
        pagina.update { it?.copy(cuadernoId = cuadernoId) }
    }

    fun eliminar() = alcanceApp.launch { repo.eliminarPagina(id) }
}
