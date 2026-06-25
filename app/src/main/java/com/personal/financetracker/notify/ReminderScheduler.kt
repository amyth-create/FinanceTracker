package com.personal.financetracker.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.personal.financetracker.data.PlannedPayment
import com.personal.financetracker.util.Formatters
import java.util.*

/** Schedules / cancels reminder notifications for planned payments. */
object ReminderScheduler {

    const val CHANNEL_ID = "planned_reminders"
    const val EXTRA_ID = "extra_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_TEXT = "extra_text"

    /** Hour of day (local) when the reminder fires. */
    private const val REMINDER_HOUR = 9

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Planned payments",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Reminders for upcoming planned payments" }
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    fun schedule(context: Context, payment: PlannedPayment) {
        if (payment.isDone) return
        val triggerAt = reminderTime(payment.plannedDate)
        if (triggerAt <= System.currentTimeMillis()) return // don't fire for past dates
        ensureChannel(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, buildPendingIntent(context, payment))
    }

    fun cancel(context: Context, id: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, id.toInt(), intent, noCreateFlags())
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    private fun buildPendingIntent(context: Context, payment: PlannedPayment): PendingIntent {
        val label = payment.note.ifEmpty { payment.categoryName }
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_ID, payment.id.toInt())
            putExtra(EXTRA_TITLE, "Planned payment due")
            putExtra(EXTRA_TEXT, "$label · ${Formatters.formatAmount(payment.amount)}")
        }
        return PendingIntent.getBroadcast(context, payment.id.toInt(), intent, updateFlags())
    }

    private fun reminderTime(plannedDate: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = plannedDate
            set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun updateFlags(): Int {
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags = flags or PendingIntent.FLAG_IMMUTABLE
        return flags
    }

    private fun noCreateFlags(): Int {
        var flags = PendingIntent.FLAG_NO_CREATE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags = flags or PendingIntent.FLAG_IMMUTABLE
        return flags
    }
}
