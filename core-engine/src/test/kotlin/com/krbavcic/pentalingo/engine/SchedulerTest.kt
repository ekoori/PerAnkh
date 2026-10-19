package com.krbavcic.pentalingo.engine

import com.krbavcic.pentalingo.model.Memory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerTest {

    private val day = 86_400_000L
    private val t0 = 1_760_000_000_000L

    @Test
    fun `recall is ninety percent after one stability`() {
        assertEquals(0.9, Memory.retrievability(4.0, 4.0), 1e-9)
        assertTrue(Memory.retrievability(8.0, 4.0) < 0.9)
    }

    @Test
    fun `a harder first exercise earns a stronger start`() {
        val easy = Scheduler.first(Grade.GOOD, 1, t0)
        val hard = Scheduler.first(Grade.GOOD, 4, t0)
        assertTrue(hard.stability > easy.stability)
        assertTrue(Scheduler.first(Grade.AGAIN, 4, t0).stability < easy.stability)
    }

    @Test
    fun `a correct review a day later grows stability, a lapse shrinks it`() {
        val m = Scheduler.first(Grade.GOOD, 2, t0)
        val good = Scheduler.review(m, Grade.GOOD, 2, t0 + day)
        val again = Scheduler.review(m, Grade.AGAIN, 2, t0 + day)
        assertTrue(good.stability > m.stability)
        assertTrue(again.stability <= m.stability)
        assertEquals(1, again.lapses)
    }

    @Test
    fun `grinding one day cannot fake a month of memory`() {
        var m = Scheduler.first(Grade.GOOD, 4, t0)
        repeat(30) { i -> m = Scheduler.review(m, Grade.EASY, 4, t0 + i * 60_000L) }
        assertTrue("stability ${m.stability}", m.stability <= Scheduler.SHORT_TERM_CEILING + 1e-9)
    }

    @Test
    fun `production earns more than recognition`() {
        val m = Scheduler.first(Grade.GOOD, 2, t0)
        val recognised = Scheduler.review(m, Grade.GOOD, 1, t0 + 2 * day)
        val produced = Scheduler.review(m, Grade.GOOD, 4, t0 + 2 * day)
        assertTrue(produced.stability > recognised.stability)
    }

    @Test
    fun `a lapse after being known is remembered`() {
        var m = Scheduler.first(Grade.GOOD, 4, t0)
        m = Scheduler.review(m, Grade.GOOD, 4, t0 + 3 * day)
        assertTrue(m.stability >= Memory.KNOWN_DAYS)
        m = Scheduler.review(m, Grade.AGAIN, 4, t0 + 40 * day)
        assertTrue(m.everLapsed)
    }
}
