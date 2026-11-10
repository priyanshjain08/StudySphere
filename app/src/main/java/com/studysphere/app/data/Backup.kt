package com.studysphere.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
data class ProfileBackup(
    val name: String = "",
    val level: String = "Undergraduate",
    val goalMin: Int = 120,
    val theme: Int = 2,
    val focusMin: Int = 25,
    val shortMin: Int = 5,
    val longMin: Int = 15,
    val nSession: Boolean = true,
    val nTask: Boolean = true,
    val nExam: Boolean = true,
    val nDaily: Boolean = true
)

@Serializable
data class Backup(
    val app: String = "StudySphere",
    val version: Int = 1,
    val exportedAt: Long = 0,
    val profile: ProfileBackup = ProfileBackup(),
    val subjects: List<Subject> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val sessions: List<StudySession> = emptyList(),
    val notes: List<Note> = emptyList(),
    val decks: List<FlashcardDeck> = emptyList(),
    val cards: List<Flashcard> = emptyList(),
    val reviews: List<FlashcardReview> = emptyList(),
    val exams: List<Examination> = emptyList(),
    val focus: List<FocusSession> = emptyList()
)

object BackupIO {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    fun encode(b: Backup): String = json.encodeToString(Backup.serializer(), b)

    /** Parses and validates a backup. Throws IllegalArgumentException with a readable message when invalid. */
    fun decode(text: String): Backup {
        val b = try {
            json.decodeFromString(Backup.serializer(), text)
        } catch (e: SerializationException) {
            throw IllegalArgumentException("This file is not a valid StudySphere backup.")
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("This file is not a valid StudySphere backup.")
        }
        validate(b)
        return b
    }

    fun validate(b: Backup) {
        require(b.app == "StudySphere") { "This file was not created by StudySphere." }
        require(b.version == 1) { "Unsupported backup version ${b.version}." }
        fun unique(ids: List<Long>, what: String) =
            require(ids.toSet().size == ids.size) { "Backup contains duplicate $what." }
        unique(b.subjects.map { it.id }, "subjects"); unique(b.tasks.map { it.id }, "tasks")
        unique(b.sessions.map { it.id }, "study sessions"); unique(b.notes.map { it.id }, "notes")
        unique(b.decks.map { it.id }, "decks"); unique(b.cards.map { it.id }, "flashcards")
        unique(b.reviews.map { it.id }, "reviews"); unique(b.exams.map { it.id }, "exams")
        unique(b.focus.map { it.id }, "focus sessions")

        val sid = b.subjects.map { it.id }.toSet()
        fun ok(id: Long?) = id == null || id in sid
        require(b.tasks.all { ok(it.subjectId) && it.priority in 0..3 && it.status in 0..2 && it.title.isNotBlank() }) { "Backup contains invalid tasks." }
        require(b.sessions.all { ok(it.subjectId) && it.startMin >= 0 && it.endMin <= 1440 && it.endMin > it.startMin }) { "Backup contains invalid study sessions." }
        require(b.notes.all { ok(it.subjectId) }) { "Backup contains notes with unknown subjects." }
        require(b.decks.all { ok(it.subjectId) }) { "Backup contains decks with unknown subjects." }
        require(b.exams.all { ok(it.subjectId) && it.progress in 0..100 }) { "Backup contains invalid exams." }
        require(b.focus.all { ok(it.subjectId) && it.durationSec > 0 }) { "Backup contains invalid focus sessions." }
        val did = b.decks.map { it.id }.toSet()
        require(b.cards.all { it.deckId in did && ok(it.subjectId) }) { "Backup contains flashcards without a deck." }
        val cid = b.cards.map { it.id }.toSet()
        require(b.reviews.all { it.cardId in cid && it.deckId in did && it.rating in 1..3 }) { "Backup contains invalid review history." }
        require(b.profile.goalMin in 5..1440 && b.profile.theme in 0..2) { "Backup contains invalid preferences." }
        require(b.profile.focusMin in 1..300 && b.profile.shortMin in 1..120 && b.profile.longMin in 1..120) { "Backup contains invalid timer preferences." }
    }
}
