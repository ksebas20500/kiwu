package app.kiwu.android

import app.kiwu.android.domain.Repeticion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeticionTest {
    @Test
    fun cadaRepeticionTocaSoloSusDias() {
        assertTrue((1..7).all { Repeticion.toca(Repeticion.TODOS_LOS_DIAS, it) })
        assertEquals(listOf(1, 2, 3, 4, 5), (1..7).filter { Repeticion.toca(Repeticion.LUNES_A_VIERNES, it) })
        assertEquals(listOf(6, 7), (1..7).filter { Repeticion.toca(Repeticion.FINES_DE_SEMANA, it) })
        assertFalse((1..7).any { Repeticion.toca(Repeticion.NINGUNA, it) })
    }
}
