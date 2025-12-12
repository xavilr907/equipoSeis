package com.univalle.inventarioapp.ui.home

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.univalle.inventarioapp.InventoryWidget
import com.univalle.inventarioapp.LoginActivity
import com.univalle.inventarioapp.R
import com.univalle.inventarioapp.databinding.FragmentHomeBinding
import com.univalle.inventarioapp.workers.WidgetUpdateWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.univalle.inventarioapp.data.model.ProductEntity


@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val vm: HomeViewModel by viewModels()

    @Inject
    lateinit var auth: FirebaseAuth

    private val adapter by lazy {
        ProductAdapter { code ->
            val bundle = Bundle().apply { putString("productCode", code) }
            findNavController().navigate(
                R.id.action_homeFragment_to_productDetailFragment,
                bundle
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            requireActivity().moveTaskToBack(true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbarHome)

        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter

        // STATEFLOW
        viewLifecycleOwner.lifecycleScope.launch {
            vm.uiState.collect { state ->
                when (state) {
                    is UiState.Loading -> binding.progressHome.visibility = View.VISIBLE
                    is UiState.Success -> {
                        binding.progressHome.visibility = View.GONE
                        adapter.submitList(state.products)

                        // Actualizar widget cuando cambian los productos
                        updateWidget()
                        calculateAndSaveTotal(state.products)
                    }
                    is UiState.Error -> binding.progressHome.visibility = View.GONE
                }
            }
        }

        // LIVEDATA (por compatibilidad)
        vm.products.observe(viewLifecycleOwner) { list ->
            binding.progressHome.visibility = View.GONE
            adapter.submitList(list)
            calculateAndSaveTotal(list)
        }

        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_addProductFragment)
        }
    }

    /**
     * ======================================================
     * 🟧 CÁLCULO DEL INVENTARIO + GUARDA EN PREFERENCIAS
     * ======================================================
     */
    private fun calculateAndSaveTotal(products: List<ProductEntity>) {
        var total = 0.0

        for (p in products) {
            val price = p.priceCents / 100.0
            total += price * p.quantity
        }

        val formatted = "$ " + String.format("%.2f", total)

        // Guardar para el widget
        val prefs = requireContext().getSharedPreferences("inventory_widget_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("totalInventory", formatted).apply()

        // Enviar broadcast al widget
        refreshWidget()
    }

    private fun refreshWidget() {
        val intent = Intent(requireContext(), InventoryWidget::class.java).apply {
            action = "com.univalle.inventarioapp.ACTION_REFRESH_WIDGET"
        }
        requireContext().sendBroadcast(intent)
    }

    // MENÚ
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                auth.signOut()

                // Limpiar SharedPreferences del widget
                val prefs = requireContext().getSharedPreferences("inventory_widget_prefs", Context.MODE_PRIVATE)
                prefs.edit().clear().apply()

                // Actualizar widget para mostrar estado sin sesión
                updateWidget()

                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("fromWidget", false)
                }
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateWidget() {
        // Disparar Worker para actualizar el widget con el total actualizado
        val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .addTag(WidgetUpdateWorker.TAG)
            .build()

        WorkManager.getInstance(requireContext()).enqueue(workRequest)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
