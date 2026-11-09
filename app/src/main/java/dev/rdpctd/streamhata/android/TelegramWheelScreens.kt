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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import dev.rdpctd.streamhata.core.TelegramCommandSummary
import dev.rdpctd.streamhata.core.WheelSummary
import dev.rdpctd.streamhata.core.ui.StreamHataBorder
import dev.rdpctd.streamhata.core.ui.StreamHataMuted
import dev.rdpctd.streamhata.core.ui.StreamHataPink
import dev.rdpctd.streamhata.core.ui.StreamHataSurface

@Composable
fun TelegramScreen(api: ApiClient, onBack: () -> Unit) {
    var commands by remember { mutableStateOf<List<TelegramCommandSummary>>(emptyList()) }
    LaunchedEffect(Unit) { api.telegramCommands { commands = it } }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Telegram", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Назад") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(commands) { command ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                    border = BorderStroke(1.dp, StreamHataBorder)
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(command.command, style = MaterialTheme.typography.titleMedium)
                        Text(command.response, color = StreamHataMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun WheelScreen(api: ApiClient, onBack: () -> Unit) {
    var wheel by remember { mutableStateOf<WheelSummary?>(null) }
    var status by remember { mutableStateOf("") }
    fun load() = api.wheel { wheel = it }
    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Колесо", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Назад") }
        }
        wheel?.let {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                border = BorderStroke(1.dp, StreamHataBorder)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(it.title, style = MaterialTheme.typography.titleMedium)
                    Text("Состояние: ${it.state} · Банк: ${it.pot} · Участников: ${it.players}", color = StreamHataMuted)
                }
            }
            Button(
                onClick = {
                    api.spinWheel { ok ->
                        status = if (ok) "Крутим…" else "Ошибка"
                        load()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Крутить") }
        } ?: Text("Раунд не запущен", color = StreamHataMuted)
        if (status.isNotBlank()) Text(status, color = StreamHataPink)
    }
}
