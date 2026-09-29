package com.mazkiplay.trade.data.repository

import com.mazkiplay.trade.data.api.CoinGeckoMarketDto
import com.mazkiplay.trade.data.api.NetworkModule
import com.mazkiplay.trade.data.model.CryptoMarketCoin
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/** Public/free crypto catalog. Listing refreshes detect newly ranked coins without synthetic data. */
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
    private var socket: WebSocket? = null
    private var reconnectAttempt = 0
    private var streamConnected = false

    private val streamSymbols = setOf(
        "BTC", "ETH", "BNB", "SOL", "XRP", "DOGE", "ADA", "AVAX", "LINK", "DOT",
        "TRX", "SHIB", "PEPE"
    )

    fun startAutoRefresh(intervalMillis: Long = 300_000L) {
        if (started) return
        started = true
        connectStream()
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
            // Public/free is rate-limited; 250 spot + 250 meme is the safe practical cap.
            val spot = NetworkModule.coinGeckoApi.markets(
                perPage = 250, page = 1, sparkline = true
            ).mapNotNull { it.toModel("Spot") }
            val meme = NetworkModule.coinGeckoApi.markets(
                perPage = 250, page = 1, sparkline = true, category = "meme-token"
            ).mapNotNull { it.toModel("Meme Coin") }
            (spot + meme).distinctBy { it.id }
        }.onSuccess { result ->
            if (result.isNotEmpty()) {
                _coins.value = result
                _lastUpdated.value = System.currentTimeMillis()
                _lastError.value = null
                _status.value = if (streamConnected) CryptoFeedStatus.STREAMING else CryptoFeedStatus.LIVE
            } else {
                _status.value = if (_coins.value.isEmpty()) CryptoFeedStatus.OFFLINE else CryptoFeedStatus.STALE
                _lastError.value = "CoinGecko mengembalikan daftar kosong"
            }
        }.onFailure { error ->
            _status.value = if (_coins.value.isEmpty()) CryptoFeedStatus.OFFLINE else CryptoFeedStatus.STALE
            _lastError.value = error.message ?: "Feed crypto tidak tersedia"
        }
    }

    /** Public Binance market-data stream; no trading permission or secret is required. */
    private fun connectStream() {
        val streams = streamSymbols.joinToString("/") { "${it.lowercase()}usdt@ticker" }
        val request = Request.Builder()
            .url("wss://stream.binance.com:9443/stream?streams=$streams")
            .build()
        socket?.cancel()
        socket = NetworkModule.client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempt = 0
                streamConnected = true
                _status.value = CryptoFeedStatus.STREAMING
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val data = JsonParser.parseString(text).asJsonObject.getAsJsonObject("data")
                    val symbol = data.get("s")?.asString?.removeSuffix("USDT") ?: return@runCatching
                    val now = System.currentTimeMillis()
                    _coins.value = _coins.value.map { coin ->
                        if (coin.symbol == symbol) coin.copy(
                            currentPrice = data.get("c")?.asDouble ?: coin.currentPrice,
                            priceChangePercentage24h = data.get("P")?.asDouble ?: coin.priceChangePercentage24h,
                            totalVolume = data.get("v")?.asDouble ?: coin.totalVolume,
                            lastUpdated = now,
                            dataSource = "BINANCE_WS",
                            isRealtime = true
                        ) else coin
                    }
                    _lastUpdated.value = now
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                streamConnected = false
                _status.value = if (_coins.value.isEmpty()) CryptoFeedStatus.OFFLINE else CryptoFeedStatus.STALE
                reconnectAttempt = (reconnectAttempt + 1).coerceAtMost(6)
                scope.launch {
                    delay((1L shl reconnectAttempt) * 1_000L)
                    if (started) connectStream()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (started) {
                    streamConnected = false
                    _status.value = CryptoFeedStatus.STALE
                    scope.launch {
                        delay(5_000L)
                        if (started) connectStream()
                    }
                }
            }
        })
    }

    private fun CoinGeckoMarketDto.toModel(group: String): CryptoMarketCoin? {
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
            marketGroup = group,
            sparkline7d = sparklineIn7d?.price.orEmpty(),
            lastUpdated = System.currentTimeMillis(),
            dataSource = "COINGECKO",
            isRealtime = false
        )
    }
}

enum class CryptoFeedStatus(val label: String) {
    CONNECTING("MENGHUBUNGKAN"),
    STREAMING("STREAMING"),
    LIVE("LIVE"),
    STALE("TERTUNDA"),
    OFFLINE("OFFLINE")
}
