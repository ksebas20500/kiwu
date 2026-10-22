package app.kiwu.android.data

import app.kiwu.android.domain.Clase
import app.kiwu.android.domain.Cuaderno
import app.kiwu.android.domain.Deber
import app.kiwu.android.domain.Documento
import app.kiwu.android.domain.Horario
import app.kiwu.android.domain.Materia
import app.kiwu.android.domain.Pagina
import app.kiwu.android.domain.ReglasHorario
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

class ErrorCopia(mensaje: String) : Exception(mensaje)

data class ResumenImportacion(
    val materias: Int, val deberes: Int, val cuadernos: Int, val paginas: Int,
    val horarios: Int = 0, val clases: Int = 0,
)

/**
 * Copia de seguridad `.kiwu.json`: el mismo formato que exporta e importa Kiwu para Linux, así que sirve para mover
 * los datos entre el teléfono y el escritorio. Al importar no se pisa lo existente ni resucita lo eliminado.
 * Las carpetas vinculadas no se exportan: solo valen en el dispositivo donde se eligieron.
 */
object Copia {
    const val FORMATO = 1
    private val json = Json { prettyPrint = true }

    private fun iso(ms: Long?): JsonElement =
        if (ms == null) JsonNull else JsonPrimitive(Instant.ofEpochMilli(ms).truncatedTo(ChronoUnit.SECONDS).toString())

