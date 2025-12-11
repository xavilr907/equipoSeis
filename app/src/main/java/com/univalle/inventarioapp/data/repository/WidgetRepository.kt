package com.univalle.inventarioapp.data.repository

import com.univalle.inventarioapp.data.model.ProductEntity
import com.univalle.inventarioapp.data.remote.FirestoreRepository
import kotlinx.coroutines.flow.first
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository dedicado para operaciones del Widget
 * Calcula el total del inventario y lo formatea correctamente
 */
@Singleton
class WidgetRepository @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) {

    /**
     * Calcula el total del inventario: sum(precio * cantidad)
     * @return String formateado como "$ 3.326,00"
     */
    suspend fun calculateTotalInventory(): String {
        return try {
            // Obtener todos los productos de Firestore
            val products = firestoreRepository.observeProducts().first()

            // Calcular total: suma(precio * cantidad)
            val totalCents = products.sumOf { product ->
                product.priceCents * product.quantity
            }

            // Convertir centavos a pesos
            val totalPesos = totalCents / 100.0

            // Formatear con separador de miles (punto) y decimales (coma)
            formatCurrency(totalPesos)
        } catch (e: Exception) {
            "$ 0,00"
        }
    }

    /**
     * Formatea un número como moneda con formato colombiano
     * Ej: 3326.00 -> "$ 3.326,00"
     */
    fun formatCurrency(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale("es", "CO")).apply {
            groupingSeparator = '.'  // Separador de miles: punto
            decimalSeparator = ','   // Separador decimal: coma
        }

        val formatter = DecimalFormat("$ #,##0.00", symbols)
        return formatter.format(amount)
    }
}

