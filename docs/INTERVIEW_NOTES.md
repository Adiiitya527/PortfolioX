# PortfolioX: interview notes

Short answers to the questions this project is most likely to raise, each tied to the code that backs it up. Read the file next to each answer before the interview so you can open it and point at it.

---

## The 60-second pitch

> PortfolioX is a native Android app for a simulated stock portfolio, written in Kotlin with Jetpack Compose and MVVM. You can search stocks, watch live prices, read charts, place buy and sell orders, and see holdings, P&L and history. User data lives in Room, and every screen observes it through Flow, so a trade updates the whole app without refresh code. Market data sits behind an interface: a built-in simulator, or a Retrofit client talking to a FastAPI server I wrote. Money is BigDecimal end to end, trades run in one database transaction, and the business rules are pure Kotlin with unit tests: about 125 JVM tests, Room tests on a device, and pytest for the server.

---

## Walk through one trade (the best end-to-end story)

1. **Tap Buy** on the stock detail screen. `StockDetailScreen` opens `TradeSheet` and calls `TradeViewModel.open(BUY)`.
2. **Type 10.** `TradeViewModel.onQuantityChange` keeps digits only. `TradeUiState` is a `combine` of the form input, the live quote, cash and the current holding. Its computed `validationError` calls the same `TradeValidator` the database uses.
3. **Tap "Buy 10 shares".** `submit()` builds a `TradeOrder` at the **live price** (a market order) and calls `PortfolioRepository.executeTrade`.
4. **The repository runs one Room transaction** (`TransactionRunner` → `withTransaction`): read cash and holding → validate again → `PortfolioCalculator.applyBuy` (weighted average cost) → upsert holding → update cash → insert transaction. If anything throws, everything rolls back. `DatabaseTest.failedTransactionRollsBackEveryWrite` proves it.
5. **Success is state, not an event.** `completed` is set, the screen's `LaunchedEffect` closes the sheet, shows a snackbar, then calls `onCompletionHandled()`.
6. **Room re-emits** the holdings, cash and transaction queries, so the position card, the dashboard and History all update by themselves.

---

## Architecture

**Why MVVM?**
The ViewModel survives configuration changes and holds screen state as a `StateFlow`. Composables only render state and send events, which is unidirectional data flow. Business logic stays out of the UI and is testable on the JVM.

**What does a repository do here?**
It's the only thing ViewModels talk to for data. `PortfolioRepository` hides Room and owns the trade transaction. `MarketRepository` hides which market source is used, caches stock details, and shares live quote streams.

**Why a `domain/` layer without use cases?**
The rules that matter (average cost, realized P&L, validation) are pure functions in `PortfolioCalculator` and `TradeValidator`. A use-case class per action would add files without adding clarity at this size.

**How is dependency injection done?**
By hand, in `di/AppContainer.kt`: `by lazy` singletons, created once in `PortfolioXApplication`. ViewModel factories read the container from `CreationExtras`. *Why not Hilt?* Every dependency stays visible and there's no annotation processing to explain. Hilt is the next step when the dependency graph grows.

**How would you swap in a real broker API?**
Write a new `MarketDataSource` implementation. Nothing above it changes; Phase 9 proved this with `RemoteMarketDataSource`.

---

## Kotlin

| Topic | Where | One-line answer |
|---|---|---|
| Data classes | `domain/model/` | equals/hashCode/copy for free. Models are immutable and updated with `copy` |
| Sealed types | `UiState`, `TradeError`, `TradeResult` | Closed hierarchies, so `when` must handle every case or it won't compile |
| Null safety | `Holding?`, `realizedPnl: BigDecimal?` | Absence is in the type. `?:`, `?.let`, `checkNotNull` instead of `!!` |
| Extension functions | `BigDecimal.toMoney()`, `Holding.toEntity()` | Add behaviour to types you don't own, keeping call sites readable |
| `object` / `companion object` | `PortfolioCalculator`, ViewModel `Factory` | A stateless singleton / static-like members |
| Class delegation `by` | `MarketRepositoryImplTest.CountingDataSource` | Forward an interface to another instance and override one method, as a test spy with no mocking library |
| `init` + `require` | `Holding` | Invariants enforced at construction: a holding never has 0 shares |
| `inline` + lambdas | `notFoundAsDomainError` | Zero-overhead wrappers, and non-local `return` inside `let` |

---

## Coroutines and Flow

**suspend vs Flow?** One value later vs many values over time.

**Cold vs hot?** `flow {}` is cold and runs per collector. `stateIn`/`shareIn` make it hot and shared.

**Why `stateIn(WhileSubscribed(5_000))`?** The screen always has a current value. Collection stops 5 s after the UI goes away, so a rotation doesn't restart queries, but a backgrounded app stops working.

**`flatMapLatest`?** When the input changes, the old inner flow is cancelled. Search uses it so a slow, stale result can't overwrite a newer one. The dashboard uses it to resubscribe prices when the set of held symbols changes.

**`combine`?** Re-emits when any source changes. The dashboard combines holdings, quotes, names and cash.

**`debounce(300)`?** Typing "msft" runs one search, not four. `SearchViewModelTest` checks this with virtual time.

**Structured concurrency?** `MarketRepository.getStocks` uses `coroutineScope { async … awaitAll() }`, so lookups run in parallel, and if one fails the rest are cancelled.

