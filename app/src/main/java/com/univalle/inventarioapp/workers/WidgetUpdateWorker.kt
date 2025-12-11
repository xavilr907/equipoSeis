package com.univalle.inventarioapp.workers

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.univalle.inventarioapp.InventoryWidget
import com.univalle.inventarioapp.data.repository.AuthRepository
import com.univalle.inventarioapp.data.repository.WidgetRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker con Hilt para actualizar el widget de forma asíncrona
 * Calcula el total del inventario y actualiza el widget
 */
@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val widgetRepository: WidgetRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "WidgetUpdateWorker"
        private const val PREFS = "inventory_widget_prefs"
        private const val KEY_TOTAL = "totalInventory"
    }

    override suspend fun doWork(): Result {
        return try {
            // Verificar si hay usuario autenticado
            val currentUser = authRepository.getCurrentUser()

            if (currentUser != null) {
                // Calcular total del inventario
                val totalFormatted = widgetRepository.calculateTotalInventory()

                // Guardar en SharedPreferences para que el widget lo lea
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_TOTAL, totalFormatted).apply()
            }

            // Disparar actualización del widget
            val intent = Intent(context, InventoryWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, InventoryWidget::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)

            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

