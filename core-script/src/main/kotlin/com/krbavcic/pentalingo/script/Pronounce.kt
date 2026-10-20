package com.krbavcic.pentalingo.script

/**
 * How a transliteration is said, by the Egyptological convention — with the consonants given
 * their real Afroasiatic values instead of English stand-ins.
 *
 * Nobody knows Middle Egyptian's vowels. Egyptologists pronounce the consonant skeleton by a
 * fixed convention: ꜣ and ꜥ are read "a", j is read "i", w between consonants "u", and an e is
 * slipped between any two consonants that would otherwise meet — nfr is "nefer", ptḥ "peteḥ".
 * What the convention does *not* say is that ḥ may be an English h or ḫ an English k; those
 * letters stand for sounds Arabic still has, and that is how they are rendered here:
 * ḥ /ħ/, ḫ /χ/, ẖ /ç/, ꜥ /ʕ/, q /q/, ṯ /tʃ/, ḏ /dʒ/.
 *
 * Output comes in three forms from one phoneme sequence, so they cannot disagree:
 * [latin] for learners to read ("ḥetep"), [ipa] for reference and reports, and [arabic] — the
 * text actually given to the speech model, because a native Arabic voice reading Arabic script
 * produces ḥ, ꜥ, ḫ and q reliably where English respellings and even IPA do not.
 */
object Pronounce {

    /** One sound. Vowels remember where they came from: their length depends on it. */
    data class Phone(val ipa: String, val kind: Kind) {
        enum class Kind { CONSONANT, LONG, SHORT, EPENTHETIC }

        val isVowel: Boolean get() = kind != Kind.CONSONANT
    }

    private fun c(ipa: String) = Phone(ipa, Phone.Kind.CONSONANT)
    private val A_LONG = Phone("a", Phone.Kind.LONG)
    private val A_SHORT = Phone("a", Phone.Kind.SHORT)
    private val I_LONG = Phone("i", Phone.Kind.LONG)
    private val U_LONG = Phone("u", Phone.Kind.LONG)
    private val E = Phone("ɛ", Phone.Kind.EPENTHETIC)

    private val CONSONANTS = mapOf(
        'b' to "b", 'p' to "p", 'f' to "f", 'm' to "m", 'n' to "n", 'r' to "r", 'h' to "h",
        'ḥ' to "ħ", 'ḫ' to "χ", 'ẖ' to "ç", 'z' to "s", 's' to "s", 'š' to "ʃ", 'q' to "q",
        'k' to "k", 'g' to "g", 't' to "t", 'ṯ' to "tʃ", 'd' to "d", 'ḏ' to "dʒ",
    )

    /**
     * Readings the convention fixes differently from the rule. Kept short on purpose: every entry
     * is a place where the app knowingly departs from mechanical pronunciation.
     */
    private val OVERRIDES: Map<String, String> = mapOf(
        // "ankh", never "anekh": the classroom form, from Coptic ōnh.
        "ꜥnḫ" to "ʕa nχ",
        // Prepositions and other one-consonant words are said with the e in front.
        "m" to "ɛ m", "n" to "ɛ n", "r" to "ɛ r",
        // ...except these nouns, said with it after.
        "z" to "s ɛ", "t" to "t ɛ",
    )

