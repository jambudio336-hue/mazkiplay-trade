package com.mazkiplay.trade.data.tradingview

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mazkiplay.trade.data.model.TvCalendarEvent
import com.mazkiplay.trade.data.model.TvNewsItem
import com.mazkiplay.trade.data.model.TvQuote
import com.mazkiplay.trade.data.model.TvTechnical
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Everything the app reads from TradingView's public JSON endpoints.
 *
 * Three undocumented but keyless services are used:
 *  * `POST /global/scan` for prices **and** technical readings in a single round trip,
 *  * `/economic-calendar/events` for the calendar (needs `Origin`/`Referer`, else 403),
 *  * `/news-flow/v2/news` for headlines.
 *
 * Two behaviours of these endpoints shape the whole class:
 *  - An unresolved ticker is **silently dropped** from a scan response, so results are
 *    keyed by the returned `s` value instead of by list position.
 *  - `d` is a positional array with no field names, so the column list and the index
 *    constants below are declared next to each other and must be changed together.
 *
 * Every method returns an empty result on failure instead of throwing: the app keeps
 * its last good snapshot and shows a stale/offline badge rather than an error screen.
 */
class TvDataSource(
    private val client: OkHttpClient = com.mazkiplay.trade.data.api.NetworkModule.client
) {

    // ------------------------------------------------------------------ columns
    // The scanner answers with `d: [...]`; the order is exactly this list.
    private val columns = listOf(
        "name", "description", "close", "change", "change_abs",
        "high", "low", "open", "volume",
        "Recommend.All", "Recommend.MA", "Recommend.Other",
        "RSI", "ATR", "EMA20", "EMA50", "EMA200",
        "Volatility.D", "ADX", "CCI20"
    )

    private object Col {
        const val NAME = 0
        const val DESCRIPTION = 1
        const val CLOSE = 2
        const val CHANGE = 3
        const val CHANGE_ABS = 4
        const val HIGH = 5
        const val LOW = 6
        const val OPEN = 7
        const val VOLUME = 8
        const val RECOMMEND_ALL = 9
        const val RECOMMEND_MA = 10
        const val RECOMMEND_OTHER = 11
        const val RSI = 12
        const val ATR = 13
        const val EMA20 = 14
        const val EMA50 = 15
        const val EMA200 = 16
        const val VOLATILITY = 17
        const val ADX = 18
        const val CCI = 19
    }

    companion object {
        const val SCAN_URL = "https://scanner.tradingview.com/global/scan"
        const val SYMBOL_URL = "https://scanner.tradingview.com/symbol"
        const val CALENDAR_URL = "https://economic-calendar.tradingview.com/events"
        const val NEWS_URL = "https://news-mediator.tradingview.com/news-flow/v2/news"
        const val NEWS_BASE = "https://www.tradingview.com"

        /**
         * The calendar endpoint answers 403 to anything without browser navigation
         * headers. This is not an auth problem \u2014 there is no key involved.
         */
        private const val ORIGIN = "https://www.tradingview.com"

        /** Markets the news flow is filtered to. */
        private const val NEWS_MARKETS = "forex,indices,commodities,crypto"
    }

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    // --------------------------------------------------------------------- scan

    /**
     * One POST for every instrument: live price, session range and the full technical
     * reading. Batching matters \u2014 a request per symbol per tick gets throttled.
     */
    suspend fun scan(symbols: List<String>): TvSnapshot {
        if (symbols.isEmpty()) return TvSnapshot()
        val tickers = symbols.map { TvTickers.ticker(it) }
        val payload = JsonObject().apply {
            add("symbols", JsonObject().apply {
                add("tickers", JsonArray().apply { tickers.forEach { add(it) } })
                add("query", JsonObject().apply { add("types", JsonArray()) })
            })
            add("columns", JsonArray().apply { columns.forEach { add(it) } })
        }.toString()

        val body = runCatching {
            client.newCall(
                Request.Builder()
                    .url(SCAN_URL)
                    .post(payload.toRequestBody(jsonType))
                    .build()
            ).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()
            }
        }.getOrNull() ?: return TvSnapshot()

        return runCatching { parseScan(body, symbols.size) }.getOrElse { TvSnapshot() }
    }

    private fun parseScan(raw: String, requested: Int): TvSnapshot {
        val root = JsonParser.parseString(raw).asJsonObject
        val rows = root.getAsJsonArray("data") ?: return TvSnapshot()

        val quotes = LinkedHashMap<String, TvQuote>()
        val technicals = LinkedHashMap<String, TvTechnical>()

        rows.forEach { element ->
            val row = element.asJsonObject
            val ticker = row.get("s")?.asString ?: return@forEach
            val d = row.getAsJsonArray("d") ?: return@forEach
            if (d.size() < columns.size) return@forEach

            val symbol = TvTickers.symbolOf(ticker)
            quotes[symbol] = TvQuote(
                symbol = symbol,
                ticker = ticker,
                name = d.str(Col.DESCRIPTION).ifBlank { d.str(Col.NAME) },
                close = d.num(Col.CLOSE),
                changePercent = d.num(Col.CHANGE),
                changeAbs = d.num(Col.CHANGE_ABS),
                high = d.num(Col.HIGH),
                low = d.num(Col.LOW),
                open = d.num(Col.OPEN),
                volume = d.num(Col.VOLUME)
            )
            technicals[symbol] = TvTechnical(
                symbol = symbol,
                recommendAll = d.num(Col.RECOMMEND_ALL),
                recommendMa = d.num(Col.RECOMMEND_MA),
                recommendOther = d.num(Col.RECOMMEND_OTHER),
                rsi = d.num(Col.RSI, 50.0),
                atr = d.num(Col.ATR),
                ema20 = d.num(Col.EMA20),
                ema50 = d.num(Col.EMA50),
                ema200 = d.num(Col.EMA200),
                volatility = d.num(Col.VOLATILITY),
                adx = d.num(Col.ADX),
                cci = d.num(Col.CCI)
            )
        }

        return TvSnapshot(
            quotes = quotes,
            technicals = technicals,
            requestedCount = requested,
            resolvedCount = quotes.size
        )
    }

    // ------------------------------------------------------- multi-timeframe rating

    /**
     * Per-timeframe recommendation for one symbol, read from the pipe-suffixed GET
     * form of the scanner. Used by the chart overlay strip and the signal panel.
     */
    suspend fun multiTimeframeRatings(symbol: String): Map<String, Double> {
        val ticker = TvTickers.ticker(symbol)
        val fields = listOf(
            "Recommend.All|1", "Recommend.All|5", "Recommend.All|15",
            "Recommend.All|60", "Recommend.All|240", "Recommend.All"
        ).joinToString(",")

        val url = buildString {
            append(SYMBOL_URL)
            append("?symbol=").append(enc(ticker))
            append("&fields=").append(enc(fields))
            append("&no_404=true")
        }

        val raw = runCatching {
            client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()
            }
        }.getOrNull() ?: return emptyMap()

        return runCatching {
            val root = JsonParser.parseString(raw).asJsonObject
            val out = LinkedHashMap<String, Double>()
            out["M1"] = root.get("Recommend.All|1")?.asDouble ?: 0.0
            out["M5"] = root.get("Recommend.All|5")?.asDouble ?: 0.0
            out["M15"] = root.get("Recommend.All|15")?.asDouble ?: 0.0
            out["H1"] = root.get("Recommend.All|60")?.asDouble ?: 0.0
            out["H4"] = root.get("Recommend.All|240")?.asDouble ?: 0.0
            out["D1"] = root.get("Recommend.All")?.asDouble ?: 0.0
            out
        }.getOrElse { emptyMap() }
    }

    // ----------------------------------------------------------------- calendar

    /**
     * Economic calendar between two instants, filtered to the majors plus the
     * currencies the app trades. `importance` is -1 holiday, 0 low, 1 medium, 2 high.
     */
    suspend fun calendar(from: Long = System.currentTimeMillis() - 6 * 3_600_000L,
                         to: Long = System.currentTimeMillis() + 7 * 86_400_000L): List<TvCalendarEvent> {
        val url = buildString {
            append(CALENDAR_URL)
            append("?from=").append(iso(from))
            append("&to=").append(iso(to))
            append("&countries=US,EU,GB,JP,AU,CA,CH,NZ,CN,DE,FR,IT,ES")
        }

        val raw = runCatching {
            client.newCall(
                Request.Builder()
                    .url(url)
                    .header("Origin", ORIGIN)
                    .header("Referer", "$ORIGIN/")
                    .header("Accept", "application/json")
                    .get()
                    .build()
            ).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()
            }
        }.getOrNull() ?: return emptyList()

        // A 403 body is HTML; a lenient parser would mangle it into junk, so bail out
        // unless the payload really is JSON.
        if (!raw.trimStart().startsWith("{")) return emptyList()

        return runCatching {
            val root = JsonParser.parseString(raw).asJsonObject
            if (root.get("status")?.asString != "ok") return@runCatching emptyList()
            val result = root.getAsJsonArray("result") ?: return@runCatching emptyList()
            result.mapNotNull { element -> parseEvent(element.asJsonObject) }
                .sortedBy { it.dateMillis }
        }.getOrElse { emptyList() }
    }

    private fun parseEvent(obj: JsonObject): TvCalendarEvent? {
        val title = obj.get("title")?.asString?.takeIf { it.isNotBlank() } ?: return null
        val dateRaw = obj.get("date")?.asString ?: return null
        val stamp = parseIso(dateRaw) ?: return null
        val currency = obj.get("currency")?.asString?.trim().orEmpty()
        return TvCalendarEvent(
            id = obj.get("id")?.asString ?: "$title|$dateRaw",
            title = title,
            country = obj.get("country")?.asString.orEmpty(),
            currency = currency.ifBlank { obj.get("country")?.asString.orEmpty() },
            indicator = obj.get("indicator")?.asString.orEmpty(),
            dateMillis = stamp,
            importance = obj.get("importance")?.asInt ?: 0,
            actual = obj.cleanString("actual"),
            forecast = obj.cleanString("forecast"),
            previous = obj.cleanString("previous")
        )
    }

    // --------------------------------------------------------------------- news

    /** Headlines from the TradingView news flow, newest first. */
    suspend fun news(limit: Int = 60): List<TvNewsItem> {
        val url = buildString {
            append(NEWS_URL)
            append("?filter=lang:en")
            NEWS_MARKETS.split(',').forEach { market ->
                append("&filter=market:").append(market)
            }
            append("&client=web")
        }

        val raw = runCatching {
            client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()
            }
        }.getOrNull() ?: return emptyList()

        return runCatching {
            val root = JsonParser.parseString(raw).asJsonObject
            val items = root.getAsJsonArray("items") ?: return@runCatching emptyList()
            items.mapNotNull { element -> parseNews(element.asJsonObject) }
                .sortedByDescending { it.publishedAt }
                .distinctBy { it.title.lowercase().take(70) }
                .take(limit)
        }.getOrElse { emptyList() }
    }

    private fun parseNews(obj: JsonObject): TvNewsItem? {
        val id = obj.get("id")?.asString ?: return null
        val title = obj.get("title")?.asString?.takeIf { it.isNotBlank() } ?: return null
        val path = obj.get("storyPath")?.asString.orEmpty()
        // Paywalled items carry no `link`; the story path still works on the site.
        val link = obj.get("link")?.asString?.takeIf { it.isNotBlank() }
            ?: if (path.isNotBlank()) NEWS_BASE + path else NEWS_BASE

        val symbols = obj.getAsJsonArray("relatedSymbols")?.mapNotNull { element ->
            element.asJsonObject.get("symbol")?.asString?.let { TvTickers.symbolOf(it) }
        } ?: emptyList()

        val provider = obj.getAsJsonObject("provider")
        return TvNewsItem(
            id = id,
            title = title,
            summary = obj.get("shortDescription")?.asString
                ?: obj.get("summary")?.asString
                ?: "",
            url = link,
            source = provider?.get("name")?.asString ?: "TradingView",
            publishedAt = (obj.get("published")?.asLong ?: 0L) * 1000L,
            urgency = obj.get("urgency")?.asInt ?: 0,
            symbols = symbols,
            paywalled = obj.get("paywall")?.asBoolean ?: false
        )
    }

    // ------------------------------------------------------------------ helpers

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun iso(millis: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date(millis))
    }

    private fun parseIso(raw: String): Long? {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        patterns.forEach { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = true
                }.parse(raw.trim())?.time
            }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun JsonArray.num(index: Int, fallback: Double = 0.0): Double =
        runCatching { get(index)?.takeIf { !it.isJsonNull }?.asDouble ?: fallback }
            .getOrDefault(fallback)

    private fun JsonArray.str(index: Int): String =
        runCatching { get(index)?.takeIf { !it.isJsonNull }?.asString.orEmpty() }
            .getOrDefault("")

    private fun JsonObject.cleanString(key: String): String {
        val raw = get(key)?.takeIf { !it.isJsonNull }?.asString ?: return ""
        return raw.trim().takeIf { it != "null" && it != "\u2014" }.orEmpty()
    }
}

/** One batched reading of the whole instrument universe. */
data class TvSnapshot(
    val quotes: Map<String, TvQuote> = emptyMap(),
    val technicals: Map<String, TvTechnical> = emptyMap(),
    val requestedCount: Int = 0,
    val resolvedCount: Int = 0,
    val atMillis: Long = System.currentTimeMillis()
) {
    val isEmpty: Boolean get() = quotes.isEmpty()

    /** True when at least one requested ticker failed to resolve \u2014 a mapping bug. */
    val hasUnresolved: Boolean get() = requestedCount > resolvedCount
}
