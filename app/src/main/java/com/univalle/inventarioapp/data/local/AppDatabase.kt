package com.univalle.inventarioapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.univalle.inventarioapp.data.model.ProductEntity   // ← IMPORT CORRECTO

@Database(
    entities = [ProductEntity::class],
    version = 4, // Incrementada porque la estructura (schema) cambió
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}
