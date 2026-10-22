package com.krbavcic.pentalingo.feature.path

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krbavcic.pentalingo.core.ui.Glyphs
import com.krbavcic.pentalingo.core.ui.IconKind
import com.krbavcic.pentalingo.core.ui.PaintButton
import com.krbavcic.pentalingo.core.ui.Palette
import com.krbavcic.pentalingo.core.ui.ProgressRing
import com.krbavcic.pentalingo.core.ui.RoundIconButton
import com.krbavcic.pentalingo.core.ui.Rubric
import com.krbavcic.pentalingo.core.ui.Type
import com.krbavcic.pentalingo.engine.Knowledge
import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.model.CourseSection
import com.krbavcic.pentalingo.model.CourseUnit
import com.krbavcic.pentalingo.model.ItemKind
import com.krbavcic.pentalingo.model.Lesson
import com.krbavcic.pentalingo.model.Progress
import com.krbavcic.pentalingo.model.UnitColor

/** Where a lesson stands for this learner. */
enum class LessonState { LOCKED, NEXT, STARTED, DONE }

fun lessonState(course: Course, progress: Progress, lesson: Lesson): LessonState {
    if (progress.isCompleted(lesson.id)) return LessonState.DONE
    val order = course.lessons
    val index = order.indexOf(lesson)
    val open = index == 0 || progress.isCompleted(order[index - 1].id)
    if (!open) return LessonState.LOCKED
    return if (lesson.teach.any(progress::isIntroduced)) LessonState.STARTED else LessonState.NEXT
}

/** How much of a lesson's material has been learned, 0..1. */
fun lessonFraction(progress: Progress, lesson: Lesson): Float =
    if (lesson.teach.isEmpty()) 0f else lesson.teach.count(progress::isIntroduced).toFloat() / lesson.teach.size

/** One row of the path below the header: a section heading, a sealed door, a unit banner or a lesson. */
private sealed interface PathRow {
    val key: String

    data class Heading(val number: Int, val section: CourseSection) : PathRow {
        override val key get() = "section-${section.id}"
    }

    data class Door(val number: Int, val section: CourseSection, val lessonsLeft: Int) : PathRow {
        override val key get() = "door-${section.id}"
    }

    data class Banner(val number: Int, val unit: CourseUnit) : PathRow {
        override val key get() = unit.id
    }

    data class Node(val lesson: Lesson, val unit: CourseUnit, val index: Int) : PathRow {
        override val key get() = lesson.id
    }
}

/**
 * The path, row by row. Each open section is headed by its name; the first sealed section shows
 * as a door, and nothing after it is shown at all. A course without sections is one open section.
 */
private fun pathRows(course: Course, progress: Progress, revealAll: Boolean): List<PathRow> = buildList {
    val sections = course.sections.ifEmpty { listOf(CourseSection("all", "", "", "", course.units.map { it.id })) }
    var unitNumber = 0
    for ((s, section) in sections.withIndex()) {
        if (!revealAll && course.isSealed(section, progress)) {
            val left = sections.take(s).flatMap(course::unitsOf).flatMap { it.lessons }.count { !progress.isCompleted(it.id) }
            add(PathRow.Door(s + 1, section, left))
            break
        }
        if (course.sections.size > 1) add(PathRow.Heading(s + 1, section))
        for (unit in course.unitsOf(section)) {
            add(PathRow.Banner(++unitNumber, unit))
            unit.lessons.forEachIndexed { i, lesson -> add(PathRow.Node(lesson, unit, i)) }
        }
    }
}

/**
 * The course as a path of medallions, one per lesson, through painted unit banners.
 *
 * Above it, what has been learned — counted in signs, words and readable texts, not points —
 * and, when memories are fading, the invitation to re-ink them.
 *
 * @param revealAll show sealed sections too (tester mode, where any lesson may be opened).
 */
@Composable
fun PathScreen(
    course: Course,
    progress: Progress,
    now: Long,
    onOpenLesson: (Lesson) -> Unit,
    onReview: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    revealAll: Boolean = false,
) {
    val knowledge = remember(progress, now / 60_000) { Knowledge(course, progress, now) }
    val listState = rememberLazyListState()
    val next = course.lessons.firstOrNull { !progress.isCompleted(it.id) }
    val rows = remember(course, progress, revealAll) { pathRows(course, progress, revealAll) }

    // Open on the learner's place in the course rather than at the top of unit one. The header
    // and the review card come before the rows.
    LaunchedEffect(Unit) {
        val row = rows.indexOfFirst { it is PathRow.Node && it.lesson == next }
        val target = row + 2
        if (row >= 0 && target > 4) listState.scrollToItem((target - 2).coerceAtLeast(0))
    }

    LazyColumn(
        modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Header(knowledge, progress, onSettings)
        }
        item {
            ReviewCard(knowledge, progress, onReview)
        }
        items(rows, key = { it.key }) { row ->
            when (row) {
                is PathRow.Heading -> SectionHeading(row.number, row.section)
                is PathRow.Door -> SealedDoor(row.number, row.section, row.lessonsLeft)
                is PathRow.Banner -> UnitBanner(row.number, row.unit, progress)
                is PathRow.Node -> LessonNode(
                    lesson = row.lesson,
                    unit = row.unit,
                    state = lessonState(course, progress, row.lesson),
                    fraction = lessonFraction(progress, row.lesson),
                    offsetIndex = row.index,
                    reading = row.lesson.reading != null,
                    onClick = { onOpenLesson(row.lesson) },
                )
            }
        }
        item { Spacer(Modifier.height(96.dp)) }
    }
}

