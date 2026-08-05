package dev.vantafyn.core.jellyfin

import java.util.UUID

data class JellyfinServerConfig(
    val url: String,
    val name: String? = null,
    val version: String? = null,
    val serverId: String? = null,
)

data class JellyfinUser(
    val id: UUID,
    val name: String,
    val serverName: String? = null,
)

data class JellyfinSession(
    val server: JellyfinServerConfig,
    val user: JellyfinUser,
    internal val accessToken: String,
)

data class JellyfinLibrary(
    val id: UUID,
    val name: String,
    val collectionType: String?,
)

sealed interface JellyfinResult<out T> {
    data class Success<T>(val value: T) : JellyfinResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : JellyfinResult<Nothing>
}

sealed interface JellyfinConnectionState {
    data object Idle : JellyfinConnectionState
    data object Loading : JellyfinConnectionState
    data class Connected(val server: JellyfinServerConfig) : JellyfinConnectionState
    data class Authenticated(val session: JellyfinSession) : JellyfinConnectionState
    data class Error(val message: String) : JellyfinConnectionState
}

data class StoredJellyfinSession(
    val serverUrl: String,
    val serverName: String?,
    val serverVersion: String?,
    val serverId: String?,
    val userId: UUID,
    val userName: String,
    val accessToken: String,
)

interface JellyfinSessionStorage {
    suspend fun read(): StoredJellyfinSession?
    suspend fun write(session: StoredJellyfinSession)
    suspend fun clear()
}

interface JellyfinAuthRepository {
    suspend fun savedServerHint(): JellyfinServerConfig?
    suspend fun restoreSession(): JellyfinResult<JellyfinSession>
    suspend fun testServer(serverUrl: String): JellyfinResult<JellyfinServerConfig>
    suspend fun login(serverUrl: String, username: String, password: String): JellyfinResult<JellyfinSession>
    suspend fun logout()
}

interface JellyfinLibraryRepository {
    suspend fun getLibraries(session: JellyfinSession): JellyfinResult<List<JellyfinLibrary>>
}
