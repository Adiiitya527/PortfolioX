package com.portfoliox.data.local

import androidx.room.TypeConverter
import java.math.BigDecimal
import java.time.Instant

/**
 * SQLite only stores INTEGER, REAL, TEXT and BLOB. Type converters teach Room how to store
 * other types.
 *
 * - BigDecimal → TEXT ("1234.50"). Not REAL, because REAL is floating point and would lose the
 *   exactness we chose BigDecimal for. The trade-off: SQL can't SUM these columns, so totals
 *   are calculated in Kotlin (PortfolioCalculator), which we do anyway.
 * - Instant → INTEGER (epoch milliseconds), which sorts correctly and is compact.
 */
class Converters {

    @TypeConverter
    fun bigDecimalToString(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun stringToBigDecimal(value: String?): BigDecimal? = value?.let(::BigDecimal)

    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}
