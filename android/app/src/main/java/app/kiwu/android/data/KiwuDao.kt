package app.kiwu.android.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import app.kiwu.android.domain.Clase
import app.kiwu.android.domain.Cuaderno
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.Falta
import app.kiwu.android.domain.Horario
import app.kiwu.android.domain.TareaRacha
import app.kiwu.android.domain.GrupoLista
import app.kiwu.android.domain.Materia
import app.kiwu.android.domain.Pagina
import kotlinx.coroutines.flow.Flow

/** Lo necesario para listar páginas sin cargar todo su contenido. */
data class ResumenPagina(
    val id: String,
    @ColumnInfo(name = "cuaderno_id") val cuadernoId: String,
    val titulo: String,
    val icono: String,
    val favorita: Boolean,
    @ColumnInfo(name = "texto_plano") val textoPlano: String,
    val creada: Long,
    val actualizada: Long,
)

/**
 * Acceso a los datos. Todas las consultas de lectura ignoran lo eliminado (`eliminada IS NULL`) y los borrados son
 * lógicos: se marca la fila (y la de sus hijos) en vez de quitarla.
 */
@Dao
abstract class KiwuDao {
    // --- materias -------------------------------------------------------------------------------

    @Query("SELECT * FROM materia WHERE eliminada IS NULL ORDER BY orden, nombre COLLATE NOCASE")
    abstract fun materias(): Flow<List<Materia>>

    @Query("SELECT * FROM materia WHERE id = :id AND eliminada IS NULL")
    abstract suspend fun materia(id: String): Materia?

    @Query("SELECT COALESCE(MAX(orden), -1) + 1 FROM materia WHERE eliminada IS NULL")
    abstract suspend fun siguienteOrdenMateria(): Int

    @Upsert
    abstract suspend fun guardarMateria(m: Materia)

    @Upsert
    abstract suspend fun guardarMaterias(filas: List<Materia>)

    // --- carpetas de listas (grupos) --------------------------------------------------------------

    @Query("SELECT * FROM grupo_lista WHERE eliminada IS NULL ORDER BY orden, nombre COLLATE NOCASE")
    abstract fun grupos(): Flow<List<GrupoLista>>

    @Query("SELECT * FROM grupo_lista WHERE eliminada IS NULL ORDER BY orden, nombre COLLATE NOCASE")
    abstract suspend fun gruposAhora(): List<GrupoLista>

    @Query("SELECT * FROM grupo_lista WHERE id = :id AND eliminada IS NULL")
    abstract suspend fun grupo(id: String): GrupoLista?

    @Query("SELECT COALESCE(MAX(orden), -1) + 1 FROM grupo_lista WHERE eliminada IS NULL")
    abstract suspend fun siguienteOrdenGrupo(): Int

    @Upsert
    abstract suspend fun guardarGrupo(g: GrupoLista)

    @Upsert
    abstract suspend fun guardarGrupos(filas: List<GrupoLista>)

    @Query("UPDATE grupo_lista SET eliminada = :ahora, actualizada = :ahora WHERE id = :id AND eliminada IS NULL")
    protected abstract suspend fun marcarGrupoListas(id: String, ahora: Long)

    @Query("UPDATE materia SET grupo_id = NULL, actualizada = :ahora WHERE grupo_id = :id AND eliminada IS NULL")
    protected abstract suspend fun soltarListasDeGrupo(id: String, ahora: Long)

