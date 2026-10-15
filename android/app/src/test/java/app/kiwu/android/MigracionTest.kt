package app.kiwu.android

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import app.kiwu.android.data.KiwuDb
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** La base de datos de la versión 1 (sin horarios) se actualiza sin perder nada y con el esquema esperado. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(application = android.app.Application::class)
class MigracionTest {
    @get:Rule
    val ayudante = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), KiwuDb::class.java)

    @Test
    fun deLaVersion1ALa2ConservaLosDatosYCreaLasTablasNuevas() {
        ayudante.createDatabase("migracion", 1).apply {
            execSQL("INSERT INTO materia (id, nombre, icono, orden, carpeta, creada, actualizada, eliminada) VALUES ('M1', 'Uni', 'book', 0, NULL, 1, 1, NULL)")
            execSQL("INSERT INTO deber (id, materia_id, titulo, completado, estado, fecha_entrega, recordatorio_horas, fecha_completado, nota, creada, actualizada, recordado, eliminada) VALUES ('D1', 'M1', 'Estudiar', 0, 0, NULL, 24, NULL, '{}', 1, 1, NULL, NULL)")
            close()
        }
        val db = ayudante.runMigrationsAndValidate("migracion", 2, true, KiwuDb.MIGRACION_1_2)
        db.query("SELECT titulo FROM deber").use {
            it.moveToFirst()
            assertEquals("Estudiar", it.getString(0))
        }
        db.execSQL("INSERT INTO horario (id, nombre, icono, orden, creada, actualizada, eliminada) VALUES ('H1', 'Uni', '🎓', 0, 1, 1, NULL)")
        db.execSQL("INSERT INTO clase (id, horario_id, materia_id, grupo, titulo, dia_semana, inicio_min, fin_min, aula, notas, aviso_min, creada, actualizada, eliminada) VALUES ('C1', 'H1', 'M1', 'G', 'Cálculo', 1, 480, 600, '', '', NULL, 1, 1, NULL)")
        db.query("SELECT COUNT(*) FROM clase").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
    }
}
