package com.krbavcic.pentalingo.core.ui

import android.content.Context
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.krbavcic.pentalingo.script.Inscription
import com.krbavcic.pentalingo.script.Layout
import com.krbavcic.pentalingo.script.Mdc
import com.krbavcic.pentalingo.script.PlacedSign
import com.krbavcic.pentalingo.script.Rect
import com.krbavcic.pentalingo.script.SignTable

private object HieroglyphFont {
    @Volatile
    private var typeface: Typeface? = null

    fun get(context: Context): Typeface = typeface ?: synchronized(this) {
        typeface ?: (ResourcesCompat.getFont(context, R.font.noto_sans_egyptian_hieroglyphs) ?: Typeface.DEFAULT)
            .also { typeface = it }
    }
}

/** One layout engine for the whole app; it holds no state beyond the sign table. */
val SharedLayout: Layout by lazy { Layout() }

private val inscriptionCache = object : LinkedHashMap<String, Inscription>(256, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Inscription>?) = size > 400
}

/** Lays out a spelling once and remembers it: the same words recur constantly in a session. */
fun inscriptionOf(mdc: String): Inscription = synchronized(inscriptionCache) {
    inscriptionCache.getOrPut(mdc) { SharedLayout.layout(mdc) }
}

fun lineOf(words: List<String>): Inscription = synchronized(inscriptionCache) {
    inscriptionCache.getOrPut(words.joinToString(" | ")) { SharedLayout.line(words.map(Mdc::parse)) }
}

/**
 * Draws an inscription with the bundled hieroglyph font, sign by sign, exactly where the layout
 * engine placed it.
 *
 * Each glyph is scaled so its *ink*, not its em box, fills the box it was given — the same
 * transform the JVM preview test uses, so what the layout tests prove is what the screen shows.
 *
 * @param quadrat the height of one quadrat; the whole thing shrinks to fit narrower space.
 * @param mirrored draw right-to-left. A mirror image is literally what Egyptian right-to-left
 *   writing is.
 * @param color per-sign colour, so learned signs can be painted and the rest left in ink.
 * @param onToken tap on a word of a multi-word line.
 */
