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

@AndroidEntryPoint
class AddProductFragment : Fragment() {

    private var _binding: FragmentAddProductBinding? = null
    private val binding get() = _binding!!

    private val vm: AddProductViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Flecha de la toolbar: volver atrás (Home en tu flujo) ---
        binding.toolbarAddProduct.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // --- Validación en tiempo real ---
        binding.etCode.addTextChangedListener(watcher)
        binding.etName.addTextChangedListener(watcher)
        binding.etPrice.addTextChangedListener(watcher)
        binding.etQty.addTextChangedListener(watcher)

        // --- Click en Guardar ---
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

    // --- TextWatcher compartido para todos los campos ---
    private val watcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            updateButtonState()
        }
    }

    // Habilita/deshabilita el botón según la validación
    private fun updateButtonState() {
        binding.btnSave.isEnabled = validateAll(showErrors = false)
    }

    // Valida todos los campos según la HU
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

        // Nombre: requerido, máximo 40 (maxLength ya limita)
        if (name.isEmpty() || name.length > 40) {
            ok = false
            setError(
                binding.tilName,
                if (showErrors) "Campo requerido (máx. 40 caracteres)" else null
            )
        } else setError(binding.tilName, null)

        // Precio: número >= 0 (máx. 20 dígitos lo controla maxLength)
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

    private fun setError(til: TextInputLayout, message: String?) {
        til.error = message
        til.isErrorEnabled = !message.isNullOrEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remover listeners para prevenir memory leaks
        binding.etCode.removeTextChangedListener(watcher)
        binding.etName.removeTextChangedListener(watcher)
        binding.etPrice.removeTextChangedListener(watcher)
        binding.etQty.removeTextChangedListener(watcher)
        _binding = null
    }
}
