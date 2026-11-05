package dev.rdpctd.streamhata.chat

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rdpctd.streamhata.core.ApiClient
import dev.rdpctd.streamhata.core.AuthManager
import dev.rdpctd.streamhata.core.ChatMessage
import dev.rdpctd.streamhata.core.TwitchProfile
import dev.rdpctd.streamhata.core.ui.StreamHataBorder
import dev.rdpctd.streamhata.core.ui.StreamHataCyan
import dev.rdpctd.streamhata.core.ui.StreamHataMuted
import dev.rdpctd.streamhata.core.ui.StreamHataPink
import dev.rdpctd.streamhata.core.ui.StreamHataSurface
import dev.rdpctd.streamhata.core.ui.StreamHataSurfaceHigh
import dev.rdpctd.streamhata.core.ui.StreamHataText
import dev.rdpctd.streamhata.core.ui.StreamHataTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var auth: AuthManager
    private lateinit var api: ApiClient
    private val loggedIn = mutableStateOf(false)
    private val profile = mutableStateOf<TwitchProfile?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = AuthManager(this, "streamhata-chat")
        api = ApiClient { auth.token() }
        loggedIn.value = auth.isLoggedIn()
        auth.handleIntent(intent.data) { loggedIn.value = it; if (it) load() }
        if (loggedIn.value) load()
        setContent {
            StreamHataTheme {
                ChatRoot(
                    loggedIn = loggedIn.value,
                    profile = profile.value,
                    api = api,
                    onLogin = { auth.openLogin() },
                    onLogout = { auth.logout(); loggedIn.value = false; profile.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        auth.handleIntent(intent.data) { loggedIn.value = it; if (it) load() }
    }

    private fun load() {
        api.profile { value ->
            runOnUiThread {
                profile.value = value
                if (value == null) loggedIn.value = false
            }
        }
    }
}

@Composable
private fun ChatRoot(
    loggedIn: Boolean,
    profile: TwitchProfile?,
    api: ApiClient,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var draft by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            api.chatRecent { list -> messages.clear(); messages.addAll(list) }
            delay(3000)
        }
    }

    CompositionLocalProvider(LocalContentColor provides StreamHataText) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F0817), Color(0xFF130B1D), Color(0xFF0F0817))))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(StreamHataSurface.copy(alpha = 0.94f)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(34.dp).background(Brush.linearGradient(listOf(StreamHataPink, StreamHataCyan)), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("S", color = Color(0xFF0F0817), fontWeight = FontWeight.Bold) }
            Column(modifier = Modifier.weight(1f)) {
                Text("StreamHata Chat", style = MaterialTheme.typography.titleMedium)
                Text(if (loggedIn) "@${profile?.login.orEmpty()}" else "android alpha", color = StreamHataMuted, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = if (loggedIn) onLogout else onLogin) { Text(if (loggedIn) "Выйти" else "Войти") }
        }
        Column(modifier = Modifier.fillMaxSize().padding(14.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!loggedIn) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                    border = BorderStroke(1.dp, StreamHataBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Вход через Twitch", style = MaterialTheme.typography.titleMedium)
                        Text("Откроется браузер, после логина сессия вернётся в приложение.", color = StreamHataMuted)
                    }
                }
            }
            status.takeIf { it.isNotBlank() }?.let { Text(it, color = StreamHataPink, style = MaterialTheme.typography.bodySmall) }
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(messages) { message ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StreamHataSurfaceHigh),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                message.author,
                                color = message.color?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() } ?: StreamHataCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(message.text, color = StreamHataText)
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Сообщение в Twitch-чат") },
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    modifier = Modifier.padding(start = 8.dp),
                    onClick = {
                        if (!loggedIn) {
                            status = "Сначала войдите"
                        } else if (draft.isNotBlank()) {
                            val text = draft.trim()
                            api.sendChat(text, "") { ok -> status = if (ok) "Отправлено" else "Ошибка отправки" }
                            draft = ""
                        }
                    }
                ) { Text("→") }
            }
        }
    }
    }
}
