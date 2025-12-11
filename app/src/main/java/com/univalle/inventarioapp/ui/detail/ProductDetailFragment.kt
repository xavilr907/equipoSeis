package com.univalle.inventarioapp.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.univalle.inventarioapp.R
import com.univalle.inventarioapp.data.model.ProductEntity
import com.univalle.inventarioapp.databinding.FragmentProductDetailBinding
import com.univalle.inventarioapp.ui.EditProductActivity
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

    // Variable para guardar el producto completo que estamos viendo
    private var currentProduct: ProductEntity? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProductDetailBinding.bind(view)

        setupListeners()
        setupObservers()

        vm.loadProduct(args.productCode)
    }

    private fun setupListeners() {
        binding.toolbarDetail.setNavigationIcon(R.drawable.ic_arrow_back_white)
        binding.toolbarDetail.setNavigationOnClickListener { vm.onBack() }
        binding.buttonDelete.setOnClickListener { vm.deleteProduct() }
        binding.fabEdit.setOnClickListener { vm.onEdit() }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.uiState.collect { ui ->
                    binding.progressBar.visibility = if (ui.loading) View.VISIBLE else View.GONE

                    ui.product?.let { product ->
                        // Guardamos el producto actual para enviarlo después
                        currentProduct = product

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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.events.collect { event ->
                    when (event) {
                        ProductDetailEvent.NavigateBack -> findNavController().navigateUp()
                        ProductDetailEvent.NavigateToEdit -> {
                            if (currentProduct != null) {
                                // ESTRATEGIA INFALIBLE: Pasamos dato por dato
                                val intent = Intent(requireContext(), EditProductActivity::class.java)
                                intent.putExtra("EXTRA_CODE", currentProduct!!.code)
                                intent.putExtra("EXTRA_ID", currentProduct!!.id)
                                intent.putExtra("EXTRA_NAME", currentProduct!!.name)
                                intent.putExtra("EXTRA_PRICE", currentProduct!!.priceCents) // long
                                intent.putExtra("EXTRA_QTY", currentProduct!!.quantity)     // int
                                startActivity(intent)
                            } else {
                                Toast.makeText(requireContext(), "Cargando datos...", Toast.LENGTH_SHORT).show()
                            }
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