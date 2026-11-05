package dev.rdpctd.streamhata.core

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPrivateKeySpec
import java.security.spec.ECPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private fun b64urlEncode(bytes: ByteArray): String =
    Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

private fun b64urlDecode(value: String): ByteArray =
    Base64.decode(value, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

private fun fixedBytes(value: BigInteger, size: Int): ByteArray {
    val raw = value.toByteArray()
    val unsigned = if (raw.size > size) raw.copyOfRange(raw.size - size, raw.size) else raw
    return ByteArray(size).also { unsigned.copyInto(it, size - unsigned.size) }
}

private const val SALT = "streamhata-messaging-v1"

data class PublicBundle(
    val keyId: String,
    val encryptionKey: JSONObject,
    val signingKey: JSONObject
) {
    fun toJson(): JSONObject = JSONObject()
        .put("algorithm", "ECDH-P256+ECDSA-P256")
        .put("keyId", keyId)
        .put("encryptionKey", encryptionKey)
        .put("signingKey", signingKey)

    companion object {
        fun from(json: JSONObject): PublicBundle = PublicBundle(
            keyId = json.optString("keyId"),
            encryptionKey = json.getJSONObject("encryptionKey"),
            signingKey = json.getJSONObject("signingKey")
        )
    }
}

class MessagingIdentity(
    val d: ByteArray,
    val x: ByteArray,
    val y: ByteArray
) {
    val keyId: String by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(x)
        digest.update(y)
        b64urlEncode(digest.digest())
    }

    val encryptionKey: JSONObject get() = jwk(x, y)
    val signingKey: JSONObject get() = jwk(x, y)
    val bundle: PublicBundle get() = PublicBundle(keyId, encryptionKey, signingKey)

    fun toJson(): JSONObject = JSONObject()
        .put("d", b64urlEncode(d))
        .put("x", b64urlEncode(x))
        .put("y", b64urlEncode(y))

    companion object {
        fun from(json: JSONObject): MessagingIdentity = MessagingIdentity(
            d = b64urlDecode(json.getString("d")),
            x = b64urlDecode(json.getString("x")),
            y = b64urlDecode(json.getString("y"))
        )

        fun generate(): MessagingIdentity {
            val generator = KeyPairGenerator.getInstance("EC")
            generator.initialize(ECGenParameterSpec("secp256r1"))
            val pair = generator.generateKeyPair()
            val public = pair.public as java.security.interfaces.ECPublicKey
            val private = pair.private as java.security.interfaces.ECPrivateKey
            val x = fixedBytes(public.w.affineX, 32)
            val y = fixedBytes(public.w.affineY, 32)

            return MessagingIdentity(fixedBytes(private.s, 32), x, y)
        }

        private fun jwk(x: ByteArray, y: ByteArray): JSONObject = JSONObject()
            .put("kty", "EC")
            .put("crv", "P-256")
            .put("x", b64urlEncode(x))
            .put("y", b64urlEncode(y))
    }
}

class MessagingIdentityStore(context: Context) {
    private val store = TokenStore(context)

    fun loadOrCreate(): MessagingIdentity {
        val raw = store.loadValue("messaging_identity")
        if (raw != null) {
            runCatching { return MessagingIdentity.from(JSONObject(raw)) }
        }
        val identity = MessagingIdentity.generate()
        store.saveValue("messaging_identity", identity.toJson().toString())
        return identity
    }

    fun clear() = store.clearValue("messaging_identity")
}

object MessagingCrypto {
    fun encrypt(
        identity: MessagingIdentity,
        sender: String,
        recipient: String,
        recipientBundle: PublicBundle,
        text: String,
        encrypted: Boolean = true
    ): JSONObject {
        if (!encrypted) {
            val fields = linkedMapOf(
                "algorithm" to "PLAINTEXT",
                "body" to text,
                "keyId" to identity.keyId,
                "recipient" to recipient,
                "sender" to sender,
                "version" to "1"
            )
            return JSONObject(stable(fields)).put("signature", sign(identity, fields))
        }

        val secret = deriveSecret(identity.d, recipientBundle.encryptionKey)
        val key = SecretKeySpec(hkdf(secret, sender, recipient), "AES")
        val nonce = ByteArray(12).also { java.security.SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))
        val ciphertext = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
        val fields = linkedMapOf(
            "algorithm" to "ECDH-P256+HKDF-SHA256+AES-256-GCM",
            "ciphertext" to Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            "keyId" to identity.keyId,
            "nonce" to Base64.encodeToString(nonce, Base64.NO_WRAP),
            "recipient" to recipient,
            "sender" to sender,
            "version" to "1"
        )

