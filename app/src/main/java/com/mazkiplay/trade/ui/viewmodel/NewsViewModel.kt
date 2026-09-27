package com.mazkiplay.trade.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.EconomicEvent
import com.mazkiplay.trade.data.model.NewsItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * News room state: the headline feed, the category filter and the economic calendar.
 *
 * Filtering happens here rather than in the screen so swapping the chip does not
 * re-request anything from the network.
 */
class NewsViewModel(private val app: MazkiplayApp) : ViewModel() {

    private val repository = app.newsRepository
    private val market = app.marketRepository

    val news: StateFlow<List<NewsItem>> = repository.news
    val newIds: StateFlow<Set<String>> = repository.newIds
    val loading: StateFlow<Boolean> = repository.loading
    val lastUpdated: StateFlow<Long> = repository.lastUpdated

    /** Calendar rows come from the market repository, which refreshes them in place. */
    val events: StateFlow<List<EconomicEvent>> = market.events

    private val _category = MutableStateFlow(DEFAULT_CATEGORY)
    val category: StateFlow<String> = _category.asStateFlow()

    /** Headlines narrowed to the active chip. */
    val filtered: StateFlow<List<NewsItem>> =
        combine(repository.news, _category) { items, selected ->
            if (selected == DEFAULT_CATEGORY) items else items.filter { it.category == selected }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * The fixed chip list, trimmed to the categories actually present in the feed so
     * the row never shows a filter that can only ever return nothing.
     */
    val categories: StateFlow<List<String>> = repository.news
        .map { items ->
            val present = items.map { it.category }.toSet()
            BASE_CATEGORIES.filter { it == DEFAULT_CATEGORY || present.contains(it) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, BASE_CATEGORIES)

    fun setCategory(value: String) {
        _category.value = value
        // Anything the user has now looked at is no longer "BARU".
        repository.markSeen()
    }

    private companion object {
        const val DEFAULT_CATEGORY = "Semua"
        val BASE_CATEGORIES = listOf(
            "Semua", "Forex", "Central Bank", "Gold", "Crypto", "Commodity", "Equity"
        )
    }
}
