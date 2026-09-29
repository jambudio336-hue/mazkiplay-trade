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
        "SOLUSD" to "COINBASE:SOLUSD",
        "XRPUSD" to "BITSTAMP:XRPUSD",
        "DOGEUSD" to "COINBASE:DOGEUSD",
        "ADAUSD" to "COINBASE:ADAUSD",
        "USDTUSD" to "COINBASE:USDTUSD",
        // ------------------------------------------------------------------ indices
        "US30" to "OANDA:US30USD",
        "NAS100" to "NASDAQ:NDX",
        "SP500" to "SP:SPX",
        "DAX" to "XETR:DAX",
        "FTSE" to "TVC:UKX",
        "NIKKEI" to "TVC:NI225",
        "HSI" to "TVC:HSI",
        "IHSG" to "IDX:COMPOSITE",
        "BBCA" to "IDX:BBCA",
        "BBRI" to "IDX:BBRI",
        "BMRI" to "IDX:BMRI",
        "TLKM" to "IDX:TLKM",
        "ASII" to "IDX:ASII",
        "AAPL" to "NASDAQ:AAPL",
        "MSFT" to "NASDAQ:MSFT",
        "NVDA" to "NASDAQ:NVDA",
        "TSLA" to "NASDAQ:TSLA",
        "AMZN" to "NASDAQ:AMZN",
        "TOYOTA" to "TSE:7203",
        "TENCENT" to "HKEX:700",
        "SAP" to "XETR:SAP",
        "BHP" to "ASX:BHP",
        "DXY" to "TVC:DXY",
        "VIX" to "CBOE:VIX",
        "US10Y" to "TVC:US10Y",
        "US30Y" to "TVC:US30Y"
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
        Timeframe.M1, Timeframe.M3 -> "1"
        Timeframe.M5 -> "5"
        Timeframe.M15 -> "15"
        Timeframe.M30 -> "30"
        Timeframe.H1 -> "60"
        Timeframe.H2 -> "120"
        Timeframe.H4 -> "240"
        Timeframe.H6 -> "360"
        Timeframe.H8 -> "480"
        Timeframe.H12 -> "720"
        Timeframe.D1 -> "D"
        Timeframe.W1 -> "W"
        Timeframe.MN1 -> "M"
    }

    /** Timeframes the embedded widget is told to offer, in display order. */
    val widgetIntervals: String = "1,5,15,30,60,120,240,360,480,720,D,W"

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
