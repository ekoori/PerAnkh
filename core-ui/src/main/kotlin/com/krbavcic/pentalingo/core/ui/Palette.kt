package com.krbavcic.pentalingo.core.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.krbavcic.pentalingo.model.Paint
import com.krbavcic.pentalingo.model.UnitColor

/**
 * A sheet of papyrus and a scribe's paint box.
 *
 * The ground is the warm cream of fresh papyrus; text is carbon ink; headings and emphasis are
 * red ochre, as on a real roll. The bright colours are the pigments of painted tombs — Egyptian
 * blue, gold, orange ochre, turquoise faience, malachite — and each has one job, so colour stays
 * a signal rather than decoration:
 *
 * - **lapis** is "you can act here": buttons, the current lesson, selection;
 * - **gold** is achievement: mastered signs, finished lessons, amulets;
 * - **turquoise** is "right";
 * - **ochre** is "nearly" and things in progress;
 * - **rubric** red is "not this" — the colour scribes used for headings, not a scolding.
 *
 * With [Contrast.high] (Settings → High contrast) the sheet turns plain and pale and every ink
 * and paint darkens to at least 4.5:1 against it: learners found brown on light brown hard to
 * read (the codex, Nov 2026). The faint ink of unlearned signs and the gold, malachite and ochre
 * paints fell below 3:1 on the papyrus.
 */
object Palette {
    private val high get() = Contrast.high

    val sheet get() = if (high) Color(0xFFFFFCF5) else Color(0xFFEFE2BF)
    val sheetLight get() = if (high) Color(0xFFFFFFFF) else Color(0xFFF7EED6)
    val sheetDeep get() = if (high) Color(0xFFF0E9DA) else Color(0xFFE2D0A2)
    val fiber = Color(0xFFC7AB72)
    val edge get() = if (high) Color(0xFF8C7246) else Color(0xFFB9975A)

    val ink get() = if (high) Color(0xFF110D09) else Color(0xFF2B2118)
    val inkSoft get() = if (high) Color(0xFF2E241A) else Color(0xFF5B4935)
    val inkFaint get() = if (high) Color(0xFF6A5A43) else Color(0xFF9A8463)
    val inkGhost = Color(0x332B2118)

    val rubric get() = if (high) Color(0xFF8A2A20) else Color(0xFFB3382B)
    val rubricDeep = Color(0xFF8A2A20)

    val gold = Color(0xFFDDAA2E)
    val goldDeep = Color(0xFFA9780E)
    val goldLight = Color(0xFFF6DA8A)

    val lapis = Color(0xFF2154A6)
    val lapisDeep = Color(0xFF173C7A)
    val lapisLight = Color(0xFF7FA2E0)

    val turquoise = Color(0xFF239B8E)
    val turquoiseDeep = Color(0xFF187066)
    val turquoiseLight = Color(0xFFBFE6DF)

    val ochre = Color(0xFFE5862D)
    val ochreDeep = Color(0xFFB2621A)
    val ochreLight = Color(0xFFF7D2A8)

    val malachite = Color(0xFF3F8F5B)
    val malachiteDeep = Color(0xFF2C6A42)
    val carnelian = Color(0xFFB9472F)

    /** The paint a learned sign is drawn in; darker under high contrast. */
    fun paint(p: Paint): Color = if (high) when (p) {
        Paint.CARNELIAN -> Color(0xFF8E3220)
        Paint.OCHRE -> Color(0xFF8A4A10)
        Paint.LAPIS -> Color(0xFF1C4791)
        Paint.TURQUOISE -> Color(0xFF0F5A50)
        Paint.MALACHITE -> Color(0xFF24603A)
        Paint.GOLD -> Color(0xFF7A5600)
        Paint.INK -> ink
    } else when (p) {
        Paint.CARNELIAN -> carnelian
        Paint.OCHRE -> ochreDeep
        Paint.LAPIS -> lapis
        Paint.TURQUOISE -> turquoiseDeep
        Paint.MALACHITE -> malachite
        Paint.GOLD -> goldDeep
        Paint.INK -> ink
    }

    data class Pigment(val face: Color, val deep: Color, val light: Color)

    fun unit(c: UnitColor): Pigment = when (c) {
        UnitColor.LAPIS -> Pigment(lapis, lapisDeep, lapisLight)
        UnitColor.OCHRE -> Pigment(ochre, ochreDeep, ochreLight)
        UnitColor.TURQUOISE -> Pigment(turquoise, turquoiseDeep, turquoiseLight)
        UnitColor.CARNELIAN -> Pigment(carnelian, rubricDeep, Color(0xFFF0C2B5))
        UnitColor.GOLD -> Pigment(gold, goldDeep, goldLight)
        UnitColor.MALACHITE -> Pigment(malachite, malachiteDeep, Color(0xFFC6E3CF))
    }

    /**
     * How a sign is coloured for a learner: ghostly before it is met, ink while it is being
     * learned, and painted once known — fading back towards ink as recall drops.
     */
    fun forRecall(paint: Paint, introduced: Boolean, known: Boolean, recall: Double): Color = when {
        !introduced -> inkFaint
        !known -> ink
        else -> lerp(ink, paint(paint), recall.toFloat().coerceIn(0.35f, 1f))
    }

    fun lerp(a: Color, b: Color, t: Float): Color = Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = a.alpha + (b.alpha - a.alpha) * t,
    )
}

/**
 * Reading options that change how everything is drawn: set once from the learner's settings and
 * read wherever colour is chosen, so a change repaints the whole app.
 */
object Contrast {
    var high: Boolean by mutableStateOf(false)
}
