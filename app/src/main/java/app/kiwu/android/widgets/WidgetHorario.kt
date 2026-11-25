package app.kiwu.android.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ImageProvider
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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.kiwu.android.KiwuApp
import app.kiwu.android.domain.ContadorFaltas
import app.kiwu.android.domain.ReglasHorario
import app.kiwu.android.domain.UrgenciaTarea
import kotlinx.coroutines.flow.first
import app.kiwu.android.R

/** Widget «Horario»: la tarjeta «Ahora / Siguiente» y las clases de hoy, con su color y sus puntos de tareas. */
class HorarioWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as KiwuApp
        val horarios = app.repo.horarios.first()
        val clases = app.repo.clases.first()
        val deberes = app.repo.deberes.first()
        val faltas = app.repo.faltas.first()
        val materias = app.repo.materias.first()
        val colores = coloresDelWidget(context)
        TextosWidget.de(context) // deja «Eventos» y los demás nombres fijos en el idioma de Kiwu
        provideContent {
            val config = currentState<Preferences>().configWidget()
            val datos = DatosWidgets.horario(horarios, clases, deberes, config, System.currentTimeMillis(), faltas = faltas, materias = materias)
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
    val destacadaHoy = datos.enCurso ?: datos.siguiente?.takeIf { datos.diasSiguiente == 0 }
    val otrasClases = datos.hoy.filterNot { it.id == destacadaHoy?.id }
    val maximo = when {
        alto < 140.dp -> 0
        alto < 200.dp -> 2
        alto < 260.dp -> 4
        else -> 7
    }
    Box(
        GlanceModifier.fillMaxSize()
            .background(ImageProvider(R.drawable.widget_borde_cristal))
            .padding(1.dp),
    ) {
        Column(
            GlanceModifier.fillMaxSize().cornerRadius(17.dp).background(colores.fondo).padding(14.dp)
                .clickable(actionStartActivity(intencionDeApertura(contexto, seccion = "horario"))),
        ) {
            Row(GlanceModifier.fillMaxWidth()) {
                Text(textoWidget(R.string.horario), style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colores.texto))
                if (datos.nombre != null) {
                    Text(
                        "  ·  ${datos.nombre}",
                        style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, color = colores.secundario),
                        maxLines = 1,
                    )
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            if (!datos.hayHorarios) {
                AvisoHorario(textoWidget(R.string.w_crea_horario), colores)
            } else {
                when {
                    datos.enCurso != null -> TarjetaClase(datos.enCurso, textoWidget(R.string.w_ahora), colores)
                    datos.siguiente != null -> TarjetaClase(datos.siguiente, textoWidget(R.string.w_siguiente), colores, datos.diasSiguiente)
                    else -> AvisoHorario(textoWidget(R.string.w_sin_mas_clases), colores)
                }
                if (maximo > 0 && (otrasClases.isNotEmpty() || datos.hoy.isEmpty())) {
                    Spacer(GlanceModifier.height(10.dp))
                    Text(textoWidget(R.string.w_clases_hoy), style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colores.secundario))
                    Spacer(GlanceModifier.height(6.dp))
                    if (datos.hoy.isEmpty()) {
                        AvisoHorario(textoWidget(R.string.w_hoy_sin_clases), colores)
                    } else {
                        LazyColumn(GlanceModifier.fillMaxSize()) {
                            items(otrasClases.take(maximo), itemId = { it.id.hashCode().toLong() }) { clase ->
                                FilaClaseWidget(clase, colores)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaClase(clase: FilaClase, etiqueta: String, colores: ColoresWidget, dias: Int = 0) {
    val contexto = LocalContext.current
    Row(
        GlanceModifier.fillMaxWidth().cornerRadius(12.dp)
            .background(ColorProvider(clase.color.fondo.copy(alpha = 0.14f)))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(GlanceModifier.defaultWeight()) {
            Text(etiqueta, style = estiloHorario(10, colores.secundario, FontWeight.Bold), maxLines = 1)
            Text(clase.titulo, style = estiloHorario(15, colores.texto, FontWeight.Bold), maxLines = 1)
            Text(detalle(clase, dias), style = estiloHorario(12, colores.secundario), maxLines = 1)
        }
        if (clase.aula.isNotBlank() || clase.faltas != null) {
            Column(GlanceModifier.width(76.dp), horizontalAlignment = Alignment.End) {
                if (clase.aula.isNotBlank()) PastillaHorario(clase.aula, colores.texto, colores)
                clase.faltas?.let {
                    if (clase.aula.isNotBlank()) Spacer(GlanceModifier.height(3.dp))
                    PastillaHorario(textoFaltas(it, contexto), colores.texto, colores)
                }
            }
        }
    }
}

@Composable
private fun detalle(clase: FilaClase, dias: Int = 0, incluirAula: Boolean = false): String = listOfNotNull(
    when {
        dias <= 0 -> null
        dias == 1 -> textoWidget(R.string.manana)
        else -> app.kiwu.android.ui.Fechas.diaCorto(clase.diaSemana)
    },
    "${ReglasHorario.hora(clase.inicioMin)} – ${ReglasHorario.hora(clase.finMin)}",
    clase.aula.takeIf { incluirAula && it.isNotBlank() },
    clase.horario,
).joinToString(" · ")

@Composable
private fun FilaClaseWidget(clase: FilaClase, colores: ColoresWidget) {
    val contexto = LocalContext.current
    val alpha = if (clase.pasada) 0.55f else 1f
    val texto = ColorProvider(colores.texto.getColor(contexto).copy(alpha = alpha))
    val secundario = ColorProvider(colores.secundario.getColor(contexto).copy(alpha = alpha))
    val tonoMateria = clase.colorMateria?.let {
        if (colores.oscuro) it.fondo else it.texto
    } ?: colores.acento.getColor(contexto)
    Row(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(GlanceModifier.width(3.dp).height(34.dp).cornerRadius(2.dp)
            .background(ColorProvider(tonoMateria.copy(alpha = alpha)))) { }
        Column(GlanceModifier.defaultWeight().padding(horizontal = 8.dp)) {
            Text(clase.titulo, style = estiloHorario(14, texto, FontWeight.Medium), maxLines = 1)
            Text(detalle(clase, incluirAula = true), style = estiloHorario(12, secundario), maxLines = 1)
        }
        clase.faltas?.takeIf { it.maximo != null }?.let {
            Box(GlanceModifier.width(76.dp), contentAlignment = Alignment.CenterEnd) {
                PastillaHorario(textoFaltas(it, contexto), secundario, colores)
            }
        }
        clase.urgencia?.let {
            Spacer(GlanceModifier.width(4.dp))
            PuntoUrgencia(it, colores)
        }
    }
}

private fun textoFaltas(f: ContadorFaltas, contexto: Context): String =
    f.quedan?.takeIf { it > 0 }?.let {
        contexto.resources.getQuantityString(R.plurals.faltas_n, it, it)
    } ?: app.kiwu.android.ui.horario.textoCortoFaltas(f, contexto.resources)

@Composable
private fun PastillaHorario(etiqueta: String, color: ColorProvider, colores: ColoresWidget) {
    val fondo = colores.texto.getColor(LocalContext.current).copy(alpha = 0.09f)
    Text(
        etiqueta,
        GlanceModifier.cornerRadius(8.dp).background(ColorProvider(fondo)).padding(horizontal = 5.dp, vertical = 2.dp),
        style = estiloHorario(10, color, FontWeight.Medium),
        maxLines = 1,
    )
}

private fun estiloHorario(tamano: Int, color: ColorProvider, peso: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = FontFamily.SansSerif, fontSize = tamano.sp, color = color, fontWeight = peso)

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
    Text(texto, style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, color = colores.secundario), maxLines = 2)
}
