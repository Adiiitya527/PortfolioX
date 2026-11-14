package com.portfoliox.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.portfoliox.data.local.entity.HoldingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HoldingDao {

    @Query("SELECT * FROM holdings ORDER BY symbol")
    fun observeAll(): Flow<List<HoldingEntity>>

    /** Emits null while the user doesn't own the stock. */
    @Query("SELECT * FROM holdings WHERE symbol = :symbol")
    fun observe(symbol: String): Flow<HoldingEntity?>

    @Query("SELECT * FROM holdings WHERE symbol = :symbol")
    suspend fun get(symbol: String): HoldingEntity?

    /** Upsert = insert, or update if a row with the same primary key exists. */
    @Upsert
    suspend fun upsert(entity: HoldingEntity)

    @Query("DELETE FROM holdings WHERE symbol = :symbol")
    suspend fun delete(symbol: String)

    @Query("DELETE FROM holdings")
    suspend fun deleteAll()
}
