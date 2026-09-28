package com.mazkiplay.trade.ui.widgets

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mazkiplay.trade.data.tradingview.TvTickers

/**
 * TradingView's own embeddable widgets, rendered inside a WebView.
 *
 * Numeric prices in the app come from the scanner endpoint, but **nothing** can
 * reproduce a true tick-by-tick chart except TradingView's own charting engine, so the
 * chart, the recommendation gauge, the economic calendar, the news timeline and the
 * ticker tape are all embedded here as the official widgets.
 *
 * Two details make these work reliably on Android:
 *  - `loadDataWithBaseURL` is given `https://www.tradingview.com/` as its base URL, so
 *    the page gets a real https origin and the widgets' DOM storage is not blocked.
 *  - JavaScript and DOM storage are enabled explicitly; without storage the chart
 *    loses its layout between recompositions.
 */

private const val TV_ORIGIN = "https://www.tradingview.com/"

/** Wraps TradingView's widget config in the minimum viable document. */
private fun widgetDocument(scriptSrc: String, configJson: String, background: String): String = """
    <!DOCTYPE html>
    <html>
      <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
        <style>
          html, body { margin: 0; padding: 0; background: $background; overflow: hidden; }
          .tv-wrap { width: 100%; height: 100%; }
          .tradingview-widget-container { width: 100%; height: 100%; }
          .tradingview-widget-copyright { display: none !important; }
        </style>
      </head>
      <body>
        <div class="tv-wrap">
          <div class="tradingview-widget-container">
            <div class="tradingview-widget-container__widget"></div>
            <script type="text/javascript" src="https://s3.tradingview.com/external-embedding/$scriptSrc" async>
            $configJson
            </script>
          </div>
        </div>
      </body>
    </html>
""".trimIndent()

