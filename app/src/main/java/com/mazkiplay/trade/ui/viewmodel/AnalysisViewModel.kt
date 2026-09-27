package com.mazkiplay.trade.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.AnalysisResult
import com.mazkiplay.trade.data.model.Candle
import com.mazkiplay.trade.data.model.FundamentalSnapshot
import com.mazkiplay.trade.data.model.Instruments
import com.mazkiplay.trade.data.model.TechnicalSnapshot
import com.mazkiplay.trade.data.model.Timeframe
import com.mazkiplay.trade.domain.analysis.FundamentalAnalyzer
import com.mazkiplay.trade.domain.analysis.TechnicalAnalyzer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The "Analisa Otomatis" screen.
 *
 * A single pass loads the candle series, measures the technical and fundamental
 * snapshots for the panel and stores the blended verdict. The optional auto mode
 * simply repeats that pass on a timer while the toggle is on.
 */
class AnalysisViewModel(private val app: MazkiplayApp) : ViewModel() {

    private val market = app.marketRepository
    private val newsRepository = app.newsRepository
    private val settings = app.settings

    private val _symbol = MutableStateFlow("XAUUSD")
    val symbol: StateFlow<String> = _symbol.asStateFlow()

    private val _timeframe = MutableStateFlow(Timeframe.H1)
    val timeframe: StateFlow<Timeframe> = _timeframe.asStateFlow()

    private val _tpRatio = MutableStateFlow(2.0)
    val tpRatio: StateFlow<Double> = _tpRatio.asStateFlow()

    private val _result = MutableStateFlow<AnalysisResult?>(null)
    val result: StateFlow<AnalysisResult?> = _result.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _auto = MutableStateFlow(false)
    val auto: StateFlow<Boolean> = _auto.asStateFlow()

    private val _candles = MutableStateFlow<List<Candle>>(emptyList())
    val candles: StateFlow<List<Candle>> = _candles.asStateFlow()

    private val _snapshot = MutableStateFlow<TechnicalSnapshot?>(null)
    val snapshot: StateFlow<TechnicalSnapshot?> = _snapshot.asStateFlow()

    private val _fundamental = MutableStateFlow(FundamentalSnapshot())
    val fundamental: StateFlow<FundamentalSnapshot> = _fundamental.asStateFlow()

    init {
        // Adopt the saved TP ratio, then analyse straight away so the first visit
        // already shows a verdict instead of an empty panel.
        viewModelScope.launch {
            _tpRatio.value = settings.current().tpRatio
            runAnalysis()
        }
        // Auto mode: re-run on a slow timer; the guard makes it a no-op when off.
        viewModelScope.launch {
            while (true) {
                delay(AUTO_INTERVAL_MS)
                if (_auto.value) runAnalysis()
            }
        }
    }

    fun setSymbol(value: String) {
        if (value == _symbol.value) return
        _symbol.value = value
        runAnalysis()
    }

    fun setTimeframe(value: Timeframe) {
        if (value == _timeframe.value) return
        _timeframe.value = value
        runAnalysis()
    }

    fun setTpRatio(value: Double) {
        _tpRatio.value = value
        if (_result.value != null) runAnalysis()
    }

    fun toggleAuto() {
        _auto.value = !_auto.value
    }

    /** Run one full analysis pass for the current symbol, timeframe and TP ratio. */
    fun runAnalysis() {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            runCatching {
                val series = market.loadCandles(_symbol.value, _timeframe.value)
                val instrument = Instruments.bySymbol(_symbol.value)
                val events = market.events.value
                val news = newsRepository.news.value

                _candles.value = series
                _snapshot.value = TechnicalAnalyzer.analyze(series, instrument)
                _fundamental.value = FundamentalAnalyzer.analyze(instrument, events, news)
                _result.value = market.analyse(
                    symbol = _symbol.value,
                    timeframe = _timeframe.value,
                    tpRatio = _tpRatio.value,
                    news = news
                )
            }
            _busy.value = false
        }
    }

    private companion object {
        const val AUTO_INTERVAL_MS = 5 * 60_000L
    }
}
