package com.smeet.telesis.core

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.smeet.telesis.util.Security

class AppLockManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = createPrivatePrefs(appContext)

    init {
        moveOldPrefsIfPresent(appContext)
    }

    fun isPinEnabled(): Boolean = prefs.getString(KEY_HASH, null) != null
    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC, false) && isPinEnabled() && canUseBiometric()
    fun isTemporarilyBlocked(now: Long = System.currentTimeMillis()): Boolean = prefs.getLong(KEY_BLOCKED_UNTIL, 0L) > now

    fun blockRemainingSeconds(now: Long = System.currentTimeMillis()): Long {
        val until = prefs.getLong(KEY_BLOCKED_UNTIL, 0L)
        return ((until - now).coerceAtLeast(0L) + 999L) / 1000L
    }

    fun setPin(pin: String): Boolean {
        if (!pin.matches(Regex("^[0-9]{4,8}$"))) return false
        val salt = Security.randomBytesBase64()
        val iterations = Security.DEFAULT_PIN_ITERATIONS
        val hash = Security.pbkdf2Base64(pin, salt, iterations)
        prefs.edit()
            .putString(KEY_SALT, salt)
            .putString(KEY_HASH, hash)
            .putInt(KEY_ITERATIONS, iterations)
            .putString(KEY_ALGORITHM, ALGORITHM_PBKDF2)
            .putInt(KEY_BAD_TRIES, 0)
            .putLong(KEY_BLOCKED_UNTIL, 0L)
            .apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        if (isTemporarilyBlocked()) return false
        val salt = prefs.getString(KEY_SALT, null)
        val hash = prefs.getString(KEY_HASH, null)
        if (salt == null || hash == null) {
            noteBadTry()
            return false
        }
        val algorithm = prefs.getString(KEY_ALGORITHM, ALGORITHM_LEGACY_SHA256)
        val ok = when (algorithm) {
            ALGORITHM_PBKDF2 -> {
                val iterations = prefs.getInt(KEY_ITERATIONS, Security.DEFAULT_PIN_ITERATIONS)
                Security.constantTimeEquals(Security.pbkdf2Base64(pin, salt, iterations), hash)
            }
            else -> Security.constantTimeEquals(Security.sha256(salt + pin), hash)
        }
        if (ok) {
            prefs.edit().putInt(KEY_BAD_TRIES, 0).putLong(KEY_BLOCKED_UNTIL, 0L).apply()
            if (algorithm != ALGORITHM_PBKDF2) setPin(pin)
        } else {
            noteBadTry()
        }
        return ok
    }

    fun setBiometricEnabled(enabled: Boolean): Boolean {
        if (enabled && !isPinEnabled()) return false
        if (enabled && !canUseBiometric()) return false
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
        return true
    }

    fun canUseBiometric(): Boolean {
        val flags = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return BiometricManager.from(appContext).canAuthenticate(flags) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun disablePin() {
        prefs.edit().clear().apply()
        appContext.getSharedPreferences(OLD_PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun noteBadTry(now: Long = System.currentTimeMillis()) {
        val tries = prefs.getInt(KEY_BAD_TRIES, 0) + 1
        val blockedUntil = if (tries >= MAX_BAD_TRIES) now + BLOCK_MS else 0L
        prefs.edit()
            .putInt(KEY_BAD_TRIES, if (blockedUntil > 0L) 0 else tries)
            .putLong(KEY_BLOCKED_UNTIL, blockedUntil)
            .apply()
    }

    private fun createPrivatePrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun moveOldPrefsIfPresent(context: Context) {
        if (prefs.getString(KEY_HASH, null) != null) return
        val oldPrefs = context.getSharedPreferences(OLD_PREFS_NAME, Context.MODE_PRIVATE)
        val oldHash = oldPrefs.getString(KEY_HASH, null) ?: return
        prefs.edit()
            .putString(KEY_HASH, oldHash)
            .putString(KEY_SALT, oldPrefs.getString(KEY_SALT, null))
            .putString(KEY_ALGORITHM, oldPrefs.getString(KEY_ALGORITHM, ALGORITHM_LEGACY_SHA256))
            .putInt(KEY_ITERATIONS, oldPrefs.getInt(KEY_ITERATIONS, Security.DEFAULT_PIN_ITERATIONS))
            .putBoolean(KEY_BIOMETRIC, oldPrefs.getBoolean(KEY_BIOMETRIC, false))
            .putInt(KEY_BAD_TRIES, 0)
            .putLong(KEY_BLOCKED_UNTIL, 0L)
            .apply()
        oldPrefs.edit().clear().apply()
    }

    companion object {
        private const val SECURE_PREFS_NAME = "telesis_lock_private"
        private const val OLD_PREFS_NAME = "telesis_lock"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_HASH = "pin_hash"
        private const val KEY_ITERATIONS = "pin_iterations"
        private const val KEY_ALGORITHM = "pin_algorithm"
        private const val KEY_BIOMETRIC = "biometric_enabled"
        private const val KEY_BAD_TRIES = "bad_tries"
        private const val KEY_BLOCKED_UNTIL = "blocked_until"
        private const val ALGORITHM_PBKDF2 = "PBKDF2WithHmacSHA256"
        private const val ALGORITHM_LEGACY_SHA256 = "legacy_sha256"
        private const val MAX_BAD_TRIES = 5
        private const val BLOCK_MS = 5 * 60 * 1000L
    }
}
