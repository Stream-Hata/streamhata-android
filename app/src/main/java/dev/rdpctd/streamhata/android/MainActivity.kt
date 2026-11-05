package dev.rdpctd.streamhata.android

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rdpctd.streamhata.core.AnalyticsSummary
import dev.rdpctd.streamhata.core.ApiClient
import dev.rdpctd.streamhata.core.AuthManager
import dev.rdpctd.streamhata.core.ChatMessage
import dev.rdpctd.streamhata.core.Contact
import dev.rdpctd.streamhata.core.MessagingCrypto
import dev.rdpctd.streamhata.core.MessagingIdentity
import dev.rdpctd.streamhata.core.MessagingIdentityStore
import dev.rdpctd.streamhata.core.RecoveryManager
import dev.rdpctd.streamhata.core.TwitchProfile
import dev.rdpctd.streamhata.core.ui.StreamHataBorder
import dev.rdpctd.streamhata.core.ui.StreamHataCyan
import dev.rdpctd.streamhata.core.ui.StreamHataGreen
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
    private lateinit var identityStore: MessagingIdentityStore
    private lateinit var recovery: RecoveryManager
    private val loggedIn = mutableStateOf(false)
    private val profile = mutableStateOf<TwitchProfile?>(null)
    private val identity = mutableStateOf<MessagingIdentity?>(null)

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
            StreamHataTheme {
                App(
                    loggedIn = loggedIn.value,
                    profile = profile.value,
                    identity = identity.value,
                    auth = auth,
                    api = api,
                    recovery = recovery,
                    onLogin = { auth.openLogin() },
                    onLogout = {
                        auth.logout()
                        loggedIn.value = false
                        profile.value = null
                    },
                    onProfile = { loadProfile() },
                    onRestored = { restored -> identity.value = restored; identityStore.save(restored) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        auth.handleIntent(intent.data) { loggedIn.value = it; if (it) loadProfile() }
    }

    private fun loadProfile() {
        api.profile { value ->
            runOnUiThread {
                profile.value = value
                if (value == null) {
                    loggedIn.value = false
                } else {
                    identity.value?.let { api.saveIdentity(it.bundle) { } }
                }
            }
        }
    }
}

@Composable
private fun App(
    loggedIn: Boolean,
    profile: TwitchProfile?,
    identity: MessagingIdentity?,
    auth: AuthManager,
    api: ApiClient,
    recovery: RecoveryManager,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onProfile: () -> Unit,
    onRestored: (MessagingIdentity) -> Unit,
) {
    var screen by remember { mutableStateOf("chat") }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { AppHeader(loggedIn = loggedIn, profile = profile) },
        bottomBar = {
            NavigationBar(containerColor = StreamHataSurface, contentColor = StreamHataText) {
                listOf(
                    "chat" to "Чат",
                    "messages" to "Чаты",
                    "stats" to "Статистика",
                    "profile" to "Профиль"
                ).forEach { (id, label) ->
                    NavigationBarItem(
                        selected = screen == id,
                        onClick = { screen = id },
                        icon = {},
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        CompositionLocalProvider(LocalContentColor provides StreamHataText) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F0817), Color(0xFF130B1D), Color(0xFF0F0817))
                    )
                )
        ) {
            when (screen) {
                "messages" -> MessagesScreen(
                    loggedIn = loggedIn,
                    identity = identity,
                    selfLogin = profile?.login.orEmpty(),
                    api = api,
                    onLogin = onLogin,
                )
                "stats" -> DashboardScreen(api = api, loggedIn = loggedIn, onLogin = onLogin)
                "profile" -> ProfileScreen(
                    profile = profile,
                    loggedIn = loggedIn,
                    onLogin = onLogin,
                    onLogout = onLogout,
                    onRefresh = onProfile,
                    onRecovery = { screen = "recovery" },
                )
                "recovery" -> RecoveryScreen(
                    recovery = recovery,
                    identity = identity,
                    api = api,
                    onBack = { screen = "profile" },
                    onRestored = onRestored,
                )
                else -> ChatScreen(
                    loggedIn = loggedIn,
                    self = profile?.login.orEmpty(),
                    api = api,
                    onLogin = onLogin,
                )
            }
        }
        }
    }
}

