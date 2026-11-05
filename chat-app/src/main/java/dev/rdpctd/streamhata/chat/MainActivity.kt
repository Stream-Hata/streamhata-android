package dev.rdpctd.streamhata.chat

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.rdpctd.streamhata.core.ApiClient
import dev.rdpctd.streamhata.core.AuthManager
import dev.rdpctd.streamhata.core.ChatMessage
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var auth: AuthManager
    private lateinit var api: ApiClient
    private val loggedIn = mutableStateOf(false)
    private val profileName = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = AuthManager(this, "streamhata-chat")
        api = ApiClient { auth.token() }
        loggedIn.value = auth.isLoggedIn()
        auth.handleIntent(intent.data) { loggedIn.value = it; if (it) loadProfile() }
        if (loggedIn.value) loadProfile()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChatScreen(
                        loggedIn = loggedIn.value,
                        profileName = profileName.value,
                        onLogin = { auth.openLogin() },
                        onLogout = { auth.logout(); loggedIn.value = false; profileName.value = "" },
                        api = api,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        auth.handleIntent(intent.data) { loggedIn.value = it; if (it) loadProfile() }
    }

    private fun loadProfile() {
        api.profile { profile ->
            runOnUiThread {
                profileName.value = profile?.displayName ?: ""
                if (profile == null) loggedIn.value = false
            }
        }
    }
}

@Composable
private fun ChatScreen(
    loggedIn: Boolean,
    profileName: String,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    api: ApiClient,
    scheme: String = "streamhata",
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

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (loggedIn) "Вы вошли как ${profileName.ifBlank { "стример" }}" else "StreamHata Chat")
            Button(onClick = if (loggedIn) onLogout else onLogin) {
                Text(if (loggedIn) "Выйти" else "Войти")
            }
        }
        status.takeIf { it.isNotBlank() }?.let { Text(it) }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(messages) { message ->
                Text(
                    text = "${message.author}: ${message.text}",
                    color = message.color?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() } ?: Color.Unspecified
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Сообщение в Twitch-чат") }
            )
            Button(
                modifier = Modifier.padding(start = 8.dp),
                onClick = {
                    if (!loggedIn) {
                        status = "Сначала войдите"
                    } else if (draft.isNotBlank()) {
                        api.sendChat(draft.trim(), "") { ok -> status = if (ok) "Отправлено" else "Ошибка отправки" }
                        draft = ""
                    }
                }
            ) { Text("Отправить") }
        }
    }
}
