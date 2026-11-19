package app.kiwu.android.ui.tareas

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import app.kiwu.android.ui.casillaDe
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import app.kiwu.android.ui.SelectorHora
import app.kiwu.android.ui.SelectorFechaHora
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kiwu.android.domain.Columna
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.esEvento
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.clip
import app.kiwu.android.domain.Materia
import app.kiwu.android.domain.Reglas
import app.kiwu.android.ui.Chip
import app.kiwu.android.ui.Fechas
import app.kiwu.android.ui.fechaCorta
import app.kiwu.android.R
import androidx.compose.ui.res.stringResource

private val VerdeHoy = Color(0xFF2E7D32)

/** Los eventos van siempre de azul claro pastel (acuarela suave), con su texto azul grisáceo, en la app y en el widget. */
val AzulEvento = Color(0xFFCFE5F7)
val AzulEventoTexto = Color(0xFF3B6486)

/** Chip con la fecha de entrega: rojo si venció, verde si es hoy. */
@Composable
fun ChipFecha(d: Deber, ahora: Long, reglas: Reglas) {
    val entrega = d.fechaEntrega ?: return
    val vencida = reglas.estaVencido(d, ahora)
    val hoy = !d.completado && !vencida && reglas.inicioDelDia(entrega) == reglas.inicioDelDia(ahora)
    val color = when {
        vencida -> MaterialTheme.colorScheme.error
        hoy -> VerdeHoy
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Chip(fechaCorta(entrega, ahora), fondo = color.copy(alpha = 0.12f), color = color)
}

/** Menú de una tarea: solo eliminar (mover de columna se hace arrastrando en el tablero o desde el detalle). */
@Composable
fun MenuTarea(
    abierto: Boolean,
    alCerrar: () -> Unit,
    alEliminar: () -> Unit,
    esEvento: Boolean = false,
) {
    DropdownMenu(expanded = abierto, onDismissRequest = alCerrar) {
        DropdownMenuItem(
            text = { Text(stringResource(if (esEvento) R.string.evento_eliminar else R.string.tarea_eliminar), color = MaterialTheme.colorScheme.error) },
            onClick = { alCerrar(); alEliminar() },
        )
    }
}

/** Bloque de agenda diferenciado para eventos de calendario. */
@Composable
fun TarjetaEventoAgenda(
    d: Deber,
    nombreLista: String?,
    alAbrir: () -> Unit,
) {
    val esSoloLectura = d.esExterno
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .then(if (!esSoloLectura) Modifier.clickable(onClick = alAbrir) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = AzulEvento.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, AzulEventoTexto.copy(alpha = 0.3f))
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(36.dp).background(AzulEvento, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Event, null, Modifier.size(20.dp), tint = AzulEventoTexto)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(d.titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (nombreLista != null || d.fechaEntrega != null || esSoloLectura) {
                    Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (esSoloLectura) {
                            Text(stringResource(R.string.ver_google_calendar), style = MaterialTheme.typography.labelSmall, color = AzulEventoTexto, fontWeight = FontWeight.Bold)
                        } else if (nombreLista != null) {
                            Text(nombreLista, style = MaterialTheme.typography.bodySmall, color = AzulEventoTexto)
                        }
                        d.fechaEntrega?.let { Text(fechaCorta(it, System.currentTimeMillis()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

/**
 * Envoltorio de gestos para una fila de tarea: deslizar a la derecha completa (o reabre una completada); a la
 * izquierda pospone la entrega a mañana (se ignora si la tarea es un evento, se repite, está completada o no
 * se pasa `alPosponer`). La fila siempre vuelve a su sitio.
 */
@Composable
fun ConGestosTarea(
    d: Deber,
    alMarcar: () -> Unit,
    alPosponer: () -> Unit = {},
    contenido: @Composable () -> Unit,
) {
    // El estado del swipe recuerda la primera lambda que recibe: se le dan las actuales con rememberUpdatedState.
    val marcarAhora by rememberUpdatedState(alMarcar)
    val posponerAhora by rememberUpdatedState(alPosponer)
    val deslizar = rememberSwipeToDismissBoxState(
        positionalThreshold = { tamano -> tamano * 0.4f },
        confirmValueChange = { valor ->
            when (valor) {
                SwipeToDismissBoxValue.StartToEnd -> { marcarAhora(); false } // siempre vuelve a su sitio
                SwipeToDismissBoxValue.EndToStart -> { if (d.repite == 0) posponerAhora(); false }
                else -> false
            }
        },
    )
    val deslizable = !d.esEvento && !d.esExterno
    // La izquierda solo se habilita si la tarea no está completada, no se repite y hay acción de posponer.
    val puedeIzquierda = deslizable && d.repite == 0 && !d.completado && alPosponer != {}
    SwipeToDismissBox(
        state = deslizar,
        enableDismissFromStartToEnd = deslizable,
        enableDismissFromEndToStart = puedeIzquierda,
        backgroundContent = {
            val dir = deslizar.dismissDirection
            // Solo pintar el fondo cuando hay un deslizamiento activo. Settled = en reposo, no mostrar nada.
            // Además, no mostrar el fondo de izquierda si la tarea completada lo inhabilitó: evita el flash rojo.
            if (dir == SwipeToDismissBoxValue.Settled) return@SwipeToDismissBox
            val haciaIzquierda = dir == SwipeToDismissBoxValue.EndToStart
            if (haciaIzquierda && !puedeIzquierda) return@SwipeToDismissBox
            if (haciaIzquierda) {
                Box(
                    Modifier.fillMaxSize().padding(vertical = 2.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                        Icon(Icons.Filled.Schedule, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text(stringResource(R.string.posponer_manana), color = MaterialTheme.colorScheme.onTertiaryContainer, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            } else {
                FondoCompletar(deslizar, d.completado)
            }
        },
        content = { contenido() },
    )
}

/** Una fila de la lista de tareas. */
@Composable
fun FilaTarea(
    d: Deber,
    nombreLista: String?,
    ahora: Long,
    reglas: Reglas,
    alAbrir: () -> Unit,
    alMarcar: (Boolean) -> Unit,
    alEliminar: () -> Unit = {},
    /** Deslizar la fila a la izquierda: pospone la entrega a mañana (se ignora si está vacío o la tarea se repite). */
    alPosponer: () -> Unit = {},
) {
    val columna = reglas.columnaDe(d)
    val esSoloLectura = d.esExterno
    
    // Si es externo (Google Calendar), no permitimos swipe
    val contenidoFila = @Composable {
        androidx.compose.material3.Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
            onClick = { if (!esSoloLectura) alAbrir() },
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (d.esEvento) {
                    // Un evento no se completa: en lugar del círculo lleva el icono de calendario.
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(30.dp).background(AzulEvento, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Event, stringResource(R.string.evento), Modifier.size(18.dp), tint = AzulEventoTexto)
                        }
                    }
                } else {
                    Checkbox(checked = d.completado, onCheckedChange = alMarcar, modifier = Modifier.casillaDe(d.titulo))
                }
                Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                    Text(
                        d.titulo,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (d.completado) TextDecoration.LineThrough else null,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (d.completado) 0.5f else 1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    val hayExtras = d.fechaEntrega != null || nombreLista != null || columna == Columna.EN_CURSO || d.repite != 0 ||
                        d.prioridad != 0 || d.etiquetas.isNotEmpty() || d.pasos.isNotEmpty() || d.examen || esSoloLectura
                    if (hayExtras) {
                        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (columna == Columna.EN_CURSO) {
                                Chip(stringResource(R.string.en_curso), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            if (d.examen && !d.completado && d.fechaEntrega != null) {
                                val dias = app.kiwu.android.domain.Examenes.diasRestantes(d.fechaEntrega, ahora)
                                Chip(
                                    when {
                                        dias < 0 -> stringResource(R.string.examen)
                                        dias == 0L -> stringResource(R.string.examen_hoy)
                                        dias == 1L -> stringResource(R.string.examen_manana)
                                        else -> stringResource(R.string.examen_dias, dias.toInt())
                                    },
                                    fondo = MaterialTheme.colorScheme.tertiaryContainer, color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            } else if (d.examen) Chip(stringResource(R.string.examen))
                            if (d.prioridad != 0) colorPrioridad(d.prioridad).let { Chip(textoPrioridad(d.prioridad), fondo = it.copy(alpha = 0.12f), color = it) }
                            app.kiwu.android.domain.Pasos.progreso(d.pasos)?.let { Chip("☑ $it") }
                            for (e in app.kiwu.android.domain.Etiquetas.leer(d.etiquetas).take(2)) Chip("#$e")
                            if (d.repite != 0) Chip("↻ " + textoRepeticion(d.repite))
                            ChipFecha(d, ahora, reglas)
                            if (nombreLista != null) {
                                Chip(nombreLista, fondo = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f), color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            if (esSoloLectura) {
                                Chip("Google", fondo = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f), color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (esSoloLectura) {
        contenidoFila()
    } else {
        ConGestosTarea(d, alMarcar = { alMarcar(!d.completado) }, alPosponer = alPosponer) {
            contenidoFila()
        }
    }
}

/** Campo «Añadir tarea» con selector de lista (si no hay una lista fija). */
@Composable
fun CampoNuevaTarea(
    materias: List<Materia>,
    fija: Materia?,
    alCrear: (titulo: String, materiaId: String, fecha: Long?) -> Unit,
    modifier: Modifier = Modifier,
    marcador: String = stringResource(R.string.anadir_tarea),
    /** El calendario fija el día que se está viendo: entonces el botón solo elige la hora de entrega. */
    diaFijo: java.time.LocalDate? = null,
    /** Pone el cursor en el campo al abrirse (la pantalla de añadir desde el widget). */
    enfocar: Boolean = false,
) {
    val foco = remember { androidx.compose.ui.focus.FocusRequester() }
    // El campo está desactivado hasta que llegan las listas: se enfoca cuando ya se puede escribir.
    LaunchedEffect(materias.isNotEmpty()) { if (enfocar && materias.isNotEmpty()) foco.requestFocus() }
    var texto by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf<Long?>(null) }
    var hora by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var selector by remember { mutableStateOf(false) }
    var destino by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    val elegida = fija ?: materias.firstOrNull { it.id == destino } ?: materias.firstOrNull()
    val enviar = {
        if (texto.isNotBlank() && elegida != null) {
            val entrega = if (diaFijo != null) hora?.let { (h, m) -> diaFijo.atTime(h, m).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() } else fecha
            alCrear(texto.trim(), elegida.id, entrega)
            texto = ""
            fecha = null
            hora = null
        }
    }
    Surface(
        modifier = modifier.fillMaxWidth().imePadding().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
    ) {
        Column {
        // La fecha elegida va en su propia línea: en una fila con el campo le quitaba el ancho y el recuadro crecía a lo alto.
        if (diaFijo != null) {
            hora?.let { (h, m) -> Box(Modifier.padding(start = 12.dp, top = 8.dp).clickable { hora = null }) { Chip("%02d:%02d  ✕".format(h, m)) } }
        } else {
            fecha?.let { f -> Box(Modifier.padding(start = 12.dp, top = 8.dp).clickable { fecha = null }) { Chip(fechaCorta(f, System.currentTimeMillis(), completo = true) + "  ✕") } }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val puesta = if (diaFijo != null) hora != null else fecha != null
            IconButton(onClick = { selector = true }) {
                Icon(
                    if (diaFijo != null) Icons.Filled.Schedule else Icons.Filled.Event,
                    stringResource(if (diaFijo != null) R.string.elegir_hora_entrega else R.string.elegir_fecha_entrega),
                    tint = if (puesta) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextField(
                value = texto,
                onValueChange = { texto = it },
                modifier = Modifier.weight(1f).widthIn(min = 96.dp).focusRequester(foco),
                placeholder = { Text(if (materias.isEmpty()) stringResource(R.string.crea_lista_primero) else marcador, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                enabled = materias.isNotEmpty(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { enviar() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )
            if (fija == null && materias.isNotEmpty() && elegida != null) {
                Box {
                    TextButton(onClick = { menu = true }) {
                        Text(elegida.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(end = 2.dp))
                        Icon(Icons.Filled.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        for (m in materias) DropdownMenuItem(text = { Text(m.nombre) }, onClick = { destino = m.id; menu = false })
                    }
                }
            }
            IconButton(onClick = enviar, enabled = texto.isNotBlank() && elegida != null) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    stringResource(R.string.anadir),
                    tint = if (texto.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
        }
    }
    if (selector) {
        if (diaFijo != null) SelectorHora(hora?.first ?: 18, hora?.second ?: 0, alCerrar = { selector = false }) { h, m -> hora = h to m }
        else SelectorFechaHora(fecha, alCerrar = { selector = false }, alElegir = { fecha = it })
    }
}

/**
 * Fondo del swipe derecha: el color va de `primaryContainer` suave a `primary` pleno conforme aumenta el
 * progreso del arrastre. Al superar el umbral, el ícono hace un rebote con spring y crece para confirmar
 * que la acción se ejecutará. El texto cambia de color junto al fondo para mantener el contraste.
 * Colores del tema: legible en modo claro y oscuro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FondoCompletar(deslizar: SwipeToDismissBoxState, completado: Boolean) {
    val superaUmbral = deslizar.targetValue == SwipeToDismissBoxValue.StartToEnd

    // Escala del ícono: rebote al superar el umbral, retorno suave si el usuario retrocede.
    val escalaIcono by animateFloatAsState(
        targetValue = if (superaUmbral) 1.4f else 1f,
        animationSpec = if (superaUmbral)
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
        else
            tween(180),
        label = "escala_icono",
    )

    // Progreso del arrastre (0..1): anima el color de fondo entre tenue y pleno.
    val progreso = try {
        (deslizar.requireOffset() / 1f).coerceIn(0f, 1f)
    } catch (_: Exception) { 0f }
    // Interpolar entre primaryContainer (inicio) y primary (superado el umbral).
    val colorFondo = MaterialTheme.colorScheme.primaryContainer
    val colorFondoFull = MaterialTheme.colorScheme.primary
    val colorTexto = MaterialTheme.colorScheme.onPrimaryContainer
    val colorTextoFull = MaterialTheme.colorScheme.onPrimary

    // La mezcla se basa en si ya superó el umbral (animado) para no depender del offset raw.
    val mezcla by animateFloatAsState(
        targetValue = if (superaUmbral) 1f else 0f,
        animationSpec = tween(220),
        label = "mezcla_color",
    )
    val fondo = androidx.compose.ui.graphics.lerp(colorFondo, colorFondoFull, mezcla)
    val contenido = androidx.compose.ui.graphics.lerp(colorTexto, colorTextoFull, mezcla)

    Box(
        Modifier
            .fillMaxSize()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(fondo),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = contenido,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer { scaleX = escalaIcono; scaleY = escalaIcono },
            )
            Text(
                stringResource(if (completado) R.string.deslizar_reabrir else R.string.completar),
                color = contenido,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
