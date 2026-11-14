# PortfolioX Market API

A small FastAPI server that the Android app's `RemoteMarketDataSource` talks to. It serves the
same 15 sample stocks and price model as the app's built-in sample source, so the app looks the
same either way, but every price now arrives over HTTP as JSON.

## Run

```powershell
cd server
py -m venv .venv                                         # first time only
.venv\Scripts\python -m pip install -r requirements-dev.txt   # first time only
.venv\Scripts\python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

Interactive docs: http://localhost:8000/docs

## Connect the app

1. Tunnel the device's port 8000 to your PC (repeat after each emulator restart):
   ```powershell
   adb reverse tcp:8000 tcp:8000
   ```
   This works for emulators and USB-connected phones, and needs no firewall changes because
   traffic goes through adb.
2. In `gradle.properties` set `portfoliox.marketDataSource=remote`, then run the app.
   The default base URL is `http://localhost:8000/`.

Alternative without `adb reverse` (emulator only): start uvicorn with `--host 0.0.0.0`, allow
Python through Windows Firewall, and set `portfoliox.apiBaseUrl=http://10.0.2.2:8000/`.

## Endpoints

| Method | Path | Returns |
|---|---|---|
| GET | `/health` | `{"status": "ok"}` |
| GET | `/stocks?query=a` | Stocks whose symbol or name contains the query (all when blank), symbol-prefix matches first |
| GET | `/stocks/{symbol}` | One stock, or 404 |
| GET | `/quotes?symbols=AAPL,MSFT` | Latest quotes; unknown symbols are skipped |
| GET | `/stocks/{symbol}/history?range=1M` | Price history for `1W`, `1M`, `3M` or `1Y`, oldest first |

Money is sent as **strings** (`"231.45"`) so no precision is lost to floating point.
Timestamps are ISO-8601 in UTC.

## Test

```powershell
.venv\Scripts\python -m pytest
```
