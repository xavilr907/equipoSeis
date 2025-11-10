package com.univalle.inventarioapp.ui.add

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.room.Room
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.univalle.inventarioapp.data.local.AppDatabase
import com.univalle.inventarioapp.data.model.ProductEntity
import com.univalle.inventarioapp.data.remote.FirestoreRepository
import com.univalle.inventarioapp.databinding.FragmentAddProductBinding

class AddProductFragment : Fragment() {

    private var _binding: FragmentAddProductBinding? = null
    private val binding get() = _binding!!

    private lateinit var vm: AddProductViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Room
        val db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java,
            "inventario.db"
        ).fallbackToDestructiveMigration().build()

        // Firestore repo
        val fsRepo = FirestoreRepository(FirebaseFirestore.getInstance())

        // VM con ambos
        val factory = AddProductViewModelFactory(db.productDao(), fsRepo)
        vm = ViewModelProvider(this, factory)[AddProductViewModel::class.java]

        // Validación en tiempo real
        binding.etCode.addTextChangedListener(watcher)
        binding.etName.addTextChangedListener(watcher)
        binding.etPrice.addTextChangedListener(watcher)
        binding.etQty.addTextChangedListener(watcher)

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

            vm.upsert(
                product,
                onDone = {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Producto guardado (local y nube)", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                },
                onError = { e ->
                    requireActivity().runOnUiThread {
                        // Si ves este toast y ya guardó local, solo falló la nube.
                        Toast.makeText(requireContext(), "Aviso: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        updateButtonState()
    }

    private val watcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) { updateButtonState() }
    }

    private fun updateButtonState() {
        binding.btnSave.isEnabled = validateAll(showErrors = false)
    }

    private fun validateAll(showErrors: Boolean = true): Boolean {
        val code = binding.etCode.text.toString().trim()
        val name = binding.etName.text.toString().trim()
        val priceStr = binding.etPrice.text.toString().trim()
        val qtyStr = binding.etQty.text.toString().trim()

        var ok = true

        if (!(code.length == 4 && code.all { it.isDigit() })) {
            ok = false
            setError(binding.tilCode, if (showErrors) "Debe tener 4 dígitos" else null)
        } else setError(binding.tilCode, null)

        if (name.isEmpty() || name.length > 40) {
            ok = false
            setError(binding.tilName, if (showErrors) "Requerido (máx. 40)" else null)
        } else setError(binding.tilName, null)

        val priceCents = priceStr.toLongOrNull()
        if (priceCents == null || priceCents < 0) {
            ok = false
            setError(binding.tilPrice, if (showErrors) "Número válido (>=0)" else null)
        } else setError(binding.tilPrice, null)

        val qty = qtyStr.toIntOrNull()
        if (qty == null || qty < 0) {
            ok = false
            setError(binding.tilQty, if (showErrors) "Entero válido (>=0)" else null)
        } else setError(binding.tilQty, null)

        return ok
    }

    private fun setError(til: TextInputLayout, message: String?) {
        til.error = message
        til.isErrorEnabled = !message.isNullOrEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
