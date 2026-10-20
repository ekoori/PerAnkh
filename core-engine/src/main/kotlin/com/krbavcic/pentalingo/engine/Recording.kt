package com.krbavcic.pentalingo.engine

import com.krbavcic.pentalingo.model.Award
import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.model.LessonRecord
import com.krbavcic.pentalingo.model.Memory
import com.krbavcic.pentalingo.model.Progress
import com.krbavcic.pentalingo.model.Requirement
import java.time.Instant
import java.time.ZoneId
import kotlin.math.max

/** A finished session folded into progress, and what it earned. */
data class Recorded(val progress: Progress, val newAwards: List<Award>)

/**
 * Applies a finished (or abandoned) session to the learner's progress.
 *
 * Every graded answer reaches the scheduler, including those from a session left half-way:
 * effort is never thrown away. Only a completed lesson is marked complete, and only a completed
 * reading counts as a text read.
 */
object Recording {

    fun grade(outcome: Outcome): Grade = when (outcome) {
        Outcome.CORRECT -> Grade.GOOD
        Outcome.NEAR -> Grade.HARD
        Outcome.WRONG -> Grade.AGAIN
    }

    fun epochDay(at: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(at).atZone(zone).toLocalDate().toEpochDay()

    fun apply(
        course: Course,
        progress: Progress,
        result: SessionResult,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Recorded {
        val memories = progress.memories.toMutableMap()
        for (a in result.attempts.sortedBy { it.at }) {
            val existing = memories[a.itemId]
            memories[a.itemId] = if (existing == null) {
                Scheduler.first(grade(a.outcome), a.tier, a.at)
            } else {
                Scheduler.review(existing, grade(a.outcome), a.tier, a.at)
            }
        }

        val lessons = progress.lessons.toMutableMap()
        val lessonId = result.lessonId
        // A lesson is done when its last sitting is: every item it teaches has been learned.
        val lesson = lessonId?.let(course::lesson)
        val allTaught = lesson != null && lesson.teach.all { it in memories }
        if (lessonId != null && result.completed && allTaught) {
            val old = lessons[lessonId]
            lessons[lessonId] = LessonRecord(
                completions = (old?.completions ?: 0) + 1,
                bestAccuracy = max(old?.bestAccuracy ?: 0.0, result.accuracy),
                firstCompleted = old?.firstCompleted ?: result.finishedAt,
                lastCompleted = result.finishedAt,
            )
        }

        val confusions = progress.confusions.toMutableMap()
        for ((a, b) in result.confusions) confusions.merge(Progress.confusionKey(a, b), 1, Int::plus)
        val counters = progress.counters.toMutableMap()
        for ((k, v) in result.tallies) counters.merge(k, v, Int::plus)

        val studied = result.graded > 0 || result.introduced.isNotEmpty()
        val updated = progress.copy(
            memories = memories,
            lessons = lessons,
            studyDays = if (studied) progress.studyDays + epochDay(result.finishedAt, zone) else progress.studyDays,
            confusions = confusions,
            counters = counters,
            textsRead = result.textRead?.let { progress.textsRead + it } ?: progress.textsRead,
            answered = progress.answered + result.graded,
            correct = progress.correct + result.correct,
            secondsStudied = progress.secondsStudied + ((result.finishedAt - result.startedAt) / 1000).coerceIn(0, 3 * 3600),
        )

        val earned = Awards.newlyEarned(course, updated, result.finishedAt)
        val withAwards = updated.copy(awards = updated.awards + earned.associate { it.id to result.finishedAt })
        return Recorded(withAwards, earned)
    }
}

/** How far along an award is, for the treasury's progress marks. */
data class AwardProgress(val current: Int, val target: Int) {
    val done: Boolean get() = current >= target
    val fraction: Float get() = if (target == 0) 1f else (current.toFloat() / target).coerceAtMost(1f)
}

object Awards {

    fun progress(award: Award, course: Course, progress: Progress, now: Long): AwardProgress {
        val k = Knowledge(course, progress, now)
        return when (val r = award.requirement) {
            is Requirement.Know -> AwardProgress(r.itemIds.count(k::isKnown), r.count)
            is Requirement.KnowKind -> AwardProgress(k.knownCount(r.kind), r.count)
            is Requirement.Master -> AwardProgress(k.masteredCount(), r.count)
            is Requirement.Tally -> AwardProgress(progress.counter(r.key), r.count)
            is Requirement.ReadText -> AwardProgress(if (r.textId in progress.textsRead) 1 else 0, 1)
            is Requirement.StudyDays -> AwardProgress(progress.studyDays.size, r.count)
            is Requirement.Recovered -> AwardProgress(
                progress.memories.values.count { it.everLapsed && it.stability >= Memory.KNOWN_DAYS * 3 },
                r.count,
            )
            is Requirement.Coverage -> AwardProgress((k.coverage() * 100).toInt(), r.percent)
            is Requirement.ClearedReview -> {
                val known = progress.memories.values.count { it.stability >= Memory.KNOWN_DAYS }
                AwardProgress(if (known >= r.minKnown && k.fadingCount() == 0) 1 else 0, 1)
            }
        }
    }

    fun newlyEarned(course: Course, progress: Progress, now: Long): List<Award> =
        course.awards.filter { it.id !in progress.awards && progress(it, course, progress, now).done }
}
