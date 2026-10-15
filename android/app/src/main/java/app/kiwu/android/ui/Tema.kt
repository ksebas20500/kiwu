package app.kiwu.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import java.math.BigInteger
import kotlin.math.abs
import kotlin.math.roundToInt
import java.security.MessageDigest

private val Coral = Color(0xFFE8705A)

private val Claro = lightColorScheme(
    primary = Color(0xFFC44B35),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD2),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF77574F),
    secondaryContainer = Color(0xFFFFDAD2),
    onSecondaryContainer = Color(0xFF2C1511),
    tertiary = Color(0xFF6C5D2F),
    background = Color(0xFFFFF8F6),
    surface = Color(0xFFFFF8F6),
    surfaceVariant = Color(0xFFF5DDD8),
    onSurfaceVariant = Color(0xFF534340),
    error = Color(0xFFBA1A1A),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFFFFB4A5),
    onPrimary = Color(0xFF690004),
    primaryContainer = Color(0xFF93261A),
    onPrimaryContainer = Color(0xFFFFDAD2),
    secondary = Color(0xFFE7BDB3),
    secondaryContainer = Color(0xFF5D3F38),
    onSecondaryContainer = Color(0xFFFFDAD2),
    tertiary = Color(0xFFD8C58D),
    background = Color(0xFF1A1110),
    surface = Color(0xFF1A1110),
    surfaceVariant = Color(0xFF534340),
    onSurfaceVariant = Color(0xFFD8C2BD),
    error = Color(0xFFFFB4AB),
)

/** Un color de acento que se puede elegir en Ajustes. «coral» es el original de Kiwu. */
class Acento(val id: String, val titulo: String, val muestra: Color)

object Acentos {
    const val CORAL = "coral"
    const val SISTEMA = "sistema"

    val predefinidos = listOf(
        Acento(CORAL, "Coral", Coral),
        Acento("rojo", "Rojo", Color(0xFFD0382E)),
        Acento("naranja", "Naranja", Color(0xFFE07B10)),
        Acento("verde", "Verde", Color(0xFF2E8B57)),
        Acento("azul", "Azul", Color(0xFF2F6FD0)),
        Acento("morado", "Morado", Color(0xFF8A55C2)),
        Acento("rosa", "Rosa", Color(0xFFD14A8B)),
        Acento("grafito", "Grafito", Color(0xFF5F6B73)),
    )

    /** Color de un acento: predefinido por nombre, o personalizado como «#RRGGBB». null si no se reconoce. */
    fun muestra(id: String): Color? = predefinidos.firstOrNull { it.id == id }?.muestra ?: personalizado(id)

    fun personalizado(id: String): Color? {
        if (id.length != 7 || id[0] != '#') return null
        val v = id.substring(1).toLongOrNull(16) ?: return null
        return Color(0xFF000000 or v)
    }

    fun aTexto(c: Color): String = "#%06X".format(
        ((c.red * 255).roundToInt() shl 16) or ((c.green * 255).roundToInt() shl 8) or (c.blue * 255).roundToInt(),
    )
}

/** Tono (0..360) y saturación (0..1) de un color. */
internal fun tonoYSaturacion(c: Color): Pair<Float, Float> {
    val max = maxOf(c.red, c.green, c.blue)
    val min = minOf(c.red, c.green, c.blue)
    val d = max - min
    val tono = when {
        d == 0f -> 0f
        max == c.red -> 60f * (((c.green - c.blue) / d) % 6f)
        max == c.green -> 60f * ((c.blue - c.red) / d + 2f)
        else -> 60f * ((c.red - c.green) / d + 4f)
    }.let { if (it < 0f) it + 360f else it }
    val l = (max + min) / 2f
    val s = if (d == 0f) 0f else d / (1f - abs(2f * l - 1f))
    return tono to s.coerceIn(0f, 1f)
}

internal fun contraste(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}

/** Oscurece [luz] (en HSL) hasta que el color contraste al menos [minimo] con [contra]. */
private fun conContraste(tono: Float, sat: Float, luz: Float, contra: Color, minimo: Float): Color {
    var l = luz
    var c = Color.hsl(tono, sat, l)
    while (contraste(c, contra) < minimo && l > 0.05f) {
        l -= 0.02f
        c = Color.hsl(tono, sat, l)
    }
    return c
}

