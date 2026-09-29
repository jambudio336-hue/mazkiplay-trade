package com.mazkiplay.trade.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mazkiplay.trade.util.AppConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mazkiplay_settings")

/** Everything the Settings screen can change, persisted with DataStore. */
data class UserPreferences(
    val language: String = "in",
    val theme: String = "dark",
    val brightness: Float = 1.0f,
    val timeFormat: String = "24h",
    val dateFormat: String = "dd/MM/yyyy",
    val balance: Double = AppConstants.DEFAULT_BALANCE,
    val riskPercent: Double = AppConstants.DEFAULT_RISK_PERCENT,
    val slPercent: Double = AppConstants.DEFAULT_SL_PERCENT,
    val tpRatio: Double = 2.0,
    val leverage: Int = AppConstants.DEFAULT_LEVERAGE,
    val newsAlert: Boolean = true,
    val priceAlert: Boolean = true,
    val entryAlert: Boolean = true,
    val autoRefresh: Boolean = true,
    val watchlist: Set<String> = setOf("XAUUSD", "EURUSD", "GBPUSD", "USDJPY", "AUDUSD"),
    val copySubscriptions: Set<String> = emptySet(),
    val defaultTimeframe: String = "M15",
    val priceAlertThreshold: Double = 1.0
) {
    /** SimpleDateFormat pattern matching the chosen time format. */
    val timePattern: String get() = if (timeFormat == "24h") "HH:mm" else "hh:mm a"
    val dateTimePattern: String get() = "$dateFormat $timePattern"
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val language = stringPreferencesKey("language")
        val theme = stringPreferencesKey("theme")
        val brightness = floatPreferencesKey("brightness")
        val timeFormat = stringPreferencesKey("time_format")
        val dateFormat = stringPreferencesKey("date_format")
        val balance = floatPreferencesKey("balance")
        val riskPercent = floatPreferencesKey("risk_percent")
        val slPercent = floatPreferencesKey("sl_percent")
        val tpRatio = floatPreferencesKey("tp_ratio")
        val leverage = intPreferencesKey("leverage")
        val newsAlert = booleanPreferencesKey("alert_news")
        val priceAlert = booleanPreferencesKey("alert_price")
        val entryAlert = booleanPreferencesKey("alert_entry")
        val autoRefresh = booleanPreferencesKey("auto_refresh")
        val watchlist = stringSetPreferencesKey("watchlist")
        val copySubs = stringSetPreferencesKey("copy_subscriptions")
        val defaultTimeframe = stringPreferencesKey("default_timeframe")
        val priceAlertThreshold = floatPreferencesKey("price_alert_threshold")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            language = "in",
            theme = prefs[Keys.theme] ?: "dark",
            brightness = prefs[Keys.brightness] ?: 1.0f,
            timeFormat = prefs[Keys.timeFormat] ?: "24h",
            dateFormat = prefs[Keys.dateFormat] ?: "dd/MM/yyyy",
            balance = (prefs[Keys.balance] ?: AppConstants.DEFAULT_BALANCE.toFloat()).toDouble(),
            riskPercent = (prefs[Keys.riskPercent] ?: AppConstants.DEFAULT_RISK_PERCENT.toFloat()).toDouble(),
            slPercent = (prefs[Keys.slPercent] ?: AppConstants.DEFAULT_SL_PERCENT.toFloat()).toDouble(),
            tpRatio = (prefs[Keys.tpRatio] ?: 2.0f).toDouble(),
            leverage = prefs[Keys.leverage] ?: AppConstants.DEFAULT_LEVERAGE,
            newsAlert = prefs[Keys.newsAlert] ?: true,
            priceAlert = prefs[Keys.priceAlert] ?: true,
            entryAlert = prefs[Keys.entryAlert] ?: true,
            autoRefresh = prefs[Keys.autoRefresh] ?: true,
            watchlist = prefs[Keys.watchlist] ?: UserPreferences().watchlist,
            copySubscriptions = prefs[Keys.copySubs] ?: emptySet(),
            defaultTimeframe = prefs[Keys.defaultTimeframe] ?: "M15",
            priceAlertThreshold = (prefs[Keys.priceAlertThreshold] ?: 1.0f).toDouble()
        )
    }

    suspend fun current(): UserPreferences = preferences.first()

    suspend fun setLanguage(value: String) = edit { it[Keys.language] = "in" }
    suspend fun setTheme(value: String) = edit { it[Keys.theme] = value }
    suspend fun setBrightness(value: Float) = edit { it[Keys.brightness] = value }
    suspend fun setTimeFormat(value: String) = edit { it[Keys.timeFormat] = value }
    suspend fun setDateFormat(value: String) = edit { it[Keys.dateFormat] = value }
    suspend fun setBalance(value: Double) = edit { it[Keys.balance] = value.toFloat() }
    suspend fun setRiskPercent(value: Double) = edit { it[Keys.riskPercent] = value.toFloat() }
    suspend fun setSlPercent(value: Double) = edit { it[Keys.slPercent] = value.toFloat() }
    suspend fun setTpRatio(value: Double) = edit { it[Keys.tpRatio] = value.toFloat() }
    suspend fun setLeverage(value: Int) = edit { it[Keys.leverage] = value }
    suspend fun setNewsAlert(value: Boolean) = edit { it[Keys.newsAlert] = value }
    suspend fun setPriceAlert(value: Boolean) = edit { it[Keys.priceAlert] = value }
    suspend fun setEntryAlert(value: Boolean) = edit { it[Keys.entryAlert] = value }
    suspend fun setAutoRefresh(value: Boolean) = edit { it[Keys.autoRefresh] = value }
    suspend fun setDefaultTimeframe(value: String) = edit { it[Keys.defaultTimeframe] = value }
    suspend fun setPriceAlertThreshold(value: Double) =
        edit { it[Keys.priceAlertThreshold] = value.toFloat() }

    suspend fun toggleWatchlist(symbol: String) = edit { prefs ->
        val current = prefs[Keys.watchlist]?.toMutableSet() ?: UserPreferences().watchlist.toMutableSet()
        if (current.contains(symbol)) current.remove(symbol) else current.add(symbol)
        prefs[Keys.watchlist] = current
    }

    suspend fun toggleCopySubscription(id: String) = edit { prefs ->
        val current = prefs[Keys.copySubs]?.toMutableSet() ?: mutableSetOf()
        if (current.contains(id)) current.remove(id) else current.add(id)
        prefs[Keys.copySubs] = current
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