/**
 * Renders a TradingView widget, with a loading indicator until the first frame and an
 * explicit offline panel if the widget cannot be reached.
 *
 * @param cacheKey identity of the current configuration; changing it rebuilds the page,
 *   which is how symbol/timeframe switches take effect inside a single WebView.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TvWidgetView(
    scriptSrc: String,
    configJson: String,
    cacheKey: String,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val background = if (darkTheme) "#0B1220" else "#FFFFFF"
    val html = remember(scriptSrc, configJson, darkTheme) {
        widgetDocument(scriptSrc, configJson, background)
    }

    var loading by remember(cacheKey) { mutableStateOf(true) }
    var failed by remember(cacheKey) { mutableStateOf(false) }

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = false
                        displayZoomControls = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            loading = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            // Only a failed *main frame* means the widget is unreachable;
                            // sub-resource failures are routine and must not blank the panel.
                            if (request?.isForMainFrame == true) {
                                loading = false
                                failed = true
                            }
                        }
                    }
                }
            },
            update = { webView ->
                val current = webView.tag as? String
                if (current != cacheKey) {
                    webView.tag = cacheKey
                    loading = true
                    failed = false
                    webView.loadDataWithBaseURL(TV_ORIGIN, html, "text/html", "UTF-8", null)
                }
            }
        )

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Memuat widget TradingView...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (failed) {
            WidgetOffline(message = "Widget TradingView tidak dapat dimuat. Periksa koneksi internet Anda.")
        }
    }
}

/** Fallback panel shown when a widget cannot be reached. */
@Composable
fun WidgetOffline(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                Icons.Filled.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ------------------------------------------------------------------------ configs

/**
 * Builder for the TradingView widget configuration blocks.
 *
 * Kept as small JSON fragments rather than a serialiser so the exact key names the
 * widgets expect stay visible and greppable in one place.
 */
private object Cfg {
    const val CHART = "embed-widget-advanced-chart.js"
    const val GAUGE = "embed-widget-technical-analysis.js"
    const val CALENDAR = "embed-widget-events.js"
    const val TIMELINE = "embed-widget-timeline.js"
    const val TICKER = "embed-widget-ticker-tape.js"
    const val OVERVIEW = "embed-widget-market-overview.js"
    const val MINI = "embed-widget-mini-symbol-overview.js"
}

private fun themeName(dark: Boolean) = if (dark) "dark" else "light"

/** Full TradingView Advanced Real-Time Chart. */
fun advancedChartConfig(
    symbol: String,
    interval: String,
    dark: Boolean,
    studies: List<String> = listOf("STD;EMA", "STD;RSI", "STD;MACD")
): Pair<String, String> {
    val ticker = TvTickers.ticker(symbol)
    val studiesJson = studies.joinToString(",") { "\"$it\"" }
    val config = """
        {
          "autosize": true,
          "symbol": "$ticker",
          "interval": "$interval",
          "timezone": "Asia/Jakarta",
          "theme": "${themeName(dark)}",
          "style": "1",
          "locale": "id",
          "enable_publishing": false,
          "allow_symbol_change": true,
          "hide_side_toolbar": false,
          "hide_top_toolbar": false,
          "save_image": false,
          "details": false,
          "calendar": false,
          "withdateranges": true,
          "studies": [$studiesJson],
          "support_host": "https://www.tradingview.com"
        }
    """.trimIndent()
    return Cfg.CHART to config
}

/** The gauge widget: the same Buy/Sell verdict the scanner returns, rendered natively. */
fun technicalGaugeConfig(symbol: String, interval: String, dark: Boolean): Pair<String, String> {
    val config = """
        {
          "interval": "$interval",
          "width": "100%",
          "height": "100%",
          "symbol": "${TvTickers.ticker(symbol)}",
          "showIntervalTabs": true,
          "displayMode": "single",
          "locale": "id",
          "colorTheme": "${themeName(dark)}",
          "isTransparent": true
        }
    """.trimIndent()
    return Cfg.GAUGE to config
}

/** TradingView's own economic calendar, with the country filter open. */
fun economicCalendarConfig(dark: Boolean): Pair<String, String> {
    val config = """
        {
          "colorTheme": "${themeName(dark)}",
          "isTransparent": true,
          "width": "100%",
          "height": "100%",
          "locale": "id",
          "importanceFilter": "0,1",
          "countryFilter": "us,eu,gb,jp,au,ca,ch,nz,cn,de",
          "utm_source": "mazkiplay_trade"
        }
    """.trimIndent()
    return Cfg.CALENDAR to config
}

/** The live news timeline widget. */
fun newsTimelineConfig(dark: Boolean, feedMode: String = "market"): Pair<String, String> {
    val config = """
        {
          "feedMode": "$feedMode",
          "colorTheme": "${themeName(dark)}",
          "isTransparent": true,
          "displayMode": "regular",
          "width": "100%",
          "height": "100%",
          "locale": "id"
        }
    """.trimIndent()
    return Cfg.TIMELINE to config
}

/** The scrolling ticker tape, driven by the user's watchlist. */
fun tickerTapeConfig(symbols: List<String>, dark: Boolean): Pair<String, String> {
    val symbolsJson = symbols.joinToString(",") { symbol ->
        """{"proName": "${TvTickers.ticker(symbol)}", "title": "$symbol"}"""
    }
    val config = """
        {
          "symbols": [$symbolsJson],
          "showSymbolLogo": true,
          "isTransparent": true,
          "displayMode": "adaptive",
          "colorTheme": "${themeName(dark)}",
          "locale": "id"
        }
    """.trimIndent()
    return Cfg.TICKER to config
}

/** Compact market overview strip for the dashboard. */
fun marketOverviewConfig(dark: Boolean): Pair<String, String> {
    val config = """
        {
          "colorTheme": "${themeName(dark)}",
          "dateRange": "1D",
          "showChart": true,
          "locale": "id",
          "width": "100%",
          "height": "100%",
          "isTransparent": true,
          "showSymbolLogo": true,
          "showFloatingTooltip": true,
          "tabs": [
            {
              "title": "Forex",
              "symbols": [
                {"s": "OANDA:XAUUSD", "d": "Emas"},
                {"s": "OANDA:EURUSD", "d": "Euro"},
                {"s": "OANDA:GBPUSD", "d": "Pound"},
                {"s": "OANDA:USDJPY", "d": "Yen"}
              ],
              "originalTitle": "Forex"
            },
            {
              "title": "Indeks",
              "symbols": [
                {"s": "OANDA:US30USD", "d": "Dow 30"},
                {"s": "NASDAQ:NDX", "d": "Nasdaq 100"},
                {"s": "TVC:DXY", "d": "Dolar Index"}
              ],
              "originalTitle": "Indices"
            },
            {
              "title": "Kripto",
              "symbols": [
                {"s": "BITSTAMP:BTCUSD", "d": "Bitcoin"},
                {"s": "BITSTAMP:ETHUSD", "d": "Ethereum"}
              ],
              "originalTitle": "Crypto"
            }
          ]
        }
    """.trimIndent()
    return Cfg.OVERVIEW to config
}

/** A tiny sparkline chart, used inside list rows. */
fun miniChartConfig(symbol: String, dark: Boolean): Pair<String, String> {
    val config = """
        {
          "symbol": "${TvTickers.ticker(symbol)}",
          "width": "100%",
          "height": "100%",
          "locale": "id",
          "dateRange": "1D",
          "colorTheme": "${themeName(dark)}",
          "isTransparent": true,
          "autosize": true,
          "largeChartUrl": "",
          "noTimeScale": true,
          "chartOnly": true
        }
    """.trimIndent()
    return Cfg.MINI to config
}

/** Helper so screens can render any config pair without repeating the call shape. */
@Composable
fun TradingViewWidget(
    config: Pair<String, String>,
    cacheKey: String,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    TvWidgetView(
        scriptSrc = config.first,
        configJson = config.second,
        cacheKey = "$cacheKey|${config.first}",
        darkTheme = darkTheme,
        modifier = modifier
    )
}

/** Convenience: a fixed-height widget strip. */
@Composable
fun TradingViewStrip(
    config: Pair<String, String>,
    cacheKey: String,
    darkTheme: Boolean,
    heightDp: Int = 84,
    modifier: Modifier = Modifier
) {
    TradingViewWidget(
        config = config,
        cacheKey = cacheKey,
        darkTheme = darkTheme,
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
    )
}
