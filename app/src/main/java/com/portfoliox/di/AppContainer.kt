package com.portfoliox.di

import android.content.Context
import com.portfoliox.data.local.PortfolioDatabase
import com.portfoliox.data.local.RoomTransactionRunner
import com.portfoliox.data.market.MarketDataSource
import com.portfoliox.data.market.sample.SampleMarketDataSource
import com.portfoliox.data.repository.MarketRepository
import com.portfoliox.data.repository.MarketRepositoryImpl
import com.portfoliox.data.repository.PortfolioRepository
import com.portfoliox.data.repository.PortfolioRepositoryImpl
import com.portfoliox.data.repository.WatchlistRepository
import com.portfoliox.data.repository.WatchlistRepositoryImpl

/**
 * Manual dependency injection: the one place that decides which implementation of each
 * interface the app uses, and creates each object once.
 *
 * Web analogy: the composition root of an Express app, where you create the DB client and
 * pass it into your services and routers.
 *
 * `by lazy` creates each object the first time it's used, then reuses it (a thread-safe singleton).
 * Hilt would generate this wiring from annotations; for an app this size, writing it by hand
 * keeps every dependency visible.
 */
class AppContainer(context: Context) {

    private val database: PortfolioDatabase by lazy { PortfolioDatabase.create(context) }

    // Swap this line for a Retrofit-based source in Phase 9. Nothing else changes.
    private val marketDataSource: MarketDataSource by lazy { SampleMarketDataSource() }

    val marketRepository: MarketRepository by lazy { MarketRepositoryImpl(marketDataSource) }

    val watchlistRepository: WatchlistRepository by lazy {
        WatchlistRepositoryImpl(database.watchlistDao())
    }

    val portfolioRepository: PortfolioRepository by lazy {
        PortfolioRepositoryImpl(
            accountDao = database.accountDao(),
            holdingDao = database.holdingDao(),
            transactionDao = database.transactionDao(),
            transactionRunner = RoomTransactionRunner(database),
        )
    }
}
