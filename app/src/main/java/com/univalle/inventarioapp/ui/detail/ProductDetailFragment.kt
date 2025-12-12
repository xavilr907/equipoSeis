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
import com.univalle.inventarioapp.ui.edit.EditProductActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Fragment encargado de mostrar la información detallada de un producto.
 * Desde aquí el usuario puede:
 * - Ver nombre, cantidad, precio unitario y total.
 * - Editar el producto.
 * - Eliminarlo.
 * - Volver atrás.
 *
 * Usa un ViewModel para manejar el estado y los eventos de navegación.
 */
@AndroidEntryPoint
class ProductDetailFragment : Fragment(R.layout.fragment_product_detail) {

    /** Binding del layout asociado al fragmento */
    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    /** ViewModel encargado de manejar los datos y eventos */
    private val vm: ProductDetailViewModel by viewModels()

    /** Argumentos recibidos mediante Safe Args (código del producto) */
    private val args: ProductDetailFragmentArgs by navArgs()

    /**
     * Variable para almacenar temporalmente el producto completo cargado
     * Esto permite enviarlo luego a la Activity de edición.
     */
    private var currentProduct: ProductEntity? = null

    /**
     * Se ejecuta cuando la vista ha sido creada
     * Configura listeners, observadores y solicita la carga de datos.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProductDetailBinding.bind(view)

        setupListeners()
        setupObservers()

        // Cargar el producto según el código recibido
        vm.loadProduct(args.productCode)
    }

    /**
     * Configura los botones:
     * - Flecha atrás
     * - Botón eliminar
     * - Botón editar
     */
    private fun setupListeners() {
        binding.toolbarDetail.setNavigationIcon(R.drawable.ic_arrow_back_white)
        binding.toolbarDetail.setNavigationOnClickListener { vm.onBack() }
        binding.buttonDelete.setOnClickListener { vm.deleteProduct() }
        binding.fabEdit.setOnClickListener { vm.onEdit() }
    }

    /**
     * Observa tanto el estado UI como los eventos emitidos por el ViewModel.
     * - Actualiza la interfaz según la información del producto.
     * - Navega hacia atrás o hacia la pantalla de edición según el evento.
     */
    private fun setupObservers() {
        // Observador del UI State
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.uiState.collect { ui ->
                    // Mostrar u ocultar progress bar
                    binding.progressBar.visibility = if (ui.loading) View.VISIBLE else View.GONE

                    // Si se cargó el producto, mostrar datos en pantalla
                    ui.product?.let { product ->
                        // Guardamos el producto para usarlo al editar
                        currentProduct = product

                        binding.textName.text = product.name
                        binding.textQuantity.text = product.quantity.toString()

                        // Formatear precio con moneda local
                        val price = product.priceCents / 100.0
                        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
                        binding.textPrice.text = formatter.format(price)
                        binding.textTotal.text = formatter.format(ui.total)
                    }
                }
            }
        }

        // Observador de eventos de navegación
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.events.collect { event ->
                    when (event) {
                        ProductDetailEvent.NavigateBack ->
                            findNavController().navigateUp()

                        ProductDetailEvent.NavigateToEdit -> {
                            if (currentProduct != null) {

                                // Enviamos los datos uno por uno para garantizar compatibilidad
                                val intent = Intent(requireContext(), EditProductActivity::class.java)
                                intent.putExtra("EXTRA_CODE", currentProduct!!.code)
                                intent.putExtra("EXTRA_ID", currentProduct!!.id)
                                intent.putExtra("EXTRA_NAME", currentProduct!!.name)
                                intent.putExtra("EXTRA_PRICE", currentProduct!!.priceCents)
                                intent.putExtra("EXTRA_QTY", currentProduct!!.quantity)

                                startActivity(intent)

                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Cargando datos...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Limpia el binding cuando la vista se destruye
     * para evitar fugas de memoria.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
