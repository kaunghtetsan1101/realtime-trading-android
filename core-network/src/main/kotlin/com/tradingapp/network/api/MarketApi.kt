package com.tradingapp.network.api

import com.tradingapp.network.model.BinanceMiniTickerDto
import com.tradingapp.network.model.BinanceTicker24hrDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Binance public REST API — no API key required.
 *
 * Base URL: https://api.binance.com/
 */
interface MarketApi {
    /**
     * Returns mini-ticker data (symbol + quoteVolume) for ALL active trading pairs.
     *
     * Used for symbol discovery: ~1 600 pairs returned, no auth required.
     * Call once to determine which symbols to track, then use [get24hrTickers] for full data.
     */
    @GET("api/v3/ticker/24hr")
    suspend fun getAllMiniTickers(@Query("type") type: String = "MINI"): List<BinanceMiniTickerDto>

    /**
     * Returns full 24-hour statistics for the requested symbols.
     *
     * [symbols] must be a JSON array string, e.g. `["BTCUSDT","ETHUSDT"]`.
     * `encoded = true` prevents Retrofit from double-encoding the brackets.
     */
    @GET("api/v3/ticker/24hr")
    suspend fun get24hrTickers(@Query(value = "symbols", encoded = true) symbols: String): List<BinanceTicker24hrDto>
}
