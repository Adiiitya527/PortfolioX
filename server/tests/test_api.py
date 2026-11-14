from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health():
    assert client.get("/health").json() == {"status": "ok"}


def test_search_ranks_symbol_prefix_first():
    symbols = [s["symbol"] for s in client.get("/stocks", params={"query": "a"}).json()]
    assert symbols[0].startswith("A")
    first_other = next(i for i, s in enumerate(symbols) if not s.startswith("A"))
    assert all(not s.startswith("A") for s in symbols[first_other:])


def test_blank_search_lists_all_stocks():
    assert len(client.get("/stocks").json()) == 15


def test_unknown_stock_is_404():
    response = client.get("/stocks/NOPE")
    assert response.status_code == 404


def test_quotes_send_money_as_decimal_strings_and_skip_unknown_symbols():
    quotes = client.get("/quotes", params={"symbols": "AAPL,NOPE,ibkr"}).json()
    assert [q["symbol"] for q in quotes] == ["AAPL", "IBKR"]
    price = quotes[0]["price"]
    assert isinstance(price, str) and len(price.split(".")[1]) == 2
    assert float(quotes[0]["day_low"]) <= float(price) <= float(quotes[0]["day_high"])


def test_history_has_one_point_per_step_and_ends_at_the_current_price():
    history = client.get("/stocks/NVDA/history", params={"range": "1W"}).json()
    assert len(history["points"]) == 85  # 7 days every 2 hours + 1
    timestamps = [p["timestamp"] for p in history["points"]]
    assert timestamps == sorted(timestamps)


def test_invalid_range_is_rejected():
    assert client.get("/stocks/NVDA/history", params={"range": "5Y"}).status_code == 422
