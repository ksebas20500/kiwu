package app.kiwu.android.widgets

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kiwu.android.KiwuApp
import app.kiwu.android.data.AjustesUi
import app.kiwu.android.ui.KiwuTheme
import app.kiwu.android.ui.tareas.CampoNuevaTarea
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * «Añadir tarea» del widget de deberes: una barra sobre el escritorio con el mismo campo que la app, sin abrir Kiwu. Al
 * crear la tarea se cierra; los widgets se actualizan solos porque siguen la base de datos.
 */
class NuevaTareaActivity : ComponentActivity() {
    override fun attachBaseContext(base: android.content.Context) = super.attachBaseContext(app.kiwu.android.Idioma.contexto(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as KiwuApp
        val lista = intent?.getStringExtra(EXTRA_LISTA)
        val vencenHoy = intent?.getBooleanExtra(EXTRA_HOY, false) == true
        val ajustes = runBlocking { withTimeoutOrNull(1500) { app.ajustes.flujo.first() } } ?: AjustesUi()
        val oscuro = when (ajustes.tema) {
            "claro" -> false
            "oscuro" -> true
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
        setContent {
            KiwuTheme(oscuro, ajustes.acento) {
                val materias by app.repo.materias.collectAsStateWithLifecycle(emptyList())
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { finish() },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    // Tocar la barra no la cierra: solo el fondo oscuro.
                    Surface(
                        Modifier.pointerInput(Unit) { detectTapGestures { } },
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        CampoNuevaTarea(
                            materias = materias,
                            fija = materias.firstOrNull { it.id == lista },
                            alCrear = { titulo, materiaId, fecha ->
                                // Si el widget solo muestra «Hoy», la tarea sin fecha no se vería: vence hoy a las 18:00 como en la app.
                                val entrega = fecha ?: if (vencenHoy) {
                                    LocalDate.now().atTime(LocalTime.of(18, 0)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                } else null
                                app.alcance.launch { app.repo.crearDeber(titulo, materiaId, entrega) }
                                finish()
                            },
                            enfocar = true,
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_LISTA = "lista"
        const val EXTRA_HOY = "hoy"
    }
}
