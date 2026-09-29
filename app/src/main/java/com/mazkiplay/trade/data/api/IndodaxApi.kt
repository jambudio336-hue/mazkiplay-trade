package com.mazkiplay.trade.data.api

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Public, read-only Indodax endpoints. No account keys or order execution are used. */
interface IndodaxApi {
    @GET("api/server_time")
    suspend fun serverTime(): JsonObject

    @GET("api/pairs")
    suspend fun pairs(): JsonObject

    @GET("api/ticker_all")
    suspend fun tickerAll(): JsonObject

    @GET("api/ticker/{pair}")
    suspend fun ticker(@Path("pair") pair: String): JsonObject

    @GET("api/depth/{pair}")
    suspend fun depth(@Path("pair") pair: String): JsonObject

    @GET("api/trades/{pair}")
    suspend fun trades(@Path("pair") pair: String): JsonObject

    @GET("tradingview/history_v2")
    suspend fun history(
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("symbol") symbol: String,
        @Query("tf") timeframe: String
    ): JsonObject
}
