package app.kiwu.android.domain

/** Recordatorios: qué tareas toca avisar y con qué texto. Las notificaciones viven en el paquete `notify`. */
object Recordatorios {
    private const val HORA_MS = 3_600_000L

    /** No se avisa de entregas que vencieron hace más de este tiempo (p. ej. al abrir la app tras días). */
    const val GRACIA_MS = HORA_MS

    /** Momento en que debe sonar el aviso de una tarea; null si no lleva aviso. */
    fun momentoDeAviso(d: Deber): Long? {
        val entrega = d.fechaEntrega ?: return null
        if (d.completado || d.recordatorioHoras <= 0) return null
        return entrega - d.recordatorioHoras * HORA_MS
    }

    /**
     * Cuándo hay que programar la alarma de una tarea, o null si no toca avisar (sin fecha, completada, ya avisada
     * o vencida hace mucho). Si el aviso ya pasó pero aún está en plazo, sale enseguida.
     */
    fun momentoDeDisparo(d: Deber, ahora: Long): Long? {
        val aviso = momentoDeAviso(d) ?: return null
        val entrega = d.fechaEntrega ?: return null
        if (ahora > entrega + GRACIA_MS) return null
        if (d.recordado != null && d.recordado >= aviso) return null
        return maxOf(aviso, ahora + 2_000)
    }

    /** Tareas cuyo aviso ya llegó y todavía no se ha notificado. */
    fun pendientesDeAviso(deberes: List<Deber>, ahora: Long): List<Deber> =
        deberes.filter { d ->
            val aviso = momentoDeAviso(d) ?: return@filter false
            val entrega = d.fechaEntrega ?: return@filter false
            aviso <= ahora && ahora <= entrega + GRACIA_MS && (d.recordado == null || d.recordado < aviso)
        }.sortedBy { it.fechaEntrega }

    /** «Vence en 5 h», «Vence mañana», «Venció hace 10 min»… */
    fun textoAviso(d: Deber, ahora: Long): String {
        val entrega = d.fechaEntrega ?: return ""
        val resto = entrega - ahora
        if (resto < 0) {
            val minutos = (-resto / 60_000).toInt()
            return if (minutos < 1) "Acaba de vencer" else "Venció hace $minutos min"
        }
        val horas = resto.toDouble() / HORA_MS
        if (horas < 1) return "Vence en ${maxOf(1, (resto / 60_000).toInt())} min"
        if (horas < 24) return "Vence en ${horas.toInt()} h"
        val dias = (horas / 24).toInt()
        return if (dias == 1) "Vence mañana" else "Vence en $dias días"
    }
}
