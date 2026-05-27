package com.tradingapp.marketdetail

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Embeds the TradingView Advanced Chart Widget inside a Compose layout via [AndroidView].
 *
 * The HTML is generated as a Kotlin string and loaded via [WebView.loadDataWithBaseURL] —
 * no asset file is required. When [symbol] or [darkTheme] changes, the update lambda
 * reloads the content.
 *
 * Widget: embed-widget-advanced-chart.js — free, no API key required.
 * Requires the INTERNET permission (already declared in the app manifest).
 *
 * Symbol mapping: bare symbol (e.g. "BTC") → TradingView symbol "BINANCE:BTCUSDT".
 */
@Composable
fun TradingViewChart(symbol: String, darkTheme: Boolean, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                // MATCH_PARENT is required — WebView defaults to WRAP_CONTENT and ignores
                // the height that Compose allocates via the modifier.
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                loadDataWithBaseURL(null, buildChartHtml(symbol, darkTheme), "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, buildChartHtml(symbol, darkTheme), "text/html", "UTF-8", null)
        },
        modifier = modifier,
    )
}

private fun buildChartHtml(symbol: String, darkTheme: Boolean): String {
    val tvSymbol = "BINANCE:${symbol}USDT"
    val theme = if (darkTheme) "dark" else "light"
    return """
        <!DOCTYPE html>
        <html>
          <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
              html, body { margin: 0; padding: 0; height: 100%; background: transparent; }
              .tradingview-widget-container { width: 100%; height: 100%; }
            </style>
          </head>
          <body>
            <div class="tradingview-widget-container">
              <div id="tradingview_chart"></div>
              <script type="text/javascript"
                src="https://s3.tradingview.com/external-embedding/embed-widget-advanced-chart.js"
                async>
              {
                "autosize": true,
                "symbol": "$tvSymbol",
                "interval": "D",
                "timezone": "Etc/UTC",
                "theme": "$theme",
                "style": "1",
                "locale": "en",
                "enable_publishing": false,
                "allow_symbol_change": false,
                "container_id": "tradingview_chart"
              }
              </script>
            </div>
          </body>
        </html>
    """.trimIndent()
}
