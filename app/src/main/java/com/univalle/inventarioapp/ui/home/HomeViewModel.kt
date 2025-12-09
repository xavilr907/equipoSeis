package com.univalle.inventarioapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.map
import com.univalle.inventarioapp.data.model.ProductEntity
import com.univalle.inventarioapp.data.remote.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

/**
 * Estados de UI para la pantalla de inventario
 */
sealed class UiState {
    object Loading : UiState()
    data class Success(val products: List<ProductEntity>) : UiState()
    data class Error(val message: String) : UiState()
}

/**
 * ViewModel para la pantalla de Inventario (HU3)
 * Usa Dagger Hilt para inyección de dependencias
 * Arquitectura MVVM con Repository Pattern
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: FirestoreRepository
) : ViewModel() {

    // StateFlow privado mutable para estados de UI
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)

    // StateFlow público de solo lectura
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // LiveData para compatibilidad con Fragment (derivado de StateFlow)
    val products: LiveData<List<ProductEntity>> = uiState.asLiveData().map { state ->
        when (state) {
            is UiState.Success -> state.products
            else -> emptyList()
        }
    }

    // Calcula el total del inventario en centavos
    val totalCents: LiveData<Long> = products.map { list ->
        list.fold(0L) { acc, p -> acc + (p.priceCents * p.quantity.toLong()) }
    }

    // Formatea el total como moneda
    val totalFormatted: LiveData<String> = totalCents.map { cents ->
        val units = cents / 100.0
        NumberFormat.getCurrencyInstance(Locale.getDefault()).format(units)
    }

    init {
        observeProducts()
    }

    /**
     * Observa los productos desde Firestore en tiempo real
     * Maneja estados de Loading, Success y Error
     */
    private fun observeProducts() {
        viewModelScope.launch {
            repository.observeProducts()
                .onStart {
                    _uiState.value = UiState.Loading
                }
                .catch { exception ->
                    _uiState.value = UiState.Error(
                        exception.message ?: "Error al cargar productos"
                    )
                }
                .collect { productList ->
                    _uiState.value = UiState.Success(productList)
                }
        }
    }

    /**
     * Recarga los productos manualmente (para pull-to-refresh o retry)
     */
    fun reloadProducts() {
        observeProducts()
    }
}
