package com.krbavcic.pentalingo

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.krbavcic.pentalingo.core.ui.Palette
import com.krbavcic.pentalingo.core.ui.Scrim
import com.krbavcic.pentalingo.core.ui.Type
import com.krbavcic.pentalingo.core.ui.papyrus
import com.krbavcic.pentalingo.core.ui.rememberVoice
import com.krbavcic.pentalingo.data.ProgressStore
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
import com.krbavcic.pentalingo.model.Award
import com.krbavcic.pentalingo.model.Lesson
import com.krbavcic.pentalingo.model.Progress
import com.krbavcic.pentalingo.model.ReadingText
import com.krbavcic.pentalingo.model.SignItem
import com.krbavcic.pentalingo.model.WordItem
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

    when (val s = screen) {
        is Screen.Playing -> {
            LessonScreen(
                session = s.session,
                soundEffects = settings.sounds,
                autoplay = settings.autoplay,
                onLeave = { result -> record(result, s.lesson, show = false) },
                onFinish = { result -> record(result, s.lesson, show = true) },
                debugSolve = BuildConfig.DEBUG,
            )
            return
        }
        is Screen.Done -> {
            BackHandler { screen = Screen.Home }
            SummaryScreen(s.summary, course, settings.sounds) { screen = Screen.Home }
            return
        }
        is Screen.Reading -> {
            BackHandler { screen = Screen.Home }
            Box(Modifier.fillMaxSize().papyrus()) {
                ReaderScreen(knowledge, s.text, voice, onBack = { screen = Screen.Home })
            }
            return
        }
        Screen.Home -> Unit
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
                    )
                    Tab.CODEX -> CodexScreen(
                        knowledge,
                        onSign = { overlay = Overlay.SignInfo(it) },
                        onWord = { overlay = Overlay.WordInfo(it) },
                    )
                    Tab.LIBRARY -> LibraryScreen(knowledge, onOpen = { screen = Screen.Reading(it) })
                    Tab.TREASURY -> TreasuryScreen(knowledge, onAward = { overlay = Overlay.AwardInfo(it) })
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
                    allowLocked = BuildConfig.DEBUG,
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
                )
                is Overlay.Message -> MessageSheet(current.text)
                null -> Unit
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
