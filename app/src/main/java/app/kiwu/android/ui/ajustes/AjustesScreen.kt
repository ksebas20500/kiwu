package app.kiwu.android.ui.ajustes

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.BuildConfig
import app.kiwu.android.EstadoActualizacion
import app.kiwu.android.Idioma
import app.kiwu.android.KiwuApp
import app.kiwu.android.R
import app.kiwu.android.data.AjustesUi
import app.kiwu.android.data.AutorizacionDrive
import app.kiwu.android.data.CalendarioTelefono
import app.kiwu.android.data.Copia
import app.kiwu.android.data.ErrorCopia
import app.kiwu.android.data.ErrorCuenta
import app.kiwu.android.data.MotivoErrorCopia
import app.kiwu.android.data.PermisoDrive
import app.kiwu.android.data.RespaldoDrive
import app.kiwu.android.data.TareasGoogle
import app.kiwu.android.notify.Avisos
import app.kiwu.android.notify.ProgramadorAvisos
import app.kiwu.android.ui.Acentos
import app.kiwu.android.ui.FondoArchivos
import app.kiwu.android.ui.FondoEnVivo
import app.kiwu.android.ui.abrirEnPlay
import app.kiwu.android.ui.tonoYSaturacion
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (titulo.isNotBlank()) Text(titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        contenido()
    }
}

private fun textoErrorCopia(contexto: android.content.Context, e: ErrorCopia): String = when (e.motivo) {
    MotivoErrorCopia.Ilegible -> contexto.getString(R.string.copia_ilegible, e.detalle.orEmpty())
    MotivoErrorCopia.NoEsKiwu -> contexto.getString(R.string.copia_no_es_kiwu)
    MotivoErrorCopia.MasNueva -> contexto.getString(R.string.copia_nueva)
}