/** Where a section begins: its number, its name and what it holds. */
@Composable
private fun SectionHeading(number: Int, section: CourseSection) {
    Column(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 30.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Rubric("Section $number")
        Text(section.title, style = Type.title, textAlign = TextAlign.Center)
        Text(section.promise, style = Type.bodySoft, textAlign = TextAlign.Center)
    }
}

/** The arch of a tomb door, or of a stela. */
private val DoorShape: Shape = RoundedCornerShape(topStart = 96.dp, topEnd = 96.dp, bottomStart = 10.dp, bottomEnd = 10.dp)

/**
 * The next section, sealed: its name and promise on a stone door with an unbroken clay seal, and
 * how far there is still to go. What is inside stays a surprise until the seal breaks.
 */
@Composable
private fun SealedDoor(number: Int, section: CourseSection, lessonsLeft: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 28.dp, top = 36.dp)
            .clip(DoorShape)
            .background(Brush.verticalGradient(listOf(Palette.sheetDeep, Palette.edge)))
            .border(3.dp, Palette.inkSoft.copy(alpha = 0.45f), DoorShape)
            .padding(horizontal = 22.dp, vertical = 28.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Section $number, ${section.title}, sealed" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Rubric("Section $number", color = Palette.rubricDeep)
        Text(section.title, style = Type.title, textAlign = TextAlign.Center)
        SealStamp(section.seal)
        Text(section.promise, style = Type.body, textAlign = TextAlign.Center)
        Text(
            if (lessonsLeft == 1) "The seal breaks when you finish the last lesson before it."
            else "The seal breaks when you finish the $lessonsLeft lessons before it.",
            style = Type.small,
            textAlign = TextAlign.Center,
        )
    }
}

/** A clay seal stamped over the cords that tie a door shut, as on Tutankhamun’s shrines. */
@Composable
internal fun SealStamp(sign: String, modifier: Modifier = Modifier, size: Dp = 84.dp) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.width(size * 2.6f).height(size)) {
            val mid = this.size.height / 2
            drawLine(Palette.ochreDeep, Offset(0f, mid - 10f), Offset(this.size.width, mid + 10f), 7f)
            drawLine(Palette.ochreDeep, Offset(0f, mid + 10f), Offset(this.size.width, mid - 10f), 7f)
        }
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Palette.lerp(Palette.carnelian, Color.White, 0.2f), Palette.carnelian, Palette.rubricDeep)))
                .border(3.dp, Palette.rubricDeep, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Glyphs(sign, quadrat = size * 0.5f, color = { Palette.sheetLight })
        }
    }
}

@Composable
private fun Header(knowledge: Knowledge, progress: Progress, onSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Per Ankh", style = Type.display)
                Rubric("Middle Egyptian")
            }
            RoundIconButton(IconKind.GEAR, onSettings, size = 42.dp, fill = Palette.sheetDeep, tint = Palette.inkSoft, description = "Settings")
        }
        // What could be read right now: learned and not yet faded. It falls as ink fades and
        // rises again with review — the honest number, and a reason to review.
        val course = knowledge.course
        val signs = course.signs.count { knowledge.canRecall(it.id) }
        val words = course.words.count { knowledge.canRecall(it.id) }
        val readable = course.texts.count { knowledge.readability(it).complete }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("G17", signs.toString(), if (signs == 1) "sign" else "signs", Modifier.weight(1f))
            Stat("Y1", words.toString(), if (words == 1) "word" else "words", Modifier.weight(1f))
            Stat("V10", readable.toString(), if (readable == 1) "text" else "texts", Modifier.weight(1f))
            val days = progress.studyDays.size
            Stat("R11", days.toString(), if (days == 1) "day" else "days", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(sign: String, value: String, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.sheetLight.copy(alpha = 0.75f))
            .border(1.dp, Palette.edge.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$value $label" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Glyphs(sign, quadrat = 20.dp, color = { Palette.goldDeep })
        Column {
            Text(value, style = Type.heading, maxLines = 1)
            Text(label, style = Type.small.copy(fontSize = 12.sp), maxLines = 1, softWrap = false)
        }
    }
}

/**
 * The review prompt. Fading ink is the metaphor and the truth: what is not revisited fades, and a
 * review re-inks it. Nothing is lost by skipping a day except a little colour.
 */
