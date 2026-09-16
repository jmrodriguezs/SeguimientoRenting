package com.manursan.seguimientokm

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formateo en estilo español (punto de miles, coma decimal, dd/MM/yyyy). */
object Fmt {
    private val es = Locale.forLanguageTag("es-ES")
    private val sym = DecimalFormatSymbols(es)
    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", es)
    private val dateShortFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yy", es)

    private fun df(pattern: String) = DecimalFormat(pattern, sym)

    fun date(d: LocalDate): String = d.format(dateFmt)
    fun dateShort(d: LocalDate): String = d.format(dateShortFmt)
    fun int(v: Double): String = df("#,##0").format(v)
    fun int(v: Long): String = df("#,##0").format(v)
    fun dec(v: Double, decimals: Int = 2): String =
        df(if (decimals == 0) "#,##0" else "#,##0." + "0".repeat(decimals)).format(v)
    fun eur(v: Double, decimals: Int = 2): String = dec(v, decimals) + " €"
    fun km(v: Double): String = int(v) + " km"
    fun pct(v: Double?, decimals: Int = 1): String = if (v == null) "—" else dec(v * 100, decimals) + " %"
    fun signed(v: Double, decimals: Int = 0, suffix: String = ""): String =
        (if (v > 0) "+" else "") + dec(v, decimals) + suffix

    /** Parsea números escritos con coma o punto decimal. */
    fun parseDouble(s: String): Double? = s.trim().replace(" ", "").replace(',', '.').toDoubleOrNull()
    fun parseInt(s: String): Int? = s.trim().replace(" ", "").replace(".", "").toIntOrNull()
}
