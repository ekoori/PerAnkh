package com.krbavcic.pentalingo.model

/**
 * Everything the course can teach is an item, and every item has a memory of its own.
 *
 * Signs, words and phrases are separate items on purpose. Knowing that the owl says "m" and
 * knowing that `jmn` is Amun are different memories that fade at different rates, and a learner
 * who can read every sign of a word may still not know what it means.
 */
sealed interface Item {
    val id: String
    val kind: ItemKind
}

enum class ItemKind { SIGN, WORD, PHRASE, CONCEPT }

/** What a sign does in writing. A sign can do several things; this is its main job in the course. */
enum class SignRole {
    /** Spells one consonant: the "alphabet". */
    UNILITERAL,

    /** Spells two consonants at once. */
    BILITERAL,

    /** Spells three. */
    TRILITERAL,

    /** Stands for a whole word by picture. */
    LOGOGRAM,

    /** Unpronounced; tells the reader what kind of word came before it. */
    DETERMINATIVE,

    NUMERAL,
}

/**
 * The traditional pigment a sign was painted in on polychrome monuments.
 *
 * Signs are drawn in plain ink until they are learned, then in their paint. The colours follow
 * Egyptian practice loosely — water and sky blue, plants green, flesh red-brown, the sun red —
 * because a sign list that fills with the colours of a tomb wall is a better record of progress
 * than a number.
 */
enum class Paint { CARNELIAN, OCHRE, LAPIS, TURQUOISE, MALACHITE, GOLD, INK }

data class SignItem(
    override val id: String,
    val code: String,
    /** What the picture shows: "owl", "twisted flax". */
    val name: String,
    val role: SignRole,
    /** Sound values in canonical transliteration; empty for pure determinatives. */
    val sounds: List<String> = emptyList(),
    /** For logograms and determinatives: what the sign means or classifies. */
    val meaning: String? = null,
    /** What the voice says when this sign's sound is played: an Egyptological reading like "meh". */
    val say: String? = null,
    /** A memory hook. */
    val hint: String? = null,
    /** History, usage, culture. */
    val note: String? = null,
    val paint: Paint = Paint.INK,
    /** Signs easily mistaken for this one by eye; offered side by side once it is known. */
    val lookalikes: List<String> = emptyList(),
) : Item {
    override val kind get() = ItemKind.SIGN
}

enum class Pos { NOUN, VERB, ADJECTIVE, PREPOSITION, PRONOUN, NAME, PARTICLE, EXPRESSION }

data class WordItem(
    override val id: String,
    val translit: String,
    /** The standard spelling, in Manuel de Codage. */
    val mdc: String,
    /** The meaning shown and asked for. */
    val gloss: String,
    val pos: Pos,
    /** Egyptological pronunciation, as the voice should say it: "nefer". */
    val say: String,
    /** Other renderings accepted as correct in typed or tile answers. */
    val accepts: List<String> = emptyList(),
    val note: String? = null,
    /** Gardiner code of the determinative, when the spelling ends in one. */
    val determinative: String? = null,
    /** Other correct spellings, for spelling exercises. */
    val variants: List<String> = emptyList(),
) : Item {
    override val kind get() = ItemKind.WORD
}

/**
 * One word of a phrase or text, spelled as it appears there.
 *
 * Phrases spell their own tokens because inflected forms are spelled differently from the
 * dictionary form: `rn=f` "his name" is `rn` plus the viper. [wordId] links back to the lexeme so
 * knowing the word counts towards reading the phrase.
 */
data class Token(
    val mdc: String,
    val translit: String,
    val gloss: String,
    val wordId: String? = null,
)

data class PhraseItem(
    override val id: String,
    val tokens: List<Token>,
    /** An idiomatic translation. */
    val translation: String,
    /** The translation cut into tiles, in order, for the arrange exercise. */
    val tiles: List<String>,
    val say: String,
    /** A word-for-word rendering that shows the grammar. */
    val literal: String? = null,
    val note: String? = null,
    /** Other orderings of [tiles] that are also good English. */
    val tileOrders: List<List<String>> = emptyList(),
) : Item {
    override val kind get() = ItemKind.PHRASE
    val translit: String get() = tokens.joinToString(" ") { it.translit }
}

/**
 * A piece of grammar or culture: a card to read, then one question that proves it was read.
 */
data class ConceptItem(
    override val id: String,
    val title: String,
    val body: String,
    val question: String,
    val answer: String,
    val wrong: List<String>,
    /** Shown after answering either way: the reason, not just the fact. */
    val explain: String,
    /** An inscription illustrating the idea. */
    val mdc: String? = null,
    /** Draw the illustration right-to-left. */
    val mirrored: Boolean = false,
) : Item {
    override val kind get() = ItemKind.CONCEPT
}
