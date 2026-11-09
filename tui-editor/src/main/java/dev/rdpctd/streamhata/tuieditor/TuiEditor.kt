package dev.rdpctd.streamhata.tuieditor

import android.text.Html
import android.view.KeyEvent
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class TuiEditorController(initial: String = "") {
    var value by mutableStateOf(TextFieldValue(initial))
        internal set

    val text: String
        get() = value.text

    val selection: TextRange
        get() = value.selection

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

    fun transformed(plugins: List<TuiEditorPlugin>): String =
        plugins.fold(text) { current, plugin -> plugin.transform(current) }
}

data class EditorShortcut(
    val keyCode: Int,
    val ctrl: Boolean = true,
    val shift: Boolean = false,
    val alt: Boolean = false
)

data class EditorTool(
    val id: String,
    val label: String,
    val action: (TuiEditorController) -> Unit,
    val section: String = "format",
    val priority: Int = 0,
    val shortcut: EditorShortcut? = null
)

fun tool(id: String, label: String, action: (TuiEditorController) -> Unit): EditorTool =
    EditorTool(id, label, action)

interface TuiEditorPlugin {
    val id: String
        get() = javaClass.name

    fun tools(controller: TuiEditorController): List<EditorTool> = emptyList()

    fun onAttach(controller: TuiEditorController) = Unit

    fun onDetach(controller: TuiEditorController) = Unit

    fun onTextChanged(current: String, previous: String) = Unit

    fun transform(text: String): String = text

    fun render(text: String): String = text
}

class TuiEditorPluginRegistry(initial: List<TuiEditorPlugin> = emptyList()) {
    private val entries = mutableStateListOf<TuiEditorPlugin>().apply { addAll(initial) }

    val plugins: List<TuiEditorPlugin>
        get() = entries.toList()

    fun register(plugin: TuiEditorPlugin) {
        if (entries.none { it.id == plugin.id }) {
            entries += plugin
        }
    }

    fun unregister(pluginId: String) {
        entries.removeAll { it.id == pluginId }
    }

    fun clear() {
        entries.clear()
    }
}

@Composable
fun rememberTuiEditorPluginRegistry(initial: List<TuiEditorPlugin> = emptyList()): TuiEditorPluginRegistry =
    remember { TuiEditorPluginRegistry(initial) }

private fun defaultTools(): List<EditorTool> = listOf(
    EditorTool("bold", "Ж", { it.wrap("<b>", "</b>") }, priority = 10, shortcut = EditorShortcut(KeyEvent.KEYCODE_B)),
    EditorTool("italic", "К", { it.wrap("<i>", "</i>") }, priority = 20, shortcut = EditorShortcut(KeyEvent.KEYCODE_I)),
    EditorTool("underline", "Ч", { it.wrap("<u>", "</u>") }, priority = 30),
    EditorTool("strike", "З", { it.wrap("<s>", "</s>") }, priority = 40),
    EditorTool("code", "</>", { it.wrap("<code>", "</code>") }, priority = 50),
    EditorTool("quote", "❝", { it.wrap("<blockquote>", "</blockquote>") }, priority = 60),
    EditorTool("link", "🔗", { it.wrap("<a href=\"\">", "</a>") }, priority = 70),
    EditorTool("clear", "×", { it.setText("") }, section = "danger", priority = 90)
)

@Composable
fun rememberTuiEditorController(initial: String = ""): TuiEditorController =
    remember(initial) { TuiEditorController(initial) }

/**
 * Native Android port of the Taiga TuiEditor contract used by StreamHata.
 * Extensions are compile-time Kotlin plugins; runtime DEX loading is intentionally unsupported.
 */
@Composable
fun TuiEditor(
    controller: TuiEditorController,
    modifier: Modifier = Modifier,
    placeholder: String = "Текст…",
    plugins: List<TuiEditorPlugin> = emptyList(),
    registry: TuiEditorPluginRegistry? = null,
    extraTools: List<EditorTool> = emptyList(),
    minLines: Int = 4,
    maxLines: Int = 10,
    showPreview: Boolean = true
) {
    val activePlugins = remember(plugins, registry) { plugins + (registry?.plugins ?: emptyList()) }
    val currentPlugins by rememberUpdatedState(activePlugins)
    val allTools = (activePlugins.flatMap { it.tools(controller) } + extraTools + defaultTools())
        .distinctBy { it.id }
        .sortedWith(compareBy<EditorTool> { it.section }.thenBy { it.priority })

    DisposableEffect(activePlugins.map { it.id }.joinToString("|")) {
        currentPlugins.forEach { it.onAttach(controller) }
        onDispose { currentPlugins.forEach { it.onDetach(controller) } }
    }

    var previousText by remember { mutableStateOf(controller.text) }
    LaunchedEffect(controller.text) {
        val current = controller.text
        val previous = previousText

        if (previous != current) {
            previousText = current
            currentPlugins.forEach { it.onTextChanged(current, previous) }
        }
    }

    val latestPlugins by rememberUpdatedState(activePlugins)
    val keyHandler: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = { event ->
        if (event.type != KeyEventType.KeyDown) {
            false
        } else {
            allTools.firstOrNull { tool ->
                val shortcut = tool.shortcut
                shortcut != null &&
                    event.key == androidx.compose.ui.input.key.Key(shortcut.keyCode) &&
                    event.isCtrlPressed == shortcut.ctrl &&
                    event.isShiftPressed == shortcut.shift &&
                    event.isAltPressed == shortcut.alt
            }?.let {
                it.action(controller)
                true
            } ?: false
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            allTools.forEach { tool ->
                TextButton(onClick = { tool.action(controller) }) {
                    Text(tool.label)
                }
            }
        }
        OutlinedTextField(
            value = controller.value,
            onValueChange = { controller.value = it },
            modifier = Modifier.fillMaxWidth().onPreviewKeyEvent(keyHandler),
            placeholder = { Text(placeholder) },
            minLines = minLines,
            maxLines = maxLines,
            shape = RoundedCornerShape(12.dp)
        )
        if (showPreview) {
            val rendered = latestPlugins.fold(controller.text) { current, plugin -> plugin.render(current) }
            val preview = Html.fromHtml(rendered, Html.FROM_HTML_MODE_LEGACY)

            if (preview.isNotBlank()) {
                Text("Предпросмотр", style = MaterialTheme.typography.labelSmall)
                AndroidView(
                    factory = { context ->
                        TextView(context).apply {
                            textSize = 15f
                            setPadding(0, 8, 0, 0)
                        }
                    },
                    update = { it.text = Html.fromHtml(rendered, Html.FROM_HTML_MODE_LEGACY) },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }
    }
}
