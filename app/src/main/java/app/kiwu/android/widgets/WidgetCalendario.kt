package app.kiwu.android.widgets

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import app.kiwu.android.domain.EventoExterno
import app.kiwu.android.domain.esEvento
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.ImageProvider
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.state.updateAppWidgetState
import app.kiwu.android.data.CalendarioTelefono
import app.kiwu.android.data.TareasGoogle
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.height
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import app.kiwu.android.KiwuApp
import app.kiwu.android.R
import app.kiwu.android.ui.Fechas
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.ZoneId

/** Cuántos meses se ha movido el calendario respecto al mes actual (0 = el mes de hoy). Se guarda por widget. */
private val ClaveMesCalendario = intPreferencesKey("mes_calendario")
private val ClaveDiaSeleccionado = longPreferencesKey("dia_seleccionado")
private val ClaveElegirDia = ActionParameters.Key<Long>("elegir_dia")
private val ClaveDeltaMes = ActionParameters.Key<Int>("delta_mes")

/** Widget «Calendario»: el mes con las tareas y eventos pendientes de cada día, todas las listas juntas. */
class CalendarioWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as KiwuApp
        val repo = app.repo
        // Primer dibujo con lo ya leído; después se sigue la base de datos en vivo (ver `DeberesWidget`).
        val deberes0 = repo.deberes.first()
        val materias0 = repo.materias.first()
        val grupos0 = repo.grupos.first()
        val colores = coloresDelWidget(context)
        TextosWidget.de(context)
        val aj0 = app.ajustes.flujo.first()
        provideContent {
            val deberes by repo.deberes.collectAsState(deberes0)
            val materias by repo.materias.collectAsState(materias0)
            val grupos by repo.grupos.collectAsState(grupos0)
            val ajustes by app.ajustes.flujo.collectAsState(aj0)
            val preferencias = currentState<Preferences>()
            val desplazamiento = preferencias[ClaveMesCalendario] ?: 0
            val ahora = System.currentTimeMillis()
            val hoy = Instant.ofEpochMilli(ahora).atZone(ZoneId.systemDefault()).toLocalDate()
            val mes = hoy.withDayOfMonth(1).plusMonths(desplazamiento.toLong())
            // Eventos de Google Calendar: se vuelven a leer al cambiar de mes, de ajuste o de datos (Glance no repite `provideGlance`).
            val externos by produceState(emptyList<EventoExterno>(), mes, ajustes.calendarioGoogle, ajustes.tareasGoogle, deberes) {
                value = withContext(Dispatchers.IO) {
                    val eventos = if (ajustes.calendarioGoogle) CalendarioTelefono.eventos(context, mes.minusDays(7), mes.plusMonths(1).plusDays(7)) else emptyList()
                    if (ajustes.tareasGoogle && TareasGoogle.vieja(context)) TareasGoogle.actualizar(context)
                    eventos + if (ajustes.tareasGoogle) TareasGoogle.leer(context) else emptyList()
                }
            }
            val datos = DatosWidgets.calendario(deberes, mes, ahora, externos = externos)
            val seleccion = preferencias[ClaveDiaSeleccionado]?.let(LocalDate::ofEpochDay)
                ?.takeIf { it.withDayOfMonth(1) == mes }
                ?: hoy.takeIf { it.withDayOfMonth(1) == mes }
                ?: mes
            val ubicaciones = ubicacionesDeListas(materias, grupos)
            val rutas = deberes.asSequence()
                .filterNot { it.esEvento }
                .mapNotNull { deber -> ubicaciones[deber.materiaId]?.etiqueta?.let { deber.id to it } }
                .toMap()
            ContenidoCalendario(datos, seleccion, rutas, colores)
        }
    }
}

class CalendarioWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarioWidget()
}

/** Las flechas del mes (±1) y tocar el nombre del mes (0), que vuelve al mes de hoy. */
class CambiarMesCalendario : ActionCallback {
    override suspend fun onAction(contexto: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val delta = parameters[ClaveDeltaMes] ?: return
        updateAppWidgetState(contexto, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[ClaveMesCalendario] = if (delta == 0) 0 else (this[ClaveMesCalendario] ?: 0) + delta
                if (delta == 0) remove(ClaveDiaSeleccionado)
            }
        }
        CalendarioWidget().update(contexto, glanceId)
    }
}

class SeleccionarDiaCalendario : ActionCallback {
    override suspend fun onAction(contexto: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val dia = parameters[ClaveElegirDia]?.let(LocalDate::ofEpochDay) ?: return
        updateAppWidgetState(contexto, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            val mesActual = LocalDate.now().withDayOfMonth(1)
            prefs.toMutablePreferences().apply {
                this[ClaveDiaSeleccionado] = dia.toEpochDay()
                this[ClaveMesCalendario] = ChronoUnit.MONTHS.between(mesActual, dia.withDayOfMonth(1)).toInt()
            }
        }
        CalendarioWidget().update(contexto, glanceId)
    }
}

