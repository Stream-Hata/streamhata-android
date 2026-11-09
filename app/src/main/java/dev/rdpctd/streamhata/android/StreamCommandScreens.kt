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
import androidx.compose.material3.OutlinedTextField
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
import dev.rdpctd.streamhata.core.CommandSummary
import dev.rdpctd.streamhata.core.StreamStatus
import dev.rdpctd.streamhata.core.ui.StreamHataBorder
import dev.rdpctd.streamhata.core.ui.StreamHataMuted
import dev.rdpctd.streamhata.core.ui.StreamHataPink
import dev.rdpctd.streamhata.core.ui.StreamHataSurface

@Composable
fun StreamScreen(api: ApiClient, onBack: () -> Unit) {
    var status by remember { mutableStateOf<StreamStatus?>(null) }
    var message by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { api.stream { status = it } }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Стрим", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Назад") }
        }
        status?.let { info ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                border = BorderStroke(1.dp, StreamHataBorder)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(info.title.ifBlank { "Без названия" }, style = MaterialTheme.typography.titleMedium)
                    Text(info.gameName.ifBlank { "Категория не указана" }, color = StreamHataMuted)
                    Text("Зрителей: ${info.viewers} · Shield: ${if (info.shieldMode) "вкл" else "выкл"}", color = StreamHataMuted)
                }
            }
            Button(
                onClick = {
                    api.setShield(!info.shieldMode) { ok -> if (ok) api.stream { status = it } }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (info.shieldMode) "Выключить Shield" else "Включить Shield") }
            Button(
                onClick = {
                    api.startCommercial(60) { ok -> message = if (ok) "Реклама запущена" else "Ошибка" }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Реклама 60 секунд") }
        } ?: Text("Нет данных о стриме", color = StreamHataMuted)
        if (message.isNotBlank()) Text(message, color = StreamHataPink)
    }
}

@Composable
fun CommandsScreen(api: ApiClient, onBack: () -> Unit) {
    var commands by remember { mutableStateOf<List<CommandSummary>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var cooldown by remember { mutableStateOf("10") }
    var status by remember { mutableStateOf("") }
    fun load() = api.commands { commands = it }
    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Команды чата", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Назад") }
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.filter { char -> char.isLetterOrDigit() || char == '_' }.take(32) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Новая команда: имя") }
        )
        OutlinedTextField(
            value = response,
            onValueChange = { response = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Ответ (можно {{ 1 + 2 }})") },
            minLines = 2
        )
        OutlinedTextField(
            value = cooldown,
            onValueChange = { cooldown = it.filter(Char::isDigit).take(4) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Кулдаун, сек") }
        )
        Button(
            enabled = name.isNotBlank() && response.isNotBlank(),
            onClick = {
                api.saveCommand(name.trim(), "say", response.trim(), cooldown.toIntOrNull() ?: 0) { ok ->
                    status = if (ok) "Команда создана" else "Ошибка"
                    if (ok) { name = ""; response = ""; load() }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Создать команду") }
        if (status.isNotBlank()) Text(status, color = StreamHataPink)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(commands) { command ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                    border = BorderStroke(1.dp, StreamHataBorder)
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("!${command.name}", style = MaterialTheme.typography.titleMedium)
                            Text(command.action, color = StreamHataMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(command.response.take(160), color = StreamHataMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                api.setCommandEnabled(command.id, !command.enabled) { ok -> if (ok) load() }
                            }) { Text(if (command.enabled) "Выключить" else "Включить") }
                            TextButton(onClick = {
                                api.deleteCommand(command.id) { ok -> if (ok) load() }
                            }) { Text("Удалить") }
                        }
                    }
                }
            }
        }
    }
}
