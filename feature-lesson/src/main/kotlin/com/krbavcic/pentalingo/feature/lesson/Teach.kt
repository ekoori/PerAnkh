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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krbavcic.pentalingo.core.ui.Chip
import com.krbavcic.pentalingo.core.ui.Glyphs
import com.krbavcic.pentalingo.core.ui.IconKind
import com.krbavcic.pentalingo.core.ui.Palette
import com.krbavcic.pentalingo.core.ui.RoundIconButton
import com.krbavcic.pentalingo.core.ui.Rubric
import com.krbavcic.pentalingo.core.ui.Type
import com.krbavcic.pentalingo.core.ui.Voice
import com.krbavcic.pentalingo.core.ui.lineOf
import com.krbavcic.pentalingo.engine.Numerals
import com.krbavcic.pentalingo.model.ConceptItem
import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.model.Item
import com.krbavcic.pentalingo.model.PhraseItem
import com.krbavcic.pentalingo.model.SignItem
import com.krbavcic.pentalingo.model.SignRole
import com.krbavcic.pentalingo.model.WordItem
import com.krbavcic.pentalingo.script.Mdc
import com.krbavcic.pentalingo.script.Pronounce
import com.krbavcic.pentalingo.script.Translit

/** A teaching card: everything worth knowing about one new thing, before it is ever asked. */
@Composable
fun TeachCard(item: Item, course: Course, voice: Voice, autoplay: Boolean, firstTime: Boolean) {
    LaunchedEffect(item.id) {
        if (autoplay && voice.has(item.id)) voice.play(item.id)
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (item) {
            is SignItem -> SignCard(item, voice, firstTime)
            is WordItem -> WordCard(item, course, voice, firstTime)
            is PhraseItem -> PhraseCard(item, course, voice)
            is ConceptItem -> ConceptCard(item)
        }
    }
}

fun roleLabel(role: SignRole): String = when (role) {
    SignRole.UNILITERAL -> "Alphabet sign · one sound"
    SignRole.BILITERAL -> "Two sounds"
    SignRole.TRILITERAL -> "Three sounds"
    SignRole.LOGOGRAM -> "Picture-word"
    SignRole.DETERMINATIVE -> "Determinative · silent"
    SignRole.NUMERAL -> "Number"
}

@Composable
private fun Framed(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Palette.sheetLight.copy(alpha = 0.8f))
            .border(2.dp, Palette.edge.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
            .padding(vertical = 22.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun SoundLine(translit: String?, said: String?, id: String, voice: Voice) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1f)) {
            translit?.let { Text(Translit.display(it), style = Type.translit.copy(fontSize = 30.sp, lineHeight = 36.sp)) }
            said?.let { Text("said “$it”", style = Type.bodySoft) }
        }
        if (voice.has(id)) {
            RoundIconButton(IconKind.SPEAKER, { voice.play(id) }, description = "Hear it")
            SlowButton { voice.play(id, slow = true) }
        }
    }
}

@Composable
private fun SignCard(sign: SignItem, voice: Voice, firstTime: Boolean) {
    Rubric(if (firstTime) "New sign" else "A sign to remember")
    Framed {
        Glyphs(sign.code, quadrat = 132.dp, color = { Palette.paint(sign.paint) }, description = sign.name)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(sign.name.replaceFirstChar { it.uppercase() }, style = Type.title, modifier = Modifier.weight(1f))
        Chip(sign.code, color = Palette.sheetDeep)
    }
    Chip(roleLabel(sign.role), color = Palette.lapis.copy(alpha = 0.12f), textColor = Palette.lapisDeep)
    when {
        sign.role == SignRole.NUMERAL -> {
            Text(Numerals.valueOf(sign.code)?.let(Numerals::format).orEmpty(), style = Type.display.copy(fontSize = 40.sp))
            SoundLine(sign.reading, sign.reading?.let(Pronounce::latin), sign.id, voice)
        }
        sign.sounds.isNotEmpty() -> SoundLine(
            sign.sounds.joinToString(" / "),
            if (sign.role == SignRole.UNILITERAL) Pronounce.letterLatin(sign.sounds[0]) else Pronounce.latin(sign.sounds[0]),
            sign.id,
            voice,
        )
    }
    sign.meaning?.let {
        if (sign.role != SignRole.NUMERAL) Text(
            if (sign.role == SignRole.DETERMINATIVE) "Marks: $it" else "Means: $it",
            style = Type.heading,
        )
    }
    sign.hint?.let { Text(Translit.display(it), style = Type.body) }
    sign.note?.let { Text(Translit.display(it), style = Type.bodySoft.copy(fontStyle = FontStyle.Italic)) }
}

