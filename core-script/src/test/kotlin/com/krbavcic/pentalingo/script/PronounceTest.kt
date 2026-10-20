package com.krbavcic.pentalingo.script

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PronounceTest {

    @Test
    fun `the Egyptological convention, with real consonants`() {
        val cases = mapOf(
            "nfr" to "nefer",
            "ḥtp" to "ḥetep",
            "ptḥ" to "peteḥ",
            "ḫpr" to "kheper",
            "nṯr" to "netjer",
            "sḏm" to "sedjem",
            "jmn" to "imen",
            "nswt" to "nesut",
            "ḏḥwtj" to "djeḥuti",
            "ꜣst" to "aset",
            "mꜣꜥt" to "maʿat",
            "rꜥ" to "raʿ",
            "ꜥḥꜥ" to "ʿaḥaʿ",
            "wn" to "wen",
            "wsjr" to "wesir",
            "ḥrw" to "ḥeru",
            "ḫwfw" to "khufu",
        )
        for ((tr, said) in cases) assertEquals(tr, said, Pronounce.latin(tr))
    }

    @Test
    fun `a cluster between vowels is kept, as in Inpu`() {
        assertEquals("inpu", Pronounce.latin("jnpw"))
    }

    @Test
    fun `conventional readings override the rule`() {
        assertEquals("ʿankh", Pronounce.latin("ꜥnḫ"))
        assertEquals("em", Pronounce.latin("m"))
        assertEquals("se", Pronounce.latin("z"))
    }

    @Test
    fun `suffixes join their word, compounds split into words`() {
        assertEquals("renef", Pronounce.latin("rn=f"))
        assertEquals("ḥetep di nesut", Pronounce.latin("ḥtp-dj-nswt"))
        assertEquals("ḥati ʿai", Pronounce.latin("ḥꜣtj-ꜥ=j"))
    }

    @Test
    fun `j is a vowel and y a consonant, so they never sound alike`() {
        assertNotEquals(Pronounce.letterIpa("j"), Pronounce.letterIpa("y"))
        assertTrue(Pronounce.ipa("jmn").startsWith("i"))
    }

    @Test
    fun `letters are said as their sounds, not by Semitic names`() {
        assertEquals("a", Pronounce.letterLatin("ꜣ"))
        assertEquals("ʿa", Pronounce.letterLatin("ꜥ"))
        assertEquals("i", Pronounce.letterLatin("j"))
        assertEquals("ye", Pronounce.letterLatin("y"))
        assertEquals("we", Pronounce.letterLatin("w"))
        assertEquals("be", Pronounce.letterLatin("b"))
        assertEquals("ḥe", Pronounce.letterLatin("ḥ"))
        assertEquals("khe", Pronounce.letterLatin("ḫ"))
        assertEquals("tje", Pronounce.letterLatin("ṯ"))
        assertEquals(listOf("حِ"), Pronounce.letterArabic("ḥ"))
        assertEquals(listOf("قِ"), Pronounce.letterArabic("q"))
        assertEquals(listOf("يِ"), Pronounce.letterArabic("y"))
        assertEquals("ħɛ", Pronounce.letterIpa("ḥ"))
    }

    @Test
    fun `the four h's are four different sounds`() {
        val ipa = listOf("h", "ḥ", "ḫ", "ẖ").map(Pronounce::letterIpa)
        assertEquals(4, ipa.toSet().size)
        assertEquals("ħɛtɛp", Pronounce.ipa("ḥtp"))
    }

    @Test
    fun `arabic script carries the consonants and every vowel`() {
        assertEquals("حِتِپْ", Pronounce.arabic("ḥtp"))
        assertEquals("عَنْخْ", Pronounce.arabic("ꜥnḫ"))
        assertEquals("مَاعَتْ", Pronounce.arabic("mꜣꜥt"))
        assertEquals("پِتِحْ", Pronounce.arabic("ptḥ"))
        assertEquals("آسِتْ", Pronounce.arabic("ꜣst"))
        assertEquals("إِمْ", Pronounce.arabic("m"))
        assertTrue(Pronounce.arabic("jqr").contains("ق"))
    }
}
