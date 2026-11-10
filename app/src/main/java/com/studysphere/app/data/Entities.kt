package com.studysphere.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

private const val SET_NULL = ForeignKey.SET_NULL
private const val CASCADE = ForeignKey.CASCADE

@Serializable
@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    val icon: String = "book"
)

@Serializable
@Entity(
    tableName = "study_sessions",
    foreignKeys = [ForeignKey(Subject::class, ["id"], ["subjectId"], onDelete = SET_NULL)],
    indices = [Index("subjectId"), Index("date")]
)
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subjectId: Long? = null,
    /** Local date as epoch day. */
    val date: Long,
    /** Minutes after midnight. */
    val startMin: Int,
    val endMin: Int,
    val description: String = "",
    val completed: Boolean = false
)

@Serializable
@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(Subject::class, ["id"], ["subjectId"], onDelete = SET_NULL)],
    indices = [Index("subjectId"), Index("deadline")]
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val subjectId: Long? = null,
    val deadline: Long,
    /** 0 Low, 1 Medium, 2 High, 3 Urgent */
    val priority: Int = 1,
    /** 0 Pending, 1 In progress, 2 Completed */
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Serializable
@Entity(
    tableName = "notes",
    foreignKeys = [ForeignKey(Subject::class, ["id"], ["subjectId"], onDelete = SET_NULL)],
    indices = [Index("subjectId")]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String = "",
    val subjectId: Long? = null,
    /** Comma separated tags. */
    val tags: String = "",
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "flashcard_decks",
    foreignKeys = [ForeignKey(Subject::class, ["id"], ["subjectId"], onDelete = SET_NULL)],
    indices = [Index("subjectId")]
)
data class FlashcardDeck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val subjectId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(FlashcardDeck::class, ["id"], ["deckId"], onDelete = CASCADE),
        ForeignKey(Subject::class, ["id"], ["subjectId"], onDelete = SET_NULL)
    ],
    indices = [Index("deckId"), Index("subjectId"), Index("dueDay")]
)
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val question: String,
    val answer: String,
    val subjectId: Long? = null,
    val tags: String = "",
    /** Next review date (epoch day). New cards are due immediately. */
    val dueDay: Long = 0,
    val intervalDays: Int = 0,
    val ease: Double = 2.5,
    val reps: Int = 0
)

@Serializable
@Entity(
    tableName = "flashcard_reviews",
    foreignKeys = [
        ForeignKey(Flashcard::class, ["id"], ["cardId"], onDelete = CASCADE),
        ForeignKey(FlashcardDeck::class, ["id"], ["deckId"], onDelete = CASCADE)
    ],
    indices = [Index("cardId"), Index("deckId")]
)
data class FlashcardReview(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val deckId: Long,
    /** 1 Hard, 2 Medium, 3 Easy */
    val rating: Int,
    val reviewedAt: Long,
    val day: Long
)

@Serializable
@Entity(
    tableName = "examinations",
    foreignKeys = [ForeignKey(Subject::class, ["id"], ["subjectId"], onDelete = SET_NULL)],
    indices = [Index("subjectId"), Index("date")]
)
data class Examination(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val subjectId: Long? = null,
    val date: Long,
    val timeMin: Int = 9 * 60,
    val location: String = "",
    /** Preparation progress, 0..100 */
    val progress: Int = 0,
    val notes: String = ""
)

@Serializable
@Entity(
    tableName = "focus_sessions",
    foreignKeys = [ForeignKey(Subject::class, ["id"], ["subjectId"], onDelete = SET_NULL)],
    indices = [Index("subjectId"), Index("day")]
)
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long? = null,
    val label: String = "",
    val durationSec: Int,
    /** 0 Focus, 3 Custom focus (breaks are not recorded) */
    val mode: Int = 0,
    val completedAt: Long,
    val day: Long
)
