package app.kiwu.android.domain

/** La guía de inicio: cuándo se muestra y qué trae la lista de ejemplo que se crea al terminarla. */
object Guia {
    /** Id fijo de la lista de ejemplo: así no se crea dos veces aunque se repita la guía. */
    const val ID_LISTA = "empieza-aqui"
    const val NOMBRE_LISTA = "Empieza aquí"
    const val EMOJI_LISTA = "👋"

    /** Tareas que enseñan a usar la app haciendo (en este orden). */
    val tareas = listOf(
        "Toca la casilla para completarme",
        "Ábreme para ponerme una fecha, un aviso o una nota",
        "Abre el menú ☰ y crea tu primera lista con ＋",
        "En el menú, mantén pulsada una lista para llevarla a una carpeta",
        "Añade tu primera clase en Horario",
    )

    /** Solo la primera vez y con la app vacía: quien actualiza con datos ya sabe usarla. */
    fun debeMostrarse(guiaVista: Boolean, hayDatos: Boolean): Boolean = !guiaVista && !hayDatos
}
