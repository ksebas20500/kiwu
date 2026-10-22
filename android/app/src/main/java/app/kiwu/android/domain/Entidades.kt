package app.kiwu.android.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Mismo modelo que Kiwu para Linux (esquema v2) y macOS. Las fechas son milisegundos desde 1970.
 * `eliminada` es el borrado lógico: con valor, la fila ya no existe para la persona usuaria, pero se conserva
 * para poder propagar el borrado cuando haya sincronización.
 */

@Entity(tableName = "materia")
data class Materia(
    @PrimaryKey val id: String = nuevoId(),
    val nombre: String,
    val icono: String = "book",
    val orden: Int = 0,
    /** Carpeta vinculada: dirección `content://` de un árbol del selector de archivos. Solo vale en este dispositivo. */
    val carpeta: String? = null,
    /** Carpeta de listas (grupo) en la que está en el menú de Tareas; null = lista suelta. */
    @ColumnInfo(name = "grupo_id") val grupoId: String? = null,
    /** Emoji propio de la lista (opcional; null = el icono de lista de siempre). */
    val emoji: String? = null,
    val creada: Long,
    val actualizada: Long,
    val eliminada: Long? = null,
)

/**
 * Carpeta de listas del menú de Tareas (un solo nivel: las carpetas no se anidan). En el código se llama «grupo» para no
 * confundirla con [Materia.carpeta], que es la carpeta de archivos del teléfono vinculada a una lista.
 */
@Entity(tableName = "grupo_lista")
data class GrupoLista(
    @PrimaryKey val id: String = nuevoId(),
    val nombre: String,
    /** Emoji de la carpeta (el de una carpeta por defecto). */
    val icono: String = "📁",
    val orden: Int = 0,
    /** Si la carpeta está plegada en el menú. */
    val plegado: Boolean = false,
    val creada: Long,
    val actualizada: Long,
    val eliminada: Long? = null,
)

@Entity(
    tableName = "deber",
    foreignKeys = [ForeignKey(Materia::class, ["id"], ["materia_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("materia_id"), Index("fecha_entrega")],
)
data class Deber(
    @PrimaryKey val id: String = nuevoId(),
    @ColumnInfo(name = "materia_id") val materiaId: String,
    val titulo: String,
    val completado: Boolean = false,
    /** Columna del tablero: 0 por hacer, 1 en curso. «Hecho» se deduce de `completado`. */
    val estado: Int = 0,
    @ColumnInfo(name = "fecha_entrega") val fechaEntrega: Long? = null,
    @ColumnInfo(name = "recordatorio_horas") val recordatorioHoras: Int = 24,
    @ColumnInfo(name = "fecha_completado") val fechaCompletado: Long? = null,
    /** Nota de la tarea: JSON de bloques (ver `Documento`). */
    val nota: String = DOCUMENTO_VACIO,
    val creada: Long,
    val actualizada: Long,
    /** Cuándo se mostró el último aviso (para no repetirlo). */
    val recordado: Long? = null,
    val eliminada: Long? = null,
)

@Entity(tableName = "cuaderno")
data class Cuaderno(
    @PrimaryKey val id: String = nuevoId(),
    val nombre: String,
    val icono: String = "📓",
    val orden: Int = 0,
    val creada: Long,
    val actualizada: Long,
    val eliminada: Long? = null,
)

@Entity(
    tableName = "pagina",
    foreignKeys = [ForeignKey(Cuaderno::class, ["id"], ["cuaderno_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cuaderno_id")],
)
data class Pagina(
    @PrimaryKey val id: String = nuevoId(),
    @ColumnInfo(name = "cuaderno_id") val cuadernoId: String,
    val titulo: String = "",
    val icono: String = "",
    val contenido: String = DOCUMENTO_VACIO,
    @ColumnInfo(name = "texto_plano") val textoPlano: String = "",
    val favorita: Boolean = false,
    val creada: Long,
    val actualizada: Long,
    val eliminada: Long? = null,
)
