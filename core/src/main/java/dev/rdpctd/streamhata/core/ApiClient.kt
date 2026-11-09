package dev.rdpctd.streamhata.core

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class TwitchProfile(
    val login: String,
    val displayName: String,
    val avatar: String
)

data class ChatMessage(
    val author: String,
    val text: String,
    val color: String?
)

data class Contact(
    val login: String,
    val displayName: String,
    val avatar: String,
    val isTest: Boolean,
    val bundle: PublicBundle
)

data class EnvelopeItem(
    val id: Long,
    val sender: String,
    val recipient: String,
    val envelope: JSONObject
)

data class AnalyticsSummary(
    val live: Boolean,
    val peakViewers: Int,
    val avgViewers: Int,
    val chatters: Int,
    val messages: Int
)
data class JurySettings(
    val enabled: Boolean,
    val durationSeconds: Int,
    val minConfidence: Double
)

data class ObsStatusSummary(
    val connected: Boolean,
    val currentScene: String,
    val scenes: List<String>,
    val streaming: Boolean,
    val recording: Boolean,
    val error: String
)

data class TelegramCommandSummary(
    val id: String,
    val command: String,
    val response: String
)

data class WheelSummary(
    val title: String,
    val state: String,
    val pot: Int,
    val players: Int
)

data class DonationSummary(
    val username: String,
    val amount: Double,
    val currency: String,
    val message: String
)

data class JurySummary(
    val text: String,
    val yea: Int,
    val nei: Int,
    val verdict: String
)

data class WidgetState(
    val kind: String,
    val enabled: Boolean
)

data class ModerationSummary(
    val dryRun: Boolean,
    val bannedWords: Int,
    val blockLinks: Boolean,
    val capsEnabled: Boolean,
    val logSize: Int
)

data class StreamStatus(
    val title: String,
    val gameName: String,
    val viewers: Int,
    val shieldMode: Boolean
)

data class TriggerSummary(
    val action: String,
    val command: String,
    val aliases: List<String>,
    val enabled: Boolean,
    val description: String
)

data class VariableSummary(
    val id: String,
    val name: String,
    val value: String,
    val action: String
)

data class CommandSummary(
    val id: String,
    val name: String,
    val action: String,
    val response: String,
    val enabled: Boolean,
    val aliases: List<String> = emptyList(),
    val cooldownSeconds: Int = 0,
    val userLevel: String = "everyone"
)

data class PollSummary(
    val title: String,
    val options: List<String>,
    val ended: Boolean
)

data class RouletteSummary(
    val remaining: Int,
    val lastTitle: String,
    val lastPlatform: String
)

data class AlertSummary(
    val id: String,
    val type: String,
    val username: String,
    val message: String,
    val createdAt: String
)

data class MusicTrack(
    val videoId: String,
    val title: String,
    val author: String,
    val durationSeconds: Int
)

data class ViewerSummary(
    val login: String,
    val displayName: String,
    val watchStreak: Int
)

data class PointsWallet(
    val login: String,
    val displayName: String,
    val balance: Int
)

data class StreamEvent(
    val id: Int,
    val title: String,
    val type: String,
    val status: String,
    val startsAt: String,
    val createdAt: String
)

data class GameSummary(
    val slug: String,
    val title: String,
    val year: Int,
    val cover: String
)

data class GamePlatform(
    val name: String,
    val code: String
)

data class GameDownload(
    val name: String,
    val url: String,
    val note: String
)

data class GameDetail(
    val slug: String,
    val title: String,
    val titleRu: String,
    val year: Int,
    val cover: String,
    val description: String,
    val descriptionRu: String,
    val platforms: List<GamePlatform>,
    val genres: List<String>,
    val developers: List<String>,
    val publishers: List<String>,
    val rating: Double,
    val screenshots: List<String>,
    val downloads: List<GameDownload>,
    val website: String
)

