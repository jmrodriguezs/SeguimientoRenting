package com.manursan.seguimientokm

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.BackupManager
import android.app.backup.FullBackupDataOutput
import android.content.Context
import android.os.ParcelFileDescriptor
import java.io.File

/**
 * Copia automática de Android (Google Drive).
 *
 * Está **desactivada por defecto**: los datos viven solo en el dispositivo y la forma de llevarlos
 * a otro móvil es la copia de seguridad en ZIP del menú. Quien quiera la copia en la nube la activa
 * en Ajustes; mientras esté desactivada no se sube nada y, al reinstalar, la aplicación empieza vacía.
 */
object CopiaNube {
    private const val PREFS = "ui"
    const val CLAVE = "copiaNube"

    fun activada(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(CLAVE, false)

    /** Guarda la preferencia y avisa al sistema para que programe (o deje de programar) la copia. */
    fun activar(context: Context, valor: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(CLAVE, valor).apply()
        runCatching { BackupManager(context).dataChanged() }
    }

    /** true si en este dispositivo ya se decidió alguna vez (sirve para detectar restauraciones antiguas). */
    fun decidida(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(CLAVE)

    /** Datos que Android restauró de la nube y que están a la espera de que el usuario decida. */
    const val EN_ESPERA = "restaurado_drive.json"

    fun ficheroEnEspera(context: Context) = File(context.filesDir, EN_ESPERA)

    fun hayRestaurada(context: Context): Boolean = ficheroEnEspera(context).exists()
}

/**
 * Agente de copia: el sistema lo llama para respaldar y restaurar. Solo entrega datos cuando el
 * usuario ha activado la copia en la nube; si está desactivada no escribe nada, de modo que no
 * queda copia que restaurar al reinstalar.
 */
class CopiaNubeAgent : BackupAgent() {

    override fun onFullBackup(data: FullBackupDataOutput) {
        if (CopiaNube.activada(this)) super.onFullBackup(data)
        // Desactivada: no se escribe nada, la copia de la nube queda vacía
    }

    override fun onRestoreFinished() {
        // Si el usuario no había activado la copia en este dispositivo (instalación nueva o copia
        // creada por una versión anterior), los datos restaurados NO se cargan solos: quedan en
        // espera y la aplicación arranca vacía. En Ajustes se ofrece recuperarlos o descartarlos.
        if (!CopiaNube.decidida(this)) {
            val datos = File(filesDir, Storage.FICHERO)
            if (datos.exists()) {
                val espera = CopiaNube.ficheroEnEspera(this)
                espera.delete()
                if (!datos.renameTo(espera)) { espera.writeText(datos.readText()); datos.delete() }
            }
        }
    }

    // La aplicación no usa copia por clave-valor; solo copia completa de ficheros.
    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor?) = Unit

    override fun onRestore(data: BackupDataInput?, appVersionCode: Int, newState: ParcelFileDescriptor?) = Unit
}
