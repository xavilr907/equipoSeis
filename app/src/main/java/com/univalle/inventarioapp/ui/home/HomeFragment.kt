package com.univalle.inventarioapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.room.Room
import com.univalle.inventarioapp.R
import com.univalle.inventarioapp.data.local.AppDatabase
import com.univalle.inventarioapp.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var vm: HomeViewModel
    // pass click lambda to adapter
    private val adapter by lazy { ProductAdapter { code ->
        val bundle = Bundle().apply { putString("productCode", code) }
        findNavController().navigate(R.id.action_homeFragment_to_productDetailFragment, bundle)
    } }

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

        binding.rvProducts.adapter = adapter
        binding.rvProducts.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        // Room DB
        val db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java,
            "inventario.db"
        ).fallbackToDestructiveMigration().build()

        val factory = HomeViewModelFactory(db.productDao())
        vm = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        // Observa los datos y actualiza la lista
        vm.products.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }

        // Observa el total formateado y lo muestra en el widget
        vm.totalFormatted.observe(viewLifecycleOwner) { total ->
            binding.tvTotalInventory.text = "Total inventario: $total"
        }

        // Navegar a AddProductFragment
        binding.btnAdd.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_addProductFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
