package com.krbavcic.pentalingo.feature.codex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.krbavcic.pentalingo.core.ui.Chip
import com.krbavcic.pentalingo.core.ui.Glyphs
import com.krbavcic.pentalingo.core.ui.IconKind
import com.krbavcic.pentalingo.core.ui.Palette
import com.krbavcic.pentalingo.core.ui.RoundIconButton
import com.krbavcic.pentalingo.core.ui.Rubric
import com.krbavcic.pentalingo.core.ui.Type
import com.krbavcic.pentalingo.core.ui.Voice
import com.krbavcic.pentalingo.engine.Awards
import com.krbavcic.pentalingo.engine.Knowledge
import com.krbavcic.pentalingo.engine.Numerals
import com.krbavcic.pentalingo.model.Award
import com.krbavcic.pentalingo.model.Memory
import com.krbavcic.pentalingo.model.SignItem
import com.krbavcic.pentalingo.model.SignRole
import com.krbavcic.pentalingo.model.WordItem
import com.krbavcic.pentalingo.script.Mdc
import com.krbavcic.pentalingo.script.Translit
import kotlin.math.roundToInt

@Composable
private fun SheetColumn(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(Palette.sheetLight)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

/**
 * What the memory model thinks of an item, in words: recall now, and roughly when it will next
 * need a review. Shown so the scheduling is not a black box.
 */
@Composable
private fun MemoryLine(k: Knowledge, id: String) {
    val m = k.memory(id) ?: return
    val recall = (k.recall(id) * 100).roundToInt()
    // Days from now until recall falls to the review threshold, from the forgetting curve.
    val target = 0.85
    val totalDays = m.stability * ((Math.pow(target, 1 / Memory.DECAY) - 1) / Memory.FACTOR)
    val left = (totalDays - m.elapsedDays(k.now)).coerceAtLeast(0.0)
    val due = when {
        left < 0.5 -> "due for review now"
        left < 1.5 -> "review in about a day"
        else -> "review in about ${left.roundToInt()} days"
    }
    Text("Recall $recall% · $due · reviewed ${m.reps} ${if (m.reps == 1) "time" else "times"}", style = Type.small)
    if (m.stability >= Memory.MASTERED_DAYS) Chip("Mastered", color = Palette.gold, textColor = Palette.ink)
}

@Composable
fun SignDetail(k: Knowledge, sign: SignItem, voice: Voice) {
    val course = k.course
    SheetColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Palette.sheet)
                    .border(2.dp, Palette.edge.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) { Glyphs(sign.code, quadrat = 84.dp, color = { signColor(k, sign) }) }
            Column(Modifier.padding(start = 16.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(sign.name.replaceFirstChar { it.uppercase() }, style = Type.title)
                Text("Gardiner ${sign.code}", style = Type.small)
                when {
                    sign.role == SignRole.NUMERAL -> Text(Numerals.valueOf(sign.code)?.let(Numerals::format).orEmpty(), style = Type.title)
                    sign.sounds.isNotEmpty() -> Text(sign.sounds.joinToString(" / ") { Translit.display(it) }, style = Type.translit)
                }
            }
            if (voice.has(sign.id)) RoundIconButton(IconKind.SPEAKER, { voice.play(sign.id) }, description = "Hear it")
        }
        MemoryLine(k, sign.id)
        sign.meaning?.let { Text(if (sign.role == SignRole.DETERMINATIVE) "Marks: $it" else "Means: $it", style = Type.heading) }
        sign.hint?.let { Text(Translit.display(it), style = Type.body) }
        sign.note?.let { Text(Translit.display(it), style = Type.bodySoft.copy(fontStyle = FontStyle.Italic)) }
        val words = course.words.filter { k.isIntroduced(it.id) && sign.code in Mdc.parse(it.mdc).signs }
        if (words.isNotEmpty()) {
            Rubric("In words you know")
            words.take(8).forEach { w ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(110.dp)) { Glyphs(w.mdc, quadrat = 30.dp) }
                    Text("${Translit.display(w.translit)} — ${w.gloss}", style = Type.body)
                }
            }
        }
    }
}

@Composable
fun WordDetail(k: Knowledge, word: WordItem, voice: Voice) {
    val course = k.course
    SheetColumn {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Glyphs(word.mdc, quadrat = 72.dp, color = { s -> course.signFor(s.glyph.code)?.let { signColor(k, it) } ?: Palette.ink })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(word.gloss.replaceFirstChar { it.uppercase() }, style = Type.title)
                Text("${Translit.display(word.translit)} · said “${word.say}”", style = Type.translit)
            }
            if (voice.has(word.id)) RoundIconButton(IconKind.SPEAKER, { voice.play(word.id) }, description = "Hear it")
        }
        MemoryLine(k, word.id)
        Rubric("Sign by sign")
        for (code in Mdc.parse(word.mdc).signs.distinct()) {
            val s = course.signFor(code)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(52.dp), contentAlignment = Alignment.Center) { Glyphs(code, quadrat = 30.dp) }
                Text(
                    when {
                        s == null -> code
                        s.role == SignRole.DETERMINATIVE -> "${s.name} — silent, marks ${s.meaning}"
                        s.sounds.isNotEmpty() -> "${s.name} — ${Translit.display(s.sounds[0])}"
                        else -> s.name
                    },
                    style = Type.body,
                )
            }
        }
        word.note?.let { Text(Translit.display(it), style = Type.bodySoft.copy(fontStyle = FontStyle.Italic)) }
    }
}

@Composable
fun AwardDetail(k: Knowledge, award: Award) {
    val earned = award.id in k.progress.awards
    val at = Awards.progress(award, k.course, k.progress, k.now)
    SheetColumn {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        if (earned) Brush.radialGradient(listOf(Palette.goldLight, Palette.gold))
                        else Brush.radialGradient(listOf(Palette.sheetLight, Palette.sheetDeep)),
                    )
                    .border(3.dp, if (earned) Palette.goldDeep else Palette.edge, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Glyphs(award.sign, quadrat = 54.dp, color = { if (earned) Palette.ink else Palette.inkFaint }) }
            Column(Modifier.weight(1f)) {
                Text(award.title, style = Type.title)
                Text(Translit.display(award.egyptian), style = Type.translit)
                Text(if (earned) "Earned" else award.howTo, style = Type.small)
            }
        }
        if (!earned && at.target > 1) Measured(at.fraction, "${at.current.coerceAtMost(at.target)} of ${at.target}")
        Text(Translit.display(award.lore), style = Type.body.copy(fontStyle = FontStyle.Italic))
    }
}
