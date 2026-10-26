package app.kiwu.android.domain

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Marca de un cambio: hora, contador y dispositivo. Se compara en ese orden, así dos cambios nunca empatan. */
data class Marca(val ms: Long, val contador: Int, val dispositivo: String) : Comparable<Marca> {
    override fun compareTo(other: Marca): Int = compareValuesBy(this, other, Marca::ms, Marca::contador, Marca::dispositivo)

    override fun toString(): String = "$ms-$contador-$dispositivo"

    companion object {
        /** Lee lo que escribió [toString]; `null` si el texto no es una marca. El dispositivo puede llevar guiones (un UUID). */
        fun deTexto(texto: String): Marca? {
            val partes = texto.split("-", limit = 3)
            if (partes.size != 3 || partes[2].isEmpty()) return null
            return Marca(partes[0].toLongOrNull() ?: return null, partes[1].toIntOrNull() ?: return null, partes[2])
        }
    }
}

/**
 * Reloj híbrido de un dispositivo: la hora real mientras avanza, y un contador cuando dos cambios caen en el mismo
 * milisegundo o el reloj retrocede. Una marca nueva siempre es mayor que cualquiera anterior, propia o recibida.
 */
class RelojHibrido(private val dispositivo: String, private var ultima: Marca = Marca(0, 0, dispositivo)) {
    fun nueva(ahora: Long): Marca {
        val ms = maxOf(ahora, ultima.ms)
        ultima = Marca(ms, if (ms == ultima.ms) ultima.contador + 1 else 0, dispositivo)
        return ultima
    }

    /** Se llama con cada marca recibida del servidor. Una más de un día adelantada (reloj mal puesto) se ignora. */
    fun ver(remota: Marca, ahora: Long) {
        if (remota.ms > ahora + MAX_ADELANTO_MS) return
        if (remota.ms > ultima.ms || (remota.ms == ultima.ms && remota.contador > ultima.contador)) {
            ultima = Marca(remota.ms, remota.contador, dispositivo)
        }
    }

    companion object {
        const val MAX_ADELANTO_MS = 24L * 60 * 60 * 1000
    }
}

/** El valor de un campo y cuándo se escribió. */
data class Campo(val valor: JsonElement, val marca: Marca)

object FusionSync {
    /**
     * Último escritor gana **por campo**: cambiar el título en un dispositivo y la fecha en otro conserva los dos. El borrado
     * es un campo más («eliminada»), así que gana la operación más reciente. Es conmutativa e idempotente: el orden en que
     * lleguen los lotes y reenviarlos no cambia el resultado.
     */
    fun fusionar(a: Map<String, Campo>, b: Map<String, Campo>): Map<String, Campo> {
        val resultado = a.toMutableMap()
        for ((nombre, campo) in b) {
            val actual = resultado[nombre]
            if (actual == null || campo.marca > actual.marca) resultado[nombre] = campo
        }
        return resultado
    }
}

/**
 * La nota tal como viaja a la nube: **sin imágenes ni archivos**. Se quitan los bloques de imagen, las vistas previas
 * de enlace en base64 y las rutas del dispositivo; de un archivo queda solo su nombre.
 */
fun Documento.paraSincronizar(): Documento {
    val limpios = bloques.filter { it.kind != TiposBloque.IMAGEN }.map {
        it.copy(
            imageData = null, bookmark = null, path = null,
            preview = it.preview?.let { p -> JsonObject(p.filterKeys { clave -> clave != "imagen" }) },
        )
    }
    return Documento(limpios.ifEmpty { listOf(Bloque()) })
}

/**
 * Lo contrario de [paraSincronizar] al recibir una nota: el texto viene de la nube y las imágenes se conservan de la copia
 * de este dispositivo. Cada imagen vuelve a su sitio, detrás del bloque que la precedía (o al final si ese ya no existe).
 */
fun Documento.conImagenesDe(local: Documento): Documento {
    val resultado = bloques.toMutableList()
    local.bloques.forEachIndexed { i, b ->
        if (b.kind != TiposBloque.IMAGEN || resultado.any { it.id == b.id }) return@forEachIndexed
        val previo = local.bloques.getOrNull(i - 1)?.id
        val donde = resultado.indexOfFirst { it.id == previo }
        resultado.add(if (donde >= 0) donde + 1 else resultado.size, b)
    }
    return Documento(resultado)
}
