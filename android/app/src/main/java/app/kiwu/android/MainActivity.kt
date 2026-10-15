package app.kiwu.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.kiwu.android.data.AjustesUi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.kiwu.android.ui.Raiz

class MainActivity : ComponentActivity() {
    /** Tarea que pidió abrir una notificación o un acceso directo. */
    private var abrirTarea by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        abrirTarea = idDeTarea(intent)
        val app = application as KiwuApp
        // Los ajustes (tema, color, fondo) se leen antes del primer fotograma: así no se ve un instante el aspecto por defecto.
        val inicial = runBlocking { withTimeoutOrNull(1500) { app.ajustes.flujo.first() } } ?: AjustesUi()
        setContent { Raiz(app, abrirTarea, inicial) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        abrirTarea = idDeTarea(intent)
    }

    private fun idDeTarea(intent: Intent?): String? = intent?.getStringExtra(EXTRA_TAREA)

    companion object {
        const val EXTRA_TAREA = "tarea"
    }
}
