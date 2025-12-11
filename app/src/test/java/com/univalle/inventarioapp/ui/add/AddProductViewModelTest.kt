package com.univalle.inventarioapp.ui.add

import com.univalle.inventarioapp.data.local.ProductDao
import com.univalle.inventarioapp.data.model.ProductEntity
import com.univalle.inventarioapp.data.remote.FirestoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class AddProductViewModelTest {

    @Mock
    private lateinit var productDao: ProductDao

    @Mock
    private lateinit var firestoreRepo: FirestoreRepository

    private lateinit var viewModel: AddProductViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = AddProductViewModel(productDao, firestoreRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `upsert llama a productDao y firestoreRepo exitosamente`() = runTest {
        val product = ProductEntity(code = "1234", name = "Test Product", priceCents = 1000, quantity = 10)
        var onDoneCalled = false

        `when`(productDao.upsert(product)).thenReturn(Unit)
        `when`(firestoreRepo.upsertProduct(product)).thenReturn(Unit)

        viewModel.upsert(product, { onDoneCalled = true }, {})

        verify(productDao).upsert(product)
        verify(firestoreRepo).upsertProduct(product)
        assert(onDoneCalled) { "onDone callback should be called" }
    }

    @Test
    fun `upsert maneja error de base de datos local`() = runTest {
        val product = ProductEntity(code = "1234", name = "Test Product", priceCents = 1000, quantity = 10)
        var errorCaptured: Throwable? = null
        val exception = RuntimeException("Database error")

        `when`(productDao.upsert(product)).thenThrow(exception)

        viewModel.upsert(product, {}, { errorCaptured = it })

        assert(errorCaptured == exception) { "Error callback should receive the exception" }
    }

    @Test
    fun `upsert continua si Firestore falla pero llama onError`() = runTest {
        val product = ProductEntity(code = "1234", name = "Test Product", priceCents = 1000, quantity = 10)
        var onDoneCalled = false
        var errorCaptured: Throwable? = null
        val exception = RuntimeException("Firestore error")

        `when`(productDao.upsert(product)).thenReturn(Unit)
        `when`(firestoreRepo.upsertProduct(product)).thenThrow(exception)

        viewModel.upsert(product, { onDoneCalled = true }, { errorCaptured = it })

        verify(productDao).upsert(product)
        assert(errorCaptured == exception) { "Error callback should receive Firestore exception" }
        assert(onDoneCalled) { "onDone should still be called even if Firestore fails" }
    }

    @Test
    fun `upsert valida que producto tenga datos correctos`() = runTest {
        val product = ProductEntity(code = "0001", name = "Laptop", priceCents = 150000, quantity = 5)
        var onDoneCalled = false

        `when`(productDao.upsert(product)).thenReturn(Unit)
        `when`(firestoreRepo.upsertProduct(product)).thenReturn(Unit)

        viewModel.upsert(product, { onDoneCalled = true }, {})

        verify(productDao).upsert(product)
        verify(firestoreRepo).upsertProduct(product)
        assert(onDoneCalled)
    }

    @Test
    fun `upsert no llama a Firestore si productDao falla`() = runTest {
        val product = ProductEntity(code = "1234", name = "Test", priceCents = 1000, quantity = 10)
        val exception = RuntimeException("DAO error")

        `when`(productDao.upsert(product)).thenThrow(exception)

        viewModel.upsert(product, {}, {})

        verify(productDao).upsert(product)
        verify(firestoreRepo, never()).upsertProduct(any())
    }
}

