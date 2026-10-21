package app.kiwu.android.ui

import android.content.Context
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import app.kiwu.android.KiwuApp
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val app = contexto.applicationContext as KiwuApp
    val permitido = remember { pasoLaCortesia(contexto) }
    val sinAnuncios by app.compras.sinAnuncios.collectAsState()
    if (!permitido || sinAnuncios) return
    var listo by remember { mutableStateOf(false) }
    var cargado by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Primero el consentimiento de privacidad (obligatorio en Europa): solo se piden anuncios si el usuario lo permite.
        if (pedirConsentimiento(contexto)) {
            withContext(Dispatchers.IO) { MobileAds.initialize(contexto) }
            listo = true
        }
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
                adUnitId = BuildConfig.BLOQUE_BANNER
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

/** Pide el consentimiento (UMP). Devuelve verdadero si ya se pueden pedir anuncios; en países sin obligación no muestra nada. */
private suspend fun pedirConsentimiento(contexto: Context): Boolean {
    var c = contexto
    while (c is android.content.ContextWrapper && c !is android.app.Activity) c = c.baseContext
    val actividad = c as? android.app.Activity ?: return false
    val info = UserMessagingPlatform.getConsentInformation(actividad)
    return suspendCancellableCoroutine { k ->
        info.requestConsentInfoUpdate(
            actividad, ConsentRequestParameters.Builder().build(),
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(actividad) { if (k.isActive) k.resume(info.canRequestAds()) } },
            { if (k.isActive) k.resume(info.canRequestAds()) },
        )
    }
}
