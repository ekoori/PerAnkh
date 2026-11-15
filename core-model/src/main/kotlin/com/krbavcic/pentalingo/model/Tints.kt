package com.krbavcic.pentalingo.model

/**
 * What a sign is doing where it stands, for colouring it: how it is read in its word, or what kind
 * of word it belongs to in a sentence. Painting every sign in its own tomb colour said nothing
 * about reading (reported, Nov 2026); these say something.
 */
enum class SignTint {
    // ---- in a word, by sound
    /** Spells sounds, as the sign usually does. */
    READ,
    /** Spells sounds it does not usually spell: the throne as ws in Osiris. */
    READ_OTHER,
    /** Repeats a sound a sign beside it spells: a phonetic complement. */
    ECHO,
    /** Classifies the word and is not read. */
    DETERMINATIVE,
    /** A stroke or another sign that is not read. */
    SILENT,

    // ---- in a sentence, by kind of word
    NOUN,
    VERB,
    ADJECTIVE,
    /** Prepositions and particles. */
    LITTLE_WORD,
    PRONOUN,
    NAME,
    /** An ending on a word: the .n of the past, a feminine t, a plural w. */
    ENDING,
}

/** How hieroglyphs are coloured outside the codex: by what each sign does, by its tomb paint, or not at all. */
enum class GlyphColours { BY_ROLE, PAINT, INK }
