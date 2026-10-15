package app.kiwu.android.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import app.kiwu.android.data.AjustesUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/** Carga una imagen del dispositivo reducida a [maximo] píxeles de lado (para no gastar memoria con fotos grandes). */
internal fun cargarImagen(contexto: android.content.Context, uri: Uri, maximo: Int = 1600): Bitmap? = try {
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contexto.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
    var muestreo = 1
    while (limites.outWidth / (muestreo * 2) >= maximo || limites.outHeight / (muestreo * 2) >= maximo) muestreo *= 2
    contexto.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestreo }) }
} catch (e: Exception) {
    null
} catch (e: OutOfMemoryError) {
    null
}

/**
 * Carga la imagen ajustada a la pantalla (recorte centrado) y con el velo ya mezclado: el resultado es **un solo mapa de
 * bits opaco del tamaño de la pantalla**. Así dibujarlo cuesta lo mismo que un color liso, aunque la foto original sea
 * enorme, y el menú y el calendario se mueven con fluidez.
 */
internal fun cargarFondo(
    contexto: android.content.Context, uri: Uri, ancho: Int, alto: Int, colorFondo: Int, visibilidad: Float, velo: Float,
): Bitmap? = try {
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contexto.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
    // Se reduce a potencias de dos mientras la imagen siga cubriendo la pantalla.
    var muestreo = 1
    while (limites.outWidth / (muestreo * 2) >= ancho && limites.outHeight / (muestreo * 2) >= alto) muestreo *= 2
    val origen = contexto.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestreo }) }
    if (origen == null) null else {
        val salida = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(salida)
        lienzo.drawColor(colorFondo)
        val escala = max(ancho.toFloat() / origen.width, alto.toFloat() / origen.height)
        val m = Matrix().apply {
            postScale(escala, escala)
            postTranslate((ancho - origen.width * escala) / 2f, (alto - origen.height * escala) / 2f)
        }
        lienzo.drawBitmap(origen, m, Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = (visibilidad.coerceIn(0f, 1f) * 255).toInt() })
        // El velo es el color de fondo con transparencia, encima de la imagen.
        lienzo.drawColor((colorFondo and 0x00FFFFFF) or ((velo.coerceIn(0f, 1f) * 255).toInt() shl 24))
        origen.recycle()
        salida.setHasAlpha(false)
        salida
    }
} catch (e: Exception) {
    null
} catch (e: OutOfMemoryError) {
    null
}

/** Fondo de la app: color liso, o la imagen elegida con un velo para que el contenido se lea. */
@Composable
fun FondoKiwu(ajustes: AjustesUi, contenido: @Composable () -> Unit) {
    val contexto = LocalContext.current
    val color = MaterialTheme.colorScheme.background
    val pantalla = contexto.resources.displayMetrics
    val imagen by cargarAsync(
        ajustes.fondoUri, ajustes.fondoVisibilidad, ajustes.opacidadPaneles, color, pantalla.widthPixels, pantalla.heightPixels,
    ) {
        ajustes.fondoUri?.let {
            withContext(Dispatchers.IO) {
                cargarFondo(contexto, Uri.parse(it), pantalla.widthPixels, pantalla.heightPixels, color.toArgb(), ajustes.fondoVisibilidad, ajustes.opacidadPaneles)
            }
        }
    }
    val aparicion by animateFloatAsState(if (imagen != null) 1f else 0f, tween(350), label = "fondo")
    Box(Modifier.fillMaxSize().background(color)) {
        imagen?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize().graphicsLayer { alpha = aparicion }, contentScale = ContentScale.FillBounds) }
        contenido()
    }
}
