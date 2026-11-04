package app.kiwu.android

import app.kiwu.android.data.RespaldoDrive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveTest {
    @Test
    fun `el cuerpo multipart lleva los metadatos y la copia entre limites`() {
        val cuerpo = RespaldoDrive.cuerpoMultipart("""{"name":"x"}""", """{"app":"kiwu"}""")
        val partes = cuerpo.split("\r\n")
        assertTrue(partes.first().startsWith("--"))
        assertTrue(partes.last().startsWith("--") && partes.last().endsWith("--"))
        assertEquals(2, partes.count { it.contains("Content-Type: application/json") })
        assertTrue(cuerpo.contains("""{"name":"x"}""") && cuerpo.contains("""{"app":"kiwu"}"""))
    }
}
