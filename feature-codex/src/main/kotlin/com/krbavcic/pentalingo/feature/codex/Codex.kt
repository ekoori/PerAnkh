package com.krbavcic.pentalingo.feature.codex

import androidx.compose.foundation.background
import com.krbavcic.pentalingo.model.Pos
import com.krbavcic.pentalingo.model.ConceptItem
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.krbavcic.pentalingo.core.ui.GoldBar
import com.krbavcic.pentalingo.core.ui.Glyphs
import com.krbavcic.pentalingo.core.ui.PageMargin
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

private enum class Shelf(val label: String) { SIGNS("Signs"), WORDS("Words"), GRAMMAR("Grammar") }

/** What a shelf can be narrowed to: groups of signs, kinds of word, tables or notes. */
private fun categoriesOf(shelf: Shelf): List<String> = when (shelf) {
    Shelf.SIGNS -> SIGN_GROUPS
    Shelf.WORDS -> Pos.entries.map(::posLabel)
    Shelf.GRAMMAR -> listOf(TABLES, NOTES)
}

private const val TABLES = "Tables"
private const val NOTES = "Other notes"

private fun posLabel(pos: Pos): String = when (pos) {
    Pos.NOUN -> "Nouns"
    Pos.VERB -> "Verbs"
    Pos.ADJECTIVE -> "Adjectives"
    Pos.PREPOSITION -> "Prepositions"
    Pos.PRONOUN -> "Pronouns"
    Pos.NAME -> "Names"
    Pos.PARTICLE -> "Particles"
    Pos.EXPRESSION -> "Expressions"
}

/** Signs are found by code or name, and once met by sound or meaning. */
private fun matches(k: Knowledge, sign: SignItem, query: String): Boolean {
    if (query.isBlank()) return true
    val q = query.trim().lowercase()
    if (sign.code.lowercase() == q || sign.name.lowercase().contains(q)) return true
    if (!k.isIntroduced(sign.id)) return false
    val sound = Translit.normalize(q)
    return (sound.isNotEmpty() && sign.sounds.any { Translit.normalize(it).startsWith(sound) }) ||
        sign.meaning?.lowercase()?.contains(q) == true
}

/** Words once met, by sound or sense. */
private fun matches(k: Knowledge, word: WordItem, query: String): Boolean {
    if (query.isBlank()) return true
    if (!k.isIntroduced(word.id)) return false
    val q = query.trim().lowercase()
    val sound = Translit.normalize(q)
    return (sound.isNotEmpty() && Translit.normalize(word.translit).contains(sound)) ||
        word.gloss.lowercase().contains(q) || word.accepts.any { it.lowercase().contains(q) }
}

/** Grammar cards once met, by title, text or anything in their table. */
private fun matches(k: Knowledge, concept: ConceptItem, query: String): Boolean {
    if (query.isBlank()) return true
    if (!k.isIntroduced(concept.id)) return false
    val q = query.trim().lowercase()
    val cells = concept.table?.rows.orEmpty().flatten() + listOfNotNull(concept.table?.note)
    return concept.title.lowercase().contains(q) || concept.body.lowercase().contains(q) ||
        cells.any { it.lowercase().contains(q) }
}

/**
 * Every sign, word and grammar card in the course, as a sign list that fills with colour.
 *
 * Unmet signs are ghosts, signs being learned are ink, known signs are painted in their tomb
 * colours — and the paint fades as recall drops, so the codex is also an honest map of what
 * needs review. A search field and categories find things in it; on a wide screen they stand in
 * a panel beside the shelf.
 */
@Composable
fun CodexScreen(
    knowledge: Knowledge,
    onSign: (SignItem) -> Unit,
    onWord: (WordItem) -> Unit,
    modifier: Modifier = Modifier,
    onConcept: (ConceptItem) -> Unit = {},
    /** Items of sections still sealed: not listed until their seal breaks, unless already met. */
    hidden: Set<String> = emptySet(),
) {
    var shelf by rememberSaveable { mutableStateOf(Shelf.SIGNS) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val course = knowledge.course
    val finder = Finder(shelf, { shelf = it; category = null }, query, { query = it }, category, { category = it })

    @Composable
    fun ShelfContent(header: (@Composable () -> Unit)?, sideMargin: Dp) = when (shelf) {
        Shelf.SIGNS -> SignShelf(knowledge, course, hidden, sideMargin, header, query, category, onSign)
        Shelf.WORDS -> WordShelf(knowledge, course, hidden, sideMargin, header, query, category, onWord)
        Shelf.GRAMMAR -> GrammarShelf(knowledge, course, hidden, sideMargin, header, query, category, onConcept)
    }

    PageMargin(modifier.fillMaxSize()) { margin ->
        if (maxWidth >= 840.dp) {
            // A tablet: the finder stands in a panel at the side, and the shelf has the rest.
            Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
                Column(
                    Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CodexHeader(knowledge, Modifier)
                    FinderControls(finder, vertical = true)
                }
                Box(Modifier.weight(1f).fillMaxHeight()) { ShelfContent(null, 8.dp) }
            }
        } else {
            // Where there is height to spare, the header stays above the shelf with its tabs, search
            // and categories. On a short screen (a tablet on its side) it would leave the signs a
            // single row, so it scrolls away with the shelf instead.
            val pinned = maxHeight >= 600.dp
            val listHeader: (@Composable () -> Unit)? = if (pinned) null else {
                {
                    Column(
                        Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CodexHeader(knowledge, Modifier)
                        FinderControls(finder, vertical = false)
                    }
                }
            }
            Column(Modifier.fillMaxSize()) {
                if (pinned) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(start = 20.dp + margin, end = 20.dp + margin, top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CodexHeader(knowledge, Modifier)
                        FinderControls(finder, vertical = false)
                    }
                }
                ShelfContent(listHeader, margin)
            }
        }
    }
}

