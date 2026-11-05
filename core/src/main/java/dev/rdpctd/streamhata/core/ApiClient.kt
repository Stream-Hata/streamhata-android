package dev.rdpctd.streamhata.core

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class TwitchProfile(
    val login: String,
    val displayName: String,
    val avatar: String
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
