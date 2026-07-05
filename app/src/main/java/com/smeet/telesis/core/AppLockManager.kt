package com.smeet.telesis.core

import android.content.Context
import androidx.biometric.BiometricManager
import com.smeet.telesis.util.Security

class AppLockManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("telesis_lock", Context.MODE_PRIVATE)

    fun isPinEnabled(): Boolean = prefs.getString(KEY_HASH, null) != null
    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC, false) && isPinEnabled() && canUseBiometric()

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
            .apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null) ?: return false
        val hash = prefs.getString(KEY_HASH, null) ?: return false
        val algorithm = prefs.getString(KEY_ALGORITHM, ALGORITHM_LEGACY_SHA256)
        val ok = when (algorithm) {
            ALGORITHM_PBKDF2 -> {
                val iterations = prefs.getInt(KEY_ITERATIONS, Security.DEFAULT_PIN_ITERATIONS)
                Security.constantTimeEquals(Security.pbkdf2Base64(pin, salt, iterations), hash)
            }
            else -> Security.constantTimeEquals(Security.sha256(salt + pin), hash)
        }
        if (ok && algorithm != ALGORITHM_PBKDF2) setPin(pin)
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
    }

    companion object {
        private const val KEY_SALT = "pin_salt"
        private const val KEY_HASH = "pin_hash"
        private const val KEY_ITERATIONS = "pin_iterations"
        private const val KEY_ALGORITHM = "pin_algorithm"
        private const val KEY_BIOMETRIC = "biometric_enabled"
        private const val ALGORITHM_PBKDF2 = "PBKDF2WithHmacSHA256"
        private const val ALGORITHM_LEGACY_SHA256 = "legacy_sha256"
    }
}
