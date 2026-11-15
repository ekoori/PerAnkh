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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.krbavcic.pentalingo.core.ui.Chip
import com.krbavcic.pentalingo.core.ui.GlyphLine
import com.krbavcic.pentalingo.core.ui.Glyphs
import com.krbavcic.pentalingo.core.ui.IconKind
import com.krbavcic.pentalingo.core.ui.PageMargin
import com.krbavcic.pentalingo.core.ui.Palette
import com.krbavcic.pentalingo.core.ui.ReportFlag
import com.krbavcic.pentalingo.core.ui.RoundIconButton
import com.krbavcic.pentalingo.core.ui.Rubric
import com.krbavcic.pentalingo.core.ui.Type
import com.krbavcic.pentalingo.core.ui.Voice
import com.krbavcic.pentalingo.core.ui.describeItem
import com.krbavcic.pentalingo.core.ui.lineOf
import com.krbavcic.pentalingo.core.ui.page
import com.krbavcic.pentalingo.engine.Knowledge
import com.krbavcic.pentalingo.model.PhraseItem
import com.krbavcic.pentalingo.model.GlyphColours
import com.krbavcic.pentalingo.engine.LineTints
import com.krbavcic.pentalingo.core.ui.glyphPaint
import com.krbavcic.pentalingo.core.ui.LocalGlyphColours
import com.krbavcic.pentalingo.model.ReadingText
import com.krbavcic.pentalingo.script.Translit

/**
 * The texts the course is building towards, and how much of each can be read today.
 *
 * This is the progress measure that means something: not lessons done but inscriptions you can
 * stand in front of and read.
 */
@Composable
fun LibraryScreen(
    knowledge: Knowledge,
    onOpen: (ReadingText) -> Unit,
    modifier: Modifier = Modifier,
    /** Texts of sections still sealed: not shown until their seal breaks. */
    hidden: Set<String> = emptySet(),
) {
    val course = knowledge.course
    val texts = course.texts.filter { it.id !in hidden || it.id in knowledge.progress.textsRead }
    // A long work's chapters stand together under its name, in story order, where its first chapter would be.
    val entries = buildList {
        val works = mutableSetOf<String>()
        for (text in texts) {
            val work = text.work
            if (work == null) add(listOf(text))
            else if (works.add(work)) add(texts.filter { it.work == work }.sortedBy { it.chapter })
        }
    }
    PageMargin(modifier.fillMaxSize()) { margin ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp + margin, end = 20.dp + margin, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 12.dp)) {
                    Text("Library", style = Type.display)
                    Text("Real texts, and how much of each you can read now.", style = Type.bodySoft)
                }
            }
            items(entries, key = { group -> group.first().work?.let { "work:$it" } ?: group.first().id }) { group ->
                val work = group.first().work
                if (work == null) TextCard(knowledge, group.first(), onOpen)
                else WorkCard(knowledge, work, group, onOpen)
            }
        }
    }
}

/** A long work: its name, how much of the whole can be read, and its chapters. */
@Composable
private fun WorkCard(knowledge: Knowledge, work: String, chapters: List<ReadingText>, onOpen: (ReadingText) -> Unit) {
    val all = chapters.map(knowledge::readability)
    val words = all.sumOf { it.words }
    val readable = all.sumOf { it.readableWords }
    val read = chapters.all { it.id in knowledge.progress.textsRead && it.lines.all(knowledge::isIntroduced) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(work, style = Type.title)
                Text(if (chapters.size == 1) "1 chapter so far" else "${chapters.size} chapters", style = Type.small)
            }
            if (read) Chip("Read", color = Palette.gold, textColor = Palette.ink)
        }
        Measured(if (words == 0) 0f else readable.toFloat() / words, "You can read $readable of $words words of the whole work")
        for (text in chapters) TextCard(knowledge, text, onOpen, label = "Chapter ${text.chapter}")
    }
}

