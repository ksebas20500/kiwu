package app.kiwu.android.domain

import java.time.LocalDate

/** Cómo va una materia con sus faltas: decide el color del contador. */
enum class EstadoFaltas { SinLimite, Tranquilo, Aviso, Superado }

/** Resumen de las faltas de una materia: las que cuentan, el máximo y su estado. */
data class ContadorFaltas(val cuentan: Int, val total: Int, val maximo: Int?) {
    val estado: EstadoFaltas get() = ReglasFaltas.estado(cuentan, maximo)
    val quedan: Int? get() = maximo?.let { ReglasFaltas.quedan(cuentan, it) }
}

/** Reglas de las faltas de clase: funciones puras, sin textos (la interfaz pone las palabras en cada idioma). */
object ReglasFaltas {
    /** Desde este porcentaje del máximo el contador pasa a ámbar. */
    const val UMBRAL_AVISO = 60

    fun porGrupo(faltas: List<Falta>): Map<String, List<Falta>> = faltas.groupBy { it.grupo }

    /** Las justificadas se ven, pero no cuentan para el límite. */
    fun contar(faltas: List<Falta>): Int = faltas.count { !it.justificada }

    fun estado(cuentan: Int, maximo: Int?): EstadoFaltas = when {
        maximo == null || maximo <= 0 -> EstadoFaltas.SinLimite
        cuentan >= maximo -> EstadoFaltas.Superado
        cuentan * 100 >= maximo * UMBRAL_AVISO -> EstadoFaltas.Aviso
        else -> EstadoFaltas.Tranquilo
    }

    fun quedan(cuentan: Int, maximo: Int): Int = (maximo - cuentan).coerceAtLeast(0)

    /** El máximo de la materia: el de sus filas (se guarda igual en todas; si no, el primero que haya). */
    fun maximo(clasesDelGrupo: List<Clase>): Int? = clasesDelGrupo.firstNotNullOfOrNull { it.maxFaltas }?.takeIf { it > 0 }

    fun contador(faltas: List<Falta>, maximo: Int?): ContadorFaltas = ContadorFaltas(contar(faltas), faltas.size, maximo)

    /** Contador de cada materia (por grupo) a partir de todas las clases y faltas. */
    fun contadores(clases: List<Clase>, faltas: List<Falta>): Map<String, ContadorFaltas> {
        val porGrupo = porGrupo(faltas)
        return clases.groupBy { it.grupo }.mapValues { (grupo, filas) -> contador(porGrupo[grupo].orEmpty(), maximo(filas)) }
    }

    /** Si ese día hay clase de la materia (para avisar al marcar un día suelto, sin impedirlo). */
    fun tocaClase(dia: LocalDate, clasesDelGrupo: List<Clase>): Boolean = clasesDelGrupo.any { it.diaSemana == dia.dayOfWeek.value }

    /** Las faltas de la materia, de la más reciente a la más antigua. */
    fun ordenadas(faltas: List<Falta>): List<Falta> = faltas.sortedWith(compareByDescending<Falta> { it.dia }.thenByDescending { it.creada })

    /** Si se puede mostrar algo en la tarjeta de la clase: con máximo, o sin máximo pero con alguna falta. */
    fun seMuestra(c: ContadorFaltas?): Boolean = c != null && (c.maximo != null || c.total > 0)
}
