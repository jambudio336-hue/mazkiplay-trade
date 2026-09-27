package com.mazkiplay.trade.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.AnalysisResult
import com.mazkiplay.trade.data.model.Candle
import com.mazkiplay.trade.data.model.EconomicEvent
import com.mazkiplay.trade.data.model.Instruments
import com.mazkiplay.trade.data.model.MarketProfile
import com.mazkiplay.trade.data.model.MarketSession
import com.mazkiplay.trade.data.model.OrderBook
import com.mazkiplay.trade.data.model.Quote
import com.mazkiplay.trade.data.model.TechnicalSnapshot
import com.mazkiplay.trade.data.model.Timeframe
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.domain.analysis.Indicators
import com.mazkiplay.trade.domain.analysis.TechnicalAnalyzer
import com.mazkiplay.trade.domain.session.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Market-facing state for the dashboard, chart, watchlist and market-analysis screens.
 *
 * The repositories already expose hot [StateFlow]s for prices, candles and the
 * calendar, so this ViewModel mostly adds the two pieces of state that belong to the
 * screen itself (the selected instrument and timeframe) plus the session clock.
 */
class MarketViewModel(private val app: MazkiplayApp) : ViewModel() {

    private val market = app.marketRepository
    private val settings = app.settings

    val quotes: StateFlow<Map<String, Quote>> = market.quotes
    val events: StateFlow<List<EconomicEvent>> = market.events
    val loading: StateFlow<Boolean> = market.loading
    val lastUpdated: StateFlow<Long> = market.lastUpdated

    /** Ticks once a minute so session status and liquidity stay truthful on screen. */
    private val ticker: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000L)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, System.currentTimeMillis())

    val sessions: StateFlow<List<MarketSession>> = ticker
        .map { SessionManager.sessions(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionManager.sessions())

    val liquidity: StateFlow<Int> = ticker
        .map { SessionManager.liquidityScore(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionManager.liquidityScore())

    /** Live preferences; the watchlist chips read `watchlist` out of this snapshot. */
    val watchlist: StateFlow<UserPreferences> = settings.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    private val _selectedSymbol = MutableStateFlow("XAUUSD")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    private val _timeframe = MutableStateFlow(Timeframe.M15)
    val timeframe: StateFlow<Timeframe> = _timeframe.asStateFlow()

    private val _candles = MutableStateFlow<List<Candle>>(emptyList())
    val candles: StateFlow<List<Candle>> = _candles.asStateFlow()

    private val _analysis = MutableStateFlow<AnalysisResult?>(null)
    val analysis: StateFlow<AnalysisResult?> = _analysis.asStateFlow()

    init {
        // Keep the repository tracking exactly what the user has on their watchlist.
        viewModelScope.launch {
            settings.preferences.collect { prefs ->
                market.setTrackedSymbols(prefs.watchlist.toList())
            }
        }
        loadCandles()
    }

    fun selectSymbol(symbol: String) {
        if (symbol == _selectedSymbol.value) return
        _selectedSymbol.value = symbol
        _analysis.value = null
        loadCandles()
    }

    fun setTimeframe(timeframe: Timeframe) {
        if (timeframe == _timeframe.value) return
        _timeframe.value = timeframe
        loadCandles()
    }

    private fun loadCandles() {
        viewModelScope.launch {
            runCatching {
                market.loadCandles(_selectedSymbol.value, _timeframe.value, force = true)
            }.onSuccess { _candles.value = it }
        }
    }

    /** Technical reading of the candles currently held; null until data arrives. */
    fun snapshot(): TechnicalSnapshot? = _candles.value
        .takeIf { it.isNotEmpty() }
        ?.let { TechnicalAnalyzer.analyze(it, Instruments.bySymbol(_selectedSymbol.value)) }

    fun orderBook(): OrderBook =
        Indicators.orderBook(_candles.value, _selectedSymbol.value)

    fun marketProfile(): MarketProfile =
        Indicators.marketProfile(_candles.value, _selectedSymbol.value)

    /** Run the combined analyser for the current selection and cache the result. */
    fun runAnalysis() {
        viewModelScope.launch {
            runCatching {
                market.analyse(
                    symbol = _selectedSymbol.value,
                    timeframe = _timeframe.value,
                    tpRatio = settings.current().tpRatio,
                    news = app.newsRepository.news.value
                )
            }.onSuccess { _analysis.value = it }
        }
    }

    /** Last cached analysis for a symbol, used by the dashboard summary card. */
    fun cachedAnalysis(symbol: String): AnalysisResult? = market.cachedAnalysis(symbol)

    fun quoteOf(symbol: String): Quote = market.quoteOf(symbol)

    fun toggleWatchlist(symbol: String) {
        viewModelScope.launch { runCatching { settings.toggleWatchlist(symbol) } }
    }
}
