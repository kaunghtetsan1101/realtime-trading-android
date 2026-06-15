package com.tradingapp.data.provider

import com.tradingapp.database.dao.AssetMetadataDao
import com.tradingapp.database.entity.AssetMetadataEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomBackedAssetMetadataProviderTest {

    private val metadataDao = mockk<AssetMetadataDao>()
    private val provider = RoomBackedAssetMetadataProvider(metadataDao)

    // -------------------------------------------------------------------------
    // Before refresh — empty snapshot
    // -------------------------------------------------------------------------

    @Test
    fun `getMetadata before refresh returns symbol as displayName`() {
        val result = provider.getMetadata("BTC")
        assertEquals("BTC", result.displayName)
    }

    @Test
    fun `getMetadata before refresh returns null imageUrl`() {
        val result = provider.getMetadata("BTC")
        assertNull(result.imageUrl)
    }

    @Test
    fun `getMetadata before refresh returns the queried symbol as baseSymbol`() {
        val result = provider.getMetadata("ETH")
        assertEquals("ETH", result.baseSymbol)
    }

    // -------------------------------------------------------------------------
    // After refresh — snapshot populated from Room
    // -------------------------------------------------------------------------

    @Test
    fun `getMetadata returns displayName after refresh`() = runTest {
        coEvery { metadataDao.getAll() } returns listOf(
            entity("BTC", "Bitcoin", "https://example.com/btc.png"),
        )
        provider.refresh()
        assertEquals("Bitcoin", provider.getMetadata("BTC").displayName)
    }

    @Test
    fun `getMetadata returns imageUrl after refresh`() = runTest {
        val url = "https://coin-images.coingecko.com/coins/images/1/large/bitcoin.png"
        coEvery { metadataDao.getAll() } returns listOf(entity("BTC", "Bitcoin", url))
        provider.refresh()
        assertEquals(url, provider.getMetadata("BTC").imageUrl)
    }

    @Test
    fun `getMetadata for unknown symbol returns fallback after refresh`() = runTest {
        coEvery { metadataDao.getAll() } returns listOf(entity("BTC", "Bitcoin"))
        provider.refresh()
        val result = provider.getMetadata("UNKNOWN")
        assertEquals("UNKNOWN", result.displayName)
        assertNull(result.imageUrl)
    }

    @Test
    fun `refresh replaces previous snapshot`() = runTest {
        coEvery { metadataDao.getAll() } returns listOf(entity("BTC", "Bitcoin Old"))
        provider.refresh()
        coEvery { metadataDao.getAll() } returns listOf(entity("BTC", "Bitcoin New"))
        provider.refresh()
        assertEquals("Bitcoin New", provider.getMetadata("BTC").displayName)
    }

    @Test
    fun `refresh with empty list clears snapshot — unknown falls back`() = runTest {
        coEvery { metadataDao.getAll() } returns listOf(entity("BTC", "Bitcoin"))
        provider.refresh()
        coEvery { metadataDao.getAll() } returns emptyList()
        provider.refresh()
        assertEquals("BTC", provider.getMetadata("BTC").displayName)
        assertNull(provider.getMetadata("BTC").imageUrl)
    }

    @Test
    fun `getMetadata handles multiple assets after refresh`() = runTest {
        coEvery { metadataDao.getAll() } returns listOf(
            entity("BTC", "Bitcoin", "https://example.com/btc.png"),
            entity("ETH", "Ethereum", "https://example.com/eth.png"),
            entity("SOL", "Solana", "https://example.com/sol.png"),
        )
        provider.refresh()
        assertEquals("Bitcoin", provider.getMetadata("BTC").displayName)
        assertEquals("Ethereum", provider.getMetadata("ETH").displayName)
        assertEquals("Solana", provider.getMetadata("SOL").displayName)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun entity(symbol: String, displayName: String, imageUrl: String? = null) =
        AssetMetadataEntity(
            baseSymbol = symbol,
            displayName = displayName,
            imageUrl = imageUrl,
            lastUpdated = 1_000L,
        )
}
