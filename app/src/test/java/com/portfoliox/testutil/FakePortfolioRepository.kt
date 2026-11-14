package com.portfoliox.testutil

import com.portfoliox.data.repository.PortfolioRepository
import com.portfoliox.data.repository.TradeResult
import com.portfoliox.domain.model.Holding
import com.portfoliox.domain.model.TradeOrder
import com.portfoliox.domain.model.Transaction
import com.portfoliox.domain.model.toMoney
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal

/**
 * A controllable PortfolioRepository for ViewModel tests. Tests push values into the public
 * MutableStateFlows, or set [failure] to make every observed flow throw.
 */
class FakePortfolioRepository : PortfolioRepository {

    val cash = MutableStateFlow("100000.00".toMoney())
    val holdings = MutableStateFlow<List<Holding>>(emptyList())
    val transactions = MutableStateFlow<List<Transaction>>(emptyList())

    /** When set, observe* flows throw this instead of emitting. */
    var failure: Throwable? = null

    /** What executeTrade returns. Tests set it before triggering a trade. */
    var nextTradeResult: TradeResult? = null
    val executedOrders = mutableListOf<TradeOrder>()

    override fun observeCash(): Flow<BigDecimal> = failable(cash)
    override fun observeHoldings(): Flow<List<Holding>> = failable(holdings)
    override fun observeHolding(symbol: String): Flow<Holding?> =
        failable(holdings).let { flow -> flow { flow.collect { list -> emit(list.firstOrNull { it.symbol == symbol }) } } }
    override fun observeTransactions(): Flow<List<Transaction>> = failable(transactions)

    override suspend fun executeTrade(order: TradeOrder): TradeResult {
        executedOrders += order
        return checkNotNull(nextTradeResult) { "Set nextTradeResult before executing a trade" }
    }

    private fun <T> failable(source: Flow<T>): Flow<T> = flow {
        failure?.let { throw it }
        emitAll(source)
    }
}
