package com.mazkiplay.trade.data.api

import com.mazkiplay.trade.data.model.Candle
import com.mazkiplay.trade.data.model.EconomicEvent
import com.mazkiplay.trade.data.model.Impact
import com.mazkiplay.trade.data.model.Instrument
import com.mazkiplay.trade.data.model.Instruments
import com.mazkiplay.trade.data.model.NewsItem
import com.mazkiplay.trade.data.model.Quote
import com.mazkiplay.trade.data.repository.TwelveDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * All live market data in one place: candles/quotes from Yahoo, the economic
 * calendar from the public Forex Factory JSON feed, and headlines from RSS.
 *
 * Every call degrades gracefully — a failing endpoint returns an empty list rather
 * than propagating an exception into the ViewModel, so the UI keeps its last good
 * data and shows a refresh failure instead of a crash screen.
 */
class MarketDataSource(
    private val api: MarketApi = NetworkModule.marketApi,
    private val client: okhttp3.OkHttpClient = NetworkModule.client,
    private val twelveData: TwelveDataRepository? = null
) {

    // ------------------------------------------------------------------ candles

    suspend fun candles(instrument: Instrument, interval: String, range: String): List<Candle> =
        withContext(Dispatchers.IO) {
            twelveData?.candles(instrument, interval, 200)?.takeIf { it.isNotEmpty() }?.let { return@withContext it }
            runCatching {
                val response = api.chart(instrument.yahooSymbol, interval, range)
                val result = response.chart?.result?.firstOrNull() ?: return@runCatching emptyList()
                val stamps = result.timestamp ?: return@runCatching emptyList()
                val quote = result.indicators?.quote?.firstOrNull() ?: return@runCatching emptyList()
                val opens = quote.open ?: return@runCatching emptyList()
                val highs = quote.high ?: return@runCatching emptyList()
                val lows = quote.low ?: return@runCatching emptyList()
                val closes = quote.close ?: return@runCatching emptyList()
                val volumes = quote.volume

                stamps.indices.mapNotNull { i ->
                    val o = opens.getOrNull(i)
                    val h = highs.getOrNull(i)
                    val l = lows.getOrNull(i)
                    val c = closes.getOrNull(i)
                    if (o == null || h == null || l == null || c == null) return@mapNotNull null
                    Candle(
                        time = stamps[i] * 1000L,
                        open = o,
                        high = h,
                        low = l,
                        close = c,
                        volume = volumes?.getOrNull(i)?.toDouble() ?: 0.0
                    )
                }
            }.getOrElse { emptyList() }
        }

    // -------------------------------------------------------------------- quote

    suspend fun quote(instrument: Instrument): Quote = withContext(Dispatchers.IO) {
        twelveData?.quote(instrument)?.takeIf { it.price > 0.0 }?.let { return@withContext it }
        runCatching {
            val response = api.chart(instrument.yahooSymbol, "5m", "5d")
            val result = response.chart?.result?.firstOrNull() ?: return@runCatching Quote(instrument.symbol)
            val meta = result.meta
            val closes = result.indicators?.quote?.firstOrNull()?.close?.filterNotNull() ?: emptyList()
            val price = meta?.regularMarketPrice
                ?: closes.lastOrNull()
                ?: 0.0
            val previous = meta?.previousClose
                ?: meta?.chartPreviousClose
                ?: closes.firstOrNull()
                ?: price
            Quote(
                symbol = instrument.symbol,
                price = price,
                previousClose = previous,
                dayHigh = meta?.regularMarketDayHigh ?: (closes.maxOrNull() ?: price),
                dayLow = meta?.regularMarketDayLow ?: (closes.minOrNull() ?: price),
                updatedAt = (meta?.regularMarketTime ?: (System.currentTimeMillis() / 1000L)) * 1000L,
                spark = com.mazkiplay.trade.domain.analysis.Indicators.sparkline(closes, 48)
            )
        }.getOrElse { Quote(instrument.symbol) }
    }

    /** Fan out every watchlist quote concurrently (one HTTP round trip per symbol). */
    suspend fun quotes(instruments: List<Instrument>): List<Quote> = coroutineScope {
        instruments.map { instrument -> async { quote(instrument) } }.map { it.await() }
    }

    // ----------------------------------------------------------------- calendar

    suspend fun economicCalendar(): List<EconomicEvent> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(NetworkModule.CALENDAR_URL)
                .header("User-Agent", NetworkModule.UA)
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@use emptyList()
                val list = NetworkModule.parseList(body, Array<FfEventDto>::class.java) ?: emptyList()
                list.mapNotNull { dto ->
                    val title = dto.title?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val dateRaw = dto.date ?: return@mapNotNull null
                    val stamp = parseCalendarDate(dateRaw) ?: return@mapNotNull null
                    val currency = dto.country?.trim()?.uppercase().orEmpty()
                    EconomicEvent(
                        id = "$title|$dateRaw|$currency",
                        title = title,
                        country = currency,
                        currency = currency,
                        dateMillis = stamp,
                        impact = Impact.from(dto.impact ?: ""),
                        actual = dto.actual?.trim().orEmpty(),
                        forecast = dto.forecast?.trim().orEmpty(),
                        previous = dto.previous?.trim().orEmpty()
                    )
                }.sortedBy { it.dateMillis }
            }
        }.getOrElse { emptyList() }
    }

    /**
     * The calendar feed writes local exchange time with no zone designator
     * (e.g. "2026-09-27T08:30:00-04:00" or "2026-09-27T08:30:00"), so several
     * layouts are attempted in order of specificity.
     */
    private fun parseCalendarDate(raw: String): Long? {
        val trimmed = raw.trim()
        val layouts = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd"
        )
        layouts.forEach { layout ->
            runCatching {
                SimpleDateFormat(layout, Locale.US).apply { isLenient = true }.parse(trimmed)?.time
            }.getOrNull()?.let { return it }
        }
        return null
    }

    // --------------------------------------------------------------------- news

    private val feeds = listOf(
        Feed("FXStreet", "https://www.fxstreet.com/rss/news"),
        Feed("ForexLive", "https://www.forexlive.com/feed/news"),
        Feed("Investing.com", "https://www.investing.com/rss/news_285.rss"),
        Feed("CNBC Markets", "https://www.cnbc.com/id/20910258/device/rss/rss.html"),
        Feed("Yahoo Finance", "https://finance.yahoo.com/news/rssindex")
    )

    data class Feed(val name: String, val url: String)

    suspend fun news(limit: Int = 60): List<NewsItem> = coroutineScope {
        feeds.map { feed -> async { fetchFeed(feed) } }
            .flatMap { it.await() }
            .sortedByDescending { it.publishedAt }
            .distinctBy { it.title.lowercase().take(70) }
            .take(limit)
    }

    private suspend fun fetchFeed(feed: Feed): List<NewsItem> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(feed.url)
                .header("User-Agent", NetworkModule.UA)
                .build()
            client.newCall(request).execute().use { response ->
                val xml = response.body?.string().orEmpty()
                if (xml.isBlank()) emptyList() else RssParser.parse(xml, feed.name)
            }
        }.getOrElse { emptyList() }
    }

    /** Instruments whose Yahoo ticker needs the futures/gold mapping. */
    fun instrumentFor(symbol: String): Instrument = Instruments.bySymbol(symbol)
}