    /** Borra la carpeta (borrado lógico) y deja sueltas sus listas: las listas y sus tareas **no** se tocan. */
    @Transaction
    open suspend fun eliminarGrupoListas(id: String, ahora: Long) {
        soltarListasDeGrupo(id, ahora)
        marcarGrupoListas(id, ahora)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertarGrupos(filas: List<GrupoLista>): List<Long>

    @Query("SELECT id FROM grupo_lista WHERE eliminada IS NULL")
    abstract suspend fun idsGrupos(): List<String>

    @Query("UPDATE materia SET eliminada = :ahora, actualizada = :ahora WHERE id = :id AND eliminada IS NULL")
    protected abstract suspend fun marcarMateria(id: String, ahora: Long)

    @Query("UPDATE deber SET eliminada = :ahora, actualizada = :ahora WHERE materia_id = :materiaId AND eliminada IS NULL")
    protected abstract suspend fun marcarDeberesDeMateria(materiaId: String, ahora: Long)

    @Transaction
    open suspend fun eliminarMateria(id: String, ahora: Long) {
        marcarMateria(id, ahora)
        marcarDeberesDeMateria(id, ahora)
    }

    // --- deberes --------------------------------------------------------------------------------

    @Query("SELECT * FROM deber WHERE eliminada IS NULL")
    abstract fun deberes(): Flow<List<Deber>>

    @Query("SELECT * FROM deber WHERE eliminada IS NULL")
    abstract suspend fun deberesAhora(): List<Deber>

    @Query("SELECT * FROM deber WHERE id = :id AND eliminada IS NULL")
    abstract suspend fun deber(id: String): Deber?

    @Upsert
    abstract suspend fun guardarDeber(d: Deber)

    @Query("UPDATE deber SET eliminada = :ahora, actualizada = :ahora WHERE id = :id AND eliminada IS NULL")
    abstract suspend fun eliminarDeber(id: String, ahora: Long)

    @Query("UPDATE deber SET recordado = :cuando WHERE id = :id AND eliminada IS NULL")
    abstract suspend fun marcarAvisado(id: String, cuando: Long)

    /**
     * Lo que la racha necesita de cada tarea. A propósito **incluye las eliminadas** (hay que saber cuándo dejaron de estar
     * pendientes) y deja fuera los eventos, que no se completan.
     */
    @Query("SELECT fecha_entrega, completado, fecha_completado, eliminada, creada FROM deber WHERE materia_id != 'eventos'")
    abstract fun tareasRacha(): Flow<List<TareaRacha>>

    @Query("SELECT fecha_entrega, completado, fecha_completado, eliminada, creada FROM deber WHERE materia_id != 'eventos'")
    abstract suspend fun tareasRachaAhora(): List<TareaRacha>

    /** Si ya hay algo guardado (listas, cuadernos u horarios, aunque estén borrados): entonces no es una instalación nueva. */
    @Query("SELECT EXISTS(SELECT 1 FROM materia) OR EXISTS(SELECT 1 FROM cuaderno) OR EXISTS(SELECT 1 FROM horario)")
    abstract suspend fun hayDatos(): Boolean

    // --- cuadernos y páginas --------------------------------------------------------------------

    @Query("SELECT * FROM cuaderno WHERE eliminada IS NULL ORDER BY orden, nombre COLLATE NOCASE")
    abstract fun cuadernos(): Flow<List<Cuaderno>>

    @Query("SELECT * FROM cuaderno WHERE id = :id AND eliminada IS NULL")
    abstract suspend fun cuaderno(id: String): Cuaderno?

    @Query("SELECT COALESCE(MAX(orden), -1) + 1 FROM cuaderno WHERE eliminada IS NULL")
    abstract suspend fun siguienteOrdenCuaderno(): Int

    @Upsert
    abstract suspend fun guardarCuaderno(c: Cuaderno)

    @Query("UPDATE cuaderno SET eliminada = :ahora, actualizada = :ahora WHERE id = :id AND eliminada IS NULL")
    protected abstract suspend fun marcarCuaderno(id: String, ahora: Long)

    @Query("UPDATE pagina SET eliminada = :ahora, actualizada = :ahora WHERE cuaderno_id = :cuadernoId AND eliminada IS NULL")
    protected abstract suspend fun marcarPaginasDeCuaderno(cuadernoId: String, ahora: Long)

    @Transaction
    open suspend fun eliminarCuaderno(id: String, ahora: Long) {
        marcarCuaderno(id, ahora)
        marcarPaginasDeCuaderno(id, ahora)
    }

    @Query(
        "SELECT id, cuaderno_id, titulo, icono, favorita, texto_plano, creada, actualizada " +
            "FROM pagina WHERE eliminada IS NULL ORDER BY actualizada DESC",
    )
    abstract fun paginas(): Flow<List<ResumenPagina>>

    @Query("SELECT * FROM pagina WHERE eliminada IS NULL")
    abstract suspend fun paginasAhora(): List<Pagina>

    @Query("SELECT * FROM pagina WHERE id = :id AND eliminada IS NULL")
    abstract suspend fun pagina(id: String): Pagina?

    @Upsert
    abstract suspend fun guardarPagina(p: Pagina)

    @Query("UPDATE pagina SET eliminada = :ahora, actualizada = :ahora WHERE id = :id AND eliminada IS NULL")
    abstract suspend fun eliminarPagina(id: String, ahora: Long)

    // --- horarios y clases ----------------------------------------------------------------------

    @Query("SELECT * FROM horario WHERE eliminada IS NULL ORDER BY orden, nombre COLLATE NOCASE")
    abstract fun horarios(): Flow<List<Horario>>

    @Query("SELECT * FROM horario WHERE id = :id AND eliminada IS NULL")
    abstract suspend fun horario(id: String): Horario?

    @Query("SELECT COALESCE(MAX(orden), -1) + 1 FROM horario WHERE eliminada IS NULL")
    abstract suspend fun siguienteOrdenHorario(): Int

    @Upsert
    abstract suspend fun guardarHorario(h: Horario)

    @Query("UPDATE horario SET eliminada = :ahora, actualizada = :ahora WHERE id = :id AND eliminada IS NULL")
    protected abstract suspend fun marcarHorario(id: String, ahora: Long)

    @Query("UPDATE clase SET eliminada = :ahora, actualizada = :ahora WHERE horario_id = :horarioId AND eliminada IS NULL")
    protected abstract suspend fun marcarClasesDeHorario(horarioId: String, ahora: Long)

    @Transaction
    open suspend fun eliminarHorario(id: String, ahora: Long) {
        marcarHorario(id, ahora)
        marcarClasesDeHorario(id, ahora)
    }

    @Query("SELECT * FROM clase WHERE eliminada IS NULL")
    abstract fun clases(): Flow<List<Clase>>

    @Query("SELECT * FROM clase WHERE eliminada IS NULL")
    abstract suspend fun clasesAhora(): List<Clase>

    /** Días de la semana (1 = lunes … 7) con alguna clase: los días exigentes de la racha. */
    @Query("SELECT DISTINCT dia_semana FROM clase WHERE eliminada IS NULL")
    abstract fun diasConClase(): Flow<List<Int>>

    @Query("SELECT DISTINCT dia_semana FROM clase WHERE eliminada IS NULL")
    abstract suspend fun diasConClaseAhora(): List<Int>

    @Query("SELECT * FROM clase WHERE id = :id AND eliminada IS NULL")
    abstract suspend fun clase(id: String): Clase?

    @Query("SELECT * FROM clase WHERE grupo = :grupo AND eliminada IS NULL")
    abstract suspend fun clasesDeGrupo(grupo: String): List<Clase>

    @Upsert
    abstract suspend fun guardarClases(filas: List<Clase>)

    @Query("UPDATE clase SET eliminada = :ahora, actualizada = :ahora WHERE id IN (:ids) AND eliminada IS NULL")
    abstract suspend fun eliminarClases(ids: List<String>, ahora: Long)

    @Query("UPDATE clase SET eliminada = :ahora, actualizada = :ahora WHERE grupo = :grupo AND eliminada IS NULL")
    abstract suspend fun eliminarGrupo(grupo: String, ahora: Long)

    // --- faltas de clase --------------------------------------------------------------------------

    @Query("SELECT * FROM falta ORDER BY dia DESC, creada DESC")
    abstract fun faltas(): Flow<List<Falta>>

    @Query("SELECT * FROM falta")
    abstract suspend fun faltasAhora(): List<Falta>

    @Upsert
    abstract suspend fun guardarFalta(f: Falta)

    @Query("DELETE FROM falta WHERE id = :id")
    abstract suspend fun quitarFalta(id: String)

    @Query("UPDATE falta SET justificada = :justificada WHERE id = :id")
    abstract suspend fun justificarFalta(id: String, justificada: Boolean)

    @Query("DELETE FROM falta WHERE horario_id = :horarioId")
    abstract suspend fun vaciarFaltas(horarioId: String)

    @Query("DELETE FROM falta WHERE grupo = :grupo")
    abstract suspend fun quitarFaltasDeGrupo(grupo: String)

    @Query("UPDATE clase SET max_faltas = :maximo, actualizada = :ahora WHERE grupo = :grupo AND eliminada IS NULL")
    abstract suspend fun ponerMaximoFaltas(grupo: String, maximo: Int?, ahora: Long)

    @Query("SELECT DISTINCT grupo FROM clase WHERE eliminada IS NULL")
    abstract suspend fun gruposDeClases(): List<String>

    // --- importación (no pisa lo existente ni resucita lo eliminado) ------------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertarMaterias(filas: List<Materia>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertarDeberes(filas: List<Deber>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertarCuadernos(filas: List<Cuaderno>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertarPaginas(filas: List<Pagina>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertarHorarios(filas: List<Horario>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertarClases(filas: List<Clase>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertarFaltas(filas: List<Falta>): List<Long>

    @Query("SELECT id FROM horario WHERE eliminada IS NULL")
    abstract suspend fun idsHorarios(): List<String>

    @Query("SELECT id FROM materia WHERE eliminada IS NULL")
    abstract suspend fun idsMaterias(): List<String>

    @Query("SELECT id FROM cuaderno WHERE eliminada IS NULL")
    abstract suspend fun idsCuadernos(): List<String>
}

@Database(
    entities = [Materia::class, Deber::class, Cuaderno::class, Pagina::class, Horario::class, Clase::class, GrupoLista::class, Falta::class],
    version = 5,
    exportSchema = true,
)
abstract class KiwuDb : RoomDatabase() {
    abstract fun dao(): KiwuDao

