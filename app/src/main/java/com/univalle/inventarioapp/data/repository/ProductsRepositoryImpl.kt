package com.univalle.inventarioapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.univalle.inventarioapp.data.model.ProductEntity
import com.univalle.inventarioapp.data.model.Resource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProductsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
    // Si quieres, luego podemos inyectar también ProductDao para sincronizar con Room
) : ProductsRepository {

    private val collection = firestore.collection("products")

    override suspend fun getProductById(id: String): Resource<ProductEntity> {
        return try {
            val snapshot = collection.document(id).get().await()
            if (!snapshot.exists()) {
                return Resource.Error("Producto no encontrado")
            }

            val product = snapshot.toObject(ProductEntity::class.java)
                ?.apply { this.id = snapshot.id }

            if (product != null) {
                Resource.Success(product)
            } else {
                Resource.Error("Error al mapear el producto")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error obteniendo el producto")
        }
    }

    override suspend fun deleteProduct(id: String): Resource<Unit> {
        return try {
            collection.document(id).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error eliminando el producto")
        }
    }
}
