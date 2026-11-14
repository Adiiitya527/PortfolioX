package com.portfoliox.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.portfoliox.data.local.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO = Data Access Object: the queries for one table. We write an interface with SQL in
 * annotations, and Room generates the implementation at compile time. A typo in a column name
 * is a build error, not a runtime crash.
 *
 * Return types decide behaviour:
 * - `Flow<…>` re-emits automatically whenever the table changes (an observable query).
 * - `suspend fun` runs once, off the main thread.
 */
@Dao
interface WatchlistDao {

    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE symbol = :symbol)")
    fun observeContains(symbol: String): Flow<Boolean>

    /** IGNORE: adding a stock that's already in the watchlist is a no-op that keeps the original date. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun delete(symbol: String)
}
