package com.portfoliox.testutil

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import java.math.BigDecimal

/**
 * Compares money by numeric value. BigDecimal.equals also compares scale,
 * so BigDecimal("150.0") != BigDecimal("150.00"), even though they are the same amount.
 * compareTo ignores scale, which is what tests usually mean.
 */
fun assertMoney(expected: String, actual: BigDecimal?) {
    assertNotNull("Expected $expected but was null", actual)
    assertTrue("Expected $expected but was $actual", BigDecimal(expected).compareTo(actual) == 0)
}
