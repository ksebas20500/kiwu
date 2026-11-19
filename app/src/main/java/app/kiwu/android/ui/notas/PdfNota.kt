package app.kiwu.android.ui.notas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.graphics.toArgb
import app.kiwu.android.domain.Bloque
import app.kiwu.android.domain.Documento
import app.kiwu.android.domain.TextRun
import app.kiwu.android.domain.TiposBloque
import app.kiwu.android.ui.ColoresNota
import java.io.File
import java.util.Base64

/**
 * Exporta una nota a PDF respetando la estructura de sus bloques: títulos, listas, citas, código, imágenes y
 * tarjetas. Se dibuja con `PdfDocument` (sin librerías nuevas) y el texto queda seleccionable.
 *
 * Las hojas son A4 y el contenido se pagina solo; cada hoja lleva al pie el nombre de Kiwu, el título de la nota,
 * el cuaderno y el número de página.
 */
object PdfNota {
    private const val ANCHO = 595 // A4 en puntos (72 ppp)
    private const val ALTO = 842
    private const val MARGEN = 48f
    private const val MARGEN_INFERIOR = 62f
    private const val SANGRIA = 16f
    private const val CUERPO = 11f
    private const val INTERLINEADO = 1.25f

    /** Metadatos que van en las hojas: el título de la nota y, si se conoce, el cuaderno. */
    data class Datos(val titulo: String, val cuaderno: String? = null)

