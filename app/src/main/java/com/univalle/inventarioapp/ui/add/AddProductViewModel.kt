package com.univalle.inventarioapp.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.univalle.inventarioapp.data.local.ProductDao
import com.univalle.inventarioapp.data.model.ProductEntity
import com.univalle.inventarioapp.data.remote.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel encargado de manejar la lógica de agregar o actualizar
 * productos tanto en la base de datos local (Room) como en Firestore.
 *
 * Funciones principales:
 * - upsert(product): actualiza o inserta un producto.
 * - Sincroniza los datos localmente y en la nube.
 *
 * @property productDao DAO para operaciones con la base de datos Room.
 * @property fsRepo Repositorio para operaciones en Firestore.
 */
@HiltViewModel
class AddProductViewModel @Inject constructor(
    private val productDao: ProductDao,
    private val fsRepo: FirestoreRepository
) : ViewModel() {

    /**
     * Inserta o actualiza un producto localmente y en Firestore.
     *
     * Flujo:
     * 1. Guarda el producto en Room mediante `productDao.upsert`.
     * 2. Intenta sincronizar con Firestore mediante `fsRepo.upsertProduct`.
     * 3. Si Room falla → se envía error inmediatamente.
     * 4. Si Firestore falla → se llama `onError`, pero Room ya quedó actualizado.
     * 5. Si todo va bien → se ejecuta `onDone`.
     *
     * @param product entidad del producto a registrar.
     * @param onDone callback ejecutado cuando ambas operaciones terminan correctamente.
     * @param onError callback ejecutado si ocurre algún error en Room o Firestore.
     */
    fun upsert(product: ProductEntity, onDone: () -> Unit, onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            try {
                // Guardar localmente
                productDao.upsert(product)

                // Intentar sincronizar en Firestore
                try {
                    fsRepo.upsertProduct(product)
                } catch (e: Throwable) {
                    onError(e)
                }

                // Finalizó todo correctamente
                onDone()
            } catch (e: Throwable) {
                // Error en Room
                onError(e)
            }
        }
    }
}
