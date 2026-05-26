# Architecture Guide

## Recommended Pattern

Use Clean Architecture with MVI for realtime screens.

```text
UI Composable
  -> ViewModel
  -> Use Case
  -> Repository Interface
  -> Repository Implementation
  -> Remote / Local Data Source
```

## UI State Example

```kotlin
data class WatchlistUiState(
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val items: List<MarketAssetUiModel> = emptyList(),
    val errorMessage: String? = null
)
```

## Event Example

```kotlin
sealed interface WatchlistEvent {
    data object OnRetryClicked : WatchlistEvent
    data class OnAssetClicked(val symbol: String) : WatchlistEvent
    data class OnFavoriteToggled(val symbol: String) : WatchlistEvent
}
```

## Effect Example

```kotlin
sealed interface WatchlistEffect {
    data class NavigateToAssetDetail(val symbol: String) : WatchlistEffect
    data class ShowSnackbar(val message: String) : WatchlistEffect
}
```

## Data Flow

1. WebSocket emits price tick DTO.
2. Remote data source maps raw message to network model.
3. Repository combines stream with local favorites/cache.
4. Use case emits domain market list.
5. ViewModel maps domain data to UI state.
6. Compose renders minimal affected UI.

## Error Handling

Use typed errors where possible:

```kotlin
sealed interface AppError {
    data object NetworkUnavailable : AppError
    data object Unauthorized : AppError
    data class Unknown(val message: String?) : AppError
}
```

## Module Rules

- Feature modules must not depend directly on network or database modules.
- Domain must not depend on Android framework classes.
- Data can depend on network, database, and domain.
- UI models should live in feature modules.
- Domain models should be platform/framework independent.

## Performance Notes

- Keep frequently changing price values localized to row-level composables.
- Avoid rebuilding whole lists unnecessarily.
- Use stable keys in lazy lists.
- Use Flow operators such as `distinctUntilChanged`, `sample`, or `debounce` carefully.
- Do not hide business logic inside Composables.
