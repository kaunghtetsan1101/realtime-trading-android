package com.tradingapp.domain.usecase

import app.cash.turbine.test
import com.tradingapp.domain.model.Asset
import com.tradingapp.domain.model.Position
import com.tradingapp.domain.repository.AssetRepository
import com.tradingapp.domain.repository.TradeRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetPortfolioUseCaseTest {

    private val tradeRepository: TradeRepository = mockk()
    private val assetRepository: AssetRepository = mockk()
    private val useCase = GetPortfolioUseCase(tradeRepository, assetRepository)

    // -------------------------------------------------------------------------
    // Empty portfolio
    // -------------------------------------------------------------------------

    @Test
    fun `empty positions emits portfolio with only cash balance`() = runTest {
        every { tradeRepository.observePositions() } returns flowOf(emptyList())
        every { assetRepository.observeAssets() } returns flowOf(emptyList())
        every { tradeRepository.observeCashBalance() } returns flowOf(10_000.0)

        useCase().test {
            val portfolio = awaitItem()
            assertEquals(10_000.0, portfolio.cashBalance, 0.001)
            assertEquals(10_000.0, portfolio.totalValue, 0.001)
            assertEquals(0.0, portfolio.totalUnrealizedPnL, 0.001)
            assertEquals(0.0, portfolio.totalUnrealizedPnLPct, 0.001)
            assertTrue(portfolio.positions.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -------------------------------------------------------------------------
    // P&L calculations
    // -------------------------------------------------------------------------

    @Test
    fun `position with higher live price shows positive unrealised PnL`() = runTest {
        every { tradeRepository.observePositions() } returns flowOf(
            listOf(Position("BTC", quantity = 1.0, averagePrice = 50_000.0)),
        )
        every { assetRepository.observeAssets() } returns flowOf(
            listOf(fakeAsset("BTC", price = 60_000.0)),
        )
        every { tradeRepository.observeCashBalance() } returns flowOf(0.0)

        useCase().test {
            val portfolio = awaitItem()
            val btc = portfolio.positions.first()
            assertEquals(60_000.0, btc.currentPrice, 0.001)
            assertEquals(60_000.0, btc.totalValue, 0.001)
            assertEquals(10_000.0, btc.unrealizedPnL, 0.001) // (60k - 50k) × 1
            assertEquals(20.0, btc.unrealizedPnLPct, 0.001) // 10k / 50k × 100
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `position with lower live price shows negative unrealised PnL`() = runTest {
        every { tradeRepository.observePositions() } returns flowOf(
            listOf(Position("ETH", quantity = 2.0, averagePrice = 3_000.0)),
        )
        every { assetRepository.observeAssets() } returns flowOf(
            listOf(fakeAsset("ETH", price = 2_500.0)),
        )
        every { tradeRepository.observeCashBalance() } returns flowOf(0.0)

        useCase().test {
            val portfolio = awaitItem()
            val eth = portfolio.positions.first()
            assertEquals(-1_000.0, eth.unrealizedPnL, 0.001) // (2500 - 3000) × 2
            assertEquals(-16.666, eth.unrealizedPnLPct, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `missing live price falls back to average price giving zero PnL`() = runTest {
        every { tradeRepository.observePositions() } returns flowOf(
            listOf(Position("SOL", quantity = 10.0, averagePrice = 100.0)),
        )
        every { assetRepository.observeAssets() } returns flowOf(emptyList()) // no live price
        every { tradeRepository.observeCashBalance() } returns flowOf(0.0)

        useCase().test {
            val portfolio = awaitItem()
            val sol = portfolio.positions.first()
            assertEquals(100.0, sol.currentPrice, 0.001) // falls back to avgPrice
            assertEquals(0.0, sol.unrealizedPnL, 0.001)
            assertEquals(0.0, sol.unrealizedPnLPct, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `total value equals cashBalance plus all position values`() = runTest {
        every { tradeRepository.observePositions() } returns flowOf(
            listOf(
                Position("BTC", quantity = 0.5, averagePrice = 50_000.0),
                Position("ETH", quantity = 2.0, averagePrice = 2_000.0),
            ),
        )
        every { assetRepository.observeAssets() } returns flowOf(
            listOf(
                fakeAsset("BTC", price = 60_000.0), // 0.5 × 60_000 = 30_000
                fakeAsset("ETH", price = 3_000.0), // 2.0 × 3_000  =  6_000
            ),
        )
        every { tradeRepository.observeCashBalance() } returns flowOf(4_000.0)

        useCase().test {
            val portfolio = awaitItem()
            assertEquals(40_000.0, portfolio.totalValue, 0.001) // 4_000 + 30_000 + 6_000
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `total unrealised PnL aggregates across all positions`() = runTest {
        every { tradeRepository.observePositions() } returns flowOf(
            listOf(
                Position("BTC", quantity = 1.0, averagePrice = 50_000.0),
                Position("ETH", quantity = 1.0, averagePrice = 3_000.0),
            ),
        )
        every { assetRepository.observeAssets() } returns flowOf(
            listOf(
                fakeAsset("BTC", price = 55_000.0), // PnL = +5_000
                fakeAsset("ETH", price = 2_500.0), // PnL = −500
            ),
        )
        every { tradeRepository.observeCashBalance() } returns flowOf(0.0)

        useCase().test {
            val portfolio = awaitItem()
            assertEquals(4_500.0, portfolio.totalUnrealizedPnL, 0.001) // 5_000 − 500
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `PnL percentage is zero when cost basis is zero`() = runTest {
        every { tradeRepository.observePositions() } returns flowOf(
            listOf(Position("BTC", quantity = 1.0, averagePrice = 0.0)),
        )
        every { assetRepository.observeAssets() } returns flowOf(
            listOf(fakeAsset("BTC", price = 60_000.0)),
        )
        every { tradeRepository.observeCashBalance() } returns flowOf(0.0)

        useCase().test {
            val portfolio = awaitItem()
            assertEquals(0.0, portfolio.totalUnrealizedPnLPct, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `new price emission triggers updated portfolio`() = runTest {
        val priceFlow = MutableStateFlow(listOf(fakeAsset("BTC", price = 50_000.0)))
        every { tradeRepository.observePositions() } returns flowOf(
            listOf(Position("BTC", quantity = 1.0, averagePrice = 45_000.0)),
        )
        every { assetRepository.observeAssets() } returns priceFlow
        every { tradeRepository.observeCashBalance() } returns flowOf(0.0)

        useCase().test {
            val first = awaitItem()
            assertEquals(5_000.0, first.positions.first().unrealizedPnL, 0.001)

            priceFlow.value = listOf(fakeAsset("BTC", price = 55_000.0))

            val second = awaitItem()
            assertEquals(10_000.0, second.positions.first().unrealizedPnL, 0.001)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun assertTrue(value: Boolean) = org.junit.Assert.assertTrue(value)

    private fun fakeAsset(symbol: String, price: Double) = Asset(
        symbol = symbol,
        name = symbol,
        currentPrice = price,
        priceChange24h = 0.0,
        priceChangePct24h = 0.0,
        marketCap = 0.0,
        volume24h = 0.0,
        logoUrl = null,
        isFavorite = false,
        lastUpdated = 0L,
    )
}
