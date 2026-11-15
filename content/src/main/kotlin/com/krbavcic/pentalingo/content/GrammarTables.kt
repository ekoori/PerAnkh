package com.krbavcic.pentalingo.content

import com.krbavcic.pentalingo.model.GrammarTable
import com.krbavcic.pentalingo.model.GrammarTable.Cell

/**
 * The grammar cards' tables: each pattern at a glance, shown at the start of the lesson that
 * teaches it and kept in the codex. Spellings are the course's own (the words' entries), and a form
 * the course does not spell is given in transliteration only. Paradigms follow Allen's Middle
 * Egyptian and Gardiner's Egyptian Grammar, in the course's transliteration (=ṯ, not =t).
 */
private val SIGNS_SAID_MEANING = listOf(Cell.GLYPHS, Cell.TRANSLIT, Cell.TEXT)

private fun paradigm(vararg rows: List<String>, note: String? = null, header: String = "Meaning") =
    GrammarTable(listOf("Signs", "Egyptian", header), SIGNS_SAID_MEANING, rows.toList(), note)

private val SINGULAR_SUFFIXES = listOf(
    listOf("A1", "=j", "I, me, my"),
    listOf("V31", "=k", "you, your — to a man"),
    listOf("V13", "=ṯ", "you, your — to a woman"),
    listOf("I9", "=f", "he, him, his"),
    listOf("S29", "=s", "she, her"),
)

/** The numeral signs in the course's readings, smallest first. */
private val NUMERALS = listOf(
    listOf("Z1", "wꜥ", "1"),
    listOf("V20", "mḏ", "10"),
    listOf("V1", "št", "100"),
    listOf("M12", "ḫꜣ", "1,000"),
    listOf("D50", "ḏbꜥ", "10,000"),
    listOf("I8", "ḥfn", "100,000"),
    listOf("C11", "ḥḥ", "1,000,000"),
)

