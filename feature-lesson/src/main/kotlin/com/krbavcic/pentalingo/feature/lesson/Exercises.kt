package com.krbavcic.pentalingo.feature.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krbavcic.pentalingo.core.ui.EvenRow
import com.krbavcic.pentalingo.core.ui.Glyphs
import com.krbavcic.pentalingo.core.ui.IconKind
import com.krbavcic.pentalingo.core.ui.Palette
import com.krbavcic.pentalingo.core.ui.RoundIconButton
import com.krbavcic.pentalingo.core.ui.Tile
import com.krbavcic.pentalingo.core.ui.TileState
import com.krbavcic.pentalingo.core.ui.Type
import com.krbavcic.pentalingo.core.ui.Voice
import com.krbavcic.pentalingo.core.ui.lineOf
import com.krbavcic.pentalingo.engine.Answer
import com.krbavcic.pentalingo.engine.Arrange
import com.krbavcic.pentalingo.engine.BuildNumber
import com.krbavcic.pentalingo.engine.Choice
import com.krbavcic.pentalingo.engine.Display
import com.krbavcic.pentalingo.engine.ExerciseKind
import com.krbavcic.pentalingo.engine.MultipleChoice
import com.krbavcic.pentalingo.engine.Numerals
import com.krbavcic.pentalingo.engine.Outcome
import com.krbavcic.pentalingo.engine.Pairs
import com.krbavcic.pentalingo.engine.ReadNumber
import com.krbavcic.pentalingo.engine.Spell
import com.krbavcic.pentalingo.engine.Verdict
import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.script.Mdc
import com.krbavcic.pentalingo.script.Translit
import kotlinx.coroutines.delay

/**
 * Lets a learner tap a sign to see what it is. Using it is free, but marks the answer as helped:
 * a right answer then counts as a near miss for the memory model.
 */
internal class SignHint(val course: Course, val enabled: Boolean, private val onUsed: () -> Unit) {
    fun describe(code: String): String {
        val s = course.signFor(code) ?: return code
        return when {
            s.role == com.krbavcic.pentalingo.model.SignRole.NUMERAL ->
                "${s.name} · ${Numerals.valueOf(code)?.let(Numerals::format)}"
            s.sounds.isNotEmpty() -> "${s.name} · ${Translit.display(s.sounds[0])}"
            else -> "${s.name} · ${s.meaning}"
        }
    }

    fun used() = onUsed()
}

@Composable
internal fun Instruction(text: String) {
    Text(Translit.display(text), style = Type.title, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
}

/**
 * The question itself: hieroglyphs, transliteration, English and audio, whichever it has.
 * A prompt that is only a sound plays itself, large, as soon as it appears.
 */
@Composable
internal fun Prompt(
    display: Display,
    voice: Voice,
    autoplayAudio: Boolean = true,
    hint: SignHint? = null,
    quadrat: androidx.compose.ui.unit.Dp? = null,
) {
    val audioOnly = display.audio != null && display.mdc == null && display.words.isEmpty() &&
        display.translit == null && display.text == null
    var peek by remember(display) { mutableStateOf<String?>(null) }
    val onSign: ((com.krbavcic.pentalingo.script.PlacedSign) -> Unit)? = if (hint?.enabled == true) {
        { s ->
            peek = hint.describe(s.glyph.code)
            hint.used()
        }
    } else null
    Column(
        Modifier.fillMaxWidth().padding(bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (display.words.isNotEmpty()) {
            Glyphs(lineOf(display.words), quadrat = 50.dp, mirrored = display.mirrored, onSign = onSign)
        } else display.mdc?.let { mdc ->
            val single = Mdc.parse(mdc).signs.size == 1
            Glyphs(mdc, quadrat = quadrat ?: if (single) 104.dp else 64.dp, mirrored = display.mirrored, highlight = display.highlight, onSign = onSign)
        }
        when {
            peek != null -> Text(peek!!, style = Type.body.copy(color = Palette.lapisDeep))
            onSign != null -> Text("Tap a sign for a hint", style = Type.small)
        }
        display.translit?.let { Text(Translit.display(it), style = Type.translit.copy(fontSize = 26.sp, lineHeight = 32.sp)) }
        display.text?.let { Text("“$it”", style = Type.title, textAlign = TextAlign.Center) }
        if (display.audio != null && voice.has(display.audio)) {
            LaunchedEffect(display.audio) { if (autoplayAudio || audioOnly) voice.play(display.audio) }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(
                    IconKind.SPEAKER, { voice.play(display.audio) },
                    size = if (audioOnly) 84.dp else 52.dp, description = "Play the sound",
                )
                SlowButton { voice.play(display.audio, slow = true) }
            }
        }
    }
}

/** "Slow": the same clip at 70% speed, for catching every consonant. */
@Composable
internal fun SlowButton(onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.lapisLight.copy(alpha = 0.35f))
            .border(1.5.dp, Palette.lapis, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text("slow", style = Type.label.copy(color = Palette.lapisDeep))
    }
}

