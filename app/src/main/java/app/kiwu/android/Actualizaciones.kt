package app.kiwu.android

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface EstadoActualizacion {
    data object Nada : EstadoActualizacion
    data class Disponible(val version: Int) : EstadoActualizacion
    data object Descargando : EstadoActualizacion
    data object Lista : EstadoActualizacion
}

/** Cuándo volver a ofrecer una versión que se dejó para más tarde. */
object ReglasActualizacion {
    const val ESPERA_MS = 24 * 60 * 60 * 1000L

    fun debeOfrecer(version: Int, pospuesta: Int, pospuestaEn: Long, ahora: Long): Boolean =
        version != pospuesta || ahora - pospuestaEn >= ESPERA_MS
}

/**
 * Actualización dentro de la app con Google Play (modo flexible): si hay una versión nueva se ofrece, se descarga mientras se sigue
 * usando Kiwu y al terminar se pide reiniciar. Solo funciona con la app instalada desde Play (también en las pistas de prueba);
 * en otro caso Google no responde y no se muestra nada.
 */
class Actualizaciones(
    contexto: Context,
    private val gestor: AppUpdateManager = AppUpdateManagerFactory.create(contexto),
    private val reloj: () -> Long = System::currentTimeMillis,
) {
    private val prefs = contexto.getSharedPreferences("kiwu", Context.MODE_PRIVATE)
    private val _estado = MutableStateFlow<EstadoActualizacion>(EstadoActualizacion.Nada)
    val estado: StateFlow<EstadoActualizacion> = _estado
    private var info: AppUpdateInfo? = null

    private val oyente = InstallStateUpdatedListener { s ->
        _estado.value = when (s.installStatus()) {
            InstallStatus.PENDING, InstallStatus.DOWNLOADING -> EstadoActualizacion.Descargando
            InstallStatus.DOWNLOADED -> EstadoActualizacion.Lista
            else -> EstadoActualizacion.Nada
        }
    }

    init {
        gestor.registerListener(oyente)
    }

    /**
     * Pregunta a Google Play. Con [forzar] se ofrece aunque se haya dejado para más tarde. [alTerminar] recibe el estado, o null si
     * Google no respondió (sin conexión o app no instalada desde Play).
     */
    fun comprobar(forzar: Boolean = false, alTerminar: (EstadoActualizacion?) -> Unit = {}) {
        gestor.appUpdateInfo
            .addOnSuccessListener { i ->
                info = i
                val nuevo = when {
                    i.installStatus() == InstallStatus.DOWNLOADED -> EstadoActualizacion.Lista
                    i.installStatus() == InstallStatus.DOWNLOADING || i.installStatus() == InstallStatus.PENDING -> EstadoActualizacion.Descargando
                    i.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && i.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) &&
                        (forzar || ReglasActualizacion.debeOfrecer(i.availableVersionCode(), prefs.getInt(CLAVE_VERSION, -1), prefs.getLong(CLAVE_CUANDO, 0), reloj())) ->
                        EstadoActualizacion.Disponible(i.availableVersionCode())
                    else -> EstadoActualizacion.Nada
                }
                _estado.value = nuevo
                alTerminar(nuevo)
            }
            .addOnFailureListener { alTerminar(null) }
    }

    /** Abre la ventana de Google Play para descargar. Devuelve false si no se pudo (entonces conviene abrir la ficha de Play). */
    fun actualizar(lanzador: ActivityResultLauncher<IntentSenderRequest>): Boolean {
        val i = info ?: return false
        info = null // Google no deja usar la misma respuesta dos veces.
        _estado.value = EstadoActualizacion.Nada
        return gestor.startUpdateFlowForResult(i, lanzador, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())
    }

    /** «Más tarde»: no vuelve a preguntar por esta versión hasta dentro de un día. */
    fun posponer() {
        val e = _estado.value
        val version = (e as? EstadoActualizacion.Disponible)?.version ?: info?.availableVersionCode() ?: -1
        prefs.edit().putInt(CLAVE_VERSION, version).putLong(CLAVE_CUANDO, reloj()).apply()
        if (e is EstadoActualizacion.Disponible) _estado.value = EstadoActualizacion.Nada
    }

    /** La descarga terminó y se deja reiniciar para luego: se volverá a pedir al volver a la app. */
    fun ocultar() {
        if (_estado.value == EstadoActualizacion.Lista) _estado.value = EstadoActualizacion.Nada
    }

    /** Instala la versión descargada; Google Play cierra y vuelve a abrir la app. */
    fun reiniciar() {
        gestor.completeUpdate()
    }

    private companion object {
        const val CLAVE_VERSION = "actualizacion_pospuesta"
        const val CLAVE_CUANDO = "actualizacion_pospuesta_en"
    }
}
