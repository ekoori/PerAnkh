package com.krbavcic.pentalingo.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The uploader against a stand-in for the report server Per Ankh shares with BushLines. */
class ReportUploaderTest {

    private val server = StandIn()

    @After
    fun stop() = server.close()

    private fun uploader() = ReportUploader(server.url("/v1/reports"), "perankh", "0.4.0 (400)", sdk = 34)

    private fun report(id: String) = Report(
        at = 1_793_500_000_000,
        category = "Pronunciation",
        note = "The t is swallowed at the end",
        app = "0.4.0 (400)",
        context = mapOf("screen" to "lesson", "lesson" to "l21 · The great gods", "item" to "s:Q1"),
        id = id,
    )

    @Test
    fun `sends the reports marked as Per Ankh's`() = runBlocking {
        server.reply = """{"accepted": ["a1"], "duplicate": ["a2"], "rejected": []}"""
        val outcome = uploader().upload(listOf(report("a1"), report("a2")))

        assertEquals(ReportUploader.Outcome.Sent(listOf("a1", "a2"), 0), outcome)
        val request = server.received.single()
        assertEquals("POST /v1/reports HTTP/1.1", request.line)
        val body = Json.parseToJsonElement(String(request.body)).jsonObject
        val app = body["app"]!!.jsonObject
        assertEquals("perankh", app["id"]!!.jsonPrimitive.content)
        assertEquals("0.4.0 (400)", app["version"]!!.jsonPrimitive.content)
        assertEquals(34, app["sdk"]!!.jsonPrimitive.int)
        val first = body["reports"]!!.jsonArray[0].jsonObject
        assertEquals("a1", first["id"]!!.jsonPrimitive.content)
        assertEquals("s:Q1", first["context"]!!.jsonObject["item"]!!.jsonPrimitive.content)
    }

    @Test
    fun `rejected reports are counted, not kept`() = runBlocking {
        server.reply = """{"accepted": ["a1"], "duplicate": [], "rejected": [{"id": "a2", "error": "note too long"}]}"""
        assertEquals(ReportUploader.Outcome.Sent(listOf("a1"), 1), uploader().upload(listOf(report("a1"), report("a2"))))
    }

    @Test
    fun `more than fifty go in batches`() = runBlocking {
        server.reply = """{"accepted": [], "duplicate": [], "rejected": []}"""
        uploader().upload((1..51).map { report("r$it") })
        assertEquals(listOf(50, 1), server.received.map {
            Json.parseToJsonElement(String(it.body)).jsonObject["reports"]!!.jsonArray.size
        })
    }

    @Test
    fun `a refusal is a failure to retry later`() = runBlocking {
        server.status = 429
        val outcome = uploader().upload(listOf(report("a1")))
        assertTrue(outcome is ReportUploader.Outcome.Failed)
    }

    @Test
    fun `no server is a failure too`() = runBlocking {
        server.close()
        val outcome = uploader().upload(listOf(report("a1")))
        assertTrue(outcome is ReportUploader.Outcome.Failed)
    }
}
