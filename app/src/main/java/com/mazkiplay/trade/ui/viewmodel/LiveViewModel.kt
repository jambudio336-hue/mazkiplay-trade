package com.mazkiplay.trade.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.CurrencyStrength
import com.mazkiplay.trade.data.model.FeedStatus
import com.mazkiplay.trade.data.model.Instruments
import com.mazkiplay.trade.data.model.ScreenerFilter
import com.mazkiplay.trade.data.model.ScreenerRow
import com.mazkiplay.trade.data.model.Timeframe
import com.mazkiplay.trade.data.model.TradingSignal
import com.mazkiplay.trade.data.model.TvCalendarEvent
import com.mazkiplay.trade.data.model.TvNewsItem
import com.mazkiplay.trade.data.model.TvQuote
import com.mazkiplay.trade.data.model.TvTechnical
import com.mazkiplay.trade.data.tradingview.TvTickers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backs every real-time screen with the single shared [com.mazkiplay.trade.data.tradingview.TvRepository].
 *
 * The ViewModel deliberately holds almost no state of its own: prices, technicals,
 * calendar and news are read straight from the repository's flows, so a poll that lands
 * while the user is on the chart screen updates the watchlist and the screener too,
 * without any of them re-fetching. Only genuinely view-local state lives here \u2014 the
 * selected symbol, the chart timeframe and the active screener filter.
 */
class LiveViewModel(private val app: MazkiplayApp) : ViewModel() {

    private val repo = app.tvRepository

    // ------------------------------------------------------------------ live feed

    val quotes: StateFlow<Map<String, TvQuote>> = repo.quotes
    val technicals: StateFlow<Map<String, TvTechnical>> = repo.technicals
    val status: StateFlow<FeedStatus> = repo.status
    val lastUpdate: StateFlow<Long> = repo.lastUpdate
    val lastError: StateFlow<String?> = repo.lastError
    val strength: StateFlow<List<CurrencyStrength>> = repo.strength
    val calendar: StateFlow<List<TvCalendarEvent>> = repo.calendar
    val news: StateFlow<List<TvNewsItem>> = repo.news
    val signal: StateFlow<TradingSignal?> = repo.signal
    val signals: StateFlow<Map<String, TradingSignal>> = repo.signals

    /** Surfaced in the debug strip when a ticker fails to resolve. */
    val unresolvedTickers: StateFlow<Int> = repo.unresolvedTickers

    // --------------------------------------------------------------- view state

    private val _symbol = MutableStateFlow("XAUUSD")
    val symbol: StateFlow<String> = _symbol.asStateFlow()

    private val _timeframe = MutableStateFlow(Timeframe.M15)
    val timeframe: StateFlow<Timeframe> = _timeframe.asStateFlow()

    private val _filter = MutableStateFlow(ScreenerFilter.ALL)
    val filter: StateFlow<ScreenerFilter> = _filter.asStateFlow()

    private val _filteredRows = MutableStateFlow<List<ScreenerRow>>(emptyList())
    val filteredRows: StateFlow<List<ScreenerRow>> = _filteredRows.asStateFlow()

    /** True while the screen is showing the previous snapshot because a poll failed. */
    val isDegraded: StateFlow<FeedStatus> = repo.status

    init {
        // Keep the user's preference in step with the feed, and seed the first reading
        // so the very first frame is never empty.
        viewModelScope.launch {
            val prefs = app.settings.current()
            _timeframe.value = Timeframe.fromLabel(prefs.defaultTimeframe)
            repo.setSignalPreferences(prefs.tpRatio, prefs.slPercent, prefs.defaultTimeframe)
            repo.setAlertThreshold(prefs.priceAlertThreshold)
        }
        viewModelScope.launch {
            quotes.collect { _filteredRows.value = applyFilter(_filter.value) }
        }
        viewModelScope.launch {
            technicals.collect { _filteredRows.value = applyFilter(_filter.value) }
        }
        repo.selectSymbol(_symbol.value)
    }

    // ------------------------------------------------------------------ actions

    fun selectSymbol(newSymbol: String) {
        _symbol.value = newSymbol
        repo.selectSymbol(newSymbol)
    }

    fun selectTimeframe(tf: Timeframe) {
        _timeframe.value = tf
        viewModelScope.launch {
            app.settings.setDefaultTimeframe(tf.label)
        }
        viewModelScope.launch {
            val prefs = app.settings.current()
            repo.setSignalPreferences(prefs.tpRatio, prefs.slPercent, tf.label)
        }
    }

    fun selectFilter(next: ScreenerFilter) {
        _filter.value = next
        _filteredRows.value = applyFilter(next)
    }

    /** Manual refresh, shared by every live screen's pull affordance. */
    fun refresh() {
        viewModelScope.launch { repo.refreshNow() }
    }

    /** Re-run the combined analysis for the selected symbol on demand. */
    fun analyseNow() {
        repo.refreshSignal(_symbol.value)
    }

    fun toggleWatchlist(symbol: String) {
        viewModelScope.launch {
            app.settings.toggleWatchlist(symbol)
            val prefs = app.settings.current()
            repo.setSymbols(prefs.watchlist.toList())
        }
    }

    fun setAlertThreshold(percent: Double) {
        repo.setAlertThreshold(percent)
        viewModelScope.launch { app.settings.setPriceAlertThreshold(percent) }
    }

    // ------------------------------------------------------------------ derived

    private fun applyFilter(filter: ScreenerFilter): List<ScreenerRow> =
        repo.screener().filter { filter.test(it) }

    /** The live quote for the selected symbol, or null before the first poll lands. */
    fun selectedQuote(): TvQuote? = repo.quoteOf(_symbol.value)

    fun selectedTechnical(): TvTechnical? = repo.technicalOf(_symbol.value)

    /** TradingView interval code for the chart widget. */
    fun chartInterval(): String = TvTickers.interval(_timeframe.value)

    /** Instruments that resolved in the last poll \u2014 used by the symbol picker. */
    fun tradableSymbols(): List<String> =
        if (quotes.value.isEmpty()) Instruments.all.map { it.symbol }
        else quotes.value.keys.toList().sorted()

    /** Ask the repository for the next release of a currency, for calendar reminders. */
    fun nextEventFor(currency: String): TvCalendarEvent? = repo.nextEventFor(currency)

    /** Headlines related to the selected symbol. */
    fun newsForSelected(): List<TvNewsItem> = repo.newsFor(listOf(_symbol.value))
}
