package com.krbavcic.pentalingo.script

import kotlin.math.max
import kotlin.math.min

/** A rectangle in quadrat units: one quadrat is 1.0 tall. y grows downwards, as on screen. */
data class Rect(val x: Float, val y: Float, val w: Float, val h: Float) {
    val right: Float get() = x + w
    val bottom: Float get() = y + h

    fun contains(other: Rect, epsilon: Float = 1e-4f): Boolean =
        other.x >= x - epsilon && other.y >= y - epsilon &&
            other.right <= right + epsilon && other.bottom <= bottom + epsilon

    fun overlaps(other: Rect, epsilon: Float = 1e-4f): Boolean =
        x < other.right - epsilon && other.x < right - epsilon &&
            y < other.bottom - epsilon && other.y < bottom - epsilon
}

/**
 * One sign, placed.
 *
 * @property index position in reading order across the whole inscription, so a lesson can point
 *   at "the third sign" without knowing anything about geometry.
 * @property token which word of a multi-word line this sign belongs to.
 */
data class PlacedSign(
    val glyph: Glyph,
    val box: Rect,
    val index: Int,
    val token: Int,
)

/**
 * A laid-out line of hieroglyphs, in quadrat units.
 *
 * @property tokens the horizontal extent of each word, for tap targets and highlighting.
 * @property rings cartouche outlines; the tie bar sits at the right-hand end.
 */
data class Inscription(
    val width: Float,
    val height: Float,
    val signs: List<PlacedSign>,
    val rings: List<Rect>,
    val tokens: List<Rect>,
)

data class LayoutParams(
    val signGap: Float = 0.06f,
    val quadratGap: Float = 0.16f,
    val wordGap: Float = 0.55f,
    /** Wider groups are scaled down; a quadrat that sprawls stops reading as one unit. */
    val maxQuadratWidth: Float = 1.6f,
    val ringPad: Float = 0.16f,
    val ringBar: Float = 0.14f,
)

/**
 * Arranges signs into quadrats the way a scribe would, from the signs' real proportions.
 *
 * The rules are the simple core of what JSesh and the Egyptological fonts do: signs set side by
 * side are centred on each other vertically, stacked signs are centred horizontally, and any
 * group that outgrows its square is scaled down as a unit. Nothing is ever scaled *up* — a small
 * sign alone in its quadrat stays small, as it does on a monument.
 *
 * Writing runs left to right, which is the direction the font's signs face. Right-to-left
 * display is a mirror image, done by the renderer, because that is literally what it is.
 */
class Layout(
    private val table: SignTable = SignTable.bundled,
    private val params: LayoutParams = LayoutParams(),
) {

    private class Laid(val w: Float, val h: Float, val items: List<Pair<Glyph, Rect>>) {
        fun scaled(s: Float) = Laid(
            w * s, h * s,
            items.map { (g, r) -> g to Rect(r.x * s, r.y * s, r.w * s, r.h * s) },
        )

        fun moved(dx: Float, dy: Float): List<Pair<Glyph, Rect>> =
            items.map { (g, r) -> g to r.copy(x = r.x + dx, y = r.y + dy) }

        fun fit(maxW: Float, maxH: Float): Laid {
            val s = min(1f, min(maxW / w, maxH / h))
            return if (s < 1f) scaled(s) else this
        }
    }

    private val quadrat = table.quadrat.toFloat()

    private fun lay(group: Group): Laid = when (group) {
        is SignRef -> {
            val glyph = table.require(group.code)
            val w = glyph.ink.width / quadrat
            val h = glyph.ink.height / quadrat
            Laid(w, h, listOf(glyph to Rect(0f, 0f, w, h)))
        }

        is Beside -> {
            val parts = group.parts.map(::lay)
            val h = parts.maxOf { it.h }
            var x = 0f
            val items = parts.flatMap { part ->
                val moved = part.moved(x, (h - part.h) / 2f)
                x += part.w + params.signGap
                moved
            }
            Laid(x - params.signGap, h, items).fit(params.maxQuadratWidth, 1f)
        }

        is Stacked -> {
            val parts = group.parts.map(::lay)
            val w = parts.maxOf { it.w }
            var y = 0f
            val items = parts.flatMap { part ->
                val moved = part.moved((w - part.w) / 2f, y)
                y += part.h + params.signGap
                moved
            }
            Laid(w, y - params.signGap, items).fit(params.maxQuadratWidth, 1f)
        }
    }

    /** Lays out one spelling. */
    fun layout(writing: Writing): Inscription = line(listOf(writing))

    fun layout(mdc: String): Inscription = layout(Mdc.parse(mdc))

    /** Lays out several words as one line, with word spacing between them. */
    fun line(words: List<Writing>): Inscription {
        val hasRing = words.any { w -> w.elements.any { it is Cartouche } }
        val pad = params.ringPad
        // Lines with a cartouche grow so the ring has room; every sign keeps the 1.0 quadrat.
        val top = if (hasRing) pad else 0f
        val height = if (hasRing) 1f + 2 * pad else 1f

        val signs = mutableListOf<PlacedSign>()
        val rings = mutableListOf<Rect>()
        val tokens = mutableListOf<Rect>()
        var x = 0f
        var index = 0

        fun place(quadratGroup: Group, token: Int) {
            val laid = lay(quadratGroup).fit(params.maxQuadratWidth, 1f)
            for ((glyph, r) in laid.moved(x, top + (1f - laid.h) / 2f)) {
                signs += PlacedSign(glyph, r, index++, token)
            }
            x += laid.w
        }

        words.forEachIndexed { token, writing ->
            if (token > 0) x += params.wordGap
            val start = x
            writing.elements.forEachIndexed { i, element ->
                if (i > 0) x += params.quadratGap
                when (element) {
                    is Quadrat -> place(element.group, token)
                    is Cartouche -> {
                        val ringStart = x
                        x += pad
                        element.quadrats.forEachIndexed { j, q ->
                            if (j > 0) x += params.quadratGap
                            place(q.group, token)
                        }
                        x += pad + params.ringBar
                        rings += Rect(ringStart, 0f, x - ringStart, height)
                    }
                }
            }
            tokens += Rect(start, 0f, x - start, height)
        }

        return Inscription(width = max(x, 0f), height = height, signs = signs, rings = rings, tokens = tokens)
    }
}
