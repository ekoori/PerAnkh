package com.krbavcic.pentalingo.model

import kotlin.math.pow
import kotlinx.serialization.Serializable

/**
 * How well one item is remembered, in the terms of FSRS (the Free Spaced Repetition Scheduler).
 *
 * Two numbers describe a memory: [stability], the number of days until the chance of recalling
 * it falls to 90%, and [difficulty], how stubborn this particular item is for this particular
 * learner, from 1 (easy) to 10. Recall probability decays along a power curve between reviews,
 * which is what [retrievability] computes; the update rules live in the engine's scheduler.
 *
 * The point of modelling memory at all is the user's brief: repeat, but not too much. An item
 * is asked again when it is about to be forgotten, not on a fixed rota, so strong items get out
 * of the way and weak ones come back while they can still be saved.
 */
@Serializable
data class Memory(
    val stability: Double,
    val difficulty: Double,
    /** Epoch milliseconds of the last graded answer. */
    val lastReview: Long,
    val firstSeen: Long,
    val reps: Int = 1,
    val lapses: Int = 0,
    /** The hardest exercise tier answered correctly, so difficulty can climb with skill. */
    val bestTier: Int = 0,
    /** Set when this item was ever forgotten after being known; feeds the scarab award. */
    val everLapsed: Boolean = false,
) {
    fun elapsedDays(now: Long): Double = ((now - lastReview).coerceAtLeast(0L)) / DAY_MS

    /** Probability of recall right now, 0..1. */
    fun retrievability(now: Long): Double = retrievability(elapsedDays(now), stability)

    fun state(now: Long): MemoryState = when {
        stability >= MASTERED_DAYS -> MemoryState.MASTERED
        stability >= KNOWN_DAYS -> MemoryState.KNOWN
        else -> MemoryState.LEARNING
    }.let { if (it != MemoryState.LEARNING && retrievability(now) < FADING_BELOW) MemoryState.FADING else it }

    companion object {
        const val DAY_MS = 86_400_000.0

        /** FSRS's forgetting curve: R(t) = (1 + F·t/S)^D, tuned so R(S) = 0.9. */
        const val DECAY = -0.5
        const val FACTOR = 19.0 / 81.0

        /** Stable for two days: survived a review on another day, or was strongly learned. */
        const val KNOWN_DAYS = 2.0

        /** Three weeks between reviews: as good as permanent at this stage of learning. */
        const val MASTERED_DAYS = 21.0

        /** Below this, a known item is shown as fading ink and is due for review. */
        const val FADING_BELOW = 0.8

        fun retrievability(elapsedDays: Double, stability: Double): Double =
            (1.0 + FACTOR * elapsedDays / stability.coerceAtLeast(0.01)).pow(DECAY)
    }
}

enum class MemoryState {
    LEARNING,
    KNOWN,
    MASTERED,

    /** Known once, but the ink is fading: recall has dropped below [Memory.FADING_BELOW]. */
    FADING,
}
