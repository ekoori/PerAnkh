package com.krbavcic.pentalingo

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.krbavcic.pentalingo.content.MIDDLE_EGYPTIAN
import com.krbavcic.pentalingo.core.ui.Glyphs
import com.krbavcic.pentalingo.core.ui.LocalReporter
import com.krbavcic.pentalingo.core.ui.Palette
import com.krbavcic.pentalingo.core.ui.Reporter
import com.krbavcic.pentalingo.core.ui.Scrim
import com.krbavcic.pentalingo.core.ui.Type
import com.krbavcic.pentalingo.core.ui.papyrus
import com.krbavcic.pentalingo.core.ui.rememberVoice
import com.krbavcic.pentalingo.data.ProgressStore
import com.krbavcic.pentalingo.data.Report
import com.krbavcic.pentalingo.data.ReportStore
import com.krbavcic.pentalingo.data.ReportUploader
import com.krbavcic.pentalingo.data.Settings
import com.krbavcic.pentalingo.engine.Knowledge
import com.krbavcic.pentalingo.engine.Recording
import com.krbavcic.pentalingo.engine.Session
import com.krbavcic.pentalingo.engine.SessionResult
import com.krbavcic.pentalingo.feature.codex.AwardDetail
import com.krbavcic.pentalingo.feature.codex.CodexScreen
import com.krbavcic.pentalingo.feature.codex.LibraryScreen
import com.krbavcic.pentalingo.feature.codex.ReaderScreen
import com.krbavcic.pentalingo.feature.codex.SignDetail
import com.krbavcic.pentalingo.feature.codex.TreasuryScreen
import com.krbavcic.pentalingo.feature.codex.WordDetail
import com.krbavcic.pentalingo.feature.lesson.LessonScreen
import com.krbavcic.pentalingo.feature.lesson.Summary
import com.krbavcic.pentalingo.feature.lesson.SummaryScreen
import com.krbavcic.pentalingo.feature.path.LessonSheet
import com.krbavcic.pentalingo.feature.path.PathScreen
import com.krbavcic.pentalingo.feature.path.RevealScreen
import com.krbavcic.pentalingo.model.Award
import com.krbavcic.pentalingo.model.CourseSection
import com.krbavcic.pentalingo.model.Lesson
import com.krbavcic.pentalingo.model.Progress
import com.krbavcic.pentalingo.model.ReadingText
import com.krbavcic.pentalingo.model.SignItem
import com.krbavcic.pentalingo.model.WordItem
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Tab(val label: String, val sign: String) {
    PATH("Path", "D54"),
    CODEX("Codex", "Y3"),
    LIBRARY("Library", "Y1"),
    TREASURY("Treasury", "S34"),
}

private sealed interface Screen {
    data object Home : Screen
    data class Playing(val session: Session, val lesson: Lesson?) : Screen
    data class Done(val summary: Summary) : Screen
    data class Reading(val text: ReadingText) : Screen
    /** Tester mode: a section's reveal played on request, changing nothing. */
    data class RevealPreview(val section: CourseSection) : Screen
}

private sealed interface Overlay {
    data class Preview(val lesson: Lesson) : Overlay
    data class SignInfo(val sign: SignItem) : Overlay
    data class WordInfo(val word: WordItem) : Overlay
    data class AwardInfo(val award: Award) : Overlay
    data object SettingsSheet : Overlay
    data class Message(val text: String) : Overlay
}

/**
 * The whole app: four tabs over the papyrus, and full-screen lessons on top.
 *
 * Navigation is a few pieces of state rather than a navigation library — there are four tabs,
 * one lesson flow and a reader, and the back stack is never deeper than two.
 */
