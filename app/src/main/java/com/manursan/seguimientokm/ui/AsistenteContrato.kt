package com.manursan.seguimientokm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.manursan.seguimientokm.ContractParams
import com.manursan.seguimientokm.Fmt
import com.manursan.seguimientokm.FuelPrices
import java.time.LocalDate

/**
 * Alta guiada del contrato: pide los datos en seis pasos cortos, valida cada uno por separado
 * y solo guarda al final. Pensado para quien abre la aplicación por primera vez y se encuentra
 * el formulario completo de Ajustes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsistenteContratoDialog(
    inicial: ContractParams,
    onDismiss: () -> Unit,
    onFinish: (ContractParams) -> Unit,
) {
    val hoy = remember { LocalDate.now() }
    var paso by remember { mutableStateOf(0) }

    // 1. Vehículo
    var contrato by remember { mutableStateOf(inicial.contrato) }
    var vehiculo by remember { mutableStateOf(inicial.vehiculo.ifBlank { ContractParams.VEHICULO_POR_DEFECTO }) }
    var matricula by remember { mutableStateOf(inicial.matricula) }
    // 2. Compañía
    var empresa by remember { mutableStateOf(inicial.empresa) }
    var telefono1 by remember { mutableStateOf(inicial.telefono1) }
    var telefono2 by remember { mutableStateOf(inicial.telefono2) }
    var email by remember { mutableStateOf(inicial.email) }
    // 3. Plazo y kilómetros
    var inicio by remember { mutableStateOf(if (inicial.configurado) inicial.inicio else hoy) }
    var meses by remember { mutableStateOf(if (inicial.configurado) inicial.meses.toString() else "36") }
    var fin by remember { mutableStateOf(if (inicial.configurado) inicial.fin else hoy.plusMonths(36)) }
    var finTocado by remember { mutableStateOf(inicial.configurado) }
    var kmAnio by remember { mutableStateOf(if (inicial.kmAnio > 0) inicial.kmAnio.toString() else "") }
    // 4. Cuotas
    var cuota by remember { mutableStateOf(if (inicial.cuotaMensual > 0) Fmt.dec(inicial.cuotaMensual, 2) else "") }
    var cuotaSinIva by remember { mutableStateOf(if (inicial.cuotaSinIva > 0) Fmt.dec(inicial.cuotaSinIva, 2) else "") }
    var repDanos by remember { mutableStateOf(if (inicial.repDanos > 0) Fmt.dec(inicial.repDanos, 2) else "") }
    var deposito by remember { mutableStateOf(if (inicial.deposito > 0) Fmt.dec(inicial.deposito, 2) else "") }
    // 5. Liquidación
    var eurNoRec by remember { mutableStateOf(if (inicial.eurKmNoRecorrido > 0) Fmt.dec(inicial.eurKmNoRecorrido, 4) else "") }
    var eurExceso by remember { mutableStateOf(if (inicial.eurKmExceso > 0) Fmt.dec(inicial.eurKmExceso, 4) else "") }
    var umbralLiq by remember { mutableStateOf(Fmt.dec(inicial.umbralLiquidacion * 100, 0)) }
    var umbralAj by remember { mutableStateOf(Fmt.dec(inicial.umbralAjuste * 100, 0)) }
    var recargo by remember { mutableStateOf(Fmt.dec(inicial.recargoExceso, 2)) }
    // 6. Combustible e IVA
    var combustibleId by remember { mutableStateOf(inicial.combustibleId) }
    var provinciaId by remember { mutableStateOf(inicial.provinciaId) }
    var iva by remember { mutableStateOf(Fmt.dec(inicial.tipoIva * 100, 0)) }
    var deduccion by remember { mutableStateOf(Fmt.dec(inicial.pctDeduccion * 100, 0)) }

    fun d(s: String) = Fmt.parseDouble(s)
    val mesesInt = Fmt.parseInt(meses)
    val ivaFrac = d(iva)?.div(100)

    // Al cambiar inicio o plazo se recalcula la fecha de fin mientras el usuario no la haya tocado
    fun recalcularFin() {
        val m = Fmt.parseInt(meses)
        if (!finTocado && m != null && m > 0) fin = inicio.plusMonths(m.toLong())
    }

    val candidato: ContractParams? = runCatching {
        ContractParams(
            contrato = contrato.trim(),
            vehiculo = vehiculo.trim().ifBlank { ContractParams.VEHICULO_POR_DEFECTO },
            matricula = matricula.trim().uppercase().replace(" ", "").replace("-", ""),
            empresa = empresa.trim(), telefono1 = telefono1.trim(), telefono2 = telefono2.trim(), email = email.trim(),
            inicio = inicio, fin = fin,
            meses = mesesInt!!.also { require(it > 0) },
            kmAnio = Fmt.parseInt(kmAnio)!!.also { require(it > 0) },
            cuotaMensual = d(cuota) ?: 0.0,
            cuotaSinIva = d(cuotaSinIva) ?: ((d(cuota) ?: 0.0) / (1 + (ivaFrac ?: 0.0))),
            repDanos = d(repDanos) ?: 0.0,
            deposito = d(deposito) ?: 0.0,
            eurKmNoRecorrido = d(eurNoRec) ?: 0.0,
            eurKmExceso = d(eurExceso) ?: 0.0,
            recargoExceso = d(recargo)!!,
            umbralLiquidacion = d(umbralLiq)!! / 100,
            umbralAjuste = d(umbralAj)!! / 100,
            modoProyeccion = inicial.modoProyeccion,
            kmDiaProyeccion = inicial.kmDiaProyeccion,
            provinciaId = provinciaId,
            combustibleId = combustibleId,
            tipoIva = d(iva)!! / 100,
            pctDeduccion = d(deduccion)!! / 100,
        )
    }.getOrNull()

    // Qué falta en el paso actual (vacío = se puede continuar)
    val pendiente: String? = when (paso) {
        0 -> null // todo opcional
        1 -> null
        2 -> when {
            mesesInt == null || mesesInt <= 0 -> "Indica el plazo en meses."
            !fin.isAfter(inicio) -> "La fecha de fin debe ser posterior a la de inicio."
            kotlin.math.abs(java.time.temporal.ChronoUnit.MONTHS.between(inicio, fin) - mesesInt) > 1 ->
                "El plazo no coincide con las fechas: entre ellas hay ${java.time.temporal.ChronoUnit.MONTHS.between(inicio, fin)} meses."
            Fmt.parseInt(kmAnio).let { it == null || it <= 0 } -> "Indica los kilómetros contratados al año."
            else -> null
        }
        3 -> {
            val sinIva = d(cuotaSinIva) ?: d(cuota)?.div(1 + (ivaFrac ?: 0.0))
            when {
                cuota.isNotBlank() && d(cuota) == null -> "La cuota mensual no es un número válido."
                d(cuotaSinIva) != null && d(cuotaSinIva)!! > d(cuota)!! -> "La cuota sin IVA no puede superar la cuota con IVA."
                ivaFrac != null && d(cuotaSinIva) != null && d(cuotaSinIva)!! > 0 && kotlin.math.abs(d(cuotaSinIva)!! * (1 + ivaFrac) - d(cuota)!!) > 1.0 ->
                    "Cuota sin IVA × (1 + IVA) = ${Fmt.dec(d(cuotaSinIva)!! * (1 + ivaFrac), 2)} €, no cuadra con la cuota con IVA."
                repDanos.isNotBlank() && d(repDanos) == null -> "La reparación de daños no es un número válido."
                deposito.isNotBlank() && d(deposito) == null -> "El depósito no es un número válido."
                d(repDanos) != null && sinIva != null && d(repDanos)!! > sinIva -> "La reparación de daños no puede superar la cuota sin IVA."
                else -> null
            }
        }
        4 -> when {
            eurNoRec.isNotBlank() && d(eurNoRec).let { it == null || it < 0 } -> "El abono por kilómetro no es válido."
            eurExceso.isNotBlank() && d(eurExceso).let { it == null || it < 0 } -> "El cargo por kilómetro no es válido."
            d(recargo).let { it == null || it < 1 } -> "El recargo debe ser 1 o mayor (1 = sin recargo)."
            d(umbralLiq).let { it == null || it !in 0.0..100.0 } || d(umbralAj).let { it == null || it !in 0.0..100.0 } ->
                "Los umbrales se expresan en porcentaje (0 a 100)."
            else -> null
        }
        else -> when {
            d(iva).let { it == null || it !in 0.0..100.0 } -> "El tipo de IVA debe estar entre 0 y 100."
            d(deduccion).let { it == null || it !in 0.0..100.0 } -> "El porcentaje de deducción debe estar entre 0 y 100."
            candidato == null -> "Faltan datos obligatorios en pasos anteriores."
            candidato.errores().isNotEmpty() -> candidato.errores().first()
            else -> null
        }
    }

    val titulos = listOf("Vehículo", "Compañía de renting", "Plazo y kilómetros", "Cuotas", "Liquidación de kilómetros", "Combustible e IVA")
    val ultimo = paso == titulos.lastIndex

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Configurar paso a paso", fontWeight = FontWeight.Bold)
                Text("${paso + 1} de ${titulos.size} · ${titulos[paso]}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { (paso + 1f) / titulos.size }, modifier = Modifier.fillMaxWidth())
            }
        },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (paso) {
                    0 -> {
                        Ayuda("Datos identificativos del vehículo y del contrato. Puedes dejarlos en blanco y rellenarlos más adelante.")
                        Recomendacion()
                        OutlinedTextField(contrato, { contrato = it }, label = { Text("Nº de contrato") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(vehiculo, { vehiculo = it }, label = { Text("Vehículo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(matricula, { matricula = it }, label = { Text("Matrícula") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    1 -> {
                        Ayuda("Con estos datos podrás llamar o escribir a tu compañía desde Ajustes con un toque. También son opcionales.")
                        OutlinedTextField(empresa, { empresa = it }, label = { Text("Compañía de renting") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(telefono1, { telefono1 = it }, label = { Text("Teléfono de atención al cliente") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(telefono2, { telefono2 = it }, label = { Text("Otro teléfono") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(email, { email = it }, label = { Text("Correo de contacto") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    2 -> {
                        Ayuda("Fechas y kilómetros del contrato. La fecha de fin se calcula sola a partir del plazo; puedes cambiarla si tu contrato indica otra.")
                        DateField("Inicio (puesta a disposición)", inicio, { inicio = it; recalcularFin() }, Modifier.fillMaxWidth())
                        NumberField("Plazo", meses, { meses = it; recalcularFin() }, Modifier.fillMaxWidth(), suffix = "meses", decimal = false)
                        DateField("Fin de contrato", fin, { fin = it; finTocado = true }, Modifier.fillMaxWidth())
                        NumberField("Kilómetros al año", kmAnio, { kmAnio = it }, Modifier.fillMaxWidth(), suffix = "km", decimal = false)
                        val total = Fmt.parseInt(kmAnio)?.let { k -> mesesInt?.let { m -> k.toDouble() / 12 * m } }
                        if (total != null) Resumen("Kilómetros contratados: ${Fmt.km(total)}")
                    }
                    3 -> {
                        Ayuda("Todos estos importes son opcionales: si los dejas vacíos, la aplicación sigue el contrato pero no calcula el coste por kilómetro, el coste total ni el IVA. La cuota sin IVA se deduce de la cuota con IVA.")
                        NumberField("Cuota mensual con IVA (opcional)", cuota, {
                            cuota = it
                            val c = d(it); val v = ivaFrac
                            if (c != null && v != null && v >= 0) cuotaSinIva = Fmt.dec(c / (1 + v), 2)
                        }, Modifier.fillMaxWidth(), suffix = "€")
                        NumberField("Cuota mensual sin IVA (opcional)", cuotaSinIva, { cuotaSinIva = it }, Modifier.fillMaxWidth(), suffix = "€",
                            supporting = "Calculada a partir de la cuota con IVA")
                        NumberField("Reparación de daños incluida (opcional)", repDanos, { repDanos = it }, Modifier.fillMaxWidth(), suffix = "€",
                            supporting = "Parte de la cuota que la compañía destina a daños; déjalo vacío si tu factura no lo detalla")
                        NumberField("Depósito o fianza (opcional)", deposito, { deposito = it }, Modifier.fillMaxWidth(), suffix = "€")
                    }
                    4 -> {
                        Ayuda("Condiciones de la liquidación final que figuran en tu contrato. También son opcionales: sin ellas no se estiman el abono ni el cargo por kilómetros.")
                        NumberField("Abono por km no recorrido (opcional)", eurNoRec, { eurNoRec = it }, Modifier.fillMaxWidth(), suffix = "€/km")
                        NumberField("Cargo por km de exceso (opcional)", eurExceso, { eurExceso = it }, Modifier.fillMaxWidth(), suffix = "€/km")
                        NumberField("Umbral de abono", umbralLiq, { umbralLiq = it }, Modifier.fillMaxWidth(), suffix = "%",
                            supporting = "Solo hay abono si recorres menos de este porcentaje de los km contratados")
                        NumberField("Umbral de recargo", umbralAj, { umbralAj = it }, Modifier.fillMaxWidth(), suffix = "%",
                            supporting = "Porcentaje de exceso a partir del cual se aplica el recargo")
                        NumberField("Recargo si se supera el umbral", recargo, { recargo = it }, Modifier.fillMaxWidth(), suffix = "×",
                            supporting = "1 = sin recargo; 1,25 = un 25 % más caro")
                    }
                    else -> {
                        Ayuda("Combustible y provincia para consultar el precio medio de mercado, e IVA para el coste del contrato.")
                        var combOpen by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = combOpen, onExpandedChange = { combOpen = it }) {
                            OutlinedTextField(
                                value = FuelPrices.nombreCombustible(combustibleId), onValueChange = {}, readOnly = true,
                                label = { Text("Combustible") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = combOpen) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                            )
                            DropdownMenu(expanded = combOpen, onDismissRequest = { combOpen = false }, modifier = Modifier.heightIn(max = 320.dp)) {
                                FuelPrices.COMBUSTIBLES.forEach { (id, nombre) ->
                                    DropdownMenuItem(text = { Text(nombre) }, onClick = { combustibleId = id; combOpen = false })
                                }
                            }
                        }
                        var provOpen by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = provOpen, onExpandedChange = { provOpen = it }) {
                            OutlinedTextField(
                                value = FuelPrices.nombreProvincia(provinciaId), onValueChange = {}, readOnly = true,
                                label = { Text("Provincia") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provOpen) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                            )
                            DropdownMenu(expanded = provOpen, onDismissRequest = { provOpen = false }, modifier = Modifier.heightIn(max = 320.dp)) {
                                DropdownMenuItem(text = { Text("Toda España") }, onClick = { provinciaId = null; provOpen = false })
                                FuelPrices.PROVINCIAS.forEach { (id, nombre) ->
                                    DropdownMenuItem(text = { Text(nombre) }, onClick = { provinciaId = id; provOpen = false })
                                }
                            }
                        }
                        Row {
                            NumberField("Tipo de IVA", iva, { iva = it }, Modifier.weight(1f), suffix = "%")
                            Spacer(Modifier.width(8.dp))
                            NumberField("% deducción", deduccion, { deduccion = it }, Modifier.weight(1f), suffix = "%",
                                supporting = "0 si no deduces IVA")
                        }
                        Recomendacion()
                        if (pendiente == null && candidato != null) {
                            Resumen(
                                "Todo listo: ${Fmt.km(candidato.kmContratados)} en ${candidato.meses} meses" +
                                    (if (candidato.tieneCostes) ", cuota ${Fmt.eur(candidato.cuotaMensual)}/mes" else ", sin cuota (no se calcularán costes)") +
                                    ". Pulsa Guardar para crear el contrato.",
                            )
                        }
                    }
                }
                if (pendiente != null) {
                    Text(pendiente, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = pendiente == null,
                onClick = { if (ultimo) candidato?.let(onFinish) else paso++ },
            ) { Text(if (ultimo) "Guardar" else "Siguiente") }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (paso > 0) TextButton(onClick = { paso-- }) { Text("Atrás") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}

@Composable
private fun Ayuda(texto: String) = Text(
    texto,
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun Resumen(texto: String) = Text(
    texto,
    style = MaterialTheme.typography.bodySmall,
    fontWeight = FontWeight.SemiBold,
    color = accentText(Palette.green),
)

/** Recordatorio de que, aunque casi todo sea opcional, con el contrato completo se aprovecha toda la aplicación. */
@Composable
private fun Recomendacion() = Text(
    "Se recomienda rellenar todos los datos del contrato: cada dato que falte deja sin calcular la parte que depende de él " +
        "(costes, liquidación, IVA…). Siempre puedes completarlos después en Ajustes.",
    style = MaterialTheme.typography.bodySmall,
    color = accentText(Palette.amber),
)
