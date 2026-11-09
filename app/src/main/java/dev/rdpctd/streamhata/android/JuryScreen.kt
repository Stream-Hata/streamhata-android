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
import dev.rdpctd.streamhata.core.JurySummary
import dev.rdpctd.streamhata.core.ui.StreamHataBorder
import dev.rdpctd.streamhata.core.ui.StreamHataMuted
import dev.rdpctd.streamhata.core.ui.StreamHataPink
import dev.rdpctd.streamhata.core.ui.StreamHataSurface

@Composable
fun JuryScreen(api: ApiClient, onBack: () -> Unit) {
    var jury by remember { mutableStateOf<JurySummary?>(null) }
    var text by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(true) }
    var duration by remember { mutableStateOf("90") }
    var confidence by remember { mutableStateOf("0.7") }

    fun load() = api.jury { jury = it }
    LaunchedEffect(Unit) {
        load()
        api.jurySettings { settings ->
            if (settings != null) {
                enabled = settings.enabled
                duration = settings.durationSeconds.toString()
                confidence = settings.minConfidence.toString()
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Чат-жюри", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Назад") }
        }
        jury?.let { current ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                border = BorderStroke(1.dp, StreamHataBorder)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(current.text.ifBlank { "Нет активного голосования" }, style = MaterialTheme.typography.titleMedium)
                    Text("За: ${current.yea} · Против: ${current.nei}", color = StreamHataMuted)
                    if (current.verdict.isNotBlank()) Text("Вердикт: ${current.verdict}", color = StreamHataPink)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { api.finishJury { ok -> status = if (ok) "Голосование завершено" else "Ошибка"; load() } }) {
                    Text("Завершить")
                }
                TextButton(onClick = { load() }) { Text("Обновить") }
            }
        } ?: Text("Активного голосования нет", color = StreamHataMuted)

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Текст для тестового жюри") },
            minLines = 2
        )
        Button(
            enabled = text.isNotBlank(),
            onClick = {
                api.startJury(text.trim()) { ok ->
                    status = if (ok) "Жюри запущено" else "Ошибка"
                    if (ok) { text = ""; load() }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Запустить тестовое жюри") }
        Text("Настройки", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Включено")
            Switch(checked = enabled, onCheckedChange = { enabled = it })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it.filter(Char::isDigit).take(5) },
                modifier = Modifier.weight(1f),
                label = { Text("Длительность, сек") }
            )
            OutlinedTextField(
                value = confidence,
                onValueChange = { confidence = it.filter { char -> char.isDigit() || char == '.' }.take(5) },
                modifier = Modifier.weight(1f),
                label = { Text("Мин. уверенность") }
            )
        }
        Button(
            onClick = {
                api.saveJurySettings(
                    enabled = enabled,
                    durationSeconds = duration.toIntOrNull() ?: 90,
                    minConfidence = confidence.toDoubleOrNull() ?: 0.7
                ) { ok -> status = if (ok) "Настройки сохранены" else "Ошибка" }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Сохранить настройки") }
        if (status.isNotBlank()) Text(status, color = StreamHataPink)
    }
}
