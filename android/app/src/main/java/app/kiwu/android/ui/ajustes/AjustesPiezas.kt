package app.kiwu.android.ui.ajustes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Una sección en la pantalla principal de Ajustes: icono, título y, debajo, el valor que tiene ahora. */
@Composable
fun FilaAjuste(icono: ImageVector, titulo: String, resumen: String, alPulsar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = alPulsar).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icono, null, tint = MaterialTheme.colorScheme.primary) }
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(
                resumen, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Los tres aspectos del menú de Tareas como tres mini-menús: se elige viendo cómo queda, sin leer una explicación. */
@Composable
fun SelectorAspectoMenu(actual: String, conContadores: Boolean, alElegir: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for ((id, titulo) in listOf("plegable" to "Plegable", "compacto" to "Compacto", "color" to "Con color")) {
            val elegido = actual == id
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        if (elegido) 2.dp else 1.dp,
                        if (elegido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.fillMaxWidth().height(88.dp).clickable { alElegir(id) }
                        .semantics { contentDescription = "$titulo" + if (elegido) " (elegido)" else "" },
                ) {
                    Column(
                        Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(if (id == "compacto") 2.dp else 8.dp),
                    ) {
                        MiniFila("📁", "Semestre", if (conContadores) "5" else null, id, carpeta = true)
                        MiniFila(if (id == "color") null else "", "Cálculo", if (conContadores) "3" else null, id, sangria = true)
                        MiniFila(if (id == "color") null else "", "Física", if (conContadores) "2" else null, id, sangria = true)
                    }
                }
                Text(
                    titulo, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelMedium,
                    color = if (elegido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Una fila de ejemplo. Con [icono] null y aspecto «color» lleva un punto de color; con "" no lleva nada. */
@Composable
private fun MiniFila(icono: String?, nombre: String, cuenta: String?, aspecto: String, sangria: Boolean = false, carpeta: Boolean = false) {
    Row(Modifier.padding(start = if (sangria) 8.dp else 0.dp), verticalAlignment = Alignment.CenterVertically) {
        when {
            carpeta -> Text(icono ?: "", fontSize = 11.sp)
            icono == null && aspecto == "color" -> Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
        Text(
            nombre, Modifier.weight(1f).padding(start = if (carpeta || aspecto == "color") 4.dp else 0.dp), fontSize = 11.sp, maxLines = 1,
            overflow = TextOverflow.Clip,
        )
        if (cuenta != null) Text(cuenta, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