    /** The phones of one word. Suffix pronouns (=f) and morpheme dots join the word they follow. */
    fun word(translit: String): List<Phone> {
        val key = translit.replace(".", "")
        OVERRIDES[key]?.let { return parseOverride(it) }
        val letters = Translit.letters(key)
        if (letters.size == 1 && letters[0] in CONSONANTS) {
            return listOf(E, c(CONSONANTS.getValue(letters[0])))
        }
        // First pass: each letter becomes a consonant or a vowel by its neighbours.
        val units = mutableListOf<Phone>()
        letters.forEachIndexed { i, ch ->
            val next = letters.getOrNull(i + 1)
            val prevIsConsonant = units.lastOrNull()?.let { !it.isVowel } == true
            when (ch) {
                'ꜣ' -> {
                    if (units.isEmpty()) units += c("ʔ")
                    units += A_LONG
                }
                'ꜥ' -> if (prevIsConsonant) {
                    units += A_SHORT
                    units += c("ʕ")
                } else {
                    units += c("ʕ")
                    units += A_SHORT
                }
                'j' -> units += I_LONG
                'y' -> units += if (next != null && isVowelLetter(next)) c("j") else I_LONG
                // A w that starts a word or meets a vowel is a consonant ("wen", "waas");
                // between consonants or at the end it is the vowel u ("nesut", "ḥeru").
                'w' -> units += if (i == 0 || (next != null && isVowelLetter(next))) c("w") else U_LONG
                else -> units += c(CONSONANTS[ch] ?: ch.toString())
            }
        }
        // Second pass: an e between consonants that meet — but a two-consonant cluster may
        // stand between two vowels (jnpw is "inpu", not "inepu").
        val out = mutableListOf<Phone>()
        units.forEachIndexed { i, u ->
            val prev = out.lastOrNull()
            if (prev != null && !prev.isVowel && !u.isVowel) {
                val clusterBetweenVowels = i >= 2 && units[i - 2].isVowel &&
                    units.getOrNull(i + 1)?.isVowel == true
                if (!clusterBetweenVowels) out += E
            }
            out += u
        }
        return out
    }

    private fun isVowelLetter(ch: Char) = ch == 'ꜣ' || ch == 'ꜥ' || ch == 'j' || ch == 'y'

    private fun parseOverride(spec: String): List<Phone> = spec.split(' ').flatMap { part ->
        // An override is written as consonant/vowel chunks: "ʕa nχ" → ʕ a n χ.
        val phones = mutableListOf<Phone>()
        var i = 0
        while (i < part.length) {
            val two = if (i + 1 < part.length) part.substring(i, i + 2) else ""
            when {
                two == "tʃ" || two == "dʒ" -> { phones += c(two); i += 2 }
                part[i] == 'a' -> { phones += A_SHORT; i++ }
                part[i] == 'i' -> { phones += I_LONG; i++ }
                part[i] == 'u' -> { phones += U_LONG; i++ }
                part[i] == 'ɛ' -> { phones += E; i++ }
                else -> { phones += c(part[i].toString()); i++ }
            }
        }
        phones
    }

    /** Words of a transliteration: spaces and compound hyphens separate them, "=" joins. */
    fun words(translit: String): List<String> =
        translit.replace("=", "").split(' ', '-').filter { it.isNotBlank() }

    fun phones(translit: String): List<List<Phone>> = words(translit).map(::word)

    // ---- renderings --------------------------------------------------------------------------

    fun ipa(translit: String): String = phones(translit).joinToString(" ") { w -> w.joinToString("") { it.ipa } }

    private val LATIN = mapOf(
        "ʔ" to "", "ʕ" to "ʿ", "ħ" to "ḥ", "χ" to "kh", "ç" to "kh", "ʃ" to "sh",
        "tʃ" to "tj", "dʒ" to "dj", "j" to "y", "ɛ" to "e",
    )

    /** What learners read: "ḥetep", "ʿankh", "netjer". */
    fun latin(translit: String): String = phones(translit).joinToString(" ") { w ->
        w.joinToString("") { LATIN[it.ipa] ?: it.ipa }
    }

    private val ARABIC = mapOf(
        "ʔ" to "أ", "ʕ" to "ع", "ħ" to "ح", "χ" to "خ", "ç" to "خ", "q" to "ق", "k" to "ك",
        "g" to "گ", "t" to "ت", "tʃ" to "تش", "d" to "د", "dʒ" to "ج", "s" to "س", "ʃ" to "ش",
        "h" to "ه", "b" to "ب", "p" to "پ", "f" to "ف", "m" to "م", "n" to "ن", "r" to "ر",
        "w" to "و", "j" to "ي",
    )
    private const val FATHA = "َ"
    private const val KASRA = "ِ"
    private const val DAMMA = "ُ"
    private const val SUKUN = "ْ"

    /**
     * Arabic script with full vowel marks, for the speech model. The epenthetic e is a kasra,
     * which a speaker of Arabic realises as [e]~[i]; a vowel at the start of a word sits on an
     * alif carrier; a consonant with no vowel after it gets a sukun, so none is invented.
     */
    fun arabic(translit: String): String = phones(translit).joinToString(" ") { arabicWord(it) }

