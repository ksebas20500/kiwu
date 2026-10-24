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
import app.kiwu.android.domain.Racha
import app.kiwu.android.domain.Rachas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Aviso «No pierdas tu racha»: una alarma diaria a las 20:00 que solo avisa si hoy aún no se ha completado nada. */
object AvisosRacha {
    const val CANAL = "racha"
    const val ACCION = "app.kiwu.android.AVISAR_RACHA"
    private const val IDENTIFICADOR = 0x52414348 // «RACH»

    fun crearCanal(contexto: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = contexto.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CANAL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CANAL, "Racha", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Te avisa por la tarde si tu racha está a punto de perderse" },
            )
        }
    }

    fun alarma(contexto: Context): PendingIntent =
        PendingIntent.getBroadcast(
            contexto, IDENTIFICADOR, Intent(contexto, RachaReceiver::class.java).setAction(ACCION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun mostrar(contexto: Context, r: Racha): Boolean {
        if (!Avisos.tienePermiso(contexto)) return false
        crearCanal(contexto)
        val abrir = PendingIntent.getActivity(
            contexto, IDENTIFICADOR, Intent(contexto, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notificacion = NotificationCompat.Builder(contexto, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle("🔥 No pierdas tu racha de ${Rachas.texto(r.actual)}")
            .setContentText("Completa una tarea antes de medianoche para mantenerla.")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .build()
        return try {
            NotificationManagerCompat.from(contexto).notify(IDENTIFICADOR, notificacion)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun quitar(contexto: Context) = NotificationManagerCompat.from(contexto).cancel(IDENTIFICADOR)
}

/** Programa (o cancela) la alarma diaria. No necesita ser exacta: unos minutos de retraso no importan. */
class ProgramadorRacha(private val contexto: Context) {
    private val alarmas = contexto.getSystemService(AlarmManager::class.java)

    fun reconciliar(activo: Boolean, ahora: Long = System.currentTimeMillis()) {
        val pi = AvisosRacha.alarma(contexto)
        if (activo) alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, Rachas.proximoAviso(ahora), pi) else alarmas.cancel(pi)
    }
}

private val alcanceRacha = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** Suena a las 20:00: avisa solo si la racha está en riesgo, y deja programada la de mañana. */
class RachaReceiver : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        val app = contexto.applicationContext as KiwuApp
        val pendiente = goAsync()
        alcanceRacha.launch {
            try {
                val ahora = System.currentTimeMillis()
                if (!app.ajustes.flujo.first().rachaAviso) return@launch
                val r = Rachas.calcular(app.repo.fechasCompletadoAhora(), ahora)
                if (r.enRiesgo) AvisosRacha.mostrar(contexto, r)
                // Un minuto después, para que la próxima sea la de mañana y no esta misma.
                ProgramadorRacha(contexto).reconciliar(true, ahora + 60_000)
            } finally {
                pendiente.finish()
            }
        }
    }
}
