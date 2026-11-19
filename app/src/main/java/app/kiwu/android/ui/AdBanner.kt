package app.kiwu.android.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.KiwuApp
import app.kiwu.android.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

@Composable
fun AdBanner(
    app: KiwuApp,
    modifier: Modifier = Modifier,
    adUnitId: String = "ca-app-pub-6922015805223220/1097570531",
) {
    val comprado by app.billing.comprado.collectAsStateWithLifecycle()
    if (comprado) return

    val context = LocalContext.current
    var sdkInicializado by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        MobileAds.initialize(context) {
            sdkInicializado = true
        }
    }

    if (!sdkInicializado) return

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else adUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
