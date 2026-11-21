package com.krbavcic.pentalingo.engine

import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.model.Item
import com.krbavcic.pentalingo.model.PhraseItem
import com.krbavcic.pentalingo.model.Pos
import com.krbavcic.pentalingo.model.SignItem
import com.krbavcic.pentalingo.model.SignRole
import com.krbavcic.pentalingo.model.SignTint
import com.krbavcic.pentalingo.model.Token
import com.krbavcic.pentalingo.model.WordItem
import com.krbavcic.pentalingo.script.Mdc
import com.krbavcic.pentalingo.script.PlacedSign
import com.krbavcic.pentalingo.script.Translit

/**
 * Each sign of a spelling by what it does in sound, in written order: read as the sign usually
 * is, read otherwise (the throne as ws in Osiris), repeating a sound (a complement), classifying
 * the word (a determinative), or not read at all (a stroke).
 */
fun soundTints(course: Course, translit: String, mdc: String): List<SignTint> =
    readingsInWord(course, translit, mdc).map { soundTint(course, it) }

/** What one sign does in sound, from its reading in its word. */
fun soundTint(course: Course, r: SignReading): SignTint {
    val sign = course.signFor(r.code)
    return when (r) {
        is SignReading.Reads -> {
            val usual = sign?.let { (it.sounds.firstOrNull() ?: it.reading)?.let(Translit::normalize) }
            if (usual == null || usual == r.value) SignTint.READ else SignTint.READ_OTHER
        }
        is SignReading.Repeats -> SignTint.ECHO
        is SignReading.Silent -> if (r.classifies) SignTint.DETERMINATIVE else SignTint.SILENT
        // Written out of order (the king first, out of respect) or not matched: still a read sign.
        is SignReading.Unexplained -> SignTint.READ
    }
}

fun soundTints(course: Course, word: WordItem): List<SignTint> = readingsInWord(course, word).map { soundTint(course, it) }

private fun tintOf(pos: Pos): SignTint = when (pos) {
    Pos.NOUN, Pos.EXPRESSION -> SignTint.NOUN
    Pos.VERB -> SignTint.VERB
    Pos.ADJECTIVE -> SignTint.ADJECTIVE
    Pos.PREPOSITION, Pos.PARTICLE -> SignTint.LITTLE_WORD
    Pos.PRONOUN -> SignTint.PRONOUN
    Pos.NAME -> SignTint.NAME
}

/**
 * Each sign of one word of a sentence by what kind of word it belongs to. The word's own spelling
 * takes its part of speech (determinatives included: they belong to the word); what the sentence
 * adds after it is an ending (.n of the past, a feminine t), except a suffix pronoun at the end,
 * which is a pronoun like the others.
 */
fun grammarTints(course: Course, token: Token): List<SignTint> {
    val codes = Mdc.parse(token.mdc).signs
    val word = token.wordId?.let { course[it] } as? WordItem ?: return codes.map { SignTint.NOUN }
    val stem = tintOf(word.pos)
    val out = MutableList(codes.size) { stem }
    // The suffix pronoun at the end, spelled as the course spells it on its own (=sn: folded cloth, ripple, strokes).
    val suffix = token.translit.substringAfterLast('=', "").takeIf { '=' in token.translit }
    var suffixStart = codes.size
    if (suffix != null) {
        course.words.firstOrNull { it.translit == "=$suffix" }?.let { pronoun ->
            val spelled = Mdc.parse(pronoun.mdc).signs
            if (spelled.isNotEmpty() && spelled.size < codes.size && codes.takeLast(spelled.size) == spelled) {
                suffixStart = codes.size - spelled.size
            }
        }
        for (i in suffixStart until codes.size) out[i] = SignTint.PRONOUN
    }
    // Between the word's own spelling and the suffix: endings.
    val stemLength = (listOf(word.mdc) + word.variants).map { Mdc.parse(it).signs }
        .filter { it.size <= suffixStart && codes.subList(0, it.size) == it }
        .maxOfOrNull { it.size }
    if (stemLength != null) for (i in stemLength until suffixStart) out[i] = SignTint.ENDING
    return out
}

/** A sign drawn on its own, with no word around it: a determinative classifies, any other sign is read. */
fun signTint(sign: SignItem): SignTint =
    if (sign.role == SignRole.DETERMINATIVE) SignTint.DETERMINATIVE else SignTint.READ

/** Signs drawn one by one (a palette to spell with, a spelling being built): each by its own role. */
fun loneTints(course: Course): (PlacedSign) -> SignTint? = { s -> course.signFor(s.glyph.code)?.let(::signTint) }

/**
 * What each sign of an item does, to draw the item in the learner's colours wherever it appears: a
 * word's signs by sound, a sentence's by the kind of word they belong to, a sign by its own role.
 *
 * [mdc] or [words] is what is actually drawn. A word drawn short of its determinative (the
 * determinative question) keeps the roles of the signs it shows; a drawing that is not the item's
 * own spelling gets no roles at all, rather than wrong ones.
 */
fun itemTints(course: Course, item: Item?, mdc: String? = null, words: List<String> = emptyList()): (PlacedSign) -> SignTint? {
    val none: (PlacedSign) -> SignTint? = { null }
    return when (item) {
        is WordItem -> {
            val own = Mdc.parse(item.mdc).signs
            val drawn = Mdc.parse(mdc ?: words.singleOrNull() ?: item.mdc).signs
            if (drawn.size > own.size || own.subList(0, drawn.size) != drawn) return none
            val tints = soundTints(course, item)
            val byIndex: (PlacedSign) -> SignTint? = { s -> tints.getOrNull(s.index) }
            byIndex
        }
        is PhraseItem -> {
            if (mdc != null || (words.isNotEmpty() && words != item.tokens.map { it.mdc })) return none
            val tints = LineTints.grammar(course, item.tokens)
            val byPlace: (PlacedSign) -> SignTint? = { s -> tints.at(s.token, s.index) }
            byPlace
        }
        is SignItem -> loneTints(course)
        else -> none
    }
}

/**
 * The tint of a sign drawn in a line of several words, from its place in the whole line: signs are
 * numbered across the line, word after word, in the order their spellings list them.
 */
class LineTints(private val perToken: List<List<SignTint>>) {
    private val offsets = perToken.runningFold(0) { at, t -> at + t.size }

    fun at(token: Int, index: Int): SignTint? = perToken.getOrNull(token)?.getOrNull(index - offsets[token])

    companion object {
        fun grammar(course: Course, tokens: List<Token>) = LineTints(tokens.map { grammarTints(course, it) })
        fun sound(course: Course, tokens: List<Token>) = LineTints(tokens.map { soundTints(course, it.translit, it.mdc) })
    }
}
