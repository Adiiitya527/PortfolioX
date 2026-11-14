package com.portfoliox.ui.stockdetail

import com.portfoliox.domain.model.ChartRange
import com.portfoliox.domain.model.PricePoint
import com.portfoliox.domain.model.percentOf
import com.portfoliox.domain.model.toMoney
import com.portfoliox.ui.common.UiState
import java.math.BigDecimal

/**
 * The chart's own state, separate from the header, so changing the range reloads only the chart.
 */
data class ChartUiState(
    val range: ChartRange,
    val points: UiState<List<PricePoint>>,
)

/** How much the price moved between the first and last point of a chart. */
data class RangeChange(
    val change: BigDecimal,
    val changePercent: BigDecimal,
) {
    companion object {
        /** Null when there aren't at least two points to compare. */
        fun of(points: List<PricePoint>): RangeChange? {
            if (points.size < 2) return null
            val first = points.first().close
            val last = points.last().close
            return RangeChange(change = (last - first).toMoney(), changePercent = percentOf(last - first, first))
        }
    }
}

/**
 * Converts prices into vertical positions between 0 (lowest) and 1 (highest), the shape
 * the chart draws. Kept as a pure function so the maths is unit tested separately from drawing.
 *
 * A flat line (all prices equal) would divide by zero, so it's placed in the middle instead.
 */
fun normalizeForChart(values: List<Float>): List<Float> {
    if (values.isEmpty()) return emptyList()
    val min = values.min()
    val max = values.max()
    val span = max - min
    if (span == 0f) return values.map { 0.5f }
    return values.map { (it - min) / span }
}

/** Which point a touch at horizontal position [x] (0..[width]) refers to, for chart scrubbing. */
fun pointIndexAt(x: Float, width: Float, pointCount: Int): Int {
    if (pointCount <= 1 || width <= 0f) return 0
    val fraction = (x / width).coerceIn(0f, 1f)
    return Math.round(fraction * (pointCount - 1))
}