@Composable
fun AppHost() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) { ProgressStore(context) }
    val reports = remember(context) { ReportStore(context) }
    val loaded by store.progress.collectAsState(initial = null)
    val settings by store.settings.collectAsState(initial = Settings())
    val course = MIDDLE_EGYPTIAN
    val voice = rememberVoice()

    var tab by rememberSaveable { mutableStateOf(Tab.PATH) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }

    // Recall decays with the clock, so "now" advances while the app is open.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = System.currentTimeMillis()
        }
    }

    val progress = loaded ?: run {
        Box(Modifier.fillMaxSize().papyrus())
        return
    }
    val knowledge = remember(progress, now) { Knowledge(course, progress, now) }

    // Sections after the first stay sealed until everything before them is done; their contents
    // are held back everywhere. Testers who may open any lesson see everything.
    val revealAll = settings.tester && settings.openAnyLesson
    val hidden = remember(progress, revealAll) { if (revealAll) emptySet() else course.sealedIds(progress) }
    // A section whose seal has broken opens with its own moment, once: whether it was just earned
    // or arrived in an update after everything before it was done.
    var revealedNow by remember { mutableStateOf(emptySet<String>()) }
    val toReveal = course.sections.drop(1).firstOrNull {
        it.id !in progress.revealed && it.id !in revealedNow && !course.isSealed(it, progress)
    }

    // Anyone can report a problem with the flag on a page. The report is kept on the phone and sent
    // at once; if the server cannot be reached it waits, and goes out the next time the app starts
    // or another report is sent.
    val build = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}${if (BuildConfig.DEBUG) ", debug" else ""})"
    val uploader = remember { ReportUploader(BuildConfig.REPORTS_URL, build) }
    var reportCount by remember { mutableIntStateOf(0) }
    var unsentCount by remember { mutableIntStateOf(0) }
    var sending by remember { mutableStateOf(false) }
    var sendResult by remember { mutableStateOf<String?>(null) }

    /**
     * Sends every report the server does not have yet, including any saved while sending. Returns
     * the last outcome, or null if another send was already running (it will pick these up).
     */
    suspend fun sendPending(): ReportUploader.Outcome? {
        if (sending) return null
        sending = true
        var last: ReportUploader.Outcome? = null
        try {
            while (true) {
                val pending = reports.unsent()
                if (pending.isEmpty()) break
                val outcome = uploader.upload(pending)
                last = outcome
                if (outcome is ReportUploader.Outcome.Sent) reports.markSent(outcome.stored)
                // Stop on failure, or when nothing more was taken (reports the server rejects).
                if (outcome !is ReportUploader.Outcome.Sent || outcome.stored.isEmpty()) break
            }
        } finally {
            unsentCount = reports.unsent().size
            reportCount = reports.all().size
            sending = false
        }
        return last
    }

    val reporter = remember {
        Reporter { category, note, ctx ->
            scope.launch {
                reports.add(Report(System.currentTimeMillis(), category, note, build, ctx, UUID.randomUUID().toString()))
                val message = when (sendPending()) {
                    is ReportUploader.Outcome.Sent -> "Report sent. Thank you!"
                    is ReportUploader.Outcome.Failed -> "Report saved. It will be sent once the app can reach the internet."
                    null -> "Report saved. It will be sent in a moment."
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }
    // Reports that could not go out last time.
    LaunchedEffect(Unit) { sendPending() }
    LaunchedEffect(overlay is Overlay.SettingsSheet) {
        if (overlay is Overlay.SettingsSheet) {
            reportCount = reports.all().size
            unsentCount = reports.unsent().size
        } else {
            sendResult = null
        }
    }

    fun record(result: SessionResult, lesson: Lesson?, show: Boolean) {
        scope.launch {
            var awards = emptyList<Award>()
            var after: Progress? = null
            store.update { current ->
                val rec = Recording.apply(course, current, result)
                awards = rec.newAwards
                after = rec.progress
                rec.progress
            }
            now = System.currentTimeMillis()
            if (!show) {
                screen = Screen.Home
                return@launch
            }
            val introduced = result.introduced.toSet()
            val finished = lesson != null && after?.isCompleted(lesson.id) == true
            val remaining = lesson?.teach?.count { after?.isIntroduced(it) != true } ?: 0
            screen = Screen.Done(
                Summary(
                    heading = when {
                        lesson == null -> "Review done"
                        finished -> "Lesson complete"
                        else -> "Part done"
                    },
                    subtitle = when {
                        lesson == null -> null
                        finished -> lesson.title
                        else -> "${lesson.title} · $remaining more to learn next time"
                    },
                    keySign = lesson?.keySign ?: "H6",
                    result = result,
                    learned = result.introduced.mapNotNull { course[it] },
                    refreshed = result.attempts.map { it.itemId }.filter { it !in introduced }.toSet().size,
                    awards = awards,
                    text = result.textRead?.let(course::text),
                ),
            )
        }
    }

    fun startLesson(lesson: Lesson) {
        overlay = null
        val stamp = System.currentTimeMillis()
        screen = Screen.Playing(Session.forLesson(course, progress, lesson, stamp), lesson)
    }

    fun startReview() {
        val stamp = System.currentTimeMillis()
        val session = Session.forReview(course, progress, stamp)
        if (session == null) {
            overlay = Overlay.Message("Learn a few signs first — then there will be something to review.")
        } else {
            screen = Screen.Playing(session, null)
        }
    }

    CompositionLocalProvider(LocalReporter provides reporter) {
        when (val s = screen) {
            is Screen.Playing -> {
                LessonScreen(
                    session = s.session,
                    soundEffects = settings.sounds,
                    autoplay = settings.autoplay,
                    onLeave = { result -> record(result, s.lesson, show = false) },
                    onFinish = { result -> record(result, s.lesson, show = true) },
                    debugSolve = settings.tester && settings.answerButton,
                )
                return@CompositionLocalProvider
            }
            is Screen.Done -> {
                BackHandler { screen = Screen.Home }
                SummaryScreen(s.summary, course, settings.sounds) { screen = Screen.Home }
                return@CompositionLocalProvider
            }
            is Screen.Reading -> {
                BackHandler { screen = Screen.Home }
                Box(Modifier.fillMaxSize().papyrus()) {
                    ReaderScreen(knowledge, s.text, voice, onBack = { screen = Screen.Home })
                }
                return@CompositionLocalProvider
            }
            is Screen.RevealPreview -> {
                BackHandler { screen = Screen.Home }
                RevealScreen(course, s.section, settings.sounds) { screen = Screen.Home }
                return@CompositionLocalProvider
            }
            Screen.Home -> if (toReveal != null) {
                RevealScreen(course, toReveal, settings.sounds) {
                    revealedNow = revealedNow + toReveal.id
                    scope.launch { store.update { it.copy(revealed = it.revealed + toReveal.id) } }
                    tab = Tab.PATH
                }
                return@CompositionLocalProvider
            }
        }

        Box(Modifier.fillMaxSize().papyrus()) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        Tab.PATH -> PathScreen(
                            course, progress, now,
                            onOpenLesson = { overlay = Overlay.Preview(it) },
                            onReview = ::startReview,
                            onSettings = { overlay = Overlay.SettingsSheet },
                            revealAll = revealAll,
                        )
                        Tab.CODEX -> CodexScreen(
                            knowledge,
                            onSign = { overlay = Overlay.SignInfo(it) },
                            onWord = { overlay = Overlay.WordInfo(it) },
                            hidden = hidden,
                        )
                        Tab.LIBRARY -> LibraryScreen(knowledge, onOpen = { screen = Screen.Reading(it) }, hidden = hidden)
                        Tab.TREASURY -> TreasuryScreen(knowledge, onAward = { overlay = Overlay.AwardInfo(it) }, hidden = hidden)
                    }
                }
                TabBar(tab) { tab = it }
            }

            val current = overlay
            if (current != null) {
                BackHandler { overlay = null }
                Scrim { overlay = null }
            }
            AnimatedVisibility(
                visible = current != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                when (current) {
                    is Overlay.Preview -> LessonSheet(
                        course, progress, current.lesson,
                        onStart = { startLesson(current.lesson) },
                        allowLocked = settings.tester && settings.openAnyLesson,
                    )
                    is Overlay.SignInfo -> SignDetail(knowledge, current.sign, voice)
                    is Overlay.WordInfo -> WordDetail(knowledge, current.word, voice)
                    is Overlay.AwardInfo -> AwardDetail(knowledge, current.award)
                    is Overlay.SettingsSheet -> SettingsSheet(
                        settings,
                        onSounds = { scope.launch { store.setSounds(it) } },
                        onAutoplay = { scope.launch { store.setAutoplay(it) } },
                        onReset = {
                            scope.launch { store.reset() }
                            overlay = null
                        },
                        onPrivacy = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_URL)))
                            } catch (e: ActivityNotFoundException) {
                                overlay = Overlay.Message("The privacy policy is at ${BuildConfig.PRIVACY_URL}")
                            }
                        },
                        reports = ReportOptions(
                            saved = reportCount,
                            unsent = unsentCount,
                            sending = sending,
                            sendResult = sendResult,
                            path = reports.path,
                            onSend = {
                                scope.launch {
                                    sendResult = when (val outcome = sendPending()) {
                                        is ReportUploader.Outcome.Sent -> {
                                            val n = outcome.stored.size
                                            "Sent $n ${if (n == 1) "report" else "reports"}. Thank you." +
                                                if (outcome.rejected > 0) " ${outcome.rejected} could not be accepted." else ""
                                        }
                                        is ReportUploader.Outcome.Failed -> outcome.reason
                                        null -> null
                                    }
                                }
                            },
                            onShare = {
                                scope.launch {
                                    val text = reports.export()
                                    if (text.isBlank()) return@launch
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Per Ankh tester reports")
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    }
                                    context.startActivity(Intent.createChooser(send, "Share reports"))
                                }
                            },
                            onClear = {
                                scope.launch {
                                    reports.clear()
                                    reportCount = 0
                                    unsentCount = 0
                                }
                            },
                        ),
                        tester = TesterOptions(
                            onTester = { scope.launch { store.setTester(it) } },
                            onOpenAnyLesson = { scope.launch { store.setOpenAnyLesson(it) } },
                            onAnswerButton = { scope.launch { store.setAnswerButton(it) } },
                            onPreviewReveal = course.sections.getOrNull(1)?.let { section ->
                                {
                                    overlay = null
                                    screen = Screen.RevealPreview(section)
                                }
                            },
                        ),
                    )
                    is Overlay.Message -> MessageSheet(current.text)
                    null -> Unit
                }
            }
        }
    }
}

@Composable
private fun TabBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Palette.sheetLight)
            .border(1.dp, Palette.edge.copy(alpha = 0.5f))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        for (tab in Tab.entries) {
            val on = tab == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (on) Palette.lapis.copy(alpha = 0.12f) else Palette.sheetLight)
                    .clickable(role = Role.Tab) { onSelect(tab) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // A fixed slot: lone signs are scaled as specimens, so their boxes differ in height.
                Box(Modifier.height(30.dp), contentAlignment = Alignment.Center) {
                    Glyphs(tab.sign, quadrat = 30.dp, color = { if (on) Palette.lapis else Palette.inkFaint })
                }
                Text(tab.label, style = Type.small.copy(color = if (on) Palette.lapisDeep else Palette.inkSoft))
            }
        }
    }
}

@Composable
private fun MessageSheet(text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(Palette.sheetLight)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(24.dp),
    ) {
        Text(text, style = Type.body)
    }
}
