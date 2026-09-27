package com.mazkiplay.trade.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mazkiplay.trade.MazkiplayApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms every enabled alarm after a reboot.
 *
 * AlarmManager forgets pending alarms across restarts, so without this receiver a
 * user's alarms would silently disappear whenever the phone reboots.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? MazkiplayApp ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                app.tradeRepository.pendingAlarms().forEach { alarm ->
                    val trigger = if (alarm.triggerAt <= System.currentTimeMillis()) {
                        AlarmScheduler.nextDailyOccurrence(alarm.triggerAt)
                    } else {
                        alarm.triggerAt
                    }
                    app.tradeRepository.rescheduleAlarm(alarm.id, trigger)
                    AlarmScheduler.schedule(context, alarm.copy(triggerAt = trigger))
                }
                app.marketRepository.refreshAll()
                app.newsRepository.refresh()
            }
            pendingResult.finish()
        }
    }
}
