package com.univalle.inventarioapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.univalle.inventarioapp.data.model.Resource
import com.univalle.inventarioapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla de autenticación (Login/Registro).
 * Maneja la lógica de validación, estados de UI y operaciones de Firebase Auth.
 *
 * Reglas de Negocio:
 * - Email: Max 40 caracteres
 * - Password: Solo números, Min 6 - Max 10 dígitos
 * - Validación en tiempo real
 * - Botones habilitados solo con formulario válido
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Actualiza el email y valida el formulario.
     */
    fun onEmailChanged(email: String) {
        // Limitar a 40 caracteres (la UI ya tiene el filtro, esto es seguridad adicional)
        val sanitizedEmail = email.take(40)
        _uiState.update { currentState ->
            currentState.copy(
                email = sanitizedEmail,
                isFormValid = validateForm(sanitizedEmail, currentState.password)
            )
        }
    }

    /**
     * Actualiza la contraseña, valida en tiempo real y actualiza errores.
     */
    fun onPasswordChanged(password: String) {
        // Solo permitir números y limitar a 10 dígitos
        val sanitizedPassword = password.filter { it.isDigit() }.take(10)

        // Validación en tiempo real: Mínimo 6 dígitos
        val error = if (sanitizedPassword.isNotEmpty() && sanitizedPassword.length < 6) {
            "Mínimo 6 dígitos"
        } else {
            null
        }

        _uiState.update { currentState ->
            currentState.copy(
                password = sanitizedPassword,
                passwordError = error,
                isFormValid = validateForm(currentState.email, sanitizedPassword)
            )
        }
    }

    /**
     * Alterna la visibilidad de la contraseña.
     */
    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    /**
     * Valida el formulario completo.
     * Criterios:
     * - Email no vacío
     * - Password entre 6 y 10 dígitos numéricos
     */
    private fun validateForm(email: String, password: String): Boolean {
        return email.isNotEmpty() && password.length in 6..10
    }

    /**
     * Ejecuta el login con Firebase Auth.
     * Emite estados de Loading, Success o Error.
     */
    fun login(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentEmail = _uiState.value.email
        val currentPassword = _uiState.value.password

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = authRepository.signIn(currentEmail, currentPassword)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                    onError("Login incorrecto")
                }
                is Resource.Loading -> {
                    // Estado intermedio, ya manejado arriba
                }
            }
        }
    }

    /**
     * Ejecuta el registro con Firebase Auth.
     * Emite estados de Loading, Success o Error.
     */
    fun register(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentEmail = _uiState.value.email
        val currentPassword = _uiState.value.password

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = authRepository.signUp(currentEmail, currentPassword)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                    onError("Error en el registro")
                }
                is Resource.Loading -> {
                    // Estado intermedio, ya manejado arriba
                }
            }
        }
    }

    /**
     * Limpia el mensaje de error (útil para toasts temporales).
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

