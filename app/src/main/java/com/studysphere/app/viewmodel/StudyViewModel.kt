package com.studysphere.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studysphere.app.StudyApp
import com.studysphere.app.data.*
import com.studysphere.app.domain.Analytics
import com.studysphere.app.domain.AnalyticsCalc
import com.studysphere.app.domain.Constants
import com.studysphere.app.util.Dates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudyViewModel(app: Application) : AndroidViewModel(app) {
    private val sa = app as StudyApp
    val repo = sa.repo
    val timer = sa.timer

    val prefs: StateFlow<Prefs> = sa.prefsRepo.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, Prefs())

    /** Lists start as null while Room is loading so screens can show a loading state. */
    private fun <T> Flow<T>.st(): StateFlow<T?> =
        map { it as T? }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val subjects = repo.subjects.st()
    val sessions = repo.sessions.st()
    val tasks = repo.tasks.st()
    val notes = repo.notes.st()
    val decks = repo.decks.st()
    val cards = repo.cards.st()
    val reviews = repo.reviews.st()
    val exams = repo.exams.st()
    val focus = repo.focus.st()

    val analytics: StateFlow<Analytics?> = combine(repo.focus, repo.sessions, repo.tasks, repo.subjects) { f, s, t, sub ->
        AnalyticsCalc.compute(f, s, t, sub, Dates.today())
    }.flowOn(Dispatchers.Default).st()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()
    val pendingImport = MutableStateFlow<Backup?>(null)

    fun launchSafe(ok: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                ok?.let { _messages.tryEmit(it) }
            } catch (e: IllegalArgumentException) {
                _messages.tryEmit(e.message ?: "Invalid input")
            } catch (e: Exception) {
                _messages.tryEmit("Something went wrong: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    // ---- Preferences
    fun <T> pref(key: Preferences.Key<T>, value: T) = launchSafe { sa.prefsRepo.set(key, value) }

    fun finishOnboarding(name: String, level: String, goal: Int, theme: Int, subjectNames: List<String>) = launchSafe {
        subjectNames.map { it.trim() }.filter { it.isNotEmpty() }.distinct().forEachIndexed { i, n ->
            repo.saveSubject(Subject(name = n, color = Constants.SUBJECT_COLORS[i % Constants.SUBJECT_COLORS.size], icon = Constants.SUBJECT_ICONS[i % Constants.SUBJECT_ICONS.size]))
        }
        sa.prefsRepo.completeOnboarding(name.trim(), level, goal, theme)
    }

    // ---- Subjects
    fun saveSubject(s: Subject) = launchSafe("Subject saved") { repo.saveSubject(s) }
    fun deleteSubject(id: Long) = launchSafe("Subject deleted. Its items were kept without a subject.") { repo.deleteSubject(id) }

    // ---- Study sessions
    fun saveSession(s: StudySession) = launchSafe("Study session saved") { repo.saveSession(s) }
    fun deleteSession(id: Long) = launchSafe("Study session deleted") { repo.deleteSession(id) }
    fun toggleSession(s: StudySession) = launchSafe { repo.saveSession(s.copy(completed = !s.completed)) }

    // ---- Tasks
    fun saveTask(t: Task) = launchSafe("Task saved") {
        val done = t.status == Constants.DONE
        repo.saveTask(t.copy(completedAt = if (done) (t.completedAt ?: System.currentTimeMillis()) else null))
    }
    fun deleteTask(id: Long) = launchSafe("Task deleted") { repo.deleteTask(id) }
    fun toggleTask(t: Task) = launchSafe {
        val done = t.status != Constants.DONE
        repo.saveTask(t.copy(status = if (done) Constants.DONE else Constants.PENDING, completedAt = if (done) System.currentTimeMillis() else null))
    }

    // ---- Notes
    fun deleteNote(id: Long) = launchSafe("Note deleted") { repo.deleteNote(id) }
    fun togglePin(n: Note) = launchSafe { repo.saveNote(n.copy(pinned = !n.pinned, updatedAt = n.updatedAt)) }

    // ---- Flashcards
    fun saveDeck(d: FlashcardDeck) = launchSafe("Deck saved") { repo.saveDeck(d) }
    fun deleteDeck(id: Long) = launchSafe("Deck deleted") { repo.deleteDeck(id) }
    fun saveCard(c: Flashcard) = launchSafe("Flashcard saved") { repo.saveCard(c) }
    fun deleteCard(id: Long) = launchSafe("Flashcard removed") { repo.deleteCard(id) }
    fun review(c: Flashcard, rating: Int) = launchSafe { repo.review(c, rating) }

    // ---- Exams
    fun saveExam(e: Examination) = launchSafe("Exam saved") { repo.saveExam(e) }
    fun deleteExam(id: Long) = launchSafe("Exam deleted") { repo.deleteExam(id) }
    fun setExamProgress(e: Examination, p: Int) = launchSafe { repo.saveExam(e.copy(progress = p)) }

    // ---- Backup / restore / reset
    fun export(uri: Uri) = launchSafe("Backup exported") {
        val text = BackupIO.encode(repo.buildBackup(prefs.value))
        withContext(Dispatchers.IO) {
            val out = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
                ?: throw IllegalArgumentException("Could not open the selected file.")
            out.use { it.write(text.toByteArray()) }
        }
    }

    fun prepareImport(uri: Uri) = launchSafe {
        val text = withContext(Dispatchers.IO) {
            val input = getApplication<Application>().contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Could not open the selected file.")
            input.use { String(it.readBytes()) }
        }
        pendingImport.value = BackupIO.decode(text)
    }

    fun confirmImport() = launchSafe("Backup restored") {
        val b = pendingImport.value ?: return@launchSafe
        repo.replaceAll(b)
        sa.prefsRepo.applyProfile(b.profile)
        pendingImport.value = null
    }

    fun cancelImport() { pendingImport.value = null }

    fun resetApp() = launchSafe {
        timer.reset()
        repo.clearAll()
        sa.prefsRepo.clear()
    }
}
