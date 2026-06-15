package com.tradingapp.network.model

import com.google.gson.annotations.SerializedName

data class CoinGeckoMarketDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("name") val name: String,
    @SerializedName("image") val image: String?,
)
