package com.univalle.inventarioapp.ui.edit

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.univalle.inventarioapp.MainActivity
import com.univalle.inventarioapp.databinding.ActivityEditProductBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProductBinding
    private val viewModel: EditProductViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar Toolbar
        setSupportActionBar(binding.toolbarEdit)
        binding.toolbarEdit.setNavigationOnClickListener { finish() }

        // Recuperar datos del Intent
        val code = intent.getStringExtra("EXTRA_CODE") ?: ""
        val id = intent.getStringExtra("EXTRA_ID")
        val name = intent.getStringExtra("EXTRA_NAME") ?: ""
        val priceCents = intent.getLongExtra("EXTRA_PRICE", 0)
        val quantity = intent.getIntExtra("EXTRA_QTY", 0)

        if (code.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Error al recibir datos", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Cargar datos en ViewModel
        viewModel.loadProduct(code, name, priceCents, quantity, id)

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        binding.etName.addTextChangedListener {
            viewModel.onNameChanged(it.toString())
        }
        binding.etPrice.addTextChangedListener {
            viewModel.onPriceChanged(it.toString())
        }
        binding.etQty.addTextChangedListener {
            viewModel.onQuantityChanged(it.toString())
        }
        binding.btnEditar.setOnClickListener {
            viewModel.updateProduct()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Pre-llenar campos (solo en la primera carga)
                    if (state.product != null && binding.etId.text.isNullOrEmpty()) {
                        binding.etId.setText(state.code)
                        binding.etName.setText(state.name)
                        binding.etPrice.setText(state.priceText)
                        binding.etQty.setText(state.quantityText)
                    }

                    // Actualizar estado del botón
                    binding.btnEditar.isEnabled = state.isValid && !state.loading

                    if (state.isValid && !state.loading) {
                        binding.btnEditar.setTextColor(android.graphics.Color.WHITE)
                        binding.btnEditar.setTypeface(null, android.graphics.Typeface.BOLD)
                        binding.btnEditar.background.setTint(android.graphics.Color.parseColor("#FF5722"))
                    } else {
                        binding.btnEditar.setTextColor(android.graphics.Color.LTGRAY)
                        binding.btnEditar.setTypeface(null, android.graphics.Typeface.NORMAL)
                        binding.btnEditar.background.setTint(android.graphics.Color.DKGRAY)
                    }

                    // Actualizar texto del botón
                    binding.btnEditar.text = if (state.loading) "Guardando..." else "Editar"
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is EditProductEvent.NavigateToHome -> {
                            Toast.makeText(
                                this@EditProductActivity,
                                "Producto actualizado",
                                Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(this@EditProductActivity, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            finish()
                        }
                        is EditProductEvent.ShowError -> {
                            Toast.makeText(
                                this@EditProductActivity,
                                event.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }
}