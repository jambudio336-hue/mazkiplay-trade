package com.mazkiplay.trade.data.tradingview

import com.mazkiplay.trade.data.model.CurrencyStrength
import com.mazkiplay.trade.data.model.FeedStatus
import com.mazkiplay.trade.data.model.Instruments
import com.mazkiplay.trade.data.model.ScreenerRow
import com.mazkiplay.trade.data.model.TradingSignal
import com.mazkiplay.trade.data.model.TvCalendarEvent
import com.mazkiplay.trade.data.model.TvNewsItem
import com.mazkiplay.trade.data.model.TvQuote
import com.mazkiplay.trade.data.model.TvTechnical
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
import kotlin.math.abs

/**
 * The live TradingView feed for the whole app.
 *
 * One supervised loop polls the scanner every [quoteIntervalMillis] (10 s by default)
 * and the news flow every [newsIntervalMillis], while the calendar is refreshed every
 * few minutes. Every screen observes the same [StateFlow]s, so a single HTTP round trip
 * serves the dashboard ticker, the watchlist, the screener and the heatmap at once.
 *
 * Feeds are published together as one snapshot. When a poll fails the last good data is
 * kept and the status degrades to STALE then OFFLINE, which is what the header badge
 * reads \u2014 the UI never blanks out on a transient network error.
 */
