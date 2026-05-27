package com.tradingapp.network.websocket

import com.google.gson.Gson
import com.tradingapp.network.model.BinanceStreamMessage
import com.tradingapp.network.model.PriceTickDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps a Binance combined-stream WebSocket into a cold [Flow] of [PriceTickDto].
 *
 * The Flow is cold — a new WebSocket is opened for each collector, and closed
 * automatically when the collector cancels via [awaitClose].
 *
 * Symbol normalisation: Binance sends symbols as "BTCUSDT"; the "USDT" suffix is
 * stripped before emitting so the rest of the app works with bare symbols ("BTC").
 */
@Singleton
class WebSocketManager
@Inject
constructor(private val okHttpClient: OkHttpClient, private val gson: Gson) {
    fun observePriceTicks(url: String): Flow<PriceTickDto> = callbackFlow {
        val request = Request.Builder().url(url).build()

        val ws =
            okHttpClient.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        Timber.tag(TAG).d("Connected → %s", response.request.url)
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        val msg =
                            runCatching {
                                gson.fromJson(text, BinanceStreamMessage::class.java)
                            }.getOrNull() ?: return

                        val data = msg.data
                        val price = data.closePrice?.toDoubleOrNull() ?: return
                        val bareSymbol = data.symbol.removeSuffix("USDT")

                        Timber.tag(TAG).v("%s → %.2f", bareSymbol, price)

                        trySend(
                            PriceTickDto(
                                symbol = bareSymbol,
                                price = price,
                                timestamp = data.eventTime,
                            ),
                        )
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        Timber.tag(TAG).e(t, "WebSocket failure")
                        close(t)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        Timber.tag(TAG).d("Closed (code=%d reason=%s)", code, reason)
                    }
                },
            )

        awaitClose { ws.close(NORMAL_CLOSURE_CODE, "Flow collector cancelled") }
    }

    companion object {
        private const val TAG = "WebSocketManager"
        private const val NORMAL_CLOSURE_CODE = 1000
    }
}
