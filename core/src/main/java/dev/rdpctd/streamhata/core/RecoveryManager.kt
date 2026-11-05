package dev.rdpctd.streamhata.core

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class RecoveryManager(context: Context) {
    private val words: List<String> = context.assets.open("bip39-english.txt").bufferedReader().useLines { lines ->
        lines.toList()
    }

    fun generateMnemonic(): String {
        val entropy = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val checksum = MessageDigest.getInstance("SHA-256").digest(entropy)[0].toInt() and 0xff
        val bits = StringBuilder()
        entropy.forEach { bits.append(String.format("%8s", Integer.toBinaryString(it.toInt() and 0xff)).replace(' ', '0')) }
        bits.append(String.format("%8s", Integer.toBinaryString(checksum)).replace(' ', '0'))

        return (0 until 24).joinToString(" ") { index ->
            words[Integer.parseInt(bits.substring(index * 11, index * 11 + 11), 2)]
        }
    }

    fun validate(mnemonic: String): Boolean = runCatching {
        val parts = mnemonic.trim().lowercase().split(Regex("\\s+"))
        if (parts.size != 24) return false
        val indexes = parts.map { words.indexOf(it).also { index -> require(index >= 0) } }
        val bits = indexes.joinToString("") { String.format("%11s", Integer.toBinaryString(it)).replace(' ', '0') }
        val entropy = ByteArray(32)
        for (i in 0 until 32) entropy[i] = Integer.parseInt(bits.substring(i * 8, i * 8 + 8), 2).toByte()
        val expected = MessageDigest.getInstance("SHA-256").digest(entropy)[0].toInt() and 0xff
        val actual = Integer.parseInt(bits.substring(256, 264), 2)

        expected == actual
    }.getOrDefault(false)

    fun encrypt(identity: MessagingIdentity, mnemonic: String): JSONObject {
        val key = recoveryKey(mnemonic)
        val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        val ciphertext = cipher.doFinal(identity.toJson().toString().toByteArray())

        return JSONObject()
            .put("version", 1)
            .put("iv", Base64.encodeToString(nonce, Base64.NO_WRAP))
            .put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
    }

    fun decrypt(blob: JSONObject, mnemonic: String): MessagingIdentity {
        require(validate(mnemonic)) { "invalid mnemonic" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(recoveryKey(mnemonic), "AES"),
            GCMParameterSpec(128, Base64.decode(blob.getString("iv"), Base64.NO_WRAP))
        )
        val plain = cipher.doFinal(Base64.decode(blob.getString("ciphertext"), Base64.NO_WRAP))

        return MessagingIdentity.from(JSONObject(String(plain)))
    }

    private fun recoveryKey(mnemonic: String): ByteArray {
        val normalized = mnemonic.trim().lowercase().split(Regex("\\s+")).joinToString(" ")
        val spec = PBEKeySpec(normalized.toCharArray(), "mnemonic".toByteArray(), 2048, 512)
        val seed = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512").generateSecret(spec).encoded
        val extract = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec("streamhata-recovery-v1".toByteArray(), "HmacSHA256"))
        }.doFinal(seed)
        val expand = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(extract, "HmacSHA256")) }
        expand.update("identity".toByteArray())
        expand.update(1)

        return expand.doFinal().copyOf(32)
    }
}
