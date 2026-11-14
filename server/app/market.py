"""Simulated market data: the same 15 stocks and price model as the Android sample source.

Prices are a pure function of (symbol, time), so every client asking at the same moment gets
the same price and the server keeps no mutable state.
"""

from __future__ import annotations

import math
import random
import zlib
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from decimal import ROUND_HALF_EVEN, Decimal

CENT = Decimal("0.01")
TICK_SECONDS = 3
SLOW_WAVE_SECONDS = 6 * 60 * 60
FAST_WAVE_SECONDS = 47 * 60
ANNUAL_DRIFT = 0.08


def money(value: float | Decimal) -> Decimal:
    """Rounds to cents with banker's rounding, matching the app's Money.toMoney()."""
    return Decimal(str(value)).quantize(CENT, rounding=ROUND_HALF_EVEN)


@dataclass(frozen=True)
class SampleStock:
    symbol: str
    name: str
    exchange: str
    sector: str
    reference_price: Decimal  # yesterday's close
    daily_volatility: float


SAMPLE_STOCKS: list[SampleStock] = [
    SampleStock("AAPL", "Apple Inc.", "NASDAQ", "Technology", Decimal("231.45"), 0.015),
    SampleStock("MSFT", "Microsoft Corporation", "NASDAQ", "Technology", Decimal("428.10"), 0.014),
    SampleStock("NVDA", "NVIDIA Corporation", "NASDAQ", "Semiconductors", Decimal("142.80"), 0.030),
    SampleStock("GOOGL", "Alphabet Inc. Class A", "NASDAQ", "Communication Services", Decimal("176.25"), 0.017),
    SampleStock("AMZN", "Amazon.com, Inc.", "NASDAQ", "Consumer Discretionary", Decimal("198.60"), 0.018),
    SampleStock("META", "Meta Platforms, Inc.", "NASDAQ", "Communication Services", Decimal("585.30"), 0.020),
    SampleStock("TSLA", "Tesla, Inc.", "NASDAQ", "Consumer Discretionary", Decimal("251.75"), 0.035),
    SampleStock("NFLX", "Netflix, Inc.", "NASDAQ", "Communication Services", Decimal("702.40"), 0.022),
    SampleStock("AMD", "Advanced Micro Devices, Inc.", "NASDAQ", "Semiconductors", Decimal("158.90"), 0.028),
    SampleStock("IBKR", "Interactive Brokers Group, Inc.", "NASDAQ", "Financials", Decimal("187.55"), 0.019),
    SampleStock("JPM", "JPMorgan Chase & Co.", "NYSE", "Financials", Decimal("222.35"), 0.012),
    SampleStock("GS", "The Goldman Sachs Group, Inc.", "NYSE", "Financials", Decimal("538.20"), 0.016),
    SampleStock("V", "Visa Inc.", "NYSE", "Financials", Decimal("289.70"), 0.011),
    SampleStock("KO", "The Coca-Cola Company", "NYSE", "Consumer Staples", Decimal("68.15"), 0.008),
    SampleStock("DIS", "The Walt Disney Company", "NYSE", "Communication Services", Decimal("97.40"), 0.016),
]

STOCKS_BY_SYMBOL = {stock.symbol: stock for stock in SAMPLE_STOCKS}

# range label -> (span, step between points); mirrors ChartRange in the app.
RANGES: dict[str, tuple[timedelta, timedelta]] = {
    "1W": (timedelta(days=7), timedelta(hours=2)),
    "1M": (timedelta(days=30), timedelta(hours=8)),
    "3M": (timedelta(days=90), timedelta(days=1)),
    "1Y": (timedelta(days=365), timedelta(days=2)),
}


def find(symbol: str) -> SampleStock | None:
    return STOCKS_BY_SYMBOL.get(symbol.upper())


def search(query: str) -> list[SampleStock]:
    """Symbol or name contains the query; symbols starting with it come first."""
    term = query.strip().lower()
    stocks = sorted(SAMPLE_STOCKS, key=lambda s: s.symbol)
    if not term:
        return stocks
    matches = [s for s in stocks if term in s.symbol.lower() or term in s.name.lower()]
    return sorted(matches, key=lambda s: (not s.symbol.lower().startswith(term), s.symbol))


def _phase(symbol: str) -> float:
    # zlib.crc32 is stable across runs; Python's built-in hash() of strings is randomised per process.
    return (zlib.crc32(symbol.encode()) % 360) * math.pi / 180


def quote(stock: SampleStock, now: datetime) -> dict:
    reference = float(stock.reference_price)
    volatility = stock.daily_volatility
    phase = _phase(stock.symbol)
    seconds = now.timestamp()

    slow_wave = 0.6 * math.sin(2 * math.pi * seconds / SLOW_WAVE_SECONDS + phase)
    fast_wave = 0.3 * math.sin(2 * math.pi * seconds / FAST_WAVE_SECONDS + 2 * phase)
    tick = int(seconds // TICK_SECONDS)
    noise = 0.1 * (random.Random(f"{stock.symbol}:{tick}").random() * 2 - 1)
    price = reference * (1 + volatility * (slow_wave + fast_wave + noise))

    open_price = reference * (1 + volatility * 0.3 * math.sin(phase))
    high = max(open_price, price) * (1 + volatility * 0.25)
    low = min(open_price, price) * (1 - volatility * 0.25)

    return {
        "symbol": stock.symbol,
        "price": money(price),
        "previous_close": stock.reference_price,
        "open": money(open_price),
        "day_high": money(high),
        "day_low": money(low),
        "timestamp": now,
    }


def history(stock: SampleStock, range_label: str, now: datetime) -> list[dict]:
    """Random walk that ends exactly at the current price, seeded per (symbol, range)."""
    span, step = RANGES[range_label]
    count = int(span / step) + 1
    step_days = step / timedelta(days=1)
    step_volatility = stock.daily_volatility * math.sqrt(step_days)
    step_drift = ANNUAL_DRIFT * step_days / 365

    rng = random.Random(f"{stock.symbol}:{range_label}")
    prices = [0.0] * count
    prices[-1] = float(quote(stock, now)["price"])
    for i in range(count - 2, -1, -1):
        step_return = step_drift + step_volatility * rng.gauss(0, 1)
        prices[i] = max(prices[i + 1] / (1 + step_return), 1.0)

    return [
        {"timestamp": now - step * (count - 1 - i), "close": money(price)}
        for i, price in enumerate(prices)
    ]


def utc_now() -> datetime:
    return datetime.now(timezone.utc)
