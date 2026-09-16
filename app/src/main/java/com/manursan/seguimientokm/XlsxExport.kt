package com.manursan.seguimientokm

import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Genera un .xlsx con la misma disposición y fórmulas que SEGUIMIENTO_KILOMETROS.xlsx
 * (tabla A:H, repostajes I:L, parámetros M:N, notas), sin dependencias externas.
 */
object XlsxExport {

    // Índices de estilo (ver styles.xml más abajo)
    private const val S_GENERAL = 0
    private const val S_BOLD = 1
    private const val S_DATE = 2
    private const val S_INT = 3
    private const val S_D1 = 4
    private const val S_D2 = 5
    private const val S_D4 = 6
    private const val S_THOUS = 7
    private const val S_THOUS2 = 8
    private const val S_PCT = 9
    private const val S_INPUT_INT = 10
    private const val S_INPUT_D2 = 11
    private const val S_INPUT_DATE = 12
    private const val S_NOTE = 13

    private val EPOCH = LocalDate.of(1899, 12, 30)
    private fun serial(d: LocalDate): Long = d.toEpochDay() - EPOCH.toEpochDay()

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    /** Celdas indexadas por fila y columna: el XML exige las celdas de cada fila ordenadas por columna. */
    private class Sheet {
        val rows = sortedMapOf<Int, java.util.TreeMap<Int, String>>()
        private fun put(ref: String, xml: String) {
            rows.getOrPut(rowOf(ref)) { java.util.TreeMap() }[colOf(ref)] = xml
        }
        fun str(ref: String, text: String, style: Int = S_GENERAL) =
            put(ref, "<c r=\"$ref\" s=\"$style\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(text)}</t></is></c>")
        fun num(ref: String, v: Number, style: Int) =
            put(ref, "<c r=\"$ref\" s=\"$style\"><v>${fmt(v)}</v></c>")
        fun date(ref: String, d: LocalDate, style: Int = S_DATE) = num(ref, serial(d), style)
        fun formula(ref: String, f: String, style: Int, cached: Number? = null) {
            val v = if (cached == null) "" else "<v>${fmt(cached)}</v>"
            put(ref, "<c r=\"$ref\" s=\"$style\"><f>${esc(f)}</f>$v</c>")
        }
        /** Fórmula que puede devolver texto vacío (IF(...,"",...)): sin valor cacheado para no fijar un tipo. */
        fun formulaStr(ref: String, f: String, style: Int) =
            put(ref, "<c r=\"$ref\" s=\"$style\" t=\"str\"><f>${esc(f)}</f></c>")
        private fun rowOf(ref: String) = ref.dropWhile { it.isLetter() }.toInt()
        private fun colOf(ref: String) = ref.takeWhile { it.isLetter() }.fold(0) { acc, ch -> acc * 26 + (ch - 'A' + 1) }
        private fun fmt(v: Number): String = when (v) {
            is Int, is Long -> v.toString()
            else -> {
                val d = v.toDouble()
                if (d == Math.floor(d) && Math.abs(d) < 1e15) d.toLong().toString() else d.toString()
            }
        }
    }

