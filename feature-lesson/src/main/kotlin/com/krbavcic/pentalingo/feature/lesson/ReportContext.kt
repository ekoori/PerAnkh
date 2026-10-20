package com.krbavcic.pentalingo.feature.lesson

import com.krbavcic.pentalingo.core.ui.Voice
import com.krbavcic.pentalingo.core.ui.describeItem
import com.krbavcic.pentalingo.engine.Answer
import com.krbavcic.pentalingo.engine.Arrange
import com.krbavcic.pentalingo.engine.BuildNumber
import com.krbavcic.pentalingo.engine.Choice
import com.krbavcic.pentalingo.engine.Display
import com.krbavcic.pentalingo.engine.Exercise
import com.krbavcic.pentalingo.engine.MultipleChoice
import com.krbavcic.pentalingo.engine.Pairs
import com.krbavcic.pentalingo.engine.ReadNumber
import com.krbavcic.pentalingo.engine.ReadText
import com.krbavcic.pentalingo.engine.Session
import com.krbavcic.pentalingo.engine.Spell
import com.krbavcic.pentalingo.engine.Teach
import com.krbavcic.pentalingo.engine.Verdict

/**
 * Everything on a lesson page, flattened to strings for a tester report: the lesson, the
 * exercise with its prompt, options and accepted answers, the item behind it (spelling,
 * transliteration, how it is said, which clip plays), what the tester answered and what the
 * checker said. Enough to find the data or the rule at fault without reproducing the lesson.
 */
internal fun lessonReport(
    session: Session,
    exercise: Exercise?,
    verdict: Verdict?,
    pending: Answer?,
    step: Int,
    voice: Voice,
): Map<String, String> {
    val course = session.course
    val out = linkedMapOf<String, String>()
    fun put(key: String, value: String?) {
        if (!value.isNullOrBlank()) out[key] = value
    }
    put("screen", if (session.lesson != null) "lesson" else "review")
    session.lesson?.let { put("lesson", "${it.id} · ${it.title}") }
    // Pages are counted from 1: the first page is shown after one advance.
    put("step", "${step.coerceAtLeast(1)}")
    if (exercise == null) return out
    put("exercise", exercise.kind.name)
    put("instruction", exercise.instruction)
    out += describeItem(course, exercise.itemId, voice)

    fun choice(c: Choice): String = listOfNotNull(
        c.label, c.mdc?.let { "[$it]" }, c.translit, c.itemId?.let { "($it)" },
        c.audio?.let { "clip ${voice.fileFor(it) ?: "none"}" },
    ).joinToString(" ")

    fun display(key: String, d: Display) {
        put("$key.mdc", d.mdc)
        put("$key.translit", d.translit)
        put("$key.text", d.text)
        if (d.words.isNotEmpty()) put("$key.words", d.words.joinToString(" | "))
        d.audio?.let { put("$key.audio", "$it, clip ${voice.fileFor(it) ?: "none"}") }
        d.highlight?.let { put("$key.highlight", "$it") }
        if (d.mirrored) put("$key.mirrored", "yes")
    }

    when (exercise) {
        is Teach -> put("teach", if (exercise.firstTime) "first time" else "again")
        is ReadText -> course.text(exercise.textId)?.let { text ->
            put("text", "${text.id} · ${text.title}")
            put("text.source", text.source)
            put("text.lines", text.lines.joinToString(" / "))
        }
        is MultipleChoice -> {
            display("prompt", exercise.prompt)
            exercise.choices.forEachIndexed { i, c -> put("choice.${i + 1}", choice(c) + if (i == exercise.correct) "  [correct]" else "") }
            put("explain", exercise.explain)
            if (exercise.credits != listOf(exercise.itemId)) put("credits", exercise.credits.joinToString(", "))
        }
        is Arrange -> {
            display("prompt", exercise.prompt)
            put("tiles", exercise.tiles.joinToString(" | ") { choice(it) })
            exercise.answers.forEachIndexed { i, a -> put("accepts.${i + 1}", a.joinToString(" ")) }
            display("solution", exercise.solution)
            put("explain", exercise.explain)
        }
        is Spell -> {
            display("prompt", exercise.prompt)
            put("palette", exercise.palette.joinToString(" "))
            exercise.answers.forEachIndexed { i, a -> put("accepts.${i + 1}", a.joinToString(" ")) }
            put("spelling", exercise.mdc)
            put("determinative", exercise.determinative)
        }
        is ReadNumber -> {
            put("number.mdc", exercise.mdc)
            put("number.value", "${exercise.value}")
        }
        is BuildNumber -> {
            put("number.value", "${exercise.value}")
            put("numerals", exercise.numerals.joinToString(" ") { (code, value) -> "$code=$value" })
        }
        is Pairs -> exercise.left.forEachIndexed { i, left ->
            put("pair.${i + 1}", "${choice(left)}  <->  ${exercise.right.getOrNull(exercise.match[i])?.let(::choice)}")
        }
    }

    pending?.let { answer ->
        put("answer", when (answer) {
            is Answer.Pick -> (exercise as? MultipleChoice)?.choices?.getOrNull(answer.index)?.let { "${answer.index + 1}: ${choice(it)}" }
                ?: "${answer.index + 1}"
            is Answer.Tiles -> answer.faces.joinToString(" ")
            is Answer.Signs -> answer.codes.joinToString(" ")
            is Answer.Value -> "${answer.value}"
            is Answer.Matched -> "missed ${answer.missed.joinToString(", ").ifEmpty { "none" }}"
            Answer.Seen -> "seen"
        })
    }
    verdict?.let { v ->
        put("verdict", v.outcome.name)
        put("verdict.solution", v.solution)
        put("verdict.solutionMdc", v.solutionMdc)
        put("verdict.note", v.note)
        v.confusedWith?.let { other -> out += describeItem(course, other, voice, "confusedWith") }
    }
    return out
}
