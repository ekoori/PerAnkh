package com.krbavcic.pentalingo.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Noto Serif throughout: a book face suits a subject that lives in grammars and museum labels,
 * and it carries the transliteration letters (ḥ ḫ ẖ š ṯ ḏ) that a UI sans would draw from a
 * fallback font with different proportions.
 */
val Serif = FontFamily(
    Font(R.font.noto_serif_regular, FontWeight.Normal),
    Font(R.font.noto_serif_bold, FontWeight.Bold),
    Font(R.font.noto_serif_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.noto_serif_bold_italic, FontWeight.Bold, FontStyle.Italic),
)

object Type {
    val display = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, color = Palette.ink)
    val title = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 27.sp, color = Palette.ink)
    val heading = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp, color = Palette.ink)
    val body = TextStyle(fontFamily = Serif, fontSize = 16.sp, lineHeight = 23.sp, color = Palette.ink)
    val bodySoft = body.copy(color = Palette.inkSoft)
    val small = TextStyle(fontFamily = Serif, fontSize = 13.sp, lineHeight = 18.sp, color = Palette.inkSoft)
    val label = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.6.sp, color = Palette.ink)
    val translit = TextStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 18.sp, lineHeight = 24.sp, color = Palette.ink)
    val rubric = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.4.sp, color = Palette.rubric)
}

@Composable
fun PentalingoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Palette.lapis,
            onPrimary = Palette.sheetLight,
            secondary = Palette.gold,
            onSecondary = Palette.ink,
            tertiary = Palette.turquoise,
            background = Palette.sheet,
            onBackground = Palette.ink,
            surface = Palette.sheetLight,
            onSurface = Palette.ink,
            surfaceVariant = Palette.sheetDeep,
            onSurfaceVariant = Palette.inkSoft,
            outline = Palette.edge,
            error = Palette.rubric,
        ),
        typography = Typography(
            bodyLarge = Type.body,
            bodyMedium = Type.body,
            titleLarge = Type.title,
            labelLarge = Type.label,
        ),
        content = content,
    )
}
