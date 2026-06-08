# Architecture Reference

## Overview

Realtime Trading Android follows Clean Architecture with feature-based modules. All UI is
Jetpack Compose + MVI. Realtime prices arrive via a Binance WebSocket and are persisted to
Room, which is the single source of truth for every screen.

Simulated trading (orders, positions, wallet, TP/SL) is also persisted in Room and updated
atomically inside transactions.

---

## Module Dependency Graph

```
app
 └── core-navigation
      ├── feature-watchlist       (Market tab + Favorites tab ViewModels)
      ├── feature-market-detail
      ├── feature-search
      ├── feature-trading         (TradingScreen + PortfolioScreen)
      └── feature-settings
           ├── domain             ← use cases + repository interfaces
           ├── core-ui            ← shared Compose components, theme wrapper
           ├── core-designsystem  ← Color, Typography, Spacing, Shape tokens
           └── core-common        ← Result<T>, DispatcherProvider, ErrorMapper

app (Hilt aggregation)
 ├── data                 ← repository implementations, mappers
 │    ├── core-network    ← Retrofit, OkHttp, WebSocketManager, NetworkMonitor
 │    ├── core-database   ← Room DB, DAOs, migrations
 │    ├── domain
 │    └── core-common
 ├── core-datastore       ← ThemeMode + verbose logging (Preferences DataStore)
 ├── core-network
 ├── core-database
 ├── domain
 └── core-common

baseline-profile / macrobenchmark  → target :app (test-only modules)
```

**Rules enforced by module boundaries:**

- `domain` and `core-common` are pure Kotlin JVM — zero Android imports.
- Feature modules depend on `domain` only — no direct data/network access.
- `core-navigation` depends on all feature modules; no feature depends on another feature.
- `core-ui` exposes `core-designsystem` via `api` so features get tokens transitively.
- `app` is the only module that depends on everything (Hilt component aggregation).

---

## Layer Responsibilities

| Layer | Modules | Responsibility |
|-------|---------|----------------|
| Presentation | `feature-*`, `core-navigation`, `app` | Compose UI, ViewModels, MVI contracts, navigation |
| Domain | `domain` | Pure use cases, repository interfaces, domain models |
| Data | `data`, `core-network`, `core-database` | Repository implementations, mappers, REST/WS, Room |
| Infrastructure | `core-common`, `core-ui`, `core-designsystem`, `core-datastore` | Cross-cutting utilities, design tokens, preferences |

---

## Navigation

Navigation 3 (`androidx.navigation3`) lives in `core-navigation`. Routes are `@Serializable`
keys on the back stack — no string templates or `SavedStateHandle` lookups.

| Route | Screen | Notes |
|-------|--------|-------|
| `RouteWatchlist` | Market tab — full asset list | `GetWatchlistUseCase` |
| `RouteWatchlistFavorites` | Watchlist tab — favourites only | `GetFavoritesUseCase` |
| `RoutePortfolio` | Portfolio tab | positions + order history |
| `RouteMarketDetail(symbol)` | Asset detail | pushed onto back stack |
| `RouteSearch` | Search | pushed onto back stack |
| `RouteTrading(symbol)` | BUY/SELL screen | pushed onto back stack |
| `RouteSettings` | Theme + developer options | pushed onto back stack |

`NavigationViewModel` retains the back stack across configuration changes. The bottom
`NavigationBar` (Market / Watchlist / Portfolio) is visible only on the three tab routes;
detail flows use each screen's own `TopAppBar` back affordance.

`MainActivity` calls `enableEdgeToEdge()`. The outer `Scaffold` in `AppNavGraph` uses
`contentWindowInsets = WindowInsets(0)` so status-bar insets are not applied twice when
feature screens own their own scaffold.

---

## MVI Pattern

Each feature screen follows the same contract:

