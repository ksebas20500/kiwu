package app.kiwu.android.domain

import app.kiwu.android.R

/** La guía de inicio: cuándo se muestra y qué trae la lista de ejemplo que se crea al terminarla. */
object Guia {
    /** Id fijo de la lista de ejemplo: así no se crea dos veces aunque se repita la guía. */
    const val ID_LISTA = "empieza-aqui"
    const val EMOJI_LISTA = "👋"

    /** Nombre de la lista y tareas que enseñan a usar la app haciendo (en este orden), como recursos de texto. */
    val NOMBRE_LISTA = R.string.guia_lista
    val tareas = listOf(R.string.guia_tarea_1, R.string.guia_tarea_2, R.string.guia_tarea_3, R.string.guia_tarea_4, R.string.guia_tarea_5)

    /** Solo la primera vez y con la app vacía: quien actualiza con datos ya sabe usarla. */
    fun debeMostrarse(guiaVista: Boolean, hayDatos: Boolean): Boolean = !guiaVista && !hayDatos
}
