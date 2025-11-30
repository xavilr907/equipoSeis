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

        auth = FirebaseAuth.getInstance()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Limitar email a 40 caracteres
        binding.etEmail.filters = arrayOf(InputFilter.LengthFilter(40))

        // Mostrar/ocultar contraseña
        binding.ivTogglePassword.setOnClickListener {
            val isVisible = binding.etPassword.transformationMethod == null
            if (isVisible) {
                binding.etPassword.transformationMethod =
                    android.text.method.PasswordTransformationMethod.getInstance()
                binding.ivTogglePassword.setImageResource(R.drawable.cerrado)
            } else {
                binding.etPassword.transformationMethod = null
                binding.ivTogglePassword.setImageResource(R.drawable.abierto)
            }
            binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
        }

        // Validaciones en tiempo real
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateForm()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.etEmail.addTextChangedListener(watcher)
        binding.etPassword.addTextChangedListener(watcher)

        binding.btnLogin.setOnClickListener { doLogin() }
        binding.tvRegister.setOnClickListener { doRegister() }

        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && binding.btnLogin.isEnabled) {
                doLogin()
                true
            } else false
        }

        validateForm()
    }

    private fun validateForm() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        val emailOk = email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
        val passwordOk = password.matches(Regex("^\\d{6,10}\$"))

        if (password.isNotEmpty() && password.length < 6) {
            binding.tilPassword.error = "Mínimo 6 dígitos"
        } else {
            binding.tilPassword.error = null
        }

        binding.btnLogin.isEnabled = emailOk && passwordOk
        binding.tvRegister.isEnabled = emailOk && passwordOk
        binding.tvRegister.alpha = if (binding.tvRegister.isEnabled) 1f else 0.6f
    }

    private fun doLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        binding.progressBar.alpha = 1f
        binding.progressBar.isIndeterminate = true
        binding.btnLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.progressBar.alpha = 0f
                binding.btnLogin.isEnabled = true

                if (task.isSuccessful) {
                    onAuthSuccess()
                } else {
                    Toast.makeText(this, "Login incorrecto", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun doRegister() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        binding.progressBar.alpha = 1f
        binding.progressBar.isIndeterminate = true
        binding.tvRegister.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.progressBar.alpha = 0f
                binding.tvRegister.isEnabled = true

                if (task.isSuccessful) {
                    onAuthSuccess(isRegistration = true)
                } else {
                    Toast.makeText(this, "Error en el registro", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun onAuthSuccess(isRegistration: Boolean = false) {
        val fromWidget = intent.getBooleanExtra("fromWidget", false)

        if (fromWidget) {
            // Avisar al widget que ya hay sesión
            val refresh = Intent().apply {
                action = ACTION_REFRESH
                setClass(this@LoginActivity, InventoryWidget::class.java)
            }
            sendBroadcast(refresh)

            // Abrir la app (pantalla principal)
            val homeIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(homeIntent)

            return
        }

        // Si NO viene desde el widget -> flujo normal
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
