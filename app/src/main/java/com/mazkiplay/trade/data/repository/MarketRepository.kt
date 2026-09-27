package com.mazkiplay.trade.data.repository

import com.mazkiplay.trade.data.api.MarketDataSource
import com.mazkiplay.trade.data.model.AnalysisResult
import com.mazkiplay.trade.data.model.Candle
import com.mazkiplay.trade.data.model.EconomicEvent
import com.mazkiplay.trade.data.model.Instrument
import com.mazkiplay.trade.data.model.Instruments
import com.mazkiplay.trade.data.model.Quote
import com.mazkiplay.trade.data.model.Timeframe
import com.mazkiplay.trade.domain.analysis.AutoAnalyzer
import com.mazkiplay.trade.domain.analysis.Indicators
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Live market state: quotes for the watchlist, candles for the chart, the economic
 * calendar and the last automatic analysis per symbol.
 *
 * A single supervised background loop refreshes prices on an interval while the app
 * is alive, so every screen observes the same [StateFlow] instead of polling.
 */
class MarketRepository(
    private val dataSource: MarketDataSource = MarketDataSource()
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _quotes = MutableStateFlow<Map<String, Quote>>(emptyMap())
    val quotes: StateFlow<Map<String, Quote>> = _quotes.asStateFlow()

    private val _candles = MutableStateFlow<Map<String, List<Candle>>>(emptyMap())
    val candles: StateFlow<Map<String, List<Candle>>> = _candles.asStateFlow()

    private val _events = MutableStateFlow<List<EconomicEvent>>(emptyList())
    val events: StateFlow<List<EconomicEvent>> = _events.asStateFlow()

    private val _analyses = MutableStateFlow<Map<String, AnalysisResult>>(emptyMap())
    val analyses: StateFlow<Map<String, AnalysisResult>> = _analyses.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _lastUpdated = MutableStateFlow(0L)
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var symbolsToTrack: List<String> = Instruments.featured.map { it.symbol }

    fun setTrackedSymbols(symbols: List<String>) {
        symbolsToTrack = symbols.ifEmpty { Instruments.featured.map { it.symbol } }
        scope.launch { refreshQuotes() }
    }

    /** Start the perpetual auto-refresh loop; safe to call more than once. */
    fun startAutoRefresh(intervalMillis: Long = 60_000L) {
        scope.launch {
            refreshAll()
            while (isActive) {
                delay(intervalMillis)
                runCatching { refreshQuotes() }
                runCatching { refreshCalendar() }
            }
        }
    }

    suspend fun refreshAll() {
        _loading.value = true
        runCatching { refreshQuotes() }
        runCatching { refreshCalendar() }
        runCatching { refreshNewsDependent() }
        _loading.value = false
    }

    suspend fun refreshQuotes() {
        val instruments = symbolsToTrack.map { Instruments.bySymbol(it) }
        val fetched = dataSource.quotes(instruments)
        if (fetched.isNotEmpty()) {
            _quotes.update { current ->
                val merged = current.toMutableMap()
                fetched.forEach { quote ->
                    if (quote.price > 0.0) merged[quote.symbol] = quote
                }
                merged
            }
            _lastUpdated.value = System.currentTimeMillis()
            _error.value = null
        } else {
            _error.value = "Gagal memuat harga terbaru"
        }
    }

    suspend fun refreshCalendar() {
        val fetched = dataSource.economicCalendar()
        if (fetched.isNotEmpty()) _events.value = fetched
    }

    /** Analysis needs the calendar, which the news repository keeps warm. */
    private suspend fun refreshNewsDependent() {
        // Nothing to do here today; kept so callers have one entry point.
    }

    suspend fun loadCandles(symbol: String, timeframe: Timeframe, force: Boolean = false): List<Candle> {
        val key = "$symbol-${timeframe.name}"
        if (!force) {
            _candles.value[key]?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        val instrument = Instruments.bySymbol(symbol)
        val raw = dataSource.candles(instrument, timeframe.interval, timeframe.range)
        val data = if (timeframe.aggregate > 1) Indicators.aggregate(raw, timeframe.aggregate) else raw
        _candles.update { it + (key to data) }
        return data
    }

    fun cachedCandles(symbol: String, timeframe: Timeframe): List<Candle> =
        _candles.value["$symbol-${timeframe.name}"].orEmpty()

    /** Run the automatic analyser and cache the result for the analysis panel. */
    suspend fun analyse(
        symbol: String,
        timeframe: Timeframe,
        tpRatio: Double,
        news: List<com.mazkiplay.trade.data.model.NewsItem>
    ): AnalysisResult {
        val series = loadCandles(symbol, timeframe)
        val result = AutoAnalyzer.analyze(symbol, series, _events.value, news, tpRatio)
        _analyses.update { it + (symbol to result) }
        return result
    }

    fun cachedAnalysis(symbol: String): AnalysisResult? = _analyses.value[symbol]

    fun quoteOf(symbol: String): Quote = _quotes.value[symbol] ?: Quote(symbol)

    fun instrumentOf(symbol: String): Instrument = Instruments.bySymbol(symbol)

    /** High-impact events still ahead of us today, used for the news banner. */
    fun upcomingHighImpact(limit: Int = 5): List<EconomicEvent> {
        val now = System.currentTimeMillis()
        return _events.value
            .filter { it.dateMillis >= now && it.impact == com.mazkiplay.trade.data.model.Impact.HIGH }
            .take(limit)
    }
}
