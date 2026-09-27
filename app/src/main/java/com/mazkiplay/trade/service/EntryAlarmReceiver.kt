package com.mazkiplay.trade.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.EntryAlarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires when a scheduled entry alarm is due. */
class EntryAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ENTRY_ALARM = "com.mazkiplay.trade.ACTION_ENTRY_ALARM"
        const val EXTRA_ID = "alarm_id"
        const val EXTRA_LABEL = "alarm_label"
        const val EXTRA_SYMBOL = "alarm_symbol"
        const val EXTRA_NOTE = "alarm_note"
        const val EXTRA_REPEAT = "alarm_repeat"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "Entry"
        val symbol = intent.getStringExtra(EXTRA_SYMBOL) ?: "XAUUSD"
        val note = intent.getStringExtra(EXTRA_NOTE).orEmpty()
        val id = intent.getLongExtra(EXTRA_ID, 0L)
        val repeat = intent.getBooleanExtra(EXTRA_REPEAT, false)

        val app = context.applicationContext as? MazkiplayApp
        app?.notifications?.notifyEntryAlarm(label, symbol, note)

        if (repeat && app != null) {
            val next = AlarmScheduler.nextDailyOccurrence(System.currentTimeMillis())
            CoroutineScope(Dispatchers.IO).launch {
                app.tradeRepository.rescheduleAlarm(id, next)
                AlarmScheduler.schedule(
                    context,
                    EntryAlarm(id = id, label = label, symbol = symbol, triggerAt = next, note = note, repeatDaily = true)
                )
            }
        }
    }
}
