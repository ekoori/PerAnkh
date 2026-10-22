package com.krbavcic.pentalingo.engine

import com.krbavcic.pentalingo.model.ConceptItem
import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.model.Item
import com.krbavcic.pentalingo.model.Lesson
import com.krbavcic.pentalingo.model.Progress
import com.krbavcic.pentalingo.model.SignItem
import com.krbavcic.pentalingo.model.WordItem
import com.krbavcic.pentalingo.script.Mdc
import com.krbavcic.pentalingo.script.Translit
import kotlin.math.min
import kotlin.random.Random

/** Why an exercise is in the queue; it decides how hard the exercise is. */
enum class Origin { TEACH, NEW_FIRST, NEW_SECOND, REVIEW, REASK, PAIRS, STRETCH, READ }

data class Planned(val itemId: String, val origin: Origin, val textId: String? = null)

/** One graded answer, per item credited. */
data class Attempt(val itemId: String, val tier: Int, val outcome: Outcome, val at: Long)

data class SessionResult(
    val lessonId: String?,
    val startedAt: Long,
    val finishedAt: Long,
    val attempts: List<Attempt>,
    /** Items whose teaching card was shown for the first time. */
    val introduced: List<String>,
    val confusions: List<Pair<String, String>>,
    val tallies: Map<String, Int>,
    /** Graded exercises, and how they went — the learner's view, not the per-item one. */
    val graded: Int,
    val correct: Int,
    val near: Int,
    val completed: Boolean,
    val textRead: String?,
) {
    val accuracy: Double get() = if (graded == 0) 1.0 else (correct + 0.5 * near) / graded
}

/**
 * One lesson or review, from first card to last.
 *
 * The plan is a queue of *intentions* — teach this, review that, re-ask the one just missed —
 * and each becomes a concrete exercise only when it reaches the front. Deciding late is what
 * lets the session adapt: if the last few answers went badly the next exercise is pitched a tier
 * lower, if they all went well it may stretch a tier higher, and distractors always reflect the
 * confusions recorded so far.
 *
 * Mistakes are allowed and expected. A wrong answer is never the end of anything: it brings a
 * fresh exercise on the same item three steps later, at most twice, and after that the item is
 * left for the scheduler to bring back another day. Nobody runs out of hearts.
 */
