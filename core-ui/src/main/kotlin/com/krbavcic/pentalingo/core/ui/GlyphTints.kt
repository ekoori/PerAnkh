package com.krbavcic.pentalingo.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.model.GlyphColours
import com.krbavcic.pentalingo.script.PlacedSign
import com.krbavcic.pentalingo.model.SignTint

/** How hieroglyphs are coloured outside the codex: Settings → Reading. */
val LocalGlyphColours = compositionLocalOf { GlyphColours.BY_ROLE }

private fun label(t: SignTint): String = when (t) {
    SignTint.READ -> "read"
    SignTint.READ_OTHER -> "read another way"
    SignTint.ECHO -> "sound repeated"
    SignTint.DETERMINATIVE -> "determinative, not read"
    SignTint.SILENT -> "not read"
    SignTint.NOUN -> "noun"
    SignTint.VERB -> "verb"
    SignTint.ADJECTIVE -> "adjective"
    SignTint.LITTLE_WORD -> "little word"
    SignTint.PRONOUN -> "pronoun"
    SignTint.NAME -> "name"
    SignTint.ENDING -> "ending"
}

/** A key to the colours on screen: only those used, in a fixed order, each a dot and a word. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TintLegend(tints: Collection<SignTint>, modifier: Modifier = Modifier) {
    val shown = SignTint.entries.filter { it in tints }
    if (shown.size < 2) return
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (t in shown) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Palette.tint(t)))
                Text(label(t), style = Type.small, modifier = Modifier.padding(start = 5.dp))
            }
        }
    }
}

/**
 * The colour of each sign by the learner's choice: by what it does here ([role]), by its tomb
 * paint, or in plain ink. A sign with no role given is drawn in ink.
 */
@Composable
fun glyphPaint(course: Course?, role: (PlacedSign) -> SignTint?): (PlacedSign) -> Color {
    val mode = LocalGlyphColours.current
    return { s ->
        when (mode) {
            GlyphColours.BY_ROLE -> role(s)?.let(Palette::tint) ?: Palette.ink
            GlyphColours.PAINT -> course?.signFor(s.glyph.code)?.let { Palette.paint(it.paint) } ?: Palette.ink
            GlyphColours.INK -> Palette.ink
        }
    }
}