    fun build(data: AppData, r: Resultado = Calc.compute(data)): ByteArray {
        val p = data.params
        val s = Sheet()
        val meas = r.reales
        val proy = r.proyeccion
        val reps = r.repostajes

        // --- Filas de la tabla de kilómetros ---
        val firstReal = 6
        val lastReal = if (meas.isEmpty()) 5 else firstReal + meas.size - 1
        val firstProy = lastReal + 1
        val lastProy = if (proy.isEmpty()) lastReal else firstProy + proy.size - 1
        val lastKm = lastProy
        val lastRep = if (reps.isEmpty()) 2 else 1 + reps.size
        val repRange = "\$J\$2:\$J\$$lastRep"
        val repDates = "\$I\$2:\$I\$$lastRep"

        // --- Cabecera y resumen (fila 1-2) ---
        s.str("A1", "MESES RENTING", S_BOLD); s.str("B1", "KILOMETROS AÑO", S_BOLD); s.str("C1", "KILOMETROS DÍA", S_BOLD)
        s.str("D1", "CUOTA ANUAL", S_BOLD); s.str("E1", "COSTE TOTAL", S_BOLD)
        s.num("A2", p.meses, S_INPUT_INT); s.num("B2", p.kmAnio, S_INPUT_INT)
        s.formula("C2", "\$N\$6/(\$N\$4-\$A\$5)", S_D1, r.seguimiento.kmDiaTeoricos)
        s.formula("D2", "\$N\$7*12", S_D1, p.cuotaAnual)
        s.formula("E2", "\$N\$36", S_D2, r.coste.total)

        // --- Tabla de kilómetros (fila 4 cabecera, 5 inicio) ---
        listOf("FECHA", "KILOMETROS REALES", "KILOMETROS TEÓRICOS", "DESVIACIÓN", "KMS DIA REALES", "GASTO GASOLINA", "COSTE KM", "DESVIACION %")
            .forEachIndexed { i, h -> s.str("${'A' + i}4", h, S_BOLD) }
        s.date("A5", p.inicio, S_INPUT_DATE); s.num("B5", 0, S_INPUT_INT); s.num("C5", 0, S_D1)
        s.formulaStr("H5", "IF(C5=0,\"\",D5/C5)", S_PCT)

        fun derived(row: Int, k: KmRow) {
            s.formula("C$row", "(A$row-\$A\$5)*\$C\$2", S_D1, k.teoricos)
            s.formula("D$row", "B$row-C$row", S_D1, k.desviacion)
            s.formula("E$row", "B$row/(A$row-\$A\$5)", S_D1, k.kmDia)
            s.formula("G$row", "((A$row-\$A\$5)*(\$D\$2/365)+F$row)/B$row", S_D2, k.costeKm)
            s.formulaStr("H$row", "IF(C$row=0,\"\",D$row/C$row)", S_PCT)
        }
        meas.forEachIndexed { i, k ->
            val row = firstReal + i
            s.date("A$row", k.fecha, S_INPUT_DATE)
            s.num("B$row", k.km.toLong(), S_INPUT_INT)
            s.formula("F$row", "SUMIFS($repRange,$repDates,\"<=\"&A$row)", S_D1, k.gastoGasolina)
            derived(row, k)
        }
        proy.forEachIndexed { i, k ->
            val row = firstProy + i
            s.date("A$row", k.fecha, S_DATE)
            s.formula("B$row", "B${row - 1}+\$N\$22*(A$row-A${row - 1})", S_INT, k.km)
            s.formula("F$row", "\$N\$20+\$N\$21*(B$row-\$N\$19)", S_D1, k.gastoGasolina)
            derived(row, k)
        }

        // --- Repostajes (I:L) ---
        s.str("I1", "FECHA", S_BOLD); s.str("J1", "REPOSTAJE", S_BOLD); s.str("K1", "ACUMULADO", S_BOLD); s.str("L1", "COSTE DIARIO", S_BOLD)
        reps.forEachIndexed { i, x ->
            val row = 2 + i
            s.date("I$row", x.refuel.fecha, S_INPUT_DATE)
            s.num("J$row", x.refuel.importe, S_INPUT_D2)
            s.formula("K$row", if (row == 2) "J2" else "K${row - 1}+J$row", S_INT, x.acumulado)
            s.formulaStr("L$row", "IF(OR(I$row<=\$A\$5,J$row=\"\"),\"\",K$row/(I$row-\$A\$5))", S_D2)
        }

        // --- Parámetros del contrato (M:N) ---
        val seg = r.seguimiento
        val liq = r.liquidacion
        val c = r.coste
        s.str("M1", "PARAMETROS DEL CONTRATO", S_BOLD)
        s.str("M2", "Contrato n"); s.str("N2", p.contrato, S_INPUT_INT)
        s.str("M3", "Inicio (puesta a disposicion)"); s.formula("N3", "A5", S_DATE, serial(p.inicio))
        s.str("M4", "Fin de contrato"); s.date("N4", p.fin, S_INPUT_DATE)
        s.str("M5", "Plazo (meses)"); s.formula("N5", "A2", S_INT, p.meses)
        s.str("M6", "Km contratados"); s.formula("N6", "B2/12*A2", S_THOUS, p.kmContratados)
        s.str("M7", "Cuota mensual (IVA incl.)"); s.num("N7", p.cuotaMensual, S_INPUT_D2)
        s.str("M8", "Cuota mensual (sin IVA)"); s.num("N8", p.cuotaSinIva, S_INPUT_D2)
        s.str("M9", "   de la cual Rep. Danos"); s.num("N9", p.repDanos, S_INPUT_D2)
        s.str("M10", "Deposito en garantia"); s.num("N10", p.deposito, S_INPUT_D2)
        s.str("M11", "EUR / km no recorrido"); s.num("N11", p.eurKmNoRecorrido, S_D4)
        s.str("M12", "EUR / km exceso"); s.num("N12", p.eurKmExceso, S_D4)
        s.str("M13", "Recargo si exceso > umbral"); s.num("N13", p.recargoExceso, S_INPUT_D2)
        s.str("M14", "Umbral liquidacion (% km)"); s.num("N14", p.umbralLiquidacion, S_PCT)
        s.str("M15", "Umbral ajuste (%)"); s.num("N15", p.umbralAjuste, S_PCT)

        s.str("M17", "SEGUIMIENTO", S_BOLD)
        s.str("M18", "Ultima fila real"); s.num("N18", lastReal, S_INPUT_INT)
        s.str("M19", "Km ultima medida"); s.formula("N19", "INDEX(\$B:\$B,\$N\$18)", S_THOUS, seg.kmUltima)
        s.str("M20", "Combustible acum. (EUR)"); s.formula("N20", "SUMIFS($repRange,$repDates,\"<=\"&INDEX(\$A:\$A,\$N\$18))", S_D2, seg.combustibleAcumulado)
        s.str("M21", "EUR / km combustible"); s.formula("N21", "\$N\$20/\$N\$19", S_D4, seg.eurKmCombustible)
        s.str("M22", "km/dia proyeccion")
        if (p.modoProyeccion == ModoProyeccion.Acumulada) s.formula("N22", "\$N\$23", S_D2, seg.kmDiaProyeccion) else s.num("N22", seg.kmDiaProyeccion, S_INPUT_D2)
        s.str("M23", "km/dia real acumulado (ref.)"); s.formula("N23", "\$N\$19/(INDEX(\$A:\$A,\$N\$18)-\$A\$5)", S_D2, seg.kmDiaRealAcumulado)

        s.str("M25", "LIQUIDACION FIN DE CONTRATO", S_BOLD)
        s.str("M26", "Km proyectados"); s.formula("N26", "B$lastKm", S_THOUS, liq.kmProyectados)
        s.str("M27", "Umbral abono (km, 90%)"); s.formula("N27", "\$N\$6*\$N\$14", S_THOUS, liq.umbralAbono)
        s.str("M28", "Umbral cargo (km contratados)"); s.formula("N28", "\$N\$6", S_THOUS, liq.umbralCargo)
        s.str("M29", "Abono (+) / cargo (-) sin IVA")
        s.formula("N29", "IF(\$N\$26<\$N\$27,(\$N\$27-\$N\$26)*\$N\$11,IF(\$N\$26<=\$N\$28,0,-(\$N\$26-\$N\$28)*\$N\$12*IF((\$N\$26-\$N\$28)/\$N\$28>\$N\$15,\$N\$13,1)))", S_D2, liq.abonoCargo)

        s.str("M31", "COSTE TOTAL DEL CONTRATO", S_BOLD)
        s.str("M32", "Cuotas (${p.meses} x IVA incl.)"); s.formula("N32", "\$N\$5*\$N\$7", S_THOUS2, c.cuotas)
        s.str("M33", "Cuota irregular inicial")
        s.formula("N33", "\$N\$7*(DATE(YEAR(\$N\$3),MONTH(\$N\$3)+1,1)-\$N\$3)/(DATE(YEAR(\$N\$3),MONTH(\$N\$3)+1,1)-DATE(YEAR(\$N\$3),MONTH(\$N\$3),1))", S_THOUS2, c.cuotaIrregular)
        s.str("M34", "Combustible proyectado"); s.formula("N34", "F$lastKm", S_THOUS2, c.combustibleProyectado)
        s.str("M35", "Abono km no recorridos"); s.formula("N35", "\$N\$29", S_THOUS2, c.abono)
        s.str("M36", "TOTAL", S_BOLD); s.formula("N36", "\$N\$32+\$N\$33+\$N\$34-\$N\$35", S_THOUS2, c.total)
        s.str("M37", "Coste por km"); s.formula("N37", "\$N\$36/\$N\$26", S_D4, c.costePorKm)
        s.str("M38", "Deposito (recuperable, no coste)"); s.formula("N38", "\$N\$10", S_THOUS2, c.deposito)

        s.str("M40", "IVA Y DEDUCCION", S_BOLD)
        s.str("M41", "Tipo de IVA"); s.num("N41", p.tipoIva, S_PCT)
        s.str("M42", "% deduccion aplicado"); s.num("N42", p.pctDeduccion, S_PCT)
        s.str("M43", "Base cuotas (${p.meses})"); s.formula("N43", "\$N\$5*\$N\$8", S_THOUS2, c.baseCuotas)
        s.str("M44", "IVA cuotas"); s.formula("N44", "\$N\$32-\$N\$43", S_THOUS2, c.ivaCuotas)
        s.str("M45", "Base cuota irregular"); s.formula("N45", "\$N\$33/(1+\$N\$41)", S_THOUS2, c.baseCuotaIrregular)
        s.str("M46", "IVA cuota irregular"); s.formula("N46", "\$N\$33-\$N\$45", S_THOUS2, c.ivaCuotaIrregular)
        s.str("M47", "IVA combustible (incluido en surtidor)"); s.formula("N47", "\$N\$34-\$N\$34/(1+\$N\$41)", S_THOUS2, c.ivaCombustible)
        s.str("M48", "IVA TOTAL SOPORTADO"); s.formula("N48", "\$N\$44+\$N\$46+\$N\$47", S_THOUS2, c.ivaTotal)
        s.str("M49", "IVA deducible"); s.formula("N49", "\$N\$48*\$N\$42", S_THOUS2, c.ivaDeducible)
        s.str("M50", "Coste neto tras deduccion", S_BOLD); s.formula("N50", "\$N\$36-\$N\$49", S_THOUS2, c.costeNeto)

        // --- Notas (debajo de la tabla de km) ---
        val notasRow = maxOf(44, lastKm + 2)
        val notas = listOf(
            "Generado por la app Seguimiento Renting el ${Fmt.date(LocalDate.now())}. AZUL = dato de entrada manual. AMARILLO (N18) = numero de fila de la ultima medida real; actualizarlo al anadir una medicion a mano.",
            "Contrato ${p.contrato}, ${p.vehiculo}. Los parametros de M1:N15 salen de las Condiciones Particulares y Generales.",
            "C2 (km/dia teoricos) = km contratados / dias reales de contrato, de modo que los teoricos cierran en los km contratados exactos el dia de fin de contrato.",
            "N22 (km/dia proyeccion) es la palanca de escenario. Si esta enlazada a N23 usa la media real acumulada; sustituir por un valor fijo para otros escenarios.",
            "LIQUIDACION, tres tramos: por debajo del umbral de abono hay abono a N11 EUR/km; entre el umbral y los km contratados no hay ni abono ni cargo; por encima se cobra cada km de exceso a N12 EUR/km, multiplicado por N13 si el exceso supera N15.",
            "El colchon del 90% aplica SOLO al abono. En el exceso no hay franquicia: se paga desde el primer km de exceso.",
            "IVA: el deposito en garantia no lleva IVA. El abono por km no recorridos se pacta sin IVA.",
            "N42 = porcentaje de deduccion aplicado. El art. 95 LIVA presume un 50% de afectacion en turismos. Confirmar con asesoria fiscal.",
            "El deposito en garantia no es coste: se devuelve al final previa liquidacion, sin intereses.",
            "No incluidos en el coste total: peajes, aparcamiento, multas, lavado y adblue.",
        )
        s.str("A$notasRow", "NOTAS", S_BOLD)
        notas.forEachIndexed { i, n -> s.str("A${notasRow + 1 + i}", n, S_NOTE) }

        return zip(sheetXml(s))
    }

