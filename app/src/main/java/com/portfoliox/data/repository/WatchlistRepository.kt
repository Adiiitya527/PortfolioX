package com.portfoliox.data.repository

import com.portfoliox.data.local.dao.WatchlistDao
import com.portfoliox.data.local.entity.WatchlistEntity
import com.portfoliox.domain.model.WatchlistEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock

/** The user's watchlist, most recently added first. */
interface WatchlistRepository {
    fun observeEntries(): Flow<List<WatchlistEntry>>
    fun observeIsWatched(symbol: String): Flow<Boolean>

    /** Adds a stock now. Adding one that's already watched does nothing. */
    suspend fun add(symbol: String)

    suspend fun remove(symbol: String)

    /** Puts a removed entry back with its original date, so "undo" returns it to the same position. */
    suspend fun restore(entry: WatchlistEntry)
}

class WatchlistRepositoryImpl(
    private val watchlistDao: WatchlistDao,
    private val clock: Clock = Clock.systemUTC(),
) : WatchlistRepository {

    override fun observeEntries(): Flow<List<WatchlistEntry>> =
        watchlistDao.observeAll().map { entities -> entities.map { WatchlistEntry(it.symbol, it.addedAt) } }

    override fun observeIsWatched(symbol: String): Flow<Boolean> = watchlistDao.observeContains(symbol)

    override suspend fun add(symbol: String) {
        watchlistDao.insert(WatchlistEntity(symbol = symbol, addedAt = clock.instant()))
    }

    override suspend fun remove(symbol: String) {
        watchlistDao.delete(symbol)
    }

    override suspend fun restore(entry: WatchlistEntry) {
        watchlistDao.insert(WatchlistEntity(symbol = entry.symbol, addedAt = entry.addedAt))
    }
}