```
┌─ UI (Composable) ──────────────────────────────────────────────┐
│  collectAsStateWithLifecycle(viewModel.state)                   │
│  LaunchedEffect { viewModel.effects.collect { ... } }          │
│  onEvent(event) ──► viewModel.onEvent(event)                   │
└────────────────────────────────────────────────────────────────┘
            │ Event                       ▲ State (StateFlow)
            ▼                             │
┌─ ViewModel ────────────────────────────┼───────────────────────┐
│  stateMutable: MutableStateFlow<S>     │                       │
│  effectsMutable: Channel<E>            │                       │
│  onEvent(event) → update state  ───────┘                       │
│                 → send effect (one-shot, via Channel)          │
└────────────────────────────────────────────────────────────────┘
```

**Naming convention:** private mutable backing properties use the `nameMutable` suffix
(e.g., `stateMutable`, `effectsMutable`) to distinguish them from the public read-only
`state`/`effects` exposures. This satisfies ktlint's backing-property-naming rule.

**Effect delivery:** `Channel<Effect>` is used instead of `SharedFlow` — one-shot
semantics guarantee navigation and snackbar events are not replayed on recomposition.

---

## Design System

Design tokens and components are split across two modules:

| Module | Role |
|--------|------|
| `core-designsystem` | Pure tokens — `Color`, `Typography`, `Spacing`, `Shape`; no composable logic |
| `core-ui` | `TradingAppTheme`, `AssetRow`, `PriceText`, `PercentageBadge`, `OfflineBanner`, `ErrorState`, `EmptyState` |

`MainActivity` reads `ThemeMode` from DataStore and passes the resolved dark/light flag into
`TradingAppTheme`. Components use Material3 semantic colours (`onSurfaceVariant`, `PriceUp`,
`PriceDown`) rather than hard-coded alpha values.

---

## Data Flow: Live Price Updates

```
Binance REST  GET /api/v3/ticker/24hr?type=MINI
      │  all USDT pairs → filter → sort by volume → top 100
      ▼
AssetRepositoryImpl.syncAssets()
  ├── upserts 100 assets into Room (insert-or-ignore + updateMarketData)
  └── emits new WebSocket URL to wsUrlMutable: MutableStateFlow<String?>
                                │
         flatMapLatest: old WS cancelled, new WS opens
                                │
                                ▼
Binance WebSocket  wss://stream.binance.com/stream?streams=…miniTicker
      │  ~1 frame/second per symbol (top 50 streams)
      ▼
WebSocketManager.observePriceTicks()   [callbackFlow + OkHttp listener]
      │  retryWhen: exponential backoff up to 30 s
      ▼
priceTicksMutable: MutableSharedFlow (replay=1, DROP_OLDEST)
  ├── emits PriceTick to MarketDetailViewModel  (symbol filter)
  └── writes updated price + timestamp to Room
                                │
                                ▼
Room emits updated rows to all active flows
  ├── observeAssets()      → WatchlistViewModel (Market tab)
  ├── observeFavorites()   → FavoritesViewModel (Watchlist tab)
  ├── observeAsset(sym)    → MarketDetailViewModel
  └── (price ticks)        → PortfolioViewModel unrealised P&L
                                │
                                ▼
              UI re-renders with new price (key-stable LazyColumn)
```

**Offline state:** Room is always the source of truth. When the device loses connectivity,
`NetworkMonitor` (via `ConnectivityManager.NetworkCallback`) emits `NetworkStatus.Offline`.
ViewModels set `state.isOffline = true`, which shows the `OfflineBanner` with a
"Cached · X min ago" label derived from `asset.lastUpdated`.

---

## Realtime WebSocket Pipeline

```kotlin
// Startup sequence in AssetRepositoryImpl.init {}

1. Read top symbols from DB (or use BOOTSTRAP_SYMBOLS if DB empty)
2. wsUrlMutable.value = buildWsUrl(symbols)       // null → real URL
3. flatMapLatest on wsUrlMutable:
   - collect WebSocket frames → priceTicksMutable
   - retryWhen: exponential backoff (2 s base, 30 s cap)
4. After syncAssets():
   wsUrlMutable.value = buildWsUrl(newTopSymbols) // triggers reconnect
```

Key design choices:

