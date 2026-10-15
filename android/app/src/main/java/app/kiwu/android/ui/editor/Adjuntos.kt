package app.kiwu.android.ui.editor

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import app.kiwu.android.domain.Enlaces
import app.kiwu.android.domain.VistaPrevia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val AGENTE = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

/** Descarga con límite de tamaño. Solo http/https. */
internal fun descargar(direccion: String, maxBytes: Int): ByteArray? {
    if (!direccion.startsWith("http://") && !direccion.startsWith("https://")) return null
    return try {
        val c = URL(direccion).openConnection() as HttpURLConnection
        c.connectTimeout = 8_000
        c.readTimeout = 12_000
        c.setRequestProperty("User-Agent", AGENTE)
        c.setRequestProperty("Accept-Language", "es,en;q=0.8")
        c.instanceFollowRedirects = true
        try {
            if (c.responseCode !in 200..299) return null
            val salida = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            c.inputStream.use { entrada ->
                while (true) {
                    val n = entrada.read(buffer)
                    if (n < 0) break
                    salida.write(buffer, 0, n)
                    if (salida.size() > maxBytes) return null
                }
            }
            salida.toByteArray()
        } finally {
            c.disconnect()
        }
    } catch (e: Exception) {
        null
    }
}

/** Reduce una imagen a un máximo de [lado] píxeles y la guarda en JPEG (para la vista previa de un enlace). */
internal fun reducirImagen(datos: ByteArray, lado: Int = 480, calidad: Int = 80): ByteArray? {
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(datos, 0, datos.size, limites)
    if (limites.outWidth <= 0) return null
    var muestreo = 1
    while (limites.outWidth / (muestreo * 2) >= lado) muestreo *= 2
    val bmp = BitmapFactory.decodeByteArray(datos, 0, datos.size, BitmapFactory.Options().apply { inSampleSize = muestreo }) ?: return null
    val escala = lado.toFloat() / maxOf(bmp.width, bmp.height)
    val final = if (escala < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * escala).toInt(), (bmp.height * escala).toInt(), true) else bmp
    return ByteArrayOutputStream().also { final.compress(Bitmap.CompressFormat.JPEG, calidad, it) }.toByteArray()
}

/** Imagen adjunta: se reduce y se guarda dentro de la nota (en base64), igual que en macOS y Linux. */
fun imagenParaNota(contexto: Context, uri: Uri): String? = try {
    contexto.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?.let { reducirImagen(it, lado = 1600, calidad = 82) }
        ?.let { java.util.Base64.getEncoder().encodeToString(it) }
} catch (e: Exception) {
    null
}

private class Pendientes {
    var imagen: ((Uri) -> Unit)? = null
    var archivo: ((Uri) -> Unit)? = null
}

/** Nombre visible de un archivo elegido con el selector del sistema. */
fun nombreDeArchivo(contexto: Context, uri: Uri): String? = try {
    contexto.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    }
} catch (e: Exception) {
    null
} ?: uri.lastPathSegment

@Composable
fun rememberOrigenAdjuntos(): OrigenAdjuntos {
    val contexto = LocalContext.current
    // Las respuestas de los selectores llegan más tarde: se guardan en un objeto que sobrevive a las recomposiciones.
    val pendientes = remember { Pendientes() }
    val lanzadorImagen = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { pendientes.imagen?.invoke(it) } }
    val lanzadorArchivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            // Permiso persistente: la nota guarda la referencia, no copia el archivo.
            try {
                contexto.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) {
                // no todos los proveedores lo ofrecen
            }
            pendientes.archivo?.invoke(it)
        }
    }
    val contextoActual by rememberUpdatedState(contexto)
    return remember {
        OrigenAdjuntos(
            elegirImagen = { callback ->
                pendientes.imagen = callback
                lanzadorImagen.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            elegirArchivo = { callback ->
                pendientes.archivo = callback
                lanzadorArchivo.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "*/*"))
            },
            abrirArchivo = { uri, mime ->
                val intento = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), mime ?: "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                try {
                    contextoActual.startActivity(intento)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(contextoActual, "No hay una app para abrir este archivo", Toast.LENGTH_SHORT).show()
                }
            },
            abrirUrl = { url ->
                try {
                    contextoActual.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(contextoActual, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
                }
            },
            vistaPrevia = { url -> withContext(Dispatchers.IO) { Enlaces.obtener(url, ::descargar, ::reducirImagen) } },
        )
    }
}
