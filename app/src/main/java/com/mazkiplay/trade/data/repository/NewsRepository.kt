package com.mazkiplay.trade.data.repository

import com.mazkiplay.trade.data.api.MarketDataSource
import com.mazkiplay.trade.data.model.NewsItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Headline feed with change detection.
 *
 * New items are tracked by id so the UI can flag "baru" and the notification layer
 * can fire exactly one alert per fresh headline, even across refreshes.
 */
class NewsRepository(
    private val dataSource: MarketDataSource = MarketDataSource()
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _news = MutableStateFlow<List<NewsItem>>(emptyList())
    val news: StateFlow<List<NewsItem>> = _news.asStateFlow()

    private val _newIds = MutableStateFlow<Set<String>>(emptySet())
    val newIds: StateFlow<Set<String>> = _newIds.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _lastUpdated = MutableStateFlow(0L)
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()

    /** Optional callback so the background worker can raise a notification. */
    var onFreshHeadlines: ((List<NewsItem>) -> Unit)? = null

    private var knownIds: Set<String> = emptySet()

    fun startAutoRefresh(intervalMillis: Long = 300_000L) {
        scope.launch {
            refresh()
            while (isActive) {
                delay(intervalMillis)
                runCatching { refresh() }
            }
        }
    }

    suspend fun refresh() {
        _loading.value = true
        val fetched = dataSource.news()
        if (fetched.isNotEmpty()) {
            val fresh = fetched.filter { it.id !in knownIds }
            knownIds = knownIds + fetched.map { it.id }
            if (knownIds.size > 400) knownIds = knownIds.toList().takeLast(400).toSet()
            _news.value = fetched
            _newIds.value = fresh.map { it.id }.toSet()
            _lastUpdated.value = System.currentTimeMillis()
            if (fresh.isNotEmpty()) onFreshHeadlines?.invoke(fresh)
        }
        _loading.value = false
    }

    fun byCategory(category: String): List<NewsItem> =
        if (category == "Semua") _news.value else _news.value.filter { it.category == category }

    fun categories(): List<String> {
        val base = listOf("Semua", "Forex", "Central Bank", "Gold", "Crypto", "Commodity", "Equity")
        val present = _news.value.map { it.category }.toSet()
        return base.filter { it == "Semua" || present.contains(it) }
    }

    fun markSeen() {
        _newIds.value = emptySet()
    }
}
