package com.krbavcic.pentalingo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.krbavcic.pentalingo.core.ui.PaintButton
import com.krbavcic.pentalingo.core.ui.Palette
import com.krbavcic.pentalingo.core.ui.Rubric
import com.krbavcic.pentalingo.core.ui.Type
import com.krbavcic.pentalingo.data.Settings
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

@Composable
fun SettingsSheet(
    settings: Settings,
    onSounds: (Boolean) -> Unit,
    onAutoplay: (Boolean) -> Unit,
    onReset: () -> Unit,
    onPrivacy: () -> Unit,
    reports: ReportOptions,
    tester: TesterOptions,
) {
    var confirming by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 660.dp)
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(Palette.sheetLight)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Settings", style = Type.display)
        Toggle("Sound effects", "Harp plucks for right and wrong answers", settings.sounds, onSounds)
        Toggle("Speak automatically", "Play each word’s pronunciation as it appears", settings.autoplay, onAutoplay)

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
                "device ID is sent.",
            style = Type.small,
        )
        Text(
            "Read the privacy policy",
            style = Type.small.copy(color = Palette.lapisDeep),
            modifier = Modifier.clickable(role = Role.Button, onClick = onPrivacy),
        )
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

        Rubric("Testing")
        Toggle(
            "Tester mode", "Tools for testing the course: open locked lessons, an answer button, and the reports kept on this phone",
            settings.tester, tester.onTester,
        )
        if (settings.tester) {
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
