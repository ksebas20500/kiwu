package app.kiwu.android.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

object NotebookCoverStorage {
    private fun directorio(context: Context): File {
        val dir = File(context.filesDir, "covers")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun archivoPortada(context: Context, notebookId: String): File {
        return File(directorio(context), "cover_${notebookId}.webp")
    }

    fun guardarPortada(context: Context, notebookId: String, uri: Uri): String? {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(stream) ?: return null
            stream.close()

            val maxAncho = 600
            val bitmapProcesado = if (original.width > maxAncho) {
                val alto = (original.height * (maxAncho.toFloat() / original.width)).roundToInt()
                Bitmap.createScaledBitmap(original, maxAncho, alto, true)
            } else {
                original
            }

            val destino = archivoPortada(context, notebookId)
            val out = FileOutputStream(destino)
            @Suppress("DEPRECATION")
            val format = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.WEBP
            }
            bitmapProcesado.compress(format, 80, out)
            out.flush()
            out.close()
            if (bitmapProcesado != original) {
                bitmapProcesado.recycle()
            }
            original.recycle()

            destino.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun borrarPortada(context: Context, notebookId: String) {
        try {
            val f = archivoPortada(context, notebookId)
            if (f.exists()) f.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
