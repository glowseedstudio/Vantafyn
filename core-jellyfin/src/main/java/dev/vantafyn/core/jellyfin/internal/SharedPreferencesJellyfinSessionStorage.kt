package dev.vantafyn.core.jellyfin

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SharedPreferencesJellyfinSessionStorage(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinSessionStorage {
    private val preferences = context.applicationContext.getSharedPreferences(
        "vantafyn_jellyfin_session",
        Context.MODE_PRIVATE,
    )
    private val secrets = JellyfinEncryptedSessionSecrets(context.applicationContext)

    override suspend fun read(): StoredJellyfinSession? =
        withContext(ioDispatcher) {
            val profiles = readAllInternal()
            val lastProfileId = preferences.getString(KEY_LAST_PROFILE_ID, null)
            profiles.firstOrNull { it.profileId == lastProfileId } ?: profiles.maxByOrNull { it.lastUsedAt }
        }

    override suspend fun read(profileId: String): StoredJellyfinSession? =
        withContext(ioDispatcher) {
            readAllInternal().firstOrNull { it.profileId == profileId }
        }

    override suspend fun readAll(): List<StoredJellyfinSession> =
        withContext(ioDispatcher) {
            readAllInternal()
        }

    override suspend fun write(session: StoredJellyfinSession) {
        withContext(ioDispatcher) {
            val ids = preferences.getStringSet(KEY_PROFILE_IDS, emptySet()).orEmpty().toMutableSet()
            ids += session.profileId
            preferences
                .edit()
                .putStringSet(KEY_PROFILE_IDS, ids)
                .putString(KEY_LAST_PROFILE_ID, session.profileId)
                .putString(key(session.profileId, KEY_SERVER_URL), session.serverUrl)
                .putString(key(session.profileId, KEY_LOCAL_SERVER_URL), session.localServerUrl)
                .putString(key(session.profileId, KEY_REMOTE_SERVER_URL), session.remoteServerUrl)
                .putString(key(session.profileId, KEY_SERVER_NAME), session.serverName)
                .putString(key(session.profileId, KEY_SERVER_VERSION), session.serverVersion)
                .putString(key(session.profileId, KEY_SERVER_ID), session.serverId)
                .putString(key(session.profileId, KEY_USER_ID), session.userId.toString())
                .putString(key(session.profileId, KEY_USER_NAME), session.userName)
                .putString(key(session.profileId, KEY_USER_IMAGE_TAG), session.userImageTag)
                .remove(key(session.profileId, KEY_ACCESS_TOKEN))
                .putLong(key(session.profileId, KEY_LAST_USED_AT), session.lastUsedAt)
                .apply()
            secrets.saveAccessToken(session.profileId, session.accessToken)
        }
    }

    override suspend fun remove(profileId: String) {
        withContext(ioDispatcher) {
            val ids = preferences.getStringSet(KEY_PROFILE_IDS, emptySet()).orEmpty().toMutableSet()
            ids -= profileId
            preferences
                .edit()
                .putStringSet(KEY_PROFILE_IDS, ids)
                .remove(key(profileId, KEY_SERVER_URL))
                .remove(key(profileId, KEY_LOCAL_SERVER_URL))
                .remove(key(profileId, KEY_REMOTE_SERVER_URL))
                .remove(key(profileId, KEY_SERVER_NAME))
                .remove(key(profileId, KEY_SERVER_VERSION))
                .remove(key(profileId, KEY_SERVER_ID))
                .remove(key(profileId, KEY_USER_ID))
                .remove(key(profileId, KEY_USER_NAME))
                .remove(key(profileId, KEY_USER_IMAGE_TAG))
                .remove(key(profileId, KEY_ACCESS_TOKEN))
                .remove(key(profileId, KEY_LAST_USED_AT))
                .apply()
            secrets.removeAccessToken(profileId)
        }
    }

    override suspend fun clear() {
        withContext(ioDispatcher) {
            preferences.edit().clear().apply()
            secrets.clear()
        }
    }

    private fun readAllInternal(): List<StoredJellyfinSession> {
        migrateLegacySessionIfNeeded()
        return preferences
            .getStringSet(KEY_PROFILE_IDS, emptySet())
            .orEmpty()
            .mapNotNull(::readProfileInternal)
            .sortedByDescending { it.lastUsedAt }
    }

    private fun readProfileInternal(profileId: String): StoredJellyfinSession? {
        val serverUrl = preferences.getString(key(profileId, KEY_SERVER_URL), null) ?: return null
        val userId = preferences.getString(key(profileId, KEY_USER_ID), null)?.let(UUID::fromString) ?: return null
        val userName = preferences.getString(key(profileId, KEY_USER_NAME), null) ?: return null
        val accessToken = readAccessToken(profileId) ?: return null
        return StoredJellyfinSession(
            profileId = profileId,
            serverUrl = serverUrl,
            localServerUrl = preferences.getString(key(profileId, KEY_LOCAL_SERVER_URL), null)
                ?: serverUrl.takeIf(::looksLikeLocalServerAddress),
            remoteServerUrl = preferences.getString(key(profileId, KEY_REMOTE_SERVER_URL), null)
                ?: serverUrl.takeUnless(::looksLikeLocalServerAddress),
            serverName = preferences.getString(key(profileId, KEY_SERVER_NAME), null),
            serverVersion = preferences.getString(key(profileId, KEY_SERVER_VERSION), null),
            serverId = preferences.getString(key(profileId, KEY_SERVER_ID), null),
            userId = userId,
            userName = userName,
            userImageTag = preferences.getString(key(profileId, KEY_USER_IMAGE_TAG), null),
            accessToken = accessToken,
            lastUsedAt = preferences.getLong(key(profileId, KEY_LAST_USED_AT), 0L),
        )
    }

    private fun readAccessToken(profileId: String): String? {
        secrets.readAccessToken(profileId)?.let { return it }
        val legacyToken = preferences.getString(key(profileId, KEY_ACCESS_TOKEN), null)?.takeIf { it.isNotBlank() } ?: return null
        secrets.saveAccessToken(profileId, legacyToken)
        preferences.edit().remove(key(profileId, KEY_ACCESS_TOKEN)).apply()
        return legacyToken
    }

    private fun migrateLegacySessionIfNeeded() {
        if (preferences.contains(KEY_PROFILE_IDS)) return
        val serverUrl = preferences.getString(KEY_SERVER_URL, null) ?: return
        val userId = preferences.getString(KEY_USER_ID, null)?.let(UUID::fromString) ?: return
        val userName = preferences.getString(KEY_USER_NAME, null) ?: return
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null) ?: return
        val profileId = profileId(serverUrl, userId)
        preferences
            .edit()
            .putStringSet(KEY_PROFILE_IDS, setOf(profileId))
            .putString(KEY_LAST_PROFILE_ID, profileId)
            .putString(key(profileId, KEY_SERVER_URL), serverUrl)
            .putString(key(profileId, KEY_LOCAL_SERVER_URL), serverUrl.takeIf(::looksLikeLocalServerAddress))
            .putString(key(profileId, KEY_REMOTE_SERVER_URL), serverUrl.takeUnless(::looksLikeLocalServerAddress))
            .putString(key(profileId, KEY_SERVER_NAME), preferences.getString(KEY_SERVER_NAME, null))
            .putString(key(profileId, KEY_SERVER_VERSION), preferences.getString(KEY_SERVER_VERSION, null))
            .putString(key(profileId, KEY_SERVER_ID), preferences.getString(KEY_SERVER_ID, null))
            .putString(key(profileId, KEY_USER_ID), userId.toString())
            .putString(key(profileId, KEY_USER_NAME), userName)
            .remove(KEY_ACCESS_TOKEN)
            .putLong(key(profileId, KEY_LAST_USED_AT), System.currentTimeMillis())
            .apply()
        secrets.saveAccessToken(profileId, accessToken)
    }

    private companion object {
        const val KEY_PROFILE_IDS = "profiles.ids"
        const val KEY_LAST_PROFILE_ID = "profiles.last"
        const val KEY_SERVER_URL = "server.url"
        const val KEY_LOCAL_SERVER_URL = "server.local_url"
        const val KEY_REMOTE_SERVER_URL = "server.remote_url"
        const val KEY_SERVER_NAME = "server.name"
        const val KEY_SERVER_VERSION = "server.version"
        const val KEY_SERVER_ID = "server.id"
        const val KEY_USER_ID = "user.id"
        const val KEY_USER_NAME = "user.name"
        const val KEY_USER_IMAGE_TAG = "user.image_tag"
        const val KEY_ACCESS_TOKEN = "auth.access_token"
        const val KEY_LAST_USED_AT = "profile.last_used_at"

        fun key(profileId: String, field: String): String = "profile.$profileId.$field"
        fun profileId(serverUrl: String, userId: UUID): String = "${serverUrl.hashCode().toUInt()}-$userId"

        fun looksLikeLocalServerAddress(url: String): Boolean {
            val host = runCatching { java.net.URI(url).host.orEmpty().lowercase() }.getOrDefault("")
            if (host == "localhost" || host.endsWith(".local")) return true
            val parts = host.split('.').mapNotNull { it.toIntOrNull() }
            if (parts.size != 4) return false
            return parts[0] == 10 ||
                parts[0] == 127 ||
                parts[0] == 192 && parts[1] == 168 ||
                parts[0] == 172 && parts[1] in 16..31
        }
    }
}

private class JellyfinEncryptedSessionSecrets(context: Context) {
    private val preferences = context.getSharedPreferences("vantafyn_jellyfin_session_secrets", Context.MODE_PRIVATE)

    fun saveAccessToken(profileId: String, token: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString("$profileId.iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("$profileId.value", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun readAccessToken(profileId: String): String? {
        val iv = preferences.getString("$profileId.iv", null)
            ?.let { runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull() }
            ?: return null
        val encrypted = preferences.getString("$profileId.value", null)
            ?.let { runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull() }
            ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrElse {
            removeAccessToken(profileId)
            null
        }
    }

    fun removeAccessToken(profileId: String) {
        preferences.edit().remove("$profileId.iv").remove("$profileId.value").apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
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

    private companion object {
        const val KEY_ALIAS = "vantafyn_jellyfin_session_secret_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