    /**
     * The same, with each e next to ḥ or ꜥ coloured towards a — as Semitic languages lower vowels
     * beside pharyngeals. Speech models articulate ḥ far more distinctly in "ḥa" than in "ḥe", so
     * this is offered as a second candidate when the plain rendering loses the sound.
     */
    fun arabicColoured(translit: String): String = phones(translit).joinToString(" ") { word ->
        arabicWord(
            word.mapIndexed { i, p ->
                val nearPharyngeal = word.getOrNull(i - 1)?.ipa in PHARYNGEALS || word.getOrNull(i + 1)?.ipa in PHARYNGEALS
                if (p.kind == Phone.Kind.EPENTHETIC && nearPharyngeal) A_SHORT else p
            },
        )
    }

    private val PHARYNGEALS = setOf("ħ", "ʕ")

    private fun arabicWord(phones: List<Phone>): String = buildString {
        phones.forEachIndexed { i, p ->
            val next = phones.getOrNull(i + 1)
            if (!p.isVowel) {
                // ʔ followed by a long a is written as the one letter alif-madda.
                if (p.ipa == "ʔ" && next == A_LONG) {
                    append("آ")
                    return@forEachIndexed
                }
                append(ARABIC[p.ipa] ?: p.ipa)
                if (next == null || !next.isVowel) append(SUKUN)
                return@forEachIndexed
            }
            val prev = phones.getOrNull(i - 1)
            if (prev?.ipa == "ʔ" && p == A_LONG) return@forEachIndexed
            when {
                // A word-initial vowel sits on an alif carrier.
                prev == null -> append(
                    when (p.ipa) {
                        "a" -> if (p.kind == Phone.Kind.LONG) "آ" else "أَ"
                        "i" -> "إِي"
                        "u" -> "أُو"
                        else -> "إِ"
                    },
                )
                // A vowel after a vowel glides on from it: jw is "iu", not "i-u".
                prev.isVowel -> append(
                    when (p.ipa) {
                        "a" -> "ا"
                        "i" -> "ي"
                        "u" -> "و"
                        else -> "ئِ"
                    },
                )
                else -> append(
                    when (p.ipa) {
                        "a" -> if (p.kind == Phone.Kind.LONG) "${FATHA}ا" else FATHA
                        "i" -> "${KASRA}ي"
                        "u" -> "${DAMMA}و"
                        else -> KASRA
                    },
                )
            }
        }
    }

    // ---- single letters --------------------------------------------------------------------

    /**
     * How a single alphabet sign is voiced. A consonant on its own is swallowed by any speech
     * model, so it is said between vowels, where it is fully articulated: "aḥa", "aqa". The four
     * letters Egyptologists name by their Semitic names are said by those names — aleph, ayin,
     * waw — except yod, which is said as the vowel "i" it is read as, so it cannot be confused
     * with y.
     */
    fun letterLatin(letter: String): String = when (letter) {
        "ꜣ" -> "aleph"
        "j" -> "i"
        "y" -> "ya"
        "ꜥ" -> "ʿayin"
        "w" -> "waw"
        else -> "a" + (LATIN[CONSONANTS[letter.single()] ?: letter] ?: CONSONANTS[letter.single()] ?: letter) + "a"
    }

    /** Arabic-script candidates for a letter, best first; the voice tool keeps the first that checks out. */
    fun letterArabic(letter: String): List<String> = when (letter) {
        "ꜣ" -> listOf("أَلِفْ", "آ")
        "j" -> listOf("إِي", "إِيإِي")
        "y" -> listOf("يَا", "أَيَا")
        "ꜥ" -> listOf("عَيْنْ", "أَعَا", "عَا")
        "w" -> listOf("وَاوْ", "أَوَا")
        else -> {
            val ar = ARABIC[CONSONANTS[letter.single()] ?: letter] ?: letter
            listOf("أَ$ar${FATHA}ا", "$ar${KASRA}$ar$SUKUN", "$ar${FATHA}اء")
        }
    }

    fun letterIpa(letter: String): String = when (letter) {
        "ꜣ" -> "ʔalɛf"
        "j" -> "ʔi"
        "y" -> "ja"
        "ꜥ" -> "ʕajin"
        "w" -> "waw"
        else -> "a" + (CONSONANTS[letter.single()] ?: letter) + "a"
    }
}
