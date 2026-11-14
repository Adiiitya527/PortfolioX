package com.portfoliox.data.market.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// DTOs (Data Transfer Objects) mirror the server's JSON exactly. They are kept separate from
// domain models so the API's shape (snake_case names, money as strings, ISO date strings) never
// leaks into the rest of the app. Mapping happens in DtoMappers.kt.
//
// @Serializable makes the compiler plugin generate the JSON parser for each class at build
// time, with no reflection at runtime. Web analogy: a zod schema plus its parser.
// @SerialName maps a JSON key to a differently named Kotlin property.

@Serializable
data class StockDto(
    val symbol: String,
    val name: String,
    val exchange: String,
    val sector: String,
)

/** Money arrives as strings ("231.45") so no precision is lost; mappers turn it into BigDecimal. */
@Serializable
data class QuoteDto(
    val symbol: String,
    val price: String,
    @SerialName("previous_close") val previousClose: String,
    val open: String,
    @SerialName("day_high") val dayHigh: String,
    @SerialName("day_low") val dayLow: String,
    /** ISO-8601, e.g. "2026-11-14T15:09:15.326577Z". */
    val timestamp: String,
)

@Serializable
data class PricePointDto(
    val timestamp: String,
    val close: String,
)

@Serializable
data class HistoryDto(
    val symbol: String,
    val range: String,
    val points: List<PricePointDto>,
)
