package com.personal.financetracker.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.personal.financetracker.data.AppDatabase
import com.personal.financetracker.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Re-schedules reminders for upcoming planned payments after a reboot. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = Repository(AppDatabase.getDatabase(appContext))
                repo.getUpcomingPlannedOnce().forEach { ReminderScheduler.schedule(appContext, it) }
            } finally {
                pending.finish()
            }
        }
    }
}
