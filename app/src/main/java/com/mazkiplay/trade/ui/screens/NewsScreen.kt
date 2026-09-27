package com.mazkiplay.trade.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.ui.components.CalendarRow
import com.mazkiplay.trade.ui.components.EmptyHint
import com.mazkiplay.trade.ui.components.NewsCard
import com.mazkiplay.trade.ui.components.Pill
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.newsViewModel
import com.mazkiplay.trade.ui.stringsOf
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold
import com.mazkiplay.trade.util.Formatters

/**
 * Live news room: rolling headlines with per-category filters plus the full
 * economic calendar for the current week.
 */
@Composable
fun NewsScreen(app: MazkiplayApp, prefs: UserPreferences) {
    val s = stringsOf(prefs)
    val vm = newsViewModel(app)
    val context = LocalContext.current

    val news by vm.filtered.collectAsState()
    val allNews by vm.news.collectAsState()
    val newIds by vm.newIds.collectAsState()
    val categories by vm.categories.collectAsState()
    val category by vm.category.collectAsState()
    val events by vm.events.collectAsState()
    val loading by vm.loading.collectAsState()
    val lastUpdated by vm.lastUpdated.collectAsState()

    val now = System.currentTimeMillis()
    val todayEvents = events.filter { it.dateMillis >= now - 7_200_000L }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = s.news,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${allNews.size} headline • ${s.updated} ${if (lastUpdated > 0) Formatters.time(lastUpdated, prefs.timePattern) else "--:--"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                categories.forEach { cat ->
                    val selected = cat == category
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) Bull else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { vm.setCategory(cat) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            SectionCard(
                title = "Kalender Ekonomi",
                subtitle = "Agenda fundamental pekan ini (Forex Factory)",
                trailing = { Pill(text = "${todayEvents.size} event", color = Gold) }
            ) {
                if (todayEvents.isEmpty()) {
                    EmptyHint(text = "Kalender sedang dimuat atau tidak ada agenda terdekat.")
                } else {
                    todayEvents.take(10).forEach { event ->
                        CalendarRow(event = event, timePattern = prefs.timePattern)
                    }
                }
            }
        }

        if (news.isEmpty()) {
            item {
                SectionCard(title = "Berita") {
                    EmptyHint(text = "Memuat berita dari berbagai sumber...")
                }
            }
        } else {
            items(news, key = { it.id }) { item ->
                NewsCard(
                    item = item,
                    isNew = newIds.contains(item.id),
                    language = prefs.language,
                    timePattern = prefs.timePattern,
                    onClick = {
                        if (item.url.isNotBlank()) {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                                )
                            }
                        }
                    }
                )
            }
        }

        item {
            Text(
                text = s.signalDisclaimer,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}
