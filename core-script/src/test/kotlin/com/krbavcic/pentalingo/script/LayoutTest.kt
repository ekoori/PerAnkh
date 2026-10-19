package com.krbavcic.pentalingo.script

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutTest {

    private val layout = Layout()
    private val table = SignTable.bundled

    private fun natural(code: String): Pair<Float, Float> {
        val ink = table.require(code).ink
        return ink.width / table.quadrat.toFloat() to ink.height / table.quadrat.toFloat()
    }

    @Test
    fun `every sign lies inside the inscription`() {
        for (src in listOf("F35-I9:D21", "S34-N35:Aa1", "M17-Y5:N35-G17-F4:X1", "<M17-Y5:N35-X1:G43-X1-S34>")) {
            val ins = layout.layout(src)
            val bounds = Rect(0f, 0f, ins.width, ins.height)
            for (s in ins.signs) assertTrue("$src ${s.glyph.code} ${s.box}", bounds.contains(s.box))
        }
    }

    @Test
    fun `stacked signs do not overlap and fit one quadrat`() {
        val ins = layout.layout("I9:D21")
        val (a, b) = ins.signs
        assertFalse(a.box.overlaps(b.box))
        assertTrue(a.box.bottom <= b.box.y + 1e-4f)
        assertTrue(ins.signs.maxOf { it.box.bottom } - ins.signs.minOf { it.box.y } <= 1f + 1e-4f)
    }

    @Test
    fun `two full-height signs stacked are scaled to share the square`() {
        val ins = layout.layout("M17:M17")
        val (top, bottom) = ins.signs
        assertTrue(top.box.h < 0.5f)
        assertEquals(top.box.h, bottom.box.h, 1e-4f)
        assertFalse(top.box.overlaps(bottom.box))
    }

    @Test
    fun `nothing is ever scaled up`() {
        val ins = layout.layout("X1")
        val (w, h) = natural("X1")
        val box = ins.signs.single().box
        assertEquals(w, box.w, 1e-4f)
        assertEquals(h, box.h, 1e-4f)
        // ... and a small sign alone sits in the middle of its quadrat.
        assertEquals(0.5f, box.y + box.h / 2f, 1e-3f)
    }

    @Test
    fun `signs side by side are centred on each other`() {
        val ins = layout.layout("X1*Q3")
        val (a, b) = ins.signs
        assertEquals(a.box.y + a.box.h / 2f, b.box.y + b.box.h / 2f, 1e-4f)
        assertTrue(a.box.right <= b.box.x + 1e-4f)
    }

    @Test
    fun `reading order is kept`() {
        val ins = layout.layout("M17-Y5:N35-G17")
        assertEquals(listOf("M17", "Y5", "N35", "G17"), ins.signs.map { it.glyph.code })
        assertEquals(listOf(0, 1, 2, 3), ins.signs.map { it.index })
    }

    @Test
    fun `cartouche ring encloses its name`() {
        val ins = layout.layout("<Aa1-G43-I9-G43>")
        val ring = ins.rings.single()
        for (s in ins.signs) assertTrue(ring.contains(s.box))
        assertTrue(ins.height > 1f)
    }

    @Test
    fun `words in a line keep their own extents`() {
        val ins = layout.line(listOf(Mdc.parse("D21:N35"), Mdc.parse("I9")))
        assertEquals(2, ins.tokens.size)
        for (s in ins.signs) assertTrue(ins.tokens[s.token].contains(s.box))
        assertTrue(ins.tokens[0].right < ins.tokens[1].x)
    }
}
