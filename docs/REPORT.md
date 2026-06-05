# Portfolio Readiness Audit Report

**Project**: Realtime Trading / Market Watch App (Native Android)
**Date**: 2026-06-05
**Reviewer roles**: Senior Android Engineer · QA Engineer · Technical Writer · Hiring Manager

---

## Snapshot

| Metric | Finding |
|--------|---------|
| Modules | 20 (app, 7 core, 5 feature, 3 special) |
| Source files | ~80 Kotlin files across all layers |
| Test files | 15 |
| Test cases | 133 |
| README word count | ~7,500 words |
| CI jobs | 4 parallel (build, test, detekt, spotless) |
| Architecture | Clean — no cross-feature imports, domain is pure JVM |

---

## Risk Areas

### High Risk — would hurt in a PR review or interview

**1. No instrumented tests exist**
The README mentions `./gradlew connectedAndroidTest` but there are zero files in any `androidTest`
source set. A recruiter who clones and runs that command gets nothing. The README implies coverage
that does not exist.

**2. Screenshots section is empty**
Every screenshots table cell says `_(coming soon)_`. This is the first thing a recruiter sees when
landing on the repo. An empty screenshot section reads as an unfinished project.

**3. CI badge is a placeholder**
The README contains the literal text `Replace <your-username>`. This is visible on GitHub and
signals the repo was never actually published.

**4. `fallbackToDestructiveMigration()` in production Room DB**
`TradingDatabase` uses destructive migration. Acceptable for a demo, but a hiring manager who reads
the code will question it. There is no mention of this decision in the README or design decisions
table.

**5. `open class TradeRepositoryImpl` has no rationale comment**
Making a `@Singleton` Hilt class `open` for testability is a valid pattern, but without an inline
comment it looks like an oversight to a reviewer. The design decision is documented in the README
but not at the point of declaration in the source file.

---

### Medium Risk — noticeable in a code review

**6. Stale version comment in `TradingDatabase`**
The comment says "version 2 → 3 adds tables" but no migration object exists — only
`fallbackToDestructiveMigration`. The mismatch is confusing to a reader.

**7. `GetPortfolioUseCase` combine has no internal error boundary**
The use case combines three flows (positions, assets, cashBalance). If any of them throws, the
entire portfolio flow terminates silently. The ViewModel has a `.catch {}` but the use case itself
has no fallback, making the error path difficult to reason about.

**8. `AssetMapper` coin name map is a hardcoded `when` block**
Only ~20 coins are mapped. BNB, XRP, DOGE, DOT, MATIC, and others fall through to the raw symbol.
This is fine for a demo but an interviewer will notice it is not scalable. There are no tests for
these fallthrough cases.

**9. `WalletEntity.INITIAL_BALANCE` is never seeded via a Room migration**
The $10,000 default only applies when `walletDao.get()` returns `null`. No Room seed migration
inserts the initial row. Fine for a demo, but fragile if the wallet table is ever cleared directly.

**10. `OfflineBanner` `lastUpdatedMs` is always `null` in Trading and Portfolio screens**
The component accepts a timestamp to display cache staleness ("Last updated 3 min ago") but both
`TradingScreen` and `PortfolioScreen` pass `null`. The banner shows but the cache-age feature is
non-functional. The README documents this behaviour as if it works.

---

### Low Risk — polish items

- `Copyright (c) 2026` in the MIT License — off by a year, or looks AI-generated
- `ObserveNetworkStatusUseCase` is a thin wrapper with no unit test of its own
- `references/interview_talking_points.md` exists but is not linked from the README — interviewers
  will not find it
- `buildViewModel()` helper in `TradingViewModelTest` accepts a `cashBalance` parameter that goes
  unused in the basic state tests

---

## Missing Tests

### Priority 1 — gaps in already-tested layers

| Gap | Why it matters |
|-----|----------------|
| `AssetRepositoryImplTest`: no test for exponential backoff / reconnect | The reconnect strategy is cited in every interview answer; it should be verified |
| `AssetRepositoryImplTest`: no test for `toggleFavorite` → Room update | Favorites persistence is a listed MVP feature |
| `AssetMapperTest`: no test for fallthrough symbols (BNB, XRP, DOGE) | Shows mapper behaviour for the majority of real-world coins |
| `GetPortfolioUseCaseTest`: no test for a combined flow throwing | The combine has no internal error boundary; the failure path is undocumented |
| `WatchlistViewModelTest`: no test for `Sync` event success/failure | Sync is a primary user action and a top-bar button |
| `SearchViewModelTest`: no test for 300 ms debounce timing | Debounce is an explicit design decision worth verifying |
| `TradingViewModelTest`: no `ConfirmOrder` failure path test | The happy-path test exists; the order-rejection path does not |
| `PortfolioViewModelTest`: no test for reactive portfolio update on price tick | Only static flow emissions tested; live price recalculation is the core feature |

### Priority 2 — new test files worth adding

