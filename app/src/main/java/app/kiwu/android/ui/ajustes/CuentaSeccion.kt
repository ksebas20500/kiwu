package app.kiwu.android.ui.ajustes

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.KiwuApp
import app.kiwu.android.R
import app.kiwu.android.data.ErrorCuenta
import kotlinx.coroutines.launch

/** El texto de un fallo de la cuenta, en el idioma de Kiwu. */
internal fun textoErrorCuenta(c: Context, e: ErrorCuenta): String = c.getString(
    when (e.codigo) {
        "EMAIL_EXISTS" -> R.string.cuenta_err_existe
        "INVALID_LOGIN_CREDENTIALS", "INVALID_PASSWORD", "EMAIL_NOT_FOUND" -> R.string.cuenta_err_credenciales
        "WEAK_PASSWORD" -> R.string.cuenta_err_clave_debil
        "INVALID_EMAIL", "MISSING_EMAIL" -> R.string.cuenta_err_correo
        "TOO_MANY_ATTEMPTS_TRY_LATER" -> R.string.cuenta_err_muchos
        "GOOGLE_CANCELADO" -> R.string.cuenta_err_cancelado
        "GOOGLE_ERROR" -> R.string.cuenta_err_google
        "DRIVE_AUTORIZACION" -> R.string.drive_err_permiso
        "DRIVE_NO_ACTIVADA" -> R.string.drive_err_api
        "DRIVE_LLENO" -> R.string.drive_err_lleno
        null -> R.string.cuenta_err_red
        else -> R.string.cuenta_err_generico
    },
)

/** «Cuenta y sincronización»: iniciar sesión, ver el estado y cerrarla. Sin cuenta, Kiwu funciona igual. */
@Composable
fun SeccionCuenta(app: KiwuApp) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val sync = app.sync
    val actividad = contexto.actividad()
    val estado by sync.estado.collectAsStateWithLifecycle()
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var ocupado by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var confirmarBorrado by remember { mutableStateOf(false) }
    var claveBorrado by remember { mutableStateOf("") }

    fun intentar(accion: suspend () -> Unit) {
        alcance.launch {
            ocupado = true
            mensaje = try {
                accion()
                null
            } catch (e: ErrorCuenta) {
                textoErrorCuenta(contexto, e)
            } finally {
                ocupado = false
            }
        }
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            !sync.cuenta.disponible -> Text(stringResource(R.string.cuenta_no_disponible), style = MaterialTheme.typography.bodyMedium)
            estado.correo == null -> {
                Text(stringResource(R.string.cuenta_texto), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    correo, { correo = it }, Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(stringResource(R.string.cuenta_correo)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                OutlinedTextField(
                    clave, { clave = it }, Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(stringResource(R.string.cuenta_clave)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                Button(onClick = { intentar { sync.entrar(correo, clave) } }, enabled = !ocupado && correo.isNotBlank() && clave.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cuenta_entrar))
                }
                OutlinedButton(onClick = { intentar { sync.registrar(correo, clave) } }, enabled = !ocupado && correo.isNotBlank() && clave.length >= 6, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cuenta_crear))
                }
                if (sync.google.disponible && actividad != null) {
                    OutlinedButton(onClick = { intentar { sync.entrarConGoogle(sync.google.token(actividad)) } }, enabled = !ocupado, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.cuenta_google))
                    }
                }
                TextButton(
                    onClick = {
                        intentar {
                            sync.cuenta.recuperar(correo)
                            mensaje = contexto.getString(R.string.cuenta_recuperar_enviado)
                        }
                    },
                    enabled = !ocupado && correo.isNotBlank(),
                ) { Text(stringResource(R.string.cuenta_olvide)) }
            }
            else -> {
                Text(estado.correo.orEmpty(), style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        estado.sincronizando -> stringResource(R.string.cuenta_sincronizando)
                        estado.limiteDiario -> stringResource(R.string.cuenta_limite)
                        estado.error != null -> textoErrorCuenta(contexto, estado.error!!)
                        estado.ultima != null -> stringResource(
                            R.string.cuenta_sincronizado,
                            DateUtils.getRelativeTimeSpanString(estado.ultima!!, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                        )
                        else -> stringResource(R.string.cuenta_sin_sincronizar)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (estado.error != null || estado.limiteDiario) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (estado.pendientes > 0) Text(stringResource(R.string.cuenta_pendientes, estado.pendientes), style = MaterialTheme.typography.bodySmall)
                Button(onClick = { alcance.launch { sync.sincronizar() } }, enabled = !estado.sincronizando, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cuenta_sincronizar_ahora))
                }
                OutlinedButton(onClick = { sync.salir() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cuenta_salir)) }
                Text(stringResource(R.string.cuenta_salir_nota), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { confirmarBorrado = true }, enabled = !ocupado) {
                    Text(stringResource(R.string.cuenta_borrar), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }

    val conGoogle = sync.cuenta.sesion?.google == true
    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text(stringResource(R.string.cuenta_borrar)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.cuenta_borrar_texto))
                    // Borrar la cuenta es una acción delicada: Firebase pide una sesión reciente, así que se confirma con la contraseña (o con Google).
                    if (!conGoogle) OutlinedTextField(
                        claveBorrado, { claveBorrado = it }, Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text(stringResource(R.string.cuenta_clave)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarBorrado = false
                        val clave = claveBorrado
                        claveBorrado = ""
                        intentar {
                            sync.borrarCuenta {
                                if (conGoogle) sync.cuenta.entrarConGoogle(sync.google.token(actividad!!))
                                else sync.cuenta.entrar(sync.cuenta.sesion?.correo.orEmpty(), clave)
                            }
                        }
                    },
                    enabled = conGoogle && actividad != null || claveBorrado.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.cuenta_borrar_confirmar), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text(stringResource(R.string.cancelar)) } },
        )
    }
}
