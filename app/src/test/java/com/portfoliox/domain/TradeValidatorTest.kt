package com.portfoliox.domain

import com.portfoliox.domain.model.TradeError
import com.portfoliox.domain.model.TradeOrder
import com.portfoliox.domain.model.TradeType
import com.portfoliox.domain.model.toMoney
import com.portfoliox.testutil.assertMoney
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TradeValidatorTest {

    private fun order(type: TradeType, quantity: Int, price: String) =
        TradeOrder(symbol = "AAPL", type = type, quantity = quantity, price = price.toMoney())

    @Test
    fun `buy costing exactly the available cash is allowed`() {
        val error = TradeValidator.validate(order(TradeType.BUY, 10, "100.00"), availableCash = "1000".toMoney(), ownedQuantity = 0)

        assertNull(error)
    }

    @Test
    fun `buy costing more than available cash is rejected with the amounts`() {
        val error = TradeValidator.validate(order(TradeType.BUY, 10, "100.01"), availableCash = "1000".toMoney(), ownedQuantity = 0)

        // `as` casts to the expected type. If it's any other type, the cast throws and the test fails.
        error as TradeError.InsufficientCash
        // After a successful `as`, the compiler smart-casts `error`, so its fields are accessible.
        assertMoney("1000.10", error.required)
        assertMoney("1000.00", error.available)
    }

    @Test
    fun `zero or negative quantity is rejected`() {
        assertEquals(TradeError.InvalidQuantity, TradeValidator.validate(order(TradeType.BUY, 0, "100"), "1000".toMoney(), 0))
        assertEquals(TradeError.InvalidQuantity, TradeValidator.validate(order(TradeType.SELL, -1, "100"), "1000".toMoney(), 5))
    }

    @Test
    fun `zero price is rejected as unavailable`() {
        val error = TradeValidator.validate(order(TradeType.BUY, 1, "0"), availableCash = "1000".toMoney(), ownedQuantity = 0)

        assertEquals(TradeError.PriceUnavailable, error)
    }

    @Test
    fun `selling more shares than owned is rejected`() {
        val error = TradeValidator.validate(order(TradeType.SELL, 6, "100"), availableCash = "0".toMoney(), ownedQuantity = 5)

        assertEquals(TradeError.InsufficientShares(requested = 6, owned = 5), error)
    }

    @Test
    fun `selling every owned share is allowed and does not need cash`() {
        val error = TradeValidator.validate(order(TradeType.SELL, 5, "100"), availableCash = "0".toMoney(), ownedQuantity = 5)

        assertNull(error)
    }
}
