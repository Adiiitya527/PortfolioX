package com.portfoliox.data.local

import androidx.room.withTransaction

/**
 * Runs a block of database work atomically: either every write in [block] is saved, or,
 * if anything throws, none are. Web analogy: a MongoDB session or SQL BEGIN … COMMIT/ROLLBACK.
 *
 * Why an interface instead of calling Room directly in the repository: unit tests can pass a
 * simple runner that just calls the block, so repository logic is testable without a database.
 */
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

class RoomTransactionRunner(private val database: PortfolioDatabase) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = database.withTransaction(block)
}
