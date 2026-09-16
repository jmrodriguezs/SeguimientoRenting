package com.manursan.seguimientokm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

/** Valores de referencia tomados de SEGUIMIENTO_KILOMETROS.xlsx (celdas cacheadas). */
class CalcTest {
    // El Excel fija N22 = 24,54 a mano; se replica aquí para comparar celda a celda.
    private val excel = SeedData.create().let { it.copy(params = it.params.copy(modoProyeccion = ModoProyeccion.Manual, kmDiaProyeccion = 24.54)) }
    private val r = Calc.compute(excel, hoy = LocalDate.of(2026, 9, 12))

    @Test fun kmDiaTeoricos() = assertEquals(27.3224043715847, r.seguimiento.kmDiaTeoricos, 1e-9)

    @Test fun filasReales() {
        val f = r.reales.first { it.fecha == LocalDate.of(2026, 9, 12) }   // fila 14 del Excel
        assertEquals(6320.0, f.km, 0.0)
        assertEquals(7021.8579, f.teoricos, 1e-3)
        assertEquals(-701.8579, f.desviacion, 1e-3)
        assertEquals(24.5914, f.kmDia, 1e-3)
        assertEquals(512.49, f.gastoGasolina, 1e-9)
        assertEquals(0.4761, f.costeKm, 1e-3)
        assertEquals(-0.1, f.desviacionPct!!, 1e-4)

        val f6 = r.reales.first { it.fecha == LocalDate.of(2026, 2, 27) }  // fila 6
        assertEquals(1639.3443, f6.teoricos, 1e-3)
        assertEquals(167.19, f6.gastoGasolina, 1e-9)
        assertEquals(0.3848, f6.costeKm, 1e-3)
        assertEquals(0.1889, f6.desviacionPct!!, 1e-3)
    }

    @Test fun repostajes() {
        val ultimo = r.repostajes.last()
        assertEquals(512.49, ultimo.acumulado, 1e-9)
        assertEquals(2.066491935483871, ultimo.costeDiario!!, 1e-9)
        assertEquals(16.0, r.repostajes[1].costeDiario!!, 1e-9)
        assertEquals(null, r.repostajes[0].costeDiario)  // mismo día que el inicio → vacío en Excel
    }

    @Test fun seguimiento() {
        assertEquals(6320.0, r.seguimiento.kmUltima, 0.0)
        assertEquals(512.49, r.seguimiento.combustibleAcumulado, 1e-9)
        assertEquals(0.08109018987341772, r.seguimiento.eurKmCombustible, 1e-9)
        assertEquals(24.591439688715955, r.seguimiento.kmDiaRealAcumulado, 1e-9)
        assertEquals(24.54, r.seguimiento.kmDiaProyeccion, 0.0)
    }

    @Test fun proyeccion() {
        val oct = r.proyeccion.first { it.fecha == LocalDate.of(2026, 10, 15) }  // fila 15
        assertEquals(7129.82, oct.km, 1e-6)
        assertEquals(578.1585, oct.gastoGasolina, 1e-3)
        assertEquals(0.4762, oct.costeKm, 1e-3)
        val fin = r.proyeccion.last()
        assertEquals(LocalDate.of(2028, 12, 31), fin.fecha)
        assertEquals(26958.14, fin.km, 1e-6)
        assertEquals(30000.0, fin.teoricos, 1e-9)
        assertEquals(2186.0407, fin.gastoGasolina, 1e-3)
    }

    @Test fun liquidacion() {
        assertEquals(26958.14, r.liquidacion.kmProyectados, 1e-6)
        assertEquals(27000.0, r.liquidacion.umbralAbono, 0.0)
        assertEquals(30000.0, r.liquidacion.umbralCargo, 0.0)
        assertEquals(0.699062, r.liquidacion.abonoCargo, 1e-6)
        assertFalse(r.liquidacion.recargoAplicado)
    }

