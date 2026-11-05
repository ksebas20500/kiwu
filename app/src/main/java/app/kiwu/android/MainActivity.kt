package app.kiwu.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import app.kiwu.android.data.AjustesUi
import app.kiwu.android.domain.Guia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.kiwu.android.ui.Raiz

class MainActivity : ComponentActivity() {
    // En Android 8–12, el idioma elegido en Ajustes (en 13+ lo aplica el sistema).
    override fun attachBaseContext(base: android.content.Context) = super.attachBaseContext(Idioma.contexto(base))

    /** Tarea que pidió abrir una notificación o un acceso directo (también los widgets). */
    private var abrirTarea by mutableStateOf<String?>(null)
    /** Página de notas que pidió abrir el widget de nota rápida. */
    private var abrirPagina by mutableStateOf<String?>(null)
    /** Cuaderno donde crear una nota nueva desde el widget (vacío = el primero que haya). */
    private var nuevaNota by mutableStateOf<String?>(null)
    /** Sección que pidió abrir un widget («tareas», «notas» u «horario»). */
    private var seccion by mutableStateOf<String?>(null)

    private var estadoInicial by mutableStateOf<Pair<AjustesUi, Boolean>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        splashScreen.setKeepOnScreenCondition { estadoInicial == null }
        
        Textos.actualizar(this)
        enableEdgeToEdge()
        // Al girar la pantalla se conserva el estado: no se vuelve a leer el intent (crearía otra nota rápida).
        if (savedInstanceState == null) leer(intent)
        val app = application as KiwuApp

        lifecycleScope.launch {
            val (inicial, guia) = withContext(Dispatchers.IO) {
                val ini = withTimeoutOrNull(1500) { app.ajustes.flujo.first() } ?: AjustesUi()
                val hayDat = if (ini.guiaVista) true else (withTimeoutOrNull(1500) { app.repo.hayDatos() } ?: true)
                if (!ini.guiaVista && hayDat) {
                    runCatching { app.ajustes.cambiar { it.copy(guiaVista = true) } }
                }
                val g = Guia.debeMostrarse(ini.guiaVista, hayDat)
                ini to g
            }
            estadoInicial = inicial to guia
        }

        setContent {
            val estado = estadoInicial
            if (estado != null) {
                Raiz(
                    app = app,
                    abrirTarea = abrirTarea,
                    abrirPagina = abrirPagina,
                    nuevaNota = nuevaNota,
                    seccion = seccion,
                    inicial = estado.first,
                    guia = estado.second,
                    alAtender = ::olvidarPedido,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as KiwuApp).sync.alPrimerPlano()
        // Por si un cambio hecho fuera (widget, notificación, hoja del día) no llegó a quitar su aviso.
        (application as KiwuApp).revisarAvisos()
    }

    override fun onStop() {
        (application as KiwuApp).sync.alSegundoPlano()
        super.onStop()
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
