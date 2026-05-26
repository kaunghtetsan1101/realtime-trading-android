# Realtime Trading — Android Portfolio App

A production-style Native Android app for realtime market watching and trading simulation.
Built with Kotlin, Jetpack Compose, MVI, Clean Architecture, and Hilt.

---

## Status

| Milestone | Status |
|-----------|--------|
| M1 — Project Scaffold | ✅ Done |
| M2 — Core Infrastructure | ✅ Done |
| M3 — Domain Layer | ✅ Done |
| M4 — Data Layer | ✅ Done |
| M5 — Watchlist Feature (MVP core) | ✅ Done |
| M6 — Asset Detail | ✅ Done |
| M7 — Design System + Dark Mode | Pending |
| M8 — Search | Pending |
| M9 — Trading Screen | Pending |
| M10 — Settings + Polish | Pending |

---

## Tech Stack

| Category | Library |
|----------|---------|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 |
| Architecture | MVI + Clean Architecture |
| DI | Hilt |
| Async | Coroutines + Flow / StateFlow |
| Network | Retrofit + OkHttp + WebSocket |
| Persistence | Room |
| Build | Gradle multi-module + Version Catalog |
| Testing | JUnit + MockK + Turbine + Compose UI Test |

---

## Module Structure

```
realtime-trading-android/
│
├── app/                    # Entry point: TradingApp, MainActivity, NavHost
│
├── core-common/            # Result<T>, DispatcherProvider, Flow extensions
├── core-ui/                # Shared Composables, Material3 theme, typography
├── core-network/           # Retrofit, OkHttp, WebSocketManager, mock WS server
├── core-database/          # Room DB, AssetDao, AssetEntity
│
├── domain/                 # Models, repository interfaces, use cases (pure Kotlin)
├── data/                   # Repository implementations, mappers, Hilt bindings
│
├── feature-watchlist/      # Watchlist screen — MVI ViewModel, UI, contract, tests
└── feature-market-detail/  # Asset detail screen — TradingView chart, live price ticker
```

### Dependency Rules

```
feature-* ──► domain, core-ui, core-common
data       ──► domain, core-network, core-database, core-common
domain     ──► core-common   (no Android imports)
core-ui    ──► core-common
app        ──► all modules
```

---

## Architecture

The app follows Clean Architecture with three layers per feature:

```
UI Layer (Compose / ViewModel)
    │  State + Events
    ▼
Domain Layer (Use Cases / Repository interfaces)
    │  Domain models
    ▼
Data Layer (Repository impl / DAOs / Network DTOs)
```

**MVI pattern inside each feature:**
```
Event ──► ViewModel ──► State (StateFlow)
                   └──► Effect (Channel — one-shot)
```

---

## Data Flow (Live Prices)

```
MockWebSocketServer
       │  JSON frames (1 500 ms interval)
       ▼
WebSocketManager.observePriceTicks()   [callbackFlow]
       │
       ▼
AssetRepositoryImpl
  ├── filters by symbol
  ├── writes updated price to Room (offline cache)
  └── maps to domain PriceTick
       │
       ▼
GetWatchlistUseCase ──► observeAssets() ──► Room emits ──► UI re-renders

observePriceTicks(symbol) ──► MarketDetailViewModel.recentPrices ──► price ticker
```

---

## Setup

### Prerequisites
- Android Studio Ladybug (2024.2) or newer
- JDK 17
- Android SDK 35

### Clone and open
```bash
git clone <repo-url>
cd realtime-trading-android
# Open in Android Studio — it will sync Gradle automatically
```

### Build from CLI
```bash
./gradlew assembleDebug
```

### Run tests
```bash
./gradlew test                              # Unit tests (all modules)
./gradlew :feature-watchlist:test           # Watchlist tests only
./gradlew :feature-market-detail:test       # Market detail tests only
./gradlew connectedAndroidTest              # Instrumented tests (requires emulator)
```

---

## Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Domain module plugin | `kotlin.jvm` | Pure Kotlin — no Android deps, fast JVM tests |
| Offline cache strategy | Room as single source of truth | WebSocket ticks write to DB; UI observes DB only |
| Effect delivery | `Channel<Effect>` | One-shot, not replayed on recomposition |
| WS lifecycle | `callbackFlow` + `awaitClose` | WS closes automatically when collector cancels |
| Dispatcher injection | `DispatcherProvider` interface | Enables deterministic coroutine tests |
| Mock WS in production sources | `MockWebSocketServer` | Self-contained demo; swap for real URL via DI |
