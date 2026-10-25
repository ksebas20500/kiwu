package app.kiwu.android.ui.horario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import app.kiwu.android.domain.Clase
import app.kiwu.android.domain.ReglasHorario
import app.kiwu.android.ui.ColorPastel
import app.kiwu.android.ui.Pasteles
import app.kiwu.android.R
import androidx.compose.ui.res.stringResource

/** Colores que se pueden elegir para una clase: sólidos, claros y los pasteles de siempre (el automático es «sin color»). */
object ColoresClase {
    /** [nombre] es un recurso de texto; [numero] o [base] completan los que llevan un dato («Pastel 3», «Rojo claro»). */
    class Opcion(@androidx.annotation.StringRes val nombre: Int, val hex: String, val numero: Int? = null, @androidx.annotation.StringRes val base: Int? = null)

    private fun claro(@androidx.annotation.StringRes base: Int, hex: String) = Opcion(R.string.color_claro_n, hex, base = base)

    val solidos = listOf(
        Opcion(R.string.color_rojo, "#E53935"), Opcion(R.string.color_naranja, "#FB8C00"), Opcion(R.string.color_ambar, "#FFB300"),
        Opcion(R.string.color_verde, "#43A047"), Opcion(R.string.color_turquesa, "#00897B"), Opcion(R.string.color_azul, "#1E88E5"),
        Opcion(R.string.color_indigo, "#3949AB"), Opcion(R.string.color_morado, "#8E24AA"), Opcion(R.string.color_rosa, "#D81B60"),
        Opcion(R.string.color_gris, "#607D8B"),
    )
    val claros = listOf(
        claro(R.string.color_rojo, "#FFCDD2"), claro(R.string.color_naranja, "#FFE0B2"), claro(R.string.color_amarillo, "#FFF3C4"),
        claro(R.string.color_verde, "#C8E6C9"), claro(R.string.color_turquesa, "#B2DFDB"), claro(R.string.color_azul, "#BBDEFB"),
        claro(R.string.color_indigo, "#C5CAE9"), claro(R.string.color_morado, "#E1BEE7"), claro(R.string.color_rosa, "#F8BBD0"),
        claro(R.string.color_gris, "#CFD8DC"),
    )
    val pasteles = Pasteles.colores.mapIndexed { i, c -> Opcion(R.string.color_pastel_n, hex(c.fondo), numero = i + 1) }

    fun hex(c: Color): String = "#%06X".format(c.toArgb() and 0xFFFFFF)

    /** `#RRGGBB` → color. Se hace sin `android.graphics` para poder usarlo también en las pruebas JVM (widgets). */
    private fun deHex(hex: String): Color = Color(0xFF000000L or hex.removePrefix("#").toLong(16))

    /** Texto legible sobre ese fondo: casi negro del mismo tono si el fondo es claro, blanco si es oscuro. */
    fun contraste(fondo: Color): Color = if (fondo.luminance() > 0.45f) lerp(fondo, Color.Black, 0.72f) else Color.White

    /** El color con el que se dibuja una clase: el elegido o, si no hay, el automático de siempre. */
    fun de(c: Clase): ColorPastel {
        val elegido = ReglasHorario.colorValido(c.color)
        if (elegido != null) {
            val fondo = deHex(elegido)
            return ColorPastel(fondo, contraste(fondo))
        }
        return Pasteles.colores[Pasteles.indicePreferido(c.materiaId ?: c.grupo)]
    }
}

/** Selector de color de una clase: «Automático», sólidos, claros, pasteles y «Otro color». */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SelectorColorClase(actual: String?, alElegir: (String?) -> Unit, alOtro: () -> Unit, textoAutomatico: String = stringResource(R.string.color_automatico)) {
    val activo = ReglasHorario.colorValido(actual)
    @Composable
    fun Grupo(titulo: String, opciones: List<ColoresClase.Opcion>) {
        Text(titulo, style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (o in opciones) Muestra(o.hex, nombreColor(o), activo == o.hex) { alElegir(o.hex) }
        }
    }
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(if (activo == null) 3.dp else 1.dp, if (activo == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { alElegir(null) },
                contentAlignment = Alignment.Center,
            ) { Text("A", style = MaterialTheme.typography.labelLarge) }
            Text(textoAutomatico, Modifier.align(Alignment.CenterVertically), style = MaterialTheme.typography.bodyMedium)
        }
        Grupo(stringResource(R.string.colores_solidos), ColoresClase.solidos)
        Grupo(stringResource(R.string.colores_claros), ColoresClase.claros)
        Grupo(stringResource(R.string.colores_pasteles), ColoresClase.pasteles)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val propio = activo != null && ColoresClase.solidos.none { it.hex == activo } && ColoresClase.claros.none { it.hex == activo } && ColoresClase.pasteles.none { it.hex == activo }
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(if (propio) 3.dp else 1.dp, if (propio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable(onClick = alOtro),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Palette, stringResource(R.string.color_otro), Modifier.size(18.dp)) }
            Text(stringResource(R.string.color_otro_mas), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun nombreColor(o: ColoresClase.Opcion): String = when {
    o.numero != null -> stringResource(o.nombre, o.numero)
    o.base != null -> stringResource(o.nombre, stringResource(o.base).lowercase()).replaceFirstChar { it.uppercase() }
    else -> stringResource(o.nombre)
}

@Composable
private fun Muestra(hex: String, nombre: String, elegida: Boolean, alPulsar: () -> Unit) {
    val color = Color(android.graphics.Color.parseColor(hex))
    Box(
        Modifier.size(34.dp).clip(CircleShape).background(color)
            .border(if (elegida) 3.dp else 1.dp, if (elegida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = alPulsar),
        contentAlignment = Alignment.Center,
    ) {
        if (elegida) Icon(Icons.Filled.Check, nombre, Modifier.size(18.dp), tint = ColoresClase.contraste(color))
    }
}
