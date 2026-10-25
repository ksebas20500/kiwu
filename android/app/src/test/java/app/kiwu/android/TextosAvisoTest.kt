package app.kiwu.android

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.ID_EVENTOS
import app.kiwu.android.domain.Recordatorios
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** El texto de las notificaciones de tareas y eventos, en español (la base) y en inglés. */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class TextosAvisoTest {
    private val t0 = 1_800_000_000_000L
    private val hora = 3_600_000L
    private val r: Resources get() = ApplicationProvider.getApplicationContext<Context>().resources

    private fun tarea(fecha: Long, materia: String = "M") = Deber(materiaId = materia, titulo = "x", fechaEntrega = fecha, creada = 0, actualizada = 0)

    @Test
    @Config(qualifiers = "es")
    fun enEspanol() {
        val d = tarea(t0 + 5 * hora)
        assertEquals("Vence en 5 h", Recordatorios.textoAviso(d, t0, r))
        assertEquals("Vence en 30 min", Recordatorios.textoAviso(d, t0 + (4.5 * hora).toLong(), r))
        assertEquals("Venció hace 10 min", Recordatorios.textoAviso(d, t0 + 5 * hora + 600_000, r))
        assertEquals("Vence mañana", Recordatorios.textoAviso(d.copy(fechaEntrega = t0 + 30 * hora), t0, r))
        // 7 minutos de más: así un evento nunca cae en la medianoche de la zona del equipo que corre la prueba.
        val inicio = t0 + 5 * hora + 7 * 60_000L
        val e = tarea(inicio, ID_EVENTOS)
        assertEquals("Empieza en 5 h", Recordatorios.textoAviso(e, t0, r))
        assertEquals("Empezó hace 10 min", Recordatorios.textoAviso(e, inicio + 600_000, r))
    }

    @Test
    @Config(qualifiers = "en")
    fun enIngles() {
        val d = tarea(t0 + 5 * hora)
        assertEquals("Due in 5 h", Recordatorios.textoAviso(d, t0, r))
        assertEquals("Due tomorrow", Recordatorios.textoAviso(d.copy(fechaEntrega = t0 + 30 * hora), t0, r))
        assertEquals("Starts in 5 h", Recordatorios.textoAviso(tarea(t0 + 5 * hora + 7 * 60_000L, ID_EVENTOS), t0, r))
    }
}
