package app.kiwu.android

import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.Recordatorios
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class PosponerManianaTest {
    private val zona = ZoneId.of("America/Bogota")

    // miércoles 30 de septiembre de 2026, 15:00
    private val ahora = en(0, 15)

    private fun en(dias: Long, hora: Int, minuto: Int = 0): Long =
        LocalDateTime.of(2026, 9, 30, hora, minuto).plusDays(dias).atZone(zona).toInstant().toEpochMilli()

    private fun tarea(entrega: Long?) = Deber(materiaId = "m", titulo = "Informe", fechaEntrega = entrega, creada = 0, actualizada = 0)

    @Test
    fun unaEntregaDeMananaSuenaMananaALasOchoDeLaManana() {
        // Aviso de un día antes pospuesto: la entrega es mañana a las 18:00, así que suena a las 8:00.
        assertEquals(en(1, 8), Recordatorios.mananaPospuesto(tarea(en(1, 18)), ahora, zona))
    }

    @Test
    fun unaEntregaTempranoDeMananaSuenaMediaHoraAntes() {
        assertEquals(en(1, 6, 30), Recordatorios.mananaPospuesto(tarea(en(1, 7)), ahora, zona))
    }

    @Test
    fun unaEntregaDeHoyOSinFechaSuenaMananaALasOcho() {
        assertEquals(en(1, 8), Recordatorios.mananaPospuesto(tarea(en(0, 18)), ahora, zona))
        assertEquals(en(1, 8), Recordatorios.mananaPospuesto(tarea(null), ahora, zona))
    }

    @Test
    fun unaEntregaJustoDespuesDeMedianocheNoSeAdelantaAHoy() {
        // 00:10 de mañana: media hora antes sería hoy a las 23:40, no «mañana».
        assertEquals(en(1, 8), Recordatorios.mananaPospuesto(tarea(en(1, 0, 10)), ahora, zona))
    }
}
