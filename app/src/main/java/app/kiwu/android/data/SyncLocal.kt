package app.kiwu.android.data

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteProgram
import app.kiwu.android.domain.AlmacenSync
import app.kiwu.android.domain.Campo
import app.kiwu.android.domain.Documento
import app.kiwu.android.domain.FilaSync
import app.kiwu.android.domain.Marca
import app.kiwu.android.domain.conImagenesDe
import app.kiwu.android.domain.paraSincronizar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.util.UUID

/**
 * Qué tablas viajan y cómo. La cola de cambios la llenan unos *triggers* de SQLite: así no hay que tocar cada consulta de
 * `KiwuDao`, y cualquier edición futura se sincroniza sola. Las faltas de clase no viajan todavía (no tienen marca de
 * tiempo ni borrado lógico).
 */
object SyncSql {
    /** Padres antes que hijos. */
    val tablas = listOf("materia", "grupo_lista", "horario", "cuaderno", "deber", "clase", "pagina")

    /** Columnas que son de este dispositivo y no viajan (carpeta vinculada, aviso ya mostrado). */
    val locales = mapOf("materia" to setOf("carpeta"), "deber" to setOf("recordado"))

    /** Columnas con una nota en JSON: viajan sin imágenes. */
    val documentos = mapOf("deber" to "nota", "pagina" to "contenido")

    private const val NO_APLICANDO = "(SELECT valor FROM sync_estado WHERE clave = 'aplicando') = '0'"

    /** Idempotente: se llama en cada apertura de la base, así sirve tanto en instalaciones nuevas como al actualizar. */
    fun instalar(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_estado (clave TEXT NOT NULL PRIMARY KEY, valor TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_cola (tabla TEXT NOT NULL, fila TEXT NOT NULL, PRIMARY KEY (tabla, fila))")
        db.execSQL("INSERT OR IGNORE INTO sync_estado (clave, valor) VALUES ('aplicando', '0')")
        for (t in tablas) {
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS sync_${t}_ins AFTER INSERT ON $t WHEN $NO_APLICANDO " +
                    "BEGIN INSERT OR REPLACE INTO sync_cola (tabla, fila) VALUES ('$t', NEW.id); END",
            )
            // Solo cuando cambia `actualizada`: marcar un aviso como mostrado no es una edición.
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS sync_${t}_upd AFTER UPDATE ON $t WHEN NEW.actualizada IS NOT OLD.actualizada AND $NO_APLICANDO " +
                    "BEGIN INSERT OR REPLACE INTO sync_cola (tabla, fila) VALUES ('$t', NEW.id); END",
            )
        }
    }
}

/** La base local vista por el motor de sincronización. Todo en el hilo de entrada/salida. */
class AlmacenRoom(private val db: KiwuDb) : AlmacenSync {
    private val sql: SupportSQLiteDatabase get() = db.openHelper.writableDatabase

    private fun estado(clave: String): String? =
        sql.query("SELECT valor FROM sync_estado WHERE clave = ?", arrayOf(clave)).use { if (it.moveToFirst()) it.getString(0) else null }

    private fun ponerEstado(clave: String, valor: String) =
        sql.execSQL("INSERT OR REPLACE INTO sync_estado (clave, valor) VALUES (?, ?)", arrayOf(clave, valor))

    /** Identifica a este dispositivo en las marcas; se crea la primera vez. */
    val dispositivo: String by lazy { estado("dispositivo") ?: UUID.randomUUID().toString().also { ponerEstado("dispositivo", it) } }

    override var cursor: String?
        get() = estado("cursor")?.takeIf { it.isNotEmpty() }
        set(v) = ponerEstado("cursor", v.orEmpty())

    /** La cuenta a la que pertenece la cola y el cursor actuales. */
    var cuentaUid: String?
        get() = estado("uid")?.takeIf { it.isNotEmpty() }
        set(v) = ponerEstado("uid", v.orEmpty())

