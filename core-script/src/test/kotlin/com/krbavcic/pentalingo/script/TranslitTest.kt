package com.krbavcic.pentalingo.script

import com.krbavcic.pentalingo.script.Translit.Verdict
import org.junit.Assert.assertEquals
import org.junit.Test

class TranslitTest {

    @Test
    fun `display swaps in the glyphs the serif has`() {
        assertEquals("ʿnḫ", Translit.display("ꜥnḫ"))
        assertEquals("mȝʿt", Translit.display("mꜣꜥt"))
    }

    @Test
    fun `other conventions normalise to the canonical one`() {
        assertEquals(Translit.normalize("jmn"), Translit.normalize("imn"))
        assertEquals(Translit.normalize("mꜣꜥt"), Translit.normalize("mȝʿt"))
        assertEquals(Translit.normalize("sꜣ"), Translit.normalize("zꜣ"))
        assertEquals(Translit.normalize("rn=f"), Translit.normalize("rnf"))
        assertEquals(Translit.normalize("ḥtp-dj-nsw"), Translit.normalize("ḥtpdjnsw"))
    }

    @Test
    fun `a slipped h is a near miss that names the slip`() {
        val c = Translit.compare("ḥtp", "htp")
        assertEquals(Verdict.NEAR, c.verdict)
        assertEquals(listOf('ḥ' to 'h'), c.confused)
    }

    @Test
    fun `a different consonant is wrong`() {
        assertEquals(Verdict.WRONG, Translit.compare("nfr", "nbr").verdict)
        assertEquals(Verdict.WRONG, Translit.compare("nfr", "nf").verdict)
    }

    @Test
    fun `exact is exact`() {
        assertEquals(Verdict.EXACT, Translit.compare("ꜥnḫ", "ʿnḫ").verdict)
    }
}

class YAndJTest {
    @org.junit.Test
    fun `y and j are the same letter in an answer`() {
        // Books write the end of ḏḥwtj either way; the course writes j, a learner may write y.
        org.junit.Assert.assertEquals(Translit.Verdict.EXACT, Translit.compare("ḏḥwtj", "ḏḥwty").verdict)
        org.junit.Assert.assertEquals(Translit.Verdict.EXACT, Translit.compare("šꜥy", "šꜥj").verdict)
    }
}
