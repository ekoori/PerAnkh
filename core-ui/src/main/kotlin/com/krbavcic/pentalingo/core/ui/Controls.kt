package com.krbavcic.pentalingo.core.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A painted button that stands up off the page and presses down when touched.
 *
 * The raised lip is the one bit of Duolingo worth keeping: it makes a tap feel like pressing
 * something, which matters on a screen where every answer is a tap.
 */
@Composable
fun PaintButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pigment: Palette.Pigment = Palette.unit(com.krbavcic.pentalingo.model.UnitColor.LAPIS),
    enabled: Boolean = true,
    textColor: Color = Palette.sheetLight,
    height: Dp = 54.dp,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val lift by animateDpAsState(if (pressed || !enabled) 0.dp else 4.dp, spring(stiffness = 1800f), label = "lift")
    val face = if (enabled) pigment.face else Palette.sheetDeep
    val deep = if (enabled) pigment.deep else Palette.edge.copy(alpha = 0.6f)
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .heightIn(min = height)
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        // The lip: the button's own darker side, showing below the face until it is pressed.
        Box(
            Modifier
                .matchParentSize()
                .padding(top = 4.dp)
                .clip(shape)
                .background(deep),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = height - 4.dp)
                .offset(y = 4.dp - lift)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Palette.lerp(face, Color.White, 0.12f), face))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                style = Type.label,
                color = if (enabled) textColor else Palette.inkFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }
    }
}

/** The look of an answer tile at each stage of answering. */
enum class TileState { IDLE, SELECTED, CORRECT, WRONG, NEAR, DIM }

/**
 * A tile to choose: a faience-glazed card on the papyrus. Its state colours follow the palette's
 * meanings, so a learner reads "right", "nearly" and "not this" before reading any words.
 */
@Composable
fun Tile(
    state: TileState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val (fill, line) = when (state) {
        TileState.IDLE -> Palette.sheetLight to Palette.edge
        TileState.SELECTED -> Color(0xFFE3EBFA) to Palette.lapis
        TileState.CORRECT -> Palette.turquoiseLight to Palette.turquoise
        TileState.WRONG -> Color(0xFFF5D3CC) to Palette.rubric
        TileState.NEAR -> Palette.ochreLight to Palette.ochre
        TileState.DIM -> Palette.sheet to Palette.edge.copy(alpha = 0.4f)
    }
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val sink by animateDpAsState(if (pressed) 3.dp else 0.dp, label = "sink")
    val alpha by animateFloatAsState(if (state == TileState.DIM) 0.45f else 1f, label = "dim")
    Box(
        modifier
            .offset(y = sink)
            .clip(RoundedCornerShape(14.dp))
            .background(line.copy(alpha = 0.55f * alpha))
            .padding(bottom = 3.dp - sink)
            .clip(RoundedCornerShape(14.dp))
            .background(fill.copy(alpha = if (state == TileState.DIM) 0.6f else 1f))
            .border(2.dp, line.copy(alpha = alpha), RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(source, null, role = Role.Button, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** A bar of gold leaf filling up. */
@Composable
fun GoldBar(fraction: Float, modifier: Modifier = Modifier, height: Dp = 14.dp, fill: Color = Palette.gold) {
    val shown by animateFloatAsState(fraction.coerceIn(0f, 1f), spring(stiffness = 120f), label = "bar")
    Box(
        modifier
            .height(height)
            .clip(CircleShape)
            .background(Palette.sheetDeep)
            .border(1.dp, Palette.edge.copy(alpha = 0.6f), CircleShape),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(shown)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Palette.goldLight, fill, Palette.goldDeep))),
        )
    }
}

/** A thin ring showing how far something has come. */
@Composable
fun ProgressRing(fraction: Float, modifier: Modifier = Modifier, color: Color = Palette.gold, track: Color = Palette.inkGhost, width: Dp = 4.dp) {
    Canvas(modifier) {
        val w = width.toPx()
        drawArc(track, 0f, 360f, false, topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w), style = Stroke(w))
        drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, topLeft = Offset(w / 2, w / 2),
            size = Size(size.width - w, size.height - w), style = Stroke(w, cap = StrokeCap.Round))
    }
}

/** A small label on a coloured ground. */
@Composable
fun Chip(text: String, modifier: Modifier = Modifier, color: Color = Palette.sheetDeep, textColor: Color = Palette.ink) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, style = Type.small, color = textColor)
    }
}

enum class IconKind { SPEAKER, CLOSE, BACK, CHECK, BACKSPACE, GEAR, SLOW, FLAG }

