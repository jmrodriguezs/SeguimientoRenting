package com.manursan.seguimientokm

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Copia de seguridad: ZIP con datos.json y las fotos. La restauración admite también un JSON suelto. */
object Backup {
    fun write(context: Context, data: AppData, out: OutputStream) {
        ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry("datos.json"))
            z.write(Storage.toJson(data).toByteArray(Charsets.UTF_8))
            z.closeEntry()
            data.measurements.mapNotNull { it.foto }.forEach { name ->
                val f = Photos.file(context, name)
                if (f.exists()) {
                    z.putNextEntry(ZipEntry("fotos/$name"))
                    f.inputStream().use { it.copyTo(z) }
                    z.closeEntry()
                }
            }
        }
    }

    /** Lee un ZIP o un JSON; deja las fotos en su carpeta y devuelve los datos. */
    fun read(context: Context, input: InputStream): AppData {
        val bytes = input.readBytes()
        val esZip = bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()
        if (!esZip) return Storage.fromJson(bytes.toString(Charsets.UTF_8))
        var json: String? = null
        ZipInputStream(bytes.inputStream()).use { z ->
            var e = z.nextEntry
            while (e != null) {
                when {
                    e.name == "datos.json" -> json = z.readBytes().toString(Charsets.UTF_8)
                    e.name.startsWith("fotos/") && !e.isDirectory -> {
                        val name = File(e.name).name
                        Photos.file(context, name).outputStream().use { z.copyTo(it) }
                    }
                }
                e = z.nextEntry
            }
        }
        return Storage.fromJson(json ?: error("La copia no contiene datos.json"))
    }
}
