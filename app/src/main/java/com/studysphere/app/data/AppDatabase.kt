package com.studysphere.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        Subject::class, StudySession::class, Task::class, Note::class,
        FlashcardDeck::class, Flashcard::class, FlashcardReview::class,
        Examination::class, FocusSession::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        /**
         * Add a Migration(from, to) here whenever the schema version is increased.
         * The database is never rebuilt destructively, so user data is always preserved.
         */
        val MIGRATIONS: Array<Migration> = arrayOf()

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "studysphere.db")
                .addMigrations(*MIGRATIONS)
                .build()
                .also { instance = it }
        }
    }
}
