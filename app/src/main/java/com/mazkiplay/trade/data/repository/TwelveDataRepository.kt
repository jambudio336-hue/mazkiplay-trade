package com.mazkiplay.trade.data.repository

import com.mazkiplay.trade.data.api.NetworkModule
import com.mazkiplay.trade.data.api.TwelveDataApi
import com.mazkiplay.trade.data.model.Candle
import com.mazkiplay.trade.data.model.Instrument
import com.mazkiplay.trade.data.model.InstrumentClass
import com.mazkiplay.trade.data.model.Quote
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Optional Twelve Data lane. The API key is read from Android Keystore-backed
 * credential storage and is never compiled into the APK or printed to logs.
 */
class TwelveDataRepository(
    private val credentials: AiProviderRepository,
    private val api: TwelveDataApi = NetworkModule.twelveDataApi
) {
    suspend fun testConnection(): Result<Unit> = runCatching {
        val authorization = credentials.readCredential("twelvedata")?.let { "apikey $it" }
            ?: error("Credential Twelve Data belum disimpan.")
        val response = api.price("AAPL", authorization)
        if (response.price?.toDoubleOrNull() == null) {
            error(response.message?.take(180) ?: "Provider tidak mengembalikan harga yang valid.")
        }
    }

    suspend fun quote(instrument: Instrument): Quote? {
        val authorization = credentials.readCredential("twelvedata")?.let { "apikey $it" } ?: return null
        return runCatching {
            val raw = api.price(symbolFor(instrument), authorization)
            val price = raw.price?.toDoubleOrNull() ?: return@runCatching null
            Quote(instrument.symbol, price, price, price, price, System.currentTimeMillis())
        }.getOrNull()
    }

    suspend fun candles(instrument: Instrument, interval: String, outputSize: Int): List<Candle> {
        val authorization = credentials.readCredential("twelvedata")?.let { "apikey $it" } ?: return emptyList()
        val tdInterval = intervalMap(interval)
        return runCatching {
            api.timeSeries(symbolFor(instrument), tdInterval, outputSize.coerceIn(1, 5000), authorization).values.orEmpty().mapNotNull { value ->
                val open = value.open?.toDoubleOrNull() ?: return@mapNotNull null
                val high = value.high?.toDoubleOrNull() ?: return@mapNotNull null
                val low = value.low?.toDoubleOrNull() ?: return@mapNotNull null
                val close = value.close?.toDoubleOrNull() ?: return@mapNotNull null
                Candle(parseTime(value.datetime), open, high, low, close, value.volume?.toDoubleOrNull() ?: 0.0)
            }.sortedBy { it.time }
        }.getOrDefault(emptyList())
    }

    private fun symbolFor(instrument: Instrument): String = when (instrument.klass) {
        InstrumentClass.CRYPTO -> "${instrument.baseCurrency}/${instrument.quoteCurrency}"
        InstrumentClass.MAJOR, InstrumentClass.MINOR -> "${instrument.baseCurrency}/${instrument.quoteCurrency}"
        InstrumentClass.EQUITY -> instrument.symbol.removeSuffix(".JK")
        else -> instrument.symbol
    }

    private fun intervalMap(interval: String): String = when (interval) {
        "1m" -> "1min"
        "5m" -> "5min"
        "15m" -> "15min"
        "30m" -> "30min"
        "1h" -> "1h"
        "1d" -> "1day"
        "1wk" -> "1week"
        "1mo" -> "1month"
        else -> "15min"
    }

    private fun parseTime(raw: String?): Long {
        if (raw.isNullOrBlank()) return System.currentTimeMillis()
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(raw)?.time
        }.getOrNull() ?: System.currentTimeMillis()
    }
}
