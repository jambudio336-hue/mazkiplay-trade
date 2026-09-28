package com.mazkiplay.trade.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mazkiplay.trade.MainActivity
import com.mazkiplay.trade.R
import com.mazkiplay.trade.data.model.EconomicEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Every notification the app can raise: price moves, fresh headlines and entry
 * alarms. Each category has its own channel so users can mute one without the rest.
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_PRICE = "mazkiplay_price"
        const val CHANNEL_NEWS = "mazkiplay_news"
        const val CHANNEL_ALARM = "mazkiplay_alarm"

        private const val ID_PRICE = 1101
        private const val ID_NEWS = 1102
        private const val ID_ALARM = 1103
    }

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val priceChannel = NotificationChannel(
            CHANNEL_PRICE,
            context.getString(R.string.notification_channel_price),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.notification_channel_desc) }

        val newsChannel = NotificationChannel(
            CHANNEL_NEWS,
            context.getString(R.string.notification_channel_news),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.notification_channel_desc) }

        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            context.getString(R.string.notification_channel_alarm),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
            enableVibration(true)
        }

        manager.createNotificationChannels(listOf(priceChannel, newsChannel, alarmChannel))
    }

    private fun allowed(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun base(title: String, body: String, channel: String, id: Int) =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppIntent(id))

    private fun openAppIntent(id: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun notifyPrice(symbol: String, price: Double, changePercent: Double, digits: Int) {
        if (!allowed()) return
        val arrow = if (changePercent >= 0) "\u25B2" else "\u25BC"
        val title = "$symbol $arrow ${String.format(java.util.Locale.US, "%+.2f%%", changePercent)}"
        val body = "Harga terbaru ${String.format(java.util.Locale.US, "%,.${digits}f", price)}"
        NotificationManagerCompat.from(context).notify(ID_PRICE, base(title, body, CHANNEL_PRICE, ID_PRICE).build())
    }

    fun notifyNews(titleText: String, source: String, url: String) {
        if (!allowed()) return
        val notification = base("Berita baru • $source", titleText, CHANNEL_NEWS, ID_NEWS)
            .setStyle(NotificationCompat.BigTextStyle().bigText(titleText))
        if (url.isNotBlank()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            val pending = PendingIntent.getActivity(
                context,
                ID_NEWS + 100,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notification.setContentIntent(pending)
        }
        NotificationManagerCompat.from(context).notify(ID_NEWS, notification.build())
    }

    fun notifyEntryAlarm(label: String, symbol: String, note: String) {
        if (!allowed()) return
        val body = if (note.isBlank()) {
            "Waktunya cek entry untuk $symbol. Siapkan rencana TP & SL."
        } else {
            note
        }
        val notification = base("Alarm Entry • $label", body, CHANNEL_ALARM, ID_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        NotificationManagerCompat.from(context).notify(ID_ALARM, notification.build())
    }

    /** Notify once when a high-impact calendar event enters the background reminder window. */
    fun notifyEconomicEvent(event: EconomicEvent) {
        if (!allowed()) return
        val prefs = context.getSharedPreferences("notification_state", Context.MODE_PRIVATE)
        val key = "calendar_${event.id}"
        if (prefs.getBoolean(key, false)) return
        val at = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(event.dateMillis))
        val body = "${event.currency.ifBlank { event.country }} • ${event.title} • $at • " +
            "Impact: ${event.impact.label}. Forecast: ${event.forecast.ifBlank { "--" }} • " +
            "Previous: ${event.previous.ifBlank { "--" }} • Actual: ${event.actual.ifBlank { "menunggu" }}"
        val notificationId = ID_ALARM + event.id.hashCode()
        NotificationManagerCompat.from(context).notify(
            notificationId,
            base("Event berdampak tinggi", body, CHANNEL_ALARM, notificationId)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()
        )
        prefs.edit().putBoolean(key, true).apply()
    }
}
