package app.kiwu.android.widgets

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
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
import androidx.glance.appwidget.lazy.itemsIndexed
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
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontFamily
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
    val maximo = ((alto.value - 61f) / 49f).toInt().coerceAtLeast(0)
    Box(
        GlanceModifier.fillMaxSize()
            .background(ImageProvider(R.drawable.widget_borde_cristal))
            .padding(1.dp),
    ) {
        Column(
            GlanceModifier.fillMaxSize().cornerRadius(17.dp).background(colores.fondo).padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    GlanceModifier.defaultWeight().height(48.dp).clickable(actionStartActivity(intencionDeApertura(contexto, seccion = "tareas"))),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(textoWidget(R.string.deberes), style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colores.texto))
                        if (datos.hayListas) {
                            Spacer(GlanceModifier.width(6.dp))
                            Text(
                                "${datos.total}",
                                GlanceModifier.cornerRadius(10.dp)
                                    .background(ColorProvider(colores.acento.getColor(contexto).copy(alpha = 0.18f)))
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colores.acento),
                                maxLines = 1,
                            )
                        }
                    }
                    if (config != null && config != ConfigWidget.TODAS && datos.titulo != textoWidget(R.string.deberes)) {
                        Text(datos.titulo, style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, color = colores.secundario), maxLines = 1)
                    }
                }
                if (datos.hayListas) {
                    Spacer(GlanceModifier.width(8.dp))
                    Box(
                        GlanceModifier.size(48.dp)
                            .clickable(actionStartActivity(intencionNuevaTarea(
                                contexto,
                                lista = config?.takeIf { it.startsWith(ConfigWidget.LISTA) }?.removePrefix(ConfigWidget.LISTA),
                                hoy = config == ConfigWidget.HOY || config == ConfigWidget.PROXIMOS,
                                carpeta = config?.takeIf { it.startsWith(ConfigWidget.GRUPO) }?.removePrefix(ConfigWidget.GRUPO),
                            ))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            GlanceModifier.size(38.dp).cornerRadius(12.dp)
                                .background(ColorProvider(colores.acento.getColor(contexto).copy(alpha = 0.22f))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                ImageProvider(R.drawable.ic_widget_mas), textoWidget(R.string.anadir_tarea),
                                GlanceModifier.size(18.dp), colorFilter = ColorFilter.tint(colores.acento),
                            )
                        }
                    }
                }
            }
            when {
                !datos.hayListas -> Aviso(textoWidget(R.string.w_crea_lista), colores)
                datos.pendientes.isEmpty() -> Aviso(textoWidget(R.string.w_sin_pendientes), colores)
                maximo > 0 -> LazyColumn(GlanceModifier.fillMaxSize()) {
                    val filas = datos.pendientes.take(maximo)
                    itemsIndexed(filas, itemId = { _, fila -> fila.id.hashCode().toLong() }) { indice, fila ->
                        Column {
                            FilaDeberWidget(fila, colores, mostrarCarpeta = config?.startsWith(ConfigWidget.GRUPO) != true)
                            if (indice < filas.lastIndex) {
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
}

@Composable
private fun FilaDeberWidget(fila: FilaDeber, colores: ColoresWidget, mostrarCarpeta: Boolean) {
    val contexto = LocalContext.current
    val ubicacion = if (mostrarCarpeta) fila.ubicacion else fila.lista
    val anchoFecha = (LocalSize.current.width.value * 0.23f).coerceIn(44f, 84f).dp
    Row(GlanceModifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        if (fila.esEvento) {
            // Cumpleaños, charlas y demás eventos propios: no se completan; la pastilla azul los distingue de las tareas.
            Box(GlanceModifier.size(48.dp).padding(7.dp), contentAlignment = Alignment.Center) {
                Box(
                    GlanceModifier.fillMaxSize().cornerRadius(15.dp).background(colores.eventoFondo),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("◆", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = colores.eventoGlifo))
                }
            }
        } else {
            val etiqueta = textoWidget(R.string.w_completar_tarea, fila.titulo)
            Box(
                GlanceModifier.size(48.dp).semantics { contentDescription = etiqueta }
                    .clickable(actionRunCallback<CompletarDeber>(actionParametersOf(ClaveDeberWidget to fila.id))),
                contentAlignment = Alignment.Center,
            ) {
                Text("○", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 24.sp, color = colores.acento))
            }
        }
        Spacer(GlanceModifier.width(8.dp))
        Row(
            GlanceModifier.defaultWeight().fillMaxHeight()
                .clickable(actionStartActivity(intencionDeApertura(contexto, tarea = fila.id))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(GlanceModifier.defaultWeight()) {
                Text(fila.titulo, style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colores.texto), maxLines = 1)
                if (ubicacion.isNotBlank()) {
                    Text(ubicacion, style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp,
                        color = if (fila.esEvento) colores.eventoTexto else colores.secundario), maxLines = 1)
                }
            }
            if (fila.fecha.isNotBlank()) {
                val colorFecha = when {
                    fila.vencida -> colores.rojo
                    fila.urgente -> colores.acento
                    else -> colores.secundario
                }
                Text(fila.fecha, GlanceModifier.padding(start = 6.dp).width(anchoFecha),
                    style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = colorFecha,
                        textAlign = androidx.glance.text.TextAlign.End), maxLines = 1)
            }
        }
    }
}

@Composable
private fun Aviso(texto: String, colores: ColoresWidget) {
    Text(texto, style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, color = colores.secundario), maxLines = 2)
}
