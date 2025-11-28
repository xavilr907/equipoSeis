package com.univalle.inventarioapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
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
                    // No hay sesión -> abrir login y salir
                    openLogin(context)
                    return
                }
                // Solo alternamos si hay sesión
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val current = prefs.getBoolean(KEY_HIDDEN, true)
                prefs.edit().putBoolean(KEY_HIDDEN, !current).apply()
                // refrescar widgets
                onUpdate(context, manager, ids)
            }

            ACTION_GESTIONAR -> {
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    openLogin(context)
                } else {
                    openHome(context)
                }
            }

            ACTION_REFRESH -> {
                // Forzar actualización (ej: tras login)
                onUpdate(context, manager, ids)
            }
        }
    }

    private fun updateSingleWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.inventory_widget)

        // PendingIntent: toggle (ojo)
        val toggleIntent = Intent(context, InventoryWidget::class.java).apply { action = ACTION_TOGGLE }
        val togglePI = PendingIntent.getBroadcast(
            context, 100, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btnToggle, togglePI)

        // PendingIntent: gestionar (texto + icono)
        val gestionarIntent = Intent(context, InventoryWidget::class.java).apply { action = ACTION_GESTIONAR }
        val gestionarPI = PendingIntent.getBroadcast(
            context, 101, gestionarIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.imgGestionar, gestionarPI)
        views.setOnClickPendingIntent(R.id.txtGestionar, gestionarPI)

        // Chequear preferencias para ocultar/mostrar
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val isHiddenPref = prefs.getBoolean(KEY_HIDDEN, true)

        // Chequear sesión
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            // NO logueado: mostrar asteriscos y ojo cerrado
            views.setTextViewText(R.id.txtSaldo, "$ ****")
            views.setImageViewResource(R.id.btnToggle, R.drawable.cerrado)
            appWidgetManager.updateAppWidget(appWidgetId, views)
            return
        }

        // Si hay sesión y pref indica oculto -> ojo cerrado + asteriscos
        if (isHiddenPref) {
            views.setTextViewText(R.id.txtSaldo, "$ ****")
            views.setImageViewResource(R.id.btnToggle, R.drawable.cerrado)
            appWidgetManager.updateAppWidget(appWidgetId, views)
            return
        }

        // Si hay sesión y no está oculto -> mostramos saldo real (ojo abierto) leyendo Firestore
        views.setImageViewResource(R.id.btnToggle, R.drawable.abierto)
        appWidgetManager.updateAppWidget(appWidgetId, views) // primero actualizar UI mínima

        FirebaseFirestore.getInstance()
            .collection("products")
            .get()
            .addOnSuccessListener { snapshot ->
                var total = 0.0
                for (doc in snapshot.documents) {
                    val price = when {
                        doc.contains("price") -> doc.getDouble("price") ?: 0.0
                        doc.contains("priceCents") -> (doc.getLong("priceCents")
                            ?: 0L) / 100.0
                        else -> 0.0
                    }
                    val qty = doc.getLong("quantity") ?: 0L
                    total += price * qty
                }

                // Formateo para: 3.326.000,00
                val nf = NumberFormat.getNumberInstance(Locale("es", "CO")).apply {
                    maximumFractionDigits = 2
                    minimumFractionDigits = 2
                }
                val formatted = "$ " + nf.format(total)

                views.setTextViewText(R.id.txtSaldo, formatted)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
            .addOnFailureListener {
                views.setTextViewText(R.id.txtSaldo, "$ ****")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
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
}
