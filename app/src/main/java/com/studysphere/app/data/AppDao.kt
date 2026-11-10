package com.studysphere.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // ---- Subjects
    @Query("SELECT * FROM subjects ORDER BY name COLLATE NOCASE") fun subjects(): Flow<List<Subject>>
    @Upsert suspend fun upsertSubject(s: Subject): Long
    @Query("DELETE FROM subjects WHERE id = :id") suspend fun deleteSubject(id: Long)

    // ---- Study sessions
    @Query("SELECT * FROM study_sessions ORDER BY date, startMin") fun sessions(): Flow<List<StudySession>>
    @Upsert suspend fun upsertSession(s: StudySession): Long
    @Query("DELETE FROM study_sessions WHERE id = :id") suspend fun deleteSession(id: Long)

    // ---- Tasks
    @Query("SELECT * FROM tasks ORDER BY deadline") fun tasks(): Flow<List<Task>>
    @Upsert suspend fun upsertTask(t: Task): Long
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun deleteTask(id: Long)

    // ---- Notes
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC") fun notes(): Flow<List<Note>>
    @Upsert suspend fun upsertNote(n: Note): Long
    @Query("DELETE FROM notes WHERE id = :id") suspend fun deleteNote(id: Long)

    // ---- Flashcards
    @Query("SELECT * FROM flashcard_decks ORDER BY createdAt DESC") fun decks(): Flow<List<FlashcardDeck>>
    @Upsert suspend fun upsertDeck(d: FlashcardDeck): Long
    @Query("DELETE FROM flashcard_decks WHERE id = :id") suspend fun deleteDeck(id: Long)
    @Query("SELECT * FROM flashcards ORDER BY id") fun cards(): Flow<List<Flashcard>>
    @Upsert suspend fun upsertCard(c: Flashcard): Long
    @Query("DELETE FROM flashcards WHERE id = :id") suspend fun deleteCard(id: Long)
    @Query("SELECT * FROM flashcard_reviews ORDER BY reviewedAt DESC") fun reviews(): Flow<List<FlashcardReview>>
    @Insert suspend fun insertReview(r: FlashcardReview): Long

    // ---- Exams
    @Query("SELECT * FROM examinations ORDER BY date, timeMin") fun exams(): Flow<List<Examination>>
    @Upsert suspend fun upsertExam(e: Examination): Long
    @Query("DELETE FROM examinations WHERE id = :id") suspend fun deleteExam(id: Long)

    // ---- Focus sessions
    @Query("SELECT * FROM focus_sessions ORDER BY completedAt DESC") fun focus(): Flow<List<FocusSession>>
    @Insert suspend fun insertFocus(f: FocusSession): Long

    // ---- Bulk operations used by backup restore
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSubjects(l: List<Subject>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSessions(l: List<StudySession>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTasks(l: List<Task>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertNotes(l: List<Note>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDecks(l: List<FlashcardDeck>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCards(l: List<Flashcard>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertReviews(l: List<FlashcardReview>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertExams(l: List<Examination>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFocusList(l: List<FocusSession>)

    @Query("DELETE FROM flashcard_reviews") suspend fun clearReviews()
    @Query("DELETE FROM flashcards") suspend fun clearCards()
    @Query("DELETE FROM flashcard_decks") suspend fun clearDecks()
    @Query("DELETE FROM focus_sessions") suspend fun clearFocus()
    @Query("DELETE FROM study_sessions") suspend fun clearSessions()
    @Query("DELETE FROM tasks") suspend fun clearTasks()
    @Query("DELETE FROM notes") suspend fun clearNotes()
    @Query("DELETE FROM examinations") suspend fun clearExams()
    @Query("DELETE FROM subjects") suspend fun clearSubjects()
}
