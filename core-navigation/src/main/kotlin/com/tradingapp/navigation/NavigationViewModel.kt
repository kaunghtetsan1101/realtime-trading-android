package com.tradingapp.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Holds the Navigation 3 back stack so it survives configuration changes (rotation,
 * dark-mode toggle, locale change, etc.).
 *
 * A ViewModel is retained by the Activity's ViewModelStore for the duration of the
 * Activity's lifetime, outliving individual recompositions and config changes.
 *
 * Limitation: process death clears the ViewModel. The user returns to the Watchlist
 * if the OS kills the app while it is in the background. Full process-death restoration
 * would require serialising the back stack into SavedStateHandle, which is deferred
 * to a future milestone.
 */
@HiltViewModel
class NavigationViewModel
@Inject
constructor() : ViewModel() {
    val backStack = mutableStateListOf<Any>(RouteWatchlist)
}
