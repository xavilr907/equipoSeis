package com.univalle.inventarioapp.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.univalle.inventarioapp.data.model.ProductEntity
import com.univalle.inventarioapp.data.remote.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProductUiState(
    val loading: Boolean = false,
    val product: ProductEntity? = null,
    val code: String = "",
    val name: String = "",
    val priceText: String = "",
    val quantityText: String = "",
    val isValid: Boolean = false,
    val error: String? = null
)

sealed class EditProductEvent {
    object NavigateToHome : EditProductEvent()
    data class ShowError(val message: String) : EditProductEvent()
}

@HiltViewModel
class EditProductViewModel @Inject constructor(
    private val repository: FirestoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProductUiState())
    val uiState: StateFlow<EditProductUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditProductEvent>()
    val events: SharedFlow<EditProductEvent> = _events.asSharedFlow()

    fun loadProduct(code: String, name: String, priceCents: Long, quantity: Int, id: String?) {
        val priceInPesos = priceCents / 100.0
        _uiState.update {
            it.copy(
                product = ProductEntity(
                    id = id,
                    code = code,
                    name = name,
                    priceCents = priceCents,
                    quantity = quantity
                ),
                code = code,
                name = name,
                priceText = priceInPesos.toString(),
                quantityText = quantity.toString()
            )
        }
        validateFields()
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name) }
        validateFields()
    }

    fun onPriceChanged(price: String) {
        _uiState.update { it.copy(priceText = price) }
        validateFields()
    }

    fun onQuantityChanged(quantity: String) {
        _uiState.update { it.copy(quantityText = quantity) }
        validateFields()
    }

    private fun validateFields() {
        val state = _uiState.value
        val isValid = state.name.isNotBlank() &&
                      state.priceText.isNotBlank() &&
                      state.quantityText.isNotBlank()
        _uiState.update { it.copy(isValid = isValid) }
    }

    fun updateProduct() {
        val state = _uiState.value
        val product = state.product ?: return

        if (!state.isValid) return

        val priceDouble = state.priceText.replace(",", ".").toDoubleOrNull() ?: 0.0
        val priceCents = (priceDouble * 100).toLong()
        val quantity = state.quantityText.toIntOrNull() ?: 0

        val updatedProduct = product.copy(
            name = state.name,
            priceCents = priceCents,
            quantity = quantity
        )

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                repository.upsertProduct(updatedProduct)
                _events.emit(EditProductEvent.NavigateToHome)
            } catch (e: Exception) {
                _events.emit(EditProductEvent.ShowError("Error al actualizar: ${e.message}"))
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }
}