/** Esquema de colores derivado de un color de acento. El coral original se conserva tal cual. */
fun esquemaDe(acento: String, oscuro: Boolean): ColorScheme {
    val semilla = if (acento == Acentos.CORAL) null else Acentos.muestra(acento)
    if (semilla == null) return if (oscuro) Oscuro else Claro
    val (tono, saturacion) = tonoYSaturacion(semilla)
    val gris = saturacion < 0.15f
    val sa = if (gris) 0.08f else saturacion.coerceIn(0.30f, 0.85f)
    return if (oscuro) {
        val fondo = Color.hsl(tono, if (gris) 0.04f else 0.22f, 0.08f)
        darkColorScheme(
            primary = Color.hsl(tono, sa, 0.76f),
            onPrimary = Color.hsl(tono, sa, 0.14f),
            primaryContainer = conContraste(tono, sa, 0.30f, Color.hsl(tono, sa, 0.92f), 6f),
            onPrimaryContainer = Color.hsl(tono, sa, 0.92f),
            secondary = Color.hsl(tono, sa * 0.5f, 0.78f),
            onSecondary = Color.hsl(tono, sa * 0.5f, 0.15f),
            secondaryContainer = Color.hsl(tono, sa * 0.4f, 0.28f),
            onSecondaryContainer = Color.hsl(tono, sa * 0.5f, 0.92f),
            tertiary = Color.hsl((tono + 60f) % 360f, 0.40f, 0.76f),
            background = fondo, onBackground = Color.hsl(tono, 0.08f, 0.90f),
            surface = fondo, onSurface = Color.hsl(tono, 0.08f, 0.90f),
            surfaceVariant = Color.hsl(tono, if (gris) 0.04f else 0.16f, 0.27f),
            onSurfaceVariant = Color.hsl(tono, 0.12f, 0.80f),
            surfaceContainerLowest = Color.hsl(tono, 0.2f, 0.06f),
            surfaceContainerLow = Color.hsl(tono, 0.2f, 0.10f),
            surfaceContainer = Color.hsl(tono, 0.2f, 0.12f),
            surfaceContainerHigh = Color.hsl(tono, 0.18f, 0.16f),
            surfaceContainerHighest = Color.hsl(tono, 0.16f, 0.20f),
            outline = Color.hsl(tono, 0.08f, 0.60f),
            outlineVariant = Color.hsl(tono, 0.12f, 0.30f),
            error = Color(0xFFFFB4AB),
        )
    } else {
        val fondo = Color.hsl(tono, if (gris) 0.10f else 0.70f, 0.975f)
        lightColorScheme(
            primary = conContraste(tono, sa, 0.42f, Color.White, 4.8f),
            onPrimary = Color.White,
            primaryContainer = Color.hsl(tono, if (gris) 0.08f else 0.85f, 0.90f),
            onPrimaryContainer = Color.hsl(tono, sa, 0.12f),
            secondary = conContraste(tono, sa * 0.35f, 0.38f, Color.White, 4.8f),
            secondaryContainer = Color.hsl(tono, if (gris) 0.06f else 0.55f, 0.90f),
            onSecondaryContainer = Color.hsl(tono, sa * 0.5f, 0.12f),
            tertiary = conContraste((tono + 60f) % 360f, 0.35f, 0.38f, Color.White, 4.8f),
            background = fondo, onBackground = Color.hsl(tono, 0.10f, 0.10f),
            surface = fondo, onSurface = Color.hsl(tono, 0.10f, 0.10f),
            surfaceVariant = Color.hsl(tono, if (gris) 0.06f else 0.35f, 0.90f),
            onSurfaceVariant = Color.hsl(tono, 0.14f, 0.28f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color.hsl(tono, 0.6f, 0.96f),
            surfaceContainer = Color.hsl(tono, 0.5f, 0.94f),
            surfaceContainerHigh = Color.hsl(tono, 0.4f, 0.92f),
            surfaceContainerHighest = Color.hsl(tono, 0.35f, 0.90f),
            outline = Color.hsl(tono, 0.08f, 0.45f),
            outlineVariant = Color.hsl(tono, 0.20f, 0.82f),
            error = Color(0xFFBA1A1A),
        )
    }
}

@Composable
fun KiwuTheme(oscuro: Boolean = isSystemInDarkTheme(), acento: String = Acentos.CORAL, contenido: @Composable () -> Unit) {
    val contexto = LocalContext.current
    val esquema = if (acento == Acentos.SISTEMA && Build.VERSION.SDK_INT >= 31) {
        if (oscuro) dynamicDarkColorScheme(contexto) else dynamicLightColorScheme(contexto)
    } else {
        esquemaDe(acento, oscuro)
    }
    MaterialTheme(colorScheme = esquema, content = contenido)
}

/** Colores pastel de las tareas (los mismos que en macOS y Linux): fondo y texto. */
class ColorPastel(val fondo: Color, val texto: Color)

object Pasteles {
    val colores = listOf(
        ColorPastel(Color(1.00f, 0.85f, 0.75f), Color(0.55f, 0.28f, 0.12f)),  // melocotón
        ColorPastel(Color(1.00f, 0.90f, 0.72f), Color(0.55f, 0.35f, 0.05f)),  // albaricoque
        ColorPastel(Color(1.00f, 0.96f, 0.72f), Color(0.50f, 0.42f, 0.05f)),  // mantequilla
        ColorPastel(Color(1.00f, 0.80f, 0.78f), Color(0.60f, 0.22f, 0.20f)),  // coral
        ColorPastel(Color(0.99f, 0.83f, 0.87f), Color(0.58f, 0.22f, 0.35f)),  // rosa
        ColorPastel(Color(0.95f, 0.87f, 0.78f), Color(0.45f, 0.32f, 0.20f)),  // arena
        ColorPastel(Color(0.96f, 0.78f, 0.68f), Color(0.50f, 0.25f, 0.15f)),  // terracota claro
        ColorPastel(Color(0.98f, 0.88f, 0.62f), Color(0.50f, 0.36f, 0.00f)),  // miel
    )

    /** Índice estable según el id (mismo cálculo que Kiwu para Linux: MD5 del id módulo 8). */
    private val indices = java.util.concurrent.ConcurrentHashMap<String, Int>()

    fun indicePreferido(id: String): Int = indices.getOrPut(id) {
        val resumen = MessageDigest.getInstance("MD5").digest(id.toByteArray())
        BigInteger(1, resumen).mod(BigInteger.valueOf(colores.size.toLong())).toInt()
    }

    /** Cada tarea recibe un color estable; si dos tareas del mismo día coinciden, la segunda pasa al siguiente libre. */
    fun asignar(ids: List<String>): Map<String, ColorPastel> {
        val usados = mutableSetOf<Int>()
        val salida = LinkedHashMap<String, ColorPastel>()
        for (id in ids) {
            var indice = indicePreferido(id)
            if (usados.size < colores.size) {
                while (indice in usados) indice = (indice + 1) % colores.size
            }
            usados += indice
            salida[id] = colores[indice]
        }
        return salida
    }
}

/** Colores de texto y resaltado de las notas: mismos nombres y valores que en macOS y Linux. */
object ColoresNota {
    class Par(val claro: Color, val oscuro: Color)

    val colores = linkedMapOf(
        "gris" to Par(Color(0.45f, 0.45f, 0.45f), Color(0.62f, 0.62f, 0.62f)),
        "marron" to Par(Color(0.60f, 0.40f, 0.27f), Color(0.76f, 0.56f, 0.42f)),
        "naranja" to Par(Color(0.85f, 0.45f, 0.05f), Color(0.96f, 0.62f, 0.27f)),
        "amarillo" to Par(Color(0.74f, 0.58f, 0.00f), Color(0.96f, 0.80f, 0.27f)),
        "verde" to Par(Color(0.18f, 0.55f, 0.34f), Color(0.42f, 0.76f, 0.52f)),
        "azul" to Par(Color(0.20f, 0.45f, 0.80f), Color(0.42f, 0.66f, 0.96f)),
        "morado" to Par(Color(0.55f, 0.35f, 0.75f), Color(0.72f, 0.57f, 0.92f)),
        "rosa" to Par(Color(0.80f, 0.30f, 0.55f), Color(0.96f, 0.52f, 0.72f)),
        "rojo" to Par(Color(0.80f, 0.25f, 0.20f), Color(0.96f, 0.46f, 0.42f)),
    )

    val titulos = mapOf(
        "gris" to "Gris", "marron" to "Marrón", "naranja" to "Naranja", "amarillo" to "Amarillo", "verde" to "Verde",
        "azul" to "Azul", "morado" to "Morado", "rosa" to "Rosa", "rojo" to "Rojo",
    )

    fun texto(nombre: String?, oscuro: Boolean): Color? = colores[nombre]?.let { if (oscuro) it.oscuro else it.claro }

    /** Fondo del resaltado: el color con transparencia. */
    fun resaltado(nombre: String?, oscuro: Boolean): Color? = texto(nombre, oscuro)?.copy(alpha = if (oscuro) 0.35f else 0.28f)
}
