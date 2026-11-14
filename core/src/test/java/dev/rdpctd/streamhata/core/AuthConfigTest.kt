package dev.rdpctd.streamhata.core

import org.junit.Assert.assertTrue
import org.junit.Test

class AuthConfigTest {
    @Test
    fun selectsHttpsBaseUrl() {
        AuthConfig.selectBase()

        assertTrue(AuthConfig.baseUrl.startsWith("https://"))
    }
}
