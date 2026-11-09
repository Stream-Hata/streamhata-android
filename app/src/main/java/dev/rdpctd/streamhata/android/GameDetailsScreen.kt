package dev.rdpctd.streamhata.android

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.rdpctd.streamhata.core.ApiClient
import dev.rdpctd.streamhata.core.GameDetail
import dev.rdpctd.streamhata.core.GameSummary
import dev.rdpctd.streamhata.core.ui.StreamHataBorder
import dev.rdpctd.streamhata.core.ui.StreamHataCyan
import dev.rdpctd.streamhata.core.ui.StreamHataLoading
import dev.rdpctd.streamhata.core.ui.StreamHataMuted
import dev.rdpctd.streamhata.core.ui.StreamHataPink
import dev.rdpctd.streamhata.core.ui.StreamHataSurface
import dev.rdpctd.streamhata.core.ui.StreamHataSurfaceHigh
import dev.rdpctd.streamhata.core.ui.StreamHataText

@Composable
fun GameDetailsScreen(
    api: ApiClient,
    summary: GameSummary,
    onBack: () -> Unit,
    onDiscuss: () -> Unit
) {
    var game by remember { mutableStateOf<GameDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(summary.slug) {
        api.game(summary.slug) { value ->
            game = value
            loading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← К играм") }
            TextButton(onClick = onDiscuss) { Text("Обсуждение") }
        }

        if (loading) {
            StreamHataLoading("Загружаем игру…")
        } else if (game == null) {
            Text(
                "Не удалось загрузить игру",
                color = StreamHataMuted,
                modifier = Modifier.padding(14.dp)
            )
        } else {
            val item = game ?: return@Column
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                        border = BorderStroke(1.dp, StreamHataBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (item.cover.isNotBlank()) {
                                AsyncImage(
                                    model = item.cover,
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxWidth().height(220.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(item.titleRu.ifBlank { item.title }, style = MaterialTheme.typography.headlineSmall)
                                if (item.titleRu.isNotBlank() && item.title != item.titleRu) {
                                    Text(item.title, color = StreamHataMuted)
                                }
                                Text(
                                    listOfNotNull(
                                        item.year.takeIf { it > 0 }?.toString(),
                                        item.rating.takeIf { it > 0 }?.let { "★ %.1f".format(it) },
                                        item.platforms.joinToString(", ") { it.name }.ifBlank { null }
                                    ).joinToString(" · "),
                                    color = StreamHataCyan
                                )
                                if (item.genres.isNotEmpty()) {
                                    Text(item.genres.joinToString(" · "), color = StreamHataMuted)
                                }
                            }
                        }
                    }
                }

                item {
                    val description = item.descriptionRu.ifBlank { item.description }
                    if (description.isNotBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                            border = BorderStroke(1.dp, StreamHataBorder)
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Описание", style = MaterialTheme.typography.titleMedium)
                                Text(description, color = StreamHataText)
                            }
                        }
                    }
                }

                if (item.screenshots.isNotEmpty()) {
                    item {
                        Text("Скриншоты", style = MaterialTheme.typography.titleMedium)
                    }
                    items(item.screenshots.take(10)) { screenshot ->
                        AsyncImage(
                            model = screenshot,
                            contentDescription = "Скриншот",
                            modifier = Modifier.fillMaxWidth().height(190.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                if (item.downloads.isNotEmpty()) {
                    item {
                        Text("Где найти", style = MaterialTheme.typography.titleMedium)
                    }
                    items(item.downloads) { download ->
                        OutlinedButton(
                            onClick = { uriHandler.openUri(download.url) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(download.name)
                                if (download.note.isNotBlank()) {
                                    Text(download.note, color = StreamHataMuted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                if (item.website.isNotBlank()) {
                    item {
                        Button(
                            onClick = { uriHandler.openUri(item.website) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Сайт игры") }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StreamHataSurfaceHigh)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Обсуждение", style = MaterialTheme.typography.titleMedium)
                            Text("Читайте и оставляйте сообщения в обсуждении игры.", color = StreamHataMuted)
                            TextButton(onClick = onDiscuss) { Text("Открыть обсуждение") }
                        }
                    }
                }
            }
        }
    }
}
