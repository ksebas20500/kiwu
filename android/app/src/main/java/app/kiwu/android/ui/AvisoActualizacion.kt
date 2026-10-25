package app.kiwu.android.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.EstadoActualizacion
import app.kiwu.android.KiwuApp
import app.kiwu.android.R

/**
 * Ventana «Hay una versión nueva» al abrir o volver a Kiwu, y «Reiniciar» cuando ya se descargó. Va por encima de cualquier
 * pantalla, porque a los testers Google Play no siempre les avisa de las versiones nuevas.
 */
@Composable
fun AvisoActualizacion(app: KiwuApp) {
    val contexto = LocalContext.current
    val actualizaciones = app.actualizaciones
    val estado by actualizaciones.estado.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { actualizaciones.comprobar() }
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        // Cancelar en la ventana de Google cuenta como «Más tarde».
        if (r.resultCode != Activity.RESULT_OK) actualizaciones.posponer()
    }
    when (estado) {
        is EstadoActualizacion.Disponible -> AlertDialog(
            onDismissRequest = { actualizaciones.posponer() },
            icon = { Icon(Icons.Filled.SystemUpdate, null) },
            title = { Text(stringResource(R.string.actualizacion_nueva_titulo)) },
            text = { Text(stringResource(R.string.actualizacion_nueva_texto)) },
            confirmButton = {
                TextButton(onClick = { if (!actualizaciones.actualizar(lanzador)) abrirEnPlay(contexto) }) { Text(stringResource(R.string.actualizacion_actualizar)) }
            },
            dismissButton = { TextButton(onClick = { actualizaciones.posponer() }) { Text(stringResource(R.string.actualizacion_mas_tarde)) } },
        )
        EstadoActualizacion.Lista -> AlertDialog(
            onDismissRequest = { actualizaciones.ocultar() },
            icon = { Icon(Icons.Filled.RestartAlt, null) },
            title = { Text(stringResource(R.string.actualizacion_lista_titulo)) },
            text = { Text(stringResource(R.string.actualizacion_lista_texto)) },
            confirmButton = { TextButton(onClick = { actualizaciones.reiniciar() }) { Text(stringResource(R.string.actualizacion_reiniciar)) } },
            dismissButton = { TextButton(onClick = { actualizaciones.ocultar() }) { Text(stringResource(R.string.actualizacion_luego)) } },
        )
        else -> Unit
    }
}

/** Abre la ficha de Kiwu en Google Play (o en el navegador si no está la Play Store). */
fun abrirEnPlay(contexto: Context) {
    val paquete = contexto.packageName
    val play = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$paquete")).setPackage("com.android.vending")
    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$paquete"))
    try {
        contexto.startActivity(play.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        try { contexto.startActivity(web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
    }
}
