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
import app.kiwu.android.Idioma
import app.kiwu.android.domain.Resumenes
import app.kiwu.android.domain.ResumenSemanal
import app.kiwu.android.domain.Rachas
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** El resumen semanal: un aviso el domingo a la tarde con lo que se logró en la semana. */
object AvisosResumen {
    const val CANAL = "resumen"
    const val ACCION = "app.kiwu.android.RESUMEN"
    private const val IDENTIFICADOR = 0x52455355 // «RESU»

    fun crearCanal(contexto: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = contexto.getSystemService(NotificationManager::class.java)
        val t = Idioma.contexto(contexto)
        nm.createNotificationChannel(
            NotificationChannel(CANAL, t.getString(R.string.canal_resumen), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = t.getString(R.string.canal_resumen_desc) },
        )
    }

    fun alarma(contexto: Context): PendingIntent =
        PendingIntent.getBroadcast(
            contexto, IDENTIFICADOR, Intent(contexto, ResumenReceiver::class.java).setAction(ACCION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun mostrar(contexto: Context, r: ResumenSemanal, forzar: Boolean = false): Boolean {
        if (!forzar && !Resumenes.hayQueContar(r)) return false // una semana sin nada no merece aviso
        if (!Avisos.tienePermiso(contexto)) return false
        crearCanal(contexto)
        val t = Idioma.contexto(contexto)
        val abrir = PendingIntent.getActivity(
            contexto, IDENTIFICADOR, Intent(contexto, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val partes = buildList {
            if (r.tareasCompletadas > 0 || forzar) add(t.resources.getQuantityString(R.plurals.resumen_tareas, r.tareasCompletadas.coerceAtLeast(1), r.tareasCompletadas.coerceAtLeast(1)))
            if (r.minutosEstudio > 0 || forzar) {
                val min = if (forzar && r.minutosEstudio == 0) 25 else r.minutosEstudio
                add(
                    if (min >= 60) t.getString(R.string.resumen_horas_estudio, min / 60, min % 60)
                    else t.getString(R.string.resumen_min_estudio, min),
                )
            }
            if (r.racha > 0 || forzar) add(t.resources.getQuantityString(R.plurals.resumen_racha, r.racha.coerceAtLeast(1), r.racha.coerceAtLeast(1)))
        }
        val notificacion = NotificationCompat.Builder(contexto, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(t.getString(R.string.resumen_titulo))
            .setContentText(partes.joinToString(" · "))
            .setCategory(NotificationCompat.CATEGORY_EVENT)
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
}

/** Programa (o cancela) la alarma del próximo domingo a las 19:00. No necesita ser exacta: unos minutos de retraso no importan. */
class ProgramadorResumen(private val contexto: Context) {
    private val alarmas = contexto.getSystemService(AlarmManager::class.java)

    fun reconciliar(activo: Boolean, ahora: Long = System.currentTimeMillis()) {
        val pi = AvisosResumen.alarma(contexto)
        if (activo) {
            alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, Resumenes.proximoAviso(ahora), pi)
        } else {
            alarmas.cancel(pi)
        }
    }
}

private val alcanceResumen = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** Suena el domingo: arma el resumen de la semana y deja programado el siguiente. */
class ResumenReceiver : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        val app = contexto.applicationContext as KiwuApp
        val pendiente = goAsync()
        alcanceResumen.launch {
            try {
                val ahora = System.currentTimeMillis()
                if (!app.ajustes.flujo.first().resumenSemanal) return@launch
                val zona = ZoneId.systemDefault()
                val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
                val lunes = hoy.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val deberes = app.repo.deberes.first()
                val racha = Rachas.calcular(app.repo.entradaRachaAhora(), ahora).actual
                val resumen = Resumenes.de(deberes, app.repo.sesionesAhora(), racha, lunes, hoy, zona)
                AvisosResumen.mostrar(contexto, resumen)
                // Un minuto después, para que la próxima sea el domingo siguiente y no este mismo.
                ProgramadorResumen(contexto).reconciliar(true, ahora + 60_000)
            } finally {
                pendiente.finish()
            }
        }
    }
}
