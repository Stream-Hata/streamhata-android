package dev.rdpctd.streamhata.core

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
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

data class AnalyticsSummary(
    val live: Boolean,
    val peakViewers: Int,
    val avgViewers: Int,
    val chatters: Int,
    val messages: Int
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
            .url(AuthConfig.BASE_URL + "/rgg-sync/chat/send")
            .post(body)
            .build()
        Thread {
            val ok = runCatching {
                client.newCall(request).execute().use { it.isSuccessful }
            }.getOrDefault(false)
            callback(ok)
        }.start()
    }

    private fun request(path: String, callback: (JSONObject?) -> Unit) {
        Thread {
            val result = runCatching {
                val request = Request.Builder().url(AuthConfig.BASE_URL + path).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) null else JSONObject(response.body?.string().orEmpty())
                }
            }.getOrNull()
            callback(result)
        }.start()
    }
}
