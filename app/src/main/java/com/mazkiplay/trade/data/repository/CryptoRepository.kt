package com.mazkiplay.trade.data.repository

import com.mazkiplay.trade.data.api.CoinGeckoMarketDto
import com.mazkiplay.trade.data.api.NetworkModule
import com.mazkiplay.trade.data.model.CryptoMarketCoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Public/free crypto market catalog. It is intentionally capped to avoid API abuse. */
class CryptoRepository(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val _coins = MutableStateFlow<List<CryptoMarketCoin>>(emptyList())
    val coins: StateFlow<List<CryptoMarketCoin>> = _coins.asStateFlow()

    private val _status = MutableStateFlow(CryptoFeedStatus.CONNECTING)
    val status: StateFlow<CryptoFeedStatus> = _status.asStateFlow()

    private val _lastUpdated = MutableStateFlow(0L)
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var started = false

    fun startAutoRefresh(intervalMillis: Long = 300_000L) {
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
        runCatching {
            // CoinGecko public/free access is rate-limited; one page of 100 is deliberate.
            NetworkModule.coinGeckoApi.markets(perPage = 100, page = 1)
                .mapNotNull { it.toModel() }
        }.onSuccess { result ->
            if (result.isNotEmpty()) {
                _coins.value = result
                _lastUpdated.value = System.currentTimeMillis()
                _lastError.value = null
                _status.value = CryptoFeedStatus.LIVE
            } else {
                _status.value = if (_coins.value.isEmpty()) CryptoFeedStatus.OFFLINE else CryptoFeedStatus.STALE
                _lastError.value = "CoinGecko mengembalikan daftar kosong"
            }
        }.onFailure { error ->
            _status.value = if (_coins.value.isEmpty()) CryptoFeedStatus.OFFLINE else CryptoFeedStatus.STALE
            _lastError.value = error.message ?: "Feed crypto tidak tersedia"
        }
    }

    private fun CoinGeckoMarketDto.toModel(): CryptoMarketCoin? {
        val safeId = id?.takeIf { it.isNotBlank() } ?: return null
        return CryptoMarketCoin(
            id = safeId,
            symbol = symbol.orEmpty().uppercase(),
            name = name.orEmpty(),
            imageUrl = image,
            currentPrice = currentPrice,
            marketCap = marketCap,
            marketCapRank = marketCapRank,
            totalVolume = totalVolume,
            priceChange24h = priceChange24h,
            priceChangePercentage24h = priceChangePercentage24h,
            circulatingSupply = circulatingSupply,
            lastUpdated = System.currentTimeMillis()
        )
    }
}

enum class CryptoFeedStatus(val label: String) {
    CONNECTING("MENGHUBUNGKAN"),
    LIVE("LIVE"),
    STALE("TERTUNDA"),
    OFFLINE("OFFLINE")
}
