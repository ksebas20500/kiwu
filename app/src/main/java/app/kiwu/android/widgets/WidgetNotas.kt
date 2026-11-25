package app.kiwu.android.widgets

import android.content.Context
import androidx.compose.runtime.Composable
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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.kiwu.android.KiwuApp
import kotlinx.coroutines.flow.first
import app.kiwu.android.R

/** Widget «Nota rápida»: crea una nota en el cuaderno elegido y muestra las más recientes. */
class NotasWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as KiwuApp
        val cuadernos = app.repo.cuadernos.first()
        val paginas = app.repo.paginas.first()
        val colores = coloresDelWidget(context)
        val textos = TextosWidget.de(context)
        provideContent {
            val config = currentState<Preferences>().configWidget()
            ContenidoNotas(DatosWidgets.notas(cuadernos, paginas, config, textos), colores)
        }
    }
}

class NotasWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotasWidget()
}

@Composable
private fun ContenidoNotas(datos: DatosNotas, colores: ColoresWidget) {
    val contexto = LocalContext.current
    val alto = LocalSize.current.height
    val maximo = ((alto.value - 62f) / 48f).toInt().coerceAtLeast(0)
    val nueva = intencionDeApertura(contexto, nuevaNotaEnCuaderno = datos.cuadernoNuevo ?: "")
    Column(
        GlanceModifier.fillMaxSize().cornerRadius(18.dp).background(colores.fondo).padding(horizontal = 8.dp, vertical = 7.dp),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(
                GlanceModifier.defaultWeight().height(48.dp)
                    .clickable(actionStartActivity(intencionDeApertura(contexto, seccion = "notas"))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    datos.cuaderno?.let { "${it.icono} ${it.nombre}" } ?: textoWidget(R.string.notas),
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colores.texto),
                    maxLines = 1,
                )
                if (datos.cuaderno == null && datos.nombreDestino != null) {
                    Text(datos.nombreDestino, style = TextStyle(fontSize = 12.sp, color = colores.secundario), maxLines = 1)
                }
            }
            Box(
                GlanceModifier.cornerRadius(12.dp).background(colores.acento)
                    .height(48.dp).padding(horizontal = 10.dp)
                    .clickable(actionStartActivity(nueva)),
                contentAlignment = Alignment.Center,
            ) {
                Text(textoWidget(R.string.w_nueva), style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colores.sobreAcento))
            }
        }
        when {
            !datos.hayCuadernos -> AvisoNotas(textoWidget(R.string.w_crea_cuaderno), colores)
            datos.notas.isEmpty() -> AvisoNotas(textoWidget(R.string.w_sin_notas), colores)
            maximo > 0 -> LazyColumn(GlanceModifier.fillMaxSize()) {
                items(datos.notas.take(maximo), itemId = { it.id.hashCode().toLong() }) { nota ->
                    FilaNotaWidget(nota, colores)
                }
            }
        }
    }
}

@Composable
private fun FilaNotaWidget(nota: FilaNota, colores: ColoresWidget) {
    val contexto = LocalContext.current
    Column(
        GlanceModifier.fillMaxWidth().height(48.dp)
            .clickable(actionStartActivity(intencionDeApertura(contexto, nota = nota.id))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${nota.icono} ${nota.titulo}",
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colores.texto),
            maxLines = 1,
        )
        if (nota.resumen.isNotBlank()) {
            Text(
                nota.resumen,
                style = TextStyle(fontSize = 12.sp, color = colores.secundario),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AvisoNotas(texto: String, colores: ColoresWidget) {
    Text(texto, style = TextStyle(fontSize = 14.sp, color = colores.secundario), maxLines = 2)
}
