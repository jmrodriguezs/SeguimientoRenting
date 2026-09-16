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
        assertTrue(activas.any { it.first == "Ajuste anual de kilómetros" && it.second!!.contains("29/12/2026") })
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
        val name = "km_test.jpg"
        Photos.file(context, name).outputStream().use { Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it) }
        val base = SeedData.create()
        val data = base.copy(measurements = base.measurements.mapIndexed { i, m -> if (i == 0) m.copy(foto = name) else m })
        val zip = ByteArrayOutputStream().also { Backup.write(context, data, it) }.toByteArray()
        Photos.delete(context, name)
        assertTrue(!Photos.file(context, name).exists())

        val restaurado = Backup.read(context, zip.inputStream())
        assertEquals(data, restaurado)
        assertTrue(Photos.file(context, name).exists())
        Photos.delete(context, name)

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
}
