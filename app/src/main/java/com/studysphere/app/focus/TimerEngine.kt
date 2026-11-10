package com.studysphere.app.focus

import android.content.Context
import com.studysphere.app.data.FocusSession
import com.studysphere.app.data.StudyRepository
import com.studysphere.app.domain.TimerMath
import com.studysphere.app.notifications.Notifier
import com.studysphere.app.util.Dates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * mode: 0 focus, 1 short break, 2 long break, 3 custom focus.
 * The countdown is based on a wall-clock end time ([endAt]) that is persisted, so it stays accurate
 * in the background and can be restored after the app process is recreated.
 */
data class TimerState(
    val mode: Int = 0,
    val totalSec: Int = 25 * 60,
    val remainingSec: Int = 25 * 60,
    val running: Boolean = false,
    val active: Boolean = false,
    val endAt: Long = 0,
    val subjectId: Long? = null,
    val label: String = ""
)

class TimerEngine(
    private val ctx: Context,
    private val repo: StudyRepository,
    private val scope: CoroutineScope
) {
    private val sp = ctx.getSharedPreferences("timer", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> get() = _state

    init { restore() }

    @Synchronized
    fun restore() {
        _state.value = TimerState(
            mode = sp.getInt("mode", 0),
            totalSec = sp.getInt("total", 25 * 60),
            remainingSec = sp.getInt("remaining", 25 * 60),
            running = sp.getBoolean("running", false),
            active = sp.getBoolean("active", false),
            endAt = sp.getLong("endAt", 0),
            subjectId = sp.getLong("subject", -1).takeIf { it >= 0 },
            label = sp.getString("label", "") ?: ""
        )
        if (_state.value.running) tick()
    }

    private fun persist(s: TimerState) {
        _state.value = s
        sp.edit().putInt("mode", s.mode).putInt("total", s.totalSec).putInt("remaining", s.remainingSec)
            .putBoolean("running", s.running).putBoolean("active", s.active).putLong("endAt", s.endAt)
            .putLong("subject", s.subjectId ?: -1).putString("label", s.label).apply()
    }

    private fun remaining(s: TimerState) = TimerMath.remaining(s.running, s.endAt, s.remainingSec, System.currentTimeMillis())

    @Synchronized
    fun select(mode: Int, sec: Int) {
        val s = _state.value
        if (s.active) return
        persist(s.copy(mode = mode, totalSec = sec, remainingSec = sec))
    }

    @Synchronized
    fun start(subjectId: Long?, label: String) {
        val s = _state.value
        if (s.active) return
        persist(s.copy(running = true, active = true, remainingSec = s.totalSec, endAt = System.currentTimeMillis() + s.totalSec * 1000L, subjectId = subjectId, label = label))
    }

    @Synchronized
    fun pause() {
        val s = _state.value
        if (!s.running) return
        persist(s.copy(running = false, remainingSec = remaining(s), endAt = 0))
    }

    @Synchronized
    fun resume() {
        val s = _state.value
        if (!s.active || s.running) return
        persist(s.copy(running = true, endAt = System.currentTimeMillis() + s.remainingSec * 1000L))
    }

    @Synchronized
    fun reset() {
        val s = _state.value
        persist(s.copy(running = false, active = false, endAt = 0, remainingSec = s.totalSec))
    }

    /** Ends the session early. Study time already completed (at least one minute) is still recorded. */
    @Synchronized
    fun skip() {
        val s = _state.value
        if (!s.active) return
        record(s, s.totalSec - remaining(s))
        persist(s.copy(running = false, active = false, endAt = 0, remainingSec = s.totalSec))
    }

    /** Returns true while the timer is still running. Completes the session when the time is up. */
    @Synchronized
    fun tick(): Boolean {
        val s = _state.value
        if (!s.running) return false
        val rem = remaining(s)
        if (rem <= 0) {
            record(s, s.totalSec)
            persist(s.copy(running = false, active = false, endAt = 0, remainingSec = s.totalSec))
            Notifier.timerDone(ctx, s)
            return false
        }
        if (rem != s.remainingSec) persist(s.copy(remainingSec = rem))
        return true
    }

    private fun record(s: TimerState, elapsedSec: Int) {
        if ((s.mode == 0 || s.mode == 3) && elapsedSec >= 60) {
            val now = System.currentTimeMillis()
            scope.launch {
                repo.saveFocus(FocusSession(subjectId = s.subjectId, label = s.label, durationSec = elapsedSec, mode = s.mode, completedAt = now, day = Dates.dayOf(now)))
            }
        }
    }
}
