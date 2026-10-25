package app.kiwu.android.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.kiwu.android.KiwuApp
import app.kiwu.android.MainActivity
import app.kiwu.android.R
import app.kiwu.android.domain.Clase
import app.kiwu.android.domain.ReglasHorario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import app.kiwu.android.Idioma

/** Avisos de clases: «Cálculo empieza a las 08:00 · Aula 204». Cada clase con aviso tiene una alarma para su próxima vez. */
object AvisosClases {
    const val CANAL = "horario"
    const val ACCION = "app.kiwu.android.AVISAR_CLASE"
    const val EXTRA_ID = "clase"

    fun crearCanal(contexto: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = contexto.getSystemService(NotificationManager::class.java)
        val t = Idioma.contexto(contexto)
        nm.createNotificationChannel(
            NotificationChannel(CANAL, t.getString(R.string.canal_horario), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = t.getString(R.string.canal_horario_desc) },
        )
    }

    private fun identificador(id: String) = ("clase:" + id).hashCode()

    fun alarma(contexto: Context, id: String): PendingIntent =
        PendingIntent.getBroadcast(
            contexto, identificador(id),
            Intent(contexto, ClaseReceiver::class.java).setAction(ACCION).putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /** Muestra el aviso. Devuelve false si no hay permiso de notificaciones. */
    fun mostrar(contexto: Context, c: Clase, horario: String?): Boolean {
        if (!Avisos.tienePermiso(contexto)) return false
        crearCanal(contexto)
        val textos = Idioma.contexto(contexto)
        val abrir = PendingIntent.getActivity(
            contexto, identificador(c.id), Intent(contexto, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notificacion = NotificationCompat.Builder(contexto, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(c.titulo)
            .setContentText(listOfNotNull(ReglasHorario.textoAviso(c) { textos.getString(R.string.clase_empieza_a, it) }, horario).joinToString(" · "))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .build()
        return try {
            NotificationManagerCompat.from(contexto).notify(identificador(c.id), notificacion)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}

/** Mantiene una alarma por clase con aviso (para su próxima vez) y quita las de clases eliminadas o sin aviso. */
class ProgramadorClases(private val contexto: Context) {
    private val alarmas = contexto.getSystemService(AlarmManager::class.java)
    private val programadas = HashSet<String>()

    fun reconciliar(clases: List<Clase>, ahora: Long = System.currentTimeMillis()) {
        val vigentes = HashSet<String>()
        for (c in clases) {
            val cuando = ReglasHorario.momentoDeAviso(c, ahora) ?: continue
            programar(c.id, cuando)
            vigentes += c.id
        }
        for (id in programadas - vigentes) alarmas.cancel(AvisosClases.alarma(contexto, id))
        programadas.clear()
        programadas += vigentes
    }

    fun programar(id: String, cuando: Long) {
        val pi = AvisosClases.alarma(contexto, id)
        try {
            if (Build.VERSION.SDK_INT < 31 || alarmas.canScheduleExactAlarms()) alarmas.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi)
            else alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi)
        } catch (e: SecurityException) {
            alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi)
        }
    }
}

private val alcanceClases = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** Suena la alarma de una clase: se muestra el aviso y se programa el de la semana siguiente. */
class ClaseReceiver : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        val id = intent.getStringExtra(AvisosClases.EXTRA_ID) ?: return
        val app = contexto.applicationContext as KiwuApp
        val pendiente = goAsync()
        alcanceClases.launch {
            try {
                val c = app.repo.clase(id) ?: return@launch
                val horario = app.repo.horarioNombre(c.horarioId)
                AvisosClases.mostrar(contexto, c, horario)
                // La próxima vez: un minuto después para no repetir este mismo aviso.
                ReglasHorario.momentoDeAviso(c, System.currentTimeMillis() + 60_000)?.let { ProgramadorClases(contexto).programar(c.id, it) }
            } finally {
                pendiente.finish()
            }
        }
    }
}
