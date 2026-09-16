package com.manursan.seguimientokm

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** Informe de estado de una página (A4 vertical) con PdfDocument, sin dependencias. */
object PdfReport {
    private const val W = 595f
    private const val H = 842f
    private const val M = 36f

    private const val AZUL = 0xFF2F6BFF.toInt()
    private const val VERDE = 0xFF1E8E4E.toInt()
    private const val ROJO = 0xFFE5484D.toInt()
    private const val NARANJA = 0xFFD9640E.toInt()
    private const val MORADO = 0xFF7C4DFF.toInt()
    private const val TINTA = 0xFF1B1F2B.toInt()
    private const val GRIS = 0xFF555C6E.toInt()
    private const val LINEA = 0xFFB8C0D0.toInt()
    private const val LINEA_SUAVE = 0xFFE3E7EF.toInt()

    fun build(r: Resultado, hoy: LocalDate = LocalDate.now()): ByteArray {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(W.toInt(), H.toInt(), 1).create())
        Painter(page.canvas).draw(r, hoy)
        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    private class Painter(val c: Canvas) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)

        fun txt(s: String, x: Float, y: Float, size: Float = 9f, bold: Boolean = false, color: Int = TINTA, align: Paint.Align = Paint.Align.LEFT) {
            p.style = Paint.Style.FILL; p.color = color; p.textSize = size; p.textAlign = align
            p.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            p.pathEffect = null
            c.drawText(s, x, y, p)
        }

        fun box(l: Float, t: Float, r: Float, b: Float, color: Int = LINEA, width: Float = 0.8f) {
            p.style = Paint.Style.STROKE; p.color = color; p.strokeWidth = width; p.pathEffect = null
            c.drawRoundRect(RectF(l, t, r, b), 4f, 4f, p)
        }

        fun fill(l: Float, t: Float, r: Float, b: Float, color: Int) {
            p.style = Paint.Style.FILL; p.color = color; p.pathEffect = null
            c.drawRoundRect(RectF(l, t, r, b), 4f, 4f, p)
        }

        fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int = LINEA_SUAVE, width: Float = 0.6f, dash: FloatArray? = null) {
            p.style = Paint.Style.STROKE; p.color = color; p.strokeWidth = width
            p.pathEffect = dash?.let { DashPathEffect(it, 0f) }
            c.drawLine(x1, y1, x2, y2, p)
            p.pathEffect = null
        }

        /** Fila etiqueta (izquierda) / valor (derecha) dentro de un bloque. */
        fun row(label: String, value: String, x: Float, y: Float, w: Float, valueColor: Int = TINTA, bold: Boolean = false, size: Float = 8.5f) {
            txt(label, x, y, size, color = GRIS)
            txt(value, x + w, y, size, bold = bold, color = valueColor, align = Paint.Align.RIGHT)
        }

        fun blockTitle(title: String, x: Float, y: Float, color: Int) {
            txt(title, x, y, 8.5f, bold = true, color = color)
            line(x, y + 4f, x + 1f, y + 4f) // nada: separador lo dibuja el llamador
        }

        fun draw(r: Resultado, hoy: LocalDate) {
            val p0 = r.params
            val s = r.seguimiento
            val ultima = r.reales.lastOrNull()
            val neg = ROJO; val pos = VERDE

            // ---- Cabecera ----
            fill(M, M, W - M, M + 46f, AZUL)
            txt("SEGUIMIENTO RENTING", M + 12f, M + 19f, 14f, bold = true, color = Color.WHITE)
            txt(listOf(p0.vehiculo, p0.matricula, "contrato ${p0.contrato}", p0.empresa).filter { it.isNotBlank() }.joinToString(" · "), M + 12f, M + 36f, 9f, color = Color.WHITE)
            txt("Informe a ${Fmt.date(hoy)}", W - M - 12f, M + 19f, 10f, bold = true, color = Color.WHITE, align = Paint.Align.RIGHT)
            txt("Última medida ${Fmt.date(s.fechaUltima)}", W - M - 12f, M + 36f, 8.5f, color = Color.WHITE, align = Paint.Align.RIGHT)
            var y = M + 46f + 16f
            txt(
                "Inicio ${Fmt.date(p0.inicio)}  ·  Fin ${Fmt.date(p0.fin)}  ·  Día ${Fmt.int(s.diasTranscurridos)} de ${Fmt.int(s.diasContrato)}  ·  Quedan ${Fmt.int(s.diasRestantes)} días (${Fmt.dec(s.diasRestantes / 30.4375, 1)} meses)  ·  ${Fmt.km(p0.kmContratados)} contratados",
                M, y, 8.5f, color = GRIS,
            )
            y += 12f

            // ---- 4 indicadores ----
            val gap = 8f
            val kw = (W - 2 * M - 3 * gap) / 4
            val kh = 58f
            data class Kpi(val titulo: String, val valor: String, val pie: String, val color: Int)
            val kpis = listOf(
                Kpi("KM ACTUALES", Fmt.km(s.kmUltima), "a ${Fmt.date(s.fechaUltima)}", AZUL),
                Kpi(
                    "DESVIACIÓN", ultima?.let { Fmt.signed(it.desviacion, 0, " km") } ?: "—",
                    ultima?.let { "${Fmt.pct(it.desviacionPct)} vs. ${Fmt.km(it.teoricos)} teóricos" } ?: "sin mediciones",
                    if (ultima != null && ultima.desviacion > 0) neg else pos,
                ),
                Kpi("RITMO", "${Fmt.dec(s.kmDiaRealAcumulado, 1)} km/día", "contrato ${Fmt.dec(s.kmDiaTeoricos, 1)} · 6 meses ${s.kmDiaReciente?.let { Fmt.dec(it, 1) } ?: "—"}", VERDE),
                Kpi("COSTE / KM", Fmt.eur(r.costeKm.total, 4), "renting ${Fmt.dec(r.costeKm.renting, 3)} · comb. ${Fmt.dec(r.costeKm.combustible, 3)} · otros ${Fmt.dec(r.costeKm.otros, 3)}", NARANJA),
            )
            kpis.forEachIndexed { i, k ->
                val x = M + i * (kw + gap)
                box(x, y, x + kw, y + kh)
                txt(k.titulo, x + 8f, y + 14f, 7.5f, bold = true, color = GRIS)
                txt(k.valor, x + 8f, y + 34f, 15f, bold = true, color = k.color)
                txt(k.pie, x + 8f, y + 49f, 6.8f, color = GRIS)
            }
            y += kh + 10f

            // ---- Gráfica ----
            val ch = 190f
            box(M, y, W - M, y + ch)
            txt("KILÓMETROS FRENTE AL CONTRATO", M + 8f, y + 14f, 7.5f, bold = true, color = GRIS)
            chart(r, M + 8f, y + 22f, W - M - 8f, y + ch - 8f)
            y += ch + 10f

            // ---- Dos columnas: margen / liquidación ----
            val cw = (W - 2 * M - gap) / 2
            val xL = M; val xR = M + cw + gap
            val bh1 = 118f
            box(xL, y, xL + cw, y + bh1); box(xR, y, xR + cw, y + bh1)
            val m = r.margen
            var yy = y + 14f
            txt("MARGEN HASTA EL FIN DE CONTRATO", xL + 8f, yy, 7.5f, bold = true, color = AZUL); yy += 14f
            val iw = cw - 16f
            if (m.kmHastaContratados > 0) {
                row("Sin exceso (≤ ${Fmt.km(p0.kmContratados)})", "${Fmt.km(m.kmHastaContratados)} → máx. ${Fmt.dec(m.kmDiaMaxSinCargo, 1)} km/día", xL + 8f, yy, iw, bold = true); yy += 12f
                row("", "${Fmt.int(m.kmMesMaxSinCargo)} km/mes", xL + 8f, yy, iw, color(GRIS)); yy += 12f
            } else {
                row("Exceso sobre lo contratado", Fmt.km(-m.kmHastaContratados), xL + 8f, yy, iw, neg, bold = true); yy += 24f
            }
            if (m.kmHastaUmbralAbono > 0) {
                row("Con abono (< ${Fmt.km(r.liquidacion.umbralAbono)})", "${Fmt.km(m.kmHastaUmbralAbono)} → máx. ${Fmt.dec(m.kmDiaMaxConAbono, 1)} km/día", xL + 8f, yy, iw); yy += 12f
            } else {
                row("Umbral de abono", "superado", xL + 8f, yy, iw); yy += 12f
            }
            row("Ritmo actual", "${Fmt.dec(s.kmDiaRealAcumulado, 1)} km/día · ${if (s.kmDiaRealAcumulado > m.kmDiaMaxSinCargo) "por encima del máximo" else "dentro del margen"}", xL + 8f, yy, iw,
                if (s.kmDiaRealAcumulado > m.kmDiaMaxSinCargo) neg else pos); yy += 14f
            r.proximoAjuste?.let { a ->
                line(xL + 8f, yy - 9f, xL + cw - 8f, yy - 9f)
                row("Ajuste anual ${Fmt.date(a.fecha)} (en ${Fmt.int(a.diasHasta)} días)", "${Fmt.km(a.kmPrevistos)} vs. ${Fmt.km(a.kmTeoricos)}", xL + 8f, yy, iw); yy += 12f
                row("", "${Fmt.pct(a.desviacionPct)} · ${if (a.dentroDeBanda) "dentro" else "fuera"} de la banda ±${Fmt.pct(p0.umbralAjuste, 0)}", xL + 8f, yy, iw, if (a.dentroDeBanda) pos else NARANJA)
            }

            val l = r.liquidacion
            yy = y + 14f
            txt("LIQUIDACIÓN (proyección a ${Fmt.dec(s.kmDiaProyeccion, 1)} km/día)", xR + 8f, yy, 7.5f, bold = true, color = MORADO); yy += 14f
            row("Km estimados a ${Fmt.date(p0.fin)}", Fmt.km(l.kmProyectados), xR + 8f, yy, iw, bold = true); yy += 12f
            row("Umbral abono / km contratados", "${Fmt.km(l.umbralAbono)} / ${Fmt.km(l.umbralCargo)}", xR + 8f, yy, iw); yy += 12f
            val resultado = when {
                l.abonoCargo > 0 -> "abono ${Fmt.eur(l.abonoCargo)}"
                l.abonoCargo < 0 -> "cargo ${Fmt.eur(-l.abonoCargo)}" + if (l.recargoAplicado) " (con recargo)" else ""
                else -> "sin abono ni cargo"
            }
            row("Resultado (sin IVA)", resultado, xR + 8f, yy, iw, if (l.abonoCargo < 0) neg else pos, bold = true); yy += 12f
            val tramo = when {
                l.kmProyectados < l.umbralAbono -> "Por debajo del umbral: ${Fmt.dec(p0.eurKmNoRecorrido, 4)} €/km no recorrido"
                l.kmProyectados <= l.umbralCargo -> "Entre umbral y contratados: sin ajuste"
                else -> "Exceso a ${Fmt.dec(p0.eurKmExceso, 4)} €/km" + if (l.recargoAplicado) " ×${Fmt.dec(p0.recargoExceso, 2)}" else ""
            }
            txt(tramo, xR + 8f, yy, 7.5f, color = GRIS); yy += 14f
            line(xR + 8f, yy - 9f, xR + cw - 8f, yy - 9f)
            row("Escenario", when (p0.modoProyeccion) { ModoProyeccion.Acumulada -> "media acumulada"; ModoProyeccion.Reciente -> "ritmo últimos 6 meses"; ModoProyeccion.Manual -> "manual" }, xR + 8f, yy, iw); yy += 12f
            row("Combustible estimado", "${Fmt.dec(s.eurKmCombustible, 4)} €/km", xR + 8f, yy, iw)
            y += bh1 + 10f

            // ---- Dos columnas: combustible+gastos / coste ----
            val bh2 = 150f
            box(xL, y, xL + cw, y + bh2); box(xR, y, xR + cw, y + bh2)
            yy = y + 14f
            txt("COMBUSTIBLE Y OTROS GASTOS", xL + 8f, yy, 7.5f, bold = true, color = NARANJA); yy += 14f
            val ultimoRep = r.repostajes.lastOrNull()
            row("Total repostado", Fmt.eur(ultimoRep?.acumulado ?: 0.0), xL + 8f, yy, iw, bold = true); yy += 12f
            row("Repostajes", if (ultimoRep != null) "${r.repostajes.size} · último ${Fmt.date(ultimoRep.refuel.fecha)}" else "—", xL + 8f, yy, iw); yy += 12f
            val cons = r.consumo
            row("Consumo", cons.litros100km?.let { "${Fmt.dec(it, 2)} l/100 km" + if (cons.estimado) " (estimado)" else "" } ?: "sin datos de litros", xL + 8f, yy, iw); yy += 12f
            row("Precio medio · litros", cons.precioMedioLitro?.let { "${Fmt.dec(it, 3)} €/l · ${Fmt.dec(cons.litrosConocidos, 0)} l" } ?: "—", xL + 8f, yy, iw); yy += 12f
            row("Combustible por km", "${Fmt.dec(s.eurKmCombustible, 4)} €/km", xL + 8f, yy, iw); yy += 14f
            line(xL + 8f, yy - 9f, xL + cw - 8f, yy - 9f)
            val g = r.gastos
            val cats = g.porCategoria.entries.sortedByDescending { it.value }.take(3).joinToString(" · ") { "${it.key.label} ${Fmt.dec(it.value, 0)}" }
            row("Otros gastos (${g.lista.size})", Fmt.eur(g.total), xL + 8f, yy, iw, bold = true); yy += 12f
            if (cats.isNotEmpty()) { txt(cats, xL + 8f, yy, 7.5f, color = GRIS); yy += 12f }
            row("Coste de uso hasta hoy", Fmt.eur(g.costeUsoHastaHoy), xL + 8f, yy, iw, bold = true)

            val ct = r.coste
            yy = y + 14f
            txt("COSTE DEL CONTRATO (proyección)", xR + 8f, yy, 7.5f, bold = true, color = 0xFFC2185B.toInt()); yy += 14f
            row("Cuotas (${p0.meses} × ${Fmt.eur(p0.cuotaMensual)})", Fmt.eur(ct.cuotas), xR + 8f, yy, iw); yy += 12f
            row("Cuota irregular inicial", Fmt.eur(ct.cuotaIrregular), xR + 8f, yy, iw); yy += 12f
            row("Combustible proyectado", Fmt.eur(ct.combustibleProyectado), xR + 8f, yy, iw); yy += 12f
            row(if (ct.abono >= 0) "Abono km no recorridos" else "Cargo km de exceso", (if (ct.abono >= 0) "−" else "+") + Fmt.eur(abs(ct.abono)), xR + 8f, yy, iw); yy += 12f
            line(xR + 8f, yy - 4f, xR + cw - 8f, yy - 4f, LINEA)
            yy += 6f
            row("TOTAL", Fmt.eur(ct.total), xR + 8f, yy, iw, bold = true, size = 9.5f); yy += 13f
            row("Coste por km (con otros gastos)", Fmt.eur(r.costeKm.proyectado, 4), xR + 8f, yy, iw); yy += 12f
            row("IVA soportado · deducible (${Fmt.pct(p0.pctDeduccion, 0)})", "${Fmt.eur(ct.ivaTotal)} · ${Fmt.eur(ct.ivaDeducible)}", xR + 8f, yy, iw); yy += 12f
            row("Coste neto tras deducción", Fmt.eur(ct.costeNeto), xR + 8f, yy, iw, bold = true); yy += 12f
            row("Depósito (recuperable)", Fmt.eur(ct.deposito), xR + 8f, yy, iw)
            y += bh2 + 10f

            // ---- Últimas mediciones (tabla compacta) ----
            val ultimas = r.reales.takeLast(6).asReversed()
            if (ultimas.isNotEmpty()) {
                val th = 38f + ultimas.size * 12f
                box(M, y, W - M, y + th)
                yy = y + 14f
                txt("ÚLTIMAS MEDICIONES", M + 8f, yy, 7.5f, bold = true, color = VERDE); yy += 13f
                val cols = listOf("Fecha", "Día", "Km reales", "Km teóricos", "Desviación", "Km/día", "Gasolina acum.", "Coste/km")
                val cx = FloatArray(cols.size) { i -> M + 8f + i * ((W - 2 * M - 16f) / cols.size) }
                val cwid = (W - 2 * M - 16f) / cols.size
                cols.forEachIndexed { i, h -> txt(h, if (i == 0) cx[i] else cx[i] + cwid - 6f, yy, 7f, bold = true, color = GRIS, align = if (i == 0) Paint.Align.LEFT else Paint.Align.RIGHT) }
                yy += 4f
                line(M + 8f, yy, W - M - 8f, yy, LINEA)
                yy += 10f
                ultimas.forEach { k ->
                    val vals = listOf(
                        Fmt.date(k.fecha), Fmt.int(k.dias), Fmt.int(k.km), Fmt.int(k.teoricos),
                        "${Fmt.signed(k.desviacion, 0)} (${Fmt.pct(k.desviacionPct)})", Fmt.dec(k.kmDia, 1), Fmt.eur(k.gastoGasolina), Fmt.eur(k.costeKmTotal, 4),
                    )
                    vals.forEachIndexed { i, v ->
                        val col = if (i == 4) (if (k.desviacion > 0) ROJO else VERDE) else TINTA
                        txt(v, if (i == 0) cx[i] else cx[i] + cwid - 6f, yy, 7.5f, color = col, align = if (i == 0) Paint.Align.LEFT else Paint.Align.RIGHT)
                    }
                    yy += 12f
                }
                y += th + 8f
            }

            // ---- Pie ----
            txt(
                "Generado por Seguimiento Renting · Teóricos = km contratados repartidos linealmente en los días de contrato · Precio de mercado: datos abiertos del Ministerio de Industria (Gasolina 95)",
                M, H - M + 10f, 6.5f, color = GRIS,
            )
        }

        private fun color(c: Int) = c

        /** Gráfica km reales / teóricos / proyección con umbrales, en coordenadas de página. */
        private fun chart(r: Resultado, l: Float, t: Float, rt: Float, b: Float) {
            val p0 = r.params
            val left = l + 28f; val right = rt - 4f; val top = t + 4f; val bottom = b - 12f
            val totalDias = ChronoUnit.DAYS.between(p0.inicio, p0.fin).coerceAtLeast(1)
            val maxKm = (maxOf(p0.kmContratados, r.liquidacion.kmProyectados, r.seguimiento.kmUltima) * 1.08).coerceAtLeast(1000.0)
            fun x(d: LocalDate) = left + (right - left) * ChronoUnit.DAYS.between(p0.inicio, d).toFloat() / totalDias
            fun y(km: Double) = bottom - (bottom - top) * (km / maxKm).toFloat()

            val paso = if (maxKm > 40000) 10000.0 else 5000.0
            var k = 0.0
            while (k <= maxKm) {
                line(left, y(k), right, y(k))
                txt(Fmt.int(k / 1000) + "k", left - 3f, y(k) + 2.5f, 6.5f, color = GRIS, align = Paint.Align.RIGHT)
                k += paso
            }
            var year = p0.inicio.year
            while (year <= p0.fin.year) {
                val d = LocalDate.of(year, 1, 1)
                if (!d.isBefore(p0.inicio) && !d.isAfter(p0.fin)) {
                    line(x(d), top, x(d), bottom)
                    txt(year.toString(), x(d) + 2f, bottom + 9f, 6.5f, color = GRIS)
                }
                year++
            }
            line(left, bottom, right, bottom, LINEA, 0.8f)

            // Umbrales
            line(left, y(r.liquidacion.umbralCargo), right, y(r.liquidacion.umbralCargo), ROJO, 0.8f, floatArrayOf(2f, 3f))
            line(left, y(r.liquidacion.umbralAbono), right, y(r.liquidacion.umbralAbono), 0xFF00A392.toInt(), 0.8f, floatArrayOf(2f, 3f))
            // Teórica
            line(x(p0.inicio), y(0.0), x(p0.fin), y(p0.kmContratados), GRIS, 1.4f)
            // Proyección
            r.proyeccion.lastOrNull()?.let { fin ->
                line(x(r.seguimiento.fechaUltima), y(r.seguimiento.kmUltima), x(fin.fecha), y(fin.km), MORADO, 1.4f, floatArrayOf(5f, 4f))
            }
            // Reales
            val pts = listOf(x(p0.inicio) to y(0.0)) + r.reales.map { x(it.fecha) to y(it.km) }
            val path = Path()
            pts.forEachIndexed { i, (px, py) -> if (i == 0) path.moveTo(px, py) else path.lineTo(px, py) }
            p.style = Paint.Style.STROKE; p.color = VERDE; p.strokeWidth = 1.8f; p.pathEffect = null
            c.drawPath(path, p)
            pts.drop(1).forEach { (px, py) ->
                p.style = Paint.Style.FILL; p.color = Color.WHITE; c.drawCircle(px, py, 2.6f, p)
                p.style = Paint.Style.STROKE; p.color = VERDE; p.strokeWidth = 1.4f; c.drawCircle(px, py, 2.6f, p)
            }
            // Leyenda
            var lx = left + 6f
            val ly = top + 8f
            listOf("Reales" to VERDE, "Teóricos" to GRIS, "Proyección" to MORADO, "Umbrales 27k / 30k" to ROJO).forEach { (name, col) ->
                p.style = Paint.Style.FILL; p.color = col; c.drawCircle(lx, ly - 2.5f, 2.5f, p)
                txt(name, lx + 5f, ly, 6.5f, color = GRIS)
                lx += 12f + name.length * 3.6f
            }
        }
    }
}
