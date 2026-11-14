package com.portfoliox.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.portfoliox.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    /** Newest first. `id DESC` breaks ties between trades made in the same millisecond. */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    /** Returns the generated id of the new row. */
    @Insert
    suspend fun insert(entity: TransactionEntity): Long
}
