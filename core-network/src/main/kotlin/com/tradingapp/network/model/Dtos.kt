package com.tradingapp.network.model

import com.google.gson.annotations.SerializedName

data class PriceTickDto(
    @SerializedName("symbol")    val symbol: String,
    @SerializedName("price")     val price: Double,
    @SerializedName("timestamp") val timestamp: Long,
)

/** REST response: GET api/v3/ticker/24hr */
data class BinanceTicker24hrDto(
    @SerializedName("symbol")             val symbol: String,
    @SerializedName("lastPrice")          val lastPrice: String,
    @SerializedName("priceChange")        val priceChange: String,
    @SerializedName("priceChangePercent") val priceChangePercent: String,
    @SerializedName("highPrice")          val highPrice: String,
    @SerializedName("lowPrice")           val lowPrice: String,
    @SerializedName("volume")             val volume: String,
    @SerializedName("quoteVolume")        val quoteVolume: String,
)

/** WebSocket inner data object for combined stream miniTicker frames */
data class BinanceMiniTickerData(
    @SerializedName("s") val symbol: String,
    @SerializedName("c") val closePrice: String,
    @SerializedName("o") val openPrice: String,
    @SerializedName("v") val volume: String,
    @SerializedName("q") val quoteVolume: String,
    @SerializedName("E") val eventTime: Long,
)

/** WebSocket outer envelope: {"stream":"btcusdt@miniTicker","data":{...}} */
data class BinanceStreamMessage(
    @SerializedName("stream") val stream: String,
    @SerializedName("data")   val data: BinanceMiniTickerData,
)
