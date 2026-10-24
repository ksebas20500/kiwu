package app.kiwu.android

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ActualizacionesTest {
    private val contexto = ApplicationProvider.getApplicationContext<Context>()
    private val falso = FakeAppUpdateManager(contexto)
    private var ahora = 1_000_000_000L
    private lateinit var act: Actualizaciones

    @Before
    fun preparar() {
        contexto.getSharedPreferences("kiwu", Context.MODE_PRIVATE).edit().clear().commit()
        act = Actualizaciones(contexto, falso) { ahora }
    }

    private fun comprobar(forzar: Boolean = false): EstadoActualizacion? {
        var r: EstadoActualizacion? = null
        act.comprobar(forzar) { r = it }
        shadowOf(Looper.getMainLooper()).idle()
        return r
    }

    @Test
    fun seOfreceLaVersionNueva() {
        falso.setUpdateAvailable(12, AppUpdateType.FLEXIBLE)
        assertEquals(EstadoActualizacion.Disponible(12), comprobar())
        assertEquals(EstadoActualizacion.Disponible(12), act.estado.value)
    }

    @Test
    fun sinVersionNuevaNoSeMuestraNada() {
        falso.setUpdateNotAvailable()
        assertEquals(EstadoActualizacion.Nada, comprobar())
    }

    @Test
    fun masTardeEsperaUnDiaSalvoQueSeBusqueAMano() {
        falso.setUpdateAvailable(12, AppUpdateType.FLEXIBLE)
        comprobar()
        act.posponer()
        assertEquals(EstadoActualizacion.Nada, act.estado.value)
        assertEquals(EstadoActualizacion.Nada, comprobar())
        assertEquals(EstadoActualizacion.Disponible(12), comprobar(forzar = true))
        act.posponer()
        ahora += ReglasActualizacion.ESPERA_MS
        assertEquals(EstadoActualizacion.Disponible(12), comprobar())
    }

    @Test
    fun unaVersionMasNuevaSeOfreceAunqueSePospusieraLaAnterior() {
        falso.setUpdateAvailable(12, AppUpdateType.FLEXIBLE)
        comprobar()
        act.posponer()
        falso.setUpdateAvailable(13, AppUpdateType.FLEXIBLE)
        assertEquals(EstadoActualizacion.Disponible(13), comprobar())
    }

    @Test
    fun soloSeOfreceSiGooglePermiteElModoFlexible() {
        falso.setUpdateAvailable(12, AppUpdateType.IMMEDIATE)
        assertEquals(EstadoActualizacion.Nada, comprobar())
    }

    @Test
    fun reglas() {
        assertTrue(ReglasActualizacion.debeOfrecer(12, -1, 0, ahora))
        assertFalse(ReglasActualizacion.debeOfrecer(12, 12, ahora - 1000, ahora))
        assertTrue(ReglasActualizacion.debeOfrecer(12, 12, ahora - ReglasActualizacion.ESPERA_MS, ahora))
        assertTrue(ReglasActualizacion.debeOfrecer(13, 12, ahora, ahora))
    }
}
