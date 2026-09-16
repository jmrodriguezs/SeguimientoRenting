package com.manursan.seguimientokm

import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.time.LocalDate

/** Pruebas que necesitan un dispositivo: notificación, widget y copia de seguridad con fotos. */
@RunWith(AndroidJUnit4::class)
class DeviceTest {
    @get:Rule
    val permiso: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun notificacionDeRecordatorio() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancelAll()
        Reminders.notify(context, Reminders.TIPO_KM)
        Reminders.notify(context, Reminders.TIPO_AJUSTE)
        Thread.sleep(500)
        val activas = nm.activeNotifications.map { it.notification.extras.getString("android.title") to it.notification.extras.getString("android.text") }
        assertTrue(activas.size >= 2)
        assertTrue(activas.any { it.first == "Anota los kilómetros" && it.second!!.contains("km/día") })
        // El texto depende de los datos guardados en el dispositivo: basta con que lleve una fecha
        assertTrue(activas.any { it.first == "Ajuste anual de kilómetros" && Regex("""\d{2}/\d{2}/\d{4}""").containsMatchIn(it.second!!) })
        nm.cancelAll()
    }

    @Test
    fun widgetSeInflaConDatos() {
        val r = Calc.compute(Storage.load(context))
        // Se construye igual que en KmWidget y se infla de verdad (valida layout y RemoteViews)
        val method = KmWidget.Companion::class.java.getDeclaredMethod("build", Context::class.java, Resultado::class.java).apply { isAccessible = true }
        val views = method.invoke(KmWidget.Companion, context, r) as RemoteViews
        val root = views.apply(context, LinearLayout(context))
        val km = root.findViewById<TextView>(R.id.w_km).text.toString()
        assertEquals(Fmt.km(r.seguimiento.kmUltima), km)
        assertTrue(root.findViewById<TextView>(R.id.w_margen).text.toString().contains("km/día"))
    }

    @Test
    fun copiaDeSeguridadConFotos() {
        // Foto de prueba en la carpeta de fotos
        val name = "km_test.jpg"; val tique = "tique_test.jpg"
        for (n in listOf(name, tique)) Photos.file(context, n).outputStream().use { Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it) }
        val base = SeedData.create()
        val data = base.copy(
            measurements = base.measurements.mapIndexed { i, m -> if (i == 0) m.copy(foto = name) else m },
            refuels = base.refuels.mapIndexed { i, f -> if (i == 0) f.copy(foto = tique) else f },
        )
        val zip = ByteArrayOutputStream().also { Backup.write(context, data, it) }.toByteArray()
        for (n in listOf(name, tique)) { Photos.delete(context, n); assertTrue(!Photos.file(context, n).exists()) }

        val restaurado = Backup.read(context, zip.inputStream())
        assertEquals(data, restaurado)
        for (n in listOf(name, tique)) { assertTrue(Photos.file(context, n).exists()); Photos.delete(context, n) }

        // Un JSON suelto (formato antiguo) también se restaura
        val json = Storage.toJson(base).toByteArray()
        assertEquals(base, Backup.read(context, json.inputStream()))
    }

    @Test
    fun importarFotoLaReduce() {
        val grande = java.io.File(context.cacheDir, "grande.jpg")
        grande.outputStream().use { Bitmap.createBitmap(3200, 2000, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it) }
        val name = Photos.import(context, android.net.Uri.fromFile(grande))
        val f = Photos.file(context, name)
        assertTrue(f.exists())
        val b = android.graphics.BitmapFactory.decodeFile(f.path)
        assertEquals(1600, maxOf(b.width, b.height))
        assertEquals(1000, minOf(b.width, b.height))
        Photos.delete(context, name)
        grande.delete()
    }

    @Test
    fun informePdfDeUnaPagina() {
        val base = SeedData.create()
        val data = base.copy(
            refuels = base.refuels.map { it.copy(precioLitro = 1.55, precioMercado = true) },
            expenses = listOf(Expense(fecha = LocalDate.of(2026, 9, 1), categoria = ExpenseCategory.Peaje, importe = 45.0)),
        )
        val bytes = PdfReport.build(Calc.compute(data, hoy = LocalDate.of(2026, 9, 14)), hoy = LocalDate.of(2026, 9, 14))
        assertTrue(bytes.size > 2000)
        assertEquals("%PDF", String(bytes, 0, 4, Charsets.US_ASCII))
        val f = java.io.File(context.getExternalFilesDir(null), "informe_test.pdf").apply { writeBytes(bytes) }
        val pfd = android.os.ParcelFileDescriptor.open(f, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
        android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
            assertEquals(1, renderer.pageCount)
            renderer.openPage(0).use { page ->
                val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                java.io.File(context.getExternalFilesDir(null), "informe_test.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }

    /** Genera imágenes del informe, el widget y las notificaciones con los datos guardados (para el manual). */
    @Test
    fun capturasParaElManual() {
        val data = Storage.load(context)
        val r = Calc.compute(data)
        val dir = context.getExternalFilesDir(null)!!
        val pdf = java.io.File(dir, "manual_informe.pdf").apply { writeBytes(PdfReport.build(r)) }
        android.graphics.pdf.PdfRenderer(android.os.ParcelFileDescriptor.open(pdf, android.os.ParcelFileDescriptor.MODE_READ_ONLY)).use { ren ->
            ren.openPage(0).use { page ->
                val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                java.io.File(dir, "manual_informe.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        val method = KmWidget.Companion::class.java.getDeclaredMethod("build", Context::class.java, Resultado::class.java).apply { isAccessible = true }
        val views = method.invoke(KmWidget.Companion, context, r) as RemoteViews
        val root = views.apply(context, LinearLayout(context))
        val w = 1000; val h = 440
        root.measure(android.view.View.MeasureSpec.makeMeasureSpec(w, android.view.View.MeasureSpec.EXACTLY), android.view.View.MeasureSpec.makeMeasureSpec(h, android.view.View.MeasureSpec.EXACTLY))
        root.layout(0, 0, w, h)
        val wb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(wb))
        java.io.File(dir, "manual_widget.png").outputStream().use { wb.compress(Bitmap.CompressFormat.PNG, 100, it) }
        Reminders.notify(context, Reminders.TIPO_KM)
        Reminders.notify(context, Reminders.TIPO_AJUSTE)
    }

    @Test
    fun precioDeMercadoReal() {
        // Consulta real a la API (provincia de Madrid, fecha pasada): comprueba rango razonable y caché
        val fecha = LocalDate.of(2026, 8, 20)
        val r = kotlinx.coroutines.runBlocking { FuelPrices.precioMedio(context, fecha, "28") }
        val precio = r.getOrThrow()
        assertTrue("precio fuera de rango: $precio", precio in 1.0..2.5)
        val cacheado = context.getSharedPreferences("precios_g95", Context.MODE_PRIVATE).getFloat("$fecha|28", -1f)
        assertEquals(precio.toFloat(), cacheado, 1e-6f)
    }

    /** Tique ficticio dibujado en un bitmap: comprueba el OCR real de ML Kit y la extracción. Guarda la imagen para el manual. */
    @Test fun leeTiqueDeRepostaje() {
        val bmp = tiqueFicticio()
        java.io.File(context.getExternalFilesDir(null), "manual_tique.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val texto = kotlinx.coroutines.runBlocking { TicketOcr.reconocer(context, bmp) }
        val d = TicketOcr.parse(texto)
        assertEquals("texto: $texto", 53.83, d.importe!!, 1e-9)
        assertEquals(32.45, d.litros!!, 1e-9)
        assertEquals(1.659, d.precioLitro!!, 1e-9)
        assertEquals("texto: $texto", LocalDate.of(2026, 9, 12), d.fecha)
    }

    private fun tiqueFicticio(): Bitmap {
        val w = 720; val h = 1100
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp); c.drawColor(android.graphics.Color.WHITE)
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK; textSize = 34f; typeface = android.graphics.Typeface.MONOSPACE
        }
        val lineas = listOf(
            "   ESTACION DE SERVICIO EJEMPLO", "   AVDA. DE LA CONSTITUCION, 1", "   CIF B00000000", "",
            "FECHA: 12/09/2026   HORA: 18:32", "SURTIDOR: 3", "", "PRODUCTO: GASOLINA 95 E5",
            "LITROS:          32,45", "PRECIO/L:         1,659", "IMPORTE:          53,83 EUR", "",
            "BASE IMPONIBLE    44,49", "IVA 21%            9,34", "TOTAL             53,83", "",
            "PAGO CON TARJETA", "   GRACIAS POR SU VISITA",
        )
        lineas.forEachIndexed { i, l -> c.drawText(l, 40f, 80f + i * 52f, p) }
        return bmp
    }
}
