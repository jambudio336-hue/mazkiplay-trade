package com.mazkiplay.trade.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Yahoo Finance public chart endpoint (no API key required). */
interface MarketApi {

    @GET("v8/finance/chart/{symbol}")
    suspend fun chart(
        @Path("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("range") range: String,
        @Query("includePrePost") includePrePost: Boolean = false
    ): YahooChartResponse
}

interface CoinGeckoApi {
    @GET("api/v3/coins/markets")
    suspend fun markets(
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1,
        @Query("sparkline") sparkline: Boolean = false,
        @Query("price_change_percentage") priceChangePercentage: String = "24h"
    ): List<CoinGeckoMarketDto>
}