internal val GRAMMAR_TABLES: Map<String, GrammarTable> = mapOf(
    "c:suffix" to paradigm(
        *SINGULAR_SUFFIXES.toTypedArray(),
        note = "After a noun they say whose: rn=f “his name”. After a verb, who: sḏm=f “he hears”.",
    ),
    "c:weyou" to paradigm(
        *SINGULAR_SUFFIXES.toTypedArray(),
        listOf("N35:Z2", "=n", "we, us, our"),
        listOf("V13-N35:Z2", "=ṯn", "you, your — to several"),
        listOf("S29-N35:Z2", "=sn", "they, them, their"),
        listOf("X1-G43", "=tw", "one, someone"),
        note = "The plurals add the water ripple and three strokes.",
    ),
    "c:pronouns" to paradigm(
        listOf("M17-N35:V31-A1", "jnk", "I"),
        listOf("N35:X1-V31", "ntk", "you — to a man"),
        listOf("N35:X1-V13", "ntṯ", "you — to a woman"),
        listOf("N35:X1-I9", "ntf", "he"),
        listOf("N35:X1-S29", "nts", "she"),
        listOf("", "jnn", "we"),
        listOf("", "ntṯn", "you — to several"),
        listOf("", "ntsn", "they"),
        note = "They head a sentence: jnk sꜣ=k, “I am your son”. After nt- comes the suffix: nt + =f is ntf.",
    ),
    "c:dependent" to paradigm(
        listOf("G43-A1", "wj", "me"),
        listOf("V13-G43", "ṯw", "you — a man"),
        listOf("V13-N35", "ṯn", "you — a woman"),
        listOf("M23-G43", "sw", "him, it"),
        listOf("S29-Z4", "sj", "her, it"),
        listOf("N35:Z2", "n", "us"),
        listOf("V13-N35", "ṯn", "you — several"),
        listOf("S29-N35:Z2", "sn", "them"),
        listOf("S29-X1", "st", "it, them"),
        note = "They follow a verb as its object, and words like mk, “look”: sḏm=f wj, “he hears me”.",
    ),
    "c:sdmf" to paradigm(
        listOf("F21-G17-A1", "sḏm=j", "I hear"),
        listOf("F21-G17-V31", "sḏm=k", "you hear — a man"),
        listOf("F21-G17-V13", "sḏm=ṯ", "you hear — a woman"),
        listOf("F21-G17-I9", "sḏm=f", "he hears"),
        listOf("F21-G17-S29", "sḏm=s", "she hears"),
        listOf("F21-G17-N35:Z2", "sḏm=n", "we hear"),
        listOf("F21-G17-V13-N35:Z2", "sḏm=ṯn", "you hear — several"),
        listOf("F21-G17-S29-N35:Z2", "sḏm=sn", "they hear"),
        note = "With a noun instead of a pronoun, the noun follows the verb: sḏm sꜣ, “the son hears”.",
    ),
    "c:sdmnf" to paradigm(
        listOf("F21-G17-N35-A1", "sḏm.n=j", "I heard"),
        listOf("F21-G17-N35:V31", "sḏm.n=k", "you heard"),
        listOf("F21-G17-N35:I9", "sḏm.n=f", "he heard"),
        listOf("F21-G17-N35:S29", "sḏm.n=s", "she heard"),
        listOf("F21-G17-N35-N35:Z2", "sḏm.n=n", "we heard"),
        listOf("F21-G17-N35-S29-N35:Z2", "sḏm.n=sn", "they heard"),
        note = "The n comes between the verb and its subject: jr.n=j, “I did”.",
    ),
    "c:negation" to paradigm(
        listOf("D35-F21-G17-I9", "n sḏm=f", "he did not hear"),
        listOf("D35-F21-G17-N35:I9", "n sḏm.n=f", "he cannot hear"),
        listOf("D35:N35-F21-G17-I9", "nn sḏm=f", "he will not hear"),
        listOf("", "nn + a noun", "there is no …"),
        note = "n with the past form means “cannot”: back to front, it seems, but that is how it works.",
    ),
    "c:imperative" to paradigm(
        listOf("F21-G17", "sḏm", "listen!"),
        listOf("", "jr mꜣꜥt", "do what is right!"),
        listOf("", "m šm", "don’t go!"),
        note = "The bare verb gives an order; m, “do not”, forbids.",
    ),
    "c:stative" to GrammarTable(
        listOf("Ending", "Egyptian", "Meaning"),
        listOf(Cell.TRANSLIT, Cell.TRANSLIT, Cell.TEXT),
        listOf(
            listOf(".kw", "jj.kw", "I have come"),
            listOf(".tj", "jj.tj", "you have come"),
            listOf("—", "jj", "he has come"),
            listOf(".tj", "jj.tj", "she has come"),
            listOf(".wjn", "jj.wjn", "we have come"),
            listOf(".w", "jj.w", "they have come"),
        ),
        "How things stand once something has happened. For “he” there is usually no ending at all.",
    ),
    "c:gender" to GrammarTable(
        listOf("", "Masculine", "Feminine"),
        listOf(Cell.TEXT, Cell.TRANSLIT, Cell.TRANSLIT),
        listOf(
            listOf("one", "sn", "snt"),
            listOf("two", "snwj", "sntj"),
            listOf("many", "snw", "snwt"),
        ),
        "sn “brother”, snt “sister”: the feminine adds t. Two strokes or a doubled sign often write a pair, three strokes many.",
    ),
    "c:numbers" to paradigm(
        *NUMERALS.take(3).toTypedArray(),
        header = "Value",
        note = "Repeat a sign for each unit, largest first: three coils and two hobbles are 320.",
    ),
    "c:heh" to paradigm(
        *NUMERALS.toTypedArray(),
        header = "Value",
        note = "Each sign is ten of the one before. The god Heh, a million, also means “countless”.",
    ),
    "c:titulary" to GrammarTable(
        listOf("Name", "Egyptian", "What it says"),
        listOf(Cell.TEXT, Cell.TRANSLIT, Cell.TEXT),
        listOf(
            listOf("Horus name", "ḥr", "the king as Horus, the falcon"),
            listOf("Two Ladies name", "nbtj", "under the vulture and the cobra, goddesses of Upper and Lower Egypt"),
            listOf("Golden Horus name", "ḥr nbw", "Horus of gold"),
            listOf("Throne name", "nswt-bjtj", "King of Upper and Lower Egypt — in a cartouche"),
            listOf("Birth name", "sꜣ rꜥ", "Son of Ra — in a cartouche"),
        ),
        "The two in cartouches are the names you meet most.",
    ),
    "c:calendar" to GrammarTable(
        listOf("Season", "Egyptian", "Named for"),
        listOf(Cell.TEXT, Cell.TRANSLIT, Cell.TEXT),
        listOf(
            listOf("first", "ꜣḫt", "the flood"),
            listOf("second", "prt", "“coming forth”, when the fields rose from the water"),
            listOf("third", "šmw", "the harvest"),
        ),
        "Four months of thirty days each, and five extra days: 365.",
    ),
    "c:genitive" to GrammarTable(
        listOf("Of", "After", "Example"),
        listOf(Cell.TRANSLIT, Cell.TEXT, Cell.TEXT),
        listOf(
            listOf("—", "a close bond", "nb tꜣwj, “lord of the Two Lands”"),
            listOf("n", "a masculine noun", "pr n jt=f, “the house of his father”"),
            listOf("nt", "a feminine noun", "st nt snḏm, “a place of resting”"),
            listOf("nw", "a plural noun", "nṯrw nw pt, “the gods of the sky”"),
        ),
        "n agrees with the noun before it, as an adjective would.",
    ),
    "c:hrsdm" to GrammarTable(
        listOf("Egyptian", "Meaning", "Word for word"),
        listOf(Cell.TRANSLIT, Cell.TEXT, Cell.TEXT),
        listOf(
            listOf("jw=j ḥr sḏm", "I am listening", "I am on hearing"),
            listOf("jw dpt r jj.t", "a ship will come", "a ship is towards coming"),
            listOf("sḏm.n=f", "he heard", "heard-he"),
            listOf("jj.kw", "I have come", "come-I"),
        ),
        "What is happening, what will happen, what happened, and how things stand.",
    ),
)
