package com.univalle.inventarioapp.ui.detail

import com.univalle.inventarioapp.data.model.ProductEntity
import com.univalle.inventarioapp.data.remote.FirestoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModelTest {

    @Mock
    private lateinit var repository: FirestoreRepository

    private lateinit var viewModel: ProductDetailViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = ProductDetailViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============================================
    // TEST 1: Cálculo de Total Correcto
    // ============================================
    @Test
    fun `loadProduct calcula total correctamente`() = runTest {
        // Given: Producto con precio 5000 centavos ($50) y cantidad 10
        val product = ProductEntity(
            code = "1234",
            name = "Test Product",
            priceCents = 5000,
            quantity = 10
        )
        `when`(repository.getProductByCode("1234")).thenReturn(product)

        // When: Cargar producto
        viewModel.loadProduct("1234")

        // Then: Total debe ser 50.0 * 10 = 500.0
        val state = viewModel.uiState.value
        assertEquals(500.0, state.total, 0.01)
        assertEquals(product, state.product)
        assertEquals(false, state.loading)
    }

    // ============================================
    // TEST 2: Producto No Encontrado
    // ============================================
    @Test
    fun `loadProduct maneja producto no encontrado`() = runTest {
        // Given: Repository retorna null
        `when`(repository.getProductByCode("9999")).thenReturn(null)

        // When: Cargar producto inexistente
        viewModel.loadProduct("9999")

        // Then: Estado debe tener error
        val state = viewModel.uiState.value
        assertNull(state.product)
        assertEquals("No se encontró el producto", state.error)
        assertEquals(false, state.loading)
    }

    // ============================================
    // TEST 3: Eliminación de Producto
    // ============================================
    @Test
    fun `deleteProduct llama a repository y emite evento de navegación`() = runTest {
        // Given: Producto cargado
        val product = ProductEntity(code = "1234", name = "Test", priceCents = 1000, quantity = 5)
        `when`(repository.getProductByCode("1234")).thenReturn(product)
        `when`(repository.deleteProduct("1234")).thenReturn(Unit)

        viewModel.loadProduct("1234")

        // When: Eliminar producto
        viewModel.deleteProduct()

        // Then: Repository debe ser llamado
        verify(repository).deleteProduct("1234")

        // Then: Evento de navegación debe ser emitido
        val event = viewModel.events.first()
        assertTrue(event is ProductDetailEvent.NavigateBack)
    }

    // ============================================
    // TEST 4: Navegación a Editar
    // ============================================
    @Test
    fun `onEdit emite evento NavigateToEdit`() = runTest {
        // When: Usuario presiona FAB editar
        viewModel.onEdit()

        // Then: Evento correcto debe ser emitido
        val event = viewModel.events.first()
        assertTrue(event is ProductDetailEvent.NavigateToEdit)
    }

    // ============================================
    // TEST 5: Navegación Atrás
    // ============================================
    @Test
    fun `onBack emite evento NavigateBack`() = runTest {
        // When: Usuario presiona flecha atrás
        viewModel.onBack()

        // Then: Evento correcto debe ser emitido
        val event = viewModel.events.first()
        assertTrue(event is ProductDetailEvent.NavigateBack)
    }

    // ============================================
    // TEST 6: Estado de Carga
    // ============================================
    @Test
    fun `loadProduct muestra y oculta loading correctamente`() = runTest {
        // Given
        val product = ProductEntity(code = "1234", name = "Test", priceCents = 1000, quantity = 5)
        `when`(repository.getProductByCode("1234")).thenReturn(product)

        // Initial state
        assertEquals(true, viewModel.uiState.value.loading)

        // When
        viewModel.loadProduct("1234")

        // Then: Loading debe ser false después de cargar
        assertEquals(false, viewModel.uiState.value.loading)
    }
}

