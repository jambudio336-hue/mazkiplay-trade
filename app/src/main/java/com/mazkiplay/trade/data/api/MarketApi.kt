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
