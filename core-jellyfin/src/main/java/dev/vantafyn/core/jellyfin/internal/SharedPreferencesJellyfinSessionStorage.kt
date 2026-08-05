package dev.vantafyn.core.jellyfin

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class SharedPreferencesJellyfinSessionStorage(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinSessionStorage {
    private val preferences = context.applicationContext.getSharedPreferences(
        "vantafyn_jellyfin_session",
        Context.MODE_PRIVATE,
    )

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
                .putString(key(session.profileId, KEY_SERVER_NAME), session.serverName)
                .putString(key(session.profileId, KEY_SERVER_VERSION), session.serverVersion)
                .putString(key(session.profileId, KEY_SERVER_ID), session.serverId)
                .putString(key(session.profileId, KEY_USER_ID), session.userId.toString())
                .putString(key(session.profileId, KEY_USER_NAME), session.userName)
                .putString(key(session.profileId, KEY_USER_IMAGE_TAG), session.userImageTag)
                .putString(key(session.profileId, KEY_ACCESS_TOKEN), session.accessToken)
                .putLong(key(session.profileId, KEY_LAST_USED_AT), session.lastUsedAt)
                .apply()
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
                .remove(key(profileId, KEY_SERVER_NAME))
                .remove(key(profileId, KEY_SERVER_VERSION))
                .remove(key(profileId, KEY_SERVER_ID))
                .remove(key(profileId, KEY_USER_ID))
                .remove(key(profileId, KEY_USER_NAME))
                .remove(key(profileId, KEY_USER_IMAGE_TAG))
                .remove(key(profileId, KEY_ACCESS_TOKEN))
                .remove(key(profileId, KEY_LAST_USED_AT))
                .apply()
        }
    }

    override suspend fun clear() {
        withContext(ioDispatcher) {
            preferences.edit().clear().apply()
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
        val accessToken = preferences.getString(key(profileId, KEY_ACCESS_TOKEN), null) ?: return null
        return StoredJellyfinSession(
            profileId = profileId,
            serverUrl = serverUrl,
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
            .putString(key(profileId, KEY_SERVER_NAME), preferences.getString(KEY_SERVER_NAME, null))
            .putString(key(profileId, KEY_SERVER_VERSION), preferences.getString(KEY_SERVER_VERSION, null))
            .putString(key(profileId, KEY_SERVER_ID), preferences.getString(KEY_SERVER_ID, null))
            .putString(key(profileId, KEY_USER_ID), userId.toString())
            .putString(key(profileId, KEY_USER_NAME), userName)
            .putString(key(profileId, KEY_ACCESS_TOKEN), accessToken)
            .putLong(key(profileId, KEY_LAST_USED_AT), System.currentTimeMillis())
            .apply()
    }

    private companion object {
        const val KEY_PROFILE_IDS = "profiles.ids"
        const val KEY_LAST_PROFILE_ID = "profiles.last"
        const val KEY_SERVER_URL = "server.url"
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
    }
}
