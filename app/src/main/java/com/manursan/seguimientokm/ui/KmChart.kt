package com.manursan.seguimientokm.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manursan.seguimientokm.Fmt
import com.manursan.seguimientokm.Resultado
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Km reales (puntos) frente a la recta teórica del contrato, con la proyección y los umbrales. */
@Composable
fun KmChart(r: Resultado, modifier: Modifier = Modifier) {
    val p = r.params
    val measurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = TextStyle(fontSize = 10.sp, color = axisColor)
    val cTeor = axisColor
    val cReal = Palette.green
    val cProy = Palette.purple
    val cAbono = Palette.teal
    val cCargo = Palette.red

    val totalDias = ChronoUnit.DAYS.between(p.inicio, p.fin).coerceAtLeast(1)
    val maxBruto = maxOf(p.kmContratados, r.liquidacion.kmProyectados, r.seguimiento.kmUltima)
    // Protección frente a valores no finitos (proyecciones extremas) y escalas mínimas
    val maxKm = (if (maxBruto.isFinite()) maxBruto * 1.08 else 1000.0).coerceIn(1000.0, 5_000_000.0)
    // El eje se divide en pasos "redondos" (1, 2, 2,5 o 5 × 10^n) para que salgan entre 4 y 6 marcas,
    // nunca decenas de etiquetas encima unas de otras cuando el contrato tiene muchos kilómetros.
    val paso = pasoRedondo(maxKm)
    val marcas = generateSequence(0.0) { it + paso }.takeWhile { it <= maxKm + paso * 0.01 }.take(12).toList()

    Canvas(modifier.fillMaxWidth().height(230.dp)) {
        // El margen izquierdo se ajusta al ancho real de la etiqueta más larga
        val textos = marcas.map { measurer.measure(etiquetaKm(it, paso), labelStyle) }
        val anchoEje = (textos.maxOfOrNull { it.size.width } ?: 0) + 6.dp.toPx()
        val left = anchoEje; val bottom = size.height - 18.dp.toPx(); val top = 8.dp.toPx(); val right = size.width - 8.dp.toPx()
        fun x(d: LocalDate) = left + (right - left) * ChronoUnit.DAYS.between(p.inicio, d).toFloat() / totalDias
        fun y(km: Double) = bottom - (bottom - top) * (km / maxKm).toFloat()

        // Rejilla horizontal y etiquetas de km, centradas en su línea
        marcas.forEachIndexed { i, k ->
            drawLine(gridColor, Offset(left, y(k)), Offset(right, y(k)), 1f)
            val t = textos[i]
            drawText(t, topLeft = Offset(left - t.size.width - 4.dp.toPx(), y(k) - t.size.height / 2f))
        }
        // Etiquetas de años en el eje X; si el contrato es muy largo se muestran de dos en dos
        val anios = (p.inicio.year..p.fin.year).map { LocalDate.of(it, 1, 1) }.filter { !it.isBefore(p.inicio) && !it.isAfter(p.fin) }
        val salto = if (anios.size > 5) 2 else 1
        anios.forEachIndexed { i, d ->
            drawLine(gridColor, Offset(x(d), top), Offset(x(d), bottom), 1f)
            if (i % salto == 0) {
                val t = measurer.measure(d.year.toString(), labelStyle)
                // Si la etiqueta se saldría por la derecha se pega al borde en lugar de cortarse
                val px = minOf(x(d) + 3f, right - t.size.width)
                drawText(t, topLeft = Offset(px, bottom + 2f))
            }
        }
        drawLine(axisColor, Offset(left, bottom), Offset(right, bottom), 1.5f)

        // Umbrales de liquidación
        val dotted = PathEffect.dashPathEffect(floatArrayOf(4f, 8f))
        drawLine(cCargo, Offset(left, y(r.liquidacion.umbralCargo)), Offset(right, y(r.liquidacion.umbralCargo)), 2f, pathEffect = dotted)
        drawLine(cAbono, Offset(left, y(r.liquidacion.umbralAbono)), Offset(right, y(r.liquidacion.umbralAbono)), 2f, pathEffect = dotted)

        // Recta teórica
        drawLine(cTeor, Offset(x(p.inicio), y(0.0)), Offset(x(p.fin), y(p.kmContratados)), 3f, cap = StrokeCap.Round)

        // Proyección (discontinua) desde la última medida
        val dashed = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
        val s = r.seguimiento
        if (r.proyeccion.isNotEmpty()) {
            val fin = r.proyeccion.last()
            drawLine(cProy, Offset(x(s.fechaUltima), y(s.kmUltima)), Offset(x(fin.fecha), y(fin.km)), 3f, pathEffect = dashed, cap = StrokeCap.Round)
        }

        // Reales: línea y puntos
        val pts = listOf(Offset(x(p.inicio), y(0.0))) + r.reales.map { Offset(x(it.fecha), y(it.km)) }
        for (i in 1 until pts.size) drawLine(cReal, pts[i - 1], pts[i], 4f, cap = StrokeCap.Round)
        pts.drop(1).forEach {
            drawCircle(Color.White, 6f, it)
            drawCircle(cReal, 6f, it, style = Stroke(3f))
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Legend("Reales", cReal); Legend("Teóricos", cTeor); Legend("Proyección", cProy)
        // Umbrales del contrato, no un valor fijo
        Legend("${etiquetaKm(r.liquidacion.umbralAbono, 1000.0)} / ${etiquetaKm(r.liquidacion.umbralCargo, 1000.0)}", cAbono)
    }
}

@Composable
private fun Legend(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Paso "redondo" (1, 2, 2,5 o 5 × 10^n) para dividir el eje: se elige el que deja un número de
 * marcas lo más cercano a [objetivo], de modo que el eje siempre tenga entre 4 y 6 divisiones.
 */
internal fun pasoRedondo(maximo: Double, objetivo: Int = 5): Double {
    if (!maximo.isFinite() || maximo <= 0) return 1000.0
    val exp = kotlin.math.floor(kotlin.math.log10(maximo / objetivo))
    val candidatos = (-1..1).flatMap { d ->
        val base = Math.pow(10.0, exp + d)
        listOf(1.0, 2.0, 2.5, 5.0).map { it * base }
    }.filter { it > 0 }
    return candidatos.minByOrNull { paso ->
        val n = kotlin.math.ceil(maximo / paso).toInt()
        if (n < 3 || n > 8) 1000 else kotlin.math.abs(n - objetivo)
    } ?: 1000.0
}

/** Etiqueta del eje: en miles cuando el paso lo permite ("10k", "2,5k"), si no el número entero. */
internal fun etiquetaKm(valor: Double, paso: Double): String = when {
    !valor.isFinite() -> ""
    paso >= 1000 -> {
        val miles = valor / 1000.0
        if (paso % 1000.0 == 0.0) Fmt.int(miles) + "k" else Fmt.dec(miles, 1) + "k"
    }
    else -> Fmt.int(valor)
}
