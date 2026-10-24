package app.kiwu.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.kiwu.android.data.AjustesUi
import app.kiwu.android.domain.Guia
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.kiwu.android.ui.Raiz

class MainActivity : ComponentActivity() {
    /** Tarea que pidió abrir una notificación o un acceso directo (también los widgets). */
    private var abrirTarea by mutableStateOf<String?>(null)
    /** Página de notas que pidió abrir el widget de nota rápida. */
    private var abrirPagina by mutableStateOf<String?>(null)
    /** Cuaderno donde crear una nota nueva desde el widget (vacío = el primero que haya). */
    private var nuevaNota by mutableStateOf<String?>(null)
    /** Sección que pidió abrir un widget («tareas», «notas» u «horario»). */
    private var seccion by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Al girar la pantalla se conserva el estado: no se vuelve a leer el intent (crearía otra nota rápida).
        if (savedInstanceState == null) leer(intent)
        val app = application as KiwuApp
        // Los ajustes (tema, color, fondo) se leen antes del primer fotograma: así no se ve un instante el aspecto por defecto.
        val inicial = runBlocking { withTimeoutOrNull(1500) { app.ajustes.flujo.first() } } ?: AjustesUi()
        // La guía de inicio solo sale la primera vez y con la app vacía; quien actualiza con datos ya no la ve.
        val hayDatos = if (inicial.guiaVista) true else runBlocking { withTimeoutOrNull(1500) { app.repo.hayDatos() } }
        if (!inicial.guiaVista && hayDatos == true) app.alcance.launch { app.ajustes.cambiar { it.copy(guiaVista = true) } }
        val guia = Guia.debeMostrarse(inicial.guiaVista, hayDatos ?: true)
        setContent {
            Raiz(
                app = app,
                abrirTarea = abrirTarea,
                abrirPagina = abrirPagina,
                nuevaNota = nuevaNota,
                seccion = seccion,
                inicial = inicial,
                guia = guia,
                alAtender = ::olvidarPedido,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        leer(intent)
    }

    private fun leer(intent: Intent?) {
        abrirTarea = intent?.getStringExtra(EXTRA_TAREA)
        abrirPagina = intent?.getStringExtra(EXTRA_NOTA)
        nuevaNota = if (intent?.hasExtra(EXTRA_NUEVA_NOTA) == true) intent.getStringExtra(EXTRA_NUEVA_NOTA).orEmpty() else null
        seccion = intent?.getStringExtra(EXTRA_SECCION)
    }

    /** Ya se navegó a lo que pedía el widget: no se repite al girar la pantalla ni al recomponer. */
    private fun olvidarPedido() {
        abrirTarea = null
        abrirPagina = null
        nuevaNota = null
        seccion = null
    }

    companion object {
        const val EXTRA_TAREA = "tarea"
        const val EXTRA_NOTA = "nota"
        const val EXTRA_NUEVA_NOTA = "nueva_nota"
        const val EXTRA_SECCION = "seccion"
    }
}
