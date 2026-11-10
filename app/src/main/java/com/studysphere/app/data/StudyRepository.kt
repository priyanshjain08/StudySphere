package com.studysphere.app.data

import androidx.room.withTransaction
import com.studysphere.app.domain.SpacedRepetition
import com.studysphere.app.util.Dates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Single access point for the Room database (offline-first source of truth). */
class StudyRepository(private val db: AppDatabase) {
    private val d = db.dao()

    val subjects = d.subjects()
    val sessions = d.sessions()
    val tasks = d.tasks()
    val notes = d.notes()
    val decks = d.decks()
    val cards = d.cards()
    val reviews = d.reviews()
    val exams = d.exams()
    val focus = d.focus()

    suspend fun saveSubject(s: Subject) = d.upsertSubject(s)
    suspend fun deleteSubject(id: Long) = d.deleteSubject(id)
    suspend fun saveSession(s: StudySession) = d.upsertSession(s)
    suspend fun deleteSession(id: Long) = d.deleteSession(id)
    suspend fun saveTask(t: Task) = d.upsertTask(t)
    suspend fun deleteTask(id: Long) = d.deleteTask(id)
    suspend fun saveNote(n: Note) = d.upsertNote(n)
    suspend fun deleteNote(id: Long) = d.deleteNote(id)
    suspend fun saveDeck(x: FlashcardDeck) = d.upsertDeck(x)
    suspend fun deleteDeck(id: Long) = d.deleteDeck(id)
    suspend fun saveCard(c: Flashcard) = d.upsertCard(c)
    suspend fun deleteCard(id: Long) = d.deleteCard(id)
    suspend fun saveExam(e: Examination) = d.upsertExam(e)
    suspend fun deleteExam(id: Long) = d.deleteExam(id)
    suspend fun saveFocus(f: FocusSession) = d.insertFocus(f)

    /** Applies the spaced-repetition rules and stores the review in one transaction. */
    suspend fun review(card: Flashcard, rating: Int) {
        val today = Dates.today()
        val next = SpacedRepetition.next(card, rating, today)
        db.withTransaction {
            d.upsertCard(next)
            d.insertReview(FlashcardReview(cardId = card.id, deckId = card.deckId, rating = rating, reviewedAt = System.currentTimeMillis(), day = today))
        }
    }

    suspend fun buildBackup(p: Prefs) = Backup(
        exportedAt = System.currentTimeMillis(),
        profile = ProfileBackup(p.name, p.level, p.goalMin, p.theme, p.focusMin, p.shortMin, p.longMin, p.nSession, p.nTask, p.nExam, p.nDaily),
        subjects = subjects.first(), tasks = tasks.first(), sessions = sessions.first(), notes = notes.first(),
        decks = decks.first(), cards = cards.first(), reviews = reviews.first(), exams = exams.first(), focus = focus.first()
    )

    /** Replaces all data atomically. The backup must already be validated. */
    suspend fun replaceAll(b: Backup) = db.withTransaction {
        d.clearReviews(); d.clearCards(); d.clearDecks(); d.clearFocus(); d.clearSessions()
        d.clearTasks(); d.clearNotes(); d.clearExams(); d.clearSubjects()
        d.insertSubjects(b.subjects); d.insertDecks(b.decks); d.insertCards(b.cards); d.insertReviews(b.reviews)
        d.insertSessions(b.sessions); d.insertTasks(b.tasks); d.insertNotes(b.notes)
        d.insertExams(b.exams); d.insertFocusList(b.focus)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) { db.clearAllTables() }
}
