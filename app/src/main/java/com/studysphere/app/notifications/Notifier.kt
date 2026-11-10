package com.studysphere.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.studysphere.app.MainActivity
import com.studysphere.app.R
import com.studysphere.app.domain.Constants
import com.studysphere.app.focus.TimerState
import com.studysphere.app.util.Dates

object Notifier {
    const val CH_TIMER = "timer"
    const val CH_DONE = "focus_done"
    const val CH_REMIND = "reminders"
    const val TIMER_ID = 1001
    const val DONE_ID = 1002

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_TIMER, "Focus timer", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shows the running focus timer"
        })
        nm.createNotificationChannel(NotificationChannel(CH_DONE, "Focus session finished", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alerts you when a focus session or break ends"
        })
        nm.createNotificationChannel(NotificationChannel(CH_REMIND, "Study reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Study sessions, deadlines, exams and daily reminders"
        })
    }

    private fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun timerNotification(ctx: Context, s: TimerState): Notification =
        NotificationCompat.Builder(ctx, CH_TIMER)
            .setSmallIcon(R.drawable.ic_stat_study)
            .setContentTitle("${Constants.MODE_NAMES[s.mode]} ${if (s.running) "running" else "paused"}")
            .setContentText("${Dates.clock(s.remainingSec)} remaining" + if (s.label.isNotBlank()) " · ${s.label}" else "")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openApp(ctx))
            .build()

    fun show(ctx: Context, id: Int, channel: String, title: String, text: String) {
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_study)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx))
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (e: SecurityException) {
            // Notification permission not granted: nothing to show.
        }
    }

    fun timerDone(ctx: Context, s: TimerState) {
        val isBreak = s.mode == 1 || s.mode == 2
        show(
            ctx, DONE_ID, CH_DONE,
            if (isBreak) "Break is over" else "Focus session complete",
            if (isBreak) "Ready for your next focus session?" else "Great work! ${Dates.duration(s.totalSec / 60)} of study time recorded."
        )
    }
}
