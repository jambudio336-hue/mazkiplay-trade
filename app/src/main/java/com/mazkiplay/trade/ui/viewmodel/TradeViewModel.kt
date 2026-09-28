package com.mazkiplay.trade.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.EntryAlarm
import com.mazkiplay.trade.data.model.Instruments
import com.mazkiplay.trade.data.model.JournalEntry
import com.mazkiplay.trade.data.model.Position
import com.mazkiplay.trade.data.model.SizedTrade
import com.mazkiplay.trade.data.model.TradeDirection
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.domain.trade.PositionCalculator
import com.mazkiplay.trade.domain.trade.RiskCalculator
import com.mazkiplay.trade.service.AlarmScheduler
import com.mazkiplay.trade.util.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * One ViewModel for the four trading screens: the order ticket, the journal, the two
 * calculators and the entry-alarm list.
 *
 * The derived figures (`ticketResult`, `calcResult`, `riskPlan`) are plain functions
 * rather than flows. The inputs already live in state here, so recomputing on demand
 * keeps the screens' `remember(...)` keys honest and avoids a second layer of caching.
 */
class TradeViewModel(private val app: MazkiplayApp) : ViewModel() {

    private val trade = app.tradeRepository
    private val market = app.marketRepository
    private val settings = app.settings

    val positions: StateFlow<List<Position>> = trade.positions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val journal: StateFlow<List<JournalEntry>> = trade.journal
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val alarms: StateFlow<List<EntryAlarm>> = trade.alarms
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Live defaults (balance, leverage, risk) used by the ticket and both calculators. */
    val preferences: StateFlow<UserPreferences> = settings.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // ------------------------------------------------------------- order ticket

    private val _ticketSymbol = MutableStateFlow("XAUUSD")
    val ticketSymbol: StateFlow<String> = _ticketSymbol.asStateFlow()

    private val _ticketDirection = MutableStateFlow(TradeDirection.BUY)
    val ticketDirection: StateFlow<TradeDirection> = _ticketDirection.asStateFlow()

    private val _ticketRisk = MutableStateFlow(1.0)
    val ticketRisk: StateFlow<Double> = _ticketRisk.asStateFlow()

    private val _ticketSl = MutableStateFlow(1.0)
    val ticketSl: StateFlow<Double> = _ticketSl.asStateFlow()

    private val _ticketTp = MutableStateFlow(2.0)
    val ticketTp: StateFlow<Double> = _ticketTp.asStateFlow()

    private val _ticketResult = MutableStateFlow<SizedTrade?>(null)
    val ticketResult: StateFlow<SizedTrade?> = _ticketResult.asStateFlow()

    init {
        // Seed the ticket from the saved defaults once, then let the user drive it.
        viewModelScope.launch {
            val initial = settings.current()
            _ticketSymbol.value = initial.watchlist.firstOrNull() ?: "XAUUSD"
            _ticketRisk.value = initial.riskPercent
            _ticketSl.value = initial.slPercent
            _ticketTp.value = initial.tpRatio
            recalculateTicket(initial)
        }
        // Quotes move underneath us, so re-size whenever they do.
        viewModelScope.launch {
            market.quotes.collect { recalculateTicket() }
        }
    }

    private fun recalculateTicket(prefs: UserPreferences = preferences.value) {
        val price = market.quoteOf(_ticketSymbol.value).price
        if (price <= 0.0) {
            _ticketResult.value = null
            return
        }
        _ticketResult.value = PositionCalculator.size(
            instrument = Instruments.bySymbol(_ticketSymbol.value),
            direction = _ticketDirection.value,
            entry = price,
            balance = prefs.balance,
            riskPercent = _ticketRisk.value,
            tpRatio = _ticketTp.value,
            slPercent = _ticketSl.value,
            leverage = prefs.leverage
        )
    }

    fun selectTicketSymbol(symbol: String) {
        _ticketSymbol.value = symbol
        recalculateTicket()
    }

    fun setTicketDirection(direction: TradeDirection) {
        _ticketDirection.value = direction
        recalculateTicket()
    }

    fun setTicketRisk(value: Double) {
        _ticketRisk.value = value
        recalculateTicket()
    }

    fun setTicketSl(value: Double) {
        _ticketSl.value = value
        recalculateTicket()
    }

    fun setTicketTp(value: Double) {
        _ticketTp.value = value
        recalculateTicket()
    }

    fun openPosition() {
        val ticket = _ticketResult.value ?: return
        viewModelScope.launch {
            runCatching {
                trade.savePosition(ticket, market.quoteOf(ticket.symbol).price)
            }.onSuccess {
                _message.value = "Posisi ${ticket.symbol} ${ticket.direction.label} " +
                    "${Formatters.lot(ticket.lot)} lot tersimpan"
            }.onFailure {
                _message.value = "Gagal menyimpan posisi: ${it.message}"
            }
        }
    }

    fun closePosition(position: Position) {
        viewModelScope.launch {
            runCatching {
                trade.closePosition(position, market.quoteOf(position.symbol).price)
            }.onSuccess {
                _message.value = "Posisi ${position.symbol} ditutup"
            }.onFailure {
                _message.value = "Gagal menutup posisi: ${it.message}"
            }
        }
    }

