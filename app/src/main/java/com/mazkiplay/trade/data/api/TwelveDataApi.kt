package com.mazkiplay.trade.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface TwelveDataApi {
    @GET("price")
    suspend fun price(
        @Query("symbol") symbol: String,
        @Header("Authorization") authorization: String
    ): TwelvePriceDto

    @GET("time_series")
    suspend fun timeSeries(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("outputsize") outputSize: Int,
        @Header("Authorization") authorization: String
    ): TwelveTimeSeriesDto
}

data class TwelvePriceDto(
    @SerializedName("price") val price: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null
)

data class TwelveTimeSeriesDto(
    @SerializedName("values") val values: List<TwelveValueDto>? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null
)

data class TwelveValueDto(
    @SerializedName("datetime") val datetime: String? = null,
    @SerializedName("open") val open: String? = null,
    @SerializedName("high") val high: String? = null,
    @SerializedName("low") val low: String? = null,
    @SerializedName("close") val close: String? = null,
    @SerializedName("volume") val volume: String? = null
)
