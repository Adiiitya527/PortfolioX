# PortfolioX – Stock Portfolio Manager

A native Android app for tracking a simulated stock portfolio: a watchlist, live prices, historical charts, buy/sell trades, holdings and transaction history.

> Status: **Phase 5 – stock details and price chart**. Features are added phase by phase (see the roadmap below).

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, repository pattern, manual dependency injection |
| Async | Kotlin Coroutines and Flow |
| Persistence | Room (SQLite) |
| Networking | Retrofit + kotlinx.serialization |
| Testing | JUnit 4, kotlinx-coroutines-test, Room in-memory tests |

## Architecture

```
UI (Compose screens)  ──events──▶  ViewModel  ──▶  Repository  ──▶  Room DAOs (user data)
        ▲                              │                     └──▶  MarketDataSource (prices)
        └──── StateFlow<UiState> ◀─────┘                               ├─ FakeMarketDataSource (sample data)
                                                                       └─ RemoteMarketDataSource (Retrofit)
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

Packages are created as the phases that need them are implemented.

## Roadmap

- [x] Phase 0 – Project setup, theme, Git
- [x] Phase 1 – Domain models, sample market data, business logic and tests
- [x] Phase 2 – Room persistence and repositories
- [x] Phase 3 – App shell: navigation and shared UI state
- [x] Phase 4 – Watchlist and search
- [x] Phase 5 – Stock details and price chart
- [ ] Phase 6 – Simulated buy/sell
- [ ] Phase 7 – Dashboard and holdings
- [ ] Phase 8 – Transaction history
- [ ] Phase 9 – REST API integration
- [ ] Phase 10 – Polish

## Running the app

1. Open the project folder in Android Studio and wait for Gradle sync.
2. Select an emulator or device and press **Run**.

Command line: `./gradlew assembleDebug` builds the APK, `./gradlew testDebugUnitTest` runs the JVM unit tests, and `./gradlew connectedDebugAndroidTest` runs the database tests on a running emulator.
