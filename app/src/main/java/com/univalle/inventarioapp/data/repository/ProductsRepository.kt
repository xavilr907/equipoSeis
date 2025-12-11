package com.univalle.inventarioapp.data.repository

import com.univalle.inventarioapp.data.model.ProductEntity
import com.univalle.inventarioapp.data.model.Resource

interface ProductsRepository {
    suspend fun getProductById(id: String): Resource<ProductEntity>
    suspend fun deleteProduct(id: String): Resource<Unit>
}