    @Test fun liquidacionTramos() {
        val base = SeedData.create()
        fun con(kmDia: Double) = Calc.compute(base.copy(params = base.params.copy(modoProyeccion = ModoProyeccion.Manual, kmDiaProyeccion = kmDia))).liquidacion
        // Entre 27.000 y 30.000: sin ajuste
        val medio = con(27.5)
        assert(medio.kmProyectados in 27000.0..30000.0)
        assertEquals(0.0, medio.abonoCargo, 0.0)
        // Exceso moderado (< 10 %): 0,0217 €/km sin recargo
        val exceso = con(29.0)
        assert(exceso.kmProyectados > 30000.0)
        assertEquals(-(exceso.kmProyectados - 30000.0) * 0.0217, exceso.abonoCargo, 1e-9)
        assertFalse(exceso.recargoAplicado)
        // Exceso > 10 %: recargo ×1,25
        val grande = con(40.0)
        assert(grande.recargoAplicado)
        assertEquals(-(grande.kmProyectados - 30000.0) * 0.0217 * 1.25, grande.abonoCargo, 1e-9)
    }

    @Test fun costeTotalEIva() {
        val c = r.coste
        assertEquals(10636.56, c.cuotas, 1e-9)
        assertEquals(28.59290322580645, c.cuotaIrregular, 1e-9)
        assertEquals(2186.0406912341787, c.combustibleProyectado, 1e-9)
        assertEquals(12850.494532459985, c.total, 1e-6)
        assertEquals(0.4766832775725617, c.costePorKm, 1e-9)
        assertEquals(8790.48, c.baseCuotas, 1e-9)
        assertEquals(1846.08, c.ivaCuotas, 1e-9)
        assertEquals(23.63049853372434, c.baseCuotaIrregular, 1e-9)
        assertEquals(4.96240469208211, c.ivaCuotaIrregular, 1e-9)
        assertEquals(379.3954918670888, c.ivaCombustible, 1e-9)
        assertEquals(2230.437896559171, c.ivaTotal, 1e-6)
        assertEquals(1115.2189482795854, c.ivaDeducible, 1e-6)
        assertEquals(11735.2755841804, c.costeNeto, 1e-6)
    }

    @Test fun proyeccionAutomatica() {
        // Sin valor manual, el ritmo de proyección es la media real acumulada
        val auto = Calc.compute(SeedData.create()).seguimiento
        assertEquals(auto.kmDiaRealAcumulado, auto.kmDiaProyeccion, 1e-12)
    }

    @Test fun sinMediciones() {
        val d = SeedData.create().let { it.copy(measurements = emptyList()) }
        val res = Calc.compute(d)
        assertEquals(0.0, res.seguimiento.kmUltima, 0.0)
        assertEquals(res.seguimiento.kmDiaTeoricos, res.seguimiento.kmDiaProyeccion, 1e-9)
        assertEquals(30000.0, res.liquidacion.kmProyectados, 1e-6)
    }

    @Test fun contratoVacioNoRompe() {
        val r = Calc.compute(AppData(params = ContractParams.vacio(LocalDate.of(2026, 9, 14))), hoy = LocalDate.of(2026, 9, 14))
        assertEquals(0.0, r.params.kmContratados, 0.0)
        assertEquals(0.0, r.seguimiento.kmUltima, 0.0)
        assertEquals(0.0, r.coste.total, 0.0)
        assertEquals(0.0, r.costeKm.total, 0.0)
        assert(r.reales.isEmpty() && r.repostajes.isEmpty())
        assert(r.liquidacion.abonoCargo == 0.0)
    }

    @Test fun validacionDeParametros() {
        val ok = ContractParams()  // los del Excel: 29/12/2025 → 31/12/2028, 36 meses, 244,18 × 1,21 = 295,46
        assertEquals(emptyList<String>(), ok.errores())
        assert(ok.configurado)
        assertFalse(ContractParams.vacio().configurado)
        assert(ok.copy(fin = ok.inicio).errores().any { it.contains("posterior") })
        assert(ok.copy(meses = 24).errores().any { it.contains("plazo", ignoreCase = true) })
        assert(ok.copy(meses = 37).errores().isEmpty())   // ±1 mes de tolerancia
        assert(ok.copy(cuotaSinIva = 300.0).errores().any { it.contains("no puede superar") })
        assert(ok.copy(cuotaSinIva = 200.0).errores().any { it.contains("no coincide") })
        assert(ok.copy(kmAnio = 0).errores().any { it.contains("km/año") })
        assert(ok.copy(tipoIva = 1.5).errores().any { it.contains("porcentajes") })
    }

