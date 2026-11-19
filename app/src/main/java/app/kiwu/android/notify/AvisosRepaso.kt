package app.kiwu.android.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.kiwu.android.Idioma
import app.kiwu.android.KiwuApp
import app.kiwu.android.MainActivity
import app.kiwu.android.R
import app.kiwu.android.domain.RepasoGlobal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Aviso diario «Tarjetas para hoy»: a las 18:00, solo si la cola global de repaso tiene tarjetas vencidas. */
object AvisosRepaso {
    const val CANAL = "repaso"
    const val ACCION = "app.kiwu.android.REPASO"
    private const val IDENTIFICADOR = 0x52455041 // «REPA»

    fun crearCanal(contexto: Context) {
        val nm = contexto.getSystemService(NotificationManager::class.java)
        val t = Idioma.contexto(contexto)
        nm.createNotificationChannel(
            NotificationChannel(CANAL, t.getString(R.string.canal_repaso), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = t.getString(R.string.canal_repaso_desc) },
        )
    }

    fun alarma(contexto: Context): PendingIntent =
        PendingIntent.getBroadcast(
            contexto, IDENTIFICADOR, Intent(contexto, RepasoReceiver::class.java).setAction(ACCION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun mostrar(contexto: Context, tarjetas: Int): Boolean {
        if (tarjetas <= 0 || !Avisos.tienePermiso(contexto)) return false
        crearCanal(contexto)
        val textos = Idioma.contexto(contexto)
        val abrir = PendingIntent.getActivity(
            contexto, IDENTIFICADOR,
            Intent(contexto, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_REPASO, true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notificacion = NotificationCompat.Builder(contexto, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(textos.getString(R.string.repaso_titulo))
            .setContentText(textos.resources.getQuantityString(R.plurals.repaso_texto, tarjetas, tarjetas))
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

    fun quitar(contexto: Context) {
        NotificationManagerCompat.from(contexto).cancel(IDENTIFICADOR)
        Avisos.quitarSobrantes(contexto, CANAL, emptyList())
    }
}

/** Programa (o cancela) el aviso diario. No necesita ser exacto: unos minutos de retraso no importan. */
class ProgramadorRepaso(private val contexto: Context) {
    private val alarmas = contexto.getSystemService(AlarmManager::class.java)

    fun reconciliar(activo: Boolean, ahora: Long = System.currentTimeMillis()) {
        val pi = AvisosRepaso.alarma(contexto)
        if (activo) {
            alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, RepasoGlobal.proximoAviso(ahora), pi)
        } else {
            alarmas.cancel(pi)
            AvisosRepaso.quitar(contexto)
        }
    }
}

private val alcanceRepaso = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** Suena a las 18:00: cuenta las tarjetas vencidas de todas las notas y deja programado el aviso de mañana. */
class RepasoReceiver : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        val app = contexto.applicationContext as KiwuApp
        val pendiente = goAsync()
        alcanceRepaso.launch {
            try {
                val ahora = System.currentTimeMillis()
                if (!app.ajustes.flujo.first().repasoAviso) return@launch
                val cola = RepasoGlobal.cola(
                    app.repo.paginasConTarjetas.first(),
                    app.repo.cuadernos.first(),
                    app.repo.deberes.first(),
                    app.repo.materias.first(),
                    ahora,
                )
                if (cola.isNotEmpty()) AvisosRepaso.mostrar(contexto, cola.size)
                // Un minuto después, para que la próxima sea la de mañana y no esta misma.
                ProgramadorRepaso(contexto).reconciliar(true, ahora + 60_000)
            } finally {
                pendiente.finish()
            }
        }
    }
}
