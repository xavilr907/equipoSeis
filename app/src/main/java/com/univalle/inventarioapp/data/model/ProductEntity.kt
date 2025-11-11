// com.univalle.inventarioapp.data.model.ProductEntity
package com.univalle.inventarioapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val code: String = "",
    val name: String = "",
    val priceCents: Long = 0L,
    val quantity: Int = 0
)
