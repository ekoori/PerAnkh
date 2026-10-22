package com.krbavcic.pentalingo.feature.codex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.krbavcic.pentalingo.core.ui.Glyphs
import com.krbavcic.pentalingo.core.ui.Palette
import com.krbavcic.pentalingo.core.ui.Rubric
import com.krbavcic.pentalingo.core.ui.Type
import com.krbavcic.pentalingo.engine.Awards
import com.krbavcic.pentalingo.engine.Knowledge
import com.krbavcic.pentalingo.model.Award
import com.krbavcic.pentalingo.model.ItemKind
import com.krbavcic.pentalingo.model.Memory
import com.krbavcic.pentalingo.model.Requirement
import kotlin.math.roundToInt

/**
 * Amulets earned and still to earn, and the plain numbers behind them.
 *
 * Each amulet stands for an ability, and its lore is part of the course: the wedjat eye, the
 * djed pillar and the scarab teach something about Egypt as well as marking progress.
 */
@Composable
fun TreasuryScreen(
    knowledge: Knowledge,
    onAward: (Award) -> Unit,
    modifier: Modifier = Modifier,
    /** Items and texts of sections still sealed: their amulets and counts are held back. */
    hidden: Set<String> = emptySet(),
) {
    val course = knowledge.course
    val progress = knowledge.progress
    val awards = course.awards.filter { award ->
        val req = award.requirement
        award.id in progress.awards || req !is Requirement.ReadText || req.textId !in hidden
    }
    LazyVerticalGrid(
        GridCells.Fixed(2),
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 12.dp)) {
                Text("Treasury", style = Type.display)
                Text("${progress.awards.size} of ${awards.size} amulets", style = Type.bodySoft)
            }
        }
        items(awards, key = { it.id }) { award ->
            val earned = award.id in progress.awards
            val at = Awards.progress(award, course, progress, knowledge.now)
            Column(
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (earned) Palette.goldLight.copy(alpha = 0.35f) else Palette.sheetLight.copy(alpha = 0.6f))
                    .border(2.dp, if (earned) Palette.gold else Palette.edge.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .clickable { onAward(award) }
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(
                            if (earned) Brush.radialGradient(listOf(Palette.goldLight, Palette.gold))
                            else Brush.radialGradient(listOf(Palette.sheetLight, Palette.sheetDeep)),
                        )
                        .border(3.dp, if (earned) Palette.goldDeep else Palette.edge.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Glyphs(award.sign, quadrat = 38.dp, color = { if (earned) Palette.ink else Palette.inkFaint })
                }
                Text(award.title, style = Type.heading, textAlign = TextAlign.Center)
                if (earned) {
                    Text("Earned", style = Type.small.copy(color = Palette.goldDeep))
                } else {
                    Text(award.howTo, style = Type.small, textAlign = TextAlign.Center)
                    if (at.target > 1) Measured(at.fraction, "${at.current.coerceAtMost(at.target)} / ${at.target}")
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Ledger(knowledge, hidden) }
    }
}

/** The plain numbers, for those who want them. */
@Composable
private fun Ledger(k: Knowledge, hidden: Set<String>) {
    val p = k.progress
    val signsKnown = k.knownCount(ItemKind.SIGN)
    val wordsKnown = k.knownCount(ItemKind.WORD)
    val mastered = p.memories.values.count { it.stability >= Memory.MASTERED_DAYS }
    val accuracy = if (p.answered == 0) 0 else (100.0 * p.correct / p.answered).roundToInt()
    Column(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.sheetLight.copy(alpha = 0.7f))
            .border(1.5.dp, Palette.edge.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Rubric("The ledger")
        Line("Signs known", "$signsKnown of ${k.course.signs.count { it.id !in hidden || k.isIntroduced(it.id) }}")
        Line("Words known", "$wordsKnown of ${k.course.words.count { it.id !in hidden || k.isIntroduced(it.id) }}")
        Line("Mastered (safe for three weeks)", "$mastered")
        Line("Fading now", "${k.fadingCount()}")
        Line("Days studied", "${p.studyDays.size}")
        Line("Answers", "${p.answered} · $accuracy% right")
        Line("Time studied", "${(p.secondsStudied / 60)} min")
        Spacer(Modifier.height(4.dp))
        Text("No streaks are kept. Memory grows between visits, not by visiting every day.", style = Type.small)
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = Type.body, modifier = Modifier.weight(1f))
        Text(value, style = Type.heading)
    }
}
