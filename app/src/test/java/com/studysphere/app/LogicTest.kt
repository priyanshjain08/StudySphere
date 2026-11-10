package com.studysphere.app

import com.studysphere.app.data.*
import com.studysphere.app.domain.*
import org.junit.Assert.*
import org.junit.Test

class StreakCalcTest {
    @Test fun emptyHasNoStreak() {
        assertEquals(0, StreakCalc.current(emptySet(), 100))
        assertEquals(0, StreakCalc.longest(emptySet()))
    }
    @Test fun streakIncludingToday() = assertEquals(3, StreakCalc.current(setOf(98L, 99L, 100L), 100))
    @Test fun streakEndingYesterdayStillCounts() = assertEquals(2, StreakCalc.current(setOf(98L, 99L), 100))
    @Test fun gapBreaksStreak() = assertEquals(0, StreakCalc.current(setOf(90L, 91L), 100))
    @Test fun longestStreak() = assertEquals(4, StreakCalc.longest(setOf(1L, 2L, 3L, 4L, 10L, 11L)))
}

class TaskLogicTest {
    private fun t(id: Long, deadline: Long, status: Int = 0, priority: Int = 1) = Task(id = id, title = "T$id", deadline = deadline, status = status, priority = priority, createdAt = id)
    @Test fun overdueRules() {
        assertTrue(TaskLogic.isOverdue(t(1, 9), 10))
        assertFalse(TaskLogic.isOverdue(t(1, 10), 10))
        assertFalse(TaskLogic.isOverdue(t(1, 9, status = Constants.DONE), 10))
    }
    @Test fun filterAndSearch() {
        val list = listOf(t(1, 5), t(2, 20, status = 2), t(3, 30, status = 1))
        assertEquals(listOf(1L), TaskLogic.filter(list, "", 4, 10).map { it.id })
        assertEquals(listOf(2L), TaskLogic.filter(list, "t2", 0, 10).map { it.id })
        assertEquals(listOf(3L), TaskLogic.filter(list, "", 2, 10).map { it.id })
    }
    @Test fun sortByPriorityAndDeadline() {
        val list = listOf(t(1, 5, priority = 0), t(2, 6, priority = 3), t(3, 4, priority = 3))
        assertEquals(listOf(3L, 2L, 1L), TaskLogic.sort(list, 1).map { it.id })
        assertEquals(listOf(3L, 1L, 2L), TaskLogic.sort(list, 0).map { it.id })
    }
}

class SessionLogicTest {
    private fun s(id: Long, day: Long, a: Int, b: Int) = StudySession(id = id, title = "S$id", date = day, startMin = a, endMin = b)
    @Test fun invalidRangesRejected() {
        assertFalse(SessionLogic.validTimes(600, 600))
        assertFalse(SessionLogic.validTimes(700, 600))
        assertTrue(SessionLogic.validTimes(600, 660))
    }
    @Test fun detectsOverlapButNotTouching() {
        val all = listOf(s(1, 5, 600, 660))
        assertEquals(1, SessionLogic.overlaps(all, 5, 630, 700, 0).size)
        assertEquals(0, SessionLogic.overlaps(all, 5, 660, 700, 0).size)
        assertEquals(0, SessionLogic.overlaps(all, 6, 630, 700, 0).size)
        assertEquals(0, SessionLogic.overlaps(all, 5, 630, 700, 1).size)
    }
}

class TimerMathTest {
    @Test fun runningUsesEndTime() = assertEquals(60, TimerMath.remaining(true, 61_000, 999, 1_500))
    @Test fun pausedUsesStoredValue() = assertEquals(300, TimerMath.remaining(false, 0, 300, 5_000))
    @Test fun neverNegative() = assertEquals(0, TimerMath.remaining(true, 1_000, 0, 9_000))
    @Test fun progress() {
        assertEquals(0.5f, TimerMath.progress(100, 50), 0.001f)
        assertEquals(0f, TimerMath.progress(0, 0), 0.001f)
    }
}

class SpacedRepetitionTest {
    private val card = Flashcard(deckId = 1, question = "q", answer = "a")
    @Test fun newEasyCardIsDueInThreeDays() = assertEquals(103L, SpacedRepetition.next(card, SpacedRepetition.EASY, 100).dueDay)
    @Test fun hardResetsToTomorrowAndLowersEase() {
        val n = SpacedRepetition.next(card.copy(reps = 4, intervalDays = 20), SpacedRepetition.HARD, 100)
        assertEquals(101L, n.dueDay); assertEquals(0, n.reps); assertTrue(n.ease < 2.5)
    }
    @Test fun mediumIntervalsGrow() {
        val first = SpacedRepetition.next(card, SpacedRepetition.MEDIUM, 100)
        val second = SpacedRepetition.next(first, SpacedRepetition.MEDIUM, 101)
        val third = SpacedRepetition.next(second, SpacedRepetition.MEDIUM, 104)
        assertTrue(first.intervalDays < second.intervalDays && second.intervalDays < third.intervalDays)
    }
    @Test fun easeNeverBelowFloor() {
        var c = card
        repeat(20) { c = SpacedRepetition.next(c, SpacedRepetition.HARD, 100) }
        assertTrue(c.ease >= 1.3)
    }
}

class AnalyticsTest {
    private val today = 20_000L
    @Test fun noActivityGivesZeros() {
        val a = AnalyticsCalc.compute(emptyList(), emptyList(), emptyList(), emptyList(), today)
        assertEquals(0, a.totalMin); assertEquals(0, a.streak); assertEquals(0, a.longest)
        assertTrue(a.bySubject.isEmpty()); assertEquals(0, a.avgFocusMin)
    }
    @Test fun combinesFocusAndCompletedSessions() {
        val subj = Subject(id = 1, name = "Math", color = 0)
        val focus = listOf(FocusSession(subjectId = 1, durationSec = 1500, completedAt = 0, day = today))
        val sessions = listOf(
            StudySession(title = "a", subjectId = 1, date = today - 1, startMin = 600, endMin = 660, completed = true),
            StudySession(title = "b", subjectId = 1, date = today, startMin = 700, endMin = 760, completed = false)
        )
        val a = AnalyticsCalc.compute(focus, sessions, emptyList(), listOf(subj), today)
        assertEquals(85, a.totalMin); assertEquals(25, a.todayMin); assertEquals(2, a.streak)
        assertEquals("Math", a.bySubject.first().first)
    }
}

class BackupValidationTest {
    @Test fun roundTrip() {
        val b = Backup(subjects = listOf(Subject(id = 1, name = "Math", color = 1)),
            tasks = listOf(Task(id = 1, title = "t", subjectId = 1, deadline = 10)))
        val decoded = BackupIO.decode(BackupIO.encode(b))
        assertEquals(1, decoded.tasks.size)
    }
    @Test fun rejectsGarbage() { assertThrows(IllegalArgumentException::class.java) { BackupIO.decode("not json") } }
    @Test fun rejectsUnknownSubjectReference() {
        val b = Backup(tasks = listOf(Task(id = 1, title = "t", subjectId = 99, deadline = 10)))
        assertThrows(IllegalArgumentException::class.java) { BackupIO.validate(b) }
    }
    @Test fun rejectsInvalidTimeRange() {
        val b = Backup(sessions = listOf(StudySession(id = 1, title = "s", date = 1, startMin = 700, endMin = 600)))
        assertThrows(IllegalArgumentException::class.java) { BackupIO.validate(b) }
    }
}
