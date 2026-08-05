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
            val serverUrl = preferences.getString(KEY_SERVER_URL, null) ?: return@withContext null
            val userId = preferences.getString(KEY_USER_ID, null)?.let(UUID::fromString) ?: return@withContext null
            val userName = preferences.getString(KEY_USER_NAME, null) ?: return@withContext null
            val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null) ?: return@withContext null
            StoredJellyfinSession(
                serverUrl = serverUrl,
                serverName = preferences.getString(KEY_SERVER_NAME, null),
                serverVersion = preferences.getString(KEY_SERVER_VERSION, null),
                serverId = preferences.getString(KEY_SERVER_ID, null),
                userId = userId,
                userName = userName,
                accessToken = accessToken,
            )
        }

    override suspend fun write(session: StoredJellyfinSession) {
        withContext(ioDispatcher) {
            preferences
                .edit()
                .putString(KEY_SERVER_URL, session.serverUrl)
                .putString(KEY_SERVER_NAME, session.serverName)
                .putString(KEY_SERVER_VERSION, session.serverVersion)
                .putString(KEY_SERVER_ID, session.serverId)
                .putString(KEY_USER_ID, session.userId.toString())
                .putString(KEY_USER_NAME, session.userName)
                .putString(KEY_ACCESS_TOKEN, session.accessToken)
                .apply()
        }
    }

    override suspend fun clear() {
        withContext(ioDispatcher) {
            preferences.edit().clear().apply()
        }
    }

    private companion object {
        const val KEY_SERVER_URL = "server.url"
        const val KEY_SERVER_NAME = "server.name"
        const val KEY_SERVER_VERSION = "server.version"
        const val KEY_SERVER_ID = "server.id"
        const val KEY_USER_ID = "user.id"
        const val KEY_USER_NAME = "user.name"
        const val KEY_ACCESS_TOKEN = "auth.access_token"
    }
}
