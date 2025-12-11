package com.univalle.inventarioapp.ui

import android.content.Intent // Import necesario para navegar entre Activities
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

/**
 * Activity para editar un producto existente.
 *
 * Funciones principales:
 * 1. Recupera los datos de un producto desde un Intent.
 * 2. Muestra los datos en la interfaz para su edición.
 * 3. Valida los campos en tiempo real antes de habilitar el botón de guardar.
 * 4. Guarda los cambios en la base de datos local (Room) y en Firestore.
 * 5. Redirige al MainActivity al finalizar la edición.
 */
class EditProductActivity : AppCompatActivity() {

    // Binding de la vista
    private lateinit var binding: ActivityEditProductBinding

    // Instancia de la base de datos local
    private lateinit var db: AppDatabase

    // Instancia de Firestore (nube)
    private val firestore = FirebaseFirestore.getInstance()

    // Código original del producto (no editable)
    private var originalCode: String = ""

    // ID del producto en Firestore (puede ser nulo)
    private var currentProductId: String? = null

    /**
     * Método llamado al crear la Activity.
     * Inicializa la interfaz, recupera datos del Intent y configura validaciones.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Instancia de la base de datos
        db = AppDatabase.getInstance(applicationContext)

        // Configurar Toolbar con botón de navegación
        setSupportActionBar(binding.toolbarEdit)
        binding.toolbarEdit.setNavigationOnClickListener { finish() }

        // 1. RECUPERAR DATOS DEL INTENT
        val code = intent.getStringExtra("EXTRA_CODE")
        val id = intent.getStringExtra("EXTRA_ID")
        val name = intent.getStringExtra("EXTRA_NAME")
        val priceCents = intent.getLongExtra("EXTRA_PRICE", -1)
        val quantity = intent.getIntExtra("EXTRA_QTY", -1)

        if (code != null && name != null) {
            // Datos recibidos correctamente
            originalCode = code
            currentProductId = id

            // Llenar la UI con los datos del producto
            binding.etId.setText(code)
            binding.etName.setText(name)
            binding.etQty.setText(quantity.toString())

            val pesos = priceCents / 100.0
            binding.etPrice.setText(pesos.toString())
        } else {
            // Error al recibir datos del Intent
            Toast.makeText(this, "Error al recibir datos", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Configurar validaciones en tiempo real
        setupValidation()

        // Botón para guardar cambios
        binding.btnEditar.setOnClickListener {
            saveChanges()
        }
    }

    /**
     * Configura la validación de los campos en tiempo real.
     *
     * Habilita o deshabilita el botón de guardar según si todos los campos están llenos.
     * Cambia el color, estilo y fondo del botón según el estado.
     */
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

        // Asignar validación a cambios en los campos de texto
        binding.etName.addTextChangedListener { validar() }
        binding.etPrice.addTextChangedListener { validar() }
        binding.etQty.addTextChangedListener { validar() }
    }

    /**
     * Guarda los cambios realizados en el producto.
     *
     * Flujo:
     * 1. Valida que los campos no estén vacíos.
     * 2. Convierte el precio a centavos.
     * 3. Actualiza la base de datos local (Room).
     * 4. Actualiza Firestore (si el ID existe).
     * 5. Muestra un Toast al finalizar.
     * 6. Redirige al MainActivity limpiando la pila de Activities previas.
     */
    private fun saveChanges() {
        val nombre = binding.etName.text.toString().trim()
        val precioTxt = binding.etPrice.text.toString().trim()
        val cantidadTxt = binding.etQty.text.toString().trim()

        // Validación básica
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

        // Actualizar UI mientras se guardan los datos
        binding.btnEditar.isEnabled = false
        binding.btnEditar.text = "Guardando..."

        lifecycleScope.launch(Dispatchers.IO) {
            // 1. Guardar en Room (local)
            val dao = db.productDao()
            dao.upsert(updated)

            // 2. Guardar en Firestore (nube)
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

                // --- Redirigir al Home (MainActivity) ---
                val intent = Intent(this@EditProductActivity, MainActivity::class.java)
                // Flags para limpiar la pila: Borra Detalle y Editar del historial
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
    }
}
