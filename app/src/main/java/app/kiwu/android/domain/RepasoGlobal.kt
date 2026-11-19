package app.kiwu.android.domain

import androidx.room.ColumnInfo
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Una página con sus tarjetas guardadas (JSON), sin cargar el apunte entero: la usa la cola global de repaso. */
data class PaginaTarjetas(
    val id: String,
    @ColumnInfo(name = "cuaderno_id") val cuadernoId: String,
    val titulo: String,
    val tarjetas: String,
)

/** Una tarjeta de la cola global, con la nota de la que sale y lo cerca que está su examen (si lo hay). */
data class TarjetaDeNota(
    val paginaId: String,
    val paginaTitulo: String,
    val cuaderno: String,
    val tarjeta: Tarjeta,
    /** 0 normal; 1 examen en ≤7 días; 2 en ≤3; 3 en ≤1 (hoy o mañana). */
    val prioridad: Int = 0,
    val examen: String? = null,
    val diasExamen: Int? = null,
)

/**
 * Repaso espaciado de todas las notas a la vez: junta las tarjetas que ya toca repasar y adelanta las de las notas
 * cuya materia tiene un examen cerca (7, 3 y 1 días). La nota se asocia a la materia por el nombre de su cuaderno
 * («Cálculo» ↔ materia «Cálculo») o por el título del examen; no hace falta configurar nada.
 */
object RepasoGlobal {
    /** Hora del aviso diario (18:00), como la racha avisa a las 20:00. */
    const val HORA_AVISO = 18

    /** Prioridad de una tarjeta según los días que faltan para su examen (0 = sin examen cerca). */
    fun prioridad(dias: Long): Int = when {
        dias <= 1 -> 3
        dias <= 3 -> 2
        dias <= 7 -> 1
        else -> 0
    }

    /** Próximo aviso diario a las [HORA_AVISO]; si hoy ya pasó, el de mañana. */
    fun proximoAviso(ahora: Long, zona: ZoneId = ZoneId.systemDefault()): Long {
        val momento = Instant.ofEpochMilli(ahora).atZone(zona)
        val dia = if (momento.hour >= HORA_AVISO) momento.toLocalDate().plusDays(1) else momento.toLocalDate()
        return dia.atTime(HORA_AVISO, 0).atZone(zona).toInstant().toEpochMilli()
    }

    /** Todas las tarjetas vencidas, primero las de exámenes más cercanos y luego las más atrasadas. */
    fun cola(
        paginas: List<PaginaTarjetas>,
        cuadernos: List<Cuaderno>,
        deberes: List<Deber>,
        materias: List<Materia>,
        ahora: Long,
        zona: ZoneId = ZoneId.systemDefault(),
    ): List<TarjetaDeNota> {
        val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
        val examenesDeTareas = examenesCerca(deberes, materias, ahora, zona)
        val nombreCuaderno = cuadernos.associate { it.id to it.nombre }
        return paginas.flatMap { p ->
            val cuaderno = nombreCuaderno[p.cuadernoId].orEmpty()
            // Examen de tarea vinculado por nombre.
            val cercaTarea = prioridadDe(p, cuaderno, examenesDeTareas)
            // Examen local de la nota (fecha guardada directamente en sus tarjetas).
            val fechaLocalStr = Tarjetas.leerFechaExamen(p.tarjetas)
            val cercaLocal: Cerca = if (fechaLocalStr != null) {
                try {
                    val fechaLocal = java.time.LocalDate.parse(fechaLocalStr)
                    val dias = ChronoUnit.DAYS.between(hoy, fechaLocal)
                    if (dias in 0..7) Cerca(prioridad(dias), p.titulo.ifBlank { "Examen" }, dias.toInt())
                    else Cerca(0, null, null)
                } catch (_: Exception) { Cerca(0, null, null) }
            } else Cerca(0, null, null)
            // Gana la prioridad más alta entre las dos fuentes.
            val cerca = if (cercaLocal.prioridad >= cercaTarea.prioridad) cercaLocal else cercaTarea
            Repaso.vencen(Tarjetas.leer(p.tarjetas), ahora).map { t ->
                TarjetaDeNota(p.id, p.titulo, cuaderno, t, cerca.prioridad, cerca.examen, cerca.dias)
            }
        }.sortedWith(
            compareByDescending<TarjetaDeNota> { it.prioridad }
                .thenBy { it.tarjeta.vence }
                .thenBy { it.paginaTitulo.lowercase(Locale.ROOT) },
        )
    }

    private class Examen(val titulo: String, val materia: String, val dias: Int)

    private class Cerca(val prioridad: Int, val examen: String?, val dias: Int?)

    private fun examenesCerca(deberes: List<Deber>, materias: List<Materia>, ahora: Long, zona: ZoneId): List<Examen> {
        val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
        val nombres = materias.associate { it.id to it.nombre }
        return deberes.mapNotNull { d ->
            val fecha = d.fechaEntrega ?: return@mapNotNull null
            if (!d.examen || d.completado) return@mapNotNull null
            val dias = ChronoUnit.DAYS.between(hoy, Instant.ofEpochMilli(fecha).atZone(zona).toLocalDate())
            if (dias < 0 || dias > 7) return@mapNotNull null
            Examen(d.titulo, nombres[d.materiaId].orEmpty(), dias.toInt())
        }
    }

    private fun prioridadDe(p: PaginaTarjetas, cuaderno: String, examenes: List<Examen>): Cerca {
        var mejor: Examen? = null
        for (e in examenes) {
            val sujeta = (e.materia.isNotBlank() && seParecen(cuaderno, e.materia)) || seParecen(p.titulo, e.titulo)
            if (!sujeta) continue
            if (mejor == null || e.dias < mejor.dias) mejor = e
        }
        val e = mejor ?: return Cerca(0, null, null)
        return Cerca(prioridad(e.dias.toLong()), e.titulo, e.dias)
    }

    /** Compara nombres sin acentos ni mayúsculas; admite que uno contenga al otro («Cálculo I» ↔ «Cálculo»). */
    internal fun seParecen(a: String, b: String): Boolean {
        val na = normalizar(a)
        val nb = normalizar(b)
        if (na.isEmpty() || nb.isEmpty()) return false
        if (na == nb) return true
        if (na.length < 3 || nb.length < 3) return false
        return na.contains(nb) || nb.contains(na)
    }

    private fun normalizar(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).trim()
}
