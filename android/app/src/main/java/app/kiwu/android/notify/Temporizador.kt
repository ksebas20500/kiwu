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
import app.kiwu.android.domain.SesionEstudio
import app.kiwu.android.domain.TiempoEstudio
import java.time.LocalDate

/**
 * Temporizador de estudio (Pomodoro): una cuenta atrás que sobrevive a cerrar la pantalla. Se guarda el momento de fin y
 * una alarma avisa al terminar, aunque la app no esté abierta. Mientras corre sale como notificación permanente con el
 * tiempo que queda (el sistema lo actualiza solo, visible en bloqueo y en pantalla apagada) con botón para parar.
 * Se puede pausar: a media cuenta queda retenida y al reanudar sigue desde donde estaba. También se puede arrancar una
 * pausa activa (descanso), que NO se registra como tiempo de estudio. Si se elige, al estudiar se activa «No molestar»
 * (permiso especial que se da una vez) y se devuelve el teléfono a la normalidad al terminar o parar.
 */
object Temporizador {
    const val CANAL = "temporizador"
    const val CANAL_CURSO = "temporizador_curso"
    const val ACCION = "app.kiwu.android.TEMPORIZADOR"
    const val ACCION_PARAR = "app.kiwu.android.TEMPORIZADOR_PARAR"
    private const val IDENTIFICADOR = 0x54454D50 // «TEMP»
    private const val ID_CURSO = IDENTIFICADOR + 1

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences("temporizador", Context.MODE_PRIVATE)

    /** Cuándo termina la cuenta atrás en marcha (ms), o null si no hay ninguna (ni en pausa). */
    fun fin(c: Context): Long? = prefs(c).getLong("fin", 0L).takeIf { it > System.currentTimeMillis() }

    fun minutos(c: Context): Int = prefs(c).getInt("minutos", 25)

    /** Lista (materia) a la que se apunta el tiempo estudiado; null = ninguna. */
    fun materia(c: Context): String? = prefs(c).getString("materia", null)

    fun elegirMateria(c: Context, id: String?) = prefs(c).edit().putString("materia", id).apply()

    /** Tiempo estudiado (solo en este dispositivo). */
    fun sesiones(c: Context): List<SesionEstudio> = TiempoEstudio.leer(prefs(c).getString("sesiones", "").orEmpty())

    private fun registrar(c: Context, minutos: Int) {
        if (minutos < 1) return
        val nueva = SesionEstudio(materia(c), LocalDate.now().toEpochDay(), minutos)
        prefs(c).edit().putString("sesiones", TiempoEstudio.escribir(TiempoEstudio.anadir(sesiones(c), nueva))).apply()
    }

    /** Qué queda de la cuenta atrás cuando está pausada (ms); 0 = no hay pausa. */
    fun pausaRestante(c: Context): Long = prefs(c).getLong("pausa", 0L)

    fun quiereNoMolestar(c: Context): Boolean = prefs(c).getBoolean("noMolestar", false)

    fun elegirNoMolestar(c: Context, sí: Boolean) = prefs(c).edit().putBoolean("noMolestar", sí).apply()

    /** El permiso «No molestar» (política de notificaciones) se da en Ajustes del sistema, una sola vez. */
    fun puedeNoMolestar(c: Context): Boolean =
        c.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted == true

    /** Color del número grande: nombre de un acento, «#RRGGBB» o «auto» (el color del tema). */
    fun color(c: Context): String = prefs(c).getString("color", "auto") ?: "auto"

    fun elegirColor(c: Context, cual: String) = prefs(c).edit().putString("color", cual).apply()

    fun iniciar(c: Context, minutos: Int) = arrancar(c, minutos, descanso = false)

    /** Pausa activa: una cuenta atrás igual a las demás, pero no se registra como tiempo de estudio. */
    fun iniciarDescanso(c: Context, minutos: Int) = arrancar(c, minutos, descanso = true)

