package com.portfoliox.data.repository

import com.portfoliox.data.local.TransactionRunner
import com.portfoliox.data.local.dao.AccountDao
import com.portfoliox.data.local.dao.HoldingDao
import com.portfoliox.data.local.dao.TransactionDao
import com.portfoliox.data.local.entity.AccountEntity
import com.portfoliox.data.local.toDomain
import com.portfoliox.data.local.toEntity
import com.portfoliox.domain.PortfolioCalculator
import com.portfoliox.domain.TradeValidator
import com.portfoliox.domain.model.Holding
import com.portfoliox.domain.model.TradeError
import com.portfoliox.domain.model.TradeOrder
import com.portfoliox.domain.model.TradeType
import com.portfoliox.domain.model.Transaction
import com.portfoliox.domain.model.toMoney
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Clock

/** Outcome of placing a trade. Rejection is an expected result, not an exception. */
sealed interface TradeResult {
    data class Success(val transaction: Transaction) : TradeResult
    data class Rejected(val error: TradeError) : TradeResult
}

/** The user's money and positions: cash, holdings, trade history, and placing trades. */
interface PortfolioRepository {
    fun observeCash(): Flow<BigDecimal>
    fun observeHoldings(): Flow<List<Holding>>
    fun observeHolding(symbol: String): Flow<Holding?>
    fun observeTransactions(): Flow<List<Transaction>>
    suspend fun executeTrade(order: TradeOrder): TradeResult

    /** Back to a fresh account: starting cash, no holdings, no history. The watchlist is kept. */
    suspend fun resetPortfolio()
}

/**
 * @param startingCash the simulated balance before any trade. There's no account row until
 * the first trade; until then the starting cash is reported. This avoids seeding the database.
 */
class PortfolioRepositoryImpl(
    private val accountDao: AccountDao,
    private val holdingDao: HoldingDao,
    private val transactionDao: TransactionDao,
    private val transactionRunner: TransactionRunner,
    private val clock: Clock = Clock.systemUTC(),
    private val startingCash: BigDecimal = DEFAULT_STARTING_CASH,
) : PortfolioRepository {

    override fun observeCash(): Flow<BigDecimal> =
        accountDao.observeCash().map { it ?: startingCash }

    override fun observeHoldings(): Flow<List<Holding>> =
        holdingDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeHolding(symbol: String): Flow<Holding?> =
        holdingDao.observe(symbol).map { it?.toDomain() }

    override fun observeTransactions(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    /**
     * Validate → update holding → update cash → record transaction, all in one database
     * transaction. Reading the balance inside the transaction matters: two quick taps on "Buy"
     * can't both spend the same cash, because the second runs after the first has committed.
     */
    override suspend fun executeTrade(order: TradeOrder): TradeResult =
        transactionRunner.run { executeTradeAtomically(order) }

    private suspend fun executeTradeAtomically(order: TradeOrder): TradeResult {
        val cash = accountDao.getCash() ?: startingCash
        val holding = holdingDao.get(order.symbol)?.toDomain()

        TradeValidator.validate(order, availableCash = cash, ownedQuantity = holding?.quantity ?: 0)
            ?.let { error -> return TradeResult.Rejected(error) }

        val realizedPnl: BigDecimal?
        val newCash: BigDecimal

        when (order.type) {
            TradeType.BUY -> {
                val updated = PortfolioCalculator.applyBuy(holding, order.symbol, order.quantity, order.price)
                holdingDao.upsert(updated.toEntity())
                newCash = cash - order.total
                realizedPnl = null
            }
            TradeType.SELL -> {
                // The validator guaranteed we own enough shares, so `holding` is non-null here.
                // `checkNotNull` documents that assumption and fails loudly if it's ever wrong.
                val sell = PortfolioCalculator.applySell(checkNotNull(holding), order.quantity, order.price)
                val remaining = sell.remaining
                if (remaining == null) holdingDao.delete(order.symbol) else holdingDao.upsert(remaining.toEntity())
                newCash = cash + order.total
                realizedPnl = sell.realizedPnl
            }
        }

        accountDao.upsert(AccountEntity(cashBalance = newCash.toMoney()))

        val transaction = Transaction(
            id = 0,
            symbol = order.symbol,
            type = order.type,
            quantity = order.quantity,
            price = order.price,
            total = order.total,
            realizedPnl = realizedPnl,
            timestamp = clock.instant(),
        )
        val id = transactionDao.insert(transaction.toEntity())
        return TradeResult.Success(transaction.copy(id = id))
    }

    /** All three deletes in one transaction: the account is never seen half-reset. */
    override suspend fun resetPortfolio() {
        transactionRunner.run {
            holdingDao.deleteAll()
            transactionDao.deleteAll()
            accountDao.deleteAll()
        }
    }

    companion object {
        val DEFAULT_STARTING_CASH: BigDecimal = "100000.00".toMoney()
    }
}