- `SupervisorJob`: a WebSocket error never cancels unrelated repository coroutines.
- `replay=1` on `priceTicksMutable`: the detail screen gets the last price immediately on open.
- `DROP_OLDEST` overflow: back-pressure from a slow collector never stalls the pump.
- `flatMapLatest`: symbol list changes cancel the old socket cleanly without races.

---

## Search: In-Memory Filtering

Search does not make network calls. It runs in-memory against Room data via `GetWatchlistUseCase`.

```
combine(
    getWatchlist(),                          // Room flow
    queryMutable.debounce(300),              // TextField input
    filterInputMutable,                      // ALL | FAVORITES | GAINERS | LOSERS
    sortInputMutable,                        // NONE | PRICE↓ | PRICE↑ | VOL↓ | VOL↑
)
```

Three separate `MutableStateFlow` inputs (query, filter, sort) prevent feedback loops from
reading and writing the same `StateFlow` inside a `combine` transform.

The query is debounced at 300 ms for the combine; a parallel `onEach` updates `state.query`
immediately so the `TextField` stays responsive.

---

## Trading & Portfolio

All trading is client-side simulation — no broker API. State lives in Room:

| Table | Purpose |
|-------|---------|
| `wallet` | Single-row virtual cash balance (seeded $10 000) |
| `positions` | Open positions with direction, avg cost, optional TP/SL |
| `orders` | Placed and closed order history with realised P&L |

**Order placement flow:**

```
TradingViewModel.onEvent(ConfirmOrder)
  → ValidateOrderUseCase        (sync, pure — balance / quantity checks)
  → PlaceOrderUseCase
      → TradeRepositoryImpl.placeOrder()
          room.withTransaction { debit wallet, upsert/delete position, insert order }
```

`TradingViewModel` uses `@AssistedInject` to receive the asset `symbol` at navigation time.
Validation runs on every keystroke; a confirmation bottom sheet gates the final submit.

**Portfolio TP/SL auto-exit:**

```
PortfolioViewModel.monitorExits()
  getPortfolio().map { positions }
    .flatMapLatest { rebuild when list changes }
      .flatMapMerge { per-position price tick flow }
        monitorPositionExit(position, priceFlow).take(1)
          → ClosePositionUseCase on TP/SL trigger
          → ShowSnackbar effect
```

`flatMapLatest` cancels stale monitors when positions open or close. `take(1)` prevents
double-trigger if price oscillates around a threshold. Monitoring runs in `viewModelScope`
and stops when the Portfolio screen leaves the back stack.

Unrealised P&L is derived reactively in `GetPortfolioUseCase` by combining Room positions
with live price ticks — no extra DB column.

---

## Settings & Preferences

`core-datastore` wraps Preferences DataStore for:

- `ThemeMode` — System / Light / Dark (read in `MainActivity`, applied to `TradingAppTheme`)
- Verbose logging flag — controls Timber tree planting on next cold start

`SettingsViewModel` exposes MVI state for the settings screen; `AppInfo` (version name) is
provided by an `app`-module Hilt binding because only `app` knows `BuildConfig.VERSION_NAME`.

---

## Testing Strategy

| Layer | Tool | What is tested |
|-------|------|----------------|
| ViewModel | JUnit + MockK + Turbine | State transitions, effect delivery, debounce, offline flag |
| Repository | JUnit + MockK + Turbine | Sync logic, WS tick routing, atomic order placement |
| Use cases | JUnit + MockK | Validation, P&L math, TP/SL monitoring, error wrapping |
| HTTP parsing | MockWebServer + JUnit | Retrofit parameter encoding, Gson deserialization |
| Mapper | JUnit | DTO → entity → domain field mapping |

**Dispatcher injection:** `DispatcherProvider` is injected into ViewModels and repositories.
Tests swap `Dispatchers.IO` and `Dispatchers.Main` for `UnconfinedTestDispatcher`, making all
coroutine test code synchronous and deterministic.