data class GameDiscussionMessage(
    val id: Long,
    val username: String,
    val displayName: String,
    val message: String,
    val parentId: Long?,
    val likes: Int,
    val liked: Boolean,
    val createdAt: String
)

class ApiClient(private val tokenProvider: () -> String?) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val token = tokenProvider().orEmpty()
            val request = chain.request().newBuilder()
                .header("Cookie", "e-hub-refresh-token=$token")
                .build()
            chain.proceed(request)
        }
        .build()

    fun profile(callback: (TwitchProfile?) -> Unit) {
        request("/rgg-sync/twitch") { json ->
            val user = json?.optJSONObject("user")
            val login = user?.optString("username").orEmpty()
            if (json?.optBoolean("valid") == true && login.isNotBlank()) {
                callback(
                    TwitchProfile(
                        login = login,
                        displayName = user?.optString("displayName").orEmpty().ifBlank { login },
                        avatar = user?.optString("avatar").orEmpty()
                    )
                )
            } else {
                callback(null)
            }
        }
    }

    fun saveIdentity(bundle: PublicBundle, callback: (Boolean) -> Unit) {
        postJson("/rgg-sync/messaging/identity", bundle.toJson()) { callback(it != null) }
    }

    fun contacts(query: String = "", callback: (List<Contact>) -> Unit) {
        val suffix = if (query.isBlank()) "" else "?q=" + URLEncoder.encode(query, "UTF-8")
        request("/rgg-sync/messaging/contacts$suffix") { json ->
            val result = mutableListOf<Contact>()
            val array = json?.optJSONArray("items")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    runCatching {
                        result += Contact(
                            login = item.getString("login"),
                            displayName = item.optString("displayName").ifBlank { item.getString("login") },
                            avatar = item.optString("avatar"),
                            isTest = item.optBoolean("isTest"),
                            bundle = PublicBundle.from(item)
                        )
                    }
                }
            }
            callback(result)
        }
    }

    fun messages(peer: String, callback: (List<EnvelopeItem>, PublicBundle?) -> Unit) {
        request("/rgg-sync/messaging/messages?with=" + URLEncoder.encode(peer, "UTF-8")) { json ->
            val items = mutableListOf<EnvelopeItem>()
            val array = json?.optJSONArray("items")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    runCatching {
                        items += EnvelopeItem(
                            id = item.getLong("id"),
                            sender = item.getString("sender"),
                            recipient = item.getString("recipient"),
                            envelope = item.getJSONObject("envelope")
                        )
                    }
                }
            }
            val peerBundle = json?.optJSONObject("peer")?.let { runCatching { PublicBundle.from(it) }.getOrNull() }
            callback(items, peerBundle)
        }
    }

    fun sendMessage(recipient: String, envelope: JSONObject, callback: (Boolean) -> Unit) {
        postJson(
            "/rgg-sync/messaging/messages",
            JSONObject().put("recipient", recipient).put("envelope", envelope)
        ) { callback(it != null) }
    }

    fun saveRecovery(blob: JSONObject, callback: (Boolean) -> Unit) {
        postJson("/rgg-sync/messaging/recovery", blob) { callback(it != null) }
    }

    fun loadRecovery(callback: (JSONObject?) -> Unit) {
        request("/rgg-sync/messaging/recovery") { callback(it) }
    }

    fun jurySettings(callback: (JurySettings?) -> Unit) {
        request("/rgg-sync/chat-jury/settings") { json ->
            if (json == null) callback(null) else callback(
                JurySettings(
                    enabled = json.optBoolean("enabled"),
                    durationSeconds = json.optInt("duration_seconds"),
                    minConfidence = json.optDouble("min_confidence")
                )
            )
        }
    }

    fun startJury(text: String, callback: (Boolean) -> Unit) {
        postJson("/rgg-sync/chat-jury/start", JSONObject().put("text", text).put("author", "streamer").put("displayName", "Стример").put("confidence", 0.9)) { callback(it != null) }
    }

    fun finishJury(callback: (Boolean) -> Unit) {
        postJson("/rgg-sync/chat-jury/finish", JSONObject()) { callback(it != null) }
    }

    fun saveJurySettings(enabled: Boolean, durationSeconds: Int, minConfidence: Double, callback: (Boolean) -> Unit) {
        patchJson("/rgg-sync/chat-jury/settings", JSONObject().put("enabled", enabled).put("duration_seconds", durationSeconds).put("min_confidence", minConfidence)) { callback(it != null) }
    }

    fun obs(callback: (ObsStatusSummary?) -> Unit) {
        request("/rgg-sync/obs") { json ->
            if (json == null) return@request callback(null)
            val scenes = mutableListOf<String>()
            val array = json.optJSONArray("scenes")
            if (array != null) for (index in 0 until array.length()) scenes += array.optString(index)
            callback(
                ObsStatusSummary(
                    connected = json.optBoolean("connected"),
                    currentScene = json.optString("currentScene"),
                    scenes = scenes,
                    streaming = json.optBoolean("streaming"),
                    recording = json.optBoolean("recording"),
                    error = json.optString("error")
                )
            )
        }
    }

    fun setObsScene(scene: String, callback: (Boolean) -> Unit) {
        postJson("/rgg-sync/obs/action", JSONObject().put("type", "scene").put("scene", scene)) { callback(it != null) }
    }

    fun telegramCommands(callback: (List<TelegramCommandSummary>) -> Unit) {
        request("/rgg-sync/telegram/commands") { json ->
            val items = mutableListOf<TelegramCommandSummary>()
            val array = json?.optJSONArray("commands")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    items += TelegramCommandSummary(
                        id = item.optString("id"),
                        command = item.optString("command"),
                        response = item.optString("response")
                    )
                }
            }
            callback(items)
        }
    }

    fun wheel(callback: (WheelSummary?) -> Unit) {
        request("/rgg-sync/games/wheel") { json ->
            val round = json?.optJSONObject("round") ?: return@request callback(null)
            callback(
                WheelSummary(
                    title = round.optString("title"),
                    state = round.optString("state"),
                    pot = round.optInt("pot"),
                    players = round.optJSONArray("participants")?.length() ?: 0
                )
            )
        }
    }

    fun spinWheel(callback: (Boolean) -> Unit) {
        postJson("/rgg-sync/games/wheel/spin", JSONObject()) { callback(it != null) }
    }

    fun donations(callback: (List<DonationSummary>) -> Unit) {
        request("/rgg-sync/donations") { json ->
            val items = mutableListOf<DonationSummary>()
            val array = json?.optJSONArray("recent")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    items += DonationSummary(
                        username = item.optString("username"),
                        amount = item.optDouble("amount"),
                        currency = item.optString("currency"),
                        message = item.optString("message")
                    )
                }
            }
            callback(items)
        }
    }

    fun jury(callback: (JurySummary?) -> Unit) {
        request("/rgg-sync/chat-jury/current") { json ->
            if (json == null) return@request callback(null)
            val votes = json.optJSONObject("votes")
            val message = json.optJSONObject("message")
            callback(
                JurySummary(
                    text = message?.optString("text").orEmpty(),
                    yea = votes?.optInt("yea") ?: 0,
                    nei = votes?.optInt("nei") ?: 0,
                    verdict = json.optString("verdict")
                )
            )
        }
    }

    fun widgets(callback: (List<WidgetState>) -> Unit) {
        request("/rgg-sync/widgets") { json ->
            val widgets = json?.optJSONObject("widgets") ?: return@request callback(emptyList())
            val items = mutableListOf<WidgetState>()
            val keys = widgets.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                items += WidgetState(key, widgets.optBoolean(key))
            }
            callback(items.sortedBy { it.kind })
        }
    }

    fun setWidget(kind: String, enabled: Boolean, callback: (Boolean) -> Unit) {
        postJson("/rgg-sync/widgets/$kind", JSONObject().put("enabled", enabled)) { callback(it != null) }
    }

    fun moderation(callback: (ModerationSummary?) -> Unit) {
        request("/rgg-sync/moderation") { json ->
            val filters = json?.optJSONObject("filters") ?: return@request callback(null)
            callback(
                ModerationSummary(
                    dryRun = filters.optBoolean("dryRun"),
                    bannedWords = filters.optJSONArray("bannedWords")?.length() ?: 0,
                    blockLinks = filters.optBoolean("blockLinks"),
                    capsEnabled = filters.optBoolean("capsEnabled"),
                    logSize = json?.optJSONArray("log")?.length() ?: 0
                )
            )
        }
    }

    fun stream(callback: (StreamStatus?) -> Unit) {
        request("/rgg-sync/stream") { json ->
            if (json == null) return@request callback(null)
            callback(
                StreamStatus(
                    title = json.optString("title"),
                    gameName = json.optString("gameName"),
                    viewers = json.optInt("viewers"),
                    shieldMode = json.optBoolean("shieldMode")
                )
            )
        }
    }

    fun setShield(active: Boolean, callback: (Boolean) -> Unit) {
        postJson("/rgg-sync/stream/shield", JSONObject().put("active", active)) { callback(it != null) }
    }

    fun startCommercial(seconds: Int, callback: (Boolean) -> Unit) {
        postJson("/rgg-sync/stream/commercial", JSONObject().put("length", seconds)) { callback(it != null) }
    }

    fun commands(callback: (List<CommandSummary>) -> Unit) {
        request("/rgg-sync/commands") { json ->
            val items = mutableListOf<CommandSummary>()
            val array = json?.optJSONArray("commands")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    items += CommandSummary(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        action = item.optString("action"),
                        response = item.optString("response"),
                        enabled = item.optBoolean("enabled"),
                        aliases = mutableListOf<String>().also { aliases ->
                            val aliasArray = item.optJSONArray("aliases")
                            if (aliasArray != null) {
                                for (aliasIndex in 0 until aliasArray.length()) {
                                    aliases += aliasArray.optString(aliasIndex)
                                }
                            }
                        },
                        cooldownSeconds = item.optInt("cooldownSeconds"),
                        userLevel = item.optString("userLevel", "everyone")
                    )
                }
            }
            callback(items)
        }
    }

    fun saveCommand(name: String, action: String, response: String, cooldown: Int, callback: (Boolean) -> Unit) {
        postJson(
            "/rgg-sync/commands",
            JSONObject()
                .put("name", name)
                .put("action", action)
                .put("response", response)
                .put("cooldownSeconds", cooldown)
                .put("userLevel", "everyone")
                .put("enabled", true)
        ) { callback(it != null) }
    }

    fun saveCommand(command: CommandSummary, callback: (Boolean) -> Unit) {
        postJson(
            "/rgg-sync/commands",
            JSONObject()
                .put("id", command.id)
                .put("name", command.name)
                .put("aliases", command.aliases)
                .put("action", command.action)
                .put("response", command.response)
                .put("cooldownSeconds", command.cooldownSeconds)
                .put("userLevel", command.userLevel)
                .put("enabled", command.enabled)
        ) { callback(it != null) }
    }

    fun triggers(callback: (List<TriggerSummary>) -> Unit) {
        request("/rgg-sync/triggers") { json ->
            val items = mutableListOf<TriggerSummary>()
            val array = json?.optJSONArray("triggers")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    items += TriggerSummary(
                        action = item.optString("action"),
                        command = item.optString("command"),
                        aliases = mutableListOf<String>().also { aliases ->
                            val aliasesArray = item.optJSONArray("aliases")
                            if (aliasesArray != null) {
                                for (aliasIndex in 0 until aliasesArray.length()) {
                                    aliases += aliasesArray.optString(aliasIndex)
                                }
                            }
                        },
                        enabled = item.optBoolean("enabled"),
                        description = item.optString("description")
                    )
                }
            }
            callback(items)
        }
    }

    fun saveTriggers(items: List<TriggerSummary>, callback: (Boolean) -> Unit) {
        val array = org.json.JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("action", item.action)
                    .put("command", item.command)
                    .put("aliases", item.aliases)
                    .put("enabled", item.enabled)
                    .put("description", item.description)
            )
        }
        postJson("/rgg-sync/triggers", JSONObject().put("triggers", array)) { callback(it != null) }
    }

    fun variables(callback: (List<VariableSummary>) -> Unit) {
        request("/rgg-sync/command-variables") { json ->
            val items = mutableListOf<VariableSummary>()
            val array = json?.optJSONArray("variables")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    items += VariableSummary(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        value = item.optString("value"),
                        action = item.optString("action")
                    )
                }
            }
            callback(items)
        }
    }

    fun saveVariable(variable: VariableSummary, callback: (Boolean) -> Unit) {
        postJson(
            "/rgg-sync/command-variables",
            JSONObject()
                .put("id", variable.id)
                .put("name", variable.name)
                .put("value", variable.value)
                .put("action", variable.action)
        ) { callback(it != null) }
    }

    fun deleteVariable(id: String, callback: (Boolean) -> Unit) {
        Thread {
            val ok = runCatching {
                val request = Request.Builder().url(AuthConfig.baseUrl + "/rgg-sync/command-variables/$id").delete().build()
                client.newCall(request).execute().use { it.isSuccessful }
            }.getOrDefault(false)
            callback(ok)
        }.start()
    }

    fun deleteCommand(id: String, callback: (Boolean) -> Unit) {
        Thread {
            val ok = runCatching {
                val request = Request.Builder().url(AuthConfig.baseUrl + "/rgg-sync/commands/$id").delete().build()
                client.newCall(request).execute().use { it.isSuccessful }
            }.getOrDefault(false)
            callback(ok)
        }.start()
    }

    fun setCommandEnabled(id: String, enabled: Boolean, callback: (Boolean) -> Unit) {
        patchJson("/rgg-sync/commands/$id", JSONObject().put("enabled", enabled)) { callback(it != null) }
    }

    fun polls(callback: (PollSummary?) -> Unit) {
        request("/rgg-sync/polls") { json ->
            val current = json?.optJSONObject("current") ?: return@request callback(null)
            val options = mutableListOf<String>()
            val array = current.optJSONArray("choices")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    options += item.optString("text").ifBlank { item.optString("title") }
                }
            }
            callback(PollSummary(current.optString("title"), options, current.optBoolean("ended")))
        }
    }

    fun createPoll(title: String, choices: String, seconds: Int, callback: (Boolean) -> Unit) {
        postJson("/rgg-sync/polls", JSONObject().put("title", title).put("choices", choices.split("|").map { it.trim() }).put("durationSeconds", seconds)) { callback(it != null) }
    }

    fun roulette(callback: (RouletteSummary?) -> Unit) {
        request("/rgg-sync/roulette/state") { json ->
            val history = json?.optJSONArray("history")
            val last = history?.optJSONObject(0)
            callback(
                RouletteSummary(
                    remaining = json?.optInt("remaining") ?: 0,
                    lastTitle = last?.optJSONObject("game")?.optString("title").orEmpty(),
                    lastPlatform = last?.optJSONObject("platform")?.optString("name").orEmpty()
                )
            )
        }
    }

    fun spinRoulette(callback: (Boolean) -> Unit) {
        postJson("/rgg-sync/roulette/spin", JSONObject()) { callback(it != null) }
    }

    fun alerts(callback: (List<AlertSummary>) -> Unit) {
        request("/rgg-sync/alerts") { json ->
            val items = mutableListOf<AlertSummary>()
            val array = json?.optJSONArray("events")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    items += AlertSummary(
                        id = item.optString("id"),
                        type = item.optString("type"),
                        username = item.optString("username"),
                        message = item.optString("message"),
                        createdAt = item.optString("createdAt")
                    )
                }
            }
            callback(items)
        }
    }

    fun music(callback: (List<MusicTrack>) -> Unit) {
        request("/rgg-sync/music") { json ->
            val items = mutableListOf<MusicTrack>()
            val array = json?.optJSONArray("tracks")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    items += MusicTrack(
                        videoId = item.optString("videoId"),
                        title = item.optString("title"),
                        author = item.optString("author"),
                        durationSeconds = item.optInt("durationSeconds")
                    )
                }
            }
            callback(items)
        }
    }

    fun viewers(callback: (List<ViewerSummary>) -> Unit) {
        request("/rgg-sync/viewers") { json ->
            val items = mutableListOf<ViewerSummary>()
            val array = json?.optJSONArray("viewers")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    items += ViewerSummary(
                        login = item.optString("login"),
                        displayName = item.optString("displayName").ifBlank { item.optString("login") },
                        watchStreak = item.optInt("watchStreak")
                    )
                }
            }
            callback(items)
        }
    }

    fun points(callback: (List<PointsWallet>) -> Unit) {
        request("/rgg-sync/points") { json ->
            val items = mutableListOf<PointsWallet>()
            val array = json?.optJSONArray("top")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    items += PointsWallet(
                        login = item.optString("login"),
                        displayName = item.optString("displayName").ifBlank { item.optString("login") },
                        balance = item.optInt("balance")
                    )
                }
            }
            callback(items)
        }
    }

    fun events(callback: (List<StreamEvent>) -> Unit) {
        request("/rgg-sync/events") { json ->
            val items = mutableListOf<StreamEvent>()
            val array = json?.optJSONArray("events")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    items += StreamEvent(
                        id = item.optInt("id"),
                        title = item.optString("title"),
                        type = item.optString("type"),
                        status = item.optString("status"),
                        startsAt = item.optString("startsAt"),
                        createdAt = item.optString("createdAt")
                    )
                }
            }
            callback(items)
        }
    }

    fun createEvent(title: String, type: String, startsAt: String?, callback: (Boolean) -> Unit) {
        val body = JSONObject().put("title", title).put("type", type)
        if (!startsAt.isNullOrBlank()) body.put("startsAt", startsAt)
        postJson("/rgg-sync/events", body) { callback(it != null) }
    }

    fun updateEventStatus(id: Int, status: String, callback: (Boolean) -> Unit) {
        patchJson("/rgg-sync/events/$id", JSONObject().put("status", status)) { callback(it != null) }
    }

    fun deleteEvent(id: Int, callback: (Boolean) -> Unit) {
        Thread {
            val ok = runCatching {
                val request = Request.Builder().url(AuthConfig.baseUrl + "/rgg-sync/events/$id").delete().build()
                client.newCall(request).execute().use { it.isSuccessful }
            }.getOrDefault(false)
            callback(ok)
        }.start()
    }

    fun game(slug: String, callback: (GameDetail?) -> Unit) {
        request("/rgg-sync/catalog/games/" + URLEncoder.encode(slug, "UTF-8") + "?enrich=0") { json ->
            if (json == null) {
                callback(null)
                return@request
            }
            callback(parseGameDetail(json))
        }
    }

    fun uploadDiscussionImage(fileName: String, bytes: ByteArray, mime: String, callback: (String?) -> Unit) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, bytes.toRequestBody(mime.toMediaType()))
            .build()
        val request = Request.Builder()
            .url(AuthConfig.baseUrl + "/rgg-sync/messaging/upload")
            .post(body)
            .build()
        Thread {
            val result = runCatching {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) null
                    else JSONObject(response.body?.string().orEmpty()).optString("url").ifBlank { null }
                }
            }.getOrNull()
            callback(result)
        }.start()
    }

    fun desktopCode(callback: (String?) -> Unit) {
        postJson("/rgg-sync/desktop/code", JSONObject()) { json ->
            callback(json?.optString("code")?.takeIf { it.isNotBlank() })
        }
    }

    fun games(callback: (List<GameSummary>) -> Unit) {
        request("/rgg-sync/catalog/games?page=1&pageSize=50&facets=0") { json ->
            val items = mutableListOf<GameSummary>()
            val array = json?.optJSONArray("items")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    items += GameSummary(
                        slug = item.optString("slug"),
                        title = item.optString("title"),
                        year = item.optInt("year"),
                        cover = item.optString("cover")
                    )
                }
            }
            callback(items)
        }
    }

    fun gameDiscussion(slug: String, callback: (List<GameDiscussionMessage>) -> Unit) {
        request("/rgg-sync/games/" + URLEncoder.encode(slug, "UTF-8") + "/discussion") { json ->
            val items = mutableListOf<GameDiscussionMessage>()
            val array = json?.optJSONArray("items")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    items += parseDiscussion(item)
                }
            }
            callback(items)
        }
    }

    fun postGameDiscussion(slug: String, message: String, parentId: Long?, callback: (GameDiscussionMessage?) -> Unit) {
        val body = JSONObject().put("message", message)
        if (parentId != null) body.put("parentId", parentId)
        postJson("/rgg-sync/games/" + URLEncoder.encode(slug, "UTF-8") + "/discussion", body) { json ->
            callback(json?.let { parseDiscussion(it) })
        }
    }

    fun likeGameDiscussion(slug: String, id: Long, callback: (Pair<Boolean, Int>?) -> Unit) {
        val body = JSONObject()
        val path = "/rgg-sync/games/" + URLEncoder.encode(slug, "UTF-8") + "/discussion/" + id + "/like"
        Thread {
            val result = runCatching {
                val request = Request.Builder()
                    .url(AuthConfig.baseUrl + path)
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) null
                    else JSONObject(response.body?.string().orEmpty()).let {
                        it.optBoolean("liked") to it.optInt("likes")
                    }
                }
            }.getOrNull()
            callback(result)
        }.start()
    }

    fun deleteGameDiscussion(slug: String, id: Long, callback: (Boolean) -> Unit) {
        val path = "/rgg-sync/games/" + URLEncoder.encode(slug, "UTF-8") + "/discussion/" + id
        Thread {
            val ok = runCatching {
                val request = Request.Builder()
                    .url(AuthConfig.baseUrl + path)
                    .delete()
                    .build()
                client.newCall(request).execute().use { it.isSuccessful }
            }.getOrDefault(false)
            callback(ok)
        }.start()
    }

    private fun parseGameDetail(item: JSONObject): GameDetail {
        fun strings(name: String): List<String> {
            val result = mutableListOf<String>()
            val array = item.optJSONArray(name)
            if (array != null) {
                for (index in 0 until array.length()) array.opt(index)?.toString()?.let(result::add)
            }
            return result
        }

        val platforms = mutableListOf<GamePlatform>()
        val platformArray = item.optJSONArray("platforms")
        if (platformArray != null) {
            for (index in 0 until platformArray.length()) {
                val platform = platformArray.optJSONObject(index) ?: continue
                platforms += GamePlatform(
                    name = platform.optString("name"),
                    code = platform.optString("code")
                )
            }
        }
        val downloads = mutableListOf<GameDownload>()
        val downloadArray = item.optJSONArray("downloads")
        if (downloadArray != null) {
            for (index in 0 until downloadArray.length()) {
                val download = downloadArray.optJSONObject(index) ?: continue
                downloads += GameDownload(
                    name = download.optString("name"),
                    url = download.optString("url"),
                    note = download.optString("note")
                )
            }
        }

        return GameDetail(
            slug = item.optString("slug"),
            title = item.optString("title"),
            titleRu = item.optString("titleRu"),
            year = item.optInt("year"),
            cover = item.optString("cover"),
            description = item.optString("description"),
            descriptionRu = item.optString("descriptionRu"),
            platforms = platforms,
            genres = strings("genresRu").ifEmpty { strings("genres") },
            developers = strings("developers"),
            publishers = strings("publishers"),
            rating = item.optDouble("rating"),
            screenshots = strings("screenshots"),
            downloads = downloads,
            website = item.optString("website")
        )
    }

    private fun parseDiscussion(item: JSONObject) = GameDiscussionMessage(
        id = item.optLong("id"),
        username = item.optString("username"),
        displayName = item.optString("displayName").ifBlank { item.optString("username") },
        message = item.optString("message"),
        parentId = if (item.has("parentId") && !item.isNull("parentId")) item.optLong("parentId") else null,
        likes = item.optInt("likes"),
        liked = item.optBoolean("liked"),
        createdAt = item.optString("createdAt")
    )

    fun analytics(callback: (AnalyticsSummary?) -> Unit) {
        request("/rgg-sync/analytics") { json ->
            val session = json?.optJSONObject("current") ?: json?.optJSONArray("history")?.optJSONObject(0)
            if (session == null) {
                callback(null)
                return@request
            }
            callback(
                AnalyticsSummary(
                    live = json?.optJSONObject("current") != null,
                    peakViewers = session.optInt("peakViewers"),
                    avgViewers = session.optInt("avgViewers"),
                    chatters = session.optInt("chatters"),
                    messages = session.optInt("messages")
                )
            )
        }
    }

    fun chatRecent(callback: (List<ChatMessage>) -> Unit) {
        request("/rgg-sync/chat/recent?limit=50") { json ->
            val messages = mutableListOf<ChatMessage>()
            val array = json?.optJSONArray("messages")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val author = item.optJSONObject("author")
                    messages += ChatMessage(
                        author = author?.optString("display_name").orEmpty()
                            .ifBlank { author?.optString("login").orEmpty() },
                        text = item.optString("text"),
                        color = item.optString("color").takeIf { it.startsWith("#") }
                    )
                }
            }
            callback(messages)
        }
    }

    fun sendChat(text: String, channel: String, callback: (Boolean) -> Unit) {
        val body = JSONObject().put("text", text).put("channel", channel).toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(AuthConfig.baseUrl + "/rgg-sync/chat/send")
            .post(body)
            .build()
        Thread {
            val ok = runCatching {
                client.newCall(request).execute().use { it.isSuccessful }
            }.getOrDefault(false)
            callback(ok)
        }.start()
    }

    private fun patchJson(path: String, body: JSONObject, callback: (JSONObject?) -> Unit) {
        Thread {
            val result = runCatching {
                val request = Request.Builder()
                    .url(AuthConfig.baseUrl + path)
                    .patch(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) null else JSONObject(response.body?.string().orEmpty())
                }
            }.getOrNull()
            callback(result)
        }.start()
    }

    private fun postJson(path: String, body: JSONObject, callback: (JSONObject?) -> Unit) {
        Thread {
            val result = runCatching {
                val request = Request.Builder()
                    .url(AuthConfig.baseUrl + path)
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) null else JSONObject(response.body?.string().orEmpty())
                }
            }.getOrNull()
            callback(result)
        }.start()
    }

    private fun request(path: String, callback: (JSONObject?) -> Unit) {
        Thread {
            val result = runCatching {
                val request = Request.Builder().url(AuthConfig.baseUrl + path).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) null else JSONObject(response.body?.string().orEmpty())
                }
            }.getOrNull()
            callback(result)
        }.start()
    }
}
