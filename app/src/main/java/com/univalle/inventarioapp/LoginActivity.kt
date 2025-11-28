package com.univalle.inventarioapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Patterns
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.univalle.inventarioapp.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    companion object {
        const val ACTION_REFRESH = "com.univalle.inventarioapp.ACTION_REFRESH_WIDGET"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Limitar email a 40 caracteres
        binding.etEmail.filters = arrayOf(InputFilter.LengthFilter(40))

        // Password solo números: podemos controlar con InputType en XML, pero validamos aquí también
        // Mostrar/ocultar contraseña
        binding.ivTogglePassword.setOnClickListener {
            val isVisible = binding.etPassword.transformationMethod == null
            if (isVisible) {
                // actualmente visible -> ocultar
                binding.etPassword.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                binding.ivTogglePassword.setImageResource(R.drawable.cerrado) // adapta tu drawable
            } else {
                binding.etPassword.transformationMethod = null
                binding.ivTogglePassword.setImageResource(R.drawable.abierto) // adapta tu drawable
            }
            // mover cursor al final
            binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
        }

        // TextWatchers para validación en tiempo real
        val tw = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateForm()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.etEmail.addTextChangedListener(tw)
        binding.etPassword.addTextChangedListener(tw)

        // Acciones botones
        binding.btnLogin.setOnClickListener { doLogin() }
        binding.tvRegister.setOnClickListener { doRegister() }

        // Manejo "done" en teclado
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && binding.btnLogin.isEnabled) {
                doLogin()
                true
            } else false
        }

        // Inicial
        validateForm()
    }

    private fun validateForm() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Email válido básico
        val emailOk = email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

        // Password: solo números y entre 6 y 10 dígitos
        val passwordOk = password.matches(Regex("^\\d{6,10}\$"))

        // Mensaje de error en tiempo real
        if (password.isNotEmpty() && password.length < 6) {
            binding.tilPassword.error = "Mínimo 6 dígitos"
        } else {
            binding.tilPassword.error = null
        }

        binding.btnLogin.isEnabled = emailOk && passwordOk
        // "Registrarse" visible/activo solo si campos completos
        binding.tvRegister.isEnabled = emailOk && passwordOk
        binding.tvRegister.alpha = if (binding.tvRegister.isEnabled) 1f else 0.6f
    }

    private fun doLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Mostrar loading simple
        binding.progressBar.alpha = 1f
        binding.progressBar.isIndeterminate = true
        binding.btnLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.progressBar.alpha = 0f
                binding.btnLogin.isEnabled = true

                if (task.isSuccessful) {
                    // Login exitoso
                    onAuthSuccess()
                } else {
                    // Login incorrecto
                    Toast.makeText(this, "Login incorrecto", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun doRegister() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Guardamos con FirebaseAuth
        binding.progressBar.alpha = 1f
        binding.progressBar.isIndeterminate = true
        binding.tvRegister.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.progressBar.alpha = 0f
                binding.tvRegister.isEnabled = true

                if (task.isSuccessful) {
                    // Registro exitoso
                    onAuthSuccess(isRegistration = true)
                } else {
                    // Error en el registro
                    Toast.makeText(this, "Error en el registro", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun onAuthSuccess(isRegistration: Boolean = false) {
        val fromWidget = intent.getBooleanExtra("fromWidget", false)

        if (fromWidget) {
            // Si vino desde el widget, solo avisamos al widget para que se actualice y cerramos
            val refresh = Intent().apply {
                action = ACTION_REFRESH
                setClass(this@LoginActivity, InventoryWidget::class.java)
            }
            sendBroadcast(refresh)
            // Cerramos para que el usuario vuelva al launcher / widget
            finish()
            return
        }

        // Si vino desde la app: abrir MainActivity (Home)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
