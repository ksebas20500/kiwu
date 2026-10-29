package app.kiwu.android.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Un evento (o una tarea de Google Tasks, con [tarea]) de fuera de Kiwu: solo se lee, nunca se guarda en Kiwu. */
data class EventoExterno(val id: Long, val titulo: String, val inicio: Long, val todoElDia: Boolean, val tarea: Boolean = false) {
    /** Los eventos de todo el día vienen en UTC: su día es el de UTC, no el de la zona local. */
    fun dia(zona: ZoneId): LocalDate = Instant.ofEpochMilli(inicio).atZone(if (todoElDia) ZoneOffset.UTC else zona).toLocalDate()
}
