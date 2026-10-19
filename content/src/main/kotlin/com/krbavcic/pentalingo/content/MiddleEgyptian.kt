package com.krbavcic.pentalingo.content

import com.krbavcic.pentalingo.model.Award
import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.model.CourseUnit
import com.krbavcic.pentalingo.model.ItemKind
import com.krbavcic.pentalingo.model.Lesson
import com.krbavcic.pentalingo.model.ReadingText
import com.krbavcic.pentalingo.model.Requirement
import com.krbavcic.pentalingo.model.UnitColor

private fun s(vararg codes: String) = codes.map { "s:$it" }
private fun w(vararg keys: String) = keys.map { "w:$it" }
private fun p(vararg keys: String) = keys.map { "p:$it" }
private fun c(vararg keys: String) = keys.map { "c:$it" }

private val UNITS = listOf(
    CourseUnit(
        "u1", "The Scribe’s Alphabet", "25 signs that spell single sounds", UnitColor.LAPIS,
        "Twenty-five signs each spell one consonant. The Egyptians never used them as an alphabet on their own, but they are the key to everything else — and with them you can already sound out names.",
        listOf(
            Lesson("l1", "First signs", "Owl, water, mouth, stool and bread", "G17",
                teach = s("G17", "N35", "D21", "Q3", "X1") + c("consonants")),
            Lesson("l2", "Birds and reeds", "The vulture, the reed, the arm and the chick", "G1",
                teach = s("G1", "M17", "D36", "G43", "Z4") + c("direction"), practice = s("G17")),
            Lesson("l3", "Body and basket", "Foot, viper, hand, basket, stand and shelter", "D58",
                teach = s("D58", "I9", "D46", "V31", "W11", "O4"), practice = s("X1", "G43")),
            Lesson("l4", "The four h’s", "Telling ḥ, ḫ and ẖ from h", "V28",
                teach = s("V28", "Aa1", "F32") + c("sounds"), practice = s("O4")),
            Lesson("l5", "Snakes, bolts and pools", "The last six letters", "I10",
                teach = s("I10", "V13", "O34", "S29", "N37", "N29"), practice = s("I9", "D46")),
            Lesson("l6", "The scribe’s test", "The whole alphabet, at full difficulty", "Y3",
                teach = s("Y3") + c("scribes"), checkpoint = true),
        ),
    ),
    CourseUnit(
        "u2", "First Words", "Reading by sound, and silent pictures", UnitColor.OCHRE,
        "With the alphabet you can read real words. Many end in a silent picture — a determinative — that tells you what kind of thing the word is.",
        listOf(
            Lesson("l7", "Spelled by sound", "Name, sky, say, with, health", "D21",
                teach = w("rn", "pt") + s("N1") + w("dd", "hna", "snb") + c("determinatives"),
                practice = s("D21", "N35", "Q3", "X1")),
            Lesson("l8", "Pictures that classify", "Man, god, idea and sparrow", "A1",
                teach = s("A1", "A40", "Y1", "G37") + w("jt", "rx", "bjn", "ptH")),
            Lesson("l9", "Day and night", "Time words and the sun", "N5",
                teach = s("N5", "N2") + w("hrw", "grh", "xt")),
            Lesson("l10", "Little words", "In, to, for, by", "G17",
                teach = w("m", "n", "r", "jn") + p("mpt", "ddjnptH")),
            Lesson("l11", "His name", "Pronouns that stick to nouns", "I9",
                teach = c("suffix") + p("rnf", "jtf", "Hnajtf") + s("Z2") + c("plural")),
            Lesson("l12", "Anubis", "The jackal at the cemetery", "E16",
                teach = s("E16") + w("jnpw") + c("anubis"), practice = w("ptH", "jt", "rn")),
            Lesson("l13", "The word test", "Everything so far", "Y1", checkpoint = true),
        ),
    ),
    CourseUnit(
        "u3", "Signs of Two and Three", "Most signs carry several sounds", UnitColor.TURQUOISE,
        "Most hieroglyphs spell two or three consonants at once, and many are also pictures of the word they spell. Alphabet signs often follow to confirm the reading.",
        listOf(
            Lesson("l14", "House and board", "pr, mn and nb", "O1",
                teach = s("O1", "Y5", "V30") + w("pr", "nb", "mn", "jmn") + c("complements")),
            Lesson("l15", "Life and beauty", "ꜥnḫ, nfr and ḥtp", "S34",
                teach = s("S34", "F35", "R4") + w("anx", "nfr", "Htp") + p("hrwnfr"), practice = w("hrw")),
            Lesson("l16", "Gods and sons", "nṯr, sꜣ and wr", "R8",
                teach = s("R8", "G39", "G36", "O29", "B1") + w("nTr", "sA", "sAt", "wr", "aA") + c("gender")),
            Lesson("l17", "Amulets", "Scarab, djed and was", "L1",
                teach = s("L1", "R11", "S40") + w("xpr", "dd2", "wAs")),
            Lesson("l18", "The Black Land", "Egypt, land and town", "I6",
                teach = s("I6", "N16", "N23", "O49") + w("kmt", "tA", "tAwj", "njwt") + c("kemet")),
            Lesson("l19", "The sign test", "Signs of two and three", "S34", checkpoint = true),
        ),
    ),
    CourseUnit(
        "u4", "Gods and People", "Families, gods and the parts of a soul", UnitColor.CARNELIAN,
        "Mother and brother, Osiris and Isis, the ka and the ba: the vocabulary of every tomb wall.",
        listOf(
            Lesson("l20", "Family", "Mother, wife, brother, sister, people", "G14",
                teach = s("G14", "N41", "T22") + w("mwt", "Hmt", "sn", "snt", "z", "rmT")),
            Lesson("l21", "The great gods", "Ra, Osiris, Isis and Horus", "G5",
                teach = s("Q1", "D4", "G5", "H8") + w("ra", "wsjr", "Ast", "Hrw")),
            Lesson("l22", "Thoth and Maat", "Writing, truth and judgement", "G26",
                teach = s("G26", "H6", "C10") + w("DHwtj", "mAat", "Sw") + c("judgement")),
            Lesson("l23", "Sky and storm", "Nut, Seth and Hathor", "W24",
                teach = s("W24", "E20", "O10") + w("nwt", "stx", "HwtHr")),
            Lesson("l24", "Ka, ba and akh", "The parts of a person", "D28",
                teach = s("D28", "G29", "G25", "F34") + w("kA", "bA", "Ax", "jb") + c("soul")),
            Lesson("l25", "The temple test", "Gods and people", "A40", checkpoint = true),
        ),
    ),
    CourseUnit(
        "u5", "The King", "Titles, names and cartouches", UnitColor.GOLD,
        "The king was Horus on earth and carried five names. Two of them were always written inside a cartouche — which is how hieroglyphs were first read again.",
        listOf(
            Lesson("l26", "Sedge and bee", "The words for king", "M23",
                teach = s("M23", "L2", "U36") + w("nswt", "bjtj", "Hmf") + p("nswtbjtj") + c("honorific")),
            Lesson("l27", "Royal titles", "Lord of the Two Lands, Son of Ra", "X8",
                teach = s("U28", "X8", "N17") + w("dj", "wDA", "Dt") + p("nbtAwj", "sAra", "anxwDAsnb", "djanxDt") + c("titulary")),
            Lesson("l28", "Cartouches", "Khufu, Amenemhat, Thutmose, Tutankhamun", "V10",
                teach = s("V10", "F4", "F31") + w("xwfw", "jmnmHAt", "DHwtjms", "twtanxjmn") + c("cartouche", "champollion")),
            Lesson("l29", "Reading royal names", "Four kings, as they were written", "S40",
                teach = p("royal1", "royal2", "royal3", "royal4"), reading = "t:royal"),
            Lesson("l30", "The royal test", "Titles and names", "V10", checkpoint = true),
        ),
    ),
    CourseUnit(
        "u6", "Counting", "Seven signs for every number", UnitColor.MALACHITE,
        "Seven signs write every number up to ten million. Here the exercises never run out: each one is a new number.",
        listOf(
            Lesson("l31", "Strokes, hobbles, coils", "One, ten and a hundred", "V20",
                teach = s("Z1", "V20", "V1") + c("numbers")),
            Lesson("l32", "Lotus to million", "Thousands to millions", "C11",
                teach = s("M12", "D50", "I8", "C11") + c("heh")),
            Lesson("l33", "Seasons and years", "The calendar, and more counting", "M12",
                teach = c("calendar"), practice = s("Z1", "V20", "V1", "M12", "D50", "I8", "C11")),
        ),
    ),
    CourseUnit(
        "u7", "Making Sentences", "No verb “to be”, and adjectives first", UnitColor.LAPIS,
        "Middle Egyptian sentences are short and logical: no verb “to be”, adjectives first, and pronouns glued to the words they belong to.",
        listOf(
            Lesson("l34", "I, you, he", "Independent pronouns", "A1",
                teach = w("jnk", "ntk", "ntf") + c("pronouns") + p("jnksAk", "ntkjtj")),
            Lesson("l35", "In the house", "Saying where things are", "E9",
                teach = s("E9") + w("jw") + c("sentence1") + p("jwfmpr", "jwramtpt")),
            Lesson("l36", "Good is he", "Saying what things are like", "F35",
                teach = w("sw") + c("sentence2") + p("nfrsw", "nfrrnf")),
            Lesson("l37", "Your house, her son", "More suffixes, bread and water", "V31",
                teach = p("prk", "sAs") + s("N35A") + w("mw", "t")),
            Lesson("l38", "The grammar test", "Sentences", "Y1", checkpoint = true),
        ),
    ),
    CourseUnit(
        "u8", "Reading the Stones", "Real texts, as scribes wrote them", UnitColor.OCHRE,
        "An offering formula from a tomb stela, and the first lines of two of the great stories of the Middle Kingdom.",
        listOf(
            Lesson("l39", "Bread and beer", "The menu of eternity", "W22",
                teach = s("O3", "W22", "F1", "H1") + w("prtxrw", "Hnqt", "kA2", "Apd")),
            Lesson("l40", "An offering the king gives", "Reading a funerary stela", "R4",
                teach = s("F39") + w("HtpDjnswt", "Ddw", "jmAxy") + c("offering") + p("offering1", "offering2", "offering3"),
                reading = "t:offering"),
            Lesson("l41", "The Eloquent Peasant", "There was a man…", "E34",
                teach = s("E34", "A2") + w("wn", "wnm", "pw", "xwnjnpw") + c("literature") + p("peasant1"),
                reading = "t:peasant", practice = w("z", "rn")),
            Lesson("l42", "The Shipwrecked Sailor", "May your heart be glad", "T18",
                teach = s("T18") + w("Smsw", "jqr", "HAtja") + p("sailor1"),
                reading = "t:sailor", practice = w("jb", "wDA", "dd")),
            Lesson("l43", "The scribe’s examination", "Everything", "Y3", checkpoint = true),
        ),
    ),
)

