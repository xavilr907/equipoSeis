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

/**
 * Representa el estado de la interfaz en la pantalla de detalle de producto.
 *
 * @property loading Indica si se está cargando la información.
 * @property product Producto recuperado desde el repositorio.
 * @property total Precio total calculado (precio * cantidad).
 * @property error Mensaje de error, si ocurre alguno.
 */
data class ProductDetailUiState(
    val loading: Boolean = true,
    val product: ProductEntity? = null,
    val total: Double = 0.0,
    val error: String? = null
)

/**
 * ViewModel encargado de la lógica de negocio de la pantalla de detalle de producto.
 *
 * Funciones principales:
 * - Cargar un producto desde Firestore por su código.
 * - Calcular el precio total.
 * - Eliminar un producto.
 * - Emitir eventos de navegación (editar, volver).
 *
 * Maneja:
 * - [uiState] → Estado observable para la interfaz.
 * - [events] → Flujo de eventos de navegación o acciones puntuales.
 */
@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val repository: FirestoreRepository
) : ViewModel() {

    /** Estado interno mutable del UI */
    private val _uiState = MutableStateFlow(ProductDetailUiState())

    /** Estado del UI expuesto de forma inmutable a la vista */
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    /** Flujo interno para eventos puntuales (navegación, mensajes, etc.) */
    private val _events = MutableSharedFlow<ProductDetailEvent>()

    /** Flujo público e inmutable de eventos */
    val events: SharedFlow<ProductDetailEvent> = _events.asSharedFlow()

    /**
     * Carga un producto desde Firestore usando su código.
     *
     * - Actualiza el estado a loading.
     * - Si el producto existe, calcula el total y actualiza el UI.
     * - Si no existe, registra un error.
     *
     * @param code Código único del producto.
     */
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

    /**
     * Elimina el producto actualmente cargado.
     *
     * - Muestra loading.
     * - Llama al repositorio para eliminarlo.
     * - Emite un evento de navegación hacia atrás.
     */
    fun deleteProduct() {
        val p = _uiState.value.product ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }

            repository.deleteProduct(p.code)

            _uiState.update { it.copy(loading = false) }
            _events.emit(ProductDetailEvent.NavigateBack)
        }
    }

    /**
     * Emite un evento para navegar a la pantalla de edición del producto.
     */
    fun onEdit() {
        viewModelScope.launch { _events.emit(ProductDetailEvent.NavigateToEdit) }
    }

    /**
     * Emite un evento para volver a la pantalla anterior.
     */
    fun onBack() {
        viewModelScope.launch { _events.emit(ProductDetailEvent.NavigateBack) }
    }
}
