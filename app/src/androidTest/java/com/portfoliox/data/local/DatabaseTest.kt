package com.portfoliox.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.portfoliox.data.local.entity.HoldingEntity
import com.portfoliox.data.local.entity.WatchlistEntity
import com.portfoliox.data.repository.PortfolioRepositoryImpl
import com.portfoliox.data.repository.TradeResult
import com.portfoliox.domain.model.TradeOrder
import com.portfoliox.domain.model.TradeType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant

/**
 * Instrumented tests: they run on an emulator or device against real SQLite, so they check
 * what fakes can't: the SQL itself, the type converters, and transaction rollback.
 * The database lives in memory and is discarded after each test.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var database: PortfolioDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder<PortfolioDatabase>(ApplicationProvider.getApplicationContext()).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    // ---- WatchlistDao ----

    @Test
    fun watchlistIsNewestFirstAndIgnoresDuplicates() = runTest {
        val dao = database.watchlistDao()
        dao.insert(WatchlistEntity("AAPL", Instant.ofEpochMilli(1_000)))
        dao.insert(WatchlistEntity("MSFT", Instant.ofEpochMilli(2_000)))
        dao.insert(WatchlistEntity("AAPL", Instant.ofEpochMilli(3_000))) // duplicate: ignored

        val symbols = dao.observeAll().first().map { it.symbol }

        assertEquals(listOf("MSFT", "AAPL"), symbols)
    }

    @Test
    fun watchlistContainsReflectsAddAndRemove() = runTest {
        val dao = database.watchlistDao()
        dao.insert(WatchlistEntity("AAPL", Instant.EPOCH))
        assertTrue(dao.observeContains("AAPL").first())

        dao.delete("AAPL")

        assertFalse(dao.observeContains("AAPL").first())
    }

    // ---- HoldingDao + converters ----

    @Test
    fun holdingKeepsExactDecimalValue() = runTest {
        val dao = database.holdingDao()
        dao.upsert(HoldingEntity("AAPL", quantity = 3, averageCost = BigDecimal("100.6667")))

        assertEquals(BigDecimal("100.6667"), dao.get("AAPL")?.averageCost)
    }

    // ---- Transactions ----

    @Test
    fun failedTransactionRollsBackEveryWrite() = runTest {
        val runner = RoomTransactionRunner(database)

        val result = runCatching {
            runner.run {
                database.holdingDao().upsert(HoldingEntity("AAPL", 10, BigDecimal("150")))
                error("Simulated crash halfway through a trade")
            }
        }

        assertTrue(result.isFailure)
        assertNull(database.holdingDao().get("AAPL")) // the upsert was undone
    }

    @Test
    fun repositoryTradeRoundTripAgainstRealDatabase() = runTest {
        val repository = PortfolioRepositoryImpl(
            accountDao = database.accountDao(),
            holdingDao = database.holdingDao(),
            transactionDao = database.transactionDao(),
            transactionRunner = RoomTransactionRunner(database),
        )

        val bought = repository.executeTrade(TradeOrder("AAPL", TradeType.BUY, 10, BigDecimal("150.00")))
        val sold = repository.executeTrade(TradeOrder("AAPL", TradeType.SELL, 10, BigDecimal("160.00")))

        assertTrue(bought is TradeResult.Success)
        assertEquals(BigDecimal("100.00"), (sold as TradeResult.Success).transaction.realizedPnl)
        assertNull(repository.observeHolding("AAPL").first())
        assertEquals(BigDecimal("100100.00"), repository.observeCash().first())
        assertEquals(listOf(TradeType.SELL, TradeType.BUY), repository.observeTransactions().first().map { it.type })
    }
}
