package com.univalle.inventarioapp.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.univalle.inventarioapp.data.model.ProductEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    private val products = db.collection("products")

    /**
     * Inserta o actualiza un producto en Firestore
     */
    suspend fun upsertProduct(product: ProductEntity) {
        products.document(product.code).set(product).await()
    }

    /**
     * Observa los productos en tiempo real desde Firestore
     * Retorna un Flow que emite la lista actualizada cada vez que hay cambios
     */
    fun observeProducts(): Flow<List<ProductEntity>> = callbackFlow {
        val listener = products.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ProductEntity::class.java)?.apply {
                    id = doc.id
                }
            } ?: emptyList()

            trySend(list).isSuccess
        }

        awaitClose { listener.remove() }
    }

    /**
     * Obtiene un producto específico por código
     */
    suspend fun getProductByCode(code: String): ProductEntity? {
        return try {
            products.document(code).get().await().toObject(ProductEntity::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Elimina un producto por código
     */
    suspend fun deleteProduct(code: String) {
        products.document(code).delete().await()
    }
}
