package com.univalle.inventarioapp.ui.detail

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.room.Room
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.univalle.inventarioapp.data.local.AppDatabase
import com.univalle.inventarioapp.databinding.FragmentProductDetailBinding

class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var vm: ProductDetailViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar back
        binding.toolbarDetail.setNavigationIcon(com.univalle.inventarioapp.R.drawable.ic_arrow_back_white)
        binding.toolbarDetail.setNavigationOnClickListener {
            // Prefer explicit action back to home if present
            try {
                findNavController().navigate(com.univalle.inventarioapp.R.id.action_productDetail_to_home)
            } catch (e: Exception) {
                findNavController().navigateUp()
            }
        }

        // DB and ViewModel
        val db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java,
            "inventario.db"
        ).fallbackToDestructiveMigration().build()

        val productCode = arguments?.getString("productCode") ?: run {
            // no code -> go back
            findNavController().navigateUp()
            return
        }

        val factory = ProductDetailViewModelFactory(db.productDao(), productCode)
        vm = ViewModelProvider(this, factory)[ProductDetailViewModel::class.java]

        // Observers
        vm.product.observe(viewLifecycleOwner) { p ->
            if (p == null) return@observe
            binding.tvProductName.text = p.name
            binding.tvPrice.text = formatCurrency(p.priceCents)
            binding.tvQuantity.text = p.quantity.toString()
        }

        vm.totalFormatted.observe(viewLifecycleOwner) { t ->
            binding.tvTotal.text = t
        }

        vm.navigateBack.observe(viewLifecycleOwner) { goBack ->
            if (goBack == true) {
                // After delete, navigate explicitly to Home (HU 3.0)
                try {
                    findNavController().navigate(com.univalle.inventarioapp.R.id.action_productDetail_to_home)
                } catch (e: Exception) {
                    findNavController().navigateUp()
                }
            }
        }

        vm.error.observe(viewLifecycleOwner) { err ->
            err?.let {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Error")
                    .setMessage(it)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        // Delete button
        binding.btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que deseas eliminar este producto?")
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Si") { _, _ ->
                    vm.deleteProduct()
                }
                .show()
        }

        // FAB edit -> navigate to EditProductFragment (HU 6.0)
        binding.fabEdit.setOnClickListener {
            val bundle = Bundle().apply { putString("productCode", productCode) }
            // try navigate to edit; if destination not present, just ignore
            try {
                findNavController().navigate(com.univalle.inventarioapp.R.id.action_productDetail_to_editProductFragment, bundle)
            } catch (e: Exception) {
                // fallback: show message
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("Funcionalidad de edición no disponible")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun formatCurrency(cents: Long): String {
        val units = cents / 100.0
        return java.text.NumberFormat.getCurrencyInstance(java.util.Locale.getDefault()).format(units)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
