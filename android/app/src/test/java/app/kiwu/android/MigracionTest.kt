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

    /** La versión 2 (con horarios) pasa a la 3 sin tocar ningún dato: las clases ganan un color vacío (= automático). */
    @Test
    fun deLaVersion2ALa3ConservaLasClasesYAnadeElColorVacio() {
        ayudante.createDatabase("migracion2", 2).apply {
            execSQL("INSERT INTO materia (id, nombre, icono, orden, carpeta, creada, actualizada, eliminada) VALUES ('M1', 'Uni', 'book', 0, NULL, 1, 1, NULL)")
            execSQL("INSERT INTO horario (id, nombre, icono, orden, creada, actualizada, eliminada) VALUES ('H1', 'Uni', '🎓', 0, 1, 1, NULL)")
            execSQL("INSERT INTO clase (id, horario_id, materia_id, grupo, titulo, dia_semana, inicio_min, fin_min, aula, notas, aviso_min, creada, actualizada, eliminada) VALUES ('C1', 'H1', 'M1', 'G', 'Cálculo', 1, 480, 600, 'A-1', 'nota', 10, 1, 1, NULL)")
            close()
        }
        val db = ayudante.runMigrationsAndValidate("migracion2", 3, true, KiwuDb.MIGRACION_2_3)
        db.query("SELECT titulo, materia_id, aula, notas, aviso_min, color FROM clase").use {
            assertEquals(1, it.count)
            it.moveToFirst()
            assertEquals("Cálculo", it.getString(0))
            assertEquals("M1", it.getString(1))
            assertEquals("A-1", it.getString(2))
            assertEquals("nota", it.getString(3))
            assertEquals(10, it.getInt(4))
            assertEquals(true, it.isNull(5))
        }
        db.execSQL("UPDATE clase SET color = '#E53935' WHERE id = 'C1'")
    }

    /** Desde la versión 1 se llega a la 3 pasando por las dos migraciones. */
    @Test
    fun deLaVersion1ALa3EnUnSoloPaso() {
        ayudante.createDatabase("migracion13", 1).apply { close() }
        ayudante.runMigrationsAndValidate("migracion13", 3, true, KiwuDb.MIGRACION_1_2, KiwuDb.MIGRACION_2_3)
    }

    /** La versión 3 pasa a la 4: aparecen las carpetas de listas y las listas existentes quedan sueltas, sin perder nada. */
    @Test
    fun deLaVersion3ALa4ConservaLasListasYCreaLasCarpetas() {
        ayudante.createDatabase("migracion3", 3).apply {
            execSQL("INSERT INTO materia (id, nombre, icono, orden, carpeta, creada, actualizada, eliminada) VALUES ('M1', 'Uni', 'book', 2, 'content://x', 1, 1, NULL)")
            execSQL("INSERT INTO deber (id, materia_id, titulo, completado, estado, fecha_entrega, recordatorio_horas, fecha_completado, nota, creada, actualizada, recordado, eliminada) VALUES ('D1', 'M1', 'Estudiar', 0, 0, NULL, 24, NULL, '{}', 1, 1, NULL, NULL)")
            close()
        }
        val db = ayudante.runMigrationsAndValidate("migracion3", 4, true, KiwuDb.MIGRACION_3_4)
        db.query("SELECT nombre, orden, carpeta, grupo_id, emoji FROM materia").use {
            assertEquals(1, it.count)
            it.moveToFirst()
            assertEquals("Uni", it.getString(0)); assertEquals(2, it.getInt(1)); assertEquals("content://x", it.getString(2))
            assertEquals(true, it.isNull(3)); assertEquals(true, it.isNull(4))
        }
        db.query("SELECT titulo FROM deber").use { it.moveToFirst(); assertEquals("Estudiar", it.getString(0)) }
        db.execSQL("INSERT INTO grupo_lista (id, nombre, icono, orden, plegado, creada, actualizada, eliminada) VALUES ('G1', 'Semestre 3', '📁', 0, 0, 1, 1, NULL)")
        db.execSQL("UPDATE materia SET grupo_id = 'G1' WHERE id = 'M1'")
        db.query("SELECT COUNT(*) FROM grupo_lista").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
    }

    /** La versión 4 pasa a la 5: las clases quedan sin límite de faltas y aparece la tabla de faltas, sin perder nada. */
    @Test
    fun deLaVersion4ALa5ConservaLasClasesYCreaLasFaltas() {
        ayudante.createDatabase("migracion4", 4).apply {
            execSQL("INSERT INTO horario (id, nombre, icono, orden, creada, actualizada, eliminada) VALUES ('H1', 'Uni', '🎓', 0, 1, 1, NULL)")
            execSQL("INSERT INTO clase (id, horario_id, materia_id, grupo, titulo, dia_semana, inicio_min, fin_min, aula, notas, aviso_min, color, creada, actualizada, eliminada) VALUES ('C1', 'H1', NULL, 'G', 'Cálculo', 1, 480, 600, 'A-1', '', 10, '#E53935', 1, 1, NULL)")
            close()
        }
        val db = ayudante.runMigrationsAndValidate("migracion4", 5, true, KiwuDb.MIGRACION_4_5)
        db.query("SELECT titulo, color, aviso_min, max_faltas FROM clase").use {
            assertEquals(1, it.count)
            it.moveToFirst()
            assertEquals("Cálculo", it.getString(0)); assertEquals("#E53935", it.getString(1)); assertEquals(10, it.getInt(2))
            assertEquals(true, it.isNull(3))
        }
        db.execSQL("INSERT INTO falta (id, grupo, horario_id, dia, justificada, creada) VALUES ('F1', 'G', 'H1', 20752, 0, 1)")
        db.query("SELECT COUNT(*) FROM falta").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
    }

    /** La versión 5 pasa a la 6: las tareas que ya había quedan sin repetición. */
    @Test
    fun deLaVersion5ALa6DejaLasTareasSinRepeticion() {
        ayudante.createDatabase("migracion5", 5).apply {
            execSQL("INSERT INTO materia (id, nombre, icono, orden, carpeta, grupo_id, emoji, creada, actualizada, eliminada) VALUES ('M1', 'Mate', 'book', 0, NULL, NULL, NULL, 1, 1, NULL)")
            execSQL("INSERT INTO deber (id, materia_id, titulo, completado, estado, fecha_entrega, recordatorio_horas, fecha_completado, nota, creada, actualizada, recordado, eliminada) VALUES ('D1', 'M1', 'Informe', 0, 0, NULL, 24, NULL, '', 1, 1, NULL, NULL)")
            close()
        }
        val db = ayudante.runMigrationsAndValidate("migracion5", 6, true, KiwuDb.MIGRACION_5_6)
        db.query("SELECT titulo, repite FROM deber").use { it.moveToFirst(); assertEquals("Informe", it.getString(0)); assertEquals(0, it.getInt(1)) }
    }

    /** La versión 6 pasa a la 7: las tareas quedan sin prioridad, etiquetas ni pasos. */
    @Test
    fun deLaVersion6ALa7DejaLasTareasSinPrioridadNiPasos() {
        ayudante.createDatabase("migracion6", 6).apply {
            execSQL("INSERT INTO materia (id, nombre, icono, orden, carpeta, grupo_id, emoji, creada, actualizada, eliminada) VALUES ('M1', 'Mate', 'book', 0, NULL, NULL, NULL, 1, 1, NULL)")
            execSQL("INSERT INTO deber (id, materia_id, titulo, completado, estado, fecha_entrega, recordatorio_horas, repite, fecha_completado, nota, creada, actualizada, recordado, eliminada) VALUES ('D1', 'M1', 'Informe', 0, 0, NULL, 24, 0, NULL, '', 1, 1, NULL, NULL)")
            close()
        }
        val db = ayudante.runMigrationsAndValidate("migracion6", 7, true, KiwuDb.MIGRACION_6_7)
        db.query("SELECT titulo, prioridad, etiquetas, pasos FROM deber").use {
            it.moveToFirst(); assertEquals("Informe", it.getString(0)); assertEquals(0, it.getInt(1)); assertEquals("", it.getString(2)); assertEquals("", it.getString(3))
        }
    }

    /** La versión 7 pasa a la 8: las tareas no son examen y las listas no tienen notas. */
    @Test
    fun deLaVersion7ALa8DejaLasTareasSinExamenNiNotas() {
        ayudante.createDatabase("migracion7", 7).apply {
            execSQL("INSERT INTO materia (id, nombre, icono, orden, carpeta, grupo_id, emoji, creada, actualizada, eliminada) VALUES ('M1', 'Mate', 'book', 0, NULL, NULL, NULL, 1, 1, NULL)")
            execSQL("INSERT INTO deber (id, materia_id, titulo, completado, estado, fecha_entrega, recordatorio_horas, repite, prioridad, etiquetas, pasos, fecha_completado, nota, creada, actualizada, recordado, eliminada) VALUES ('D1', 'M1', 'Informe', 0, 0, NULL, 24, 0, 0, '', '', NULL, '', 1, 1, NULL, NULL)")
            close()
        }
        val db = ayudante.runMigrationsAndValidate("migracion7", 8, true, KiwuDb.MIGRACION_7_8)
        db.query("SELECT examen FROM deber").use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
        db.query("SELECT calificaciones FROM materia").use { it.moveToFirst(); assertEquals("", it.getString(0)) }
    }

    /** La versión 8 pasa a la 9: las notas que ya había quedan sin tarjetas. */
    @Test
    fun deLaVersion8ALa9DejaLasNotasSinTarjetas() {
        ayudante.createDatabase("migracion8", 8).apply {
            execSQL("INSERT INTO cuaderno (id, nombre, icono, orden, creada, actualizada, eliminada) VALUES ('C1', 'Uni', '📓', 0, 1, 1, NULL)")
            execSQL("INSERT INTO pagina (id, cuaderno_id, titulo, icono, contenido, texto_plano, favorita, creada, actualizada, eliminada) VALUES ('P1', 'C1', 'Apuntes', '', '', '', 0, 1, 1, NULL)")
            close()
        }
        val db = ayudante.runMigrationsAndValidate("migracion8", 9, true, KiwuDb.MIGRACION_8_9)
        db.query("SELECT titulo, tarjetas FROM pagina").use { it.moveToFirst(); assertEquals("Apuntes", it.getString(0)); assertEquals("", it.getString(1)) }
    }

    /** De la primera versión a la última pasando por todas las migraciones. */
    @Test
    fun deLaVersion1ALa9EnUnSoloPaso() {
        ayudante.createDatabase("migracion19", 1).apply { close() }
        ayudante.runMigrationsAndValidate("migracion19", 9, true, KiwuDb.MIGRACION_1_2, KiwuDb.MIGRACION_2_3, KiwuDb.MIGRACION_3_4, KiwuDb.MIGRACION_4_5, KiwuDb.MIGRACION_5_6, KiwuDb.MIGRACION_6_7, KiwuDb.MIGRACION_7_8, KiwuDb.MIGRACION_8_9)
    }
}
