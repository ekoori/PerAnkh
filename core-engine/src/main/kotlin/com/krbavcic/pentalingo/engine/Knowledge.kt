package com.krbavcic.pentalingo.engine

import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.model.Item
import com.krbavcic.pentalingo.model.ItemKind
import com.krbavcic.pentalingo.model.Memory
import com.krbavcic.pentalingo.model.MemoryState
import com.krbavcic.pentalingo.model.PhraseItem
import com.krbavcic.pentalingo.model.Progress
import com.krbavcic.pentalingo.model.ReadingText
import com.krbavcic.pentalingo.script.Mdc

/** How much of a text the learner could read right now. */
data class Readability(
    val words: Int,
    val readableWords: Int,
    val signs: Int,
    val knownSigns: Int,
) {
    val fraction: Float get() = if (words == 0) 0f else readableWords.toFloat() / words
    val complete: Boolean get() = words > 0 && readableWords == words
}

/**
 * What the learner knows at a moment, derived from [Progress] and the clock.
 *
 * Nothing here is stored: recall decays continuously, so "known" is a question asked of a
 * particular instant, and the same progress record gives different answers tomorrow.
 */
class Knowledge(val course: Course, val progress: Progress, val now: Long) {

    fun memory(itemId: String): Memory? = progress.memories[itemId]

    fun state(itemId: String): MemoryState? = memory(itemId)?.state(now)

    fun recall(itemId: String): Double = memory(itemId)?.retrievability(now) ?: 0.0

    fun isIntroduced(itemId: String): Boolean = progress.isIntroduced(itemId)

    /** Stable for at least [Memory.KNOWN_DAYS], even if a little faded now. */
    fun isKnown(itemId: String): Boolean = (memory(itemId)?.stability ?: 0.0) >= Memory.KNOWN_DAYS

    fun isMastered(itemId: String): Boolean = (memory(itemId)?.stability ?: 0.0) >= Memory.MASTERED_DAYS

    /** Introduced and still likely to be recalled: what "can read" means for a word in a text. */
    fun canRecall(itemId: String): Boolean = isIntroduced(itemId) && recall(itemId) >= READABLE_RECALL

    fun knownCount(kind: ItemKind): Int = course.items.count { it.kind == kind && isKnown(it.id) }

    fun masteredCount(): Int = progress.memories.values.count { it.stability >= Memory.MASTERED_DAYS }

    /**
     * Items that need a review, weakest first.
     *
     * Due means recall has dropped below [DUE_RECALL] — the point where a review does the most
     * good. Items still fresh are left alone however few others there are; asking about something
     * the learner certainly remembers is the repetition this course is meant not to do.
     */
    fun due(): List<Item> = progress.memories.entries
        .filter { (_, m) -> m.retrievability(now) < DUE_RECALL }
        .sortedBy { (_, m) -> m.retrievability(now) }
        .mapNotNull { (id, _) -> course[id] }

    fun fadingCount(): Int = progress.memories.values.count {
        it.state(now) == MemoryState.FADING
    }

    fun readability(text: ReadingText): Readability {
        var words = 0
        var readable = 0
        val codes = linkedSetOf<String>()
        for (lineId in text.lines) {
            val phrase = course[lineId] as? PhraseItem ?: continue
            val lineLearned = canRecall(phrase.id)
            for (token in phrase.tokens) {
                words++
                // A word counts if the line was learned, or if the word itself is known from
                // elsewhere — reading a new text with old words is exactly the skill being built.
                if (lineLearned || token.wordId?.let(::canRecall) == true) readable++
                codes += Mdc.parse(token.mdc).signs
            }
        }
        val taught = codes.mapNotNull { course.signFor(it) }
        return Readability(
            words = words,
            readableWords = readable,
            signs = taught.size,
            knownSigns = taught.count { canRecall(it.id) },
        )
    }

    companion object {
        /** Reviews are scheduled for when recall falls to this. */
        const val DUE_RECALL = 0.85

        const val READABLE_RECALL = 0.7
    }
}
