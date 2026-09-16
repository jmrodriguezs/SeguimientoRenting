package com.manursan.seguimientokm

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/** Guarda/carga todos los datos en un JSON en el almacenamiento privado de la app. */
object Storage {
    private const val FILE_NAME = "seguimiento_km.json"

    fun load(context: Context): AppData {
        val f = File(context.filesDir, FILE_NAME)
        // Primera instalación: sin datos ni contrato (el usuario los introduce o restaura una copia)
        if (!f.exists()) {
            val vacio = AppData(params = ContractParams.vacio())
            save(context, vacio)
            return vacio
        }
        return runCatching { fromJson(f.readText()) }.getOrElse { AppData(params = ContractParams.vacio()) }
    }

    fun save(context: Context, data: AppData) {
        val f = File(context.filesDir, FILE_NAME)
        val tmp = File(context.filesDir, "$FILE_NAME.tmp")
        tmp.writeText(toJson(data))
        if (!tmp.renameTo(f)) {
            f.writeText(toJson(data))
            tmp.delete()
        }
    }

    fun toJson(data: AppData): String {
        val p = data.params
        val params = JSONObject()
            .put("contrato", p.contrato)
            .put("vehiculo", p.vehiculo)
            .put("matricula", p.matricula)
            .put("empresa", p.empresa).put("telefono1", p.telefono1).put("telefono2", p.telefono2).put("email", p.email)
            .put("inicio", p.inicio.toString())
            .put("fin", p.fin.toString())
            .put("meses", p.meses)
            .put("kmAnio", p.kmAnio)
            .put("cuotaMensual", p.cuotaMensual)
            .put("cuotaSinIva", p.cuotaSinIva)
            .put("repDanos", p.repDanos)
            .put("deposito", p.deposito)
            .put("eurKmNoRecorrido", p.eurKmNoRecorrido)
            .put("eurKmExceso", p.eurKmExceso)
            .put("recargoExceso", p.recargoExceso)
            .put("umbralLiquidacion", p.umbralLiquidacion)
            .put("umbralAjuste", p.umbralAjuste)
            .put("modoProyeccion", p.modoProyeccion.name)
            .put("kmDiaProyeccion", p.kmDiaProyeccion ?: JSONObject.NULL)
            .put("provinciaId", p.provinciaId ?: JSONObject.NULL)
            .put("combustibleId", p.combustibleId)
            .put("tipoIva", p.tipoIva)
            .put("pctDeduccion", p.pctDeduccion)
        val meas = JSONArray()
        data.measurements.forEach {
            meas.put(
                JSONObject().put("id", it.id).put("fecha", it.fecha.toString()).put("km", it.km).put("nota", it.nota)
                    .put("foto", it.foto ?: JSONObject.NULL)
            )
        }
        val ref = JSONArray()
        data.refuels.forEach {
            ref.put(
                JSONObject().put("id", it.id).put("fecha", it.fecha.toString()).put("importe", it.importe).put("nota", it.nota)
                    .put("litros", it.litros ?: JSONObject.NULL)
                    .put("precioLitro", it.precioLitro ?: JSONObject.NULL)
                    .put("precioMercado", it.precioMercado)
                    .put("foto", it.foto ?: JSONObject.NULL)
            )
        }
        val exp = JSONArray()
        data.expenses.forEach {
            exp.put(
                JSONObject().put("id", it.id).put("fecha", it.fecha.toString()).put("categoria", it.categoria.name)
                    .put("importe", it.importe).put("nota", it.nota)
            )
        }
        return JSONObject()
            .put("version", 2)
            .put("params", params)
            .put("measurements", meas)
            .put("refuels", ref)
            .put("expenses", exp)
            .toString(2)
    }

