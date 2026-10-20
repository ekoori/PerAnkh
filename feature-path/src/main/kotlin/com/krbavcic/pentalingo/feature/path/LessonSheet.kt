package com.krbavcic.pentalingo.feature.path

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.krbavcic.pentalingo.core.ui.GoldBar
import com.krbavcic.pentalingo.core.ui.Glyphs
import com.krbavcic.pentalingo.core.ui.PaintButton
import com.krbavcic.pentalingo.core.ui.Palette
import com.krbavcic.pentalingo.core.ui.Rubric
import com.krbavcic.pentalingo.core.ui.Type
import com.krbavcic.pentalingo.model.ConceptItem
import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.model.Lesson
import com.krbavcic.pentalingo.model.PhraseItem
import com.krbavcic.pentalingo.model.Progress
import com.krbavcic.pentalingo.model.SignItem
import com.krbavcic.pentalingo.model.WordItem

/** A preview of a lesson, from the path: what it teaches, how far along it is, and the way in. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LessonSheet(
    course: Course,
    progress: Progress,
    lesson: Lesson,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    /** Debug builds only: open lessons out of order, to test later material on a device. */
    allowLocked: Boolean = false,
) {
    val state = lessonState(course, progress, lesson)
    val unit = course.unitOf(lesson.id)
    val pigment = unit?.let { Palette.unit(it.color) } ?: Palette.unit(com.krbavcic.pentalingo.model.UnitColor.LAPIS)
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(Palette.sheetLight)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        unit?.let { Rubric(it.title, color = pigment.deep) }
        Text(lesson.title, style = Type.display)
        Text(lesson.focus, style = Type.bodySoft)

        val previews = lesson.teach.mapNotNull { id ->
            when (val item = course[id]) {
                is SignItem -> item.code
                is WordItem -> item.mdc
                is PhraseItem -> null
                is ConceptItem -> null
                null -> null
            }
        }.take(8)
        if (previews.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                previews.forEach { mdc ->
                    Glyphs(mdc, quadrat = 40.dp, color = { s ->
                        val sign = course.signFor(s.glyph.code)
                        if (sign != null && progress.isIntroduced(sign.id)) Palette.paint(sign.paint) else Palette.ink
                    })
                }
            }
        }
        val learned = lesson.teach.count(progress::isIntroduced)
        if (lesson.teach.isNotEmpty()) {
            GoldBar(lessonFraction(progress, lesson), Modifier.fillMaxWidth())
            Text("$learned of ${lesson.teach.size} learned", style = Type.small)
        }
        if (lesson.checkpoint) {
            Text("A checkpoint reviews the whole unit at full difficulty. Nothing is locked behind it.", style = Type.small)
        }
        lesson.reading?.let { course.text(it) }?.let {
            Text("Ends by reading a real text: ${it.title}.", style = Type.small)
        }
        when (state) {
            LessonState.LOCKED -> if (allowLocked) {
                PaintButton("Open anyway (tester)", onStart, Modifier.fillMaxWidth(), pigment = pigment)
            } else {
                PaintButton("Finish the lesson before first", {}, Modifier.fillMaxWidth(), enabled = false)
            }
            LessonState.NEXT -> PaintButton("Start", onStart, Modifier.fillMaxWidth(), pigment = pigment)
            LessonState.STARTED -> PaintButton("Continue", onStart, Modifier.fillMaxWidth(), pigment = pigment)
            LessonState.DONE -> PaintButton(
                "Practise again", onStart, Modifier.fillMaxWidth(),
                pigment = Palette.unit(com.krbavcic.pentalingo.model.UnitColor.GOLD), textColor = Palette.ink,
            )
        }
    }
}
