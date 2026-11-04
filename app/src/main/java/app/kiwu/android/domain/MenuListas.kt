package app.kiwu.android.domain

/** Un nodo del menú de listas de Tareas: una carpeta (con sus listas) o una lista suelta. */
sealed interface NodoMenu {
    data class Carpeta(val grupo: GrupoLista, val listas: List<Entrada>, val pendientes: Int) : NodoMenu
    data class Lista(val entrada: Entrada) : NodoMenu
}

/** Una lista con su número de tareas pendientes. */
data class Entrada(val materia: Materia, val pendientes: Int)

/** Cómo se ordena el menú: manual (el que elige la persona), alfabético o por tareas pendientes. */
object OrdenMenu {
    const val MANUAL = "manual"
    const val ALFABETICO = "alfabetico"
    const val PENDIENTES = "pendientes"
}

/** Arma el árbol del menú (un solo nivel). Funciones puras, sin base de datos. */
object MenuListas {
    private val porNombre = Comparator<String> { a, b -> a.compareTo(b, ignoreCase = true) }

    private fun ordenar(entradas: List<Entrada>, orden: String): List<Entrada> = when (orden) {
        OrdenMenu.ALFABETICO -> entradas.sortedWith(compareBy(porNombre) { it.materia.nombre })
        OrdenMenu.PENDIENTES -> entradas.sortedWith(compareByDescending<Entrada> { it.pendientes }.thenBy(porNombre) { it.materia.nombre })
        else -> entradas.sortedWith(compareBy<Entrada> { it.materia.orden }.thenBy(porNombre) { it.materia.nombre })
    }

    /**
     * Las carpetas van primero y después las listas sueltas. Una lista cuya carpeta ya no existe cuenta como suelta.
     * [pendientes] es el número de tareas pendientes de cada lista (por id).
     */
    fun construir(materias: List<Materia>, grupos: List<GrupoLista>, pendientes: Map<String, Int>, orden: String): List<NodoMenu> {
        val idsGrupo = grupos.map { it.id }.toSet()
        val entradas = materias.map { Entrada(it, pendientes[it.id] ?: 0) }
        val carpetas = grupos.map { g ->
            val hijas = ordenar(entradas.filter { it.materia.grupoId == g.id }, orden)
            NodoMenu.Carpeta(g, hijas, hijas.sumOf { it.pendientes })
        }
        val carpetasOrdenadas = when (orden) {
            OrdenMenu.ALFABETICO -> carpetas.sortedWith(compareBy(porNombre) { it.grupo.nombre })
            OrdenMenu.PENDIENTES -> carpetas.sortedWith(compareByDescending<NodoMenu.Carpeta> { it.pendientes }.thenBy(porNombre) { it.grupo.nombre })
            else -> carpetas.sortedWith(compareBy<NodoMenu.Carpeta> { it.grupo.orden }.thenBy(porNombre) { it.grupo.nombre })
        }
        val sueltas = ordenar(entradas.filter { it.materia.grupoId == null || it.materia.grupoId !in idsGrupo }, orden)
        return carpetasOrdenadas + sueltas.map { NodoMenu.Lista(it) }
    }

    /** Ids de las listas de una carpeta. */
    fun listasDe(materias: List<Materia>, grupoId: String): Set<String> =
        materias.filter { it.grupoId == grupoId }.map { it.id }.toSet()
}