        return JSONObject(stable(fields)).put("signature", sign(identity, fields))
    }

    fun decrypt(
        identity: MessagingIdentity,
        senderBundle: PublicBundle,
        envelope: JSONObject,
        recipientBundle: PublicBundle? = null
    ): String {
        val fields = linkedMapOf(
            "algorithm" to envelope.getString("algorithm"),
            "keyId" to envelope.getString("keyId"),
            "recipient" to envelope.getString("recipient"),
            "sender" to envelope.getString("sender"),
            "version" to envelope.getString("version")
        )
        if (envelope.getString("algorithm") == "PLAINTEXT") {
            fields["body"] = envelope.getString("body")
        } else {
            fields["ciphertext"] = envelope.getString("ciphertext")
            fields["nonce"] = envelope.getString("nonce")
        }
        require(verify(senderBundle, envelope.getString("signature"), fields)) { "invalid signature" }

        if (envelope.getString("algorithm") == "PLAINTEXT") {
            return envelope.getString("body")
        }
        val publicJson = (recipientBundle ?: senderBundle).encryptionKey
        val secret = deriveSecret(identity.d, publicJson)
        val key = SecretKeySpec(hkdf(secret, envelope.getString("sender"), envelope.getString("recipient")), "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(128, Base64.decode(envelope.getString("nonce"), Base64.NO_WRAP))
        )

        return String(cipher.doFinal(Base64.decode(envelope.getString("ciphertext"), Base64.NO_WRAP)), Charsets.UTF_8)
    }

    private fun deriveSecret(d: ByteArray, publicJwk: JSONObject): ByteArray {
        val params = params()
        val privateKey = KeyFactory.getInstance("EC").generatePrivate(ECPrivateKeySpec(BigInteger(1, d), params))
        val point = ECPoint(
            BigInteger(1, b64urlDecode(publicJwk.getString("x"))),
            BigInteger(1, b64urlDecode(publicJwk.getString("y")))
        )
        val publicKey = KeyFactory.getInstance("EC").generatePublic(ECPublicKeySpec(point, params))
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(privateKey)
        agreement.doPhase(publicKey, true)

        return agreement.generateSecret()
    }

    private fun hkdf(secret: ByteArray, sender: String, recipient: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(SALT.toByteArray(), "HmacSHA256"))
        val prk = mac.doFinal(secret)
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        mac.update("$sender\u0000$recipient".toByteArray())
        mac.update(1)

        return mac.doFinal().copyOf(32)
    }

    private fun sign(identity: MessagingIdentity, fields: Map<String, String>): String {
        val params = params()
        val privateKey = KeyFactory.getInstance("EC").generatePrivate(ECPrivateKeySpec(BigInteger(1, identity.d), params))
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(stable(fields).toByteArray())
        val der = signature.sign()
        val (r, s) = derToRaw(der)

        return Base64.encodeToString(r + s, Base64.NO_WRAP)
    }

    private fun verify(bundle: PublicBundle, signatureValue: String, fields: Map<String, String>): Boolean = runCatching {
        val raw = Base64.decode(signatureValue, Base64.NO_WRAP)
        require(raw.size == 64)
        val point = ECPoint(
            BigInteger(1, b64urlDecode(bundle.signingKey.getString("x"))),
            BigInteger(1, b64urlDecode(bundle.signingKey.getString("y")))
        )
        val publicKey = KeyFactory.getInstance("EC").generatePublic(ECPublicKeySpec(point, params()))
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initVerify(publicKey)
        signature.update(stable(fields).toByteArray())

        signature.verify(rawToDer(raw))
    }.getOrDefault(false)

    private fun params(): ECParameterSpec {
        val parameters = AlgorithmParameters.getInstance("EC")
        parameters.init(ECGenParameterSpec("secp256r1"))

        return parameters.getParameterSpec(ECParameterSpec::class.java)
    }

    private fun stable(fields: Map<String, String>): String =
        fields.toSortedMap().entries.joinToString(",", "{", "}") { "${quote(it.key)}:${quote(it.value)}" }

    private fun quote(value: String): String {
        val builder = StringBuilder("\"")
        for (char in value) {
            when (char) {
                '"' -> builder.append("\\\"")
                '\\' -> builder.append("\\\\")
                '\b' -> builder.append("\\b")
                '\u000C' -> builder.append("\\f")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> if (char < ' ') builder.append("\\u%04x".format(char.code)) else builder.append(char)
            }
        }

        return builder.append('"').toString()
    }

    private fun derToRaw(der: ByteArray): Pair<ByteArray, ByteArray> {
        var index = 0
        require(der[index++] == 0x30.toByte())
        if (der[index].toInt() and 0x80 != 0) index += der[index].toInt() and 0x7f
        index++
        require(der[index++] == 0x02.toByte())
        val rLength = der[index++].toInt()
        val r = fixed(BigInteger(1, der.copyOfRange(index, index + rLength)), 32)
        index += rLength
        require(der[index++] == 0x02.toByte())
        val sLength = der[index++].toInt()
        val s = fixed(BigInteger(1, der.copyOfRange(index, index + sLength)), 32)

        return r to s
    }

    private fun rawToDer(raw: ByteArray): ByteArray {
        val r = derInteger(raw.copyOfRange(0, 32))
        val s = derInteger(raw.copyOfRange(32, 64))
        val body = r + s
        val length = if (body.size < 128) byteArrayOf(body.size.toByte()) else byteArrayOf(0x81.toByte(), body.size.toByte())

        return byteArrayOf(0x30) + length + body
    }

    private fun derInteger(value: ByteArray): ByteArray {
        val trimmedRaw = value.dropWhile { it == 0.toByte() }.toByteArray()
        val trimmed = if (trimmedRaw.isEmpty()) byteArrayOf(0) else trimmedRaw
        val content = if (trimmed[0].toInt() and 0x80 != 0) byteArrayOf(0) + trimmed else trimmed
        val length = if (content.size < 128) byteArrayOf(content.size.toByte()) else byteArrayOf(0x81.toByte(), content.size.toByte())

        return byteArrayOf(0x02) + length + content
    }

    private fun fixed(value: BigInteger, size: Int): ByteArray {
        val raw = value.toByteArray()
        val unsigned = if (raw.size > size) raw.copyOfRange(raw.size - size, raw.size) else raw
        return ByteArray(size).also { unsigned.copyInto(it, size - unsigned.size) }
    }

    private fun b64url(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private fun b64urlDecode(value: String): ByteArray =
        Base64.decode(value, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}
