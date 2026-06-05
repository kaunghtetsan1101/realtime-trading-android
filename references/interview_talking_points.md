# Interview Talking Points

Reference answers for common senior Android engineer interview questions, grounded in decisions made in this project.

---

## Architecture

**Q: Why Clean Architecture in an Android app?**

The domain layer (`domain` module) is pure Kotlin JVM with zero Android imports. This means all business logic — use cases, repository interfaces, domain models — can be unit tested on the JVM without a device or emulator. The cost is boilerplate (mappers between DTO/Entity/Domain/UI). The benefit is that the test suite runs in milliseconds and the business logic can survive UI framework rewrites. For a trading app where correctness matters more than velocity, this tradeoff is worth it.

**Q: What is MVI and why use it?**

MVI enforces a single direction of data flow: the UI sends `Events`, the ViewModel updates an immutable `State`, and one-shot notifications travel as `Effects` via a `Channel`. Immutable state makes the UI predictable — the only thing that can change the screen is the ViewModel, and every UI state is reproducible from the state snapshot alone. `Channel` for effects avoids the "show snackbar twice on recomposition" problem that `SharedFlow` with replay causes. The tradeoff is verbosity: every screen needs a `Contract.kt` with three sealed types. Acceptable cost for a feature that has live data and network failures.

**Q: How do you prevent feature modules from knowing about each other?**

`core-navigation` is the only module that imports all feature modules. Individual feature modules expose a single public `@Composable` entry point. Routes are `@Serializable` data classes in `core-navigation`. Features never import or reference each other — they communicate only through effects emitted upward to the navigation host. This means you can delete or replace a feature module without touching any other feature.

---

## Realtime Data

**Q: How does realtime data flow from the WebSocket to the UI?**

1. `AssetRepositoryImpl` opens a Binance WebSocket using `OkHttp` and wraps it in `callbackFlow { awaitClose { ws.cancel() } }`.
2. Every incoming miniTicker frame is parsed to a `PriceTickDto` and emitted.
3. The flow is shared across subscribers using `MutableSharedFlow(replay = 1, onBufferOverflow = DROP_OLDEST)` — the latest price is always available immediately; if the consumer is slow, old ticks are dropped rather than causing backpressure.
4. Every tick also calls `assetDao.updatePrice()` — Room is always the source of truth, not in-memory state.
5. `GetWatchlistUseCase` observes Room via `assetDao.observeAll()` — the watchlist updates automatically when Room writes new prices.
6. `ObservePriceTicksUseCase` filters the shared flow by symbol for the detail screen, delivering sub-second updates.

**Q: How do you reconnect after a WebSocket failure?**

The WebSocket URL is stored in a `MutableStateFlow<String?>`. `observePriceTicks()` uses `.flatMapLatest { url -> webSocketManager.observe(url) }`. When the URL changes (after a re-sync or an error recovery), `flatMapLatest` automatically cancels the old connection and opens a new one. Inside `WebSocketManager`, `retryWhen` applies exponential backoff starting at 2 seconds, capped at 30 seconds. The reconnect logic is stateless — there is no manual "retry counter" to manage.

**Q: How do you prevent the WebSocket from freezing the UI?**

The WebSocket callback runs on OkHttp's internal thread. Emissions are processed in a coroutine on `Dispatchers.IO`. The `DROP_OLDEST` buffer strategy ensures that if Room writes are slow, frames are dropped rather than building up. Compose only triggers recomposition when the `StateFlow` value changes — and since Room is the intermediary, rapid WS ticks that don't change the price won't trigger redundant recompositions. The `LazyColumn` keys on asset symbol, so only the changed item row is measured and drawn, not the entire list.

---

## Compose Performance

**Q: How do you control recomposition in a list that updates every second?**

Three layers:
1. `Room` is the source of truth, not the raw WebSocket. Room's Flow only emits when data actually changes, reducing spurious recompositions.
2. `LazyColumn` uses `key = { it.symbol }` — Compose tracks each item by symbol and only recomposes the row whose price changed.
3. `AssetRow` takes a stable data class. The Compose compiler can skip recomposition of unchanged rows because all parameters are stable. `@Stable`/`@Immutable` annotations are not needed — Kotlin `data class` properties that are all primitive or `String` are inherently stable.

**Q: What is `derivedStateOf` and when would you use it here?**

`derivedStateOf` memoises a computation that depends on state, so it only recomputes when the inputs change. Example: the "Review Order" button enable state depends on `quantityInput` and `validationError`. Wrapping this in `derivedStateOf` prevents the button from re-evaluating on every state snapshot even when unrelated state fields (like `currentPrice`) change. In this project, the ViewModel computes `isQuantityValid` before emitting state, so the Composable receives a pre-computed Boolean — `derivedStateOf` in the UI would be redundant here.

---

## Testing

**Q: How do you test ViewModel logic that uses coroutines?**

