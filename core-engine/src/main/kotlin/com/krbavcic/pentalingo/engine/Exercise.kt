package com.krbavcic.pentalingo.engine

/**
 * The kinds of exercise, each with the tier of difficulty it represents.
 *
 * Tiers climb from recognising (pick the sound of a sign you are shown) through recalling (hear
 * a word, find its spelling) to producing (spell a word sign by sign). An item moves up the
 * ladder as its memory strengthens, so a learner meets harder questions about things they know
 * well instead of the same easy question forever.
 */
enum class ExerciseKind(val tier: Int) {
    TEACH(0),
    SIGN_TO_SOUND(1),
    SIGN_TO_MEANING(1),
    SOUND_TO_SIGN(2),
    MEANING_TO_SIGN(2),
    LISTEN_TO_SIGN(2),
    WORD_TO_MEANING(1),
    MEANING_TO_WORD(2),
    LISTEN_TO_WORD(2),
    SOUND_OUT(2),
    TRANSLITERATE(3),
    DETERMINATIVE(3),
    SPELL(4),
    PHRASE_TO_MEANING(2),
    ARRANGE_TRANSLATION(3),
    ARRANGE_GLYPHS(4),
    READ_NUMBER(2),
    BUILD_NUMBER(3),
    CONCEPT_QUIZ(1),
    PAIRS(1),
}

/**
 * What an exercise shows as its question.
 *
 * @property audio the id of the item whose clip plays, if any.
 * @property highlight index of a sign to mark in [mdc], in reading order.
 */
data class Display(
    val mdc: String? = null,
    val translit: String? = null,
    val text: String? = null,
    val audio: String? = null,
    val mirrored: Boolean = false,
    val highlight: Int? = null,
    /** Several words shown as one line, each with its own spelling. */
    val words: List<String> = emptyList(),
)

/** An option or tile. The first of [mdc], [translit] or [label] that is set is the face; the rest annotate. */
data class Choice(
    val label: String? = null,
    val mdc: String? = null,
    val translit: String? = null,
    /** The item this option stands for, so a wrong pick can be recorded as a confusion. */
    val itemId: String? = null,
    val audio: String? = null,
) {
    /** What the tile shows, and so what an [Answer.Tiles] built from it says. */
    val face: String get() = mdc ?: translit ?: label.orEmpty()
}

sealed interface Exercise {
    /** The item the exercise is about. */
    val itemId: String
    val kind: ExerciseKind
    val instruction: String

    /** Items whose memory this exercise reviews. Usually just [itemId]. */
    val credits: List<String> get() = listOf(itemId)

    val tier: Int get() = kind.tier

    /**
     * Whether tapping a sign may reveal what it is. Not where the sign *is* the question —
     * revealing the sound of the one sign on screen would simply be the answer.
     */
    val hintable: Boolean get() = kind in HINTABLE
}

private val HINTABLE = setOf(
    ExerciseKind.WORD_TO_MEANING, ExerciseKind.TRANSLITERATE, ExerciseKind.PHRASE_TO_MEANING,
    ExerciseKind.ARRANGE_TRANSLATION, ExerciseKind.READ_NUMBER,
)

/** A card that teaches; not graded. */
data class Teach(override val itemId: String, val firstTime: Boolean) : Exercise {
    override val kind get() = ExerciseKind.TEACH
    override val instruction get() = if (firstTime) "New" else "Remember this?"
}

/** The finale card of a reading lesson: the whole text, now readable. */
data class ReadText(override val itemId: String, val textId: String) : Exercise {
    override val kind get() = ExerciseKind.TEACH
    override val instruction get() = "Read it"
    override val credits get() = emptyList<String>()
}

data class MultipleChoice(
    override val itemId: String,
    override val kind: ExerciseKind,
    override val instruction: String,
    val prompt: Display,
    val choices: List<Choice>,
    val correct: Int,
    /** Why the answer is the answer, shown after answering either way. */
    val explain: String? = null,
    override val credits: List<String> = listOf(itemId),
    /** Tallies to bump on a correct answer, for awards. */
    val tallies: List<String> = emptyList(),
) : Exercise

