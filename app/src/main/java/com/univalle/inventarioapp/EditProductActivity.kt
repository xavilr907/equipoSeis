package com.univalle.inventarioapp.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore // Importación necesaria
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
    private val firestore = FirebaseFirestore.getInstance() // Instancia de Firestore

    // PK del producto
    private var originalCode: String = ""
    // id de Firestore
    private var currentProductId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar
        setSupportActionBar(binding.toolbarEdit)
        binding.toolbarEdit.setNavigationOnClickListener {
            finish()
        }

        // 1. Recibimos el CÓDIGO (String) desde el Detalle
        originalCode = intent.getStringExtra("EXTRA_CODE") ?: run {
            Toast.makeText(this, "Producto no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db = AppDatabase.getInstance(applicationContext)

        loadProduct()

        // Validaciones en tiempo real
        fun validarCampos() {
            val nombre = binding.etName.text?.isNotEmpty() == true
            val precio = binding.etPrice.text?.isNotEmpty() == true
            val cantidad = binding.etQty.text?.isNotEmpty() == true

            val habilitar = nombre && precio && cantidad
            binding.btnEditar.isEnabled = habilitar

            if (habilitar) {
                binding.btnEditar.setTextColor(android.graphics.Color.WHITE)
                binding.btnEditar.setTypeface(null, android.graphics.Typeface.BOLD)
                binding.btnEditar.background.setTint(android.graphics.Color.parseColor("#FF5722")) // Naranja activo
            } else {
                binding.btnEditar.setTextColor(android.graphics.Color.LTGRAY)
                binding.btnEditar.setTypeface(null, android.graphics.Typeface.NORMAL)
                binding.btnEditar.background.setTint(android.graphics.Color.DKGRAY) // Gris inactivo
            }
        }

        binding.etName.addTextChangedListener { validarCampos() }
        binding.etPrice.addTextChangedListener { validarCampos() }
        binding.etQty.addTextChangedListener { validarCampos() }

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
                    Toast.makeText(this@EditProductActivity, "Producto no encontrado", Toast.LENGTH_SHORT).show()
                    finish()
                    return@withContext
                }

                currentProductId = product.id

                binding.etId.setText(product.code)
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

        if (nombre.isEmpty() || precioTxt.isEmpty() || cantidadTxt.isEmpty()) return

        val cantidad = cantidadTxt.toIntOrNull()
        val precioDouble = precioTxt.replace(",", ".").toDoubleOrNull()

        if (cantidad == null || precioDouble == null) {
            Toast.makeText(this, "Valores numéricos inválidos", Toast.LENGTH_SHORT).show()
            return
        }

        val priceCents = (precioDouble * 100).roundToLong()

        // Objeto actualizado
        val updated = ProductEntity(
            id = currentProductId,
            code = originalCode,
            name = nombre,
            priceCents = priceCents,
            quantity = cantidad
        )

        binding.btnEditar.isEnabled = false
        binding.btnEditar.text = "Guardando..."

        lifecycleScope.launch(Dispatchers.IO) {
            // 1. ACTUALIZAR ROOM (LOCAL)
            val dao = db.productDao()
            dao.upsert(updated)

            // 2. ACTUALIZAR FIRESTORE (NUBE) - ¡AGREGADO!
            if (currentProductId != null) {
                val firestoreMap = mapOf(
                    "name" to nombre,
                    "price" to precioDouble, // Firestore suele usar el valor decimal, no centavos
                    "quantity" to cantidad
                    // Agrega "image" si fuera necesario
                )

                try {
                    // Usamos tasks.await() o un listener simple, aquí lo hacemos asíncrono simple
                    firestore.collection("products")
                        .document(currentProductId!!)
                        .update(firestoreMap)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Si falla internet, al menos ya se guardó en local (Room)
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@EditProductActivity, "Producto actualizado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}