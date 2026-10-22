package com.krbavcic.pentalingo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.krbavcic.pentalingo.core.ui.Glyphs
import com.krbavcic.pentalingo.core.ui.PaintButton
import com.krbavcic.pentalingo.core.ui.Palette
import com.krbavcic.pentalingo.core.ui.Rubric
import com.krbavcic.pentalingo.core.ui.Type
import com.krbavcic.pentalingo.data.Release
import com.krbavcic.pentalingo.data.Settings
import com.krbavcic.pentalingo.data.compareVersions
import com.krbavcic.pentalingo.model.UnitColor

/** Problem reports kept on this phone: how many, how many are still waiting to go out. */
class ReportOptions(
    val saved: Int,
    val unsent: Int,
    val sending: Boolean,
    val sendResult: String?,
    val path: String,
    val onSend: () -> Unit,
    val onShare: () -> Unit,
    val onClear: () -> Unit,
)

/** Tester mode's switches. */
class TesterOptions(
    val onTester: (Boolean) -> Unit,
    val onOpenAnyLesson: (Boolean) -> Unit,
    val onAnswerButton: (Boolean) -> Unit,
    /** Plays the moment a sealed section opens, without opening it; null if there is no second section. */
    val onPreviewReveal: (() -> Unit)? = null,
)

/** What the About page knows about the newest release. */
sealed interface LatestRelease {
    data object NotChecked : LatestRelease
    data object Checking : LatestRelease
    data class Found(val release: Release) : LatestRelease

    /** Neither GitHub nor the Per Ankh server answered. */
    data object Unreachable : LatestRelease
}

/** The About page: this build, the newest release, and where to read more. */
class AboutOptions(
    /** The version installed, as released: "0.3.0". */
    val version: String,
    /** The same with its build number, as shown: "0.3.0 (300)". */
    val build: String,
    val latest: LatestRelease,
    val onCheck: () -> Unit,
    /** Opens a web page in the browser. */
    val onOpen: (String) -> Unit,
    /** The newest release's page, whichever it is, for when it cannot be looked up. */
    val latestUrl: String,
    val projectUrl: String,
    val privacyUrl: String,
)

private enum class Page(val label: String) { SETTINGS("Settings"), TESTING("Testing"), ABOUT("About") }

@Composable
fun SettingsSheet(
    settings: Settings,
    onSounds: (Boolean) -> Unit,
    onAutoplay: (Boolean) -> Unit,
    onReset: () -> Unit,
    reports: ReportOptions,
    tester: TesterOptions,
    about: AboutOptions,
) {
    var page by rememberSaveable { mutableStateOf(Page.SETTINGS) }
    Column(
        Modifier
            .fillMaxWidth()
            // One height for every page, so the tabs stay where the finger left them.
            .fillMaxHeight(0.8f)
            .heightIn(max = 660.dp)
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(Palette.sheetLight)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        PageTabs(page) { page = it }
        // Each page scrolls on its own, from the top.
        key(page) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (page) {
                    Page.SETTINGS -> SettingsPage(settings, onSounds, onAutoplay, onReset, reports)
                    Page.TESTING -> TestingPage(settings, reports, tester)
                    Page.ABOUT -> AboutPage(about)
                }
            }
        }
    }
}

/** The pages' names, picked the way the app's own tabs are. */
@Composable
private fun PageTabs(selected: Page, onSelect: (Page) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (page in Page.entries) {
            val on = page == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (on) Palette.lapis.copy(alpha = 0.12f) else Palette.sheetLight)
                    .selectable(selected = on, role = Role.Tab) { onSelect(page) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(page.label, style = Type.heading.copy(color = if (on) Palette.lapisDeep else Palette.inkSoft))
            }
        }
    }
    Box(
        Modifier
            .padding(start = 22.dp, end = 22.dp, top = 10.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(Palette.edge.copy(alpha = 0.35f)),
    )
}