/** How one sign works inside a word: its sound, or what it classifies. */
fun signRoleInWord(course: Course, code: String): String {
    val s = course.signFor(code) ?: return code
    return when {
        s.role == SignRole.DETERMINATIVE -> "silent · ${s.meaning}"
        s.role == SignRole.NUMERAL -> "stroke: “this sign means what it shows”"
        s.sounds.isNotEmpty() -> "${Translit.display(s.sounds[0])} · ${s.name}"
        else -> s.name
    }
}

@Composable
private fun Breakdown(mdc: String, course: Course) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.sheetDeep.copy(alpha = 0.45f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Rubric("Sign by sign", color = Palette.inkSoft)
        for (code in Mdc.parse(mdc).signs.distinct()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) { Glyphs(code, quadrat = 34.dp) }
                Text(signRoleInWord(course, code), style = Type.body, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun WordCard(word: WordItem, course: Course, voice: Voice, firstTime: Boolean) {
    Rubric(if (firstTime) "New word" else "A word to remember")
    Framed {
        Glyphs(word.mdc, quadrat = 84.dp, color = { s -> course.signFor(s.glyph.code)?.let { Palette.paint(it.paint) } ?: Palette.ink })
    }
    Text(word.gloss.replaceFirstChar { it.uppercase() }, style = Type.display)
    SoundLine(word.translit, Pronounce.latin(word.translit), word.id, voice)
    Breakdown(word.mdc, course)
    word.note?.let { Text(Translit.display(it), style = Type.bodySoft.copy(fontStyle = FontStyle.Italic)) }
}

/** A phrase laid out interlinear: each word's signs, sounds and meaning stacked in a column. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Interlinear(phrase: PhraseItem, voice: Voice, course: Course? = null) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        for (token in phrase.tokens) {
            Column(
                Modifier
                    .widthIn(min = 64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (token.wordId != null && voice.has(token.wordId)) {
                            Modifier.clickable { voice.play(token.wordId) }
                        } else Modifier,
                    )
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Glyphs(token.mdc, quadrat = 46.dp, color = { s ->
                    course?.signFor(s.glyph.code)?.let { Palette.paint(it.paint) } ?: Palette.ink
                })
                Spacer(Modifier.height(4.dp))
                Text(Translit.display(token.translit), style = Type.translit.copy(fontSize = 17.sp))
                Text(token.gloss, style = Type.small, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun PhraseCard(phrase: PhraseItem, course: Course, voice: Voice) {
    Rubric("New phrase")
    Framed { Interlinear(phrase, voice, course) }
    Text(phrase.translation.replaceFirstChar { it.uppercase() }, style = Type.title)
    SoundLine(phrase.translit, Pronounce.latin(phrase.translit), phrase.id, voice)
    phrase.literal?.let { Text("Word for word: $it", style = Type.bodySoft.copy(fontStyle = FontStyle.Italic)) }
    phrase.note?.let { Text(Translit.display(it), style = Type.body) }
    Text("Tap a word to hear it alone.", style = Type.small)
}

@Composable
private fun ConceptCard(concept: ConceptItem) {
    Rubric("How it works")
    Text(concept.title, style = Type.display)
    concept.mdc?.let { mdc ->
        Framed { Glyphs(mdc, quadrat = 64.dp, mirrored = concept.mirrored) }
    }
    Text(Translit.display(concept.body), style = Type.body.copy(fontSize = 17.sp, lineHeight = 26.sp))
}

/** The finale of a reading lesson: a whole real text, with every word tappable. */
@Composable
fun ReadTextCard(course: Course, textId: String, voice: Voice) {
    val text = course.text(textId) ?: return
    // Which word was last tapped, as (line, word): it stays lit while its sound plays.
    var tapped by remember { mutableStateOf<Pair<String, Int>?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Rubric("You can read this")
        Text(text.title, style = Type.display)
        Text(text.source, style = Type.small)
        for (lineId in text.lines) {
            val phrase = course[lineId] as? PhraseItem ?: continue
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Palette.sheetLight.copy(alpha = 0.75f))
                    .border(1.5.dp, Palette.edge.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Glyphs(
                    lineOf(phrase.tokens.map { it.mdc }),
                    quadrat = 46.dp,
                    color = { s -> course.signFor(s.glyph.code)?.let { Palette.paint(it.paint) } ?: Palette.ink },
                    highlightToken = tapped?.takeIf { it.first == lineId }?.second,
                    onToken = { i ->
                        tapped = lineId to i
                        voice.play(phrase.tokens[i].wordId)
                    },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Translit.display(phrase.translit), style = Type.translit, modifier = Modifier.weight(1f))
                    if (voice.has(phrase.id)) RoundIconButton(IconKind.SPEAKER, { voice.play(phrase.id) }, size = 40.dp, description = "Hear the line")
                }
                Text(phrase.translation, style = Type.heading)
            }
        }
        Text(text.note, style = Type.bodySoft.copy(fontStyle = FontStyle.Italic))
    }
}
