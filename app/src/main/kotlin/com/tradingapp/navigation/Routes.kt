package com.tradingapp.navigation

sealed class Routes(val route: String) {
    data object Watchlist     : Routes("watchlist")
    data object MarketDetail  : Routes("market_detail/{symbol}") {
        fun createRoute(symbol: String) = "market_detail/$symbol"
    }
    data object Search        : Routes("search")
    data object Settings      : Routes("settings")
}
