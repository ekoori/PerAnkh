package com.krbavcic.pentalingo.engine

import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.model.SignItem
import com.krbavcic.pentalingo.script.Translit

/**
 * Grades answers, and — the part that matters — says why.
 *
 * Three outcomes, not two. A NEAR answer is one that shows the learner knows the thing but
 * slipped on a detail worth naming: ḥ written as h, the right signs in the wrong order, a
 * spelling missing only its determinative, ten strokes where a scribe would write a hobble.
 * Those cost nothing but a note; treating them as plain wrong would teach the learner that the
 * app cannot tell a slip from ignorance.
 */
class Checker(private val course: Course) {

    fun check(exercise: Exercise, answer: Answer): Verdict = when (exercise) {
        is Teach, is ReadText -> Verdict(Outcome.CORRECT)
        is MultipleChoice -> choice(exercise, answer as Answer.Pick)
        is Arrange -> arrange(exercise, answer as Answer.Tiles)
        is Spell -> spell(exercise, answer as Answer.Signs)
        is ReadNumber -> readNumber(exercise, answer as Answer.Value)
        is BuildNumber -> buildNumber(exercise, answer as Answer.Signs)
        is Pairs -> pairs(exercise, answer as Answer.Matched)
    }

    private fun choice(ex: MultipleChoice, answer: Answer.Pick): Verdict {
        val right = ex.choices[ex.correct]
        val solution = right.label ?: right.translit?.let(Translit::display) ?: describeGlyphs(right)
        if (answer.index == ex.correct) return Verdict(Outcome.CORRECT, solution, ex.explain, solutionMdc = right.mdc)

        val picked = ex.choices[answer.index]
        val note = buildString {
            describeWrongPick(ex, picked)?.let { append(it).append(' ') }
            ex.explain?.let(::append)
        }.ifBlank { null }
        return Verdict(Outcome.WRONG, solution, note, confusedWith = picked.itemId, solutionMdc = right.mdc)
    }

    /** "the owl" for a sign, the word itself for a spelling. */
    private fun describeGlyphs(choice: Choice): String? {
        val item = choice.itemId?.let(course::get)
        return when (item) {
            is SignItem -> "the ${item.name}"
            is com.krbavcic.pentalingo.model.WordItem -> "${Translit.display(item.translit)}, “${item.gloss}”"
            else -> null
        }
    }

    /** "You picked the placenta, ḫ" — naming the thing chosen makes the contrast teachable. */
    private fun describeWrongPick(ex: MultipleChoice, picked: Choice): String? {
        val other = picked.itemId?.let(course::get) as? SignItem ?: return null
        if (other.id == ex.itemId) return null
        return when {
            other.sounds.isNotEmpty() ->
                "That one is the ${other.name}, ${Translit.display(other.sounds[0])}."
            other.meaning != null -> "That one is the ${other.name}: ${other.meaning}."
            else -> null
        }
    }

    private fun arrange(ex: Arrange, answer: Answer.Tiles): Verdict {
        val solution = ex.solution.text ?: ex.solution.translit?.let(Translit::display)
            ?: ex.answers.first().joinToString(" ")
        if (ex.kind == ExerciseKind.TRANSLITERATE) {
            val expected = ex.answers.first().joinToString("")
            val cmp = Translit.compare(expected, answer.faces.joinToString(""))
            return when (cmp.verdict) {
                Translit.Verdict.EXACT -> Verdict(Outcome.CORRECT, solution, ex.explain)
                Translit.Verdict.NEAR -> Verdict(
                    Outcome.NEAR,
                    solution,
                    cmp.confused.joinToString(" ") { (want, got) ->
                        "Close — ${Translit.display(want.toString())}, not ${Translit.display(got.toString())}."
                    } + (ex.explain?.let { " $it" } ?: ""),
                )
                Translit.Verdict.WRONG -> Verdict(Outcome.WRONG, solution, ex.explain)
            }
        }

        val given = answer.faces.map(::norm)
        if (ex.answers.any { it.map(::norm) == given }) return Verdict(Outcome.CORRECT, solution, ex.explain)
        // Every right tile, nothing extra, wrong order: they understood the words but not the
        // sentence. Worth saying so, and worth a near miss rather than a fail.
        val best = ex.answers.first().map(::norm)
        if (given.sorted() == best.sorted()) {
            return Verdict(Outcome.NEAR, solution, "Right words, different order. " + (ex.explain ?: ""))
        }
        return Verdict(Outcome.WRONG, solution, ex.explain)
    }

    private fun norm(s: String) = s.trim().lowercase().trimEnd('.', ',', '!', '?')

    private fun spell(ex: Spell, answer: Answer.Signs): Verdict {
        val given = answer.codes
        if (ex.answers.any { it == given }) return Verdict(Outcome.CORRECT, null, solutionMdc = ex.mdc)
        val main = ex.answers.first()
        val det = ex.determinative
        if (det != null && main.lastOrNull() == det && given == main.dropLast(1)) {
            val name = course.signFor(det)?.name ?: det
            return Verdict(
                Outcome.NEAR, null,
                "Nearly — it ends with a determinative, the $name, which isn't spoken but tells you what kind of word this is.",
                solutionMdc = ex.mdc,
            )
        }
        if (given.sorted() == main.sorted()) {
            return Verdict(Outcome.NEAR, null, "The right signs, but in a different order.", solutionMdc = ex.mdc)
        }
        return Verdict(Outcome.WRONG, null, "It's written ${describe(main)}.", solutionMdc = ex.mdc)
    }

    private fun describe(codes: List<String>): String = codes.joinToString(", ") { code ->
        val s = course.signFor(code)
        when {
            s == null -> code
            s.sounds.isNotEmpty() -> "${s.name} (${Translit.display(s.sounds[0])})"
            else -> s.name
        }
    }

    private fun readNumber(ex: ReadNumber, answer: Answer.Value): Verdict {
        val solution = Numerals.format(ex.value)
        if (answer.value == ex.value) return Verdict(Outcome.CORRECT, solution)
        return Verdict(Outcome.WRONG, solution, "Add up each sign: ${numberBreakdown(ex.value)}.")
    }

    private fun buildNumber(ex: BuildNumber, answer: Answer.Signs): Verdict {
        val solution = Numerals.format(ex.value)
        val sum = Numerals.sum(answer.codes)
        if (sum != ex.value) {
            return Verdict(
                Outcome.WRONG, solution,
                "That makes ${Numerals.format(sum)}. ${Numerals.format(ex.value)} is ${numberBreakdown(ex.value)}.",
            )
        }
        if (!Numerals.isCanonical(answer.codes)) {
            return Verdict(
                Outcome.NEAR, solution,
                "The right total, but a scribe never wrote a sign ten times — ten of one sign is one of the next.",
            )
        }
        return Verdict(Outcome.CORRECT, solution)
    }

    private fun numberBreakdown(value: Int): String {
        var rest = value
        val parts = mutableListOf<String>()
        for ((code, unit) in Numerals.SIGNS) {
            val count = rest / unit
            rest %= unit
            if (count == 0) continue
            val name = course.signFor(code)?.name ?: code
            parts += "$count × $name (${Numerals.format(unit)})"
        }
        return parts.joinToString(" + ")
    }

    private fun pairs(ex: Pairs, answer: Answer.Matched): Verdict =
        if (answer.missed.isEmpty()) Verdict(Outcome.CORRECT) else Verdict(Outcome.NEAR)
}
