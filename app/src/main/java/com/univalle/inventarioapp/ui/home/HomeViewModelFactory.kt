package com.univalle.inventarioapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.univalle.inventarioapp.data.local.ProductDao

/**
 * @Deprecated Obsoleto: HomeViewModel ahora usa Dagger Hilt para inyección.
 * Este Factory ya no es necesario con @HiltViewModel
 */
@Deprecated("No longer needed with Hilt")
class HomeViewModelFactory(
    private val dao: ProductDao
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            throw IllegalStateException("HomeViewModel now uses Hilt injection. Use ViewModelProvider(fragment) instead.")
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
