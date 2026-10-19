package com.krbavcic.pentalingo.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckerTest {

    private val checker = Checker(Fixture.course)

    @Test
    fun `transliteration with a slipped h is a near miss`() {
        val ex = Arrange(
            "w:ptH", ExerciseKind.TRANSLITERATE, "", Display(),
            tiles = emptyList(), answers = listOf(listOf("p", "t", "ḥ")), solution = Display(translit = "ptḥ"),
        )
        val v = checker.check(ex, Answer.Tiles(listOf("p", "t", "h")))
        assertEquals(Outcome.NEAR, v.outcome)
        assertTrue(v.note!!.contains("ḥ"))
    }

    @Test
    fun `a spelling missing only its determinative is a near miss`() {
        val ex = Spell("w:jt", "", Display(), emptyList(), listOf(listOf("M17", "X1", "I9", "A1")), "M17-X1:I9-A1", "A1")
        assertEquals(Outcome.NEAR, checker.check(ex, Answer.Signs(listOf("M17", "X1", "I9"))).outcome)
        assertEquals(Outcome.CORRECT, checker.check(ex, Answer.Signs(listOf("M17", "X1", "I9", "A1"))).outcome)
        assertEquals(Outcome.WRONG, checker.check(ex, Answer.Signs(listOf("M17", "A1"))).outcome)
    }

    @Test
    fun `ten strokes are the right total but not how a scribe writes ten`() {
        val ex = BuildNumber("s:V20", 12, Numerals.SIGNS.takeLast(2), emptyList())
        assertEquals(Outcome.NEAR, checker.check(ex, Answer.Signs(List(12) { "Z1" })).outcome)
        assertEquals(Outcome.CORRECT, checker.check(ex, Answer.Signs(listOf("V20", "Z1", "Z1"))).outcome)
        assertEquals(Outcome.WRONG, checker.check(ex, Answer.Signs(listOf("V20"))).outcome)
    }

    @Test
    fun `numbers spell largest first in grouped rows`() {
        assertEquals("V20*V20-Z1*Z1*Z1", Numerals.spell(23))
        assertEquals("Z1*Z1*Z1:Z1*Z1", Numerals.spell(5))
        assertEquals("C11", Numerals.spell(1_000_000))
        assertEquals(1234, Numerals.sum(listOf("M12", "V1", "V1", "V20", "V20", "V20", "Z1", "Z1", "Z1", "Z1")))
    }

    @Test
    fun `right words in the wrong order are a near miss`() {
        val ex = Arrange(
            "p:mpt", ExerciseKind.ARRANGE_TRANSLATION, "", Display(), emptyList(),
            answers = listOf(listOf("in", "the", "sky")), solution = Display(text = "in the sky"),
        )
        assertEquals(Outcome.CORRECT, checker.check(ex, Answer.Tiles(listOf("In", "the", "sky"))).outcome)
        assertEquals(Outcome.NEAR, checker.check(ex, Answer.Tiles(listOf("the", "in", "sky"))).outcome)
        assertEquals(Outcome.WRONG, checker.check(ex, Answer.Tiles(listOf("in", "sky"))).outcome)
    }
}

class ReadsInOrderTest {
    @Test
    fun `complements and determinatives are skipped, transposed names fail`() {
        val c = Fixture.course
        org.junit.Assert.assertTrue(readsInOrder(c, c.require("w:nfr") as com.krbavcic.pentalingo.model.WordItem))
        org.junit.Assert.assertTrue(readsInOrder(c, c.require("w:ptH") as com.krbavcic.pentalingo.model.WordItem))
        // jt "father" keeps a fossil f from older jtf: sounded out it gives jtf, so it is excluded.
        org.junit.Assert.assertFalse(readsInOrder(c, c.require("w:jt") as com.krbavcic.pentalingo.model.WordItem))
        val transposed = com.krbavcic.pentalingo.model.WordItem(
            "w:x", "ptm", "G17-Q3:X1", "test", com.krbavcic.pentalingo.model.Pos.NAME, "x",
        )
        org.junit.Assert.assertFalse(readsInOrder(c, transposed))
    }
}

class ListeningTest {
    @Test
    fun `a listening question never offers a sign that sounds the same`() {
        // In the fixture h and ḥ are both voiced "he": by ear they are one sound.
        val course = Fixture.course
        val progress = com.krbavcic.pentalingo.model.Progress()
        val k = Knowledge(course, progress, 0L)
        repeat(60) { seed ->
            val factory = ExerciseFactory(course, k, kotlin.random.Random(seed)) { course.items.map { it.id }.toSet() }
            val ex = factory.build(course.require("s:V28"), ExerciseKind.LISTEN_TO_SIGN, hard = true) as MultipleChoice? ?: return@repeat
            val offered = ex.choices.mapNotNull { it.itemId }
            org.junit.Assert.assertFalse("h offered against ḥ by ear: $offered", "s:O4" in offered)
        }
    }
}

class HintTest {
    @Test
    fun `a right answer given with a hint counts as a near miss`() {
        val s = Session.forLesson(Fixture.course, com.krbavcic.pentalingo.model.Progress(), Fixture.lesson1, 0L, seed = 1, clock = { 0L })
        var ex = s.advance()
        while (ex is Teach) { s.submit(Answer.Seen); ex = s.advance() }
        val v = s.submit(ex!!.solution(), hinted = true)
        org.junit.Assert.assertEquals(Outcome.NEAR, v.outcome)
        org.junit.Assert.assertTrue(v.note!!.startsWith("Right"))
    }
}
