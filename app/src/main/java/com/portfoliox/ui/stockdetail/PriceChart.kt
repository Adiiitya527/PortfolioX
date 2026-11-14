package com.portfoliox.ui.stockdetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.portfoliox.domain.model.PricePoint
import com.portfoliox.ui.theme.PortfolioXTheme
import java.math.BigDecimal
import java.time.Instant

/**
 * A line chart drawn by hand on a Canvas. No chart library: every pixel is explained here.
 *
 * Canvas gives a DrawScope where `size` is the drawing area in pixels and (0, 0) is the
 * top-left corner, so y grows downwards. Web analogy: the HTML <canvas> 2D context.
 *
 * Touch: press and drag horizontally to select a point. The selection is reported through
 * [onSelectedIndexChange] (null when the finger lifts), and the caller shows that point's
 * price and date.
 */
@Composable
fun PriceChart(
    points: List<PricePoint>,
    lineColor: Color,
    selectedIndex: Int?,
    onSelectedIndexChange: (Int?) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    // remember(points): recompute the 0..1 positions only when the data changes, not on every frame.
    val fractions = remember(points) { normalizeForChart(points.map { it.close.toFloat() }) }
    val baselineColor = MaterialTheme.colorScheme.outlineVariant
    val cursorColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    // The gesture block below is started once per `points` value. rememberUpdatedState makes it
    // always call the latest callback, even if the caller passes a new lambda later.
    val currentOnSelect by rememberUpdatedState(onSelectedIndexChange)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(points) {
                fun indexAt(x: Float) = pointIndexAt(x, size.width.toFloat(), points.size)
                detectHorizontalDragGestures(
                    onDragStart = { offset -> currentOnSelect(indexAt(offset.x)) },
                    onDragEnd = { currentOnSelect(null) },
                    onDragCancel = { currentOnSelect(null) },
                    onHorizontalDrag = { change, _ -> currentOnSelect(indexAt(change.position.x)) },
                )
            },
    ) {
        if (fractions.size < 2) return@Canvas

        val verticalPadding = 12.dp.toPx()
        val drawableHeight = size.height - 2 * verticalPadding
        val stepX = size.width / (fractions.size - 1)

        // fraction 1 (highest price) → top of the drawable area; 0 (lowest) → bottom.
        fun xOf(index: Int) = index * stepX
        fun yOf(fraction: Float) = verticalPadding + (1 - fraction) * drawableHeight

        val line = Path().apply {
            fractions.forEachIndexed { index, fraction ->
                if (index == 0) moveTo(xOf(index), yOf(fraction)) else lineTo(xOf(index), yOf(fraction))
            }
        }

        // Area under the line: the line path, then down to the bottom corners and closed.
        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(area, brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.25f), Color.Transparent)))

        // Dashed reference line at the starting price: above it is a gain, below it a loss.
        val startY = yOf(fractions.first())
        drawLine(
            color = baselineColor,
            start = Offset(0f, startY),
            end = Offset(size.width, startY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())),
        )

        drawPath(line, color = lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Scrubbing cursor: vertical line plus a dot on the selected point.
        if (selectedIndex != null && selectedIndex in fractions.indices) {
            val x = xOf(selectedIndex)
            val y = yOf(fractions[selectedIndex])
            drawLine(cursorColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
            drawCircle(lineColor, radius = 6.dp.toPx(), center = Offset(x, y))
            drawCircle(surfaceColor, radius = 3.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PriceChartPreview() {
    val closes = listOf(100, 102, 101, 105, 104, 108, 107, 111, 109, 114)
    val points = closes.mapIndexed { i, c -> PricePoint(Instant.ofEpochSecond(i * 86_400L), BigDecimal(c)) }
    PortfolioXTheme {
        PriceChart(points, Color(0xFF0B8043), selectedIndex = 6, onSelectedIndexChange = {}, contentDescription = "")
    }
}
