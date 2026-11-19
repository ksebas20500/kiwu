package app.kiwu.android

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import app.kiwu.android.data.AjustesStore
import app.kiwu.android.data.BillingManager
import app.kiwu.android.data.KiwuDb
import app.kiwu.android.data.Repositorio
import app.kiwu.android.data.ServicioSync
import app.kiwu.android.domain.TiempoEstudio
import app.kiwu.android.notify.Avisos
import app.kiwu.android.notify.AvisosAndroid
import app.kiwu.android.domain.Rachas
import app.kiwu.android.notify.AvisosClases
import app.kiwu.android.notify.AvisosRacha
import app.kiwu.android.notify.AvisosRepaso
import app.kiwu.android.notify.AvisosResumen
import app.kiwu.android.notify.ProgramadorAvisos
import app.kiwu.android.notify.ProgramadorClases
import app.kiwu.android.notify.ProgramadorRacha
import app.kiwu.android.notify.ProgramadorRepaso
import app.kiwu.android.notify.ProgramadorResumen
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

/** Contenedor de dependencias de la app (sin framework de inyección: todo sale de aquí — db, repo, ajustes, sync, billing…). */
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
    val billing by lazy { BillingManager(this) }
    private val programadorAvisos by lazy { ProgramadorAvisos(this) }

    /** Alcance que sobrevive a las pantallas: para guardar lo pendiente cuando se cierra una (p. ej. una nota). */
    val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun avisosPermitidos(): Boolean = Avisos.tienePermiso(this)

    private var avisosIniciados = false
    private var widgetsIniciados = false
    private var observadorCalendario: ContentObserver? = null
    private var refrescoCalendarioPendiente: Job? = null

    /** Refresca los widgets sin romper la app si no hay lanzador o es una prueba JVM. */
    private fun refrescarWidgets(programar: Boolean = false) {
        alcance.launch {
            try {
                Widgets.refrescar(this@KiwuApp)
                if (programar) Widgets.programar(this@KiwuApp)
            } catch (_: Exception) {
                // Sin widgets o en pruebas: no debe romper la app.
            }
        }
    }

    // En Android 8–12, el idioma elegido en Ajustes también para lo que no es una pantalla (fechas por defecto, servicios).
    override fun attachBaseContext(base: android.content.Context) = super.attachBaseContext(Idioma.contexto(base))

    override fun onCreate() {
        super.onCreate()
        Textos.actualizar(this)
        iniciarAvisos()
        iniciarWidgets()
        iniciarObservadorCalendario()
        sync.iniciar()
        importarSesionesViejas()
    }

    /**
     * Las sesiones de estudio vivían en preferencias hasta 0.8.2: pasan a la base una sola vez (con marca propia en
     * las preferencias, así un respaldo restaurado no las duplica) y el temporizador ya solo escribe en Room.
     */
    private fun importarSesionesViejas() {
        alcance.launch {
            val p = getSharedPreferences("temporizador", Context.MODE_PRIVATE)
            if (p.getBoolean("sesiones_importadas", false)) return@launch
            val viejas = TiempoEstudio.leer(p.getString("sesiones", "").orEmpty())
            if (viejas.isNotEmpty()) {
                repo.guardarSesionesImportadas(viejas)
            }
            p.edit().putBoolean("sesiones_importadas", true).remove("sesiones").apply()
        }
    }

    /** Con el tema «sistema», el widget tiene que seguir al modo claro/oscuro del teléfono sin esperar 30 minutos. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!widgetsIniciados) return
        refrescarWidgets()
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
        // Resumen semanal: una alarma cada domingo a la tarde.
        AvisosResumen.crearCanal(this)
        val programadorResumen = ProgramadorResumen(this)
        alcance.launch { ajustes.flujo.map { it.resumenSemanal }.distinctUntilChanged().collect { programadorResumen.reconciliar(it) } }
        // Repaso diario: una alarma a las 18:00 si hay tarjetas vencidas.
        AvisosRepaso.crearCanal(this)
        val programadorRepaso = ProgramadorRepaso(this)
        alcance.launch { ajustes.flujo.map { it.repasoAviso }.distinctUntilChanged().collect { programadorRepaso.reconciliar(it) } }
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
                ProgramadorRepaso(this@KiwuApp).reconciliar(a.repasoAviso)
            } catch (_: Exception) {
                // Sin base o sin permiso de notificaciones: no debe romper la app.
            }
        }
        // Red de seguridad de los widgets: si el proceso estuvo muerto, el observador del calendario
        // no corrió; al volver, se re-registra (por si el permiso llegó después de arrancar) y se
        // redibuja todo con lo que haya en el teléfono, borrando los eventos borrados mientras tanto.
        iniciarObservadorCalendario()
        refrescarWidgets()
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
                refrescarWidgets(programar = true)
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
                    refrescarWidgets()
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
