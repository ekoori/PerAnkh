package com.krbavcic.pentalingo.feature.lesson

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.krbavcic.pentalingo.core.ui.GoldBar
import com.krbavcic.pentalingo.core.ui.IconKind
import com.krbavcic.pentalingo.core.ui.PaintButton
import com.krbavcic.pentalingo.core.ui.Palette
import com.krbavcic.pentalingo.core.ui.RoundIconButton
import com.krbavcic.pentalingo.core.ui.Scrim
import com.krbavcic.pentalingo.core.ui.Type
import com.krbavcic.pentalingo.core.ui.papyrus
import com.krbavcic.pentalingo.core.ui.rememberHaptics
import com.krbavcic.pentalingo.core.ui.rememberSfx
import com.krbavcic.pentalingo.core.ui.rememberVoice
import com.krbavcic.pentalingo.engine.Answer
import com.krbavcic.pentalingo.engine.Arrange
import com.krbavcic.pentalingo.engine.BuildNumber
import com.krbavcic.pentalingo.engine.Exercise
import com.krbavcic.pentalingo.engine.MultipleChoice
import com.krbavcic.pentalingo.engine.Outcome
import com.krbavcic.pentalingo.engine.Pairs
import com.krbavcic.pentalingo.engine.ReadNumber
import com.krbavcic.pentalingo.engine.ReadText
import com.krbavcic.pentalingo.engine.Session
import com.krbavcic.pentalingo.engine.SessionResult
import com.krbavcic.pentalingo.engine.Spell
import com.krbavcic.pentalingo.engine.Teach
import com.krbavcic.pentalingo.engine.Verdict
import com.krbavcic.pentalingo.engine.solution
import com.krbavcic.pentalingo.model.UnitColor
import com.krbavcic.pentalingo.script.Translit

/**
 * A lesson or review, one exercise at a time.
 *
 * There are no hearts and no game over. A wrong answer gets an explanation and comes back a few
 * exercises later; the only way to leave a lesson early is to choose to, and even then every
 * answer given so far is kept.
 *
 * @param onLeave called with the partial result when the learner quits.
 * @param onFinish called with the full result after the last exercise.
 */
