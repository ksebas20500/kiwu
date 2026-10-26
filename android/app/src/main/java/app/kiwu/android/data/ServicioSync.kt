package app.kiwu.android.data

import androidx.room.InvalidationTracker
import app.kiwu.android.BuildConfig
import app.kiwu.android.KiwuApp
import app.kiwu.android.domain.MotorSync
import app.kiwu.android.domain.RelojHibrido
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/** Lo que muestra la pantalla «Cuenta». */
data class EstadoSync(
    val correo: String? = null,
    val sincronizando: Boolean = false,
    val ultima: Long? = null,
    val pendientes: Int = 0,
    val limiteDiario: Boolean = false,
    val error: ErrorCuenta? = null,
)

/**
 * Mantiene al día los dispositivos de una misma cuenta: sube los cambios unos segundos después de hacerlos (para
 * agruparlos), mira si hay novedades al abrir la app y cada minuto mientras está a la vista. Sin sesión no hace nada y
 * la app funciona igual que siempre.
 */
class ServicioSync(private val app: KiwuApp) {
    val cuenta = CuentaFirebase(BuildConfig.FIREBASE_API_KEY, AlmacenSesion(app))
    val google = InicioGoogle(BuildConfig.GOOGLE_WEB_CLIENT_ID)
    private val almacen by lazy { AlmacenRoom(app.db) }
    private val nube = NubeFirestore(BuildConfig.FIREBASE_PROJECT_ID, cuenta)
    private val reloj by lazy { RelojHibrido(almacen.dispositivo) }
    private val cierre = Mutex()
    private val peticiones = Channel<Unit>(Channel.CONFLATED)
    private var sondeo: Job? = null
    private var iniciado = false

    private val _estado = MutableStateFlow(EstadoSync(correo = cuenta.sesion?.correo))
    val estado: StateFlow<EstadoSync> = _estado

    /** Se llama una vez al arrancar la app. */
    fun iniciar() {
        // Sin sesión no se toca la base: quien no usa la cuenta no paga nada en el arranque.
        if (iniciado || cuenta.sesion == null) return
        iniciado = true
        app.db.invalidationTracker.addObserver(object : InvalidationTracker.Observer(SyncSql.tablas.toTypedArray()) {
            override fun onInvalidated(tables: Set<String>) { peticiones.trySend(Unit) }
        })
        app.alcance.launch {
            for (p in peticiones) {
                delay(3_000) // varios cambios seguidos se envían juntos
                if (cuenta.sesion != null) sincronizar()
            }
        }
        // Al abrir la app ya sincroniza alPrimerPlano: pedirlo aquí también gastaría una consulta vacía.
    }

    /**
     * La app pasó a primer plano: se mira si hay novedades al abrir y después cada minuto; si nadie más cambió nada la espera
     * se duplica hasta [ESPERA_MAX_MS] (cada consulta, aunque vuelva vacía, cuenta como una lectura de la cuota diaria). Con la
     * app oculta no se gasta cuota ni batería.
     */
    fun alPrimerPlano() {
        if (!cuenta.disponible || sondeo?.isActive == true) return
        sondeo = app.alcance.launch {
            var espera = ESPERA_MIN_MS
            while (true) {
                val novedades = cuenta.sesion != null && sincronizar()
                espera = if (novedades) ESPERA_MIN_MS else minOf(espera * 2, ESPERA_MAX_MS)
                delay(espera)
            }
        }
    }

    fun alSegundoPlano() {
        sondeo?.cancel()
        sondeo = null
    }

    /** Devuelve si bajó algo nuevo de otro dispositivo (lo usa el sondeo para espaciar las consultas). */
    suspend fun sincronizar(): Boolean {
        if (cuenta.sesion == null || !cierre.tryLock()) return false
        try {
            _estado.update { it.copy(sincronizando = true, error = null) }
            val r = MotorSync(nube, almacen, reloj).sincronizar()
            _estado.update {
                it.copy(
                    ultima = if (r.limiteDiario) it.ultima else System.currentTimeMillis(),
                    limiteDiario = r.limiteDiario, pendientes = almacen.pendientesTotal(),
                )
            }
            return r.bajadas > 0
        } catch (e: ErrorCuenta) {
            _estado.update { it.copy(error = e) }
        } catch (e: Exception) {
            _estado.update { it.copy(error = ErrorCuenta(null, e.message ?: "Error")) }
        } finally {
            _estado.update { it.copy(sincronizando = false, correo = cuenta.sesion?.correo) }
            cierre.unlock()
        }
        return false
    }

    private companion object {
        const val ESPERA_MIN_MS = 60_000L
        const val ESPERA_MAX_MS = 300_000L
    }

    private suspend fun alIniciarSesion() {
        val s = cuenta.sesion ?: return
        // Una cuenta distinta a la de antes empieza de cero en el servidor: se vuelve a subir todo lo de este dispositivo.
        if (almacen.cuentaUid != s.uid) {
            almacen.reiniciar()
            almacen.cuentaUid = s.uid
        }
        _estado.update { it.copy(correo = s.correo) }
        iniciar()
        sincronizar()
    }

    suspend fun entrar(correo: String, clave: String) { cuenta.entrar(correo, clave); alIniciarSesion() }

    suspend fun registrar(correo: String, clave: String) { cuenta.registrar(correo, clave); alIniciarSesion() }

    suspend fun entrarConGoogle(tokenGoogle: String) { cuenta.entrarConGoogle(tokenGoogle); alIniciarSesion() }

    /** Cierra la sesión; lo que hay en el dispositivo no se toca. */
    fun salir() {
        cuenta.salir()
        _estado.value = EstadoSync()
    }

    /** Borra los datos de la nube y la cuenta. Lo local se queda. */
    suspend fun borrarCuenta(reautenticar: suspend () -> Unit) {
        // Primero se comprueba la identidad (contraseña o Google): si falla no se borra nada. Además renueva la sesión, que Firebase exige reciente.
        reautenticar()
        nube.borrarTodo()
        cuenta.borrar()
        almacen.cuentaUid = null
        _estado.value = EstadoSync()
    }
}
