package com.krbavcic.pentalingo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.krbavcic.pentalingo.model.Progress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val Context.store: DataStore<Preferences> by preferencesDataStore("pentalingo")

/** The learner's choices that are not progress. */
data class Settings(
    val sounds: Boolean = true,
    /** Play a word's pronunciation as soon as it appears. */
    val autoplay: Boolean = true,
    /** Tester mode: a report button on every lesson screen, and the switches below. */
    val tester: Boolean = false,
    /** Tester mode: open any lesson, in any order. */
    val openAnyLesson: Boolean = false,
    /** Tester mode: a button that answers the current exercise correctly. */
    val answerButton: Boolean = false,
)

/**
 * Where progress lives between sessions: one JSON document in DataStore, read and written whole.
 *
 * The record is small — a few hundred memories — and always used whole, so a database would be
 * cost without benefit. Unreadable data yields a fresh [Progress] rather than a crash: losing a
 * record is bad, an app that will not open is worse.
 */
class ProgressStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    val progress: Flow<Progress> = context.store.data.map { it[PROGRESS]?.let(::decode) ?: Progress() }

    val settings: Flow<Settings> = context.store.data.map {
        Settings(
            sounds = it[SOUNDS] ?: true,
            autoplay = it[AUTOPLAY] ?: true,
            tester = it[TESTER] ?: false,
            openAnyLesson = it[OPEN_ANY] ?: false,
            answerButton = it[ANSWER_BUTTON] ?: false,
        )
    }

    suspend fun update(transform: (Progress) -> Progress) {
        context.store.edit { prefs ->
            val current = prefs[PROGRESS]?.let(::decode) ?: Progress()
            prefs[PROGRESS] = json.encodeToString(Progress.serializer(), transform(current))
        }
    }

    suspend fun setSounds(on: Boolean) {
        context.store.edit { it[SOUNDS] = on }
    }

    suspend fun setAutoplay(on: Boolean) {
        context.store.edit { it[AUTOPLAY] = on }
    }

    suspend fun setTester(on: Boolean) {
        context.store.edit { it[TESTER] = on }
    }

    suspend fun setOpenAnyLesson(on: Boolean) {
        context.store.edit { it[OPEN_ANY] = on }
    }

    suspend fun setAnswerButton(on: Boolean) {
        context.store.edit { it[ANSWER_BUTTON] = on }
    }

    suspend fun reset() {
        context.store.edit { it.remove(PROGRESS) }
    }

    private fun decode(raw: String): Progress? = try {
        json.decodeFromString(Progress.serializer(), raw)
    } catch (e: SerializationException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    private companion object {
        val PROGRESS = stringPreferencesKey("progress-json")
        val SOUNDS = booleanPreferencesKey("sounds")
        val AUTOPLAY = booleanPreferencesKey("autoplay")
        val TESTER = booleanPreferencesKey("tester")
        val OPEN_ANY = booleanPreferencesKey("tester-open-any-lesson")
        val ANSWER_BUTTON = booleanPreferencesKey("tester-answer-button")
    }
}
