package com.portfoliox.data.market.remote

import com.portfoliox.data.market.remote.dto.PricePointDto
import com.portfoliox.data.market.remote.dto.QuoteDto
import com.portfoliox.data.market.remote.dto.StockDto
import com.portfoliox.domain.model.PricePoint
import com.portfoliox.domain.model.Quote
import com.portfoliox.domain.model.Stock
import com.portfoliox.domain.model.toMoney
import java.time.Instant
import java.time.OffsetDateTime

// DTO → domain conversions: the only place that knows the API's formats.
// Malformed data (e.g. a price of "abc") throws here, which surfaces as an error state,
// never as a silently wrong number on screen.

fun StockDto.toDomain(): Stock = Stock(symbol = symbol, name = name, exchange = exchange, sector = sector)

fun QuoteDto.toDomain(): Quote = Quote(
    symbol = symbol,
    price = price.toMoney(),
    previousClose = previousClose.toMoney(),
    open = open.toMoney(),
    dayHigh = dayHigh.toMoney(),
    dayLow = dayLow.toMoney(),
    timestamp = parseTimestamp(timestamp),
)

fun PricePointDto.toDomain(): PricePoint = PricePoint(timestamp = parseTimestamp(timestamp), close = close.toMoney())

/**
 * OffsetDateTime accepts both "…Z" and "…+00:00" offsets, plus fractional seconds,
 * so the parser doesn't depend on exactly how the server formats UTC.
 */
internal fun parseTimestamp(value: String): Instant = OffsetDateTime.parse(value).toInstant()
