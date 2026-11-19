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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.kiwu.android.data.AjustesUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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
        nuevo.absolutePath
    } catch (e: Exception) {
        null
    }

    fun borrar(c: android.content.Context) {
        origen(c).delete()
        carpeta(c).listFiles { f -> f.name.startsWith("fondo-") }?.forEach { it.delete() }
    }
}

/** Valores de los deslizadores mientras se arrastran: el fondo los usa al instante y se guardan al soltar. */
object FondoEnVivo {
    var visibilidad by mutableStateOf<Float?>(null)
    var velo by mutableStateOf<Float?>(null)
}

private object CacheFondo {
    private var clave: String? = null
    private var referencia: java.lang.ref.SoftReference<Bitmap>? = null
    fun obtener(c: String): Bitmap? = if (clave == c) referencia?.get()?.takeIf { !it.isRecycled } else null
    fun poner(c: String, b: Bitmap) { clave = c; referencia = java.lang.ref.SoftReference(b) }
}

/**
 * Carga el bitmap del fondo desde fuera de Compose (p. ej. en MainActivity antes del primer frame).
 * El resultado puede pasarse a [FondoKiwu] como [fondoInicial] para evitar el flash de color sólido.
 */
fun cargarFondoPrevio(contexto: android.content.Context, uri: String): Bitmap? {
    val pantalla = contexto.resources.displayMetrics
    val reduccion = minOf(1f, 4096f / max(pantalla.widthPixels, pantalla.heightPixels))
    val anchoFondo = (pantalla.widthPixels * reduccion).toInt().coerceAtLeast(1)
    val altoFondo = (pantalla.heightPixels * reduccion).toInt().coerceAtLeast(1)
    return if (uri.startsWith("/")) {
        try {
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(uri, limites)
            var muestreo = 1
            while (limites.outWidth / (muestreo * 2) >= anchoFondo && limites.outHeight / (muestreo * 2) >= altoFondo) muestreo *= 2
            val origen = BitmapFactory.decodeFile(uri, BitmapFactory.Options().apply { inSampleSize = muestreo })
            if (origen == null) null else {
                val escala = max(anchoFondo.toFloat() / origen.width, altoFondo.toFloat() / origen.height)
                if (escala == 1f && origen.width == anchoFondo && origen.height == altoFondo) origen
                else {
                    val salida = Bitmap.createBitmap(anchoFondo, altoFondo, Bitmap.Config.ARGB_8888)
                    val m = Matrix().apply {
                        postScale(escala, escala)
                        postTranslate((anchoFondo - origen.width * escala) / 2f, (altoFondo - origen.height * escala) / 2f)
                    }
                    Canvas(salida).drawBitmap(origen, m, Paint(Paint.FILTER_BITMAP_FLAG))
                    origen.recycle()
                    salida
                }
            }
        } catch (e: Exception) { null } catch (e: OutOfMemoryError) { null }
    } else {
        try { cargarFondo(contexto, Uri.parse(uri), anchoFondo, altoFondo) } catch (e: Exception) { null }
    }
}

