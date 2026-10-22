package app.kiwu.android.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Elementos del menú lateral de Tareas. */
sealed interface Elemento {
    data object Hoy : Elemento
    data object Proximos : Elemento
    data object Calendario : Elemento
    data object Todos : Elemento
    data object Completadas : Elemento
    data class DeMateria(val id: String) : Elemento

    /** Todas las tareas de las listas de una carpeta. */
    data class DeGrupo(val id: String) : Elemento
}

enum class Columna(val valor: Int, val titulo: String) {
    POR_HACER(0, "Por hacer"),
    EN_CURSO(1, "En curso"),
    HECHO(2, "Hecho"),
}

/**
 * Reglas de negocio de las tareas (equivalente a `servicios.py` en Linux y `DeberService` / `ContentView` en macOS).
 * Son funciones puras: reciben los datos y devuelven resultados, sin tocar la base de datos.
 */
class Reglas(private val zona: ZoneId = ZoneId.systemDefault()) {

    private fun fecha(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zona).toLocalDate()

    private fun inicio(dia: LocalDate): Long = dia.atStartOfDay(zona).toInstant().toEpochMilli()

    fun inicioDelDia(ms: Long): Long = inicio(fecha(ms))

    /** Último milisegundo del día (mismo criterio que `Calendar.finDelDia` en macOS). */
    fun finDelDia(ms: Long): Long = inicio(fecha(ms).plusDays(1)) - 1

    fun columnaDe(d: Deber): Columna = when {
        d.completado -> Columna.HECHO
        d.estado == 1 -> Columna.EN_CURSO
        else -> Columna.POR_HACER
    }

    fun estaVencido(d: Deber, ahora: Long): Boolean =
        !d.completado && d.fechaEntrega != null && d.fechaEntrega < ahora

    fun enAlcance(d: Deber, item: Elemento, ahora: Long): Boolean = when (item) {
        Elemento.Todos, Elemento.Calendario -> true
        Elemento.Completadas -> d.completado
        is Elemento.DeMateria -> d.materiaId == item.id
        // Las tareas de una carpeta se filtran antes (por las listas que tiene); aquí valen todas las recibidas.
        is Elemento.DeGrupo -> true
        Elemento.Hoy -> d.fechaEntrega != null && d.fechaEntrega <= finDelDia(ahora)
        Elemento.Proximos -> {
            val limite = inicio(fecha(ahora).plusDays(7)) - 1
            d.fechaEntrega != null && d.fechaEntrega in inicioDelDia(ahora)..limite
        }
    }

    fun pendientes(deberes: List<Deber>, item: Elemento, ahora: Long): List<Deber> =
        deberes.filter { !it.completado && enAlcance(it, item, ahora) }
            .sortedWith(compareBy<Deber> { it.fechaEntrega ?: Long.MAX_VALUE }.thenBy { it.creada })

    fun completados(deberes: List<Deber>, item: Elemento, ahora: Long): List<Deber> {
        val lista = when (item) {
            Elemento.Hoy -> {
                val hoy = inicioDelDia(ahora)
                deberes.filter { it.completado && it.fechaCompletado != null && inicioDelDia(it.fechaCompletado) == hoy }
            }
            Elemento.Proximos, Elemento.Calendario -> emptyList()
            else -> deberes.filter { it.completado && enAlcance(it, item, ahora) }
        }
        return lista.sortedByDescending { it.fechaCompletado ?: 0L }
    }

    fun conteo(deberes: List<Deber>, item: Elemento, ahora: Long): Int =
        if (item == Elemento.Calendario || item == Elemento.Completadas) 0 else pendientes(deberes, item, ahora).size

    fun delDia(deberes: List<Deber>, dia: LocalDate): List<Deber> {
        val desde = inicio(dia)
        val hasta = inicio(dia.plusDays(1))
        return deberes.filter { it.fechaEntrega != null && it.fechaEntrega in desde until hasta }
            .sortedBy { it.fechaEntrega }
    }

    // --- acciones: devuelven la tarea ya modificada; guardarla es cosa del repositorio -----------

    fun marcar(d: Deber, completado: Boolean, ahora: Long): Deber = d.copy(
        completado = completado,
        fechaCompletado = if (completado) ahora else null,
        estado = if (!completado && d.estado != 1) 0 else d.estado,
        actualizada = ahora,
    )

    /** Mueve la tarea a una columna del tablero. */
    fun mover(d: Deber, columna: Columna, ahora: Long): Deber = when {
        columnaDe(d) == columna -> d
        columna == Columna.HECHO -> d.copy(completado = true, fechaCompletado = ahora, actualizada = ahora)
        else -> d.copy(estado = columna.valor, completado = false, fechaCompletado = null, actualizada = ahora)
    }

    /** Al cambiar la fecha, el recordatorio vuelve a poder dispararse. */
    fun ponerFecha(d: Deber, fecha: Long?, ahora: Long): Deber =
        d.copy(fechaEntrega = fecha, recordado = null, actualizada = ahora)
}
