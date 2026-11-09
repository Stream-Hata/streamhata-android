package dev.rdpctd.streamhata.android

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.rdpctd.streamhata.core.ApiClient
import dev.rdpctd.streamhata.core.CommandSummary
import dev.rdpctd.streamhata.core.TriggerSummary
import dev.rdpctd.streamhata.core.VariableSummary
import dev.rdpctd.streamhata.core.ui.StreamHataBorder
import dev.rdpctd.streamhata.core.ui.StreamHataLoading
import dev.rdpctd.streamhata.core.ui.StreamHataMuted
import dev.rdpctd.streamhata.core.ui.StreamHataPink
import dev.rdpctd.streamhata.core.ui.StreamHataSurface
import dev.rdpctd.streamhata.core.ui.StreamHataText

private data class CommandAction(val value: String, val label: String)

private val COMMAND_ACTIONS = listOf(
    CommandAction("say", "Сообщение в чат"),
    CommandAction("announce", "Анонс"),
    CommandAction("shoutout", "Шаут"),
    CommandAction("raid", "Рейд"),
    CommandAction("raid.cancel", "Отменить рейд"),
    CommandAction("marker", "Маркер стрима"),
    CommandAction("clip", "Создать клип"),
    CommandAction("delete", "Удалить сообщение"),
    CommandAction("ban", "Бан"),
    CommandAction("unban", "Разбан"),
    CommandAction("timeout", "Таймаут"),
    CommandAction("shield.on", "Shield Mode вкл"),
    CommandAction("shield.off", "Shield Mode выкл"),
    CommandAction("title.set", "Сменить заголовок"),
    CommandAction("game.set", "Сменить игру"),
    CommandAction("commercial", "Реклама"),
    CommandAction("poll", "Опрос"),
    CommandAction("poll.end", "Завершить опрос"),
    CommandAction("points.add", "Начислить баллы"),
    CommandAction("points.remove", "Снять баллы"),
    CommandAction("points.set", "Установить баллы"),
    CommandAction("points.balance", "Мой баланс"),
    CommandAction("points.top", "Топ баллов"),
    CommandAction("points.shop", "Магазин баллов"),
    CommandAction("points.redeem", "Купить награду"),
    CommandAction("music.add", "Добавить музыку"),
    CommandAction("music.current", "Текущая музыка"),
    CommandAction("lastmusic", "Последняя музыка"),
    CommandAction("wheel", "Колесо"),
    CommandAction("games.slots", "Слоты"),
    CommandAction("games.wheel", "Колесо игр"),
    CommandAction("games.trivia", "Викторина"),
    CommandAction("games.duel", "Дуэль"),
    CommandAction("games.accept", "Принять дуэль"),
    CommandAction("quote.add", "Добавить цитату"),
    CommandAction("quotes.random", "Случайная цитата"),
    CommandAction("dice", "Кубик"),
    CommandAction("coinflip", "Монетка"),
    CommandAction("random", "Случайный выбор"),
    CommandAction("random.number", "Случайное число"),
    CommandAction("variable.set", "Установить переменную"),
    CommandAction("variable.inc", "Увеличить переменную"),
    CommandAction("variable.dec", "Уменьшить переменную"),
    CommandAction("command.run", "Выполнить команду"),
    CommandAction("timer.start", "Запустить таймер"),
    CommandAction("timer.pause", "Пауза таймера"),
    CommandAction("timer.reset", "Сбросить таймер"),
    CommandAction("timer.add", "Добавить время"),
    CommandAction("twitch.followage", "Возраст фоллова"),
    CommandAction("twitch.accountage", "Возраст аккаунта"),
    CommandAction("twitch.uptime", "Аптайм"),
    CommandAction("twitch.viewers", "Зрители"),
    CommandAction("twitch.title", "Заголовок"),
    CommandAction("twitch.game", "Игра"),
    CommandAction("twitch.followers", "Фолловеры"),
    CommandAction("twitch.subscribers", "Подписчики"),
    CommandAction("twitch.chatters", "Чаттеры"),
    CommandAction("widget.enable", "Включить виджет"),
    CommandAction("widget.disable", "Выключить виджет"),
    CommandAction("widget.toggle", "Переключить виджет"),
    CommandAction("widget.config", "Настроить виджет"),
    CommandAction("alert.test", "Тест алерта"),
    CommandAction("roulette.spin", "Рулетка"),
    CommandAction("roulette.last", "Последняя рулетка"),
    CommandAction("reward.create", "Создать награду"),
    CommandAction("reward.update", "Обновить награду"),
    CommandAction("reward.delete", "Удалить награду"),
    CommandAction("reward.fulfill", "Выполнить награду"),
    CommandAction("mod.add", "Добавить модератора"),
    CommandAction("mod.remove", "Убрать модератора"),
    CommandAction("vip.add", "Добавить VIP"),
    CommandAction("vip.remove", "Убрать VIP"),
    CommandAction("chat.settings", "Настройки чата"),
)

