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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import dev.rdpctd.streamhata.core.StreamEvent
import dev.rdpctd.streamhata.core.ui.StreamHataBorder
import dev.rdpctd.streamhata.core.ui.StreamHataMuted
import dev.rdpctd.streamhata.core.ui.StreamHataPink
import dev.rdpctd.streamhata.core.ui.StreamHataSurface
import dev.rdpctd.streamhata.core.ui.StreamHataText

@Composable
fun EventsScreen(api: ApiClient, onBack: () -> Unit) {
    var events by remember { mutableStateOf<List<StreamEvent>>(emptyList()) }
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("stream") }
    var status by remember { mutableStateOf("") }

    fun load() {
        api.events { events = it }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("События", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Назад") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Название события") }
            )
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    api.createEvent(title.trim(), type, null) { ok ->
                        status = if (ok) "Событие создано" else "Ошибка"
                        if (ok) { title = ""; load() }
                    }
                }
            ) { Text("+") }
        }
        if (status.isNotBlank()) Text(status, color = StreamHataPink)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(events) { event ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                    border = BorderStroke(1.dp, StreamHataBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(event.title, style = MaterialTheme.typography.titleMedium)
                        Text("${event.type} · ${event.status}", color = StreamHataMuted, style = MaterialTheme.typography.bodySmall)
                        event.startsAt.takeIf { it.isNotBlank() }?.let { Text(it.take(16).replace('T', ' '), color = StreamHataMuted) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                api.updateEventStatus(event.id, if (event.status == "planned") "done" else "planned") { ok ->
                                    if (ok) load()
                                }
                            }) { Text(if (event.status == "planned") "Завершить" else "Вернуть") }
                            TextButton(onClick = {
                                api.deleteEvent(event.id) { ok -> if (ok) events = events.filter { it.id != event.id } }
                            }) { Text("Удалить") }
                        }
                    }
                }
            }
        }
    }
}
