package com.krbavcic.pentalingo.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** A published release: its version, and its page, where the APK and the release notes are. */
data class Release(val version: String, val url: String)

/**
 * Finds the newest release, for the About page.
 *
 * Asks each source in turn, GitHub's release API first and then the Per Ankh server, which answers
 * in GitHub's own shape from its copy of the same release, for networks where GitHub cannot be
 * reached. The question carries nothing but the app's name and version, as its User-Agent. An
 * answer counts only if it is a full release whose page is under [pages], so neither source can
 * send anyone elsewhere. See PRIVACY.md, "Checking for a new version".
 *
 * @param fetch reads a URL, giving its body or null; tests stand in for the network with it.
 */
class ReleaseCheck(
    private val sources: List<String>,
    private val pages: String,
    private val fetch: (String) -> String?,
) {
    constructor(sources: List<String>, pages: String, userAgent: String) :
        this(sources, pages, { url -> get(url, userAgent) })

    /** The newest release, or null if no source could say. */
    suspend fun latest(): Release? = withContext(Dispatchers.IO) {
        sources.firstNotNullOfOrNull { url -> fetch(url)?.let { parseRelease(it, pages) } }
    }
}

private val VERSION = Regex("""\d+\.\d+\.\d+(-[0-9A-Za-z.]+)?""")

/** The release in a GitHub API answer, or null if it is not a full release published under [pages]. */
internal fun parseRelease(body: String, pages: String): Release? = try {
    val json = Json.parseToJsonElement(body).jsonObject
    fun text(key: String) = json[key]?.jsonPrimitive?.contentOrNull
    fun flag(key: String) = json[key]?.jsonPrimitive?.booleanOrNull == true
    val version = text("tag_name")?.removePrefix("v")
    val url = text("html_url")
    when {
        version == null || url == null || flag("draft") || flag("prerelease") -> null
        !VERSION.matches(version) || !url.startsWith("$pages/") -> null
        else -> Release(version, url)
    }
} catch (e: IllegalArgumentException) {
    // Not JSON, or not shaped like a release.
    null
}

/**
 * Orders versions as semantic versioning does: 0.9.0 < 0.10.0, and pre-releases before their
 * release, 0.3.0-beta.2 < 0.3.0-beta.10 < 0.3.0.
 */
fun compareVersions(a: String, b: String): Int {
    val (coreA, preA) = a.split('-', limit = 2).let { it[0] to it.getOrNull(1) }
    val (coreB, preB) = b.split('-', limit = 2).let { it[0] to it.getOrNull(1) }
    val core = compareIdentifiers(coreA.split('.'), coreB.split('.'))
    return when {
        core != 0 -> core
        preA == preB -> 0
        preA == null -> 1
        preB == null -> -1
        else -> compareIdentifiers(preA.split('.'), preB.split('.'))
    }
}

/** Dot-separated identifiers: numbers by value and before words, words alphabetically. */
private fun compareIdentifiers(a: List<String>, b: List<String>): Int {
    for (i in 0 until minOf(a.size, b.size)) {
        val x = a[i].toIntOrNull()
        val y = b[i].toIntOrNull()
        val order = when {
            x != null && y != null -> x.compareTo(y)
            x != null -> -1
            y != null -> 1
            else -> a[i].compareTo(b[i])
        }
        if (order != 0) return order
    }
    return a.size.compareTo(b.size)
}

/** A URL's body if it answers 200 within a few seconds, else null. */
private fun get(url: String, userAgent: String): String? {
    val connection = try {
        URL(url).openConnection() as HttpURLConnection
    } catch (e: IOException) {
        return null
    }
    return try {
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.useCaches = false
        // GitHub's API refuses requests that do not name their client.
        connection.setRequestProperty("User-Agent", userAgent)
        connection.setRequestProperty("Accept", "application/vnd.github+json, application/json")
        if (connection.responseCode == 200) connection.inputStream.bufferedReader().use { it.readText() } else null
    } catch (e: IOException) {
        null
    } finally {
        connection.disconnect()
    }
}
