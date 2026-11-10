package com.studysphere.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.studysphere.app.data.AppDatabase
import com.studysphere.app.data.PrefsRepository
import com.studysphere.app.data.StudyRepository
import com.studysphere.app.focus.TimerEngine
import com.studysphere.app.notifications.Notifier
import com.studysphere.app.notifications.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.TimeUnit

class StudyApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db by lazy { AppDatabase.get(this) }
    val repo by lazy { StudyRepository(db) }
    val prefsRepo by lazy { PrefsRepository(this) }
    val timer by lazy { TimerEngine(this, repo, scope) }

    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "study_reminders", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES).build()
        )
    }
}
