package com.portfoliox.testutil

import com.portfoliox.data.local.TransactionRunner
import com.portfoliox.data.local.dao.AccountDao
import com.portfoliox.data.local.dao.HoldingDao
import com.portfoliox.data.local.dao.TransactionDao
import com.portfoliox.data.local.entity.AccountEntity
import com.portfoliox.data.local.entity.HoldingEntity
import com.portfoliox.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

// Fakes: small working implementations of the DAO interfaces that keep data in memory.
// Unlike mocks, they behave like the real thing, so tests check outcomes ("cash is now X")
// instead of interactions ("upsert was called once").
//
// MutableStateFlow holds the current value and re-emits on every change, just like a Room
// query Flow re-emits when its table changes.

class FakeAccountDao : AccountDao {
    private val cash = MutableStateFlow<BigDecimal?>(null)

    override fun observeCash(): Flow<BigDecimal?> = cash
    override suspend fun getCash(): BigDecimal? = cash.value
    override suspend fun upsert(entity: AccountEntity) {
        cash.value = entity.cashBalance
    }

    override suspend fun deleteAll() {
        cash.value = null
    }
}

class FakeHoldingDao : HoldingDao {
    private val rows = MutableStateFlow<Map<String, HoldingEntity>>(emptyMap())

    override fun observeAll(): Flow<List<HoldingEntity>> = rows.map { it.values.sortedBy(HoldingEntity::symbol) }
    override fun observe(symbol: String): Flow<HoldingEntity?> = rows.map { it[symbol] }
    override suspend fun get(symbol: String): HoldingEntity? = rows.value[symbol]
    override suspend fun upsert(entity: HoldingEntity) = rows.update { it + (entity.symbol to entity) }
    override suspend fun delete(symbol: String) = rows.update { it - symbol }
    override suspend fun deleteAll() = rows.update { emptyMap() }
}

class FakeTransactionDao : TransactionDao {
    private val rows = MutableStateFlow<List<TransactionEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<TransactionEntity>> =
        rows.map { list -> list.sortedWith(compareByDescending<TransactionEntity> { it.timestamp }.thenByDescending { it.id }) }

    override suspend fun insert(entity: TransactionEntity): Long {
        val id = nextId++
        rows.update { it + entity.copy(id = id) }
        return id
    }

    override suspend fun deleteAll() = rows.update { emptyList() }
}

/** Runs the block directly. Atomicity itself is tested against real Room in androidTest. */
class ImmediateTransactionRunner : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}
