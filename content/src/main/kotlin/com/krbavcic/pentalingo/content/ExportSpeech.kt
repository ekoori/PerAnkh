package com.krbavcic.pentalingo.content

import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.model.PhraseItem
import com.krbavcic.pentalingo.model.SignItem
import com.krbavcic.pentalingo.model.WordItem
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/** Everything the course says aloud: item id to the text the voice reads. */
fun spokenLines(course: Course): List<Pair<String, String>> = course.items.mapNotNull { item ->
    when (item) {
        is SignItem -> item.say?.let { item.id to it }
        is WordItem -> item.id to item.say
        is PhraseItem -> item.id to item.say
        else -> null
    }
}

/**
 * Writes [spokenLines] as JSON for `tools/voice.py`, which turns each line into an ElevenLabs
 * clip. Run with `gradlew :content:run`.
 */
fun main(args: Array<String>) {
    val out = File(args.firstOrNull() ?: "tools/out/speech.json")
    out.parentFile?.mkdirs()
    val json = buildJsonArray {
        for ((id, text) in spokenLines(MIDDLE_EGYPTIAN)) {
            add(buildJsonObject {
                put("id", JsonPrimitive(id))
                put("text", JsonPrimitive(text))
            })
        }
    }
    out.writeText(Json { prettyPrint = true }.encodeToString(kotlinx.serialization.json.JsonArray.serializer(), json))
    println("wrote ${json.size} lines to $out")
}
