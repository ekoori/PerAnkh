package com.krbavcic.pentalingo.engine

import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.model.SignRole
import com.krbavcic.pentalingo.model.WordItem
import com.krbavcic.pentalingo.script.Mdc
import com.krbavcic.pentalingo.script.Translit

/** What one sign does in one word. */
sealed interface SignReading {
    val code: String

    /** Spells [value]: one of the sign's sounds, or the word it stands for. */
    data class Reads(override val code: String, val value: String) : SignReading

    /** Repeats [value], which a sign beside it spells too: a phonetic complement. */
    data class Repeats(override val code: String, val value: String) : SignReading

    /**
     * Not read aloud: a stroke, or a determinative when [classifies]. A sign that is read in other
     * words classifies this one when the word names it as its determinative: the sun disk is the
     * word rꜥ "sun", but in hrw "day" it only marks a time.
     */
    data class Silent(override val code: String, val classifies: Boolean = false) : SignReading

    /** None of its values fits the word as transliterated (written out of order, or not taught). */
    data class Unexplained(override val code: String) : SignReading
}

/**
 * How each sign of a word is read in that word: which of its values it spells there.
 *
 * A sign can have several values. The throne Q1 is st in st "seat", ws in wsjr "Osiris" and ꜣs
 * in ꜣst "Isis"; the feather H6 is mꜣꜥt, and šw in Šw "Shu"; the village O49 is a determinative,
 * and also the word njwt "town". A breakdown that always gave a sign's first value got those
 * words wrong (reported by learners, Nov 2026), so it asks this instead.
 *
 * Signs are matched in written order against the transliteration. Each may spell letters already
 * spelled, before or after the sign it goes with (a phonetic complement: the š and w around the
 * feather in Šw), but may not leave a gap. Of all the ways to read the word, the one that covers
 * most of it wins, then the one with fewest signs left unexplained, then the one using each
 * sign's first-listed values.
 */
fun readingsInWord(course: Course, translit: String, mdc: String, determinative: String? = null): List<SignReading> {
    val target = Translit.normalize(translit)
    val codes = Mdc.parse(mdc).signs
    val n = codes.size

    // One option for a sign: what it does, how far the word is spelled after it, its cost.
    data class Option(val reading: SignReading, val covered: Int, val unexplained: Int, val rank: Int)
    // Best (covered, -unexplained, -rank) from sign i on, given `covered` letters spelled so far.
    data class Score(val covered: Int, val unexplained: Int, val rank: Int) : Comparable<Score> {
        override fun compareTo(other: Score) = compareValuesBy(this, other, { it.covered }, { -it.unexplained }, { -it.rank })
    }

    fun options(i: Int, covered: Int): List<Option> {
        val code = codes[i]
        val sign = course.signFor(code) ?: return listOf(Option(SignReading.Unexplained(code), covered, 1, 0))
        if (sign.role == SignRole.NUMERAL) return listOf(Option(SignReading.Silent(code), covered, 0, 0))
        val values = (sign.sounds + listOfNotNull(sign.reading)).map(Translit::normalize).filter { it.isNotEmpty() }.distinct()
        val out = mutableListOf<Option>()
        // The word's own determinative may be silent even where the sign is read in other words,
        // and silent it is when nothing more is read by reading it: listed first, it wins a tie
        // with repeating its own sound (the beer jug in ḥnqt marks beer; it does not say ḥnqt twice).
        val classifies = sign.role == SignRole.DETERMINATIVE || code == determinative
        if (classifies) out += Option(SignReading.Silent(code, classifies = true), covered, 0, 0)
        values.forEachIndexed { rank, value ->
            for (start in 0..minOf(covered, target.length - value.length)) {
                if (!target.startsWith(value, start)) continue
                val end = start + value.length
                val reading = if (end <= covered) SignReading.Repeats(code, value) else SignReading.Reads(code, value)
                out += Option(reading, maxOf(covered, end), 0, rank)
            }
        }
        if (!classifies && values.isEmpty()) out += Option(SignReading.Silent(code), covered, 0, 0)
        out += Option(SignReading.Unexplained(code), covered, 1, 0)
        return out
    }

    val memo = HashMap<Long, Pair<Score, Option?>>()
    fun best(i: Int, covered: Int): Pair<Score, Option?> {
        if (i == n) return Score(covered, 0, 0) to null
        val key = i.toLong() * 1_000 + covered
        memo[key]?.let { return it }
        var result: Pair<Score, Option?>? = null
        for (option in options(i, covered)) {
            val (rest, _) = best(i + 1, option.covered)
            val score = Score(rest.covered, rest.unexplained + option.unexplained, rest.rank + option.rank)
            if (result == null || score > result.first) result = score to option
        }
        return result!!.also { memo[key] = it }
    }

    val out = ArrayList<SignReading>(n)
    var covered = 0
    for (i in 0 until n) {
        val option = best(i, covered).second!!
        out += option.reading
        covered = option.covered
    }
    return out
}

fun readingsInWord(course: Course, word: WordItem): List<SignReading> =
    readingsInWord(course, word.translit, word.mdc, word.determinative)

/**
 * The one reading to show for each distinct sign of a word, in order: what it spells if it spells
 * anything (a sign written twice may spell once and repeat once), else what else it does.
 */
fun distinctReadings(readings: List<SignReading>): List<SignReading> =
    readings.groupBy { it.code }.map { (_, all) ->
        all.firstOrNull { it is SignReading.Reads } ?: all.firstOrNull { it is SignReading.Repeats }
            ?: all.firstOrNull { it is SignReading.Silent } ?: all.first()
    }

/** The sound a sign has in a word, as the sign lists it (z stays z), or null if it spells nothing there. */
fun soundIn(course: Course, reading: SignReading): String? {
    val value = when (reading) {
        is SignReading.Reads -> reading.value
        is SignReading.Repeats -> reading.value
        else -> return null
    }
    val sign = course.signFor(reading.code) ?: return value
    return (sign.sounds + listOfNotNull(sign.reading)).firstOrNull { Translit.normalize(it) == value } ?: value
}
