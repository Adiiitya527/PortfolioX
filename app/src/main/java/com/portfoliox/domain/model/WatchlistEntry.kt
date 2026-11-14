package com.portfoliox.domain.model

import java.time.Instant

/** A stock on the user's watchlist. [addedAt] orders the list and lets "undo" restore an entry to its original place. */
data class WatchlistEntry(
    val symbol: String,
    val addedAt: Instant,
)
