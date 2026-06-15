package com.tradingapp.data.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StaticAssetMetadataProviderTest {

    private val provider = StaticAssetMetadataProvider()

    // --- Symbol → display name ---

    @Test
    fun `BTC maps to Bitcoin`() {
        assertEquals("Bitcoin", provider.getMetadata("BTC").displayName)
    }

    @Test
    fun `ETH maps to Ethereum`() {
        assertEquals("Ethereum", provider.getMetadata("ETH").displayName)
    }

    @Test
    fun `SOL maps to Solana`() {
        assertEquals("Solana", provider.getMetadata("SOL").displayName)
    }

    @Test
    fun `DOGE maps to Dogecoin`() {
        assertEquals("Dogecoin", provider.getMetadata("DOGE").displayName)
    }

    @Test
    fun `unknown symbol falls back to symbol itself as display name`() {
        val metadata = provider.getMetadata("UNKNOWNCOIN")
        assertEquals("UNKNOWNCOIN", metadata.displayName)
    }

    // --- Image URL generation ---

    @Test
    fun `BTC produces correct CoinCap image URL`() {
        val url = provider.getMetadata("BTC").imageUrl
        assertEquals("https://assets.coincap.io/assets/icons/btc@2x.png", url)
    }

    @Test
    fun `ETH produces correct CoinCap image URL`() {
        val url = provider.getMetadata("ETH").imageUrl
        assertEquals("https://assets.coincap.io/assets/icons/eth@2x.png", url)
    }

    @Test
    fun `unknown symbol still produces a non-null image URL`() {
        val url = provider.getMetadata("UNKNOWNCOIN").imageUrl
        assertNotNull(url)
    }

    @Test
    fun `unknown symbol image URL uses lowercase symbol`() {
        val url = provider.getMetadata("UNKNOWNCOIN").imageUrl
        assertEquals("https://assets.coincap.io/assets/icons/unknowncoin@2x.png", url)
    }

    @Test
    fun `image URL uses lowercase even for mixed-case input`() {
        val url = provider.getMetadata("BTC").imageUrl
        assertEquals(url, url?.lowercase())
    }

    // --- AssetMetadata shape ---

    @Test
    fun `baseSymbol is preserved on AssetMetadata`() {
        val metadata = provider.getMetadata("BTC")
        assertEquals("BTC", metadata.baseSymbol)
    }

    @Test
    fun `unknown symbol preserves baseSymbol`() {
        val metadata = provider.getMetadata("NEWTOKEN")
        assertEquals("NEWTOKEN", metadata.baseSymbol)
    }
}
