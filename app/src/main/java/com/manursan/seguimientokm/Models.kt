package com.manursan.seguimientokm

import java.time.LocalDate
import java.util.UUID

/** Con qué ritmo de km/día se proyecta hasta el fin de contrato (palanca N22 del Excel). */
enum class ModoProyeccion(val label: String) {
    Acumulada("Media"),
    Reciente("6 meses"),
    Manual("Manual"),
}

/** Parámetros del contrato (bloque M1:N15 y N41:N42 del Excel). */
data class ContractParams(
    val contrato: String = "1941439",
    val vehiculo: String = "PEUGEOT 2008 Allure Hybrid 145 eDCS6",
    val matricula: String = "8474NHP",
    /** Compañía de renting y sus datos de contacto (opcionales). */
    val empresa: String = "",
    val telefono1: String = "",
    val telefono2: String = "",
    val email: String = "",
    val inicio: LocalDate = LocalDate.of(2025, 12, 29),
    val fin: LocalDate = LocalDate.of(2028, 12, 31),
    val meses: Int = 36,
    val kmAnio: Int = 10000,
    val cuotaMensual: Double = 295.46,
    val cuotaSinIva: Double = 244.18,
    val repDanos: Double = 116.05,
    val deposito: Double = 244.18,
    val eurKmNoRecorrido: Double = 0.0167,
    val eurKmExceso: Double = 0.0217,
    val recargoExceso: Double = 1.25,
    val umbralLiquidacion: Double = 0.90,
    val umbralAjuste: Double = 0.10,
    val modoProyeccion: ModoProyeccion = ModoProyeccion.Acumulada,
    /** km/día del escenario manual (solo se usa con modoProyeccion = Manual). */
    val kmDiaProyeccion: Double? = null,
    /** Provincia (código INE de 2 cifras) para el precio medio de mercado; null = toda España. */
    val provinciaId: String? = null,
    /** Combustible (IDProducto del Ministerio) para el precio medio de mercado. */
    val combustibleId: String = FuelPrices.G95,
    val tipoIva: Double = 0.21,
    val pctDeduccion: Double = 0.50,
) {
    val kmContratados: Double get() = kmAnio.toDouble() / 12.0 * meses
    val cuotaAnual: Double get() = cuotaMensual * 12

    /** true si hay datos mínimos para calcular (fechas válidas y km contratados). */
    val configurado: Boolean get() = kmAnio > 0 && meses > 0 && fin.isAfter(inicio)

    /** true si hay cuota: sin ella no se calculan coste por km, coste total ni IVA. */
    val tieneCostes: Boolean get() = cuotaMensual > 0

    /** true si hay tarifas de liquidación: sin ellas no hay abono ni cargo estimados. */
    val tieneTarifas: Boolean get() = eurKmNoRecorrido > 0 || eurKmExceso > 0

    /** Errores de coherencia entre parámetros; lista vacía si todo es válido. */
    fun errores(): List<String> {
        val e = mutableListOf<String>()
        if (!fin.isAfter(inicio)) e += "La fecha de fin debe ser posterior a la de inicio."
        else {
            // El plazo se cuenta desde el día 1 siguiente a la puesta a disposición: se admite ±1 mes
            val mesesReales = java.time.temporal.ChronoUnit.MONTHS.between(inicio, fin)
            if (kotlin.math.abs(mesesReales - meses) > 1) e += "El plazo ($meses meses) no coincide con las fechas (${mesesReales} meses entre inicio y fin)."
        }
        if (meses <= 0) e += "El plazo debe ser mayor que 0."
        if (kmAnio <= 0) e += "Los km/año deben ser mayores que 0."
        if (cuotaMensual <= 0) e += "La cuota mensual con IVA es obligatoria."
        if (cuotaSinIva > cuotaMensual) e += "La cuota sin IVA no puede superar la cuota con IVA."
        if (cuotaMensual > 0 && cuotaSinIva > 0 && kotlin.math.abs(cuotaSinIva * (1 + tipoIva) - cuotaMensual) > 1.0)
            e += "Cuota sin IVA × (1 + IVA) = ${"%.2f".format(cuotaSinIva * (1 + tipoIva))} €, no coincide con la cuota con IVA (${"%.2f".format(cuotaMensual)} €)."
        if (repDanos > cuotaSinIva) e += "La parte de reparación de daños no puede superar la cuota sin IVA."
        if (tipoIva !in 0.0..1.0 || pctDeduccion !in 0.0..1.0 || umbralLiquidacion !in 0.0..1.0 || umbralAjuste !in 0.0..1.0)
            e += "Los porcentajes deben estar entre 0 y 100."
        if (eurKmNoRecorrido < 0 || eurKmExceso < 0 || recargoExceso < 1) e += "Tarifas por km no válidas (el recargo debe ser ≥ 1)."
        return e
    }

    companion object {
        /** Nombre que se guarda cuando no se indica el vehículo. */
        const val VEHICULO_POR_DEFECTO = "Coche de Renting"

        /** Contrato en blanco: sin datos propios; solo quedan los valores normativos (IVA, deducción, umbrales). */
        fun vacio(hoy: LocalDate = LocalDate.now()) = ContractParams(
            contrato = "", vehiculo = "", matricula = "", empresa = "", telefono1 = "", telefono2 = "", email = "",
            inicio = hoy, fin = hoy.plusMonths(36), meses = 36, kmAnio = 0,
            cuotaMensual = 0.0, cuotaSinIva = 0.0, repDanos = 0.0, deposito = 0.0,
            eurKmNoRecorrido = 0.0, eurKmExceso = 0.0,
            modoProyeccion = ModoProyeccion.Acumulada, kmDiaProyeccion = null, provinciaId = null,
        )
    }
}

