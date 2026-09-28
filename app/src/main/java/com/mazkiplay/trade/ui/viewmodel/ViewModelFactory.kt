package com.mazkiplay.trade.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mazkiplay.trade.MazkiplayApp

/**
 * Builds every ViewModel in the app from the single [MazkiplayApp] service locator.
 *
 * Screens request a type through `appViewModel<T>()`, which arrives here with the
 * concrete class. Resolving it in one place keeps six constructors out of the
 * composition layer and guarantees every screen sees the same repository instances.
 */
class ViewModelFactory private constructor(
    private val app: MazkiplayApp,
    private val target: Class<out ViewModel>
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = build() as T

    private fun build(): ViewModel = when (target) {
        MarketViewModel::class.java -> MarketViewModel(app)
        NewsViewModel::class.java -> NewsViewModel(app)
        TradeViewModel::class.java -> TradeViewModel(app)
        AnalysisViewModel::class.java -> AnalysisViewModel(app)
        CopyTradeViewModel::class.java -> CopyTradeViewModel(app)
        SettingsViewModel::class.java -> SettingsViewModel(app)
        LiveViewModel::class.java -> LiveViewModel(app)
        else -> error("No ViewModel registered for ${target.name}")
    }

    companion object {
        fun create(app: MazkiplayApp, modelClass: Class<out ViewModel>): ViewModelFactory =
            ViewModelFactory(app, modelClass)
    }
}
