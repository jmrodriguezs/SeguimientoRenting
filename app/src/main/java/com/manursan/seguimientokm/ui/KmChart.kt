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
    val maxKm = (maxOf(p.kmContratados, r.liquidacion.kmProyectados, r.seguimiento.kmUltima) * 1.08).coerceAtLeast(1000.0)

    Canvas(modifier.fillMaxWidth().height(230.dp)) {
        val left = 44.dp.toPx(); val bottom = size.height - 18.dp.toPx(); val top = 8.dp.toPx(); val right = size.width - 8.dp.toPx()
        fun x(d: LocalDate) = left + (right - left) * ChronoUnit.DAYS.between(p.inicio, d).toFloat() / totalDias
        fun y(km: Double) = bottom - (bottom - top) * (km / maxKm).toFloat()

        // Rejilla horizontal y etiquetas de km
        val paso = if (maxKm > 40000) 10000.0 else 5000.0
        var k = 0.0
        while (k <= maxKm) {
            drawLine(gridColor, Offset(left, y(k)), Offset(right, y(k)), 1f)
            drawText(measurer, Fmt.int(k / 1000) + "k", Offset(2f, y(k) - 6.sp.toPx()), labelStyle)
            k += paso
        }
        // Etiquetas de años en el eje X
        var year = p.inicio.year
        while (year <= p.fin.year) {
            val d = LocalDate.of(year, 1, 1)
            if (!d.isBefore(p.inicio) && !d.isAfter(p.fin)) {
                drawLine(gridColor, Offset(x(d), top), Offset(x(d), bottom), 1f)
                drawText(measurer, year.toString(), Offset(x(d) + 3f, bottom + 2f), labelStyle)
            }
            year++
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
        Legend("Reales", cReal); Legend("Teóricos", cTeor); Legend("Proyección", cProy); Legend("27k / 30k", cAbono)
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
