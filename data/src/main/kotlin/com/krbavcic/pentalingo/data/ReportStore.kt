package com.krbavcic.pentalingo.data

import android.content.Context
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * An issue a tester flagged, with everything needed to find and fix it without asking.
 *
 * @property context the exact data on screen when it was flagged — item id, transliteration,
 *   spelling, pronunciation, the clip file, the exercise and its answer — as plain strings, so a
 *   report stays readable even after the course it describes has changed.
 * @property id a random id made on the phone, so a report sent twice is stored once. Reports
 *   saved before ids existed get one derived from their contents.
 */
@Serializable
data class Report(
    val at: Long,
    val category: String,
    val note: String,
    val app: String,
    val context: Map<String, String>,
    val id: String = "",
) {
    fun withId(): Report =
        if (id.isNotEmpty()) this else copy(id = UUID.nameUUIDFromBytes("$at|$category|$note".toByteArray()).toString())
}

/**
 * Tester reports, one JSON object per line, and the ids of those already sent.
 *
 * Kept in the app's external files directory — /sdcard/Android/data/<app id>/files/reports/ — so
 * they can be pulled with adb from a release build, which `run-as` cannot reach, as well as sent
 * from the app.
 */
class ReportStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val dir: File
        get() = File(context.getExternalFilesDir(null) ?: context.filesDir, "reports").also { it.mkdirs() }

    private val file: File get() = File(dir, "reports.jsonl")

    private val sentFile: File get() = File(dir, "sent.txt")

    suspend fun add(report: Report) = withContext(Dispatchers.IO) {
        file.appendText(json.encodeToString(Report.serializer(), report.withId()) + "\n")
    }

    suspend fun all(): List<Report> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        file.readLines().filter { it.isNotBlank() }.mapNotNull { line ->
            try {
                json.decodeFromString(Report.serializer(), line).withId()
            } catch (e: kotlinx.serialization.SerializationException) {
                null
            }
        }
    }

    /** Reports not yet accepted by the server, oldest first. */
    suspend fun unsent(): List<Report> {
        val sent = sentIds()
        return all().filter { it.id !in sent }
    }

    suspend fun markSent(ids: Collection<String>) = withContext(Dispatchers.IO) {
        if (ids.isNotEmpty()) sentFile.appendText(ids.joinToString("\n", postfix = "\n"))
    }

    private suspend fun sentIds(): Set<String> = withContext(Dispatchers.IO) {
        if (sentFile.exists()) sentFile.readLines().filter { it.isNotBlank() }.toSet() else emptySet()
    }

    /** Everything, as the text that gets shared: the raw lines, newest last. */
    suspend fun export(): String = withContext(Dispatchers.IO) { if (file.exists()) file.readText() else "" }

    suspend fun clear() = withContext(Dispatchers.IO) {
        file.delete()
        sentFile.delete()
    }

    val path: String get() = file.absolutePath
}
