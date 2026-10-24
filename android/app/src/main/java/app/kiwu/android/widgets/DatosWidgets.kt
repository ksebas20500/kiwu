package app.kiwu.android.widgets

import app.kiwu.android.data.ResumenPagina
import app.kiwu.android.domain.Clase
import app.kiwu.android.domain.Cuaderno
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.Elemento
import app.kiwu.android.domain.GrupoLista
import app.kiwu.android.domain.Horario
import app.kiwu.android.domain.Materia
import app.kiwu.android.domain.Reglas
import app.kiwu.android.domain.ReglasHorario
import app.kiwu.android.domain.TareasDeClase
import app.kiwu.android.domain.UrgenciaTarea
import app.kiwu.android.domain.esEvento
import app.kiwu.android.ui.horario.ColoresClase
import app.kiwu.android.ui.ColorPastel
import java.time.Instant
import java.time.ZoneId

/** Valores de la configuración de cada widget (se guardan en el estado de Glance, por id de widget). */
object ConfigWidget {
    const val TODAS = "todas"
    const val HOY = "hoy"
    const val PROXIMOS = "proximos"
    const val LISTA = "lista:"
    const val GRUPO = "grupo:"
    const val HORARIO = "horario:"
    const val CUADERNO = "cuaderno:"

    /** Hay dos configuraciones que empiezan por el mismo texto: se comprueban del prefijo más largo al más corto. */
    fun lista(id: String) = LISTA + id
    fun grupo(id: String) = GRUPO + id
    fun horario(id: String) = HORARIO + id
    fun cuaderno(id: String) = CUADERNO + id
}

data class FilaDeber(val id: String, val titulo: String, val lista: String, val fecha: String, val vencida: Boolean)

data class DatosDeberes(
    val titulo: String,
    val pendientes: List<FilaDeber>,
    val total: Int,
    /** Si hay listas (aunque no tengan tareas). Sin listas, el widget invita a crear la primera. */
    val hayListas: Boolean,
)

data class FilaClase(
    val id: String,
    val titulo: String,
    val aula: String,
    /** Nombre del horario, solo cuando el widget muestra todos. */
    val horario: String?,
    val diaSemana: Int,
    val inicioMin: Int,
    val finMin: Int,
    val color: ColorPastel,
    val urgencia: UrgenciaTarea?,
)

data class DatosHorario(
    val nombre: String?,
    val enCurso: FilaClase?,
    val siguiente: FilaClase?,
    /** Días hasta la siguiente clase: 0 = hoy, 1 = mañana… */
    val diasSiguiente: Int,
    val hoy: List<FilaClase>,
    val hayHorarios: Boolean,
)

data class FilaNota(val id: String, val titulo: String, val icono: String, val resumen: String)

data class DatosNotas(
    val cuaderno: Cuaderno?,
    val notas: List<FilaNota>,
    /** Cuaderno donde crear la nota rápida (null = que la app elija el primero). */
    val cuadernoNuevo: String?,
    val hayCuadernos: Boolean,
)

/**
 * Lo que muestran los widgets, calculado a partir de los datos de la app. Son funciones puras (sin Android) para poder
 * probarlas en JVM, igual que `Reglas` y `ReglasHorario`.
 */
object DatosWidgets {
    private val meses = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic")

    // --- deberes ---------------------------------------------------------------------------------

    fun deberes(
        materias: List<Materia>,
        grupos: List<GrupoLista>,
        deberes: List<Deber>,
        config: String?,
        ahora: Long,
        zona: ZoneId = ZoneId.systemDefault(),
    ): DatosDeberes {
        val reglas = Reglas(zona)
        val nombres = materias.associate { it.id to it.nombre }
        val (elemento, titulo, soloListas) = alcance(config, materias, grupos)
        val visibles = deberes.filter { !it.esEvento && (soloListas == null || it.materiaId in soloListas) }
        val pendientes = reglas.pendientes(visibles, elemento, ahora).map {
            FilaDeber(
                id = it.id,
                titulo = it.titulo,
                lista = nombres[it.materiaId].orEmpty(),
                fecha = textoFecha(it.fechaEntrega, ahora, zona),
                vencida = reglas.estaVencido(it, ahora),
            )
        }
        return DatosDeberes(titulo, pendientes, pendientes.size, materias.isNotEmpty())
    }

    private fun alcance(
        config: String?,
        materias: List<Materia>,
        grupos: List<GrupoLista>,
    ): Triple<Elemento, String, Set<String>?> {
        val valor = config ?: ConfigWidget.TODAS
        return when {
            valor == ConfigWidget.HOY -> Triple(Elemento.Hoy, "Hoy", null)
            valor == ConfigWidget.PROXIMOS -> Triple(Elemento.Proximos, "Próximos 7 días", null)
            valor.startsWith(ConfigWidget.LISTA) -> {
                val id = valor.removePrefix(ConfigWidget.LISTA)
                if (materias.none { it.id == id }) Triple(Elemento.Todos, "Deberes", null)
                else Triple(Elemento.DeMateria(id), materias.first { it.id == id }.nombre, setOf(id))
            }
            valor.startsWith(ConfigWidget.GRUPO) -> {
                val id = valor.removePrefix(ConfigWidget.GRUPO)
                Triple(Elemento.DeGrupo(id), grupos.firstOrNull { it.id == id }?.nombre ?: "Deberes", materias.filter { it.grupoId == id }.map { it.id }.toSet())
            }
            else -> Triple(Elemento.Todos, "Deberes", null)
        }
    }

