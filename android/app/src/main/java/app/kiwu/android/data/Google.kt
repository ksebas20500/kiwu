package app.kiwu.android.data

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private suspend fun <T> Task<T>.esperar(): T = suspendCancellableCoroutine { c ->
    addOnSuccessListener { c.resume(it) }
    addOnFailureListener { c.resumeWithException(it) }
}

/** El selector de cuentas de Google del teléfono: devuelve el token que Firebase canjea por una sesión. */
class InicioGoogle(private val clienteWeb: String) {
    val disponible: Boolean get() = clienteWeb.isNotEmpty()

    suspend fun token(actividad: Activity): String {
        val peticion = GetCredentialRequest.Builder().addCredentialOption(GetSignInWithGoogleOption.Builder(clienteWeb).build()).build()
        val credencial = try {
            CredentialManager.create(actividad).getCredential(actividad, peticion).credential
        } catch (e: GetCredentialCancellationException) {
            throw ErrorCuenta("GOOGLE_CANCELADO", "Cancelado")
        } catch (e: GetCredentialException) {
            throw ErrorCuenta("GOOGLE_ERROR", e.message ?: "Google")
        }
        if (credencial !is CustomCredential || credencial.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw ErrorCuenta("GOOGLE_ERROR", "Credencial inesperada")
        }
        return GoogleIdTokenCredential.createFrom(credencial.data).idToken
    }
}

/** Permiso para Google Drive: o ya hay token, o hay que pedirle el consentimiento a la persona (una sola vez). */
sealed interface PermisoDrive {
    data class Listo(val token: String) : PermisoDrive
    data class Pedir(val intencion: PendingIntent) : PermisoDrive
}

/**
 * Acceso a Drive con el alcance `drive.file`: Kiwu solo ve los archivos que él mismo creó, nunca el resto del Drive.
 * Funciona con cualquier cuenta de Google del teléfono, haya o no sesión de Kiwu.
 */
object AutorizacionDrive {
    const val ALCANCE = "https://www.googleapis.com/auth/drive.file"

    suspend fun pedir(actividad: Activity): PermisoDrive {
        val r = try {
            Identity.getAuthorizationClient(actividad)
                .authorize(AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(ALCANCE))).build()).esperar()
        } catch (e: Exception) {
            throw ErrorCuenta("DRIVE_AUTORIZACION", e.message ?: "Drive")
        }
        val intencion = r.pendingIntent
        return if (r.hasResolution() && intencion != null) PermisoDrive.Pedir(intencion)
        else PermisoDrive.Listo(r.accessToken ?: throw ErrorCuenta("DRIVE_AUTORIZACION", "Sin token"))
    }

    /** El resultado de la pantalla de consentimiento. */
    fun token(actividad: Activity, datos: Intent?): String =
        try {
            Identity.getAuthorizationClient(actividad).getAuthorizationResultFromIntent(datos).accessToken
        } catch (e: Exception) {
            null
        } ?: throw ErrorCuenta("DRIVE_AUTORIZACION", "Sin token")
}