    @Test fun jsonRoundTrip() {
        val base = SeedData.create()
        val d = base.copy(
            params = base.params.copy(modoProyeccion = ModoProyeccion.Reciente, provinciaId = "28"),
            measurements = base.measurements.mapIndexed { i, m -> if (i == 0) m.copy(foto = "km_1.jpg") else m },
            refuels = base.refuels.mapIndexed { i, r -> if (i == 0) r.copy(litros = 33.5, precioLitro = 1.49, precioMercado = true) else r },
            expenses = listOf(Expense(fecha = LocalDate.of(2026, 3, 1), categoria = ExpenseCategory.Peaje, importe = 12.5, nota = "AP-7")),
        )
        assertEquals(d, Storage.fromJson(Storage.toJson(d)))
    }

    @Test fun jsonAntiguoMigraElModoDeProyeccion() {
        // v1: kmDiaProyeccion null = media acumulada; con valor = manual
        val auto = Storage.fromJson("""{"params":{"kmDiaProyeccion":null},"measurements":[],"refuels":[]}""")
        assertEquals(ModoProyeccion.Acumulada, auto.params.modoProyeccion)
        val manual = Storage.fromJson("""{"params":{"kmDiaProyeccion":22.1},"measurements":[],"refuels":[]}""")
        assertEquals(ModoProyeccion.Manual, manual.params.modoProyeccion)
        assertEquals(22.1, manual.params.kmDiaProyeccion!!, 0.0)
    }

    @Test fun ritmoReciente() {
        // Últimos 180 días antes del 12/09/2026: desde el 16/03/2026, km interpolados entre 28/02 (1963) y 12/04 (3012)
        val s = Calc.compute(SeedData.create(), hoy = LocalDate.of(2026, 9, 12)).seguimiento
        val kmMarzo16 = 1963 + (3012 - 1963) * 16.0 / 43.0
        assertEquals((6320 - kmMarzo16) / 180.0, s.kmDiaReciente!!, 1e-9)
        assert(s.kmDiaReciente!! < s.kmDiaRealAcumulado)  // el ritmo ha bajado
        val rec = Calc.compute(SeedData.create().let { it.copy(params = it.params.copy(modoProyeccion = ModoProyeccion.Reciente)) })
        assertEquals(rec.seguimiento.kmDiaReciente!!, rec.seguimiento.kmDiaProyeccion, 0.0)
    }

    @Test fun margen() {
        val m = Calc.compute(SeedData.create(), hoy = LocalDate.of(2026, 9, 12)).margen
        val diasRest = 841L  // 12/09/2026 -> 31/12/2028
        assertEquals(diasRest, m.diasRestantes)
        assertEquals(30000.0 - 6320, m.kmHastaContratados, 0.0)
        assertEquals((30000.0 - 6320) / diasRest, m.kmDiaMaxSinCargo, 1e-9)
        assertEquals((27000.0 - 6320) / diasRest, m.kmDiaMaxConAbono, 1e-9)
    }

    @Test fun consumo() {
        val base = SeedData.create()
        // Sin litros ni precios: no hay consumo
        assertEquals(null, Calc.compute(base).consumo.litros100km)
        // Todos con precio 1,50 €/l: litros = importe / 1,5
        val con = base.copy(refuels = base.refuels.map { it.copy(precioLitro = 1.5, precioMercado = true) })
        val c = Calc.compute(con, hoy = LocalDate.of(2026, 9, 12)).consumo
        assertEquals(512.49 / 1.5, c.litrosConocidos, 1e-9)
        assertEquals(1.5, c.precioMedioLitro!!, 1e-9)
        assertEquals(512.49 / 1.5 / 6320 * 100, c.litros100km!!, 1e-9)
        assertFalse(c.estimado)
        // Solo uno con litros anotados: el resto se estima con su precio medio
        val parcial = base.copy(refuels = base.refuels.mapIndexed { i, r -> if (i == 0) r.copy(litros = 40.0) else r })
        val cp = Calc.compute(parcial, hoy = LocalDate.of(2026, 9, 12)).consumo
        assert(cp.estimado); assertEquals(14, cp.repostajesSinLitros)
        assertEquals(50.0 / 40.0, cp.precioMedioLitro!!, 1e-9)
        assertEquals((40.0 + (512.49 - 50.0) / 1.25) / 6320 * 100, cp.litros100km!!, 1e-9)
    }