    private fun arrancar(c: Context, minutos: Int, descanso: Boolean) {
        if (minutos < 1) return
        val fin = System.currentTimeMillis() + minutos * 60_000L
        prefs(c).edit()
            .putLong("fin", fin).putInt("minutos", minutos)
            .putLong("pausa", 0L).putBoolean("descanso", descanso)
            .apply()
        c.getSystemService(AlarmManager::class.java)?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fin, alarma(c))
        // «No molestar» es para estudiar: en el descanso el teléfono vuelve a sonar (o no se enciende).
        if (!descanso) activarNoMolestar(c)
        notificarCurso(c)
    }

    /** Retiene la cuenta atrás: se detiene la alarma y se guarda lo que queda para reanudarla luego. */
    fun pausar(c: Context) {
        fin(c)?.let { prefs(c).edit().putLong("pausa", it - System.currentTimeMillis()).apply() }
        prefs(c).edit().putLong("fin", 0L).apply()
        c.getSystemService(AlarmManager::class.java)?.cancel(alarma(c))
        NotificationManagerCompat.from(c).cancel(ID_CURSO)
    }

    /** Sigue desde donde quedó la pausa. */
    fun reanudar(c: Context) {
        val queda = pausaRestante(c)
        if (queda <= 0) return
        val fin = System.currentTimeMillis() + queda
        prefs(c).edit().putLong("pausa", 0L).putLong("fin", fin).apply()
        c.getSystemService(AlarmManager::class.java)?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fin, alarma(c))
        notificarCurso(c)
    }

    /** Para antes de tiempo (también desde la notificación): cuenta los minutos que sí se estudiaron. */
    fun cancelar(c: Context) {
        val queda = fin(c) ?: pausaRestante(c).takeIf { it > 0 }
        // Un descanso no es tiempo de estudio: no se registra.
        if (!prefs(c).getBoolean("descanso", false)) queda?.let { registrar(c, TiempoEstudio.minutosHechos(minutos(c), it)) }
        soltar(c)
    }

    /** Devuelve todo a reposo: sin cuenta atrás, sin alarma, sin notificación en curso y sin «No molestar». */
    private fun soltar(c: Context) {
        prefs(c).edit().putLong("fin", 0L).putLong("pausa", 0L).putBoolean("descanso", false).apply()
        c.getSystemService(AlarmManager::class.java)?.cancel(alarma(c))
        NotificationManagerCompat.from(c).cancel(ID_CURSO)
        quitarNoMolestar(c)
    }

    /** La alarma de fin: apunta la sesión completa (o avisa de que se acabó el descanso). */
    fun terminar(c: Context) {
        if (prefs(c).getBoolean("descanso", false)) avisoDescanso(c)
        else {
            registrar(c, minutos(c))
            avisar(c)
        }
        soltar(c)
    }

    private fun alarma(c: Context) = PendingIntent.getBroadcast(
        c, IDENTIFICADOR, Intent(c, TemporizadorReceiver::class.java).setAction(ACCION),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun parar(c: Context) = PendingIntent.getBroadcast(
        c, IDENTIFICADOR + 1, Intent(c, TemporizadorReceiver::class.java).setAction(ACCION_PARAR),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun abrir(c: Context) = PendingIntent.getActivity(
        c, IDENTIFICADOR, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    // --- «No molestar» -------------------------------------------------------------------------------

    private fun activarNoMolestar(c: Context) {
        if (!quiereNoMolestar(c) || !puedeNoMolestar(c)) return
        c.getSystemService(NotificationManager::class.java)?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        prefs(c).edit().putBoolean("dndSuyo", true).apply()
    }

    private fun quitarNoMolestar(c: Context) {
        if (!prefs(c).getBoolean("dndSuyo", false)) return
        prefs(c).edit().putBoolean("dndSuyo", false).apply()
        c.getSystemService(NotificationManager::class.java)?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
    }

    // --- avisos --------------------------------------------------------------------------------------

    /** La notificación permanente con el tiempo que queda: la bloqueo de pantalla y el AOD la muestran apagando la pantalla. */
    private fun notificarCurso(c: Context) {
        if (!Avisos.tienePermiso(c)) return
        val t = Idioma.contexto(c)
        if (Build.VERSION.SDK_INT >= 26) {
            c.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(CANAL_CURSO, t.getString(R.string.canal_curso), NotificationManager.IMPORTANCE_LOW))
        }
        val descanso = prefs(c).getBoolean("descanso", false)
        val n = NotificationCompat.Builder(c, CANAL_CURSO)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(
                if (descanso) t.getString(R.string.temporizador_descanso_curso, minutos(c))
                else t.getString(R.string.temporizador_curso, minutos(c)),
            )
            .setWhen(fin(c) ?: (System.currentTimeMillis() + pausaRestante(c)))
            .setUsesChronometer(true)
            .setChronometerCountDown(true) // API 24+, minSdk 26
            .setOngoing(true).setOnlyAlertOnce(true)
            .setContentIntent(abrir(c))
            .addAction(0, t.getString(R.string.temporizador_parar), parar(c))
            .build()
        try {
            NotificationManagerCompat.from(c).notify(ID_CURSO, n)
        } catch (e: SecurityException) {
            // el permiso se retiró mientras tanto
        }
    }

    fun avisar(c: Context) {
        if (!Avisos.tienePermiso(c)) return
        val t = Idioma.contexto(c)
        if (Build.VERSION.SDK_INT >= 26) {
            c.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(CANAL, t.getString(R.string.canal_temporizador), NotificationManager.IMPORTANCE_HIGH))
        }
        val n = NotificationCompat.Builder(c, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(t.getString(R.string.temporizador_listo))
            .setContentText(t.getString(R.string.temporizador_listo_texto, minutos(c)))
            .setContentIntent(abrir(c)).setAutoCancel(true).build()
        try {
            NotificationManagerCompat.from(c).notify(IDENTIFICADOR, n)
        } catch (e: SecurityException) {
            // el permiso se retiró mientras tanto
        }
    }

    private fun avisoDescanso(c: Context) {
        if (!Avisos.tienePermiso(c)) return
        val t = Idioma.contexto(c)
        if (Build.VERSION.SDK_INT >= 26) {
            c.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(CANAL, t.getString(R.string.canal_temporizador), NotificationManager.IMPORTANCE_HIGH))
        }
        val n = NotificationCompat.Builder(c, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(t.getString(R.string.temporizador_descanso_listo))
            .setContentText(t.getString(R.string.temporizador_descanso_listo_texto))
            .setContentIntent(abrir(c)).setAutoCancel(true).build()
        try {
            NotificationManagerCompat.from(c).notify(IDENTIFICADOR, n)
        } catch (e: SecurityException) {
            // el permiso se retiró mientras tanto
        }
    }
}

class TemporizadorReceiver : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        when (intent.action) {
            Temporizador.ACCION_PARAR -> Temporizador.cancelar(contexto)
            else -> Temporizador.terminar(contexto)
        }
    }
}
