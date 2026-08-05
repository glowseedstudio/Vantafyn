package dev.vantafyn.core.jellyfin

data class JellyfinServer(
    val address: String,
    val displayName: String? = null,
)

data class JellyfinUserSession(
    val server: JellyfinServer,
    val userId: String,
    val accessToken: String,
)

interface JellyfinSessionRepository {
    suspend fun discoverServers(): List<JellyfinServer>
    suspend fun authenticate(server: JellyfinServer, username: String, password: String): JellyfinUserSession
}
