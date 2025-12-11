package com.univalle.inventarioapp.data.repository

import com.univalle.inventarioapp.data.model.ProductEntity
import com.univalle.inventarioapp.data.remote.FirestoreRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetRepositoryTest {

    @Mock
    private lateinit var firestoreRepository: FirestoreRepository

    private lateinit var widgetRepository: WidgetRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        widgetRepository = WidgetRepository(firestoreRepository)
    }

    @Test
    fun `calculateTotalInventory con lista vacía retorna $ 0,00`() = runTest {
        // Given
        `when`(firestoreRepository.observeProducts()).thenReturn(flowOf(emptyList()))

        // When
        val result = widgetRepository.calculateTotalInventory()

        // Then
        assertEquals("$ 0,00", result)
    }

    @Test
    fun `calculateTotalInventory calcula correctamente precio por cantidad`() = runTest {
        // Given: 2 productos
        val products = listOf(
            ProductEntity(code = "001", name = "Laptop", priceCents = 150000, quantity = 2),  // $1500 * 2 = $3000
            ProductEntity(code = "002", name = "Mouse", priceCents = 3260, quantity = 10)      // $32.60 * 10 = $326
        )
        `when`(firestoreRepository.observeProducts()).thenReturn(flowOf(products))

        // When
        val result = widgetRepository.calculateTotalInventory()

        // Then: $3000 + $326 = $3326.00 -> "$ 3.326,00"
        assertEquals("$ 3.326,00", result)
    }

    @Test
    fun `formatCurrency formatea correctamente con separador de miles y decimales`() {
        // Given
        val amount = 3326.00

        // When
        val result = widgetRepository.formatCurrency(amount)

        // Then
        assertEquals("$ 3.326,00", result)
    }

    @Test
    fun `formatCurrency formatea correctamente números grandes`() {
        // Given
        val amount = 1234567.89

        // When
        val result = widgetRepository.formatCurrency(amount)

        // Then: Separador de miles: punto, decimales: coma
        assertEquals("$ 1.234.567,89", result)
    }

    @Test
    fun `formatCurrency formatea correctamente números pequeños`() {
        // Given
        val amount = 5.50

        // When
        val result = widgetRepository.formatCurrency(amount)

        // Then
        assertEquals("$ 5,50", result)
    }

    @Test
    fun `calculateTotalInventory maneja errores retornando $ 0,00`() = runTest {
        // Given
        `when`(firestoreRepository.observeProducts()).thenThrow(RuntimeException("Network error"))

        // When
        val result = widgetRepository.calculateTotalInventory()

        // Then
        assertEquals("$ 0,00", result)
    }

    @Test
    fun `calculateTotalInventory con un solo producto calcula correctamente`() = runTest {
        // Given
        val products = listOf(
            ProductEntity(code = "001", name = "Tablet", priceCents = 80000, quantity = 5)  // $800 * 5 = $4000
        )
        `when`(firestoreRepository.observeProducts()).thenReturn(flowOf(products))

        // When
        val result = widgetRepository.calculateTotalInventory()

        // Then
        assertEquals("$ 4.000,00", result)
    }
}

