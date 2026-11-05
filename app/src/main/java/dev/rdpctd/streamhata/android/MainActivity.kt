package dev.rdpctd.streamhata.android

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.rdpctd.streamhata.core.ApiClient
import dev.rdpctd.streamhata.core.AuthManager
import dev.rdpctd.streamhata.core.AnalyticsSummary
import dev.rdpctd.streamhata.core.ChatMessage
import dev.rdpctd.streamhata.core.Contact
import dev.rdpctd.streamhata.core.MessagingCrypto
import dev.rdpctd.streamhata.core.MessagingIdentity
import dev.rdpctd.streamhata.core.MessagingIdentityStore
import dev.rdpctd.streamhata.core.RecoveryManager
import dev.rdpctd.streamhata.core.TwitchProfile
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var auth: AuthManager
    private lateinit var api: ApiClient
    private val loggedIn = mutableStateOf(false)
    private val profileName = mutableStateOf("")
    private val profile = mutableStateOf<TwitchProfile?>(null)
    private val screen = mutableStateOf("chat")
    private lateinit var identityStore: MessagingIdentityStore
    private lateinit var recovery: RecoveryManager
    private val identity = mutableStateOf<MessagingIdentity?>(null)
    private val selfLogin = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = AuthManager(this, "streamhata")
        api = ApiClient { auth.token() }
        identityStore = MessagingIdentityStore(this)
        recovery = RecoveryManager(this)
        identity.value = identityStore.loadOrCreate()


        loggedIn.value = auth.isLoggedIn()
        auth.handleIntent(intent.data) { loggedIn.value = it; if (it) loadProfile() }
        if (loggedIn.value) loadProfile()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (screen.value == "profile") {
                        ProfileScreen(
                            profile = profile.value,
                            onBack = { screen.value = "chat" },
                            onLogout = { auth.logout(); loggedIn.value = false; profileName.value = ""; profile.value = null; screen.value = "chat" },
                            onRecovery = { screen.value = "recovery" },
                        )
                    } else if (screen.value == "recovery") {
                        RecoveryScreen(
                            recovery = recovery,
                            identity = identity.value,
                            api = api,
                            onBack = { screen.value = "profile" },
                            onRestored = { restored -> identity.value = restored; identityStore.save(restored) },
                        )
                    } else if (screen.value == "messages") {
                        val me = identity.value
                        if (me != null && selfLogin.value.isNotBlank()) {
                            MessagesScreen(
                                identity = me,
                                selfLogin = selfLogin.value,
                                api = api,
                                onBack = { screen.value = "chat" },
                            )
                        } else {
                            Text("Сначала войдите")
                        }
                    } else if (screen.value == "dashboard") {
                        DashboardScreen(
                            api = api,
                            loggedIn = loggedIn.value,
                            onBack = { screen.value = "chat" },
                            onLogin = { auth.openLogin() },
                        )
                    } else {
                        ChatScreen(
                            loggedIn = loggedIn.value,
                            profileName = profileName.value,
                            onLogin = { auth.openLogin() },
                            onLogout = { auth.logout(); loggedIn.value = false; profileName.value = ""; profile.value = null },
                            onOpenProfile = { screen.value = "profile" },
                            onOpenDashboard = { screen.value = "dashboard" },
                            onOpenMessages = { screen.value = "messages" },
                            api = api,
                        )
                    }
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
                selfLogin.value = profile?.login ?: ""
                this.profile.value = profile
                if (profile == null) {
                    loggedIn.value = false
                } else {
                    identity.value?.let { api.saveIdentity(it.bundle) { } }
                }
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
    onOpenProfile: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenMessages: () -> Unit,
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
            Text(if (loggedIn) "Вы вошли как ${profileName.ifBlank { "стример" }}" else "StreamHata")
            Row {
                if (loggedIn) {
                    Button(onClick = onOpenMessages) { Text("Чаты") }
                    Button(onClick = onOpenDashboard) { Text("Статистика") }
                    Button(onClick = onOpenProfile) { Text("Профиль") }
                }
                Button(onClick = if (loggedIn) onLogout else onLogin) {
                    Text(if (loggedIn) "Выйти" else "Войти")
                }
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


@Composable
private fun ProfileScreen(profile: TwitchProfile?, onBack: () -> Unit, onLogout: () -> Unit, onRecovery: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Профиль", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onBack) { Text("Назад") }
        }
        if (profile == null) {
            Text("Профиль не загружен")
        } else {
            Text(profile.displayName, style = MaterialTheme.typography.headlineMedium)
            Text("@${profile.login}")
            if (profile.avatar.isNotBlank()) {
                Text(profile.avatar, style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(onClick = onRecovery) { Text("Резервная копия") }
        Button(onClick = onLogout) { Text("Выйти") }
    }
}


@Composable
private fun DashboardScreen(
    api: ApiClient,
    loggedIn: Boolean,
    onBack: () -> Unit,
    onLogin: () -> Unit,
) {
    var summary by remember { mutableStateOf<AnalyticsSummary?>(null) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(loggedIn) {
        while (loggedIn) {
            api.analytics { value ->
                summary = value
                if (value == null) error = "Нет данных или нужен вход"
            }
            delay(10_000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Статистика", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onBack) { Text("Назад") }
        }
        if (!loggedIn) {
            Text("Войдите, чтобы видеть статистику")
            Button(onClick = onLogin) { Text("Войти") }
            return@Column
        }
        if (summary == null) {
            Text(if (error.isBlank()) "Загрузка…" else error)
        } else {
            val data = summary!!
            Text(if (data.live) "В эфире" else "Последняя сессия", style = MaterialTheme.typography.bodyLarge)
            Text("Пик зрителей: ${data.peakViewers}", style = MaterialTheme.typography.headlineSmall)
            Text("Среднее: ${data.avgViewers}")
            Text("Чаттеры: ${data.chatters}")
            Text("Сообщения: ${data.messages}")
        }
    }
}


@Composable
private fun MessagesScreen(
    identity: MessagingIdentity,
    selfLogin: String,
    api: ApiClient,
    onBack: () -> Unit,
) {
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var selected by remember { mutableStateOf<Contact?>(null) }
    var lines = remember { mutableStateListOf<String>() }
    var draft by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        api.contacts { contacts = it; if (it.isNotEmpty()) selected = it.first() }
    }
    LaunchedEffect(selected) {
        val contact = selected ?: return@LaunchedEffect
        api.messages(contact.login) { items, peer ->
            lines.clear()
            items.forEach { item ->
                val text = runCatching {
                    val senderBundle = if (item.sender == selfLogin) identity.bundle else contact.bundle
                    val recipientBundle = if (item.sender == selfLogin) contact.bundle else null
                    MessagingCrypto.decrypt(identity, senderBundle, item.envelope, recipientBundle)
                }.getOrElse { "‹не удалось расшифровать›" }
                lines += "${item.sender}: $text"
            }
            if (peer != null) {
                contacts = contacts.map { if (it.login == contact.login) it.copy(bundle = peer) else it }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Сообщения", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onBack) { Text("Назад") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LazyColumn(modifier = Modifier.weight(0.35f)) {
                items(contacts) { contact ->
                    Button(onClick = { selected = contact }, modifier = Modifier.fillMaxWidth()) {
                        Text(contact.displayName)
                    }
                }
            }
            LazyColumn(modifier = Modifier.weight(0.65f)) {
                items(lines) { line -> Text(line, style = MaterialTheme.typography.bodySmall) }
            }
        }
        status.takeIf { it.isNotBlank() }?.let { Text(it) }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Сообщение") }
            )
            Button(
                modifier = Modifier.padding(start = 8.dp),
                onClick = {
                    val contact = selected
                    if (contact == null || draft.isBlank()) return@Button
                    val text = draft.trim()
                    val envelope = MessagingCrypto.encrypt(identity, selfLogin, contact.login, contact.bundle, text)
                    api.sendMessage(contact.login, envelope) { ok ->
                        status = if (ok) "Отправлено" else "Ошибка отправки"
                        if (ok) {
                            lines += "$selfLogin: $text"
                            draft = ""
                        }
                    }
                }
            ) { Text("Отправить") }
        }
    }
}


@Composable
private fun RecoveryScreen(
    recovery: RecoveryManager,
    identity: MessagingIdentity?,
    api: ApiClient,
    onBack: () -> Unit,
    onRestored: (MessagingIdentity) -> Unit,
) {
    val context = LocalContext.current
    var mnemonic by remember { mutableStateOf("") }
    var restoreText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Recovery", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onBack) { Text("Назад") }
        }
        Button(onClick = {
            val current = identity ?: return@Button
            val phrase = recovery.generateMnemonic()
            val blob = recovery.encrypt(current, phrase)
            api.saveRecovery(blob) { ok ->
                status = if (ok) "Копия сохранена. Запиши 24 слова." else "Не удалось сохранить копию"
                if (ok) mnemonic = phrase
            }
        }) { Text("Создать резервную копию") }

        if (mnemonic.isNotBlank()) {
            Text("24 слова", style = MaterialTheme.typography.titleMedium)
            Text(mnemonic)
            Button(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, mnemonic)
                }
                context.startActivity(Intent.createChooser(send, "Сохранить recovery-фразу"))
            }) { Text("Сохранить в заметки") }
        }

        Text("Восстановление", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = restoreText,
            onValueChange = { restoreText = it },
            placeholder = { Text("24 слова через пробел") }
        )
        Button(onClick = {
            if (!recovery.validate(restoreText)) {
                status = "Неверная фраза"
                return@Button
            }
            api.loadRecovery { blob ->
                if (blob == null) {
                    status = "Резервная копия не найдена"
                } else {
                    runCatching { recovery.decrypt(blob, restoreText) }
                        .onSuccess { restored -> onRestored(restored); status = "Восстановлено" }
                        .onFailure { status = "Не удалось расшифровать" }
                }
            }
        }) { Text("Восстановить") }
        if (status.isNotBlank()) Text(status)
    }
}
