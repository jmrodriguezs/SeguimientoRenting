package com.manursan.seguimientokm

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlin.math.abs

/**
 * Lectura de tiques de repostaje: reconoce el texto de la foto con ML Kit (en el propio dispositivo,
 * sin enviar la imagen a ningún servidor) y extrae importe, litros, precio por litro y fecha.
 * Los tiques varían mucho entre gasolineras, así que el resultado se ofrece siempre para revisar.
 */
object TicketOcr {
    data class Datos(
        val importe: Double? = null,
        val litros: Double? = null,
        val precioLitro: Double? = null,
        val fecha: LocalDate? = null,
        /** Texto completo reconocido (para la nota o para depurar). */
        val texto: String = "",
    ) {
        val vacio: Boolean get() = importe == null && litros == null && precioLitro == null && fecha == null
        /** Campos reconocidos, en el orden del formulario. */
        val campos: List<String> get() = listOfNotNull(
            fecha?.let { "fecha" }, importe?.let { "importe" }, precioLitro?.let { "precio/l" }, litros?.let { "litros" })
    }

    /** OCR sobre una foto ya guardada por [Photos] y extracción de los datos. */
    suspend fun leer(context: Context, file: File): Result<Datos> = withContext(Dispatchers.IO) {
        runCatching {
            val bmp = BitmapFactory.decodeFile(file.path) ?: error("No se pudo leer la foto")
            parse(reconocer(context, bmp))
        }
    }

