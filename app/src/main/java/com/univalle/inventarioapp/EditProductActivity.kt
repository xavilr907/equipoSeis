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

/**
 * Activity para la edición de un producto existente.
 *
 * Esta actividad permite al usuario modificar los datos de un producto,
 * tales como nombre, precio y cantidad, y actualizarlos en la base de datos.
 * Implementa observadores para el estado de la interfaz y eventos del ViewModel.
 */
@AndroidEntryPoint
class EditProductActivity : AppCompatActivity() {

    /** Binding generado para la vista de edición de producto */
    private lateinit var binding: ActivityEditProductBinding

    /** ViewModel asociado a la edición de producto */
    private val viewModel: EditProductViewModel by viewModels()

    /**
     * Método llamado al crear la actividad.
     *
     * Configura la vista, toolbar, recupera los datos del producto desde el Intent
     * y carga el producto en el ViewModel. Además, inicializa los listeners y observadores.
     *
     * @param savedInstanceState Bundle con el estado previo de la actividad, si existe.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar Toolbar con botón de navegación
        setSupportActionBar(binding.toolbarEdit)
        binding.toolbarEdit.setNavigationOnClickListener { finish() }

        // Recuperar datos del Intent que inició la actividad
        val code = intent.getStringExtra("EXTRA_CODE") ?: ""
        val id = intent.getStringExtra("EXTRA_ID")
        val name = intent.getStringExtra("EXTRA_NAME") ?: ""
        val priceCents = intent.getLongExtra("EXTRA_PRICE", 0)
        val quantity = intent.getIntExtra("EXTRA_QTY", 0)

        // Validar que se recibieron los datos esenciales
        if (code.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Error al recibir datos", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Cargar datos del producto en el ViewModel
        viewModel.loadProduct(code, name, priceCents, quantity, id)

        setupListeners()
        setupObservers()
    }

    /**
     * Configura los listeners de la interfaz de usuario.
     *
     * Se incluyen:
     * - TextChangedListener para campos de nombre, precio y cantidad.
     * - ClickListener para el botón de editar producto.
     */
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

    /**
     * Configura los observadores del ViewModel.
     *
     * Observa dos flujos principales:
     * 1. [uiState]: para actualizar la interfaz de usuario en función del estado actual del producto.
     * 2. [events]: para reaccionar a eventos como la navegación o errores.
     */
    private fun setupObservers() {
        // Observador del estado de la UI
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Pre-llenar campos al cargar el producto (solo la primera vez)
                    if (state.product != null && binding.etId.text.isNullOrEmpty()) {
                        binding.etId.setText(state.code)
                        binding.etName.setText(state.name)
                        binding.etPrice.setText(state.priceText)
                        binding.etQty.setText(state.quantityText)
                    }

                    // Habilitar o deshabilitar el botón según la validez y el estado de carga
                    binding.btnEditar.isEnabled = state.isValid && !state.loading

                    // Cambiar apariencia del botón según el estado
                    if (state.isValid && !state.loading) {
                        binding.btnEditar.setTextColor(android.graphics.Color.WHITE)
                        binding.btnEditar.setTypeface(null, android.graphics.Typeface.BOLD)
                        binding.btnEditar.background.setTint(android.graphics.Color.parseColor("#FF5722"))
                    } else {
                        binding.btnEditar.setTextColor(android.graphics.Color.LTGRAY)
                        binding.btnEditar.setTypeface(null, android.graphics.Typeface.NORMAL)
                        binding.btnEditar.background.setTint(android.graphics.Color.DKGRAY)
                    }

                    // Actualizar texto del botón según si se está guardando
                    binding.btnEditar.text = if (state.loading) "Guardando..." else "Editar"
                }
            }
        }

        // Observador de eventos del ViewModel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is EditProductEvent.NavigateToHome -> {
                            // Mostrar mensaje y regresar a MainActivity
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
                            // Mostrar mensaje de error
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
