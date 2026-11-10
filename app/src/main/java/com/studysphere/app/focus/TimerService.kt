package com.studysphere.app.focus

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.studysphere.app.StudyApp
import com.studysphere.app.notifications.Notifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Foreground service that keeps the Pomodoro countdown alive while the app is in the background. */
class TimerService : Service() {
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as StudyApp
        ServiceCompat.startForeground(
            this, Notifier.TIMER_ID, Notifier.timerNotification(this, app.timer.state.value),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        job?.cancel()
        job = app.scope.launch {
            while (isActive && app.timer.tick()) {
                try {
                    NotificationManagerCompat.from(this@TimerService)
                        .notify(Notifier.TIMER_ID, Notifier.timerNotification(this@TimerService, app.timer.state.value))
                } catch (e: SecurityException) { /* notification permission denied */ }
                delay(1000)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    companion object {
        fun start(ctx: Context) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, TimerService::class.java))
        }
    }
}