/** Ajustes: apariencia, fondo, avisos del sistema y copia de seguridad. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AjustesScreen(app: KiwuApp, seccionInicial: String? = null, alVolver: () -> Unit, alVerGuia: () -> Unit = {}) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val ajustes by app.ajustes.flujo.collectAsStateWithLifecycle(AjustesUi())
    var mensaje by remember { mutableStateOf<String?>(null) }
    fun cambiar(f: (AjustesUi) -> AjustesUi) { alcance.launch { app.ajustes.cambiar(f) } }
    var elegirIdioma by remember { mutableStateOf(false) }

    var encuadrando by remember { mutableStateOf(false) }
    val elegirFondo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) alcance.launch {
            val guardada = withContext(Dispatchers.IO) { FondoArchivos.guardarOrigen(contexto, uri) }
            if (guardada) encuadrando = true else mensaje = contexto.getString(R.string.ajustes_error_imagen)
        }
    }
    val exportar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) alcance.launch {
            mensaje = try {
                val texto = Copia.exportar(app.db.dao())
                withContext(Dispatchers.IO) { contexto.contentResolver.openOutputStream(uri)?.use { it.write(texto.toByteArray(Charsets.UTF_8)) } }
                contexto.getString(R.string.ajustes_copia_guardada)
            } catch (e: Exception) {
                contexto.getString(R.string.ajustes_copia_error, e.message)
            }
        }
    }
    val permisoCalendario = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        cambiar { it.copy(calendarioGoogle = ok) }
    }
    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) alcance.launch {
            mensaje = try {
                val texto = withContext(Dispatchers.IO) { contexto.contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) } ?: "" }
                val r = Copia.importar(app.db.dao(), texto)
                textoImportado(contexto.resources, r)
            } catch (e: ErrorCopia) {
                textoErrorCopia(contexto, e)
            } catch (e: Exception) {
                contexto.getString(R.string.ajustes_importar_error, e.message)
            }
        }
    }

    val actividad = contexto.actividad()
    var ocupadoDrive by remember { mutableStateOf(false) }
    var pendienteDrive by remember { mutableStateOf<(suspend (RespaldoDrive) -> String)?>(null) }
    suspend fun correrDrive(accion: suspend () -> String?) {
        ocupadoDrive = true
        val texto = try {
            accion()
        } catch (e: ErrorCuenta) {
            textoErrorCuenta(contexto, e)
        } catch (e: ErrorCopia) {
            textoErrorCopia(contexto, e)
        } catch (e: Exception) {
            contexto.getString(R.string.ajustes_importar_error, e.message)
        }
        ocupadoDrive = false
        if (texto != null) mensaje = texto
    }
    val consentimientoDrive = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        val accion = pendienteDrive
        pendienteDrive = null
        if (accion != null && actividad != null) alcance.launch { correrDrive { accion(RespaldoDrive(AutorizacionDrive.token(actividad, r.data))) } }
    }
    suspend fun traerTareasGoogle(token: String?) {
        val ok = TareasGoogle.actualizar(contexto, token)
        mensaje = if (ok) null else contexto.getString(R.string.google_tareas_error)
        cambiar { it.copy(tareasGoogle = ok) }
    }
    val consentimientoTareas = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        alcance.launch { traerTareasGoogle(actividad?.let { TareasGoogle.token(it, r.data) }) }
    }
    fun activarTareasGoogle() {
        alcance.launch {
            try {
                when (val p = TareasGoogle.pedir(contexto)) {
                    is PermisoDrive.Listo -> traerTareasGoogle(p.token)
                    is PermisoDrive.Pedir -> consentimientoTareas.launch(IntentSenderRequest.Builder(p.intencion).build())
                }
            } catch (e: ErrorCuenta) {
                mensaje = contexto.getString(R.string.google_tareas_error)
            }
        }
    }
    fun conDrive(accion: suspend (RespaldoDrive) -> String) {
        if (actividad == null) return
        alcance.launch {
            correrDrive {
                when (val p = AutorizacionDrive.pedir(actividad)) {
                    is PermisoDrive.Listo -> accion(RespaldoDrive(p.token))
                    is PermisoDrive.Pedir -> {
                        pendienteDrive = accion
                        consentimientoDrive.launch(IntentSenderRequest.Builder(p.intencion).build())
                        null
                    }
                }
            }
        }
    }

    var confirmarRestaurarDrive by remember { mutableStateOf(false) }
    var confirmarImportarLocal by remember { mutableStateOf(false) }

    var elegida by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(seccionInicial) }
    val seccionActual = SeccionAjustes.deId(elegida)
    androidx.activity.compose.BackHandler(enabled = seccionActual != null) { elegida = null }

    Box(Modifier.fillMaxSize()) {
        if (ajustes.fondoUri != null) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f)))
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(seccionActual?.titulo ?: R.string.ajustes)) },
                    navigationIcon = { IconButton(onClick = { if (seccionActual != null) elegida = null else alVolver() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) { relleno ->
            androidx.compose.animation.AnimatedContent(
                targetState = seccionActual, modifier = Modifier.padding(relleno).fillMaxSize(),
                transitionSpec = {
                    androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(180, 40)) togetherWith
                        androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120))
                },
                label = "seccionAjustes",
            ) { s ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
                    when (s) {
                        null -> {
                            val notificaciones = Avisos.tienePermiso(contexto)
                            val resumen = ResumenAjustes(contexto.resources)

                            // Bloque 1: Personalización y uso
                            Text(
                                stringResource(R.string.ajustes_seccion_personalizacion),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp, start = 4.dp),
                            )
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column {
                                    FilaAjuste(Icons.Filled.Palette, stringResource(SeccionAjustes.APARIENCIA.titulo), resumen.apariencia(ajustes)) { elegida = SeccionAjustes.APARIENCIA.id }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    FilaAjuste(Icons.Filled.Language, stringResource(R.string.idioma), nombreIdioma(contexto)) {
                                        if (Idioma.delSistema) Idioma.abrirAjusteDelSistema(contexto) else elegirIdioma = true
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    FilaAjuste(Icons.Filled.Notifications, stringResource(SeccionAjustes.AVISOS.titulo), resumen.avisos(ajustes, notificaciones)) { elegida = SeccionAjustes.AVISOS.id }
                                }
                            }

                            // Bloque 2: Cuenta y datos
                            Text(
                                stringResource(R.string.ajustes_seccion_cuenta_datos),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 16.dp, bottom = 6.dp, start = 4.dp),
                            )
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column {
                                    FilaAjuste(Icons.Filled.Person, stringResource(SeccionAjustes.CUENTA.titulo), app.sync.estado.value.correo ?: stringResource(R.string.cuenta_sin_cuenta)) { elegida = SeccionAjustes.CUENTA.id }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    FilaAjuste(Icons.Filled.Backup, stringResource(SeccionAjustes.COPIA.titulo), resumen.copia()) { elegida = SeccionAjustes.COPIA.id }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    FilaAjuste(Icons.Filled.Delete, stringResource(SeccionAjustes.PAPELERA.titulo), stringResource(R.string.papelera_resumen)) { elegida = SeccionAjustes.PAPELERA.id }
                                }
                            }

                            // Banner destacado para Quitar Anuncios
                            val comprado by app.billing.comprado.collectAsStateWithLifecycle()
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).clickable {
                                    if (!comprado && actividad != null) {
                                        app.billing.comprar(actividad)
                                    } else if (comprado) {
                                        mensaje = contexto.getString(R.string.anuncios_quitados)
                                    }
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                    Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                                        Text(
                                            stringResource(R.string.quitar_anuncios),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                        Text(
                                            if (comprado) stringResource(R.string.anuncios_quitados) else stringResource(R.string.quitar_anuncios_descripcion),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        )
                                    }
                                }
                            }

                            // Bloque 3: Aplicación
                            Text(
                                stringResource(R.string.ajustes_seccion_aplicacion),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp, start = 4.dp),
                            )
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column {
                                    FilaAjuste(
                                        Icons.Filled.SystemUpdate,
                                        stringResource(R.string.buscar_actualizaciones),
                                        stringResource(R.string.ajustes_comprobar_actualizacion),
                                    ) {
                                        mensaje = contexto.getString(R.string.buscando_actualizacion)
                                        app.actualizaciones.comprobar(forzar = true) { e ->
                                            mensaje = when (e) {
                                                is EstadoActualizacion.Disponible, EstadoActualizacion.Lista -> null
                                                EstadoActualizacion.Descargando -> contexto.getString(R.string.actualizacion_descargando)
                                                EstadoActualizacion.Nada -> contexto.getString(R.string.actualizacion_ultima)
                                                null -> { abrirEnPlay(contexto); null }
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    FilaAjuste(Icons.Filled.Info, stringResource(SeccionAjustes.ACERCA.titulo), resumen.acerca(BuildConfig.VERSION_NAME)) { elegida = SeccionAjustes.ACERCA.id }
                                }
                            }

                            mensaje?.let { Text(it, Modifier.padding(horizontal = 12.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.primary) }
                        }
                        SeccionAjustes.APARIENCIA -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Seccion(stringResource(R.string.ajustes_tema)) {
                                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                            val opciones = listOf("sistema" to stringResource(R.string.tema_sistema), "claro" to stringResource(R.string.tema_claro), "oscuro" to stringResource(R.string.tema_oscuro))
                                            opciones.forEachIndexed { i, (id, titulo) ->
                                                SegmentedButton(ajustes.tema == id, { cambiar { it.copy(tema = id) } }, SegmentedButtonDefaults.itemShape(i, opciones.size)) { Text(titulo) }
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Seccion(stringResource(R.string.ajustes_color_app)) {
                                        SelectorColor(ajustes.acento) { id -> cambiar { it.copy(acento = id) } }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Seccion(stringResource(R.string.ajustes_fondo)) {
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = { elegirFondo.launch(arrayOf("image/*")) }) { Text(stringResource(if (ajustes.fondoUri == null) R.string.ajustes_elegir_imagen else R.string.ajustes_cambiar_imagen)) }
                                            if (ajustes.fondoUri != null && FondoArchivos.origen(contexto).exists()) {
                                                OutlinedButton(onClick = { encuadrando = true }) { Text(stringResource(R.string.ajustes_encuadrar)) }
                                            }
                                            if (ajustes.fondoUri != null) OutlinedButton(onClick = {
                                                FondoArchivos.borrar(contexto)
                                                cambiar { it.copy(fondoUri = null) }
                                            }) { Text(stringResource(R.string.ajustes_quitar)) }
                                        }
                                        if (ajustes.fondoUri != null) {
                                            var visibilidad by remember(ajustes.fondoVisibilidad) { mutableStateOf(ajustes.fondoVisibilidad) }
                                            var velo by remember(ajustes.opacidadPaneles) { mutableStateOf(ajustes.opacidadPaneles) }
                                            Text(stringResource(R.string.ajustes_visibilidad), style = MaterialTheme.typography.labelMedium)
                                            Slider(
                                                visibilidad, { v -> visibilidad = v; FondoEnVivo.visibilidad = v },
                                                onValueChangeFinished = { cambiar { it.copy(fondoVisibilidad = visibilidad) }; FondoEnVivo.visibilidad = null },
                                            )
                                            Text(stringResource(R.string.ajustes_velo), style = MaterialTheme.typography.labelMedium)
                                            Slider(
                                                velo, { v -> velo = v; FondoEnVivo.velo = v },
                                                onValueChangeFinished = { cambiar { it.copy(opacidadPaneles = velo) }; FondoEnVivo.velo = null },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        SeccionAjustes.AVISOS -> {
                            val permiso = Avisos.tienePermiso(contexto)
                            val necesitaExactas = Build.VERSION.SDK_INT >= 31 && !ProgramadorAvisos(contexto).puedeExactas()

                            if (necesitaExactas) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.Notifications,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                stringResource(R.string.ajustes_exactas_texto),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                contexto.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${contexto.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onTertiaryContainer, contentColor = MaterialTheme.colorScheme.tertiaryContainer),
                                        ) {
                                            Text(stringResource(R.string.conceder_permiso))
                                        }
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(stringResource(R.string.ajustes_segundo_aviso), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            val resumen = ResumenAjustes(contexto.resources)
                                            items(listOf(0, 5, 10, 15, 30, 60)) { minutos ->
                                                val texto = if (minutos == 0) stringResource(R.string.ajustes_sin_segundo) else stringResource(R.string.antes_de, resumen.minutos(minutos))
                                                FilterChip(
                                                    selected = ajustes.segundoAvisoMin == minutos,
                                                    onClick = { cambiar { it.copy(segundoAvisoMin = minutos) } },
                                                    label = { Text(texto) },
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f)) {
                                            Text(stringResource(R.string.ajustes_hora_sin_hora), style = MaterialTheme.typography.bodyMedium)
                                            Text(stringResource(R.string.ajustes_hora_sin_hora_texto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        OutlinedButton(onClick = {
                                            val h = ajustes.horaAvisoSinHora / 60
                                            val m = ajustes.horaAvisoSinHora % 60
                                            android.app.TimePickerDialog(
                                                contexto,
                                                { _, hora, minuto -> cambiar { it.copy(horaAvisoSinHora = hora * 60 + minuto) } },
                                                h, m, android.text.format.DateFormat.is24HourFormat(contexto),
                                            ).show()
                                        }) {
                                            Text("%02d:%02d".format(ajustes.horaAvisoSinHora / 60, ajustes.horaAvisoSinHora % 60))
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f)) {
                                            Text(stringResource(R.string.ajustes_aviso_racha), style = MaterialTheme.typography.bodyMedium)
                                            Text(stringResource(R.string.ajustes_aviso_racha_texto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = ajustes.rachaAviso, onCheckedChange = { v -> cambiar { it.copy(rachaAviso = v) } })
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f)) {
                                            Text(stringResource(R.string.ajustes_resumen), style = MaterialTheme.typography.bodyMedium)
                                            Text(stringResource(R.string.ajustes_resumen_texto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = ajustes.resumenSemanal, onCheckedChange = { v -> cambiar { it.copy(resumenSemanal = v) } })
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f)) {
                                            Text(stringResource(R.string.ajustes_aviso_repaso), style = MaterialTheme.typography.bodyMedium)
                                            Text(stringResource(R.string.ajustes_aviso_repaso_texto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = ajustes.repasoAviso, onCheckedChange = { v -> cambiar { it.copy(repasoAviso = v) } })
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(stringResource(if (permiso) R.string.ajustes_notif_on else R.string.ajustes_notif_off), style = MaterialTheme.typography.bodyMedium)
                                        if (!permiso) {
                                            OutlinedButton(onClick = {
                                                contexto.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                            }) { Text(stringResource(R.string.ajustes_abrir_notif)) }
                                        }
                                    }
                                    if (Build.VERSION.SDK_INT >= 31 && ProgramadorAvisos(contexto).puedeExactas()) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Text(stringResource(R.string.ajustes_exactas_ok), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                        SeccionAjustes.CUENTA -> SeccionCuenta(app)
                        SeccionAjustes.PAPELERA -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    SeccionPapelera(app)
                                }
                            }
                        }
                        SeccionAjustes.COPIA -> {
                            // Tarjeta 1 — Google Drive (Recomendado)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Google Drive", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                        ) {
                                            Text(
                                                stringResource(R.string.ajustes_recomendado),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        stringResource(R.string.drive_subtitulo),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { conDrive { d -> d.subir(Copia.exportar(app.db.dao())); contexto.getString(R.string.drive_subida_ok) } },
                                            enabled = !ocupadoDrive && actividad != null,
                                            modifier = Modifier.weight(1f),
                                        ) { Text(stringResource(R.string.drive_guardar)) }
                                        OutlinedButton(
                                            onClick = { confirmarRestaurarDrive = true },
                                            enabled = !ocupadoDrive && actividad != null,
                                            modifier = Modifier.weight(1f),
                                        ) { Text(stringResource(R.string.drive_restaurar)) }
                                    }
                                }
                            }

                            // Tarjeta 2 — Archivo local (.kiwu.json)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(stringResource(R.string.ajustes_copia), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        stringResource(R.string.copia_local_subtitulo),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilledTonalButton(
                                            onClick = { exportar.launch("kiwu-${LocalDate.now()}.kiwu.json") },
                                            modifier = Modifier.weight(1f),
                                        ) { Text(stringResource(R.string.exportar)) }
                                        OutlinedButton(
                                            onClick = { confirmarImportarLocal = true },
                                            modifier = Modifier.weight(1f),
                                        ) { Text(stringResource(R.string.importar)) }
                                    }
                                }
                            }

                            // Tarjeta 3 — Integraciones
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(stringResource(R.string.ajustes_integraciones), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f)) {
                                            Text(stringResource(R.string.ajustes_google_calendar), style = MaterialTheme.typography.bodyMedium)
                                            Text(stringResource(R.string.ajustes_google_calendar_texto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(
                                            checked = ajustes.calendarioGoogle && CalendarioTelefono.permitido(contexto),
                                            onCheckedChange = { v -> if (!v) cambiar { it.copy(calendarioGoogle = false) } else permisoCalendario.launch(android.Manifest.permission.READ_CALENDAR) },
                                        )
                                    }
                                    if (TareasGoogle.DISPONIBLE) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column(Modifier.weight(1f)) {
                                                Text(stringResource(R.string.ajustes_google_tareas), style = MaterialTheme.typography.bodyMedium)
                                                Text(stringResource(R.string.ajustes_google_tareas_texto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Switch(
                                                checked = ajustes.tareasGoogle,
                                                onCheckedChange = { v -> if (v) activarTareasGoogle() else { TareasGoogle.borrar(contexto); cambiar { it.copy(tareasGoogle = false) } } },
                                            )
                                        }
                                    }
                                }
                            }

                            mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 4.dp)) }
                        }
                        SeccionAjustes.ACERCA -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(stringResource(R.string.version_n, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium)
                                    OutlinedButton(onClick = alVerGuia) { Text(stringResource(R.string.ajustes_ver_guia)) }
                                    Text(stringResource(R.string.ajustes_privacidad), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 32.dp))
                }
            }
        }
    }

    if (confirmarRestaurarDrive) {
        AlertDialog(
            onDismissRequest = { confirmarRestaurarDrive = false },
            title = { Text(stringResource(R.string.ajustes_confirmar_restaurar_titulo)) },
            text = { Text(stringResource(R.string.ajustes_confirmar_restaurar_texto)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmarRestaurarDrive = false
                    conDrive { d ->
                        val copia = d.bajar()
                        if (copia == null) contexto.getString(R.string.drive_sin_copia) else textoImportado(contexto.resources, Copia.importar(app.db.dao(), copia))
                    }
                }) {
                    Text(stringResource(R.string.drive_restaurar))
                }
            },
            dismissButton = { TextButton(onClick = { confirmarRestaurarDrive = false }) { Text(stringResource(R.string.cancelar)) } },
        )
    }

    if (confirmarImportarLocal) {
        AlertDialog(
            onDismissRequest = { confirmarImportarLocal = false },
            title = { Text(stringResource(R.string.ajustes_confirmar_importar_titulo)) },
            text = { Text(stringResource(R.string.ajustes_confirmar_importar_texto)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmarImportarLocal = false
                    importar.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                }) {
                    Text(stringResource(R.string.importar))
                }
            },
            dismissButton = { TextButton(onClick = { confirmarImportarLocal = false }) { Text(stringResource(R.string.cancelar)) } },
        )
    }

    if (elegirIdioma) DialogoIdioma(alCerrar = { elegirIdioma = false })
    if (encuadrando) {
        EncuadreFondo(
            alCancelar = { encuadrando = false },
            alAceptar = { bitmap ->
                alcance.launch {
                    val direccion = withContext(Dispatchers.IO) { FondoArchivos.guardarEncuadre(contexto, bitmap) }
                    bitmap.recycle()
                    if (direccion != null) cambiar { it.copy(fondoUri = direccion) } else mensaje = contexto.getString(R.string.ajustes_error_fondo)
                    encuadrando = false
                }
            },
        )
    }
}

/** Fila de colores: los predefinidos, los del sistema (Android 12+) y uno a medida. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorColor(actual: String, alElegir: (String) -> Unit) {
    var personalizar by remember { mutableStateOf(false) }
    val personalizado = Acentos.personalizado(actual)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (a in Acentos.predefinidos) Muestra(a.muestra, stringResource(a.titulo), actual == a.id) { alElegir(a.id) }
        if (Build.VERSION.SDK_INT >= 31) {
            Muestra(null, stringResource(R.string.color_sistema), actual == Acentos.SISTEMA, Icons.Filled.Palette) { alElegir(Acentos.SISTEMA) }
        }
        Muestra(personalizado, stringResource(R.string.color_otro), personalizado != null, Icons.Filled.Colorize) { personalizar = true }
    }
    if (personalizar) {
        DialogoColor(personalizado ?: Acentos.muestra(Acentos.CORAL)!!, { personalizar = false }) { alElegir(Acentos.aTexto(it)) }
    }
}

@Composable
private fun Muestra(color: Color?, descripcion: String, elegida: Boolean, icono: ImageVector? = null, alPulsar: () -> Unit) {
    val relleno = color ?: MaterialTheme.colorScheme.surfaceVariant
    val descripcionElegida = stringResource(R.string.color_elegido, descripcion)
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(relleno)
            .border(if (elegida) 3.dp else 1.dp, if (elegida) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .semantics { contentDescription = if (elegida) descripcionElegida else descripcion }
            .clickable(onClick = alPulsar),
        contentAlignment = Alignment.Center,
    ) {
        when {
            elegida && icono == null -> Icon(Icons.Filled.Check, null, tint = Color.White)
            icono != null -> Icon(icono, null, tint = if (color == null) MaterialTheme.colorScheme.onSurfaceVariant else Color.White)
        }
    }
}

@Composable
internal fun DialogoColor(inicial: Color, alCerrar: () -> Unit, alElegir: (Color) -> Unit) {
    val (t0, s0) = remember(inicial) { tonoYSaturacion(inicial) }
    var tono by remember { mutableStateOf(t0) }
    var saturacion by remember { mutableStateOf(s0.coerceIn(0.3f, 1f)) }
    val color = Color.hsl(tono, saturacion, 0.5f)
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(R.string.color_otro)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(color))
                Text(stringResource(R.string.color_tono), style = MaterialTheme.typography.labelMedium)
                Slider(tono, { tono = it }, valueRange = 0f..360f)
                Text(stringResource(R.string.color_intensidad), style = MaterialTheme.typography.labelMedium)
                Slider(saturacion, { saturacion = it }, valueRange = 0f..1f)
            }
        },
        confirmButton = { TextButton(onClick = { alElegir(color); alCerrar() }) { Text(stringResource(R.string.color_usar)) } },
        dismissButton = { TextButton(onClick = alCerrar) { Text(stringResource(R.string.cancelar)) } },
    )
}