    /** Texto reconocido en la imagen, línea a línea en orden de lectura. */
    suspend fun reconocer(context: Context, bmp: Bitmap): String {
        val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            return suspendCancellableCoroutine { cont ->
                client.process(InputImage.fromBitmap(bmp, 0))
                    .addOnSuccessListener { result ->
                        // ML Kit separa a menudo la etiqueta ("LITROS:") del valor ("32,45") aunque estén en la misma
                        // fila del tique: se reagrupan por altura para que cada fila vuelva a ser una sola línea.
                        val lineas = result.textBlocks.flatMap { it.lines }.filter { it.boundingBox != null }
                            .sortedWith(compareBy({ it.boundingBox!!.top }, { it.boundingBox!!.left }))
                        val filas = mutableListOf<MutableList<com.google.mlkit.vision.text.Text.Line>>()
                        for (l in lineas) {
                            val f = filas.lastOrNull()
                            val ref = f?.first()?.boundingBox
                            if (ref != null && abs(l.boundingBox!!.centerY() - ref.centerY()) < ref.height() * 0.6) f.add(l) else filas.add(mutableListOf(l))
                        }
                        cont.resume(filas.joinToString("\n") { f -> f.sortedBy { it.boundingBox!!.left }.joinToString(" ") { it.text } })
                    }
                    .addOnFailureListener { e ->
                        // Con Google Play Services el módulo de OCR se descarga en segundo plano la primera vez
                        val msg = if (e.message?.contains("downloaded", true) == true || e.message?.contains("module", true) == true)
                            "el módulo de lectura de texto se está descargando; inténtalo de nuevo en un momento" else e.message ?: "error de OCR"
                        if (cont.isActive) cont.resumeWith(Result.failure(IllegalStateException(msg, e)))
                    }
            }
        } finally {
            client.close()
        }
    }

    // ---- Extracción (pura, sin Android; se prueba en los tests unitarios) ----

    private val NUM = Regex("""(?<![\d,.])(\d{1,4})[,.](\d{2,3})(?![\d])""")
    private val FECHA = Regex("""(?<!\d)(\d{1,2})\s?[/\-.]\s?(\d{1,2})\s?[/\-.]\s?(\d{2}|\d{4})(?!\d)""")
    private val PRECIO_KW = listOf("€/L", "EUR/L", "E/L", "€/LT", "PRECIO", "PVP", "P.U", "P/L", "PREU", "PREZIO", "UNIT")
    private val LITROS_KW = listOf("LITRO", "LTS", "LTR", "VOLUM", "CANTIDAD", "CANT.", "QTY", "LIT.")
    private val IMPORTE_KW = listOf("TOTAL", "IMPORTE", "A PAGAR", "TARJETA", "EFECTIVO", "VENTA", "EUR", "€", "PAGO")
    private val EXCLUIR_KW = listOf("IVA", "BASE", "DESCUENTO", "DTO", "PUNTOS", "SALDO", "CIF", "NIF", "TEL", "KM", "HORA", "CAMBIO")

    private data class Num(val valor: Double, val decimales: Int, val linea: Int, val pos: Int)

    fun parse(texto: String): Datos {
        val lineas = texto.uppercase().replace('|', 'I').lines().map { it.trim() }
        val nums = mutableListOf<Num>()
        lineas.forEachIndexed { i, l ->
            NUM.findAll(l).forEach { m ->
                val ent = m.groupValues[1]; val dec = m.groupValues[2]
                nums += Num("$ent.$dec".toDouble(), dec.length, i, m.range.first)
            }
        }
        fun lineaTiene(i: Int, kws: List<String>) = kws.any { it in lineas[i] }
        fun cercaTiene(i: Int, kws: List<String>) = (maxOf(0, i - 1)..minOf(lineas.lastIndex, i + 1)).any { lineaTiene(it, kws) }
        fun esPrecio(n: Num) = n.decimales == 3 && n.valor in 0.5..3.5
        fun esLitros(n: Num) = n.valor in 1.0..250.0
        fun esImporte(n: Num) = n.decimales == 2 && n.valor in 1.0..600.0

        var precio: Double? = null; var litros: Double? = null; var importe: Double? = null

        // 1) Terna coherente litros × precio ≈ importe (independiente de las etiquetas)
        val ternas = mutableListOf<Triple<Num, Num, Num>>()
        for (p in nums.filter(::esPrecio)) for (l in nums.filter { esLitros(it) && it !== p }) {
            val prod = l.valor * p.valor
            nums.filter { esImporte(it) && it !== p && it !== l && abs(it.valor - prod) <= maxOf(0.02, prod * 0.012) }
                .forEach { ternas += Triple(l, p, it) }
        }
        ternas.minByOrNull { (l, p, i) -> abs(l.linea - p.linea) + abs(p.linea - i.linea) }?.let { (l, p, i) ->
            litros = l.valor; precio = p.valor; importe = i.valor
        }

        // 2) Por etiquetas, para lo que falte
        if (precio == null) precio = nums.filter(::esPrecio).let { c ->
            c.firstOrNull { lineaTiene(it.linea, PRECIO_KW) } ?: c.firstOrNull { cercaTiene(it.linea, PRECIO_KW) } ?: c.singleOrNull()
        }?.valor
        if (litros == null) litros = nums.filter { esLitros(it) && it.valor != precio && !lineaTiene(it.linea, EXCLUIR_KW) }
            .let { c -> c.firstOrNull { lineaTiene(it.linea, LITROS_KW) } ?: c.firstOrNull { cercaTiene(it.linea, LITROS_KW) } }?.valor
        if (importe == null) {
            val cand = nums.filter { esImporte(it) && it.valor != litros && !lineaTiene(it.linea, EXCLUIR_KW) }
            importe = (cand.firstOrNull { "TOTAL" in lineas[it.linea] }
                ?: cand.firstOrNull { lineaTiene(it.linea, IMPORTE_KW) }
                ?: cand.maxByOrNull { it.valor })?.valor
        }
        // 3) Completar el que falte con los otros dos
        if (importe == null && litros != null && precio != null) importe = Math.round(litros!! * precio!! * 100) / 100.0
        if (litros == null && importe != null && precio != null) litros = Math.round(importe!! / precio!! * 100) / 100.0

        val fecha = FECHA.findAll(texto).mapNotNull { m ->
            val (d, mo, y) = m.destructured
            val anio = y.toInt().let { if (it < 100) 2000 + it else it }
            runCatching { LocalDate.of(anio, mo.toInt(), d.toInt()) }.getOrNull()
                ?.takeIf { it.year in 2000..2100 && !it.isAfter(LocalDate.now().plusDays(1)) }
        }.firstOrNull()

        return Datos(importe, litros, precio, fecha, texto)
    }
}
