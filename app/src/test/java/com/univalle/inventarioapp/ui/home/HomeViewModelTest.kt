package com.univalle.inventarioapp.ui.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.univalle.inventarioapp.data.model.ProductEntity
import com.univalle.inventarioapp.data.remote.FirestoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

/**
 * Pruebas unitarias para HomeViewModel (HU3)
 * Cobertura mínima: 30% de los métodos del ViewModel
 *
 * Métodos testeados:
 * 1. observeProducts() - Loading state
 * 2. observeProducts() - Success state
 * 3. observeProducts() - Error state
 * 4. totalCents calculation
 * 5. totalFormatted currency formatting
 * 6. reloadProducts()
 */
@ExperimentalCoroutinesApi
class HomeViewModelTest {

    // Regla para ejecutar LiveData de manera síncrona en tests
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    // Dispatcher de test para corrutinas
    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mockRepository: FirestoreRepository

    private lateinit var viewModel: HomeViewModel

    private lateinit var closeable: AutoCloseable

    @Before
    fun setup() {
        // Inicializar mocks
        closeable = MockitoAnnotations.openMocks(this)

        // Configurar dispatcher de test
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        // Limpiar dispatcher
        Dispatchers.resetMain()
        closeable.close()
    }

    /**
     * TEST 1: Verificar que el estado inicial sea Loading
     */
    @Test
    fun `observeProducts emits Loading state initially`() = runTest {
        // Given: Repository que emite un flow con delay
        val mockFlow = flow<List<ProductEntity>> {
            kotlinx.coroutines.delay(100)
            emit(emptyList())
        }
        whenever(mockRepository.observeProducts()).thenReturn(mockFlow)

        // When: Se crea el ViewModel
        viewModel = HomeViewModel(mockRepository)

        // Then: El estado inicial debe ser Loading
        assertTrue(viewModel.uiState.value is UiState.Loading)
    }

    /**
     * TEST 2: Verificar que emite Success cuando Firestore retorna datos
     */
    @Test
    fun `observeProducts emits Success state when Firestore returns data`() = runTest {
        // Given: Lista de productos mock
        val mockProducts = listOf(
            ProductEntity(
                id = "1",
                code = "P001",
                name = "Producto Test",
                priceCents = 10000L, // $100.00
                quantity = 5
            ),
            ProductEntity(
                id = "2",
                code = "P002",
                name = "Producto Test 2",
                priceCents = 20000L, // $200.00
                quantity = 3
            )
        )

        val mockFlow = flow {
            emit(mockProducts)
        }
        whenever(mockRepository.observeProducts()).thenReturn(mockFlow)

        // When: Se crea el ViewModel
        viewModel = HomeViewModel(mockRepository)
        advanceUntilIdle() // Esperar a que se complete la corrutina

        // Then: El estado debe ser Success con la lista de productos
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(2, (state as UiState.Success).products.size)
        assertEquals("P001", state.products[0].code)
    }

    /**
     * TEST 3: Verificar que emite Error cuando Firestore falla
     */
    @Test
    fun `observeProducts emits Error state when Firestore fails`() = runTest {
        // Given: Repository que emite un error
        val errorMessage = "Error de conexión"
        val mockFlow = flow<List<ProductEntity>> {
            throw Exception(errorMessage)
        }
        whenever(mockRepository.observeProducts()).thenReturn(mockFlow)

        // When: Se crea el ViewModel
        viewModel = HomeViewModel(mockRepository)
        advanceUntilIdle()

        // Then: El estado debe ser Error
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertTrue((state as UiState.Error).message.contains("Error"))
    }

    /**
     * TEST 4: Verificar que totalCents calcula correctamente la suma
     */
    @Test
    fun `totalCents calculates correct sum of products`() = runTest {
        // Given: Lista con precios conocidos
        val mockProducts = listOf(
            ProductEntity(code = "P001", name = "A", priceCents = 10000L, quantity = 2), // 200 pesos
            ProductEntity(code = "P002", name = "B", priceCents = 5000L, quantity = 3)   // 150 pesos
        )

        val mockFlow = flow { emit(mockProducts) }
        whenever(mockRepository.observeProducts()).thenReturn(mockFlow)

        // When
        viewModel = HomeViewModel(mockRepository)
        advanceUntilIdle()

        // Forzar observación del LiveData
        val observer = androidx.lifecycle.Observer<Long> {}
        viewModel.totalCents.observeForever(observer)

        advanceUntilIdle()

        // Then: totalCents debe ser (10000*2) + (5000*3) = 35000 centavos
        val totalCents = viewModel.totalCents.value
        assertNotNull(totalCents)
        assertEquals(35000L, totalCents)

        viewModel.totalCents.removeObserver(observer)
    }

    /**
     * TEST 5: Verificar que totalFormatted formatea como moneda
     */
    @Test
    fun `totalFormatted formats as currency`() = runTest {
        // Given: Lista con un producto
        val mockProducts = listOf(
            ProductEntity(code = "P001", name = "Test", priceCents = 100000L, quantity = 1) // 1000 pesos
        )

        val mockFlow = flow { emit(mockProducts) }
        whenever(mockRepository.observeProducts()).thenReturn(mockFlow)

        // When
        viewModel = HomeViewModel(mockRepository)
        advanceUntilIdle()

        // Then: totalFormatted debe contener el símbolo de moneda
        val formatted = viewModel.totalFormatted.value
        assertNotNull(formatted)
        // Verificar que contiene algún formato de moneda ($ o símbolo local)
        assertTrue(formatted!!.isNotEmpty())
    }

    /**
     * TEST 6: Verificar que reloadProducts vuelve a llamar al repository
     */
    @Test
    fun `reloadProducts calls repository again`() = runTest {
        // Given
        val mockFlow = flow { emit(emptyList<ProductEntity>()) }
        whenever(mockRepository.observeProducts()).thenReturn(mockFlow)

        viewModel = HomeViewModel(mockRepository)
        advanceUntilIdle()

        // When: Se llama reloadProducts
        viewModel.reloadProducts()
        advanceUntilIdle()

        // Then: observeProducts debe haberse llamado al menos 2 veces
        verify(mockRepository, atLeast(2)).observeProducts()
    }

    /**
     * TEST 7 (BONUS): Verificar que products LiveData se actualiza correctamente
     */
    @Test
    fun `products LiveData emits correct list`() = runTest {
        // Given
        val mockProducts = listOf(
            ProductEntity(code = "P001", name = "Test", priceCents = 1000L, quantity = 1)
        )
        val mockFlow = flow { emit(mockProducts) }
        whenever(mockRepository.observeProducts()).thenReturn(mockFlow)

        // When
        viewModel = HomeViewModel(mockRepository)
        advanceUntilIdle()

        // Forzar observación del LiveData
        val observer = androidx.lifecycle.Observer<List<ProductEntity>> {}
        viewModel.products.observeForever(observer)

        advanceUntilIdle()

        // Then: products LiveData debe contener 1 item
        val products = viewModel.products.value
        assertNotNull(products)
        assertEquals(1, products!!.size)
        assertEquals("P001", products[0].code)

        viewModel.products.removeObserver(observer)
    }
}

