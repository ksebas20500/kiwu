package app.kiwu.android.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import app.kiwu.android.domain.EventoExterno
import app.kiwu.android.domain.EventosExternos
import java.time.LocalDate
import java.time.ZoneId

/**
 * Los eventos de los calendarios del teléfono (Google Calendar sincroniza los suyos ahí), solo lectura. Necesita el
 * permiso `READ_CALENDAR`; sin él, o si falla la consulta, devuelve una lista vacía.
 *
 * El teléfono trae una copia del mismo evento por cada calendario donde esté, y cada cuenta de Google tiene su propio
 * calendario de festivos: las copias se quitan con [EventosExternos.deduplicar] (ver por qué allí).
 *
 * shortcut: un evento de varios días solo cuenta en su día de inicio. Repartirlo por días si hace falta.
 */
object CalendarioTelefono {
    fun permitido(c: Context): Boolean = c.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun eventos(c: Context, desde: LocalDate, hasta: LocalDate, zona: ZoneId = ZoneId.systemDefault()): List<EventoExterno> {
        if (!permitido(c)) return emptyList()
        // Los de todo el día están en UTC: se pide un día de margen a cada lado y luego se filtra por su día real.
        val ini = desde.minusDays(1).atStartOfDay(zona).toInstant().toEpochMilli()
        val finRango = hasta.plusDays(2).atStartOfDay(zona).toInstant().toEpochMilli()
        return try {
            val consulta = CalendarContract.Instances.query(
                c.contentResolver,
                arrayOf(
                    CalendarContract.Instances.EVENT_ID,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.ALL_DAY,
                ),
                ini,
                finRango,
            )
            buildList {
                consulta?.use {
                    while (it.moveToNext()) {
                        add(
                            EventoExterno(
                                id = it.getLong(0),
                                titulo = it.getString(1).orEmpty().ifBlank { "·" },
                                inicio = it.getLong(2),
                                todoElDia = it.getInt(4) == 1,
                                fin = it.getLong(3).takeIf { f -> f > 0 },
                            ),
                        )
                    }
                }
            }
                .filter { it.dia(zona) in desde..hasta }
                .let(EventosExternos::deduplicar)
                .sortedBy { it.inicio }
        } catch (_: Exception) {
            emptyList() // sin permiso, sin proveedor o sin calendarios: no debe romper la app
        }
    }
}
