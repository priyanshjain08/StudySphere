package com.studysphere.app.domain

import com.studysphere.app.data.FocusSession
import com.studysphere.app.data.StudySession
import com.studysphere.app.data.Subject
import com.studysphere.app.data.Task
import com.studysphere.app.util.Dates
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

data class Analytics(
    val totalMin: Int,
    val todayMin: Int,
    val weekMin: Int,
    val monthMin: Int,
    val last7: List<Pair<String, Int>>,
    val monthly: List<Pair<String, Int>>,
    /** name, minutes, ARGB colour */
    val bySubject: List<Triple<String, Int, Int>>,
    val completedTasks: Int,
    val pendingTasks: Int,
    val streak: Int,
    val longest: Int,
    val focusCount: Int,
    val focusMin: Int,
    val avgFocusMin: Int
)

object AnalyticsCalc {
    private class Act(val day: Long, val min: Int, val subjectId: Long?)

    /**
     * Study time = recorded focus sessions + completed planned study sessions.
     * Everything is computed from stored data using the device's local dates.
     */
    fun compute(focus: List<FocusSession>, sessions: List<StudySession>, tasks: List<Task>, subjects: List<Subject>, today: Long): Analytics {
        val studyFocus = focus.filter { it.mode == 0 || it.mode == 3 }
        val acts = studyFocus.map { Act(it.day, it.durationSec / 60, it.subjectId) } +
            sessions.filter { it.completed }.map { Act(it.date, it.endMin - it.startMin, it.subjectId) }
        val byDay = acts.groupBy { it.day }.mapValues { e -> e.value.sumOf { it.min } }

        val now = LocalDate.ofEpochDay(today)
        val monthStart = now.withDayOfMonth(1).toEpochDay()
        val last7 = (6 downTo 0).map { off ->
            val d = today - off
            Dates.fmt(d, "EEE") to (byDay[d] ?: 0)
        }
        val ym = YearMonth.from(now)
        val monthly = (5 downTo 0).map { off ->
            val m = ym.minusMonths(off.toLong())
            val from = m.atDay(1).toEpochDay(); val to = m.atEndOfMonth().toEpochDay()
            m.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()) to byDay.filterKeys { it in from..to }.values.sum()
        }
        val names = subjects.associateBy { it.id }
        val bySubject = acts.groupBy { it.subjectId }
            .map { (id, l) ->
                val s = names[id]
                Triple(s?.name ?: "Unassigned", l.sumOf { it.min }, s?.color ?: 0xFF64748B.toInt())
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }

        val activeDays = byDay.filterValues { it > 0 }.keys
        val focusMin = studyFocus.sumOf { it.durationSec } / 60
        return Analytics(
            totalMin = byDay.values.sum(),
            todayMin = byDay[today] ?: 0,
            weekMin = (0..6).sumOf { byDay[today - it] ?: 0 },
            monthMin = byDay.filterKeys { it in monthStart..today }.values.sum(),
            last7 = last7,
            monthly = monthly,
            bySubject = bySubject,
            completedTasks = tasks.count { it.status == Constants.DONE },
            pendingTasks = tasks.count { it.status != Constants.DONE },
            streak = StreakCalc.current(activeDays, today),
            longest = StreakCalc.longest(activeDays),
            focusCount = studyFocus.size,
            focusMin = focusMin,
            avgFocusMin = if (studyFocus.isEmpty()) 0 else focusMin / studyFocus.size
        )
    }
}
