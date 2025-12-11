package com.univalle.inventarioapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.univalle.inventarioapp.databinding.ActivityLoginBinding
import com.univalle.inventarioapp.ui.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Activity de Login y Registro con arquitectura MVVM.
 * Implementa HU2 con validación en tiempo real, estados visuales y navegación.
 *
 * Criterios de Aceptación:
 * - Fondo negro, sin Toolbar
 * - Email: Max 40 chars, hint blanco flotante
 * - Password: Solo números (6-10 dígitos), validación en tiempo real con borde rojo/blanco
 * - Botón Login: Naranja (habilitado) / Gris (deshabilitado)
 * - Botón Registro: Texto gris, mismas reglas de habilitación
 */
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    companion object {
        const val ACTION_REFRESH = "com.univalle.inventarioapp.ACTION_REFRESH_WIDGET"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    /**
     * Configura listeners de la UI (TextWatchers, clicks).
     */
    private fun setupUI() {
        // TextWatcher para Email
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onEmailChanged(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // TextWatcher para Password (validación en tiempo real)
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onPasswordChanged(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Click en botón Login
        binding.btnLogin.setOnClickListener {
            viewModel.login(
                onSuccess = { navigateToHome() },
                onError = { message -> showToast(message) }
            )
        }

        // Click en TextView Registro
        binding.tvRegister.setOnClickListener {
            viewModel.register(
                onSuccess = { navigateToHome() },
                onError = { message -> showToast(message) }
            )
        }
    }

    /**
     * Observa cambios en el StateFlow del ViewModel y actualiza la UI.
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Actualizar texto de Email (si el ViewModel lo sanitiza)
                if (binding.etEmail.text.toString() != state.email) {
                    binding.etEmail.setText(state.email)
                    binding.etEmail.setSelection(state.email.length)
                }

                // Actualizar texto de Password (solo números, max 10)
                if (binding.etPassword.text.toString() != state.password) {
                    binding.etPassword.setText(state.password)
                    binding.etPassword.setSelection(state.password.length)
                }

                // Mostrar error de password en tiempo real (criterio 4)
                binding.tilPassword.error = state.passwordError

                // Cambiar color del borde según error (rojo si hay error, blanco si no)
                if (state.passwordError != null) {
                    binding.tilPassword.boxStrokeColor = ContextCompat.getColor(this@LoginActivity, R.color.error_red)
                } else {
                    binding.tilPassword.boxStrokeColor = ContextCompat.getColor(this@LoginActivity, R.color.white)
                }

                // Habilitar/deshabilitar botones según validación (criterios 6 y 7)
                binding.btnLogin.isEnabled = state.isFormValid && !state.isLoading
                binding.tvRegister.isEnabled = state.isFormValid && !state.isLoading

                // Criterio 12: Cambiar color a blanco bold cuando se habilita
                binding.tvRegister.setTextColor(
                    if (state.isFormValid && !state.isLoading) {
                        ContextCompat.getColor(this@LoginActivity, R.color.white)
                    } else {
                        ContextCompat.getColor(this@LoginActivity, R.color.text_gray)
                    }
                )
                binding.tvRegister.alpha = if (state.isFormValid && !state.isLoading) 1f else 0.6f

                // Mostrar/ocultar ProgressBar
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    /**
     * Navega a MainActivity (Home - HU3) o cierra para volver al Widget.
     * CRITERIO 10: Si viene del widget (ojo), NO ir al Home, solo cerrar y volver al widget.
     */
    private fun navigateToHome() {
        val fromWidget = intent.getBooleanExtra("fromWidget", false)
        val fromWidgetGestionar = intent.getBooleanExtra("fromWidgetGestionar", false)

        when {
            // CASO 1: Click en OJO del widget (Criterio 10)
            // Usuario quiere ver el saldo → Volver al widget, NO al Home
            fromWidget && !fromWidgetGestionar -> {
                // 1. Actualizar widget para mostrar saldo
                val refresh = Intent().apply {
                    action = ACTION_REFRESH
                    setClass(this@LoginActivity, InventoryWidget::class.java)
                }
                sendBroadcast(refresh)

                // 2. Cerrar LoginActivity y volver al Widget
                finish()
            }

            // CASO 2: Click en GESTIONAR del widget
            // Usuario quiere administrar inventario → Ir al Home
            fromWidget && fromWidgetGestionar -> {
                // 1. Actualizar widget
                val refresh = Intent().apply {
                    action = ACTION_REFRESH
                    setClass(this@LoginActivity, InventoryWidget::class.java)
                }
                sendBroadcast(refresh)

                // 2. Ir al Home
                val homeIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(homeIntent)
                finish()
            }

            // CASO 3: Login normal (desde la app, no desde widget)
            else -> {
                val homeIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(homeIntent)
                finish()
            }
        }
    }

    /**
     * Muestra un Toast con el mensaje especificado.
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
