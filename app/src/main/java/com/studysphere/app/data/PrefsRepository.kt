package com.studysphere.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "studysphere_prefs")

object Keys {
    val ONBOARDED = booleanPreferencesKey("onboarded")
    val NAME = stringPreferencesKey("name")
    val LEVEL = stringPreferencesKey("level")
    val GOAL = intPreferencesKey("goal_min")
    /** 0 = system, 1 = light, 2 = dark */
    val THEME = intPreferencesKey("theme")
    val FOCUS_MIN = intPreferencesKey("focus_min")
    val SHORT_MIN = intPreferencesKey("short_min")
    val LONG_MIN = intPreferencesKey("long_min")
    val N_SESSION = booleanPreferencesKey("n_session")
    val N_TASK = booleanPreferencesKey("n_task")
    val N_EXAM = booleanPreferencesKey("n_exam")
    val N_DAILY = booleanPreferencesKey("n_daily")
}

data class Prefs(
    val loaded: Boolean = false,
    val onboarded: Boolean = false,
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

class PrefsRepository(private val context: Context) {
    val prefs: Flow<Prefs> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { p ->
            Prefs(
                loaded = true,
                onboarded = p[Keys.ONBOARDED] ?: false,
                name = p[Keys.NAME] ?: "",
                level = p[Keys.LEVEL] ?: "Undergraduate",
                goalMin = p[Keys.GOAL] ?: 120,
                theme = p[Keys.THEME] ?: 2,
                focusMin = p[Keys.FOCUS_MIN] ?: 25,
                shortMin = p[Keys.SHORT_MIN] ?: 5,
                longMin = p[Keys.LONG_MIN] ?: 15,
                nSession = p[Keys.N_SESSION] ?: true,
                nTask = p[Keys.N_TASK] ?: true,
                nExam = p[Keys.N_EXAM] ?: true,
                nDaily = p[Keys.N_DAILY] ?: true
            )
        }

    suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun completeOnboarding(name: String, level: String, goal: Int, theme: Int) {
        context.dataStore.edit {
            it[Keys.NAME] = name; it[Keys.LEVEL] = level; it[Keys.GOAL] = goal
            it[Keys.THEME] = theme; it[Keys.ONBOARDED] = true
        }
    }

    suspend fun clear() { context.dataStore.edit { it.clear() } }

    suspend fun applyProfile(p: ProfileBackup) {
        context.dataStore.edit {
            it[Keys.NAME] = p.name; it[Keys.LEVEL] = p.level; it[Keys.GOAL] = p.goalMin
            it[Keys.THEME] = p.theme; it[Keys.FOCUS_MIN] = p.focusMin
            it[Keys.SHORT_MIN] = p.shortMin; it[Keys.LONG_MIN] = p.longMin
            it[Keys.N_SESSION] = p.nSession; it[Keys.N_TASK] = p.nTask
            it[Keys.N_EXAM] = p.nExam; it[Keys.N_DAILY] = p.nDaily
            it[Keys.ONBOARDED] = true
        }
    }
}
