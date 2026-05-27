# Architecture Reference

## Overview

Realtime Trading Android follows Clean Architecture with feature-based modules. All UI is
Jetpack Compose + MVI. Realtime prices arrive via a Binance WebSocket and are persisted to
Room, which is the single source of truth for every screen.

---

## Module Dependency Graph

```
app
 └── core-navigation
      ├── feature-watchlist
      │    ├── domain          ← use cases + repository interfaces
      │    ├── core-ui         ← shared Compose components, theme
      │    └── core-common     ← Result<T>, DispatcherProvider
      ├── feature-market-detail
      │    ├── domain
      │    ├── core-ui
      │    └── core-common
      └── feature-search
           ├── domain
           ├── core-ui
           └── core-common

app (Hilt aggregation)
 ├── data            ← repository implementations, mappers
 │    ├── core-network   ← Retrofit, OkHttp, WebSocketManager
 │    ├── core-database  ← Room DB, DAOs
 │    ├── domain
 │    └── core-common
 ├── core-network
 ├── core-database
 ├── domain
 └── core-common
```

**Rules enforced by module boundaries:**
- `domain` and `core-common` are pure Kotlin JVM — zero Android imports.
- Feature modules depend on `domain` only — no direct data/network access.
- `core-navigation` depends on all feature modules; no feature depends on another.
- `app` is the only module that depends on everything (for Hilt component aggregation).

---

## Layer Responsibilities

| Layer | Modules | Responsibility |
|-------|---------|----------------|
| Presentation | `feature-*`, `core-navigation`, `app` | Compose UI, ViewModels, MVI contracts, navigation |
| Domain | `domain` | Pure use cases, repository interfaces, domain models |
| Data | `data`, `core-network`, `core-database` | Repository implementations, mappers, REST/WS, Room |
| Infrastructure | `core-common`, `core-ui` | Cross-cutting utilities, shared Compose components |

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
  ├── observeAssets()   → WatchlistViewModel
  ├── observeAsset(sym) → MarketDetailViewModel
  └── observeFavorites() → (future use)
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

## Testing Strategy

| Layer | Tool | What is tested |
|-------|------|----------------|
| ViewModel | JUnit + MockK + Turbine | State transitions, effect delivery, debounce |
| Repository | JUnit + MockK + Turbine | Sync logic, WS tick routing, DB writes |
| Use cases | JUnit + MockK | Data transformations, error wrapping |
| HTTP parsing | MockWebServer + JUnit | Retrofit parameter encoding, Gson deserialization |
| Mapper | JUnit | DTO → entity → domain field mapping |

**Dispatcher injection:** `DispatcherProvider` is injected into ViewModels and repositories.
Tests swap `Dispatchers.IO` and `Dispatchers.Main` for `UnconfinedTestDispatcher`, making all
coroutine test code synchronous and deterministic.

**Flow testing with Turbine:** `turbine` provides `awaitItem()`, `awaitError()`, and
`cancelAndConsumeRemainingEvents()` to assert Flow emissions without manual coroutine
coordination or `delay()` calls.

---

## Build System

### Convention Plugins (`build-logic`)

A Gradle composite build at `build-logic/` defines six convention plugins that eliminate
~150 lines of duplicated Gradle config across 12 modules:

| Plugin ID | Applied by | Provides |
|-----------|-----------|---------|
| `tradingapp.kotlin.library` | `core-common`, `domain` | `kotlin.jvm` + toolchain 17 |
| `tradingapp.android.library` | Android libraries | `android.library` + `kotlin.android` + SDK versions |
| `tradingapp.android.library.compose` | Compose libraries | above + `kotlin.plugin.compose` + `buildFeatures.compose` |
| `tradingapp.android.hilt` | Any module using Hilt | `hilt` + `ksp` + hilt-android + hilt-compiler deps |
| `tradingapp.android.feature` | `feature-*` modules | compose + hilt + lifecycle + Compose BOM + test stack |
| `tradingapp.android.application` | `app` | `android.application` + kotlin + compose + SDK |

The version catalog (`gradle/libs.versions.toml`) is shared between the main build and
build-logic via `versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }`
in `build-logic/settings.gradle.kts`.

### Baseline Profiles (`:baseline-profile`)

`BaselineProfileGenerator` runs on a connected device (API 28+) and records the methods
exercised during cold start and watchlist scroll. The output (`baseline-prof.txt`) is
packaged into the APK and pre-compiled by `ProfileInstaller` at install time via ART's
profile-guided optimisation (PGO).

```bash
./gradlew :baseline-profile:generateBaselineProfile
```

### Macrobenchmarks (`:macrobenchmark`)

Two benchmarks run on a connected device/emulator (API 29+):

| Benchmark | Metric | Variants |
|-----------|--------|---------|
| `StartupBenchmark` | `StartupTimingMetric` | `None()` vs `Partial()` (profile) |
| `WatchlistScrollBenchmark` | `FrameTimingMetric` | `None()` vs `Partial()` (profile) |

```bash
./gradlew :macrobenchmark:connectedBenchmarkAndroidTest
```

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
