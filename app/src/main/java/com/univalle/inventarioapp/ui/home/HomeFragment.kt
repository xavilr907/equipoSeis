package com.univalle.inventarioapp.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.univalle.inventarioapp.LoginActivity
import com.univalle.inventarioapp.R
import com.univalle.inventarioapp.data.local.AppDatabase
import com.univalle.inventarioapp.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var vm: HomeViewModel

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
        setHasOptionsMenu(true) // habilita menú en el fragment
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbarHome)

        // RecyclerView
        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter

        binding.progressHome.visibility = View.VISIBLE

        val db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java,
            "inventario.db"
        ).fallbackToDestructiveMigration().build()

        val factory = HomeViewModelFactory(db.productDao())
        vm = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        // Observers
        vm.products.observe(viewLifecycleOwner) { list ->
            binding.progressHome.visibility = View.GONE
            adapter.submitList(list)
        }

        vm.totalFormatted.observe(viewLifecycleOwner) { total ->
            binding.tvTotalInventory.text = "Total inventario: $total"

            // --- NUEVO: Guardar total en SharedPreferences para el widget ---
            val prefs = requireContext().getSharedPreferences("inventory_widget_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("totalInventory", total).apply()
        }

        // FAB agregar producto
        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_addProductFragment)
        }
    }

    // Menú cerrar sesión
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                FirebaseAuth.getInstance().signOut()
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
