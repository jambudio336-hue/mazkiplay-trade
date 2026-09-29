package com.mazkiplay.trade

import android.app.Application
import com.mazkiplay.trade.data.local.MazkiplayDatabase
import com.mazkiplay.trade.data.repository.CopyTradeRepository
import com.mazkiplay.trade.data.repository.CryptoRepository
import com.mazkiplay.trade.data.repository.IndodaxRepository
import com.mazkiplay.trade.data.repository.VideoNewsRepository
import com.mazkiplay.trade.data.repository.MarketRepository
import com.mazkiplay.trade.data.repository.NewsRepository
import com.mazkiplay.trade.data.repository.SettingsRepository
import com.mazkiplay.trade.data.repository.TradeRepository
import com.mazkiplay.trade.data.tradingview.TvRepository
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
    val cryptoRepository: CryptoRepository by lazy { CryptoRepository() }
    val indodaxRepository: IndodaxRepository by lazy { IndodaxRepository() }
    val videoNewsRepository: VideoNewsRepository by lazy { VideoNewsRepository() }

    /**
     * The primary live feed. Every screen reads its own state, so one poll serves the
     * dashboard ticker, watchlist, screener, heatmap and signal panel at once.
     */
    val tvRepository: TvRepository by lazy { TvRepository() }

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
        cryptoRepository.startAutoRefresh(intervalMillis = 300_000L)
        indodaxRepository.startAutoRefresh(intervalMillis = 30_000L)
        videoNewsRepository.startAutoRefresh(intervalMillis = 300_000L)

        // TradingView runs the fast lane: prices and technicals every 10s, calendar
        // every 4 minutes, news every 2 minutes. The Yahoo-backed repositories above
        // stay as the degraded fallback path.
        appScope.launch {
            val prefs = settings.current()
            tvRepository.setSymbols(prefs.watchlist.toList())
            tvRepository.setSignalPreferences(
                tpRatio = prefs.tpRatio,
                slPercent = prefs.slPercent,
                timeframeLabel = prefs.defaultTimeframe
            )
            tvRepository.setAlertThreshold(prefs.priceAlertThreshold)
        }
        tvRepository.start(intervalMillis = 10_000L, newsIntervalMillis = 120_000L)

        // Surface TradingView headlines and imminent high-impact releases through the
        // same notification channel the Yahoo feed already uses.
        tvRepository.onPriceAlert = { symbol, quote ->
            appScope.launch {
                val prefs = settings.current()
                if (prefs.priceAlert) {
                    val digits = com.mazkiplay.trade.data.tradingview.TvTickers
                        .instrumentOf(symbol).digits
                    notifications.notifyPrice(
                        symbol = symbol,
                        price = quote.close,
                        changePercent = quote.changePercent,
                        digits = digits
                    )
                }
            }
        }

        appScope.launch {
            tvRepository.news.collect { items ->
                items.firstOrNull { it.isFresh && it.isBreaking }?.let { breaking ->
                    val prefs = settings.current()
                    if (prefs.newsAlert) {
                        notifications.notifyNews(breaking.title, breaking.source, breaking.url)
                    }
                }
            }
        }
    }
}
