package com.univalle.inventarioapp.ui.home

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
import com.google.firebase.auth.FirebaseAuth
import com.univalle.inventarioapp.LoginActivity
import com.univalle.inventarioapp.R
import com.univalle.inventarioapp.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

/**
 * Fragment de Inventario (HU3)
 * Muestra la lista de productos desde Firestore
 * Cumple criterios:
 * - Persistencia de sesión (MainActivity verifica)
 * - Diseño con colores especificados
 * - Logout limpia backstack
 * - Botón atrás minimiza app
 * - Lista con diseño especificado
 * - Estado de carga
 * - FAB naranja
 * - Navegación a detalle
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // ViewModel inyectado por Hilt
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

        // CRITERIO 4: BackHandler para minimizar app en lugar de volver a Login
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

        // CRITERIO 2: Toolbar configurado
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbarHome)

        // RecyclerView
        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter

        // Observar StateFlow para estados UI (Loading, Success, Error)
        viewLifecycleOwner.lifecycleScope.launch {
            vm.uiState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.progressHome.visibility = View.VISIBLE
                    }
                    is UiState.Success -> {
                        binding.progressHome.visibility = View.GONE
                        adapter.submitList(state.products)
                    }
                    is UiState.Error -> {
                        binding.progressHome.visibility = View.GONE
                        // Mostrar error (puedes agregar un TextView o Snackbar)
                    }
                }
            }
        }

        vm.products.observe(viewLifecycleOwner) { list ->
            binding.progressHome.visibility = View.GONE
            adapter.submitList(list)

            // Calcular total del inventario
            val totalValueCents = list.sumOf { it.priceCents * it.quantity }

            //Covertir a pesos
            val totalValuePesos = totalValueCents/ 100.0

            // Formatear con puntos de miles sin deprecated
            val formattedTotal = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO")).format(totalValuePesos)

            // Guardar en SharedPreferences para que el widget lo muestre
            val prefs = requireContext().getSharedPreferences("inventory_widget_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("totalInventory", "$ $formattedTotal").apply()
        }


        // Observar LiveData de productos (compatibilidad)
        vm.products.observe(viewLifecycleOwner) { list ->
            binding.progressHome.visibility = View.GONE
            adapter.submitList(list)
        }


        // CRITERIO 7: FAB agregar producto
        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_addProductFragment)
        }
    }

    // CRITERIO 3: Menú cerrar sesión
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                // CRITERIO 3: Logout limpia backstack
                auth.signOut()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