    companion object {
        @Volatile private var instancia: KiwuDb? = null

        /** v1 → v2: horarios semanales (tablas `horario` y `clase`). No toca las tablas existentes. */
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `horario` (`id` TEXT NOT NULL, `nombre` TEXT NOT NULL, `icono` TEXT NOT NULL, `orden` INTEGER NOT NULL, `creada` INTEGER NOT NULL, `actualizada` INTEGER NOT NULL, `eliminada` INTEGER, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `clase` (`id` TEXT NOT NULL, `horario_id` TEXT NOT NULL, `materia_id` TEXT, `grupo` TEXT NOT NULL, `titulo` TEXT NOT NULL, `dia_semana` INTEGER NOT NULL, `inicio_min` INTEGER NOT NULL, `fin_min` INTEGER NOT NULL, `aula` TEXT NOT NULL, `notas` TEXT NOT NULL, `aviso_min` INTEGER, `creada` INTEGER NOT NULL, `actualizada` INTEGER NOT NULL, `eliminada` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`horario_id`) REFERENCES `horario`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_clase_horario_id` ON `clase` (`horario_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_clase_grupo` ON `clase` (`grupo`)")
            }
        }

        /** v2 → v3: color elegido para cada clase (columna opcional: las clases existentes siguen con el color automático). */
        val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `clase` ADD COLUMN `color` TEXT")
            }
        }

        /**
         * v3 → v4: carpetas de listas. Tabla nueva `grupo_lista` y dos columnas opcionales en `materia`
         * (carpeta y emoji): las listas existentes quedan sueltas y con su aspecto de siempre.
         */
        val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `grupo_lista` (`id` TEXT NOT NULL, `nombre` TEXT NOT NULL, `icono` TEXT NOT NULL, `orden` INTEGER NOT NULL, `plegado` INTEGER NOT NULL, `creada` INTEGER NOT NULL, `actualizada` INTEGER NOT NULL, `eliminada` INTEGER, PRIMARY KEY(`id`))")
                db.execSQL("ALTER TABLE `materia` ADD COLUMN `grupo_id` TEXT")
                db.execSQL("ALTER TABLE `materia` ADD COLUMN `emoji` TEXT")
            }
        }

        /** v4 → v5: faltas de clase. Tabla nueva `falta` y el máximo opcional en `clase` (sin límite en las existentes). */
        val MIGRACION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `clase` ADD COLUMN `max_faltas` INTEGER")
                db.execSQL("CREATE TABLE IF NOT EXISTS `falta` (`id` TEXT NOT NULL, `grupo` TEXT NOT NULL, `horario_id` TEXT NOT NULL, `dia` INTEGER NOT NULL, `justificada` INTEGER NOT NULL, `creada` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_falta_grupo` ON `falta` (`grupo`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_falta_horario_id` ON `falta` (`horario_id`)")
            }
        }

        fun obtener(contexto: Context): KiwuDb = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(contexto.applicationContext, KiwuDb::class.java, "kiwu.db")
                .addMigrations(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4, MIGRACION_4_5)
                // Las tablas y triggers de sincronización no son entidades de Room: se instalan en cada apertura.
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) = SyncSql.instalar(db)
                })
                .build().also { instancia = it }
        }
    }
}
