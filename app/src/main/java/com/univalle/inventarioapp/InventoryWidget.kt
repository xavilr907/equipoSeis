package com.univalle.inventarioapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.google.firebase.auth.FirebaseAuth
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class InventoryWidget : AppWidgetProvider() {

    companion object {
        private const val ACTION_TOGGLE = "com.univalle.inventarioapp.ACTION_TOGGLE_SALDO"
        private const val ACTION_GESTIONAR = "com.univalle.inventarioapp.ACTION_GESTIONAR"
        private const val ACTION_REFRESH = "com.univalle.inventarioapp.ACTION_REFRESH_WIDGET"
        private const val PREFS = "inventory_widget_prefs"
        private const val KEY_HIDDEN = "is_hidden"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateSingleWidget(context, appWidgetManager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val action = intent.action
        val manager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, InventoryWidget::class.java)
        val ids = manager.getAppWidgetIds(thisWidget)

        when (action) {
            ACTION_TOGGLE -> {
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    openLogin(context)
                    return
                }

                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val current = prefs.getBoolean(KEY_HIDDEN, true)
                prefs.edit().putBoolean(KEY_HIDDEN, !current).apply()
                onUpdate(context, manager, ids)
            }

            ACTION_GESTIONAR -> {
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) openLogin(context) else openHome(context)
            }

            ACTION_REFRESH -> onUpdate(context, manager, ids)
        }
    }

    private fun updateSingleWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.inventory_widget)

        // Intent toggle
        val togglePI = PendingIntent.getBroadcast(
            context, 100,
            Intent(context, InventoryWidget::class.java).apply { action = ACTION_TOGGLE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btnToggle, togglePI)

        // Intent gestionar
        val gestionarPI = PendingIntent.getBroadcast(
            context, 101,
            Intent(context, InventoryWidget::class.java).apply { action = ACTION_GESTIONAR },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.imgGestionar, gestionarPI)
        views.setOnClickPendingIntent(R.id.txtGestionar, gestionarPI)

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val isHiddenPref = prefs.getBoolean(KEY_HIDDEN, true)

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null || isHiddenPref) {
            views.setTextViewText(R.id.txtSaldo, "$ ****")
            views.setImageViewResource(R.id.btnToggle, R.drawable.cerrado)
            views.setTextColor(R.id.txtSaldo, 0xFFFFFFFF.toInt())
            appWidgetManager.updateAppWidget(appWidgetId, views)
            return
        }

        // Mostrar el valor real
        views.setImageViewResource(R.id.btnToggle, R.drawable.abierto)

        // --- FORMATEO DEL SALDO ---
        val rawTotal = prefs.getString("totalInventory", "0") ?: "0"
        val formatted = formatSaldo(rawTotal)

        views.setTextViewText(R.id.txtSaldo, "$ $formatted")
        views.setTextColor(R.id.txtSaldo, 0xFFFFFFFF.toInt())

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun openLogin(context: Context) {
        val i = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("fromWidget", true)
        }
        context.startActivity(i)
    }

    private fun openHome(context: Context) {
        val i = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("fromWidget", true)
        }
        context.startActivity(i)
    }

    // ------- FORMATEO PERSONALIZADO -------
    private fun formatSaldo(value: String): String {
        return try {
            val clean = value
                .replace("$", "")
                .replace(".", "")
                .replace(",", ".")
                .trim()

            val number = clean.toDouble()

            val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
                groupingSeparator = '.'
                decimalSeparator = ','
            }

            val formatter = DecimalFormat("#,###,###.00", symbols)
            formatter.format(number)

        } catch (e: Exception) {
            value
        }
    }
}
