package com.krbavcic.pentalingo.script

/**
 * Egyptological transliteration: the consonant skeleton Egyptologists write hieroglyphic words in.
 *
 * Canonical text uses Allen's conventions — `j` for the reed leaf, `q` for the hill slope, and
 * the proper Unicode letters ꜣ (U+A723) and ꜥ (U+A725) for alef and ayin. Learners' answers are
 * compared through [normalize], which accepts the other conventions in circulation (Gardiner's
 * `i` and `ḳ`, the `z`/`s` distinction Middle Egyptian no longer made, the ASCII `3`).
 */
object Translit {

    /** The 25 letters in Egyptological dictionary order. */
    val ALPHABET: List<Char> = listOf(
        'ꜣ', 'j', 'y', 'ꜥ', 'w', 'b', 'p', 'f', 'm', 'n', 'r', 'h', 'ḥ', 'ḫ', 'ẖ',
        'z', 's', 'š', 'q', 'k', 'g', 't', 'ṯ', 'd', 'ḏ',
    )

    /** Marks inside a transliteration that are not letters: suffix `=`, morpheme `.`, compound `-`. */
    private val SEPARATORS = setOf('.', '=', '-', ' ', '(', ')', '⸗', ' ')

    /**
     * The letters as the bundled serif can draw them.
     *
     * Noto Serif has no glyphs for ꜣ and ꜥ, so they are shown with the yogh ȝ and the half ring
     * ʿ — the forms Gardiner's own Grammar was printed with, so this is a return to the classic
     * typography rather than a workaround that looks like one.
     */
    fun display(text: String): String = buildString(text.length) {
        for (c in text) append(
            when (c) {
                'ꜣ' -> 'ȝ'
                'Ꜣ' -> 'Ȝ'
                'ꜥ', 'Ꜥ' -> 'ʿ'
                else -> c
            },
        )
    }

    /** One letter in canonical form, or null for punctuation that carries no sound. */
    fun canonical(c: Char): Char? = when (c) {
        in SEPARATORS -> null
        'ȝ', 'Ȝ', '3', 'Ꜣ' -> 'ꜣ'
        'ʿ', 'Ꜥ' -> 'ꜥ'
        'i', 'ỉ', 'ı' -> 'j'
        'ḳ' -> 'q'
        else -> c.lowercaseChar()
    }

    fun letters(text: String): List<Char> = text.mapNotNull(::canonical)

    /** For comparing answers: canonical letters, with z and s merged as they were by this period. */
    fun normalize(text: String): String =
        letters(text).joinToString("") { if (it == 'z') "s" else it.toString() }

    /**
     * Letters a learner can be forgiven for mixing up while still knowing the word.
     *
     * Each family shares a base sound in conventional pronunciation — all four h's are "h" to an
     * English speaker — so confusing them is a near miss worth a precise note, not a wrong answer
     * worth a repeat.
     */
    fun family(c: Char): Char = when (canonical(c)) {
        'h', 'ḥ', 'ḫ', 'ẖ' -> 'h'
        's', 'z', 'š' -> 's'
        't', 'ṯ' -> 't'
        'd', 'ḏ' -> 'd'
        'ꜣ', 'ꜥ' -> 'a'
        'j', 'y' -> 'j'
        'k', 'q', 'g' -> 'k'
        else -> canonical(c) ?: c
    }

    enum class Verdict { EXACT, NEAR, WRONG }

    data class Comparison(val verdict: Verdict, val confused: List<Pair<Char, Char>> = emptyList())

    /**
     * Compares a learner's transliteration with the expected one.
     *
     * NEAR means every letter is right or swapped within its [family] — "ḥtp" written "htp". The
     * list of swaps lets feedback name exactly which distinction slipped.
     */
    fun compare(expected: String, given: String): Comparison {
        val e = normalize(expected)
        val g = normalize(given)
        if (e == g) return Comparison(Verdict.EXACT)
        if (e.length != g.length) return Comparison(Verdict.WRONG)
        val swaps = mutableListOf<Pair<Char, Char>>()
        for (i in e.indices) {
            if (e[i] == g[i]) continue
            if (family(e[i]) != family(g[i])) return Comparison(Verdict.WRONG)
            swaps += e[i] to g[i]
        }
        return Comparison(Verdict.NEAR, swaps)
    }
}
