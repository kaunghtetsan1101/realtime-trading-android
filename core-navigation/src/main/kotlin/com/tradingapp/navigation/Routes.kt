package com.tradingapp.navigation

import kotlinx.serialization.Serializable

/**
 * Navigation 3 route keys.
 *
 * Each key is a serializable value/data class so the back stack can survive
 * process death. Data passed to a destination (e.g. [symbol]) lives directly
 * on the key — no string route templates or SavedStateHandle look-ups needed.
 */

@Serializable
data object RouteWatchlist

@Serializable
data class RouteMarketDetail(val symbol: String)

@Serializable
data object RouteSearch

@Serializable
data object RouteSettings

@Serializable
data class RouteTrading(val symbol: String)

@Serializable
data object RoutePortfolio
