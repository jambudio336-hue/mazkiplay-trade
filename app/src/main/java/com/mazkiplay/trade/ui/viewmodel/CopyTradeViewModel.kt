package com.mazkiplay.trade.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.AnalysisResult
import com.mazkiplay.trade.data.model.CopyTrader
import com.mazkiplay.trade.data.model.Timeframe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Copy-trade desk state: the provider ranking, the selected provider's detail panel
 * and the set of providers the user follows.
 *
 * Subscriptions are persisted through the settings repository rather than held in
 * memory, so the follow list survives a restart.
 */
class CopyTradeViewModel(private val app: MazkiplayApp) : ViewModel() {

    private val repository = app.copyTradeRepository
    private val market = app.marketRepository
    private val settings = app.settings

    val traders: StateFlow<List<CopyTrader>> = repository.traders
    val lastUpdated: StateFlow<Long> = repository.lastUpdated

    /** Provider ids the user follows, read back from DataStore. */
    val subscriptions: StateFlow<Set<String>> = settings.preferences
        .map { it.copySubscriptions }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _detail = MutableStateFlow<CopyTrader?>(null)
    val detail: StateFlow<CopyTrader?> = _detail.asStateFlow()

    private val _detailAnalysis = MutableStateFlow<AnalysisResult?>(null)
    val detailAnalysis: StateFlow<AnalysisResult?> = _detailAnalysis.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun isSubscribed(trader: CopyTrader): Boolean = subscriptions.value.contains(trader.id)

    fun toggleSubscription(trader: CopyTrader) {
        viewModelScope.launch {
            val subscribing = !subscriptions.value.contains(trader.id)
            runCatching { settings.toggleCopySubscription(trader.id) }
            _message.value = if (subscribing) {
                "Copy trade ${trader.name} diaktifkan"
            } else {
                "Berhenti mengikuti ${trader.name}"
            }
        }
    }

    /**
     * Open the detail panel and analyse the provider's headline instrument, so the
     * panel shows a real combined verdict rather than a placeholder.
     */
    fun openDetail(trader: CopyTrader) {
        _detail.value = trader
        _detailAnalysis.value = null
        viewModelScope.launch {
            _busy.value = true
            runCatching {
                val symbol = trader.topSymbols.firstOrNull() ?: "XAUUSD"
                market.loadCandles(symbol, Timeframe.H1)
                market.analyse(symbol, Timeframe.H1, 2.0, app.newsRepository.news.value)
            }.onSuccess { _detailAnalysis.value = it }
            _busy.value = false
        }
    }

    fun closeDetail() {
        _detail.value = null
        _detailAnalysis.value = null
    }

    fun clearMessage() {
        _message.value = null
    }
}