    fun fromJson(text: String): AppData {
        val root = JSONObject(text)
        val pj = root.getJSONObject("params")
        val d = ContractParams()
        val params = ContractParams(
            contrato = pj.optString("contrato", d.contrato),
            vehiculo = pj.optString("vehiculo", d.vehiculo),
            matricula = pj.optString("matricula", d.matricula),
            empresa = pj.optString("empresa", ""), telefono1 = pj.optString("telefono1", ""),
            telefono2 = pj.optString("telefono2", ""), email = pj.optString("email", ""),
            inicio = LocalDate.parse(pj.optString("inicio", d.inicio.toString())),
            fin = LocalDate.parse(pj.optString("fin", d.fin.toString())),
            meses = pj.optInt("meses", d.meses),
            kmAnio = pj.optInt("kmAnio", d.kmAnio),
            cuotaMensual = pj.optDouble("cuotaMensual", d.cuotaMensual),
            cuotaSinIva = pj.optDouble("cuotaSinIva", d.cuotaSinIva),
            repDanos = pj.optDouble("repDanos", d.repDanos),
            deposito = pj.optDouble("deposito", d.deposito),
            eurKmNoRecorrido = pj.optDouble("eurKmNoRecorrido", d.eurKmNoRecorrido),
            eurKmExceso = pj.optDouble("eurKmExceso", d.eurKmExceso),
            recargoExceso = pj.optDouble("recargoExceso", d.recargoExceso),
            umbralLiquidacion = pj.optDouble("umbralLiquidacion", d.umbralLiquidacion),
            umbralAjuste = pj.optDouble("umbralAjuste", d.umbralAjuste),
            kmDiaProyeccion = if (pj.isNull("kmDiaProyeccion")) null else pj.getDouble("kmDiaProyeccion"),
            // Ficheros antiguos (v1): null = media acumulada, valor = manual
            modoProyeccion = if (pj.has("modoProyeccion")) runCatching { ModoProyeccion.valueOf(pj.getString("modoProyeccion")) }.getOrDefault(ModoProyeccion.Acumulada)
                else if (pj.isNull("kmDiaProyeccion")) ModoProyeccion.Acumulada else ModoProyeccion.Manual,
            provinciaId = if (pj.isNull("provinciaId")) null else pj.getString("provinciaId").takeIf { it.isNotBlank() },
            combustibleId = pj.optString("combustibleId", "").takeIf { it.isNotBlank() } ?: FuelPrices.G95,
            tipoIva = pj.optDouble("tipoIva", d.tipoIva),
            pctDeduccion = pj.optDouble("pctDeduccion", d.pctDeduccion),
        )
        val meas = root.optJSONArray("measurements") ?: JSONArray()
        val measurements = (0 until meas.length()).map { i ->
            val o = meas.getJSONObject(i)
            Measurement(
                id = o.optString("id", java.util.UUID.randomUUID().toString()),
                fecha = LocalDate.parse(o.getString("fecha")),
                km = o.getInt("km"),
                nota = o.optString("nota", ""),
                foto = if (o.isNull("foto")) null else o.getString("foto"),
            )
        }
        val ref = root.optJSONArray("refuels") ?: JSONArray()
        val refuels = (0 until ref.length()).map { i ->
            val o = ref.getJSONObject(i)
            Refuel(
                id = o.optString("id", java.util.UUID.randomUUID().toString()),
                fecha = LocalDate.parse(o.getString("fecha")),
                importe = o.getDouble("importe"),
                nota = o.optString("nota", ""),
                litros = if (o.isNull("litros")) null else o.getDouble("litros"),
                precioLitro = if (o.isNull("precioLitro")) null else o.getDouble("precioLitro"),
                precioMercado = o.optBoolean("precioMercado", false),
                foto = if (o.isNull("foto")) null else o.getString("foto"),
            )
        }
        val exp = root.optJSONArray("expenses") ?: JSONArray()
        val expenses = (0 until exp.length()).map { i ->
            val o = exp.getJSONObject(i)
            Expense(
                id = o.optString("id", java.util.UUID.randomUUID().toString()),
                fecha = LocalDate.parse(o.getString("fecha")),
                categoria = runCatching { ExpenseCategory.valueOf(o.getString("categoria")) }.getOrDefault(ExpenseCategory.Otro),
                importe = o.getDouble("importe"),
                nota = o.optString("nota", ""),
            )
        }
        return AppData(params, measurements, refuels, expenses)
    }
}
