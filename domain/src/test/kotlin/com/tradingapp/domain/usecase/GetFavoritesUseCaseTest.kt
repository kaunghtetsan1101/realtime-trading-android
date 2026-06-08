package com.tradingapp.domain.usecase

import app.cash.turbine.test
import com.tradingapp.common.result.Result
import com.tradingapp.domain.model.Asset
import com.tradingapp.domain.repository.AssetRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetFavoritesUseCaseTest {
    private val repository = mockk<AssetRepository>()
    private val useCase = GetFavoritesUseCase(repository)

    @Test
    fun `emits Success with favourited assets`() = runTest {
        val favorites = listOf(fakeAsset("BTC"), fakeAsset("ETH"))
        every { repository.observeFavorites() } returns flowOf(favorites)

        useCase().test {
            awaitItem() // Result.Loading from asResult()
            val item = awaitItem()
            assertTrue(item is Result.Success)
            assertEquals(favorites, (item as Result.Success).data)
            awaitComplete()
        }
    }

    @Test
    fun `emits Error when repository throws`() = runTest {
        every { repository.observeFavorites() } returns kotlinx.coroutines.flow.flow {
            throw IllegalStateException("DB error")
        }

        useCase().test {
            awaitItem() // Result.Loading
            val item = awaitItem()
            assertTrue(item is Result.Error)
            assertEquals("DB error", (item as Result.Error).exception.message)
            awaitComplete()
        }
    }

    private fun fakeAsset(symbol: String) = Asset(
        symbol = symbol,
        name = symbol,
        currentPrice = 100.0,
        priceChange24h = 1.0,
        priceChangePct24h = 1.0,
        marketCap = 1_000_000.0,
        volume24h = 100_000.0,
        logoUrl = null,
        isFavorite = true,
        lastUpdated = 0L,
    )
}
