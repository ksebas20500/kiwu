package app.kiwu.android.ui.ajustes

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import app.kiwu.android.ui.Acentos
import app.kiwu.android.ui.FondoArchivos
import app.kiwu.android.ui.FondoEnVivo
import app.kiwu.android.ui.tonoYSaturacion
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.BuildConfig
import app.kiwu.android.KiwuApp
import app.kiwu.android.data.AjustesUi
import app.kiwu.android.data.Copia
import app.kiwu.android.data.ErrorCopia
import app.kiwu.android.notify.Avisos
import app.kiwu.android.notify.ProgramadorAvisos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import app.kiwu.android.Idioma
import androidx.compose.material.icons.filled.Language
import app.kiwu.android.R
import androidx.compose.ui.res.stringResource
import app.kiwu.android.data.MotivoErrorCopia

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (titulo.isNotBlank()) Text(titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        contenido()
    }
}

/** Ajustes: apariencia, fondo, avisos del sistema y copia de seguridad. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AjustesScreen(app: KiwuApp, alVolver: () -> Unit, alVerGuia: () -> Unit = {}) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val ajustes by app.ajustes.flujo.collectAsStateWithLifecycle(AjustesUi())
    var mensaje by remember { mutableStateOf<String?>(null) }
    fun cambiar(f: (AjustesUi) -> AjustesUi) { alcance.launch { app.ajustes.cambiar(f) } }
    var elegirIdioma by remember { mutableStateOf(false) }

    var encuadrando by remember { mutableStateOf(false) }
    val elegirFondo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) alcance.launch {
            // La app guarda su propia copia de la foto: así no depende del permiso ni de que el archivo siga donde estaba.
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
    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) alcance.launch {
            mensaje = try {
                val texto = withContext(Dispatchers.IO) { contexto.contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) } ?: "" }
                val r = Copia.importar(app.db.dao(), texto)
                textoImportado(contexto.resources, r)
            } catch (e: ErrorCopia) {
                when (e.motivo) {
                    MotivoErrorCopia.Ilegible -> contexto.getString(R.string.copia_ilegible, e.detalle.orEmpty())
                    MotivoErrorCopia.NoEsKiwu -> contexto.getString(R.string.copia_no_es_kiwu)
                    MotivoErrorCopia.MasNueva -> contexto.getString(R.string.copia_nueva)
                }
            } catch (e: Exception) {
                contexto.getString(R.string.ajustes_importar_error, e.message)
            }
        }
    }

    // La pantalla principal lista las secciones; al tocar una se abre sola. Atrás vuelve a la lista de secciones.
    var elegida by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val seccionActual = SeccionAjustes.deId(elegida)
    androidx.activity.compose.BackHandler(enabled = seccionActual != null) { elegida = null }

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
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = if (s == null) 0.dp else 20.dp)) {
                when (s) {
                    null -> {
                        val notificaciones = Avisos.tienePermiso(contexto)
                        val resumen = ResumenAjustes(contexto.resources)
                        FilaAjuste(Icons.Filled.Palette, stringResource(SeccionAjustes.APARIENCIA.titulo), resumen.apariencia(ajustes)) { elegida = SeccionAjustes.APARIENCIA.id }
                        FilaAjuste(Icons.Filled.Language, stringResource(R.string.idioma), nombreIdioma(contexto)) {
                            if (Idioma.delSistema) Idioma.abrirAjusteDelSistema(contexto) else elegirIdioma = true
                        }
                        FilaAjuste(Icons.Filled.ViewList, stringResource(SeccionAjustes.MENU.titulo), resumen.menu(ajustes)) { elegida = SeccionAjustes.MENU.id }
                        FilaAjuste(Icons.Filled.Notifications, stringResource(SeccionAjustes.AVISOS.titulo), resumen.avisos(ajustes, notificaciones)) { elegida = SeccionAjustes.AVISOS.id }
                        FilaAjuste(Icons.Filled.Backup, stringResource(SeccionAjustes.COPIA.titulo), resumen.copia()) { elegida = SeccionAjustes.COPIA.id }
                        FilaAjuste(Icons.Filled.Info, stringResource(SeccionAjustes.ACERCA.titulo), resumen.acerca(BuildConfig.VERSION_NAME)) { elegida = SeccionAjustes.ACERCA.id }
                        mensaje?.let { Text(it, Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.primary) }
                    }
                    SeccionAjustes.APARIENCIA -> {
                        Seccion(stringResource(R.string.ajustes_tema)) {
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                val opciones = listOf("sistema" to stringResource(R.string.tema_sistema), "claro" to stringResource(R.string.tema_claro), "oscuro" to stringResource(R.string.tema_oscuro))
                                opciones.forEachIndexed { i, (id, titulo) ->
                                    SegmentedButton(ajustes.tema == id, { cambiar { it.copy(tema = id) } }, SegmentedButtonDefaults.itemShape(i, opciones.size)) { Text(titulo) }
                                }
                            }
                        }
                        HorizontalDivider()
                        Seccion(stringResource(R.string.ajustes_color_app)) {
                            SelectorColor(ajustes.acento) { id -> cambiar { it.copy(acento = id) } }
                        }
                        HorizontalDivider()
                        Seccion(stringResource(R.string.ajustes_fondo)) {
                            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                // Los deslizadores se mueven con un valor propio (sin esperar al guardado) y el fondo los aplica al instante.
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
                    SeccionAjustes.MENU -> {
                        Seccion(stringResource(R.string.ajustes_aspecto_listas)) {
                            SelectorAspectoMenu(ajustes.menuAspecto, ajustes.menuContadores) { id -> cambiar { it.copy(menuAspecto = id) } }
                        }
                        HorizontalDivider()
                        Seccion(stringResource(R.string.ajustes_orden)) {
                            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for ((id, texto) in listOf("manual" to stringResource(R.string.orden_manual), "alfabetico" to stringResource(R.string.orden_alfabetico), "pendientes" to stringResource(R.string.orden_pendientes))) {
                                    androidx.compose.material3.FilterChip(
                                        selected = ajustes.menuOrden == id, onClick = { cambiar { it.copy(menuOrden = id) } }, label = { Text(texto) },
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                        Seccion("") {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.ajustes_mostrar_numero), style = MaterialTheme.typography.bodyMedium)
                                androidx.compose.material3.Switch(checked = ajustes.menuContadores, onCheckedChange = { v -> cambiar { it.copy(menuContadores = v) } })
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.ajustes_mostrar_racha), style = MaterialTheme.typography.bodyMedium)
                                androidx.compose.material3.Switch(checked = ajustes.rachaVisible, onCheckedChange = { v -> cambiar { it.copy(rachaVisible = v) } })
                            }
                        }
                    }
                    SeccionAjustes.AVISOS -> {
                        Seccion("") {
                            val permiso = Avisos.tienePermiso(contexto)
                            // Segundo aviso: otro recordatorio unos minutos antes de la entrega, por si se borró o se olvidó el primero.
                            Text(stringResource(R.string.ajustes_segundo_aviso), style = MaterialTheme.typography.labelMedium)
                            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val resumen = ResumenAjustes(contexto.resources)
                                for (minutos in listOf(0, 5, 10, 15, 30, 60)) {
                                    val texto = if (minutos == 0) stringResource(R.string.ajustes_sin_segundo) else stringResource(R.string.antes_de, resumen.minutos(minutos))
                                    androidx.compose.material3.FilterChip(
                                        selected = ajustes.segundoAvisoMin == minutos,
                                        onClick = { cambiar { it.copy(segundoAvisoMin = minutos) } },
                                        label = { Text(texto) },
                                    )
                                }
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.ajustes_aviso_racha), style = MaterialTheme.typography.bodyMedium)
                                    Text(stringResource(R.string.ajustes_aviso_racha_texto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                androidx.compose.material3.Switch(checked = ajustes.rachaAviso, onCheckedChange = { v -> cambiar { it.copy(rachaAviso = v) } })
                            }
                            Text(stringResource(if (permiso) R.string.ajustes_notif_on else R.string.ajustes_notif_off), style = MaterialTheme.typography.bodyMedium)
                            if (!permiso) {
                                OutlinedButton(onClick = {
                                    contexto.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                }) { Text(stringResource(R.string.ajustes_abrir_notif)) }
                            }
                            if (Build.VERSION.SDK_INT >= 31 && !ProgramadorAvisos(contexto).puedeExactas()) {
                                Text(stringResource(R.string.ajustes_exactas_texto), style = MaterialTheme.typography.bodyMedium)
                                OutlinedButton(onClick = {
                                    contexto.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${contexto.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                }) { Text(stringResource(R.string.ajustes_permitir_exactas)) }
                            }
                        }
                    }
                    SeccionAjustes.COPIA -> {
                        Seccion("") {
                            Text(stringResource(R.string.ajustes_copia_texto), style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { exportar.launch("kiwu-${LocalDate.now()}.kiwu.json") }) { Text(stringResource(R.string.exportar)) }
                                OutlinedButton(onClick = { importar.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }) { Text(stringResource(R.string.importar)) }
                            }
                            mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                        }
                    }
                    SeccionAjustes.ACERCA -> {
                        Seccion("") {
                            Text(stringResource(R.string.version_n, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = alVerGuia) { Text(stringResource(R.string.ajustes_ver_guia)) }
                            Text(stringResource(R.string.ajustes_privacidad), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 32.dp))
            }
        }
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