| Test file | Module | What to cover |
|-----------|--------|---------------|
| `ObserveNetworkStatusUseCaseTest` | `domain` | Delegates to `NetworkMonitor.observeIsOnline()`, maps values correctly |
| `ValidateOrderUseCaseTest` (extend) | `feature-trading` | Boundary: exact $0.00 balance after BUY; input `"1."` (trailing dot) |
| `TradeRepositoryImplTest` (extend) | `data` | `observePosition(symbol)` mapping; `observeOrders()` empty-list case |
| Integration: `AssetMapper` + `AssetRepositoryImpl` | `data` | Full REST → Room → domain chain with a MockWebServer fixture |

### Priority 3 — instrumented (high signal for portfolio)

One end-to-end Compose UI smoke test:

> Launch app → watchlist loads → tap an asset row → detail screen visible → back → watchlist still
> visible

A single passing instrumented test is more impressive in an interview than explaining why there are
none.

---

## Documentation Gaps

| Gap | Severity | Detail |
|-----|----------|--------|
| Screenshots | Critical | No screenshots = no visual proof the app works |
| CI badge | High | Placeholder `<your-username>` is visible on GitHub |
| `references/` not linked from README | High | `interview_talking_points.md` exists but no README entry |
| `fallbackToDestructiveMigration` not explained | Medium | Missing from Known Limitations and Design Decisions |
| `OfflineBanner` null timestamp | Medium | README implies cache-age display; code passes `null` |
| Performance numbers missing | Medium | Baseline Profile and macrobenchmark sections exist but contain no measurements |
| License year | Low | `Copyright (c) 2026` will confuse reviewers in 2025 |
| Troubleshooting section missing | Low | Common issues: Binance geo-blocking, WS refused from CI |
| `open class` rationale | Low | No inline comment at the declaration explaining the testability design |

---

## Full Test Inventory (133 cases, 15 files)

| File | Module | Cases | Coverage summary |
|------|--------|-------|-----------------|
| `ErrorMapperTest` | core-common | 6 | All exception types + blank fallback |
| `MarketApiTest` | core-network | 5 | Query params, Gson parsing, empty array |
| `WebSocketManagerTest` | core-network | 5 | Valid tick, malformed JSON, missing fields, USDT stripping, WS failure |
| `AssetMapperTest` | data | 12 | Symbol stripping, named coins, unknown coins, numeric parsing, logoUrl |
| `AssetRepositoryImplTest` | data | 7 | WS→DB, SharedFlow filtering, REST discovery, favorites, observe delegation |
| `TradeRepositoryImplTest` | data | 11 | BUY/SELL wallet math, weighted avg price, position upsert/delete, error propagation |
| `GetPortfolioUseCaseTest` | domain | 7 | P&L math, reactive update, zero-cost basis, missing-price fallback |
| `WatchlistViewModelTest` | feature-watchlist | 8 | Loading/success/error, navigate, favorite toggle, online/offline, retry |
| `MarketDetailViewModelTest` | feature-market-detail | 9 | Loading/success/error, price ticks, recentPrices cap (50), navigate, retry, offline |
| `SearchViewModelTest` | feature-search | 15 | All filters, all sort orders, debounce, no-match, navigate, favorite toggle |
| `ValidateOrderUseCaseTest` | feature-trading | 10 | All BUY/SELL validation error paths and boundaries |
| `PlaceOrderUseCaseTest` | feature-trading | 8 | Order construction, SELL correctness, FILLED status, totalValue math, exception wrapping |
| `TradingViewModelTest` | feature-trading | 16 | State, validation, price tick, QuickFill, sheet toggle, ConfirmOrder success, offline |
| `PortfolioViewModelTest` | feature-trading | 8 | Loading/error/portfolio, order history, Retry, effects, offline |
| `SettingsViewModelTest` | feature-settings | 7 | Version, theme, logging state, persistence, navigate, reactive updates |

---

## Architecture Findings

### Strengths

- Domain module is pure Kotlin JVM — no Android imports, fast unit tests
- Feature modules have zero knowledge of each other — `core-navigation` is the only fan-out point
- MVI contract files (`*Contract.kt`) make state/event/effect exhaustive and easy to review
- `DispatcherProvider` interface enables deterministic coroutine testing without `Dispatchers.setMain` hacks
- `AssistedInject` on `TradingViewModel` passes `symbol` at creation time — no `SavedStateHandle` workaround
- `callbackFlow { awaitClose { ws.cancel() } }` correctly ties WebSocket lifecycle to the collector
- Room `withTransaction` wrapper extracted as `open internal fun runInTransaction` — testable without `mockkStatic`
- Convention plugins in `build-logic` eliminate ~150 lines of duplicated Gradle config

### Concerns

