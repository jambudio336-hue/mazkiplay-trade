package com.mazkiplay.trade.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.ui.viewmodel.AnalysisViewModel
import com.mazkiplay.trade.ui.viewmodel.CopyTradeViewModel
import com.mazkiplay.trade.ui.viewmodel.LiveViewModel
import com.mazkiplay.trade.ui.viewmodel.MarketViewModel
import com.mazkiplay.trade.ui.viewmodel.NewsViewModel
import com.mazkiplay.trade.ui.viewmodel.SettingsViewModel
import com.mazkiplay.trade.ui.viewmodel.TradeViewModel
import com.mazkiplay.trade.ui.viewmodel.VideoNewsViewModel
import com.mazkiplay.trade.ui.viewmodel.ViewModelFactory
import com.mazkiplay.trade.util.AppStrings
import com.mazkiplay.trade.util.LocaleStrings

/**
 * Small composition-local helpers.
 *
 * Each screen reads its own preferences snapshot and its own ViewModel through these
 * three functions, which keeps the navigation graph free of plumbing and means a
 * screen is always driven by the current settings values.
 */

@Composable
fun rememberPreferences(app: MazkiplayApp): UserPreferences {
    val prefs by app.settings.preferences.collectAsStateWithLifecycle(initialValue = UserPreferences())
    return prefs
}

@Composable
fun stringsOf(prefs: UserPreferences): AppStrings = remember(prefs.language) {
    LocaleStrings.of(prefs.language)
}

@Composable
inline fun <reified T : ViewModel> appViewModel(app: MazkiplayApp): T =
    viewModel(factory = ViewModelFactory.create(app, T::class.java))

@Composable
fun marketViewModel(app: MazkiplayApp): MarketViewModel = appViewModel(app)

@Composable
fun newsViewModel(app: MazkiplayApp): NewsViewModel = appViewModel(app)

@Composable
fun tradeViewModel(app: MazkiplayApp): TradeViewModel = appViewModel(app)

@Composable
fun analysisViewModel(app: MazkiplayApp): AnalysisViewModel = appViewModel(app)

@Composable
fun copyTradeViewModel(app: MazkiplayApp): CopyTradeViewModel = appViewModel(app)

@Composable
fun settingsViewModel(app: MazkiplayApp): SettingsViewModel = appViewModel(app)

@Composable
fun liveViewModel(app: MazkiplayApp): LiveViewModel = appViewModel(app)

@Composable
fun videoNewsViewModel(app: MazkiplayApp): VideoNewsViewModel = appViewModel(app)
