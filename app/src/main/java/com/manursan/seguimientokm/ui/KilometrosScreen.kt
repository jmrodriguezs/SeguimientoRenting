package com.manursan.seguimientokm.ui

import androidx.compose.foundation.clickable
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.manursan.seguimientokm.Photos
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.manursan.seguimientokm.Fmt
import com.manursan.seguimientokm.KmRow
import com.manursan.seguimientokm.MainViewModel
import com.manursan.seguimientokm.Resultado
import java.time.LocalDate

/** Estado del diálogo de alta/edición de una medición. */
data class MeasurementEdit(val id: String?, val fecha: LocalDate, val km: String, val nota: String, val foto: String? = null)

@Composable
fun KilometrosScreen(
    r: Resultado,
    vm: MainViewModel,
    padding: PaddingValues,
    edit: MeasurementEdit?,
    onEdit: (MeasurementEdit?) -> Unit,
) {
    val rows = r.reales.asReversed()
    val pos = positiveColor()
    val neg = negativeColor()
    var verFoto by remember { mutableStateOf<String?>(null) }

    if (!r.params.configurado) {
        Column(Modifier.fillMaxSize().padding(padding)) {
            EmptyHint("Contrato sin configurar", "Rellena los datos del contrato en Ajustes (fechas, plazo y km/año) para poder anotar mediciones.", Icons.Default.DirectionsCar, Palette.indigo)
        }
    } else if (rows.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(padding)) {
            EmptyHint("Sin mediciones", "Pulsa + para anotar los kilómetros del cuentakilómetros.", Icons.Default.Speed, Palette.green)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "${rows.size} mediciones · km/día teórico ${Fmt.dec(r.seguimiento.kmDiaTeoricos, 2)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            items(rows, key = { it.id ?: it.fecha.toString() }) { row ->
                val foto = vm.data.measurements.firstOrNull { it.id == row.id }?.foto
                KmRowCard(row, pos, neg, foto, onPhoto = { verFoto = it }) {
                    onEdit(MeasurementEdit(row.id, row.fecha, row.km.toInt().toString(), row.nota, foto))
                }
            }
        }
    }

    if (edit != null) {
        MeasurementDialog(
            edit = edit,
            vm = vm,
            existing = r.reales,
            onDismiss = { onEdit(null) },
            onSave = { fecha, km, nota, foto ->
                if (edit.id == null) vm.addMeasurement(fecha, km, nota, foto) else vm.updateMeasurement(edit.id, fecha, km, nota, foto)
                onEdit(null)
            },
            onDelete = if (edit.id != null) ({ vm.deleteMeasurement(edit.id); onEdit(null) }) else null,
        )
    }
    verFoto?.let { name -> PhotoViewer(name) { verFoto = null } }
}

