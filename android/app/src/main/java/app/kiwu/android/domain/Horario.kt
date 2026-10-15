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
    val creada: Long,
    val actualizada: Long,
    val eliminada: Long? = null,
)

/** Reglas del horario: funciones puras, sin base de datos (equivalente a `Reglas` para las tareas). */
object ReglasHorario {
    const val MINUTOS_DIA = 1440

    val diasLargos = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
    val diasCortos = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    val diasLetra = listOf("L", "M", "X", "J", "V", "S", "D")

    /** Opciones de aviso, en minutos de antelación. */
    val avisos = listOf(5, 10, 15, 30, 60)

    fun nombreDia(dia: Int): String = diasLargos[(dia - 1).coerceIn(0, 6)]

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
    fun textoAviso(c: Clase): String = listOfNotNull("Empieza a las ${hora(c.inicioMin)}", c.aula.takeIf { it.isNotBlank() }).joinToString(" · ")

    /** Cuántos días faltan hasta la siguiente clase, dicho en palabras. */
    fun cuando(dias: Int, dia: Int): String = when (dias) {
        0 -> "hoy"
        1 -> "mañana"
        else -> diasLargos[(dia - 1).coerceIn(0, 6)].lowercase()
    }
}
