package com.tradingapp.network.api

import com.tradingapp.network.model.CoinGeckoMarketDto
import retrofit2.http.GET
import retrofit2.http.Query

interface CoinGeckoApi {

    @GET("coins/markets")
    suspend fun getMarkets(
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 250,
        @Query("page") page: Int = 1,
    ): List<CoinGeckoMarketDto>
}
