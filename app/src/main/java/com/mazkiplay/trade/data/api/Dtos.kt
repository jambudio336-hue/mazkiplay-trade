package com.mazkiplay.trade.data.api

import com.google.gson.annotations.SerializedName

/** ---------------------------------------------------------------- Yahoo chart */

data class YahooChartResponse(
    @SerializedName("chart") val chart: YahooChart?
)

data class YahooChart(
    @SerializedName("result") val result: List<YahooResult>?,
    @SerializedName("error") val error: YahooError?
)

data class YahooError(
    @SerializedName("code") val code: String?,
    @SerializedName("description") val description: String?
)

data class YahooResult(
    @SerializedName("meta") val meta: YahooMeta?,
    @SerializedName("timestamp") val timestamp: List<Long>?,
    @SerializedName("indicators") val indicators: YahooIndicators?
)

data class YahooMeta(
    @SerializedName("symbol") val symbol: String?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("exchangeName") val exchangeName: String?,
    @SerializedName("regularMarketPrice") val regularMarketPrice: Double?,
    @SerializedName("previousClose") val previousClose: Double?,
    @SerializedName("chartPreviousClose") val chartPreviousClose: Double?,
    @SerializedName("regularMarketDayHigh") val regularMarketDayHigh: Double?,
    @SerializedName("regularMarketDayLow") val regularMarketDayLow: Double?,
    @SerializedName("regularMarketTime") val regularMarketTime: Long?
)

data class YahooIndicators(
    @SerializedName("quote") val quote: List<YahooQuote>?
)

data class YahooQuote(
    @SerializedName("open") val open: List<Double?>?,
    @SerializedName("high") val high: List<Double?>?,
    @SerializedName("low") val low: List<Double?>?,
    @SerializedName("close") val close: List<Double?>?,
    @SerializedName("volume") val volume: List<Long?>?
)

/** ------------------------------------------------------- Forex Factory calendar */

data class FfEventDto(
    @SerializedName("title") val title: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("impact") val impact: String?,
    @SerializedName("forecast") val forecast: String?,
    @SerializedName("previous") val previous: String?,
    @SerializedName("actual") val actual: String?
)

/** --------------------------------------------------------- CoinGecko public market */
data class CoinGeckoMarketDto(
    @SerializedName("id") val id: String?,
    @SerializedName("symbol") val symbol: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("current_price") val currentPrice: Double?,
    @SerializedName("market_cap") val marketCap: Double?,
    @SerializedName("market_cap_rank") val marketCapRank: Int?,
    @SerializedName("total_volume") val totalVolume: Double?,
    @SerializedName("price_change_24h") val priceChange24h: Double?,
    @SerializedName("price_change_percentage_24h") val priceChangePercentage24h: Double?,
    @SerializedName("circulating_supply") val circulatingSupply: Double?,
    @SerializedName("last_updated") val lastUpdated: String?
)

/** ------------------------------------------------------------- Copy-trade feed */

/**
 * Shape of the public signal-provider list. It is served as a plain JSON array so
 * the app can be pointed at any compatible endpoint without a schema change.
 */
data class SignalProviderDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("flag") val flag: String?,
    @SerializedName("strategy") val strategy: String?,
    @SerializedName("riskLevel") val riskLevel: String?,
    @SerializedName("monthlyReturn") val monthlyReturn: Double?,
    @SerializedName("totalReturn") val totalReturn: Double?,
    @SerializedName("maxDrawdown") val maxDrawdown: Double?,
    @SerializedName("winRate") val winRate: Double?,
    @SerializedName("followers") val followers: Int?,
    @SerializedName("aum") val aum: Double?,
    @SerializedName("trades") val trades: Int?,
    @SerializedName("rewardPercent") val rewardPercent: Double?,
    @SerializedName("equityCurve") val equityCurve: List<Double>?,
    @SerializedName("topSymbols") val topSymbols: List<String>?,
    @SerializedName("verified") val verified: Boolean?
)
