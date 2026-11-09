package dev.rdpctd.streamhata.core

import java.util.Locale
import java.util.TimeZone

object AuthConfig {
    private const val GLOBAL_BASE_URL = "https://streamhata.rdpctd.dev"
    private const val RU_BASE_URL = "https://ru.streamhata.rdpctd.dev"

    @Volatile
    var baseUrl: String = GLOBAL_BASE_URL
        private set

    fun selectBase() {
        val locale = Locale.getDefault()
        val timezone = TimeZone.getDefault().id
        baseUrl = if (locale.language == "ru" || timezone.startsWith("Europe/Moscow")) {
            RU_BASE_URL
        } else {
            GLOBAL_BASE_URL
        }
    }

    fun authUrl(scheme: String): String = "$baseUrl/desktop-auth?scheme=$scheme"
    fun exchangeUrl(): String = "$baseUrl/rgg-sync/desktop/exchange"
}