class TvRepository(
    private val dataSource: TvDataSource = TvDataSource(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    /** How long live prices may go without a successful poll before they are stale. */
    private val staleAfterMillis = 45_000L
    private val offlineAfterMillis = 150_000L

    private val _quotes = MutableStateFlow<Map<String, TvQuote>>(emptyMap())
    val quotes: StateFlow<Map<String, TvQuote>> = _quotes.asStateFlow()

    private val _technicals = MutableStateFlow<Map<String, TvTechnical>>(emptyMap())
    val technicals: StateFlow<Map<String, TvTechnical>> = _technicals.asStateFlow()

    private val _signal = MutableStateFlow<TradingSignal?>(null)
    val signal: StateFlow<TradingSignal?> = _signal.asStateFlow()

    private val _calendar = MutableStateFlow<List<TvCalendarEvent>>(emptyList())
    val calendar: StateFlow<List<TvCalendarEvent>> = _calendar.asStateFlow()

    private val _news = MutableStateFlow<List<TvNewsItem>>(emptyList())
    val news: StateFlow<List<TvNewsItem>> = _news.asStateFlow()

    private val _strength = MutableStateFlow<List<CurrencyStrength>>(emptyList())
    val strength: StateFlow<List<CurrencyStrength>> = _strength.asStateFlow()

    private val _status = MutableStateFlow(FeedStatus.CONNECTING)
    val status: StateFlow<FeedStatus> = _status.asStateFlow()

    private val _lastUpdate = MutableStateFlow(0L)
    val lastUpdate: StateFlow<Long> = _lastUpdate.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _signals = MutableStateFlow<Map<String, TradingSignal>>(emptyMap())
    val signals: StateFlow<Map<String, TradingSignal>> = _signals.asStateFlow()

    private val _unresolved = MutableStateFlow(0)
    val unresolvedTickers: StateFlow<Int> = _unresolved.asStateFlow()

    private var symbols: List<String> = Instruments.all.map { it.symbol }
    private var quoteIntervalMillis: Long = 10_000L
    private var started = false

    /** Percentage move that triggers a local price alert. */
    private var alertThreshold = 1.0
    private val lastAlertSide = mutableMapOf<String, Boolean>()

    /** Raised once per threshold crossing, never on every poll. */
    var onPriceAlert: ((String, TvQuote) -> Unit)? = null

    fun setAlertThreshold(percent: Double) {
        alertThreshold = percent.coerceAtLeast(0.1)
    }

    /** Restrict the live feed to the user's watchlist (plus the benchmark series). */
    fun setSymbols(list: List<String>) {
        val next = if (list.isEmpty()) Instruments.all.map { it.symbol } else list
        if (next == symbols) return
        symbols = next
        scope.launch { runCatching { pollQuotes() } }
    }

    /**
     * Start the perpetual refresh loops. Safe to call more than once \u2014 the guard keeps
     * a second launch from doubling the request rate.
     */
    fun start(intervalMillis: Long = 10_000L, newsIntervalMillis: Long = 120_000L) {
        if (started) return
        started = true
        quoteIntervalMillis = intervalMillis

        scope.launch {
            while (isActive) {
                runCatching { pollQuotes() }
                delay(quoteIntervalMillis)
                runCatching { decayStatus() }
            }
        }

        scope.launch {
            runCatching { pollCalendar() }
            while (isActive) {
                delay(240_000L)
                runCatching { pollCalendar() }
            }
        }

        scope.launch {
            runCatching { pollNews() }
            while (isActive) {
                delay(newsIntervalMillis)
                runCatching { pollNews() }
            }
        }
    }

    /** Force an immediate full refresh, used by the manual refresh affordance. */
    suspend fun refreshNow() {
        runCatching { pollQuotes() }
        runCatching { pollCalendar() }
        runCatching { pollNews() }
    }

    // ------------------------------------------------------------------ polling

    private suspend fun pollQuotes() {
        val snapshot = dataSource.scan(symbols)
        if (snapshot.isEmpty) {
            _lastError.value = "Tidak ada data dari feed TradingView"
            decayStatus()
            return
        }

        _quotes.value = snapshot.quotes
        _technicals.value = snapshot.technicals
        _strength.value = TvAnalyzer.currencyStrength(snapshot.quotes)
        _unresolved.value = (snapshot.requestedCount - snapshot.resolvedCount).coerceAtLeast(0)
        _lastUpdate.value = System.currentTimeMillis()
        _lastError.value = null
        _status.value = FeedStatus.LIVE

        evaluateAlerts(snapshot.quotes)

        // Re-score every symbol the user is watching so the signal panel and the chart
        // strip always reflect the reading that just arrived.
        recomputeSignals()
    }

    /**
     * Fires [onPriceAlert] only when a symbol *crosses* the threshold, so a market
     * that sits above 1% for an hour raises one alert rather than one per poll.
     */
    private fun evaluateAlerts(quotes: Map<String, TvQuote>) {
        val callback = onPriceAlert ?: return
        quotes.forEach { (symbol, quote) ->
            val side = abs(quote.changePercent) >= alertThreshold
            val previous = lastAlertSide.put(symbol, side)
            if (side && previous != true) callback(symbol, quote)
        }
    }

    private suspend fun pollCalendar() {
        val fetched = dataSource.calendar()
        if (fetched.isNotEmpty()) {
            _calendar.value = fetched
            recomputeSignals()
        }
    }

    private suspend fun pollNews() {
        val fetched = dataSource.news()
        if (fetched.isNotEmpty()) _news.value = fetched
    }

    /**
     * Move LIVE \u2192 STALE \u2192 OFFLINE purely from the age of the last good reading, so a
     * silently-dropped request still shows up as a status change.
     */
    private fun decayStatus() {
        val age = System.currentTimeMillis() - _lastUpdate.value
        _status.value = when {
            _lastUpdate.value == 0L -> FeedStatus.CONNECTING
            age <= staleAfterMillis -> FeedStatus.LIVE
            age <= offlineAfterMillis -> FeedStatus.STALE
            else -> FeedStatus.OFFLINE
        }
    }

    // ------------------------------------------------------------------ signals

    private fun recomputeSignals() {
        val signals = LinkedHashMap<String, TradingSignal>()
        symbols.forEach { symbol ->
            signals[symbol] = buildSignal(symbol)
        }
        _signals.value = signals
        _signal.value = signals[selectedSymbol]
    }

    private fun buildSignal(
        symbol: String,
        tpRatio: Double = this.tpRatio,
        slPercent: Double = this.slPercent
    ): TradingSignal = TvAnalyzer.buildSignal(
        symbol = symbol,
        technical = _technicals.value[symbol],
        quote = _quotes.value[symbol],
        events = _calendar.value,
        tpRatio = tpRatio,
        timeframeLabel = timeframeLabel,
        slPercent = slPercent
    )

    private var selectedSymbol: String = "XAUUSD"
    private var timeframeLabel: String = "M15"
    private var tpRatio: Double = 2.0
    private var slPercent: Double = 1.0

    fun selectSymbol(symbol: String) {
        selectedSymbol = symbol
        _signal.value = _signals.value[symbol] ?: buildSignal(symbol)
    }

    /** Keep the signal maths in step with the user's trading preferences. */
    fun setSignalPreferences(tpRatio: Double, slPercent: Double, timeframeLabel: String) {
        val changed = this.tpRatio != tpRatio ||
            this.slPercent != slPercent ||
            this.timeframeLabel != timeframeLabel
        this.tpRatio = tpRatio
        this.slPercent = slPercent
        this.timeframeLabel = timeframeLabel
        if (changed) recomputeSignals()
    }

    /** Recompute only the selected symbol's signal on demand from the UI. */
    fun refreshSignal(symbol: String) {
        selectSymbol(symbol)
        _signal.value = buildSignal(symbol)
    }

    // ------------------------------------------------------------------- access

    fun quoteOf(symbol: String): TvQuote? = _quotes.value[symbol]

    fun technicalOf(symbol: String): TvTechnical? = _technicals.value[symbol]

    fun signalOf(symbol: String): TradingSignal? = _signals.value[symbol]

    /** Screener rows derived from the readings currently held. */
    fun screener(): List<ScreenerRow> = TvAnalyzer.screenerRows(_quotes.value, _technicals.value)

    /** High-impact calendar events still ahead, nearest first. */
    fun upcomingHighImpact(limit: Int = 6): List<TvCalendarEvent> {
        val now = System.currentTimeMillis()
        return _calendar.value
            .filter { it.importance >= 2 && it.dateMillis >= now }
            .take(limit)
    }

    /** The next release touching a specific currency, used by the calendar reminder. */
    fun nextEventFor(currency: String): TvCalendarEvent? {
        val now = System.currentTimeMillis()
        return _calendar.value
            .filter { it.currency.equals(currency, ignoreCase = true) && it.dateMillis >= now }
            .minByOrNull { it.dateMillis }
    }

    /**
     * Headlines that mention one of the given instruments or a currency inside them.
     * TradingView tags stories with related symbols, so this is a real join rather than
     * a text search.
     */
    fun newsFor(symbols: List<String>, limit: Int = 20): List<TvNewsItem> {
        val wanted = symbols.map { it.uppercase() }.toSet()
        return _news.value
            .filter { item -> item.symbols.any { it.uppercase() in wanted } }
            .take(limit)
    }
}
