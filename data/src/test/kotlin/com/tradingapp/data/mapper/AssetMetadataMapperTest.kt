package com.tradingapp.data.mapper

import com.tradingapp.database.entity.AssetMetadataEntity
import com.tradingapp.network.model.CoinGeckoMarketDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssetMetadataMapperTest {

    // -------------------------------------------------------------------------
    // CoinGeckoMarketDto → AssetMetadataEntity
    // -------------------------------------------------------------------------

    @Test
    fun `toEntity uppercases symbol from CoinGecko`() {
        val entity = CoinGeckoMarketDto("btc", "Bitcoin", null).toEntity(timestamp = 1000L)
        assertEquals("BTC", entity.baseSymbol)
    }

    @Test
    fun `toEntity uppercases multi-character symbol`() {
        val entity = CoinGeckoMarketDto("sol", "Solana", null).toEntity(timestamp = 0L)
        assertEquals("SOL", entity.baseSymbol)
    }

    @Test
    fun `toEntity preserves display name`() {
        val entity = CoinGeckoMarketDto("eth", "Ethereum", "https://example.com/eth.png").toEntity(1000L)
        assertEquals("Ethereum", entity.displayName)
    }

    @Test
    fun `toEntity stores image URL when present`() {
        val url = "https://coin-images.coingecko.com/coins/images/1/large/bitcoin.png"
        val entity = CoinGeckoMarketDto("btc", "Bitcoin", url).toEntity(1000L)
        assertEquals(url, entity.imageUrl)
    }

    @Test
    fun `toEntity stores null image URL when absent`() {
        val entity = CoinGeckoMarketDto("btc", "Bitcoin", null).toEntity(1000L)
        assertNull(entity.imageUrl)
    }

    @Test
    fun `toEntity records the provided timestamp`() {
        val ts = 1_700_000_000_000L
        val entity = CoinGeckoMarketDto("btc", "Bitcoin", null).toEntity(ts)
        assertEquals(ts, entity.lastUpdated)
    }

    // -------------------------------------------------------------------------
    // AssetMetadataEntity → AssetMetadata (domain)
    // -------------------------------------------------------------------------

    @Test
    fun `toDomain preserves baseSymbol`() {
        val entity = entity("BTC")
        assertEquals("BTC", entity.toDomain().baseSymbol)
    }

    @Test
    fun `toDomain preserves displayName`() {
        val entity = entity("BTC", displayName = "Bitcoin")
        assertEquals("Bitcoin", entity.toDomain().displayName)
    }

    @Test
    fun `toDomain preserves imageUrl`() {
        val url = "https://example.com/logo.png"
        val entity = entity("BTC", imageUrl = url)
        assertEquals(url, entity.toDomain().imageUrl)
    }

    @Test
    fun `toDomain preserves null imageUrl`() {
        val entity = entity("UNKNOWN", imageUrl = null)
        assertNull(entity.toDomain().imageUrl)
    }

    @Test
    fun `toDomain preserves lastUpdated timestamp`() {
        val ts = 1_234_567_890L
        val entity = entity("BTC", lastUpdated = ts)
        assertEquals(ts, entity.toDomain().lastUpdated)
    }

    // -------------------------------------------------------------------------
    // Symbol parser — Binance suffix stripping
    // -------------------------------------------------------------------------

    @Test
    fun `BTCUSDT suffix stripped to BTC`() {
        assertEquals("BTC", "BTCUSDT".removeSuffix("USDT"))
    }

    @Test
    fun `ETHUSDT suffix stripped to ETH`() {
        assertEquals("ETH", "ETHUSDT".removeSuffix("USDT"))
    }

    @Test
    fun `SOLUSDT suffix stripped to SOL`() {
        assertEquals("SOL", "SOLUSDT".removeSuffix("USDT"))
    }

    @Test
    fun `symbol without USDT suffix is unchanged`() {
        assertEquals("UNKNOWN", "UNKNOWN".removeSuffix("USDT"))
    }

    @Test
    fun `bare symbol already stripped is returned as-is`() {
        assertEquals("BTC", "BTC".removeSuffix("USDT"))
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun entity(
        symbol: String,
        displayName: String = symbol,
        imageUrl: String? = null,
        lastUpdated: Long = 0L,
    ) = AssetMetadataEntity(
        baseSymbol = symbol,
        displayName = displayName,
        imageUrl = imageUrl,
        lastUpdated = lastUpdated,
    )
}
