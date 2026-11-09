package dev.rdpctd.streamhata.android

data class AppDestination(
    val id: String,
    val label: String,
    val external: Boolean = false,
    val advanced: Boolean = false
)

data class AppSection(
    val title: String,
    val items: List<AppDestination>
)

val APP_SECTIONS = listOf(
    AppSection(
        "Основное",
        listOf(
            AppDestination("chat", "Чат", external = true),
            AppDestination("messages", "Сообщения", external = true),
            AppDestination("games", "Игры"),
            AppDestination("events", "События"),
            AppDestination("stats", "Дашборд")
        )
    ),
    AppSection(
        "Стрим",
        listOf(
            AppDestination("stream", "Стрим"),
            AppDestination("commands", "Редактор команд"),
            AppDestination("widgets", "Виджеты", advanced = true),
            AppDestination("moderation", "Модерация", advanced = true),
            AppDestination("donations", "Донаты", advanced = true),
            AppDestination("jury", "Чат-жюри", advanced = true),
            AppDestination("telegram", "Telegram", advanced = true),
            AppDestination("wheel", "Колесо", advanced = true)
        )
    ),
    AppSection(
        "Активность",
        listOf(
            AppDestination("viewers", "Зрители", advanced = true),
            AppDestination("points", "Баллы", advanced = true),
            AppDestination("polls", "Опросы", advanced = true),
            AppDestination("roulette", "Рулетка", advanced = true),
            AppDestination("music", "Музыка", advanced = true),
            AppDestination("journal", "Журнал", advanced = true),
            AppDestination("notifications", "Уведомления", advanced = true)
        )
    ),
    AppSection(
        "Аккаунт",
        listOf(
            AppDestination("settings", "Настройки"),
            AppDestination("profile", "Профиль")
        )
    )
)