**Flow testing with Turbine:** `turbine` provides `awaitItem()`, `awaitError()`, and
`cancelAndConsumeRemainingEvents()` to assert Flow emissions without manual coroutine
coordination or `delay()` calls.

**Room transactions in unit tests:** `TradeRepositoryImpl.runInTransaction` is `open internal`
so tests override it and run the block directly without `mockkStatic` on Room extensions.

---

## Build System

### Convention Plugins (`build-logic`)

A Gradle composite build at `build-logic/` defines six convention plugins:

| Plugin ID | Applied by | Provides |
|-----------|-----------|---------|
| `tradingapp.kotlin.library` | `core-common`, `domain` | `kotlin.jvm` + JVM toolchain 21 |
| `tradingapp.android.library` | Android libraries | `android.library` + SDK versions |
| `tradingapp.android.library.compose` | Compose libraries | above + Compose plugin + `buildFeatures.compose` |
| `tradingapp.android.hilt` | Hilt modules | Hilt + KSP + compiler deps |
| `tradingapp.android.feature` | `feature-*` modules | compose + hilt + lifecycle + test stack |
| `tradingapp.android.application` | `app` | application + compose + SDK + profile-installer |

The version catalog (`gradle/libs.versions.toml`) is shared between the main build and
build-logic via `versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }`
in `build-logic/settings.gradle.kts`. Module scripts use typed `libs.*` accessors; convention
plugins read the catalog via `Project.catalog` to avoid shadowing Gradle's generated `libs`.

### Baseline Profiles (`:baseline-profile`)

`BaselineProfileGenerator` runs on a connected device (API 28+) and records the methods
exercised during cold start and watchlist scroll. The output (`baseline-prof.txt`) is
packaged into the APK and pre-compiled by `ProfileInstaller` at install time via ART's
profile-guided optimisation (PGO).

```bash
./gradlew :baseline-profile:generateBaselineProfile
```

### Macrobenchmarks (`:macrobenchmark`)

Self-instrumenting macrobenchmark module targeting `:app`. Requires a physical device (API 29+).

| Benchmark | Metric | Variants |
|-----------|--------|---------|
| `StartupBenchmark` | `StartupTimingMetric` (TTID) | `None()` vs `Partial()` |
| `WatchlistScrollBenchmark` | `FrameTimingMetric` | `None()` vs `Partial()` |

```bash
./gradlew :macrobenchmark:connectedBenchmarkAndroidTest
```

**Device run setup:**

- `app` defines a `benchmark` build type — `initWith(release)` + debug signing — so the APK installs on device.
- `macrobenchmark` depends on `benchmark-junit4` (provides `AndroidBenchmarkRunner`) and `benchmark-macro-junit4`.

**Measured results (OPPO CPH2689, Android 16, 2026-06-08):**

| Test | JIT median TTID | Profile median TTID |
|------|-----------------|---------------------|
| `coldStartNoCompilation` | 315.7 ms | — |
| `coldStartBaselineProfile` | — | 289.1 ms |

Scroll benchmarks currently fail with `StaleObjectException` — UiAutomator holds a scrollable
node while realtime price updates invalidate the accessibility tree. Re-query the list each
scroll iteration to fix.

---

## Performance Strategy

### Compose Recomposition

- `StateFlow<UiState>` is a single stable object — changing one field replaces the whole
  state and triggers a single recomposition of the observing composable tree.
- `LazyColumn` uses `key = { asset.symbol }` — stable keys prevent item views from being
  recreated when only price text changes.
- Price updates write to Room, which emits a new list to `StateFlow`. The `LazyColumn`
  diffing only remeasures/redraws changed items.
- `TradingViewChart` is a `WebView`-backed composable isolated from the price-tick flow —
  it does not recompose on every tick.

### WebSocket Back-Pressure

`priceTicksMutable` is a `MutableSharedFlow` with `replay=1` and `DROP_OLDEST` overflow.
If the Room write coroutine lags (e.g., during a sync), the oldest tick is discarded rather
than suspending the WebSocket read loop. Users see the next frame rather than a stale one.
