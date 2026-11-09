package dev.rdpctd.streamhata.android

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.rdpctd.streamhata.core.ui.StreamHataMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JournalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("streamhata_journal", Context.MODE_PRIVATE) }
    val entries = remember { mutableStateListOf<String>().apply { addAll(prefs.getStringSet("entries", emptySet()).orEmpty().sorted()) } }
    var draft by remember { mutableStateOf("") }
    val formatter = remember { SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Журнал стрима", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Назад") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Заметка или маркер…") }
            )
            Button(
                enabled = draft.isNotBlank(),
                onClick = {
                    val entry = "${formatter.format(Date())} — ${draft.trim()}"
                    entries.add(entry)
                    prefs.edit().putStringSet("entries", entries.toSet()).apply()
                    draft = ""
                }
            ) { Text("+") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(entries) { entry -> Text(entry, color = StreamHataMuted) }
        }
        if (entries.isNotEmpty()) {
            TextButton(onClick = {
                entries.clear()
                prefs.edit().remove("entries").apply()
            }) { Text("Очистить журнал") }
        }
    }
}
