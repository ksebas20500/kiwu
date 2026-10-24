package app.kiwu.android.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.kiwu.android.KiwuApp
import app.kiwu.android.domain.ReglasHorario
import app.kiwu.android.domain.UrgenciaTarea
import kotlinx.coroutines.flow.first

/** Widget «Horario»: la tarjeta «Ahora / Siguiente» y las clases de hoy, con su color y sus puntos de tareas. */
class HorarioWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as KiwuApp
        val horarios = app.repo.horarios.first()
        val clases = app.repo.clases.first()
        val deberes = app.repo.deberes.first()
        val colores = coloresDelWidget(context)
        provideContent {
            val config = currentState<Preferences>().configWidget()
            val datos = DatosWidgets.horario(horarios, clases, deberes, config, System.currentTimeMillis())
            ContenidoHorario(datos, colores)
        }
    }
}

class HorarioWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HorarioWidget()
}

@Composable
private fun ContenidoHorario(datos: DatosHorario, colores: ColoresWidget) {
    val contexto = LocalContext.current
    val alto = LocalSize.current.height
    val maximo = when {
        alto < 140.dp -> 0
        alto < 200.dp -> 2
        alto < 260.dp -> 4
        else -> 7
    }
    Column(
        GlanceModifier.fillMaxSize().cornerRadius(18.dp).background(colores.fondo).padding(14.dp)
            .clickable(actionStartActivity(intencionDeApertura(contexto, seccion = "horario"))),
    ) {
        Row(GlanceModifier.fillMaxWidth()) {
            Text("Horario", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colores.texto))
            if (datos.nombre != null) {
                Text(
                    "  ·  ${datos.nombre}",
                    style = TextStyle(fontSize = 13.sp, color = colores.secundario),
                    maxLines = 1,
                )
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        if (!datos.hayHorarios) {
            AvisoHorario("Crea tu horario en Kiwu.", colores)
        } else {
            when {
                datos.enCurso != null -> TarjetaClase(datos.enCurso, "AHORA", colores)
                datos.siguiente != null -> TarjetaClase(datos.siguiente, "SIGUIENTE", colores, datos.diasSiguiente)
                else -> AvisoHorario("Sin más clases próximas.", colores)
            }
            if (maximo > 0) {
                Spacer(GlanceModifier.height(10.dp))
                Text("Clases de hoy", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colores.secundario))
                Spacer(GlanceModifier.height(6.dp))
                if (datos.hoy.isEmpty()) {
                    AvisoHorario("Hoy no tienes clases 🎉", colores)
                } else {
                    LazyColumn(GlanceModifier.fillMaxSize()) {
                        items(datos.hoy.take(maximo), itemId = { it.id.hashCode().toLong() }) { clase ->
                            FilaClaseWidget(clase, colores)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaClase(clase: FilaClase, etiqueta: String, colores: ColoresWidget, dias: Int = 0) {
    val texto = ColorProvider(clase.color.texto)
    Column(
        GlanceModifier.fillMaxWidth().cornerRadius(12.dp).background(colorDeClase(clase.color)).padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(etiqueta, style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = texto))
        Text(
            clase.titulo,
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = texto),
            maxLines = 1,
        )
        Text(
            detalle(clase, dias),
            style = TextStyle(fontSize = 12.sp, color = texto),
            maxLines = 1,
        )
    }
}

/** Día de la próxima clase, solo cuando no es hoy («Mañana», «Sáb»…). */
private fun diaDeLaClase(dias: Int, diaSemana: Int): String? = when {
    dias <= 0 -> null
    dias == 1 -> "Mañana"
    else -> ReglasHorario.diasCortos[(diaSemana - 1).coerceIn(0, 6)]
}

private fun detalle(clase: FilaClase, dias: Int = 0): String = listOfNotNull(
    diaDeLaClase(dias, clase.diaSemana),
    "${ReglasHorario.hora(clase.inicioMin)} – ${ReglasHorario.hora(clase.finMin)}",
    clase.aula.takeIf { it.isNotBlank() },
    clase.horario,
).joinToString(" · ")

@Composable
private fun FilaClaseWidget(clase: FilaClase, colores: ColoresWidget) {
    Row(GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box(GlanceModifier.width(4.dp).height(30.dp).cornerRadius(2.dp).background(colorDeClase(clase.color))) { }
        Column(GlanceModifier.defaultWeight().padding(start = 8.dp)) {
            Text(
                clase.titulo,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colores.texto),
                maxLines = 1,
            )
            Text(
                detalle(clase),
                style = TextStyle(fontSize = 12.sp, color = colores.secundario),
                maxLines = 1,
            )
        }
        if (clase.urgencia != null) {
            PuntoUrgencia(clase.urgencia, colores)
        }
    }
}

@Composable
private fun PuntoUrgencia(urgencia: UrgenciaTarea, colores: ColoresWidget) {
    val color = when (urgencia) {
        UrgenciaTarea.Vencida -> colores.rojo
        UrgenciaTarea.Pronto -> ColorProvider(Color(0xFFF9A825))
        UrgenciaTarea.Normal -> colores.secundario
    }
    Box(GlanceModifier.width(8.dp).height(8.dp).cornerRadius(4.dp).background(color)) { }
}

@Composable
private fun AvisoHorario(texto: String, colores: ColoresWidget) {
    Text(texto, style = TextStyle(fontSize = 14.sp, color = colores.secundario), maxLines = 2)
}