@Composable
private fun ReviewCard(knowledge: Knowledge, progress: Progress, onReview: () -> Unit) {
    if (progress.memories.isEmpty()) return
    val fading = knowledge.fadingCount()
    val due = knowledge.due().size
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (due > 0) Palette.lapis.copy(alpha = 0.1f) else Palette.sheetLight.copy(alpha = 0.6f))
            .border(2.dp, if (due > 0) Palette.lapis else Palette.edge.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (due > 0) {
            Text(
                if (fading > 0) "$fading known ${if (fading == 1) "thing is" else "things are"} fading" else "$due things are ready for review",
                style = Type.heading,
            )
            Text("A review now, while you can still recall them, does the most good.", style = Type.bodySoft)
            PaintButton("Re-ink them", onReview, Modifier.fillMaxWidth())
        } else {
            Text("Nothing is fading.", style = Type.heading)
            Text("Reviewing what you already remember well does little. Come back when the ink fades — or carry on below.", style = Type.bodySoft)
        }
    }
}

@Composable
private fun UnitBanner(number: Int, unit: CourseUnit, progress: Progress) {
    val p = Palette.unit(unit.color)
    val done = unit.lessons.count { progress.isCompleted(it.id) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 10.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Palette.lerp(p.face, Color.White, 0.1f), p.face)))
            .border(3.dp, p.deep, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val onFace = if (unit.color == UnitColor.GOLD) Palette.ink else Palette.sheetLight
        Text("UNIT $number", style = Type.rubric.copy(color = onFace.copy(alpha = 0.8f)))
        Text(unit.title, style = Type.title.copy(color = onFace))
        Text(unit.subtitle, style = Type.body.copy(color = onFace.copy(alpha = 0.9f)))
        Text(unit.intro, style = Type.small.copy(color = onFace.copy(alpha = 0.85f)))
        Text("$done of ${unit.lessons.size} lessons", style = Type.small.copy(color = onFace))
    }
}

/** A zigzag, as the path winds down the page. */
private val ZIG = listOf(0, 54, 92, 54, 0, -54, -92, -54)

private val StelaShape: Shape = RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp, bottomStart = 10.dp, bottomEnd = 10.dp)

@Composable
private fun LessonNode(
    lesson: Lesson,
    unit: CourseUnit,
    state: LessonState,
    fraction: Float,
    offsetIndex: Int,
    reading: Boolean,
    onClick: () -> Unit,
) {
    val p = Palette.unit(unit.color)
    val shape: Shape = if (lesson.checkpoint) StelaShape else CircleShape
    val x = ZIG[offsetIndex % ZIG.size].dp
    // A few breaths to say "start here", then still: an endless pulse costs battery and keeps
    // the screen from ever going idle.
    val glow = remember { Animatable(1f) }
    if (state == LessonState.NEXT) {
        LaunchedEffect(lesson.id) {
            repeat(4) {
                glow.animateTo(1.08f, tween(900))
                glow.animateTo(1f, tween(900))
            }
        }
    }
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The trail from the node above.
        Canvas(Modifier.height(18.dp).width(4.dp).offset(x = x)) {
            drawLine(
                Palette.edge, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
            )
        }
        Box(Modifier.offset(x = x), contentAlignment = Alignment.Center) {
            if (state == LessonState.STARTED) {
                ProgressRing(fraction, Modifier.size(100.dp), color = p.face, width = 6.dp)
            }
            val (face, rim, ink) = when (state) {
                LessonState.DONE -> Triple(Brush.radialGradient(listOf(Palette.goldLight, Palette.gold)), Palette.goldDeep, Palette.ink)
                LessonState.NEXT, LessonState.STARTED -> Triple(Brush.radialGradient(listOf(p.light, p.face)), p.deep, Palette.sheetLight)
                LessonState.LOCKED -> Triple(Brush.radialGradient(listOf(Palette.sheetLight, Palette.sheetDeep)), Palette.edge.copy(alpha = 0.6f), Palette.inkFaint)
            }
            Box(
                Modifier
                    .size(if (lesson.checkpoint) 92.dp else 80.dp)
                    .scale(if (state == LessonState.NEXT) glow.value else 1f)
                    .clip(shape)
                    .background(face)
                    .border(4.dp, rim, shape)
                    .clickable(onClick = onClick)
                    .semantics { contentDescription = "${lesson.title}, ${state.name.lowercase()}" },
                contentAlignment = Alignment.Center,
            ) {
                Glyphs(lesson.keySign, quadrat = 44.dp, color = { ink })
            }
            if (reading) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-4).dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Palette.ochre)
                        .border(2.dp, Palette.ochreDeep, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Glyphs("Y1", quadrat = 16.dp, color = { Palette.sheetLight }) }
            }
        }
        Text(
            lesson.title,
            style = Type.small.copy(color = if (state == LessonState.LOCKED) Palette.inkFaint else Palette.ink),
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(x = x).padding(top = 6.dp).width(160.dp),
        )
    }
}
