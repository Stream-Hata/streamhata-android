package dev.rdpctd.streamhata.android

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import dev.rdpctd.streamhata.core.PollSummary
import dev.rdpctd.streamhata.core.RouletteSummary
import dev.rdpctd.streamhata.core.ui.StreamHataBorder
import dev.rdpctd.streamhata.core.ui.StreamHataMuted
import dev.rdpctd.streamhata.core.ui.StreamHataPink
import dev.rdpctd.streamhata.core.ui.StreamHataSurface

@Composable
fun PollsScreen(api: ApiClient, onBack: () -> Unit) {
    var poll by remember { mutableStateOf<PollSummary?>(null) }
    var title by remember { mutableStateOf("") }
    var choices by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    fun load() = api.polls { poll = it }
    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Опросы", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Назад") }
        }
        poll?.let { current ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                border = BorderStroke(1.dp, StreamHataBorder)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(current.title, style = MaterialTheme.typography.titleMedium)
                    current.options.forEach { Text("• $it", color = StreamHataMuted) }
                }
            }
        } ?: Text("Активных опросов нет", color = StreamHataMuted)

        OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Вопрос") })
        OutlinedTextField(value = choices, onValueChange = { choices = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Варианты через |") })
        Button(
            enabled = title.isNotBlank() && choices.isNotBlank(),
            onClick = {
                api.createPoll(title.trim(), choices, 60) { ok ->
                    status = if (ok) "Опрос создан" else "Ошибка"
                    if (ok) { title = ""; choices = ""; load() }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Создать опрос") }
        if (status.isNotBlank()) Text(status, color = StreamHataPink)
    }
}

@Composable
fun RouletteScreen(api: ApiClient, onBack: () -> Unit) {
    var state by remember { mutableStateOf<RouletteSummary?>(null) }
    var status by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { api.roulette { state = it } }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Рулетка", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Назад") }
        }
        state?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                border = BorderStroke(1.dp, StreamHataBorder)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Осталось: ${it.remaining}", style = MaterialTheme.typography.titleMedium)
                    if (it.lastTitle.isNotBlank()) Text("Последняя: ${it.lastTitle} · ${it.lastPlatform}", color = StreamHataMuted)
                }
            }
        }
        Button(
            onClick = {
                api.spinRoulette { ok ->
                    status = if (ok) "Крутим…" else "Ошибка"
                    api.roulette { state = it }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Крутить") }
        if (status.isNotBlank()) Text(status, color = StreamHataPink)
    }
}
