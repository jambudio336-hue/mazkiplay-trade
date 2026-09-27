package com.mazkiplay.trade

import android.app.Application
import com.mazkiplay.trade.data.local.MazkiplayDatabase
import com.mazkiplay.trade.data.repository.CopyTradeRepository
import com.mazkiplay.trade.data.repository.MarketRepository
import com.mazkiplay.trade.data.repository.NewsRepository
import com.mazkiplay.trade.data.repository.SettingsRepository
import com.mazkiplay.trade.data.repository.TradeRepository
import com.mazkiplay.trade.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point and hand-rolled service locator.
 *
 * A DI framework would be overkill here: the graph is small, flat and has no
 * runtime variants, so lazy singletons give the same wiring with far less machinery.
 */
class MazkiplayApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: MazkiplayDatabase by lazy { MazkiplayDatabase.get(this) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
    val marketRepository: MarketRepository by lazy { MarketRepository() }
    val newsRepository: NewsRepository by lazy { NewsRepository() }
    val tradeRepository: TradeRepository by lazy { TradeRepository(database) }
    val copyTradeRepository: CopyTradeRepository by lazy { CopyTradeRepository() }
    val notifications: NotificationHelper by lazy { NotificationHelper(this) }

    override fun onCreate() {
        super.onCreate()

        notifications.createChannels()

        // Headline alerts are raised from the repository so both the foreground UI
        // and the background worker share the exact same detection logic.
        newsRepository.onFreshHeadlines = { fresh ->
            appScope.launch {
                val prefs = settings.current()
                if (prefs.newsAlert) {
                    val top = fresh.firstOrNull() ?: return@launch
                    notifications.notifyNews(top.title, top.source, top.url)
                }
            }
        }

        // Warm the caches immediately so the first frame already has data.
        appScope.launch {
            val prefs = settings.current()
            marketRepository.setTrackedSymbols(prefs.watchlist.toList())
            marketRepository.refreshAll()
            newsRepository.refresh()
        }

        marketRepository.startAutoRefresh(intervalMillis = 60_000L)
        newsRepository.startAutoRefresh(intervalMillis = 300_000L)
        copyTradeRepository.startAutoRefresh(intervalMillis = 120_000L)
    }
}
