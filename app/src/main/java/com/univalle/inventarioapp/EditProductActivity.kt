package com.univalle.inventarioapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.univalle.inventarioapp.data.local.AppDatabase
import com.univalle.inventarioapp.data.model.ProductEntity
import com.univalle.inventarioapp.databinding.ActivityEditProductBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong
import androidx.core.widget.addTextChangedListener


class EditProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProductBinding
    private lateinit var db: AppDatabase

    // PK del producto
    private var originalCode: String = ""
    // id de Firestore (si lo hubiera)
    private var currentProductId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar con flecha atrás
        setSupportActionBar(binding.toolbarEdit)
        binding.toolbarEdit.setNavigationOnClickListener {
            finish()   // vuelve a la pantalla anterior
        }

        // Código recibido desde ProductDetailFragment
        originalCode = intent.getStringExtra("EXTRA_CODE") ?: run {
            Toast.makeText(this, "Producto no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Instancia Room
        db = AppDatabase.getInstance(applicationContext)

        // Cargar datos del producto para editar
        loadProduct()

        // Habiitar boton dinamicamente
        fun validarCampos() {
            val nombre = binding.etName.text?.isNotEmpty() == true
            val precio = binding.etPrice.text?.isNotEmpty() == true
            val cantidad = binding.etQty.text?.isNotEmpty() == true

            val habilitar = nombre && precio && cantidad

            binding.btnEditar.isEnabled = habilitar

            if (habilitar) {
                binding.btnEditar.setTextColor(android.graphics.Color.WHITE)
                binding.btnEditar.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                binding.btnEditar.setTextColor(android.graphics.Color.GRAY)
                binding.btnEditar.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }

// Detectar cambios
        binding.etName.addTextChangedListener { validarCampos() }
        binding.etPrice.addTextChangedListener { validarCampos() }
        binding.etQty.addTextChangedListener { validarCampos() }


        // Botón Editar
        binding.btnEditar.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadProduct() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = db.productDao()
            val product = dao.getByCode(originalCode)

            withContext(Dispatchers.Main) {
                if (product == null) {
                    Toast.makeText(
                        this@EditProductActivity,
                        "Producto no encontrado",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@withContext
                }

                // Guardamos el id de Firestore por si acaso
                currentProductId = product.id

                // Rellenar campos
                binding.etId.setText(product.code)          // ID visible (code)
                binding.etName.setText(product.name)

                val pesos = product.priceCents / 100.0
                binding.etPrice.setText(pesos.toString())

                binding.etQty.setText(product.quantity.toString())
            }
        }
    }

    private fun saveChanges() {
        val nombre = binding.etName.text.toString().trim()
        val precioTxt = binding.etPrice.text.toString().trim()
        val cantidadTxt = binding.etQty.text.toString().trim()

        if (nombre.isEmpty() || precioTxt.isEmpty() || cantidadTxt.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val cantidad = cantidadTxt.toIntOrNull()
        // Cambiamos coma por punto por si el usuario mete decimales con coma
        val precioDouble = precioTxt.replace(",", ".").toDoubleOrNull()

        if (cantidad == null || precioDouble == null) {
            Toast.makeText(this, "Valores numéricos inválidos", Toast.LENGTH_SHORT).show()
            return
        }

        // pesos -> centavos
        val priceCents = (precioDouble * 100).roundToLong()

        val updated = ProductEntity(
            id = currentProductId,   // mantenemos id de Firestore
            code = originalCode,     // PK no se cambia
            name = nombre,
            priceCents = priceCents,
            quantity = cantidad
        )

        lifecycleScope.launch(Dispatchers.IO) {
            val dao = db.productDao()
            dao.upsert(updated)

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@EditProductActivity,
                    "Producto actualizado",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
}
