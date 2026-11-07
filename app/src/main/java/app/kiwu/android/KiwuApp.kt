package app.kiwu.android

import android.app.Application
import android.content.res.Configuration
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import app.kiwu.android.data.AjustesStore
import app.kiwu.android.data.KiwuDb
import app.kiwu.android.data.Repositorio
import app.kiwu.android.data.ServicioSync
import app.kiwu.android.notify.Avisos
import app.kiwu.android.notify.AvisosAndroid
import app.kiwu.android.domain.Rachas
import app.kiwu.android.notify.AvisosClases
import app.kiwu.android.notify.AvisosRacha
import app.kiwu.android.notify.ProgramadorAvisos
import app.kiwu.android.notify.ProgramadorClases
import app.kiwu.android.notify.ProgramadorRacha
import app.kiwu.android.widgets.Widgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/** Contenedor de dependencias de la app (sin framework de inyección: son solo dos objetos). */
class KiwuApp : Application() {
    val db by lazy { KiwuDb.obtener(this) }
    private val prefsRacha by lazy { getSharedPreferences("racha", MODE_PRIVATE) }
    val repo by lazy {
        val banco = prefsRacha.getStringSet("banco", emptySet()).orEmpty().mapNotNull { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }.toSet()
        Repositorio(db.dao(), bancoRacha = kotlinx.coroutines.flow.MutableStateFlow(banco), avisos = AvisosAndroid(this))
    }
    val ajustes by lazy { AjustesStore(this) }
    val sync by lazy { ServicioSync(this) }
    val actualizaciones by lazy { Actualizaciones(this) }
    private val programadorAvisos by lazy { ProgramadorAvisos(this) }

    /** Alcance que sobrevive a las pantallas: para guardar lo pendiente cuando se cierra una (p. ej. una nota). */
    val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun avisosPermitidos(): Boolean = Avisos.tienePermiso(this)

    private var avisosIniciados = false
    private var widgetsIniciados = false
    private var observadorCalendario: ContentObserver? = null
    private var refrescoCalendarioPendiente: Job? = null

    // En Android 8–12, el idioma elegido en Ajustes también para lo que no es una pantalla (fechas por defecto, servicios).
    override fun attachBaseContext(base: android.content.Context) = super.attachBaseContext(Idioma.contexto(base))

    override fun onCreate() {
        super.onCreate()
        Textos.actualizar(this)
        com.google.android.gms.ads.MobileAds.initialize(this) {}
        iniciarAvisos()
        iniciarWidgets()
        iniciarObservadorCalendario()
        sync.iniciar()
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
        val programador = programadorAvisos
        alcance.launch {
            kotlinx.coroutines.flow.combine(
                repo.deberes,
                ajustes.flujo.map { it.segundoAvisoMin to it.horaAvisoSinHora }.distinctUntilChanged(),
            ) { deberes, (segundo, horaSinHora) -> Triple(deberes, segundo, horaSinHora) }
                .collect { (deberes, segundo, horaSinHora) -> programador.reconciliar(deberes, segundo, horaSinHora = horaSinHora) }
        }
        // Los hábitos (tareas que se repiten) vuelven a empezar cada día.
        alcance.launch { repo.deberes.collect { repo.reiniciarRepetidas() } }
        val programadorClases = ProgramadorClases(this)
        alcance.launch { repo.clases.collect { programadorClases.reconciliar(it) } }
        AvisosRacha.crearCanal(this)
        val programadorRacha = ProgramadorRacha(this)
        alcance.launch { ajustes.flujo.map { it.rachaAviso }.distinctUntilChanged().collect { programadorRacha.reconciliar(it) } }
        // Si se completa una tarea después del aviso de la racha, el aviso sobra.
        alcance.launch {
            repo.entradaRacha.collect {
                val r = Rachas.calcular(it, System.currentTimeMillis())
                if (!r.enRiesgo) AvisosRacha.quitar(this@KiwuApp)
                // Los días cumplidos se guardan: la racha solo sube con los días y baja si hoy se desmarca algo.
                if (r.dias != it.banco) {
                    repo.bancoRacha.value = r.dias
                    prefsRacha.edit().putStringSet("banco", r.dias.map(java.time.LocalDate::toString).toSet()).apply()
                }
            }
        }
    }

    /**
     * Red de seguridad al volver a la app: vuelve a cuadrar alarmas y barra con la base. Cubre los cambios hechos
     * fuera de una pantalla (widget, hoja del día, notificación) cuando el proceso pudo morir antes de que el flujo
     * de la base reaccionara.
     */
    fun revisarAvisos() {
        alcance.launch {
            try {
                val a = ajustes.flujo.first()
                programadorAvisos.reconciliar(repo.deberesAhora(), a.segundoAvisoMin, horaSinHora = a.horaAvisoSinHora)
            } catch (_: Exception) {
                // Sin base o sin permiso de notificaciones: no debe romper la app.
            }
        }
        // Red de seguridad de los widgets: si el proceso estuvo muerto, el observador del calendario
        // no corrió; al volver, se re-registra (por si el permiso llegó después de arrancar) y se
        // redibuja todo con lo que haya en el teléfono, borrando los eventos borrados mientras tanto.
        iniciarObservadorCalendario()
        alcance.launch {
            try {
                Widgets.refrescar(this@KiwuApp)
            } catch (_: Exception) {
                // Sin widgets o en pruebas: no debe romper la app.
            }
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

    /**
     * Los widgets enseñan eventos del calendario del teléfono (donde Google Calendar sincroniza los suyos), pero el
     * proveedor no avisa por sí solo: sin este observador, un evento borrado o editado en Google seguía pintado en el
     * widget hasta el próximo cambio interno o hasta la alarma del próximo cambio de clase.
     *
     * Se escuchan tanto los eventos como sus instancias expandidas, y el refresco va con retraso corto: las
     * sincronizaciones del sistema llegan en ráfagas y no hay que redibujar los widgets por cada cambio.
     */
    private fun iniciarObservadorCalendario() {
        if (observadorCalendario != null) return
        val observador = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                refrescoCalendarioPendiente?.cancel()
                refrescoCalendarioPendiente = alcance.launch {
                    delay(1000)
                    try {
                        Widgets.refrescar(this@KiwuApp)
                    } catch (_: Exception) {
                        // Sin widgets o en pruebas: no debe romper la app.
                    }
                }
            }
        }
        try {
            if (!app.kiwu.android.data.CalendarioTelefono.permitido(this)) return // sin permiso no hay eventos que vigilar
            contentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI, true, observador)
            contentResolver.registerContentObserver(CalendarContract.Instances.CONTENT_URI, true, observador)
            observadorCalendario = observador
        } catch (_: Exception) {
            // Proveedor ausente: el refresco por alarma cubre el resto.
        }
    }
}
