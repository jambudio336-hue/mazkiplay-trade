package com.mazkiplay.trade.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.util.AppConstants
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Background auto-refresh.
 *
 * WorkManager keeps the app's data warm while it is not on screen: prices and the
 * calendar every 30 minutes (the platform minimum for periodic work), headlines and
 * the copy-trade desk alongside them. Price alerts are raised here so they still
 * arrive while the app is backgrounded.
 */
class MarketRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val WORK_NAME = "mazkiplay_market_refresh"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<MarketRefreshWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as? MazkiplayApp ?: return Result.failure()
        return runCatching {
            val prefs = app.settings.preferences.first()
            if (!prefs.autoRefresh) return Result.success()

            val before = app.marketRepository.quotes.value
            app.marketRepository.setTrackedSymbols(prefs.watchlist.toList())
            app.marketRepository.refreshAll()
            app.newsRepository.refresh()
            app.copyTradeRepository.refresh()

            // Calendar alerts use the same background refresh and are deduplicated
            // locally by NotificationHelper, so a 30-minute worker does not spam.
            if (prefs.newsAlert) {
                val now = System.currentTimeMillis()
                app.marketRepository.upcomingHighImpact(10)
                    .filter { it.dateMillis in now..(now + 15 * 60_000L) }
                    .forEach(app.notifications::notifyEconomicEvent)
            }

            // Price alert: compare the refreshed quotes against the previous snapshot.
            if (prefs.priceAlert) {
                app.marketRepository.quotes.value.forEach { (symbol, quote) ->
                    val previous = before[symbol]
                    if (previous != null && previous.price > 0.0 && quote.price > 0.0) {
                        val movePercent = (quote.price - previous.price) / previous.price * 100.0
                        if (kotlin.math.abs(movePercent) >= 0.35) {
                            val digits = app.marketRepository.instrumentOf(symbol).digits
                            app.notifications.notifyPrice(symbol, quote.price, movePercent, digits)
                        }
                    }
                }
            }
            Result.success()
        }.getOrElse { Result.retry() }
    }
}

/** Convenience holder so the constants stay discoverable from one place. */
object RefreshIntervals {
    val PRICE_MINUTES = AppConstants.PRICE_REFRESH_MINUTES
    val NEWS_MINUTES = AppConstants.NEWS_REFRESH_MINUTES
}
