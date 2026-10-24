package app.kiwu.android.ui.ajustes

import app.kiwu.android.data.AjustesUi
import app.kiwu.android.ui.Acentos

/** Las secciones de Ajustes. [id] es lo que recuerda la pantalla al girar o volver. */
enum class SeccionAjustes(val id: String, val titulo: String) {
    APARIENCIA("apariencia", "Apariencia"),
    MENU("menu", "Menú de Tareas"),
    AVISOS("avisos", "Avisos"),
    COPIA("copia", "Copia de seguridad"),
    ANUNCIOS("anuncios", "Quitar anuncios"),
    ACERCA("acerca", "Acerca de Kiwu");

    companion object {
        fun deId(id: String?): SeccionAjustes? = entries.firstOrNull { it.id == id }
    }
}

/** La línea que va bajo cada sección de la pantalla principal de Ajustes: el valor que tiene ahora. Funciones puras. */
object ResumenAjustes {
    fun tema(id: String): String = when (id) {
        "claro" -> "Claro"
        "oscuro" -> "Oscuro"
        else -> "Del sistema"
    }

    fun acento(id: String): String = when {
        id == Acentos.SISTEMA -> "del sistema"
        else -> Acentos.predefinidos.firstOrNull { it.id == id }?.titulo?.lowercase() ?: "propio"
    }

    fun aspecto(id: String): String = when (id) {
        "compacto" -> "Compacto"
        "color" -> "Con color"
        else -> "Plegable"
    }

    fun orden(id: String): String = when (id) {
        "alfabetico" -> "orden alfabético"
        "pendientes" -> "por tareas pendientes"
        else -> "orden manual"
    }

    fun minutos(m: Int): String = if (m % 60 == 0) "${m / 60} h" else "$m min"

    fun apariencia(a: AjustesUi): String =
        listOfNotNull(tema(a.tema), "color " + acento(a.acento), if (a.fondoUri != null) "con fondo" else null).joinToString(" · ")

    fun menu(a: AjustesUi): String =
        listOfNotNull(
            aspecto(a.menuAspecto), orden(a.menuOrden), if (!a.menuContadores) "sin contadores" else null, if (!a.rachaVisible) "sin racha" else null,
        ).joinToString(" · ")

    fun avisos(a: AjustesUi, notificacionesActivas: Boolean): String = when {
        !notificacionesActivas -> "Notificaciones desactivadas"
        else -> listOfNotNull(
            "Activados",
            if (a.segundoAvisoMin > 0) "segundo aviso ${minutos(a.segundoAvisoMin)} antes" else null,
            if (!a.rachaAviso) "sin aviso de racha" else null,
        ).joinToString(" · ")
    }

    fun anuncios(sinAnuncios: Boolean): String = if (sinAnuncios) "Gracias por apoyar Kiwu: sin anuncios" else "Pago único · quita el banner"

    fun copia(): String = "Exportar e importar (.kiwu.json)"

    fun acerca(version: String): String = "Versión $version"
}
