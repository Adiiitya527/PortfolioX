package com.portfoliox.data.local

import com.portfoliox.data.local.entity.HoldingEntity
import com.portfoliox.domain.model.TradeType
import com.portfoliox.domain.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class ConvertersAndMappersTest {

    private val converters = Converters()

    @Test
    fun `BigDecimal survives a round trip exactly, including scale`() {
        val value = BigDecimal("1234.5000")

        val stored = converters.bigDecimalToString(value)

        assertEquals("1234.5000", stored)
        assertEquals(value, converters.stringToBigDecimal(stored))
    }

    @Test
    fun `large values are stored without scientific notation`() {
        // BigDecimal.toString() could give "1E+7"; toPlainString() always gives digits.
        assertEquals("10000000", converters.bigDecimalToString(BigDecimal("1E+7")))
    }

    @Test
    fun `Instant is stored as epoch milliseconds`() {
        val instant = Instant.parse("2026-11-14T10:00:00Z")

        val stored = converters.instantToEpochMillis(instant)

        assertEquals(instant.toEpochMilli(), stored)
        assertEquals(instant, converters.epochMillisToInstant(stored))
    }

    @Test
    fun `null values stay null`() {
        assertNull(converters.bigDecimalToString(null))
        assertNull(converters.stringToBigDecimal(null))
    }

    @Test
    fun `holding and transaction map to entities and back unchanged`() {
        val holdingEntity = HoldingEntity(symbol = "AAPL", quantity = 3, averageCost = BigDecimal("100.6667"))
        assertEquals(holdingEntity, holdingEntity.toDomain().toEntity())

        val transaction = Transaction(
            id = 7,
            symbol = "AAPL",
            type = TradeType.SELL,
            quantity = 2,
            price = BigDecimal("175.00"),
            total = BigDecimal("350.00"),
            realizedPnl = BigDecimal("50.00"),
            timestamp = Instant.parse("2026-11-14T10:00:00Z"),
        )
        assertEquals(transaction, transaction.toEntity().toDomain())
    }
}
