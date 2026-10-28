package app.kiwu.android.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
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
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.kiwu.android.KiwuApp
import app.kiwu.android.R
import app.kiwu.android.ui.Fechas
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId

/** Cuántos meses se ha movido el calendario respecto al mes actual (0 = el mes de hoy). Se guarda por widget. */
private val ClaveMesCalendario = intPreferencesKey("mes_calendario")
private val ClaveDeltaMes = ActionParameters.Key<Int>("delta_mes")

/** Widget «Calendario»: el mes con las tareas y eventos pendientes de cada día, todas las listas juntas. */
class CalendarioWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = (context.applicationContext as KiwuApp).repo
        // Primer dibujo con lo ya leído; después se sigue la base de datos en vivo (ver `DeberesWidget`).
        val deberes0 = repo.deberes.first()
        val colores = coloresDelWidget(context)
        TextosWidget.de(context)
        provideContent {
            val deberes by repo.deberes.collectAsState(deberes0)
            val desplazamiento = currentState<Preferences>()[ClaveMesCalendario] ?: 0
            val ahora = System.currentTimeMillis()
            val hoy = Instant.ofEpochMilli(ahora).atZone(ZoneId.systemDefault()).toLocalDate()
            val datos = DatosWidgets.calendario(deberes, hoy.withDayOfMonth(1).plusMonths(desplazamiento.toLong()), ahora)
            ContenidoCalendario(datos, colores)
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
            prefs.toMutablePreferences().apply { this[ClaveMesCalendario] = if (delta == 0) 0 else (this[ClaveMesCalendario] ?: 0) + delta }
        }
        CalendarioWidget().update(contexto, glanceId)
    }
}

@Composable
private fun ContenidoCalendario(datos: DatosCalendario, colores: ColoresWidget) {
    val contexto = LocalContext.current
    val alto = LocalSize.current.height
    // Alto de cada semana: lo que queda tras los márgenes (20 dp), la cabecera (32 dp) y los días de la semana (16 dp).
    val altoSemana = (alto.value - 68f) / datos.semanas.size
    // Cada tarea ocupa 11 dp bajo el número del día (18 dp); si no cabe ninguna, solo se marca que hay algo.
    val maximo = ((altoSemana - 18f) / 11f).toInt().coerceIn(0, 3)
    Column(GlanceModifier.fillMaxSize().cornerRadius(18.dp).background(colores.fondo).padding(10.dp)) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                Fechas.mes(datos.mes),
                GlanceModifier.defaultWeight().clickable(actionRunCallback<CambiarMesCalendario>(actionParametersOf(ClaveDeltaMes to 0))),
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colores.texto),
                maxLines = 1,
            )
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
                for (dia in semana) CasillaDia(dia, maximo, colores)
            }
        }
    }
}

@Composable
private fun Flecha(signo: String, descripcion: String, delta: Int, colores: ColoresWidget) {
    Box(
        GlanceModifier.size(32.dp).semantics { contentDescription = descripcion }
            .clickable(actionRunCallback<CambiarMesCalendario>(actionParametersOf(ClaveDeltaMes to delta))),
        contentAlignment = Alignment.Center,
    ) {
        Text(signo, style = TextStyle(fontSize = 20.sp, color = colores.texto))
    }
}

@Composable
private fun androidx.glance.layout.RowScope.CasillaDia(dia: DiaCalendario, maximo: Int, colores: ColoresWidget) {
    val contexto = LocalContext.current
    Box(GlanceModifier.defaultWeight().fillMaxHeight().padding(1.5.dp)) {
    Column(
        GlanceModifier.fillMaxSize().cornerRadius(6.dp).let { if (dia.delMes) it.background(colores.celda) else it }
            .clickable(actionStartActivity(intencionDiaCalendario(contexto, dia.fecha))),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            GlanceModifier.size(18.dp).cornerRadius(9.dp).let { if (dia.esHoy) it.background(colores.acento) else it },
            contentAlignment = Alignment.Center,
        ) {
            val color = when {
                dia.esHoy -> colores.sobreAcento
                dia.delMes -> colores.texto
                else -> colores.secundario
            }
            Text("${dia.fecha.dayOfMonth}", style = TextStyle(fontSize = 10.sp, fontWeight = if (dia.esHoy) FontWeight.Bold else FontWeight.Normal, color = color))
        }
        if (dia.chips.isNotEmpty()) {
            if (maximo == 0) {
                Text("•", style = TextStyle(fontSize = 10.sp, color = colores.acento))
            } else {
                for (chip in dia.chips.take(maximo)) {
                    val fondo = when {
                        chip.vencida -> colores.rojo
                        chip.esEvento -> ColorProvider(app.kiwu.android.ui.tareas.AzulEvento)
                        else -> colores.acento
                    }
                    val texto = if (chip.vencida) ColorProvider(Color.White) else if (chip.esEvento) ColorProvider(app.kiwu.android.ui.tareas.AzulEventoTexto) else colores.sobreAcento
                    Text(
                        chip.titulo,
                        GlanceModifier.fillMaxWidth().padding(top = 1.dp).cornerRadius(3.dp).background(fondo),
                        style = TextStyle(fontSize = 8.sp, color = texto),
                        maxLines = 1,
                    )
                }
                if (dia.chips.size > maximo) Text("+${dia.chips.size - maximo}", style = TextStyle(fontSize = 8.sp, color = colores.secundario))
            }
        }
    }
    }
}