    /** Cuántos cambios esperan a subirse. */
    suspend fun pendientesTotal(): Int = withContext(Dispatchers.IO) {
        sql.query("SELECT COUNT(*) FROM sync_cola").use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    /** Al iniciar sesión por primera vez: todo lo que ya hay en el dispositivo se sube. */
    suspend fun encolarTodo() = withContext(Dispatchers.IO) {
        for (t in SyncSql.tablas) sql.execSQL("INSERT OR REPLACE INTO sync_cola (tabla, fila) SELECT '$t', id FROM $t")
    }

    /** Al cambiar de cuenta: se olvida de dónde iba y se vuelve a subir todo a la cuenta nueva. */
    suspend fun reiniciar() = withContext(Dispatchers.IO) {
        cursor = null
        encolarTodo()
    }

    private fun leerFila(tabla: String, id: String): Map<String, JsonElement>? =
        sql.query("SELECT * FROM $tabla WHERE id = ?", arrayOf(id)).use { c ->
            if (!c.moveToFirst()) return null
            val locales = SyncSql.locales[tabla].orEmpty()
            buildMap {
                for (i in 0 until c.columnCount) {
                    val col = c.getColumnName(i)
                    if (col !in locales) put(col, valor(c, i))
                }
            }
        }

    private fun valor(c: Cursor, i: Int): JsonElement = when (c.getType(i)) {
        Cursor.FIELD_TYPE_NULL -> JsonNull
        Cursor.FIELD_TYPE_INTEGER -> JsonPrimitive(c.getLong(i))
        Cursor.FIELD_TYPE_FLOAT -> JsonPrimitive(c.getDouble(i))
        else -> JsonPrimitive(c.getString(i))
    }

    private fun aFila(tabla: String, id: String, datos: Map<String, JsonElement>): FilaSync {
        val marca = Marca((datos["actualizada"] as? JsonPrimitive)?.longOrNull ?: 0L, 0, dispositivo)
        val limpios = SyncSql.documentos[tabla]?.let { col ->
            val texto = (datos[col] as? JsonPrimitive)?.contentOrNull
            if (texto == null) datos else datos + (col to JsonPrimitive(Documento.deJson(texto).paraSincronizar().aJson()))
        } ?: datos
        return FilaSync(tabla, id, limpios.mapValues { Campo(it.value, marca) })
    }

    override suspend fun pendientes(limite: Int): List<FilaSync> = withContext(Dispatchers.IO) {
        val claves = sql.query("SELECT tabla, fila FROM sync_cola ORDER BY rowid LIMIT ?", arrayOf(limite.toString())).use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0) to c.getString(1)) }
        }
        claves.mapNotNull { (tabla, id) ->
            val datos = leerFila(tabla, id)
            if (datos == null) {
                sql.execSQL("DELETE FROM sync_cola WHERE tabla = ? AND fila = ?", arrayOf(tabla, id))
                null
            } else aFila(tabla, id, datos)
        }
    }

    override suspend fun marcarSubidas(enviadas: List<FilaSync>) = withContext(Dispatchers.IO) {
        for (f in enviadas) {
            val ahora = (leerFila(f.tabla, f.id)?.get("actualizada") as? JsonPrimitive)?.longOrNull
            // Si se editó mientras se subía, `actualizada` ya es otra y la fila se queda en la cola.
            if (ahora == f.campos["actualizada"]?.marca?.ms) sql.execSQL("DELETE FROM sync_cola WHERE tabla = ? AND fila = ?", arrayOf(f.tabla, f.id))
        }
    }

    override suspend fun aplicar(filas: List<FilaSync>) = withContext(Dispatchers.IO) {
        // Los padres y los hijos pueden llegar en lotes distintos: sin comprobar claves ajenas, y sin borrados en cascada.
        sql.execSQL("PRAGMA foreign_keys = OFF")
        try {
            sql.beginTransaction()
            try {
                sql.execSQL("UPDATE sync_estado SET valor = '1' WHERE clave = 'aplicando'")
                for (f in filas.sortedBy { SyncSql.tablas.indexOf(it.tabla) }) {
                    if (f.tabla !in SyncSql.tablas) continue
                    try {
                        aplicarFila(f)
                    } catch (_: android.database.SQLException) {
                        // Una fila rara no frena las demás.
                    }
                }
                sql.execSQL("UPDATE sync_estado SET valor = '0' WHERE clave = 'aplicando'")
                sql.setTransactionSuccessful()
            } finally {
                sql.endTransaction()
            }
        } finally {
            sql.execSQL("PRAGMA foreign_keys = ON")
        }
        // Se escribió por SQL directo: Room no se entera solo, y sin esto las pantallas no mostrarían lo recibido hasta otro cambio.
        db.invalidationTracker.refreshVersionsAsync()
    }

    private fun aplicarFila(f: FilaSync) {
        val remoto = f.campos.mapValues { it.value.valor }
        val local = leerFila(f.tabla, f.id)
        val nuevaMs = (remoto["actualizada"] as? JsonPrimitive)?.longOrNull ?: return
        val localMs = (local?.get("actualizada") as? JsonPrimitive)?.longOrNull ?: -1L
        // Gana la edición más reciente; con la misma hora, la de mayor contenido, igual en todos los dispositivos.
        val gana = nuevaMs > localMs || (nuevaMs == localMs && local != null && remoto.toString() > local.toString())
        if (!gana) return

        val columnas = columnasDe(f.tabla)
        val ignoradas = SyncSql.locales[f.tabla].orEmpty()
        val datos = remoto.filterKeys { it in columnas && it !in ignoradas && it != "id" }.toMutableMap()
        SyncSql.documentos[f.tabla]?.let { col ->
            val entrante = (datos[col] as? JsonPrimitive)?.contentOrNull ?: return@let
            val propio = (local?.get(col) as? JsonPrimitive)?.contentOrNull
            val doc = Documento.deJson(entrante).let { if (propio != null) it.conImagenesDe(Documento.deJson(propio)) else it }
            datos[col] = JsonPrimitive(doc.aJson())
        }
        val nombres = datos.keys.toList()
        if (local != null) {
            val cambios = sql.compileStatement("UPDATE ${f.tabla} SET ${nombres.joinToString { "$it = ?" }} WHERE id = ?")
            nombres.forEachIndexed { i, n -> enlazar(cambios, i + 1, datos[n]) }
            cambios.bindString(nombres.size + 1, f.id)
            cambios.executeUpdateDelete()
        } else {
            val alta = sql.compileStatement("INSERT INTO ${f.tabla} (id, ${nombres.joinToString()}) VALUES (?, ${nombres.joinToString { "?" }})")
            alta.bindString(1, f.id)
            nombres.forEachIndexed { i, n -> enlazar(alta, i + 2, datos[n]) }
            alta.executeInsert()
        }
        sql.execSQL("DELETE FROM sync_cola WHERE tabla = ? AND fila = ?", arrayOf(f.tabla, f.id))
    }

    private fun columnasDe(tabla: String): Set<String> =
        sql.query("PRAGMA table_info($tabla)").use { c ->
            val nombre = c.getColumnIndexOrThrow("name")
            buildSet { while (c.moveToNext()) add(c.getString(nombre)) }
        }

    private fun enlazar(s: SupportSQLiteProgram, i: Int, v: JsonElement?) {
        val p = v as? JsonPrimitive
        when {
            p == null || p is JsonNull -> s.bindNull(i)
            p.isString -> s.bindString(i, p.content)
            p.longOrNull != null -> s.bindLong(i, p.longOrNull!!)
            p.doubleOrNull != null -> s.bindDouble(i, p.doubleOrNull!!)
            else -> s.bindString(i, p.content)
        }
    }
}
