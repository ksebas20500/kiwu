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
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.kiwu.android.KiwuApp
import kotlinx.coroutines.flow.first

/** Widget «Deberes»: las tareas pendientes, con su lista y fecha, y una casilla para completarlas sin abrir la app. */
class DeberesWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as KiwuApp
        val materias = app.repo.materias.first()
        val grupos = app.repo.grupos.first()
        val deberes = app.repo.deberes.first()
        val colores = coloresDelWidget(context)
        provideContent {
            val config = currentState<Preferences>().configWidget()
            val datos = DatosWidgets.deberes(materias, grupos, deberes, config, System.currentTimeMillis())
            ContenidoDeberes(datos, colores)
        }
    }
}

class DeberesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DeberesWidget()
}

@Composable
private fun ContenidoDeberes(datos: DatosDeberes, colores: ColoresWidget) {
    val contexto = LocalContext.current
    val alto = LocalSize.current.height
    val maximo = when {
        alto < 140.dp -> 1
        alto < 200.dp -> 3
        alto < 260.dp -> 5
        else -> 8
    }
    Column(
        GlanceModifier.fillMaxSize().cornerRadius(18.dp).background(colores.fondo).padding(14.dp),
    ) {
        Row(
            GlanceModifier.fillMaxWidth()
                .clickable(actionStartActivity(intencionDeApertura(contexto, seccion = "tareas"))),
        ) {
            Text("Deberes", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colores.texto))
            Spacer(GlanceModifier.defaultWeight())
            if (datos.hayListas) {
                Text("${datos.total}", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colores.acento))
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        when {
            !datos.hayListas -> Aviso("Crea una lista y añade tareas en Kiwu.", colores)
            datos.pendientes.isEmpty() -> Aviso("Sin pendientes 🎉", colores)
            else -> LazyColumn(GlanceModifier.fillMaxSize()) {
                items(datos.pendientes.take(maximo), itemId = { it.id.hashCode().toLong() }) { fila ->
                    FilaDeberWidget(fila, colores)
                }
            }
        }
    }
}

@Composable
private fun FilaDeberWidget(fila: FilaDeber, colores: ColoresWidget) {
    val contexto = LocalContext.current
    Row(GlanceModifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(
            "○",
            GlanceModifier.padding(end = 8.dp)
                .clickable(actionRunCallback<CompletarDeber>(actionParametersOf(ClaveDeberWidget to fila.id))),
            style = TextStyle(fontSize = 17.sp, color = colores.acento),
        )
        Column(
            GlanceModifier.defaultWeight()
                .clickable(actionStartActivity(intencionDeApertura(contexto, tarea = fila.id))),
        ) {
            Text(
                fila.titulo,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colores.texto),
                maxLines = 1,
            )
            if (fila.lista.isNotBlank() || fila.fecha.isNotBlank()) {
                Text(
                    listOf(fila.lista, fila.fecha).filter { it.isNotBlank() }.joinToString(" · "),
                    style = TextStyle(fontSize = 12.sp, color = if (fila.vencida) colores.rojo else colores.secundario),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun Aviso(texto: String, colores: ColoresWidget) {
    Text(texto, style = TextStyle(fontSize = 14.sp, color = colores.secundario), maxLines = 2)
}
