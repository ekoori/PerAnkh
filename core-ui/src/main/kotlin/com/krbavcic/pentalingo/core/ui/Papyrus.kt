package com.krbavcic.pentalingo.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

/**
 * The papyrus sheet everything sits on.
 *
 * Papyrus is two layers of pith strips pressed crosswise, so it shows a fine weave: long
 * horizontal fibres over fainter vertical ones, with the odd darker strip edge. That is drawn
 * here procedurally, once per size, from a fixed seed — the same sheet every time, and no
 * bitmap to ship or scale.
 */
fun Modifier.papyrus(base: Color = Palette.sheet, seed: Int = 7): Modifier = drawWithCache {
    val fibres = fibres(size, seed)
    val vignette = Brush.radialGradient(
        colors = listOf(Color.Transparent, Palette.edge.copy(alpha = 0.18f)),
        center = Offset(size.width / 2, size.height * 0.45f),
        radius = maxOf(size.width, size.height) * 0.75f,
    )
    onDrawBehind {
        drawRect(base)
        fibres.forEach { it.draw(this) }
        drawRect(vignette)
    }
}

private class Fibre(val start: Offset, val end: Offset, val width: Float, val color: Color) {
    fun draw(scope: DrawScope) = scope.drawLine(color, start, end, strokeWidth = width)
}

private fun fibres(size: Size, seed: Int): List<Fibre> {
    if (size.width <= 0f || size.height <= 0f) return emptyList()
    val rng = Random(seed)
    val out = mutableListOf<Fibre>()
    // The vertical under-layer: broad, faint strips.
    var x = 0f
    while (x < size.width) {
        val w = 28f + rng.nextFloat() * 46f
        out += Fibre(Offset(x, 0f), Offset(x, size.height), 1.4f, Palette.fiber.copy(alpha = 0.10f + rng.nextFloat() * 0.08f))
        x += w
    }
    // The horizontal top layer: many fine fibres, some long, some broken.
    var y = 0f
    while (y < size.height) {
        y += 2.5f + rng.nextFloat() * 6f
        val alpha = 0.05f + rng.nextFloat() * 0.13f
        val dark = rng.nextFloat() < 0.08f
        var sx = -20f
        while (sx < size.width) {
            val len = 60f + rng.nextFloat() * size.width
            val drift = (rng.nextFloat() - 0.5f) * 2f
            out += Fibre(
                Offset(sx, y), Offset(sx + len, y + drift),
                if (dark) 1.6f else 0.8f + rng.nextFloat() * 0.6f,
                (if (dark) Palette.edge else Palette.fiber).copy(alpha = if (dark) alpha + 0.12f else alpha),
            )
            sx += len + rng.nextFloat() * 40f
        }
    }
    return out
}

/** A full-screen sheet of papyrus. */
@Composable
fun PapyrusSheet(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.papyrus(), content = content)
}
