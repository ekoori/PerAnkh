package com.krbavcic.pentalingo.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krbavcic.pentalingo.model.GrammarTable
import com.krbavcic.pentalingo.script.PlacedSign
import com.krbavcic.pentalingo.script.Translit

/**
 * A grammar card's table: the pattern at a glance — pronouns, endings, forms — read down its rows.
 * Hieroglyph cells are drawn, transliteration set in italics, the rest as plain text.
 */
@Composable
fun GrammarTableView(
    table: GrammarTable,
    modifier: Modifier = Modifier,
    glyphColor: (PlacedSign) -> Color = { Palette.ink },
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
                            GrammarTable.Cell.TRANSLIT ->
                                Text(Translit.display(cell), style = Type.translit.copy(fontSize = 17.sp, lineHeight = 22.sp))
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