    fun deletePosition(id: Long) {
        viewModelScope.launch { runCatching { trade.deletePosition(id) } }
    }

    fun addJournal(symbol: String, title: String, body: String, mood: String) {
        if (title.isBlank() && body.isBlank()) return
        viewModelScope.launch {
            runCatching {
                trade.saveJournal(JournalEntry(symbol = symbol.ifBlank { "Umum" }, title = title.ifBlank { "Catatan trading" }, body = body, mood = mood))
            }.onSuccess { _message.value = "Catatan jurnal tersimpan" }
                .onFailure { _message.value = "Gagal menyimpan jurnal: ${it.message}" }
        }
    }

    fun deleteJournal(id: Long) {
        viewModelScope.launch { runCatching { trade.deleteJournal(id) } }
    }

    /** Floating P/L of an open position at the latest available price. */
    fun floatingPnl(position: Position): Double =
        trade.floatingPnl(position, market.quoteOf(position.symbol).price)

    // ------------------------------------------------------------ entry alarms

    fun addAlarm(
        label: String,
        symbol: String,
        triggerAt: Long,
        note: String,
        repeatDaily: Boolean
    ) {
        viewModelScope.launch {
            runCatching {
                trade.saveAlarm(
                    EntryAlarm(
                        label = label,
                        symbol = symbol,
                        triggerAt = triggerAt,
                        note = note,
                        enabled = true,
                        repeatDaily = repeatDaily
                    )
                )
            }.onSuccess { id ->
                // The row is stored; now hand it to the platform scheduler.
                AlarmScheduler.schedule(
                    app,
                    EntryAlarm(
                        id = id,
                        label = label,
                        symbol = symbol,
                        triggerAt = triggerAt,
                        note = note,
                        enabled = true,
                        repeatDaily = repeatDaily
                    )
                )
                _message.value = "Alarm $symbol dijadwalkan"
            }.onFailure {
                _message.value = "Gagal menyimpan alarm: ${it.message}"
            }
        }
    }

    fun toggleAlarm(alarm: EntryAlarm) {
        viewModelScope.launch {
            val enabled = !alarm.enabled
            runCatching { trade.setAlarmEnabled(alarm.id, enabled) }.onSuccess {
                if (enabled) AlarmScheduler.schedule(app, alarm.copy(enabled = true))
                else AlarmScheduler.cancel(app, alarm)
            }
        }
    }

    fun deleteAlarm(id: Long) {
        viewModelScope.launch { runCatching { trade.deleteAlarm(id) } }
    }

    fun clearMessage() {
        _message.value = null
    }

    // --------------------------------------------------------------- calculator

    private val _calcSymbol = MutableStateFlow("EURUSD")
    val calcSymbol: StateFlow<String> = _calcSymbol.asStateFlow()

    private val _calcLot = MutableStateFlow(0.10)
    val calcLot: StateFlow<Double> = _calcLot.asStateFlow()

    private val _calcPips = MutableStateFlow(20.0)
    val calcPips: StateFlow<Double> = _calcPips.asStateFlow()

    private val _calcLeverage = MutableStateFlow(100)
    val calcLeverage: StateFlow<Int> = _calcLeverage.asStateFlow()

    fun setCalcSymbol(value: String) {
        _calcSymbol.value = value
    }

    fun setCalcLot(value: Double) {
        _calcLot.value = value
    }

    fun setCalcPips(value: Double) {
        _calcPips.value = value
    }

    fun setCalcLeverage(value: Int) {
        _calcLeverage.value = value
    }

    /** Pip value, margin and profit/loss estimate for the current inputs. */
    fun calcResult(): PositionCalculator.CalcResult = PositionCalculator.calculate(
        instrument = Instruments.bySymbol(_calcSymbol.value),
        lot = _calcLot.value,
        currentPrice = market.quoteOf(_calcSymbol.value).price,
        pips = _calcPips.value,
        leverage = _calcLeverage.value
    )

    // ------------------------------------------------------------ risk planning

    private val _riskSymbol = MutableStateFlow("XAUUSD")
    val riskSymbol: StateFlow<String> = _riskSymbol.asStateFlow()

    private val _riskWinRate = MutableStateFlow(55.0)
    val riskWinRate: StateFlow<Double> = _riskWinRate.asStateFlow()

    fun setRiskSymbol(value: String) {
        _riskSymbol.value = value
    }

    fun setRiskWinRate(value: Double) {
        _riskWinRate.value = value
    }

    /** Money-management plan derived from the saved trading preferences. */
    fun riskPlan(): RiskCalculator.RiskPlan {
        val prefs = preferences.value
        return RiskCalculator.plan(
            instrument = Instruments.bySymbol(_riskSymbol.value),
            balance = prefs.balance,
            riskPercent = prefs.riskPercent,
            entry = market.quoteOf(_riskSymbol.value).price,
            slPercent = prefs.slPercent,
            tpRatio = prefs.tpRatio,
            winRate = _riskWinRate.value,
            leverage = prefs.leverage
        )
    }
}
