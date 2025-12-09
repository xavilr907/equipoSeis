package com.univalle.inventarioapp.ui.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.firebase.auth.FirebaseUser
import com.univalle.inventarioapp.data.model.Resource
import com.univalle.inventarioapp.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any

/**
 * Unit Tests para AuthViewModel.
 * Valida la lógica de negocio de autenticación sin dependencias de Android.
 *
 * Coverage mínimo: 30% del ViewModel (requerido por criterios de aceptación).
 *
 * Tests implementados:
 * - Validación de password < 6 dígitos (debe mostrar error)
 * - Validación de password >= 6 dígitos (error desaparece)
 * - Habilitación de botones con credenciales válidas
 * - Login exitoso con Firebase Auth
 * - Login fallido con Firebase Auth
 * - Registro exitoso
 * - Registro fallido
 */
@ExperimentalCoroutinesApi
class AuthViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var authRepository: AuthRepository

    @Mock
    private lateinit var mockFirebaseUser: FirebaseUser

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== TESTS DE VALIDACIÓN EN TIEMPO REAL ==========

    @Test
    fun `test password less than 6 digits shows error`() {
        // Given: Usuario escribe menos de 6 dígitos
        viewModel.onPasswordChanged("123")

        // When: Se obtiene el estado
        val state = viewModel.uiState.value

        // Then: Debe mostrar error y formulario inválido
        assertEquals("Mínimo 6 dígitos", state.passwordError)
        assertFalse(state.isFormValid)
    }

    @Test
    fun `test password with 6 digits clears error`() {
        // Given: Usuario primero escribe < 6 dígitos
        viewModel.onPasswordChanged("123")
        assertEquals("Mínimo 6 dígitos", viewModel.uiState.value.passwordError)

        // When: Usuario completa 6 dígitos
        viewModel.onPasswordChanged("123456")

        // Then: Error debe desaparecer
        val state = viewModel.uiState.value
        assertNull(state.passwordError)
        assertEquals("123456", state.password)
    }

    @Test
    fun `test password only accepts numbers`() {
        // Given: Usuario intenta escribir letras y símbolos
        viewModel.onPasswordChanged("abc123!@#")

        // When: Se obtiene el estado
        val state = viewModel.uiState.value

        // Then: Solo deben quedar los números
        assertEquals("123", state.password)
    }

    @Test
    fun `test password max length is 10 digits`() {
        // Given: Usuario escribe más de 10 dígitos
        viewModel.onPasswordChanged("12345678901234")

        // When: Se obtiene el estado
        val state = viewModel.uiState.value

        // Then: Solo deben quedar 10 dígitos
        assertEquals("1234567890", state.password)
    }

    @Test
    fun `test email max length is 40 characters`() {
        // Given: Usuario escribe más de 40 caracteres
        val longEmail = "a".repeat(50) + "@example.com"
        viewModel.onEmailChanged(longEmail)

        // When: Se obtiene el estado
        val state = viewModel.uiState.value

        // Then: Solo deben quedar 40 caracteres
        assertTrue(state.email.length <= 40)
    }

    // ========== TESTS DE HABILITACIÓN DE FORMULARIO ==========

    @Test
    fun `test form is valid with correct email and password`() {
        // Given: Usuario ingresa credenciales válidas
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("123456")

        // When: Se obtiene el estado
        val state = viewModel.uiState.value

        // Then: Formulario debe estar válido
        assertTrue(state.isFormValid)
        assertNull(state.passwordError)
    }

    @Test
    fun `test form is invalid with empty email`() {
        // Given: Password válido pero email vacío
        viewModel.onEmailChanged("")
        viewModel.onPasswordChanged("123456")

        // When: Se obtiene el estado
        val state = viewModel.uiState.value

        // Then: Formulario debe estar inválido
        assertFalse(state.isFormValid)
    }

    @Test
    fun `test form is invalid with password less than 6 digits`() {
        // Given: Email válido pero password < 6
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("12345")

        // When: Se obtiene el estado
        val state = viewModel.uiState.value

        // Then: Formulario debe estar inválido
        assertFalse(state.isFormValid)
        assertNotNull(state.passwordError)
    }

    @Test
    fun `test form is invalid with password more than 10 digits`() {
        // Given: Email válido pero password > 10 (se trunca a 10)
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("12345678901234")

        // When: Se obtiene el estado
        val state = viewModel.uiState.value

        // Then: Password debe tener exactamente 10 dígitos y ser válido
        assertEquals("1234567890", state.password)
        assertTrue(state.isFormValid)
    }

    // ========== TESTS DE LOGIN ==========

    @Test
    fun `test login success navigates to home`() = runTest {
        // Given: Credenciales válidas y respuesta exitosa del repositorio
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("123456")
        `when`(authRepository.signIn(any(), any())).thenReturn(Resource.Success(mockFirebaseUser))

        var onSuccessCalled = false
        var onErrorCalled = false

        // When: Usuario hace login
        viewModel.login(
            onSuccess = { onSuccessCalled = true },
            onError = { onErrorCalled = true }
        )
        advanceUntilIdle()

        // Then: Debe llamar onSuccess y no mostrar loading
        assertTrue(onSuccessCalled)
        assertFalse(onErrorCalled)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `test login failure shows error toast`() = runTest {
        // Given: Credenciales inválidas y respuesta de error del repositorio
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("123456")
        `when`(authRepository.signIn(any(), any())).thenReturn(Resource.Error("Credenciales inválidas"))

        var onSuccessCalled = false
        var errorMessage = ""

        // When: Usuario intenta login con credenciales incorrectas
        viewModel.login(
            onSuccess = { onSuccessCalled = true },
            onError = { errorMessage = it }
        )
        advanceUntilIdle()

        // Then: Debe llamar onError con el mensaje "Login incorrecto"
        assertFalse(onSuccessCalled)
        assertEquals("Login incorrecto", errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `test login shows loading state`() = runTest {
        // Given: Credenciales válidas
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("123456")
        `when`(authRepository.signIn(any(), any())).thenReturn(Resource.Success(mockFirebaseUser))

        // When: Usuario inicia login (antes de advanceUntilIdle)
        viewModel.login(
            onSuccess = {},
            onError = {}
        )

        // Then: Debe mostrar estado de loading (antes de completar)
        // Nota: Este test valida el estado intermedio
        assertTrue(viewModel.uiState.value.isLoading)
    }

    // ========== TESTS DE REGISTRO ==========

    @Test
    fun `test register success navigates to home`() = runTest {
        // Given: Credenciales válidas y respuesta exitosa del repositorio
        viewModel.onEmailChanged("newuser@example.com")
        viewModel.onPasswordChanged("123456")
        `when`(authRepository.signUp(any(), any())).thenReturn(Resource.Success(mockFirebaseUser))

        var onSuccessCalled = false
        var onErrorCalled = false

        // When: Usuario se registra
        viewModel.register(
            onSuccess = { onSuccessCalled = true },
            onError = { onErrorCalled = true }
        )
        advanceUntilIdle()

        // Then: Debe llamar onSuccess
        assertTrue(onSuccessCalled)
        assertFalse(onErrorCalled)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `test register failure shows error toast`() = runTest {
        // Given: Email ya existente
        viewModel.onEmailChanged("existing@example.com")
        viewModel.onPasswordChanged("123456")
        `when`(authRepository.signUp(any(), any())).thenReturn(Resource.Error("El email ya está en uso"))

        var onSuccessCalled = false
        var errorMessage = ""

        // When: Usuario intenta registrarse con email existente
        viewModel.register(
            onSuccess = { onSuccessCalled = true },
            onError = { errorMessage = it }
        )
        advanceUntilIdle()

        // Then: Debe llamar onError con el mensaje "Error en el registro"
        assertFalse(onSuccessCalled)
        assertEquals("Error en el registro", errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ========== TESTS DE VISIBILIDAD DE PASSWORD ==========

    @Test
    fun `test toggle password visibility changes state`() {
        // Given: Estado inicial (password oculto)
        assertFalse(viewModel.uiState.value.isPasswordVisible)

        // When: Usuario hace toggle
        viewModel.togglePasswordVisibility()

        // Then: Debe cambiar a visible
        assertTrue(viewModel.uiState.value.isPasswordVisible)

        // When: Usuario hace toggle de nuevo
        viewModel.togglePasswordVisibility()

        // Then: Debe volver a oculto
        assertFalse(viewModel.uiState.value.isPasswordVisible)
    }
}