**Shared quote streams** (`MarketRepositoryImpl.observeQuotes`): `shareIn` on an app-wide scope, so the detail screen and the trade sheet share one polling loop. A SharedFlow never completes, so errors are wrapped in `Result`, rethrown per subscriber, and the failed stream is evicted so a retry starts fresh.

**Cancellation:** `catch (e: CancellationException) { throw e }` in `TradeViewModel.submit()`. Never swallow it. That's also why there's no `runCatching` in production code.

**Testing coroutines:** `runTest` with virtual time. `MainDispatcherRule` replaces `Dispatchers.Main`, and `backgroundScope` holds collectors for `WhileSubscribed` flows.

---

## Compose

- **State hoisting:** `XScreen` (gets the ViewModel and collects state) wraps `XContent(state, callbacks)`, which is stateless and previewable.
- **`collectAsStateWithLifecycle`:** stops collecting in the background.
- **`remember` / `rememberSaveable`:** survive recomposition / also rotation and process death (search text, dialog and sheet visibility).
- **`LaunchedEffect(key)`:** runs side effects. Changing the key restarts it, which is why the trade snackbar runs in a separate `rememberCoroutineScope`.
- **Lists:** `LazyColumn` with stable `key`s, `stickyHeader` for days in History, `SwipeToDismissBox` for the watchlist.
- **Custom drawing:** `PriceChart` uses `Canvas`, `Path`, gradients, dashed lines and `pointerInput` drag gestures. The scaling maths is a pure function (`normalizeForChart`) with tests.
- **Theming:** Material 3 colour schemes plus a `CompositionLocal` for gain/loss colours (`MaterialTheme.financialColors`). Dynamic colour is off so red and green always mean the same thing.
- **Navigation:** type-safe `@Serializable` routes. Screens get callbacks, never the `NavController`. Arguments reach ViewModels via `SavedStateHandle.toRoute()` in the factory.

---

## Data and persistence

**Why Room?** Compile-time checked SQL, Flow queries that re-emit on change, and transactions.

**Entities vs domain models?** Separate on purpose, so the database schema can change without touching UI or rules. Mappers sit at the boundary.

**Why is BigDecimal stored as TEXT?** REAL is floating point. The trade-off is that SQL can't `SUM` these columns, which is fine because totals are calculated in Kotlin.

**How do you test the database?** Two levels. JVM tests use in-memory fake DAOs for trade logic. Instrumented tests use an in-memory Room database for SQL, converters, rollback and reset.

**Survives process death?** Room data, yes. Navigation is restored automatically, and the chart range is kept in `SavedStateHandle` (tested in `StockDetailViewModelTest`).

---

## Money and domain rules

- **Never `Double` for money:** `0.1 + 0.2 != 0.3`. `BigDecimal` with explicit scale and `HALF_EVEN` rounding everywhere (`Money.kt`).
- **Kotlin trap:** `/` on `BigDecimal` keeps the left operand's scale, so `1 / 3 = 0`. Always use `divide(x, scale, rounding)`.
- **Average cost** keeps 4 decimals so repeated buys don't drift: `(oldQty·oldAvg + qty·price) / newQty`.
- **Selling** leaves the average cost unchanged and realizes `(price − avg) × qty`.
- **Validation runs twice:** live in the form for feedback, and inside the transaction as the final authority. It's the same function, so they can't disagree.

---

## Networking

- **Retrofit interface** (`MarketApi`) + OkHttp (timeouts, debug logging) + kotlinx.serialization converter.
- **DTOs** mirror the JSON (`@SerialName` for snake_case). Mappers turn decimal strings into `BigDecimal` and ISO strings into `Instant`.
- **Errors:** 404 → `StockNotFoundException` (same as the simulator). `IOException` → "can't reach the service" with retry.
- **Live prices = polling** every 3 s with exponential backoff (1 s → 30 s) on transient failures. Screens keep their last prices and recover on their own. *In production:* WebSocket or server-sent events behind the same `Flow` interface.
- **`ignoreUnknownKeys`** keeps old app versions working when the server adds fields.
- **Security:** HTTPS everywhere except localhost and 10.0.2.2 for local development (`network_security_config.xml`).

---

## Testing strategy

| Level | Examples |
|---|---|
| Pure domain | Average cost, realized P&L, rounding, validation (`PortfolioCalculatorTest`, `TradeValidatorTest`) |
| Repository + fakes | Trades change cash, holding and history together; a rejected trade changes nothing |
| ViewModels | Loading → Success/Empty/Error, debounce timing, undo position, live updates, retry |
| Networking | Fake `MarketApi`: 404 mapping, polling interval, backoff; real server JSON parsed |
| Device (Room) | SQL ordering, exact decimals, **rollback**, reset |
| Server | pytest: endpoints, 404/422, money as strings |

**Fakes over mocks:** tests assert outcomes ("cash is now $8,500"), not calls, so they survive refactoring.

---

## Trade-offs and what I'd do next

- **Same-day "today" P&L** compares to yesterday's close even for shares bought today. A real broker uses the purchase price for intraday lots.
- **Polling vs push:** WebSockets for production.
- **Manual DI:** move to Hilt as the graph grows.
- **Holdings queried three times** on the dashboard (rows, price symbols, names). That's cheap here; I'd use `shareIn` for large tables.
- **Release build:** enable R8 minification, plus Room schema export and migrations before any schema change.
- **Accessibility:** large fonts tested at 2×. Next would be a TalkBack pass on the chart (spoken summaries per range).
- **Fractional shares, limit orders, multiple currencies** are domain extensions. `TradeOrder` and `Money` are where they'd start.
