package com.krbavcic.pentalingo.feature.lesson

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.krbavcic.pentalingo.core.ui.Glyphs
import com.krbavcic.pentalingo.core.ui.PaintButton
import com.krbavcic.pentalingo.core.ui.Palette
import com.krbavcic.pentalingo.core.ui.Rubric
import com.krbavcic.pentalingo.core.ui.Type
import com.krbavcic.pentalingo.core.ui.papyrus
import com.krbavcic.pentalingo.core.ui.rememberSfx
import com.krbavcic.pentalingo.engine.SessionResult
import com.krbavcic.pentalingo.model.Award
import com.krbavcic.pentalingo.model.ConceptItem
import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.model.Item
import com.krbavcic.pentalingo.model.PhraseItem
import com.krbavcic.pentalingo.model.ReadingText
import com.krbavcic.pentalingo.model.SignItem
import com.krbavcic.pentalingo.model.UnitColor
import com.krbavcic.pentalingo.model.WordItem
import com.krbavcic.pentalingo.script.Translit
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** What a finished sitting amounted to. */
data class Summary(
    val heading: String,
    val subtitle: String?,
    val keySign: String,
    val result: SessionResult,
    val learned: List<Item>,
    /** Distinct items whose memory was refreshed by review. */
    val refreshed: Int,
    val awards: List<Award>,
    val text: ReadingText?,
)

/**
 * The end of a sitting: what is now known that was not before, and what was earned.
 *
 * Deliberately not a score screen. Accuracy is shown because it is useful to know, but the
 * headline is the list of new things in the codex and any amulet earned — what changed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummaryScreen(summary: Summary, course: Course, soundEffects: Boolean, onContinue: () -> Unit) {
    val sfx = rememberSfx(soundEffects)
    val seal = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        sfx.complete()
        seal.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 220f))
        if (summary.awards.isNotEmpty()) {
            delay(400)
            sfx.award()
        }
    }
    Box(Modifier.fillMaxSize().papyrus()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .padding(top = 12.dp)
                    .size(132.dp)
                    .scale(seal.value)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Palette.goldLight, Palette.gold, Palette.goldDeep)))
                    .border(4.dp, Palette.goldDeep, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Glyphs(summary.keySign, quadrat = 70.dp, color = { Palette.ink })
            }
            Text(summary.heading, style = Type.display, textAlign = TextAlign.Center)
            summary.subtitle?.let { Text(it, style = Type.bodySoft, textAlign = TextAlign.Center) }

            val r = summary.result
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("${(r.accuracy * 100).roundToInt()}%", "accuracy", Modifier.weight(1f))
                Stat("${r.graded}", "answers", Modifier.weight(1f))
                val minutes = ((r.finishedAt - r.startedAt) / 60_000.0).roundToInt().coerceAtLeast(1)
                Stat("$minutes min", "time", Modifier.weight(1f))
            }

            val learned = summary.learned.filter { it !is ConceptItem }
            if (learned.isNotEmpty()) {
                Rubric("New in your codex", Modifier.fillMaxWidth())
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    learned.forEach { LearnedChip(it, course) }
                }
            }
            if (summary.refreshed > 0) {
                Text(
                    "Ink refreshed on ${summary.refreshed} ${if (summary.refreshed == 1) "thing" else "things"} you already knew.",
                    style = Type.bodySoft, modifier = Modifier.fillMaxWidth(),
                )
            }
            summary.text?.let { text ->
                Panel(UnitColor.OCHRE) {
                    Rubric("Text read", color = Palette.ochreDeep)
                    Text(text.title, style = Type.title)
                    Text(text.source, style = Type.small)
                }
            }
            summary.awards.forEach { AmuletCard(it) }
            Spacer(Modifier.height(4.dp))
            PaintButton("Continue", onContinue, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.sheetLight.copy(alpha = 0.8f))
            .border(1.5.dp, Palette.edge.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = Type.title)
        Text(label, style = Type.small)
    }
}

@Composable
private fun LearnedChip(item: Item, course: Course) {
    val (mdc, caption) = when (item) {
        is SignItem -> item.code to (item.sounds.firstOrNull()?.let(Translit::display) ?: item.meaning.orEmpty())
        is WordItem -> item.mdc to item.gloss
        is PhraseItem -> item.tokens.first().mdc to item.translation
        else -> return
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.sheetLight.copy(alpha = 0.85f))
            .border(1.5.dp, Palette.gold, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Glyphs(mdc, quadrat = 30.dp, color = { s -> course.signFor(s.glyph.code)?.let { Palette.paint(it.paint) } ?: Palette.ink })
        Text(caption, style = Type.small.copy(color = Palette.ink))
    }
}

@Composable
private fun Panel(color: UnitColor, content: @Composable () -> Unit) {
    val p = Palette.unit(color)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(p.light.copy(alpha = 0.6f))
            .border(2.dp, p.face, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
}

/** An amulet: its sign in gold, its name, and what it meant to those who wore it. */
@Composable
fun AmuletCard(award: Award) {
    Panel(UnitColor.GOLD) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Palette.goldLight, Palette.gold)))
                    .border(3.dp, Palette.goldDeep, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Glyphs(award.sign, quadrat = 40.dp, color = { Palette.ink })
            }
            Column(Modifier.weight(1f)) {
                Rubric("Amulet earned", color = Palette.goldDeep)
                Text(award.title, style = Type.title)
                Text(Translit.display(award.egyptian), style = Type.translit)
            }
        }
        Text(Translit.display(award.lore), style = Type.body.copy(fontStyle = FontStyle.Italic))
    }
}
