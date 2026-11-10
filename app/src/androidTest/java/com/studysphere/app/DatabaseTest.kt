package com.studysphere.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.studysphere.app.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: StudyRepository

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
        repo = StudyRepository(db)
    }
    @After fun tearDown() = db.close()

    @Test fun deletingSubjectKeepsTasks() = runBlocking {
        val sid = repo.saveSubject(Subject(name = "Math", color = 1))
        repo.saveTask(Task(title = "HW", subjectId = sid, deadline = 5))
        repo.deleteSubject(sid)
        val tasks = repo.tasks.first()
        assertEquals(1, tasks.size); assertNull(tasks[0].subjectId)
    }

    @Test fun deletingDeckRemovesCards() = runBlocking {
        val did = repo.saveDeck(FlashcardDeck(name = "D"))
        repo.saveCard(Flashcard(deckId = did, question = "q", answer = "a"))
        repo.deleteDeck(did)
        assertTrue(repo.cards.first().isEmpty())
    }

    @Test fun reviewUpdatesCardAndHistory() = runBlocking {
        val did = repo.saveDeck(FlashcardDeck(name = "D"))
        val cid = repo.saveCard(Flashcard(deckId = did, question = "q", answer = "a"))
        repo.review(repo.cards.first().first { it.id == cid }, 3)
        assertEquals(1, repo.reviews.first().size)
        assertTrue(repo.cards.first().first().dueDay > 0)
    }

    @Test fun backupRestoreReplacesData() = runBlocking {
        repo.saveSubject(Subject(name = "Old", color = 1))
        repo.replaceAll(Backup(subjects = listOf(Subject(id = 7, name = "New", color = 2))))
        assertEquals(listOf("New"), repo.subjects.first().map { it.name })
    }
}
