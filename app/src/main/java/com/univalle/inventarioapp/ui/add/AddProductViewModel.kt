package com.univalle.inventarioapp.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.univalle.inventarioapp.data.local.ProductDao
import com.univalle.inventarioapp.data.model.ProductEntity
import com.univalle.inventarioapp.data.remote.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddProductViewModel @Inject constructor(
    private val productDao: ProductDao,
    private val fsRepo: FirestoreRepository
) : ViewModel() {

    fun upsert(product: ProductEntity, onDone: () -> Unit, onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            try {
                productDao.upsert(product)
                try { fsRepo.upsertProduct(product) } catch (e: Throwable) { onError(e) }
                onDone()
            } catch (e: Throwable) { onError(e) }
        }
    }
}