package com.univalle.inventarioapp.ui

import android.content.Intent // Import necesario
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.univalle.inventarioapp.MainActivity // Import necesario para volver al Home
import com.univalle.inventarioapp.data.local.AppDatabase
import com.univalle.inventarioapp.data.model.ProductEntity
import com.univalle.inventarioapp.databinding.ActivityEditProductBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong

class EditProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProductBinding
    private lateinit var db: AppDatabase
    private val firestore = FirebaseFirestore.getInstance()

    // Variables para los datos
    private var originalCode: String = ""
    private var currentProductId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getInstance(applicationContext)

        // Configurar Toolbar
        setSupportActionBar(binding.toolbarEdit)
        binding.toolbarEdit.setNavigationOnClickListener { finish() }

        // 1. RECUPERAR DATOS DEL INTENT
        val code = intent.getStringExtra("EXTRA_CODE")
        val id = intent.getStringExtra("EXTRA_ID")
        val name = intent.getStringExtra("EXTRA_NAME")
        val priceCents = intent.getLongExtra("EXTRA_PRICE", -1)
        val quantity = intent.getIntExtra("EXTRA_QTY", -1)

        if (code != null && name != null) {
            originalCode = code
            currentProductId = id

            // Llenar la UI
            binding.etId.setText(code)
            binding.etName.setText(name)
            binding.etQty.setText(quantity.toString())

            val pesos = priceCents / 100.0
            binding.etPrice.setText(pesos.toString())
        } else {
            Toast.makeText(this, "Error al recibir datos", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupValidation()

        binding.btnEditar.setOnClickListener {
            saveChanges()
        }
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