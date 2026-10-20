package com.krbavcic.pentalingo.data

import android.content.Context
import java.io.File
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
 */
@Serializable
data class Report(
    val at: Long,
    val category: String,
    val note: String,
    val app: String,
    val context: Map<String, String>,
)

/**
 * Tester reports, one JSON object per line.
 *
 * Kept in the app's external files directory — /sdcard/Android/data/com.krbavcic.pentalingo/
 * files/reports/reports.jsonl — so they can be pulled with adb from a release build, which
 * `run-as` cannot reach, as well as shared from the app.
 */
class ReportStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val file: File
        get() {
            val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "reports")
            dir.mkdirs()
            return File(dir, "reports.jsonl")
        }

    suspend fun add(report: Report) = withContext(Dispatchers.IO) {
        file.appendText(json.encodeToString(Report.serializer(), report) + "\n")
    }

    suspend fun all(): List<Report> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        file.readLines().filter { it.isNotBlank() }.mapNotNull { line ->
            try {
                json.decodeFromString(Report.serializer(), line)
            } catch (e: kotlinx.serialization.SerializationException) {
                null
            }
        }
    }

    /** Everything, as the text that gets shared: the raw lines, newest last. */
    suspend fun export(): String = withContext(Dispatchers.IO) { if (file.exists()) file.readText() else "" }

    suspend fun clear() = withContext(Dispatchers.IO) { file.delete() }

    val path: String get() = file.absolutePath
}
