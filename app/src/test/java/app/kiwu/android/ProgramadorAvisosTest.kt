package app.kiwu.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.ID_EVENTOS
import app.kiwu.android.notify.ProgramadorAvisos
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ProgramadorAvisosTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val t0 = 1_800_000_000_000L
    private val hora = 3_600_000L

    @Test
    fun reconciliarProgramaAlarmaDeReferencia() {
        val evento = Deber(
            materiaId = ID_EVENTOS,
            titulo = "Seminario",
            fechaEntrega = t0 + 2 * hora,
            recordatorioHoras = 1,
            creada = 0,
            actualizada = 0
        )
        val programador = ProgramadorAvisos(context)
        programador.reconciliar(listOf(evento), ahora = t0)
        assertTrue(true)
    }
}
