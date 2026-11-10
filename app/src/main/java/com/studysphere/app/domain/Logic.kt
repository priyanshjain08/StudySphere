package com.studysphere.app.domain

import com.studysphere.app.data.Flashcard
import com.studysphere.app.data.StudySession
import com.studysphere.app.data.Task

object Constants {
    val PRIORITIES = listOf("Low", "Medium", "High", "Urgent")
    val STATUSES = listOf("Pending", "In Progress", "Completed")
    const val PENDING = 0
    const val IN_PROGRESS = 1
    const val DONE = 2
    val PRIORITY_COLORS = listOf(0xFF4ADE80, 0xFFFBBF24, 0xFFFB923C, 0xFFEF4444).map { it.toInt() }
    val LEVELS = listOf("High school", "Undergraduate", "Postgraduate", "Professional")
    val SUBJECT_COLORS = listOf(
        0xFF8B7CF6, 0xFF4F9DFF, 0xFF2DD4BF, 0xFF34D399, 0xFFFBBF24,
        0xFFFB923C, 0xFFF472B6, 0xFFEF4444, 0xFFA78BFA, 0xFF38BDF8
    ).map { it.toInt() }
    val SUBJECT_ICONS = listOf("book", "science", "math", "language", "code", "art", "history", "music", "sport", "world")
    val MODE_NAMES = listOf("Focus", "Short break", "Long break", "Custom")
}

object TaskLogic {
    fun isOverdue(t: Task, today: Long) = t.status != Constants.DONE && t.deadline < today

    /** filter: 0 All, 1 Pending, 2 In progress, 3 Completed, 4 Overdue */
    fun filter(list: List<Task>, query: String, filter: Int, today: Long): List<Task> {
        val q = query.trim()
        return list.filter { t ->
            val matches = q.isEmpty() || t.title.contains(q, true) || t.description.contains(q, true)
            val inFilter = when (filter) {
                1 -> t.status == Constants.PENDING
                2 -> t.status == Constants.IN_PROGRESS
                3 -> t.status == Constants.DONE
                4 -> isOverdue(t, today)
                else -> true
            }
            matches && inFilter
        }
    }

    /** mode: 0 Deadline, 1 Priority, 2 Recently created, 3 Completion status */
    fun sort(list: List<Task>, mode: Int): List<Task> = when (mode) {
        1 -> list.sortedWith(compareByDescending<Task> { it.priority }.thenBy { it.deadline })
        2 -> list.sortedByDescending { it.createdAt }
        3 -> list.sortedWith(compareBy<Task> { it.status }.thenBy { it.deadline })
        else -> list.sortedWith(compareBy<Task> { it.status == Constants.DONE }.thenBy { it.deadline })
    }
}

object SessionLogic {
    fun validTimes(start: Int, end: Int) = start in 0..1439 && end in 1..1440 && end > start

    fun overlaps(all: List<StudySession>, day: Long, start: Int, end: Int, ignoreId: Long): List<StudySession> =
        all.filter { it.id != ignoreId && it.date == day && it.startMin < end && start < it.endMin }
}

object StreakCalc {
    /** Consecutive active days ending today (or yesterday if today has no activity yet). */
    fun current(days: Set<Long>, today: Long): Int {
        var d = if (today in days) today else today - 1
        var n = 0
        while (d in days) { n++; d-- }
        return n
    }

    fun longest(days: Set<Long>): Int {
        if (days.isEmpty()) return 0
        val s = days.sorted()
        var best = 1; var run = 1
        for (i in 1 until s.size) {
            if (s[i] == s[i - 1] + 1) { run++; if (run > best) best = run } else run = 1
        }
        return best
    }
}

object TimerMath {
    fun remaining(running: Boolean, endAt: Long, storedRemaining: Int, now: Long): Int =
        if (running) Math.ceil((endAt - now) / 1000.0).toInt().coerceAtLeast(0) else storedRemaining

    fun progress(total: Int, remaining: Int): Float =
        if (total <= 0) 0f else (1f - remaining.toFloat() / total).coerceIn(0f, 1f)
}

object SpacedRepetition {
    const val HARD = 1
    const val MEDIUM = 2
    const val EASY = 3

    fun next(c: Flashcard, rating: Int, today: Long): Flashcard = when (rating) {
        HARD -> c.copy(reps = 0, intervalDays = 1, ease = (c.ease - 0.2).coerceAtLeast(1.3), dueDay = today + 1)
        MEDIUM -> {
            val i = when (c.reps) {
                0 -> 1
                1 -> 3
                else -> Math.round(c.intervalDays * c.ease).toInt().coerceAtLeast(c.intervalDays + 1)
            }
            c.copy(reps = c.reps + 1, intervalDays = i, dueDay = today + i)
        }
        else -> {
            val i = when (c.reps) {
                0 -> 3
                1 -> 6
                else -> Math.round(c.intervalDays * c.ease * 1.3).toInt().coerceAtLeast(c.intervalDays + 2)
            }
            c.copy(reps = c.reps + 1, intervalDays = i, ease = c.ease + 0.1, dueDay = today + i)
        }
    }
}
