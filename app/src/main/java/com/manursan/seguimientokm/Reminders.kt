package com.manursan.seguimientokm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Recordatorio mensual de anotar los km y aviso previo al ajuste anual (AlarmManager, sin dependencias). */
object Reminders {
    private const val CHANNEL = "recordatorios"
    private const val PREFS = "recordatorios"
    const val TIPO_KM = "km"
    const val TIPO_AJUSTE = "ajuste"
    const val TIPO_MULTAS = "multas"
    private val HORA: LocalTime = LocalTime.of(10, 0)

    data class Settings(
        val kmEnabled: Boolean = false,
        /** Día del mes (1..28) en que se recuerda anotar los km. */
        val kmDay: Int = 1,
        val ajusteEnabled: Boolean = false,
        /** Días de antelación del aviso del ajuste anual. */
        val ajusteDias: Int = 30,
        /** Revisión semanal del tablón del BOE con aviso si aparece un anuncio nuevo. */
        val multasEnabled: Boolean = false,
    )

    fun load(context: Context): Settings {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Settings(
            kmEnabled = p.getBoolean("kmEnabled", false),
            kmDay = p.getInt("kmDay", 1),
            ajusteEnabled = p.getBoolean("ajusteEnabled", false),
            ajusteDias = p.getInt("ajusteDias", 30),
            multasEnabled = p.getBoolean("multasEnabled", false),
        )
    }

    fun save(context: Context, s: Settings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("kmEnabled", s.kmEnabled).putInt("kmDay", s.kmDay)
            .putBoolean("ajusteEnabled", s.ajusteEnabled).putInt("ajusteDias", s.ajusteDias)
            .putBoolean("multasEnabled", s.multasEnabled)
            .apply()
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun pending(context: Context, tipo: String): PendingIntent {
        val i = Intent(context, ReminderReceiver::class.java).setAction("com.manursan.seguimientokm.REMINDER_$tipo").putExtra("tipo", tipo)
        return PendingIntent.getBroadcast(context, tipo.hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Próximo día `day` del mes a las 10:00, estrictamente posterior a ahora. */
    fun nextMonthly(day: Int, now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        var d = now.toLocalDate().withDayOfMonth(day.coerceIn(1, 28))
        var dt = LocalDateTime.of(d, HORA)
        if (!dt.isAfter(now)) { d = d.plusMonths(1); dt = LocalDateTime.of(d, HORA) }
        return dt
    }

    /** Aviso `diasAntes` días antes del próximo aniversario del contrato, si queda alguno. */
    fun nextAjuste(params: ContractParams, diasAntes: Int, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        val hoy = now.toLocalDate()
        val aniversarios = generateSequence(1L) { it + 1 }.map { params.inicio.plusYears(it) }.takeWhile { it.plusMonths(6).isBefore(params.fin) }
        val fecha = aniversarios.map { it.minusDays(diasAntes.toLong()) }.firstOrNull { !it.isBefore(hoy) } ?: return null
        val dt = LocalDateTime.of(fecha, HORA)
        return if (dt.isAfter(now)) dt else LocalDateTime.of(fecha.plusDays(1), HORA).takeIf { fecha.plusDays(1).isBefore(params.fin) }
    }

    /** (Re)programa las alarmas según la configuración guardada. */
    fun schedule(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val s = load(context)
        am.cancel(pending(context, TIPO_KM))
        am.cancel(pending(context, TIPO_AJUSTE))
        am.cancel(pending(context, TIPO_MULTAS))
        if (s.multasEnabled) set(am, LocalDateTime.now().plusDays(7).with(HORA), pending(context, TIPO_MULTAS))
        if (s.kmEnabled) set(am, nextMonthly(s.kmDay), pending(context, TIPO_KM))
        if (s.ajusteEnabled) {
            nextAjuste(Storage.load(context).params, s.ajusteDias)?.let { set(am, it, pending(context, TIPO_AJUSTE)) }
        }
    }

    private fun set(am: AlarmManager, at: LocalDateTime, pi: PendingIntent) {
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Alarma inexacta (no requiere permiso de alarmas exactas); basta con que llegue ese día
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    /** Consulta el BOE y avisa solo si hay anuncios que no estaban en la última consulta guardada. */
    fun revisarMultas(context: Context) {
        val matricula = Storage.load(context).params.matricula
        val previos = FinesCheck.load(context)?.takeIf { it.matricula == matricula }?.anuncios?.map { it.id }?.toSet() ?: emptySet()
        val consulta = runCatching { FinesCheck.consultarBloqueante(context, matricula) }.getOrNull() ?: return
        val nuevos = consulta.anuncios.filter { it.id !in previos }
        if (nuevos.isNotEmpty() && hasPermission(context)) {
            val a = nuevos.first()
            notifyText(context, TIPO_MULTAS, "Anuncio en el BOE para $matricula", "${a.organismo} · ${a.fecha}. ${a.texto}")
        }
    }

    private fun notifyText(context: Context, tipo: String, titulo: String, texto: String) {
        val nm = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(titulo).setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto)).setContentIntent(open).setAutoCancel(true).build()
        runCatching { nm.notify(tipo.hashCode(), n) }
    }

    fun notify(context: Context, tipo: String) {
        if (!hasPermission(context)) return
        val nm = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val r = Calc.compute(Storage.load(context))
        val (titulo, texto) = when (tipo) {
            TIPO_AJUSTE -> {
                val a = r.proximoAjuste
                "Ajuste anual de kilómetros" to if (a != null)
                    "El ${Fmt.date(a.fecha)} toca el ajuste anual. Previsión: ${Fmt.km(a.kmPrevistos)} frente a ${Fmt.km(a.kmTeoricos)} teóricos (${Fmt.pct(a.desviacionPct)})."
                else "Se acerca el ajuste anual de kilómetros del contrato."
            }
            else -> "Anota los kilómetros" to
                "Última medida: ${Fmt.km(r.seguimiento.kmUltima)} el ${Fmt.date(r.seguimiento.fechaUltima)}. Máximo sin cargo: ${Fmt.dec(r.margen.kmDiaMaxSinCargo, 1)} km/día."
        }
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(tipo.hashCode(), n) }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val tipo = intent.getStringExtra("tipo") ?: Reminders.TIPO_KM
        if (tipo == Reminders.TIPO_MULTAS) {
            // Consulta de red: fuera del hilo principal, manteniendo vivo el receptor
            val result = goAsync()
            Thread {
                try { Reminders.revisarMultas(context) } finally {
                    Reminders.schedule(context)
                    result.finish()
                }
            }.start()
            return
        }
        Reminders.notify(context, tipo)
        Reminders.schedule(context) // siguiente ocurrencia
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) Reminders.schedule(context)
    }
}
