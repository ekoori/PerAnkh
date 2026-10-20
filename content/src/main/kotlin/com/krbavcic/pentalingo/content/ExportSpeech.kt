package com.krbavcic.pentalingo.content

import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.model.PhraseItem
import com.krbavcic.pentalingo.model.SignItem
import com.krbavcic.pentalingo.model.SignRole
import com.krbavcic.pentalingo.model.WordItem
import com.krbavcic.pentalingo.script.Pronounce
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * One recorded clip: what it says, and how to check that it said it.
 *
 * @property candidates Arabic-script inputs for the speech model, best first.
 * @property expect for each consonant that must be heard, the Arabic letters a transcriber may
 *   write for it. Arabic has letters for ḥ, ꜥ, ḫ, q and the rest, so an Arabic transcript is an
 *   objective check that the voice really produced them.
 * @property critical which of [expect] are the sounds English cannot make — the ones this whole
 *   pipeline exists for.
 */
data class Clip(
    val id: String,
    val spoken: String,
    val latin: String,
    val ipa: String,
    val candidates: List<String>,
    val expect: List<List<String>>,
    val critical: List<Boolean>,
    val hasP: Boolean,
)

/** The Arabic letters a transcriber might write for a sound, and whether the sound is one of the hard ones. */
private val HEARD: Map<String, Pair<List<String>, Boolean>> = mapOf(
    "ħ" to (listOf("ح") to true), "ʕ" to (listOf("ع") to true), "χ" to (listOf("خ") to true),
    "ç" to (listOf("خ", "ش", "ح") to true), "q" to (listOf("ق") to true), "ʃ" to (listOf("ش") to true),
    "dʒ" to (listOf("ج") to true), "tʃ" to (listOf("ش", "ت", "ج", "چ") to false),
    "h" to (listOf("ه") to false), "k" to (listOf("ك") to false), "g" to (listOf("ج", "غ", "ك", "ق", "گ") to false),
    "t" to (listOf("ت", "ط") to false), "d" to (listOf("د", "ض") to false), "s" to (listOf("س", "ص", "ز") to false),
    "b" to (listOf("ب") to false), "p" to (listOf("ب", "ف", "پ") to false), "f" to (listOf("ف") to false),
    "m" to (listOf("م") to false), "n" to (listOf("ن") to false), "r" to (listOf("ر") to false),
    "w" to (listOf("و") to false), "j" to (listOf("ي") to false),
)

private fun checks(phones: List<String>): Pair<List<List<String>>, List<Boolean>> {
    val heard = phones.mapNotNull { HEARD[it] }
    return heard.map { it.first } to heard.map { it.second }
}

private fun consonants(translit: String): List<String> =
    Pronounce.phones(translit).flatten().filter { !it.isVowel && it.ipa != "ʔ" }.map { it.ipa }

private fun wordClip(id: String, translit: String): Clip {
    val (expect, critical) = checks(consonants(translit))
    return Clip(
        id = id, spoken = translit, latin = Pronounce.latin(translit), ipa = Pronounce.ipa(translit),
        candidates = listOf(Pronounce.arabic(translit), Pronounce.arabicColoured(translit)).distinct(),
        expect = expect, critical = critical,
        hasP = 'p' in translit,
    )
}

private fun letterClip(id: String, letter: String): Clip {
    val phone = when (letter) {
        "ꜣ" -> emptyList()
        "j" -> emptyList()
        "y" -> listOf("j")
        "ꜥ" -> listOf("ʕ")
        "w" -> listOf("w")
        else -> consonants(letter)
    }
    val (expect, critical) = checks(phone)
    return Clip(
        id = id, spoken = letter, latin = Pronounce.letterLatin(letter), ipa = Pronounce.letterIpa(letter),
        candidates = Pronounce.letterArabic(letter), expect = expect, critical = critical, hasP = letter == "p",
    )
}

/** Every clip the course needs. */
fun clips(course: Course): List<Clip> = course.items.mapNotNull { item ->
    when (item) {
        is SignItem -> item.spoken?.let { spoken ->
            if (item.role == SignRole.UNILITERAL) letterClip(item.id, spoken) else wordClip(item.id, spoken)
        }
        is WordItem -> wordClip(item.id, item.translit)
        is PhraseItem -> wordClip(item.id, item.translit)
        else -> null
    }
}

/**
 * Writes [clips] as JSON for `tools/voice.py`, which records them with ElevenLabs, and for
 * `tools/tla.py`, which counts the words in real texts (it uses each word's part of speech to
 * tell homographs apart). Run with `gradlew :content:run`.
 */
fun main(args: Array<String>) {
    val out = File(args.firstOrNull() ?: "tools/out/speech.json")
    out.parentFile?.mkdirs()
    val json = buildJsonArray {
        for (clip in clips(MIDDLE_EGYPTIAN)) {
            add(buildJsonObject {
                (MIDDLE_EGYPTIAN[clip.id] as? WordItem)?.let { word ->
                    put("pos", JsonPrimitive(word.pos.name))
                    put("gloss", JsonPrimitive(word.gloss))
                }
                put("id", JsonPrimitive(clip.id))
                put("spoken", JsonPrimitive(clip.spoken))
                put("latin", JsonPrimitive(clip.latin))
                put("ipa", JsonPrimitive(clip.ipa))
                put("candidates", JsonArray(clip.candidates.map(::JsonPrimitive)))
                put("expect", JsonArray(clip.expect.map { slot -> JsonArray(slot.map(::JsonPrimitive)) }))
                put("critical", JsonArray(clip.critical.map(::JsonPrimitive)))
                put("hasP", JsonPrimitive(clip.hasP))
            })
        }
    }
    out.writeText(Json { prettyPrint = true }.encodeToString(JsonArray.serializer(), json))
    println("wrote ${json.size} clips to $out")
}
