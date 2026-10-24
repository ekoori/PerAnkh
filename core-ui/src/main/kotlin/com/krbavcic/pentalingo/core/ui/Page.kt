package com.krbavcic.pentalingo.core.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The widest a page grows. Per Ankh is drawn for a phone held upright; on a tablet a page keeps to
 * a column this wide in the middle of the papyrus, so lines stay short enough to read and answer
 * tiles stay the size of a fingertip rather than of a hand.
 */
val PageWidth: Dp = 600.dp

/**
 * Lays this out as a page: the whole width on a phone, a centred column [PageWidth] wide on
 * anything wider. Put it inside a scroll, so the papyrus either side still scrolls the page.
 */
fun Modifier.page(): Modifier =
    fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = PageWidth).fillMaxWidth()

/**
 * For pages that scroll as a lazy list or grid: [content] gets the margin to add on each side of
 * the list's own padding, which keeps it to a [PageWidth] column. It is zero on a phone. The
 * content can also see the space it has, as maxWidth and maxHeight.
 */
@Composable
fun PageMargin(modifier: Modifier = Modifier, content: @Composable BoxWithConstraintsScope.(margin: Dp) -> Unit) {
    BoxWithConstraints(modifier) {
        content(((maxWidth - PageWidth) / 2).coerceAtLeast(0.dp))
    }
}
