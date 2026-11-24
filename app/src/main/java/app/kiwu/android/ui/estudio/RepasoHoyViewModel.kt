package app.kiwu.android.ui.estudio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kiwu.android.data.Repositorio
import app.kiwu.android.domain.Repaso
import app.kiwu.android.domain.TarjetaDeNota
import app.kiwu.android.domain.Tarjetas
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lo que muestra «Repasar hoy»: la cola de todas las notas y cuántas tarjetas y exámenes la forman. */
data class RepasoHoyEstado(
    val cola: List<TarjetaDeNota> = emptyList(),
    /** Notas distintas con tarjetas pendientes. */
    val notas: Int = 0,
    /** Exámenes cercanos que están dando prioridad (título y días que faltan). */
    val examenes: List<Pair<String, Int>> = emptyList(),
    val cargado: Boolean = false,
)

/**
 * Cola global de repaso: junta las tarjetas vencidas de todas las notas y guarda la caja de cada una al contestarla.
 * La prioridad por examen la calcula [RepasoGlobal] con los deberes y las materias.
 */
class RepasoHoyViewModel(private val repo: Repositorio) : ViewModel() {

    val estado: StateFlow<RepasoHoyEstado> = repo.tarjetasParaRepasar.map { cola ->
        RepasoHoyEstado(
            cola = cola,
            notas = cola.distinctBy { it.paginaId }.size,
            examenes = cola.mapNotNull { t -> t.examen?.let { it to (t.diasExamen ?: 0) } }.distinct(),
            cargado = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RepasoHoyEstado())

    /** Vuelve a calcular «vencidas» con la hora actual (por si el teléfono estuvo abierto desde ayer). */
    fun refrescar() = repo.refrescoRepaso.update { it + 1 }

    /** Contesta una tarjeta: guarda su caja nueva en la nota de la que salió. */
    fun contestar(t: TarjetaDeNota, sabida: Boolean) = viewModelScope.launch {
        val p = repo.pagina(t.paginaId) ?: return@launch
        val lista = Tarjetas.leer(p.tarjetas).toMutableList()
        val i = lista.indexOfFirst { it.pregunta == t.tarjeta.pregunta && it.respuesta == t.tarjeta.respuesta }
        if (i < 0) return@launch
        val ahora = System.currentTimeMillis()
        lista[i] = if (sabida) Repaso.acertada(lista[i], ahora) else Repaso.fallada(lista[i], ahora)
        repo.cambiarTarjetas(t.paginaId, lista)
    }
}
