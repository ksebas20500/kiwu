package app.kiwu.android.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.kiwu.android.KiwuApp
import app.kiwu.android.MainActivity
import app.kiwu.android.R
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.Recordatorios
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import app.kiwu.android.Idioma
import app.kiwu.android.Textos
import app.kiwu.android.domain.ID_EVENTOS

/** Canal de notificaciones y las acciones de cada aviso. */
object Avisos {
    const val CANAL = "recordatorios"
    const val ACCION_AVISAR = "app.kiwu.android.AVISAR"
    const val ACCION_COMPLETAR = "app.kiwu.android.COMPLETAR"
    const val ACCION_POSPONER = "app.kiwu.android.POSPONER"
    const val EXTRA_ID = "id"
    const val EXTRA_REAVISO = "reaviso"
    const val EXTRA_SEGUNDO = "segundo"

    /** Cuánto se pospone un aviso con el botón «Posponer». */
    const val POSPONER_MS = 3_600_000L

    fun crearCanal(contexto: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = contexto.getSystemService(NotificationManager::class.java)
        // Se vuelve a crear siempre: así el nombre sigue al idioma de la app (Android conserva lo que el usuario cambió).
        val t = Idioma.contexto(contexto)
        nm.createNotificationChannel(
            NotificationChannel(CANAL, t.getString(R.string.canal_tareas), NotificationManager.IMPORTANCE_HIGH)
                .apply { description = t.getString(R.string.canal_tareas_desc) },
        )
    }

