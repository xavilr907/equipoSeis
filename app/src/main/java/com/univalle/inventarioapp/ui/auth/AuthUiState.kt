package com.univalle.inventarioapp.ui.auth

/**
 * Data class que representa el estado de la UI de autenticación.
 * Se usa con StateFlow en AuthViewModel para reactividad.
 */
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val passwordError: String? = null,
    val isFormValid: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

