package com.mazkiplay.trade.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.data.model.FeedStatus
import com.mazkiplay.trade.data.model.SignalBias
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Colours shared by every price-bearing element, so "up" always reads the same. */
object MarketColors {
    val Up = Color(0xFF16C784)
    val Down = Color(0xFFEA3943)
    val Neutral = Color(0xFF8A94A6)

    fun of(change: Double): Color = when {
        change > 0.0 -> Up
        change < 0.0 -> Down
        else -> Neutral
    }
}

/** Live/offline indicator with the age of the last successful reading. */
@Composable
fun FeedStatusBadge(
    status: FeedStatus,
    lastUpdateMillis: Long,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(
        targetValue = when (status) {
            FeedStatus.LIVE -> MarketColors.Up
            FeedStatus.STALE -> Color(0xFFFFB020)
            FeedStatus.CONNECTING -> Color(0xFF4A90E2)
            FeedStatus.OFFLINE -> MarketColors.Down
        },
        label = "feedStatus"
    )

    val age = if (lastUpdateMillis == 0L) null
    else System.currentTimeMillis() - lastUpdateMillis

    val detail = when {
        age == null -> "menunggu"
        age < 60_000L -> "${age / 1000}s lalu"
        else -> "${age / 60_000L}m lalu"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = if (status == FeedStatus.LIVE) "LIVE \u00b7 $detail" else "${status.label} \u00b7 $detail",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

/** Coloured percentage chip. */
@Composable
fun ChangeChip(changePercent: Double, modifier: Modifier = Modifier) {
    val color = MarketColors.of(changePercent)
    val arrow = if (changePercent >= 0) "\u25B2" else "\u25BC"
    Text(
        text = "$arrow ${String.format(Locale.US, "%.2f", kotlin.math.abs(changePercent))}%",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

/** Bias pill used by the signal panel and the screener. */
@Composable
fun SignalBiasChip(bias: SignalBias, modifier: Modifier = Modifier) {
    val color = when (bias) {
        SignalBias.STRONG_BUY -> MarketColors.Up
        SignalBias.BUY -> MarketColors.Up.copy(alpha = 0.75f)
        SignalBias.NEUTRAL -> MarketColors.Neutral
        SignalBias.SELL -> MarketColors.Down.copy(alpha = 0.75f)
        SignalBias.STRONG_SELL -> MarketColors.Down
    }
    Text(
        text = bias.label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/**
 * A price sparkline drawn from a real series.
 *
 * The path is normalised against the series' own min/max so tiny forex moves still
 * read as a visible shape; a flat series collapses to a centred line instead of
 * dividing by zero.
 */
@Composable
fun LiveSparkline(
    series: List<Double>,
    positive: Boolean,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 2.4f
) {
    val color = if (positive) MarketColors.Up else MarketColors.Down
    Canvas(modifier = modifier) {
        if (series.size < 2) return@Canvas
        val min = series.min()
        val max = series.max()
        val span = (max - min).takeIf { it > 0.0 } ?: 1.0

        val stepX = size.width / (series.size - 1)
        val path = Path()
        series.forEachIndexed { index, value ->
            val x = stepX * index
            val y = size.height - (((value - min) / span) * size.height).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = strokeWidth))

        // Fade under the line so the shape reads at a glance on a dark card.
        val fill = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, color = color.copy(alpha = 0.12f))
    }
}

/** Horizontal confidence/strength bar. */
@Composable
fun MetricBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        label = "metricBar"
    )
    LinearProgressIndicator(
        progress = { animated },
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(50))
    )
}

/** Standard centred loading state. */
@Composable
fun LoadingBox(message: String = "Memuat data...", modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Empty-state panel. */
@Composable
fun EmptyBox(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Inbox, contentDescription = null, tint = MarketColors.Neutral)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Error panel; used when a feed genuinely cannot be reached. */
@Composable
fun ErrorBox(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MarketColors.Down)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Offline banner shown above live screens when the feed degrades. */
@Composable
fun OfflineBanner(
    visible: Boolean,
    message: String = "Koneksi feed terputus \u2014 menampilkan data terakhir yang tersimpan.",
    modifier: Modifier = Modifier
) {
    if (!visible) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFFB020).copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Filled.CloudOff,
            contentDescription = null,
            tint = Color(0xFFFFB020),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Formats an epoch as a short local clock label. */
fun clockLabel(millis: Long, pattern: String = "HH:mm:ss"): String {
    if (millis <= 0L) return "--:--"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}
