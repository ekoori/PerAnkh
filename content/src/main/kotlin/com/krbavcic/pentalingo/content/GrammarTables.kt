package com.krbavcic.pentalingo.content

import com.krbavcic.pentalingo.model.GrammarTable
import com.krbavcic.pentalingo.model.GrammarTable.Cell

/**
 * The cards' tables: each pattern, or each set of things a card names, at a glance, shown with the
 * card in its lesson and kept in the codex. Spellings are the course's own (the words' entries and
 * the forms its sentences write), and a form the course does not spell is given in transliteration
 * only. Every Egyptian form can be heard: tapped, it plays the recording of the word, sign or
 * sentence that says it, or its own (`ExportSpeech.tableForms`). Paradigms follow Allen's Middle
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

    // Cards whose words or things come side by side read better as a table too: script notes,
    // grammar notes and culture cards alike.
    "c:sounds" to GrammarTable(
        listOf("Signs", "Letter", "Said like"),
        SIGNS_SAID_MEANING,
        listOf(
            listOf("O4", "h", "the h of “house”"),
            listOf("V28", "ḥ", "an h breathed hard from the throat"),
            listOf("Aa1", "ḫ", "the ch of Scottish “loch”"),
            listOf("F32", "ẖ", "a softer ch, as in German “ich”"),
        ),
        "Written, they are four letters: the marks under the h tell them apart.",
    ),
    "c:complements" to paradigm(
        listOf("Y5:N35", "mn", "the game board spells mn; the water ripple repeats its n"),
        listOf("M17-Y5:N35-A40", "jmn", "Amun: the reed j, then mn with its n, then the god"),
        listOf("", "nfr", "next lesson: the heart and windpipe spell nfr; f and r repeat its end"),
        header = "What repeats",
        note = "Read the sound once: the repeated signs only confirm it.",
    ),
    "c:kemet" to paradigm(
        listOf("I6-G17-X1:O49", "kmt", "the Black Land: Egypt, dark with the silt of the flood"),
        listOf("", "dšrt", "the Red Land: the desert around it"),
        note = "kmt is km, “black”, written with a piece of crocodile skin; the town sign marks a place.",
    ),
    "c:judgement" to paradigm(
        listOf("M17-N35:Q3-G43-E16", "jnpw", "Anubis, who worked the scales"),
        listOf("G26-X1:Z4-A40", "ḏḥwtj", "Thoth, who wrote down the result"),
        listOf("H6-X1:H8-C10", "mꜣꜥt", "Maat, truth: her feather against the heart"),
        listOf("", "ꜥm-mwt", "Ammit, “devourer of the dead”, who swallowed a heavy heart"),
        header = "At the weighing",
    ),
    "c:soul" to paradigm(
        listOf("D28-Z1", "kꜣ", "the life force, born with you and fed by offerings after death"),
        listOf("G29-Z1", "bꜣ", "the personality, drawn as a bird with a human head, free to leave the tomb by day"),
        listOf("G25-Aa1", "ꜣḫ", "the shining spirit of one who passed the judgement"),
        header = "What it was",
        note = "Raised arms write the ka, the jabiru stork the ba, the crested ibis the akh.",
    ),
    "c:participle" to paradigm(
        listOf("F21-G17", "sḏm", "one who hears, from sḏm, “hear”"),
        listOf("D4-D21", "jrr", "one who does, from jrj, “do”"),
        listOf("U7:D21-D21-A2", "mrr", "one who loves, from mrj, “love”"),
        note = "mrr sḏm pw jrr ḏd.t, “one who loves to listen is one who does what is said”.",
    ),
    "c:story" to paradigm(
        listOf("P6-D36:N35", "ꜥḥꜥ.n", "then: it moves the story on"),
        listOf("I10:D46-N35-I9", "ḏd.n=f", "he said: the past, with .n"),
        listOf("I10:D46-M17-N35-I9", "ḏd.jn=f", "then he said: .jn on the verb does both"),
        note = "ꜥḥꜥ.n ḏd.n=f and ḏd.jn=f both mean “then he said”.",
    ),
    "c:future" to paradigm(
        listOf("", "jw dpt r jj.t", "a ship will come: “a ship is towards coming”"),
        listOf("N40-G17-D54-V31", "šm=k", "you will go: the plain verb can promise too"),
    ),
    "c:bodywords" to paradigm(
        listOf("D2:Z1", "ḥr", "face; and on, upon"),
        listOf("D1:Z1", "tp", "head; and upon"),
        listOf("D36:Z1", "ꜥ", "arm; and m-ꜥ, “from”"),
        listOf("D21:Z1", "r", "mouth; and r, “to”"),
        header = "The body, and the little word",
        note = "Each little word began as a part of the body.",
    ),
    "c:relative" to paradigm(
        listOf("W14-O34-O34-X1", "ḥzzt", "what is praised: ḥzzt nswt, “what the king praises”"),
        listOf("U7:D21-D21-X1", "mrrt", "what is loved: mrrt rmṯ, “what people love”"),
        note = "The verb, a -t for the thing, then whoever does it.",
    ),
    "c:justified" to paradigm(
        listOf("Aa11:P8", "mꜣꜥ-ḫrw", "true of voice: said of a man"),
        listOf("Aa11:X1-P8", "mꜣꜥt-ḫrw", "true of voice: said of a woman"),
        listOf("F31-S29-N35", "msj.n", "born of: Ky msj.n Mrtj, “Ky, born of Merti”"),
        note = "Titles, the name, then mꜣꜥ-ḫrw: the way a stela names its owner.",
    ),
    "c:wishes" to paradigm(
        listOf("X8-I9", "dj=f", "may he give: a verb and its subject can wish"),
        listOf("M17-Aa1-Y1", "jḫ", "may…: it makes the wish plain, jḫ ḏd=sn, “may they say”"),
        listOf("F31-S29-X1-D37", "ms.tw", "may (they) be held out: .tw makes it passive"),
    ),
    "c:causative" to paradigm(
        listOf("S29-D21-G17-D9", "srmj", "to make weep, from rmj, “weep”"),
        listOf("S29-V28-N29-D21-G37", "sḥqr", "to starve, from ḥqr, “hungry”"),
        listOf("S29-S34-N35:Aa1", "sꜥnḫ", "to keep alive, from ꜥnḫ, “live”"),
        listOf("S29-L1-D21", "sḫpr", "to bring about, from ḫpr, “come about”"),
        note = "The folded cloth s in front makes the word cause it.",
    ),
    "c:sdmtf" to paradigm(
        listOf("M18-M17-X1-D54-I9", "jj.t=f", "n jj.t=f, “before it came”"),
        listOf("L1-D21-X1-I9", "ḫpr.t=f", "n ḫpr.t=f, “before it happened”"),
        note = "n, then the verb with .t and its subject.",
    ),
    "c:myrrh" to paradigm(
        listOf("D36-N35-G4-W23-Z2", "ꜥntjw", "myrrh"),
        listOf("", "sntr", "incense"),
        listOf("", "tꜣ-nṯr", "god’s land: what Punt was called"),
    ),
    "c:jwtj" to paradigm(
        listOf("N35-X1-Z4", "ntj", "who, which"),
        listOf("D35-X1-G43-G37", "jwtj", "who does not, which does not"),
        note = "wḫꜣ jwtj sḏm=f, “the fool who does not listen”.",
    ),
    "c:admire" to paradigm(
        listOf("F35-I9:D21-G43-Z4", "nfr.wj", "how good!"),
        listOf("D21-N37-D19-A2-G43-Z4", "rš.wj", "how happy!"),
        note = "Add .wj to an adjective; what is admired follows it.",
    ),
    "c:article" to paradigm(
        listOf("G41-G1", "pꜣ", "this, becoming “the”: pꜣ š, “the lake”"),
        listOf("G41-G1-M17-M17-S29", "pꜣy=s", "her, before its noun: pꜣy=s rmn, “her side”"),
        listOf("X1-G1-M17-M17-N35:Z2", "tꜣy=n", "our, before a feminine noun: tꜣy=n štyt, “our lead rower”"),
        note = "Later Egyptian, and Coptic after it, use these everywhere.",
    ),
)
