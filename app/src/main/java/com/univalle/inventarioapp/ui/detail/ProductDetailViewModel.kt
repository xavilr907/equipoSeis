package com.univalle.inventarioapp.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.univalle.inventarioapp.data.model.ProductEntity
import com.univalle.inventarioapp.data.remote.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductDetailUiState(
    val loading: Boolean = true,
    val product: ProductEntity? = null,
    val total: Double = 0.0,
    val error: String? = null
)



@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val repository: FirestoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProductDetailEvent>()
    val events: SharedFlow<ProductDetailEvent> = _events.asSharedFlow()

    fun loadProduct(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }

            val product = repository.getProductByCode(code)

            if (product == null) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = "No se encontró el producto"
                    )
                }
                return@launch
            }

            val total = (product.priceCents / 100.0) * product.quantity

            _uiState.update {
                it.copy(
                    loading = false,
                    product = product,
                    total = total
                )
            }
        }
    }

    fun deleteProduct() {
        val p = _uiState.value.product ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }

            repository.deleteProduct(p.code)

            _uiState.update { it.copy(loading = false) }
            _events.emit(ProductDetailEvent.NavigateBack)
        }
    }

    fun onEdit() {
        viewModelScope.launch { _events.emit(ProductDetailEvent.NavigateToEdit) }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(ProductDetailEvent.NavigateBack) }
    }
}
