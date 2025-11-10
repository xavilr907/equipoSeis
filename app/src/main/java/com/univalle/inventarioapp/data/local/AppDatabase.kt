package com.univalle.inventarioapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.univalle.inventarioapp.data.model.ProductEntity

@Database(
    entities = [ProductEntity::class],
    version = 3,          // ← súbelo (antes 2) porque cambió el tipo de columna
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}