@Composable
fun LessonScreen(
    session: Session,
    soundEffects: Boolean,
    autoplay: Boolean,
    onLeave: (SessionResult) -> Unit,
    onFinish: (SessionResult) -> Unit,
    modifier: Modifier = Modifier,
    /** Debug builds only: a button that answers correctly, for testing later material. */
    debugSolve: Boolean = false,
) {
    val voice = rememberVoice()
    val sfx = rememberSfx(soundEffects)
    val haptics = rememberHaptics()

    // The session is a plain mutable object; these mirror it as snapshot state, and [step]
    // counts exercises so each one starts with fresh local state even if two are identical.
    var exercise by remember { mutableStateOf(session.current) }
    var verdict by remember { mutableStateOf(session.verdict) }
    var fraction by remember { mutableFloatStateOf(session.fraction) }
    var step by remember { mutableIntStateOf(0) }
    var pending by remember { mutableStateOf<Answer?>(null) }
    var hinted by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }

    fun sync() {
        exercise = session.current
        verdict = session.verdict
        fraction = session.fraction
    }

    fun next() {
        pending = null
        hinted = false
        voice.stop()
        val more = session.advance()
        step++
        sync()
        if (more == null) onFinish(session.result())
    }

    LaunchedEffect(session) {
        if (session.current == null) next()
    }

    fun check(answer: Answer) {
        val v = session.submit(answer, hinted)
        sync()
        when (v.outcome) {
            Outcome.CORRECT -> { sfx.correct(); haptics.correct() }
            Outcome.NEAR -> { sfx.near(); haptics.correct() }
            Outcome.WRONG -> { sfx.wrong(); haptics.wrong() }
        }
        val ex = session.current
        if (autoplay && ex != null) {
            // Hearing the right answer after every question is how the sounds stick.
            val audioId = (ex as? MultipleChoice)?.prompt?.audio ?: ex.itemId
            if (voice.has(audioId)) voice.play(audioId)
        }
    }

    BackHandler { confirmLeave = true }

    Box(modifier.fillMaxSize().papyrus()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(
                    IconKind.CLOSE, onClick = { confirmLeave = true }, size = 40.dp,
                    fill = Palette.sheetDeep, tint = Palette.inkSoft, description = "Leave lesson",
                )
                Spacer(Modifier.width(12.dp))
                GoldBar(fraction, Modifier.weight(1f))
                val solvable = exercise
                if (debugSolve && solvable != null && verdict == null) {
                    Spacer(Modifier.width(10.dp))
                    com.krbavcic.pentalingo.core.ui.Chip(
                        "solve",
                        Modifier.clickable {
                            if (solvable is Teach || solvable is ReadText) {
                                session.submit(Answer.Seen)
                                next()
                            } else {
                                check(solvable.solution())
                            }
                        },
                        color = Palette.ochreLight,
                    )
                }
            }

            val current = exercise
            if (current != null) {
                key(step) {
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        ExerciseBody(
                            exercise = current,
                            session = session,
                            verdict = verdict,
                            autoplay = autoplay,
                            voice = voice,
                            onTap = { sfx.tap(); haptics.tap() },
                            onPending = { pending = it },
                            onSubmitNow = { check(it) },
                            onHint = { hinted = true },
                        )
                    }
                }
                BottomBar(
                    exercise = current,
                    verdict = verdict,
                    pending = pending,
                    onCheck = { pending?.let(::check) },
                    onContinue = {
                        if (current is Teach || current is ReadText) session.submit(Answer.Seen)
                        next()
                    },
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }

        if (confirmLeave) {
            Scrim { confirmLeave = false }
            LeaveDialog(
                onStay = { confirmLeave = false },
                onLeave = {
                    voice.stop()
                    onLeave(session.result())
                },
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun ExerciseBody(
    exercise: Exercise,
    session: Session,
    verdict: Verdict?,
    autoplay: Boolean,
    voice: com.krbavcic.pentalingo.core.ui.Voice,
    onTap: () -> Unit,
    onPending: (Answer?) -> Unit,
    onSubmitNow: (Answer) -> Unit,
    onHint: () -> Unit,
) {
    val course = session.course
    val hint = SignHint(course, enabled = exercise.hintable && verdict == null, onHint)
    when (exercise) {
        is Teach -> TeachCard(course.require(exercise.itemId), course, voice, autoplay, exercise.firstTime)
        is ReadText -> ReadTextCard(course, exercise.textId, voice)
        is MultipleChoice -> ChoiceExercise(exercise, verdict, voice, onTap, onPending, hint)
        is Arrange -> ArrangeExercise(exercise, verdict, voice, onTap, onPending, hint)
        is Spell -> SpellExercise(exercise, course, verdict, voice, onTap, onPending)
        is ReadNumber -> ReadNumberExercise(exercise, verdict, onTap, onPending, hint)
        is BuildNumber -> BuildNumberExercise(exercise, course, verdict, onTap, onPending)
        is Pairs -> PairsExercise(exercise, verdict, voice, onTap, onSubmitNow)
    }
}

@Composable
private fun BottomBar(
    exercise: Exercise,
    verdict: Verdict?,
    pending: Answer?,
    onCheck: () -> Unit,
    onContinue: () -> Unit,
) {
    val teaching = exercise is Teach || exercise is ReadText
    Box(Modifier.fillMaxWidth()) {
        when {
            teaching -> PaintButton("Continue", onContinue, Modifier.fillMaxWidth().padding(16.dp))
            verdict == null -> if (exercise !is Pairs) PaintButton(
                "Check", onCheck,
                Modifier.fillMaxWidth().padding(16.dp),
                enabled = pending != null,
            )
        }
        AnimatedVisibility(
            visible = verdict != null && !teaching,
            enter = slideInVertically(tween(220)) { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            if (verdict != null) Feedback(verdict, onContinue)
        }
    }
}

@Composable
private fun Feedback(verdict: Verdict, onContinue: () -> Unit) {
    val (band, title, pigment) = when (verdict.outcome) {
        Outcome.CORRECT -> Triple(Palette.turquoiseLight, praise(verdict), Palette.unit(UnitColor.TURQUOISE))
        Outcome.NEAR -> Triple(Palette.ochreLight, "Nearly", Palette.unit(UnitColor.OCHRE))
        Outcome.WRONG -> Triple(androidx.compose.ui.graphics.Color(0xFFF3D5CD), "Not quite", Palette.unit(UnitColor.CARNELIAN))
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(band)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = Type.title, color = pigment.deep)
        if (verdict.outcome != Outcome.CORRECT) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                verdict.solutionMdc?.let { com.krbavcic.pentalingo.core.ui.Glyphs(it, quadrat = 40.dp) }
                verdict.solution?.let { Text("Answer: ${Translit.display(it)}", style = Type.heading, color = Palette.ink) }
            }
        }
        verdict.note?.let { Text(Translit.display(it), style = Type.body, color = Palette.inkSoft) }
        Spacer(Modifier.height(6.dp))
        PaintButton("Continue", onContinue, Modifier.fillMaxWidth(), pigment = pigment)
    }
}

private val PRAISE = listOf("Correct", "Right", "Well read", "Exactly", "Yes")

private fun praise(verdict: Verdict): String = PRAISE[(verdict.hashCode() and 0x7fffffff) % PRAISE.size]

@Composable
private fun LeaveDialog(onStay: () -> Unit, onLeave: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .padding(28.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.sheetLight)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Leave this lesson?", style = Type.title)
        Text(
            "Everything you have answered so far is kept. The lesson itself will wait for you.",
            style = Type.bodySoft,
        )
        PaintButton("Keep going", onStay, Modifier.fillMaxWidth())
        PaintButton(
            "Leave", onLeave, Modifier.fillMaxWidth(),
            pigment = Palette.Pigment(Palette.sheetDeep, Palette.edge, Palette.sheet), textColor = Palette.rubric,
        )
    }
}
