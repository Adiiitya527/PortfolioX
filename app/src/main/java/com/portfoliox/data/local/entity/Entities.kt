package com.portfoliox.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.portfoliox.domain.model.TradeType
import java.math.BigDecimal
import java.time.Instant

// Entities describe database tables: each class is a table, each property a column.
// Web analogy: Mongoose schemas or SQLAlchemy models.
//
// Entities are kept separate from domain models (Holding, Transaction…) on purpose:
// the database shape can change (new columns, renamed tables) without touching the UI or
// business logic. Mappers in EntityMappers.kt convert between the two.

/** The simulated cash balance. Always a single row with id = 1. */
@Entity(tableName = "account")
data class AccountEntity(
    @PrimaryKey val id: Int = SINGLE_ACCOUNT_ID,
    val cashBalance: BigDecimal,
) {
    companion object {
        const val SINGLE_ACCOUNT_ID = 1
    }
}

/** A stock the user is watching. The symbol is the primary key, so a stock can't be added twice. */
@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val symbol: String,
    val addedAt: Instant,
)

/** Shares owned in one stock: one row per symbol. */
@Entity(tableName = "holdings")
data class HoldingEntity(
    @PrimaryKey val symbol: String,
    val quantity: Int,
    val averageCost: BigDecimal,
)

/**
 * One executed trade. `transaction` is an SQL keyword, hence the plural table name.
 * Indexes speed up the queries we run most: newest first, and filtering by symbol.
 * An id of 0 tells Room to generate the next id (autoGenerate).
 */
@Entity(
    tableName = "transactions",
    indices = [Index("symbol"), Index("timestamp")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val type: TradeType, // Room stores enums by name ("BUY" / "SELL")
    val quantity: Int,
    val price: BigDecimal,
    val total: BigDecimal,
    val realizedPnl: BigDecimal?,
    val timestamp: Instant,
)