    @Test fun otrosGastosYAjusteAnual() {
        val base = SeedData.create()
        val d = base.copy(expenses = listOf(
            Expense(fecha = LocalDate.of(2026, 3, 1), categoria = ExpenseCategory.Peaje, importe = 12.5),
            Expense(fecha = LocalDate.of(2026, 4, 1), categoria = ExpenseCategory.Peaje, importe = 7.5),
            Expense(fecha = LocalDate.of(2026, 5, 1), categoria = ExpenseCategory.Lavado, importe = 9.0),
        ))
        val r = Calc.compute(d, hoy = LocalDate.of(2026, 9, 12))
        assertEquals(29.0, r.gastos.total, 1e-9)
        assertEquals(20.0, r.gastos.porCategoria[ExpenseCategory.Peaje]!!, 1e-9)
        assertEquals(r.gastos.cuotasDevengadas + 512.49 + 29.0, r.gastos.costeUsoHastaHoy, 1e-9)
        // Otros gastos hasta la última medida entran en el coste/km total, no en la columna G del Excel
        val ultima = r.reales.last()
        assertEquals(29.0, ultima.otrosGastos, 1e-9)
        assertEquals(ultima.costeKm + 29.0 / 6320, ultima.costeKmTotal, 1e-9)
        assertEquals(29.0 / 6320, r.costeKm.otros, 1e-9)
        assertEquals(r.costeKm.renting + r.costeKm.combustible + r.costeKm.otros, r.costeKm.total, 1e-9)
        assertEquals(r.coste.costePorKm + 29.0 / 6320, r.costeKm.proyectado, 1e-9)
        val a = r.proximoAjuste!!
        assertEquals(LocalDate.of(2026, 12, 29), a.fecha)
        assertEquals(108L, a.diasHasta)
        assertEquals(365 * r.seguimiento.kmDiaTeoricos, a.kmTeoricos, 1e-9)
        assertEquals(6320 + r.seguimiento.kmDiaProyeccion * 108, a.kmPrevistos, 1e-9)
    }
}

class RemindersTest {
    @Test fun proximoMensual() {
        val now = java.time.LocalDateTime.of(2026, 9, 13, 12, 0)
        assertEquals(java.time.LocalDateTime.of(2026, 10, 1, 10, 0), Reminders.nextMonthly(1, now))
        assertEquals(java.time.LocalDateTime.of(2026, 9, 15, 10, 0), Reminders.nextMonthly(15, now))
        assertEquals(java.time.LocalDateTime.of(2026, 9, 13, 10, 0), Reminders.nextMonthly(13, java.time.LocalDateTime.of(2026, 9, 13, 9, 0)))
    }

    @Test fun proximoAjuste() {
        val p = ContractParams()
        val now = java.time.LocalDateTime.of(2026, 9, 13, 12, 0)
        assertEquals(java.time.LocalDateTime.of(2026, 11, 29, 10, 0), Reminders.nextAjuste(p, 30, now))
        // Tras el último aniversario (29/12/2027) no hay más avisos
        assertEquals(null, Reminders.nextAjuste(p, 30, java.time.LocalDateTime.of(2028, 1, 1, 12, 0)))
    }
}

class FinesCheckTest {
    @Test fun sinResultados() {
        assertEquals(0, FinesCheck.parse("<html><p>No se han encontrado documentos que satisfagan sus criterios de búsqueda</p></html>").size)
    }

