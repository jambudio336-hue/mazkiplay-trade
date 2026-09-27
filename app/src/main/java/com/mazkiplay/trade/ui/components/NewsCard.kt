package com.mazkiplay.trade.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.data.model.NewsItem
import com.mazkiplay.trade.data.model.EconomicEvent
import com.mazkiplay.trade.data.model.Impact
import com.mazkiplay.trade.ui.theme.Bear
import com.mazkiplay.trade.ui.theme.Gold
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Aqua
import com.mazkiplay.trade.util.Formatters

/** Headline row with source, age and a "BARU" flag for fresh items. */
@Composable
fun NewsCard(
    item: NewsItem,
    isNew: Boolean,
    language: String,
    timePattern: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(text = item.source, color = Aqua)
                Spacer(Modifier.width(6.dp))
                Pill(text = item.category, color = MaterialTheme.colorScheme.outline)
                if (isNew) {
                    Spacer(Modifier.width(6.dp))
                    Pill(text = "BARU", color = Bull, filled = true)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = Formatters.ago(item.publishedAt, language),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (item.summary.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = Formatters.time(item.publishedAt, "$timePattern \u2022 dd MMM yyyy"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Economic calendar row: time, currency, impact and actual vs forecast. */
@Composable
fun CalendarRow(
    event: EconomicEvent,
    timePattern: String,
    modifier: Modifier = Modifier
) {
    val impactColor = when (event.impact) {
        Impact.HIGH -> Bear
        Impact.MEDIUM -> Gold
        Impact.LOW -> Bull
        Impact.HOLIDAY -> MaterialTheme.colorScheme.outline
    }
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.width(62.dp)) {
            Text(
                text = Formatters.time(event.dateMillis, timePattern),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = event.currency,
                style = MaterialTheme.typography.labelSmall,
                color = impactColor
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
            Row {
                if (event.actual.isNotBlank()) {
                    Text(
                        text = "A: ${event.actual}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(8.dp))
                }
                if (event.forecast.isNotBlank()) {
                    Text(
                        text = "F: ${event.forecast}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                }
                if (event.previous.isNotBlank()) {
                    Text(
                        text = "P: ${event.previous}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Pill(text = event.impact.label, color = impactColor, filled = event.impact == Impact.HIGH)
    }
}
