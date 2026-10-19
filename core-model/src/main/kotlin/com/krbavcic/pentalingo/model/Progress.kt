package com.krbavcic.pentalingo.model

import kotlinx.serialization.Serializable

@Serializable
data class LessonRecord(
    val completions: Int,
    val bestAccuracy: Double,
    val firstCompleted: Long,
    val lastCompleted: Long,
)

/**
 * Everything the app remembers about a learner, persisted whole as one JSON document.
 *
 * Two kinds of record live here and behave differently on purpose. [memories] decay: they are a
 * model of what is in the learner's head, and heads forget. [awards], [lessons] and [textsRead]
 * never go backwards: they record things that were genuinely done. Nothing here counts streaks —
 * a missed day costs a little fading ink, never a reset to zero.
 */
@Serializable
data class Progress(
    val memories: Map<String, Memory> = emptyMap(),
    val lessons: Map<String, LessonRecord> = emptyMap(),
    /** Award id to the epoch millisecond it was earned. */
    val awards: Map<String, Long> = emptyMap(),
    /** Local epoch days (days since 1970-01-01 in the learner's time zone) with any study. */
    val studyDays: Set<Long> = emptySet(),
    /**
     * Which items get mistaken for which, as "a|b" keys with counts (ids sorted). Wrong choices
     * feed back into the distractors: a learner who confuses ḥ and ḫ will see them side by side
     * until they stop.
     */
    val confusions: Map<String, Int> = emptyMap(),
    /** Named tallies award requirements read, such as h-family discriminations. */
    val counters: Map<String, Int> = emptyMap(),
    val textsRead: Set<String> = emptySet(),
    val answered: Int = 0,
    val correct: Int = 0,
    val secondsStudied: Long = 0,
) {
    fun memory(itemId: String): Memory? = memories[itemId]

    fun isIntroduced(itemId: String): Boolean = itemId in memories

    fun isCompleted(lessonId: String): Boolean = lessonId in lessons

    fun counter(key: String): Int = counters[key] ?: 0

    fun confusionCount(a: String, b: String): Int = confusions[confusionKey(a, b)] ?: 0

    companion object {
        fun confusionKey(a: String, b: String): String = if (a < b) "$a|$b" else "$b|$a"
    }
}
