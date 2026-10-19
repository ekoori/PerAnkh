package com.krbavcic.pentalingo.content

import com.krbavcic.pentalingo.engine.Arrange
import com.krbavcic.pentalingo.engine.Exercise
import com.krbavcic.pentalingo.engine.MultipleChoice
import com.krbavcic.pentalingo.engine.Recording
import com.krbavcic.pentalingo.engine.Session
import com.krbavcic.pentalingo.engine.Teach
import com.krbavcic.pentalingo.engine.solution
import com.krbavcic.pentalingo.model.Progress
import java.io.File
import org.junit.Test

/**
 * Writes what sample lessons look like to `build/transcripts.txt`: the sequence of exercises a
 * learner meets, with kinds and tiers, so pacing and variety can be read rather than guessed.
 */
class TranscriptTest {

    private fun describe(ex: Exercise): String = when (ex) {
        is Teach -> "TEACH ${ex.itemId}"
        is MultipleChoice -> "${ex.kind}(t${ex.tier}) ${ex.itemId}: ${ex.instruction} [" +
            ex.choices.joinToString(" | ") { it.label ?: it.translit ?: it.mdc ?: "?" } + "]"
        is Arrange -> "${ex.kind}(t${ex.tier}) ${ex.itemId}: tiles=" + ex.tiles.joinToString(" ") { it.label ?: it.translit ?: it.mdc ?: "?" }
        else -> "${ex.kind}(t${ex.tier}) ${ex.itemId}"
    }

    @Test
    fun `write transcripts`() {
        val course = MIDDLE_EGYPTIAN
        val out = StringBuilder()
        var progress = Progress()
        var now = 1_760_000_000_000L
        for (lesson in course.lessons) {
            var sitting = 0
            while (!progress.isCompleted(lesson.id)) {
                sitting++
                val s = Session.forLesson(course, progress, lesson, now, seed = 99, clock = { now })
                val show = lesson.id in setOf("l1", "l2", "l7", "l16", "l29", "l31", "l40")
                if (show) out.appendLine("=== ${lesson.id} ${lesson.title}, sitting $sitting")
                var n = 0
                while (true) {
                    val ex = s.advance() ?: break
                    if (show) out.appendLine("  ${++n}. ${describe(ex)}")
                    s.submit(ex.solution())
                }
                progress = Recording.apply(course, progress, s.result()).progress
                now += 86_400_000L
            }
        }
        val r = Session.forReview(course, progress, now + 20 * 86_400_000L, seed = 5, clock = { now })!!
        out.appendLine("=== review after 20 days")
        var n = 0
        while (true) {
            val ex = r.advance() ?: break
            out.appendLine("  ${++n}. ${describe(ex)}")
            r.submit(ex.solution())
        }
        File("build/transcripts.txt").writeText(out.toString())
    }
}
