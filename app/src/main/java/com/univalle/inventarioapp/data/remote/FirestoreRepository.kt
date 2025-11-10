package com.univalle.inventarioapp.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.univalle.inventarioapp.data.model.ProductEntity
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val db: FirebaseFirestore
) {
    private val products = db.collection("products")

    suspend fun upsertProduct(product: ProductEntity) {
        products.document(product.code).set(product).await()
    }
}
