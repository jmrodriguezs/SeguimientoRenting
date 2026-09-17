package com.manursan.seguimientokm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.manursan.seguimientokm.ui.Palette
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.manursan.seguimientokm.ui.ExpenseEdit
import com.manursan.seguimientokm.ui.GastoSeccion
import com.manursan.seguimientokm.ui.KilometrosScreen
import com.manursan.seguimientokm.ui.MeasurementEdit
import com.manursan.seguimientokm.ui.ParametrosScreen
import com.manursan.seguimientokm.ui.ProyeccionScreen
import com.manursan.seguimientokm.ui.RefuelEdit
import com.manursan.seguimientokm.ui.RepostajesScreen
import com.manursan.seguimientokm.ui.ResumenScreen
import com.manursan.seguimientokm.ui.SeguimientoKmTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Nombre de la copia con fecha y hora de creación. */
fun nombreCopia(): String =
    "seguimiento_renting_" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm")) + ".zip"

enum class Tab(val label: String, val icon: ImageVector, val color: Color) {
    Resumen("Resumen", Icons.Default.Dashboard, Palette.blue),
    Kilometros("Kilómetros", Icons.Default.Speed, Palette.green),
    Repostajes("Repostajes", Icons.Default.LocalGasStation, Palette.orange),
    Proyeccion("Proyección", Icons.Default.TrendingUp, Palette.purple),
    Ajustes("Ajustes", Icons.Default.Settings, Palette.teal),
}

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barra de estado transparente con iconos claros: la barra superior siempre es de color
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        Reminders.schedule(this)
        setContent {
            SeguimientoKmTheme(mode = vm.themeMode) {
                App(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(vm: MainViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.Resumen) }
    var menuOpen by remember { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf(false) }
    var measurementEdit by remember { mutableStateOf<MeasurementEdit?>(null) }
    var refuelEdit by remember { mutableStateOf<RefuelEdit?>(null) }
    var expenseEdit by remember { mutableStateOf<ExpenseEdit?>(null) }
    var gastoSeccion by rememberSaveable { mutableStateOf(GastoSeccion.Repostajes) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resultado = vm.resultado

    fun toast(msg: String) = scope.launch { snackbar.showSnackbar(msg) }

    // Exportar / importar mediante el selector de ficheros del sistema (sin permisos de almacenamiento)
    val saveXlsx = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportXlsx()) }
        }.onSuccess { toast("Excel exportado") }.onFailure { toast("Error al exportar: ${it.message}") }
    }
    fun sharePdf() {
        runCatching {
            val f = vm.pdfFile()
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "com.manursan.seguimientokm.fileprovider", f)
            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Informe Seguimiento Renting ${Fmt.date(LocalDate.now())}")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(send, "Compartir informe"))
        }.onFailure { toast("Error al generar el informe: ${it.message}") }
    }
    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { vm.writeBackup(it) }
        }.onSuccess { toast("Copia de seguridad guardada (datos + fotos)") }.onFailure { toast("Error al guardar: ${it.message}") }
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val err = runCatching {
            val stream = context.contentResolver.openInputStream(uri) ?: return@runCatching "No se pudo leer el fichero"
            stream.use { vm.restoreBackup(it) }  // null = restaurada sin error
        }.getOrElse { "No se pudo leer el fichero: ${it.message}" }
        toast(err ?: "Copia restaurada (contrato, datos y fotos)")
    }

    Scaffold(
        topBar = {
            val barColor by animateColorAsState(tab.color, label = "barColor")
            TopAppBar(
                title = {
                    Text(if (tab == Tab.Resumen) "Seguimiento Renting" else tab.label, fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = barColor,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
                actions = {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Menú") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Informe PDF") }, onClick = {
                            menuOpen = false; sharePdf()
                        })
                        DropdownMenuItem(text = { Text("Exportar a Excel (.xlsx)") }, onClick = {
                            menuOpen = false; saveXlsx.launch("SEGUIMIENTO_KILOMETROS_${LocalDate.now()}.xlsx")
                        })
                        DropdownMenuItem(text = { Text("Completar precios de mercado") }, onClick = {
                            menuOpen = false
                            vm.completarPreciosMercado { ok, ko ->
                                toast(if (ok == 0 && ko == 0) "Todos los repostajes ya tienen precio" else "Precios completados: $ok" + if (ko > 0) " · sin datos: $ko" else "")
                            }
                        })
                        DropdownMenuItem(text = { Text("Guardar copia de seguridad") }, onClick = {
                            menuOpen = false; saveBackup.launch(nombreCopia())
                        })
                        DropdownMenuItem(text = { Text("Restaurar copia de seguridad") }, onClick = {
                            menuOpen = false; openBackup.launch(arrayOf("application/zip", "application/json", "*/*"))
                        })
                        DropdownMenuItem(text = { Text("Aviso legal y fuentes") }, onClick = {
                            menuOpen = false; showLegalDialog = true
                        })
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            indicatorColor = t.color,
                            unselectedIconColor = t.color,
                        ),
                    )
                }
            }
        },
        floatingActionButton = {
            // Sin contrato configurado no se pueden anotar datos
            if (vm.data.params.configurado) when (tab) {
                Tab.Kilometros -> FloatingActionButton(
                    containerColor = Tab.Kilometros.color, contentColor = Color.White,
                    onClick = { measurementEdit = MeasurementEdit(null, LocalDate.now(), "", "") },
                ) { Icon(Icons.Default.Add, contentDescription = "Añadir medición") }
                Tab.Repostajes -> FloatingActionButton(
                    containerColor = if (gastoSeccion == GastoSeccion.Repostajes) Tab.Repostajes.color else Palette.amber, contentColor = Color.White,
                    onClick = {
                        if (gastoSeccion == GastoSeccion.Repostajes) refuelEdit = RefuelEdit(null, LocalDate.now(), "", "")
                        else expenseEdit = ExpenseEdit(null, LocalDate.now(), ExpenseCategory.Peaje, "", "")
                    },
                ) { Icon(Icons.Default.Add, contentDescription = "Añadir") }
                else -> {}
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box {
        when (tab) {
            Tab.Resumen -> ResumenScreen(resultado, padding)
            Tab.Kilometros -> KilometrosScreen(resultado, vm, padding, measurementEdit) { measurementEdit = it }
            Tab.Repostajes -> RepostajesScreen(
                resultado, vm, padding,
                seccion = gastoSeccion, onSeccion = { gastoSeccion = it },
                edit = refuelEdit, onEdit = { refuelEdit = it },
                expenseEdit = expenseEdit, onExpenseEdit = { expenseEdit = it },
            )
            Tab.Proyeccion -> ProyeccionScreen(resultado, vm, padding)
            Tab.Ajustes -> ParametrosScreen(vm, padding) { toast(it) }
        }
        vm.tarea?.let { msg ->
            // Progreso de una tarea larga (descarga de precios) superpuesto al contenido
            Box(Modifier.fillMaxWidth().padding(padding), contentAlignment = Alignment.TopCenter) {
                Text(
                    msg, style = MaterialTheme.typography.labelMedium, color = Color.White,
                    modifier = Modifier.padding(top = 8.dp).background(Palette.orange, RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
        }
    }

    if (showLegalDialog) {
        AlertDialog(
            onDismissRequest = { showLegalDialog = false },
            title = { Text("Aviso legal y fuentes oficiales", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Seguimiento Renting es una aplicación independiente de gestión y control para uso personal.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Esta aplicación NO representa a ninguna entidad gubernamental ni pública (como la DGT, el BOE o ministerios del Gobierno de España) ni tiene vinculación oficial con las mismas.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Fuentes de información oficiales utilizadas:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("• BOE - Tablón Edictal Único (TEU):", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "  https://www.boe.es/tablon_edictal_unico/",
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.blue,
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.boe.es/tablon_edictal_unico/")))
                            },
                        )
                        Text("• Sede Electrónica DGT:", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "  https://sede.dgt.gob.es/",
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.blue,
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://sede.dgt.gob.es/")))
                            },
                        )
                        Text("• Geoportal de Gasolineras (Ministerio):", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "  https://geoportalgasolineras.es/",
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.blue,
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://geoportalgasolineras.es/")))
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLegalDialog = false }) { Text("Entendido") }
            },
        )
    }
}