/**
 * Build an answer from tiles: transliteration letters, English words or hieroglyphic words.
 *
 * @property answers acceptable sequences of tile *faces*, not indices — duplicate tiles are
 *   interchangeable, so tapping either of two identical "the" tiles is right.
 */
data class Arrange(
    override val itemId: String,
    override val kind: ExerciseKind,
    override val instruction: String,
    val prompt: Display,
    val tiles: List<Choice>,
    val answers: List<List<String>>,
    val solution: Display,
    val explain: String? = null,
) : Exercise

/** Spell a word by choosing its signs in order from a palette. */
data class Spell(
    override val itemId: String,
    override val instruction: String,
    val prompt: Display,
    val palette: List<String>,
    val answers: List<List<String>>,
    /** The spelling as it should be shown once solved. */
    val mdc: String,
    val determinative: String?,
) : Exercise {
    override val kind get() = ExerciseKind.SPELL
}

/** Read a number written in hieroglyphs, typed on a keypad. */
data class ReadNumber(
    override val itemId: String,
    val mdc: String,
    val value: Int,
    override val credits: List<String>,
) : Exercise {
    override val kind get() = ExerciseKind.READ_NUMBER
    override val instruction get() = "What number is this?"
}

/** Write a number by tapping numeral signs. */
data class BuildNumber(
    override val itemId: String,
    val value: Int,
    /** Numeral signs on offer, as Gardiner code to value, largest first. */
    val numerals: List<Pair<String, Int>>,
    override val credits: List<String>,
) : Exercise {
    override val kind get() = ExerciseKind.BUILD_NUMBER
    override val instruction get() = "Write this number in hieroglyphs"
}

/** Match left to right; [match] maps each left index to its right index. */
data class Pairs(
    override val itemId: String,
    val left: List<Choice>,
    val right: List<Choice>,
    val match: List<Int>,
    override val credits: List<String>,
) : Exercise {
    override val kind get() = ExerciseKind.PAIRS
    override val instruction get() = "Match the pairs"
}

/**
 * The answer a learner who knows everything would give.
 *
 * Used by the tests that walk every lesson of the course, to prove each generated exercise has
 * an answer the checker accepts — an unanswerable question is the worst bug a course can ship.
 */
fun Exercise.solution(): Answer = when (this) {
    is Teach, is ReadText -> Answer.Seen
    is MultipleChoice -> Answer.Pick(correct)
    is Arrange -> {
        // Tap tiles as a learner would, each once, so an answer the tiles cannot build fails here
        // rather than on every phone.
        val unused = tiles.indices.toMutableList()
        Answer.Tiles(
            answers.first().map { want ->
                val i = unused.first { tiles[it].face == want }
                unused.remove(i)
                tiles[i].face
            },
        )
    }
    is Spell -> Answer.Signs(answers.first())
    is ReadNumber -> Answer.Value(value)
    is BuildNumber -> Answer.Signs(
        com.krbavcic.pentalingo.script.Mdc.parse(Numerals.spell(value)!!).signs,
    )
    is Pairs -> Answer.Matched(emptySet())
}

sealed interface Answer {
    data class Pick(val index: Int) : Answer
    data class Tiles(val faces: List<String>) : Answer
    data class Signs(val codes: List<String>) : Answer
    data class Value(val value: Int) : Answer

    /** Pairs report which items were ever matched wrongly on the way to finishing. */
    data class Matched(val missed: Set<String>) : Answer
    data object Seen : Answer
}

enum class Outcome { CORRECT, NEAR, WRONG }

/**
 * The result of an answer.
 *
 * @property note the explanation: what was right, what the mistake was, why. Never just "wrong".
 * @property confusedWith the item the learner seems to have mistaken this one for.
 */
data class Verdict(
    val outcome: Outcome,
    val solution: String? = null,
    val note: String? = null,
    val confusedWith: String? = null,
    /** When the answer is hieroglyphs, its spelling, so it can be drawn rather than named. */
    val solutionMdc: String? = null,
)
