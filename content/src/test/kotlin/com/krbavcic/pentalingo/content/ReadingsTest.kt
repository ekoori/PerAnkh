package com.krbavcic.pentalingo.content

import com.krbavcic.pentalingo.engine.SignReading
import com.krbavcic.pentalingo.engine.SignReading.Reads
import com.krbavcic.pentalingo.engine.SignReading.Repeats
import com.krbavcic.pentalingo.engine.SignReading.Silent
import com.krbavcic.pentalingo.engine.readingsInWord
import com.krbavcic.pentalingo.engine.soundIn
import com.krbavcic.pentalingo.model.WordItem
import org.junit.Assert.assertEquals
import org.junit.Test

/** A word's sign-by-sign breakdown gives each sign the value it has in that word. */
class ReadingsTest {

    private val course = MIDDLE_EGYPTIAN

    private fun readings(wordId: String): List<SignReading> = readingsInWord(course, course.require(wordId) as WordItem)

    private fun said(wordId: String): List<String> = readings(wordId).map { r -> soundIn(course, r) ?: "-" }

    @Test
    fun `the throne is ws in Osiris and ꜣs in Isis, not st`() {
        assertEquals(listOf("ws", "jr", "-"), said("w:wsjr"))
        assertEquals(listOf("ꜣs", "t", "-", "-"), said("w:Ast"))
    }

    @Test
    fun `the feather is šw in Shu, with its complements on either side`() {
        val r = readings("w:Sw")
        assertEquals(Reads("N37", "š"), r[0])
        assertEquals(Reads("H6", "šw"), r[1])
        assertEquals(Repeats("G43", "w"), r[2])
        assertEquals(Silent("A40"), r[3])
        assertEquals(listOf("mꜣꜥt", "t", "-", "-"), said("w:mAat"))
    }

    @Test
    fun `the village sign is the word njwt where it stands for town`() {
        assertEquals(listOf("njwt", "t", "-"), said("w:njwt"))
        assertEquals(Repeats("X1", "t"), readings("w:njwt")[1])
    }

    @Test
    fun `complements repeat what the sign before them spelled`() {
        assertEquals(listOf(Reads("F35", "nfr"), Repeats("I9", "f"), Repeats("D21", "r")), readings("w:nfr"))
    }
}
