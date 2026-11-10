package com.studysphere.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studysphere.app.StudyApp
import com.studysphere.app.domain.Constants
import com.studysphere.app.util.Dates
import kotlinx.coroutines.flow.first

/**
 * Runs about every 15 minutes (Android may delay it to save battery, so reminders are not exact).
 * Each reminder is sent once per day thanks to a small de-duplication store.
 */
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as StudyApp
        val p = app.prefsRepo.prefs.first()
        if (!p.onboarded) return Result.success()

        val sp = applicationContext.getSharedPreferences("reminders", Context.MODE_PRIVATE)
        val today = Dates.today()
        val suffix = "_$today"
        val editor = sp.edit()
        sp.all.keys.filter { !it.endsWith(suffix) }.forEach { editor.remove(it) }
        editor.apply()
        fun once(key: String): Boolean {
            val k = key + suffix
            if (sp.getBoolean(k, false)) return false
            sp.edit().putBoolean(k, true).apply()
            return true
        }

        val nowMin = Dates.nowMin()
        val subjects = app.repo.subjects.first().associateBy { it.id }

        if (p.nSession) {
            app.repo.sessions.first()
                .filter { it.date == today && !it.completed && it.startMin - nowMin in 0..30 }
                .forEach {
                    if (once("s${it.id}")) Notifier.show(
                        applicationContext, 2000 + (it.id % 500).toInt(), Notifier.CH_REMIND,
                        "Study session soon",
                        "${it.title}${subjects[it.subjectId]?.let { s -> " (${s.name})" } ?: ""} starts at ${Dates.time(it.startMin)}."
                    )
                }
        }
        if (p.nTask) {
            app.repo.tasks.first()
                .filter { it.status != Constants.DONE && it.deadline in today..(today + 1) }
                .forEach {
                    if (once("t${it.id}")) Notifier.show(
                        applicationContext, 3000 + (it.id % 500).toInt(), Notifier.CH_REMIND,
                        "Assignment deadline",
                        "${it.title} is due ${if (it.deadline == today) "today" else "tomorrow"}."
                    )
                }
        }
        if (p.nExam) {
            app.repo.exams.first()
                .filter { it.date in today..(today + 3) }
                .forEach {
                    if (once("e${it.id}")) Notifier.show(
                        applicationContext, 4000 + (it.id % 500).toInt(), Notifier.CH_REMIND,
                        "Exam coming up",
                        "${it.name}: ${Dates.countdown(it.date, today)} (${it.progress}% prepared)."
                    )
                }
        }
        if (p.nDaily && nowMin >= 18 * 60) {
            val studied = app.repo.focus.first().any { it.day == today } ||
                app.repo.sessions.first().any { it.date == today && it.completed }
            if (!studied && once("daily")) Notifier.show(
                applicationContext, 5000, Notifier.CH_REMIND,
                "Time to study", "You haven't studied today yet. Even 15 minutes keeps your streak going."
            )
        }
        return Result.success()
    }
}
