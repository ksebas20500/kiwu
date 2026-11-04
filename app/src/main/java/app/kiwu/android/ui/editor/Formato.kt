package app.kiwu.android.ui.editor

import app.kiwu.android.domain.TextRun
import app.kiwu.android.domain.runsONada

/**
 * Formato de un carácter. El editor guarda una lista con un `Fmt` por carácter del texto (más simple de mantener al
 * escribir que rangos que hay que desplazar) y la convierte en tramos (`TextRun`) solo al guardar.
 */
data class Fmt(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strike: Boolean = false,
    val code: Boolean = false,
    val color: String? = null,
    val highlight: String? = null,
    val link: String? = null,
) {
    fun alternar(nombre: String): Fmt = when (nombre) {
        "b" -> copy(bold = !bold)
        "i" -> copy(italic = !italic)
        "u" -> copy(underline = !underline)
        "s" -> copy(strike = !strike)
        "code" -> copy(code = !code)
        else -> this
    }

    fun tiene(nombre: String): Boolean = when (nombre) {
        "b" -> bold
        "i" -> italic
        "u" -> underline
        "s" -> strike
        "code" -> code
        else -> false
    }

    fun conValor(nombre: String, activo: Boolean): Fmt = if (tiene(nombre) == activo) this else alternar(nombre)

    companion object {
        val NINGUNO = Fmt()
    }
}

object Formato {
    /** Un `Fmt` por carácter a partir de los tramos guardados. Si no cuadran con el texto, se descarta el formato. */
    fun desdeRuns(texto: String, runs: List<TextRun>?): List<Fmt> {
        if (runs.isNullOrEmpty()) return List(texto.length) { Fmt.NINGUNO }
        if (runs.sumOf { it.text.length } != texto.length) return List(texto.length) { Fmt.NINGUNO }
        val salida = ArrayList<Fmt>(texto.length)
        for (r in runs) {
            val f = Fmt(r.bold, r.italic, r.underline, r.strike, r.code, r.color, r.highlight, r.link)
            repeat(r.text.length) { salida.add(f) }
        }
        return salida
    }

    /** Tramos para guardar (null si el texto no tiene ningún formato). */
    fun aRuns(texto: String, fmt: List<Fmt>): List<TextRun>? {
        if (texto.isEmpty() || fmt.size != texto.length) return null
        val tramos = ArrayList<TextRun>()
        var inicio = 0
        for (i in 1..texto.length) {
            if (i == texto.length || fmt[i] != fmt[inicio]) {
                val f = fmt[inicio]
                tramos.add(TextRun(texto.substring(inicio, i), f.bold, f.italic, f.underline, f.strike, f.code, f.color, f.highlight, f.link))
                inicio = i
            }
        }
        return runsONada(tramos)
    }

    /**
     * Ajusta el formato cuando el texto cambia de [viejo] a [nuevo]. Se busca el trozo que cambió (prefijo y sufijo
     * comunes); lo escrito hereda el formato del carácter anterior, o [pendiente] si la persona activó un formato
     * antes de escribir. Un enlace no se extiende a lo que se escribe a continuación.
     */
    fun alCambiarTexto(viejo: String, fmtViejo: List<Fmt>, nuevo: String, pendiente: Fmt?): List<Fmt> {
        if (viejo == nuevo) return fmtViejo
        var prefijo = 0
        val tope = minOf(viejo.length, nuevo.length)
        while (prefijo < tope && viejo[prefijo] == nuevo[prefijo]) prefijo++
        var sufijo = 0
        while (sufijo < tope - prefijo && viejo[viejo.length - 1 - sufijo] == nuevo[nuevo.length - 1 - sufijo]) sufijo++
        val insertados = nuevo.length - prefijo - sufijo
        val herencia = pendiente ?: if (prefijo > 0) fmtViejo[prefijo - 1].copy(link = null) else Fmt.NINGUNO
        val salida = ArrayList<Fmt>(nuevo.length)
        for (i in 0 until prefijo) salida.add(fmtViejo[i])
        repeat(insertados) { salida.add(herencia) }
        for (i in viejo.length - sufijo until viejo.length) salida.add(fmtViejo[i])
        return salida
    }

    /** Formato de un rango: true si TODOS los caracteres lo cumplen. */
    fun todosCumplen(fmt: List<Fmt>, desde: Int, hasta: Int, condicion: (Fmt) -> Boolean): Boolean {
        if (desde >= hasta) return false
        for (i in desde until hasta) if (!condicion(fmt[i])) return false
        return true
    }

    fun aplicar(fmt: List<Fmt>, desde: Int, hasta: Int, cambio: (Fmt) -> Fmt): List<Fmt> =
        fmt.mapIndexed { i, f -> if (i in desde until hasta) cambio(f) else f }
}
