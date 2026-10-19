package com.krbavcic.pentalingo.feature.codex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.krbavcic.pentalingo.core.ui.GoldBar
import com.krbavcic.pentalingo.core.ui.Glyphs
import com.krbavcic.pentalingo.core.ui.Palette
import com.krbavcic.pentalingo.core.ui.Rubric
import com.krbavcic.pentalingo.core.ui.Tile
import com.krbavcic.pentalingo.core.ui.TileState
import com.krbavcic.pentalingo.core.ui.Type
import com.krbavcic.pentalingo.engine.Knowledge
import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.model.SignItem
import com.krbavcic.pentalingo.model.SignRole
import com.krbavcic.pentalingo.model.WordItem
import com.krbavcic.pentalingo.script.PlacedSign
import com.krbavcic.pentalingo.script.Translit

/** Colour for a sign in the codex: unmet, being learned, or painted and fading with recall. */
fun signColor(k: Knowledge, sign: SignItem) =
    Palette.forRecall(sign.paint, k.isIntroduced(sign.id), k.isKnown(sign.id), k.recall(sign.id))

private enum class Shelf { SIGNS, WORDS }

/**
 * Every sign and word in the course, as a sign list that fills with colour.
 *
 * Unmet signs are ghosts, signs being learned are ink, known signs are painted in their tomb
 * colours — and the paint fades as recall drops, so the codex is also an honest map of what
 * needs review.
 */
@Composable
fun CodexScreen(
    knowledge: Knowledge,
    onSign: (SignItem) -> Unit,
    onWord: (WordItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shelf by rememberSaveable { mutableStateOf(Shelf.SIGNS) }
    val course = knowledge.course
    Column(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(start = 20.dp, end = 20.dp, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Codex", style = Type.display)
            Text(
                "Ink while you are learning a sign; its paint once you remember it on another day. " +
                    "Paint fades as memory does — review brings it back.",
                style = Type.small,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShelfTab("Signs", shelf == Shelf.SIGNS, Modifier.weight(1f)) { shelf = Shelf.SIGNS }
                ShelfTab("Words", shelf == Shelf.WORDS, Modifier.weight(1f)) { shelf = Shelf.WORDS }
            }
        }
        when (shelf) {
            Shelf.SIGNS -> SignShelf(knowledge, course, onSign)
            Shelf.WORDS -> WordShelf(knowledge, course, onWord)
        }
    }
}

@Composable
private fun ShelfTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Tile(if (selected) TileState.SELECTED else TileState.IDLE, onClick, modifier.height(46.dp)) {
        Text(label, style = Type.label.copy(color = if (selected) Palette.lapisDeep else Palette.inkSoft))
    }
}

private fun groupOf(sign: SignItem): String = when (sign.role) {
    SignRole.UNILITERAL -> "The alphabet"
    SignRole.BILITERAL, SignRole.TRILITERAL, SignRole.LOGOGRAM -> "Signs of two and three, and picture-words"
    SignRole.DETERMINATIVE -> "Determinatives"
    SignRole.NUMERAL -> "Numbers"
}

@Composable
private fun SignShelf(k: Knowledge, course: Course, onSign: (SignItem) -> Unit) {
    val groups = remember(course) { course.signs.groupBy(::groupOf) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 78.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for ((title, signs) in groups) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val known = signs.count { k.isKnown(it.id) }
                val met = signs.count { k.isIntroduced(it.id) }
                Column(Modifier.padding(top = 10.dp)) {
                    Rubric(title)
                    Text("$met of ${signs.size} met · $known known well", style = Type.small)
                }
            }
            items(signs, key = { it.id }) { sign -> SignCell(k, sign) { onSign(sign) } }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SignCell(k: Knowledge, sign: SignItem, onClick: () -> Unit) {
    val met = k.isIntroduced(sign.id)
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (met) Palette.sheetLight.copy(alpha = 0.8f) else Palette.sheet.copy(alpha = 0.5f))
            .border(1.5.dp, if (k.isMastered(sign.id)) Palette.gold else Palette.edge.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.15f), contentAlignment = Alignment.Center) {
            Glyphs(sign.code, quadrat = 44.dp, color = { signColor(k, sign) }, description = sign.name)
        }
        val caption = when {
            !met -> "?"
            sign.role == SignRole.NUMERAL -> com.krbavcic.pentalingo.engine.Numerals.valueOf(sign.code)?.let(com.krbavcic.pentalingo.engine.Numerals::format).orEmpty()
            sign.sounds.isNotEmpty() -> Translit.display(sign.sounds[0])
            else -> sign.meaning.orEmpty()
        }
        Text(
            caption, style = if (sign.sounds.isNotEmpty() && met) Type.translit else Type.small,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
        if (met) InkBar(k.recall(sign.id).toFloat())
    }
}

/** A thin bar of ink: how fresh the memory is right now. */
@Composable
fun InkBar(recall: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(top = 4.dp)
            .width(40.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Palette.inkGhost),
    ) {
        Box(
            Modifier
                .fillMaxWidth(recall.coerceIn(0f, 1f))
                .height(4.dp)
                .background(if (recall >= 0.8f) Palette.turquoise else if (recall >= 0.6f) Palette.ochre else Palette.rubric),
        )
    }
}

@Composable
private fun WordShelf(k: Knowledge, course: Course, onWord: (WordItem) -> Unit) {
    val order = remember(course) {
        val lessonIndex = course.lessons.flatMapIndexed { i, l -> l.teach.map { it to i } }.toMap()
        course.words.sortedBy { lessonIndex[it.id] ?: Int.MAX_VALUE }
    }
    androidx.compose.foundation.lazy.LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            val known = order.count { k.isKnown(it.id) }
            Text("$known of ${order.size} words known", style = Type.small)
        }
        items(order.size, key = { order[it].id }) { i ->
            val word = order[i]
            val met = k.isIntroduced(word.id)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (met) Palette.sheetLight.copy(alpha = 0.8f) else Palette.sheet.copy(alpha = 0.4f))
                    .border(1.5.dp, if (k.isMastered(word.id)) Palette.gold else Palette.edge.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .clickable(enabled = met) { onWord(word) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val paint: (PlacedSign) -> Color = if (met) {
                    { s -> course.signFor(s.glyph.code)?.let { signColor(k, it) } ?: Palette.ink }
                } else {
                    { Palette.inkFaint }
                }
                Box(Modifier.width(120.dp), contentAlignment = Alignment.CenterStart) {
                    Glyphs(word.mdc, quadrat = 34.dp, color = paint)
                }
                Column(Modifier.weight(1f)) {
                    if (met) {
                        Text(Translit.display(word.translit), style = Type.translit)
                        Text(word.gloss, style = Type.body)
                        InkBar(k.recall(word.id).toFloat())
                    } else {
                        Text("Not met yet", style = Type.small)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

/** A gold bar with a caption, for readability and award progress. */
@Composable
fun Measured(fraction: Float, caption: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        GoldBar(fraction, Modifier.fillMaxWidth(), height = 10.dp)
        Text(caption, style = Type.small)
    }
}
