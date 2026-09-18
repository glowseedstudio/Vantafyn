package dev.vantafyn.core.media

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts Subsonic credentials at rest using Android Keystore + AES-256-GCM.
 * Falls back to plaintext SharedPreferences for migration (reads old values and re-encrypts).
 */
object SecureSubsonicStorage {
    private const val PREFS_NAME = "vantafyn_subsonic_prefs"
    private const val KEY_ALIAS = "vantafyn_subsonic_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private const val KEY_SERVER_URL = "subsonic_server_url"
    private const val KEY_USERNAME_PLAIN = "subsonic_username"
    private const val KEY_PASSWORD_PLAIN = "subsonic_password"
    private const val KEY_USERNAME_IV = "subsonic_username_iv"
    private const val KEY_USERNAME_ENC = "subsonic_username_enc"
    private const val KEY_PASSWORD_IV = "subsonic_password_iv"
    private const val KEY_PASSWORD_ENC = "subsonic_password_enc"
    private const val KEY_MIGRATED = "subsonic_migrated"

    data class SubsonicCredentials(
        val serverUrl: String,
        val username: String,
        val password: String,
    )

    fun save(context: Context, serverUrl: String, username: String, password: String) {
        val key = getOrCreateKey()
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Encrypt username
        val userCipher = Cipher.getInstance(TRANSFORMATION)
        userCipher.init(Cipher.ENCRYPT_MODE, key)
        val userEncrypted = userCipher.doFinal(username.toByteArray(Charsets.UTF_8))

        // Encrypt password
        val passCipher = Cipher.getInstance(TRANSFORMATION)
        passCipher.init(Cipher.ENCRYPT_MODE, key)
        val passEncrypted = passCipher.doFinal(password.toByteArray(Charsets.UTF_8))

        prefs.edit()
            .putString(KEY_SERVER_URL, serverUrl)
            .putString(KEY_USERNAME_IV, Base64.encodeToString(userCipher.iv, Base64.NO_WRAP))
            .putString(KEY_USERNAME_ENC, Base64.encodeToString(userEncrypted, Base64.NO_WRAP))
            .putString(KEY_PASSWORD_IV, Base64.encodeToString(passCipher.iv, Base64.NO_WRAP))
            .putString(KEY_PASSWORD_ENC, Base64.encodeToString(passEncrypted, Base64.NO_WRAP))
            .putBoolean(KEY_MIGRATED, true)
            .remove(KEY_USERNAME_PLAIN)
            .remove(KEY_PASSWORD_PLAIN)
            .apply()
    }

    fun read(context: Context): SubsonicCredentials? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val serverUrl = prefs.getString(KEY_SERVER_URL, null) ?: return null

        // Try encrypted first
        val userIv = prefs.getString(KEY_USERNAME_IV, null)
        val userEnc = prefs.getString(KEY_USERNAME_ENC, null)
        val passIv = prefs.getString(KEY_PASSWORD_IV, null)
        val passEnc = prefs.getString(KEY_PASSWORD_ENC, null)

        if (userIv != null && userEnc != null && passIv != null && passEnc != null) {
            return try {
                val key = getOrCreateKey()

                val userCipher = Cipher.getInstance(TRANSFORMATION)
                userCipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, Base64.decode(userIv, Base64.NO_WRAP)))
                val username = String(userCipher.doFinal(Base64.decode(userEnc, Base64.NO_WRAP)), Charsets.UTF_8)

                val passCipher = Cipher.getInstance(TRANSFORMATION)
                passCipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, Base64.decode(passIv, Base64.NO_WRAP)))
                val password = String(passCipher.doFinal(Base64.decode(passEnc, Base64.NO_WRAP)), Charsets.UTF_8)

                SubsonicCredentials(serverUrl, username, password)
            } catch (_: Exception) {
                null
            }
        }

        // Fallback: read plaintext (pre-migration) and re-encrypt
        val username = prefs.getString(KEY_USERNAME_PLAIN, null) ?: return null
        val password = prefs.getString(KEY_PASSWORD_PLAIN, null) ?: return null
        save(context, serverUrl, username, password)
        return SubsonicCredentials(serverUrl, username, password)
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }
}
