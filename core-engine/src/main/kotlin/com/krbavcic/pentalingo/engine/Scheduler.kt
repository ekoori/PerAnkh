package com.krbavcic.pentalingo.engine

import com.krbavcic.pentalingo.model.Memory
import com.krbavcic.pentalingo.model.MemoryState
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** How an answer went, in FSRS's four grades. */
enum class Grade(val value: Int) { AGAIN(1), HARD(2), GOOD(3), EASY(4) }

/**
 * FSRS-4.5's memory updates, with the default published weights, plus two adaptations for a
 * course rather than a flashcard deck.
 *
 * **Exercises are not equally hard.** Picking a sign's sound from four options right after
 * seeing it proves far less than spelling a word from memory, so the stability a correct answer
 * earns is scaled by the exercise's tier ([tierWeight]). Without this, an app that asked only
 * easy questions would believe everything was mastered.
 *
 * **Same-day repetition is worth little.** Answers within [SHORT_TERM_DAYS] of the last one use
 * FSRS-5's short-term rule and cannot lift stability past [SHORT_TERM_CEILING]. Drilling an item
 * ten times in a session should not convince the scheduler it will be remembered for a month —
 * that is the "repeat, but not too much" brief, enforced by the maths rather than by willpower.
 */
object Scheduler {

    private val w = doubleArrayOf(
        0.4872, 1.4003, 3.7145, 13.8206, 5.1618, 1.2298, 0.8975, 0.031, 1.6474,
        0.1367, 1.0461, 2.1072, 0.0793, 0.3246, 1.587, 0.2272, 2.8755,
    )

    /** FSRS-5's short-term weights. */
    private const val W17 = 0.5
    private const val W18 = 0.6

    const val SHORT_TERM_DAYS = 0.5
    const val SHORT_TERM_CEILING = 4.0

    fun tierWeight(tier: Int): Double = when (tier) {
        0, 1 -> 0.55
        2 -> 0.8
        3 -> 1.0
        else -> 1.15
    }

    private fun initialDifficulty(grade: Grade): Double =
        (w[4] - (grade.value - 3) * w[5]).coerceIn(1.0, 10.0)

    /** The first graded answer for an item that has just been taught. */
    fun first(grade: Grade, tier: Int, now: Long): Memory {
        val stability = when (grade) {
            Grade.AGAIN -> 0.3
            Grade.HARD -> 0.6
            Grade.GOOD -> goodStart(tier)
            Grade.EASY -> goodStart(tier) * 1.6
        }
        return Memory(
            stability = stability,
            difficulty = initialDifficulty(grade),
            lastReview = now,
            firstSeen = now,
            reps = 1,
            lapses = if (grade == Grade.AGAIN) 1 else 0,
            bestTier = if (grade != Grade.AGAIN) tier else 0,
        )
    }

    private fun goodStart(tier: Int): Double = when (tier) {
        0, 1 -> 0.9
        2 -> 1.4
        3 -> 2.4
        else -> 3.2
    }

    fun review(memory: Memory, grade: Grade, tier: Int, now: Long): Memory {
        val elapsed = memory.elapsedDays(now)
        val d = memory.difficulty
        val s = memory.stability
        val weight = tierWeight(tier)

        val stability = if (elapsed < SHORT_TERM_DAYS) {
            val factor = exp(W17 * (grade.value - 3 + W18))
            val scaled = if (factor > 1.0) 1.0 + (factor - 1.0) * weight else factor
            val next = s * scaled
            if (grade == Grade.AGAIN) next else min(next, max(s, SHORT_TERM_CEILING))
        } else {
            val r = Memory.retrievability(elapsed, s)
            if (grade == Grade.AGAIN) {
                min(s, w[11] * d.pow(-w[12]) * ((s + 1).pow(w[13]) - 1) * exp(w[14] * (1 - r)))
            } else {
                val hard = if (grade == Grade.HARD) w[15] else 1.0
                val easy = if (grade == Grade.EASY) w[16] else 1.0
                s * (1 + exp(w[8]) * (11 - d) * s.pow(-w[9]) * (exp(w[10] * (1 - r)) - 1) * hard * easy * weight)
            }
        }.coerceIn(0.1, 36_500.0)

        val shifted = d - w[6] * (grade.value - 3)
        val difficulty = (w[7] * initialDifficulty(Grade.GOOD) + (1 - w[7]) * shifted).coerceIn(1.0, 10.0)

        val wasKnown = memory.state(now) != MemoryState.LEARNING
        return memory.copy(
            stability = stability,
            difficulty = difficulty,
            lastReview = now,
            reps = memory.reps + 1,
            lapses = if (grade == Grade.AGAIN) memory.lapses + 1 else memory.lapses,
            bestTier = if (grade != Grade.AGAIN) max(memory.bestTier, tier) else memory.bestTier,
            everLapsed = memory.everLapsed || (grade == Grade.AGAIN && wasKnown),
        )
    }
}
