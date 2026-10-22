package com.krbavcic.pentalingo.feature.path

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.krbavcic.pentalingo.core.ui.Glyphs
import com.krbavcic.pentalingo.core.ui.PaintButton
import com.krbavcic.pentalingo.core.ui.Palette
import com.krbavcic.pentalingo.core.ui.Rubric
import com.krbavcic.pentalingo.core.ui.Type
import com.krbavcic.pentalingo.core.ui.papyrus
import com.krbavcic.pentalingo.core.ui.rememberSfx
import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.model.CourseSection
import kotlinx.coroutines.delay

/**
 * The moment a section opens: the seal on its door cracks and falls away, the doors part, and the
 * units inside are named for the first time. Shown once, when everything before it is done.
 */
@Composable
fun RevealScreen(course: Course, section: CourseSection, soundEffects: Boolean, onEnter: () -> Unit) {
    val sfx = rememberSfx(soundEffects)
    val crack = remember { Animatable(0f) }
    val open = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(700)
        crack.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        sfx.award()
        open.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
    }
    val number = course.sections.indexOf(section) + 1
    val units = course.unitsOf(section)
    val firstUnit = course.units.indexOfFirst { it.id == units.firstOrNull()?.id } + 1

    Box(Modifier.fillMaxSize().papyrus()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Rubric("Section $number")
            Text(section.title, style = Type.display, textAlign = TextAlign.Center)
            Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
                // Light from inside, as the doors part.
                Box(
                    Modifier
                        .size(210.dp)
                        .alpha(open.value)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Palette.goldLight, Palette.goldLight.copy(alpha = 0f)))),
                )
                Row(Modifier.width(232.dp).fillMaxHeight()) {
                    Leaf(Modifier.weight(1f).offset(x = (-118).dp * open.value).alpha(1f - 0.6f * open.value), left = true)
                    Leaf(Modifier.weight(1f).offset(x = 118.dp * open.value).alpha(1f - 0.6f * open.value), left = false)
                }
                SealStamp(
                    section.seal,
                    Modifier.graphicsLayer {
                        scaleX = 1f + 0.35f * crack.value
                        scaleY = 1f + 0.35f * crack.value
                        rotationZ = 28f * crack.value
                        alpha = 1f - crack.value
                    },
                )
            }
            AnimatedVisibility(open.value > 0.5f, enter = fadeIn() + slideInVertically { it / 4 }) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(section.promise, style = Type.body, textAlign = TextAlign.Center)
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        units.forEachIndexed { i, unit ->
                            val p = Palette.unit(unit.color)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Palette.sheetLight.copy(alpha = 0.8f))
                                    .border(2.dp, p.face, RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    Modifier.size(40.dp).clip(CircleShape).background(p.face),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Glyphs(unit.lessons.first().keySign, quadrat = 24.dp, color = { Palette.sheetLight })
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("Unit ${firstUnit + i} · ${unit.title}", style = Type.heading)
                                    Text(unit.subtitle, style = Type.small)
                                }
                            }
                        }
                    }
                    PaintButton("Enter", onEnter, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** One leaf of a stone door. */
@Composable
private fun Leaf(modifier: Modifier, left: Boolean) {
    val shape = if (left) RoundedCornerShape(topStart = 110.dp, bottomStart = 8.dp) else RoundedCornerShape(topEnd = 110.dp, bottomEnd = 8.dp)
    Box(
        modifier
            .fillMaxHeight()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Palette.sheetDeep, Palette.edge)))
            .border(2.dp, Palette.inkSoft.copy(alpha = 0.4f), shape),
    )
}
