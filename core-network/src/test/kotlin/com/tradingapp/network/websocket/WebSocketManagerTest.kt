package com.tradingapp.network.websocket

import app.cash.turbine.test
import com.google.gson.GsonBuilder
import com.tradingapp.network.model.PriceTickDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.Assert.assertEquals
import org.junit.Test

class WebSocketManagerTest {
    private val gson = GsonBuilder().create()
    private val mockWebSocket = mockk<WebSocket>(relaxed = true)
    private val mockClient = mockk<OkHttpClient>()
    private val listenerSlot = slot<WebSocketListener>()

    private val manager =
        WebSocketManager(mockClient, gson).also {
            every { mockClient.newWebSocket(any(), capture(listenerSlot)) } returns mockWebSocket
        }

    @Test
    fun `valid Binance miniTicker frame emits PriceTickDto with USDT stripped`() = runTest {
        val json =
            """
                {
                  "stream": "btcusdt@miniTicker",
                  "data": {
                    "s": "BTCUSDT",
                    "c": "67500.50",
                    "o": "66000.00",
                    "v": "1234.5",
                    "q": "83000000.0",
                    "E": 1716700000000
                  }
                }
            """.trimIndent()

        manager.observePriceTicks("wss://fake").test {
            listenerSlot.captured.onMessage(mockWebSocket, json)

            val tick: PriceTickDto = awaitItem()
            assertEquals("BTC", tick.symbol)
            assertEquals(67500.50, tick.price, 0.001)
            assertEquals(1716700000000L, tick.timestamp)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `malformed JSON frame does not crash and emits nothing`() = runTest {
        manager.observePriceTicks("wss://fake").test {
            listenerSlot.captured.onMessage(mockWebSocket, "{{not valid json}}")

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `missing closePrice field does not crash and emits nothing`() = runTest {
        // "c" is absent — toDoubleOrNull on null returns null → return
        val json =
            """{"stream":"ethusdt@miniTicker","data":""" +
                """{"s":"ETHUSDT","o":"3000.0","v":"500","q":"1500000","E":1716700000000}}"""

        manager.observePriceTicks("wss://fake").test {
            listenerSlot.captured.onMessage(mockWebSocket, json)

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `non-USDT suffix symbol is emitted with original symbol unchanged`() = runTest {
        val json =
            """
                {
                  "stream": "solusdt@miniTicker",
                  "data": {
                    "s": "SOLUSDT",
                    "c": "175.0",
                    "o": "170.0",
                    "v": "50000",
                    "q": "8750000.0",
                    "E": 1716700000001
                  }
                }
            """.trimIndent()

        manager.observePriceTicks("wss://fake").test {
            listenerSlot.captured.onMessage(mockWebSocket, json)

            val tick = awaitItem()
            assertEquals("SOL", tick.symbol)
            assertEquals(175.0, tick.price, 0.001)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WebSocket failure closes the flow with the error`() = runTest {
        val error = RuntimeException("connection reset")

        manager.observePriceTicks("wss://fake").test {
            listenerSlot.captured.onFailure(mockWebSocket, error, null as Response?)

            val exception = awaitError()
            assertEquals(error.message, exception.message)
        }
    }
}
