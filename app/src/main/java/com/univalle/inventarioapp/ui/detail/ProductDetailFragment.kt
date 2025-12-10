package com.univalle.inventarioapp.ui.detail

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.univalle.inventarioapp.R
import com.univalle.inventarioapp.databinding.FragmentProductDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class ProductDetailFragment : Fragment(R.layout.fragment_product_detail) {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    private val vm: ProductDetailViewModel by viewModels()
    private val args: ProductDetailFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProductDetailBinding.bind(view)

        setupListeners()
        setupObservers()

        // Cargar el producto usando el code (documentId)
        vm.loadProduct(args.productCode)
    }

    private fun setupListeners() {
        // Flecha atrás
        binding.toolbarDetail.setNavigationOnClickListener {
            vm.onBack()
        }

        // Eliminar producto
        binding.buttonDelete.setOnClickListener {
            vm.deleteProduct()
        }

        // Editar producto
        binding.fabEdit.setOnClickListener {
            vm.onEdit()
        }
    }

    private fun setupObservers() {
        // Estado (Loading, Success, Error)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.uiState.collect { ui ->
                    binding.progressBar.visibility =
                        if (ui.loading) View.VISIBLE else View.GONE

                    if (ui.error != null) {
                        binding.textError.apply {
                            visibility = View.VISIBLE
                            text = ui.error
                        }
                    } else {
                        binding.textError.visibility = View.GONE
                    }

                    ui.product?.let { product ->
                        binding.textName.text = product.name
                        binding.textQuantity.text = product.quantity.toString()

                        val price = product.priceCents / 100.0
                        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())

                        binding.textPrice.text = formatter.format(price)
                        binding.textTotal.text = formatter.format(ui.total)
                    }
                }
            }
        }

        // Eventos de navegación
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.events.collect { event ->
                    when (event) {
                        ProductDetailEvent.NavigateBack -> {
                            findNavController().navigateUp()
                        }

                        ProductDetailEvent.NavigateToEdit -> {
                            findNavController().navigate(
                                R.id.action_productDetailFragment_to_editProductFragment,
                                Bundle().apply { putString("productCode", args.productCode) }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