    private fun sheetXml(s: Sheet): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        sb.append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"4\" topLeftCell=\"A5\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>")
        sb.append("<cols>")
        listOf(1 to 12.0, 2 to 18.8, 3 to 20.8, 4 to 15.8, 5 to 15.0, 6 to 15.0, 7 to 15.8, 8 to 15.8, 9 to 12.0, 10 to 12.0, 11 to 15.8, 12 to 15.8, 13 to 34.0, 14 to 15.0)
            .forEach { (c, w) -> sb.append("<col min=\"$c\" max=\"$c\" width=\"$w\" customWidth=\"1\"/>") }
        sb.append("</cols><sheetData>")
        s.rows.forEach { (r, cells) -> sb.append("<row r=\"$r\">"); cells.values.forEach { sb.append(it) }; sb.append("</row>") }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun stylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<numFmts count="7">
<numFmt numFmtId="164" formatCode="dd/mm/yyyy"/>
<numFmt numFmtId="165" formatCode="0.0"/>
<numFmt numFmtId="166" formatCode="0.0000"/>
<numFmt numFmtId="167" formatCode="0.0%"/>
<numFmt numFmtId="168" formatCode="#,##0"/>
<numFmt numFmtId="169" formatCode="#,##0.00"/>
<numFmt numFmtId="170" formatCode="0.00"/>
</numFmts>
<fonts count="3">
<font><sz val="11"/><name val="Calibri"/></font>
<font><b/><sz val="11"/><name val="Calibri"/></font>
<font><sz val="11"/><color rgb="FF0000FF"/><name val="Calibri"/></font>
</fonts>
<fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FFFFF2CC"/></patternFill></fill></fills>
<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="14">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
<xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
<xf numFmtId="1" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
<xf numFmtId="165" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
<xf numFmtId="170" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
<xf numFmtId="166" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
<xf numFmtId="168" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
<xf numFmtId="169" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
<xf numFmtId="167" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
<xf numFmtId="1" fontId="2" fillId="0" borderId="0" xfId="0" applyNumberFormat="1" applyFont="1"/>
<xf numFmtId="170" fontId="2" fillId="0" borderId="0" xfId="0" applyNumberFormat="1" applyFont="1"/>
<xf numFmtId="164" fontId="2" fillId="0" borderId="0" xfId="0" applyNumberFormat="1" applyFont="1"/>
<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
</cellXfs>
<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>"""

    private fun zip(sheet: String): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { z ->
            fun put(name: String, content: String) {
                z.putNextEntry(ZipEntry(name)); z.write(content.toByteArray(Charsets.UTF_8)); z.closeEntry()
            }
            put("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>""")
            put("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""")
            put("xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Hoja1" sheetId="1" r:id="rId1"/></sheets>
<calcPr fullCalcOnLoad="1"/>
</workbook>""")
            put("xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""")
            put("xl/styles.xml", stylesXml())
            put("xl/worksheets/sheet1.xml", sheet)
        }
        return bos.toByteArray()
    }
}
