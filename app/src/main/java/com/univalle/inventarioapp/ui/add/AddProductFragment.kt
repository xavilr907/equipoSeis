package com.univalle.inventarioapp.ui.add

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import com.univalle.inventarioapp.data.model.ProductEntity
import com.univalle.inventarioapp.databinding.FragmentAddProductBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment encargado de agregar un nuevo producto al inventario.
 *
 * Funcionalidades:
 * - Validación en tiempo real de todos los campos.
 * - Validación total antes de guardar.
 * - Uso de ViewModel para registrar o actualizar productos.
 * - Navegación de retorno al completar la operación.
 *
 * Este fragment cumple con la HU de validación y registro de productos.
 */
@AndroidEntryPoint
class AddProductFragment : Fragment() {

    /** Binding del layout del fragment. */
    private var _binding: FragmentAddProductBinding? = null
    private val binding get() = _binding!!

    /** ViewModel inyectado con Hilt. */
    private val vm: AddProductViewModel by viewModels()

    /**
     * Infla el layout y crea el binding.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Configura listeners, validaciones y el evento del botón Guardar.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Botón de la toolbar (flecha atrás)
        binding.toolbarAddProduct.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // Agregar validación en tiempo real
        binding.etCode.addTextChangedListener(watcher)
        binding.etName.addTextChangedListener(watcher)
        binding.etPrice.addTextChangedListener(watcher)
        binding.etQty.addTextChangedListener(watcher)

        // Acción al presionar "Guardar"
        binding.btnSave.setOnClickListener {
            if (!validateAll()) return@setOnClickListener

            val code = binding.etCode.text.toString().trim()
            val name = binding.etName.text.toString().trim()
            val priceCents = binding.etPrice.text.toString().trim().toLongOrNull() ?: 0L
            val qty = binding.etQty.text.toString().trim().toIntOrNull() ?: 0

            val product = ProductEntity(
                code = code,
                name = name,
                priceCents = priceCents,
                quantity = qty
            )

            // Guardar producto usando el ViewModel
            viewLifecycleOwner.lifecycleScope.launch {
                vm.upsert(
                    product,
                    onDone = {
                        Toast.makeText(
                            requireContext(),
                            "Producto guardado",
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().popBackStack()
                    },
                    onError = { e ->
                        Toast.makeText(
                            requireContext(),
                            "Aviso: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }

        // Estado inicial del botón
        updateButtonState()
    }

    /**
     * TextWatcher compartido para todos los EditText.
     * Actualiza el estado del botón después de cualquier cambio.
     */
    private val watcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        /**
         * Se ejecuta cuando el campo se actualiza.
         * Aquí se valida todo sin mostrar errores.
         */
        override fun afterTextChanged(s: Editable?) {
            updateButtonState()
        }
    }

    /**
     * Habilita o deshabilita el botón Guardar según si los campos son válidos.
     *
     * @param showErrors si es `true`, muestra los errores visualmente.
     */
    private fun updateButtonState() {
        binding.btnSave.isEnabled = validateAll(showErrors = false)
    }

    /**
     * Valida todos los campos siguiendo las reglas de negocio.
     *
     * Reglas:
     * - Código: 1 a 4 dígitos numéricos.
     * - Nombre: requerido, máx 40 chars.
     * - Precio: numérico >= 0.
     * - Cantidad: entero >= 0 máx 4 dígitos.
     *
     * @param showErrors Si `true`, muestra los errores en pantalla.
     * @return `true` si todo es válido, de lo contrario `false`.
     */
    private fun validateAll(showErrors: Boolean = true): Boolean {
        val code = binding.etCode.text.toString().trim()
        val name = binding.etName.text.toString().trim()
        val priceStr = binding.etPrice.text.toString().trim()
        val qtyStr = binding.etQty.text.toString().trim()

        var ok = true

        // Código: entre 1 y 4 dígitos numéricos
        if (!(code.length in 1..4 && code.all { it.isDigit() })) {
            ok = false
            setError(
                binding.tilCode,
                if (showErrors) "Debe ser un número de 1 a 4 dígitos" else null
            )
        } else setError(binding.tilCode, null)

        // Nombre: requerido, máximo 40 caracteres
        if (name.isEmpty() || name.length > 40) {
            ok = false
            setError(
                binding.tilName,
                if (showErrors) "Campo requerido (máx. 40 caracteres)" else null
            )
        } else setError(binding.tilName, null)

        // Precio: número >= 0
        val priceCents = priceStr.toLongOrNull()
        if (priceCents == null || priceCents < 0) {
            ok = false
            setError(
                binding.tilPrice,
                if (showErrors) "Ingrese un número válido (>= 0)" else null
            )
        } else setError(binding.tilPrice, null)

        // Cantidad: entero >= 0 y máx. 4 dígitos
        val qty = qtyStr.toIntOrNull()
        if (qty == null || qty < 0 || qtyStr.length > 4) {
            ok = false
            setError(
                binding.tilQty,
                if (showErrors) "Ingrese un entero válido (máx. 4 dígitos)" else null
            )
        } else setError(binding.tilQty, null)

        return ok
    }

    /**
     * Aplica o quita un mensaje de error en un TextInputLayout.
     *
     * @param til TextInputLayout al que se le aplica el error.
     * @param message mensaje a mostrar, o null para limpiar el error.
     */
    private fun setError(til: TextInputLayout, message: String?) {
        til.error = message
        til.isErrorEnabled = !message.isNullOrEmpty()
    }

    /**
     * Limpia bindings y listeners para evitar memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()

        binding.etCode.removeTextChangedListener(watcher)
        binding.etName.removeTextChangedListener(watcher)
        binding.etPrice.removeTextChangedListener(watcher)
        binding.etQty.removeTextChangedListener(watcher)

        _binding = null
    }
}
