package com.univalle.inventarioapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.univalle.inventarioapp.workers.WidgetUpdateWorker

/**
 * Widget de Inventario que muestra el total calculado
 * Usa WorkManager para operaciones asíncronas con Hilt
 */
class InventoryWidget : AppWidgetProvider() {

    companion object {
        private const val ACTION_TOGGLE = "com.univalle.inventarioapp.ACTION_TOGGLE_SALDO"
        private const val ACTION_GESTIONAR = "com.univalle.inventarioapp.ACTION_GESTIONAR"
        private const val ACTION_REFRESH = "com.univalle.inventarioapp.ACTION_REFRESH_WIDGET"
        private const val PREFS = "inventory_widget_prefs"
        private const val KEY_HIDDEN = "is_hidden"
        private const val KEY_TOTAL = "totalInventory"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Disparar Worker para calcular total asíncronamente
        scheduleWidgetUpdate(context)

        // Actualizar UI de todos los widgets
        for (id in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val action = intent.action
        val manager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, InventoryWidget::class.java)
        val ids = manager.getAppWidgetIds(thisWidget)

        when (action) {
            ACTION_TOGGLE -> {
                // CRITERIO 7 y 10: Click en ojo
                // Verificar autenticación REAL usando FirebaseAuth
                val currentUser = FirebaseAuth.getInstance().currentUser

                // Si NO hay sesión activa → Ir al Login (Criterio 10)
                if (currentUser == null) {
                    openLogin(context, fromGestionar = false)  // Vuelve al widget
                    return
                }

                // Si hay sesión → Hacer toggle normal (Criterio 7)
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val current = prefs.getBoolean(KEY_HIDDEN, true)
                prefs.edit().putBoolean(KEY_HIDDEN, !current).apply()

                // Actualizar widgets
                onUpdate(context, manager, ids)
            }

            ACTION_GESTIONAR -> {
                // Verificar autenticación REAL usando FirebaseAuth
                val currentUser = FirebaseAuth.getInstance().currentUser

                if (currentUser != null) {
                    // Hay sesión → Ir al Home
                    openHome(context)
                } else {
                    // NO hay sesión → Ir al Login
                    openLogin(context, fromGestionar = true)
                }
            }

            ACTION_REFRESH -> {
                scheduleWidgetUpdate(context)
                onUpdate(context, manager, ids)
            }
        }
    }

    private fun scheduleWidgetUpdate(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .addTag(WidgetUpdateWorker.TAG)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
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

        // CRITERIO 7 y 10: Verificar autenticación REAL
        val currentUser = FirebaseAuth.getInstance().currentUser
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val isHidden = prefs.getBoolean(KEY_HIDDEN, true)
        val total = prefs.getString(KEY_TOTAL, "$ ****") ?: "$ ****"

        // LÓGICA CORREGIDA:
        // 1. Si NO hay usuario autenticado → SIEMPRE mostrar asteriscos y ojo cerrado
        // 2. Si hay usuario Y está oculto (isHidden=true) → Mostrar asteriscos y ojo cerrado
        // 3. Si hay usuario Y está visible (isHidden=false) → Mostrar saldo y ojo abierto

        if (currentUser == null) {
            // SIN AUTENTICACIÓN: Siempre oculto
            views.setTextViewText(R.id.txtSaldo, "$ ****")
            views.setImageViewResource(R.id.btnToggle, R.drawable.cerrado)

        } else if (isHidden) {
            // CON AUTENTICACIÓN pero OCULTO: Mostrar asteriscos
            views.setTextViewText(R.id.txtSaldo, "$ ****")
            views.setImageViewResource(R.id.btnToggle, R.drawable.cerrado)

        } else {
            // CON AUTENTICACIÓN y VISIBLE: Mostrar saldo real
            views.setTextViewText(R.id.txtSaldo, total)
            views.setImageViewResource(R.id.btnToggle, R.drawable.abierto)
        }

        views.setTextColor(R.id.txtSaldo, 0xFFFFFFFF.toInt()) // Blanco

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun openLogin(context: Context, fromGestionar: Boolean = false) {
        val i = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("fromWidget", true)
            putExtra("fromWidgetGestionar", fromGestionar)
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