Three tools work together:
1. `StandardTestDispatcher` — coroutines are not started automatically; `testDispatcher.scheduler.advanceUntilIdle()` drains the queue deterministically.
2. `Dispatchers.setMain(testDispatcher)` — ViewModels that launch on `Dispatchers.Main` use the test dispatcher instead.
3. `Turbine` — replaces `collect {}` with `test { awaitItem() }`. Turbine ensures the test fails loudly if fewer or more items than expected are emitted.

**Q: How do you test a Flow that combines multiple data sources?**

`GetPortfolioUseCaseTest` uses `MutableStateFlow` to control each source independently. By emitting to the price flow and calling `awaitItem()`, the test can verify that the P&L recalculates correctly in response to a price change without any threading complexity.

**Q: Why MockK instead of Mockito?**

MockK is Kotlin-first: it handles `object`s, `companion object`s, suspend functions, and extension functions natively. Mockito's Java-based DSL requires workarounds for these. `coEvery`, `coVerify`, and `mockkStatic` are idiomatic patterns that match how Kotlin code is actually written.

---

## Offline & Error Handling

**Q: How does the app behave offline?**

`NetworkMonitor` (in `core-network`) uses `ConnectivityManager.registerNetworkCallback` to emit `true`/`false` as network status changes. `ObserveNetworkStatusUseCase` wraps this for the feature layer. Every screen with live data — Watchlist, Market Detail, Trading, Portfolio — subscribes to this flow and shows an `OfflineBanner` component. Room always holds the last-synced price, so the watchlist is readable offline with a visual indicator of staleness.

**Q: How do you handle errors consistently across screens?**

`ErrorMapper` in `core-common` maps the exception hierarchy to user-readable strings. `UnknownHostException` → "No internet connection". `SocketTimeoutException` → "Request timed out". Generic `IOException` → "Connection error". All others fall through to `localizedMessage`. Every ViewModel routes exceptions through `ErrorMapper` before writing to state. This means if the copy changes (e.g. for i18n), there is one place to update.

---

## Room & Data

**Q: How do you ensure order placement is atomic?**

`TradeRepositoryImpl.placeOrder()` wraps wallet debit, position upsert/delete, and order insert in a single `db.withTransaction { }` block. Room's transaction serialises all writes on a single connection — either all three succeed or all three roll back. Partial state (e.g. wallet debited but order not saved) is impossible. This is the same guarantee as SQL ACID transactions.

**Q: How do you calculate the average entry price on repeated buys?**

Weighted average: `newAvgPrice = (existing.quantity × existing.avgPrice + order.quantity × order.price) / (existing.quantity + order.quantity)`. This correctly represents the true average cost basis regardless of how many separate buys were made. On SELL, the average price is left unchanged — the cost basis of the remaining position doesn't change when you exit part of it.

---

## Dependency Injection

**Q: Why Hilt over manual DI or Koin?**

Hilt is the officially recommended DI library for Android. It generates the DI graph at compile time (unlike Koin's runtime reflection), so DI failures are compile errors rather than runtime crashes. `@AssistedInject` solves the "ViewModel needs a runtime parameter" problem cleanly — `TradingViewModel` receives the asset symbol at creation time without a global singleton or saved state handle.

**Q: How does the ViewModel get the asset symbol at runtime?**

`TradingViewModel` uses `@AssistedInject` with `@Assisted val symbol: String`. The Navigation 3 entry provider creates the ViewModel using `hiltViewModel(creationCallback = { factory -> factory.create(key.symbol) })`. This eliminates the Nav2 pattern of reading the symbol from `SavedStateHandle` after creation — the symbol is guaranteed non-null and available immediately in `init`.

---

## Build System

**Q: What are convention plugins and why use them?**

Convention plugins are precompiled Gradle plugins in the `build-logic` composite build. Each plugin (`tradingapp.android.library`, `tradingapp.android.feature`, etc.) encapsulates a set of build configuration rules — SDK versions, compiler options, common dependencies. A library module's `build.gradle.kts` is then 10 lines instead of 60. The approach follows the Now in Android project and is the recommended pattern for large multi-module Gradle builds.

**Q: What is the Kotlin 2.4 + Hilt metadata conflict you resolved?**

Hilt 2.59 declares a dependency on `kotlin-metadata-jvm` with an upper bound of 2.3.0. Kotlin 2.4 ships a newer `kotlin-metadata-jvm`. Gradle's version resolution would pick 2.3.0 (satisfying Hilt's constraint), causing a binary incompatibility at the annotation processor level. The fix is to force `kotlin-metadata-jvm:2.4.0` in `allprojects { configurations.all { resolutionStrategy.force(...) } }`. This overrides Hilt's declared range and tells Gradle "use 2.4.0 regardless of what any dependency requests." The root cause is Hilt not yet having updated its metadata dependency range for Kotlin 2.4.
