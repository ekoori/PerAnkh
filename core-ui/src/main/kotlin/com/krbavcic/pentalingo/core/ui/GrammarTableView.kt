package com.krbavcic.pentalingo.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.model.GrammarTable
import com.krbavcic.pentalingo.model.Token
import com.krbavcic.pentalingo.script.PlacedSign
import com.krbavcic.pentalingo.script.Pronounce
import com.krbavcic.pentalingo.script.Translit

/**
 * A card's table: the pattern at a glance — pronouns, endings, forms — read down its rows.
 * Hieroglyph cells are drawn, transliteration set in italics, the rest as plain text.
 *
 * Each Egyptian form shows how it is said, and with a [voice] and the [course] it can be heard:
 * tap it. It plays the recording of the word, sign or sentence that says it, or the form's own.
 */
@Composable
fun GrammarTableView(
    table: GrammarTable,
    modifier: Modifier = Modifier,
    glyphColor: (PlacedSign) -> Color = { Palette.ink },
    course: Course? = null,
    voice: Voice? = null,
) {
    val shape = RoundedCornerShape(14.dp)
    fun weight(column: Int) = when (table.kinds[column]) {
        GrammarTable.Cell.GLYPHS -> 1.1f
        GrammarTable.Cell.TRANSLIT -> 1f
        GrammarTable.Cell.TEXT -> 1.6f
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Palette.sheetLight.copy(alpha = 0.85f))
            .border(1.dp, Palette.edge.copy(alpha = 0.6f), shape),
    ) {
        Row(
            Modifier.fillMaxWidth().background(Palette.sheetDeep.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            table.columns.forEachIndexed { i, title ->
                Text(title, style = Type.label.copy(fontSize = 13.sp, color = Palette.inkSoft), modifier = Modifier.weight(weight(i)))
            }
        }
        table.rows.forEachIndexed { r, row ->
            if (r > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.edge.copy(alpha = 0.25f)))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.forEachIndexed { i, cell ->
                    Box(Modifier.weight(weight(i)).padding(end = 8.dp)) {
                        when (table.kinds[i]) {
                            GrammarTable.Cell.GLYPHS ->
                                if (cell.isNotBlank()) Glyphs(cell, quadrat = 28.dp, color = glyphColor)
                            GrammarTable.Cell.TRANSLIT -> SaidCell(cell, table.spelling(row), course, voice)
                            GrammarTable.Cell.TEXT -> Text(cell, style = Type.body.copy(fontSize = 15.sp, lineHeight = 20.sp))
                        }
                    }
                }
            }
        }
        table.note?.let {
            Text(
                Translit.display(it),
                style = Type.small,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 10.dp),
            )
        }
    }
}

/**
 * An Egyptian form in a table: in italics, how it is said beneath it, and a speaker when it can be
 * heard. A lone consonant is said as its letter ("ḥe"), anything longer as a word.
 */
@Composable
private fun SaidCell(cell: String, spelling: String?, course: Course?, voice: Voice?) {
    val sayable = GrammarTable.sayable(cell)
    val clip = remember(cell, spelling, course, voice) {
        if (!sayable || course == null || voice == null) null
        else course.itemSaying(cell, spelling)?.id?.takeIf(voice::has) ?: Token.formClipId(cell).takeIf(voice::has)
    }
    Column(
        if (clip != null && voice != null) {
            Modifier.clickable(role = Role.Button, onClickLabel = "Hear it") { voice.play(clip) }
        } else Modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Translit.display(cell), style = Type.translit.copy(fontSize = 17.sp, lineHeight = 22.sp))
            if (clip != null) DrawnIcon(IconKind.SPEAKER, Modifier.padding(start = 5.dp).size(15.dp), Palette.lapis)
        }
        if (sayable) {
            val said = if (Translit.display(cell).length == 1) Pronounce.letterLatin(cell) else Pronounce.latin(cell)
            Text(said, style = Type.small.copy(fontSize = 12.sp, lineHeight = 15.sp, color = Palette.inkSoft))
        }
    }
}
