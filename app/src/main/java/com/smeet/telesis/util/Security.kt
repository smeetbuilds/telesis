package com.smeet.telesis.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object Security {
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    const val DEFAULT_PIN_ITERATIONS: Int = 120_000

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun salt(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun randomBytesBase64(size: Int = 24): String {
        val bytes = ByteArray(size)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun pbkdf2Base64(pin: String, saltBase64: String, iterations: Int = DEFAULT_PIN_ITERATIONS): String {
        require(iterations >= 10_000) { "PBKDF2 iteration count is too low" }
        val saltBytes = Base64.decode(saltBase64, Base64.NO_WRAP)
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, iterations, 256)
        val bytes = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun constantTimeEquals(a: String, b: String): Boolean {
        val left = a.toByteArray(Charsets.UTF_8)
        val right = b.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(left, right)
    }
}
