package com.mazkiplay.trade.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.VideoNewsItem
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.ui.components.EmptyHint
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.videoNewsViewModel
import com.mazkiplay.trade.util.Formatters

@Composable
fun VideoNewsScreen(app: MazkiplayApp, prefs: UserPreferences) {
    val vm = videoNewsViewModel(app)
    val videos by vm.videos.collectAsState()
    val selected by vm.selected.collectAsState()
    val playing by vm.playing.collectAsState()
    val updated by vm.lastUpdated.collectAsState()
    val error by vm.error.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionCard(
                title = "Video News Online",
                subtitle = "YouTube RSS publik • update ${if (updated > 0) Formatters.time(updated, prefs.timePattern) else "menunggu"}"
            ) {
                Text(
                    "Video diputar melalui embed resmi YouTube di dalam APK. Item terbaru otomatis menjadi pilihan teratas saat feed diperbarui.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(error!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        item {
            SectionCard(title = selected?.title ?: "Belum ada video", subtitle = selected?.channel ?: "Feed publik") {
                if (selected == null) {
                    EmptyHint("Belum ada video publik yang diterima.")
                } else {
                    YouTubeEmbed(video = selected!!, playing = playing)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.togglePlaying() }, modifier = Modifier.weight(1f)) {
                            Text(if (playing) "Putar ulang" else "Putar")
                        }
                        OutlinedButton(onClick = { vm.stop() }, modifier = Modifier.weight(1f)) { Text("Stop") }
                        OutlinedButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(selected!!.watchUrl)))
                        }, modifier = Modifier.weight(1f)) { Text("YouTube") }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { vm.refresh() }, modifier = Modifier.weight(1f)) { Text("Muat ulang feed") }
            }
        }

        item { Text("Video terbaru", style = MaterialTheme.typography.titleMedium) }
        items(videos, key = { it.videoId }) { video ->
            VideoRow(video = video, selected = video.videoId == selected?.videoId, onClick = { vm.select(video) }, prefs = prefs)
        }
    }
}

@Composable
private fun YouTubeEmbed(video: VideoNewsItem, playing: Boolean) {
    val html = remember(video.videoId, playing) {
        if (!playing) "<html><body style='background:#0d1117'></body></html>" else """
            <html><body style='margin:0;background:#0d1117'>
            <iframe width='100%' height='230' src='https://www.youtube.com/embed/${video.videoId}?autoplay=1&playsinline=1&rel=0'
            title='YouTube video player' frameborder='0' allow='autoplay; encrypted-media; picture-in-picture' allowfullscreen></iframe>
            </body></html>
        """.trimIndent()
    }
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(230.dp),
        factory = { context ->
            WebView(context).apply {
                tag = ""
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.cacheMode = WebSettings.LOAD_DEFAULT
            }
        },
        update = { view ->
            if (view.tag != html) {
                view.tag = html
                view.loadDataWithBaseURL("https://www.youtube.com/", html, "text/html", "UTF-8", null)
            }
        }
    )
}

@Composable
private fun VideoRow(video: VideoNewsItem, selected: Boolean, onClick: () -> Unit, prefs: UserPreferences) {
    SectionCard(
        title = video.title,
        subtitle = "${video.channel} • ${Formatters.time(video.publishedAt, prefs.dateTimePattern)}"
    ) {
        Text(
            "Ketuk untuk memilih video${if (selected) " • sedang dipilih" else ""}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}