/** The handful of icons the app needs, drawn rather than imported. */
@Composable
fun DrawnIcon(kind: IconKind, modifier: Modifier = Modifier.size(24.dp), color: Color = Palette.ink) {
    Canvas(modifier) {
        val s = size.minDimension
        val w = s * 0.1f
        when (kind) {
            IconKind.SPEAKER -> {
                val p = Path().apply {
                    moveTo(s * 0.12f, s * 0.38f); lineTo(s * 0.3f, s * 0.38f); lineTo(s * 0.52f, s * 0.18f)
                    lineTo(s * 0.52f, s * 0.82f); lineTo(s * 0.3f, s * 0.62f); lineTo(s * 0.12f, s * 0.62f); close()
                }
                drawPath(p, color)
                drawArc(color, -45f, 90f, false, topLeft = Offset(s * 0.42f, s * 0.3f), size = Size(s * 0.36f, s * 0.4f), style = Stroke(w, cap = StrokeCap.Round))
                drawArc(color, -50f, 100f, false, topLeft = Offset(s * 0.4f, s * 0.14f), size = Size(s * 0.56f, s * 0.72f), style = Stroke(w, cap = StrokeCap.Round))
            }
            IconKind.SLOW -> {
                // A tortoise-ish dome: "play slowly".
                drawArc(color, 180f, 180f, true, topLeft = Offset(s * 0.18f, s * 0.3f), size = Size(s * 0.6f, s * 0.6f))
                drawCircle(color, s * 0.09f, Offset(s * 0.84f, s * 0.56f))
                drawLine(color, Offset(s * 0.12f, s * 0.62f), Offset(s * 0.9f, s * 0.62f), w)
            }
            IconKind.FLAG -> {
                drawLine(color, Offset(s * 0.24f, s * 0.12f), Offset(s * 0.24f, s * 0.9f), w * 1.2f, StrokeCap.Round)
                val p = Path().apply {
                    moveTo(s * 0.26f, s * 0.14f); lineTo(s * 0.82f, s * 0.24f); lineTo(s * 0.62f, s * 0.38f)
                    lineTo(s * 0.82f, s * 0.54f); lineTo(s * 0.26f, s * 0.5f); close()
                }
                drawPath(p, color)
            }
            IconKind.CLOSE -> {
                drawLine(color, Offset(s * 0.22f, s * 0.22f), Offset(s * 0.78f, s * 0.78f), w * 1.2f, StrokeCap.Round)
                drawLine(color, Offset(s * 0.78f, s * 0.22f), Offset(s * 0.22f, s * 0.78f), w * 1.2f, StrokeCap.Round)
            }
            IconKind.BACK -> {
                drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.82f, s * 0.5f), w * 1.2f, StrokeCap.Round)
                drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.45f, s * 0.25f), w * 1.2f, StrokeCap.Round)
                drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.45f, s * 0.75f), w * 1.2f, StrokeCap.Round)
            }
            IconKind.CHECK -> {
                drawLine(color, Offset(s * 0.18f, s * 0.52f), Offset(s * 0.42f, s * 0.76f), w * 1.4f, StrokeCap.Round)
                drawLine(color, Offset(s * 0.42f, s * 0.76f), Offset(s * 0.84f, s * 0.26f), w * 1.4f, StrokeCap.Round)
            }
            IconKind.BACKSPACE -> {
                val p = Path().apply {
                    moveTo(s * 0.3f, s * 0.22f); lineTo(s * 0.9f, s * 0.22f); lineTo(s * 0.9f, s * 0.78f)
                    lineTo(s * 0.3f, s * 0.78f); lineTo(s * 0.08f, s * 0.5f); close()
                }
                drawPath(p, color, style = Stroke(w))
                drawLine(color, Offset(s * 0.45f, s * 0.38f), Offset(s * 0.7f, s * 0.62f), w, StrokeCap.Round)
                drawLine(color, Offset(s * 0.7f, s * 0.38f), Offset(s * 0.45f, s * 0.62f), w, StrokeCap.Round)
            }
            IconKind.GEAR -> {
                drawCircle(color, s * 0.28f, style = Stroke(w * 1.2f))
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val c = Offset(s / 2, s / 2)
                    drawLine(color,
                        c + Offset((kotlin.math.cos(a) * s * 0.32f).toFloat(), (kotlin.math.sin(a) * s * 0.32f).toFloat()),
                        c + Offset((kotlin.math.cos(a) * s * 0.45f).toFloat(), (kotlin.math.sin(a) * s * 0.45f).toFloat()),
                        w * 1.3f, StrokeCap.Round)
                }
            }
        }
    }
}

/** A round button holding an icon, for audio and small actions. */
@Composable
fun RoundIconButton(
    kind: IconKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    fill: Color = Palette.lapis,
    tint: Color = Palette.sheetLight,
    description: String,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(fill)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        DrawnIcon(kind, Modifier.size(size * 0.55f), tint)
    }
}

/** A row of equal-width children, the basic 2-up grid for choices. */
@Composable
fun EvenRow(modifier: Modifier = Modifier, gap: Dp = 12.dp, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap), content = content)
}

/** A rubricated heading, in red ochre capitals as scribes marked a new section. */
@Composable
fun Rubric(text: String, modifier: Modifier = Modifier, color: Color = Palette.rubric) {
    Text(text.uppercase(), style = Type.rubric, color = color, modifier = modifier)
}

/** Fills its space with a soft dark scrim, for sheets and dialogs. */
@Composable
fun Scrim(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x66201810))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
    )
}
