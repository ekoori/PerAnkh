package com.krbavcic.pentalingo.data

import android.os.Build
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Sends tester reports to the Per Ankh server, only when the tester asks to.
 *
 * What goes over the wire is exactly the stored reports plus the app version and the Android API
 * level: nothing that identifies the person or the device. The connection is HTTPS only (the
 * app's network security config forbids anything else). See PRIVACY.md and server/README.md.
 */
class ReportUploader(private val url: String, private val appVersion: String) {

    sealed interface Outcome {
        /** [stored] are the ids the server now holds, including any it already had. */
        data class Sent(val stored: List<String>, val rejected: Int) : Outcome

        data class Failed(val reason: String) : Outcome
    }

    suspend fun upload(reports: List<Report>): Outcome = withContext(Dispatchers.IO) {
        val stored = mutableListOf<String>()
        var rejected = 0
        for (batch in reports.chunked(BATCH)) {
            when (val outcome = send(batch)) {
                is Outcome.Sent -> {
                    stored += outcome.stored
                    rejected += outcome.rejected
                }
                // Keep what earlier batches achieved; the rest is retried next time.
                is Outcome.Failed -> return@withContext if (stored.isEmpty()) outcome else Outcome.Sent(stored, rejected)
            }
        }
        Outcome.Sent(stored, rejected)
    }

    private fun send(batch: List<Report>): Outcome {
        val body = buildJsonObject {
            put("app", buildJsonObject {
                put("version", appVersion)
                put("sdk", Build.VERSION.SDK_INT)
            })
            put("reports", buildJsonArray {
                for (r in batch) add(buildJsonObject {
                    put("id", r.id)
                    put("at", r.at)
                    put("category", r.category)
                    put("note", r.note)
                    put("app", r.app)
                    put("context", JsonObject(r.context.mapValues { JsonPrimitive(it.value) }))
                })
            })
        }.toString().toByteArray(Charsets.UTF_8)

        val connection = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            return Outcome.Failed("Could not reach the report server.")
        }
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.doOutput = true
            connection.useCaches = false
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setFixedLengthStreamingMode(body.size)
            connection.outputStream.use { it.write(body) }
            val code = connection.responseCode
            when {
                code == 200 -> {
                    val reply = connection.inputStream.bufferedReader().use { it.readText() }
                    val result = Json.parseToJsonElement(reply).jsonObject
                    fun ids(key: String) = (result[key] as? JsonArray)?.map { it.jsonPrimitive.content }.orEmpty()
                    Outcome.Sent(ids("accepted") + ids("duplicate"), result["rejected"]?.jsonArray?.size ?: 0)
                }
                code == 429 -> Outcome.Failed("Too many sends in a row. Try again in a minute.")
                code in 500..599 -> Outcome.Failed("The report server is having trouble ($code). Try again later.")
                else -> Outcome.Failed("The report server refused the reports ($code).")
            }
        } catch (e: IOException) {
            Outcome.Failed("No connection to the report server. Check the network and try again.")
        } catch (e: IllegalArgumentException) {
            Outcome.Failed("The report server sent an answer the app did not understand.")
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        /** The server takes at most 50 reports per request. */
        const val BATCH = 50
    }
}