@Composable
fun Glyphs(
    inscription: Inscription,
    modifier: Modifier = Modifier,
    quadrat: Dp = 48.dp,
    mirrored: Boolean = false,
    color: (PlacedSign) -> Color = { Palette.ink },
    ringColor: Color = Palette.ink,
    highlight: Int? = null,
    highlightToken: Int? = null,
    description: String? = null,
    onToken: ((Int) -> Unit)? = null,
    onSign: ((PlacedSign) -> Unit)? = null,
) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val fitted = if (inscription.width <= 0f) quadrat else {
            val maxQ = maxWidth / inscription.width
            if (maxWidth == Dp.Infinity || maxQ >= quadrat) quadrat else maxQ
        }
        val w = fitted * inscription.width
        val h = fitted * inscription.height
        val context = LocalContext.current
        val typeface = remember(context) { HieroglyphFont.get(context) }
        val paint = remember(typeface) {
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { this.typeface = typeface }
        }
        val qPx = with(density) { fitted.toPx() }
        var m = Modifier.size(w, h)
        if (description != null) m = m.semantics { contentDescription = description }
        if (onToken != null || onSign != null) {
            m = m.pointerInput(inscription, mirrored, qPx) {
                detectTapGestures { pos ->
                    val x = (if (mirrored) size.width - pos.x else pos.x) / qPx
                    val y = pos.y / qPx
                    if (onSign != null) {
                        // The nearest sign to the finger, so small signs are still easy to hit.
                        inscription.signs.minByOrNull { s ->
                            val dx = maxOf(s.box.x - x, 0f, x - s.box.right)
                            val dy = maxOf(s.box.y - y, 0f, y - s.box.bottom)
                            dx * dx + dy * dy
                        }?.let(onSign)
                    }
                    if (onToken != null) {
                        val hit = inscription.tokens.indexOfFirst { x >= it.x - 0.2f && x <= it.right + 0.2f }
                        if (hit >= 0) onToken(hit)
                    }
                }
            }
        }
        Canvas(m) {
            val unitsPerEm = SignTable.bundled.unitsPerEm
            scale(if (mirrored) -1f else 1f, 1f, pivot = Offset(size.width / 2, size.height / 2)) {
                if (highlightToken != null) inscription.tokens.getOrNull(highlightToken)?.let { t ->
                    drawRoundRect(
                        Palette.goldLight.copy(alpha = 0.55f),
                        topLeft = Offset(t.x * qPx - qPx * 0.12f, 0f),
                        size = Size(t.w * qPx + qPx * 0.24f, size.height),
                        cornerRadius = CornerRadius(qPx * 0.18f),
                    )
                }
                for (ring in inscription.rings) {
                    val stroke = qPx * 0.045f
                    val bar = qPx * 0.14f
                    val ovalW = ring.w * qPx - bar
                    drawRoundRect(
                        ringColor,
                        topLeft = Offset(ring.x * qPx + stroke / 2, ring.y * qPx + stroke / 2),
                        size = Size(ovalW - stroke, ring.h * qPx - stroke),
                        cornerRadius = CornerRadius(ring.h * qPx * 0.42f),
                        style = Stroke(stroke),
                    )
                    // The knot: a bar across the end of the loop.
                    val bx = ring.x * qPx + ovalW + bar * 0.35f
                    drawLine(ringColor, Offset(bx, ring.y * qPx + qPx * 0.08f), Offset(bx, (ring.y + ring.h) * qPx - qPx * 0.08f), stroke * 1.3f)
                }
                for (s in inscription.signs) {
                    val ink = s.glyph.ink
                    if (ink.height <= 0) continue
                    val k = s.box.h * qPx / ink.height
                    if (s.index == highlight) {
                        drawRoundRect(
                            Palette.goldLight.copy(alpha = 0.7f),
                            topLeft = Offset(s.box.x * qPx - qPx * 0.08f, s.box.y * qPx - qPx * 0.08f),
                            size = Size(s.box.w * qPx + qPx * 0.16f, s.box.h * qPx + qPx * 0.16f),
                            cornerRadius = CornerRadius(qPx * 0.12f),
                        )
                    }
                    paint.textSize = k * unitsPerEm
                    paint.color = color(s).toArgb()
                    drawContext.canvas.nativeCanvas.drawText(
                        s.glyph.text,
                        s.box.x * qPx - ink.xMin * k,
                        s.box.y * qPx + ink.yMax * k,
                        paint,
                    )
                }
            }
        }
    }
}

/**
 * A spelling in Manuel de Codage, drawn.
 *
 * A sign standing alone is shown as a specimen: scaled so its longer side fills most of the
 * quadrat. In a line of writing the bread loaf is rightly a third of the owl's height, but alone
 * on a tile at that size it is a crumb.
 */
@Composable
fun Glyphs(
    mdc: String,
    modifier: Modifier = Modifier,
    quadrat: Dp = 48.dp,
    mirrored: Boolean = false,
    color: (PlacedSign) -> Color = { Palette.ink },
    highlight: Int? = null,
    description: String? = null,
    onSign: ((PlacedSign) -> Unit)? = null,
) {
    val inscription = remember(mdc) { specimenOf(inscriptionOf(mdc)) }
    Glyphs(inscription, modifier, quadrat, mirrored, color, highlight = highlight, description = description, onSign = onSign)
}

/** The size a lone sign's longer side is scaled to, in quadrats. */
private const val SPECIMEN = 0.86f

/**
 * A lone sign framed by its own outline and scaled to [SPECIMEN], so every specimen has the same
 * visual weight whatever its natural size. Anything else is returned unchanged.
 */
private fun specimenOf(ins: Inscription): Inscription {
    val only = ins.signs.singleOrNull() ?: return ins
    if (ins.rings.isNotEmpty()) return ins
    val f = SPECIMEN / maxOf(only.box.w, only.box.h)
    val box = Rect(0f, 0f, only.box.w * f, only.box.h * f)
    return Inscription(box.w, box.h, listOf(only.copy(box = box)), emptyList(), listOf(box))
}
