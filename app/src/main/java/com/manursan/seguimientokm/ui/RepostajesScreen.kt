package com.manursan.seguimientokm.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.manursan.seguimientokm.Expense
import com.manursan.seguimientokm.ExpenseCategory
import com.manursan.seguimientokm.Fmt
import com.manursan.seguimientokm.FuelPrices
import com.manursan.seguimientokm.MainViewModel
import com.manursan.seguimientokm.RefuelRow
import com.manursan.seguimientokm.Resultado
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class GastoSeccion(val label: String) { Repostajes("Repostajes"), Otros("Otros gastos") }

data class RefuelEdit(
    val id: String?, val fecha: LocalDate, val importe: String, val nota: String,
    val litros: String = "", val precio: String = "", val mercado: Boolean = false,
)

data class ExpenseEdit(val id: String?, val fecha: LocalDate, val categoria: ExpenseCategory, val importe: String, val nota: String)

@Composable
fun RepostajesScreen(
    r: Resultado,
    vm: MainViewModel,
    padding: PaddingValues,
    seccion: GastoSeccion,
    onSeccion: (GastoSeccion) -> Unit,
    edit: RefuelEdit?,
    onEdit: (RefuelEdit?) -> Unit,
    expenseEdit: ExpenseEdit?,
    onExpenseEdit: (ExpenseEdit?) -> Unit,
) {
    if (!r.params.configurado) {
        Column(Modifier.fillMaxSize().padding(padding)) {
            EmptyHint("Contrato sin configurar", "Rellena los datos del contrato en Ajustes (fechas, plazo y km/año) para poder anotar repostajes y gastos.", Icons.Default.DirectionsCar, Palette.indigo)
        }
        return
    }
    Column(Modifier.fillMaxSize().padding(padding)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            GastoSeccion.entries.forEachIndexed { i, sec ->
                SegmentedButton(
                    selected = seccion == sec,
                    onClick = { onSeccion(sec) },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = GastoSeccion.entries.size),
                ) { Text(sec.label) }
            }
        }
        when (seccion) {
            GastoSeccion.Repostajes -> RefuelList(r, onEdit)
            GastoSeccion.Otros -> ExpenseList(r, onExpenseEdit)
        }
    }

    if (edit != null) {
        RefuelDialog(
            edit = edit, vm = vm,
            onDismiss = { onEdit(null) },
            onSave = { fecha, importe, nota, litros, precio, mercado ->
                if (edit.id == null) vm.addRefuel(fecha, importe, nota, litros, precio, mercado)
                else vm.updateRefuel(edit.id, fecha, importe, nota, litros, precio, mercado)
                onEdit(null)
            },
            onDelete = if (edit.id != null) ({ vm.deleteRefuel(edit.id); onEdit(null) }) else null,
        )
    }
    if (expenseEdit != null) {
        ExpenseDialog(
            edit = expenseEdit,
            onDismiss = { onExpenseEdit(null) },
            onSave = { fecha, cat, importe, nota ->
                if (expenseEdit.id == null) vm.addExpense(fecha, cat, importe, nota) else vm.updateExpense(expenseEdit.id, fecha, cat, importe, nota)
                onExpenseEdit(null)
            },
            onDelete = if (expenseEdit.id != null) ({ vm.deleteExpense(expenseEdit.id); onExpenseEdit(null) }) else null,
        )
    }
}

