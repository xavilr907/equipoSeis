package com.univalle.inventarioapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.univalle.inventarioapp.data.model.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ProductEntity)

    @Query("SELECT * FROM products WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): ProductEntity?

    @Query("DELETE FROM products WHERE code = :code")
    suspend fun deleteByCode(code: String)

    // Suma simple del precio (sin quantity por ahora)
    @Query("SELECT SUM(priceCents) FROM products")
    suspend fun getTotalInventoryValue(): Long?

    // ---------- NUEVO: leer todos los productos una sola vez (para widget) ----------
    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun observeAllOnce(): List<ProductEntity>
}
