package dev.rdpctd.streamhata.core

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AuthManager(private val activity: Activity, private val scheme: String) {
    private val store = TokenStore(activity)
    private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).build()

    fun isLoggedIn(): Boolean = store.hasToken()

    fun openLogin() {
        val url = AuthConfig.authUrl(scheme)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val candidates = activity.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val browser = candidates.firstOrNull { candidate ->
            val packageName = candidate.activityInfo.packageName.lowercase()
            !packageName.contains("webview") && packageName != "org.chromium.webview_shell"
        }

        if (browser == null) {
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("StreamHata login", url))
            Toast.makeText(activity, "Установи Chrome/Firefox. Ссылка для входа скопирована в буфер.", Toast.LENGTH_LONG).show()

            return
        }

        intent.setPackage(browser.activityInfo.packageName)
        activity.startActivity(intent)
    }

    fun handleIntent(data: Uri?, callback: (Boolean) -> Unit): Boolean {
        if (data == null || data.scheme != scheme || data.host != "auth") return false
        val code = data.getQueryParameter("code") ?: return false
        exchange(code, callback)
        return true
    }

    fun token(): String? = store.load()

    fun secureStore(): TokenStore = store

    fun logout() = store.clear()

    private fun exchange(code: String, callback: (Boolean) -> Unit) {
        Thread {
            val ok = runCatching {
                val body = JSONObject().put("code", code).toString()
                    .toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(AuthConfig.exchangeUrl()).post(body).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use false
                    val json = JSONObject(response.body?.string().orEmpty())
                    val token = json.optString("refreshToken")
                    if (token.isBlank()) false else { store.save(token); true }
                }
            }.getOrDefault(false)
            activity.runOnUiThread { callback(ok) }
        }.start()
    }
}