@Composable
private fun KmRowCard(
    row: KmRow, pos: androidx.compose.ui.graphics.Color, neg: androidx.compose.ui.graphics.Color,
    foto: String?, onPhoto: (String) -> Unit, onClick: () -> Unit,
) {
    val accent = Palette.green
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            // Franja lateral de color
            Box(Modifier.width(6.dp).fillMaxHeight().background(accent))
            Column(Modifier.padding(start = 14.dp, end = 16.dp, top = 12.dp, bottom = 12.dp).weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Default.Speed, accent, size = 30.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(Fmt.date(row.fecha), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Text("Día ${Fmt.int(row.dias)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Fmt.km(row.km), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = accentText(accent))
                        if (foto != null) {
                            Spacer(Modifier.width(8.dp))
                            PhotoImage(foto, Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).clickable { onPhoto(foto) }, maxPx = 120)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill("${Fmt.signed(row.desviacion, 0)} km · ${Fmt.pct(row.desviacionPct)}", if (row.desviacion > 0) Palette.red else Palette.green)
                    Pill("${Fmt.dec(row.kmDia, 1)} km/día", Palette.blue)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniStat("Teóricos", Fmt.km(row.teoricos), Modifier.weight(1f))
                    MiniStat("Gasolina acum.", Fmt.eur(row.gastoGasolina), Modifier.weight(1f), valueColor = accentText(Palette.orange))
                    MiniStat("Coste/km", Fmt.eur(row.costeKmTotal, 4), Modifier.weight(1f))
                }
                if (row.nota.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(row.nota, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MeasurementDialog(
    edit: MeasurementEdit,
    vm: MainViewModel,
    existing: List<KmRow>,
    onDismiss: () -> Unit,
    onSave: (LocalDate, Int, String, String?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var fecha by remember { mutableStateOf(edit.fecha) }
    var kmText by remember { mutableStateOf(edit.km) }
    var nota by remember { mutableStateOf(edit.nota) }
    var foto by remember { mutableStateOf(edit.foto) }
    var confirmDelete by remember { mutableStateOf(false) }
    var importando by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Fotos nuevas que se descartan si se cancela el diálogo
    val nuevas = remember { mutableListOf<String>() }
    fun setFoto(name: String?) { foto = name; if (name != null) nuevas += name }
    fun cancelar() { nuevas.forEach { Photos.delete(context, it) }; onDismiss() }
    fun importar(uri: Uri?) {
        uri ?: return
        importando = true
        scope.launch { vm.importPhoto(uri)?.let { setFoto(it) }; importando = false }
    }
    val captureUri = remember {
        FileProvider.getUriForFile(context, "com.manursan.seguimientokm.fileprovider", Photos.tempCaptureFile(context))
    }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) importar(captureUri) }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { importar(it) }

    val km = Fmt.parseInt(kmText)
    // Aviso (no bloqueo) si los km no crecen respecto a la medida anterior en fecha
    val anterior = existing.filter { it.id != edit.id && it.fecha.isBefore(fecha) }.maxByOrNull { it.fecha }
    val warning = when {
        kmText.isBlank() -> null
        km == null -> "Introduce un número entero"
        km < 0 -> "No puede ser negativo"
        anterior != null && km < anterior.km -> "Menor que la medida del ${Fmt.date(anterior.fecha)} (${Fmt.int(anterior.km)} km)"
        else -> null
    }

    AlertDialog(
        onDismissRequest = ::cancelar,
        title = { Text(if (edit.id == null) "Nueva medición" else "Editar medición") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Fecha", fecha, { fecha = it })
                NumberField(
                    "Km del cuentakilómetros", kmText, { kmText = it },
                    suffix = "km", decimal = false,
                    isError = kmText.isNotBlank() && (km == null || km < 0),
                    supporting = warning,
                )
                OutlinedTextField(
                    value = nota, onValueChange = { nota = it },
                    label = { Text("Nota (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Text("Foto del cuentakilómetros (opcional)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (foto != null) {
                        PhotoImage(foto!!, Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)), maxPx = 200)
                        TextButton(onClick = { setFoto(null) }) { Text("Quitar") }
                    } else if (importando) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        OutlinedButton(onClick = { camara.launch(captureUri) }) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Cámara")
                        }
                        OutlinedButton(onClick = { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                            Icon(Icons.Default.Image, contentDescription = null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Galería")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = km != null && km >= 0 && !importando, onClick = {
                // Las fotos importadas y luego descartadas se borran; la elegida se conserva
                nuevas.filter { it != foto }.forEach { Photos.delete(context, it) }
                onSave(fecha, km!!, nota.trim(), foto)
            }) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = ::cancelar) { Text("Cancelar") }
            }
        },
    )

    if (confirmDelete && onDelete != null) {
        ConfirmDeleteDialog(
            text = "¿Eliminar la medición del ${Fmt.date(edit.fecha)}?",
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Foto a pantalla completa. */
@Composable
fun PhotoViewer(name: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black).clickable(onClick = onDismiss)) {
            PhotoImage(name, Modifier.fillMaxSize(), maxPx = 1600, contentScale = ContentScale.Fit)
        }
    }
}

@Composable
fun ConfirmDeleteDialog(text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar") },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
