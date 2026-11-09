package dev.rdpctd.streamhata.tuieditor

import android.text.Html
import android.widget.TextView
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class TuiEditorController(initial: String = "") {
    var value by mutableStateOf(TextFieldValue(initial))
        internal set

    val text: String
        get() = value.text

    fun setText(text: String) {
        value = TextFieldValue(text)
    }

    fun insert(text: String) {
        val start = minOf(value.selection.start, value.selection.end)
        val end = maxOf(value.selection.start, value.selection.end)
        val next = value.text.replaceRange(start, end, text)
        val cursor = start + text.length
        value = TextFieldValue(next, TextRange(cursor, cursor))
    }

    fun wrap(before: String, after: String = "") {
        val start = minOf(value.selection.start, value.selection.end)
        val end = maxOf(value.selection.start, value.selection.end)
        val selected = value.text.substring(start, end)
        val next = value.text.replaceRange(start, end, before + selected + after)
        val cursor = start + before.length + selected.length
        value = TextFieldValue(next, TextRange(cursor, cursor))
    }

    fun insertImage(url: String) {
        insert("<img src=\"$url\">")
    }
}

data class EditorTool(
    val id: String,
    val label: String,
    val action: (TuiEditorController) -> Unit
)

interface TuiEditorPlugin {
    fun tools(controller: TuiEditorController): List<EditorTool>
}

private fun defaultTools(controller: TuiEditorController): List<EditorTool> = listOf(
    EditorTool("bold", "Ж") { it.wrap("<b>", "</b>") },
    EditorTool("italic", "К") { it.wrap("<i>", "</i>") },
    EditorTool("underline", "Ч") { it.wrap("<u>", "</u>") },
    EditorTool("strike", "З") { it.wrap("<s>", "</s>") },
    EditorTool("code", "</>") { it.wrap("<code>", "</code>") },
    EditorTool("quote", "❝") { it.wrap("<blockquote>", "</blockquote>") },
    EditorTool("link", "🔗") { it.wrap("<a href=\"\">", "</a>") },
    EditorTool("clear", "×") { it.setText("") }
)

@Composable
fun rememberTuiEditorController(initial: String = ""): TuiEditorController =
    remember(initial) { TuiEditorController(initial) }

/**
 * Native Android port of the Taiga TuiEditor contract used by StreamHata.
 * Plugins are compile-time Kotlin objects; runtime DEX loading is intentionally unsupported.
 */
@Composable
fun TuiEditor(
    controller: TuiEditorController,
    modifier: Modifier = Modifier,
    placeholder: String = "Текст…",
    plugins: List<TuiEditorPlugin> = emptyList(),
    extraTools: List<EditorTool> = emptyList(),
    minLines: Int = 4,
    maxLines: Int = 10
) {
    val pluginTools = plugins.flatMap { it.tools(controller) }
    val tools = defaultTools(controller) + pluginTools + extraTools

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            tools.forEach { tool ->
                TextButton(onClick = { tool.action(controller) }) {
                    Text(tool.label)
                }
            }
        }
        OutlinedTextField(
            value = controller.value,
            onValueChange = { controller.value = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            minLines = minLines,
            maxLines = maxLines,
            shape = RoundedCornerShape(12.dp)
        )
        val preview = Html.fromHtml(controller.text, Html.FROM_HTML_MODE_LEGACY)
        if (preview.isNotBlank()) {
            Text("Предпросмотр", style = MaterialTheme.typography.labelSmall)
            AndroidView(
                factory = { context ->
                    TextView(context).apply {
                        textSize = 15f
                        setPadding(0, 8, 0, 0)
                    }
                },
                update = { it.text = Html.fromHtml(controller.text, Html.FROM_HTML_MODE_LEGACY) },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }
    }
}
