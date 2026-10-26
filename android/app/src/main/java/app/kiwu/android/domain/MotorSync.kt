package app.kiwu.android.domain

/** Una fila lista para viajar: de qué tabla es, su UUID y sus campos con la marca de cada uno (el borrado es el campo «eliminada»). */
data class FilaSync(val tabla: String, val id: String, val campos: Map<String, Campo>)

/** Un lote descargado y el cursor desde el que pedir el siguiente. */
data class LoteSync(val filas: List<FilaSync>, val cursor: String?)

/** El servidor rechazó la petición por la cuota diaria gratuita: no es un fallo, se reintenta mañana. */
class CuotaAgotada : Exception("Límite diario del servidor")

/** El servidor remoto (Firestore). Debe ser idempotente: subir dos veces la misma fila no cambia nada. */
interface Nube {
    suspend fun subir(filas: List<FilaSync>)
    suspend fun bajar(desde: String?, limite: Int): LoteSync
}

/** La base local. Aplicar lo recibido **no** debe volver a ponerlo en la cola de salida (sería un bucle). */
interface AlmacenSync {
    var cursor: String?
    suspend fun pendientes(limite: Int): List<FilaSync>
    /** Quita de la cola lo enviado, salvo lo que cambió mientras tanto. */
    suspend fun marcarSubidas(enviadas: List<FilaSync>)
    suspend fun aplicar(filas: List<FilaSync>)
}

data class ResultadoSync(val subidas: Int, val bajadas: Int, val limiteDiario: Boolean = false)

/**
 * Un ciclo de sincronización: primero baja lo nuevo y después sube lo pendiente. Bajar primero evita que un dispositivo que estuvo sin conexión pise con
 * una edición vieja lo que otro escribió después: lo que gana el servidor se aplica antes de decidir qué subir. Trabaja en
 * lotes para no gastar de más la cuota gratuita ni bloquear nada; la interfaz solo lee la base local.
 */
class MotorSync(
    private val nube: Nube,
    private val almacen: AlmacenSync,
    private val reloj: RelojHibrido,
    private val ahora: () -> Long = System::currentTimeMillis,
    private val lote: Int = 200,
) {
    suspend fun sincronizar(): ResultadoSync {
        var subidas = 0
        var bajadas = 0
        try {
            while (true) {
                val recibido = nube.bajar(almacen.cursor, lote)
                if (recibido.filas.isEmpty()) break
                recibido.filas.forEach { f -> f.campos.values.forEach { reloj.ver(it.marca, ahora()) } }
                almacen.aplicar(recibido.filas)
                // El cursor solo avanza después de aplicar: si algo falla a medias, se vuelve a pedir el mismo lote.
                almacen.cursor = recibido.cursor ?: almacen.cursor
                bajadas += recibido.filas.size
                if (recibido.filas.size < lote) break // un lote corto es el último: ahorra una consulta vacía
            }
            while (true) {
                val pendientes = almacen.pendientes(lote)
                if (pendientes.isEmpty()) break
                nube.subir(pendientes)
                almacen.marcarSubidas(pendientes)
                subidas += pendientes.size
            }
        } catch (e: CuotaAgotada) {
            return ResultadoSync(subidas, bajadas, limiteDiario = true)
        }
        return ResultadoSync(subidas, bajadas)
    }
}
