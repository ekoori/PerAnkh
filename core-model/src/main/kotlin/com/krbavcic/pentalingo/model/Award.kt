package com.krbavcic.pentalingo.model

/**
 * What it takes to earn an award, as data the engine evaluates.
 *
 * Every requirement names a real ability or a real habit — reading a text, telling the four h's
 * apart, coming back to a word you once forgot — never a count of taps. An award that can be
 * earned by grinding says nothing; these are meant to be claims a learner could defend.
 */
sealed interface Requirement {
    /** At least [count] of these items known (stable for [Memory.KNOWN_DAYS] or more). */
    data class Know(val itemIds: List<String>, val count: Int = itemIds.size) : Requirement

    /** At least [count] items of a kind known, anywhere in the course. */
    data class KnowKind(val kind: ItemKind, val count: Int) : Requirement

    /** At least [count] items mastered. */
    data class Master(val count: Int) : Requirement

    /** A named tally in [Progress.counters] reaches [count]. */
    data class Tally(val key: String, val count: Int) : Requirement

    data class ReadText(val textId: String) : Requirement

    data class StudyDays(val count: Int) : Requirement

    /** Items once forgotten and later relearned to at least [Memory.KNOWN_DAYS] * 3. */
    data class Recovered(val count: Int) : Requirement

    /** No fading items left, with at least [minKnown] known: a review queue honestly cleared. */
    data class ClearedReview(val minKnown: Int) : Requirement
}

/**
 * An amulet earned for an ability.
 *
 * @property sign the Gardiner code of the amulet's hieroglyph.
 * @property egyptian its name in transliteration.
 * @property howTo the requirement in plain words, shown before it is earned.
 * @property lore what the amulet meant to the people who wore it — the award teaches too.
 */
data class Award(
    val id: String,
    val title: String,
    val egyptian: String,
    val sign: String,
    val howTo: String,
    val lore: String,
    val requirement: Requirement,
)