private fun actionLabel(value: String): String = COMMAND_ACTIONS.firstOrNull { it.value == value }?.label ?: value

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionPicker(value: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }

    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Text(actionLabel(value))
    }
    if (open) {
        ModalBottomSheet(onDismissRequest = { open = false }) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Действие команды", style = MaterialTheme.typography.titleMedium)
                LazyColumn(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(COMMAND_ACTIONS) { action ->
                        TextButton(
                            onClick = { onSelect(action.value); open = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(action.label, color = if (value == action.value) StreamHataPink else StreamHataText)
                                Text(action.value, color = StreamHataMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val TAB_COMMANDS = 0
private const val TAB_TRIGGERS = 1
private const val TAB_VARIABLES = 2

@Composable
fun CommandEditorScreen(api: ApiClient, onBack: () -> Unit) {
    var tab by remember { mutableStateOf(TAB_COMMANDS) }
    var commands by remember { mutableStateOf<List<CommandSummary>>(emptyList()) }
    var triggers by remember { mutableStateOf<List<TriggerSummary>>(emptyList()) }
    var variables by remember { mutableStateOf<List<VariableSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf("") }

    fun reload() {
        api.commands { value -> commands = value }
        api.triggers { value -> triggers = value }
        api.variables { value -> variables = value; loading = false }
    }

    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Редактор чата", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Назад") }
        }
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
            Tab(selected = tab == TAB_COMMANDS, onClick = { tab = TAB_COMMANDS }, text = { Text("Команды") })
            Tab(selected = tab == TAB_TRIGGERS, onClick = { tab = TAB_TRIGGERS }, text = { Text("Триггеры") })
            Tab(selected = tab == TAB_VARIABLES, onClick = { tab = TAB_VARIABLES }, text = { Text("Переменные") })
        }
        if (status.isNotBlank()) {
            Text(status, color = StreamHataPink, modifier = Modifier.padding(12.dp))
        }
        if (loading) {
            StreamHataLoading("Загружаем настройки чата…")
        } else {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    (fadeIn(tween(180)) + slideInHorizontally(tween(180)) { it / 12 })
                        .togetherWith(fadeOut(tween(120)) + slideOutHorizontally(tween(180)) { -it / 12 })
                },
                modifier = Modifier.fillMaxSize(),
                label = "command-tab"
            ) { selectedTab ->
                when (selectedTab) {
                    TAB_COMMANDS -> CommandsTab(api, commands) { status = it; reload() }
                    TAB_TRIGGERS -> TriggersTab(api, triggers) { status = it; reload() }
                    else -> VariablesTab(api, variables) { status = it; reload() }
                }
            }
        }
    }
}

