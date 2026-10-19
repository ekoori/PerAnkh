package com.krbavcic.pentalingo

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import com.krbavcic.pentalingo.core.ui.PaintButton
import com.krbavcic.pentalingo.core.ui.Palette
import com.krbavcic.pentalingo.core.ui.Rubric
import com.krbavcic.pentalingo.core.ui.Type
import com.krbavcic.pentalingo.data.Settings
import com.krbavcic.pentalingo.model.UnitColor

@Composable
fun SettingsSheet(
    settings: Settings,
    onSounds: (Boolean) -> Unit,
    onAutoplay: (Boolean) -> Unit,
    onReset: () -> Unit,
) {
    var confirming by remember { mutableStateOf(false) }
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
            "Pronunciations were recorded with ElevenLabs’ Eleven v4 model. Nobody knows how Middle Egyptian " +
                "sounded — the script wrote no vowels — so the voice reads the conventional Egyptological " +
                "pronunciation, with an e between consonants: nfr is “nefer”. Each clip was checked by speech " +
                "recognition against the spelling it was meant to say.",
            style = Type.small,
        )
        Rubric("Credits")
        Text(
            "Hieroglyphs: Noto Sans Egyptian Hieroglyphs. Text: Noto Serif. Both © Google, under the SIL Open " +
                "Font License 1.1. Sign codes follow Gardiner’s sign list; transliteration follows Allen’s " +
                "Middle Egyptian. Royal names and several spellings were checked against Wikipedia’s hieroglyph " +
                "markup; the offering formula and the literary openings are simplified for learners.",
            style = Type.small,
        )

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