class Session private constructor(
    val course: Course,
    private val start: Progress,
    val lesson: Lesson?,
    val startedAt: Long,
    private val rng: Random,
    private val clock: () -> Long,
    plan: List<Planned>,
) {
    private val knowledge = Knowledge(course, start, startedAt)
    private val taughtNow = linkedSetOf<String>()
    private val factory = ExerciseFactory(course, knowledge, rng) { taughtNow }
    private val checker = Checker(course)

    private val queue = ArrayDeque(plan)
    private val appearances = mutableMapOf<String, Int>()
    private val reasks = mutableMapOf<String, Int>()
    private val lastTier = mutableMapOf<String, Int>()
    private val lastKindFor = mutableMapOf<String, ExerciseKind>()
    private val firstOutcome = mutableMapOf<String, Outcome>()
    private val recent = ArrayDeque<Outcome>()
    private var lastKind: ExerciseKind? = null

    private val attempts = mutableListOf<Attempt>()
    private val confusions = mutableListOf<Pair<String, String>>()
    private val tallies = mutableMapOf<String, Int>()
    private var graded = 0
    private var correct = 0
    private var near = 0
    private var done = 0
    private var textRead: String? = null

    var current: Exercise? = null
        private set

    /** The verdict on [current], once answered. */
    var verdict: Verdict? = null
        private set

    val isFinished: Boolean get() = current == null && queue.isEmpty()

    /** How far through, 0..1, counting only what is left in the queue right now. */
    val fraction: Float
        get() {
            val left = queue.size + if (current != null && verdict == null) 1 else 0
            val total = done + left
            return if (total == 0) 1f else done.toFloat() / total
        }

    val struggling: Boolean get() = recent.size >= 3 && recent.count { it == Outcome.WRONG } >= 2

    val cruising: Boolean get() = recent.size >= 4 && recent.all { it == Outcome.CORRECT }

    /** Moves to the next exercise. Returns null when the session is over. */
    fun advance(): Exercise? {
        if (current != null) done++
        current = null
        verdict = null
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            val exercise = generate(next) ?: continue
            appearances.merge(next.itemId, 1, Int::plus)
            if (exercise !is Teach && exercise !is ReadText) lastKind = exercise.kind
            current = exercise
            return exercise
        }
        return null
    }

    /**
     * Grades an answer.
     *
     * @param hinted the learner peeked at what a sign means. Help is always allowed; it just
     *   means a right answer counts as a near miss, so a hint cannot inflate the memory record.
     */
    fun submit(answer: Answer, hinted: Boolean = false): Verdict {
        val ex = checkNotNull(current) { "Nothing to answer" }
        check(verdict == null) { "Already answered" }
        val checked = checker.check(ex, answer)
        val v = if (hinted && checked.outcome == Outcome.CORRECT) {
            checked.copy(outcome = Outcome.NEAR, note = listOfNotNull("Right — with a hint.", checked.note).joinToString(" "))
        } else checked
        verdict = v
        if (ex is Teach || ex is ReadText) return v

        val now = clock()
        graded++
        when (v.outcome) {
            Outcome.CORRECT -> correct++
            Outcome.NEAR -> near++
            Outcome.WRONG -> Unit
        }
        recent.addLast(v.outcome)
        while (recent.size > 4) recent.removeFirst()
        lastTier[ex.itemId] = ex.tier
        lastKindFor[ex.itemId] = ex.kind
        firstOutcome.putIfAbsent(ex.itemId, v.outcome)

        when (ex) {
            is Pairs -> {
                val missed = (answer as Answer.Matched).missed
                for (id in ex.credits) {
                    attempts += Attempt(id, ex.tier, if (id in missed) Outcome.NEAR else Outcome.CORRECT, now)
                }
            }
            is MultipleChoice -> if (ex.kind == ExerciseKind.SOUND_OUT) {
                // A word never taught: success credits the signs that were read, failure
                // blames nothing in particular, because there is no telling which sign slipped.
                if (v.outcome == Outcome.CORRECT) ex.credits.forEach { attempts += Attempt(it, 1, Outcome.CORRECT, now) }
            } else {
                ex.credits.forEach { attempts += Attempt(it, ex.tier, v.outcome, now) }
            }
            else -> ex.credits.forEach { attempts += Attempt(it, ex.tier, v.outcome, now) }
        }

        if (v.outcome == Outcome.CORRECT && ex is MultipleChoice) {
            ex.tallies.forEach { tallies.merge(it, 1, Int::plus) }
        }
        v.confusedWith?.let { other ->
            if (other != ex.itemId) confusions += ex.itemId to other
        }
        if (v.outcome == Outcome.WRONG && ex !is Pairs && ex.kind != ExerciseKind.SOUND_OUT) reask(ex.itemId)
        return v
    }

    private fun reask(itemId: String) {
        val asked = reasks[itemId] ?: 0
        if (asked >= MAX_REASKS || (appearances[itemId] ?: 0) >= MAX_APPEARANCES) return
        // A bad day should end sooner, not later: past this length a miss is left for the
        // scheduler to bring back tomorrow rather than added to today.
        if (graded + queue.size >= MAX_LENGTH) return
        reasks[itemId] = asked + 1
        // Three exercises later: long enough that the answer is not still on screen in the
        // learner's head, short enough that the correction is fresh.
        val at = min(REASK_GAP, queue.size)
        val beforeRead = queue.indexOfFirst { it.origin == Origin.READ }.let { if (it < 0) queue.size else it }
        queue.add(min(at, beforeRead), Planned(itemId, Origin.REASK))
    }

    private fun generate(p: Planned): Exercise? {
        val item = course[p.itemId]
        return when (p.origin) {
            Origin.TEACH -> {
                taughtNow += p.itemId
                Teach(p.itemId, firstTime = true)
            }
            Origin.NEW_FIRST -> factory.make(item ?: return null, 1, avoid = lastKind)
            Origin.NEW_SECOND -> {
                item ?: return null
                val tier = when {
                    firstOutcome[p.itemId] == Outcome.WRONG -> 1
                    cruising -> 3
                    struggling -> 1
                    else -> 2
                }
                factory.make(item, tier, avoid = lastKindFor[p.itemId] ?: lastKind)
            }
            Origin.REVIEW -> factory.make(item ?: return null, reviewTier(p.itemId), avoid = lastKind)
            Origin.REASK -> {
                item ?: return null
                val tier = ((lastTier[p.itemId] ?: 1) - 1).coerceAtLeast(1)
                factory.make(item, tier, avoid = lastKindFor[p.itemId])
            }
            Origin.PAIRS -> factory.pairs(pairCandidates())
            Origin.STRETCH -> if (struggling) null else (item as? WordItem)?.let(factory::soundOut)
            Origin.READ -> ReadText(p.itemId, p.textId ?: return null).also { textRead = p.textId }
        }
    }

    private fun reviewTier(itemId: String): Int {
        val m = start.memory(itemId)
        var tier = when {
            m == null -> 1
            m.stability < 1.5 -> 1
            m.stability < 5.0 -> 2
            m.stability < 14.0 -> 3
            else -> 4
        }
        // Never more than one rung above the hardest kind of question passed so far.
        if (m != null) tier = min(tier, m.bestTier + 1)
        if (lesson?.checkpoint == true) tier++
        return when {
            struggling -> tier - 1
            cruising && rng.nextDouble() < 0.35 -> tier + 1
            rng.nextDouble() < 0.15 -> tier + if (rng.nextBoolean()) 1 else -1
            else -> tier
        }
    }

    private fun pairCandidates(): List<Item> {
        val ids = (taughtNow + (lesson?.practice ?: emptyList()) + start.memories.keys).distinct()
        val items = ids.mapNotNull { course[it] }
        val fresh = items.filter { it.id in taughtNow }
        // Prefer what was just taught, topped up with older items so there are enough to match.
        return (fresh + items.filter { it !in fresh }.shuffled(rng)).take(12)
    }

    fun result(): SessionResult = SessionResult(
        lessonId = lesson?.id,
        startedAt = startedAt,
        finishedAt = clock(),
        attempts = attempts.toList(),
        introduced = taughtNow.toList(),
        confusions = confusions.toList(),
        tallies = tallies.toMap(),
        graded = graded,
        correct = correct,
        near = near,
        completed = isFinished,
        textRead = textRead.takeIf { isFinished },
    )

    companion object {
        const val MAX_REASKS = 2
        const val MAX_APPEARANCES = 4
        const val MAX_LENGTH = 26
        const val REASK_GAP = 3

        fun forLesson(
            course: Course,
            progress: Progress,
            lesson: Lesson,
            now: Long,
            seed: Long = now,
            clock: () -> Long = { System.currentTimeMillis() },
        ): Session {
            val rng = Random(seed)
            val plan = Planner(course, progress, now, rng).lesson(lesson)
            return Session(course, progress, lesson, now, rng, clock, plan)
        }

        /** A review of whatever is fading, or the weakest items if nothing is. Null if nothing is learned yet. */
        fun forReview(
            course: Course,
            progress: Progress,
            now: Long,
            seed: Long = now,
            clock: () -> Long = { System.currentTimeMillis() },
        ): Session? {
            val rng = Random(seed)
            val plan = Planner(course, progress, now, rng).review() ?: return null
            return Session(course, progress, null, now, rng, clock, plan)
        }
    }
}