@Composable
private fun CommandsTab(api: ApiClient, commandList: List<CommandSummary>, onChanged: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var aliases by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("say") }
    var response by remember { mutableStateOf("") }
    var cooldown by remember { mutableStateOf("10") }
    var level by remember { mutableStateOf("everyone") }
    var enabled by remember { mutableStateOf(true) }
    var editing by remember { mutableStateOf<CommandSummary?>(null) }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("Имя без !") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(aliases, { aliases = it }, label = { Text("Алиасы через запятую") }, modifier = Modifier.fillMaxWidth())
        Text("Действие", color = StreamHataMuted)
        ActionPicker(action) { action = it }
        OutlinedTextField(response, { response = it }, label = { Text("Ответ") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(cooldown, { cooldown = it.filter(Char::isDigit).take(5) }, label = { Text("Кулдаун, сек") }, modifier = Modifier.weight(1f))
            OutlinedTextField(level, { level = it }, label = { Text("Уровень") }, modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Включена", color = StreamHataMuted)
            Switch(checked = enabled, onCheckedChange = { enabled = it })
        }
        Button(
            enabled = name.isNotBlank(),
            onClick = {
                val command = CommandSummary(
                    id = editing?.id.orEmpty(),
                    name = name.trim().removePrefix("!"),
                    action = action.trim().ifBlank { "say" },
                    response = response.trim(),
                    enabled = enabled,
                    aliases = aliases.split(',').map { it.trim().removePrefix("!") }.filter { it.isNotBlank() },
                    cooldownSeconds = cooldown.toIntOrNull() ?: 0,
                    userLevel = level.trim().ifBlank { "everyone" }
                )
                api.saveCommand(command) { ok ->
                    onChanged(if (ok) "Команда сохранена" else "Ошибка сохранения")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (editing == null) "Создать команду" else "Сохранить изменения") }
        Text("Существующие команды", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(commandList) { command ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                    border = BorderStroke(1.dp, StreamHataBorder)
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("!${command.name}", style = MaterialTheme.typography.titleMedium)
                            Text(if (command.enabled) "вкл" else "выкл", color = StreamHataMuted)
                        }
                        Text("${command.action} · ${command.userLevel} · ${command.cooldownSeconds}s", color = StreamHataMuted, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = {
                                editing = command
                                name = command.name
                                aliases = command.aliases.joinToString(", ")
                                action = command.action
                                response = command.response
                                cooldown = command.cooldownSeconds.toString()
                                level = command.userLevel
                                enabled = command.enabled
                            }) { Text("Изменить") }
                            TextButton(onClick = { api.deleteCommand(command.id) { onChanged("Команда удалена") } }) { Text("Удалить") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TriggersTab(api: ApiClient, triggerList: List<TriggerSummary>, onChanged: (String) -> Unit) {
    var action by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("!") }
    var aliases by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<TriggerSummary?>(null) }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Триггеры связывают команду чата с действием.", color = StreamHataMuted)
        Text("Действие", color = StreamHataMuted)
        ActionPicker(action) { action = it }
        OutlinedTextField(command, { command = it }, label = { Text("Команда") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(aliases, { aliases = it }, label = { Text("Алиасы через запятую") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text("Описание") }, modifier = Modifier.fillMaxWidth())
        Button(
            enabled = action.isNotBlank() && command.isNotBlank(),
            onClick = {
                val item = TriggerSummary(
                    action = action.trim(),
                    command = command.trim(),
                    aliases = aliases.split(',').map { it.trim() }.filter { it.isNotBlank() },
                    enabled = true,
                    description = description.trim()
                )
                val current = editing
                val next = if (current == null) triggerList + item else triggerList.map {
                    if (it.action == current.action && it.command == current.command) item else it
                }
                api.saveTriggers(next) { ok ->
                    onChanged(if (ok) "Триггер сохранён" else "Ошибка сохранения")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (editing == null) "Добавить триггер" else "Сохранить триггер") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(triggerList) { item ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                    border = BorderStroke(1.dp, StreamHataBorder)
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${item.command} → ${item.action}", style = MaterialTheme.typography.titleMedium)
                        Text(item.description.ifBlank { item.aliases.joinToString(", ") }, color = StreamHataMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = {
                                editing = item
                                action = item.action
                                command = item.command
                                aliases = item.aliases.joinToString(", ")
                                description = item.description
                            }) { Text("Изменить") }
                            TextButton(onClick = {
                                api.saveTriggers(triggerList.filter { it != item }) { onChanged("Триггер удалён") }
                            }) { Text("Удалить") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VariablesTab(api: ApiClient, variableList: List<VariableSummary>, onChanged: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("Имя переменной") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value, { value = it }, label = { Text("Значение") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(action, { action = it }, label = { Text("Действие (необязательно)") }, modifier = Modifier.fillMaxWidth())
        Button(
            enabled = name.isNotBlank(),
            onClick = {
                api.saveVariable(VariableSummary("", name.trim(), value.trim(), action.trim())) { ok ->
                    onChanged(if (ok) "Переменная сохранена" else "Ошибка сохранения")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Сохранить переменную") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(variableList) { item ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                    border = BorderStroke(1.dp, StreamHataBorder)
                ) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("\$(${item.name})", style = MaterialTheme.typography.titleMedium)
                            Text(item.value, color = StreamHataMuted)
                        }
                        TextButton(onClick = { api.deleteVariable(item.id) { onChanged("Переменная удалена") } }) {
                            Text("Удалить")
                        }
                    }
                }
            }
        }
    }
}
