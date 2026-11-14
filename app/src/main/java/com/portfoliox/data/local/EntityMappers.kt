package com.portfoliox.data.local

import com.portfoliox.data.local.entity.HoldingEntity
import com.portfoliox.data.local.entity.TransactionEntity
import com.portfoliox.domain.model.Holding
import com.portfoliox.domain.model.Transaction

// Conversions between database entities and domain models, written as extension functions
// so call sites read naturally: `entity.toDomain()`, `holding.toEntity()`.

fun HoldingEntity.toDomain(): Holding =
    Holding(symbol = symbol, quantity = quantity, averageCost = averageCost)

fun Holding.toEntity(): HoldingEntity =
    HoldingEntity(symbol = symbol, quantity = quantity, averageCost = averageCost)

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    symbol = symbol,
    type = type,
    quantity = quantity,
    price = price,
    total = total,
    realizedPnl = realizedPnl,
    timestamp = timestamp,
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    symbol = symbol,
    type = type,
    quantity = quantity,
    price = price,
    total = total,
    realizedPnl = realizedPnl,
    timestamp = timestamp,
)