    fun tienePermiso(contexto: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun identificador(id: String) = id.hashCode()

    private fun difusion(contexto: Context, accion: String, id: String, extra: Boolean = false, segundo: Boolean = false): PendingIntent =
        PendingIntent.getBroadcast(
            contexto, identificador(id) * 31 + accion.hashCode() + if (segundo) 7 else 0,
            Intent(contexto, if (accion == ACCION_AVISAR) AlarmaReceiver::class.java else AccionReceiver::class.java)
                .setAction(accion).putExtra(EXTRA_ID, id).putExtra(EXTRA_REAVISO, extra).putExtra(EXTRA_SEGUNDO, segundo),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /** Muestra el aviso de una tarea. Devuelve false si no se pudo (p. ej. sin permiso): entonces no cuenta como avisada. */
    fun mostrar(contexto: Context, d: Deber, texto: String, nombreLista: String?): Boolean {
        if (!tienePermiso(contexto)) return false
        crearCanal(contexto)
        val t = Idioma.contexto(contexto)
        val abrir = PendingIntent.getActivity(
            contexto, identificador(d.id),
            Intent(contexto, MainActivity::class.java).putExtra(MainActivity.EXTRA_TAREA, d.id).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notificacion = NotificationCompat.Builder(contexto, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(d.titulo)
            .setContentText(listOfNotNull(texto, nombreLista).joinToString(" · "))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .addAction(0, t.getString(R.string.completar), difusion(contexto, ACCION_COMPLETAR, d.id))
            .addAction(0, t.getString(R.string.posponer_1h), difusion(contexto, ACCION_POSPONER, d.id))
            .build()
        return try {
            NotificationManagerCompat.from(contexto).notify(identificador(d.id), notificacion)
            true
        } catch (e: SecurityException) {
            false // el permiso se retiró mientras tanto
        }
    }

    fun quitar(contexto: Context, id: String) = NotificationManagerCompat.from(contexto).cancel(identificador(id))

    fun alarmaAvisar(contexto: Context, id: String, reaviso: Boolean = false, segundo: Boolean = false) = difusion(contexto, ACCION_AVISAR, id, reaviso, segundo)
}

/** Mantiene las alarmas del sistema al día con las tareas: una por tarea con aviso pendiente. */
class ProgramadorAvisos(private val contexto: Context) {
    private val alarmas = contexto.getSystemService(AlarmManager::class.java)
    private val programadas = HashSet<String>()
    private val programadasSegundo = HashSet<String>()

    fun puedeExactas(): Boolean = Build.VERSION.SDK_INT < 31 || alarmas.canScheduleExactAlarms()

    /** [segundoMin]: si es mayor que 0, además del aviso normal suena otro esos minutos antes de la entrega. */
    fun reconciliar(deberes: List<Deber>, segundoMin: Int = 0, ahora: Long = System.currentTimeMillis()) {
        val vigentes = HashSet<String>()
        val vigentesSegundo = HashSet<String>()
        for (d in deberes) {
            Recordatorios.momentoDeDisparo(d, ahora)?.let { programar(d.id, it); vigentes += d.id }
            val entrega = d.fechaEntrega
            if (segundoMin > 0 && entrega != null && !d.completado) {
                val segundo = entrega - segundoMin * 60_000L
                if (segundo > ahora) { programar(d.id, segundo, segundo = true); vigentesSegundo += d.id }
            }
        }
        for (id in programadas - vigentes) alarmas.cancel(Avisos.alarmaAvisar(contexto, id))
        for (id in programadasSegundo - vigentesSegundo) alarmas.cancel(Avisos.alarmaAvisar(contexto, id, segundo = true))
        programadas.clear(); programadas += vigentes
        programadasSegundo.clear(); programadasSegundo += vigentesSegundo
    }

    fun programar(id: String, cuando: Long, reaviso: Boolean = false, segundo: Boolean = false) {
        val pi = Avisos.alarmaAvisar(contexto, id, reaviso, segundo)
        try {
            if (puedeExactas()) alarmas.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi)
            else alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi)
        } catch (e: SecurityException) {
            alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi)
        }
    }
}

private val alcanceReceptores = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** Suena la alarma: se comprueba que la tarea siga pendiente y se muestra el aviso. */
class AlarmaReceiver : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        val id = intent.getStringExtra(Avisos.EXTRA_ID) ?: return
        val reaviso = intent.getBooleanExtra(Avisos.EXTRA_REAVISO, false) || intent.getBooleanExtra(Avisos.EXTRA_SEGUNDO, false)
        val segundo = intent.getBooleanExtra(Avisos.EXTRA_SEGUNDO, false)
        val app = contexto.applicationContext as KiwuApp
        val pendiente = goAsync()
        alcanceReceptores.launch {
            try {
                val ahora = System.currentTimeMillis()
                val d = app.repo.deber(id)
                val toca = d != null && !d.completado && if (reaviso) true else Recordatorios.pendientesDeAviso(listOf(d), ahora).isNotEmpty()
                if (d != null && toca) {
                    val lista = if (d.materiaId == ID_EVENTOS) Textos.eventos else app.repo.materia(d.materiaId)?.nombre
                    // El segundo aviso no cambia el estado «avisada» del primero.
                    if (Avisos.mostrar(contexto, d, Recordatorios.textoAviso(d, ahora, Idioma.contexto(contexto).resources), lista) && !segundo) app.repo.marcarAvisado(id, ahora)
                }
            } finally {
                pendiente.finish()
            }
        }
    }
}

/** Botones del aviso: «Completar» y «Posponer». */
class AccionReceiver : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        val id = intent.getStringExtra(Avisos.EXTRA_ID) ?: return
        val app = contexto.applicationContext as KiwuApp
        val pendiente = goAsync()
        alcanceReceptores.launch {
            try {
                Avisos.quitar(contexto, id)
                when (intent.action) {
                    Avisos.ACCION_COMPLETAR -> app.repo.marcar(id, true)
                    Avisos.ACCION_POSPONER -> ProgramadorAvisos(contexto).programar(id, System.currentTimeMillis() + Avisos.POSPONER_MS, reaviso = true)
                }
            } finally {
                pendiente.finish()
            }
        }
    }
}

/** Al encender el teléfono o actualizar la app: basta con que arranque la aplicación; allí se reprograman las alarmas. */
class ArranqueReceiver : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        (contexto.applicationContext as KiwuApp).iniciarAvisos()
    }
}