private val TEXTS = listOf(
    ReadingText("t:royal", "Royal names", "Titles and cartouches of four kings, Old to New Kingdom",
        p("royal1", "royal2", "royal3", "royal4"),
        "Each king is introduced by a title and named inside a cartouche. Khufu built the Great Pyramid; Amenemhat founded the 12th Dynasty; Tutankhamun and Thutmose belong to the 18th."),
    ReadingText("t:offering", "An offering formula", "A typical Middle Kingdom funerary stela, simplified",
        p("offering1", "offering2", "offering3"),
        "Thousands of stelae carry these words. A real one adds more gods and offerings, and ends with the owner’s name and titles."),
    ReadingText("t:peasant", "The Eloquent Peasant", "Opening line, from Middle Kingdom papyri now in Berlin",
        p("peasant1"),
        "Khun-Anup is robbed on his way to market and pleads his case nine times. His speeches are so fine that the officials delay justice just to hear more."),
    ReadingText("t:sailor", "The Shipwrecked Sailor", "Opening line, Papyrus Hermitage 1115, St Petersburg",
        p("sailor1"),
        "A follower consoles his master after a failed expedition by telling of his own shipwreck on an island ruled by a giant serpent."),
)

private val UNILITERALS = s(
    "G1", "M17", "Z4", "D36", "G43", "D58", "Q3", "I9", "G17", "N35", "D21", "O4", "V28",
    "Aa1", "F32", "O34", "S29", "N37", "N29", "V31", "W11", "X1", "V13", "D46", "I10",
)

