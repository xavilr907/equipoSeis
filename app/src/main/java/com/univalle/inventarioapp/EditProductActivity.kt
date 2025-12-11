package com.univalle.inventarioapp.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
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

        // 1. RECUPERAR DATOS DEL INTENT (Sin consultar DB)
        val code = intent.getStringExtra("EXTRA_CODE")
        val id = intent.getStringExtra("EXTRA_ID")
        val name = intent.getStringExtra("EXTRA_NAME")
        val priceCents = intent.getLongExtra("EXTRA_PRICE", -1)
        val quantity = intent.getIntExtra("EXTRA_QTY", -1)

        if (code != null && name != null) {
            // Datos recibidos correctamente
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

    private fun setupValidation() {
        fun validar() {
            val n = binding.etName.text?.isNotEmpty() == true
            val p = binding.etPrice.text?.isNotEmpty() == true
            val q = binding.etQty.text?.isNotEmpty() == true
            val habilitar = n && p && q

            binding.btnEditar.isEnabled = habilitar
            if (habilitar) {
                binding.btnEditar.setTextColor(android.graphics.Color.WHITE)
                binding.btnEditar.setTypeface(null, android.graphics.Typeface.BOLD)
                binding.btnEditar.background.setTint(android.graphics.Color.parseColor("#FF5722"))
            } else {
                binding.btnEditar.setTextColor(android.graphics.Color.LTGRAY)
                binding.btnEditar.setTypeface(null, android.graphics.Typeface.NORMAL)
                binding.btnEditar.background.setTint(android.graphics.Color.DKGRAY)
            }
        }
        binding.etName.addTextChangedListener { validar() }
        binding.etPrice.addTextChangedListener { validar() }
        binding.etQty.addTextChangedListener { validar() }
    }

    private fun saveChanges() {
        val nombre = binding.etName.text.toString().trim()
        val precioTxt = binding.etPrice.text.toString().trim()
        val cantidadTxt = binding.etQty.text.toString().trim()

        if (nombre.isEmpty() || precioTxt.isEmpty() || cantidadTxt.isEmpty()) return

        val cantidad = cantidadTxt.toIntOrNull() ?: 0
        val precioDouble = precioTxt.replace(",", ".").toDoubleOrNull() ?: 0.0
        val priceCents = (precioDouble * 100).roundToLong()

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
            // 1. Guardar en Room (Local)
            val dao = db.productDao()
            dao.upsert(updated)

            // 2. Guardar en Firestore (Nube)
            if (currentProductId != null) {
                try {
                    val map = mapOf(
                        "name" to nombre,
                        "price" to precioDouble,
                        "quantity" to cantidad
                    )
                    firestore.collection("products").document(currentProductId!!).update(map)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@EditProductActivity, "Producto actualizado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}