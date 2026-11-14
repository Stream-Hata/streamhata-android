package dev.rdpctd.streamhata.android

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rdpctd.streamhata.core.ApiClient
import dev.rdpctd.streamhata.core.SecurityIncidentSummary
import dev.rdpctd.streamhata.core.ui.StreamHataBorder
import dev.rdpctd.streamhata.core.ui.StreamHataMuted
import dev.rdpctd.streamhata.core.ui.StreamHataPink
import dev.rdpctd.streamhata.core.ui.StreamHataSurface
import kotlinx.coroutines.delay

private val SecurityDanger = Color(0xFFFF5C7A)
private val SecurityWarning = Color(0xFFFFB020)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(api: ApiClient, onBack: () -> Unit) {
    val handler = remember { Handler(Looper.getMainLooper()) }
    var incidents by remember { mutableStateOf<List<SecurityIncidentSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var restricted by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<SecurityIncidentSummary?>(null) }
    var pending by remember { mutableStateOf<Pair<SecurityIncidentSummary, String>?>(null) }
    var note by remember { mutableStateOf("") }

    fun load() {
        api.securityIncidents { items ->
            handler.post {
                loading = false
                if (items == null) {
                    restricted = true
                } else {
                    restricted = false
                    incidents = items
                }
            }
        }
    }

    fun action(incident: SecurityIncidentSummary, action: String) {
        if (action == "ack") {
            api.ackSecurityIncident(incident.id) { ok ->
                handler.post {
                    if (!ok) error = "Не удалось взять инцидент в работу"
                    load()
                }
            }
            return
        }
        pending = incident to action
    }

    LaunchedEffect(Unit) {
        load()
        while (true) {
            delay(20_000)
            load()
        }
    }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Безопасность", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Назад") }
        }
        TextButton(onClick = { load() }) { Text("Обновить") }

        if (error.isNotBlank()) Text(error, color = SecurityDanger)
        if (restricted) {
            Text("Раздел доступен только админам канала", color = StreamHataMuted)
            return@Column
        }
        if (loading) {
            CircularProgressIndicator(color = StreamHataPink)
            return@Column
        }
        if (incidents.isEmpty()) {
            Text("Инцидентов нет", color = StreamHataMuted)
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(incidents) { incident ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                    border = BorderStroke(1.dp, if (incident.severity == "critical") SecurityDanger else StreamHataBorder)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                verdictLabel(incident.verdict),
                                color = verdictColor(incident),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(statusLabel(incident.status), color = StreamHataMuted, fontSize = 12.sp)
                        }
                        Text("#${incident.id} · ${incident.fileName.ifBlank { incident.kind }}")
                        Text("От: ${incident.senderLogin.ifBlank { "—" }}", color = StreamHataMuted, fontSize = 12.sp)
                        if (incident.reason.isNotBlank()) Text(incident.reason, color = StreamHataMuted, fontSize = 12.sp)
                        TextButton(onClick = { selected = incident }) { Text("Посмотреть отчёт") }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (incident.status == "pending_review" || incident.status == "acknowledged") {
                                Button(onClick = { action(incident, "ack") }) { Text("В работу") }
                            }
                            if (!resolved(incident.status)) {
                                TextButton(onClick = { action(incident, "confirm_fraud") }) {
                                    Text("Мошенничество", color = SecurityDanger)
                                }
                                TextButton(onClick = { action(incident, "false_positive") }) {
                                    Text("Ложное")
                                }
                                TextButton(onClick = { action(incident, "close") }) {
                                    Text("Закрыть")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val report = selected
    if (report != null) {
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(verdictLabel(report.verdict), color = verdictColor(report), style = MaterialTheme.typography.headlineSmall)
                Text("#${report.id} · ${report.kind}")
                Text("Отправитель: ${report.senderLogin.ifBlank { "—" }}")
                Text("Файл: ${report.fileName.ifBlank { "—" }}")
                if (report.fileHash.isNotBlank()) {
                    Text(report.fileHash, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
                Text("Причина: ${report.reason.ifBlank { "—" }}")
                Text("Дедлайн: ${report.autoBanAt ?: report.reviewDeadline}")
                Text("Бан: ${if (report.banApplied) "применён" else "нет"}")
                if (report.report.isNotBlank()) {
                    Text("Отчёт проверки", style = MaterialTheme.typography.titleMedium)
                    Text(report.report, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                TextButton(onClick = { selected = null }) { Text("Закрыть") }
            }
        }
    }

    val pendingAction = pending
    if (pendingAction != null) {
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Решение по инциденту #${pendingAction.first.id}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(actionLabel(pendingAction.second))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Комментарий") },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val (incident, action) = pendingAction
                    api.resolveSecurityIncident(incident.id, action, note) { ok ->
                        handler.post {
                            pending = null
                            note = ""
                            if (!ok) error = "Не удалось сохранить решение"
                            load()
                        }
                    }
                }) { Text("Подтвердить") }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("Отмена") }
            }
        )
    }
}

private fun resolved(status: String): Boolean =
    status == "confirmed_fraud" || status == "false_positive" || status == "closed"

private fun verdictColor(incident: SecurityIncidentSummary): Color = when {
    incident.severity == "critical" -> SecurityDanger
    incident.severity == "warning" -> SecurityWarning
    else -> StreamHataMuted
}

private fun verdictLabel(verdict: String): String = when (verdict) {
    "infected" -> "Заражён"
    "malware" -> "Вредоносный файл"
    "tampered" -> "Изменён"
    "fake" -> "Подделка"
    "fraud" -> "Мошенничество"
    "dangerous_code" -> "Опасный код"
    "suspicious" -> "Подозрительно"
    "blocked" -> "Заблокирован"
    else -> verdict
}

private fun statusLabel(status: String): String = when (status) {
    "pending_review" -> "Ждёт проверки"
    "acknowledged" -> "В работе"
    "confirmed_fraud" -> "Мошенничество"
    "false_positive" -> "Ложное срабатывание"
    "auto_banned" -> "Автобан"
    "closed" -> "Закрыт"
    else -> status
}

private fun actionLabel(action: String): String = when (action) {
    "confirm_fraud" -> "Подтвердить попытку мошенничества и забанить аккаунт"
    "false_positive" -> "Отметить как ложное срабатывание"
    "close" -> "Закрыть инцидент"
    else -> action
}