/** Builds the queue of intentions for a session. Pure: same inputs and seed, same plan. */
internal class Planner(
    private val course: Course,
    private val progress: Progress,
    private val now: Long,
    private val rng: Random,
) {
    private val knowledge = Knowledge(course, progress, now)

    companion object {
        const val MAX_NEW = 5
    }

    fun lesson(lesson: Lesson): List<Planned> {
        val remaining = lesson.teach.filter { !progress.isIntroduced(it) && course[it] != null }
        val newItems = batch(remaining)
        val finalBatch = newItems.size == remaining.size
        val replays = lesson.teach.filter { progress.isIntroduced(it) }

        val reviewPool = if (lesson.checkpoint) {
            val unit = course.unitOf(lesson.id)
            unit?.lessons.orEmpty().flatMap { it.teach }.filter(progress::isIntroduced)
                .sortedBy(knowledge::recall)
        } else {
            val practice = (lesson.practice + replays).filter { progress.isIntroduced(it) || it in newItems }
                .filter { it !in newItems }
            val unitIds = course.unitOf(lesson.id)?.lessons.orEmpty().flatMap { it.teach + it.practice }.toSet()
            val due = knowledge.due().map { it.id }.filter { it !in newItems && it !in practice }
                .sortedByDescending { if (it in unitIds) 1 else 0 }
            (practice + due).distinct()
        }
        val learnable = newItems.count { course[it] !is ConceptItem }
        val reviewCount = if (lesson.checkpoint) 14 else (12 - 2 * learnable).coerceIn(3, 8)
        val reviews = ArrayDeque(reviewPool.take(reviewCount))

        val plan = mutableListOf<Planned>()
        val seconds = mutableListOf<String>()
        for (id in newItems) {
            plan += Planned(id, Origin.TEACH)
            plan += Planned(id, Origin.NEW_FIRST)
            if (course[id] !is ConceptItem) seconds += id
            reviews.removeFirstOrNull()?.let { plan += Planned(it, Origin.REVIEW) }
        }
        if (newItems.size >= 3) plan += Planned(newItems.first(), Origin.PAIRS)
        for (id in seconds) {
            plan += Planned(id, Origin.NEW_SECOND)
            reviews.removeFirstOrNull()?.let { plan += Planned(it, Origin.REVIEW) }
        }
        while (reviews.isNotEmpty()) plan += Planned(reviews.removeFirst(), Origin.REVIEW)
        if (newItems.isEmpty() && plan.count { it.origin == Origin.REVIEW } >= 6) {
            plan.add(plan.size / 2, Planned(plan.first().itemId, Origin.PAIRS))
        }

        stretchWord(newItems)?.let { plan += Planned(it.id, Origin.STRETCH) }
        // The text is read once all its lines have been taught, at the end of the last sitting.
        if (finalBatch) lesson.reading?.let { textId -> plan += Planned(textId, Origin.READ, textId) }
        return spaced(plan)
    }

    /**
     * The next few new items of a lesson: at most [MAX_NEW] to learn per sitting, with any
     * concept cards that fall among them. A lesson with more than that unfolds over several
     * sittings; five new things at a time is about what working memory holds.
     */
    private fun batch(remaining: List<String>): List<String> {
        val out = mutableListOf<String>()
        var learnable = 0
        for (id in remaining) {
            val isConcept = course[id] is ConceptItem
            if (!isConcept && learnable == MAX_NEW) break
            out += id
            if (!isConcept) learnable++
        }
        return out
    }

    /**
     * An untaught word made only of signs the learner has met, to be sounded out — offered once
     * per lesson at most, and dropped at run time if the learner is struggling. Words sounded out
     * least often come first: early in the course only a few words qualify, and the same one
     * lesson after lesson stops being a puzzle.
     */
    private fun stretchWord(newItems: List<String>): WordItem? {
        val met = (progress.memories.keys + newItems).toSet()
        val candidates = course.words.filter { w ->
            w.id !in met && course.lessonTeaching(w.id) != null && Translit.letters(w.translit).size >= 2 &&
                readsInOrder(course, w) &&
                Mdc.parse(w.mdc).signs.all { code ->
                    val sign: SignItem? = course.signFor(code)
                    sign != null && sign.id in met
                }
        }
        val fewest = candidates.minOfOrNull { progress.counter(ExerciseFactory.soundedOut(it.id)) } ?: return null
        return candidates.filter { progress.counter(ExerciseFactory.soundedOut(it.id)) == fewest }.random(rng)
    }

    /** Moves each second exercise on a new item at least three steps after its first. */
    private fun spaced(plan: List<Planned>): List<Planned> {
        val out = plan.toMutableList()
        for (i in out.indices) {
            val p = out[i]
            if (p.origin != Origin.NEW_SECOND) continue
            val first = out.indexOfFirst { it.itemId == p.itemId && it.origin == Origin.NEW_FIRST }
            if (first >= 0 && i - first < 3) {
                out.removeAt(i)
                val target = min(first + 3, out.size)
                out.add(target, p)
            }
        }
        return out
    }

    fun review(size: Int = 12): List<Planned>? {
        if (progress.memories.isEmpty()) return null
        val due = knowledge.due().map { it.id }
        val pool = if (due.size >= 4) due else {
            // Nothing much fading: practise the weakest instead, but say so in the UI.
            (due + progress.memories.entries.sortedBy { it.value.stability }.map { it.key }).distinct()
        }
        val chosen = pool.filter { course[it] != null }.take(size)
        if (chosen.isEmpty()) return null
        val plan = chosen.map { Planned(it, Origin.REVIEW) }.toMutableList()
        if (chosen.size >= 6) plan.add(plan.size / 2, Planned(chosen.first(), Origin.PAIRS))
        return plan
    }
}
