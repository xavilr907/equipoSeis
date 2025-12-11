package com.univalle.inventarioapp.ui.edit

import com.univalle.inventarioapp.data.remote.FirestoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat

@OptIn(ExperimentalCoroutinesApi::class)
class EditProductViewModelTest {

    @Mock
    private lateinit var repository: FirestoreRepository

    private lateinit var viewModel: EditProductViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = EditProductViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============================================
    // TEST 1: Pre-llenado de Campos
    // ============================================
    @Test
    fun `loadProduct pre-llena campos correctamente`() = runTest {
        // When
        viewModel.loadProduct("1234", "Laptop", 150000, 10, "id123")

        // Then
        val state = viewModel.uiState.value
        assertEquals("1234", state.code)
        assertEquals("Laptop", state.name)
        assertEquals("1500.0", state.priceText) // 150000 centavos = 1500 pesos
        assertEquals("10", state.quantityText)
        assertTrue(state.isValid) // Todos los campos están llenos
    }

    // ============================================
    // TEST 2: Validación - Nombre Vacío
    // ============================================
    @Test
    fun `validación deshabilita botón si nombre vacío`() = runTest {
        // Given: Producto cargado
        viewModel.loadProduct("1234", "Laptop", 150000, 10, "id123")

        // When: Borrar nombre
        viewModel.onNameChanged("")

        // Then: Botón debe estar deshabilitado
        assertFalse(viewModel.uiState.value.isValid)
    }

    // ============================================
    // TEST 3: Validación - Precio Vacío
    // ============================================
    @Test
    fun `validación deshabilita botón si precio vacío`() = runTest {
        // Given
        viewModel.loadProduct("1234", "Laptop", 150000, 10, "id123")

        // When
        viewModel.onPriceChanged("")

        // Then
        assertFalse(viewModel.uiState.value.isValid)
    }

    // ============================================
    // TEST 4: Validación - Todos los Campos Llenos
    // ============================================
    @Test
    fun `validación habilita botón si todos los campos llenos`() = runTest {
        // Given
        viewModel.loadProduct("1234", "Laptop", 150000, 10, "id123")

        // Then: Estado inicial debe ser válido
        assertTrue(viewModel.uiState.value.isValid)

        // When: Cambiar valores
        viewModel.onNameChanged("Laptop Pro")
        viewModel.onPriceChanged("2000")
        viewModel.onQuantityChanged("5")

        // Then: Debe seguir siendo válido
        assertTrue(viewModel.uiState.value.isValid)
    }

    // ============================================
    // TEST 5: Update Product - Llamada a Repository
    // ============================================
    @Test
    fun `updateProduct llama a repository y emite evento de navegación`() = runTest {
        // Given
        viewModel.loadProduct("1234", "Laptop", 150000, 10, "id123")
        `when`(repository.upsertProduct(any())).thenReturn(Unit)

        // When
        viewModel.updateProduct()

        // Then: Verificar llamada al repository
        verify(repository).upsertProduct(any())

        // Then: Verificar evento de navegación
        val event = viewModel.events.first()
        assertTrue(event is EditProductEvent.NavigateToHome)
    }

    // ============================================
    // TEST 6: Conversión de Pesos a Centavos
    // ============================================
    @Test
    fun `updateProduct convierte correctamente pesos a centavos`() = runTest {
        // Given
        viewModel.loadProduct("1234", "Laptop", 100000, 10, "id123")
        viewModel.onPriceChanged("2500.50") // 2500.50 pesos

        `when`(repository.upsertProduct(any())).thenReturn(Unit)

        // When
        viewModel.updateProduct()

        // Then: Verificar que se llamó con el precio en centavos correcto
        verify(repository).upsertProduct(argThat { product ->
            product.priceCents == 250050L // 2500.50 * 100 = 250050
        })
    }

    // ============================================
    // TEST 7: Manejo de Errores
    // ============================================
    @Test
    fun `updateProduct maneja errores correctamente`() = runTest {
        // Given
        viewModel.loadProduct("1234", "Laptop", 150000, 10, "id123")
        `when`(repository.upsertProduct(any())).thenThrow(RuntimeException("Network error"))

        // When
        viewModel.updateProduct()

        // Then: Debe emitir evento de error
        val event = viewModel.events.first()
        assertTrue(event is EditProductEvent.ShowError)
        assertTrue((event as EditProductEvent.ShowError).message.contains("Network error"))
    }

    // ============================================
    // TEST 8: Estado de Loading
    // ============================================
    @Test
    fun `updateProduct muestra estado de loading correctamente`() = runTest {
        // Given
        viewModel.loadProduct("1234", "Laptop", 150000, 10, "id123")
        `when`(repository.upsertProduct(any())).thenReturn(Unit)

        // Initial state: loading debe ser false
        assertEquals(false, viewModel.uiState.value.loading)

        // When
        viewModel.updateProduct()

        // Then: Loading debe volver a false después de guardar
        assertEquals(false, viewModel.uiState.value.loading)
    }

    // ============================================
    // TEST 9: Conversión con Comas
    // ============================================
    @Test
    fun `updateProduct convierte precio con coma correctamente`() = runTest {
        // Given
        viewModel.loadProduct("1234", "Laptop", 100000, 10, "id123")
        viewModel.onPriceChanged("1500,75") // Coma en lugar de punto

        `when`(repository.upsertProduct(any())).thenReturn(Unit)

        // When
        viewModel.updateProduct()

        // Then: Debe convertir correctamente
        verify(repository).upsertProduct(argThat { product ->
            product.priceCents == 150075L // 1500.75 * 100 = 150075
        })
    }

    // ============================================
    // TEST 10: Actualización de Nombre
    // ============================================
    @Test
    fun `onNameChanged actualiza el estado correctamente`() = runTest {
        // Given
        viewModel.loadProduct("1234", "Laptop", 150000, 10, "id123")

        // When
        viewModel.onNameChanged("Laptop Pro Max")

        // Then
        assertEquals("Laptop Pro Max", viewModel.uiState.value.name)
    }
}

