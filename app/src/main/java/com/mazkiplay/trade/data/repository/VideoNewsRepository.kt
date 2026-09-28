package com.mazkiplay.trade.data.repository

import android.util.Xml
import com.mazkiplay.trade.data.api.NetworkModule
import com.mazkiplay.trade.data.model.VideoNewsItem
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
import org.xmlpull.v1.XmlPullParser
import java.text.SimpleDateFormat
import java.util.Locale

/** Public YouTube channel RSS feeds. No private API key is embedded in the APK. */
class VideoNewsRepository(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val feeds = listOf(
        "UCIALMKvObZNtJ6AmdCLP7Lg" to "Bloomberg Television",
        "UCvJJ_dzjViJCoLf5uKUTwoA" to "CNBC",
        "UCNye-wNBqNL5ZzHSJj3l8Bg" to "Al Jazeera English",
        "UCknLrEdhRCp1aegoMqRaCZg" to "DW News"
    )

    private val _videos = MutableStateFlow<List<VideoNewsItem>>(emptyList())
    val videos: StateFlow<List<VideoNewsItem>> = _videos.asStateFlow()
    private val _lastUpdated = MutableStateFlow(0L)
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private var started = false

    fun startAutoRefresh(intervalMillis: Long = 300_000L) {
        if (started) return
        started = true
        scope.launch {
            refresh()
            while (isActive) {
                delay(intervalMillis)
                refresh()
            }
        }
    }

    suspend fun refresh() {
        val result = feeds.flatMap { (id, name) -> fetchFeed(id, name) }
            .distinctBy { it.videoId }
            .sortedByDescending { it.publishedAt }
            .take(40)
        if (result.isNotEmpty()) {
            _videos.value = result
            _lastUpdated.value = System.currentTimeMillis()
            _error.value = null
        } else if (_videos.value.isEmpty()) {
            _error.value = "Feed YouTube belum tersedia"
        }
    }

    private fun fetchFeed(channelId: String, channelName: String): List<VideoNewsItem> = runCatching {
        val request = Request.Builder()
            .url("https://www.youtube.com/feeds/videos.xml?channel_id=$channelId")
            .get()
            .build()
        NetworkModule.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val parser = Xml.newPullParser().apply {
                setInput(response.body?.byteStream(), "UTF-8")
            }
            val items = mutableListOf<VideoNewsItem>()
            var event = parser.eventType
            var videoId = ""
            var title = ""
            var published = 0L
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name.substringAfter(':')) {
                        "videoId" -> videoId = parser.nextText()
                        "title" -> title = parser.nextText()
                        "published" -> published = parseDate(parser.nextText())
                    }
                } else if (event == XmlPullParser.END_TAG && parser.name == "entry") {
                    if (videoId.isNotBlank() && title.isNotBlank()) {
                        items += VideoNewsItem(videoId, title, channelName, published)
                    }
                    videoId = ""
                    title = ""
                    published = 0L
                }
                event = parser.next()
            }
            items
        }
    }.getOrDefault(emptyList())

    private fun parseDate(value: String): Long = runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US).parse(value)?.time ?: 0L
    }.getOrDefault(0L)
}