    /** Fecha de entrega en palabras: «Vencida», «Hoy», «Mañana», el día de la semana o «3 oct». */
    fun textoFecha(ms: Long?, ahora: Long, zona: ZoneId = ZoneId.systemDefault()): String {
        if (ms == null) return "Sin fecha"
        val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
        val fecha = Instant.ofEpochMilli(ms).atZone(zona).toLocalDate()
        return when {
            fecha.isBefore(hoy) -> "Vencida"
            fecha == hoy -> "Hoy"
            fecha == hoy.plusDays(1) -> "Mañana"
            fecha.isBefore(hoy.plusDays(7)) -> ReglasHorario.diasCortos[fecha.dayOfWeek.value - 1]
            else -> "${fecha.dayOfMonth} ${meses[fecha.monthValue - 1]}"
        }
    }

    // --- horario ---------------------------------------------------------------------------------

    fun horario(
        horarios: List<Horario>,
        clases: List<Clase>,
        deberes: List<Deber>,
        config: String?,
        ahora: Long,
        zona: ZoneId = ZoneId.systemDefault(),
    ): DatosHorario {
        val elegido = config?.takeIf { it.startsWith(ConfigWidget.HORARIO) }
            ?.removePrefix(ConfigWidget.HORARIO)
            ?.let { id -> horarios.firstOrNull { it.id == id } }
        val visibles = if (elegido == null) clases else clases.filter { it.horarioId == elegido.id }
        val nombres = horarios.associate { it.id to it.nombre }
        val reloj = Instant.ofEpochMilli(ahora).atZone(zona)
        val dia = reloj.dayOfWeek.value
        val minuto = reloj.hour * 60 + reloj.minute

        fun fila(c: Clase) = FilaClase(
            id = c.id,
            titulo = c.titulo,
            aula = c.aula,
            horario = if (elegido == null) nombres[c.horarioId] else null,
            diaSemana = c.diaSemana,
            inicioMin = c.inicioMin,
            finMin = c.finMin,
            color = ColoresClase.de(c),
            urgencia = TareasDeClase.masUrgente(TareasDeClase.puntos(deberes, c.materiaId, ahora)),
        )

        val enCurso = ReglasHorario.enCurso(visibles, dia, minuto)?.let(::fila)
        val siguiente = ReglasHorario.siguiente(visibles, dia, minuto)
        return DatosHorario(
            nombre = elegido?.nombre,
            enCurso = enCurso,
            siguiente = siguiente?.first?.let(::fila),
            diasSiguiente = siguiente?.second ?: 0,
            hoy = ReglasHorario.deDia(visibles, dia).map(::fila),
            hayHorarios = horarios.isNotEmpty(),
        )
    }

    /**
     * Cuándo toca refrescar los widgets de horario: el próximo cambio de clase (inicio o fin) o, si no queda ninguno
     * hoy, la medianoche. En milisegundos.
     */
    fun proximoRefresco(
        clases: List<Clase>,
        ahora: Long,
        zona: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val reloj = Instant.ofEpochMilli(ahora).atZone(zona)
        val hoy = reloj.toLocalDate()
        val inicioDia = hoy.atStartOfDay(zona).toInstant().toEpochMilli()
        val medianoche = hoy.plusDays(1).atStartOfDay(zona).toInstant().toEpochMilli()
        val minuto = reloj.hour * 60 + reloj.minute
        val cambios = ReglasHorario.deDia(clases, reloj.dayOfWeek.value)
            .flatMap { listOf(it.inicioMin, it.finMin) }
            .filter { it > minuto }
            .map { inicioDia + it * 60_000L }
            .filter { it > ahora }
        return (cambios.minOrNull() ?: medianoche).coerceAtMost(medianoche)
    }

    // --- notas -----------------------------------------------------------------------------------

    fun notas(cuadernos: List<Cuaderno>, paginas: List<ResumenPagina>, config: String?): DatosNotas {
        val elegido = config?.takeIf { it.startsWith(ConfigWidget.CUADERNO) }
            ?.removePrefix(ConfigWidget.CUADERNO)
            ?.let { id -> cuadernos.firstOrNull { it.id == id } }
        val visibles = if (elegido == null) paginas else paginas.filter { it.cuadernoId == elegido.id }
        val notas = visibles.sortedByDescending { it.actualizada }.map {
            FilaNota(
                id = it.id,
                titulo = it.titulo.ifBlank { "Sin título" },
                icono = it.icono.ifBlank { "📄" },
                resumen = resumen(it.textoPlano),
            )
        }
        return DatosNotas(elegido, notas, elegido?.id, cuadernos.isNotEmpty())
    }

    /** Primer trozo de la nota, en una línea y sin pasarse de largo. */
    fun resumen(texto: String?, limite: Int = 90): String {
        val limpio = texto?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
        if (limpio.length <= limite) return limpio
        val corte = limpio.substring(0, limite).substringBeforeLast(' ', limpio.substring(0, limite))
        return "$corte…"
    }
}
