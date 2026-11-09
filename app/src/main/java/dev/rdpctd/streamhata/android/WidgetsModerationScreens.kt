package dev.rdpctd.streamhata.android

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.rdpctd.streamhata.core.ApiClient
import dev.rdpctd.streamhata.core.ModerationSummary
import dev.rdpctd.streamhata.core.WidgetState
import dev.rdpctd.streamhata.core.ui.StreamHataBorder
import dev.rdpctd.streamhata.core.ui.StreamHataMuted
import dev.rdpctd.streamhata.core.ui.StreamHataSurface

@Composable
fun WidgetsScreen(api: ApiClient, onBack: () -> Unit) {
    var widgets by remember { mutableStateOf<List<WidgetState>>(emptyList()) }
    fun load() = api.widgets { widgets = it }
    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Виджеты", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Назад") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(widgets) { widget ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                    border = BorderStroke(1.dp, StreamHataBorder)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(widget.kind, style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = widget.enabled,
                            onCheckedChange = { enabled ->
                                api.setWidget(widget.kind, enabled) { ok -> if (ok) load() }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModerationScreen(api: ApiClient, onBack: () -> Unit) {
    var state by remember { mutableStateOf<ModerationSummary?>(null) }
    LaunchedEffect(Unit) { api.moderation { state = it } }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Модерация", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Назад") }
        }
        state?.let { info ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                border = BorderStroke(1.dp, StreamHataBorder)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Режим: ${if (info.dryRun) "проверка" else "активен"}", style = MaterialTheme.typography.titleMedium)
                    Text("Запрещённых слов: ${info.bannedWords}", color = StreamHataMuted)
                    Text("Ссылки: ${if (info.blockLinks) "блокируются" else "разрешены"}", color = StreamHataMuted)
                    Text("CAPS: ${if (info.capsEnabled) "включён" else "выключен"} · событий: ${info.logSize}", color = StreamHataMuted)
                }
            }
        } ?: Text("Нет данных", color = StreamHataMuted)
    }
}
