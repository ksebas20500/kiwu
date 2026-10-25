package app.kiwu.android.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import app.kiwu.android.R

/*
 * Horarios semanales (esquema v2 en Android). Una persona puede tener varios («Universidad», «Trabajo», «Ocio»…)
 * y cada uno contiene clases o actividades que se repiten cada semana.
 * Las horas son «flotantes»: minutos desde las 00:00 en la hora local, sin zona horaria.
 */

@Entity(tableName = "horario")
data class Horario(
    @PrimaryKey val id: String = nuevoId(),
    val nombre: String,
    val icono: String = "🎓",
    val orden: Int = 0,
    val creada: Long,
    val actualizada: Long,
    val eliminada: Long? = null,
)

/**
 * Una clase o actividad en un día de la semana. Una clase que se da varios días (lunes y miércoles) son varias filas
 * con el mismo [grupo], que se editan juntas.
 */
@Entity(
    tableName = "clase",
    foreignKeys = [ForeignKey(Horario::class, ["id"], ["horario_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("horario_id"), Index("grupo")],
)
data class Clase(
    @PrimaryKey val id: String = nuevoId(),
    @ColumnInfo(name = "horario_id") val horarioId: String,
    /** Lista de tareas con la que se relaciona (opcional). */
    @ColumnInfo(name = "materia_id") val materiaId: String? = null,
    val grupo: String = nuevoId(),
    val titulo: String,
    /** 1 = lunes … 7 = domingo. */
    @ColumnInfo(name = "dia_semana") val diaSemana: Int,
    /** Minutos desde las 00:00. */
    @ColumnInfo(name = "inicio_min") val inicioMin: Int,
    @ColumnInfo(name = "fin_min") val finMin: Int,
    val aula: String = "",
    val notas: String = "",
    /** Minutos de antelación del aviso; null = sin aviso. */
    @ColumnInfo(name = "aviso_min") val avisoMin: Int? = null,
    /** Color elegido para la clase como `#RRGGBB`; null = color automático (el de siempre). */
    val color: String? = null,
    /** Máximo de faltas de la materia (igual en todas las filas del grupo); null = sin límite. */
    @ColumnInfo(name = "max_faltas") val maxFaltas: Int? = null,
    val creada: Long,
    val actualizada: Long,
    val eliminada: Long? = null,
)

/**
 * Una inasistencia a una materia del horario. Se guarda por [grupo] (la materia, aunque se dé varios días) y no por fila,
 * y sin clave foránea: cambiar o renombrar la clase no borra su historial.
 */
@Entity(tableName = "falta", indices = [Index("grupo"), Index("horario_id")])
data class Falta(
    @PrimaryKey val id: String = nuevoId(),
    val grupo: String,
    @ColumnInfo(name = "horario_id") val horarioId: String,
    /** El día de la falta como `LocalDate.toEpochDay()`: una falta es un día, no un momento. */
    val dia: Long,
    val justificada: Boolean = false,
    val creada: Long,
)

/** Reglas del horario: funciones puras, sin base de datos (equivalente a `Reglas` para las tareas). */
object ReglasHorario {
    /** Un color de clase válido es `#RRGGBB` (hexadecimal). Cualquier otra cosa se ignora y se usa el color automático. */
    fun colorValido(texto: String?): String? = texto?.trim()?.takeIf { Regex("^#[0-9A-Fa-f]{6}$").matches(it) }?.uppercase()

    const val MINUTOS_DIA = 1440

    /** Opciones de aviso, en minutos de antelación. */
    val avisos = listOf(5, 10, 15, 30, 60)

    fun hora(minutos: Int): String = "%02d:%02d".format(minutos / 60, minutos % 60)

    fun rango(c: Clase): String = "${hora(c.inicioMin)} – ${hora(c.finMin)}"

    fun diaDe(fecha: LocalDate): Int = fecha.dayOfWeek.value

    /** Una clase necesita que el fin sea posterior al inicio, dentro del mismo día. */
    fun valida(inicioMin: Int, finMin: Int): Boolean = inicioMin in 0 until MINUTOS_DIA && finMin in 1..MINUTOS_DIA && finMin > inicioMin

    fun deDia(clases: List<Clase>, dia: Int): List<Clase> =
        clases.filter { it.diaSemana == dia }.sortedWith(compareBy<Clase> { it.inicioMin }.thenBy { it.finMin }.thenBy { it.titulo })

    fun seSolapan(a: Clase, b: Clase): Boolean =
        a.diaSemana == b.diaSemana && a.inicioMin < b.finMin && b.inicioMin < a.finMin

    /**
     * Clases ya existentes con las que chocaría una clase nueva o editada ([dias], [inicioMin], [finMin]).
     * Se ignora el propio grupo, para poder editarlo sin que choque consigo mismo.
     */
    fun choques(existentes: List<Clase>, grupo: String?, dias: Set<Int>, inicioMin: Int, finMin: Int): List<Clase> =
        existentes.filter { it.grupo != grupo && it.diaSemana in dias && it.inicioMin < finMin && inicioMin < it.finMin }
            .sortedWith(compareBy<Clase> { it.diaSemana }.thenBy { it.inicioMin })

    /** La clase en curso en ese día y minuto, si hay alguna. */
    fun enCurso(clases: List<Clase>, dia: Int, minuto: Int): Clase? =
        deDia(clases, dia).firstOrNull { minuto >= it.inicioMin && minuto < it.finMin }

    /** La siguiente clase que empieza después de ese momento (hoy o en los próximos días) y cuántos días faltan. */
    fun siguiente(clases: List<Clase>, dia: Int, minuto: Int): Pair<Clase, Int>? {
        for (desplazamiento in 0..7) {
            val d = (dia - 1 + desplazamiento) % 7 + 1
            val candidata = deDia(clases, d).firstOrNull { desplazamiento > 0 || it.inicioMin > minuto }
            if (candidata != null) return candidata to desplazamiento
        }
        return null
    }

    /** Cuándo empieza la clase la próxima vez que toca, a partir de [desde] (inclusive). */
    fun proximoInicio(c: Clase, desde: LocalDateTime): LocalDateTime {
        val dia = DayOfWeek.of(c.diaSemana.coerceIn(1, 7))
        var fecha = desde.toLocalDate()
        while (fecha.dayOfWeek != dia) fecha = fecha.plusDays(1)
        var inicio = LocalDateTime.of(fecha, LocalTime.of(c.inicioMin / 60, c.inicioMin % 60))
        if (inicio.isBefore(desde)) inicio = inicio.plusWeeks(1)
        return inicio
    }

    /** Momento (en milisegundos) en que debe sonar el aviso de la próxima clase, o null si no tiene aviso. */
    fun momentoDeAviso(c: Clase, desde: Long, zona: ZoneId = ZoneId.systemDefault()): Long? {
        val antelacion = c.avisoMin ?: return null
        var cursor = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(desde), zona)
        // Si el aviso de esta semana ya pasó, se pasa a la semana siguiente.
        repeat(3) {
            val inicio = proximoInicio(c, cursor)
            val aviso = inicio.minusMinutes(antelacion.toLong())
            val ms = aviso.atZone(zona).toInstant().toEpochMilli()
            if (ms >= desde) return ms
            cursor = inicio.plusMinutes(1)
        }
        return null
    }

    /** Texto del aviso: «Empieza a las 08:00 · Aula 204». */
    fun textoAviso(c: Clase, empieza: (hora: String) -> String): String =
        listOfNotNull(empieza(hora(c.inicioMin)), c.aula.takeIf { it.isNotBlank() }).joinToString(" · ")
}

/** Cómo de urgente es una tarea pendiente: decide el color del punto que se ve en la clase del horario. */
enum class UrgenciaTarea { Vencida, Pronto, Normal }

/** Un punto en la tarjeta de la clase: una tarea pendiente con fecha cercana (o vencida). */
data class PuntoTarea(val id: String, val urgencia: UrgenciaTarea)

/** Dónde se coloca una tarea en la hoja de tareas de una clase. */
enum class SeccionTarea(@androidx.annotation.StringRes val titulo: Int) {
    Vencidas(R.string.seccion_vencidas), EstaSemana(R.string.seccion_esta_semana), Proximas(R.string.seccion_proximas), SinFecha(R.string.seccion_sin_fecha),
}

/** Tareas de la lista vinculada a una clase: qué puntos se dibujan y cómo se agrupan en su hoja. Funciones puras. */
object TareasDeClase {
    /** Solo cuentan para los puntos las tareas con fecha dentro de este margen (más las vencidas): las lejanas no. */
    const val DIAS_VENTANA = 14
    private const val DIA_MS = 24L * 60 * 60 * 1000

    fun pendientes(deberes: List<Deber>, materiaId: String?): List<Deber> =
        if (materiaId == null) emptyList() else deberes.filter { it.materiaId == materiaId && !it.completado && it.eliminada == null }

    fun hechas(deberes: List<Deber>, materiaId: String?): List<Deber> =
        if (materiaId == null) emptyList() else deberes.filter { it.materiaId == materiaId && it.completado && it.eliminada == null }

    fun urgencia(fecha: Long, ahora: Long): UrgenciaTarea = when {
        fecha < ahora -> UrgenciaTarea.Vencida
        fecha <= ahora + 2 * DIA_MS -> UrgenciaTarea.Pronto
        else -> UrgenciaTarea.Normal
    }

    /** Puntos de la clase, de más a menos urgente (y por fecha). Sin fecha o con fecha lejana no hay punto. */
    fun puntos(deberes: List<Deber>, materiaId: String?, ahora: Long): List<PuntoTarea> =
        pendientes(deberes, materiaId)
            .filter { it.fechaEntrega != null && it.fechaEntrega <= ahora + DIAS_VENTANA * DIA_MS }
            .sortedBy { it.fechaEntrega }
            .map { PuntoTarea(it.id, urgencia(it.fechaEntrega!!, ahora)) }
            .sortedBy { it.urgencia.ordinal }

    /** La urgencia más alta entre los puntos (para el punto único de la vista de semana), o null si no hay puntos. */
    fun masUrgente(puntos: List<PuntoTarea>): UrgenciaTarea? = puntos.minByOrNull { it.urgencia.ordinal }?.urgencia

    fun seccion(d: Deber, ahora: Long): SeccionTarea {
        val f = d.fechaEntrega ?: return SeccionTarea.SinFecha
        return when {
            f < ahora -> SeccionTarea.Vencidas
            f <= ahora + 7 * DIA_MS -> SeccionTarea.EstaSemana
            else -> SeccionTarea.Proximas
        }
    }

    /** Las pendientes agrupadas por sección, en orden, cada una por fecha de entrega. Las secciones vacías no aparecen. */
    fun agrupadas(pendientes: List<Deber>, ahora: Long): List<Pair<SeccionTarea, List<Deber>>> =
        SeccionTarea.entries.mapNotNull { s ->
            val filas = pendientes.filter { seccion(it, ahora) == s }.sortedWith(compareBy({ it.fechaEntrega ?: Long.MAX_VALUE }, { it.titulo }))
            if (filas.isEmpty()) null else s to filas
        }
}
