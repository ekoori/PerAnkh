package com.krbavcic.pentalingo.engine

import com.krbavcic.pentalingo.model.Award
import com.krbavcic.pentalingo.model.Memory
import com.krbavcic.pentalingo.model.Progress
import com.krbavcic.pentalingo.model.ReadingText
import com.krbavcic.pentalingo.model.Requirement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkTest {

    private val t0 = 1_760_000_000_000L
    private val second = ReadingText("t:two", "Two", "Fixture", listOf("p:mpt"), "", work = "Demo", chapter = 2)
    private val first = ReadingText("t:one", "One", "Fixture", listOf("p:rnf"), "", work = "Demo", chapter = 1)
    private val award = Award("a:work", "Work", "šdj", "Y1", "Read the demo work", "Lore", Requirement.ReadWork("Demo"))
    private val course = Fixture.course.copy(texts = Fixture.course.texts + second + first, awards = listOf(award))

    private fun learned(vararg ids: String) = ids.associateWith { Memory(stability = 5.0, difficulty = 5.0, lastReview = t0, firstSeen = t0) }

    @Test
    fun `chapters come in story order`() {
        assertEquals(listOf("t:one", "t:two"), course.chapters("Demo").map { it.id })
    }

    @Test
    fun `a work is read when every chapter is read with all its lines learned`() {
        val one = Progress(memories = learned("p:rnf"), textsRead = setOf("t:one"))
        assertEquals(AwardProgress(1, 2), Awards.progress(award, course, one, t0))
        // A chapter that has grown since it was read waits for its new lines.
        val grown = one.copy(textsRead = setOf("t:one", "t:two"))
        assertFalse(Awards.progress(award, course, grown, t0).done)
        val both = grown.copy(memories = learned("p:rnf", "p:mpt"))
        assertTrue(Awards.progress(award, course, both, t0).done)
    }
}
