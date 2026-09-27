package com.mazkiplay.trade.data.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Single shared OkHttp/Retrofit stack.
 *
 * A browser-like User-Agent is mandatory: several public data endpoints (the Yahoo
 * chart feed in particular) answer 429 to the default Java client agent.
 */
object NetworkModule {

    const val TAG = "MazkiplayNet"

    const val UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/122.0.0.0 Mobile Safari/537.36 MazkiplayTrade/1.0"

    const val YAHOO_BASE = "https://query1.finance.yahoo.com/"
    const val CALENDAR_URL = "https://nfs.faireconomy.media/ff_calendar_thisweek.json"

    val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor { message -> Log.d(TAG, message) }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", UA)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "en-US,en;q=0.9,id;q=0.8")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }

    private val gson = com.google.gson.GsonBuilder()
        .setLenient()
        .create()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(YAHOO_BASE)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val marketApi: MarketApi by lazy { retrofit.create(MarketApi::class.java) }

    private val gsonPlain = com.google.gson.Gson()

    /** Parse a raw JSON body that is not served through Retrofit. */
    fun <T> parse(body: String, clazz: Class<T>): T? = runCatching {
        gsonPlain.fromJson(body, clazz)
    }.getOrNull()

    fun <T> parseList(body: String, clazz: Class<Array<T>>): List<T>? = runCatching {
        gsonPlain.fromJson(body, clazz)?.toList()
    }.getOrNull()
}
