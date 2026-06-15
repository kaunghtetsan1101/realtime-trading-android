package com.tradingapp.data.repository

import com.tradingapp.common.dispatcher.DispatcherProvider
import com.tradingapp.data.provider.RoomBackedAssetMetadataProvider
import com.tradingapp.database.dao.AssetMetadataDao
import com.tradingapp.network.api.CoinGeckoApi
import com.tradingapp.network.model.CoinGeckoMarketDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AssetMetadataRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val dispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private val coinGeckoApi = mockk<CoinGeckoApi>(relaxed = true)
    private val metadataDao = mockk<AssetMetadataDao>(relaxed = true)
    private val provider = mockk<RoomBackedAssetMetadataProvider>(relaxed = true)

    private fun buildRepo() = AssetMetadataRepositoryImpl(
        coinGeckoApi,
        metadataDao,
        provider,
        dispatchers,
    )

    // -------------------------------------------------------------------------
    // Cache warm-up — always refreshes provider on syncIfStale
    // -------------------------------------------------------------------------

    @Test
    fun `syncIfStale always calls provider refresh to warm in-memory cache`() = runTest(testDispatcher) {
        coEvery { metadataDao.getLatestUpdateTime() } returns null
        coEvery { coinGeckoApi.getMarkets() } returns emptyList()
        buildRepo().syncIfStale()
        coVerify(atLeast = 1) { provider.refresh() }
    }

    // -------------------------------------------------------------------------
    // Staleness detection — fetch from CoinGecko
    // -------------------------------------------------------------------------

    @Test
    fun `syncIfStale fetches from CoinGecko when table is empty`() = runTest(testDispatcher) {
        coEvery { metadataDao.getLatestUpdateTime() } returns null
        coEvery { coinGeckoApi.getMarkets() } returns emptyList()
        buildRepo().syncIfStale()
        coVerify { coinGeckoApi.getMarkets() }
    }

    @Test
    fun `syncIfStale fetches from CoinGecko when data is older than 24h`() = runTest(testDispatcher) {
        val staleTime = System.currentTimeMillis() - (25L * 60 * 60 * 1000) // 25h ago
        coEvery { metadataDao.getLatestUpdateTime() } returns staleTime
        coEvery { coinGeckoApi.getMarkets() } returns emptyList()
        buildRepo().syncIfStale()
        coVerify { coinGeckoApi.getMarkets() }
    }

    @Test
    fun `syncIfStale skips CoinGecko fetch when data is fresh`() = runTest(testDispatcher) {
        val freshTime = System.currentTimeMillis() - (1L * 60 * 60 * 1000) // 1h ago
        coEvery { metadataDao.getLatestUpdateTime() } returns freshTime
        buildRepo().syncIfStale()
        coVerify(exactly = 0) { coinGeckoApi.getMarkets() }
    }

    // -------------------------------------------------------------------------
    // Successful sync — data saved and cache refreshed
    // -------------------------------------------------------------------------

    @Test
    fun `syncIfStale saves CoinGecko DTOs to Room`() = runTest(testDispatcher) {
        coEvery { metadataDao.getLatestUpdateTime() } returns null
        coEvery { coinGeckoApi.getMarkets() } returns listOf(
            CoinGeckoMarketDto("btc", "Bitcoin", "https://example.com/btc.png"),
            CoinGeckoMarketDto("eth", "Ethereum", "https://example.com/eth.png"),
        )
        buildRepo().syncIfStale()
        coVerify { metadataDao.upsertAll(any()) }
    }

    @Test
    fun `syncIfStale refreshes provider cache after successful sync`() = runTest(testDispatcher) {
        coEvery { metadataDao.getLatestUpdateTime() } returns null
        coEvery { coinGeckoApi.getMarkets() } returns listOf(
            CoinGeckoMarketDto("btc", "Bitcoin", null),
        )
        buildRepo().syncIfStale()
        // refresh should be called at least twice: once to warm, once after sync
        coVerify(atLeast = 2) { provider.refresh() }
    }

    // -------------------------------------------------------------------------
    // Error handling — CoinGecko failure is non-fatal
    // -------------------------------------------------------------------------

    @Test
    fun `syncIfStale does not throw when CoinGecko request fails`() = runTest(testDispatcher) {
        coEvery { metadataDao.getLatestUpdateTime() } returns null
        coEvery { coinGeckoApi.getMarkets() } throws IOException("Network error")
        // Should complete without throwing
        buildRepo().syncIfStale()
    }

    @Test
    fun `syncIfStale does not persist anything when CoinGecko fails`() = runTest(testDispatcher) {
        coEvery { metadataDao.getLatestUpdateTime() } returns null
        coEvery { coinGeckoApi.getMarkets() } throws IOException("Network error")
        buildRepo().syncIfStale()
        coVerify(exactly = 0) { metadataDao.upsertAll(any()) }
    }

    @Test
    fun `syncIfStale still warms cache from Room when CoinGecko fails`() = runTest(testDispatcher) {
        coEvery { metadataDao.getLatestUpdateTime() } returns null
        coEvery { coinGeckoApi.getMarkets() } throws IOException("Network error")
        buildRepo().syncIfStale()
        // First refresh (warm from Room) must still happen even on CoinGecko failure
        coVerify(atLeast = 1) { provider.refresh() }
    }

    // -------------------------------------------------------------------------
    // observeMetadata — delegates to DAO
    // -------------------------------------------------------------------------

    @Test
    fun `observeMetadata delegates to DAO for the given symbol`() {
        every { metadataDao.observeBySymbol("BTC") } returns emptyFlow()
        buildRepo().observeMetadata("BTC")
        coVerify { metadataDao.observeBySymbol("BTC") }
    }
}
