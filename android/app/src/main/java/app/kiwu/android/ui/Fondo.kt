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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import app.kiwu.android.data.AjustesUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.ref.SoftReference
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
 * Carga la imagen ajustada a la pantalla (recorte centrado): **un solo mapa de bits opaco del tamaño de la pantalla**, así
 * dibujarlo cuesta poco aunque la foto original sea enorme. La visibilidad y el velo se aplican al dibujar (no aquí), por
 * eso mover los deslizadores no obliga a cargar la imagen otra vez.
 */
internal fun cargarFondo(contexto: android.content.Context, uri: Uri, ancho: Int, alto: Int): Bitmap? = try {
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contexto.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
    // Se reduce a potencias de dos mientras la imagen siga cubriendo la pantalla.
    var muestreo = 1
    while (limites.outWidth / (muestreo * 2) >= ancho && limites.outHeight / (muestreo * 2) >= alto) muestreo *= 2
    val origen = contexto.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestreo }) }
    if (origen == null) null else {
        if (origen.width == ancho && origen.height == alto) origen else {
            val salida = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
            val escala = max(ancho.toFloat() / origen.width, alto.toFloat() / origen.height)
            val m = Matrix().apply {
                postScale(escala, escala)
                postTranslate((ancho - origen.width * escala) / 2f, (alto - origen.height * escala) / 2f)
            }
            Canvas(salida).drawBitmap(origen, m, Paint(Paint.FILTER_BITMAP_FLAG))
            origen.recycle()
            salida
        }
    }
} catch (e: Exception) {
    null
} catch (e: OutOfMemoryError) {
    null
}

/** Archivos propios del fondo: la app guarda su copia, así no depende del permiso ni del archivo original. */
object FondoArchivos {
    private fun carpeta(c: android.content.Context) = c.filesDir

    /** La foto elegida (reducida), para poder volver a encuadrarla sin elegirla otra vez. */
    fun origen(c: android.content.Context): File = File(carpeta(c), "fondo-origen.jpg")

    fun guardarOrigen(c: android.content.Context, uri: Uri): Boolean {
        val bitmap = cargarImagen(c, uri, 2400) ?: return false
        return try {
            origen(c).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            true
        } catch (e: Exception) {
            false
        } finally {
            bitmap.recycle()
        }
    }

    /** Guarda el encuadre (del tamaño de la pantalla) y devuelve su dirección. El nombre cambia cada vez para refrescar la caché. */
    fun guardarEncuadre(c: android.content.Context, bitmap: Bitmap): String? = try {
        val nuevo = File(carpeta(c), "fondo-${System.currentTimeMillis()}.jpg")
        nuevo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        carpeta(c).listFiles { f -> f.name.startsWith("fondo-") && f.name != nuevo.name && f.name != origen(c).name }?.forEach { it.delete() }
        Uri.fromFile(nuevo).toString()
    } catch (e: Exception) {
        null
    }

    fun borrar(c: android.content.Context) {
        carpeta(c).listFiles { f -> f.name.startsWith("fondo-") }?.forEach { it.delete() }
    }
}

/** Valores de los deslizadores mientras se arrastran: el fondo los usa al instante y se guardan al soltar. */
object FondoEnVivo {
    var visibilidad by mutableStateOf<Float?>(null)
    var velo by mutableStateOf<Float?>(null)
}

/** Última imagen cargada (por si se recrea la pantalla al cambiar de app): se reutiliza en vez de volver a leerla. */
private object CacheFondo {
    private var clave: String? = null
    private var referencia: SoftReference<Bitmap>? = null
    fun obtener(c: String): Bitmap? = if (clave == c) referencia?.get()?.takeIf { !it.isRecycled } else null
    fun poner(c: String, b: Bitmap) { clave = c; referencia = SoftReference(b) }
}

/** Fondo de la app: color liso, o la imagen elegida con un velo para que el contenido se lea. */
@Composable
fun FondoKiwu(ajustes: AjustesUi, contenido: @Composable () -> Unit) {
    val contexto = LocalContext.current
    val color = MaterialTheme.colorScheme.background
    val pantalla = contexto.resources.displayMetrics
    val clave = ajustes.fondoUri?.let { "$it|${pantalla.widthPixels}x${pantalla.heightPixels}" }
    var imagen by remember(clave) { mutableStateOf(clave?.let(CacheFondo::obtener)) }
    LaunchedEffect(clave) {
        if (clave == null || imagen != null) return@LaunchedEffect
        val cargada = withContext(Dispatchers.IO) { cargarFondo(contexto, Uri.parse(ajustes.fondoUri), pantalla.widthPixels, pantalla.heightPixels) }
        if (cargada != null) { CacheFondo.poner(clave, cargada); imagen = cargada }
    }
    val aparicion by animateFloatAsState(if (imagen != null) 1f else 0f, tween(350), label = "fondo")
    val visibilidad = FondoEnVivo.visibilidad ?: ajustes.fondoVisibilidad
    val velo = FondoEnVivo.velo ?: ajustes.opacidadPaneles
    Box(Modifier.fillMaxSize().background(color)) {
        imagen?.let {
            Image(
                it.asImageBitmap(), null,
                Modifier.fillMaxSize().graphicsLayer { alpha = aparicion * visibilidad.coerceIn(0f, 1f) },
                contentScale = ContentScale.FillBounds,
            )
            // El velo es el color de fondo con transparencia, encima de la imagen.
            Box(Modifier.fillMaxSize().background(color.copy(alpha = velo.coerceIn(0f, 1f) * aparicion)))
        }
        contenido()
    }
}
