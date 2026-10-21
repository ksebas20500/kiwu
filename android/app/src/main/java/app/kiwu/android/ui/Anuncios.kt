package app.kiwu.android.ui

import android.content.Context
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.kiwu.android.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Anuncio de PRUEBA de banner de Google. Cambiar por el bloque real antes de publicar. */
private const val BLOQUE_BANNER = "ca-app-pub-3940256099942544/9214589741"
private const val DIA_MS = 24L * 60 * 60 * 1000

/** Verdadero cuando ya pasaron los días de cortesía desde la primera vez que se abrió la app. */
private fun pasoLaCortesia(contexto: Context): Boolean {
    val prefs = contexto.getSharedPreferences("kiwu", Context.MODE_PRIVATE)
    val ahora = System.currentTimeMillis()
    val primero = prefs.getLong("primer_uso", 0L).takeIf { it > 0 } ?: ahora.also { prefs.edit().putLong("primer_uso", it).apply() }
    return ahora - primero >= BuildConfig.DIAS_SIN_ANUNCIOS * DIA_MS
}

/**
 * Banner fijo y pequeño. No se muestra durante los días de cortesía, ni con el teclado abierto, ni hasta que el anuncio
 * haya cargado (si no hay conexión o falla, no deja ningún hueco). El SDK se inicia fuera del hilo principal y solo después
 * de que la pantalla ya se ve, para no retrasar el arranque ni el cambio de pestaña.
 */
@Composable
fun BannerAnuncio(modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val permitido = remember { pasoLaCortesia(contexto) }
    if (!permitido) return
    var listo by remember { mutableStateOf(false) }
    var cargado by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { MobileAds.initialize(contexto) }
        listo = true
    }
    if (!listo) return
    val ancho = LocalConfiguration.current.screenWidthDp
    val tecladoAbierto = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            // Mientras no ha cargado (o con el teclado abierto) no ocupa sitio.
            .heightIn(max = if (cargado && !tecladoAbierto) AdSize.BANNER.height.dp * 2 else 0.dp),
        factory = { c ->
            AdView(c).apply {
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(c, ancho))
                adUnitId = BLOQUE_BANNER
                adListener = object : AdListener() {
                    override fun onAdLoaded() { cargado = true }
                    override fun onAdFailedToLoad(error: LoadAdError) { cargado = false }
                }
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { it.destroy() },
    )
}
