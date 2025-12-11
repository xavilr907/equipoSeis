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

/**
 * Actividad para editar un producto existente en la base de datos.
 *
 * Esta actividad permite:
 * - Cargar los datos de un producto usando su código.
 * - Editar el nombre, precio y cantidad del producto.
 * - Validar campos dinámicamente antes de habilitar el botón de guardar.
 * - Guardar los cambios en la base de datos local (Room) y mantener la referencia a Firestore.
 */
class EditProductActivity : AppCompatActivity() {

    /** Binding generado para acceder a las vistas de la actividad */
    private lateinit var binding: ActivityEditProductBinding

    /** Instancia de la base de datos local (Room) */
    private lateinit var db: AppDatabase

    /** Código original del producto (clave primaria) */
    private var originalCode: String = ""

    /** ID del producto en Firestore (si existe) */
    private var currentProductId: String? = null

    /**
     * Método llamado al crear la actividad.
     * Inicializa el binding, toolbar, instancia de base de datos, carga los datos del producto
     * y configura la validación de campos y el botón de editar.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicialización del binding
        binding = ActivityEditProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configuración del toolbar con flecha de retroceso
        setSupportActionBar(binding.toolbarEdit)
        binding.toolbarEdit.setNavigationOnClickListener {
            finish()   // vuelve a la pantalla anterior
        }

        // Obtener el código del producto pasado desde ProductDetailFragment
        originalCode = intent.getStringExtra("EXTRA_CODE") ?: run {
            Toast.makeText(this, "Producto no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Instancia de Room
        db = AppDatabase.getInstance(applicationContext)

        // Cargar datos del producto para edición
        loadProduct()

        /**
         * Función interna para validar los campos y habilitar/deshabilitar el botón de editar
         * dinámicamente según si todos los campos obligatorios están completos.
         */
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

        // Detectar cambios en los campos de texto para habilitar/deshabilitar el botón
        binding.etName.addTextChangedListener { validarCampos() }
        binding.etPrice.addTextChangedListener { validarCampos() }
        binding.etQty.addTextChangedListener { validarCampos() }

        // Configuración del botón "Editar" para guardar los cambios
        binding.btnEditar.setOnClickListener {
            saveChanges()
        }
    }

    /**
     * Carga los datos del producto desde la base de datos local usando su código.
     *
     * Si el producto no se encuentra, se muestra un Toast y se cierra la actividad.
     * Los campos de la UI se rellenan con los valores existentes del producto.
     */
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

                // Rellenar campos de la UI
                binding.etId.setText(product.code)          // ID visible (code)
                binding.etName.setText(product.name)

                val pesos = product.priceCents / 100.0
                binding.etPrice.setText(pesos.toString())

                binding.etQty.setText(product.quantity.toString())
            }
        }
    }

    /**
     * Guarda los cambios realizados en el producto.
     *
     * Valida los campos obligatorios y numéricos antes de actualizar la base de datos.
     * Convierte el precio a centavos para almacenarlo correctamente.
     * Muestra un Toast de confirmación y cierra la actividad al finalizar.
     */
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

        // Convertir precio de pesos a centavos
        val priceCents = (precioDouble * 100).roundToLong()

        // Crear objeto actualizado del producto
        val updated = ProductEntity(
            id = currentProductId,   // mantenemos id de Firestore
            code = originalCode,     // PK no se cambia
            name = nombre,
            priceCents = priceCents,
            quantity = cantidad
        )

        // Guardar en la base de datos en un hilo IO
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
