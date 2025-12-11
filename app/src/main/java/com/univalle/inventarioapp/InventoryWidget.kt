package com.univalle.inventarioapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.google.firebase.auth.FirebaseAuth

/**
 * Widget de inventario para mostrar (u ocultar) el total del inventario.
 * Incluye:
 * - Botón para alternar visibilidad del saldo.
 * - Botón para ir a gestión del inventario.
 * - Recarga manual del widget.
 *
 * Este widget usa SharedPreferences para recordar si el saldo está oculto
 * y usa FirebaseAuth para saber si hay un usuario autenticado.
 */
class InventoryWidget : AppWidgetProvider() {

    companion object {

        /** Acción para alternar entre mostrar y ocultar el saldo. */
        private const val ACTION_TOGGLE = "com.univalle.inventarioapp.ACTION_TOGGLE_SALDO"

        /** Acción para abrir la app en modo "gestionar inventario". */
        private const val ACTION_GESTIONAR = "com.univalle.inventarioapp.ACTION_GESTIONAR"

        /** Acción para refrescar los datos del widget. */
        private const val ACTION_REFRESH = "com.univalle.inventarioapp.ACTION_REFRESH_WIDGET"

        /** Nombre del archivo de preferencias del widget. */
        private const val PREFS = "inventory_widget_prefs"

        /** Clave que indica si el saldo está oculto. */
        private const val KEY_HIDDEN = "is_hidden"
    }

    /**
     * Llamado por el sistema cuando se deben actualizar los widgets.
     * Recorre todos los IDs y actualiza cada uno individualmente.
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateSingleWidget(context, appWidgetManager, id)
    }

    /**
     * Maneja eventos enviados por PendingIntent (clics en el widget).
     * Incluye las acciones:
     * - Alternar visibilidad del saldo
     * - Abrir login o home
     * - Recargar el widget
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val action = intent.action
        val manager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, InventoryWidget::class.java)
        val ids = manager.getAppWidgetIds(thisWidget)

        when (action) {

            // Alternar visibilidad del saldo
            ACTION_TOGGLE -> {
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    openLogin(context)
                    return
                }

                // Leer y guardar el nuevo estado (visible u oculto)
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val current = prefs.getBoolean(KEY_HIDDEN, true)
                prefs.edit().putBoolean(KEY_HIDDEN, !current).apply()

                onUpdate(context, manager, ids)
            }

            // Abrir gestión de inventario (si hay usuario)
            ACTION_GESTIONAR -> {
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) openLogin(context) else openHome(context)
            }

            // Refrescar widget
            ACTION_REFRESH -> {
                onUpdate(context, manager, ids)
            }
        }
    }

    /**
     * Actualiza un widget individual.
     * Maneja:
     * - Construcción de RemoteViews
     * - Asignación de PendingIntents
     * - Lectura del estado de visibilidad
     * - Lectura del total guardado
     */
    private fun updateSingleWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.inventory_widget)

        // Intent para alternar visibilidad
        val toggleIntent = Intent(context, InventoryWidget::class.java).apply { action = ACTION_TOGGLE }
        val togglePI = PendingIntent.getBroadcast(
            context, 100, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btnToggle, togglePI)

        // Intent para gestionar inventario
        val gestionarIntent = Intent(context, InventoryWidget::class.java).apply { action = ACTION_GESTIONAR }
        val gestionarPI = PendingIntent.getBroadcast(
            context, 101, gestionarIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.imgGestionar, gestionarPI)
        views.setOnClickPendingIntent(R.id.txtGestionar, gestionarPI)

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val isHiddenPref = prefs.getBoolean(KEY_HIDDEN, true)

        val user = FirebaseAuth.getInstance().currentUser

        // Si no hay sesión o el saldo está oculto
        if (user == null || isHiddenPref) {
            views.setTextViewText(R.id.txtSaldo, "$ ****")
            views.setImageViewResource(R.id.btnToggle, R.drawable.cerrado)
            views.setTextColor(R.id.txtSaldo, 0xFFFFFFFF.toInt()) // blanco
            appWidgetManager.updateAppWidget(appWidgetId, views)
            return
        }

        // Usuario logueado y saldo visible
        views.setImageViewResource(R.id.btnToggle, R.drawable.abierto)

        // Leer el total desde SharedPreferences (guardado en HomeFragment)
        val total = prefs.getString("totalInventory", "$ ****") ?: "$ ****"
        views.setTextViewText(R.id.txtSaldo, total)
        views.setTextColor(R.id.txtSaldo, 0xFFFFFFFF.toInt())

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    /**
     * Abre la pantalla de login desde el widget.
     * Se usa cuando el usuario intenta ver datos sin estar autenticado.
     */
    private fun openLogin(context: Context) {
        val i = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("fromWidget", true)
        }
        context.startActivity(i)
    }

    /**
     * Abre la pantalla principal de la app desde el widget.
     * Solo se ejecuta si ya hay un usuario autenticado.
     */
    private fun openHome(context: Context) {
        val i = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("fromWidget", true)
        }
        context.startActivity(i)
    }

    /**
     * Extensión para RemoteViews que permite cambiar color de texto.
     * (Aquí solo reenvía a la función existente).
     */
    private fun RemoteViews.setTextColor(viewId: Int, color: Int) {
        this.setTextColor(viewId, color)
    }
}