    @Test fun conResultados() {
        val html = """<ul><li class="resultado-busqueda"> <p class="linea-dem">Suplemento de Notificaciones del Boletín Oficial del Estado <abbr title="número">núm.</abbr> 227, de 14/09/2026</p> <p class="linea-pub">Ministerio del Interior - Jefatura Central de Tráfico</p> <p>Jefatura Provincial de Tráfico de Madrid. Anuncio de notificación de 9 de septiembre de 2026 en procedimiento sancionador.</p> <ul> <li class="puntoPDF"> <a href="/boe_n/dias/2026/09/14/not.php?id=BOE-N-2026-693209" title="Documento PDF">PDF</a></li> </ul></li>
        <li class="resultado-busqueda"> <p class="linea-dem">... de 10/09/2026</p> <p class="linea-pub">Ayuntamiento de Madrid</p> <p>Anuncio de notificación de sanción de tráfico.</p> <ul> <li class="puntoPDF"> <a href="/boe_n/dias/2026/09/10/not.php?id=BOE-N-2026-680001&x=1">PDF</a></li> </ul></li></ul>"""
        val a = FinesCheck.parse(html)
        assertEquals(2, a.size)
        assertEquals("BOE-N-2026-693209", a[0].id)
        assertEquals("14/09/2026", a[0].fecha)
        assertEquals("Ministerio del Interior - Jefatura Central de Tráfico", a[0].organismo)
        assert(a[0].texto.startsWith("Jefatura Provincial de Tráfico de Madrid"))
        assertEquals("https://www.boe.es/boe_n/dias/2026/09/14/not.php?id=BOE-N-2026-693209", a[0].url)
        assertEquals("Ayuntamiento de Madrid", a[1].organismo)
    }

    @Test fun respuestaNoReconocida() {
        assert(runCatching { FinesCheck.parse("<html>mantenimiento</html>") }.isFailure)
    }
}

class FuelPricesTest {
    @Test fun mediaDePrecios() {
        val json = """{"Fecha":"13/09/2026","ListaEESSPrecio":[{"PrecioProducto":"1,500"},{"PrecioProducto":"1,700"},{"PrecioProducto":""},{"PrecioProducto":"1,600"}],"ResultadoConsulta":"OK"}"""
        assertEquals(1.6, FuelPrices.media(json), 1e-9)
    }
}

class XlsxExportTest {
    @Test fun generaXlsxValido() {
        val data = SeedData.create()
        val bytes = XlsxExport.build(data)
        val names = mutableListOf<String>()
        var sheet = ""
        java.util.zip.ZipInputStream(bytes.inputStream()).use { z ->
            var e = z.nextEntry
            while (e != null) {
                names += e.name
                if (e.name == "xl/worksheets/sheet1.xml") sheet = z.readBytes().toString(Charsets.UTF_8)
                e = z.nextEntry
            }
        }
        assert("xl/workbook.xml" in names && "xl/styles.xml" in names)
        // Celdas clave con la misma disposición que el Excel original
        assert("<c r=\"A5\"" in sheet)                       // inicio
        assert("<c r=\"B14\"" in sheet && "<v>6320</v>" in sheet) // última medida real en la fila 14
        assert("<c r=\"N18\"" in sheet && "<v>14</v>" in sheet)   // N18 = 14, como en el Excel
        assert("<f>INDEX(\$B:\$B,\$N\$18)</f>" in sheet)
        assert("<f>SUMIFS(\$J\$2:\$J\$16,\$I\$2:\$I\$16,&quot;&lt;=&quot;&amp;A6)</f>" in sheet)
        // Las celdas de cada fila van ordenadas por columna (fila 44: A44 antes que M44/N44)
        val row44 = sheet.substringAfter("<row r=\"44\">").substringBefore("</row>")
        assert(row44.indexOf("r=\"A44\"") < row44.indexOf("r=\"M44\""))
        // El XML de la hoja es bien formado
        javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(sheet.byteInputStream())
        // Copia en build/ para poder abrirla con Excel/LibreOffice
        java.io.File("build/export_test.xlsx").also { it.parentFile.mkdirs() }.writeBytes(bytes)
    }
}
