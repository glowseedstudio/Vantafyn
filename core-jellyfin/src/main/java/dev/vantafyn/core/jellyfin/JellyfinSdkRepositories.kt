package dev.vantafyn.core.jellyfin

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.authenticateUserByName
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo
import java.net.URI

class JellyfinRepositoryProvider(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val appContext = context.applicationContext
    private val jellyfin = createJellyfin {
        this.context = appContext
        clientInfo = ClientInfo(name = "Vantafyn", version = "0.1.0")
        deviceInfo = DeviceInfo(
            id = resolveDeviceId(appContext),
            name = android.os.Build.MODEL ?: "Android",
        )
        minimumServerVersion = Jellyfin.minimumVersion
    }
    private val storage = SharedPreferencesJellyfinSessionStorage(appContext, ioDispatcher)

    val authRepository: JellyfinAuthRepository =
        SdkJellyfinAuthRepository(jellyfin, storage, ioDispatcher)

    val libraryRepository: JellyfinLibraryRepository =
        SdkJellyfinLibraryRepository(jellyfin, ioDispatcher)

    private fun resolveDeviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeIf { it.isNotBlank() }
            ?: "vantafyn-android"
}

class SdkJellyfinAuthRepository(
    private val jellyfin: Jellyfin,
    private val storage: JellyfinSessionStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinAuthRepository {
    override suspend fun savedServerHint(): JellyfinServerConfig? =
        withContext(ioDispatcher) {
            storage.read()?.let { stored ->
                JellyfinServerConfig(
                    url = stored.serverUrl,
                    name = stored.serverName,
                    version = stored.serverVersion,
                    serverId = stored.serverId,
                )
            }
        }

    override suspend fun restoreSession(): JellyfinResult<JellyfinSession> =
        runCatchingResult {
            val stored = storage.read() ?: throw SessionRestoreException("No saved Jellyfin session")
            val server = JellyfinServerConfig(
                url = stored.serverUrl,
                name = stored.serverName,
                version = stored.serverVersion,
                serverId = stored.serverId,
            )
            val api = jellyfin.createApi(baseUrl = server.url, accessToken = stored.accessToken)
            val currentUser by api.userApi.getCurrentUser()
            val systemInfo = getPublicSystemInfo(api, server)
            val restored = JellyfinSession(
                server = server.copy(
                    name = systemInfo.name ?: server.name,
                    version = systemInfo.version ?: server.version,
                    serverId = systemInfo.id ?: server.serverId,
                ),
                user = JellyfinUser(
                    id = currentUser.id,
                    name = currentUser.name ?: stored.userName,
                    serverName = currentUser.serverName,
                ),
                accessToken = stored.accessToken,
            )
            storage.write(restored.toStoredSession())
            restored
        }

    override suspend fun testServer(serverUrl: String): JellyfinResult<JellyfinServerConfig> =
        runCatchingResult {
            val normalizedUrl = normalizeServerUrl(serverUrl)
            val api = jellyfin.createApi(baseUrl = normalizedUrl)
            val systemInfo = getPublicSystemInfo(api, JellyfinServerConfig(normalizedUrl))
            JellyfinServerConfig(
                url = normalizedUrl,
                name = systemInfo.name,
                version = systemInfo.version,
                serverId = systemInfo.id,
            )
        }

    override suspend fun login(
        serverUrl: String,
        username: String,
        password: String,
    ): JellyfinResult<JellyfinSession> =
        runCatchingResult {
            val normalizedUrl = normalizeServerUrl(serverUrl)
            val api = jellyfin.createApi(baseUrl = normalizedUrl)
            val auth by api.userApi.authenticateUserByName(
                username = username.trim(),
                password = password,
            )
            val accessToken = auth.accessToken ?: throw AuthenticationException("Server did not return an access token")
            val authenticatedUser = auth.user ?: throw AuthenticationException("Server did not return a user")
            val authedApi = jellyfin.createApi(baseUrl = normalizedUrl, accessToken = accessToken)
            val currentUser by authedApi.userApi.getCurrentUser()
            val systemInfo = getPublicSystemInfo(
                authedApi,
                JellyfinServerConfig(normalizedUrl, serverId = auth.serverId),
            )
            val session = JellyfinSession(
                server = JellyfinServerConfig(
                    url = normalizedUrl,
                    name = systemInfo.name ?: authenticatedUser.serverName,
                    version = systemInfo.version,
                    serverId = systemInfo.id ?: auth.serverId,
                ),
                user = JellyfinUser(
                    id = currentUser.id,
                    name = currentUser.name ?: authenticatedUser.name.orEmpty(),
                    serverName = currentUser.serverName ?: authenticatedUser.serverName,
                ),
                accessToken = accessToken,
            )
            storage.write(session.toStoredSession())
            session
        }

    override suspend fun logout() {
        withContext(ioDispatcher) {
            storage.clear()
        }
    }

    private suspend fun <T> runCatchingResult(block: suspend () -> T): JellyfinResult<T> =
        withContext(ioDispatcher) {
            try {
                JellyfinResult.Success(block())
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }
}

class SdkJellyfinLibraryRepository(
    private val jellyfin: Jellyfin,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinLibraryRepository {
    override suspend fun getLibraries(session: JellyfinSession): JellyfinResult<List<JellyfinLibrary>> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(
                    baseUrl = session.server.url,
                    accessToken = session.accessToken,
                )
                val views by api.userViewsApi.getUserViews(userId = session.user.id)
                val libraries = views.items.mapNotNull { item ->
                    JellyfinLibrary(
                        id = item.id,
                        name = item.name ?: "Untitled library",
                        collectionType = item.collectionType?.serialName,
                    )
                }
                JellyfinResult.Success(libraries)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }
}

private data class PublicSystemSummary(
    val name: String?,
    val version: String?,
    val id: String?,
)

private suspend fun getPublicSystemInfo(
    api: ApiClient,
    fallback: JellyfinServerConfig,
): PublicSystemSummary {
    val info by api.systemApi.getPublicSystemInfo()
    return PublicSystemSummary(
        name = info.serverName ?: fallback.name,
        version = info.version ?: fallback.version,
        id = info.id ?: fallback.serverId,
    )
}

private fun JellyfinSession.toStoredSession(): StoredJellyfinSession =
    StoredJellyfinSession(
        serverUrl = server.url,
        serverName = server.name,
        serverVersion = server.version,
        serverId = server.serverId,
        userId = user.id,
        userName = user.name,
        accessToken = accessToken,
    )

private fun normalizeServerUrl(input: String): String {
    val trimmed = input.trim().trimEnd('/')
    require(trimmed.isNotBlank()) { "Enter a Jellyfin server URL" }
    val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
    val uri = URI(withScheme)
    require(uri.host?.isNotBlank() == true) { "Enter a valid server URL" }
    return uri.toString().trimEnd('/')
}

private fun toUserMessage(throwable: Throwable): String {
    val className = throwable.javaClass.name
    val message = throwable.message.orEmpty()
    return when {
        throwable is SessionRestoreException -> throwable.message ?: "No saved Jellyfin session"
        throwable is AuthenticationException -> throwable.message ?: "Unable to authenticate"
        className.contains("InvalidStatusException") && message.contains("401") -> "Invalid username or password"
        className.contains("InvalidStatusException") -> "Server returned an unexpected response"
        throwable is IllegalArgumentException -> throwable.message ?: "Invalid server address"
        else -> "Could not reach the Jellyfin server"
    }
}

private class AuthenticationException(message: String) : RuntimeException(message)
private class SessionRestoreException(message: String) : RuntimeException(message)