/** Fotos de mediciones y repostajes. */
fun AppData.fotos(): List<String> = measurements.mapNotNull { it.foto } + refuels.mapNotNull { it.foto }

/** Una lectura del cuentakilómetros (columnas A:B del Excel). */
data class Measurement(
    val id: String = UUID.randomUUID().toString(),
    val fecha: LocalDate,
    val km: Int,
    val nota: String = "",
    /** Nombre de fichero de la foto del cuentakilómetros (en filesDir/fotos), si la hay. */
    val foto: String? = null,
)

/** Un repostaje (columnas I:J del Excel). Litros y precio son opcionales. */
data class Refuel(
    val id: String = UUID.randomUUID().toString(),
    val fecha: LocalDate,
    val importe: Double,
    val nota: String = "",
    val litros: Double? = null,
    val precioLitro: Double? = null,
    /** true si el precio/litro se tomó del precio medio de mercado en lugar de introducirse a mano. */
    val precioMercado: Boolean = false,
    /** Nombre de fichero de la foto del tique (en filesDir/fotos), si la hay. */
    val foto: String? = null,
) {
    /** Litros: los anotados, o los derivados del importe y el precio. */
    val litrosEfectivos: Double? get() = litros ?: precioLitro?.takeIf { it > 0 }?.let { importe / it }
}

enum class ExpenseCategory(val label: String) {
    Peaje("Peaje"), Parking("Aparcamiento"), Lavado("Lavado"), Multa("Multa"),
    Mantenimiento("Mantenimiento"), AdBlue("AdBlue"), Otro("Otro"),
}

/** Gasto de uso no incluido en el contrato (peajes, parking, lavado...). */
data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val fecha: LocalDate,
    val categoria: ExpenseCategory,
    val importe: Double,
    val nota: String = "",
)

data class AppData(
    val params: ContractParams = ContractParams(),
    val measurements: List<Measurement> = emptyList(),
    val refuels: List<Refuel> = emptyList(),
    val expenses: List<Expense> = emptyList(),
)

/** Datos de SEGUIMIENTO_KILOMETROS.xlsx: se usan en los tests para validar el motor de cálculo contra la hoja original. */
object SeedData {
    fun create(): AppData = AppData(
        params = ContractParams(),
        measurements = listOf(
            Measurement(fecha = LocalDate.of(2026, 2, 27), km = 1949),
            Measurement(fecha = LocalDate.of(2026, 2, 28), km = 1963),
            Measurement(fecha = LocalDate.of(2026, 4, 12), km = 3012),
            Measurement(fecha = LocalDate.of(2026, 5, 4), km = 3289),
            Measurement(fecha = LocalDate.of(2026, 5, 18), km = 3472),
            Measurement(fecha = LocalDate.of(2026, 6, 29), km = 4254),
            Measurement(fecha = LocalDate.of(2026, 7, 19), km = 4674),
            Measurement(fecha = LocalDate.of(2026, 8, 20), km = 5050),
            Measurement(fecha = LocalDate.of(2026, 9, 12), km = 6320),
        ),
        refuels = listOf(
            Refuel(fecha = LocalDate.of(2025, 12, 29), importe = 50.0),
            Refuel(fecha = LocalDate.of(2026, 1, 3), importe = 30.0),
            Refuel(fecha = LocalDate.of(2026, 1, 7), importe = 28.44),
            Refuel(fecha = LocalDate.of(2026, 1, 26), importe = 18.75),
            Refuel(fecha = LocalDate.of(2026, 1, 31), importe = 40.0),
            Refuel(fecha = LocalDate.of(2026, 2, 28), importe = 40.0),
            Refuel(fecha = LocalDate.of(2026, 3, 21), importe = 0.0),
            Refuel(fecha = LocalDate.of(2026, 4, 5), importe = 52.0),
            Refuel(fecha = LocalDate.of(2026, 5, 17), importe = 38.0),
            Refuel(fecha = LocalDate.of(2026, 6, 14), importe = 35.8),
            Refuel(fecha = LocalDate.of(2026, 6, 30), importe = 28.88),
            Refuel(fecha = LocalDate.of(2026, 7, 26), importe = 40.0),
            Refuel(fecha = LocalDate.of(2026, 8, 20), importe = 37.52),
            Refuel(fecha = LocalDate.of(2026, 8, 25), importe = 33.1),
            Refuel(fecha = LocalDate.of(2026, 9, 3), importe = 40.0),
        ),
    )
}