@Composable
private fun AppHeader(loggedIn: Boolean, profile: TwitchProfile?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StreamHataSurface.copy(alpha = 0.94f))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    Brush.linearGradient(listOf(StreamHataPink, StreamHataCyan)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("S", color = Color(0xFF0F0817), fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("StreamHata", style = MaterialTheme.typography.titleMedium)
            Text(
                if (loggedIn) "@${profile?.login.orEmpty()}" else "android alpha",
                style = MaterialTheme.typography.bodySmall,
                color = StreamHataMuted
            )
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(if (loggedIn) StreamHataGreen else StreamHataMuted, CircleShape)
        )
    }
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
        border = BorderStroke(1.dp, StreamHataBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(modifier = Modifier.padding(14.dp)) { content() }
    }
}

@Composable
private fun LoginCard(onLogin: () -> Unit) {
    Panel {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Вход через Twitch", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Откроется браузер. После логина приложение получит сессию автоматически.",
                style = MaterialTheme.typography.bodyMedium,
                color = StreamHataMuted
            )
            Button(onClick = onLogin) { Text("Войти через браузер") }
        }
    }
}

@Composable
private fun ChatScreen(loggedIn: Boolean, self: String, api: ApiClient, onLogin: () -> Unit) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var draft by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            api.chatRecent { list -> messages.clear(); messages.addAll(list) }
            delay(3000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp).imePadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!loggedIn) LoginCard(onLogin)
        Text("Twitch-чат", style = MaterialTheme.typography.headlineSmall)
        status.takeIf { it.isNotBlank() }?.let {
            Text(it, color = StreamHataPink, style = MaterialTheme.typography.bodySmall)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
        Composer(
            value = draft,
            placeholder = "Сообщение в Twitch-чат",
            onChange = { draft = it },
            onSend = {
                if (!loggedIn) {
                    status = "Сначала войдите"
                } else if (draft.isNotBlank()) {
                    val text = draft.trim()
                    api.sendChat(text, "") { ok -> status = if (ok) "Отправлено" else "Ошибка отправки" }
                    draft = ""
                }
            }
        )
    }
}

