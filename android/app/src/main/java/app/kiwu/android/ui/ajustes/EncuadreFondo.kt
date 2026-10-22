package app.kiwu.android.ui.ajustes

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.kiwu.android.ui.FondoArchivos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Deja elegir qué parte de la foto se ve de fondo: se mueve con un dedo y se amplía con dos. El resultado es una imagen del
 * tamaño de la pantalla (la que luego se dibuja de fondo).
 */
@Composable
fun EncuadreFondo(alAceptar: (Bitmap) -> Unit, alCancelar: () -> Unit) {
    val contexto = LocalContext.current
    val pantalla = contexto.resources.displayMetrics
    val foto by produceFoto()
    var visor by remember { mutableStateOf(IntSize.Zero) }
    var escala by remember { mutableFloatStateOf(1f) }
    var desplazamiento by remember { mutableStateOf(Offset.Zero) }
    var iniciado by remember { mutableStateOf(false) }

    val imagen = foto
    // Escala mínima: la que cubre justo toda la pantalla (sin bandas).
    val minima = if (imagen == null || visor.width == 0) 1f else max(visor.width.toFloat() / imagen.width, visor.height.toFloat() / imagen.height).takeIf { it.isFinite() && it > 0f } ?: 1f

    // El límite inferior nunca puede pasar de 0: con el redondeo de decimales, `visor - foto * escala` puede salir un pelo
    // positivo y un rango vacío haría fallar la app.
    fun limitar(e: Float, o: Offset): Offset {
        val im = imagen ?: return o
        val x = o.x.coerceIn(minOf(0f, visor.width - im.width * e), 0f)
        val y = o.y.coerceIn(minOf(0f, visor.height - im.height * e), 0f)
        return Offset(x, y)
    }
    // Al cargar (y si cambia el tamaño de la ventana: girar la tablet, ventana flotante…) la foto empieza centrada y cubriendo.
    LaunchedEffect(imagen, visor) {
        if (imagen != null && visor.width > 0) {
            escala = minima
            desplazamiento = limitar(minima, Offset((visor.width - imagen.width * minima) / 2f, (visor.height - imagen.height * minima) / 2f))
            iniciado = true
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black).onSizeChanged { visor = it }) {
        if (imagen != null && iniciado) {
            val dibujo = remember(imagen) { imagen.asImageBitmap() }
            androidx.compose.foundation.Canvas(
                Modifier.fillMaxSize().pointerInput(imagen, visor) {
                    detectTransformGestures { centro, pan, zoom, _ ->
                        val nueva = (escala * zoom).takeIf { it.isFinite() }?.coerceIn(minima, minima * 6f) ?: escala
                        // El punto bajo los dedos se queda donde está al ampliar.
                        val o = Offset(
                            centro.x - (centro.x - desplazamiento.x) * (nueva / escala) + pan.x,
                            centro.y - (centro.y - desplazamiento.y) * (nueva / escala) + pan.y,
                        )
                        escala = nueva
                        desplazamiento = limitar(nueva, o)
                    }
                },
            ) {
                withTransform({
                    translate(desplazamiento.x, desplazamiento.y)
                    scale(escala, escala, pivot = Offset.Zero)
                }) { drawImage(dibujo, filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium) }
            }
        }
        Text(
            "Arrastra para mover · pellizca para ampliar",
            Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp)
                .background(Color.Black.copy(alpha = 0.55f), androidx.compose.foundation.shape.RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 6.dp),
            color = Color.White, style = MaterialTheme.typography.labelLarge,
        )
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            OutlinedButton(onClick = alCancelar) { Text("Cancelar", color = Color.White) }
            Button(
                enabled = imagen != null && iniciado,
                onClick = {
                    val im = imagen ?: return@Button
                    // Se dibuja la parte visible en una imagen del tamaño de la pantalla (como mucho 4096 px de lado, que es
                    // lo que cualquier tarjeta gráfica acepta). Si falla (poca memoria…) se vuelve atrás en vez de cerrar la app.
                    val reduccion = minOf(1f, 4096f / max(pantalla.widthPixels, pantalla.heightPixels))
                    val ancho = (pantalla.widthPixels * reduccion).toInt().coerceAtLeast(1)
                    val alto = (pantalla.heightPixels * reduccion).toInt().coerceAtLeast(1)
                    val salida = try {
                        val k = ancho.toFloat() / visor.width
                        Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888).also {
                            val m = Matrix().apply { postScale(escala * k, escala * k); postTranslate(desplazamiento.x * k, desplazamiento.y * k) }
                            Canvas(it).drawBitmap(im, m, Paint(Paint.FILTER_BITMAP_FLAG))
                        }
                    } catch (e: Exception) { null } catch (e: OutOfMemoryError) { null }
                    if (salida != null) alAceptar(salida) else alCancelar()
                },
            ) { Text("Listo") }
        }
    }
}

/** Lee la foto guardada (la copia propia de la app) fuera del hilo principal. */
@Composable
private fun produceFoto(): androidx.compose.runtime.State<Bitmap?> {
    val contexto = LocalContext.current
    return androidx.compose.runtime.produceState<Bitmap?>(null) {
        value = withContext(Dispatchers.IO) {
            try { BitmapFactory.decodeFile(FondoArchivos.origen(contexto).absolutePath) } catch (e: Exception) { null } catch (e: OutOfMemoryError) { null }
        }
    }
}
