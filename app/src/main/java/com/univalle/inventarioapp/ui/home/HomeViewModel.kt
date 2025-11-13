package com.univalle.inventarioapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.map
import com.google.firebase.firestore.FirebaseFirestore
import com.univalle.inventarioapp.data.local.ProductDao
import com.univalle.inventarioapp.data.model.ProductEntity
import kotlinx.coroutines.flow.map as flowMap
import kotlinx.coroutines.launch

class HomeViewModel(
    private val dao: ProductDao
) : ViewModel() {

    // Convierte el Flow de Room en LiveData para poder usar .observe(...)
    val products: LiveData<List<ProductEntity>> = dao.observeAll().asLiveData()

    // Nuevo: calculamos la suma total de todos los productos (en centavos) y la exponemos formateada
    val totalCents: LiveData<Long> = dao.observeAll().flowMap { list ->
        list.fold(0L) { acc, p -> acc + (p.priceCents * p.quantity.toLong()) }
    }.asLiveData()

    val totalFormatted: LiveData<String> = totalCents.map { cents ->
        // formatear como moneda (dividir por 100.0)
        val units = cents / 100.0
        java.text.NumberFormat.getCurrencyInstance(java.util.Locale.getDefault()).format(units)
    }

    // --- Opcional: sincronía básica Firestore -> Room ---
    // Si no quieres esto aún, puedes borrar el init {}
    private val fs = FirebaseFirestore.getInstance()
    private val col = fs.collection("products")

    init {
        // Escucha cambios remotos y los replica en Room
        col.addSnapshotListener { snap, _ ->
            val list = snap?.documents?.mapNotNull { it.toObject(ProductEntity::class.java) } ?: return@addSnapshotListener
            viewModelScope.launch {
                list.forEach { dao.upsert(it) }
            }
        }
    }
}
