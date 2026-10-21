package app.kiwu.android

import android.app.Application
import app.kiwu.android.data.AjustesStore
import app.kiwu.android.data.KiwuDb
import app.kiwu.android.data.Repositorio
import app.kiwu.android.notify.Avisos
import app.kiwu.android.notify.AvisosClases
import app.kiwu.android.notify.ProgramadorAvisos
import app.kiwu.android.notify.ProgramadorClases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Contenedor de dependencias de la app (sin framework de inyección: son solo dos objetos). */
class KiwuApp : Application() {
    val db by lazy { KiwuDb.obtener(this) }
    val repo by lazy { Repositorio(db.dao()) }
    val ajustes by lazy { AjustesStore(this) }
    val compras by lazy { Compras(this) }

    /** Alcance que sobrevive a las pantallas: para guardar lo pendiente cuando se cierra una (p. ej. una nota). */
    val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun avisosPermitidos(): Boolean = Avisos.tienePermiso(this)

    private var avisosIniciados = false

    override fun onCreate() {
        super.onCreate()
        iniciarAvisos()
    }

    /** Mantiene las alarmas de los recordatorios al día con las tareas (se llama al arrancar y tras reiniciar el teléfono). */
    fun iniciarAvisos() {
        if (avisosIniciados) return
        avisosIniciados = true
        Avisos.crearCanal(this)
        AvisosClases.crearCanal(this)
        val programador = ProgramadorAvisos(this)
        alcance.launch { repo.deberes.collect { programador.reconciliar(it) } }
        val programadorClases = ProgramadorClases(this)
        alcance.launch { repo.clases.collect { programadorClases.reconciliar(it) } }
    }
}
