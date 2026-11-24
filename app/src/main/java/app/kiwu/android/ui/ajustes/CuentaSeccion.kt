package app.kiwu.android.ui.ajustes

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
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
    val estado by sync.estado.collectAsStateWithLifecycle()
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var modoRegistro by rememberSaveable { mutableStateOf(false) }
    var mostrarClave by remember { mutableStateOf(false) }
    var intentoseRegistro by remember { mutableStateOf(false) }
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

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when {
            !sync.cuenta.disponible -> {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.cuenta_no_disponible),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            estado.correo == null -> {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            stringResource(R.string.cuenta_texto),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        if (sync.google.disponible) {
                            Button(
                                onClick = {
                                    val act = contexto.actividad()
                                    if (act != null) {
                                        intentar { sync.entrarConGoogle(sync.google.token(act)) }
                                    }
                                },
                                enabled = !ocupado,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.cuenta_google))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                )
                                Text(
                                    stringResource(R.string.ajustes_o_con_correo),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                )
                            }
                        }

                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = !modoRegistro,
                                onClick = { modoRegistro = false },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            ) {
                                Text(
                                    stringResource(R.string.cuenta_entrar),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            SegmentedButton(
                                selected = modoRegistro,
                                onClick = { modoRegistro = true },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            ) {
                                Text(
                                    stringResource(R.string.cuenta_crear),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        OutlinedTextField(
                            value = correo,
                            onValueChange = { correo = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(stringResource(R.string.cuenta_correo)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        )

                        val errorClave = modoRegistro && intentoseRegistro && clave.length < 6
                        OutlinedTextField(
                            value = clave,
                            onValueChange = { clave = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = errorClave,
                            label = { Text(stringResource(R.string.cuenta_clave)) },
                            visualTransformation = if (mostrarClave) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { mostrarClave = !mostrarClave }) {
                                    Icon(
                                        if (mostrarClave) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = null,
                                    )
                                }
                            },
                            supportingText = if (errorClave) {
                                { Text(stringResource(R.string.ajustes_min_caracteres), color = MaterialTheme.colorScheme.error) }
                            } else null,
                        )

                        Button(
                            onClick = {
                                if (modoRegistro) {
                                    intentoseRegistro = true
                                    if (clave.length >= 6) {
                                        intentar { sync.registrar(correo, clave) }
                                    }
                                } else {
                                    intentar { sync.entrar(correo, clave) }
                                }
                            },
                            enabled = !ocupado && correo.isNotBlank() && clave.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(if (modoRegistro) R.string.cuenta_crear_boton else R.string.cuenta_entrar))
                        }

                        TextButton(
                            onClick = {
                                intentar {
                                    sync.cuenta.recuperar(correo)
                                    mensaje = contexto.getString(R.string.cuenta_recuperar_enviado)
                                }
                            },
                            enabled = !ocupado && correo.isNotBlank(),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text(stringResource(R.string.cuenta_olvide))
                        }
                    }
                }
            }
            else -> {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
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

                        Button(
                            onClick = { alcance.launch { sync.sincronizar() } },
                            enabled = !estado.sincronizando,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.cuenta_sincronizar_ahora))
                        }
                        OutlinedButton(
                            onClick = { sync.salir() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.cuenta_salir))
                        }
                        Text(
                            stringResource(R.string.cuenta_salir_nota),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(
                            onClick = { confirmarBorrado = true },
                            enabled = !ocupado,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text(stringResource(R.string.cuenta_borrar), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 4.dp)) }
    }

    val conGoogle = sync.cuenta.sesion?.google == true
    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text(stringResource(R.string.cuenta_borrar)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.cuenta_borrar_texto))
                    if (!conGoogle) OutlinedTextField(
                        value = claveBorrado,
                        onValueChange = { claveBorrado = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
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
                                val act = contexto.actividad()
                                if (conGoogle && act != null) sync.cuenta.entrarConGoogle(sync.google.token(act))
                                else sync.cuenta.entrar(sync.cuenta.sesion?.correo.orEmpty(), clave)
                            }
                        }
                    },
                    enabled = (conGoogle && contexto.actividad() != null) || claveBorrado.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.cuenta_borrar_confirmar), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text(stringResource(R.string.cancelar)) } },
        )
    }
}
