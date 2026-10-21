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

/** Tester mode's switches and the reports it has saved. */
class TesterOptions(
    val reports: Int,
    val unsent: Int,
    val sending: Boolean,
    val sendResult: String?,
    val path: String,
    val onSend: () -> Unit,
    val onTester: (Boolean) -> Unit,
    val onOpenAnyLesson: (Boolean) -> Unit,
    val onAnswerButton: (Boolean) -> Unit,
    val onShare: () -> Unit,
    val onClear: () -> Unit,
)

@Composable
fun SettingsSheet(
    settings: Settings,
    onSounds: (Boolean) -> Unit,
    onAutoplay: (Boolean) -> Unit,
    onReset: () -> Unit,
    onPrivacy: () -> Unit,
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
            "Per Ankh works offline and collects nothing about you. Your progress stays on this phone. " +
                "Tester reports leave it only when you send them.",
            style = Type.small,
        )
        Text(
            "Read the privacy policy",
            style = Type.small.copy(color = Palette.lapisDeep),
            modifier = Modifier.clickable(role = Role.Button, onClick = onPrivacy),
        )

        Rubric("Testing")
        Toggle(
            "Tester mode", "A flag on every lesson page saves a report with the exact data on screen",
            settings.tester, tester.onTester,
        )
        if (settings.tester) {
            Toggle("Open any lesson", "Start lessons that are still locked", settings.openAnyLesson, tester.onOpenAnyLesson)
            Toggle(
                "Answer button", "A “solve” chip that answers for you, to reach a later page quickly",
                settings.answerButton, tester.onAnswerButton,
            )
            val saved = "${tester.reports} ${if (tester.reports == 1) "report" else "reports"} saved"
            Text(
                when {
                    tester.unsent > 0 -> "$saved · ${tester.unsent} not sent yet"
                    tester.reports > 0 -> "$saved · all sent"
                    else -> saved
                },
                style = Type.heading,
            )
            if (tester.unsent > 0) {
                PaintButton(
                    if (tester.sending) "Sending…" else "Send ${tester.unsent} ${if (tester.unsent == 1) "report" else "reports"}",
                    tester.onSend, Modifier.fillMaxWidth(), enabled = !tester.sending,
                )
            }
            tester.sendResult?.let { Text(it, style = Type.body) }
            Text(
                "Sending uses an encrypted connection to the Per Ankh server. A report holds what was on " +
                    "screen and your note: no name, account or device ID. Kept on this phone in ${tester.path}",
                style = Type.small,
            )
            if (tester.reports > 0) {
                PaintButton("Share reports", tester.onShare, Modifier.fillMaxWidth())
                if (!confirmClear) {
                    PaintButton(
                        "Clear reports", { confirmClear = true }, Modifier.fillMaxWidth(),
                        pigment = Palette.Pigment(Palette.sheetDeep, Palette.edge, Palette.sheet), textColor = Palette.rubric,
                    )
                } else {
                    Text("Delete all ${tester.reports} reports? Share them first if you still need them.", style = Type.body)
                    PaintButton(
                        "Yes, delete them", { tester.onClear(); confirmClear = false }, Modifier.fillMaxWidth(),
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