@Composable
private fun RefuelList(r: Resultado, onEdit: (RefuelEdit) -> Unit) {
    val rows = r.repostajes.asReversed()
    if (rows.isEmpty()) {
        EmptyHint("Sin repostajes", "Pulsa + para anotar un repostaje.", Icons.Default.LocalGasStation, Palette.orange)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            val total = r.repostajes.lastOrNull()?.acumulado ?: 0.0
            val cons = r.consumo
            Text(
                "${rows.size} repostajes · total ${Fmt.eur(total)}" + (cons.litros100km?.let { " · ${Fmt.dec(it, 2)} l/100 km" } ?: ""),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        items(rows, key = { it.refuel.id }) { row ->
            RefuelCard(row) {
                val f = row.refuel
                onEdit(RefuelEdit(
                    f.id, f.fecha, Fmt.dec(f.importe, 2).replace(".", ""), f.nota,
                    litros = f.litros?.let { Fmt.dec(it, 2) } ?: "",
                    precio = f.precioLitro?.let { Fmt.dec(it, 3) } ?: "",
                    mercado = f.precioMercado,
                ))
            }
        }
    }
}

/** Estado vacío: icono grande, título y texto centrados. */
@Composable
fun EmptyHint(
    title: String,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Info,
    accent: androidx.compose.ui.graphics.Color = Palette.blue,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconBadge(icon, accent, size = 72.dp)
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = accentText(accent))
        Spacer(Modifier.height(10.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun RefuelCard(row: RefuelRow, onClick: () -> Unit) {
    val accent = Palette.orange
    val f = row.refuel
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconBadge(Icons.Default.LocalGasStation, accent, size = 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(Fmt.date(f.fecha), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        if (f.nota.isNotBlank()) {
                            Text(f.nota, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
                Text(Fmt.eur(f.importe), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = accentText(accent), maxLines = 1, softWrap = false)
            }
            Spacer(Modifier.height(8.dp))
            // Cuatro columnas de igual ancho: nunca se recortan
            Row(Modifier.fillMaxWidth()) {
                MiniStat("Acumulado", Fmt.eur(row.acumulado), Modifier.weight(1f))
                MiniStat("Litros", f.litrosEfectivos?.let { Fmt.dec(it, 1) + " l" } ?: "—", Modifier.weight(0.8f))
                MiniStat(
                    if (f.precioLitro == null) "€/l" else if (f.precioMercado) "€/l mercado" else "€/l manual",
                    f.precioLitro?.let { Fmt.dec(it, 3) } ?: "—",
                    Modifier.weight(1f),
                    valueColor = if (f.precioLitro != null) accentText(accent) else androidx.compose.ui.graphics.Color.Unspecified,
                )
                MiniStat("Coste diario", row.costeDiario?.let { Fmt.eur(it) } ?: "—", Modifier.weight(1f))
            }
        }
    }
}

private enum class PrecioModo(val label: String) { Manual("Manual"), Mercado("Mercado") }

@Composable
private fun RefuelDialog(
    edit: RefuelEdit,
    vm: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (LocalDate, Double, String, Double?, Double?, Boolean) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var fecha by remember { mutableStateOf(edit.fecha) }
    var importeText by remember { mutableStateOf(edit.importe) }
    var nota by remember { mutableStateOf(edit.nota) }
    var litrosText by remember { mutableStateOf(edit.litros) }
    var precioText by remember { mutableStateOf(edit.precio) }
    var precioModo by remember { mutableStateOf(if (edit.mercado || edit.precio.isBlank()) PrecioModo.Mercado else PrecioModo.Manual) }
    var mercadoPrecio by remember { mutableStateOf<Double?>(if (edit.mercado) Fmt.parseDouble(edit.precio) else null) }
    var mercadoError by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Precio de mercado: se consulta al abrir en modo mercado y cada vez que cambia la fecha.
    // Si el usuario cambia a manual a mitad de descarga, el efecto se cancela: el finally limpia el estado.
    LaunchedEffect(fecha, precioModo) {
        if (precioModo == PrecioModo.Mercado) {
            cargando = true; mercadoError = null
            try {
                vm.precioMercado(fecha).onSuccess { mercadoPrecio = it }.onFailure { mercadoPrecio = null; mercadoError = it.message ?: "Sin conexión" }
            } finally {
                cargando = false
            }
        } else {
            cargando = false
        }
    }

    val precio: Double? = when (precioModo) {
        PrecioModo.Manual -> Fmt.parseDouble(precioText)?.takeIf { it > 0 }
        PrecioModo.Mercado -> mercadoPrecio
    }
    val litros = Fmt.parseDouble(litrosText)?.takeIf { it > 0 }
    // Importe: el escrito o, si se deja vacío, litros × precio
    val importeEscrito = Fmt.parseDouble(importeText)
    val importeCalc = if (importeText.isBlank() && litros != null && precio != null) litros * precio else null
    val importe = importeEscrito ?: importeCalc
    val error = when {
        importeText.isBlank() -> null
        importeEscrito == null -> "Introduce un importe válido"
        importeEscrito < 0 -> "No puede ser negativo"
        else -> null
    }
    val valido = importe != null && importe >= 0
    val litrosCalc = if (litros == null && importeEscrito != null && precio != null) importeEscrito / precio else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (edit.id == null) "Nuevo repostaje" else "Editar repostaje") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Fecha", fecha, { fecha = it })
                NumberField(
                    "Importe", importeText, { importeText = it }, suffix = "€", isError = error != null,
                    supporting = error ?: importeCalc?.let { "Calculado: ${Fmt.dec(it, 2)} € (litros × precio)" }
                        ?: if (importeText.isBlank()) "Obligatorio, o bien litros y precio por litro" else null,
                )
                Text("Precio por litro", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    PrecioModo.entries.forEachIndexed { i, m ->
                        SegmentedButton(
                            selected = precioModo == m, onClick = { precioModo = m },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = PrecioModo.entries.size),
                        ) { Text(m.label, maxLines = 1, style = MaterialTheme.typography.labelMedium) }
                    }
                }
                when (precioModo) {
                    PrecioModo.Manual -> NumberField("Precio", precioText, { precioText = it }, suffix = "€/l")
                    PrecioModo.Mercado -> Row(verticalAlignment = Alignment.CenterVertically) {
                        if (cargando) {
                            CircularProgressIndicator(Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Consultando precio medio…", style = MaterialTheme.typography.bodySmall)
                        } else if (mercadoPrecio != null) {
                            Column {
                                Text("${Fmt.dec(mercadoPrecio!!, 3)} €/l", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accentText(Palette.orange))
                                Text("Gasolina 95 · media en ${FuelPrices.nombreProvincia(vm.data.params.provinciaId)} el ${Fmt.date(fecha)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Text("No se pudo obtener el precio (${mercadoError ?: "sin datos"}). Se guardará sin precio; podrás completarlo después desde el menú.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                NumberField(
                    "Litros (opcional)", litrosText, { litrosText = it }, suffix = "l",
                    supporting = if (litros == null && litrosCalc != null) "Calculado: ${Fmt.dec(litrosCalc, 2)} l (importe / precio)" else null,
                )
                OutlinedTextField(
                    value = nota, onValueChange = { nota = it },
                    label = { Text("Nota (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valido && !(cargando && precioModo == PrecioModo.Mercado), onClick = {
                onSave(fecha, importe!!, nota.trim(), litros, precio, precioModo == PrecioModo.Mercado && precio != null)
            }) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )

    if (confirmDelete && onDelete != null) {
        ConfirmDeleteDialog(
            text = "¿Eliminar el repostaje del ${Fmt.date(edit.fecha)}?",
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

// ---------------- Otros gastos ----------------

@Composable
private fun ExpenseList(r: Resultado, onEdit: (ExpenseEdit) -> Unit) {
    val rows = r.gastos.lista.asReversed()
    if (rows.isEmpty()) {
        EmptyHint("Sin otros gastos", "Pulsa + para anotar peajes, parking, lavados, multas…", Icons.Default.Wallet, Palette.amber)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "${rows.size} gastos · total ${Fmt.eur(r.gastos.total)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        items(rows, key = { it.id }) { g ->
            ExpenseCard(g) { onEdit(ExpenseEdit(g.id, g.fecha, g.categoria, Fmt.dec(g.importe, 2).replace(".", ""), g.nota)) }
        }
    }
}

@Composable
private fun ExpenseCard(g: Expense, onClick: () -> Unit) {
    val accent = Palette.amber
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(Icons.Default.Wallet, accent, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(g.categoria.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(Fmt.date(g.fecha) + if (g.nota.isNotBlank()) " · ${g.nota}" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(Fmt.eur(g.importe), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = accentText(accent))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDialog(
    edit: ExpenseEdit,
    onDismiss: () -> Unit,
    onSave: (LocalDate, ExpenseCategory, Double, String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var fecha by remember { mutableStateOf(edit.fecha) }
    var categoria by remember { mutableStateOf(edit.categoria) }
    var importeText by remember { mutableStateOf(edit.importe) }
    var nota by remember { mutableStateOf(edit.nota) }
    var catOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val importe = Fmt.parseDouble(importeText)
    val valido = importe != null && importe >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (edit.id == null) "Nuevo gasto" else "Editar gasto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Fecha", fecha, { fecha = it })
                ExposedDropdownMenuBox(expanded = catOpen, onExpandedChange = { catOpen = it }) {
                    OutlinedTextField(
                        value = categoria.label, onValueChange = {}, readOnly = true, label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catOpen) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    DropdownMenu(expanded = catOpen, onDismissRequest = { catOpen = false }) {
                        ExpenseCategory.entries.forEach { c ->
                            DropdownMenuItem(text = { Text(c.label) }, onClick = { categoria = c; catOpen = false })
                        }
                    }
                }
                NumberField("Importe", importeText, { importeText = it }, suffix = "€", isError = importeText.isNotBlank() && !valido)
                OutlinedTextField(value = nota, onValueChange = { nota = it }, label = { Text("Nota (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = valido, onClick = { onSave(fecha, categoria, importe!!, nota.trim()) }) { Text("Guardar") } },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = { confirmDelete = true }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
    if (confirmDelete && onDelete != null) {
        ConfirmDeleteDialog("¿Eliminar este gasto?", onConfirm = { confirmDelete = false; onDelete() }, onDismiss = { confirmDelete = false })
    }
}
