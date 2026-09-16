package com.manursan.seguimientokm.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import com.manursan.seguimientokm.FinesCheck
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.ButtonDefaults
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import com.manursan.seguimientokm.FuelPrices
import com.manursan.seguimientokm.Reminders
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import com.manursan.seguimientokm.ThemeMode
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.manursan.seguimientokm.ContractParams
import com.manursan.seguimientokm.Fmt
import com.manursan.seguimientokm.MainViewModel

/** Número en formato marcable: solo dígitos y '+'. */
private fun marcable(n: String) = n.filter { it.isDigit() || it == '+' }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParametrosScreen(vm: MainViewModel, padding: PaddingValues, onMessage: (String) -> Unit) {
    val onSaved = { onMessage("Parámetros guardados") }
    val p = vm.data.params

    // Un campo de texto por parámetro; se reinician si cambian los datos guardados
    var contrato by remember(p) { mutableStateOf(p.contrato) }
    var vehiculo by remember(p) { mutableStateOf(p.vehiculo) }
    var matricula by remember(p) { mutableStateOf(p.matricula) }
    var empresa by remember(p) { mutableStateOf(p.empresa) }
    var telefono1 by remember(p) { mutableStateOf(p.telefono1) }
    var telefono2 by remember(p) { mutableStateOf(p.telefono2) }
    var email by remember(p) { mutableStateOf(p.email) }
    var inicio by remember(p) { mutableStateOf(p.inicio) }
    var fin by remember(p) { mutableStateOf(p.fin) }
    var meses by remember(p) { mutableStateOf(p.meses.toString()) }
    var kmAnio by remember(p) { mutableStateOf(p.kmAnio.toString()) }
    var cuota by remember(p) { mutableStateOf(Fmt.dec(p.cuotaMensual, 2)) }
    var cuotaSinIva by remember(p) { mutableStateOf(Fmt.dec(p.cuotaSinIva, 2)) }
    var repDanos by remember(p) { mutableStateOf(Fmt.dec(p.repDanos, 2)) }
    var deposito by remember(p) { mutableStateOf(Fmt.dec(p.deposito, 2)) }
    var eurNoRec by remember(p) { mutableStateOf(Fmt.dec(p.eurKmNoRecorrido, 4)) }
    var eurExceso by remember(p) { mutableStateOf(Fmt.dec(p.eurKmExceso, 4)) }
    var recargo by remember(p) { mutableStateOf(Fmt.dec(p.recargoExceso, 2)) }
    var umbralLiq by remember(p) { mutableStateOf(Fmt.dec(p.umbralLiquidacion * 100, 0)) }
    var umbralAj by remember(p) { mutableStateOf(Fmt.dec(p.umbralAjuste * 100, 0)) }
    var iva by remember(p) { mutableStateOf(Fmt.dec(p.tipoIva * 100, 0)) }
    var deduccion by remember(p) { mutableStateOf(Fmt.dec(p.pctDeduccion * 100, 0)) }

    fun d(s: String) = Fmt.parseDouble(s)
    val candidato: ContractParams? = runCatching {
        ContractParams(
            contrato = contrato.trim(),
            vehiculo = vehiculo.trim(),
            matricula = matricula.trim().uppercase().replace(" ", "").replace("-", ""),
            empresa = empresa.trim(), telefono1 = telefono1.trim(), telefono2 = telefono2.trim(), email = email.trim(),
            inicio = inicio,
            fin = fin,
            meses = Fmt.parseInt(meses)!!.also { require(it > 0) },
            kmAnio = Fmt.parseInt(kmAnio)!!.also { require(it > 0) },
            cuotaMensual = d(cuota)!!,
            cuotaSinIva = d(cuotaSinIva)!!,
            repDanos = d(repDanos)!!,
            deposito = d(deposito)!!,
            eurKmNoRecorrido = d(eurNoRec)!!,
            eurKmExceso = d(eurExceso)!!,
            recargoExceso = d(recargo)!!,
            umbralLiquidacion = d(umbralLiq)!! / 100,
            umbralAjuste = d(umbralAj)!! / 100,
            modoProyeccion = p.modoProyeccion,
            kmDiaProyeccion = p.kmDiaProyeccion,
            provinciaId = p.provinciaId,
            tipoIva = d(iva)!! / 100,
            pctDeduccion = d(deduccion)!! / 100,
        )
    }.getOrNull()
    val errores = candidato?.errores() ?: listOf("Hay campos vacíos o con valores no numéricos.")
    val nuevo = candidato?.takeIf { errores.isEmpty() }
    val cambiado = nuevo != null && nuevo != p

    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(title = "Apariencia", icon = Icons.Default.Palette, accent = Palette.amber) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { i, mode ->
                    SegmentedButton(
                        selected = vm.themeMode == mode,
                        onClick = { vm.changeThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = ThemeMode.entries.size),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = vm.themeMode == mode) {
                                Icon(
                                    when (mode) {
                                        ThemeMode.System -> Icons.Default.BrightnessAuto
                                        ThemeMode.Light -> Icons.Default.LightMode
                                        ThemeMode.Dark -> Icons.Default.DarkMode
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                                )
                            }
                        },
                    ) { Text(mode.label, maxLines = 1) }
                }
            }
        }

        val context = LocalContext.current

        // --- Compañía de renting: llamada directa (con permiso) o marcador ---
        var numeroPendiente by remember { mutableStateOf<String?>(null) }
        fun marcar(numero: String) {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${marcable(numero)}")))
        }
        fun llamarDirecto(numero: String) {
            runCatching { context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:${marcable(numero)}"))) }
                .onFailure { marcar(numero) }
        }
        val pedirLlamada = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            val n = numeroPendiente; numeroPendiente = null
            if (n != null) { if (ok) llamarDirecto(n) else marcar(n) }
        }
        fun llamar(numero: String) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) llamarDirecto(numero)
            else { numeroPendiente = numero; pedirLlamada.launch(android.Manifest.permission.CALL_PHONE) }
        }
        val nombreEmpresa = p.empresa.ifBlank { "Compañía de renting" }
        SectionCard(title = nombreEmpresa, subtitle = if (p.empresa.isBlank()) "Datos de contacto de tu compañía de renting" else "Contacto", icon = Icons.Default.Phone, accent = Palette.green) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (p.telefono1.isBlank() && p.telefono2.isBlank() && p.email.isBlank()) {
                    Text("Añade el nombre, los teléfonos y el correo de tu compañía en el bloque Contrato (más abajo) para llamar o escribir con un toque.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (p.telefono1.isNotBlank()) {
                    Button(onClick = { llamar(p.telefono1) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Palette.green)) {
                        Icon(Icons.Default.Call, contentDescription = null, Modifier.size(20.dp)); Spacer(Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Atención al cliente", style = MaterialTheme.typography.labelMedium)
                            Text(p.telefono1, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (p.telefono2.isNotBlank()) {
                    OutlinedButton(onClick = { llamar(p.telefono2) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Call, contentDescription = null, Modifier.size(20.dp)); Spacer(Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Otro teléfono", style = MaterialTheme.typography.labelMedium)
                            Text(p.telefono2, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (p.email.isNotBlank()) {
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${p.email}")).putExtra(Intent.EXTRA_SUBJECT, "Contrato ${p.contrato}")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                        Text(p.email)
                    }
                }
            }
        }

        // --- Recordatorios ---
        var permisoPendiente by remember { mutableStateOf<Reminders.Settings?>(null) }
        val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            val s = permisoPendiente; permisoPendiente = null
            if (ok && s != null) vm.changeReminders(s) else onMessage("Sin permiso de notificaciones no se pueden enviar recordatorios")
        }
        fun aplicar(s: Reminders.Settings) {
            if ((s.kmEnabled || s.ajusteEnabled) && !Reminders.hasPermission(context) && Build.VERSION.SDK_INT >= 33) {
                permisoPendiente = s
                pedirPermiso.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else vm.changeReminders(s)
        }
        val rem = vm.reminders
        SectionCard(title = "Recordatorios", subtitle = "Notificaciones a las 10:00", icon = Icons.Default.Notifications, accent = Palette.orange) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Anotar los kilómetros cada mes", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (rem.kmEnabled) "Próximo: ${Fmt.date(Reminders.nextMonthly(rem.kmDay).toLocalDate())}" else "Desactivado",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = rem.kmEnabled, onCheckedChange = { aplicar(rem.copy(kmEnabled = it)) })
            }
            if (rem.kmEnabled) {
                var diaText by remember(rem.kmDay) { mutableStateOf(rem.kmDay.toString()) }
                val dia = diaText.toIntOrNull()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NumberField("Día del mes", diaText, { diaText = it }, Modifier.weight(1f), decimal = false, isError = dia == null || dia !in 1..28, supporting = "Entre 1 y 28")
                    Spacer(Modifier.width(8.dp))
                    Button(enabled = dia != null && dia in 1..28 && dia != rem.kmDay, onClick = { aplicar(rem.copy(kmDay = dia!!)) }) { Text("Aplicar") }
                }
            }
            ThinDivider()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Aviso previo al ajuste anual", style = MaterialTheme.typography.bodyMedium)
                    val prox = Reminders.nextAjuste(vm.data.params, rem.ajusteDias)
                    Text(
                        if (!rem.ajusteEnabled) "${rem.ajusteDias} días antes · desactivado" else if (prox != null) "${rem.ajusteDias} días antes · próximo: ${Fmt.date(prox.toLocalDate())}" else "No quedan ajustes anuales",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = rem.ajusteEnabled, onCheckedChange = { aplicar(rem.copy(ajusteEnabled = it)) })
            }
        }

        // --- Multas: consulta integrada del tablón edictal del BOE + sede de la DGT ---
        fun abrir(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        val consulta = vm.multas
        var matriculaConsulta by remember(p.matricula) { mutableStateOf(p.matricula) }
        SectionCard(title = "Multas", subtitle = "Tablón edictal del BOE · cualquier matrícula", icon = Icons.Default.LocalPolice, accent = Palette.red) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = matriculaConsulta, onValueChange = { matriculaConsulta = it.uppercase() },
                    label = { Text("Matrícula a consultar") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text(if (FinesCheck.normalizar(matriculaConsulta) == p.matricula) "La del contrato" else "Distinta de la del contrato (${p.matricula}); no se guarda")
                    },
                    trailingIcon = {
                        if (FinesCheck.normalizar(matriculaConsulta) != p.matricula) {
                            TextButton(onClick = { matriculaConsulta = p.matricula }) { Text("La mía") }
                        }
                    },
                )
                Button(onClick = { vm.consultarMultas(matriculaConsulta) }, enabled = !vm.multasCargando && matriculaConsulta.isNotBlank(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Palette.red)) {
                    if (vm.multasCargando) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = androidx.compose.ui.graphics.Color.White); Spacer(Modifier.width(10.dp)); Text("Consultando el BOE…")
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)); Text("Consultar ahora")
                    }
                }
                // El resultado entra animado (fundido + deslizamiento) cada vez que llega una respuesta nueva
                AnimatedContent(
                    targetState = Triple(consulta?.cuando, vm.multasError, vm.multasCargando),
                    transitionSpec = {
                        (fadeIn(tween(450, delayMillis = 100)) + slideInVertically(tween(450, delayMillis = 100)) { it / 3 })
                            .togetherWith(fadeOut(tween(150)))
                    },
                    label = "resultadoMultas",
                ) { (cuando, error, cargando) ->
                    when {
                        cargando -> Text("Consultando el tablón del BOE…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        error != null -> Text("No se pudo consultar: $error", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        cuando == null || consulta == null -> Text("Aún no se ha consultado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        consulta.anuncios.isEmpty() -> Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingPill("${consulta.matricula}: sin sanciones", Palette.green, key = cuando); Spacer(Modifier.width(8.dp))
                            Text("Consultado el ${Fmt.date(consulta.cuando.toLocalDate())} a las ${"%02d:%02d".format(consulta.cuando.hour, consulta.cuando.minute)} · últimos 3 meses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PulsingPill("${consulta.matricula}: ${consulta.anuncios.size} anuncio(s)", Palette.red, key = cuando); Spacer(Modifier.width(8.dp))
                                Text("Consultado el ${Fmt.date(consulta.cuando.toLocalDate())}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            consulta.anuncios.forEach { a ->
                                Column(Modifier.fillMaxWidth().clickable { abrir(a.url) }.padding(vertical = 4.dp)) {
                                    Text("${a.fecha} · ${a.organismo}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    Text(a.texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Ver anuncio (PDF)", style = MaterialTheme.typography.labelSmall, color = Palette.blue)
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Revisar cada semana y avisar", style = MaterialTheme.typography.bodyMedium)
                        Text("Solo la matrícula del contrato (${p.matricula}); notificación si aparece un anuncio nuevo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = vm.reminders.multasEnabled, onCheckedChange = { aplicar(vm.reminders.copy(multasEnabled = it)) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { abrir(FinesCheck.url(FinesCheck.normalizar(matriculaConsulta).ifBlank { p.matricula })) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Ver en el BOE")
                    }
                    OutlinedButton(onClick = { abrir("https://sede.dgt.gob.es/es/multas/") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Sede DGT")
                    }
                }
                if (vm.multasError != null) {
                    TextButton(onClick = { vm.diagnosticarMultas(matriculaConsulta) }) { Text("Probar conexión con el BOE") }
                }
                vm.diagnostico?.let { info ->
                    AlertDialog(
                        onDismissRequest = { vm.cerrarDiagnostico() },
                        title = { Text("Prueba de conexión") },
                        text = { Text(info, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) },
                        confirmButton = {
                            TextButton(onClick = {
                                val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                                cm.setPrimaryClip(android.content.ClipData.newPlainText("diagnostico", info)); onMessage("Copiado al portapapeles")
                            }) { Text("Copiar") }
                        },
                        dismissButton = { TextButton(onClick = { vm.cerrarDiagnostico() }) { Text("Cerrar") } },
                    )
                }
                Text(
                    "En un renting el titular es la compañía: las multas se le notifican a ella y te las reenvía. Al tablón del BOE solo llegan las que no se han podido notificar. Las que tengas a tu nombre se ven en la sede de la DGT con identificación.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // --- Precio de mercado ---
        var provOpen by remember { mutableStateOf(false) }
        SectionCard(title = "Precio de mercado", subtitle = "Gasolina 95 · datos abiertos del Ministerio de Industria", icon = Icons.Default.LocalGasStation, accent = Palette.teal) {
            ExposedDropdownMenuBox(expanded = provOpen, onExpandedChange = { provOpen = it }) {
                OutlinedTextField(
                    value = FuelPrices.nombreProvincia(p.provinciaId), onValueChange = {}, readOnly = true,
                    label = { Text("Provincia") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provOpen) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                DropdownMenu(expanded = provOpen, onDismissRequest = { provOpen = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                    DropdownMenuItem(text = { Text("Toda España") }, onClick = { vm.setProvincia(null); provOpen = false })
                    FuelPrices.PROVINCIAS.forEach { (id, nombre) ->
                        DropdownMenuItem(text = { Text(nombre) }, onClick = { vm.setProvincia(id); provOpen = false })
                    }
                }
            }
            Text(
                "Se usa al elegir \"Precio de mercado\" en un repostaje y en \"Completar precios de mercado\" del menú. Por provincia la consulta es más rápida.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(title = "Contrato", icon = Icons.Default.DirectionsCar, accent = Palette.indigo) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(contrato, { contrato = it }, label = { Text("Nº contrato") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(vehiculo, { vehiculo = it }, label = { Text("Vehículo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(matricula, { matricula = it }, label = { Text("Matrícula") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(empresa, { empresa = it }, label = { Text("Compañía de renting") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(telefono1, { telefono1 = it }, label = { Text("Tel. atención") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone))
                    OutlinedTextField(telefono2, { telefono2 = it }, label = { Text("Otro tel.") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone))
                }
                OutlinedTextField(email, { email = it }, label = { Text("Correo de contacto") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email))
                DateField("Inicio (puesta a disposición)", inicio, { inicio = it })
                DateField("Fin de contrato", fin, { fin = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Plazo", meses, { meses = it }, Modifier.weight(1f), suffix = "meses", decimal = false, isError = Fmt.parseInt(meses) == null)
                    NumberField("Km / año", kmAnio, { kmAnio = it }, Modifier.weight(1f), suffix = "km", decimal = false, isError = Fmt.parseInt(kmAnio) == null)
                }
                val kmC = Fmt.parseInt(kmAnio)?.let { a -> Fmt.parseInt(meses)?.let { m -> a / 12.0 * m } }
                if (kmC != null) Text("Km contratados: ${Fmt.km(kmC)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        SectionCard(title = "Cuotas", icon = Icons.Default.Paid, accent = Palette.pink) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Cuota (IVA incl.)", cuota, { cuota = it }, Modifier.weight(1f), suffix = "€", isError = d(cuota) == null)
                    NumberField("Cuota sin IVA", cuotaSinIva, { cuotaSinIva = it }, Modifier.weight(1f), suffix = "€", isError = d(cuotaSinIva) == null)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Rep. daños", repDanos, { repDanos = it }, Modifier.weight(1f), suffix = "€", isError = d(repDanos) == null)
                    NumberField("Depósito", deposito, { deposito = it }, Modifier.weight(1f), suffix = "€", isError = d(deposito) == null)
                }
            }
        }

        SectionCard(title = "Liquidación de kilómetros", icon = Icons.Default.Gavel, accent = Palette.purple) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("€ / km no recorrido", eurNoRec, { eurNoRec = it }, Modifier.weight(1f), isError = d(eurNoRec) == null)
                    NumberField("€ / km exceso", eurExceso, { eurExceso = it }, Modifier.weight(1f), isError = d(eurExceso) == null)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Umbral abono", umbralLiq, { umbralLiq = it }, Modifier.weight(1f), suffix = "%", isError = d(umbralLiq) == null)
                    NumberField("Umbral recargo", umbralAj, { umbralAj = it }, Modifier.weight(1f), suffix = "%", isError = d(umbralAj) == null)
                }
                NumberField("Recargo si exceso > umbral", recargo, { recargo = it }, suffix = "×", isError = d(recargo) == null)
            }
        }

        SectionCard(title = "IVA", icon = Icons.Default.Receipt, accent = Palette.teal) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Tipo de IVA", iva, { iva = it }, Modifier.weight(1f), suffix = "%", isError = d(iva) == null)
                NumberField("% deducción", deduccion, { deduccion = it }, Modifier.weight(1f), suffix = "%", isError = d(deduccion) == null)
            }
        }

        if (errores.isNotEmpty() && candidato != p) {
            SectionCard(title = "Revisa los parámetros", icon = Icons.Default.Warning, accent = Palette.red) {
                errores.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(enabled = candidato != p, onClick = {
                // Vuelve a los valores guardados: forzamos la recomposición de los campos con una copia igual
                vm.updateParams(p.copy()); onMessage("Cambios descartados")
            }) { Text("Descartar cambios") }
            Spacer(Modifier.width(8.dp))
            Button(enabled = cambiado, onClick = { vm.updateParams(nuevo!!); onSaved() }) { Text("Guardar") }
        }

        // --- Zona de peligro: borrado solo con copia de seguridad de los datos actuales ---
        var borrado by remember { mutableStateOf(false) }
        val alDia = vm.copiaAlDia
        val n = vm.data
        SectionCard(title = "Borrar datos", subtitle = "Solo con una copia de seguridad de los datos actuales", icon = Icons.Default.DeleteForever, accent = Palette.red) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(if (alDia) "Copia al día" else "Sin copia de los datos actuales", if (alDia) Palette.green else Palette.red)
                    Text(
                        "${n.measurements.size} mediciones · ${n.refuels.size} repostajes · ${n.expenses.size} gastos · ${n.measurements.count { it.foto != null }} fotos",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!alDia) {
                    Text("Guarda antes una copia de seguridad desde el menú ⋮ (ZIP con contrato, mediciones, repostajes, gastos y fotos). El borrado se habilita al guardarla; si después anotas algo, hará falta otra.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = { borrado = true }, enabled = alDia, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Palette.red)) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Borrar todos los datos")
                }
                Text("Deja la app vacía: contrato, mediciones, repostajes, gastos y fotos. Se conservan solo los ajustes de la app (apariencia, recordatorios). Para recuperar los datos, restaura la copia desde el menú ⋮.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (borrado) {
            var texto by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { borrado = false },
                title = { Text("Borrar todos los datos") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Se eliminarán los datos del contrato y todas las mediciones, repostajes, gastos y fotos: la app quedará vacía. No se puede deshacer; solo podrás recuperarlos restaurando la copia de seguridad.")
                        OutlinedTextField(texto, { texto = it }, label = { Text("Escribe BORRAR para confirmar") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    TextButton(enabled = texto.trim() == "BORRAR", onClick = {
                        vm.borrarTodo(); borrado = false; onMessage("Datos borrados")
                    }) { Text("Confirmar", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { borrado = false }) { Text("Cancelar") } },
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

/** Etiqueta que hace un breve "latido" de escala cada vez que cambia `key` (llegada de una respuesta). */
@Composable
private fun PulsingPill(text: String, color: androidx.compose.ui.graphics.Color, key: Any?) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(key) {
        scale.snapTo(0.6f)
        scale.animateTo(1.15f, tween(220))
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }
    Box(Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }) { Pill(text, color) }
}
