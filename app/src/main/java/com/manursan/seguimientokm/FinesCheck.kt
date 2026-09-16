package com.manursan.seguimientokm

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Consulta del Tablón Edictal Único del BOE (sanciones que no se han podido notificar, últimos 3 meses)
 * buscando la matrícula como texto. Se hace por HTTP y se interpreta el HTML de resultados.
 */
object FinesCheck {
    data class Anuncio(val id: String, val fecha: String, val organismo: String, val texto: String, val url: String)
    data class Consulta(val cuando: LocalDateTime, val matricula: String, val anuncios: List<Anuncio>)

    private const val PREFS = "multas"
    private const val UA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    /** Descripción legible de una excepción de red (clase + mensaje), para mostrar al usuario. */
    fun describir(e: Throwable): String {
        val msg = e.message?.takeIf { it.isNotBlank() }
        val causa = e.cause?.let { c -> " (causa: ${c.javaClass.simpleName}${c.message?.let { ": $it" } ?: ""})" } ?: ""
        return "${e.javaClass.simpleName}${msg?.let { ": $it" } ?: ""}$causa"
    }

    /** Prueba de conexión paso a paso con el BOE; devuelve un informe de texto. */
    suspend fun diagnostico(matricula: String): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        val t0 = System.currentTimeMillis()
        fun paso(nombre: String, f: () -> String) {
            try { sb.appendLine("✔ $nombre: ${f()}") } catch (e: Throwable) { sb.appendLine("✘ $nombre: ${describir(e)}") }
        }
        paso("Red disponible") {
            val n = java.net.NetworkInterface.getNetworkInterfaces()?.toList()?.filter { it.isUp && !it.isLoopback } ?: emptyList()
            if (n.isEmpty()) error("sin interfaces de red activas") else n.joinToString { it.name }
        }
        paso("DNS www.boe.es") { java.net.InetAddress.getAllByName("www.boe.es").joinToString { it.hostAddress ?: "?" } }
        paso("HTTPS www.boe.es") {
            val c = URL("https://www.boe.es/").openConnection() as HttpURLConnection
            c.connectTimeout = 15000; c.readTimeout = 15000; c.setRequestProperty("User-Agent", UA)
            try { "código ${c.responseCode}, ${c.contentType}" } finally { c.disconnect() }
        }
        paso("Búsqueda $matricula") {
            val c = URL(url(matricula)).openConnection() as HttpURLConnection
            c.connectTimeout = 15000; c.readTimeout = 30000; c.setRequestProperty("User-Agent", UA)
            try {
                val code = c.responseCode
                val body = (if (code == 200) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
                val anuncios = if (code == 200) parse(body).size else -1
                "código $code, ${body.length} caracteres, " + (if (anuncios >= 0) "$anuncios anuncios" else "sin analizar")
            } finally { c.disconnect() }
        }
        sb.appendLine("Duración: ${System.currentTimeMillis() - t0} ms · Android ${android.os.Build.VERSION.RELEASE} · ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        sb.toString()
    }
    private val FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /** URL de búsqueda del formulario GET del BOE con la matrícula como texto. */
    fun url(matricula: String): String =
        "https://www.boe.es/buscar/notificaciones.php?campo%5B0%5D=DOC&dato%5B0%5D=" + Uri.encode(matricula) +
            "&operador%5B0%5D=and&campo%5B1%5D=DEM&dato%5B1%5D=&operador%5B1%5D=and&campo%5B2%5D=MATERIA&dato%5B2%5D=&operador%5B2%5D=and" +
            "&campo%5B3%5D=NBO&dato%5B3%5D=&operador%5B4%5D=and&campo%5B4%5D=FPU&dato%5B4%5D%5B0%5D=&dato%5B4%5D%5B1%5D=" +
            "&page_hits=50&sort_field%5B0%5D=FPU&sort_order%5B0%5D=desc&sort_field%5B1%5D=id&sort_order%5B1%5D=asc&accion=Buscar"

    /** Normaliza una matrícula escrita a mano: mayúsculas, sin espacios ni guiones. */
    fun normalizar(m: String) = m.trim().uppercase().replace(" ", "").replace("-", "")

    suspend fun consultar(context: Context, matricula: String, guardar: Boolean = true): Result<Consulta> = withContext(Dispatchers.IO) {
        runCatching { consultarBloqueante(context, matricula, guardar) }
    }

    /** Versión síncrona (para el receptor de alarmas). Guarda el resultado si `guardar`. */
    fun consultarBloqueante(context: Context, matricula: String, guardar: Boolean = true): Consulta {
        val con = URL(url(matricula)).openConnection() as HttpURLConnection
        con.connectTimeout = 15000; con.readTimeout = 30000
        con.setRequestProperty("User-Agent", UA)
        con.setRequestProperty("Accept", "text/html,application/xhtml+xml")
        con.setRequestProperty("Accept-Language", "es-ES,es;q=0.9")
        con.instanceFollowRedirects = true
        val html = try {
            if (con.responseCode != 200) error("Respuesta ${con.responseCode} del BOE")
            con.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally { con.disconnect() }
        val consulta = Consulta(LocalDateTime.now(), matricula, parse(html))
        if (guardar) save(context, consulta)
        return consulta
    }

    /** Extrae los anuncios de la página de resultados; lista vacía si "No se han encontrado documentos". */
    internal fun parse(html: String): List<Anuncio> {
        if (html.contains("No se han encontrado documentos")) return emptyList()
        if (!html.contains("resultado-busqueda")) error("Respuesta del BOE no reconocida")
        val items = Regex("""<li class="resultado-busqueda">(.*?)</li>\s*</ul>""", RegexOption.DOT_MATCHES_ALL).findAll(html)
        return items.mapNotNull { m ->
            val b = m.groupValues[1]
            val fecha = Regex("""de (\d{2}/\d{2}/\d{4})</p>""").find(b)?.groupValues?.get(1) ?: ""
            val organismo = Regex("""<p class="linea-pub">(.*?)</p>""", RegexOption.DOT_MATCHES_ALL).find(b)?.groupValues?.get(1)?.let(::limpiar) ?: ""
            val texto = Regex("""<p class="linea-pub">.*?</p>\s*<p>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL).find(b)?.groupValues?.get(1)?.let(::limpiar) ?: ""
            val href = Regex("""href="([^"]*not\.php\?id=([^"&]+))""").find(b) ?: return@mapNotNull null
            Anuncio(href.groupValues[2], fecha, organismo, texto, "https://www.boe.es" + href.groupValues[1])
        }.toList()
    }

    private fun limpiar(s: String) = s.replace(Regex("<[^>]+>"), "").replace("&nbsp;", " ").replace("&amp;", "&").replace(Regex("\\s+"), " ").trim()

    fun save(context: Context, c: Consulta) {
        val arr = JSONArray()
        c.anuncios.forEach { arr.put(JSONObject().put("id", it.id).put("fecha", it.fecha).put("organismo", it.organismo).put("texto", it.texto).put("url", it.url)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("cuando", c.cuando.format(FMT)).putString("matricula", c.matricula).putString("anuncios", arr.toString()).apply()
    }

    fun load(context: Context): Consulta? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cuando = p.getString("cuando", null) ?: return null
        val arr = JSONArray(p.getString("anuncios", "[]"))
        val lista = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Anuncio(o.getString("id"), o.getString("fecha"), o.getString("organismo"), o.getString("texto"), o.getString("url"))
        }
        return Consulta(LocalDateTime.parse(cuando, FMT), p.getString("matricula", "") ?: "", lista)
    }
}
