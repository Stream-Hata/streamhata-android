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
import dev.rdpctd.streamhata.core.ObsStatusSummary
import dev.rdpctd.streamhata.core.ui.StreamHataBorder
import dev.rdpctd.streamhata.core.ui.StreamHataMuted
import dev.rdpctd.streamhata.core.ui.StreamHataPink
import dev.rdpctd.streamhata.core.ui.StreamHataSurface

@Composable
fun ObsScreen(api: ApiClient, onBack: () -> Unit) {
    var status by remember { mutableStateOf<ObsStatusSummary?>(null) }
    var message by remember { mutableStateOf("") }
    fun load() = api.obs { status = it }
    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("OBS", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Назад") }
        }
        status?.let { obs ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                border = BorderStroke(1.dp, StreamHataBorder)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (obs.connected) "Подключено" else "Не подключено", color = StreamHataPink)
                    Text("Сцена: ${obs.currentScene.ifBlank { "—" }}", color = StreamHataMuted)
                    Text("Стрим: ${if (obs.streaming) "идёт" else "нет"} · Запись: ${if (obs.recording) "идёт" else "нет"}", color = StreamHataMuted)
                    if (obs.error.isNotBlank()) Text(obs.error, color = StreamHataPink, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (obs.scenes.isNotEmpty()) {
                Text("Сцены", style = MaterialTheme.typography.titleMedium)
                obs.scenes.forEach { scene ->
                    Button(
                        onClick = {
                            api.setObsScene(scene) { ok ->
                                message = if (ok) "Сцена переключена" else "Ошибка"
                                load()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(scene) }
                }
            }
        } ?: Text("Нет данных OBS", color = StreamHataMuted)
        if (message.isNotBlank()) Text(message, color = StreamHataPink)
    }
}
