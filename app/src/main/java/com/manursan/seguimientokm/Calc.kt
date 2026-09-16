package com.manursan.seguimientokm

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Fila de la tabla de kilómetros (A:H del Excel), real o proyectada. */
data class KmRow(
    val id: String?,
    val fecha: LocalDate,
    val dias: Long,
    val km: Double,
    val teoricos: Double,
    val desviacion: Double,
    val kmDia: Double,
    val gastoGasolina: Double,
    /** Coste/km de la columna G del Excel: cuota prorrateada + gasolina, entre km. */
    val costeKm: Double,
    /** Otros gastos (peajes, parking...) hasta esa fecha; en proyección, proporcionales a los km. */
    val otrosGastos: Double,
    /** Coste/km incluyendo otros gastos. */
    val costeKmTotal: Double,
    val desviacionPct: Double?,
    val proyectado: Boolean,
    val nota: String = "",
)

/** Fila de repostajes (I:L del Excel). */
data class RefuelRow(
    val refuel: Refuel,
    val acumulado: Double,
    val costeDiario: Double?,
)

data class Liquidacion(
    val kmProyectados: Double,
    val umbralAbono: Double,
    val umbralCargo: Double,
    /** Positivo = abono a favor del cliente, negativo = cargo. Sin IVA. */
    val abonoCargo: Double,
    val recargoAplicado: Boolean,
)

data class CosteTotal(
    val cuotas: Double,
    val cuotaIrregular: Double,
    val combustibleProyectado: Double,
    val abono: Double,
    val total: Double,
    val costePorKm: Double,
    val deposito: Double,
    val baseCuotas: Double,
    val ivaCuotas: Double,
    val baseCuotaIrregular: Double,
    val ivaCuotaIrregular: Double,
    val ivaCombustible: Double,
    val ivaTotal: Double,
    val ivaDeducible: Double,
    val costeNeto: Double,
)

data class Seguimiento(
    val fechaUltima: LocalDate,
    val kmUltima: Double,
    val combustibleAcumulado: Double,
    val eurKmCombustible: Double,
    val kmDiaRealAcumulado: Double,
    /** Ritmo de los últimos 6 meses (180 días) hasta la última medida; null si el contrato es más corto. */
    val kmDiaReciente: Double?,
    val kmDiaProyeccion: Double,
    val kmDiaTeoricos: Double,
    val diasContrato: Long,
    val diasTranscurridos: Long,
    val diasRestantes: Long,
)

/** "¿Cuánto puedo conducir?": margen desde la última medida hasta el fin de contrato. */
data class Margen(
    val diasRestantes: Long,
    val kmHastaContratados: Double,
    val kmDiaMaxSinCargo: Double,
    val kmMesMaxSinCargo: Double,
    val kmHastaUmbralAbono: Double,
    val kmDiaMaxConAbono: Double,
    val kmMesMaxConAbono: Double,
)

/** Consumo a partir de los litros anotados o derivados del precio/litro. */
data class Consumo(
    val litrosConocidos: Double,
    val importeConLitros: Double,
    val repostajesSinLitros: Int,
    /** €/litro medio de los repostajes con precio. */
    val precioMedioLitro: Double?,
    /** l/100 km hasta la última medida; null si no hay datos suficientes. */
    val litros100km: Double?,
    /** true si parte de los litros se han estimado con el precio medio. */
    val estimado: Boolean,
)

data class Gastos(
    val lista: List<Expense>,
    val total: Double,
    val porCategoria: Map<ExpenseCategory, Double>,
    /** Otros gastos anotados hasta hoy (los que entran en el coste/km, repartidos entre los km de la última medida). */
    val enCosteKm: Double,
    /** €/km de otros gastos. */
    val eurKm: Double,
    /** Cuotas devengadas hasta hoy + combustible + otros gastos. */
    val costeUsoHastaHoy: Double,
    val cuotasDevengadas: Double,
)

/** Coste por kilómetro desglosado (hasta la última medida) y proyectado a fin de contrato. */
data class CosteKm(
    val renting: Double,
    val combustible: Double,
    val otros: Double,
    val total: Double,
    /** Total proyectado a fin de contrato: coste del contrato + otros gastos proporcionales, entre km estimados. */
    val proyectado: Double,
)

/** Ajuste anual a cuenta (aniversarios de la puesta a disposición) con banda ±umbralAjuste. */
data class AjusteAnual(
    val fecha: LocalDate,
    val diasHasta: Long,
    val kmPrevistos: Double,
    val kmTeoricos: Double,
    val desviacionPct: Double,
    val dentroDeBanda: Boolean,
)

