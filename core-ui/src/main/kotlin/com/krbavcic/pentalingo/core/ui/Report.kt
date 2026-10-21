package com.krbavcic.pentalingo.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.krbavcic.pentalingo.model.ConceptItem
import com.krbavcic.pentalingo.model.Course
import com.krbavcic.pentalingo.model.PhraseItem
import com.krbavcic.pentalingo.model.SignItem
import com.krbavcic.pentalingo.model.SignRole
import com.krbavcic.pentalingo.model.WordItem
import com.krbavcic.pentalingo.script.Pronounce

/** Sends a report about what is on screen. The app provides one; without it, screens show no flag. */
fun interface Reporter {
    fun report(category: String, note: String, context: Map<String, String>)
}

val LocalReporter = staticCompositionLocalOf<Reporter?> { null }

/** What a report can be about, in the words a tester would use. */
val REPORT_CATEGORIES = listOf(
    "Pronunciation",
    "Text or translation",
    "Hieroglyphs or spelling",
    "Answer checking",
    "Lesson flow",
    "Other",
)

/**
 * A small flag that opens a report about what is on screen.
 *
 * [context] is read when the flag is tapped, so the report holds the screen as the learner saw it.
 * Sending it is the [Reporter]'s job, which also says how it went. Draws nothing without one.
 */
@Composable
fun ReportFlag(context: () -> Map<String, String>, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val reporter = LocalReporter.current ?: return
    var open by remember { mutableStateOf<Map<String, String>?>(null) }
    RoundIconButton(
        IconKind.FLAG, onClick = { open = context() }, modifier = modifier, size = size,
        fill = Palette.sheetDeep, tint = Palette.inkSoft, description = "Report a problem",
    )
    val snapshot = open ?: return
    ReportDialog(
        context = snapshot,
        onSave = { category, note ->
            reporter.report(category, note, snapshot)
            open = null
        },
        onDismiss = { open = null },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportDialog(
    context: Map<String, String>,
    onSave: (category: String, note: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var category by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var showContext by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 18.dp)
                .imePadding()
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Palette.sheetLight)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Report a problem", style = Type.title)
            Text(summary(context), style = Type.small)
            Text(
                "It goes to the people who make Per Ankh, with what is on this screen. No name, account " +
                    "or device ID is sent.",
                style = Type.small,
                color = Palette.inkSoft,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (c in REPORT_CATEGORIES) {
                    val on = c == category
                    Text(
                        c,
                        style = Type.small,
                        color = if (on) Palette.sheetLight else Palette.ink,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (on) Palette.lapis else Palette.sheetDeep)
                            .border(1.dp, if (on) Palette.lapis else Palette.edge, RoundedCornerShape(12.dp))
                            .clickable(role = Role.RadioButton) { category = c }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What is wrong, and what did you expect?", style = Type.small) },
                textStyle = Type.body,
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Palette.lapis,
                    unfocusedBorderColor = Palette.edge,
                    cursorColor = Palette.lapis,
                    focusedContainerColor = Palette.sheet,
                    unfocusedContainerColor = Palette.sheet,
                ),
            )
            Text(
                if (showContext) "Hide what is attached" else "Show what is attached (${context.size} fields)",
                style = Type.small,
                color = Palette.lapisDeep,
                modifier = Modifier.clickable(role = Role.Button) { showContext = !showContext },
            )
            if (showContext) {
                Text(
                    context.entries.joinToString("\n") { (k, v) -> "$k: $v" },
                    style = Type.small,
                    color = Palette.inkSoft,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PaintButton(
                    "Cancel", onDismiss, Modifier.weight(1f),
                    pigment = Palette.Pigment(Palette.sheetDeep, Palette.edge, Palette.sheet), textColor = Palette.inkSoft,
                )
                PaintButton(
                    "Send", { onSave(category ?: "Other", note.trim()) }, Modifier.weight(1f),
                    enabled = category != null || note.isNotBlank(),
                )
            }
        }
    }
}

/** One line saying what the report will be about. */
private fun summary(context: Map<String, String>): String = listOfNotNull(
    context["lesson"] ?: context["screen"],
    context["exercise"],
    context["item"]?.let { id -> listOfNotNull(id, context["item.translit"], context["item.gloss"]).joinToString(" · ") },
).joinToString("  —  ")

/**
 * An item flattened to strings for a report: its id and data, how the app says it, and the clip
 * that plays for it. Keys start with [prefix], so a report can describe several items at once.
 */
fun describeItem(course: Course, id: String?, voice: Voice?, prefix: String = "item"): Map<String, String> {
    if (id == null) return emptyMap()
    val item = course[id] ?: return mapOf(prefix to "$id (not in the course)")
    val out = linkedMapOf(prefix to id)
    fun put(key: String, value: String?) {
        if (!value.isNullOrBlank()) out["$prefix.$key"] = value
    }
    fun said(translit: String) {
        put("said", Pronounce.latin(translit))
        put("ipa", Pronounce.ipa(translit))
        put("tts", Pronounce.arabic(translit))
    }
    when (item) {
        is SignItem -> {
            put("code", item.code)
            put("name", item.name)
            put("role", item.role.name)
            put("sounds", item.sounds.joinToString(", "))
            put("meaning", item.meaning)
            put("reading", item.reading)
            put("hint", item.hint)
            put("note", item.note)
            put("lookalikes", item.lookalikes.joinToString(", "))
            item.spoken?.let { spoken ->
                if (item.role == SignRole.UNILITERAL && item.reading == null) {
                    put("said", Pronounce.letterLatin(spoken))
                    put("ipa", Pronounce.letterIpa(spoken))
                } else {
                    said(spoken)
                }
            }
        }
        is WordItem -> {
            put("translit", item.translit)
            put("mdc", item.mdc)
            put("gloss", item.gloss)
            put("pos", item.pos.name)
            put("accepts", item.accepts.joinToString(" | "))
            put("determinative", item.determinative)
            put("variants", item.variants.joinToString(" | "))
            put("note", item.note)
            said(item.translit)
        }
        is PhraseItem -> {
            put("translit", item.translit)
            put("mdc", item.tokens.joinToString(" ") { it.mdc })
            put("gloss", item.translation)
            put("literal", item.literal)
            put("tokens", item.tokens.joinToString(" | ") { "${it.translit} = ${it.gloss}" })
            put("tiles", item.tiles.joinToString(" | "))
            put("note", item.note)
            said(item.translit)
        }
        is ConceptItem -> {
            put("title", item.title)
            put("question", item.question)
            put("answer", item.answer)
            put("mdc", item.mdc)
        }
    }
    if (item !is ConceptItem) voice?.let { put("clip", it.fileFor(id) ?: "none") }
    return out
}
