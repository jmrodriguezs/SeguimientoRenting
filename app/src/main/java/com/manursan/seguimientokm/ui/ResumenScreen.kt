package com.manursan.seguimientokm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.manursan.seguimientokm.Fmt
import com.manursan.seguimientokm.Resultado

@Composable
fun ResumenScreen(r: Resultado, padding: PaddingValues) {
    val s = r.seguimiento
    val p = r.params
    val ultima = r.reales.lastOrNull()
    val pos = positiveColor()
    val neg = negativeColor()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (p.kmContratados <= 0) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 80.dp)) {
                    EmptyHint(
                        "Contrato sin configurar",
                        "Introduce los datos del contrato en Ajustes (fechas, plazo, km/año, cuotas y tarifas) o restaura una copia de seguridad desde el menú ⋮.",
                        Icons.Default.DirectionsCar, Palette.indigo,
                    )
                }
            }
            return@LazyColumn
        }
        item { HeroCard(r) }

        item {
            SectionCard(title = "Ritmo", subtitle = "Comparado con el contrato", icon = Icons.Default.DirectionsCar, accent = Palette.green) {
                if (ultima != null) {
                    StatRow("Km teóricos a ${Fmt.date(ultima.fecha)}", Fmt.km(ultima.teoricos))
                    StatRow(
                        "Desviación",
                        "${Fmt.signed(ultima.desviacion, 0, " km")} (${Fmt.pct(ultima.desviacionPct)})",
                        valueColor = if (ultima.desviacion > 0) neg else pos,
                        emphasized = true,
                        hint = if (ultima.desviacion > 0) "Vas por encima del ritmo contratado" else "Vas por debajo del ritmo contratado",
                    )
                    ThinDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MiniStat("Km/día real", Fmt.dec(s.kmDiaRealAcumulado, 2), Modifier.weight(1f))
                        MiniStat("Km/día contrato", Fmt.dec(s.kmDiaTeoricos, 2), Modifier.weight(1f))
                        r.consumo.litros100km?.let { MiniStat("Consumo", "${Fmt.dec(it, 2)} l/100", Modifier.weight(1f), valueColor = accentText(Palette.orange)) }
                    }
                } else {
                    Text("Todavía no hay mediciones. Añade la primera en la pestaña Kilómetros.")
                }
            }
        }

        item {
            val m = r.margen
            SectionCard(
                title = "¿Cuánto puedo conducir?",
                subtitle = "Desde la última medida hasta el ${Fmt.date(p.fin)} (${Fmt.int(m.diasRestantes)} días)",
                icon = Icons.Default.Explore,
                accent = Palette.blue,
            ) {
                if (m.kmHastaContratados > 0) {
                    StatRow("Sin exceso (≤ ${Fmt.km(p.kmContratados)})", Fmt.km(m.kmHastaContratados), emphasized = true, hint = "Kilómetros que te quedan")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("máx. ${Fmt.dec(m.kmDiaMaxSinCargo, 1)} km/día", Palette.blue)
                        Pill("máx. ${Fmt.int(m.kmMesMaxSinCargo)} km/mes", Palette.blue)
                    }
                    Text(
                        "Tu ritmo actual es de ${Fmt.dec(s.kmDiaRealAcumulado, 1)} km/día" +
                            if (s.kmDiaRealAcumulado > m.kmDiaMaxSinCargo) ": por encima del máximo, acabarías pagando exceso." else ": vas dentro del margen.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    StatRow("Exceso sobre lo contratado", Fmt.km(-m.kmHastaContratados), emphasized = true, valueColor = neg, hint = "Cada km más se paga a ${Fmt.dec(p.eurKmExceso, 4)} €")
                }
                ThinDivider()
                if (m.kmHastaUmbralAbono > 0) {
                    StatRow("Con abono (< ${Fmt.km(r.liquidacion.umbralAbono)})", "máx. ${Fmt.dec(m.kmDiaMaxConAbono, 1)} km/día", hint = "${Fmt.int(m.kmMesMaxConAbono)} km/mes · abono de ${Fmt.dec(p.eurKmNoRecorrido, 4)} €/km")
                } else {
                    Text("Ya has pasado el umbral de abono (${Fmt.km(r.liquidacion.umbralAbono)}); entre ese punto y los km contratados no hay ni abono ni cargo.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            SectionCard(title = "Combustible", icon = Icons.Default.LocalGasStation, accent = Palette.orange) {
                val ultimoRep = r.repostajes.lastOrNull()
                val total = ultimoRep?.acumulado ?: 0.0
                // Si hay repostajes posteriores a la última medida, se indica la parte que entra en los cálculos
                val pendiente = total - s.combustibleAcumulado
                StatRow(
                    "Total repostado",
                    Fmt.eur(total),
                    emphasized = true,
                    hint = if (ultimoRep == null) "Sin repostajes"
                    else "${r.repostajes.size} repostajes, último ${Fmt.date(ultimoRep.refuel.fecha)}" +
                        if (pendiente > 0.005) " · ${Fmt.eur(pendiente)} posteriores a la última medida" else "",
                )
                StatRow("Combustible por km", Fmt.eur(s.eurKmCombustible, 4), hint = "Sobre los ${Fmt.eur(s.combustibleAcumulado)} hasta la última medida")
                ultimoRep?.costeDiario?.let { StatRow("Coste diario", Fmt.eur(it, 2)) }
                val cons = r.consumo
                if (cons.litros100km != null) {
                    ThinDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MiniStat("Consumo", "${Fmt.dec(cons.litros100km, 2)} l/100 km", Modifier.weight(1.2f), valueColor = accentText(Palette.orange))
                        MiniStat("Precio medio", "${Fmt.dec(cons.precioMedioLitro ?: 0.0, 3)} €/l", Modifier.weight(1f))
                        MiniStat("Litros", Fmt.dec(cons.litrosConocidos, 1), Modifier.weight(0.8f))
                    }
                    if (cons.estimado) {
                        Text(
                            "${cons.repostajesSinLitros} repostajes sin litros ni precio: estimados con el precio medio. Usa \"Completar precios de mercado\" en el menú.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Text("Anota litros o precio/litro en los repostajes (o usa \"Completar precios de mercado\" en el menú) para ver el consumo.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            val ck = r.costeKm
            SectionCard(
                title = "Coste por kilómetro",
                subtitle = "Renting + combustible + otros gastos, entre los km recorridos",
                icon = Icons.Default.Calculate,
                accent = Palette.amber,
            ) {
                if (ultima != null && ultima.km > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MiniStat("Renting", Fmt.eur(ck.renting, 4), Modifier.weight(1f))
                        MiniStat("Combustible", Fmt.eur(ck.combustible, 4), Modifier.weight(1f), valueColor = accentText(Palette.orange))
                        MiniStat("Otros", Fmt.eur(ck.otros, 4), Modifier.weight(1f))
                        MiniStat("Total", Fmt.eur(ck.total, 4), Modifier.weight(1f), valueColor = accentText(Palette.amber))
                    }
                    Text(
                        "Km a ${Fmt.date(ultima.fecha)}" + (if (r.gastos.enCosteKm > 0) " · ${Fmt.eur(r.gastos.enCosteKm)} de otros gastos" else " · sin otros gastos anotados") +
                            ". Baja a medida que haces km: la cuota es fija y se reparte entre más kilómetros.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // Solo si el escenario de proyección cambia el resultado (ritmo manual o cargo por exceso)
                    if (kotlin.math.abs(ck.proyectado - ck.total) >= 0.0005) {
                        ThinDivider()
                        StatRow(
                            "A fin de contrato (${Fmt.dec(s.kmDiaProyeccion, 2)} km/día)",
                            Fmt.eur(ck.proyectado, 4),
                            hint = "Incluye liquidación de km y otros gastos proporcionales",
                        )
                    }
                } else {
                    Text("Se calcula a partir de la primera medición.")
                }
            }
        }

        item {
            val l = r.liquidacion
            SectionCard(
                title = "Liquidación fin de contrato",
                subtitle = "A ${Fmt.date(p.fin)} con ${Fmt.dec(s.kmDiaProyeccion, 2)} km/día",
                icon = Icons.Default.Gavel,
                accent = Palette.purple,
            ) {
                StatRow("Km estimados a fin de contrato", Fmt.km(l.kmProyectados), emphasized = true, hint = "A tu ritmo actual; se compara con los contratados")
                StatRow("Umbral abono (${Fmt.pct(p.umbralLiquidacion, 0)})", Fmt.km(l.umbralAbono))
                StatRow("Km contratados (cargo)", Fmt.km(l.umbralCargo))
                ThinDivider()
                val texto = when {
                    l.abonoCargo > 0 -> "Abono a tu favor"
                    l.abonoCargo < 0 -> "Cargo por exceso" + if (l.recargoAplicado) " (con recargo ×${Fmt.dec(p.recargoExceso, 2)})" else ""
                    else -> "Sin abono ni cargo"
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Pill(texto, if (l.abonoCargo < 0) Palette.red else Palette.green)
                    Text(
                        Fmt.eur(kotlin.math.abs(l.abonoCargo)),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (l.abonoCargo < 0) neg else pos,
                    )
                }
                Spacer(Modifier.height(6.dp))
                val tramo = when {
                    l.kmProyectados < l.umbralAbono -> "Por debajo de ${Fmt.km(l.umbralAbono)}: se abona a ${Fmt.dec(p.eurKmNoRecorrido, 4)} €/km"
                    l.kmProyectados <= l.umbralCargo -> "Entre ${Fmt.km(l.umbralAbono)} y ${Fmt.km(l.umbralCargo)}: sin ajuste"
                    else -> "Por encima de ${Fmt.km(l.umbralCargo)}: ${Fmt.dec(p.eurKmExceso, 4)} €/km de exceso"
                }
                Text(tramo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        r.proximoAjuste?.let { a ->
            item {
                SectionCard(
                    title = "Próximo ajuste anual",
                    subtitle = "${Fmt.date(a.fecha)} · en ${Fmt.int(a.diasHasta)} días",
                    icon = Icons.Default.Event,
                    accent = Palette.indigo,
                ) {
                    StatRow("Km previstos a esa fecha", Fmt.km(a.kmPrevistos), hint = "Teóricos: ${Fmt.km(a.kmTeoricos)}")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Pill(if (a.dentroDeBanda) "Dentro de la banda ±${Fmt.pct(p.umbralAjuste, 0)}" else "Fuera de la banda ±${Fmt.pct(p.umbralAjuste, 0)}", if (a.dentroDeBanda) Palette.green else Palette.amber)
                        Text(Fmt.pct(a.desviacionPct), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (a.desviacionPct > 0) neg else pos)
                    }
                    Text(
                        "Ajuste a cuenta según las condiciones del contrato; lo que se pague o abone se descuenta de la liquidación final.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            val g = r.gastos
            SectionCard(
                title = "Otros gastos",
                subtitle = "Peajes, parking, lavado… no incluidos en el contrato",
                icon = Icons.Default.Wallet,
                accent = Palette.amber,
            ) {
                if (g.lista.isEmpty()) {
                    Text("Sin gastos anotados. Añádelos en la pestaña Repostajes → Otros gastos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    g.porCategoria.entries.sortedByDescending { it.value }.forEach { (cat, total) -> StatRow(cat.label, Fmt.eur(total)) }
                    ThinDivider()
                    StatRow("Total otros gastos", Fmt.eur(g.total), emphasized = true, valueColor = accentText(Palette.amber))
                }
                ThinDivider()
                StatRow("Coste de uso hasta hoy", Fmt.eur(g.costeUsoHastaHoy), emphasized = true,
                    hint = "Cuotas devengadas ${Fmt.eur(g.cuotasDevengadas)} + combustible + otros gastos")
            }
        }

        item {
            val c = r.coste
            SectionCard(title = "Coste total del contrato", subtitle = "Proyección a ${Fmt.date(p.fin)}", icon = Icons.Default.Paid, accent = Palette.pink) {
                StatRow("Cuotas (${p.meses} × ${Fmt.eur(p.cuotaMensual)})", Fmt.eur(c.cuotas))
                StatRow("Cuota irregular inicial", Fmt.eur(c.cuotaIrregular), hint = "Prorrateo desde ${Fmt.date(p.inicio)}")
                StatRow("Combustible proyectado", Fmt.eur(c.combustibleProyectado))
                StatRow(
                    if (c.abono >= 0) "Abono km no recorridos" else "Cargo km de exceso",
                    (if (c.abono >= 0) "−" else "+") + Fmt.eur(kotlin.math.abs(c.abono)),
                )
                ThinDivider()
                StatRow("TOTAL", Fmt.eur(c.total), emphasized = true, valueColor = accentText(Palette.pink))
                StatRow("Depósito en garantía", Fmt.eur(c.deposito), hint = "Recuperable, no es coste")
            }
        }

        item {
            val c = r.coste
            SectionCard(
                title = "IVA y deducción",
                subtitle = "IVA ${Fmt.pct(p.tipoIva, 0)} · deducción aplicada ${Fmt.pct(p.pctDeduccion, 0)}",
                icon = Icons.Default.Receipt,
                accent = Palette.teal,
            ) {
                StatRow("Base cuotas", Fmt.eur(c.baseCuotas))
                StatRow("IVA cuotas", Fmt.eur(c.ivaCuotas))
                StatRow("IVA cuota irregular", Fmt.eur(c.ivaCuotaIrregular))
                StatRow("IVA combustible", Fmt.eur(c.ivaCombustible), hint = "Incluido en surtidor")
                ThinDivider()
                StatRow("IVA total soportado", Fmt.eur(c.ivaTotal))
                StatRow("IVA deducible", Fmt.eur(c.ivaDeducible))
                StatRow("Coste neto tras deducción", Fmt.eur(c.costeNeto), emphasized = true, valueColor = accentText(Palette.teal))
            }
        }

        item {
            SectionCard(title = "Contrato", subtitle = p.vehiculo, icon = Icons.Default.DirectionsCar, accent = Palette.indigo) {
                if (p.empresa.isNotBlank()) StatRow("Compañía", p.empresa)
                StatRow("Nº contrato", p.contrato)
                StatRow("Matrícula", p.matricula)
                StatRow("Puesta a disposición", Fmt.date(p.inicio))
                StatRow("Fin de contrato", Fmt.date(p.fin))
                StatRow("Plazo", "${p.meses} meses")
                StatRow("Km contratados", Fmt.km(p.kmContratados), hint = "${Fmt.int(p.kmAnio.toDouble())} km/año")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniStat("Cuota IVA incl.", Fmt.eur(p.cuotaMensual))
                    MiniStat("Cuota sin IVA", Fmt.eur(p.cuotaSinIva))
                    MiniStat("Rep. daños", Fmt.eur(p.repDanos))
                }
            }
        }
    }
}

/** Cabecera con degradado: km actuales y barras de progreso de km y de tiempo. */
@Composable
private fun HeroCard(r: Resultado) {
    val s = r.seguimiento
    val p = r.params
    val fraccionKm = if (p.kmContratados > 0) (s.kmUltima / p.kmContratados).toFloat().coerceIn(0f, 1f) else 0f
    val fraccionTiempo = (s.diasTranscurridos.toFloat() / s.diasContrato).coerceIn(0f, 1f)
    val white = Color.White
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Palette.blue, Palette.teal)), RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Column {
            Text("Última medida · ${Fmt.date(s.fechaUltima)}", style = MaterialTheme.typography.labelLarge, color = white.copy(alpha = 0.85f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(
                    Fmt.km(s.kmUltima),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = white,
                )
                val meses = s.diasRestantes / 30.4375
                Text(
                    if (s.diasRestantes > 0) "quedan ${Fmt.int(s.diasRestantes)} días\n(${Fmt.dec(meses, 1)} meses)" else "contrato\nfinalizado",
                    style = MaterialTheme.typography.labelMedium, color = white.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            ProgressLine("Kilómetros", "${Fmt.pct(fraccionKm.toDouble())} de ${Fmt.km(p.kmContratados)}", fraccionKm, Palette.amber)
            Spacer(Modifier.height(8.dp))
            ProgressLine("Tiempo", "${Fmt.pct(fraccionTiempo.toDouble())} · día ${Fmt.int(s.diasTranscurridos)} de ${Fmt.int(s.diasContrato)}", fraccionTiempo, white)
        }
    }
}

@Composable
private fun ProgressLine(label: String, value: String, fraction: Float, color: Color) {
    val white = Color.White
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = white.copy(alpha = 0.9f))
        Text(value, style = MaterialTheme.typography.labelMedium, color = white, fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(4.dp))
    LinearProgressIndicator(
        progress = { fraction },
        modifier = Modifier.fillMaxWidth().height(10.dp),
        color = color,
        trackColor = white.copy(alpha = 0.25f),
        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
    )
}