data class Resultado(
    val params: ContractParams,
    val reales: List<KmRow>,
    val proyeccion: List<KmRow>,
    val repostajes: List<RefuelRow>,
    val seguimiento: Seguimiento,
    val liquidacion: Liquidacion,
    val coste: CosteTotal,
    val margen: Margen,
    val consumo: Consumo,
    val gastos: Gastos,
    val costeKm: CosteKm,
    val proximoAjuste: AjusteAnual?,
)

/** Réplica de las fórmulas de SEGUIMIENTO_KILOMETROS.xlsx. */
object Calc {

    private fun days(from: LocalDate, to: LocalDate): Long = ChronoUnit.DAYS.between(from, to)

    fun compute(data: AppData, hoy: LocalDate = LocalDate.now()): Resultado {
        val p = data.params
        val diasContrato = days(p.inicio, p.fin).coerceAtLeast(1)
        // C2 = km contratados / días reales de contrato
        val kmDiaTeoricos = p.kmContratados / diasContrato

        val refuelsSorted = data.refuels.sortedWith(compareBy({ it.fecha }, { it.id }))
        val measSorted = data.measurements.sortedWith(compareBy({ it.fecha }, { it.km }))

        // F = SUMIFS(repostajes; fecha <= fecha fila)
        fun gastoHasta(fecha: LocalDate): Double =
            refuelsSorted.filter { !it.fecha.isAfter(fecha) }.sumOf { it.importe }

        val gastosLista = data.expenses.sortedWith(compareBy({ it.fecha }, { it.id }))
        fun otrosHasta(fecha: LocalDate): Double = gastosLista.filter { !it.fecha.isAfter(fecha) }.sumOf { it.importe }

        fun row(id: String?, fecha: LocalDate, km: Double, gasto: Double, otros: Double, proyectado: Boolean, nota: String): KmRow {
            val dias = days(p.inicio, fecha)
            val teor = dias * kmDiaTeoricos
            val desv = km - teor
            val kmDia = if (dias > 0) km / dias else 0.0
            val costeKm = if (km > 0) (dias * (p.cuotaAnual / 365.0) + gasto) / km else 0.0
            val costeKmTotal = if (km > 0) costeKm + otros / km else 0.0
            val desvPct = if (teor == 0.0) null else desv / teor
            return KmRow(id, fecha, dias, km, teor, desv, kmDia, gasto, costeKm, otros, costeKmTotal, desvPct, proyectado, nota)
        }

        val reales = measSorted.map { m ->
            row(m.id, m.fecha, m.km.toDouble(), gastoHasta(m.fecha), otrosHasta(m.fecha), proyectado = false, nota = m.nota)
        }

        // Repostajes: acumulado y coste diario
        var acc = 0.0
        val repostajes = refuelsSorted.map { r ->
            acc += r.importe
            val d = days(p.inicio, r.fecha)
            RefuelRow(r, acc, if (d > 0) acc / d else null)
        }

        // Bloque SEGUIMIENTO (N18:N23)
        val ultima = measSorted.lastOrNull()
        val fechaUltima = ultima?.fecha ?: p.inicio
        val kmUltima = ultima?.km?.toDouble() ?: 0.0
        val combAcum = gastoHasta(fechaUltima)
        val eurKmComb = if (kmUltima > 0) combAcum / kmUltima else 0.0
        val diasUltima = days(p.inicio, fechaUltima)
        val kmDiaRealAcum = if (diasUltima > 0) kmUltima / diasUltima else 0.0
        // Todos los gastos anotados hasta hoy se reparten entre los km de la última medida
        val otrosHastaHoy = otrosHasta(hoy)
        val eurKmOtros = if (kmUltima > 0) otrosHastaHoy / kmUltima else 0.0

        // Km en una fecha, interpolando entre (inicio, 0) y las mediciones
        val puntos = listOf(p.inicio to 0.0) + measSorted.map { it.fecha to it.km.toDouble() }
        fun kmEn(fecha: LocalDate): Double {
            if (!fecha.isAfter(p.inicio)) return 0.0
            val siguiente = puntos.firstOrNull { !it.first.isBefore(fecha) } ?: return kmUltima
            if (siguiente.first == fecha) return siguiente.second
            val anterior = puntos.last { it.first.isBefore(fecha) }
            val tramo = days(anterior.first, siguiente.first)
            return anterior.second + (siguiente.second - anterior.second) * days(anterior.first, fecha) / tramo
        }
        val kmDiaReciente = if (diasUltima >= 180) {
            val desde = fechaUltima.minusDays(180)
            (kmUltima - kmEn(desde)) / 180.0
        } else null

        val kmDiaProy = when (p.modoProyeccion) {
            ModoProyeccion.Manual -> p.kmDiaProyeccion
            ModoProyeccion.Reciente -> kmDiaReciente
            ModoProyeccion.Acumulada -> null
        } ?: if (kmDiaRealAcum > 0) kmDiaRealAcum else kmDiaTeoricos

        // Proyección: el día 15 de cada mes posterior a la última medida y el fin de contrato
        val fechasProy = mutableListOf<LocalDate>()
        var cursor = fechaUltima.withDayOfMonth(15)
        if (!cursor.isAfter(fechaUltima)) cursor = cursor.plusMonths(1)
        while (cursor.isBefore(p.fin)) {
            fechasProy += cursor
            cursor = cursor.plusMonths(1)
        }
        if (p.fin.isAfter(fechaUltima)) fechasProy += p.fin

        val proyeccion = fechasProy.map { f ->
            val km = kmUltima + kmDiaProy * days(fechaUltima, f)
            val gasto = combAcum + eurKmComb * (km - kmUltima)
            row(null, f, km, gasto, otrosHastaHoy + eurKmOtros * (km - kmUltima), proyectado = true, nota = "")
        }

        val kmProyectados = proyeccion.lastOrNull()?.km ?: kmUltima
        val combProyectado = proyeccion.lastOrNull()?.gastoGasolina ?: combAcum

        // LIQUIDACIÓN (N26:N29)
        val umbralAbono = p.kmContratados * p.umbralLiquidacion
        val umbralCargo = p.kmContratados
        var recargo = false
        val abonoCargo = when {
            kmProyectados < umbralAbono -> (umbralAbono - kmProyectados) * p.eurKmNoRecorrido
            kmProyectados <= umbralCargo -> 0.0
            else -> {
                val exceso = kmProyectados - umbralCargo
                recargo = exceso / umbralCargo > p.umbralAjuste
                -exceso * p.eurKmExceso * (if (recargo) p.recargoExceso else 1.0)
            }
        }
        val liquidacion = Liquidacion(kmProyectados, umbralAbono, umbralCargo, abonoCargo, recargo)

        // COSTE TOTAL (N32:N38) e IVA (N41:N50)
        val cuotas = p.meses * p.cuotaMensual
        val primerDiaMesSig = p.inicio.withDayOfMonth(1).plusMonths(1)
        val cuotaIrregular = p.cuotaMensual * days(p.inicio, primerDiaMesSig) / p.inicio.lengthOfMonth()
        val total = cuotas + cuotaIrregular + combProyectado - abonoCargo
        val baseCuotas = p.meses * p.cuotaSinIva
        val ivaCuotas = cuotas - baseCuotas
        val baseIrr = cuotaIrregular / (1 + p.tipoIva)
        val ivaIrr = cuotaIrregular - baseIrr
        val ivaComb = combProyectado - combProyectado / (1 + p.tipoIva)
        val ivaTotal = ivaCuotas + ivaIrr + ivaComb
        val ivaDeducible = ivaTotal * p.pctDeduccion
        val coste = CosteTotal(
            cuotas = cuotas,
            cuotaIrregular = cuotaIrregular,
            combustibleProyectado = combProyectado,
            abono = abonoCargo,
            total = total,
            costePorKm = if (kmProyectados > 0) total / kmProyectados else 0.0,
            deposito = p.deposito,
            baseCuotas = baseCuotas,
            ivaCuotas = ivaCuotas,
            baseCuotaIrregular = baseIrr,
            ivaCuotaIrregular = ivaIrr,
            ivaCombustible = ivaComb,
            ivaTotal = ivaTotal,
            ivaDeducible = ivaDeducible,
            costeNeto = total - ivaDeducible,
        )

        val diasTranscurridos = days(p.inicio, hoy).coerceIn(0, diasContrato)
        val seguimiento = Seguimiento(
            fechaUltima = fechaUltima,
            kmUltima = kmUltima,
            combustibleAcumulado = combAcum,
            eurKmCombustible = eurKmComb,
            kmDiaRealAcumulado = kmDiaRealAcum,
            kmDiaReciente = kmDiaReciente,
            kmDiaProyeccion = kmDiaProy,
            kmDiaTeoricos = kmDiaTeoricos,
            diasContrato = diasContrato,
            diasTranscurridos = diasTranscurridos,
            diasRestantes = diasContrato - diasTranscurridos,
        )

        // MARGEN: ritmo máximo desde la última medida para no pasar de cada umbral
        val diasRestUltima = days(fechaUltima, p.fin).coerceAtLeast(1)
        val kmHastaCargo = p.kmContratados - kmUltima
        val kmHastaAbono = umbralAbono - kmUltima
        val margen = Margen(
            diasRestantes = diasRestUltima,
            kmHastaContratados = kmHastaCargo,
            kmDiaMaxSinCargo = kmHastaCargo / diasRestUltima,
            kmMesMaxSinCargo = kmHastaCargo / diasRestUltima * 30.4375,
            kmHastaUmbralAbono = kmHastaAbono,
            kmDiaMaxConAbono = kmHastaAbono / diasRestUltima,
            kmMesMaxConAbono = kmHastaAbono / diasRestUltima * 30.4375,
        )

        // CONSUMO: litros anotados o derivados del precio; el resto se estima con el precio medio
        val repsHasta = refuelsSorted.filter { !it.fecha.isAfter(fechaUltima) }
        val conLitros = repsHasta.filter { it.litrosEfectivos != null }
        val litrosConocidos = conLitros.sumOf { it.litrosEfectivos!! }
        val importeConLitros = conLitros.sumOf { it.importe }
        val precioMedio = if (litrosConocidos > 0) importeConLitros / litrosConocidos else null
        val sinLitros = repsHasta.filter { it.litrosEfectivos == null }
        val litrosEstimados = if (precioMedio != null) sinLitros.sumOf { it.importe } / precioMedio else 0.0
        val litrosTotales = litrosConocidos + litrosEstimados
        val consumo = Consumo(
            litrosConocidos = litrosConocidos,
            importeConLitros = importeConLitros,
            repostajesSinLitros = sinLitros.size,
            precioMedioLitro = precioMedio,
            litros100km = if (kmUltima > 0 && litrosTotales > 0 && precioMedio != null) litrosTotales / kmUltima * 100 else null,
            estimado = sinLitros.isNotEmpty(),
        )

        // OTROS GASTOS y coste de uso hasta hoy
        val totalGastos = gastosLista.sumOf { it.importe }
        val cuotasDevengadas = cuotaIrregular + p.cuotaMensual * (diasTranscurridos / 30.4375)
        val combHastaHoy = refuelsSorted.filter { !it.fecha.isAfter(hoy) }.sumOf { it.importe }
        val gastos = Gastos(
            lista = gastosLista,
            total = totalGastos,
            porCategoria = gastosLista.groupBy { it.categoria }.mapValues { (_, l) -> l.sumOf { it.importe } },
            costeUsoHastaHoy = cuotasDevengadas + combHastaHoy + totalGastos,
            cuotasDevengadas = cuotasDevengadas,
            enCosteKm = otrosHastaHoy,
            eurKm = eurKmOtros,
        )

        // COSTE POR KM desglosado (hasta la última medida) y proyectado
        val costeKm = CosteKm(
            renting = if (kmUltima > 0) diasUltima * (p.cuotaAnual / 365.0) / kmUltima else 0.0,
            combustible = eurKmComb,
            otros = eurKmOtros,
            total = if (kmUltima > 0) (diasUltima * (p.cuotaAnual / 365.0) + combAcum + otrosHastaHoy) / kmUltima else 0.0,
            proyectado = coste.costePorKm + eurKmOtros,
        )

        // PRÓXIMO AJUSTE ANUAL: aniversarios de la puesta a disposición anteriores al fin
        // Un aniversario que coincide con el final del contrato ya es la liquidación, no un ajuste a cuenta
        val aniversarios = generateSequence(1L) { it + 1 }.map { p.inicio.plusYears(it) }.takeWhile { it.plusMonths(6).isBefore(p.fin) }.toList()
        val proximo = aniversarios.firstOrNull { it.isAfter(hoy) }
        val proximoAjuste = proximo?.let { f ->
            val kmPrev = if (f.isAfter(fechaUltima)) kmUltima + kmDiaProy * days(fechaUltima, f) else kmEn(f)
            val teor = days(p.inicio, f) * kmDiaTeoricos
            val desv = if (teor > 0) (kmPrev - teor) / teor else 0.0
            AjusteAnual(f, days(hoy, f), kmPrev, teor, desv, kotlin.math.abs(desv) <= p.umbralAjuste)
        }

        return Resultado(p, reales, proyeccion, repostajes, seguimiento, liquidacion, coste, margen, consumo, gastos, costeKm, proximoAjuste)
    }
}
