package app.kiwu.android

import app.kiwu.android.domain.Etiquetas
import app.kiwu.android.domain.Paso
import app.kiwu.android.domain.Pasos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrganizarTest {
    @Test
    fun losPasosSeGuardanYSeLeenIgual() {
        val pasos = listOf(Paso("Leer capítulo \"3\", con comas", true), Paso("Resumir"))
        assertEquals(pasos, Pasos.leer(Pasos.escribir(pasos)))
        assertEquals("", Pasos.escribir(emptyList()))
    }

    @Test
    fun unTextoDeAyudaDanadoNoRompe() {
        assertEquals(emptyList<Paso>(), Pasos.leer("{no es json"))
        assertEquals(emptyList<Paso>(), Pasos.leer(""))
    }

    @Test
    fun elProgresoCuentaLosHechos() {
        assertEquals("1/2", Pasos.progreso(Pasos.escribir(listOf(Paso("a", true), Paso("b")))))
        assertNull(Pasos.progreso(""))
    }

    @Test
    fun lasEtiquetasSeLimpianYNoSeRepiten() {
        assertEquals("examen,lectura", Etiquetas.escribir(listOf(" examen ", "Examen", "lectura", "")))
        assertEquals("a b", Etiquetas.escribir(listOf("a,b")))
        assertEquals(listOf("examen", "lectura"), Etiquetas.leer("examen, lectura,"))
    }
}
