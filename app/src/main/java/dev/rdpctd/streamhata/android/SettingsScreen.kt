package dev.rdpctd.streamhata.android

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.rdpctd.streamhata.core.ui.StreamHataMuted

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("streamhata_settings", Context.MODE_PRIVATE) }
    var notifications by remember { mutableStateOf(prefs.getBoolean("notifications", true)) }
    var mentions by remember { mutableStateOf(prefs.getBoolean("mentions", true)) }
    var replies by remember { mutableStateOf(prefs.getBoolean("replies", true)) }
    var compact by remember { mutableStateOf(prefs.getBoolean("compact", false)) }

    fun save(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Настройки", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Назад") }
        }
        SettingRow("Уведомления", "Показывать уведомления приложения", notifications) {
            notifications = it
            save("notifications", it)
        }
        SettingRow("Упоминания", "Уведомлять об упоминаниях", mentions) {
            mentions = it
            save("mentions", it)
        }
        SettingRow("Ответы", "Уведомлять об ответах в обсуждениях", replies) {
            replies = it
            save("replies", it)
        }
        SettingRow("Компактный вид", "Меньше отступов в лентах", compact) {
            compact = it
            save("compact", it)
        }
        Text("Версия 0.7.3-alpha", color = StreamHataMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SettingRow(title: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = title },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(hint, color = StreamHataMuted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors()
        )
    }
}
