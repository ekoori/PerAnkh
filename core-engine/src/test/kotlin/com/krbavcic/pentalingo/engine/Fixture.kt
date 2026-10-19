package com.krbavcic.pentalingo.engine

import com.krbavcic.pentalingo.model.Award
import com.krbavcic.pentalingo.model.ConceptItem
import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.model.CourseUnit
import com.krbavcic.pentalingo.model.ItemKind
import com.krbavcic.pentalingo.model.Lesson
import com.krbavcic.pentalingo.model.PhraseItem
import com.krbavcic.pentalingo.model.Pos
import com.krbavcic.pentalingo.model.ReadingText
import com.krbavcic.pentalingo.model.Requirement
import com.krbavcic.pentalingo.model.SignItem
import com.krbavcic.pentalingo.model.SignRole
import com.krbavcic.pentalingo.model.Token
import com.krbavcic.pentalingo.model.UnitColor
import com.krbavcic.pentalingo.model.WordItem

/** A small course with every kind of item, so engine tests do not depend on the real one. */
object Fixture {

    private fun uni(code: String, name: String, sound: String, vararg lookalikes: String) = SignItem(
        id = "s:$code", code = code, name = name, role = SignRole.UNILITERAL, sounds = listOf(sound),
        // Voiced as the real course voices them: h and ḥ alike, as an English speaker hears them.
        say = sound.replace("ḥ", "h").replace("ḫ", "kh") + "e", lookalikes = lookalikes.toList(),
    )

    val signs = listOf(
        uni("G1", "vulture", "ꜣ", "G17", "G43"),
        uni("M17", "reed leaf", "j"),
        uni("D36", "forearm", "ꜥ"),
        uni("G43", "quail chick", "w", "G1"),
        uni("D58", "foot", "b"),
        uni("Q3", "stool", "p"),
        uni("I9", "horned viper", "f"),
        uni("G17", "owl", "m", "G1"),
        uni("N35", "water", "n"),
        uni("D21", "mouth", "r"),
        uni("O4", "shelter", "h"),
        uni("V28", "twisted flax", "ḥ"),
        uni("Aa1", "placenta", "ḫ"),
        uni("X1", "bread loaf", "t"),
        SignItem("s:F35", "F35", "heart and windpipe", SignRole.TRILITERAL, listOf("nfr"), meaning = "good"),
        SignItem("s:A1", "A1", "seated man", SignRole.DETERMINATIVE, meaning = "man, male person"),
        SignItem("s:B1", "B1", "seated woman", SignRole.DETERMINATIVE, meaning = "woman, female person"),
        SignItem("s:A40", "A40", "seated god", SignRole.DETERMINATIVE, meaning = "god"),
        SignItem("s:N1", "N1", "sky", SignRole.DETERMINATIVE, meaning = "sky, above"),
        SignItem("s:Z1", "Z1", "stroke", SignRole.NUMERAL, meaning = "one"),
        SignItem("s:V20", "V20", "hobble", SignRole.NUMERAL, meaning = "ten"),
    )

    val words = listOf(
        WordItem("w:nfr", "nfr", "F35-I9:D21", "good", Pos.ADJECTIVE, "nefer", accepts = listOf("beautiful")),
        WordItem("w:rn", "rn", "D21:N35", "name", Pos.NOUN, "ren"),
        WordItem("w:pt", "pt", "Q3:X1-N1", "sky", Pos.NOUN, "pet", determinative = "N1"),
        WordItem("w:jt", "jt", "M17-X1:I9-A1", "father", Pos.NOUN, "it", determinative = "A1"),
        WordItem("w:ptH", "ptḥ", "Q3:X1-V28-A40", "Ptah", Pos.NAME, "ptah", determinative = "A40"),
        WordItem("w:Hna", "ḥnꜥ", "V28-N35:D36", "with", Pos.PREPOSITION, "hena"),
        WordItem("w:m", "m", "G17", "in", Pos.PREPOSITION, "em"),
        WordItem("w:jn", "jn", "M17:N35", "by", Pos.PREPOSITION, "in"),
    )