private val AWARDS = listOf(
    Award("a:kit", "The Scribe’s Kit", "sš", "Y3",
        "Know six signs — still remembered on a later day",
        "The sign Y3 shows a scribe’s palette, water pot and brush holder, slung together. It writes sš, “scribe” — a title held with pride in a land where few could read.",
        Requirement.Know(s("G17", "N35", "D21", "Q3", "X1", "G1", "M17", "D36", "G43"), 6)),
    Award("a:alphabet", "Scribe of the God’s Words", "sš mdw-nṯr", "R8",
        "Know all 25 alphabet signs well enough to recall them on another day",
        "The Egyptians called hieroglyphs mdw-nṯr, “the god’s words”: Thoth had given them to humankind.",
        Requirement.Know(UNILITERALS)),
    Award("a:wedjat", "The Wedjat Eye", "wḏꜣt", "D10",
        "Pick the right h out of the other h’s twelve times",
        "The eye of Horus, torn out by Seth and healed by Thoth: wḏꜣt, “the sound one”. Its pieces were used to write fractions of a grain measure.",
        Requirement.Tally("h-family", 12)),
    Award("a:champollion", "Champollion’s Insight", "šnw", "V10",
        "Sound out five words you were never taught",
        "In 1822 Champollion read the names Ptolemy and Cleopatra sign by sign. Sounding out a word you have never seen is exactly what he did.",
        Requirement.Tally("sounded-out", 5)),
    Award("a:ankh", "Ankh", "ꜥnḫ", "S34",
        "Know 25 words, remembered across days",
        "Gods hold the ankh to the king’s nose: the breath of life. Twenty-five words are a living vocabulary.",
        Requirement.KnowKind(ItemKind.WORD, 25)),
    Award("a:shen", "The Shen Ring", "šn", "V9",
        "Read the royal names",
        "A loop of rope with no beginning and no end: eternity. The cartouche is a shen ring stretched long enough to hold a name.",
        Requirement.ReadText("t:royal")),
    Award("a:offering", "Hetep-di-nesu", "ḥtp dj nswt", "R4",
        "Read an offering formula",
        "Saying the formula fed the dead. Every stela you can now read is a meal delivered three or four thousand years late.",
        Requirement.ReadText("t:offering")),
    Award("a:heh", "Millions of Years", "ḥḥ", "C11",
        "Know all seven numeral signs, remembered across days",
        "Heh, god of infinity, holds the palm ribs that counted years. Kings asked for “millions of years” of rule — forever.",
        Requirement.Know(s("Z1", "V20", "V1", "M12", "D50", "I8", "C11"))),
    Award("a:scarab", "Kheper", "ḫpr", "L1",
        "Relearn three things you had forgotten until they stick",
        "ḫpr means “to come into being, to transform”. A word you once lost and then mastered has been transformed — mistakes are how this works.",
        Requirement.Recovered(3)),
    Award("a:djed", "The Djed Pillar", "ḏd", "R11",
        "Study on seven different days",
        "The backbone of Osiris, and stability itself. Not seven days in a row — just seven days. Memory grows between visits, not during them.",
        Requirement.StudyDays(7)),
    Award("a:maat", "The Feather of Maat", "mꜣꜥt", "H6",
        "Know 40 things and have nothing fading",
        "Maat is balance: everything in its place. A review queue honestly emptied is a small piece of it.",
        Requirement.ClearedReview(40)),
    Award("a:peasant", "Eloquence", "mdw nfr", "A2",
        "Read the opening of the Eloquent Peasant",
        "The peasant’s speeches were so good the officials kept him talking. You have read his first line in his own language.",
        Requirement.ReadText("t:peasant")),
    Award("a:sailor", "The Ship", "dpt", "P1",
        "Read the opening of the Shipwrecked Sailor",
        "Boats carried everything in Egypt, including the sun. This story is the oldest surviving shipwreck tale in the world.",
        Requirement.ReadText("t:sailor")),
    Award("a:was", "The Was Sceptre", "wꜣs", "S40",
        "Master 50 things",
        "Dominion. Mastered means remembered three weeks after the last review — knowledge that has become yours.",
        Requirement.Master(50)),
    Award("a:thoth", "Thoth’s Ibis", "ḏḥwtj", "G26",
        "Know every sign in the course",
        "Thoth invented writing and recorded the judgement of the dead. Every sign in this course is now yours to read.",
        Requirement.KnowKind(ItemKind.SIGN, SIGNS.size)),
)

/** The Middle Egyptian course. */
val MIDDLE_EGYPTIAN: Course by lazy {
    Course(units = UNITS, items = SIGNS + WORDS + PHRASES + CONCEPTS, texts = TEXTS, awards = AWARDS)
}
