package com.mazkiplay.trade.data.tradingview

import com.mazkiplay.trade.data.model.Instrument
import com.mazkiplay.trade.data.model.Instruments
import com.mazkiplay.trade.data.model.Timeframe

/**
 * Maps the app's instrument symbols to the exact TradingView tickers that resolve on
 * the public scanner endpoint.
 *
 * The exchange prefixes are **not** interchangeable per asset class \u2014 gold lives on
 * OANDA, silver only exists as `TVC:SILVER`, crude only as `NYMEX:CL1!`, the Dow on
 * `OANDA:US30USD` and the Nasdaq on `NASDAQ:NDX`. An unknown ticker is *silently
 * omitted* from a scanner response rather than reported as an error, so this table is
 * the single place where a bad symbol can sneak in. Every entry below was verified
 * against a live `/global/scan` call.
 */
object TvTickers {

    private val map: Map<String, String> = mapOf(
        // ------------------------------------------------------------------ metals
        "XAUUSD" to "OANDA:XAUUSD",
        "XAGUSD" to "TVC:SILVER",
        "WTIUSD" to "NYMEX:CL1!",
        // ------------------------------------------------------------------- majors
        "EURUSD" to "OANDA:EURUSD",
        "GBPUSD" to "OANDA:GBPUSD",
        "USDJPY" to "OANDA:USDJPY",
        "USDCHF" to "OANDA:USDCHF",
        "AUDUSD" to "OANDA:AUDUSD",
        "USDCAD" to "OANDA:USDCAD",
        "NZDUSD" to "OANDA:NZDUSD",
        // ------------------------------------------------------------------- crosses
        "EURJPY" to "OANDA:EURJPY",
        "GBPJPY" to "OANDA:GBPJPY",
        "AUDJPY" to "OANDA:AUDJPY",
        "EURGBP" to "OANDA:EURGBP",
        "GBPAUD" to "OANDA:GBPAUD",
        "EURAUD" to "OANDA:EURAUD",
        // ------------------------------------------------------------------- crypto
        "BTCUSD" to "BITSTAMP:BTCUSD",
        "ETHUSD" to "BITSTAMP:ETHUSD",
        // ------------------------------------------------------------------ indices
        "US30" to "OANDA:US30USD",
        "NAS100" to "NASDAQ:NDX"
    )

    /** Extra series that are not tradable instruments but feed the heatmap. */
    const val DOLLAR_INDEX = "TVC:DXY"

    /** Ticker used by the embedded chart widget for a symbol. */
    fun ticker(symbol: String): String =
        map[symbol.uppercase()] ?: "OANDA:${symbol.uppercase()}"

    /** Reverse lookup: a scanner `s` value back to the app symbol. */
    fun symbolOf(ticker: String): String =
        map.entries.firstOrNull { it.value.equals(ticker, ignoreCase = true) }?.key
            ?: ticker.substringAfter(':').uppercase()

    /** Every ticker the scanner should be asked for, in a stable order. */
    fun allTickers(): List<String> = Instruments.all.map { ticker(it.symbol) }

    /** The scanner ticker for the dollar index, used as a benchmark in the screener. */
    fun benchmark(): String = DOLLAR_INDEX

    /**
     * TradingView's own interval code for the chart widget and the pipe-suffixed
     * scanner fields (`|1` 1m, `|5` 5m, `|60` 1h, `|240` 4h).
     */
    fun interval(tf: Timeframe): String = when (tf) {
        Timeframe.M1 -> "1"
        Timeframe.M5 -> "5"
        Timeframe.M15 -> "15"
        Timeframe.H1 -> "60"
        Timeframe.H4 -> "240"
        Timeframe.D1 -> "D"
    }

    /** Timeframes the embedded widget is told to offer, in display order. */
    val widgetIntervals: String = "1,5,15,30,60,240,D,W"

    /**
     * Pairs the currency-strength meter is computed from, as
     * `symbol to base to quote`. The strength read is derived from live percentage
     * moves, never hard-coded: a currency gains when its base leg rises and loses
     * when it is the quote leg.
     */
    fun currencyLegs(): List<Triple<String, String, String>> =
        Instruments.all
            .filter { it.symbol != "WTIUSD" }
            .map { Triple(it.symbol, it.baseCurrency, it.quoteCurrency) }

    /** Every currency the heatmap can display. */
    fun currencies(): List<String> = listOf(
        "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "NZD", "XAU"
    )

    fun instrumentOf(symbol: String): Instrument = Instruments.bySymbol(symbol)
}
