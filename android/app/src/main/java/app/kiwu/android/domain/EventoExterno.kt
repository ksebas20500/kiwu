package app.kiwu.android.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Un evento (o una tarea de Google Tasks, con [tarea]) de fuera de Kiwu: solo se lee, nunca se guarda en Kiwu. */
data class EventoExterno(
    val id: Long,
    val titulo: String,
    val inicio: Long,
    val todoElDia: Boolean,
    /** Cuándo termina, tal cual lo da el teléfono; sirve para distinguir dos eventos con el mismo título y hora. */
    val fin: Long? = null,
    val tarea: Boolean = false,
) {
    /** Los eventos de todo el día vienen en UTC: su día es el de UTC, no el de la zona local. */
    fun dia(zona: ZoneId): LocalDate = Instant.ofEpochMilli(inicio).atZone(if (todoElDia) ZoneOffset.UTC else zona).toLocalDate()
}

/**
 * El teléfono devuelve una fila por ocurrencia **y por calendario**, y cada cuenta de Google trae su propia copia del
 * calendario de festivos: con varias cuentas, el mismo festivo llega repetido, con un id distinto en cada copia.
 * Igual que la app de Google Calendar, se comparan los detalles (no el id) y se queda una sola copia.
 *
 * shortcut: dos eventos de verdad con el mismo título, hora y fin en calendarios distintos se verán como uno.
 */
object EventosExternos {
    /** Quita las copias del mismo evento conservando el orden. */
    fun deduplicar(eventos: List<EventoExterno>): List<EventoExterno> {
        val vistos = HashSet<Firma>()
        return eventos.filter { vistos.add(Firma.de(it)) }
    }

    /** Lo que hace que dos filas sean «el mismo evento»: el id no vale, cambia en cada calendario. */
    private data class Firma(val titulo: String, val inicio: Long, val fin: Long?, val todoElDia: Boolean, val tarea: Boolean) {
        companion object {
            fun de(e: EventoExterno) = Firma(e.titulo.trim(), e.inicio, e.fin, e.todoElDia, e.tarea)
        }
    }
}