    /** Genera el PDF en la caché de la app y devuelve el archivo listo para compartir. */
    fun generar(contexto: Context, documento: Documento, datos: Datos): File {
        val carpeta = File(contexto.cacheDir, "exportaciones").apply { mkdirs() }
        val archivo = File(carpeta, nombreArchivo(datos.titulo))
        // Los PDF de días anteriores ya no hacen falta; la caché no debe crecer sin fin.
        val viejos = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        carpeta.listFiles()?.filter { it != archivo && it.lastModified() < viejos }?.forEach { it.delete() }
        val pdf = PdfDocument()
        try {
            Render(pdf, datos).aplicar(documento)
            // El documento solo escribe su contenido al cerrarse (`close`), por eso se vuelca antes de la última
            // llamada a `close` (que aquí está en el `finally`).
            archivo.outputStream().use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
        return archivo
    }

    /** Nombre de archivo seguro y reconocible a partir del título. */
    fun nombreArchivo(titulo: String): String {
        val base = titulo.trim().lowercase(java.util.Locale.ROOT)
            .replace(Regex("[áàä]"), "a").replace(Regex("[éèë]"), "e").replace(Regex("[íìï]"), "i")
            .replace(Regex("[óòö]"), "o").replace(Regex("[úùü]"), "u").replace("ñ", "n")
            .replace(Regex("[^a-z0-9]+"), "-").trim('-').take(48).ifEmpty { "nota" }
        return "$base.pdf"
    }

    /**
     * Número que le toca a cada bloque (0 si no es de lista numerada). La cuenta sigue entre elementos seguidos
     * aunque cambie la sangría y vuelve a empezar al aparecer otro tipo de bloque.
     */
    fun numeros(bloques: List<Bloque>): List<Int> {
        var n = 0
        var anterior = ""
        return bloques.map { b ->
            val numero = if (b.kind == TiposBloque.NUMERADA) {
                n++
                n
            } else {
                if (anterior == TiposBloque.NUMERADA) n = 0
                0
            }
            anterior = b.kind
            numero
        }
    }

    private class Render(private val pdf: PdfDocument, private val datos: Datos) {
        private var pagina: PdfDocument.Page? = null
        private var lienzo: Canvas? = null
        private var y = 0f
        private var numero = 0
        private var numerada = 0
        private val gris = Color.rgb(120, 120, 120)
        private val negro = Color.rgb(32, 32, 32)
        private val limite get() = ALTO - MARGEN_INFERIOR

        fun aplicar(documento: Documento) {
            nuevaPagina()
            val numeros = numeros(documento.bloques)
            for ((i, b) in documento.bloques.withIndex()) {
                numerada = numeros[i]
                dibujar(b)
            }
            cerrarPagina()
        }

        private fun cerrarPagina() {
            pagina?.let { pdf.finishPage(it) }
            pagina = null
            lienzo = null
        }

        private fun nuevaPagina() {
            cerrarPagina()
            val info = PdfDocument.PageInfo.Builder(ANCHO, ALTO, numero + 1).create()
            val p = pdf.startPage(info)
            pagina = p
            lienzo = p.canvas
            numero++
            y = MARGEN
            val pie = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = gris }
            val izq = listOfNotNull("Kiwu", datos.titulo.take(48).ifBlank { null }, datos.cuaderno)
                .joinToString(" · ")
            p.canvas.drawText(izq, MARGEN, ALTO - 26f, pie)
            val num = "$numero"
            p.canvas.drawText(num, ANCHO - MARGEN - pie.measureText(num), ALTO - 26f, pie)
            p.canvas.drawLine(
                MARGEN, ALTO - 36f, ANCHO - MARGEN, ALTO - 36f,
                Paint().apply { color = Color.rgb(226, 226, 226); strokeWidth = 1f },
            )
            if (numero == 1) portada()
        }

        /** La primera hoja empieza con el título de la nota, para que el PDF se reconozca al abrirlo. */
        private fun portada() {
            val titulo = datos.titulo.trim().ifEmpty { "—" }
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 22f
                color = negro
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val layout = StaticLayout.Builder.obtain(titulo, 0, titulo.length, paint, (ANCHO - 2 * MARGEN).toInt())
                .setIncludePad(false).setLineSpacing(0f, 1.1f).build()
            guardar { c ->
                c.translate(MARGEN, y)
                layout.draw(c)
            }
            y += layout.height + 14f
            lienzo!!.drawLine(
                MARGEN, y, ANCHO - MARGEN, y,
                Paint().apply { color = Color.rgb(210, 210, 210); strokeWidth = 1f },
            )
            y += 22f
        }

        private inline fun guardar(bloque: (Canvas) -> Unit) {
            val c = lienzo ?: return
            val guardado = c.save()
            try {
                bloque(c)
            } finally {
                c.restoreToCount(guardado)
            }
        }

        private fun dibujar(b: Bloque) {
            when (b.kind) {
                TiposBloque.TITULO -> texto(b, tam = 19f, negrita = true, antes = 14f, despues = 6f)
                TiposBloque.SUBTITULO -> texto(b, tam = 14.5f, negrita = true, antes = 10f, despues = 4f)
                TiposBloque.VINETAS -> texto(
                    b, prefijo = "•  ", sangria = SANGRIA + b.indent * SANGRIA, sangriaColgante = 12f,
                )
                TiposBloque.NUMERADA -> texto(
                    b, prefijo = "$numerada.  ", sangria = SANGRIA + b.indent * SANGRIA, sangriaColgante = 16f,
                )
                TiposBloque.TAREAS -> texto(
                    b, prefijo = if (b.isChecked) "☑  " else "☐  ", sangria = SANGRIA + b.indent * SANGRIA,
                    sangriaColgante = 14f, tachar = b.isChecked, colorTexto = if (b.isChecked) gris else null,
                )
                TiposBloque.CITA -> texto(
                    b, cursiva = true, sangria = 14f + b.indent * SANGRIA, colorTexto = Color.rgb(90, 90, 90),
                    barra = true,
                )
                TiposBloque.DESTACADO -> texto(
                    b, sangria = 10f + b.indent * SANGRIA, despues = 10f, fondo = Color.rgb(240, 244, 252),
                )
                TiposBloque.CODIGO -> texto(
                    b, monoespaciado = true, tam = 9.5f, sangria = 8f + b.indent * SANGRIA, antes = 6f,
                    despues = 8f, fondo = Color.rgb(245, 245, 245),
                )
                TiposBloque.DIVISOR -> {
                    asegurar(14f)
                    y += 6f
                    lienzo!!.drawLine(
                        MARGEN, y, ANCHO - MARGEN, y,
                        Paint().apply { color = Color.rgb(215, 215, 215) },
                    )
                    y += 10f
                }
                TiposBloque.IMAGEN -> imagen(b)
                TiposBloque.ARCHIVO -> texto(
                    b.copy(text = "📎  ${b.fileName ?: b.text}".trim()), colorTexto = Color.rgb(70, 70, 70),
                )
                TiposBloque.ENLACE -> texto(
                    b.copy(text = b.textoPlano()), colorTexto = Color.rgb(30, 90, 170),
                )
                TiposBloque.TARJETA -> {
                    texto(b.copy(text = "Pregunta: ${b.text}"), negrita = true)
                    if (!b.answer.isNullOrBlank()) {
                        texto(
                            b.copy(id = b.id + "-r", text = "Respuesta: ${b.answer}", runs = null),
                            colorTexto = Color.rgb(60, 80, 60), sangria = 10f,
                        )
                    }
                }
                else -> texto(b)
            }
        }

        private fun texto(
            b: Bloque,
            prefijo: String? = null,
            tam: Float = CUERPO,
            negrita: Boolean = false,
            cursiva: Boolean = false,
            monoespaciado: Boolean = false,
            sangria: Float = b.indent * SANGRIA,
            sangriaColgante: Float = 0f,
            antes: Float = 4f,
            despues: Float = 8f,
            fondo: Int? = null,
            barra: Boolean = false,
            tachar: Boolean = false,
            colorTexto: Int? = null,
        ) {
            val contenido = spannable(b, tam, negrita, cursiva, monoespaciado, colorTexto, tachar)
            if (contenido.isEmpty()) return
            val texto = SpannableStringBuilder().apply {
                prefijo?.let { append(it) }
                append(contenido)
            }
            if (sangriaColgante > 0f) {
                texto.setSpan(
                    LeadingMarginSpan.Standard(0, sangriaColgante.toInt()), prefijo?.length ?: 0, texto.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = tam
                color = negro
                typeface = when {
                    monoespaciado -> Typeface.MONOSPACE
                    negrita -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    cursiva -> Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                    else -> Typeface.SANS_SERIF
                }
            }
            val ancho = (ANCHO - 2 * MARGEN - sangria - 8f).toInt().coerceAtLeast(80)
            val layout = StaticLayout.Builder.obtain(texto, 0, texto.length, paint, ancho)
                .setIncludePad(false).setLineSpacing(0f, INTERLINEADO).build()
            asegurar(antes + layout.getLineBottom(0) - layout.getLineTop(0))
            y += antes
            var linea = 0
            while (linea < layout.lineCount) {
                val disponible = limite - y
                var fin = linea
                while (fin < layout.lineCount && layout.getLineBottom(fin) - layout.getLineTop(linea) <= disponible) {
                    fin++
                }
                if (fin == linea) {
                    // La hoja está llena: se abre otra y se reintenta. Si aun así no cabe (una línea más alta
                    // que una hoja entera), se dibuja igual y se recorta.
                    if (y > MARGEN + 1f) {
                        nuevaPagina()
                        continue
                    }
                    fin = linea + 1
                }
                val top = layout.getLineTop(linea)
                val alto = (layout.getLineBottom(fin - 1) - top).toFloat()
                if (fondo != null) {
                    lienzo!!.drawRoundRect(
                        MARGEN + sangria - 6f, y - 3f, ANCHO - MARGEN, y + alto + 3f, 5f, 5f,
                        Paint().apply { color = fondo },
                    )
                }
                if (barra) {
                    lienzo!!.drawRect(
                        MARGEN + sangria - 10f, y - 2f, MARGEN + sangria - 7f, y + alto + 2f,
                        Paint().apply { color = Color.rgb(200, 200, 200) },
                    )
                }
                guardar { c ->
                    c.clipRect(MARGEN + sangria, y - 2f, ANCHO - MARGEN, y + alto + 2f)
                    c.translate(MARGEN + sangria, y - top)
                    layout.draw(c)
                }
                y += alto
                linea = fin
                if (linea < layout.lineCount) nuevaPagina()
            }
            y += despues
        }

        private fun spannable(
            b: Bloque,
            tam: Float,
            negrita: Boolean,
            cursiva: Boolean,
            mono: Boolean,
            colorTexto: Int?,
            tachar: Boolean,
        ): CharSequence {
            val runs = b.runs?.takeIf { it.isNotEmpty() } ?: listOf(TextRun(b.text))
            val sb = SpannableStringBuilder()
            for (r in runs) {
                if (r.text.isEmpty()) continue
                val ini = sb.length
                sb.append(r.text)
                val fin = sb.length
                val bold = r.bold || (negrita && !r.code)
                val italic = r.italic || cursiva
                if (bold || italic) {
                    val estilo = when {
                        bold && italic -> Typeface.BOLD_ITALIC
                        bold -> Typeface.BOLD
                        else -> Typeface.ITALIC
                    }
                    sb.setSpan(StyleSpan(estilo), ini, fin, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                if (r.underline) sb.setSpan(UnderlineSpan(), ini, fin, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (r.strike || tachar) sb.setSpan(StrikethroughSpan(), ini, fin, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (r.code || mono) sb.setSpan(TypefaceSpan("monospace"), ini, fin, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                ColoresNota.colores[r.color]?.let {
                    sb.setSpan(ForegroundColorSpan(it.oscuro.toArgb()), ini, fin, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                ColoresNota.colores[r.highlight]?.let {
                    sb.setSpan(BackgroundColorSpan(it.claro.toArgb()), ini, fin, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                if (r.link != null && r.link.isNotBlank()) {
                    sb.setSpan(UnderlineSpan(), ini, fin, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    sb.setSpan(ForegroundColorSpan(Color.rgb(30, 90, 170)), ini, fin, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            colorTexto?.let { c ->
                if (sb.isNotEmpty()) sb.setSpan(ForegroundColorSpan(c), 0, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            return sb
        }

        private fun imagen(b: Bloque) {
            val bitmap = bitmapDe(b) ?: run {
                texto(b.copy(text = "🖼  ${b.fileName ?: "Imagen"}", runs = null), colorTexto = gris)
                return
            }
            val anchoMax = ANCHO - 2 * MARGEN
            val altoMax = limite - MARGEN
            val escala = minOf(anchoMax / bitmap.width, altoMax / bitmap.height, 1f)
            val ancho = bitmap.width * escala
            val alto = bitmap.height * escala
            if (alto > limite - y) nuevaPagina()
            val x = MARGEN + (anchoMax - ancho) / 2f
            lienzo!!.drawBitmap(
                bitmap, null, android.graphics.RectF(x, y, x + ancho, y + alto),
                Paint(Paint.FILTER_BITMAP_FLAG),
            )
            y += alto + 10f
        }

        private fun bitmapDe(b: Bloque): Bitmap? {
            val datos = b.imageData ?: return null
            return runCatching {
                val limpio = datos.substringAfter(',')
                val bytes = Base64.getDecoder().decode(limpio)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }

        /** Si no cabe [alto] en la hoja actual, empieza otra. */
        private fun asegurar(alto: Float) {
            if (y + alto > limite) nuevaPagina()
        }
    }
}