@Composable
private fun SettingsPage(
    settings: Settings,
    onSounds: (Boolean) -> Unit,
    onAutoplay: (Boolean) -> Unit,
    onReset: () -> Unit,
    reports: ReportOptions,
) {
    var confirming by remember { mutableStateOf(false) }
    Toggle("Sound effects", "Harp plucks for right and wrong answers", settings.sounds, onSounds)
    Toggle("Speak automatically", "Play each word’s pronunciation as it appears", settings.autoplay, onAutoplay)

    // Reports go out as soon as they are written; this shows only while some are still waiting.
    if (reports.unsent > 0) {
        val n = reports.unsent
        Text("$n ${if (n == 1) "report is" else "reports are"} waiting to be sent", style = Type.heading)
        PaintButton(
            if (reports.sending) "Sending…" else "Send now",
            reports.onSend, Modifier.fillMaxWidth(), enabled = !reports.sending,
        )
    }
    reports.sendResult?.let { Text(it, style = Type.body) }

    Rubric("Start again")
    if (!confirming) {
        PaintButton(
            "Reset all progress", { confirming = true }, Modifier.fillMaxWidth(),
            pigment = Palette.Pigment(Palette.sheetDeep, Palette.edge, Palette.sheet), textColor = Palette.rubric,
        )
    } else {
        Text("This forgets every sign, word and amulet. It cannot be undone.", style = Type.body)
        PaintButton("Yes, forget everything", onReset, Modifier.fillMaxWidth(), pigment = Palette.unit(UnitColor.CARNELIAN))
        PaintButton("Keep my progress", { confirming = false }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun TestingPage(settings: Settings, reports: ReportOptions, tester: TesterOptions) {
    var confirmClear by remember { mutableStateOf(false) }
    Toggle(
        "Tester mode", "Tools for testing the course: open locked lessons, an answer button, and the reports kept on this phone",
        settings.tester, tester.onTester,
    )
    if (!settings.tester) return
    Toggle("Open any lesson", "Start lessons that are still locked", settings.openAnyLesson, tester.onOpenAnyLesson)
    Toggle(
        "Answer button", "A “solve” chip that answers for you, to reach a later page quickly",
        settings.answerButton, tester.onAnswerButton,
    )
    tester.onPreviewReveal?.let { PaintButton("Preview the section reveal", it, Modifier.fillMaxWidth()) }
    val saved = "${reports.saved} ${if (reports.saved == 1) "report" else "reports"} on this phone"
    Text(if (reports.saved > 0 && reports.unsent == 0) "$saved · all sent" else saved, style = Type.heading)
    Text("Kept in ${reports.path}", style = Type.small)
    if (reports.saved > 0) {
        PaintButton("Share reports", reports.onShare, Modifier.fillMaxWidth())
        if (!confirmClear) {
            PaintButton(
                "Clear reports", { confirmClear = true }, Modifier.fillMaxWidth(),
                pigment = Palette.Pigment(Palette.sheetDeep, Palette.edge, Palette.sheet), textColor = Palette.rubric,
            )
        } else {
            Text(
                "Delete the ${reports.saved} reports on this phone? Ones already sent stay on the server.",
                style = Type.body,
            )
            PaintButton(
                "Yes, delete them", { reports.onClear(); confirmClear = false }, Modifier.fillMaxWidth(),
                pigment = Palette.unit(UnitColor.CARNELIAN),
            )
            PaintButton("Keep them", { confirmClear = false }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun AboutPage(about: AboutOptions) {
    // Each time the page opens without an answer, ask again.
    LaunchedEffect(Unit) {
        if (about.latest is LatestRelease.NotChecked || about.latest is LatestRelease.Unreachable) about.onCheck()
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        // pr-ꜥnḫ, "House of Life": the house and the ankh.
        Glyphs("O1-S34", quadrat = 44.dp, description = "pr-ꜥnḫ")
        Column(Modifier.padding(start = 14.dp)) {
            Text("Per Ankh", style = Type.display)
            Text("Version ${about.build}", style = Type.small)
        }
    }
    NewestRelease(about)

    Rubric("About the voice")
    Text(
        "Nobody knows exactly how Middle Egyptian sounded — the script wrote no vowels — so the app speaks " +
            "the Egyptological convention: an e between consonants (nfr is “nefer”), and the consonants " +
            "Egyptian really had. ḥ is a breathy h from deep in the throat, ꜥ (ʿayin) a throaty catch, ḫ " +
            "the kh of Scottish “loch”, q a k made far back: ptḥ is “peteḥ”, not “Ptah”.\n\n" +
            "The voice is Chaouki, a native Arabic narrator from the ElevenLabs library, speaking with the " +
            "Eleven v4 model. Each word is handed to him written in Arabic letters, because Arabic still has " +
            "letters for exactly these sounds. Short words that came out wrong on their own were said inside " +
            "a little Arabic frame — “qul: …”, “say: …” — and cut out of it. Every take was transcribed by " +
            "speech recognition, and the one in which the hard consonants were heard was kept.",
        style = Type.small,
    )
    Rubric("Credits")
    Text(
        "Hieroglyphs: Noto Sans Egyptian Hieroglyphs. Text: Noto Serif. Both © Google, under the SIL Open " +
            "Font License 1.1. Sign codes follow Gardiner’s sign list; transliteration follows Allen’s " +
            "Middle Egyptian. Royal names and several spellings were checked against Wikipedia’s hieroglyph " +
            "markup; the offering formula and the literary openings are simplified for learners.\n\n" +
            "Word frequencies were counted in the Thesaurus Linguae Aegyptiae (Berlin-Brandenburg Academy " +
            "of Sciences and Saxon Academy of Sciences, Leipzig), Earlier Egyptian corpus v18: its 3,631 " +
            "Middle Egyptian sentences. The counts are shared under CC BY-SA 4.0.",
        style = Type.small,
    )

    Rubric("Privacy")
    Text(
        "Per Ankh works offline and collects nothing about you; your progress stays on this phone. " +
            "If you report a problem with the flag on a page, the report goes to the people who make " +
            "Per Ankh over an encrypted connection, with what was on screen. No name, account or " +
            "device ID is sent. To see whether a newer version is out, this page asks GitHub, or our " +
            "own server if GitHub cannot be reached; the question carries only the app’s version.",
        style = Type.small,
    )
    Link("Read the privacy policy") { about.onOpen(about.privacyUrl) }

    Rubric("Open source")
    Text(
        "Per Ankh is free and open source under the MIT licence, published by LOVI Tech. Its code, " +
            "course and every release are on GitHub.",
        style = Type.small,
    )
    Link("Per Ankh on GitHub") { about.onOpen(about.projectUrl) }
}

/** Whether this is the newest release, and the way to it. */
@Composable
private fun NewestRelease(about: AboutOptions) {
    when (val latest = about.latest) {
        LatestRelease.NotChecked, LatestRelease.Checking -> Text("Looking for a newer version…", style = Type.small)
        LatestRelease.Unreachable -> {
            Text("Could not check for a newer version just now.", style = Type.body)
            Link("Open the newest release") { about.onOpen(about.latestUrl) }
            Link("Try again", about.onCheck)
        }
        is LatestRelease.Found -> {
            val release = latest.release
            val order = compareVersions(release.version, about.version)
            when {
                order > 0 -> {
                    Text("Version ${release.version} is out", style = Type.heading)
                    Text(
                        "On its page, tap the .apk file to download it, then open it to install over this " +
                            "version. Your progress stays.",
                        style = Type.small,
                    )
                    PaintButton("Get version ${release.version}", { about.onOpen(release.url) }, Modifier.fillMaxWidth())
                }
                order == 0 -> {
                    Text("This is the newest version.", style = Type.body)
                    Link("What’s new in ${release.version}") { about.onOpen(release.url) }
                }
                else -> {
                    Text("This build is newer than the latest release, ${release.version}.", style = Type.body)
                    Link("See version ${release.version}") { about.onOpen(release.url) }
                }
            }
        }
    }
}

/** Text that opens something, in lapis like a link. */
@Composable
private fun Link(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = Type.body.copy(color = Palette.lapisDeep),
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp),
    )
}

@Composable
private fun Toggle(title: String, detail: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = Type.heading)
            Text(detail, style = Type.small)
        }
        Switch(
            checked = on,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Palette.sheetLight,
                checkedTrackColor = Palette.lapis,
                uncheckedThumbColor = Palette.inkFaint,
                uncheckedTrackColor = Palette.sheetDeep,
            ),
        )
    }
}
