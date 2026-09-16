package com.manursan.seguimientokm

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate

enum class ThemeMode(val label: String) { System("Sistema"), Light("Claro"), Dark("Oscuro") }

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("ui", Context.MODE_PRIVATE)
    private val ctx: Context get() = getApplication()

    var data: AppData by mutableStateOf(Storage.load(app))
        private set

    val resultado: Resultado get() = Calc.compute(data)

    /** Mensaje de progreso de una tarea en segundo plano (p. ej. descarga de precios), o null. */
    var tarea: String? by mutableStateOf(null)
        private set

    private fun update(newData: AppData) {
        data = newData
        Storage.save(ctx, newData)
        KmWidget.refresh(ctx)
    }

    // --- Apariencia ---
    var themeMode: ThemeMode by mutableStateOf(
        runCatching { ThemeMode.valueOf(prefs.getString("theme", ThemeMode.System.name)!!) }.getOrDefault(ThemeMode.System)
    )
        private set

    fun changeThemeMode(mode: ThemeMode) {
        themeMode = mode
        prefs.edit().putString("theme", mode.name).apply()
    }

    // --- Recordatorios ---
    var reminders: Reminders.Settings by mutableStateOf(Reminders.load(app))
        private set

    fun changeReminders(s: Reminders.Settings) {
        reminders = s
        Reminders.save(ctx, s)
        Reminders.schedule(ctx)
    }

    // --- Kilómetros ---
    fun addMeasurement(fecha: LocalDate, km: Int, nota: String, foto: String?) =
        update(data.copy(measurements = data.measurements + Measurement(fecha = fecha, km = km, nota = nota, foto = foto)))

    fun updateMeasurement(id: String, fecha: LocalDate, km: Int, nota: String, foto: String?) {
        val anterior = data.measurements.firstOrNull { it.id == id }
        if (anterior?.foto != null && anterior.foto != foto) Photos.delete(ctx, anterior.foto)
        update(data.copy(measurements = data.measurements.map {
            if (it.id == id) it.copy(fecha = fecha, km = km, nota = nota, foto = foto) else it
        }))
    }

    fun deleteMeasurement(id: String) {
        data.measurements.firstOrNull { it.id == id }?.foto?.let { Photos.delete(ctx, it) }
        update(data.copy(measurements = data.measurements.filterNot { it.id == id }))
    }

    /** Importa una foto (cámara o galería) y devuelve su nombre de fichero, o null si falla. */
    suspend fun importPhoto(uri: Uri): String? = withContext(Dispatchers.IO) { runCatching { Photos.import(ctx, uri) }.getOrNull() }

    // --- Repostajes ---
    fun addRefuel(fecha: LocalDate, importe: Double, nota: String, litros: Double?, precio: Double?, mercado: Boolean) =
        update(data.copy(refuels = data.refuels + Refuel(fecha = fecha, importe = importe, nota = nota, litros = litros, precioLitro = precio, precioMercado = mercado)))

    fun updateRefuel(id: String, fecha: LocalDate, importe: Double, nota: String, litros: Double?, precio: Double?, mercado: Boolean) =
        update(data.copy(refuels = data.refuels.map {
            if (it.id == id) it.copy(fecha = fecha, importe = importe, nota = nota, litros = litros, precioLitro = precio, precioMercado = mercado) else it
        }))

    fun deleteRefuel(id: String) =
        update(data.copy(refuels = data.refuels.filterNot { it.id == id }))

    /** Precio medio de mercado en una fecha, según la provincia y el combustible configurados. */
    suspend fun precioMercado(fecha: LocalDate): Result<Double> =
        FuelPrices.precioMedio(ctx, fecha, data.params.provinciaId, data.params.combustibleId)

    /** Rellena con el precio de mercado los repostajes que no tienen precio/litro. Devuelve (hechos, fallos). */
    fun completarPreciosMercado(onDone: (Int, Int) -> Unit) {
        val pendientes = data.refuels.filter { it.precioLitro == null }
        if (pendientes.isEmpty()) { onDone(0, 0); return }
        viewModelScope.launch {
            var ok = 0; var ko = 0
            pendientes.forEachIndexed { i, r ->
                tarea = "Precios de mercado: ${i + 1} de ${pendientes.size}"
                precioMercado(r.fecha).onSuccess { precio ->
                    ok++
                    update(data.copy(refuels = data.refuels.map {
                        if (it.id == r.id) it.copy(precioLitro = precio, precioMercado = true) else it
                    }))
                }.onFailure { ko++ }
            }
            tarea = null
            onDone(ok, ko)
        }
    }

    // --- Otros gastos ---
    fun addExpense(fecha: LocalDate, categoria: ExpenseCategory, importe: Double, nota: String) =
        update(data.copy(expenses = data.expenses + Expense(fecha = fecha, categoria = categoria, importe = importe, nota = nota)))

    fun updateExpense(id: String, fecha: LocalDate, categoria: ExpenseCategory, importe: Double, nota: String) =
        update(data.copy(expenses = data.expenses.map {
            if (it.id == id) it.copy(fecha = fecha, categoria = categoria, importe = importe, nota = nota) else it
        }))

    fun deleteExpense(id: String) =
        update(data.copy(expenses = data.expenses.filterNot { it.id == id }))

    // --- Multas (tablón del BOE) ---
    var multas: FinesCheck.Consulta? by mutableStateOf(FinesCheck.load(app))
        private set
    var multasError: String? by mutableStateOf(null)
        private set
    var multasCargando: Boolean by mutableStateOf(false)
        private set

    /** Consulta la matrícula indicada (por defecto la del contrato). Solo se guarda la del contrato. */
    fun consultarMultas(matricula: String = data.params.matricula) {
        if (multasCargando) return
        val m = FinesCheck.normalizar(matricula)
        if (m.isBlank()) { multasError = "Escribe una matrícula"; return }
        viewModelScope.launch {
            multasCargando = true; multasError = null
            FinesCheck.consultar(ctx, m, guardar = m == data.params.matricula)
                .onSuccess { multas = it }
                .onFailure { multasError = FinesCheck.describir(it) }
            multasCargando = false
        }
    }

    var diagnostico: String? by mutableStateOf(null)
        private set

    fun diagnosticarMultas(matricula: String = data.params.matricula) {
        viewModelScope.launch {
            diagnostico = "Probando…"
            diagnostico = runCatching { FinesCheck.diagnostico(FinesCheck.normalizar(matricula)) }.getOrElse { FinesCheck.describir(it) }
        }
    }

    fun cerrarDiagnostico() { diagnostico = null }

    // --- Parámetros ---
    fun updateParams(p: ContractParams) = update(data.copy(params = p))

    fun setProyeccion(modo: ModoProyeccion, kmDia: Double? = data.params.kmDiaProyeccion) =
        update(data.copy(params = data.params.copy(modoProyeccion = modo, kmDiaProyeccion = kmDia)))

    fun setProvincia(id: String?) = update(data.copy(params = data.params.copy(provinciaId = id)))
    fun setCombustible(id: String) = update(data.copy(params = data.params.copy(combustibleId = id)))

    // --- Borrado protegido por copia de seguridad ---
    /** Huella de los datos (JSON + fotos) cubiertos por la última copia de seguridad guardada. */
    private var huellaCopia: String? by mutableStateOf(prefs.getString("huellaCopia", null))

    private fun huella(d: AppData): String {
        val fotos = d.measurements.mapNotNull { it.foto }.sorted().joinToString(",")
        val md = java.security.MessageDigest.getInstance("SHA-256").digest((Storage.toJson(d) + "|" + fotos).toByteArray())
        return md.joinToString("") { "%02x".format(it) }
    }

    /** true si los datos actuales están exactamente cubiertos por una copia de seguridad. */
    val copiaAlDia: Boolean get() = huellaCopia != null && huellaCopia == huella(data)

    private fun borrarFotos() = data.measurements.mapNotNull { it.foto }.forEach { Photos.delete(ctx, it) }

    /** Deja la app vacía: sin contrato, mediciones, repostajes, gastos ni fotos. */
    fun borrarTodo() {
        if (!copiaAlDia) return
        borrarFotos()
        limpiarConsultas()
        update(AppData(params = ContractParams.vacio()))
    }

    private fun limpiarConsultas() {
        ctx.getSharedPreferences("multas", Context.MODE_PRIVATE).edit().clear().apply()
        multas = null; multasError = null
        Reminders.schedule(ctx)
    }

    // --- Copias y exportación ---
    fun exportXlsx(): ByteArray = XlsxExport.build(data, resultado)
    fun buildPdf(): ByteArray = PdfReport.build(resultado)

    /** Escribe el informe PDF en cache/informes y devuelve el fichero (para compartir vía FileProvider). */
    fun pdfFile(): java.io.File {
        val dir = java.io.File(ctx.cacheDir, "informes").also { it.mkdirs() }
        val f = java.io.File(dir, "Informe_Seguimiento_Renting_${LocalDate.now()}.pdf")
        f.writeBytes(buildPdf())
        return f
    }
    fun writeBackup(out: OutputStream) {
        Backup.write(ctx, data, out)
        huellaCopia = huella(data)
        prefs.edit().putString("huellaCopia", huellaCopia).apply()
    }

    /** Devuelve null si ha ido bien, o el mensaje de error. */
    fun restoreBackup(input: InputStream): String? = runCatching {
        update(Backup.read(ctx, input))
    }.exceptionOrNull()?.let { "Fichero no válido: ${it.message}" }
}
