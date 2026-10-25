package app.kiwu.android

import android.app.Application
import android.content.res.Configuration
import app.kiwu.android.data.AjustesStore
import app.kiwu.android.data.KiwuDb
import app.kiwu.android.data.Repositorio
import app.kiwu.android.notify.Avisos
import app.kiwu.android.domain.Rachas
import app.kiwu.android.notify.AvisosClases
import app.kiwu.android.notify.AvisosRacha
import app.kiwu.android.notify.ProgramadorAvisos
import app.kiwu.android.notify.ProgramadorClases
import app.kiwu.android.notify.ProgramadorRacha
import app.kiwu.android.widgets.Widgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/** Contenedor de dependencias de la app (sin framework de inyección: son solo dos objetos). */
class KiwuApp : Application() {
    val db by lazy { KiwuDb.obtener(this) }
    val repo by lazy { Repositorio(db.dao()) }
    val ajustes by lazy { AjustesStore(this) }

    /** Alcance que sobrevive a las pantallas: para guardar lo pendiente cuando se cierra una (p. ej. una nota). */
    val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun avisosPermitidos(): Boolean = Avisos.tienePermiso(this)

    private var avisosIniciados = false
    private var widgetsIniciados = false

    // En Android 8–12, el idioma elegido en Ajustes también para lo que no es una pantalla (fechas por defecto, servicios).
    override fun attachBaseContext(base: android.content.Context) = super.attachBaseContext(Idioma.contexto(base))

    override fun onCreate() {
        super.onCreate()
        Textos.actualizar(this)
        iniciarAvisos()
        iniciarWidgets()
    }

    /** Con el tema «sistema», el widget tiene que seguir al modo claro/oscuro del teléfono sin esperar 30 minutos. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!widgetsIniciados) return
        alcance.launch {
            try {
                Widgets.refrescar(this@KiwuApp)
            } catch (_: Exception) {
                // Sin widgets o en pruebas: no debe romper la app.
            }
        }
    }

    /** Mantiene las alarmas de los recordatorios al día con las tareas (se llama al arrancar y tras reiniciar el teléfono). */
    fun iniciarAvisos() {
        if (avisosIniciados) return
        avisosIniciados = true
        Avisos.crearCanal(this)
        AvisosClases.crearCanal(this)
        val programador = ProgramadorAvisos(this)
        alcance.launch {
            kotlinx.coroutines.flow.combine(repo.deberes, ajustes.flujo.map { it.segundoAvisoMin }.distinctUntilChanged()) { deberes, segundo -> deberes to segundo }
                .collect { (deberes, segundo) -> programador.reconciliar(deberes, segundo) }
        }
        val programadorClases = ProgramadorClases(this)
        alcance.launch { repo.clases.collect { programadorClases.reconciliar(it) } }
        AvisosRacha.crearCanal(this)
        val programadorRacha = ProgramadorRacha(this)
        alcance.launch { ajustes.flujo.map { it.rachaAviso }.distinctUntilChanged().collect { programadorRacha.reconciliar(it) } }
        // Si se completa una tarea después del aviso de la racha, el aviso sobra.
        alcance.launch {
            repo.fechasCompletado.collect { if (Rachas.calcular(it, System.currentTimeMillis()).hoyCumplido) AvisosRacha.quitar(this@KiwuApp) }
        }
    }

    /**
     * Mantiene los widgets al día: cualquier cambio en listas, tareas, notas, cuadernos, horarios o ajustes los vuelve a
     * dibujar (con un pequeño retardo para no repetir trabajo al escribir), y programa el próximo refresco por reloj
     * (cambio de clase o medianoche).
     */
    private fun iniciarWidgets() {
        if (widgetsIniciados) return
        widgetsIniciados = true
        alcance.launch {
            merge(
                repo.deberes.map { }, repo.materias.map { }, repo.grupos.map { }, repo.paginas.map { },
                repo.cuadernos.map { }, repo.horarios.map { }, repo.clases.map { }, repo.faltas.map { }, ajustes.flujo.map { },
            ).collectLatest {
                delay(350)
                try {
                    Widgets.refrescar(this@KiwuApp)
                    Widgets.programar(this@KiwuApp)
                } catch (_: Exception) {
                    // Sin lanzador o en las pruebas JVM no hay widgets que refrescar; no debe romper la app.
                }
            }
        }
    }
}
