# QA Checklist

## Watchlist Feature

```text
[ ] Watchlist loads successfully
[ ] Loading state appears
[ ] Empty state appears when no assets are selected
[ ] Error state appears on API failure
[ ] Retry button works
[ ] Live prices update correctly
[ ] Only changed rows visibly update
[ ] Favorite state persists after restart
[ ] Cached data appears offline
[ ] Offline indicator appears when disconnected
```

## Realtime Stream

```text
[ ] WebSocket connects successfully
[ ] App handles stream disconnect
[ ] App reconnects or shows retry state
[ ] No crash during rapid updates
[ ] No visible UI freeze during frequent ticks
[ ] App handles malformed messages safely
```

## Lifecycle

```text
[ ] Stream pauses or behaves safely in background
[ ] Stream resumes correctly in foreground
[ ] No duplicate collectors after configuration change
[ ] Navigation back/forward does not duplicate streams
```

## Performance

```text
[ ] No major recomposition issue in watchlist
[ ] No obvious memory leak
[ ] No ANR during repeated navigation
[ ] Large watchlist remains scrollable
```

## Release Readiness

```text
[ ] Unit tests pass
[ ] ViewModel tests pass
[ ] App builds in debug
[ ] App builds in release
[ ] R8/ProGuard does not break models
[ ] README updated
[ ] Screenshots updated
[ ] Known limitations documented
```