private fun stateFor(index: Int, picked: Int?, correct: Int, verdict: Verdict?): TileState = when {
    verdict == null -> if (index == picked) TileState.SELECTED else TileState.IDLE
    index == correct -> TileState.CORRECT
    index == picked -> if (verdict.outcome == Outcome.NEAR) TileState.NEAR else TileState.WRONG
    else -> TileState.DIM
}

@Composable
private fun ChoiceFace(choice: Choice, glyphSize: androidx.compose.ui.unit.Dp = 54.dp) {
    when {
        choice.mdc != null -> Glyphs(choice.mdc!!, quadrat = glyphSize, modifier = Modifier.padding(10.dp))
        choice.translit != null -> Text(
            Translit.display(choice.translit!!), style = Type.translit.copy(fontSize = 26.sp, lineHeight = 30.sp),
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp), textAlign = TextAlign.Center,
        )
        else -> Text(
            Translit.display(choice.label.orEmpty()), style = Type.body.copy(fontSize = 17.sp),
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 14.dp), textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun ChoiceExercise(
    ex: MultipleChoice,
    verdict: Verdict?,
    voice: Voice,
    onTap: () -> Unit,
    onPending: (Answer?) -> Unit,
    hint: SignHint? = null,
) {
    var picked by remember { mutableStateOf<Int?>(null) }
    Instruction(ex.instruction)
    Prompt(ex.prompt, voice, autoplayAudio = ex.kind in AUDIO_KINDS, hint = hint)

    fun pick(i: Int) {
        if (verdict != null) return
        picked = i
        onTap()
        onPending(Answer.Pick(i))
    }

    val grid = ex.choices.all { it.mdc != null || (it.translit != null && it.translit!!.length <= 4) }
    if (grid) {
        ex.choices.chunked(2).forEachIndexed { row, pair ->
            EvenRow(Modifier.padding(bottom = 12.dp)) {
                pair.forEachIndexed { col, choice ->
                    val i = row * 2 + col
                    Tile(
                        stateFor(i, picked, ex.correct, verdict), { pick(i) },
                        Modifier.weight(1f).heightIn(min = 96.dp),
                    ) { ChoiceFace(choice) }
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ex.choices.forEachIndexed { i, choice ->
                Tile(stateFor(i, picked, ex.correct, verdict), { pick(i) }, Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    ChoiceFace(choice)
                }
            }
        }
    }
}

private val AUDIO_KINDS = setOf(ExerciseKind.LISTEN_TO_SIGN, ExerciseKind.LISTEN_TO_WORD, ExerciseKind.SOUND_TO_SIGN)

@Composable
private fun TileFaceSmall(choice: Choice, glyphSize: androidx.compose.ui.unit.Dp = 38.dp) {
    when {
        choice.mdc != null -> Glyphs(choice.mdc!!, quadrat = glyphSize, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
        choice.translit != null -> Text(
            Translit.display(choice.translit!!), style = Type.translit.copy(fontSize = 24.sp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
        else -> Text(choice.label.orEmpty(), style = Type.body, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp))
    }
}

/**
 * Build an answer from tiles. Tapping a tile moves it onto the ruled line; tapping it there
 * sends it back. The empty slot it leaves keeps the bank from reshuffling under the finger.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ArrangeExercise(
    ex: Arrange,
    verdict: Verdict?,
    voice: Voice,
    onTap: () -> Unit,
    onPending: (Answer?) -> Unit,
    hint: SignHint? = null,
) {
    var chosen by remember { mutableStateOf(listOf<Int>()) }
    fun update(next: List<Int>) {
        chosen = next
        onTap()
        onPending(if (next.isEmpty()) null else Answer.Tiles(next.map { ex.tiles[it].face }))
    }
    Instruction(ex.instruction)
    Prompt(ex.prompt, voice, autoplayAudio = false, hint = hint)

    val lineState = when (verdict?.outcome) {
        null -> TileState.IDLE
        Outcome.CORRECT -> TileState.CORRECT
        Outcome.NEAR -> TileState.NEAR
        Outcome.WRONG -> TileState.WRONG
    }
    FlowRow(
        Modifier
            .padding(bottom = 18.dp)
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.sheetLight.copy(alpha = 0.55f))
            .border(1.5.dp, Palette.edge.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .ruled()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        chosen.forEach { i ->
            Tile(lineState, if (verdict == null) ({ update(chosen - i) }) else null, Modifier.padding(top = 6.dp)) {
                TileFaceSmall(ex.tiles[i])
            }
        }
    }
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ex.tiles.forEachIndexed { i, tile ->
            if (i in chosen) {
                Box(Modifier.clip(RoundedCornerShape(14.dp)).background(Palette.inkGhost)) {
                    Box(Modifier.padding(2.dp).alpha(0f)) { TileFaceSmall(tile) }
                }
            } else {
                Tile(if (verdict == null) TileState.IDLE else TileState.DIM, if (verdict == null) ({ update(chosen + i) }) else null) {
                    TileFaceSmall(tile)
                }
            }
        }
    }
}

/** Faint ruling, like the guide lines a scribe drew before writing. */
private fun Modifier.ruled(): Modifier = drawBehind {
    var y = 58f * density
    while (y < size.height - 8f * density) {
        drawLine(Palette.edge.copy(alpha = 0.55f), Offset(14f * density, y), Offset(size.width - 14f * density, y), 1.5f * density)
        y += 56f * density
    }
}

/** Spell a word by tapping its signs in order. Repeats are allowed: ḫwfw has its w twice. */
@Composable
internal fun SpellExercise(
    ex: Spell,
    course: Course,
    verdict: Verdict?,
    voice: Voice,
    onTap: () -> Unit,
    onPending: (Answer?) -> Unit,
) {
    var chosen by remember { mutableStateOf(listOf<String>()) }
    fun update(next: List<String>) {
        chosen = next
        onTap()
        onPending(if (next.isEmpty()) null else Answer.Signs(next))
    }
    Instruction(ex.instruction)
    Prompt(ex.prompt, voice, autoplayAudio = false)

    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.sheetLight.copy(alpha = 0.7f))
            .border(2.dp, if (verdict?.outcome == Outcome.CORRECT) Palette.turquoise else Palette.edge, RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            verdict?.outcome == Outcome.CORRECT -> Glyphs(ex.mdc, quadrat = 64.dp)
            chosen.isEmpty() -> Text("Tap the signs in order", style = Type.bodySoft)
            else -> Glyphs(chosen.joinToString("-"), quadrat = 56.dp)
        }
        if (chosen.isNotEmpty() && verdict == null) {
            RoundIconButton(
                IconKind.BACKSPACE, { update(chosen.dropLast(1)) }, Modifier.align(Alignment.TopEnd),
                size = 36.dp, fill = Palette.sheetDeep, tint = Palette.inkSoft, description = "Remove last sign",
            )
        }
    }
    Spacer(Modifier.height(18.dp))
    ex.palette.chunked(4).forEach { row ->
        EvenRow(Modifier.padding(bottom = 10.dp), gap = 10.dp) {
            row.forEach { code ->
                Tile(if (verdict == null) TileState.IDLE else TileState.DIM, if (verdict == null) ({ update(chosen + code) }) else null,
                    Modifier.weight(1f).aspectRatio(1f)) {
                    Glyphs(code, quadrat = 46.dp, description = course.signFor(code)?.name)
                }
            }
            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun Keypad(onKey: (Char) -> Unit, onBack: () -> Unit, enabled: Boolean) {
    val keys = listOf("123", "456", "789", "<0 ")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        keys.forEach { row ->
            EvenRow(gap = 10.dp) {
                row.forEach { k ->
                    when (k) {
                        ' ' -> Spacer(Modifier.weight(1f))
                        '<' -> Tile(TileState.IDLE, if (enabled) onBack else null, Modifier.weight(1f).height(58.dp)) {
                            com.krbavcic.pentalingo.core.ui.DrawnIcon(IconKind.BACKSPACE, Modifier.size(26.dp), Palette.inkSoft)
                        }
                        else -> Tile(TileState.IDLE, if (enabled) ({ onKey(k) }) else null, Modifier.weight(1f).height(58.dp)) {
                            Text(k.toString(), style = Type.title)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ReadNumberExercise(ex: ReadNumber, verdict: Verdict?, onTap: () -> Unit, onPending: (Answer?) -> Unit, hint: SignHint? = null) {
    var typed by remember { mutableStateOf("") }
    fun update(next: String) {
        typed = next.take(8)
        onTap()
        onPending(typed.toIntOrNull()?.let { Answer.Value(it) })
    }
    Instruction(ex.instruction)
    // Numbers are counted sign by sign, so they are drawn large enough to count.
    Prompt(Display(mdc = ex.mdc), voice = com.krbavcic.pentalingo.core.ui.rememberVoice(), hint = hint, quadrat = 104.dp)
    Text(
        typed.toIntOrNull()?.let(Numerals::format) ?: "?",
        style = Type.display.copy(fontSize = 40.sp, color = if (verdict == null) Palette.ink else Palette.inkSoft),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        textAlign = TextAlign.Center,
    )
    Keypad({ update(typed + it) }, { update(typed.dropLast(1)) }, enabled = verdict == null)
}

@Composable
internal fun BuildNumberExercise(ex: BuildNumber, course: Course, verdict: Verdict?, onTap: () -> Unit, onPending: (Answer?) -> Unit) {
    var chosen by remember { mutableStateOf(listOf<String>()) }
    fun update(next: List<String>) {
        chosen = next
        onTap()
        onPending(if (next.isEmpty()) null else Answer.Signs(next))
    }
    Instruction(ex.instruction)
    Text(Numerals.format(ex.value), style = Type.display.copy(fontSize = 44.sp), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    Spacer(Modifier.height(14.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.sheetLight.copy(alpha = 0.7f))
            .border(2.dp, Palette.edge, RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        val mdc = Numerals.group(chosen)
        if (mdc == null) Text("Tap the signs to write it", style = Type.bodySoft) else Glyphs(mdc, quadrat = 58.dp)
        if (chosen.isNotEmpty() && verdict == null) {
            RoundIconButton(
                IconKind.BACKSPACE, { update(chosen.dropLast(1)) }, Modifier.align(Alignment.TopEnd),
                size = 36.dp, fill = Palette.sheetDeep, tint = Palette.inkSoft, description = "Remove last sign",
            )
        }
    }
    Spacer(Modifier.height(18.dp))
    ex.numerals.chunked(4).forEach { row ->
        EvenRow(Modifier.padding(bottom = 10.dp), gap = 10.dp) {
            row.forEach { (code, value) ->
                Tile(if (verdict == null) TileState.IDLE else TileState.DIM, if (verdict == null) ({ update(chosen + code) }) else null,
                    Modifier.weight(1f).heightIn(min = 92.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Glyphs(code, quadrat = 40.dp, modifier = Modifier.padding(top = 8.dp), description = course.signFor(code)?.name)
                        Text(Numerals.format(value), style = Type.small, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
            }
            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/**
 * Match signs to sounds or words to meanings. A wrong pairing flashes red and is remembered as a
 * slip for both items, but the exercise simply carries on until everything is matched.
 */
@Composable
internal fun PairsExercise(ex: Pairs, verdict: Verdict?, voice: Voice, onTap: () -> Unit, onDone: (Answer) -> Unit) {
    var left by remember { mutableStateOf<Int?>(null) }
    var right by remember { mutableStateOf<Int?>(null) }
    var matched by remember { mutableStateOf(setOf<Int>()) }
    var missed by remember { mutableStateOf(setOf<String>()) }
    var wrongFlash by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    LaunchedEffect(wrongFlash) {
        if (wrongFlash != null) {
            delay(650)
            wrongFlash = null
        }
    }

    fun attempt(l: Int?, r: Int?) {
        if (l == null || r == null) return
        if (ex.match[l] == r) {
            matched = matched + l
            voice.play(ex.left[l].itemId)
            if (matched.size == ex.left.size) onDone(Answer.Matched(missed))
        } else {
            wrongFlash = l to r
            missed = missed + listOfNotNull(ex.left[l].itemId, ex.right[r].itemId)
        }
        left = null
        right = null
    }

    Instruction(ex.instruction)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ex.left.forEachIndexed { i, c ->
                val state = when {
                    i in matched -> TileState.CORRECT
                    wrongFlash?.first == i -> TileState.WRONG
                    left == i -> TileState.SELECTED
                    else -> TileState.IDLE
                }
                Tile(state, if (i in matched || verdict != null) null else ({ onTap(); left = i; attempt(i, right) }),
                    Modifier.fillMaxWidth().height(76.dp)) {
                    TileFaceSmall(c, glyphSize = 52.dp)
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ex.right.forEachIndexed { j, c ->
                val done = ex.match.withIndex().any { (l, r) -> r == j && l in matched }
                val state = when {
                    done -> TileState.CORRECT
                    wrongFlash?.second == j -> TileState.WRONG
                    right == j -> TileState.SELECTED
                    else -> TileState.IDLE
                }
                Tile(state, if (done || verdict != null) null else ({ onTap(); right = j; attempt(left, j) }),
                    Modifier.fillMaxWidth().height(76.dp)) {
                    TileFaceSmall(c)
                }
            }
        }
    }
}
