package app.kiwu.android

import app.kiwu.android.domain.Calificacion
import app.kiwu.android.domain.Calificaciones
import app.kiwu.android.domain.Examenes
import app.kiwu.android.domain.SesionEstudio
import app.kiwu.android.domain.TiempoEstudio
import app.kiwu.android.domain.Tarjeta
import app.kiwu.android.domain.Tarjetas
import app.kiwu.android.domain.Repaso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class EstudioTest {
    private val zona = ZoneId.of("America/Bogota")

    @Test
    fun elPromedioEsPonderadoPorPeso() {
        val notas = listOf(Calificacion("Parcial", 4.0, 3.0), Calificacion("Quiz", 2.0, 1.0))
        assertEquals(3.5, Calificaciones.promedio(notas)!!, 1e-9)
        assertNull(Calificaciones.promedio(emptyList()))
    }

    @Test
    fun calculaCorrectamenteNotaNecesariaYPorcentajes() {
        val notas = listOf(
            Calificacion("Parcial 1", 2.5, 20.0),
            Calificacion("Talleres 1", 4.0, 15.0),
            Calificacion("Parcial 2", 3.0, 20.0),
            Calificacion("Talleres 2", 3.33, 15.0)
        )
        assertEquals(70.0, Calificaciones.sumaPesos(notas), 1e-9)
        assertEquals(2.1995, Calificaciones.puntosAcumulados(notas), 1e-4)
        val req = Calificaciones.necesariaParaAprobar(notas, 3.0)!!
        assertEquals(2.6683333333333334, req, 1e-4)
    }

    @Test
    fun lasNotasSeGuardanYSeLeenIgual() {
        val notas = listOf(Calificacion("Parcial \"1\"", 4.5, 2.0), Calificacion("Taller", 5.0))
        assertEquals(notas, Calificaciones.leer(Calificaciones.escribir(notas)))
        assertEquals(emptyList<Calificacion>(), Calificaciones.leer("{dañado"))
    }

    @Test
    fun lasTarjetasSeGuardanYSeLeenIgual() {
        val tarjetas = listOf(Tarjeta("Capital de \"Perú\"", "Lima"), Tarjeta("H2O", "Agua, líquida"))
        assertEquals(tarjetas, Tarjetas.leer(Tarjetas.escribir(tarjetas)))
        assertEquals("", Tarjetas.escribir(emptyList()))
        assertEquals(emptyList<Tarjeta>(), Tarjetas.leer("{dañado"))
    }

    @Test
    fun lasTarjetasConRepasoGuardanSuCajaYLosViejosLasLeenSinElla() {
        val dia = 24 * 60 * 60 * 1000L
        val tarjetas = listOf(Tarjeta("P", "R", caja = 2, vence = 1_700_000_000_000L), Tarjeta("P2", "R2"))
        assertEquals(tarjetas, Tarjetas.leer(Tarjetas.escribir(tarjetas)))
        // Las versiones anteriores solo guardaban pregunta y respuesta: se leen como nuevas.
        assertEquals(listOf(Tarjeta("P", "R"), Tarjeta("P2", "R2")), Tarjetas.leer("""[{"p":"P","r":"R"},{"p":"P2","r":"R2"}]"""))
        // La caja no se escribe cuando es 0 y el vence tampoco (el JSON queda como antes).
        assertEquals("""[{"p":"P","r":"R"}]""", Tarjetas.escribir(listOf(Tarjeta("P", "R"))))
        // La caja 1 espera 1 día, la 2 dos días (los índices son la caja; en milisegundos se multiplica al usarla).
        assertEquals(1L, Repaso.DIAS[1])
        assertEquals(16L, Repaso.DIAS[5])
    }

    @Test
    fun elRepasoEspaciadoSubeCajasYLasFalladasVuelvenAEmpezar() {
        val ahora = 1_700_000_000_000L
        val dia = 24 * 60 * 60 * 1000L
        val nueva = Tarjeta("P", "R")
        // Las nuevas y las vencidas entran al repaso de hoy; las que aún esperan, no.
        val lista = listOf(nueva, Tarjeta("Q", "R2", caja = 3, vence = ahora - 1), Tarjeta("Z", "R3", caja = 4, vence = ahora + dia))
        assertEquals(listOf(nueva, lista[1]), Repaso.vencen(lista, ahora)) // la nueva (vence 0) va primero
        // Acertar sube de caja: 0→1 (mañana), y luego 3→4 (el doble de días), con tope en la última caja.
        assertEquals(Tarjeta("P", "R", caja = 1, vence = ahora + dia), Repaso.acertada(nueva, ahora))
        assertEquals(Tarjeta("P", "R", caja = 4, vence = ahora + 8 * dia), Repaso.acertada(Tarjeta("P", "R", caja = 3), ahora))
        assertEquals(Repaso.CAJAS - 1, Repaso.acertada(Tarjeta("P", "R", caja = 99), ahora).caja)
        // Fallar vuelve a empezar y queda pendiente ya mismo.
        assertEquals(Tarjeta("P", "R", caja = 0, vence = ahora), Repaso.fallada(Tarjeta("P", "R", caja = 4, vence = ahora + 8 * dia), ahora))
    }

    @Test
    fun losDiasDeUnExamenSeCuentanPorDiaNaturalNoPorHoras() {
        val ahora = LocalDateTime.of(2026, 10, 28, 23, 0).atZone(zona).toInstant().toEpochMilli()
        val manana = LocalDateTime.of(2026, 10, 29, 7, 0).atZone(zona).toInstant().toEpochMilli()
        val en5 = LocalDateTime.of(2026, 11, 2, 9, 0).atZone(zona).toInstant().toEpochMilli()
        assertEquals(1L, Examenes.diasRestantes(manana, ahora, zona))
        assertEquals(5L, Examenes.diasRestantes(en5, ahora, zona))
        assertEquals(0L, Examenes.diasRestantes(ahora, ahora, zona))
    }

    @Test
    fun laNotaNecesariaCompletaElPromedioConLoQueFalta() {
        val notas = listOf(Calificacion("Parcial", 3.0, 1.0), Calificacion("Quiz", 4.0, 1.0))
        // (3 + 4 + x·2) / 4 = 4  →  x = 4.5
        assertEquals(4.5, Calificaciones.necesaria(notas, 4.0, 2.0)!!, 1e-9)
        assertEquals(4.0, Calificaciones.necesaria(emptyList(), 4.0, 1.0)!!, 1e-9)
        assertNull(Calificaciones.necesaria(notas, 4.0, 0.0))
    }

    private fun ms(d: Int, h: Int) = LocalDateTime.of(2026, 11, d, h, 0).atZone(zona).toInstant().toEpochMilli()

    @Test
    fun elPlanDeRepasoVaA7_3Y1DiasSinLosQueYaPasaron() {
        val examen = ms(20, 8)
        assertEquals(listOf(ms(13, 18), ms(17, 18), ms(19, 18)), Examenes.sesionesRepaso(examen, ms(1, 9), zona))
        // A 5 días del examen ya no cabe el de 7 días; el de hoy a las 18:00 sí, si aún no son las 18:00.
        assertEquals(listOf(ms(17, 18), ms(19, 18)), Examenes.sesionesRepaso(examen, ms(15, 9), zona))
        assertEquals(listOf(ms(19, 18)), Examenes.sesionesRepaso(examen, ms(17, 19), zona))
    }

    @Test
    fun elTiempoDeEstudioSeSumaPorListaEnLaSemana() {
        val miercoles = java.time.LocalDate.of(2026, 10, 28)
        val lunes = miercoles.minusDays(2).toEpochDay()
        fun sesion(materiaId: String?, dia: Long, minutos: Int) = SesionEstudio(materiaId = materiaId, dia = dia, minutos = minutos)
        val s = listOf(
            sesion("mate", lunes, 25), sesion("mate", lunes + 2, 50), sesion(null, lunes + 6, 25),
            sesion("mate", lunes - 1, 25), // domingo anterior: otra semana
            sesion("fisica", lunes + 1, 10),
        )
        assertEquals(listOf("mate" to 75, null to 25, "fisica" to 10), TiempoEstudio.semana(s, miercoles))
        // Las sesiones podadas (borrado lógico) ya no cuentan.
        assertEquals(emptyList<Pair<String?, Int>>(), TiempoEstudio.semana(s.map { it.copy(eliminada = 1L) }, miercoles))
        // El formato viejo de preferencias se lee igual (para la importación al arrancar); el id se genera nuevo.
        val leidas = TiempoEstudio.leer("""[{"m":"x","d":"$lunes","n":"5"}]""")
        assertEquals("x", leidas.single().materiaId)
        assertEquals(lunes, leidas.single().dia)
        assertEquals(5, leidas.single().minutos)
        assertEquals(emptyList<SesionEstudio>(), TiempoEstudio.leer("{dañado"))
        assertEquals(emptyList<SesionEstudio>(), TiempoEstudio.leer(""))
    }

    @Test
    fun losMinutosHechosCuentanLoEstudiadoAunqueSePause() {
        // 50 min elegidos, quedaban 10 (tras pausar y reanudar da igual: el tiempo restante dice todo)
        assertEquals(39, TiempoEstudio.minutosHechos(50, 10 * 60_000L))
        // quedaba solo la marca del último minuto: el −1 redondea por lo bajo
        assertEquals(49, TiempoEstudio.minutosHechos(50, 30_000L))
        // una sesión completada no entra por aquí, pero el suelo por lo bajo se respeta igual
        assertEquals(0, TiempoEstudio.minutosHechos(1, 0))
    }
}