    private fun desdeIso(valor: JsonElement?): Long? {
        val texto = (valor as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content?.takeIf { it.isNotEmpty() } ?: return null
        return try {
            Instant.parse(texto).toEpochMilli()
        } catch (e: Exception) {
            try { OffsetDateTime.parse(texto).toInstant().toEpochMilli() } catch (e2: Exception) { null }
        }
    }

    private fun JsonObject.cadena(k: String): String? = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
    private fun JsonObject.entero(k: String, defecto: Int): Int = (this[k] as? JsonPrimitive)?.intOrNull ?: defecto
    private fun JsonObject.logico(k: String): Boolean = (this[k] as? JsonPrimitive)?.booleanOrNull ?: false

    private fun contenido(valor: JsonElement?): String = when {
        valor is JsonPrimitive && valor.isString -> Documento.deJson(valor.content).aJson()
        valor is JsonObject -> Documento.deJson(valor.toString()).aJson()
        else -> Documento.vacio().aJson()
    }

    private fun comoJson(contenido: String): JsonElement = Json.parseToJsonElement(Documento.deJson(contenido).aJson())

    suspend fun exportar(dao: KiwuDao, ahora: Long = System.currentTimeMillis()): String {
        val materias = dao.materias().first()
        val deberes = dao.deberesAhora()
        val cuadernos = dao.cuadernos().first()
        val paginas = dao.paginasAhora()
        val horarios = dao.horarios().first()
        val clases = dao.clasesAhora()
        val raiz = buildJsonObject {
            put("formato", FORMATO)
            put("app", "kiwu")
            put("exportado", iso(ahora))
            put("materias", buildJsonArray {
                for (m in materias) add(buildJsonObject {
                    put("id", m.id); put("nombre", m.nombre); put("orden", m.orden); put("carpeta", JsonNull)
                    put("creada", iso(m.creada)); put("actualizada", iso(m.actualizada))
                })
            })
            put("deberes", buildJsonArray {
                for (d in deberes) add(buildJsonObject {
                    put("id", d.id); put("materia", d.materiaId); put("titulo", d.titulo)
                    put("completado", d.completado); put("estado", d.estado)
                    put("fechaEntrega", iso(d.fechaEntrega)); put("recordatorioHoras", d.recordatorioHoras)
                    put("fechaCompletado", iso(d.fechaCompletado)); put("nota", comoJson(d.nota))
                    put("creada", iso(d.creada)); put("actualizada", iso(d.actualizada))
                })
            })
            put("cuadernos", buildJsonArray {
                for (c in cuadernos) add(buildJsonObject {
                    put("id", c.id); put("nombre", c.nombre); put("icono", c.icono); put("orden", c.orden)
                    put("creada", iso(c.creada)); put("actualizada", iso(c.actualizada))
                })
            })
            put("paginas", buildJsonArray {
                for (p in paginas) add(buildJsonObject {
                    put("id", p.id); put("cuaderno", p.cuadernoId); put("titulo", p.titulo); put("icono", p.icono)
                    put("favorita", p.favorita); put("contenido", comoJson(p.contenido))
                    put("creada", iso(p.creada)); put("actualizada", iso(p.actualizada))
                })
            })
            put("horarios", buildJsonArray {
                for (h in horarios) add(buildJsonObject {
                    put("id", h.id); put("nombre", h.nombre); put("icono", h.icono); put("orden", h.orden)
                    put("creada", iso(h.creada)); put("actualizada", iso(h.actualizada))
                })
            })
            put("clases", buildJsonArray {
                for (c in clases) add(buildJsonObject {
                    put("id", c.id); put("horario", c.horarioId); put("materia", c.materiaId?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("color", c.color?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("grupo", c.grupo); put("titulo", c.titulo); put("diaSemana", c.diaSemana)
                    put("inicioMin", c.inicioMin); put("finMin", c.finMin); put("aula", c.aula); put("notas", c.notas)
                    put("avisoMin", c.avisoMin?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("creada", iso(c.creada)); put("actualizada", iso(c.actualizada))
                })
            })
        }
        return json.encodeToString(JsonElement.serializer(), raiz)
    }

    suspend fun importar(dao: KiwuDao, texto: String): ResumenImportacion {
        val datos = try {
            Json.parseToJsonElement(texto) as? JsonObject
        } catch (e: Exception) {
            throw ErrorCopia("No se pudo leer la copia: ${e.message}")
        }
        if (datos == null || datos.cadena("app") != "kiwu") throw ErrorCopia("El archivo no es una copia de seguridad de Kiwu")
        if (datos.entero("formato", 0) > FORMATO) throw ErrorCopia("La copia es de una versión más nueva de Kiwu")

        fun lista(clave: String): List<JsonObject> = (datos[clave] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

        val materias = lista("materias").mapNotNull { m ->
            val id = m.cadena("id") ?: return@mapNotNull null
            val nombre = m.cadena("nombre") ?: return@mapNotNull null
            Materia(
                id = id, nombre = nombre, orden = m.entero("orden", 0),
                creada = desdeIso(m["creada"]) ?: 0L, actualizada = desdeIso(m["actualizada"]) ?: 0L,
            )
        }
        val idsMateria = materias.map { it.id }.toSet() + dao.idsMaterias()
        val deberes = lista("deberes").mapNotNull { d ->
            val id = d.cadena("id") ?: return@mapNotNull null
            val materia = d.cadena("materia")?.takeIf { it in idsMateria } ?: return@mapNotNull null
            Deber(
                id = id, materiaId = materia, titulo = d.cadena("titulo") ?: return@mapNotNull null,
                completado = d.logico("completado"), estado = d.entero("estado", 0),
                fechaEntrega = desdeIso(d["fechaEntrega"]), recordatorioHoras = d.entero("recordatorioHoras", 24),
                fechaCompletado = desdeIso(d["fechaCompletado"]), nota = contenido(d["nota"]),
                creada = desdeIso(d["creada"]) ?: 0L, actualizada = desdeIso(d["actualizada"]) ?: 0L,
            )
        }
        val cuadernos = lista("cuadernos").mapNotNull { c ->
            Cuaderno(
                id = c.cadena("id") ?: return@mapNotNull null, nombre = c.cadena("nombre") ?: return@mapNotNull null,
                icono = c.cadena("icono") ?: "📓", orden = c.entero("orden", 0),
                creada = desdeIso(c["creada"]) ?: 0L, actualizada = desdeIso(c["actualizada"]) ?: 0L,
            )
        }
        val idsCuaderno = cuadernos.map { it.id }.toSet() + dao.idsCuadernos()
        val paginas = lista("paginas").mapNotNull { p ->
            val cuaderno = p.cadena("cuaderno")?.takeIf { it in idsCuaderno } ?: return@mapNotNull null
            val doc = Documento.deJson(contenido(p["contenido"]))
            Pagina(
                id = p.cadena("id") ?: return@mapNotNull null, cuadernoId = cuaderno,
                titulo = p.cadena("titulo") ?: "", icono = p.cadena("icono") ?: "",
                contenido = doc.aJson(), textoPlano = doc.textoPlano(), favorita = p.logico("favorita"),
                creada = desdeIso(p["creada"]) ?: 0L, actualizada = desdeIso(p["actualizada"]) ?: 0L,
            )
        }

        val horarios = lista("horarios").mapNotNull { h ->
            Horario(
                id = h.cadena("id") ?: return@mapNotNull null, nombre = h.cadena("nombre") ?: return@mapNotNull null,
                icono = h.cadena("icono") ?: "🎓", orden = h.entero("orden", 0),
                creada = desdeIso(h["creada"]) ?: 0L, actualizada = desdeIso(h["actualizada"]) ?: 0L,
            )
        }
        val idsHorario = horarios.map { it.id }.toSet() + dao.idsHorarios()
        val clases = lista("clases").mapNotNull { c ->
            val horario = c.cadena("horario")?.takeIf { it in idsHorario } ?: return@mapNotNull null
            val dia = c.entero("diaSemana", 0)
            val inicio = c.entero("inicioMin", -1)
            val fin = c.entero("finMin", -1)
            if (dia !in 1..7 || !ReglasHorario.valida(inicio, fin)) return@mapNotNull null
            val id = c.cadena("id") ?: return@mapNotNull null
            Clase(
                id = id, horarioId = horario, materiaId = c.cadena("materia"), grupo = c.cadena("grupo") ?: id,
                titulo = c.cadena("titulo") ?: return@mapNotNull null, diaSemana = dia, inicioMin = inicio, finMin = fin,
                aula = c.cadena("aula") ?: "", notas = c.cadena("notas") ?: "",
                avisoMin = (c["avisoMin"] as? JsonPrimitive)?.intOrNull,
                // Campo opcional (versión 0.3): las copias antiguas no lo traen y se quedan con el color automático.
                color = ReglasHorario.colorValido(c.cadena("color")),
                creada = desdeIso(c["creada"]) ?: 0L, actualizada = desdeIso(c["actualizada"]) ?: 0L,
            )
        }

        // Room devuelve -1 por cada fila ignorada (ya existía, viva o eliminada)
        return ResumenImportacion(
            materias = dao.insertarMaterias(materias).count { it != -1L },
            deberes = dao.insertarDeberes(deberes).count { it != -1L },
            cuadernos = dao.insertarCuadernos(cuadernos).count { it != -1L },
            paginas = dao.insertarPaginas(paginas).count { it != -1L },
            horarios = dao.insertarHorarios(horarios).count { it != -1L },
            clases = dao.insertarClases(clases).count { it != -1L },
        )
    }
}