/** Fondo de la app: ambiente Kiwu (#FBFAF8 en claro, #16101E en oscuro + luces suaves) por defecto, o la imagen elegida. */
@Composable
fun FondoKiwu(ajustes: AjustesUi, fondoInicial: Bitmap? = null, contenido: @Composable () -> Unit) {
    val contexto = LocalContext.current
    val color = MaterialTheme.colorScheme.background
    val pantalla = contexto.resources.displayMetrics
    val reduccion = minOf(1f, 4096f / max(pantalla.widthPixels, pantalla.heightPixels))
    val anchoFondo = (pantalla.widthPixels * reduccion).toInt().coerceAtLeast(1)
    val altoFondo = (pantalla.heightPixels * reduccion).toInt().coerceAtLeast(1)
    val clave = ajustes.fondoUri?.let { "$it|${anchoFondo}x$altoFondo" }
    // fondoInicial viene precargado desde MainActivity: se usa como valor inicial para evitar el flash.
    var imagen by remember(clave) {
        mutableStateOf(
            clave?.let { CacheFondo.obtener(it) }
                ?: fondoInicial?.takeIf { clave != null }
        )
    }
    // Si la precarga ya llenó el bitmap, lo mete en la caché para reusos futuros.
    if (fondoInicial != null && clave != null && CacheFondo.obtener(clave) == null) {
        CacheFondo.poner(clave, fondoInicial)
    }
    LaunchedEffect(clave) {
        if (clave == null || imagen != null) return@LaunchedEffect
        val uri = ajustes.fondoUri ?: return@LaunchedEffect
        // fondoUri puede ser una ruta de archivo local (/data/…) o un content:// URI del selector del sistema.
        // Uri.parse sobre una ruta sin esquema falla en ContentResolver, así que se distinguen los dos casos.
        val cargada = withContext(Dispatchers.IO) {
            if (uri.startsWith("/")) {
                // Archivo propio de la app (encuadre guardado): se lee directo con BitmapFactory.
                try {
                    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(uri, limites)
                    var muestreo = 1
                    while (limites.outWidth / (muestreo * 2) >= anchoFondo && limites.outHeight / (muestreo * 2) >= altoFondo) muestreo *= 2
                    val origen = BitmapFactory.decodeFile(uri, BitmapFactory.Options().apply { inSampleSize = muestreo })
                    if (origen == null) null else {
                        val escala = max(anchoFondo.toFloat() / origen.width, altoFondo.toFloat() / origen.height)
                        if (escala == 1f && origen.width == anchoFondo && origen.height == altoFondo) origen
                        else {
                            val salida = android.graphics.Bitmap.createBitmap(anchoFondo, altoFondo, android.graphics.Bitmap.Config.ARGB_8888)
                            val m = android.graphics.Matrix().apply {
                                postScale(escala, escala)
                                postTranslate((anchoFondo - origen.width * escala) / 2f, (altoFondo - origen.height * escala) / 2f)
                            }
                            android.graphics.Canvas(salida).drawBitmap(origen, m, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
                            origen.recycle()
                            salida
                        }
                    }
                } catch (e: Exception) { null } catch (e: OutOfMemoryError) { null }
            } else {
                // URI content:// del selector del sistema.
                cargarFondo(contexto, Uri.parse(uri), anchoFondo, altoFondo)
            }
        }
        if (cargada != null) { CacheFondo.poner(clave, cargada); imagen = cargada }
    }
    val aparicion by animateFloatAsState(if (imagen != null) 1f else 0f, tween(350), label = "fondo")
    val visibilidad = FondoEnVivo.visibilidad ?: ajustes.fondoVisibilidad
    val velo = FondoEnVivo.velo ?: ajustes.opacidadPaneles
    val esOscuro = ajustes.tema == "oscuro" || (ajustes.tema == "sistema" && isSystemInDarkTheme())

    Box(Modifier.fillMaxSize().background(color)) {
        // Luces ambientales decorativas idénticas a las del Onboarding si no hay foto personalizada (en modo claro u oscuro)
        if (ajustes.fondoUri == null) {
            val purpleGlow = if (esOscuro) Color(0x3D7540A1) else Color(0x6BE4D2F2)
            val greenGlow = if (esOscuro) Color(0x352E8B57) else Color(0x73D6EEE2)

            Box(
                Modifier
                    .offset(x = 120.dp, y = 60.dp)
                    .size(260.dp)
                    .align(Alignment.TopEnd)
                    .background(purpleGlow, CircleShape)
            )
            Box(
                Modifier
                    .offset(x = (-100).dp, y = (-40).dp)
                    .size(220.dp)
                    .align(Alignment.BottomStart)
                    .background(greenGlow, CircleShape)
            )
        }

        imagen?.let {
            Image(
                it.asImageBitmap(), null,
                Modifier.fillMaxSize().graphicsLayer { alpha = aparicion * visibilidad.coerceIn(0f, 1f) },
                contentScale = ContentScale.FillBounds,
            )
            Box(Modifier.fillMaxSize().background(color.copy(alpha = velo.coerceIn(0f, 1f) * aparicion)))
        }
        contenido()
    }
}
