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
import app.kiwu.android.Idioma
import app.kiwu.android.MainActivity
import app.kiwu.android.R

/**
 * Temporizador de estudio (Pomodoro): una cuenta atrás que sobrevive a cerrar la pantalla. Se guarda el momento de fin y
 * una alarma avisa al terminar, aunque la app no esté abierta.
 */
object Temporizador {
    const val CANAL = "temporizador"
    private const val ACCION = "app.kiwu.android.TEMPORIZADOR"
    private const val IDENTIFICADOR = 0x54454D50 // «TEMP»

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences("temporizador", Context.MODE_PRIVATE)

    /** Cuándo termina la cuenta atrás en marcha (ms), o null si no hay ninguna. */
    fun fin(c: Context): Long? = prefs(c).getLong("fin", 0L).takeIf { it > System.currentTimeMillis() }

    fun minutos(c: Context): Int = prefs(c).getInt("minutos", 25)

    fun iniciar(c: Context, minutos: Int) {
        val fin = System.currentTimeMillis() + minutos * 60_000L
        prefs(c).edit().putLong("fin", fin).putInt("minutos", minutos).apply()
        c.getSystemService(AlarmManager::class.java)?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fin, alarma(c))
    }

    fun cancelar(c: Context) {
        prefs(c).edit().putLong("fin", 0L).apply()
        c.getSystemService(AlarmManager::class.java)?.cancel(alarma(c))
    }

    private fun alarma(c: Context) = PendingIntent.getBroadcast(
        c, IDENTIFICADOR, Intent(c, TemporizadorReceiver::class.java).setAction(ACCION),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun avisar(c: Context) {
        if (!Avisos.tienePermiso(c)) return
        val t = Idioma.contexto(c)
        if (Build.VERSION.SDK_INT >= 26) {
            c.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(CANAL, t.getString(R.string.canal_temporizador), NotificationManager.IMPORTANCE_HIGH))
        }
        val abrir = PendingIntent.getActivity(
            c, IDENTIFICADOR, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(c, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(t.getString(R.string.temporizador_listo))
            .setContentText(t.getString(R.string.temporizador_listo_texto, minutos(c)))
            .setContentIntent(abrir).setAutoCancel(true).build()
        try {
            NotificationManagerCompat.from(c).notify(IDENTIFICADOR, n)
        } catch (e: SecurityException) {
            // el permiso se retiró mientras tanto
        }
    }
}

class TemporizadorReceiver : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) = Temporizador.avisar(contexto)
}
