package app.kiwu.android.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.kiwu.android.KiwuApp
import kotlinx.coroutines.flow.first
import app.kiwu.android.R

/** Widget «Deberes»: las tareas pendientes, con su lista y fecha, y una casilla para completarlas sin abrir la app. */
class DeberesWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = (context.applicationContext as KiwuApp).repo
        // Lo leído aquí es el primer dibujo; después los datos se siguen en vivo: si solo se leyeran aquí, un widget con la
        // sesión abierta no vería las tareas nuevas ni las completadas hasta empezar otra sesión.
        val materias0 = repo.materias.first()
        val grupos0 = repo.grupos.first()
        val deberes0 = repo.deberes.first()
        val colores = coloresDelWidget(context)
        val textos = TextosWidget.de(context)
        provideContent {
            val materias by repo.materias.collectAsState(materias0)
            val grupos by repo.grupos.collectAsState(grupos0)
            val deberes by repo.deberes.collectAsState(deberes0)
            val config = currentState<Preferences>().configWidget()
            val datos = DatosWidgets.deberes(materias, grupos, deberes, config, System.currentTimeMillis(), textos)
            ContenidoDeberes(datos, colores, config)
        }
    }
}

class DeberesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DeberesWidget()
}

@Composable
private fun ContenidoDeberes(datos: DatosDeberes, colores: ColoresWidget, config: String?) {
    val contexto = LocalContext.current
    val alto = LocalSize.current.height
    // Filas que caben: al alto se le quitan los márgenes (28 dp), la cabecera (32 dp) y su separación (8 dp); cada fila mide 44 dp.
    val maximo = maxOf(1, ((alto.value - 68f) / 44f).toInt())
    Column(
        GlanceModifier.fillMaxSize().cornerRadius(18.dp).background(colores.fondo).padding(14.dp),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                GlanceModifier.defaultWeight().clickable(actionStartActivity(intencionDeApertura(contexto, seccion = "tareas"))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(textoWidget(R.string.deberes), style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colores.texto))
                if (datos.hayListas) {
                    Text("  ${datos.total}", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colores.acento))
                }
            }
            if (datos.hayListas) {
                // El espacio va aparte: como `padding` del propio botón quedaba dentro de sus 32 dp y descentraba el «+».
                Spacer(GlanceModifier.width(8.dp))
                Box(
                    GlanceModifier.size(32.dp).cornerRadius(16.dp).background(colores.acento)
                        .clickable(actionStartActivity(intencionNuevaTarea(
                            contexto, config?.takeIf { it.startsWith(ConfigWidget.LISTA) }?.removePrefix(ConfigWidget.LISTA), hoy = config == ConfigWidget.HOY,
                        ))),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        ImageProvider(R.drawable.ic_widget_mas), textoWidget(R.string.anadir_tarea),
                        GlanceModifier.size(18.dp), colorFilter = ColorFilter.tint(colores.sobreAcento),
                    )
                }
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        when {
            !datos.hayListas -> Aviso(textoWidget(R.string.w_crea_lista), colores)
            datos.pendientes.isEmpty() -> Aviso(textoWidget(R.string.w_sin_pendientes), colores)
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
    Row(GlanceModifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        // Toda la caja es el botón (44 dp, lo mínimo cómodo para un dedo), no solo el círculo.
        Box(
            GlanceModifier.size(44.dp)
                .clickable(actionRunCallback<CompletarDeber>(actionParametersOf(ClaveDeberWidget to fila.id))),
            contentAlignment = Alignment.Center,
        ) {
            Text("○", style = TextStyle(fontSize = 24.sp, color = colores.acento))
        }
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
