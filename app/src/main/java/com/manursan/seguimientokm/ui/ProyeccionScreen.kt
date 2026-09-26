package com.manursan.seguimientokm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import com.manursan.seguimientokm.ModoProyeccion
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.RectangleShape
import java.time.temporal.ChronoUnit
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.manursan.seguimientokm.Fmt
import com.manursan.seguimientokm.KmRow
import com.manursan.seguimientokm.MainViewModel
import com.manursan.seguimientokm.Resultado

@Composable
fun ProyeccionScreen(r: Resultado, vm: MainViewModel, padding: PaddingValues) {
    val s = r.seguimiento
    val p = r.params
    val pos = positiveColor()
    val neg = negativeColor()

    // Editor de la palanca de escenario (N22 del Excel)
    val modo = p.modoProyeccion
    var kmDiaText by remember(p.kmDiaProyeccion) { mutableStateOf(p.kmDiaProyeccion?.let { Fmt.dec(it, 2) } ?: Fmt.dec(s.kmDiaRealAcumulado, 2)) }
    val kmDiaParsed = Fmt.parseDouble(kmDiaText)

    // El ritmo de los últimos 6 meses necesita 180 días de historial; si faltan, se explica al pulsarlo
    val diasHistorial = ChronoUnit.DAYS.between(p.inicio, s.fechaUltima)
    val faltanDias = (180 - diasHistorial).coerceAtLeast(0)
    var avisoReciente by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            SectionCard(title = "Escenario", subtitle = "Ritmo de km/día con el que se proyecta hasta el fin de contrato", icon = Icons.Default.Insights, accent = Palette.purple) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ModoProyeccion.entries.forEachIndexed { i, m ->
                        SegmentedButton(
                            selected = modo == m,
                            onClick = {
                                if (m == ModoProyeccion.Reciente && s.kmDiaReciente == null) {
                                    avisoReciente = true
                                } else {
                                    avisoReciente = false
                                    vm.setProyeccion(m, if (m == ModoProyeccion.Manual) (p.kmDiaProyeccion ?: Math.round(s.kmDiaRealAcumulado * 100) / 100.0) else p.kmDiaProyeccion)
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = ModoProyeccion.entries.size),
                        ) { Text(m.label, maxLines = 1, style = MaterialTheme.typography.labelMedium) }
                    }
                }
                if (avisoReciente) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "El ritmo de los últimos 6 meses necesita 180 días de mediciones: llevas $diasHistorial y faltan $faltanDias. " +
                            "Mientras tanto puedes usar Media o Manual.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniStat("Media acumulada", "${Fmt.dec(s.kmDiaRealAcumulado, 2)} km/día", Modifier.weight(1f))
                    MiniStat("Últimos 6 meses", s.kmDiaReciente?.let { "${Fmt.dec(it, 2)} km/día" } ?: "— (faltan $faltanDias días)", Modifier.weight(1f))
                }
                if (modo == ModoProyeccion.Manual) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NumberField("km/día proyección", kmDiaText, { kmDiaText = it }, Modifier.weight(1f), suffix = "km/día", isError = kmDiaParsed == null)
                        Spacer(Modifier.width(8.dp))
                        Button(
                            enabled = kmDiaParsed != null && kmDiaParsed > 0 && kmDiaParsed != p.kmDiaProyeccion,
                            onClick = { vm.setProyeccion(ModoProyeccion.Manual, kmDiaParsed) },
                        ) { Text("Aplicar") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                StatRow("Ritmo aplicado", "${Fmt.dec(s.kmDiaProyeccion, 2)} km/día", emphasized = true, valueColor = accentText(Palette.purple))
                StatRow("Ritmo teórico", "${Fmt.dec(s.kmDiaTeoricos, 2)} km/día")
                StatRow("Combustible estimado", "${Fmt.eur(s.eurKmCombustible, 4)}/km", hint = "Media real hasta la última medida")
                ThinDivider()
                StatRow("Km estimados a fin de contrato", Fmt.km(r.liquidacion.kmProyectados), emphasized = true,
                    valueColor = if (r.liquidacion.kmProyectados > p.kmContratados) neg else MaterialTheme.colorScheme.onSurface)
                StatRow(
                    if (r.liquidacion.abonoCargo >= 0) "Abono estimado" else "Cargo estimado",
                    Fmt.eur(kotlin.math.abs(r.liquidacion.abonoCargo)),
                    valueColor = if (r.liquidacion.abonoCargo < 0) neg else pos,
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        item {
            SectionCard(title = "Kilómetros frente al contrato", subtitle = "Puntos: mediciones · recta: km teóricos · discontinua: proyección", icon = Icons.Default.ShowChart, accent = Palette.green) {
                if (r.reales.size < 2) {
                    Text(
                        if (r.reales.isEmpty()) "Todavía no has anotado ninguna medición: la gráfica aparecerá en cuanto registres la primera lectura del cuentakilómetros."
                        else "Con una sola medición no hay evolución que dibujar. Anota otra lectura y aquí verás tus kilómetros frente a los del contrato.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 8.dp),
                    )
                } else {
                    KmChart(r)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        item {
            Text(
                "Proyección mensual",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "Desde la última medida (${Fmt.date(s.fechaUltima)}, ${Fmt.km(s.kmUltima)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // La desviación es la misma en todos los meses (el ritmo proyectado es constante):
            // se muestra una sola vez aquí en lugar de repetirla en cada fila
            val desv = r.proyeccion.lastOrNull()?.desviacionPct
            if (desv != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().background(tint(if (desv > 0) Palette.red else Palette.green, 0.10f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Desviación proyectada", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (desv > 0) "Por encima de los km teóricos, todos los meses" else "Por debajo de los km teóricos, todos los meses",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        Fmt.pct(desv), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        color = if (desv > 0) neg else pos,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // La tabla se compone fila a fila para que la lista solo dibuje lo visible
        item {
            Box(
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .padding(horizontal = 12.dp).padding(top = 8.dp),
            ) { ProjHeader() }
        }
        itemsIndexed(r.proyeccion, key = { _, row -> row.fecha.toString() }) { i, row ->
            val ultima = i == r.proyeccion.lastIndex
            Box(
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, if (ultima) RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp) else RectangleShape)
                    .padding(horizontal = 12.dp).padding(bottom = if (ultima) 8.dp else 0.dp),
            ) {
                Box(Modifier.background(if (i % 2 == 1) tint(Palette.purple, 0.06f) else Color.Transparent, RoundedCornerShape(8.dp))) {
                    ProjRow(row, pos, neg)
                }
            }
        }
    }
}

@Composable
private fun ProjHeader() {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        HeaderCell("Fecha", 1.2f, TextAlign.Start)
        HeaderCell("Km proyectados", 1.3f)
        HeaderCell("Km teóricos", 1.3f)
        HeaderCell("Combustible", 1.1f)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeaderCell(text: String, weight: Float, align: TextAlign = TextAlign.End) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align,
    )
}

@Composable
private fun ProjRow(row: KmRow, pos: androidx.compose.ui.graphics.Color, neg: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(Fmt.dateShort(row.fecha), Modifier.weight(1.2f).padding(start = 4.dp), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Text(
            Fmt.int(row.km), Modifier.weight(1.3f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End,
            fontWeight = FontWeight.Medium, color = if (row.desviacion > 0) neg else pos,
        )
        Text(Fmt.int(row.teoricos), Modifier.weight(1.3f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
        Text(Fmt.int(row.gastoGasolina) + " €", Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
    }
}
