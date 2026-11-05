package dev.rdpctd.streamhata.core

object AuthConfig {
    const val BASE_URL = "https://streamhata.rdpctd.dev"

    fun authUrl(scheme: String): String = "$BASE_URL/desktop-auth?scheme=$scheme"
    fun exchangeUrl(): String = "$BASE_URL/rgg-sync/desktop/exchange"
}
