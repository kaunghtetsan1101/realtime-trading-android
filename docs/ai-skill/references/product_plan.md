# Product Plan — Realtime Trading / Market Watch App

## Product Vision

Create a polished native Android portfolio app that simulates realtime market tracking and demonstrates senior Android engineering ability.

## Target Users

- Traders monitoring market movements
- Investors tracking favorite assets
- Recruiters and hiring managers evaluating Android architecture and code quality

## MVP Feature Roadmap

### Milestone 1 — Foundation

- Multi-module setup
- Design system
- Navigation shell
- Fake market data source
- Watchlist UI skeleton

### Milestone 2 — Realtime Watchlist

- WebSocket or mock streaming data source
- Watchlist screen with live price updates
- Loading/error/empty states
- Lifecycle-aware stream collection

### Milestone 3 — Persistence

- Favorite assets
- Room cache
- Offline latest price display
- DataStore settings

### Milestone 4 — Asset Detail

- Asset detail page
- Price movement summary
- Basic chart placeholder or simple chart
- Recent price history

### Milestone 5 — Quality and Portfolio Polish

- Unit tests
- ViewModel tests
- QA checklist
- README
- Screenshots/GIFs
- Architecture diagram

## User Stories

### Watchlist

```text
As a user,
I want to see my favorite market assets in one place,
so that I can monitor price movements quickly.
```

Acceptance criteria:

```text
Given the user opens the watchlist,
When market data is available,
Then the screen shows asset name, symbol, current price, and price change.
```

### Realtime Updates

```text
As a user,
I want prices to update in realtime,
so that I can react to market movement quickly.
```

Acceptance criteria:

```text
Given the WebSocket stream is connected,
When a new price tick arrives,
Then only the affected asset row updates without freezing the UI.
```

### Offline Cache

```text
As a user,
I want to see the last known market data when offline,
so that the app remains useful during network issues.
```

Acceptance criteria:

```text
Given the user loses internet connection,
When the watchlist screen is opened,
Then cached latest prices are shown with an offline indicator.
```

## Must Have

- Watchlist
- Realtime updates
- Offline cache
- Error/retry state
- Compose UI
- Clean architecture
- Tests
- README

## Should Have

- Search
- Asset detail
- Simple chart
- Dark/light theme
- Pull-to-refresh fallback

## Nice To Have

- Mock order screen
- Alerts
- Benchmark notes
- GitHub Actions

## Not Now

- Real money trading
- Complex authentication
- Real broker integration
- Payment features