@Composable
private fun ContenidoCalendario(
    datos: DatosCalendario,
    seleccion: LocalDate,
    rutas: Map<String, String>,
    colores: ColoresWidget,
) {
    val tamano = LocalSize.current
    val escalaTexto = LocalContext.current.resources.configuration.fontScale.coerceAtLeast(1f)
    val anchoContenido = tamano.width.value - 18f
    val anchoSeparador = 5.dp
    val anchoPanel = (anchoContenido - anchoSeparador.value) / 2f
    val altoMinimo = 68f + (12f + 16f * datos.semanas.size) * escalaTexto
    val lateral = anchoPanel >= 136f * escalaTexto && tamano.height.value >= altoMinimo
    Box(GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_borde_cristal)).padding(1.dp)) {
        Row(GlanceModifier.fillMaxSize().cornerRadius(17.dp).background(colores.fondo).padding(8.dp)) {
            CuadriculaCalendario(
                datos, colores, seleccion.takeIf { lateral },
                ancho = if (lateral) anchoPanel else anchoContenido,
                alto = tamano.height.value - 2f,
                escalaTexto = escalaTexto,
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            )
            if (lateral) {
                Box(GlanceModifier.width(anchoSeparador).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Box(GlanceModifier.width(1.dp).fillMaxHeight().background(colores.celda)) { }
                }
                val dia = datos.semanas.asSequence().flatten().first { it.fecha == seleccion }
                AgendaLateral(dia, rutas, GlanceModifier.defaultWeight().fillMaxHeight(), colores, compacto = anchoPanel < 200f)
            }
        }
    }
}

@Composable
private fun CuadriculaCalendario(
    datos: DatosCalendario,
    colores: ColoresWidget,
    seleccion: LocalDate?,
    ancho: Float,
    alto: Float,
    escalaTexto: Float,
    modifier: GlanceModifier,
) {
    val altoSemana = (alto - 68f - 16f * escalaTexto) / datos.semanas.size
    val diametroDia = (altoSemana - 6f).coerceIn(10f, 22f).dp
    val mesCompleto = Fechas.mes(datos.mes)
    val mesVisible = if (ancho < 200f * escalaTexto) {
        java.time.format.DateTimeFormatter.ofPattern("MMM", LocalContext.current.resources.configuration.locales[0])
            .format(datos.mes)
            .replaceFirstChar { it.titlecase() }
    } else {
        mesCompleto
    }
    Column(modifier) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                GlanceModifier.defaultWeight().height(48.dp).semantics { contentDescription = mesCompleto }
                    .clickable(actionRunCallback<CambiarMesCalendario>(actionParametersOf(ClaveDeltaMes to 0))),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    mesVisible,
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colores.texto),
                    maxLines = 1,
                )
            }
            Flecha("‹", textoWidget(R.string.mes_anterior), -1, colores)
            Flecha("›", textoWidget(R.string.mes_siguiente), 1, colores)
        }
        Row(GlanceModifier.fillMaxWidth().padding(top = 4.dp)) {
            for (d in 1..7) {
                Text(
                    Fechas.letraDia(d),
                    GlanceModifier.defaultWeight(),
                    style = TextStyle(fontSize = 10.sp, color = colores.secundario, textAlign = TextAlign.Center),
                )
            }
        }
        for (semana in datos.semanas) {
            Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
                for (dia in semana) CasillaDia(dia, diametroDia, seleccion, colores)
            }
        }
    }
}

@Composable
private fun Flecha(signo: String, descripcion: String, delta: Int, colores: ColoresWidget) {
    Box(
        GlanceModifier.size(48.dp).semantics { contentDescription = descripcion }
            .clickable(actionRunCallback<CambiarMesCalendario>(actionParametersOf(ClaveDeltaMes to delta))),
        contentAlignment = Alignment.Center,
    ) {
        Text(signo, style = TextStyle(fontSize = 20.sp, color = colores.texto))
    }
}

@Composable
private fun androidx.glance.layout.RowScope.CasillaDia(dia: DiaCalendario, diametroDia: Dp, seleccion: LocalDate?, colores: ColoresWidget) {
    val contexto = LocalContext.current
    val descripcion = descripcionDia(contexto, dia)
    val accion = if (seleccion == null) actionStartActivity(intencionDiaCalendario(contexto, dia.fecha))
    else actionRunCallback<SeleccionarDiaCalendario>(actionParametersOf(ClaveElegirDia to dia.fecha.toEpochDay()))
    Box(
        GlanceModifier.defaultWeight().fillMaxHeight()
            .semantics { contentDescription = descripcion }
            .clickable(accion)
            .padding(horizontal = 1.dp, vertical = if (diametroDia < 22.dp) 0.dp else 1.dp),
    ) {
        ContenidoDia(dia, diametroDia, seleccion, colores)
    }
}

