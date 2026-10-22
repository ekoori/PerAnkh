package com.krbavcic.pentalingo.content

import com.krbavcic.pentalingo.engine.Answer
import com.krbavcic.pentalingo.engine.ExerciseKind
import com.krbavcic.pentalingo.engine.MultipleChoice
import com.krbavcic.pentalingo.engine.Outcome
import com.krbavcic.pentalingo.engine.Recording
import com.krbavcic.pentalingo.engine.Session
import com.krbavcic.pentalingo.engine.Teach
import com.krbavcic.pentalingo.engine.solution
import com.krbavcic.pentalingo.model.ConceptItem
import com.krbavcic.pentalingo.model.LessonRecord
import com.krbavcic.pentalingo.model.PhraseItem
import com.krbavcic.pentalingo.model.Progress
import com.krbavcic.pentalingo.model.SignItem
import com.krbavcic.pentalingo.model.SignRole
import com.krbavcic.pentalingo.model.WordItem
import com.krbavcic.pentalingo.script.Mdc
import com.krbavcic.pentalingo.script.Translit
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CourseTest {

    private val course = MIDDLE_EGYPTIAN
    private val day = 86_400_000L
    private val t0 = 1_760_000_000_000L

    private fun allSpellings(): List<Pair<String, String>> = buildList {
        for (item in course.items) when (item) {
            is SignItem -> add(item.id to item.code)
            is WordItem -> { add(item.id to item.mdc); item.variants.forEach { add(item.id to it) } }
            is PhraseItem -> item.tokens.forEach { add(item.id to it.mdc) }
            is ConceptItem -> item.mdc?.let { add(item.id to it) }
        }
    }

    @Test
    fun `ids are unique`() {
        val dupes = course.items.groupBy { it.id }.filter { it.value.size > 1 }.keys
        assertTrue("duplicate ids $dupes", dupes.isEmpty())
        val lessonDupes = course.lessons.groupBy { it.id }.filter { it.value.size > 1 }.keys
        assertTrue("duplicate lessons $lessonDupes", lessonDupes.isEmpty())
    }

    @Test
    fun `every spelling parses and every sign is in the font`() {
        for ((id, mdc) in allSpellings()) {
            try {
                Mdc.parseChecked(mdc)
            } catch (e: Exception) {
                fail("$id: $mdc: ${e.message}")
            }
        }
        for (lesson in course.lessons) Mdc.parseChecked(lesson.keySign)
        for (award in course.awards) Mdc.parseChecked(award.sign)
    }

    @Test
    fun `every sign written anywhere is described by a sign item`() {
        val described = course.signs.map { it.code }.toSet()
        val missing = allSpellings().flatMap { (id, mdc) -> Mdc.parse(mdc).signs.filter { it !in described }.map { "$it in $id" } }
        assertTrue("undescribed signs: $missing", missing.isEmpty())
    }

    @Test
    fun `every item is taught exactly once`() {
        val taught = course.lessons.flatMap { it.teach }
        val dupes = taught.groupBy { it }.filter { it.value.size > 1 }.keys
        assertTrue("taught twice: $dupes", dupes.isEmpty())
        val untaught = course.items.map { it.id }.filter { it !in taught }
        assertTrue("never taught: $untaught", untaught.isEmpty())
        val unknown = taught.filter { course[it] == null }
        assertTrue("unknown ids in lessons: $unknown", unknown.isEmpty())
    }

    @Test
    fun `practice only reaches back to things already taught`() {
        val seen = mutableSetOf<String>()
        for (lesson in course.lessons) {
            for (id in lesson.practice) assertTrue("${lesson.id} practises $id before it is taught", id in seen)
            seen += lesson.teach
        }
    }

    @Test
    fun `phrases build on words taught no later than they are`() {
        val order = course.lessons.flatMapIndexed { i, l -> l.teach.map { it to i } }.toMap()
        for (phrase in course.items.filterIsInstance<PhraseItem>()) {
            for (token in phrase.tokens) {
                val word = token.wordId ?: fail("${phrase.id} token ${token.translit} has no word").let { "" }
                assertNotNull("${phrase.id}: unknown word $word", course[word])
                assertTrue("${phrase.id} uses $word before it is taught", order.getValue(word) <= order.getValue(phrase.id))
            }
        }
    }

    @Test
    fun `tiles spell the translation`() {
        fun norm(s: String) = s.lowercase().replace(Regex("[^a-z0-9 -]"), " ").split(' ').filter { it.isNotBlank() }
        for (phrase in course.items.filterIsInstance<PhraseItem>()) {
            assertEquals(phrase.id, norm(phrase.translation), norm(phrase.tiles.joinToString(" ")))
            for (order in phrase.tileOrders) assertEquals(phrase.id, phrase.tiles.sorted(), order.sorted())
        }
    }

    @Test
    fun `determinatives sit at the end of their words`() {
        for (word in course.words) {
            val det = word.determinative ?: continue
            assertTrue("${word.id}: $det is not the last quadrat of ${word.mdc}", word.mdc.endsWith("-$det"))
            val sign = course.signFor(det)
            assertNotNull("${word.id}: no sign item for $det", sign)
            assertTrue("${word.id}: $det is not a determinative", sign!!.role == SignRole.DETERMINATIVE || sign.role == SignRole.LOGOGRAM)
        }
    }

    @Test
    fun `transliterations use only Egyptological letters`() {
        val allowed = Translit.ALPHABET.toSet()
        fun check(id: String, t: String) {
            val bad = Translit.letters(t).filter { it !in allowed }
            assertTrue("$id: '$t' has non-Egyptological letters $bad", bad.isEmpty())
        }
        for (item in course.items) when (item) {
            is SignItem -> item.sounds.forEach { check(item.id, it) }
            is WordItem -> check(item.id, item.translit)
            is PhraseItem -> item.tokens.forEach { check(item.id, it.translit) }
            else -> Unit
        }
    }

    @Test
    fun `sections hold every unit once, in path order`() {
        assertEquals(course.units.map { it.id }, course.sections.flatMap { it.units })
        for (section in course.sections) Mdc.parseChecked(section.seal)
    }

    @Test
    fun `a section stays sealed until everything before it is done`() {
        val (first, second) = course.sections
        val before = course.unitsOf(first).flatMap { it.lessons }
        val done = LessonRecord(1, 1.0, t0, t0)
        val almost = Progress(lessons = before.dropLast(1).associate { it.id to done })
        assertFalse(course.isSealed(first, Progress()))
        assertTrue(course.isSealed(second, almost))
        val inside = course.unitsOf(second).flatMap { it.lessons }
        assertTrue(course.sealedIds(almost).containsAll(inside.flatMap { it.teach } + inside.mapNotNull { it.reading }))
        val finished = almost.copy(lessons = almost.lessons + (before.last().id to done))
        assertFalse(course.isSealed(second, finished))
        assertTrue(course.sealedIds(finished).none { id -> inside.any { id in it.teach } })
    }

    @Test
    fun `texts are made of phrases`() {
        for (text in course.texts) for (line in text.lines) {
            assertTrue("${text.id}: $line is not a phrase", course[line] is PhraseItem)
        }
        for (lesson in course.lessons) lesson.reading?.let { assertNotNull("${lesson.id} reads unknown $it", course.text(it)) }
    }

    @Test
    fun `everything that speaks has something to say, and a way to check it was said`() {
        val all = clips(course)
        assertTrue(all.size > 150)
        for (clip in all) {
            assertTrue("${clip.id} says nothing", clip.candidates.isNotEmpty() && clip.candidates.all { it.isNotBlank() && it.length < 160 })
            assertTrue("${clip.id} has no respelling", clip.latin.isNotBlank())
            assertEquals(clip.id, clip.expect.size, clip.critical.size)
        }
        // Words with the hard consonants must be checked for them.
        val ankh = all.first { it.id == "w:anx" }
        assertTrue(ankh.critical.count { it } >= 2)
        // Numerals are spoken as their Egyptian number words.
        assertEquals("ḥeḥ", all.first { it.id == "s:C11" }.latin)
    }

    private fun play(session: Session, answer: (com.krbavcic.pentalingo.engine.Exercise) -> Answer): List<Pair<com.krbavcic.pentalingo.engine.Exercise, Outcome>> {
        val log = mutableListOf<Pair<com.krbavcic.pentalingo.engine.Exercise, Outcome>>()
        while (true) {
            val ex = session.advance() ?: break
            log += ex to session.submit(answer(ex)).outcome
            check(log.size < 120) { "session ran away" }
        }
        return log
    }

    @Test
    fun `a perfect learner walks the whole course`() {
        var progress = Progress()
        var now = t0
        for (lesson in course.lessons) {
            val tested = mutableSetOf<String>()
            var sittings = 0
            while (!progress.isCompleted(lesson.id)) {
                assertTrue("${lesson.id} never completes", ++sittings <= 4)
                val session = Session.forLesson(course, progress, lesson, now, seed = lesson.id.hashCode().toLong() + sittings, clock = { now })
                val log = play(session) { it.solution() }
                for ((ex, outcome) in log) assertEquals("${lesson.id} ${ex.kind} ${ex.itemId}", Outcome.CORRECT, outcome)
                tested += log.filter { it.first !is Teach }.flatMap { it.first.credits + it.first.itemId }
                assertTrue("${lesson.id} sitting is too long: ${log.size}", log.size <= 32)
                progress = Recording.apply(course, progress, session.result()).progress
                now += day
            }
            for (id in lesson.teach) assertTrue("${lesson.id}: $id taught but never tested", id in tested)
        }
        assertEquals(course.lessons.size, progress.lessons.size)
        assertEquals(course.texts.map { it.id }.toSet(), progress.textsRead)
    }

    @Test
    fun `a careless learner never gets stuck`() {
        val rng = Random(42)
        var progress = Progress()
        var now = t0
        for (lesson in course.lessons) {
            var sittings = 0
            while (!progress.isCompleted(lesson.id)) {
                assertTrue("${lesson.id} never completes", ++sittings <= 6)
                val session = Session.forLesson(course, progress, lesson, now, seed = rng.nextLong(), clock = { now })
                play(session) { ex ->
                    if (ex is MultipleChoice && rng.nextDouble() < 0.4) Answer.Pick((ex.correct + 1) % ex.choices.size) else ex.solution()
                }
                assertTrue(lesson.id, session.result().completed)
                progress = Recording.apply(course, progress, session.result()).progress
                now += day / 2
            }
        }
        // And review sessions keep working a month later.
        val review = Session.forReview(course, progress, now + 30 * day, seed = 1, clock = { now + 30 * day })
        assertNotNull(review)
        assertTrue(play(review!!) { it.solution() }.none { it.first.kind == ExerciseKind.TEACH })
    }
}