    val phrases = listOf(
        PhraseItem(
            "p:rnf", listOf(Token("D21:N35", "rn", "name", "w:rn"), Token("I9", "=f", "his")),
            "his name", listOf("his", "name"), "renef",
        ),
        PhraseItem(
            "p:nfrrn", listOf(Token("F35-I9:D21", "nfr", "good", "w:nfr"), Token("D21:N35", "rn", "name", "w:rn")),
            "the name is good", listOf("the", "name", "is", "good"), "nefer ren",
        ),
        PhraseItem(
            "p:mpt", listOf(Token("G17", "m", "in", "w:m"), Token("Q3:X1-N1", "pt", "sky", "w:pt")),
            "in the sky", listOf("in", "the", "sky"), "em pet",
        ),
        PhraseItem(
            "p:jnptH", listOf(Token("M17:N35", "jn", "by", "w:jn"), Token("Q3:X1-V28-A40", "ptḥ", "Ptah", "w:ptH")),
            "by Ptah", listOf("by", "Ptah"), "in ptah",
        ),
    )

    val concept = ConceptItem(
        "c:direction", "Which way to read", "Signs face the start of the line.",
        "Which way do you read a line whose birds face left?", "Left to right",
        listOf("Right to left", "Top to bottom", "Either way"), "Read towards the faces.",
        mdc = "G17-G1",
    )

    val lesson1 = Lesson(
        "l1", "First signs", "Six signs", "G17",
        teach = listOf("s:G17", "s:N35", "s:D21", "s:Q3", "s:X1", "s:I9"),
    )
    val lesson2 = Lesson(
        "l2", "More signs", "Six more", "V28",
        teach = listOf("s:G1", "s:M17", "s:D36", "s:G43", "s:O4", "s:V28", "s:Aa1", "c:direction"),
        practice = listOf("s:G17"),
    )
    val lesson3 = Lesson(
        "l3", "Words", "Words", "F35",
        teach = listOf("s:F35", "s:A1", "s:N1", "w:nfr", "w:rn", "w:pt", "w:jt"),
        practice = listOf("s:I9", "s:D21"),
    )
    val lesson4 = Lesson(
        "l4", "Phrases", "Phrases", "A40",
        teach = listOf("s:B1", "s:A40", "w:m", "w:jn", "w:ptH", "w:Hna", "p:rnf", "p:nfrrn", "p:mpt", "p:jnptH"),
        reading = "t:demo",
    )
    val lesson5 = Lesson(
        "l5", "Numbers", "Counting", "V20",
        teach = listOf("s:Z1", "s:V20"),
    )
    val checkpoint = Lesson("l6", "Checkpoint", "Everything", "F35", checkpoint = true)

    val text = ReadingText("t:demo", "Demo", "Fixture", listOf("p:rnf", "p:mpt"), "A test text.")

    val awards = listOf(
        Award("a:first", "First", "tp", "G17", "Know three signs", "Lore",
            Requirement.Know(listOf("s:G17", "s:N35", "s:D21"))),
        Award("a:read", "Reader", "šdj", "Y1", "Read the demo", "Lore", Requirement.ReadText("t:demo")),
        Award("a:days", "Days", "hrw", "N5", "Study on 2 days", "Lore", Requirement.StudyDays(2)),
        Award("a:words", "Words", "mdw", "A2", "Know 3 words", "Lore", Requirement.KnowKind(ItemKind.WORD, 3)),
    )

    val course = Course(
        units = listOf(
            CourseUnit("u1", "Unit", "Sub", UnitColor.LAPIS, "Intro",
                listOf(lesson1, lesson2, lesson3, lesson4, lesson5, checkpoint)),
        ),
        items = signs + words + phrases + concept,
        texts = listOf(text),
        awards = awards,
    )
}
