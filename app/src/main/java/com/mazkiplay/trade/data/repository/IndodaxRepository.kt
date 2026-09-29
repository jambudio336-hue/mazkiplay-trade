package com.mazkiplay.trade.data.repository

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mazkiplay.trade.data.api.IndodaxApi
import com.mazkiplay.trade.data.api.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Read-only Indonesian crypto market adapter for dashboard/radar use. */
class IndodaxRepository(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val api: IndodaxApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://indodax.com/")
            .client(NetworkModule.client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IndodaxApi::class.java)
    }

    private val _markets = MutableStateFlow<List<IndodaxMarket>>(emptyList())
    val markets: StateFlow<List<IndodaxMarket>> = _markets.asStateFlow()
    private val _status = MutableStateFlow(IndodaxFeedStatus.CONNECTING)
    val status: StateFlow<IndodaxFeedStatus> = _status.asStateFlow()
    private val _lastUpdated = MutableStateFlow(0L)
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()
    private var started = false

    fun startAutoRefresh(intervalMillis: Long = 30_000L) {
        if (started) return
        started = true
        scope.launch {
            refresh()
            while (isActive) {
                delay(intervalMillis)
                refresh()
            }
        }
    }

    suspend fun refresh() {
        runCatching { api.tickerAll() }
            .onSuccess { body ->
                val tickers = body.getAsJsonObject("tickers") ?: body.getAsJsonObject("ticker")
                val parsed = tickers?.entrySet().orEmpty().mapNotNull { (pair, raw) ->
                    parseTicker(pair, raw.asJsonObject)
                }.sortedByDescending { it.quoteVolume }
                if (parsed.isNotEmpty()) {
                    _markets.value = parsed
                    _lastUpdated.value = System.currentTimeMillis()
                    _status.value = IndodaxFeedStatus.LIVE
                } else {
                    _status.value = if (_markets.value.isEmpty()) IndodaxFeedStatus.OFFLINE else IndodaxFeedStatus.STALE
                }
            }
            .onFailure {
                _status.value = if (_markets.value.isEmpty()) IndodaxFeedStatus.OFFLINE else IndodaxFeedStatus.STALE
            }
    }

    suspend fun loadDepth(pair: String): IndodaxDepth? = runCatching {
        val response = api.depth(pair)
        val root = response.getAsJsonObject("depth") ?: response
        val bids = root.getAsJsonArray("buy").orEmptyPairs()
        val asks = root.getAsJsonArray("sell").orEmptyPairs()
        IndodaxDepth(pair, bids, asks)
    }.getOrNull()

    private fun parseTicker(pair: String, raw: JsonObject): IndodaxMarket? {
        fun n(key: String) = raw.get(key)?.asString?.toDoubleOrNull() ?: raw.get(key)?.asDouble ?: 0.0
        val last = n("last")
        if (last <= 0.0) return null
        return IndodaxMarket(
            pair = pair.uppercase(),
            last = last,
            buy = n("buy"),
            sell = n("sell"),
            high = n("high"),
            low = n("low"),
            quoteVolume = n("vol_idr").takeIf { it > 0 } ?: n("vol_btc"),
            updatedAt = System.currentTimeMillis()
        )
    }
}

data class IndodaxMarket(
    val pair: String,
    val last: Double,
    val buy: Double,
    val sell: Double,
    val high: Double,
    val low: Double,
    val quoteVolume: Double,
    val updatedAt: Long
) {
    val spread: Double get() = (sell - buy).coerceAtLeast(0.0)
    val mid: Double get() = if (buy > 0 && sell > 0) (buy + sell) / 2.0 else last
    val rangePosition: Double get() = if (high > low) ((last - low) / (high - low) * 100.0).coerceIn(0.0, 100.0) else 50.0
}

data class IndodaxDepth(
    val pair: String,
    val bids: List<Pair<Double, Double>>,
    val asks: List<Pair<Double, Double>>
) {
    val bidVolume: Double get() = bids.sumOf { it.second }
    val askVolume: Double get() = asks.sumOf { it.second }
    val imbalance: Double get() = if (bidVolume + askVolume == 0.0) 0.0 else (bidVolume - askVolume) / (bidVolume + askVolume)
}

enum class IndodaxFeedStatus(val label: String) {
    CONNECTING("MENGHUBUNGKAN"), LIVE("LIVE"), STALE("STALE"), OFFLINE("OFFLINE")
}

private fun com.google.gson.JsonArray?.orEmptyPairs(): List<Pair<Double, Double>> = this?.mapNotNull { row ->
    row.asJsonArray.takeIf { it.size() >= 2 }?.let {
        val p = it[0].asString.toDoubleOrNull() ?: return@let null
        val q = it[1].asString.toDoubleOrNull() ?: return@let null
        p to q
    }
}.orEmpty()
