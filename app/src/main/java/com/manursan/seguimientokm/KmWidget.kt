package com.manursan.seguimientokm

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Widget de pantalla de inicio: km actuales, desviación y margen diario. */
class KmWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val r = Calc.compute(Storage.load(context))
        ids.forEach { manager.updateAppWidget(it, build(context, r)) }
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, KmWidget::class.java))
            if (ids.isEmpty()) return
            val r = Calc.compute(Storage.load(context))
            ids.forEach { manager.updateAppWidget(it, build(context, r)) }
        }

        private fun build(context: Context, r: Resultado): RemoteViews {
            val ultima = r.reales.lastOrNull()
            val v = RemoteViews(context.packageName, R.layout.widget_km)
            v.setTextViewText(R.id.w_km, Fmt.km(r.seguimiento.kmUltima))
            v.setTextViewText(R.id.w_fecha, "a ${Fmt.date(r.seguimiento.fechaUltima)}")
            v.setTextViewText(
                R.id.w_desv,
                if (ultima != null) "${Fmt.signed(ultima.desviacion, 0, " km")} (${Fmt.pct(ultima.desviacionPct)}) vs. contrato" else "Sin mediciones",
            )
            v.setTextColor(R.id.w_desv, if (ultima != null && ultima.desviacion > 0) 0xFFFFB4B4.toInt() else 0xFFB4F0C8.toInt())
            v.setTextViewText(R.id.w_margen, "Máx. sin cargo: ${Fmt.dec(r.margen.kmDiaMaxSinCargo, 1)} km/día · ${Fmt.int(r.margen.kmMesMaxSinCargo)} km/mes")
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            v.setOnClickPendingIntent(R.id.w_root, open)
            return v
        }
    }
}
