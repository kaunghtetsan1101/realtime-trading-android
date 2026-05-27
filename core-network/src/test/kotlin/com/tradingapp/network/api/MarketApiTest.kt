package com.tradingapp.network.api

import com.google.gson.GsonBuilder
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * HTTP-layer tests for [MarketApi].
 *
 * Uses [MockWebServer] to run a real HTTP server in-process, verifying that:
 * - Retrofit correctly serialises query parameters
 * - Gson correctly deserialises the response bodies
 * - HTTP method and path are as the Binance API expects
 *
 * These tests do not go to the network — MockWebServer intercepts every request.
 */
class MarketApiTest {
    private val server = MockWebServer()
    private lateinit var api: MarketApi

    @Before
    fun setUp() {
        server.start()
        api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .client(OkHttpClient())
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
                .build()
                .create(MarketApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // ── getAllMiniTickers ────────────────────────────────────────────────────

    @Test
    fun `getAllMiniTickers sends GET with type=MINI query param`() = runTest {
        server.enqueue(MockResponse().setBody("[]").setResponseCode(200))

        api.getAllMiniTickers()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(
            "Expected path to contain type=MINI, was: ${request.path}",
            request.path!!.contains("type=MINI"),
        )
    }

    @Test
    fun `getAllMiniTickers parses symbol and quoteVolume correctly`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                        [
                          {"symbol":"BTCUSDT","quoteVolume":"9876543210.0"},
                          {"symbol":"ETHUSDT","quoteVolume":"1234567890.0"}
                        ]
                    """.trimIndent(),
                ),
        )

        val tickers = api.getAllMiniTickers()

        assertEquals(2, tickers.size)
        assertEquals("BTCUSDT", tickers[0].symbol)
        assertEquals("9876543210.0", tickers[0].quoteVolume)
        assertEquals("ETHUSDT", tickers[1].symbol)
    }

    @Test
    fun `getAllMiniTickers returns empty list on empty array response`() = runTest {
        server.enqueue(MockResponse().setBody("[]").setResponseCode(200))

        val tickers = api.getAllMiniTickers()

        assertTrue(tickers.isEmpty())
    }

    // ── get24hrTickers ───────────────────────────────────────────────────────

    @Test
    fun `get24hrTickers parses all ticker fields`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                        [
                          {
                            "symbol":             "BTCUSDT",
                            "lastPrice":          "67500.0",
                            "priceChange":        "500.0",
                            "priceChangePercent": "0.75",
                            "highPrice":          "68000.0",
                            "lowPrice":           "67000.0",
                            "volume":             "1234.5",
                            "quoteVolume":        "83456789.0"
                          }
                        ]
                    """.trimIndent(),
                ),
        )

        val tickers = api.get24hrTickers(symbols = "[\"BTCUSDT\"]")

        assertEquals(1, tickers.size)
        with(tickers[0]) {
            assertEquals("BTCUSDT", symbol)
            assertEquals("67500.0", lastPrice)
            assertEquals("500.0", priceChange)
            assertEquals("0.75", priceChangePercent)
            assertEquals("68000.0", highPrice)
            assertEquals("67000.0", lowPrice)
            assertEquals("1234.5", volume)
            assertEquals("83456789.0", quoteVolume)
        }
    }

    @Test
    fun `get24hrTickers sends symbols as query parameter`() = runTest {
        server.enqueue(MockResponse().setBody("[]").setResponseCode(200))

        api.get24hrTickers(symbols = "[\"BTCUSDT\",\"ETHUSDT\"]")

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(
            "Expected path to contain symbols param, was: ${request.path}",
            request.path!!.contains("symbols"),
        )
    }
}
