package com.portfoliox.data.repository

import com.portfoliox.data.local.dao.WatchlistDao
import com.portfoliox.data.local.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock

/** The user's watchlist: which symbols they follow, most recently added first. */
interface WatchlistRepository {
    fun observeSymbols(): Flow<List<String>>
    fun observeIsWatched(symbol: String): Flow<Boolean>
    suspend fun add(symbol: String)
    suspend fun remove(symbol: String)
}

class WatchlistRepositoryImpl(
    private val watchlistDao: WatchlistDao,
    private val clock: Clock = Clock.systemUTC(),
) : WatchlistRepository {

    // `map` transforms each emitted list, like Array.map inside an RxJS map operator.
    override fun observeSymbols(): Flow<List<String>> =
        watchlistDao.observeAll().map { entities -> entities.map { it.symbol } }

    override fun observeIsWatched(symbol: String): Flow<Boolean> = watchlistDao.observeContains(symbol)

    override suspend fun add(symbol: String) {
        watchlistDao.insert(WatchlistEntity(symbol = symbol, addedAt = clock.instant()))
    }

    override suspend fun remove(symbol: String) {
        watchlistDao.delete(symbol)
    }
}