/** The finder's state: which shelf, what is searched for, and which category, with their setters. */
private class Finder(
    val shelf: Shelf,
    val onShelf: (Shelf) -> Unit,
    val query: String,
    val onQuery: (String) -> Unit,
    val category: String?,
    val onCategory: (String?) -> Unit,
)

/** The codex's name, what its colours mean, and how much of a real page can be read. */
@Composable
private fun CodexHeader(knowledge: Knowledge, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Codex", style = Type.display)
        Text(
            "Ink while you are learning a sign; its paint once you remember it on another day. " +
                "Paint fades as memory does — review brings it back.",
            style = Type.small,
        )
        if (knowledge.course.frequency.isNotEmpty()) CoverageLine(knowledge)
    }
}

/** The shelves, a search field and the shelf's categories: in rows on a phone, a column in the side panel. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FinderControls(finder: Finder, vertical: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (vertical) {
            for (s in Shelf.entries) ShelfTab(s.label, finder.shelf == s, Modifier.fillMaxWidth()) { finder.onShelf(s) }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (s in Shelf.entries) ShelfTab(s.label, finder.shelf == s, Modifier.weight(1f)) { finder.onShelf(s) }
            }
        }
        SearchField(
            finder.query, finder.onQuery,
            when (finder.shelf) {
                Shelf.SIGNS -> "Search: G17, owl, m, water…"
                Shelf.WORDS -> "Search: nfr, good, house…"
                Shelf.GRAMMAR -> "Search: pronouns, sḏm=f, not…"
            },
        )
        val chips: @Composable () -> Unit = {
            CategoryChip("All", finder.category == null) { finder.onCategory(null) }
            for (c in categoriesOf(finder.shelf)) {
                CategoryChip(c, finder.category == c) { finder.onCategory(if (finder.category == c) null else c) }
            }
        }
        if (vertical) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, hint: String) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Palette.sheetLight)
            .border(1.5.dp, Palette.edge.copy(alpha = 0.7f), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, style = Type.body.copy(color = Palette.inkSoft))
            BasicTextField(
                value, onChange,
                singleLine = true,
                textStyle = Type.body,
                cursorBrush = SolidColor(Palette.lapis),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search the codex" },
            )
        }
        if (value.isNotEmpty()) {
            Text(
                "Clear", style = Type.label.copy(color = Palette.lapisDeep),
                modifier = Modifier.padding(start = 10.dp).clickable { onChange("") },
            )
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(
        label,
        style = Type.label.copy(fontSize = 13.sp, color = if (selected) Palette.sheetLight else Palette.ink),
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Palette.lapis else Palette.sheetLight)
            .border(1.dp, if (selected) Palette.lapis else Palette.edge.copy(alpha = 0.7f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

/**
 * How much of a real page the learner's words fill: progress told in terms that mean something
 * outside the app, from word frequencies counted in Middle Kingdom texts.
 */
@Composable
private fun CoverageLine(k: Knowledge) {
    val known = k.coverage()
    val settling = k.coverage(k::isIntroduced) - known
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Words you know fill ${percent(known)} of a real page", style = Type.heading, modifier = Modifier.weight(1f))
        }
        PageBar(known, settling)
        Text(
            buildString {
                append("Counted in 26,000 words of Middle Kingdom texts")
                if (settling >= 0.001) append("; another ${percent(settling)} is still settling in")
                append(". The whole course reaches ${percent(k.fullCoverage)}.")
            },
            style = Type.small,
        )
    }
}

/** A page's worth of text: gold for words known, pale gold for words still settling in. */
@Composable
private fun PageBar(known: Double, settling: Double) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(shape)
            .background(Palette.sheetDeep)
            .border(1.dp, Palette.edge.copy(alpha = 0.6f), shape),
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth((known + settling).toFloat().coerceIn(0f, 1f)).clip(shape).background(Palette.goldLight))
        Box(Modifier.fillMaxHeight().fillMaxWidth(known.toFloat().coerceIn(0f, 1f)).clip(shape).background(Palette.gold))
    }
}

internal fun percent(fraction: Double): String {
    val p = fraction * 100
    return if (p > 0 && p < 1) "%.1f%%".format(p) else "${Math.round(p)}%"
}

@Composable
private fun ShelfTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Tile(if (selected) TileState.SELECTED else TileState.IDLE, onClick, modifier.height(46.dp)) {
        Text(label, style = Type.label.copy(color = if (selected) Palette.lapisDeep else Palette.inkSoft))
    }
}

