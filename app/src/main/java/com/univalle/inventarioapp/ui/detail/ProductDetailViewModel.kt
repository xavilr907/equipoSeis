package com.univalle.inventarioapp.ui.detail

import androidx.lifecycle.*
import androidx.lifecycle.map
import com.univalle.inventarioapp.data.local.ProductDao
import com.univalle.inventarioapp.data.model.ProductEntity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ProductDetailViewModel(
    private val dao: ProductDao,
    private val productCode: String
) : ViewModel() {

    private val _product = MutableLiveData<ProductEntity?>()
    val product: LiveData<ProductEntity?> = _product

    // Use the LiveData.map extension with explicit lambda parameter type to avoid inference issues
    val totalFormatted: LiveData<String> = _product.map { prod: ProductEntity? ->
        if (prod == null) {
            formatCurrency(0L)
        } else {
            val qty: Long = prod.quantity.toLong()
            val totalCents: Long = prod.priceCents * qty
            formatCurrency(totalCents)
        }
    }

    private val _navigateBack = MutableLiveData<Boolean>()
    val navigateBack: LiveData<Boolean> = _navigateBack

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadProduct()
    }

    fun loadProduct() {
        viewModelScope.launch {
            try {
                val p = dao.getByCode(productCode)
                _product.postValue(p)
                if (p == null) {
                    _error.postValue("Producto no encontrado")
                }
            } catch (e: Exception) {
                _error.postValue("Error cargando producto: ${e.message}")
            }
        }
    }

    private suspend fun deleteFirestoreDocument(code: String): Boolean = suspendCancellableCoroutine { cont ->
        try {
            val ref = FirebaseFirestore.getInstance().collection("products").document(code)
            val task = ref.delete()
            task.addOnSuccessListener {
                if (!cont.isCompleted) cont.resume(true)
            }.addOnFailureListener { ex ->
                if (!cont.isCompleted) cont.resume(false)
            }
            cont.invokeOnCancellation {
                // no-op
            }
        } catch (e: Exception) {
            if (!cont.isCompleted) cont.resume(false)
        }
    }

    fun deleteProduct() {
        viewModelScope.launch {
            try {
                // Primero eliminar localmente
                dao.deleteByCode(productCode)

                // Intentar eliminar en Firestore para evitar que el sync vuelva a insertarlo
                val remoteOk = try {
                    deleteFirestoreDocument(productCode)
                } catch (e: Exception) {
                    false
                }

                if (!remoteOk) {
                    _error.postValue("Eliminado localmente, pero no se pudo eliminar en Firestore")
                }

                // Navegar de regreso a Home (aunque la eliminación remota pudo fallar)
                _navigateBack.postValue(true)

            } catch (e: Exception) {
                _error.postValue("No se pudo eliminar el producto: ${e.message}")
            }
        }
    }

    private fun formatCurrency(cents: Long): String {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        // asumimos que priceCents está en centavos; convertir a unidad
        val units = cents / 100.0
        return format.format(units)
    }
}

class ProductDetailViewModelFactory(
    private val dao: ProductDao,
    private val productCode: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductDetailViewModel::class.java)) {
            return ProductDetailViewModel(dao, productCode) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
