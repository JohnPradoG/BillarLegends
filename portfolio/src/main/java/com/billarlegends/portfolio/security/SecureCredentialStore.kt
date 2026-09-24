package com.billarlegends.portfolio.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Guarda API keys/secrets y tokens fuera de la base de datos, cifrados con una clave
 * generada y resguardada en el Android Keystore (nunca sale del hardware/OS).
 *
 * IMPORTANTE: solo se deben guardar aquí credenciales de SOLO LECTURA (sin permiso de
 * retiro ni de trading). La app nunca debe pedir ni almacenar claves con esos permisos.
 */
class SecureCredentialStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "portfolio_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveBinanceCredentials(accountId: Long, apiKey: String, apiSecret: String) {
        prefs.edit()
            .putString(binanceKeyAlias(accountId), apiKey)
            .putString(binanceSecretAlias(accountId), apiSecret)
            .apply()
    }

    fun getBinanceApiKey(accountId: Long): String? = prefs.getString(binanceKeyAlias(accountId), null)

    fun getBinanceApiSecret(accountId: Long): String? = prefs.getString(binanceSecretAlias(accountId), null)

    fun deleteBinanceCredentials(accountId: Long) {
        prefs.edit()
            .remove(binanceKeyAlias(accountId))
            .remove(binanceSecretAlias(accountId))
            .apply()
    }

    fun saveExnessToken(connectionId: Long, token: String) {
        prefs.edit().putString(exnessTokenAlias(connectionId), token).apply()
    }

    fun getExnessToken(connectionId: Long): String? = prefs.getString(exnessTokenAlias(connectionId), null)

    fun deleteExnessToken(connectionId: Long) {
        prefs.edit().remove(exnessTokenAlias(connectionId)).apply()
    }

    private fun binanceKeyAlias(accountId: Long) = "binance_${accountId}_api_key"
    private fun binanceSecretAlias(accountId: Long) = "binance_${accountId}_api_secret"
    private fun exnessTokenAlias(connectionId: Long) = "exness_${connectionId}_token"
}