private val SIGN_GROUPS = listOf("The alphabet", "Signs of two and three, and picture-words", "Determinatives", "Numbers")

private fun groupOf(sign: SignItem): String = when (sign.role) {
    SignRole.UNILITERAL -> "The alphabet"
    SignRole.BILITERAL, SignRole.TRILITERAL, SignRole.LOGOGRAM -> "Signs of two and three, and picture-words"
    SignRole.DETERMINATIVE -> "Determinatives"
    SignRole.NUMERAL -> "Numbers"
}

@Composable
private fun SignShelf(
    k: Knowledge,
    course: Course,
    hidden: Set<String>,
    margin: Dp,
    header: (@Composable () -> Unit)?,
    query: String,
    category: String?,
    onSign: (SignItem) -> Unit,
) {
    val groups = remember(course, hidden, query, category, k) {
        course.signs.filter { it.id !in hidden || k.isIntroduced(it.id) }
            .filter { category == null || groupOf(it) == category }
            .filter { matches(k, it, query) }
            .groupBy(::groupOf)
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 78.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp + margin, end = 16.dp + margin, top = if (header == null) 16.dp else 0.dp, bottom = 16.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (header != null) item(span = { GridItemSpan(maxLineSpan) }) { header() }
        if (groups.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { NothingFound(query) }
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
private fun WordShelf(
    k: Knowledge,
    course: Course,
    hidden: Set<String>,
    margin: Dp,
    header: (@Composable () -> Unit)?,
    query: String,
    category: String?,
    onWord: (WordItem) -> Unit,
) {
    val order = remember(course, hidden, query, category, k) {
        val lessonIndex = course.lessons.flatMapIndexed { i, l -> l.teach.map { it to i } }.toMap()
        course.words.filter { it.id !in hidden || k.isIntroduced(it.id) }
            .filter { category == null || posLabel(it.pos) == category }
            .filter { matches(k, it, query) }
            .sortedBy { lessonIndex[it.id] ?: Int.MAX_VALUE }
    }
    androidx.compose.foundation.lazy.LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp + margin, end = 16.dp + margin, top = if (header == null) 16.dp else 0.dp, bottom = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (header != null) item { header() }
        item {
            val known = order.count { k.isKnown(it.id) }
            if (order.isEmpty()) NothingFound(query) else Text("$known of ${order.size} words known", style = Type.small)
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

@Composable
private fun NothingFound(query: String) {
    Text(
        if (query.isBlank()) "Nothing here yet." else "Nothing you have met matches “$query”.",
        style = Type.bodySoft,
        modifier = Modifier.padding(vertical = 12.dp),
    )
}

/**
 * The grammar cards, in course order: the tables first — pronouns, endings, the forms of the verb —
 * then the other notes, Egypt's gods, scribes and calendar among them. Each opens as it was taught.
 */
@Composable
private fun GrammarShelf(
    k: Knowledge,
    course: Course,
    hidden: Set<String>,
    margin: Dp,
    header: (@Composable () -> Unit)?,
    query: String,
    category: String?,
    onConcept: (ConceptItem) -> Unit,
) {
    val lessons = remember(course) { course.lessons.flatMap { l -> l.teach.map { it to l } }.toMap() }
    val sections = remember(course, hidden, query, category, k) {
        val cards = course.items.filterIsInstance<ConceptItem>()
            .filter { it.id !in hidden || k.isIntroduced(it.id) }
            .filter { matches(k, it, query) }
            .sortedBy { lessons[it.id]?.let(course.lessons::indexOf) ?: Int.MAX_VALUE }
        listOf(TABLES to cards.filter { it.table != null }, NOTES to cards.filter { it.table == null })
            .filter { (name, list) -> list.isNotEmpty() && (category == null || category == name) }
    }
    androidx.compose.foundation.lazy.LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp + margin, end = 16.dp + margin, top = if (header == null) 16.dp else 0.dp, bottom = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (header != null) item { header() }
        if (sections.isEmpty()) item { NothingFound(query) }
        for ((name, cards) in sections) {
            item(key = "section:$name") {
                Column(Modifier.padding(top = 10.dp)) {
                    Rubric(name)
                    Text("${cards.count { k.isIntroduced(it.id) }} of ${cards.size} met", style = Type.small)
                }
            }
            items(cards.size, key = { cards[it].id }) { i ->
                val card = cards[i]
                val met = k.isIntroduced(card.id)
                val lesson = lessons[card.id]
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (met) Palette.sheetLight.copy(alpha = 0.8f) else Palette.sheet.copy(alpha = 0.4f))
                        .border(1.5.dp, Palette.edge.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .clickable(enabled = met) { onConcept(card) }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (met) {
                        Text(card.title, style = Type.heading)
                        lesson?.let { Text("Lesson ${it.id.removePrefix("l")} · ${it.title}", style = Type.small) }
                    } else {
                        Text("Not met yet", style = Type.small)
                        lesson?.let { Text("Lesson ${it.id.removePrefix("l")}", style = Type.small) }
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
