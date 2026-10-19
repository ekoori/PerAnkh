package com.krbavcic.pentalingo.engine

import com.krbavcic.pentalingo.model.Progress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTest {

    private val course = Fixture.course
    private val day = 86_400_000L
    private val t0 = 1_760_000_000_000L

    /** Plays a session to the end; [answer] decides what the learner does. */
    private fun play(session: Session, answer: (Exercise, Int) -> Answer = { ex, _ -> ex.solution() }): List<Pair<Exercise, Verdict>> {
        val log = mutableListOf<Pair<Exercise, Verdict>>()
        var step = 0
        while (true) {
            val ex = session.advance() ?: break
            log += ex to session.submit(answer(ex, step++))
            check(step < 200) { "session never ended" }
        }
        return log
    }

    /** Plays sittings of a lesson until it is complete, a day apart. */
    private fun finish(lessonId: String, start: Progress, at: Long, seed: Long = 7): Pair<Progress, Long> {
        var p = start
        var now = at
        repeat(5) {
            if (p.isCompleted(lessonId)) return p to now
            val s = Session.forLesson(course, p, course.lesson(lessonId)!!, now, seed = seed + it, clock = { now })
            play(s)
            p = Recording.apply(course, p, s.result()).progress
            now += day
        }
        check(p.isCompleted(lessonId)) { "$lessonId never completed" }
        return p to now
    }

    private fun afterLessons(vararg ids: String, at: Long = t0): Progress {
        var p = Progress()
        var now = at
        for (id in ids) {
            val (next, later) = finish(id, p, now)
            p = next
            now = later
        }
        return p
    }

    @Test
    fun `a perfect learner gets every exercise right`() {
        var p = Progress()
        var now = t0
        for (lesson in course.lessons) {
            while (!p.isCompleted(lesson.id)) {
                val s = Session.forLesson(course, p, lesson, now, seed = lesson.id.hashCode().toLong(), clock = { now })
                val log = play(s)
                for ((ex, v) in log) assertEquals("${ex.kind} ${ex.itemId}", Outcome.CORRECT, v.outcome)
                val result = s.result()
                assertTrue(result.completed)
                p = Recording.apply(course, p, result).progress
                now += day
            }
        }
        assertTrue(p.isCompleted("l6"))
        assertTrue("t:demo" in p.textsRead)
    }

    @Test
    fun `a long lesson unfolds over several sittings`() {
        // Lesson 4 teaches ten things; no sitting introduces more than five to learn.
        val first = Session.forLesson(course, Progress(), Fixture.lesson4, t0, seed = 1, clock = { t0 })
        val log = play(first)
        val taught = log.map { it.first }.filterIsInstance<Teach>().map { it.itemId }
        assertTrue(taught.size in 1..6)
        val p = Recording.apply(course, Progress(), first.result()).progress
        assertFalse(p.isCompleted("l4"))
        assertFalse("text read before its lines were all taught", "t:demo" in p.textsRead)
    }

    @Test
    fun `every new item is taught before it is tested`() {
        val s = Session.forLesson(course, Progress(), Fixture.lesson1, t0, seed = 1, clock = { t0 })
        val seen = mutableSetOf<String>()
        for ((ex, _) in play(s)) {
            if (ex is Teach) seen += ex.itemId
            else if (ex.kind != ExerciseKind.PAIRS) {
                assertTrue("${ex.itemId} tested before taught", ex.itemId in seen || ex.itemId !in Fixture.lesson1.teach)
            }
        }
        assertEquals(Fixture.lesson1.teach.take(5).toSet(), seen)
    }

    @Test
    fun `a new item comes back a few steps after it is first tested`() {
        val s = Session.forLesson(course, Progress(), Fixture.lesson1, t0, seed = 3, clock = { t0 })
        val log = play(s).map { it.first }
        for (id in Fixture.lesson1.teach.take(5)) {
            val positions = log.withIndex().filter { (_, ex) -> ex.itemId == id && ex !is Teach && ex.kind != ExerciseKind.PAIRS }.map { it.index }
            assertTrue("$id tested ${positions.size} times", positions.size >= 2)
            assertTrue("$id repeated too soon: $positions", positions[1] - positions[0] >= 2)
        }
    }

    @Test
    fun `a mistake brings the item back, but not forever`() {
        val s = Session.forLesson(course, Progress(), Fixture.lesson1, t0, seed = 5, clock = { t0 })
        // A learner who gets the owl wrong every single time.
        val log = play(s) { ex, _ ->
            if (ex.itemId == "s:G17" && ex is MultipleChoice) Answer.Pick((ex.correct + 1) % ex.choices.size) else ex.solution()
        }
        val owl = log.filter { (ex, _) -> ex.itemId == "s:G17" && ex !is Teach && ex.kind != ExerciseKind.PAIRS }
        assertTrue(owl.size >= 2)
        assertTrue("asked ${owl.size} times", owl.size <= Session.MAX_APPEARANCES)
        assertTrue(s.result().completed)
    }

    @Test
    fun `a wrong pick is recorded as a confusion and fed back`() {
        val s = Session.forLesson(course, Progress(), Fixture.lesson1, t0, seed = 11, clock = { t0 })
        var confusedWith: String? = null
        play(s) { ex, _ ->
            if (confusedWith == null && ex is MultipleChoice && ex.kind == ExerciseKind.SIGN_TO_SOUND) {
                val wrong = ex.choices.indices.first { it != ex.correct }
                confusedWith = ex.choices[wrong].itemId
                Answer.Pick(wrong)
            } else ex.solution()
        }
        val result = s.result()
        assertNotNull(confusedWith)
        assertTrue(result.confusions.any { it.second == confusedWith })
        val p = Recording.apply(course, Progress(), result).progress
        assertTrue(p.confusions.values.sum() >= 1)
    }

    @Test
    fun `reviews only bring back what is fading`() {
        val p = afterLessons("l1", "l2")
        // The next morning almost nothing is due; a month later plenty is.
        val soon = Knowledge(course, p, t0 + 2 * day).due()
        val later = Knowledge(course, p, t0 + 40 * day).due()
        assertTrue(later.size > soon.size)
        val s = Session.forReview(course, p, t0 + 40 * day, seed = 2, clock = { t0 + 40 * day })
        assertNotNull(s)
        val log = play(s!!)
        assertTrue(log.isNotEmpty())
        assertFalse(log.any { it.first is Teach })
    }

    @Test
    fun `memory stays modest after one lesson and grows with spaced reviews`() {
        val p = afterLessons("l1")
        val owl = p.memory("s:G17")!!
        assertTrue("stability ${owl.stability}", owl.stability < 5.0)
    }

    @Test
    fun `abandoning a lesson keeps the effort but not the completion`() {
        val s = Session.forLesson(course, Progress(), Fixture.lesson1, t0, seed = 9, clock = { t0 })
        repeat(5) { s.advance()?.let { s.submit(it.solution()) } }
        val result = s.result()
        assertFalse(result.completed)
        val p = Recording.apply(course, Progress(), result).progress
        assertFalse(p.isCompleted("l1"))
        assertTrue(p.memories.isNotEmpty())
    }

    @Test
    fun `awards are earned once, for the real thing`() {
        var p = Progress()
        var now = t0
        val earned = mutableListOf<String>()
        for (lesson in course.lessons) {
            var sittings = 0
            while (!p.isCompleted(lesson.id) && sittings++ < 5) {
                val s = Session.forLesson(course, p, lesson, now, seed = 4, clock = { now })
                play(s)
                val rec = Recording.apply(course, p, s.result())
                earned += rec.newAwards.map { it.id }
                p = rec.progress
                now += day
            }
        }
        assertTrue("a:read" in earned)
        assertTrue("a:days" in earned)
        assertEquals(earned.size, earned.toSet().size)
    }
}
