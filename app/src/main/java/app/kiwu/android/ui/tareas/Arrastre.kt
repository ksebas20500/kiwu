package app.kiwu.android.ui.tareas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import app.kiwu.android.domain.Columna

/** Columna del tablero bajo el dedo, o null si está fuera de todas (soltar ahí no cambia nada). */
internal fun columnaEn(punto: Offset, limites: Map<Columna, Rect>): Columna? =
    limites.entries.firstOrNull { it.value.contains(punto) }?.key

/**
 * Cuánto desplazar (en píxeles por fotograma) mientras se arrastra: negativo cerca del inicio, positivo cerca del final
 * y 0 en el centro. Crece de forma lineal al acercarse al borde de la zona [inicio, fin].
 */
internal fun velocidadDeDesplazamiento(posicion: Float, inicio: Float, fin: Float, borde: Float, maxima: Float): Float {
    if (fin - inicio < borde * 2) return 0f
    return when {
        posicion < inicio + borde -> -maxima * ((inicio + borde - posicion) / borde).coerceIn(0f, 1f)
        posicion > fin - borde -> maxima * ((posicion - (fin - borde)) / borde).coerceIn(0f, 1f)
        else -> 0f
    }
}
