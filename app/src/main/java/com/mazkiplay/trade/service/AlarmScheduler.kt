package com.mazkiplay.trade.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mazkiplay.trade.data.model.EntryAlarm

/**
 * Schedules exact entry alarms through AlarmManager.
 *
 * On Android 12+ exact alarms need the SCHEDULE_EXACT_ALARM permission; when it has
 * been revoked the call falls back to an inexact window so the alarm still lands,
 * just with the few minutes of slack the platform requires.
 */
object AlarmScheduler {

    fun schedule(context: Context, alarm: EntryAlarm) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = pendingIntent(context, alarm)

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }

        runCatching {
            if (canExact) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.triggerAt, pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.triggerAt, pending)
            }
        }.onFailure {
            manager.set(AlarmManager.RTC_WAKEUP, alarm.triggerAt, pending)
        }
    }

    fun cancel(context: Context, alarm: EntryAlarm) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        manager.cancel(pendingIntent(context, alarm))
    }

    /** Re-arm a daily repeating alarm for the next occurrence of its clock time. */
    fun rescheduleDaily(context: Context, alarm: EntryAlarm) {
        val next = nextDailyOccurrence(alarm.triggerAt)
        schedule(context, alarm.copy(triggerAt = next))
    }

    fun nextDailyOccurrence(previousTrigger: Long): Long {
        val now = System.currentTimeMillis()
        var next = previousTrigger + 86_400_000L
        while (next <= now) next += 86_400_000L
        return next
    }

    private fun pendingIntent(context: Context, alarm: EntryAlarm): PendingIntent {
        val intent = Intent(context, EntryAlarmReceiver::class.java).apply {
            action = EntryAlarmReceiver.ACTION_ENTRY_ALARM
            putExtra(EntryAlarmReceiver.EXTRA_ID, alarm.id)
            putExtra(EntryAlarmReceiver.EXTRA_LABEL, alarm.label)
            putExtra(EntryAlarmReceiver.EXTRA_SYMBOL, alarm.symbol)
            putExtra(EntryAlarmReceiver.EXTRA_NOTE, alarm.note)
            putExtra(EntryAlarmReceiver.EXTRA_REPEAT, alarm.repeatDaily)
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