@Composable
private fun ContenidoDia(dia: DiaCalendario, diametroDia: Dp, seleccion: LocalDate?, colores: ColoresWidget) {
    val resaltado = dia.fecha == seleccion || (dia.esHoy && seleccion == null)
    val transparente = ColorProvider(Color.Transparent)
    val fondoDia = if (dia.delMes && seleccion == null) colores.celda else transparente
    // RemoteViews debe sobrescribir el fondo aunque la fecha haya dejado de estar seleccionada.
    val fondoNumero = if (resaltado) colores.acento else transparente
    val colorNumero = when {
        resaltado -> colores.sobreAcento
        dia.esHoy -> colores.acento
        dia.delMes -> colores.texto
        else -> colores.secundario
    }
    Column(
        GlanceModifier.fillMaxSize().cornerRadius(6.dp).background(fondoDia),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            GlanceModifier.size(diametroDia).cornerRadius(diametroDia / 2).background(fondoNumero),
            contentAlignment = Alignment.Center,
        ) {
            Text("${dia.fecha.dayOfMonth}", style = TextStyle(fontSize = if (diametroDia < 16.dp) 9.sp else 12.sp, color = colorNumero), maxLines = 1)
        }
        Box(GlanceModifier.fillMaxWidth().height(6.dp), contentAlignment = Alignment.Center) {
            if (dia.chips.isNotEmpty()) {
                Box(GlanceModifier.size(4.dp).cornerRadius(2.dp).background(colores.acento)) { }
            }
        }
    }
}

private fun descripcionDia(contexto: Context, dia: DiaCalendario): String {
    val total = dia.chips.size
    return contexto.resources.getQuantityString(R.plurals.w_fecha_elementos, total, Fechas.diaLargo(dia.fecha), total)
}

@Composable
private fun AgendaLateral(
    dia: DiaCalendario,
    rutas: Map<String, String>,
    modifier: GlanceModifier,
    colores: ColoresWidget,
    compacto: Boolean,
) {
    val contexto = LocalContext.current
    Column(modifier) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                GlanceModifier.defaultWeight().height(48.dp).padding(start = 8.dp)
                    .clickable(actionStartActivity(intencionDiaCalendario(contexto, dia.fecha))),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(Fechas.diaCorto(dia.fecha.dayOfWeek.value) + " " + dia.fecha.dayOfMonth,
                    style = TextStyle(fontSize = 13.sp, color = colores.texto, fontWeight = FontWeight.Bold), maxLines = 1)
            }
            val anadir = textoWidget(R.string.anadir_tarea)
            Box(
                GlanceModifier.size(48.dp).semantics { contentDescription = anadir }
                    .clickable(actionStartActivity(intencionDiaCalendario(contexto, dia.fecha, crear = true))),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    GlanceModifier.size(32.dp).cornerRadius(12.dp)
                        .background(ColorProvider(colores.acento.getColor(contexto).copy(alpha = 0.22f))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", style = TextStyle(fontSize = 20.sp, color = colores.acento), maxLines = 1)
                }
            }
        }
        if (dia.chips.isEmpty()) {
            Text(textoWidget(R.string.dia_sin_pendientes), modifier = GlanceModifier.padding(8.dp),
                style = TextStyle(fontSize = 12.sp, color = colores.secundario), maxLines = 2)
        } else {
            LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
                itemsIndexed(dia.chips) { indice, chip ->
                    Column {
                        val local = !chip.id.startsWith("g:")
                        Row(GlanceModifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (local && !chip.esEvento) {
                                val completar = textoWidget(R.string.w_completar_tarea, chip.titulo)
                                Box(
                                    GlanceModifier.width(if (compacto) 28.dp else 48.dp).fillMaxHeight()
                                        .semantics { contentDescription = completar }
                                        .clickable(actionRunCallback<CompletarDeber>(actionParametersOf(ClaveDeberWidget to chip.id))),
                                    contentAlignment = Alignment.Center,
                                ) { Text("○", style = TextStyle(fontSize = 20.sp, color = colores.acento)) }
                            }
                            val destino = if (local) intencionDeApertura(contexto, tarea = chip.id) else intencionDiaCalendario(contexto, dia.fecha)
                            Column(
                                GlanceModifier.defaultWeight().fillMaxHeight()
                                    .clickable(actionStartActivity(destino)).padding(start = 2.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(chip.titulo, style = TextStyle(fontSize = 11.sp, color = if (chip.vencida) colores.rojo else colores.texto), maxLines = 1)
                                val detalle = rutas[chip.id] ?: if (local && chip.esEvento) textoWidget(R.string.evento) else null
                                detalle?.let {
                                    Text(it, style = TextStyle(fontSize = 11.sp, color = colores.secundario), maxLines = 1)
                                }
                            }
                        }
                        if (indice < dia.chips.lastIndex) {
                            val color = if (colores.oscuro) Color.White else Color.Black
                            Box(GlanceModifier.fillMaxWidth().height(1.dp)
                                .background(ColorProvider(color.copy(alpha = 0.08f)))) { }
                        }
                    }
                }
            }
        }
    }
}