@Composable
private fun MessagesScreen(
    loggedIn: Boolean,
    identity: MessagingIdentity?,
    selfLogin: String,
    api: ApiClient,
    onLogin: () -> Unit,
) {
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var selected by remember { mutableStateOf<Contact?>(null) }
    var lines = remember { mutableStateListOf<String>() }
    var draft by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(loggedIn) {
        if (loggedIn) api.contacts { contacts = it; if (it.isNotEmpty() && selected == null) selected = it.first() }
    }
    LaunchedEffect(selected) {
        val contact = selected ?: return@LaunchedEffect
        val me = identity ?: return@LaunchedEffect
        api.messages(contact.login) { items, peer ->
            lines.clear()
            items.forEach { item ->
                val text = runCatching {
                    val senderBundle = if (item.sender == selfLogin) me.bundle else contact.bundle
                    val recipient = if (item.sender == selfLogin) contact.bundle else null
                    MessagingCrypto.decrypt(me, senderBundle, item.envelope, recipient)
                }.getOrElse { "‹не удалось расшифровать›" }
                lines += "${item.sender}: $text"
            }
            if (peer != null) contacts = contacts.map { if (it.login == contact.login) it.copy(bundle = peer) else it }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(14.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!loggedIn) LoginCard(onLogin)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Сообщения", style = MaterialTheme.typography.headlineSmall)
            Text(selected?.displayName ?: "выберите чат", color = StreamHataMuted, style = MaterialTheme.typography.bodySmall)
        }
        if (selected == null) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(contacts) { contact ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                        border = BorderStroke(1.dp, StreamHataBorder)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(34.dp).background(StreamHataPink.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Text(contact.displayName.take(2).uppercase(), color = StreamHataPink) }
                            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(contact.displayName, fontWeight = FontWeight.SemiBold)
                                Text("@${contact.login}", color = StreamHataMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = { selected = contact }) { Text("Открыть") }
                        }
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(lines) { line ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StreamHataSurfaceHigh),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text(line, modifier = Modifier.padding(10.dp)) }
                }
            }
            Composer(
                value = draft,
                placeholder = "Сообщение",
                onChange = { draft = it },
                onSend = {
                    val contact = selected
                    val me = identity
                    if (contact == null || me == null || draft.isBlank()) return@Composer
                    val text = draft.trim()
                    val envelope = MessagingCrypto.encrypt(me, selfLogin, contact.login, contact.bundle, text)
                    api.sendMessage(contact.login, envelope) { ok ->
                        status = if (ok) "Отправлено" else "Ошибка отправки"
                        if (ok) { lines += "$selfLogin: $text"; draft = "" }
                    }
                }
            )
        }
        status.takeIf { it.isNotBlank() }?.let { Text(it, color = StreamHataPink, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun DashboardScreen(api: ApiClient, loggedIn: Boolean, onLogin: () -> Unit) {
    var summary by remember { mutableStateOf<AnalyticsSummary?>(null) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(loggedIn) {
        while (loggedIn) {
            api.analytics { value -> summary = value; if (value == null) error = "Нет данных или нужен вход" }
            delay(10_000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Статистика", style = MaterialTheme.typography.headlineSmall)
        if (!loggedIn) LoginCard(onLogin)
        if (loggedIn && summary == null) Text(if (error.isBlank()) "Загрузка…" else error, color = StreamHataMuted)
        summary?.let { data ->
            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (data.live) "В эфире" else "Последняя сессия", color = StreamHataGreen, fontWeight = FontWeight.SemiBold)
                    Text("Пик зрителей", color = StreamHataMuted, style = MaterialTheme.typography.bodySmall)
                    Text("${data.peakViewers}", style = MaterialTheme.typography.headlineLarge)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("Среднее", "${data.avgViewers}", Modifier.weight(1f))
                MiniStat("Чаттеры", "${data.chatters}", Modifier.weight(1f))
                MiniStat("Сообщения", "${data.messages}", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
        border = BorderStroke(1.dp, StreamHataBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = StreamHataMuted, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun ProfileScreen(
    profile: TwitchProfile?,
    loggedIn: Boolean,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    onRecovery: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Профиль", style = MaterialTheme.typography.headlineSmall)
        if (!loggedIn) {
            LoginCard(onLogin)
        } else {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(54.dp).background(StreamHataPink.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text((profile?.displayName ?: "?").take(2).uppercase(), color = StreamHataPink, fontWeight = FontWeight.Bold) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile?.displayName.orEmpty(), style = MaterialTheme.typography.titleMedium)
                        Text("@${profile?.login.orEmpty()}", color = StreamHataMuted)
                    }
                }
            }
            Button(onClick = onRefresh) { Text("Обновить профиль") }
            Button(onClick = onRecovery) { Text("Резервная копия") }
            Button(onClick = onLogout) { Text("Выйти") }
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

    Column(modifier = Modifier.fillMaxSize().padding(14.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("24 слова", style = MaterialTheme.typography.titleMedium)
                    Text(mnemonic, color = StreamHataCyan)
                    Button(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, mnemonic)
                        }
                        context.startActivity(Intent.createChooser(send, "Сохранить recovery-фразу"))
                    }) { Text("Сохранить в заметки") }
                }
            }
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
                        .onSuccess { onRestored(it); status = "Восстановлено" }
                        .onFailure { status = "Не удалось расшифровать" }
                }
            }
        }) { Text("Восстановить") }
        status.takeIf { it.isNotBlank() }?.let { Text(it, color = StreamHataMuted) }
    }
}

@Composable
private fun Composer(
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder) },
            singleLine = false,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Button(onClick = onSend) { Text("→") }
    }
}
