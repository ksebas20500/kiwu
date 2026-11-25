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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import app.kiwu.android.KiwuApp
import app.kiwu.android.R
import app.kiwu.android.domain.ID_EVENTOS
import app.kiwu.android.data.AjustesUi
import app.kiwu.android.ui.Acentos
import app.kiwu.android.ui.KiwuTheme
import java.time.LocalDate

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
        val carpeta = intent?.getStringExtra(EXTRA_CARPETA)
        val conFechaInicial = intent?.getBooleanExtra(EXTRA_HOY, false) == true
        setContent {
            val ajustes by app.ajustes.flujo.collectAsStateWithLifecycle(AjustesUi())
            val oscuro = when (ajustes.tema) {
                "claro" -> false
                "oscuro", Acentos.NEGRO -> true
                else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            }
            KiwuTheme(oscuro, ajustes.acento, amoled = false) {
                val materias by app.repo.materias.collectAsStateWithLifecycle(emptyList())
                val grupos by app.repo.grupos.collectAsStateWithLifecycle(emptyList())
                val ubicaciones = remember(materias, grupos) { ubicacionesDeListas(materias, grupos) }
                val listas = remember(materias, lista, carpeta) {
                    val disponibles = materias.filter { it.id != ID_EVENTOS }
                    val fija = disponibles.firstOrNull { it.id == lista }
                    if (fija != null) listOf(fija)
                    else disponibles.sortedBy { carpeta != null && it.grupoId != carpeta }
                }
                var cerrarSolicitado by rememberSaveable { mutableStateOf(0) }
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { cerrarSolicitado++ },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    // Tocar la barra no la cierra: solo el fondo oscuro.
                    Surface(
                        Modifier.statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp).pointerInput(Unit) { detectTapGestures { } },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        CapturaTareaWidget(
                            listas = listas,
                            ubicaciones = ubicaciones,
                            tituloFormulario = stringResource(R.string.anadir_tarea),
                            alCerrar = ::finish,
                            fechaInicial = if (conFechaInicial) entregaWidget(LocalDate.now()) else null,
                            cerrarSolicitado = cerrarSolicitado,
                            cerrarDirectamente = true,
                            guardar = { titulo, materiaId, fecha, prioridad, descripcion ->
                                app.repo.crearDeber(titulo, materiaId, fecha, prioridad, descripcion)
                            },
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_LISTA = "lista"
        const val EXTRA_CARPETA = "carpeta"
        const val EXTRA_HOY = "hoy"
    }
}
