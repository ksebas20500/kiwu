package app.kiwu.android.ui.ajustes

import android.content.res.Resources
import androidx.annotation.StringRes
import app.kiwu.android.R
import app.kiwu.android.data.AjustesUi
import app.kiwu.android.ui.Acentos

/** Las secciones de Ajustes. [id] es lo que recuerda la pantalla al girar o volver. */
enum class SeccionAjustes(val id: String, @StringRes val titulo: Int) {
    APARIENCIA("apariencia", R.string.ajustes_apariencia),
    MENU("menu", R.string.ajustes_menu),
    AVISOS("avisos", R.string.ajustes_avisos),
    CUENTA("cuenta", R.string.ajustes_cuenta),
    COPIA("copia", R.string.ajustes_copia),
    ANUNCIOS("anuncios", R.string.ajustes_anuncios),
    ACERCA("acerca", R.string.ajustes_acerca);

    companion object {
        fun deId(id: String?): SeccionAjustes? = entries.firstOrNull { it.id == id }
    }
}

/** La línea que va bajo cada sección de la pantalla principal de Ajustes: el valor que tiene ahora, en el idioma de [r]. */
class ResumenAjustes(private val r: Resources) {
    fun tema(id: String): String = r.getString(
        when (id) {
            "claro" -> R.string.tema_claro
            "oscuro" -> R.string.tema_oscuro
            else -> R.string.resumen_tema_sistema
        },
    )

    fun acento(id: String): String = when {
        id == Acentos.SISTEMA -> r.getString(R.string.resumen_color_sistema)
        else -> Acentos.predefinidos.firstOrNull { it.id == id }?.let { r.getString(it.titulo).lowercase(r.configuration.locales[0]) }
            ?: r.getString(R.string.resumen_color_propio)
    }

    fun aspecto(id: String): String = r.getString(
        when (id) {
            "compacto" -> R.string.aspecto_compacto
            "color" -> R.string.aspecto_color
            else -> R.string.aspecto_plegable
        },
    )

    fun orden(id: String): String = r.getString(
        when (id) {
            "alfabetico" -> R.string.resumen_orden_alfabetico
            "pendientes" -> R.string.resumen_orden_pendientes
            else -> R.string.resumen_orden_manual
        },
    )

    fun minutos(m: Int): String = if (m % 60 == 0) r.getString(R.string.duracion_horas, m / 60) else r.getString(R.string.duracion_min, m)

    fun apariencia(a: AjustesUi): String =
        listOfNotNull(
            tema(a.tema), r.getString(R.string.resumen_color, acento(a.acento)), if (a.fondoUri != null) r.getString(R.string.resumen_con_fondo) else null,
        ).joinToString(" · ")

    fun menu(a: AjustesUi): String =
        listOfNotNull(
            aspecto(a.menuAspecto), orden(a.menuOrden),
            if (!a.menuContadores) r.getString(R.string.resumen_sin_contadores) else null,
            if (!a.rachaVisible) r.getString(R.string.resumen_sin_racha) else null,
        ).joinToString(" · ")

    fun avisos(a: AjustesUi, notificacionesActivas: Boolean): String = when {
        !notificacionesActivas -> r.getString(R.string.resumen_notificaciones_off)
        else -> listOfNotNull(
            r.getString(R.string.resumen_activados),
            if (a.segundoAvisoMin > 0) r.getString(R.string.resumen_segundo_aviso, minutos(a.segundoAvisoMin)) else null,
            if (!a.rachaAviso) r.getString(R.string.resumen_sin_aviso_racha) else null,
        ).joinToString(" · ")
    }

    fun anuncios(sinAnuncios: Boolean): String = r.getString(if (sinAnuncios) R.string.resumen_sin_anuncios else R.string.resumen_con_anuncios)

    fun copia(): String = r.getString(R.string.resumen_copia)

    fun acerca(version: String): String = r.getString(R.string.version_n, version)
}
