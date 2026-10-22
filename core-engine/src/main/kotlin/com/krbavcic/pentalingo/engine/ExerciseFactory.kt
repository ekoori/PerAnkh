package com.krbavcic.pentalingo.engine

import com.krbavcic.pentalingo.model.ConceptItem
import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.model.Item
import com.krbavcic.pentalingo.model.PhraseItem
import com.krbavcic.pentalingo.model.Progress
import com.krbavcic.pentalingo.model.SignItem
import com.krbavcic.pentalingo.model.SignRole
import com.krbavcic.pentalingo.model.WordItem
import com.krbavcic.pentalingo.script.Mdc
import com.krbavcic.pentalingo.script.Pronounce
import com.krbavcic.pentalingo.script.Translit
import kotlin.math.max
import kotlin.random.Random

/**
 * Turns an item and a difficulty tier into a concrete exercise.
 *
 * The quality of a multiple-choice question is all in its wrong answers, so most of this class is
 * about choosing them. Early on they are merely plausible; once an item is known they are its
 * real rivals — the other h's, the other birds — and any pair the learner has actually mixed up
 * before is weighted heavily, so a confusion keeps being tested until it stops happening.
 */
class ExerciseFactory(
    private val course: Course,
    private val knowledge: Knowledge,
    private val rng: Random,
    /** Items taught earlier in the current session count as seen. */
    private val seenInSession: () -> Set<String> = { emptySet() },
) {

    private fun seen(id: String): Boolean = knowledge.isIntroduced(id) || id in seenInSession()

    private fun confusion(a: String, b: String): Int = knowledge.progress.confusionCount(a, b)

    // ---- tiers and kinds -------------------------------------------------------------------

    fun maxTier(item: Item): Int = when (item) {
        is SignItem -> if (item.role == SignRole.NUMERAL) 3 else 3
        is WordItem -> 4
        is PhraseItem -> 4
        is ConceptItem -> 1
    }

    fun kindsFor(item: Item, tier: Int): List<ExerciseKind> = when (item) {
        is SignItem -> when {
            item.role == SignRole.NUMERAL -> when (tier) {
                1 -> listOf(ExerciseKind.SIGN_TO_MEANING)
                2 -> listOf(ExerciseKind.READ_NUMBER)
                else -> listOf(ExerciseKind.BUILD_NUMBER, ExerciseKind.READ_NUMBER)
            }
            item.sounds.isEmpty() -> when (tier) {
                1 -> listOf(ExerciseKind.SIGN_TO_MEANING)
                else -> listOf(ExerciseKind.MEANING_TO_SIGN, ExerciseKind.SIGN_TO_MEANING)
            }
            else -> when (tier) {
                1 -> if (item.meaning != null && item.role == SignRole.LOGOGRAM) {
                    listOf(ExerciseKind.SIGN_TO_SOUND, ExerciseKind.SIGN_TO_MEANING)
                } else {
                    listOf(ExerciseKind.SIGN_TO_SOUND)
                }
                2 -> listOfNotNull(
                    ExerciseKind.SOUND_TO_SIGN,
                    ExerciseKind.LISTEN_TO_SIGN.takeIf { item.spoken != null },
                )
                else -> listOfNotNull(
                    ExerciseKind.SOUND_TO_SIGN,
                    ExerciseKind.SIGN_TO_SOUND,
                    ExerciseKind.LISTEN_TO_SIGN.takeIf { item.spoken != null },
                )
            }
        }
        is WordItem -> when (tier) {
            1 -> listOf(ExerciseKind.WORD_TO_MEANING)
            2 -> listOf(ExerciseKind.WORD_TO_MEANING, ExerciseKind.MEANING_TO_WORD, ExerciseKind.LISTEN_TO_WORD)
            3 -> listOfNotNull(
                ExerciseKind.TRANSLITERATE,
                ExerciseKind.DETERMINATIVE.takeIf { item.determinative != null },
            )
            else -> listOf(ExerciseKind.SPELL)
        }
        is PhraseItem -> when (tier) {
            1, 2 -> listOf(ExerciseKind.PHRASE_TO_MEANING, ExerciseKind.ARRANGE_TRANSLATION)
            3 -> listOf(ExerciseKind.ARRANGE_TRANSLATION)
            else -> listOf(ExerciseKind.ARRANGE_GLYPHS)
        }
        is ConceptItem -> listOf(ExerciseKind.CONCEPT_QUIZ)
    }

    /**
     * An exercise for [item] at [tier], or at the nearest tier below that can be built.
     *
     * @param avoid a kind not to repeat, usually the one just shown — variety is part of what
     *   makes a session feel less like a drill.
     */
    fun make(item: Item, tier: Int, avoid: ExerciseKind? = null): Exercise? {
        var t = tier.coerceIn(1, maxTier(item))
        while (t >= 1) {
            val kinds = kindsFor(item, t).shuffled(rng).sortedBy { if (it == avoid) 1 else 0 }
            for (kind in kinds) {
                build(item, kind, hard = t >= 3)?.let { return it }
            }
            t--
        }
        // Nothing buildable (too few items of the kind to choose distractors from). The session
        // skips the step; the course tests make sure no real lesson ever depends on that.
        return null
    }

    fun build(item: Item, kind: ExerciseKind, hard: Boolean): Exercise? = when (item) {
        is SignItem -> when (kind) {
            ExerciseKind.SIGN_TO_SOUND -> signToSound(item, hard)
            ExerciseKind.SIGN_TO_MEANING -> signToMeaning(item, hard)
            ExerciseKind.SOUND_TO_SIGN -> soundToSign(item, hard, listen = false)
            ExerciseKind.LISTEN_TO_SIGN -> soundToSign(item, hard, listen = true)
            ExerciseKind.MEANING_TO_SIGN -> meaningToSign(item, hard)
            ExerciseKind.READ_NUMBER -> readNumber(item)
            ExerciseKind.BUILD_NUMBER -> buildNumber(item)
            else -> null
        }
        is WordItem -> when (kind) {
            ExerciseKind.WORD_TO_MEANING -> wordToMeaning(item, showTranslit = !hard && rng.nextBoolean() || knowledge.memory(item.id) == null)
            ExerciseKind.MEANING_TO_WORD -> meaningToWord(item, hard)
            ExerciseKind.LISTEN_TO_WORD -> listenToWord(item, hard)
            ExerciseKind.TRANSLITERATE -> transliterate(item)
            ExerciseKind.DETERMINATIVE -> determinative(item)
            ExerciseKind.SPELL -> spell(item)
            else -> null
        }
        is PhraseItem -> when (kind) {
            ExerciseKind.PHRASE_TO_MEANING -> phraseToMeaning(item, showTranslit = !hard)
            ExerciseKind.ARRANGE_TRANSLATION -> arrangeTranslation(item)
            ExerciseKind.ARRANGE_GLYPHS -> arrangeGlyphs(item)
            else -> null
        }
        is ConceptItem -> if (kind == ExerciseKind.CONCEPT_QUIZ) conceptQuiz(item) else null
    }

    fun teach(item: Item, firstTime: Boolean): Teach = Teach(item.id, firstTime)

    // ---- picking ---------------------------------------------------------------------------

    /** Weighted sampling without replacement. */
    private fun <T> pick(pool: List<T>, n: Int, weight: (T) -> Double): List<T> {
        val remaining = pool.toMutableList()
        val out = mutableListOf<T>()
        while (out.size < n && remaining.isNotEmpty()) {
            val weights = remaining.map { max(weight(it), 1e-4) }
            var r = rng.nextDouble() * weights.sum()
            var i = 0
            while (i < weights.lastIndex && r >= weights[i]) {
                r -= weights[i]
                i++
            }
            out += remaining.removeAt(i)
        }
        return out
    }

    /** Options in random order; returns the list and the index the answer landed at. */
    private fun shuffleWithAnswer(answer: Choice, others: List<Choice>): Pair<List<Choice>, Int> {
        val all = (others + answer).shuffled(rng)
        return all to all.indexOf(answer)
    }

    // ---- signs -----------------------------------------------------------------------------

    private val phonograms: List<SignItem> by lazy {
        course.signs.filter { it.sounds.isNotEmpty() && it.role != SignRole.NUMERAL }
    }

    private fun lookalike(a: SignItem, b: SignItem): Boolean = b.code in a.lookalikes || a.code in b.lookalikes

    private fun signWeight(target: SignItem, other: SignItem, hard: Boolean): Double {
        var w = 1.0
        // Signs the learner has met make honest distractors; strangers mostly make free points.
        if (seen(other.id)) w *= 12.0
        if (other.role == target.role) w *= 3.0
        val sameFamily = target.sounds.isNotEmpty() && other.sounds.isNotEmpty() &&
            Translit.family(target.sounds[0].first()) == Translit.family(other.sounds[0].first()) &&
            target.sounds[0].length == other.sounds[0].length
        if (hard && sameFamily) w *= 5.0
        if (lookalike(target, other)) w *= if (hard) 6.0 else 2.0
        w *= 1.0 + 2.0 * confusion(target.id, other.id)
        return w
    }

    private fun soundKey(s: String) = Translit.normalize(s)

    /** What a sign's clip says, as sound: z and s are one sound by this period. */
    /**
     * What a sign's clip says: the text the voice was given. Comparing that, not the IPA, means
     * two signs whose recordings sound the same — ḫ and ẖ are both spoken as kh — are never
     * told apart by ear.
     */
    private fun voiceOf(sign: SignItem): String? {
        val spoken = sign.spoken ?: return null
        return if (sign.role == SignRole.UNILITERAL) Pronounce.letterArabic(spoken).first() else Pronounce.arabic(spoken)
    }

    private fun soundsAlike(a: SignItem, b: SignItem): Boolean = voiceOf(a) != null && voiceOf(a) == voiceOf(b)

    private fun signToSound(sign: SignItem, hard: Boolean): MultipleChoice? {
        val answer = sign.sounds.firstOrNull() ?: return null
        val taken = mutableSetOf(soundKey(answer))
        sign.sounds.forEach { taken += soundKey(it) }
        // Same-sized sounds only, while there are enough: a one-letter sign offered against
        // three-letter answers can be solved by counting, not by knowing.
        val size = Translit.letters(answer).size
        val sameSize = phonograms.filter { it.id != sign.id && Translit.letters(it.sounds[0]).size == size }
        val pool = if (sameSize.size >= 6) sameSize else phonograms.filter { it.id != sign.id }
        val distractors = mutableListOf<SignItem>()
        for (candidate in pick(pool, pool.size, { signWeight(sign, it, hard) })) {
            val key = soundKey(candidate.sounds[0])
            if (key in taken) continue
            taken += key
            distractors += candidate
            if (distractors.size == 3) break
        }
        if (distractors.size < 3) return null
        val (choices, correct) = shuffleWithAnswer(
            Choice(translit = answer, itemId = sign.id),
            distractors.map { Choice(translit = it.sounds[0], itemId = it.id) },
        )
        return MultipleChoice(
            itemId = sign.id,
            kind = ExerciseKind.SIGN_TO_SOUND,
            instruction = "What sound does this sign spell?",
            prompt = Display(mdc = sign.code),
            choices = choices,
            correct = correct,
            explain = signExplanation(sign),
            tallies = hFamilyTally(sign, distractors),
        )
    }

    /** Counts a correct answer towards the Eye of Horus only when the other h's were on offer. */
    private fun hFamilyTally(sign: SignItem, distractors: List<SignItem>): List<String> {
        fun isH(s: SignItem) = s.sounds.firstOrNull()?.let { it.length == 1 && Translit.family(it[0]) == 'h' } == true
        return if (isH(sign) && distractors.any(::isH)) listOf(TALLY_H_FAMILY) else emptyList()
    }

    private fun meaningOf(sign: SignItem): String? = when (sign.role) {
        SignRole.NUMERAL -> Numerals.valueOf(sign.code)?.let(Numerals::format)
        else -> sign.meaning
    }

    private fun signToMeaning(sign: SignItem, hard: Boolean): MultipleChoice? {
        val answer = meaningOf(sign) ?: return null
        if (sign.role == SignRole.NUMERAL) return numeralValue(sign, answer)
        val pool = course.signs.filter {
            it.id != sign.id && meaningOf(it) != null &&
                (it.role == SignRole.NUMERAL) == (sign.role == SignRole.NUMERAL)
        }
        val taken = mutableSetOf(answer.lowercase())
        val distractors = mutableListOf<SignItem>()
        for (candidate in pick(pool, pool.size, { signWeight(sign, it, hard) })) {
            val m = meaningOf(candidate)!!.lowercase()
            if (m in taken) continue
            taken += m
            distractors += candidate
            if (distractors.size == 3) break
        }
        if (distractors.size < 3) return null
        val (choices, correct) = shuffleWithAnswer(
            Choice(label = answer, itemId = sign.id),
            distractors.map { Choice(label = meaningOf(it), itemId = it.id) },
        )
        val instruction = when (sign.role) {
            SignRole.DETERMINATIVE -> "What kind of word does this sign mark?"
            SignRole.NUMERAL -> "What number is this sign?"
            else -> "What does this sign mean?"
        }
        return MultipleChoice(
            itemId = sign.id,
            kind = ExerciseKind.SIGN_TO_MEANING,
            instruction = instruction,
            prompt = Display(mdc = sign.code),
            choices = choices,
            correct = correct,
            explain = signExplanation(sign),
        )
    }

    /** Which power of ten a numeral sign is; the wrong answers are the other powers. */
    private fun numeralValue(sign: SignItem, answer: String): MultipleChoice {
        val unit = Numerals.valueOf(sign.code)!!
        val others = Numerals.SIGNS.filter { it.second != unit }
            .sortedBy { (_, v) -> kotlin.math.abs(kotlin.math.log10(v.toDouble()) - kotlin.math.log10(unit.toDouble())) }
            .take(3)
        val (choices, correct) = shuffleWithAnswer(
            Choice(label = answer, itemId = sign.id),
            others.map { (code, v) -> Choice(label = Numerals.format(v), itemId = course.signFor(code)?.id) },
        )
        return MultipleChoice(
            itemId = sign.id,
            kind = ExerciseKind.SIGN_TO_MEANING,
            instruction = "What number is this sign?",
            prompt = Display(mdc = sign.code),
            choices = choices,
            correct = correct,
            explain = signExplanation(sign),
        )
    }

    private fun soundToSign(sign: SignItem, hard: Boolean, listen: Boolean): MultipleChoice? {
        val answer = sign.sounds.firstOrNull() ?: return null
        if (listen && sign.spoken == null) return null
        // Conventional pronunciation merges some letters — h and ḥ are both "heh" — so when the
        // question is only a sound, a sign that sounds the same cannot be a wrong answer.
        val pool = phonograms.filter { other ->
            other.id != sign.id && other.sounds.none { soundKey(it) in sign.sounds.map(::soundKey) } &&
                !(listen && soundsAlike(other, sign))
        }
        val size = Translit.letters(answer).size
        val distractors = pick(pool, 3) { other ->
            signWeight(sign, other, hard) * if (Translit.letters(other.sounds[0]).size == size) 3.0 else 1.0
        }
        if (distractors.size < 3) return null
        val (choices, correct) = shuffleWithAnswer(
            Choice(mdc = sign.code, itemId = sign.id),
            distractors.map { Choice(mdc = it.code, itemId = it.id) },
        )
        return MultipleChoice(
            itemId = sign.id,
            kind = if (listen) ExerciseKind.LISTEN_TO_SIGN else ExerciseKind.SOUND_TO_SIGN,
            instruction = if (listen) "Which sign makes the sound you hear?" else "Which sign spells this sound?",
            prompt = if (listen) Display(audio = sign.id) else Display(translit = answer, audio = sign.id),
            choices = choices,
            correct = correct,
            explain = signExplanation(sign),
            tallies = hFamilyTally(sign, distractors),
        )
    }

    private fun meaningToSign(sign: SignItem, hard: Boolean): MultipleChoice? {
        val answer = meaningOf(sign) ?: return null
        val pool = course.signs.filter {
            it.id != sign.id && meaningOf(it)?.lowercase() != answer.lowercase() && meaningOf(it) != null
        }
        val distractors = pick(pool, 3) { signWeight(sign, it, hard) * if (it.role == sign.role) 2.0 else 1.0 }
        if (distractors.size < 3) return null
        val (choices, correct) = shuffleWithAnswer(
            Choice(mdc = sign.code, itemId = sign.id),
            distractors.map { Choice(mdc = it.code, itemId = it.id) },
        )
        return MultipleChoice(
            itemId = sign.id,
            kind = ExerciseKind.MEANING_TO_SIGN,
            instruction = if (sign.role == SignRole.DETERMINATIVE) "Which determinative marks this?" else "Which sign means this?",
            prompt = Display(text = answer),
            choices = choices,
            correct = correct,
            explain = signExplanation(sign),
        )
    }

    fun signExplanation(sign: SignItem): String = buildString {
        append(sign.name.replaceFirstChar { it.uppercase() })
        when {
            sign.role == SignRole.NUMERAL -> append(" = ").append(meaningOf(sign))
            sign.sounds.isNotEmpty() -> {
                append(" spells ").append(sign.sounds.joinToString(" or ") { Translit.display(it) })
                sign.meaning?.let { append(", and on its own means “").append(it).append("”") }
            }
            sign.meaning != null -> append(": ").append(sign.meaning)
        }
        append('.')
        sign.hint?.let { append(' ').append(it) }
    }

    // ---- numbers ---------------------------------------------------------------------------

    private fun seenSign(code: String): Boolean = course.signFor(code)?.let { seen(it.id) } == true

    /** A random number led by [sign], using only numeral signs the learner has met. */
    private fun numberFor(sign: SignItem): Pair<Int, List<String>>? {
        val unit = Numerals.valueOf(sign.code) ?: return null
        var value = 0
        val used = mutableListOf<String>()
        for ((code, v) in Numerals.SIGNS) {
            if (v > unit) continue
            if (code != sign.code && !seenSign(code)) continue
            val count = if (v == unit) rng.nextInt(1, 10) else rng.nextInt(0, 10)
            if (count > 0) {
                value += count * v
                used += code
            }
        }
        return value to used
    }

    private fun creditsFor(codes: List<String>): List<String> = codes.mapNotNull { course.signFor(it)?.id }

    private fun readNumber(sign: SignItem): ReadNumber? {
        val (value, used) = numberFor(sign) ?: return null
        val mdc = Numerals.spell(value) ?: return null
        return ReadNumber(sign.id, mdc, value, creditsFor(used))
    }

    private fun buildNumber(sign: SignItem): BuildNumber? {
        val (value, used) = numberFor(sign) ?: return null
        val unit = Numerals.valueOf(sign.code)!!
        val offered = Numerals.SIGNS.filter { (code, v) -> v <= unit && (code == sign.code || seenSign(code)) }
        return BuildNumber(sign.id, value, offered, creditsFor(used))
    }

    // ---- words -----------------------------------------------------------------------------

    private fun signsOf(mdc: String): List<String> = Mdc.parse(mdc).signs

    private fun wordWeight(target: WordItem, other: WordItem, hard: Boolean): Double {
        var w = 1.0
        if (seen(other.id)) w *= 3.0
        if (other.pos == target.pos) w *= 2.0
        if (hard && signsOf(other.mdc).any { it in signsOf(target.mdc) }) w *= 3.0
        w *= 1.0 + 2.0 * confusion(target.id, other.id)
        return w
    }

    private fun glossKeys(word: WordItem): Set<String> = (listOf(word.gloss) + word.accepts).map { it.lowercase() }.toSet()

    private fun distinctWords(
        word: WordItem,
        hard: Boolean,
        exclude: (WordItem) -> Boolean = { false },
        key: (WordItem) -> String,
    ): List<WordItem> {
        val pool = course.words.filter { it.id != word.id && !exclude(it) }
        val taken = mutableSetOf(key(word))
        taken += glossKeys(word)
        val out = mutableListOf<WordItem>()
        for (candidate in pick(pool, pool.size) { wordWeight(word, it, hard) }) {
            val k = key(candidate)
            if (k in taken || candidate.gloss.lowercase() in glossKeys(word)) continue
            taken += k
            out += candidate
            if (out.size == 3) break
        }
        return out
    }

    private fun wordToMeaning(word: WordItem, showTranslit: Boolean): MultipleChoice? {
        // Words written alike — nb "lord" and nb "every" — cannot be told apart by their spelling.
        val distractors = distinctWords(word, hard = !showTranslit, exclude = { it.mdc == word.mdc }) { it.gloss.lowercase() }
        if (distractors.size < 3) return null
        val (choices, correct) = shuffleWithAnswer(
            Choice(label = word.gloss, itemId = word.id),
            distractors.map { Choice(label = it.gloss, itemId = it.id) },
        )
        return MultipleChoice(
            itemId = word.id,
            kind = ExerciseKind.WORD_TO_MEANING,
            instruction = "What does this word mean?",
            prompt = Display(mdc = word.mdc, translit = word.translit.takeIf { showTranslit }),
            choices = choices,
            correct = correct,
            explain = wordExplanation(word),
        )
    }

    private fun meaningToWord(word: WordItem, hard: Boolean): MultipleChoice? {
        val distractors = distinctWords(word, hard) { it.mdc }
        if (distractors.size < 3) return null
        val (choices, correct) = shuffleWithAnswer(
            Choice(mdc = word.mdc, itemId = word.id),
            distractors.map { Choice(mdc = it.mdc, itemId = it.id) },
        )
        return MultipleChoice(
            itemId = word.id,
            kind = ExerciseKind.MEANING_TO_WORD,
            instruction = "Which of these means “${word.gloss}”?",
            prompt = Display(text = word.gloss),
            choices = choices,
            correct = correct,
            explain = wordExplanation(word),
        )
    }

    private fun listenToWord(word: WordItem, hard: Boolean): MultipleChoice? {
        // Homophones are indistinguishable by ear: kꜣ "ka" and kꜣ "ox" cannot be told apart here.
        val distractors = distinctWords(word, hard, exclude = { Pronounce.arabic(it.translit) == Pronounce.arabic(word.translit) }) { it.mdc }
        if (distractors.size < 3) return null
        val (choices, correct) = shuffleWithAnswer(
            Choice(mdc = word.mdc, itemId = word.id),
            distractors.map { Choice(mdc = it.mdc, itemId = it.id) },
        )
        return MultipleChoice(
            itemId = word.id,
            kind = ExerciseKind.LISTEN_TO_WORD,
            instruction = "Which word do you hear?",
            prompt = Display(audio = word.id),
            choices = choices,
            correct = correct,
            explain = wordExplanation(word),
        )
    }

    /** Letters that make good wrong tiles for [letters]: their own family first. */
    private fun distractorLetters(letters: List<Char>, n: Int): List<Char> {
        val inWord = letters.toSet()
        val family = Translit.ALPHABET.filter { c -> c !in inWord && letters.any { Translit.family(it) == Translit.family(c) } && c != 'z' }
        val others = Translit.ALPHABET.filter { it !in inWord && it !in family && it != 'z' }
        return (family.shuffled(rng).take(n) + others.shuffled(rng)).take(n)
    }

    private fun transliterate(word: WordItem): Arrange? {
        val letters = Translit.letters(word.translit)
        if (letters.size < 2) return null
        val extra = distractorLetters(letters, if (letters.size <= 3) 3 else 4)
        val tiles = (letters + extra).shuffled(rng).map { Choice(translit = it.toString()) }
        return Arrange(
            itemId = word.id,
            kind = ExerciseKind.TRANSLITERATE,
            instruction = "Transliterate this word",
            prompt = Display(mdc = word.mdc, text = word.gloss),
            tiles = tiles,
            answers = listOf(letters.map { it.toString() }),
            solution = Display(translit = word.translit),
            explain = wordExplanation(word),
        )
    }

    private fun determinative(word: WordItem): MultipleChoice? {
        val det = word.determinative ?: return null
        val detSign = course.signFor(det) ?: return null
        val base = word.mdc.removeSuffix("-$det").takeIf { it != word.mdc } ?: return null
        val pool = course.signs.filter { it.role == SignRole.DETERMINATIVE && it.code != det }
        val distractors = pick(pool, 3) { other ->
            (if (seen(other.id)) 3.0 else 1.0) * (1.0 + 2.0 * confusion(detSign.id, other.id))
        }
        if (distractors.size < 3) return null
        val (choices, correct) = shuffleWithAnswer(
            Choice(mdc = det, itemId = detSign.id),
            distractors.map { Choice(mdc = it.code, itemId = it.id) },
        )
        return MultipleChoice(
            itemId = word.id,
            kind = ExerciseKind.DETERMINATIVE,
            instruction = "Which determinative completes “${word.gloss}”?",
            prompt = Display(mdc = base, translit = word.translit),
            choices = choices,
            correct = correct,
            explain = "${detSign.name.replaceFirstChar { it.uppercase() }} marks ${detSign.meaning ?: "this kind of word"}. " +
                wordExplanation(word),
            credits = listOf(word.id, detSign.id),
        )
    }

    private fun spell(word: WordItem): Spell? {
        val signs = signsOf(word.mdc)
        if (signs.size < 2) return null
        val wordSigns = signs.toSet()
        val wordItems = wordSigns.mapNotNull { course.signFor(it) }
        val pool = course.signs.filter { it.code !in wordSigns && it.role != SignRole.NUMERAL }
        val extra = pick(pool, if (signs.size <= 3) 3 else 4) { other ->
            var w = if (seen(other.id)) 3.0 else 0.5
            if (wordItems.any { lookalike(it, other) }) w *= 5.0
            if (wordItems.any { mine ->
                    mine.sounds.isNotEmpty() && other.sounds.isNotEmpty() &&
                        Translit.family(mine.sounds[0][0]) == Translit.family(other.sounds[0][0])
                }
            ) w *= 3.0
            w
        }.map { it.code }
        return Spell(
            itemId = word.id,
            instruction = "Spell “${word.gloss}” in hieroglyphs",
            prompt = Display(text = word.gloss, translit = word.translit, audio = word.id),
            palette = (wordSigns.toList() + extra).shuffled(rng),
            answers = listOf(signs) + word.variants.map(::signsOf),
            mdc = word.mdc,
            determinative = word.determinative,
        )
    }

    fun signShort(code: String): String {
        val s = course.signFor(code) ?: return code
        return when {
            s.role == SignRole.DETERMINATIVE -> "${s.name} (determinative: ${s.meaning})"
            s.sounds.isNotEmpty() -> "${s.name} ${Translit.display(s.sounds[0])}"
            s.meaning != null -> "${s.name} “${s.meaning}”"
            else -> s.name
        }
    }

    fun breakdown(mdc: String): String = signsOf(mdc).joinToString(" + ") { signShort(it) }

    fun wordExplanation(word: WordItem): String =
        "${Translit.display(word.translit)} “${word.gloss}” is written ${breakdown(word.mdc)}."

    /**
     * Sound out a word that has never been taught, from signs that have.
     *
     * The most non-drill exercise in the course: it asks for transfer, not recall, and it is
     * exactly how hieroglyphs were deciphered — Champollion sounded out cartouches sign by sign.
     */
    fun soundOut(word: WordItem): MultipleChoice? {
        val signs = signsOf(word.mdc).mapNotNull { course.signFor(it) }
        val phonetic = signs.filter { it.sounds.isNotEmpty() }
        if (phonetic.isEmpty()) return null
        // Every option as bare letters: punctuation on the right answer alone would give it away.
        val letters = Translit.letters(word.translit)
        if (letters.size < 2 || !readsInOrder(course, word)) return null
        val answer = letters.joinToString("")
        val wrong = linkedSetOf<String>()
        repeat(40) {
            if (wrong.size >= 3) return@repeat
            val variant = letters.toMutableList()
            when (rng.nextInt(3)) {
                0 -> {
                    // Swap one letter for a sibling: ḥ for ḫ, s for š.
                    val i = rng.nextInt(variant.size)
                    val siblings = Translit.ALPHABET.filter { it != variant[i] && it != 'z' && Translit.family(it) == Translit.family(variant[i]) }
                    if (siblings.isNotEmpty()) variant[i] = siblings.random(rng)
                }
                1 -> if (variant.size >= 2) {
                    val i = rng.nextInt(variant.size - 1)
                    val t = variant[i]; variant[i] = variant[i + 1]; variant[i + 1] = t
                }
                else -> {
                    val i = rng.nextInt(variant.size)
                    variant[i] = Translit.ALPHABET.filter { it != 'z' }.random(rng)
                }
            }
            val candidate = variant.joinToString("")
            if (Translit.normalize(candidate) != Translit.normalize(answer)) wrong += candidate
        }
        if (wrong.size < 3) return null
        val (choices, correct) = shuffleWithAnswer(
            Choice(translit = answer, itemId = word.id),
            wrong.take(3).map { Choice(translit = it) },
        )
        return MultipleChoice(
            itemId = word.id,
            kind = ExerciseKind.SOUND_OUT,
            instruction = "A word you haven't learned — sound it out",
            prompt = Display(mdc = word.mdc),
            choices = choices,
            correct = correct,
            explain = "${breakdown(word.mdc)} → ${Translit.display(word.translit)}, “${word.gloss}”.",
            credits = phonetic.map { it.id }.distinct(),
            tallies = listOf(TALLY_SOUNDED_OUT, soundedOut(word.id)),
        )
    }

    // ---- phrases ---------------------------------------------------------------------------

    private fun phraseDisplay(phrase: PhraseItem, translit: Boolean) = Display(
        words = phrase.tokens.map { it.mdc },
        translit = phrase.translit.takeIf { translit },
        audio = phrase.id,
    )

    private fun phraseToMeaning(phrase: PhraseItem, showTranslit: Boolean): MultipleChoice? {
        val pool = course.items.filterIsInstance<PhraseItem>().filter {
            it.id != phrase.id && it.translation != phrase.translation
        }
        val distractors = pick(pool, 3) { other -> if (seen(other.id)) 3.0 else 1.0 }
        if (distractors.size < 3) return null
        val (choices, correct) = shuffleWithAnswer(
            Choice(label = phrase.translation, itemId = phrase.id),
            distractors.map { Choice(label = it.translation, itemId = it.id) },
        )
        return MultipleChoice(
            itemId = phrase.id,
            kind = ExerciseKind.PHRASE_TO_MEANING,
            instruction = "What does this say?",
            prompt = phraseDisplay(phrase, showTranslit),
            choices = choices,
            correct = correct,
            explain = phraseExplanation(phrase),
        )
    }

    fun phraseExplanation(phrase: PhraseItem): String = buildString {
        append(phrase.tokens.joinToString("  ") { "${Translit.display(it.translit)} “${it.gloss}”" })
        phrase.literal?.let { append(". Literally: ").append(it) }
        append('.')
    }

    private fun arrangeTranslation(phrase: PhraseItem): Arrange {
        val own = phrase.tiles.map { it.lowercase() }.toSet()
        val pool = course.items.filterIsInstance<PhraseItem>()
            .filter { it.id != phrase.id }
            .flatMap { it.tiles }
            .plus(course.words.map { it.gloss })
            .filter { it.lowercase() !in own && it.split(' ').size <= 3 }
            .distinct()
        val extra = pool.shuffled(rng).take(if (phrase.tiles.size <= 4) 2 else 3)
        return Arrange(
            itemId = phrase.id,
            kind = ExerciseKind.ARRANGE_TRANSLATION,
            instruction = "Translate this",
            prompt = phraseDisplay(phrase, translit = true),
            tiles = (phrase.tiles + extra).shuffled(rng).map { Choice(label = it) },
            answers = listOf(phrase.tiles) + phrase.tileOrders,
            solution = Display(text = phrase.translation),
            explain = phraseExplanation(phrase),
        )
    }

    private fun arrangeGlyphs(phrase: PhraseItem): Arrange? {
        if (phrase.tokens.size < 2) return null
        val own = phrase.tokens.map { it.mdc }.toSet()
        val extra = course.words.filter { it.mdc !in own && seen(it.id) }.shuffled(rng).take(2)
        val tiles = phrase.tokens.map { Choice(mdc = it.mdc, translit = it.translit) } +
            extra.map { Choice(mdc = it.mdc, translit = it.translit, itemId = it.id) }
        return Arrange(
            itemId = phrase.id,
            kind = ExerciseKind.ARRANGE_GLYPHS,
            instruction = "Write this in hieroglyphs",
            prompt = Display(text = phrase.translation, audio = phrase.id),
            tiles = tiles.shuffled(rng),
            answers = listOf(phrase.tokens.map { it.mdc }),
            solution = phraseDisplay(phrase, translit = true),
            explain = phraseExplanation(phrase),
        )
    }

    // ---- concepts and pairs ----------------------------------------------------------------

    private fun conceptQuiz(concept: ConceptItem): MultipleChoice {
        val (choices, correct) = shuffleWithAnswer(
            Choice(label = concept.answer, itemId = concept.id),
            concept.wrong.map { Choice(label = it) },
        )
        return MultipleChoice(
            itemId = concept.id,
            kind = ExerciseKind.CONCEPT_QUIZ,
            instruction = concept.question,
            prompt = Display(mdc = concept.mdc, mirrored = concept.mirrored),
            choices = choices,
            correct = correct,
            explain = concept.explain,
        )
    }

    /** Four or five quick matches, all signs or all words — a fast review of what is fresh. */
    fun pairs(candidates: List<Item>): Pairs? {
        val signs = candidates.filterIsInstance<SignItem>().filter { it.sounds.isNotEmpty() && it.role != SignRole.NUMERAL }
            .distinctBy { soundKey(it.sounds[0]) }
        val words = candidates.filterIsInstance<WordItem>().distinctBy { it.gloss.lowercase() }
        val (left, right, ids) = when {
            signs.size >= 4 -> {
                val chosen = signs.shuffled(rng).take(5)
                Triple(
                    chosen.map { Choice(mdc = it.code, itemId = it.id) },
                    chosen.map { Choice(translit = it.sounds[0], itemId = it.id) },
                    chosen.map { it.id },
                )
            }
            words.size >= 4 -> {
                val chosen = words.shuffled(rng).take(4)
                Triple(
                    chosen.map { Choice(mdc = it.mdc, itemId = it.id) },
                    chosen.map { Choice(label = it.gloss, itemId = it.id) },
                    chosen.map { it.id },
                )
            }
            else -> return null
        }
        val order = right.indices.shuffled(rng)
        val shuffledRight = order.map { right[it] }
        val match = left.indices.map { order.indexOf(it) }
        return Pairs(itemId = ids.first(), left = left, right = shuffledRight, match = match, credits = ids)
    }

    companion object {
        const val TALLY_H_FAMILY = "h-family"
        const val TALLY_SOUNDED_OUT = "sounded-out"

        /** How often one word has been sounded out, so the next stretch can be a different word. */
        fun soundedOut(wordId: String) = "$TALLY_SOUNDED_OUT:$wordId"
    }
}

/**
 * True if reading [word]'s signs aloud in written order gives its transliteration.
 *
 * Walks the signs, matching each sign's sound at the current position; a sign whose sound was
 * just spelled is a phonetic complement and is skipped, as are silent determinatives. Words
 * written out of order — Tutankhamun, with Amun written first out of respect — fail, which keeps
 * them out of sound-it-out exercises, where they would be unfair.
 */
internal fun readsInOrder(course: Course, word: WordItem): Boolean {
    val target = Translit.normalize(word.translit)
    var at = 0
    var spelled = ""
    for (code in Mdc.parse(word.mdc).signs) {
        val sign = course.signFor(code) ?: return false
        val sound = sign.sounds.firstOrNull()?.let(Translit::normalize) ?: continue
        when {
            target.startsWith(sound, at) -> {
                at += sound.length
                spelled = target.substring(0, at)
            }
            spelled.contains(sound) -> Unit
            else -> return false
        }
    }
    return at == target.length
}

/** Distractor weighting reads confusions recorded in [Progress]; exposed for tests. */
internal fun Progress.confusionWeight(a: String, b: String): Double = 1.0 + 2.0 * confusionCount(a, b)
