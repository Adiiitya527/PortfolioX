package com.portfoliox.domain.model

import java.time.Duration

/**
 * The time ranges a user can pick on the stock chart.
 *
 * An enum class is a fixed set of instances. Unlike Java enums, each entry here carries data
 * through the constructor: how far back the chart goes ([span]) and the gap between points ([step]).
 */
enum class ChartRange(
    val label: String,
    val span: Duration,
    val step: Duration,
) {
    WEEK(label = "1W", span = Duration.ofDays(7), step = Duration.ofHours(2)),
    MONTH(label = "1M", span = Duration.ofDays(30), step = Duration.ofHours(8)),
    QUARTER(label = "3M", span = Duration.ofDays(90), step = Duration.ofDays(1)),
    YEAR(label = "1Y", span = Duration.ofDays(365), step = Duration.ofDays(2)),
    ;

    /** Number of points in the chart, including both ends. */
    val pointCount: Int
        get() = (span.toMinutes() / step.toMinutes()).toInt() + 1
}