@Composable
private fun TextCard(knowledge: Knowledge, text: ReadingText, onOpen: (ReadingText) -> Unit, label: String? = null) {
    val course = knowledge.course
    val r = knowledge.readability(text)
    // Read in full: a text that has grown since it was read waits for its new lines.
    val read = text.id in knowledge.progress.textsRead && text.lines.all(knowledge::isIntroduced)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.sheetLight.copy(alpha = 0.8f))
            .border(2.dp, if (r.complete) Palette.gold else Palette.edge.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .clickable { onOpen(text) }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                label?.let { Text(it, style = Type.small) }
                Text(text.title, style = Type.title)
                Text(text.source, style = Type.small)
            }
            if (read) Chip("Read", color = Palette.gold, textColor = Palette.ink)
        }
        val first = course[text.lines.first()] as? PhraseItem
        first?.let { line ->
            Glyphs(lineOf(line.tokens.map { it.mdc }), quadrat = 30.dp, color = { s ->
                course.signFor(s.glyph.code)?.let { signColor(knowledge, it) } ?: Palette.ink
            })
        }
        Measured(r.fraction, "You can read ${r.readableWords} of ${r.words} words · ${r.knownSigns} of ${r.signs} signs")
    }
}

/**
 * One text, with every word you know painted and tappable, and every word you don't left in
 * faint ink — so the page itself shows what there is still to learn.
 */
@Composable
fun ReaderScreen(knowledge: Knowledge, text: ReadingText, voice: Voice, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val course = knowledge.course
    var selected by remember { mutableStateOf<Pair<String, Int>?>(null) }
    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .page()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(IconKind.BACK, onBack, size = 40.dp, fill = Palette.sheetDeep, tint = Palette.inkSoft, description = "Back")
            Spacer(Modifier.size(12.dp))
            Rubric("Library", Modifier.weight(1f))
            ReportFlag({
                val out = linkedMapOf("screen" to "reader", "text" to "${text.id} · ${text.title}", "text.source" to text.source)
                selected?.let { (lineId, i) ->
                    out += describeItem(course, lineId, voice, "line")
                    (course[lineId] as? PhraseItem)?.tokens?.getOrNull(i)?.let { t ->
                        out["token"] = "${i + 1}: [${t.mdc}] ${t.translit} = ${t.gloss}"
                        voice.tokenClip(t)?.let { out["token.clip"] = "$it, ${voice.fileFor(it)}" }
                        out += describeItem(course, t.wordId, voice, "word")
                    }
                }
                out
            })
        }
        text.work?.let { Text("$it · chapter ${text.chapter}", style = Type.small) }
        Text(text.title, style = Type.display)
        Text(text.source, style = Type.small)
        val wholeTextRead = text.id in knowledge.progress.textsRead
        for (lineId in text.lines) {
            val phrase = course[lineId] as? PhraseItem ?: continue
            val readable = phrase.tokens.map { t -> knowledge.canRecall(phrase.id) || (t.wordId?.let(knowledge::canRecall) == true) }
            val sel = selected?.takeIf { it.first == lineId }?.second
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Palette.sheetLight.copy(alpha = 0.8f))
                    .border(1.5.dp, Palette.edge.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val tints = remember(phrase) { LineTints.grammar(course, phrase.tokens) }
                val mode = LocalGlyphColours.current
                val byRole = glyphPaint(course) { s -> tints.at(s.token, s.index) }
                GlyphLine(
                    phrase.tokens.map { it.mdc },
                    quadrat = 46.dp,
                    color = { s ->
                        when {
                            !readable[s.token] -> Palette.inkFaint
                            // The tomb paints here fade with memory, as in the codex.
                            mode == GlyphColours.PAINT -> course.signFor(s.glyph.code)?.let { signColor(knowledge, it) } ?: Palette.ink
                            else -> byRole(s)
                        }
                    },
                    highlightToken = sel,
                    onToken = { i ->
                        selected = lineId to i
                        if (readable[i]) voice.playToken(phrase.tokens[i])
                    },
                )
                sel?.let { i ->
                    val t = phrase.tokens[i]
                    Text(
                        if (readable[i]) "${Translit.display(t.translit)} — ${t.gloss}" else "A word you have not learned yet.",
                        style = Type.heading.copy(color = if (readable[i]) Palette.lapisDeep else Palette.inkSoft),
                    )
                }
                if (readable.all { it } || wholeTextRead) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Translit.display(phrase.translit), style = Type.translit, modifier = Modifier.weight(1f))
                        if (voice.has(phrase.id)) RoundIconButton(IconKind.SPEAKER, { voice.play(phrase.id) }, size = 40.dp, description = "Hear the line")
                    }
                    Text(phrase.translation, style = Type.heading)
                } else {
                    val missing = readable.count { !it }
                    Text("Learn $missing more ${if (missing == 1) "word" else "words"} to read this line.", style = Type.bodySoft)
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(text.note, style = Type.bodySoft.copy(fontStyle = FontStyle.Italic))
        }
        Spacer(Modifier.height(40.dp))
    }
}
