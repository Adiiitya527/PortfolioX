# PortfolioX – Stock Portfolio Manager

A native Android app for tracking a simulated stock portfolio: a watchlist, live prices, historical charts, buy/sell trades, holdings and transaction history.

> Status: **Phase 9 – REST API integration**. Features are added phase by phase (see the roadmap below).

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, repository pattern, manual dependency injection |
| Async | Kotlin Coroutines and Flow |
| Persistence | Room (SQLite) |
| Networking | Retrofit + kotlinx.serialization |
| Testing | JUnit 4, kotlinx-coroutines-test, Room in-memory tests, pytest (server) |
| Mock backend | FastAPI (Python) in `server/` |

## Architecture

```
UI (Compose screens)  ──events──▶  ViewModel  ──▶  Repository  ──▶  Room DAOs (user data)
        ▲                              │                     └──▶  MarketDataSource (prices)
        └──── StateFlow<UiState> ◀─────┘                               ├─ SampleMarketDataSource (built-in simulator)
                                                                       └─ RemoteMarketDataSource (Retrofit → FastAPI)
```

- **Unidirectional data flow:** screens render state and send events; ViewModels own the state.
- **Room is the single source of truth** for watchlist, holdings, transactions and cash.
- **Market data sits behind an interface**, so sample data and a real REST API are interchangeable.
- **Money uses `BigDecimal`**, never `Double`.

## Project structure

```
app/src/main/java/com/portfoliox/
├── di/         Dependency container that wires the app together
├── core/       Formatting and other shared utilities
├── domain/     Pure Kotlin models and business rules (no Android imports)
├── data/       Room database, market data sources, repositories
└── ui/         Compose screens, ViewModels, theme, navigation
```

The `server/` folder holds the FastAPI mock backend used by `RemoteMarketDataSource`.

## Roadmap

- [x] Phase 0 – Project setup, theme, Git
- [x] Phase 1 – Domain models, sample market data, business logic and tests
- [x] Phase 2 – Room persistence and repositories
- [x] Phase 3 – App shell: navigation and shared UI state
- [x] Phase 4 – Watchlist and search
- [x] Phase 5 – Stock details and price chart
- [x] Phase 6 – Simulated buy/sell
- [x] Phase 7 – Dashboard and holdings
- [x] Phase 8 – Transaction history
- [x] Phase 9 – REST API integration
- [ ] Phase 10 – Polish

## Running the app

1. Open the project folder in Android Studio and wait for Gradle sync.
2. Select an emulator or device and press **Run**.

Command line: `./gradlew assembleDebug` builds the APK, `./gradlew testDebugUnitTest` runs the JVM unit tests, and `./gradlew connectedDebugAndroidTest` runs the database tests on a running emulator.

## Using the FastAPI backend

The app ships with a built-in market simulator, so it runs with no setup. To fetch market data over HTTP from the bundled FastAPI server instead:

1. Start the server (see [server/README.md](server/README.md)): `.venv\Scripts\python -m uvicorn app.main:app --host 127.0.0.1 --port 8000`
2. Forward the port to the emulator or phone: `adb reverse tcp:8000 tcp:8000`
3. Set `portfoliox.marketDataSource=remote` in `gradle.properties` and run the app.
