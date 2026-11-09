package dev.rdpctd.streamhata.android

import android.text.Html
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.rdpctd.streamhata.core.ApiClient
import dev.rdpctd.streamhata.core.AuthConfig
import dev.rdpctd.streamhata.tuieditor.EditorTool
import dev.rdpctd.streamhata.tuieditor.TuiEditor
import dev.rdpctd.streamhata.tuieditor.TuiEditorPlugin
import dev.rdpctd.streamhata.tuieditor.rememberTuiEditorController
import dev.rdpctd.streamhata.core.GameDiscussionMessage
import dev.rdpctd.streamhata.core.GameSummary
import dev.rdpctd.streamhata.core.ui.StreamHataBorder
import dev.rdpctd.streamhata.core.ui.StreamHataLoading
import dev.rdpctd.streamhata.core.ui.StreamHataMuted
import dev.rdpctd.streamhata.core.ui.StreamHataPink
import dev.rdpctd.streamhata.core.ui.StreamHataSurface
import dev.rdpctd.streamhata.core.ui.StreamHataSurfaceHigh
import dev.rdpctd.streamhata.core.ui.StreamHataText
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@Composable
fun GamesScreen(api: ApiClient, onOpen: (GameSummary) -> Unit) {
    var games by remember { mutableStateOf<List<GameSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        api.games {
            games = it
            loading = false
            if (it.isEmpty()) error = "Игры не загрузились"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Игры", style = MaterialTheme.typography.headlineSmall)
        if (loading) {
            StreamHataLoading("Загружаем игры…")
        } else if (games.isEmpty()) {
            Text(error.ifBlank { "Игр пока нет" }, color = StreamHataMuted)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(games) { game ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpen(game) },
                        colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                        border = BorderStroke(1.dp, StreamHataBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            if (game.cover.isNotBlank()) {
                                AsyncImage(
                                    model = game.cover,
                                    contentDescription = game.title,
                                    modifier = Modifier.width(58.dp).height(78.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(game.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    listOfNotNull(game.year.takeIf { it > 0 }?.toString(), game.slug).joinToString(" · "),
                                    color = StreamHataMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text("›", color = StreamHataMuted, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameDiscussionScreen(
    api: ApiClient,
    game: GameSummary,
    onBack: () -> Unit
) {
    var messages by remember { mutableStateOf<List<GameDiscussionMessage>>(emptyList()) }
    val editor = rememberTuiEditorController()
    var replyingTo by remember { mutableStateOf<GameDiscussionMessage?>(null) }
    var status by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var uploading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun reload() {
        api.gameDiscussion(game.slug) { messages = it; loading = false }
    }



    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes == null) {
            status = "Не удалось прочитать изображение"
            return@rememberLauncherForActivityResult
        }
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val extension = when (mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        uploading = true
        status = ""
        api.uploadDiscussionImage("discussion.$extension", bytes, mime) { url ->
            uploading = false
            if (url == null) {
                status = "Не удалось загрузить изображение"
            } else {
                editor.insertImage(url)
            }
        }
    }

    LaunchedEffect(game.slug) { reload() }

    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp).imePadding().navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("← К играм") }
            Text("Обсуждение", style = MaterialTheme.typography.headlineSmall)
        }
        Text(game.title, color = StreamHataMuted, style = MaterialTheme.typography.bodySmall)
        if (loading) StreamHataLoading("Загружаем обсуждение…")

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(messages) { message ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(start = if (message.parentId != null) 14.dp else 0.dp),
                    colors = CardDefaults.cardColors(containerColor = StreamHataSurfaceHigh),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(message.displayName, color = StreamHataPink, style = MaterialTheme.typography.labelLarge)
                            Text(message.createdAt.take(16).replace('T', ' '), color = StreamHataMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        AndroidView(
                            factory = { viewContext ->
                                TextView(viewContext).apply {
                                    setTextColor(android.graphics.Color.rgb(245, 241, 250))
                                    textSize = 15f
                                    setLineSpacing(0f, 1.15f)
                                }
                            },
                            update = { view ->
                                view.text = Html.fromHtml(message.message, Html.FROM_HTML_MODE_LEGACY)
                            }
                        )
                        imageUrls(message.message).forEach { url ->
                            AsyncImage(
                                model = absoluteUrl(url),
                                contentDescription = "Изображение в обсуждении",
                                modifier = Modifier.fillMaxWidth().height(190.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                api.likeGameDiscussion(game.slug, message.id) { result ->
                                    if (result != null) {
                                        messages = messages.map {
                                            if (it.id == message.id) it.copy(liked = result.first, likes = result.second) else it
                                        }
                                    }
                                }
                            }) { Text("♥ ${message.likes}") }
                            TextButton(onClick = { replyingTo = message }) { Text("Ответить") }
                            TextButton(onClick = {
                                api.deleteGameDiscussion(game.slug, message.id) { ok ->
                                    if (ok) messages = messages.filter { it.id != message.id }
                                    else status = "Можно удалять только свои сообщения"
                                }
                            }) { Text("Удалить") }
                        }
                    }
                }
            }
        }

        replyingTo?.let { parent ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ответ на ${parent.displayName}", color = StreamHataMuted, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { replyingTo = null }) { Text("Отменить") }
            }
        }

        val imagePlugin = object : TuiEditorPlugin {
            override fun tools(controller: dev.rdpctd.streamhata.tuieditor.TuiEditorController) = listOf(
                EditorTool("image", if (uploading) "…" else "🖼") { imagePicker.launch("image/*") }
            )
        }
        TuiEditor(
            controller = editor,
            placeholder = "Написать в обсуждении…",
            plugins = listOf(imagePlugin),
            minLines = 2,
            maxLines = 6
        )
        Button(
            onClick = {
                val text = editor.text.trim()
                if (text.isEmpty()) return@Button
                api.postGameDiscussion(game.slug, text, replyingTo?.id) { item ->
                    if (item != null) {
                        messages = messages + item
                        editor.setText("")
                        replyingTo = null
                    } else {
                        status = "Войдите через Twitch, чтобы писать"
                    }
                }
            },
            enabled = editor.text.isNotBlank() && !uploading,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Отправить") }
        if (status.isNotBlank()) Text(status, color = StreamHataPink, style = MaterialTheme.typography.bodySmall)
    }
}

private fun imageUrls(value: String): List<String> =
    Regex("<img[^>]+src=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        .findAll(value)
        .map { it.groupValues[1] }
        .toList()

private fun absoluteUrl(value: String): String =
    if (value.startsWith("http://") || value.startsWith("https://")) value
    else AuthConfig.baseUrl + (if (value.startsWith("/")) value else "/$value")