- `GetPortfolioUseCase` combines three Room flows with no `catch` — silent termination on DB error
- `fallbackToDestructiveMigration` means every DB schema change wipes user data in production
- Coin name map in `AssetMapper` is a `when` block — will need a remote config or API response to scale
- `OfflineBanner` accepts `lastUpdatedMs: Long?` but callers pass `null` — partial feature
- No `@VisibleForTesting` annotation on `runInTransaction` to communicate intent at the declaration

---

## Final Improvement Plan

### Phase 1 — One-time fixes (< 2 hours, critical impact)

| # | Task | File(s) |
|---|------|---------|
| 1 | Add screenshots (5 screens: Watchlist, Detail, Trading, Portfolio, Settings) | `docs/screenshots/`, README |
| 2 | Push to GitHub and replace CI badge placeholder | README |
| 3 | Link `references/interview_talking_points.md` from README | README |
| 4 | Fix license year to 2025 | README |
| 5 | Add inline `@VisibleForTesting` comment on `open class TradeRepositoryImpl` | `data/.../TradeRepositoryImpl.kt` |
| 6 | Add `fallbackToDestructiveMigration` to Known Limitations in README | README |
| 7 | Pass a real or static `lastUpdatedMs` to `OfflineBanner` in Trading/Portfolio | `TradingScreen.kt`, `PortfolioScreen.kt` |

### Phase 2 — Test gaps (3–4 hours, confidence impact)

| # | Task | File(s) |
|---|------|---------|
| 8 | `PortfolioViewModelTest`: add reactive price-tick portfolio update test | `PortfolioViewModelTest.kt` |
| 9 | `TradingViewModelTest`: add `ConfirmOrder` failure path test | `TradingViewModelTest.kt` |
| 10 | `WatchlistViewModelTest`: add `Sync` success and failure tests | `WatchlistViewModelTest.kt` |
| 11 | `AssetMapperTest`: add fallthrough coin tests (BNB, XRP, DOGE, MATIC) | `AssetMapperTest.kt` |
| 12 | `AssetRepositoryImplTest`: add `toggleFavorite` persistence test | `AssetRepositoryImplTest.kt` |
| 13 | `GetPortfolioUseCaseTest`: add error-propagation test (combined flow throws) | `GetPortfolioUseCaseTest.kt` |

### Phase 3 — One instrumented smoke test (1 hour, high signal)

| # | Task | File(s) |
|---|------|---------|
| 14 | Add `WatchlistNavigationTest.kt` — launch → list visible → tap row → detail visible → back | `feature-watchlist/src/androidTest/` |

### Phase 4 — Documentation polish (1 hour)

| # | Task | File(s) |
|---|------|---------|
| 15 | Add `## Troubleshooting` section (Binance geo-block, JDK requirement, Gradle sync) | README |
| 16 | Add benchmark numbers once run on a device | README |
| 17 | Add TradingScreen state machine diagram (Idle → Validating → Reviewing → Placing → Done/Error) | `docs/REPORT.md` or README |

---

## Priority Matrix

| Item | Portfolio Impact | Effort | Phase |
|------|-----------------|--------|-------|
| Screenshots | Critical | Low | 1 |
| CI badge | High | Trivial | 1 |
| Link interview talking points | High | Trivial | 1 |
| `PortfolioViewModel` reactive update test | High | Low | 2 |
| `TradingViewModel` ConfirmOrder failure test | High | Low | 2 |
| `OfflineBanner` null timestamp fix | Medium | Low | 1 |
| `WatchlistViewModelTest` Sync tests | Medium | Low | 2 |
| `AssetMapperTest` fallthrough coins | Medium | Low | 2 |
| One Compose UI smoke test | High | Medium | 3 |
| Troubleshooting section | Low | Low | 4 |
| Benchmark numbers | Medium | Medium | 4 |
| State machine diagram | Medium | Medium | 4 |

---

## Interview Readiness Score: 8.5 / 10

### Strengths to lead with

- Clean Architecture with a pure-JVM domain layer — everything is unit testable without a device
- MVI pattern with immutable state — the UI is a pure function of the state snapshot
- Realtime WebSocket pipeline: `callbackFlow` → `SharedFlow` → Room → UI, with exponential backoff reconnect
- Offline-first: Room is the single source of truth; all screens degrade gracefully with `OfflineBanner`
- Atomic order placement via `db.withTransaction` — partial state (wallet debited, order not saved) is impossible
- Reactive portfolio P&L: `combine(positions, assets, cashBalance)` recalculates unrealised P&L on every price tick
- 133 unit tests covering happy paths, error paths, boundary conditions, and offline transitions
- Convention plugins in `build-logic` — same approach as AOSP's Now in Android

### Gaps to acknowledge proactively

- No instrumented UI tests — focus is on unit-testable architecture; UI correctness verified manually
- Screenshots pending — app runs correctly on emulator, documentation will be updated
- Simulated data only — no real broker API; portfolio is seeded with $10,000 virtual cash
- Performance metrics not yet captured — Baseline Profile and macrobenchmark framework is in place

---

*Generated: 2026-06-05 · Project: realtime-trading-android · Branch: main*
