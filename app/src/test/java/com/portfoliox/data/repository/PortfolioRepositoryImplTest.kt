package com.portfoliox.data.repository

import com.portfoliox.domain.model.TradeError
import com.portfoliox.domain.model.TradeOrder
import com.portfoliox.domain.model.TradeType
import com.portfoliox.domain.model.toMoney
import com.portfoliox.testutil.FakeAccountDao
import com.portfoliox.testutil.FakeHoldingDao
import com.portfoliox.testutil.FakeTransactionDao
import com.portfoliox.testutil.ImmediateTransactionRunner
import com.portfoliox.testutil.assertMoney
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class PortfolioRepositoryImplTest {

    private val now = Instant.parse("2026-11-14T10:00:00Z")

    // A fresh repository for every test: JUnit creates a new instance of the class per @Test.
    private val repository = PortfolioRepositoryImpl(
        accountDao = FakeAccountDao(),
        holdingDao = FakeHoldingDao(),
        transactionDao = FakeTransactionDao(),
        transactionRunner = ImmediateTransactionRunner(),
        clock = Clock.fixed(now, ZoneOffset.UTC),
        startingCash = "10000.00".toMoney(),
    )

    private fun buy(symbol: String, quantity: Int, price: String) =
        TradeOrder(symbol, TradeType.BUY, quantity, price.toMoney())

    private fun sell(symbol: String, quantity: Int, price: String) =
        TradeOrder(symbol, TradeType.SELL, quantity, price.toMoney())

    @Test
    fun `new portfolio starts with the starting cash and nothing else`() = runTest {
        assertMoney("10000.00", repository.observeCash().first())
        assertTrue(repository.observeHoldings().first().isEmpty())
        assertTrue(repository.observeTransactions().first().isEmpty())
    }

    @Test
    fun `buy spends cash, creates a holding and records the transaction`() = runTest {
        val result = repository.executeTrade(buy("AAPL", 10, "150.00"))

        assertTrue(result is TradeResult.Success)
        assertMoney("8500.00", repository.observeCash().first())

        val holding = repository.observeHolding("AAPL").first()
        assertEquals(10, holding?.quantity)
        assertMoney("150", holding?.averageCost)

        val transaction = repository.observeTransactions().first().single()
        assertEquals(TradeType.BUY, transaction.type)
        assertMoney("1500.00", transaction.total)
        assertNull(transaction.realizedPnl)
        assertEquals(now, transaction.timestamp)
    }

    @Test
    fun `partial sell adds cash back and records realized profit`() = runTest {
        repository.executeTrade(buy("AAPL", 10, "150.00"))

        val result = repository.executeTrade(sell("AAPL", 4, "175.00"))

        val transaction = (result as TradeResult.Success).transaction
        assertMoney("100.00", transaction.realizedPnl) // (175 − 150) × 4
        assertMoney("9200.00", repository.observeCash().first()) // 10000 − 1500 + 700
        assertEquals(6, repository.observeHolding("AAPL").first()?.quantity)
    }

    @Test
    fun `selling every share removes the holding`() = runTest {
        repository.executeTrade(buy("AAPL", 10, "150.00"))

        repository.executeTrade(sell("AAPL", 10, "140.00"))

        assertNull(repository.observeHolding("AAPL").first())
        assertMoney("9900.00", repository.observeCash().first()) // lost 100
        assertEquals(2, repository.observeTransactions().first().size)
    }

    @Test
    fun `rejected buy changes nothing`() = runTest {
        val result = repository.executeTrade(buy("AAPL", 100, "150.00")) // 15 000 > 10 000

        assertTrue((result as TradeResult.Rejected).error is TradeError.InsufficientCash)
        assertMoney("10000.00", repository.observeCash().first())
        assertTrue(repository.observeHoldings().first().isEmpty())
        assertTrue(repository.observeTransactions().first().isEmpty())
    }

    @Test
    fun `selling a stock that isn't owned is rejected`() = runTest {
        val result = repository.executeTrade(sell("MSFT", 1, "400.00"))

        assertEquals(TradeResult.Rejected(TradeError.InsufficientShares(requested = 1, owned = 0)), result)
    }

    @Test
    fun `transaction ids are returned to the caller`() = runTest {
        val first = repository.executeTrade(buy("AAPL", 1, "150.00")) as TradeResult.Success
        val second = repository.executeTrade(buy("MSFT", 1, "400.00")) as TradeResult.Success

        assertEquals(1L, first.transaction.id)
        assertEquals(2L, second.transaction.id)
    }
}
