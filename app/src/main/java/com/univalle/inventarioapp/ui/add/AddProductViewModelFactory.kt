package com.univalle.inventarioapp.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.univalle.inventarioapp.data.local.ProductDao
import com.univalle.inventarioapp.data.remote.FirestoreRepository

class AddProductViewModelFactory(
    private val productDao: ProductDao,
    private val fsRepo: FirestoreRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddProductViewModel::class.java)) {
            return AddProductViewModel(productDao, fsRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
