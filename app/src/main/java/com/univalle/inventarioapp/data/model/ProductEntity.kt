package com.univalle.inventarioapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId

@Entity(tableName = "products")
data class ProductEntity(
    @DocumentId var id: String? = null,          // Firestore docId
    @PrimaryKey var code: String = "",           // PK de Room y campo normal en Firestore
    var name: String = "",
    var priceCents: Long = 0L,
    var quantity: Int = 0
)
