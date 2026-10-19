package com.krbavcic.pentalingo.script

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MdcTest {

    @Test
    fun `quadrats split on hyphens and stacks on colons`() {
        val w = Mdc.parse("F35-I9:D21")
        assertEquals(2, w.elements.size)
        assertEquals(listOf("F35", "I9", "D21"), w.signs)
        val second = (w.elements[1] as Quadrat).group
        assertTrue(second is Stacked)
    }

    @Test
    fun `aliases resolve to the same signs as codes`() {
        assertEquals(Mdc.parse("F35-I9:D21").signs, Mdc.parse("nfr-f:r").signs)
        assertEquals(listOf("V28", "O4"), Mdc.parse("H-h").signs)
    }

    @Test
    fun `beside binds tighter than stacked`() {
        val group = (Mdc.parse("X1*Q3:N35").elements.single() as Quadrat).group
        group as Stacked
        assertTrue(group.parts[0] is Beside)
        assertEquals(SignRef("N35"), group.parts[1])
    }

    @Test
    fun `parentheses group`() {
        val group = (Mdc.parse("R4:(X1*Q3)").elements.single() as Quadrat).group
        group as Stacked
        assertEquals(SignRef("R4"), group.parts[0])
        assertTrue(group.parts[1] is Beside)
    }

    @Test
    fun `cartouches in both notations`() {
        for (src in listOf("<Aa1-G43-I9-G43>", "<-x-w-f-w->")) {
            val w = Mdc.parse(src)
            val ring = w.elements.single() as Cartouche
            assertEquals(4, ring.quadrats.size)
            assertEquals(listOf("Aa1", "G43", "I9", "G43"), w.signs)
        }
    }

    @Test
    fun `canonical codes`() {
        assertEquals("A1", Mdc.canonicalCode("A001"))
        assertEquals("Aa1", Mdc.canonicalCode("AA001"))
        assertEquals("N35A", Mdc.canonicalCode("n035a"))
        assertNull(Mdc.canonicalCode("nfr"))
    }

    @Test(expected = MdcException::class)
    fun `unknown alias is rejected`() {
        Mdc.parse("G17-quux")
    }

    @Test(expected = MdcException::class)
    fun `unclosed parenthesis is rejected`() {
        Mdc.parse("(X1*Q3")
    }

    @Test(expected = MdcException::class)
    fun `unclosed cartouche is rejected`() {
        Mdc.parse("<G17-N35")
    }

    @Test(expected = MdcException::class)
    fun `codes the font lacks are rejected when checked`() {
        Mdc.parseChecked("A999")
    }

    @Test
    fun `the bundled table knows the Gardiner list`() {
        val t = SignTable.bundled
        assertEquals(1071, t.size)
        assertEquals(0x13000, t.require("A1").codepoint)
        assertEquals(0x13153, t.require("G17").codepoint)
        assertEquals(0x1340D, t.require("Aa1").codepoint)
        assertEquals(0x13217, t.require("N35A").codepoint)
        assertEquals(2050, t.quadrat)
    }
}
