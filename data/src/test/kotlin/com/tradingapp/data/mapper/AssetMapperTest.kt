package com.tradingapp.data.mapper

import com.tradingapp.data.provider.StaticAssetMetadataProvider
import com.tradingapp.network.model.BinanceTicker24hrDto
import org.junit.Assert.assertEquals
import org.junit.Test

class AssetMapperTest {

    private val provider = StaticAssetMetadataProvider()

    private fun dto(
        symbol: String = "BTCUSDT",
        lastPrice: String = "67500.0",
        priceChange: String = "500.0",
        priceChangePercent: String = "0.75",
        highPrice: String = "68000.0",
        lowPrice: String = "67000.0",
        volume: String = "1000.0",
        quoteVolume: String = "67500000.0",
    ) = BinanceTicker24hrDto(
        symbol,
        lastPrice,
        priceChange,
        priceChangePercent,
        highPrice,
        lowPrice,
        volume,
        quoteVolume,
    )

    @Test
    fun `toEntity strips USDT suffix from symbol`() {
        val entity = dto(symbol = "BTCUSDT").toEntity(provider)
        assertEquals("BTC", entity.symbol)
    }

    @Test
    fun `toEntity maps BTC to Bitcoin`() {
        val entity = dto(symbol = "BTCUSDT").toEntity(provider)
        assertEquals("Bitcoin", entity.name)
    }

    @Test
    fun `toEntity maps ETH to Ethereum`() {
        val entity = dto(symbol = "ETHUSDT").toEntity(provider)
        assertEquals("Ethereum", entity.name)
    }

    @Test
    fun `toEntity maps SOL to Solana`() {
        val entity = dto(symbol = "SOLUSDT").toEntity(provider)
        assertEquals("Solana", entity.name)
    }

    @Test
    fun `toEntity maps ADA to Cardano`() {
        val entity = dto(symbol = "ADAUSDT").toEntity(provider)
        assertEquals("Cardano", entity.name)
    }

    @Test
    fun `toEntity falls back to bare symbol as name for unknown coins`() {
        val entity = dto(symbol = "XYZUSDT").toEntity(provider)
        assertEquals("XYZ", entity.symbol)
        assertEquals("XYZ", entity.name)
    }

    @Test
    fun `toEntity parses all string fields to Double`() {
        val entity =
            dto(
                lastPrice = "67500.12",
                priceChange = "500.50",
                priceChangePercent = "0.75",
                volume = "1234.56",
                quoteVolume = "83456789.0",
            ).toEntity(provider)

        assertEquals(67500.12, entity.price, 0.001)
        assertEquals(500.50, entity.change24h, 0.001)
        assertEquals(0.75, entity.changePct24h, 0.001)
        assertEquals(1234.56, entity.volume24h, 0.001)
        assertEquals(83456789.0, entity.marketCap, 0.001)
    }

    @Test
    fun `toEntity uses quoteVolume as marketCap proxy`() {
        val entity = dto(quoteVolume = "99000000.0").toEntity(provider)
        assertEquals(99_000_000.0, entity.marketCap, 0.001)
    }

    @Test
    fun `toEntity derives logoUrl from CoinCap CDN with lowercase symbol`() {
        val entity = dto(symbol = "BTCUSDT").toEntity(provider)
        assertEquals("https://assets.coincap.io/assets/icons/btc@2x.png", entity.logoUrl)
    }

    @Test
    fun `toEntity maps BNB to BNB`() {
        val entity = dto(symbol = "BNBUSDT").toEntity(provider)
        assertEquals("BNB", entity.name)
    }

    @Test
    fun `toEntity maps XRP to XRP`() {
        val entity = dto(symbol = "XRPUSDT").toEntity(provider)
        assertEquals("XRP", entity.name)
    }

    @Test
    fun `toEntity maps DOGE to Dogecoin`() {
        val entity = dto(symbol = "DOGEUSDT").toEntity(provider)
        assertEquals("Dogecoin", entity.name)
    }

    @Test
    fun `toEntity maps MATIC to Polygon`() {
        val entity = dto(symbol = "MATICUSDT").toEntity(provider)
        assertEquals("Polygon", entity.name)
    }

    @Test
    fun `toEntity maps DOT to Polkadot`() {
        val entity = dto(symbol = "DOTUSDT").toEntity(provider)
        assertEquals("Polkadot", entity.name)
    }

    @Test
    fun `toEntity gracefully handles non-numeric string with zero fallback`() {
        val entity = dto(lastPrice = "N/A", priceChange = "", quoteVolume = "—").toEntity(provider)
        assertEquals(0.0, entity.price, 0.0)
        assertEquals(0.0, entity.change24h, 0.0)
        assertEquals(0.0, entity.marketCap, 0.0)
    }
}
