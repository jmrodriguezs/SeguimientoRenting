package com.manursan.seguimientokm

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Precio medio del combustible configurado (Gasolina 95 E5 por defecto) a partir de los datos abiertos del Ministerio de Industria
 * (precios diarios de todas las estaciones de servicio de España, con histórico por fecha).
 */
object FuelPrices {
    private const val BASE = "https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes"
    const val G95 = "1"

    /** Carburantes de automoción (IDProducto del Ministerio); quedan fuera los de calefacción, agrícolas, marítimos y de aviación. */
    val COMBUSTIBLES: List<Pair<String, String>> = listOf(
        "1" to "Gasolina 95 E5", "23" to "Gasolina 95 E10", "20" to "Gasolina 95 E5 Premium",
        "24" to "Gasolina 95 E25", "25" to "Gasolina 95 E85",
        "3" to "Gasolina 98 E5", "21" to "Gasolina 98 E10", "28" to "Gasolina renovable",
        "4" to "Gasóleo A", "5" to "Gasóleo Premium", "27" to "Diésel renovable",
        "8" to "Biodiésel", "16" to "Bioetanol",
        "17" to "GLP (Autogas)", "18" to "Gas natural comprimido (GNC)", "19" to "Gas natural licuado (GNL)",
        "22" to "Hidrógeno",
    )

    fun nombreCombustible(id: String): String = COMBUSTIBLES.firstOrNull { it.first == id }?.second ?: "Gasolina 95 E5"
    private val FECHA_API: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    /** Provincias (código INE) para filtrar el precio medio. */
    val PROVINCIAS: List<Pair<String, String>> = listOf(
        "01" to "Araba/Álava", "02" to "Albacete", "03" to "Alicante", "04" to "Almería", "33" to "Asturias",
        "05" to "Ávila", "06" to "Badajoz", "07" to "Illes Balears", "08" to "Barcelona", "48" to "Bizkaia",
        "09" to "Burgos", "10" to "Cáceres", "11" to "Cádiz", "39" to "Cantabria", "12" to "Castellón",
        "51" to "Ceuta", "13" to "Ciudad Real", "14" to "Córdoba", "15" to "A Coruña", "16" to "Cuenca",
        "20" to "Gipuzkoa", "17" to "Girona", "18" to "Granada", "19" to "Guadalajara", "21" to "Huelva",
        "22" to "Huesca", "23" to "Jaén", "24" to "León", "25" to "Lleida", "27" to "Lugo", "28" to "Madrid",
        "29" to "Málaga", "52" to "Melilla", "30" to "Murcia", "31" to "Navarra", "32" to "Ourense",
        "34" to "Palencia", "35" to "Las Palmas", "36" to "Pontevedra", "26" to "La Rioja", "37" to "Salamanca",
        "38" to "Santa Cruz de Tenerife", "40" to "Segovia", "41" to "Sevilla", "42" to "Soria", "43" to "Tarragona",
        "44" to "Teruel", "45" to "Toledo", "46" to "Valencia", "47" to "Valladolid", "49" to "Zamora", "50" to "Zaragoza",
    ).sortedBy { it.second }

    fun nombreProvincia(id: String?): String = PROVINCIAS.firstOrNull { it.first == id }?.second ?: "Toda España"

    private fun cache(context: Context) = context.getSharedPreferences("precios_g95", Context.MODE_PRIVATE)

    /** Precio medio €/l en esa fecha (y provincia). Consulta la red si no está en caché. */
    suspend fun precioMedio(context: Context, fecha: LocalDate, provinciaId: String?, productoId: String = G95): Result<Double> {
        val key = if (productoId == G95) "${fecha}|${provinciaId ?: "ES"}" else "${fecha}|${provinciaId ?: "ES"}|$productoId"
        cache(context).getFloat(key, -1f).takeIf { it > 0 }?.let { return Result.success(it.toDouble()) }
        return withContext(Dispatchers.IO) {
            runCatching {
                val hoy = LocalDate.now()
                val url = if (fecha.isBefore(hoy)) {
                    val f = fecha.format(FECHA_API)
                    if (provinciaId != null) "$BASE/EstacionesTerrestresHist/FiltroProvinciaProducto/$f/$provinciaId/$productoId"
                    else "$BASE/EstacionesTerrestresHist/FiltroProducto/$f/$productoId"
                } else {
                    if (provinciaId != null) "$BASE/EstacionesTerrestres/FiltroProvinciaProducto/$provinciaId/$productoId"
                    else "$BASE/EstacionesTerrestres/FiltroProducto/$productoId"
                }
                val precio = media(descargar(url))
                // El precio de hoy puede cambiar durante el día: solo se cachean fechas pasadas
                if (fecha.isBefore(hoy)) cache(context).edit().putFloat(key, precio.toFloat()).apply()
                precio
            }
        }
    }

    private fun descargar(url: String): String {
        val con = URL(url).openConnection() as HttpURLConnection
        con.connectTimeout = 15000
        con.readTimeout = 60000
        con.setRequestProperty("Accept", "application/json")
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36")
        try {
            if (con.responseCode != 200) error("Respuesta ${con.responseCode} del servidor de precios")
            return con.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }.removePrefix("﻿")
        } finally {
            con.disconnect()
        }
    }

    /** Media aritmética del campo PrecioProducto de todas las estaciones devueltas. */
    internal fun media(json: String): Double {
        val root = JSONObject(json)
        val lista = root.optJSONArray("ListaEESSPrecio") ?: error("Sin datos de estaciones")
        var suma = 0.0
        var n = 0
        for (i in 0 until lista.length()) {
            val p = lista.getJSONObject(i).optString("PrecioProducto", "").replace(',', '.').toDoubleOrNull() ?: continue
            if (p > 0) { suma += p; n++ }
        }
        if (n == 0) error("No hay precios para esa fecha")
        return suma / n
    }
}
