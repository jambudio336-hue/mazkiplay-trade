package com.mazkiplay.trade.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.repository.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Settings screen state.
 *
 * Every write goes straight into DataStore, and the single [preferences] flow is the
 * only thing the screen reads, so a change is reflected everywhere the moment it
 * commits rather than being held in a parallel copy of the state.
 */
class SettingsViewModel(private val app: MazkiplayApp) : ViewModel() {

    private val settings = app.settings

    val preferences: StateFlow<UserPreferences> = settings.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    fun setLanguage(value: String) = submit { settings.setLanguage(value) }

    fun setTheme(value: String) = submit { settings.setTheme(value) }

    fun setBrightness(value: Float) = submit { settings.setBrightness(value) }

    fun setTimeFormat(value: String) = submit { settings.setTimeFormat(value) }

    fun setDateFormat(value: String) = submit { settings.setDateFormat(value) }

    fun setBalance(value: Double) = submit { settings.setBalance(value) }

    fun setRiskPercent(value: Double) = submit { settings.setRiskPercent(value) }

    fun setSlPercent(value: Double) = submit { settings.setSlPercent(value) }

    fun setTpRatio(value: Double) = submit { settings.setTpRatio(value) }

    fun setLeverage(value: Int) = submit { settings.setLeverage(value) }

    fun setNewsAlert(value: Boolean) = submit { settings.setNewsAlert(value) }

    fun setPriceAlert(value: Boolean) = submit { settings.setPriceAlert(value) }

    fun setEntryAlert(value: Boolean) = submit { settings.setEntryAlert(value) }

    fun setAutoRefresh(value: Boolean) = submit { settings.setAutoRefresh(value) }

    /** Fire-and-forget write; a failed commit must never take the screen down. */
    private fun submit(block: suspend () -> Unit) {
        viewModelScope.launch { runCatching { block() } }
    }
}
