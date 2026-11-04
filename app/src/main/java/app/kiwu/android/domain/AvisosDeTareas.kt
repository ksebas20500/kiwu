package app.kiwu.android.domain

/**
 * Lo que hay que hacer con la barra de notificaciones cuando cambia una tarea. La app lo implementa en `notify/`
 * (aviso visible + alarmas); en las pruebas no hace nada.
 */
interface AvisosDeTareas {
    /** La tarea ya no necesita aviso (completada, borrada o sin fecha): fuera de la barra y sin alarma. */
    fun olvidar(id: String)

    /** Vuelve a cuadrar alarmas y avisos con la base, p. ej. tras borrar una lista con todas sus tareas. */
    fun revisar() = Unit

    /** Para las pruebas y para quien no tenga notificaciones. */
    object Nada : AvisosDeTareas {
        override fun olvidar(id: String) = Unit
    }
}
