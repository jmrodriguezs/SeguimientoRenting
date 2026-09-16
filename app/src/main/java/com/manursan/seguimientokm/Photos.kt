package com.manursan.seguimientokm

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID

/** Fotos del cuentakilómetros: se guardan reducidas (máx. 1600 px, JPEG 85) en filesDir/fotos. */
object Photos {
    private const val MAX = 1600

    fun dir(context: Context): File = File(context.filesDir, "fotos").also { it.mkdirs() }
    fun file(context: Context, name: String): File = File(dir(context), name)

    /** Fichero temporal (en cache/) que la cámara rellena; se expone mediante FileProvider. */
    fun tempCaptureFile(context: Context): File =
        File(File(context.cacheDir, "captura").also { it.mkdirs() }, "captura.jpg")

    /** Importa una imagen (de la cámara o de la galería) reducida y orientada; devuelve el nombre de fichero. */
    fun import(context: Context, uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > MAX * 2 || bounds.outHeight / sample > MAX * 2) sample *= 2
        var bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: error("No se pudo leer la imagen")
        val scale = minOf(1f, MAX.toFloat() / maxOf(bmp.width, bmp.height))
        val rotation = when (ExifInterface(bytes.inputStream()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (scale < 1f || rotation != 0f) {
            val m = Matrix().apply { postScale(scale, scale); postRotate(rotation) }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        val name = "km_${UUID.randomUUID()}.jpg"
        file(context, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return name
    }

    fun delete(context: Context, name: String?) {
        if (name != null) file(context, name).delete()
    }
}
