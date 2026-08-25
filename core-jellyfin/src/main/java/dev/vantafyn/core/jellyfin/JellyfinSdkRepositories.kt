package dev.vantafyn.core.jellyfin

import android.content.Context
import android.provider.Settings
import android.util.Log
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.authenticateUserByName
import org.jellyfin.sdk.api.client.extensions.authenticateWithQuickConnect
import org.jellyfin.sdk.api.client.extensions.activityLogApi
import org.jellyfin.sdk.api.client.extensions.artistsApi
import org.jellyfin.sdk.api.client.extensions.devicesApi
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.api.client.extensions.lyricsApi
import org.jellyfin.sdk.api.client.extensions.mediaInfoApi
import org.jellyfin.sdk.api.client.extensions.mediaSegmentsApi
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.api.client.extensions.playlistsApi
import org.jellyfin.sdk.api.client.extensions.pluginsApi
import org.jellyfin.sdk.api.client.extensions.quickConnectApi
import org.jellyfin.sdk.api.client.extensions.scheduledTasksApi
import org.jellyfin.sdk.api.client.extensions.searchApi
import org.jellyfin.sdk.api.client.extensions.sessionApi
import org.jellyfin.sdk.api.client.extensions.syncPlayApi
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.universalAudioApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo
import org.jellyfin.sdk.model.FileInfo
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ChannelType
import org.jellyfin.sdk.model.api.CreatePlaylistDto
import org.jellyfin.sdk.model.api.CreateUserByName
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.DirectPlayProfile
import org.jellyfin.sdk.model.api.DlnaProfileType
import org.jellyfin.sdk.model.api.EncodingContext
import org.jellyfin.sdk.model.api.GeneralCommandType
import org.jellyfin.sdk.model.api.ImageFormat
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.JoinGroupRequestDto
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaSegmentDto
import org.jellyfin.sdk.model.api.MediaSegmentType
import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.MessageCommand
import org.jellyfin.sdk.model.api.NewGroupRequestDto
import org.jellyfin.sdk.model.api.PlayRequestDto
import org.jellyfin.sdk.model.api.SearchHint
import org.jellyfin.sdk.model.api.SeekRequestDto
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.PlayAccess
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import org.jellyfin.sdk.model.api.PlaybackOrder
import org.jellyfin.sdk.model.api.PlaybackProgressInfo
import org.jellyfin.sdk.model.api.PlaybackStartInfo
import org.jellyfin.sdk.model.api.PlaybackStopInfo
import org.jellyfin.sdk.model.api.RepeatMode
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.OpenLiveStreamDto
import org.jellyfin.sdk.model.api.SubtitlePlaybackMode
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import org.jellyfin.sdk.model.api.SubtitleProfile
import org.jellyfin.sdk.model.api.TranscodeSeekInfo
import org.jellyfin.sdk.model.api.TranscodingProfile
import org.jellyfin.sdk.model.api.UserItemDataDto
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.UpdateUserPassword
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.api.request.GetEpisodesRequest
import org.jellyfin.sdk.model.api.request.GetLatestMediaRequest
import org.jellyfin.sdk.model.api.request.GetLiveTvChannelsRequest
import org.jellyfin.sdk.model.api.request.GetNextUpRequest
import org.jellyfin.sdk.model.api.request.GetPlaylistItemsRequest
import org.jellyfin.sdk.model.api.request.GetRecommendedProgramsRequest
import org.jellyfin.sdk.model.api.request.GetResumeItemsRequest
import org.jellyfin.sdk.model.api.request.GetSearchHintsRequest
import org.jellyfin.sdk.model.api.request.GetSeasonsRequest
import org.jellyfin.sdk.model.api.request.GetSimilarItemsRequest
import org.jellyfin.sdk.model.api.request.GetThemeSongsRequest
import org.json.JSONArray
import org.json.JSONObject

class JellyfinRepositoryProvider(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val appContext = context.applicationContext
    private val deviceId = resolveDeviceId(appContext)
    private val jellyfin = createJellyfin {
        this.context = appContext
        clientInfo = ClientInfo(
            name = if (appContext.packageName.contains("mobile", ignoreCase = true)) "Vantafyn Mobile" else "Vantafyn TV",
            version = "0.9.2",
        )
        deviceInfo = DeviceInfo(
            id = deviceId,
            name = android.os.Build.MODEL ?: "Android",
        )
        minimumServerVersion = Jellyfin.minimumVersion
    }
    private val storage = SharedPreferencesJellyfinSessionStorage(appContext, ioDispatcher)

    val authRepository: JellyfinAuthRepository =
        SdkJellyfinAuthRepository(jellyfin, storage, ioDispatcher)

    val libraryRepository: JellyfinLibraryRepository =
        SdkJellyfinLibraryRepository(jellyfin, ioDispatcher)

    val mediaRepository: JellyfinMediaRepository =
        SdkJellyfinMediaRepository(jellyfin, ioDispatcher)

    val searchRepository: JellyfinSearchRepository =
        SdkJellyfinSearchRepository(jellyfin, ioDispatcher)

    val favoritesRepository: JellyfinFavoritesRepository =
        SdkJellyfinFavoritesRepository(jellyfin, ioDispatcher)

    val adminRepository: JellyfinAdminRepository =
        SdkJellyfinAdminRepository(jellyfin, ioDispatcher)

    val homeRepository: JellyfinHomeRepository =
        SdkJellyfinHomeRepository(jellyfin, ioDispatcher)

    val quickConnectRepository: JellyfinQuickConnectRepository =
        SdkJellyfinQuickConnectRepository(jellyfin, storage, ioDispatcher)

    val userPreferencesRepository: JellyfinUserPreferencesRepository =
        SdkJellyfinUserPreferencesRepository(jellyfin, storage, ioDispatcher)

    val playbackRepository: JellyfinPlaybackRepository =
        SdkJellyfinPlaybackRepository(jellyfin, deviceId, ioDispatcher)

    val mediaSegmentRepository: JellyfinMediaSegmentRepository =
        SdkJellyfinMediaSegmentRepository(jellyfin, ioDispatcher)

    val musicRepository: JellyfinMusicRepository =
        SdkJellyfinMusicRepository(jellyfin, ioDispatcher)

    val watchPartyRepository: JellyfinWatchPartyRepository =
        SdkJellyfinWatchPartyRepository(jellyfin, ioDispatcher)

    val realtimeClient: JellyfinRealtimeClient =
        SdkJellyfinRealtimeClient(jellyfin, ioDispatcher)

    val achievementRepository: JellyfinAchievementRepository =
        SdkJellyfinAchievementRepository(ioDispatcher)

    val socialRepository: JellyfinSocialRepository =
        SdkJellyfinSocialRepository(ioDispatcher)

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
    override suspend fun savedProfiles(): List<SavedProfile> =
        withContext(ioDispatcher) {
            storage.readAll().map { stored ->
                SavedProfile(
                    id = stored.profileId,
                    serverRef = stored.serverId ?: stored.serverUrl,
                    serverName = stored.serverName,
                    serverUrl = stored.serverUrl,
                    localServerUrl = stored.localServerUrl,
                    remoteServerUrl = stored.remoteServerUrl,
                    jellyfinUserId = stored.userId,
                    displayName = stored.userName,
                    userImageTag = stored.userImageTag,
                    imageUrl = userImageUrl(jellyfin, stored),
                    lastUsedAt = stored.lastUsedAt,
                )
            }
        }

    override suspend fun restoreSession(profileId: String): JellyfinResult<JellyfinSession> =
        runCatchingRestoreResult {
            val stored = storage.read(profileId) ?: throw SessionRestoreException("Saved profile was not found")
            restoreStoredSession(stored)
        }

    override suspend fun updateSavedServerUrl(profileId: String, serverUrl: String): JellyfinResult<JellyfinSession> =
        updateSavedServerUrls(profileId, serverUrl, null)

    override suspend fun updateSavedServerUrls(profileId: String, localUrl: String?, remoteUrl: String?): JellyfinResult<JellyfinSession> =
        runCatchingRestoreResult {
            val stored = storage.read(profileId) ?: throw SessionRestoreException("Saved profile was not found")
            val server = when (val result = testServer(localUrl, remoteUrl)) {
                is JellyfinResult.Success -> result.value
                is JellyfinResult.Failure -> throw result.cause ?: IllegalArgumentException(result.message)
            }
            validateSameServer(stored.serverId, server.serverId)
            val updated = stored.copy(
                serverUrl = server.url,
                localServerUrl = server.localUrl,
                remoteServerUrl = server.remoteUrl,
                serverName = server.name ?: stored.serverName,
                serverVersion = server.version ?: stored.serverVersion,
                serverId = server.serverId ?: stored.serverId,
            )
            storage.write(updated)
            restoreStoredSession(updated)
        }

    override suspend fun testServer(serverUrl: String): JellyfinResult<JellyfinServerConfig> =
        testServer(serverUrl, null)

    override suspend fun testServer(localUrl: String?, remoteUrl: String?): JellyfinResult<JellyfinServerConfig> =
        runCatchingResult {
            resolveServerEndpoint(localUrl = localUrl, remoteUrl = remoteUrl, expectedServerId = null)
        }

    override suspend fun publicUsers(server: JellyfinServerConfig): JellyfinResult<List<JellyfinPublicUser>> =
        runCatchingResult {
            val api = jellyfin.createApi(baseUrl = server.url)
            val users by api.userApi.getPublicUsers()
            users
                .filter { user ->
                    user.name?.isNotBlank() == true &&
                        user.policy?.isHidden != true &&
                        user.policy?.isDisabled != true
                }
                .map { user ->
                    JellyfinPublicUser(
                        server = server,
                        id = user.id,
                        displayName = user.name.orEmpty(),
                        primaryImageTag = user.primaryImageTag,
                        imageUrl = publicUserImageUrl(api, user.id, user.primaryImageTag),
                        hasPassword = user.hasPassword == true || user.hasConfiguredPassword == true,
                        isAdministrator = user.policy?.isAdministrator == true,
                    )
                }
        }

    override suspend fun login(
        serverUrl: String,
        username: String,
        password: String,
    ): JellyfinResult<JellyfinSession> =
        login(localUrl = serverUrl, remoteUrl = null, username = username, password = password)

    override suspend fun login(
        localUrl: String?,
        remoteUrl: String?,
        username: String,
        password: String,
    ): JellyfinResult<JellyfinSession> =
        runCatchingResult {
            val server = resolveServerEndpoint(localUrl = localUrl, remoteUrl = remoteUrl, expectedServerId = null)
            val api = jellyfin.createApi(baseUrl = server.url)
            val auth by api.userApi.authenticateUserByName(
                username = username.trim(),
                password = password,
            )
            val accessToken = auth.accessToken ?: throw AuthenticationException("Server did not return an access token")
            val authenticatedUser = auth.user ?: throw AuthenticationException("Server did not return a user")
            val authedApi = jellyfin.createApi(baseUrl = server.url, accessToken = accessToken)
            val currentUser by authedApi.userApi.getCurrentUser()
            val systemInfo = getPublicSystemInfo(
                authedApi,
                server.copy(serverId = auth.serverId ?: server.serverId),
            )
            val session = JellyfinSession(
                server = server.copy(
                    name = systemInfo.name ?: authenticatedUser.serverName,
                    version = systemInfo.version,
                    serverId = systemInfo.id ?: auth.serverId,
                ),
                user = JellyfinUser(
                    id = currentUser.id,
                    name = currentUser.name ?: authenticatedUser.name.orEmpty(),
                    serverName = currentUser.serverName ?: authenticatedUser.serverName,
                    primaryImageTag = currentUser.primaryImageTag ?: authenticatedUser.primaryImageTag,
                    isAdministrator = currentUser.policy?.isAdministrator == true,
                ),
                profileId = profileId(systemInfo.id ?: auth.serverId ?: server.url, currentUser.id),
                accessToken = accessToken,
            )
            storage.write(session.toStoredSession())
            session
        }

    override suspend fun createPairingPayload(code: String, profileId: String?): TvPairingPayload? =
        withContext(ioDispatcher) {
            val targetId = profileId ?: storage.read()?.profileId ?: return@withContext null
            val stored = storage.read(targetId) ?: return@withContext null
            TvPairingPayload(
                code = code,
                serverUrl = stored.serverUrl,
                localServerUrl = stored.localServerUrl,
                remoteServerUrl = stored.remoteServerUrl,
                serverName = stored.serverName,
                serverVersion = stored.serverVersion,
                serverId = stored.serverId,
                userId = stored.userId,
                userName = stored.userName,
                userImageTag = stored.userImageTag,
                accessToken = stored.accessToken,
                profileId = stored.profileId,
                profileDisplayName = stored.userName,
                profileImageUrl = userImageUrl(jellyfin, stored),
                hasPassword = false,
            )
        }

    override suspend fun importPairedSession(payload: TvPairingPayload): JellyfinResult<JellyfinSession> =
        runCatchingRestoreResult {
            val stored = StoredJellyfinSession(
                profileId = payload.profileId,
                serverUrl = payload.serverUrl,
                localServerUrl = payload.localServerUrl,
                remoteServerUrl = payload.remoteServerUrl,
                serverName = payload.serverName,
                serverVersion = payload.serverVersion,
                serverId = payload.serverId,
                userId = payload.userId,
                userName = payload.userName,
                userImageTag = payload.userImageTag,
                accessToken = payload.accessToken,
                lastUsedAt = System.currentTimeMillis(),
            )
            storage.write(stored)
            restoreStoredSession(stored)
        }

    override suspend fun removeProfile(profileId: String) {
        storage.remove(profileId)
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

    private suspend fun <T> runCatchingRestoreResult(block: suspend () -> T): JellyfinResult<T> =
        withContext(ioDispatcher) {
            try {
                JellyfinResult.Success(withTimeout(RESTORE_TIMEOUT_MS) { block() })
            } catch (throwable: Throwable) {
                val failure = throwable as? JellyfinSessionRestoreFailure ?: throwable.toRestoreFailure()
                JellyfinResult.Failure(failure.message ?: "Could not restore saved Jellyfin session", failure)
            }
        }

    private suspend fun restoreStoredSession(stored: StoredJellyfinSession): JellyfinSession {
        val server = resolveServerEndpoint(
            localUrl = stored.localServerUrl ?: stored.serverUrl,
            remoteUrl = stored.remoteServerUrl,
            expectedServerId = stored.serverId,
        ).copy(
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
                primaryImageTag = currentUser.primaryImageTag ?: stored.userImageTag,
                isAdministrator = currentUser.policy?.isAdministrator == true,
            ),
            profileId = stored.profileId,
            accessToken = stored.accessToken,
        )
        storage.write(restored.toStoredSession())
        return restored
    }

    private companion object {
        const val RESTORE_TIMEOUT_MS = 12_000L
        const val CONNECTION_TIMEOUT_MS = 5_000L
    }

    private suspend fun resolveServerEndpoint(
        localUrl: String?,
        remoteUrl: String?,
        expectedServerId: String?,
    ): JellyfinServerConfig {
        val endpoints = serverEndpointCandidates(localUrl, remoteUrl)
        var lastFailure: Throwable? = null
        endpoints.forEach { endpoint ->
            endpoint.candidates.forEach { normalizedUrl ->
                try {
                    val api = jellyfin.createApi(baseUrl = normalizedUrl)
                    val systemInfo = withTimeout(CONNECTION_TIMEOUT_MS) {
                        getPublicSystemInfo(api, JellyfinServerConfig(normalizedUrl))
                    }
                    validateSameServer(expectedServerId, systemInfo.id)
                    return JellyfinServerConfig(
                        url = normalizedUrl,
                        name = systemInfo.name,
                        version = systemInfo.version,
                        serverId = systemInfo.id,
                        localUrl = endpoint.normalizedLocal,
                        remoteUrl = endpoint.normalizedRemote,
                    )
                } catch (throwable: Throwable) {
                    lastFailure = throwable
                }
            }
        }
        throw lastFailure ?: IllegalArgumentException("Enter a local or remote Jellyfin server address")
    }

    private fun serverEndpointCandidates(localUrl: String?, remoteUrl: String?): List<ServerEndpointCandidate> {
        val localCandidates = normalizedCandidates(localUrl)
        val remoteCandidates = normalizedCandidates(remoteUrl)
        require(localCandidates.isNotEmpty() || remoteCandidates.isNotEmpty()) {
            "Enter a local or remote Jellyfin server address"
        }
        val normalizedLocal = localCandidates.firstOrNull()
        val normalizedRemote = remoteCandidates.firstOrNull()
        return buildList {
            if (localCandidates.isNotEmpty()) {
                add(ServerEndpointCandidate(localCandidates, normalizedLocal, normalizedRemote))
            }
            val remoteOnly = remoteCandidates.filterNot { it in localCandidates }
            if (remoteOnly.isNotEmpty()) {
                add(ServerEndpointCandidate(remoteOnly, normalizedLocal, normalizedRemote))
            }
        }
    }

    private fun normalizedCandidates(url: String?): List<String> =
        url?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let(JellyfinServerUrlNormalizer::candidates)
            .orEmpty()
}

private data class ServerEndpointCandidate(
    val candidates: List<String>,
    val normalizedLocal: String?,
    val normalizedRemote: String?,
)

private fun validateSameServer(expectedServerId: String?, actualServerId: String?) {
    if (expectedServerId.isNullOrBlank() || actualServerId.isNullOrBlank()) return
    if (!expectedServerId.equals(actualServerId, ignoreCase = true)) {
        throw SecurityException("That address belongs to a different Jellyfin server.")
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
                        primaryImageTag = item.imageTags?.get(ImageType.PRIMARY),
                        imageUrl = item.imageTags?.get(ImageType.PRIMARY)?.let {
                            itemImageUrl(api, item.id, ImageType.PRIMARY, it, maxWidth = 420)
                        },
                    )
                }
                JellyfinResult.Success(libraries)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun getLibraryItems(
        session: JellyfinSession,
        library: JellyfinLibrary,
        limit: Int,
    ): JellyfinResult<List<JellyfinMediaItem>> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                if (library.collectionType.isLiveTvCollection()) {
                    val channels = fetchLiveTvChannels(api, session, limit)
                    return@withContext JellyfinResult.Success(channels.map { it.toMediaItem() })
                }
                val allItems = mutableListOf<JellyfinMediaItem>()
                var startIndex = 0
                val pageSize = limit.coerceAtLeast(200)
                var totalRecordCount: Int? = null
                var pageItemCount: Int
                val isMusic = library.collectionType.isMusicCollection()
                do {
                    val response by api.itemsApi.getItems(
                        GetItemsRequest(
                            userId = session.user.id,
                            parentId = library.id,
                            recursive = true,
                            startIndex = startIndex,
                            limit = pageSize,
                            sortBy = listOf(ItemSortBy.DATE_CREATED),
                            sortOrder = listOf(SortOrder.DESCENDING),
                            fields = if (isMusic) musicItemFields else itemFields,
                            includeItemTypes = includeTypesFor(library.collectionType),
                            enableUserData = true,
                            imageTypeLimit = 2,
                            enableImageTypes = if (isMusic) listOf(ImageType.PRIMARY) else itemImageTypes,
                            enableImages = true,
                            enableTotalRecordCount = true,
                        ),
                    )
                    val pageItems = response.items.map { it.toMediaItem(api, shapeFor(it.type)) }
                    pageItemCount = pageItems.size
                    totalRecordCount = response.totalRecordCount ?: totalRecordCount
                    allItems += pageItems
                    startIndex += pageItemCount
                } while (pageItemCount > 0 && allItems.size < (totalRecordCount ?: Int.MAX_VALUE))
                JellyfinResult.Success(allItems.distinctBy { it.id })
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun getLibraryItemsPage(
        session: JellyfinSession,
        library: JellyfinLibrary,
        startIndex: Int,
        limit: Int,
        filter: JellyfinLibraryItemFilter,
        alphabetKey: String?,
    ): JellyfinResult<JellyfinLibraryPage> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val normalizedAlphabetKey = alphabetKey?.trim()?.uppercase()?.takeIf {
                    it == "#" || (it.length == 1 && it[0] in 'A'..'Z')
                }
                if (library.collectionType.isLiveTvCollection()) {
                    val allChannels = fetchLiveTvChannels(api, session, limit = 1_000)
                    val filteredChannels = when (normalizedAlphabetKey) {
                        null -> allChannels
                        "#" -> allChannels.filter { channel -> channel.name.firstOrNull()?.isLetter() != true }
                        else -> allChannels.filter { channel -> channel.name.startsWith(normalizedAlphabetKey, ignoreCase = true) }
                    }
                    val safeStart = startIndex.coerceAtLeast(0)
                    val pageItems = filteredChannels.drop(safeStart).take(limit).map { it.toMediaItem() }
                    return@withContext JellyfinResult.Success(
                        JellyfinLibraryPage(
                            items = pageItems,
                            startIndex = safeStart,
                            pageSize = limit,
                            totalItems = filteredChannels.size,
                            alphabetKey = normalizedAlphabetKey,
                        ),
                    )
                }
                val safeStart = startIndex.coerceAtLeast(0)
                val sortBy = when (filter) {
                    JellyfinLibraryItemFilter.RecentlyAdded -> listOf(ItemSortBy.DATE_CREATED)
                    JellyfinLibraryItemFilter.All,
                    JellyfinLibraryItemFilter.AZ,
                    JellyfinLibraryItemFilter.Favorites,
                    JellyfinLibraryItemFilter.Unwatched -> listOf(ItemSortBy.SORT_NAME)
                }
                val sortOrder = when (filter) {
                    JellyfinLibraryItemFilter.RecentlyAdded -> listOf(SortOrder.DESCENDING)
                    JellyfinLibraryItemFilter.All,
                    JellyfinLibraryItemFilter.AZ,
                    JellyfinLibraryItemFilter.Favorites,
                    JellyfinLibraryItemFilter.Unwatched -> listOf(SortOrder.ASCENDING)
                }
                val isMusic = library.collectionType.isMusicCollection()
                val response by api.itemsApi.getItems(
                    GetItemsRequest(
                        userId = session.user.id,
                        parentId = library.id,
                        recursive = true,
                        startIndex = safeStart,
                        limit = limit,
                        sortBy = sortBy,
                        sortOrder = sortOrder,
                        fields = if (isMusic) musicItemFields else itemFields,
                        includeItemTypes = includeTypesFor(library.collectionType),
                        isFavorite = true.takeIf { filter == JellyfinLibraryItemFilter.Favorites },
                        isPlayed = false.takeIf { filter == JellyfinLibraryItemFilter.Unwatched },
                        nameStartsWith = normalizedAlphabetKey?.takeIf { it != "#" },
                        nameLessThan = "A".takeIf { normalizedAlphabetKey == "#" },
                        enableUserData = true,
                        imageTypeLimit = 2,
                        enableImageTypes = if (isMusic) listOf(ImageType.PRIMARY) else itemImageTypes,
                        enableImages = true,
                        enableTotalRecordCount = true,
                    ),
                )
                JellyfinResult.Success(
                    JellyfinLibraryPage(
                        items = response.items.map { it.toMediaItem(api, shapeFor(it.type)) },
                        startIndex = safeStart,
                        pageSize = limit,
                        totalItems = response.totalRecordCount,
                        alphabetKey = normalizedAlphabetKey,
                    ),
                )
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun buildAvailabilityIndex(session: JellyfinSession): JellyfinResult<JellyfinAvailabilityIndex> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val itemsByProviderId = linkedMapOf<String, JellyfinAvailabilityMatch>()
                var moviesCount = 0
                var seriesCount = 0
                var startIndex = 0
                val pageSize = 500
                var pageItemCount: Int
                do {
                    val response by api.itemsApi.getItems(
                        GetItemsRequest(
                            userId = session.user.id,
                            recursive = true,
                            startIndex = startIndex,
                            limit = pageSize,
                            fields = listOf(ItemFields.PROVIDER_IDS),
                            includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
                            enableImages = false,
                            enableUserData = false,
                            enableTotalRecordCount = true,
                        ),
                    )
                    pageItemCount = response.items.size
                    response.items.forEach { item ->
                        when (item.type) {
                            BaseItemKind.MOVIE -> moviesCount += 1
                            BaseItemKind.SERIES -> seriesCount += 1
                            else -> Unit
                        }
                        item.providerIds.orEmpty()
                            .mapNotNull { (provider, value) ->
                                val safeProvider = provider?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                                val safeValue = value?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                                safeProvider to safeValue
                            }
                            .forEach { (provider, value) ->
                                val itemType = item.type?.serialName
                                val match = JellyfinAvailabilityMatch(
                                    itemId = item.id,
                                    title = item.name ?: "Untitled",
                                    itemType = itemType,
                                    serverId = session.server.serverId ?: session.server.localId,
                                    serverName = session.server.name,
                                    matchedProvider = provider,
                                    matchedProviderId = value,
                                )
                                itemsByProviderId[ProviderIdMatcher.key(provider, value)] = match
                                itemType?.takeIf { it.isNotBlank() }?.let {
                                    itemsByProviderId[ProviderIdMatcher.typedKey(it, provider, value)] = match
                                }
                            }
                    }
                    startIndex += pageItemCount
                } while (pageItemCount == pageSize)
                JellyfinResult.Success(
                    JellyfinAvailabilityIndex(
                        moviesCount = moviesCount,
                        seriesCount = seriesCount,
                        lastBuiltAt = System.currentTimeMillis(),
                        sourceServerId = session.server.serverId ?: session.server.localId,
                        sourceServerName = session.server.name,
                        itemsByProviderId = itemsByProviderId,
                    ),
                )
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

}

class SdkJellyfinMediaRepository(
    private val jellyfin: Jellyfin,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinMediaRepository {
    override suspend fun getMediaDetail(session: JellyfinSession, itemId: java.util.UUID): JellyfinResult<JellyfinMediaDetail> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val item by api.userLibraryApi.getItem(userId = session.user.id, itemId = itemId)
                val seasons = if (item.type == BaseItemKind.SERIES) fetchSeasons(api, session, item.id) else emptyList()
                val episodes = if (item.type == BaseItemKind.SERIES) {
                    fetchEpisodes(api, session, item.id, seasons.firstOrNull()?.id)
                } else {
                    emptyList()
                }
                val related = fetchRelated(api, session, item.id)
                val themeSongUrl = fetchThemeSongUrl(api, session, item.id)
                val collectionItems = if (item.type == BaseItemKind.BOX_SET) {
                    fetchCollectionItems(api, session, item.id)
                } else {
                    emptyList()
                }
                JellyfinResult.Success(
                    item.toDetail(
                        api = api,
                        seasons = seasons,
                        episodes = episodes,
                        related = related,
                        themeSongUrl = themeSongUrl,
                        collectionItems = collectionItems,
                    ),
                )
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun getSeasonEpisodes(
        session: JellyfinSession,
        seriesId: java.util.UUID,
        seasonId: java.util.UUID?,
    ): JellyfinResult<List<JellyfinEpisode>> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                JellyfinResult.Success(fetchEpisodes(api, session, seriesId, seasonId, limit = 200))
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun getNextEpisode(
        session: JellyfinSession,
        currentEpisodeId: java.util.UUID,
    ): JellyfinResult<JellyfinUpNextCandidate?> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val current by api.userLibraryApi.getItem(userId = session.user.id, itemId = currentEpisodeId)
                if (current.type != BaseItemKind.EPISODE) return@withContext JellyfinResult.Success(null)
                val seriesId = current.seriesId ?: return@withContext JellyfinResult.Success(null)
                val currentSeason = current.parentIndexNumber ?: Int.MAX_VALUE
                val currentEpisode = current.indexNumber ?: Int.MAX_VALUE
                val episodes = fetchEpisodes(api, session, seriesId, seasonId = null, limit = 1000)
                    .filter { candidate ->
                        candidate.id != currentEpisodeId &&
                            candidate.indexNumber != null &&
                            candidate.seasonIndexNumber != null
                    }
                    .sortedWith(compareBy<JellyfinEpisode> { it.seasonIndexNumber ?: Int.MAX_VALUE }.thenBy { it.indexNumber ?: Int.MAX_VALUE })
                val next = episodes.firstOrNull { episode ->
                    val season = episode.seasonIndexNumber ?: return@firstOrNull false
                    val index = episode.indexNumber ?: return@firstOrNull false
                    season > currentSeason || (season == currentSeason && index > currentEpisode)
                } ?: return@withContext JellyfinResult.Success(null)
                val nextItem by api.userLibraryApi.getItem(userId = session.user.id, itemId = next.id)
                if (nextItem.playAccess == PlayAccess.NONE) return@withContext JellyfinResult.Success(null)
                if (nextItem.mediaSources.orEmpty().isEmpty()) return@withContext JellyfinResult.Success(null)
                JellyfinResult.Success(nextItem.toUpNextCandidate(api))
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun setFavorite(
        session: JellyfinSession,
        itemId: java.util.UUID,
        isFavorite: Boolean,
    ): JellyfinResult<Boolean> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                if (isFavorite) {
                    api.userLibraryApi.markFavoriteItem(itemId, session.user.id)
                } else {
                    api.userLibraryApi.unmarkFavoriteItem(itemId, session.user.id)
                }
                val verifiedUserData by api.itemsApi.getItemUserData(itemId, session.user.id)
                val verifiedFavorite = verifiedUserData.isFavorite == true
                if (verifiedFavorite == isFavorite) {
                    JellyfinResult.Success(verifiedFavorite)
                } else {
                    JellyfinResult.Failure("Couldn't update My List. Check your server connection and try again.")
                }
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toFavoriteUserMessage(throwable), throwable)
            }
        }

    override suspend fun refreshFavoriteState(
        session: JellyfinSession,
        itemId: java.util.UUID,
    ): JellyfinResult<Boolean> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val userData by api.itemsApi.getItemUserData(itemId, session.user.id)
                JellyfinResult.Success(userData.isFavorite == true)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun setPlayed(
        session: JellyfinSession,
        itemId: java.util.UUID,
        isPlayed: Boolean,
    ): JellyfinResult<Boolean> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val userData by if (isPlayed) {
                    api.playStateApi.markPlayedItem(itemId, session.user.id, null)
                } else {
                    api.playStateApi.markUnplayedItem(itemId, session.user.id)
                }
                JellyfinResult.Success(userData.played == true)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun getPersonFilmography(
        session: JellyfinSession,
        personId: java.util.UUID,
    ): JellyfinResult<List<JellyfinMediaItem>> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val response by api.itemsApi.getItems(
                    GetItemsRequest(
                        userId = session.user.id,
                        personIds = listOf(personId),
                        recursive = true,
                        limit = 30,
                        sortBy = listOf(ItemSortBy.SORT_NAME),
                        sortOrder = listOf(SortOrder.ASCENDING),
                        fields = itemFields,
                        includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
                        enableImages = true,
                        imageTypeLimit = 1,
                        enableImageTypes = listOf(ImageType.PRIMARY),
                        enableUserData = true,
                        enableTotalRecordCount = false,
                    ),
                )
                JellyfinResult.Success(response.items.map { it.toMediaItem(api, shapeFor(it.type)) })
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    private suspend fun fetchSeasons(
        api: ApiClient,
        session: JellyfinSession,
        seriesId: java.util.UUID,
    ): List<JellyfinSeason> =
        runCatching {
            val response by api.tvShowsApi.getSeasons(
                GetSeasonsRequest(
                    seriesId = seriesId,
                    userId = session.user.id,
                    fields = itemFields,
                    enableImages = true,
                    imageTypeLimit = 2,
                    enableImageTypes = itemImageTypes,
                    enableUserData = true,
                ),
            )
            response.items.map {
                JellyfinSeason(
                    id = it.id,
                    title = it.name ?: "Season ${it.indexNumber ?: ""}".trim(),
                    indexNumber = it.indexNumber,
                    isPlayed = it.userData?.played == true,
                    imageUrl = it.primaryImageUrl(api, 360),
                    unplayedItemCount = it.userData?.unplayedItemCount ?: 0,
                )
            }
        }.getOrDefault(emptyList())

    private suspend fun fetchEpisodes(
        api: ApiClient,
        session: JellyfinSession,
        seriesId: java.util.UUID,
        seasonId: java.util.UUID?,
        limit: Int = 24,
    ): List<JellyfinEpisode> =
        runCatching {
            val response by api.tvShowsApi.getEpisodes(
                GetEpisodesRequest(
                    seriesId = seriesId,
                    userId = session.user.id,
                    fields = itemFields,
                    seasonId = seasonId,
                    limit = limit,
                    enableImages = true,
                    imageTypeLimit = 2,
                    enableImageTypes = itemImageTypes,
                    enableUserData = true,
                    sortBy = ItemSortBy.INDEX_NUMBER,
                ),
            )
            response.items.map { it.toEpisode(api) }
        }.getOrDefault(emptyList())

    private suspend fun fetchRelated(
        api: ApiClient,
        session: JellyfinSession,
        itemId: java.util.UUID,
    ): List<JellyfinMediaItem> =
        runCatching {
            val response by api.libraryApi.getSimilarItems(
                GetSimilarItemsRequest(
                    itemId = itemId,
                    userId = session.user.id,
                    limit = 16,
                    fields = itemFields,
                ),
            )
            response.items.map { it.toMediaItem(api, shapeFor(it.type)) }
        }.getOrDefault(emptyList())

    private suspend fun fetchCollectionItems(
        api: ApiClient,
        session: JellyfinSession,
        boxSetId: java.util.UUID,
    ): List<JellyfinMediaItem> =
        runCatching {
            val response by api.itemsApi.getItems(
                GetItemsRequest(
                    userId = session.user.id,
                    parentId = boxSetId,
                    recursive = true,
                    limit = 50,
                    sortBy = listOf(ItemSortBy.SORT_NAME),
                    sortOrder = listOf(SortOrder.ASCENDING),
                    fields = itemFields,
                    includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
                    enableImages = true,
                    imageTypeLimit = 1,
                    enableImageTypes = listOf(ImageType.PRIMARY),
                    enableUserData = true,
                    enableTotalRecordCount = false,
                ),
            )
            response.items.map { it.toMediaItem(api, shapeFor(it.type)) }
        }.getOrDefault(emptyList())

    private suspend fun fetchThemeSongUrl(
        api: ApiClient,
        session: JellyfinSession,
        itemId: java.util.UUID,
    ): String? =
        runCatching {
            val response by api.libraryApi.getThemeSongs(
                GetThemeSongsRequest(
                    itemId = itemId,
                    userId = session.user.id,
                    inheritFromParent = true,
                ),
            )
            val themeItem = response.items.firstOrNull() ?: return@runCatching null
            api.universalAudioApi.getUniversalAudioStreamUrl(
                itemId = themeItem.id,
                userId = session.user.id,
                maxStreamingBitrate = 128_000,
                container = listOf("mp3", "aac", "m4a", "flac", "webma", "webm"),
                audioCodec = "aac,mp3",
            ).withAccessToken(session.accessToken)
        }.getOrNull()
}

class SdkJellyfinPlaybackRepository(
    private val jellyfin: Jellyfin,
    private val deviceId: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinPlaybackRepository {
    override suspend fun getPlaybackInfo(
        session: JellyfinSession,
        itemId: java.util.UUID,
        title: String,
        subtitle: String?,
        startPositionTicks: Long,
        forceTranscode: Boolean,
        audioStreamIndex: Int?,
        subtitleStreamIndex: Int?,
        isLiveTv: Boolean,
        maxStreamingBitrate: Int?,
    ): JellyfinResult<JellyfinPlaybackInfo> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val resolvedBitrate = maxStreamingBitrate.validVideoStreamingBitrate()
                val deviceProfile = androidMobileDeviceProfile(maxVideoStreamingBitrate = resolvedBitrate)
                val info = try {
                    getAutoPlaybackInfo(
                        api = api,
                        session = session,
                        itemId = itemId,
                        title = title,
                        subtitle = subtitle,
                        startPositionTicks = startPositionTicks,
                        forceTranscode = forceTranscode,
                        audioStreamIndex = audioStreamIndex,
                        subtitleStreamIndex = subtitleStreamIndex,
                        deviceProfile = deviceProfile,
                        maxStreamingBitrate = resolvedBitrate ?: DEFAULT_MAX_VIDEO_STREAMING_BITRATE,
                    )
                } catch (throwable: Throwable) {
                    if (!isLiveTv) throw throwable
                    getExplicitLiveStreamInfo(
                        api = api,
                        session = session,
                        itemId = itemId,
                        title = title,
                        subtitle = subtitle,
                        startPositionTicks = startPositionTicks,
                        forceTranscode = forceTranscode,
                        audioStreamIndex = audioStreamIndex,
                        subtitleStreamIndex = subtitleStreamIndex,
                        deviceProfile = deviceProfile,
                        maxStreamingBitrate = resolvedBitrate ?: DEFAULT_MAX_VIDEO_STREAMING_BITRATE,
                    )
                }
                JellyfinResult.Success(info)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun getCastPlaybackInfo(
        session: JellyfinSession,
        itemId: java.util.UUID,
        title: String,
        subtitle: String?,
        startPositionTicks: Long,
        forceTranscode: Boolean,
        audioStreamIndex: Int?,
        subtitleStreamIndex: Int?,
        isLiveTv: Boolean,
    ): JellyfinResult<JellyfinPlaybackInfo> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val info = try {
                    getAutoPlaybackInfo(
                        api = api,
                        session = session,
                        itemId = itemId,
                        title = title,
                        subtitle = subtitle,
                        startPositionTicks = startPositionTicks,
                        forceTranscode = forceTranscode,
                        audioStreamIndex = audioStreamIndex,
                        subtitleStreamIndex = subtitleStreamIndex,
                        deviceProfile = googleCastDeviceProfile(),
                        maxStreamingBitrate = 20_000_000,
                        maxAudioChannels = 6,
                    )
                } catch (throwable: Throwable) {
                    if (!isLiveTv) throw throwable
                    getExplicitLiveStreamInfo(
                        api = api,
                        session = session,
                        itemId = itemId,
                        title = title,
                        subtitle = subtitle,
                        startPositionTicks = startPositionTicks,
                        forceTranscode = forceTranscode,
                        audioStreamIndex = audioStreamIndex,
                        subtitleStreamIndex = subtitleStreamIndex,
                        deviceProfile = googleCastDeviceProfile(),
                        maxStreamingBitrate = 20_000_000,
                        maxAudioChannels = 6,
                    )
                }
                JellyfinResult.Success(info)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun reportStarted(
        session: JellyfinSession,
        info: JellyfinPlaybackInfo,
        positionTicks: Long,
    ): JellyfinResult<Unit> =
        report(session) { api ->
            api.playStateApi.reportPlaybackStart(
                PlaybackStartInfo(
                    canSeek = !info.isLiveStream,
                    item = null,
                    itemId = info.itemId,
                    sessionId = null,
                    mediaSourceId = info.mediaSourceId,
                    audioStreamIndex = info.audioStreamIndex,
                    subtitleStreamIndex = info.subtitleStreamIndex,
                    isPaused = false,
                    isMuted = false,
                    positionTicks = positionTicks.coerceAtLeast(0L),
                    playbackStartTimeTicks = null,
                    volumeLevel = 100,
                    brightness = null,
                    aspectRatio = null,
                    playMethod = info.method.toSdkPlayMethod(),
                    liveStreamId = info.liveStreamId,
                    playSessionId = info.playSessionId,
                    repeatMode = RepeatMode.REPEAT_NONE,
                    playbackOrder = PlaybackOrder.DEFAULT,
                    nowPlayingQueue = emptyList(),
                    playlistItemId = null,
                ),
            )
        }

    override suspend fun reportProgress(
        session: JellyfinSession,
        info: JellyfinPlaybackInfo,
        positionTicks: Long,
        isPaused: Boolean,
    ): JellyfinResult<Unit> =
        report(session) { api ->
            api.playStateApi.reportPlaybackProgress(
                PlaybackProgressInfo(
                    canSeek = !info.isLiveStream,
                    item = null,
                    itemId = info.itemId,
                    sessionId = null,
                    mediaSourceId = info.mediaSourceId,
                    audioStreamIndex = info.audioStreamIndex,
                    subtitleStreamIndex = info.subtitleStreamIndex,
                    isPaused = isPaused,
                    isMuted = false,
                    positionTicks = positionTicks.coerceAtLeast(0L),
                    playbackStartTimeTicks = null,
                    volumeLevel = 100,
                    brightness = null,
                    aspectRatio = null,
                    playMethod = info.method.toSdkPlayMethod(),
                    liveStreamId = info.liveStreamId,
                    playSessionId = info.playSessionId,
                    repeatMode = RepeatMode.REPEAT_NONE,
                    playbackOrder = PlaybackOrder.DEFAULT,
                    nowPlayingQueue = emptyList(),
                    playlistItemId = null,
                ),
            )
        }

    override suspend fun reportStopped(
        session: JellyfinSession,
        info: JellyfinPlaybackInfo,
        positionTicks: Long,
    ): JellyfinResult<Unit> =
        report(session) { api ->
            api.playStateApi.reportPlaybackStopped(
                PlaybackStopInfo(
                    item = null,
                    itemId = info.itemId,
                    sessionId = null,
                    mediaSourceId = info.mediaSourceId,
                    positionTicks = positionTicks.coerceAtLeast(0L),
                    liveStreamId = info.liveStreamId,
                    playSessionId = info.playSessionId,
                    failed = false,
                    nextMediaType = null,
                    playlistItemId = null,
                    nowPlayingQueue = emptyList(),
                ),
            )
            info.liveStreamId?.takeIf { info.isLiveStream }?.let { api.mediaInfoApi.closeLiveStream(it) }
        }

    private suspend fun report(
        session: JellyfinSession,
        block: suspend (ApiClient) -> Unit,
    ): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            try {
                block(jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken))
                JellyfinResult.Success(Unit)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    private suspend fun getAutoPlaybackInfo(
        api: ApiClient,
        session: JellyfinSession,
        itemId: java.util.UUID,
        title: String,
        subtitle: String?,
        startPositionTicks: Long,
        forceTranscode: Boolean,
        audioStreamIndex: Int?,
        subtitleStreamIndex: Int?,
        deviceProfile: DeviceProfile = androidMobileDeviceProfile(),
        maxStreamingBitrate: Int = DEFAULT_MAX_VIDEO_STREAMING_BITRATE,
        maxAudioChannels: Int = 8,
    ): JellyfinPlaybackInfo {
        val response by api.mediaInfoApi.getPostedPlaybackInfo(
            itemId,
            PlaybackInfoDto(
                userId = session.user.id,
                maxStreamingBitrate = maxStreamingBitrate,
                startTimeTicks = startPositionTicks.takeIf { it > 0L },
                audioStreamIndex = audioStreamIndex,
                subtitleStreamIndex = subtitleStreamIndex,
                maxAudioChannels = maxAudioChannels,
                mediaSourceId = null,
                liveStreamId = null,
                deviceProfile = deviceProfile,
                enableDirectPlay = !forceTranscode,
                enableDirectStream = true,
                enableTranscoding = true,
                allowVideoStreamCopy = !forceTranscode,
                allowAudioStreamCopy = true,
                autoOpenLiveStream = true,
                alwaysBurnInSubtitleWhenTranscoding = false,
            ),
        )
        response.errorCode?.let { errorCode ->
            throw PlaybackException("Server denied playback: ${errorCode.serialName}")
        }
        val mediaSource = selectMediaSource(response.mediaSources, forceTranscode)
        return buildPlaybackInfo(
            session = session,
            itemId = itemId,
            title = title,
            subtitle = subtitle,
            mediaSource = mediaSource,
            playSessionId = response.playSessionId,
            startPositionTicks = startPositionTicks,
            forceTranscode = forceTranscode,
            audioStreamIndex = audioStreamIndex,
            subtitleStreamIndex = subtitleStreamIndex,
            explicitLiveStream = false,
        )
    }

    private suspend fun getExplicitLiveStreamInfo(
        api: ApiClient,
        session: JellyfinSession,
        itemId: java.util.UUID,
        title: String,
        subtitle: String?,
        startPositionTicks: Long,
        forceTranscode: Boolean,
        audioStreamIndex: Int?,
        subtitleStreamIndex: Int?,
        deviceProfile: DeviceProfile = androidMobileDeviceProfile(),
        maxStreamingBitrate: Int = DEFAULT_MAX_VIDEO_STREAMING_BITRATE,
        maxAudioChannels: Int = 8,
    ): JellyfinPlaybackInfo {
        val profile = deviceProfile
        val response by api.mediaInfoApi.openLiveStream(
            null,
            session.user.id,
            null,
            maxStreamingBitrate,
            startPositionTicks.takeIf { it > 0L },
            audioStreamIndex,
            subtitleStreamIndex,
            maxAudioChannels,
            itemId,
            !forceTranscode,
            true,
            false,
            OpenLiveStreamDto(
                openToken = null,
                userId = session.user.id,
                playSessionId = null,
                maxStreamingBitrate = maxStreamingBitrate,
                startTimeTicks = startPositionTicks.takeIf { it > 0L },
                audioStreamIndex = audioStreamIndex,
                subtitleStreamIndex = subtitleStreamIndex,
                maxAudioChannels = maxAudioChannels,
                itemId = itemId,
                enableDirectPlay = !forceTranscode,
                enableDirectStream = true,
                alwaysBurnInSubtitleWhenTranscoding = false,
                deviceProfile = profile,
                directPlayProtocols = listOf(MediaProtocol.HTTP),
            ),
        )
        val mediaSource = response.mediaSource ?: throw PlaybackException("Live stream unavailable.")
        return buildPlaybackInfo(
            session = session,
            itemId = itemId,
            title = title,
            subtitle = subtitle,
            mediaSource = mediaSource,
            playSessionId = mediaSource.liveStreamId,
            startPositionTicks = startPositionTicks,
            forceTranscode = forceTranscode,
            audioStreamIndex = audioStreamIndex,
            subtitleStreamIndex = subtitleStreamIndex,
            explicitLiveStream = true,
        )
    }

    private fun selectMediaSource(
        mediaSources: List<MediaSourceInfo>?,
        forceTranscode: Boolean,
    ): MediaSourceInfo =
        mediaSources.orEmpty()
            .sortedWith(
                compareByDescending<MediaSourceInfo> { it.supportsDirectPlay && !forceTranscode }
                    .thenByDescending { it.supportsDirectStream && !forceTranscode }
                    .thenByDescending { it.supportsTranscoding },
            )
            .firstOrNull()
            ?: throw PlaybackException("Server did not provide a playable stream.")

    private fun buildPlaybackInfo(
        session: JellyfinSession,
        itemId: java.util.UUID,
        title: String,
        subtitle: String?,
        mediaSource: MediaSourceInfo,
        playSessionId: String?,
        startPositionTicks: Long,
        forceTranscode: Boolean,
        audioStreamIndex: Int?,
        subtitleStreamIndex: Int?,
        explicitLiveStream: Boolean,
    ): JellyfinPlaybackInfo {
        val method = when {
            !forceTranscode && mediaSource.supportsDirectPlay -> JellyfinPlaybackMethod.DirectPlay
            !forceTranscode && mediaSource.supportsDirectStream -> JellyfinPlaybackMethod.DirectStream
            mediaSource.supportsTranscoding -> JellyfinPlaybackMethod.Transcode
            explicitLiveStream -> JellyfinPlaybackMethod.DirectStream
            else -> throw PlaybackException("This media source cannot be direct played or transcoded.")
        }
        val transcodeUrl = mediaSource.transcodingUrl
            ?.let { absoluteServerUrl(session.server.url, it).withAccessToken(session.accessToken) }
        val directUrl = directStreamUrl(session, itemId, mediaSource, playSessionId, startPositionTicks)
        val streamUrl = when (method) {
            JellyfinPlaybackMethod.DirectPlay -> directUrl
            JellyfinPlaybackMethod.DirectStream,
            JellyfinPlaybackMethod.Transcode -> transcodeUrl ?: directUrl
        }.takeIf { it.isNotBlank() } ?: throw PlaybackException("Server did not provide a playable stream.")
        return JellyfinPlaybackInfo(
            itemId = itemId,
            title = title,
            subtitle = subtitle,
            streamUrl = streamUrl,
            fallbackStreamUrl = transcodeUrl?.takeIf {
                method == JellyfinPlaybackMethod.DirectPlay && it != streamUrl
            },
            playSessionId = playSessionId,
            mediaSourceId = mediaSource.id,
            liveStreamId = mediaSource.liveStreamId,
            method = method,
            runtimeTicks = mediaSource.runTimeTicks,
            startPositionTicks = startPositionTicks.coerceAtLeast(0L),
            audioStreamIndex = audioStreamIndex ?: mediaSource.defaultAudioStreamIndex,
            subtitleStreamIndex = subtitleStreamIndex ?: mediaSource.defaultSubtitleStreamIndex,
            audioTracks = mediaSource.mediaStreams.orEmpty()
                .filter { it.type == MediaStreamType.AUDIO }
                .map { it.toAudioTrack() },
            subtitleTracks = mediaSource.mediaStreams.orEmpty()
                .filter { it.type == MediaStreamType.SUBTITLE }
                .map { it.toSubtitleTrack(session) },
            sourceLabel = sourceLabel(mediaSource, method),
            isLiveStream = explicitLiveStream || mediaSource.isInfiniteStream || mediaSource.liveStreamId != null,
        )
    }

    private fun directStreamUrl(
        session: JellyfinSession,
        itemId: java.util.UUID,
        source: MediaSourceInfo,
        playSessionId: String?,
        startPositionTicks: Long,
    ): String {
        val params = buildList {
            add("static=true")
            source.id?.takeIf { it.isNotBlank() }?.let { add("mediaSourceId=${it.urlEncoded()}") }
            add("deviceId=${deviceId.urlEncoded()}")
            playSessionId?.takeIf { it.isNotBlank() }?.let { add("playSessionId=${it.urlEncoded()}") }
            if (startPositionTicks > 0L) add("startTimeTicks=$startPositionTicks")
        }.joinToString("&")
        return "${session.server.url.trimEnd('/')}/Videos/$itemId/stream?$params".withAccessToken(session.accessToken)
    }

    private fun MediaStream.toAudioTrack(): JellyfinAudioTrack =
        JellyfinAudioTrack(
            index = index,
            label = displayTitle ?: title ?: language?.uppercase() ?: "Audio $index",
            language = language,
            codec = codec,
            channels = channels,
            isDefault = isDefault,
        )

    private fun MediaStream.toSubtitleTrack(session: JellyfinSession): JellyfinSubtitleTrack =
        JellyfinSubtitleTrack(
            index = index,
            label = displayTitle ?: title ?: language?.uppercase() ?: "Subtitle $index",
            language = language,
            codec = codec,
            isExternal = isExternal,
            isDefault = isDefault,
            deliveryUrl = deliveryUrl
                ?.let { absoluteServerUrl(session.server.url, it).withAccessToken(session.accessToken) },
        )
}

class SdkJellyfinSearchRepository(
    private val jellyfin: Jellyfin,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinSearchRepository {
    override suspend fun search(
        session: JellyfinSession,
        query: String,
        limit: Int,
    ): JellyfinResult<List<JellyfinSearchResult>> =
        withContext(ioDispatcher) {
            try {
                val trimmed = query.trim()
                if (trimmed.length < 2) return@withContext JellyfinResult.Success(emptyList())
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val hints by api.searchApi.getSearchHints(
                    GetSearchHintsRequest(
                        userId = session.user.id,
                        searchTerm = trimmed,
                        limit = limit,
                        includeMedia = true,
                    ),
                )
                JellyfinResult.Success(hints.searchHints.mapNotNull { it.toSearchResult(api) })
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }
}

class SdkJellyfinFavoritesRepository(
    private val jellyfin: Jellyfin,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinFavoritesRepository {
    override suspend fun getFavorites(session: JellyfinSession, limit: Int): JellyfinResult<List<JellyfinMediaItem>> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val response by api.itemsApi.getItems(
                    GetItemsRequest(
                        userId = session.user.id,
                        recursive = true,
                        limit = limit,
                        isFavorite = true,
                        sortBy = listOf(ItemSortBy.SORT_NAME),
                        sortOrder = listOf(SortOrder.ASCENDING),
                        fields = itemFields,
                        includeItemTypes = mediaItemTypes,
                        enableUserData = true,
                        imageTypeLimit = 2,
                        enableImageTypes = itemImageTypes,
                        enableImages = true,
                        enableTotalRecordCount = false,
                    ),
                )
                JellyfinResult.Success(response.items.map { it.toMediaItem(api, shapeFor(it.type)) })
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }
}

class SdkJellyfinMusicRepository(
    private val jellyfin: Jellyfin,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinMusicRepository {
    override suspend fun getMusicHome(session: JellyfinSession): JellyfinResult<JellyfinMusicHome> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val views by api.userViewsApi.getUserViews(userId = session.user.id)
                val libraries = views.items
                    .filter { it.collectionType?.serialName.equals("music", ignoreCase = true) }
                    .map {
                        JellyfinMusicLibrary(
                            id = it.id,
                            name = it.name ?: "Music",
                            imageUrl = it.primaryImageUrl(api, 520),
                        )
                    }
                val recentlyAdded = getMusicTracks(api, session, limit = 24, sortBy = listOf(ItemSortBy.DATE_CREATED), sortOrder = listOf(SortOrder.DESCENDING))
                val albums = getMusicAlbums(api, session, limit = 40)
                val artists = getMusicArtists(api, session, limit = 40)
                val playlists = getMusicPlaylists(api, session, limit = 40)
                val songs = getMusicTracks(api, session, limit = 60, sortBy = listOf(ItemSortBy.SORT_NAME), sortOrder = listOf(SortOrder.ASCENDING))
                JellyfinResult.Success(
                    JellyfinMusicHome(
                        libraries = libraries,
                        recentlyAdded = recentlyAdded,
                        albums = albums,
                        artists = artists,
                        playlists = playlists,
                        songs = songs,
                    ),
                )
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun getAlbumTracks(session: JellyfinSession, albumId: java.util.UUID): JellyfinResult<List<JellyfinMusicTrack>> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                JellyfinResult.Success(
                    getMusicTracks(
                        api = api,
                        session = session,
                        limit = 500,
                        parentId = albumId,
                        sortBy = listOf(ItemSortBy.PARENT_INDEX_NUMBER, ItemSortBy.INDEX_NUMBER, ItemSortBy.SORT_NAME),
                        sortOrder = listOf(SortOrder.ASCENDING),
                    ),
                )
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun getAlbumTracksPage(
        session: JellyfinSession,
        albumId: java.util.UUID,
        startIndex: Int,
        limit: Int,
    ): JellyfinResult<JellyfinMusicTrackPage> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                JellyfinResult.Success(
                    getMusicTracksPage(
                        api = api,
                        session = session,
                        startIndex = startIndex,
                        limit = limit,
                        parentId = albumId,
                        sortBy = listOf(ItemSortBy.PARENT_INDEX_NUMBER, ItemSortBy.INDEX_NUMBER, ItemSortBy.SORT_NAME),
                        sortOrder = listOf(SortOrder.ASCENDING),
                    ),
                )
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun getArtistAlbums(session: JellyfinSession, artistId: java.util.UUID): JellyfinResult<List<JellyfinMusicAlbum>> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val response by api.itemsApi.getItems(
                    GetItemsRequest(
                        userId = session.user.id,
                        recursive = true,
                        limit = 120,
                        artistIds = listOf(artistId),
                        sortBy = listOf(ItemSortBy.PREMIERE_DATE, ItemSortBy.SORT_NAME),
                        sortOrder = listOf(SortOrder.DESCENDING),
                        fields = musicItemFields,
                        includeItemTypes = listOf(BaseItemKind.MUSIC_ALBUM),
                        enableImages = true,
                        imageTypeLimit = 2,
                        enableImageTypes = listOf(ImageType.PRIMARY),
                        enableTotalRecordCount = false,
                    ),
                )
                JellyfinResult.Success(response.items.map { it.toMusicAlbum(api) })
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun getPlaylistItems(session: JellyfinSession, playlistId: java.util.UUID): JellyfinResult<List<JellyfinMusicTrack>> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val response by api.playlistsApi.getPlaylistItems(
                    GetPlaylistItemsRequest(
                        playlistId = playlistId,
                        userId = session.user.id,
                        fields = musicItemFields,
                        limit = 300,
                        enableImages = true,
                        enableUserData = true,
                        imageTypeLimit = 2,
                        enableImageTypes = listOf(ImageType.PRIMARY),
                    ),
                )
                JellyfinResult.Success(response.items.filter { it.type == BaseItemKind.AUDIO }.map { it.toMusicTrack(api, session) })
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun getPlaylistItemsPage(
        session: JellyfinSession,
        playlistId: java.util.UUID,
        startIndex: Int,
        limit: Int,
    ): JellyfinResult<JellyfinMusicTrackPage> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val safeStart = startIndex.coerceAtLeast(0)
                val response by api.playlistsApi.getPlaylistItems(
                    GetPlaylistItemsRequest(
                        playlistId = playlistId,
                        userId = session.user.id,
                        fields = musicItemFields,
                        startIndex = safeStart,
                        limit = limit,
                        enableImages = true,
                        enableUserData = true,
                        imageTypeLimit = 2,
                        enableImageTypes = listOf(ImageType.PRIMARY),
                    ),
                )
                val tracks = response.items
                    .filter { it.type == BaseItemKind.AUDIO || it.mediaType == MediaType.AUDIO }
                    .map { it.toMusicTrack(api, session) }
                JellyfinResult.Success(
                    JellyfinMusicTrackPage(
                        tracks = tracks,
                        startIndex = safeStart,
                        pageSize = limit,
                        totalItems = response.totalRecordCount,
                    ),
                )
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun getSongsPage(
        session: JellyfinSession,
        startIndex: Int,
        limit: Int,
        filter: MusicSongsFilter,
        alphabetKey: String?,
    ): JellyfinResult<JellyfinMusicTrackPage> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val sortBy = when (filter) {
                    MusicSongsFilter.RecentlyAdded -> listOf(ItemSortBy.DATE_CREATED)
                    MusicSongsFilter.All,
                    MusicSongsFilter.AZ,
                    MusicSongsFilter.Favorites -> listOf(ItemSortBy.SORT_NAME)
                }
                val sortOrder = when (filter) {
                    MusicSongsFilter.RecentlyAdded -> listOf(SortOrder.DESCENDING)
                    MusicSongsFilter.All,
                    MusicSongsFilter.AZ,
                    MusicSongsFilter.Favorites -> listOf(SortOrder.ASCENDING)
                }
                val normalizedKey = alphabetKey?.let {
                    val value = it.trim().uppercase()
                    when {
                        value == "#" -> value
                        value.length == 1 && value[0] in 'A'..'Z' -> value
                        else -> null
                    }
                }
                JellyfinResult.Success(
                    getMusicTracksPage(
                        api = api,
                        session = session,
                        startIndex = startIndex,
                        limit = limit,
                        sortBy = sortBy,
                        sortOrder = sortOrder,
                        isFavorite = filter == MusicSongsFilter.Favorites,
                        nameStartsWith = normalizedKey?.takeIf { it != "#" },
                        nameLessThan = "A".takeIf { normalizedKey == "#" },
                    ),
                )
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun searchMusic(session: JellyfinSession, query: String, limit: Int): JellyfinResult<List<JellyfinMusicTrack>> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                JellyfinResult.Success(
                    getMusicTracks(
                        api = api,
                        session = session,
                        limit = limit,
                        searchTerm = query.takeIf { it.isNotBlank() },
                        sortBy = listOf(ItemSortBy.SORT_NAME),
                        sortOrder = listOf(SortOrder.ASCENDING),
                    ),
                )
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun getLyrics(session: JellyfinSession, trackId: java.util.UUID): JellyfinResult<JellyfinLyrics?> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val response = runCatching { api.lyricsApi.getLyrics(trackId) }.getOrNull()
                    ?: return@withContext JellyfinResult.Success(null)
                val lyric by response
                val lines = lyric.lyrics.orEmpty().mapNotNull { line ->
                    val text = line.text?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    JellyfinLyricLine(startMs = line.start?.toLyricMillis(), text = text)
                }
                val plain = lines.joinToString("\n") { it.text }.trim()
                JellyfinResult.Success(
                    JellyfinLyrics(
                        plainText = plain,
                        syncedLines = lines,
                        source = "Jellyfin Lyrics API",
                    ).takeIf { it.plainText.isNotBlank() || it.syncedLines.isNotEmpty() },
                )
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun createPlaylist(session: JellyfinSession, name: String, itemIds: List<java.util.UUID>): JellyfinResult<java.util.UUID> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val response by api.playlistsApi.createPlaylist(
                    CreatePlaylistDto(
                        name = name.trim().ifBlank { "Vantafyn Playlist" },
                        ids = itemIds,
                        userId = session.user.id,
                        mediaType = MediaType.AUDIO,
                        users = emptyList(),
                        isPublic = false,
                    ),
                )
                val id = response.id?.let { runCatching { java.util.UUID.fromString(it) }.getOrNull() }
                    ?: waitForCreatedMusicPlaylist(api, session, name.trim().ifBlank { "Vantafyn Playlist" })
                    ?: return@withContext JellyfinResult.Failure("Jellyfin created the playlist, but it is not available yet. Please refresh Music in a moment.")
                JellyfinResult.Success(id)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun addToPlaylist(session: JellyfinSession, playlistId: java.util.UUID, itemIds: List<java.util.UUID>): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                api.playlistsApi.addItemToPlaylist(playlistId, itemIds, session.user.id)
                JellyfinResult.Success(Unit)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun removeFromPlaylist(session: JellyfinSession, playlistId: java.util.UUID, playlistItemIds: List<String>): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                api.playlistsApi.removeItemFromPlaylist(playlistId.toString(), playlistItemIds)
                JellyfinResult.Success(Unit)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    private suspend fun getMusicTracks(
        api: ApiClient,
        session: JellyfinSession,
        limit: Int,
        parentId: java.util.UUID? = null,
        searchTerm: String? = null,
        sortBy: List<ItemSortBy>,
        sortOrder: List<SortOrder>,
    ): List<JellyfinMusicTrack> {
        val response by api.itemsApi.getItems(
            GetItemsRequest(
                userId = session.user.id,
                parentId = parentId,
                recursive = parentId == null,
                searchTerm = searchTerm,
                limit = limit,
                sortBy = sortBy,
                sortOrder = sortOrder,
                fields = musicItemFields,
                includeItemTypes = listOf(BaseItemKind.AUDIO),
                mediaTypes = listOf(MediaType.AUDIO),
                enableUserData = true,
                imageTypeLimit = 2,
                enableImageTypes = listOf(ImageType.PRIMARY),
                enableImages = true,
                enableTotalRecordCount = false,
            ),
        )
        return response.items.map { it.toMusicTrack(api, session) }
    }

    private suspend fun getMusicTracksPage(
        api: ApiClient,
        session: JellyfinSession,
        startIndex: Int,
        limit: Int,
        parentId: java.util.UUID? = null,
        searchTerm: String? = null,
        sortBy: List<ItemSortBy>,
        sortOrder: List<SortOrder>,
        isFavorite: Boolean = false,
        nameStartsWith: String? = null,
        nameLessThan: String? = null,
    ): JellyfinMusicTrackPage {
        val safeStart = startIndex.coerceAtLeast(0)
        val response by api.itemsApi.getItems(
            GetItemsRequest(
                userId = session.user.id,
                parentId = parentId,
                recursive = parentId == null,
                searchTerm = searchTerm,
                startIndex = safeStart,
                limit = limit,
                sortBy = sortBy,
                sortOrder = sortOrder,
                fields = musicItemFields,
                includeItemTypes = listOf(BaseItemKind.AUDIO),
                mediaTypes = listOf(MediaType.AUDIO),
                isFavorite = true.takeIf { isFavorite },
                nameStartsWith = nameStartsWith,
                nameLessThan = nameLessThan,
                enableUserData = true,
                imageTypeLimit = 2,
                enableImageTypes = listOf(ImageType.PRIMARY),
                enableImages = true,
                enableTotalRecordCount = true,
            ),
        )
        return JellyfinMusicTrackPage(
            tracks = response.items.map { it.toMusicTrack(api, session) },
            startIndex = safeStart,
            pageSize = limit,
            totalItems = response.totalRecordCount,
        )
    }

    private suspend fun getMusicAlbums(api: ApiClient, session: JellyfinSession, limit: Int): List<JellyfinMusicAlbum> {
        val response by api.itemsApi.getItems(
            GetItemsRequest(
                userId = session.user.id,
                recursive = true,
                limit = limit,
                sortBy = listOf(ItemSortBy.DATE_CREATED),
                sortOrder = listOf(SortOrder.DESCENDING),
                fields = musicItemFields,
                includeItemTypes = listOf(BaseItemKind.MUSIC_ALBUM),
                enableImages = true,
                imageTypeLimit = 2,
                enableImageTypes = listOf(ImageType.PRIMARY),
                enableTotalRecordCount = false,
            ),
        )
        return response.items.map { it.toMusicAlbum(api) }
    }

    private suspend fun getMusicArtists(api: ApiClient, session: JellyfinSession, limit: Int): List<JellyfinMusicArtist> {
        val response by api.itemsApi.getItems(
            GetItemsRequest(
                userId = session.user.id,
                recursive = true,
                limit = limit,
                sortBy = listOf(ItemSortBy.SORT_NAME),
                sortOrder = listOf(SortOrder.ASCENDING),
                fields = musicItemFields,
                includeItemTypes = listOf(BaseItemKind.MUSIC_ARTIST),
                enableImages = true,
                imageTypeLimit = 1,
                enableImageTypes = listOf(ImageType.PRIMARY),
                enableTotalRecordCount = false,
            ),
        )
        return response.items.map { it.toMusicArtist(api) }
    }

    private suspend fun getMusicPlaylists(api: ApiClient, session: JellyfinSession, limit: Int): List<JellyfinMusicPlaylist> {
        val response by api.itemsApi.getItems(
            GetItemsRequest(
                userId = session.user.id,
                recursive = true,
                limit = limit,
                sortBy = listOf(ItemSortBy.SORT_NAME),
                sortOrder = listOf(SortOrder.ASCENDING),
                fields = musicItemFields,
                includeItemTypes = listOf(BaseItemKind.PLAYLIST),
                enableImages = true,
                imageTypeLimit = 1,
                enableImageTypes = listOf(ImageType.PRIMARY),
                enableTotalRecordCount = false,
            ),
        )
        return response.items.mapNotNull { playlist ->
            val playlistId = playlist.id
            val items = runCatching {
                val playlistItems by api.playlistsApi.getPlaylistItems(
                    GetPlaylistItemsRequest(
                        playlistId = playlistId,
                        userId = session.user.id,
                        fields = musicItemFields,
                        limit = 20,
                        enableImages = true,
                        enableUserData = false,
                        imageTypeLimit = 1,
                        enableImageTypes = listOf(ImageType.PRIMARY),
                    ),
                )
                playlistItems.items
            }.getOrElse { emptyList() }
            val hasItems = items.isNotEmpty()
            val audioItems = items.filter { it.type == BaseItemKind.AUDIO || it.mediaType == MediaType.AUDIO }
            val audioCount = audioItems.size
            val videoCount = items.count {
                it.type in setOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.SERIES, BaseItemKind.BOX_SET) ||
                    it.mediaType == MediaType.VIDEO
            }
            when {
                hasItems && audioCount > 0 && videoCount == 0 -> {
                    val trackImageUrls = audioItems.take(4).mapNotNull { it.primaryImageUrl(api, 200) }
                    playlist.toMusicPlaylist(api, trackImageUrls = trackImageUrls)
                }
                hasItems -> null
                else -> null
            }
        }
    }

    private suspend fun waitForCreatedMusicPlaylist(api: ApiClient, session: JellyfinSession, name: String): java.util.UUID? {
        val normalizedName = name.trim()
        repeat(8) { attempt ->
            if (attempt > 0) delay(700L)
            val playlist = getMusicPlaylists(api, session, limit = 80)
                .firstOrNull { it.name.equals(normalizedName, ignoreCase = true) }
            if (playlist != null) return playlist.id
        }
        return null
    }
}

class SdkJellyfinMediaSegmentRepository(
    private val jellyfin: Jellyfin,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinMediaSegmentRepository {
    override suspend fun getItemSegments(session: JellyfinSession, itemId: java.util.UUID): JellyfinResult<List<JellyfinMediaSegment>> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val response by api.mediaSegmentsApi.getItemSegments(itemId)
                JellyfinResult.Success(
                    response.items
                        .mapNotNull { it.toMediaSegment() }
                        .sortedBy { it.startTicks },
                )
            } catch (throwable: Throwable) {
                JellyfinResult.Success(emptyList())
            }
        }
}

class SdkJellyfinUserPreferencesRepository(
    private val jellyfin: Jellyfin,
    private val storage: JellyfinSessionStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinUserPreferencesRepository {
    override suspend fun getPlaybackPreferences(session: JellyfinSession): JellyfinResult<JellyfinUserPlaybackPreferences> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val user by api.userApi.getCurrentUser()
                val config = user.configuration ?: return@withContext JellyfinResult.Failure("Jellyfin did not return user playback preferences")
                JellyfinResult.Success(config.toPlaybackPreferences())
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun updatePlaybackPreferences(
        session: JellyfinSession,
        preferences: JellyfinUserPlaybackPreferences,
    ): JellyfinResult<JellyfinUserPlaybackPreferences> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val user by api.userApi.getCurrentUser()
                val current = user.configuration ?: return@withContext JellyfinResult.Failure("Jellyfin did not return user playback preferences")
                val updated = current.copy(
                    audioLanguagePreference = preferences.audioLanguagePreference?.trim()?.takeIf { it.isNotBlank() },
                    playDefaultAudioTrack = preferences.playDefaultAudioTrack,
                    subtitleLanguagePreference = preferences.subtitleLanguagePreference?.trim()?.takeIf { it.isNotBlank() },
                    subtitleMode = preferences.subtitleMode.toSubtitlePlaybackMode(current.subtitleMode),
                    rememberAudioSelections = preferences.rememberAudioSelections,
                    rememberSubtitleSelections = preferences.rememberSubtitleSelections,
                    enableNextEpisodeAutoPlay = preferences.enableNextEpisodeAutoPlay,
                )
                api.userApi.updateUserConfiguration(session.user.id, updated)
                JellyfinResult.Success(updated.toPlaybackPreferences())
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun changePassword(
        session: JellyfinSession,
        currentPassword: String,
        newPassword: String,
    ): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                api.userApi.updateUserPassword(
                    session.user.id,
                    UpdateUserPassword(
                        currentPassword = currentPassword,
                        currentPw = currentPassword,
                        newPw = newPassword,
                        resetPassword = false,
                    ),
                )
                JellyfinResult.Success(Unit)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun uploadCurrentUserProfileImage(
        session: JellyfinSession,
        upload: JellyfinProfileImageUpload,
    ): JellyfinResult<JellyfinSession> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                api.imageApi.postUserImage(session.user.id, FileInfo(upload.bytes, upload.mimeType))
                JellyfinResult.Success(refreshCurrentSessionUser(api, session))
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toProfileImageUserMessage(throwable), throwable)
            }
        }

    override suspend fun deleteCurrentUserProfileImage(session: JellyfinSession): JellyfinResult<JellyfinSession> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                api.imageApi.deleteUserImage(session.user.id)
                JellyfinResult.Success(refreshCurrentSessionUser(api, session))
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toProfileImageUserMessage(throwable), throwable)
            }
        }

    private suspend fun refreshCurrentSessionUser(api: ApiClient, session: JellyfinSession): JellyfinSession {
        val currentUser by api.userApi.getCurrentUser()
        val refreshed = session.copy(
            user = session.user.copy(
                id = currentUser.id,
                name = currentUser.name ?: session.user.name,
                serverName = currentUser.serverName ?: session.user.serverName,
                primaryImageTag = currentUser.primaryImageTag,
                isAdministrator = currentUser.policy?.isAdministrator == true,
            ),
        )
        storage.write(refreshed.toStoredSession())
        return refreshed
    }
}

class SdkJellyfinAdminRepository(
    private val jellyfin: Jellyfin,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinAdminRepository {
    override suspend fun getOverview(
        session: JellyfinSession,
        libraries: List<JellyfinLibrary>,
    ): JellyfinResult<JellyfinAdminOverview> =
        withContext(ioDispatcher) {
            if (!session.user.isAdministrator) {
                return@withContext JellyfinResult.Failure("Admin access is not available for this user")
            }
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val system by api.systemApi.getSystemInfo()
                val allSessions = runCatching {
                    val result by api.sessionApi.getSessions()
                    result
                }.getOrDefault(emptyList())
                val nowPlayingDetails = loadAdminNowPlayingDetails(
                    api = api,
                    session = session,
                    itemIds = allSessions.mapNotNull { it.nowPlayingItem?.id },
                )
                val sessions = allSessions
                    .filter { it.nowPlayingItem != null }
                    .sortedWith(
                        compareBy(
                            { it.userName.orEmpty().lowercase(Locale.US) },
                            { it.client.orEmpty().lowercase(Locale.US) },
                            { it.deviceName.orEmpty().lowercase(Locale.US) },
                            { it.nowPlayingItem?.name.orEmpty().lowercase(Locale.US) },
                            { it.id ?: it.deviceId ?: it.userId?.toString().orEmpty() },
                        ),
                    )
                    .map { dto ->
                        val sessionItem = dto.nowPlayingItem
                        val technicalItem = sessionItem?.id?.let { nowPlayingDetails[it] }
                        val playState = dto.playState
                        val transcode = dto.transcodingInfo
                        val mediaSource = technicalItem?.mediaSources.orEmpty().firstOrNull()
                        val mediaStreams = mediaSource?.mediaStreams.orEmpty().ifEmpty {
                            technicalItem?.mediaStreams.orEmpty().ifEmpty { sessionItem?.mediaStreams.orEmpty() }
                        }
                        val videoStream = mediaStreams.firstOrNull { it.type == MediaStreamType.VIDEO }
                        val audioStream = mediaStreams.firstOrNull { it.type == MediaStreamType.AUDIO }
                        JellyfinAdminSession(
                            id = dto.id ?: dto.deviceId ?: dto.userId?.toString().orEmpty(),
                            userId = dto.userId,
                            userName = dto.userName,
                            userImageUrl = dto.userId?.let { publicUserImageUrl(api, it, dto.userPrimaryImageTag) },
                            client = dto.client,
                            deviceName = dto.deviceName,
                            remoteEndPoint = dto.remoteEndPoint,
                            nowPlayingTitle = sessionItem?.name,
                            nowPlayingSubtitle = run {
                                val isAudio = sessionItem?.type == BaseItemKind.AUDIO
                                if (isAudio) null else sessionItem?.seasonEpisodeLabel()
                            } ?: sessionItem?.productionYear?.toString(),
                            nowPlayingImageUrl = run {
                                val isEpisode = sessionItem?.type == BaseItemKind.EPISODE
                                if (isEpisode) {
                                    val seriesId = sessionItem?.seriesId
                                    val seriesTag = sessionItem?.seriesPrimaryImageTag
                                    if (seriesId != null && !seriesTag.isNullOrBlank()) {
                                        itemImageUrl(api, seriesId, ImageType.PRIMARY, seriesTag, maxWidth = 420)
                                    } else {
                                        sessionItem?.primaryImageUrl(api, 420)
                                    }
                                } else {
                                    sessionItem?.primaryImageUrl(api, 420) ?: sessionItem?.thumbImageUrl(api, 520)
                                }
                            },
                            nowPlayingBackdropUrl = sessionItem?.backdropImageUrl(api, 760) ?: sessionItem?.thumbImageUrl(api, 760),
                            nowPlayingType = sessionItem?.type?.serialName ?: sessionItem?.type?.name,
                            playMethod = playState?.playMethod?.let { method ->
                                when (method) {
                                    PlayMethod.DIRECT_PLAY -> "Direct Play"
                                    PlayMethod.DIRECT_STREAM -> "Direct Stream"
                                    PlayMethod.TRANSCODE -> "Transcoding"
                                }
                            } ?: if (transcode != null) "Transcoding" else "Unknown",
                            isPaused = playState?.isPaused == true,
                            positionTicks = playState?.positionTicks,
                            runtimeTicks = sessionItem?.runTimeTicks ?: technicalItem?.runTimeTicks,
                            streamQuality = videoStream?.videoQualityLabel(),
                            videoCodec = transcode?.videoCodec ?: videoStream?.codec,
                            audioCodec = transcode?.audioCodec ?: audioStream?.codec,
                            container = transcode?.container ?: mediaSource?.container ?: technicalItem?.container ?: sessionItem?.container,
                            bitrate = transcode?.bitrate ?: mediaSource?.bitrate ?: mediaStreams.sumOf { it.bitRate ?: 0 }.takeIf { it > 0 },
                            transcodeReasons = transcode?.transcodeReasons.orEmpty().map { it.serialName },
                            lastPlaybackCheckIn = dto.lastPlaybackCheckIn?.toString(),
                            isTranscoding = transcode != null || playState?.playMethod == PlayMethod.TRANSCODE,
                            supportsDisplayMessage = dto.supportedCommands.orEmpty().contains(GeneralCommandType.DISPLAY_MESSAGE),
                        )
                    }
                val users = runCatching {
                    val result by api.userApi.getUsers(isHidden = null, isDisabled = null)
                    result.map { user ->
                        JellyfinAdminUser(
                            id = user.id,
                            name = user.name ?: "Unknown",
                            imageUrl = publicUserImageUrl(api, user.id, user.primaryImageTag),
                            isAdministrator = user.policy?.isAdministrator == true,
                            isDisabled = user.policy?.isDisabled == true,
                            isHidden = user.policy?.isHidden == true,
                            lastActivity = user.lastActivityDate?.toString(),
                            lastLogin = user.lastLoginDate?.toString(),
                        )
                    }
                }.getOrDefault(emptyList())
                val plugins = runCatching {
                    val result by api.pluginsApi.getPlugins()
                    result.map {
                        JellyfinAdminPlugin(
                            id = it.id,
                            name = it.name,
                            version = it.version,
                            description = it.description,
                            status = it.status?.serialName ?: it.status?.name,
                            hasImage = it.hasImage,
                            canUninstall = it.canUninstall,
                        )
                    }
                }.getOrElse { sdkThrowable ->
                    Log.w("VantafynAdmin", "SDK plugin list failed, falling back to /Plugins: ${sdkThrowable.javaClass.simpleName}")
                    fetchPluginsFallback(session)
                }
                val tasks = runCatching {
                    val result by api.scheduledTasksApi.getTasks(isHidden = false, isEnabled = null)
                    result.map {
                        JellyfinAdminTask(
                            id = it.id ?: it.key ?: it.name.orEmpty(),
                            name = it.name ?: it.key ?: "Scheduled task",
                            category = it.category,
                            state = it.state?.serialName ?: it.state?.name,
                            progress = it.currentProgressPercentage,
                            lastStatus = it.lastExecutionResult?.status?.serialName ?: it.lastExecutionResult?.status?.name,
                            lastEnded = it.lastExecutionResult?.endTimeUtc?.toString(),
                        )
                    }
                }.getOrDefault(emptyList())
                val recentActivity = runCatching {
                    val result by api.activityLogApi.getLogEntries(startIndex = 0, limit = 8, minDate = null, hasUserId = null)
                    result.items.map {
                        JellyfinAdminActivity(
                            id = it.id,
                            name = it.name,
                            shortOverview = it.shortOverview ?: it.overview,
                            type = it.type,
                            date = it.date?.toString(),
                            severity = it.severity?.serialName ?: it.severity?.name,
                        )
                    }
                }.getOrDefault(emptyList())
                val devices = runCatching {
                    val result by api.devicesApi.getDevices(userId = null)
                    result.items.map {
                        JellyfinAdminDevice(
                            id = it.id.orEmpty(),
                            name = it.customName ?: it.name ?: "Unknown device",
                            appName = it.appName,
                            appVersion = it.appVersion,
                            lastUserName = it.lastUserName,
                            lastActivity = it.dateLastActivity?.toString(),
                            iconUrl = it.iconUrl,
                        )
                    }
                }.getOrDefault(emptyList())
                val logs = runCatching {
                    val result by api.systemApi.getServerLogs()
                    result.map {
                        JellyfinAdminLogFile(
                            name = it.name,
                            sizeBytes = it.size,
                            modified = it.dateModified.toString(),
                        )
                    }
                }.getOrDefault(emptyList())
                val statistics = loadPlaybackReportingStatistics(
                    api = api,
                    session = session,
                    users = users,
                    plugins = plugins,
                    recentActivity = recentActivity,
                    days = 30,
                )
                JellyfinResult.Success(
                    JellyfinAdminOverview(
                        serverName = system.serverName,
                        serverVersion = system.version,
                        operatingSystem = system.operatingSystemDisplayName ?: system.operatingSystem,
                        activeSessions = sessions,
                        connectedSessionCount = allSessions.size,
                        users = users,
                        libraryCount = libraries.size,
                        totalItems = countItems(api, session, null),
                        moviesCount = countItems(api, session, listOf(BaseItemKind.MOVIE)),
                        seriesCount = countItems(api, session, listOf(BaseItemKind.SERIES)),
                        episodesCount = countItems(api, session, listOf(BaseItemKind.EPISODE)),
                        musicCount = countItems(api, session, listOf(BaseItemKind.AUDIO, BaseItemKind.MUSIC_ALBUM)),
                        plugins = plugins,
                        tasks = tasks,
                        recentActivity = recentActivity,
                        devices = devices,
                        serverLogs = logs,
                        statistics = statistics,
                        unavailableStats = if (statistics.capability == JellyfinStatisticsCapability.PlaybackReporting) {
                            emptyList()
                        } else {
                            listOf(statistics.message ?: "Playback Reporting statistics are unavailable.")
                        },
                    ),
                )
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    private suspend fun loadAdminNowPlayingDetails(
        api: ApiClient,
        session: JellyfinSession,
        itemIds: List<java.util.UUID>,
    ): Map<java.util.UUID, org.jellyfin.sdk.model.api.BaseItemDto> {
        val ids = itemIds.distinct().take(24)
        if (ids.isEmpty()) return emptyMap()
        return runCatching {
            val result by api.itemsApi.getItems(
                GetItemsRequest(
                    userId = session.user.id,
                    ids = ids,
                    fields = activeSessionItemFields,
                    enableImages = false,
                    enableUserData = false,
                    enableTotalRecordCount = false,
                ),
            )
            result.items.associateBy { it.id }
        }.getOrDefault(emptyMap())
    }

    private suspend fun loadPlaybackReportingStatistics(
        api: ApiClient,
        session: JellyfinSession,
        users: List<JellyfinAdminUser>,
        plugins: List<JellyfinAdminPlugin>,
        recentActivity: List<JellyfinAdminActivity>,
        days: Int,
    ): JellyfinStatisticsOverview {
        val pluginLooksInstalled = plugins.any { plugin ->
            plugin.name.contains("Playback Reporting", ignoreCase = true) &&
                !plugin.status.equals("Disabled", ignoreCase = true)
        }
        val userImages = buildMap {
            users.forEach { user ->
                put(user.id.toString().lowercase(Locale.US), user.imageUrl)
                put(user.name.lowercase(Locale.US), user.imageUrl)
            }
        }
        val endDate = playbackReportingEndDate()
        val timezoneOffset = playbackReportingTimezoneOffset()
        val fallback = JellyfinStatisticsOverview(
            capability = if (recentActivity.isNotEmpty()) {
                JellyfinStatisticsCapability.CoreActivityOnly
            } else {
                JellyfinStatisticsCapability.Unavailable
            },
            rangeDays = days,
            rangeLabel = "$days days",
            totalWatchTimeSeconds = 0L,
            totalPlayCount = 0,
            mostActiveUser = null,
            mostWatchedTitle = null,
            users = emptyList(),
            media = emptyList(),
            trend = emptyList(),
            recentActivity = recentActivity,
            message = if (pluginLooksInstalled) {
                "Playback Reporting did not return statistics. Check the plugin is enabled and has collected data."
            } else {
                "Install and enable the Jellyfin Playback Reporting plugin for watch-time statistics."
            },
        )

        return runCatching {
            val userActivity = playbackReportingJsonArray(
                session = session,
                pathAndQuery = "user_usage_stats/user_activity?days=$days&endDate=$endDate&timezoneOffset=$timezoneOffset",
            )
            val usersStats = parsePlaybackReportingUsers(userActivity, userImages)
            val enrichedUsersStats = enrichUserContentBreakdown(session, usersStats, days, endDate, timezoneOffset)
            val trend = loadPlaybackReportingTrend(session, days, endDate, timezoneOffset)
            val media = loadPlaybackReportingMediaBreakdown(api, session, days, endDate, timezoneOffset)
            val totalWatchTimeSeconds = usersStats.sumOf { it.totalWatchTimeSeconds }
            val totalPlayCount = usersStats.sumOf { it.playCount }
            JellyfinStatisticsOverview(
                capability = JellyfinStatisticsCapability.PlaybackReporting,
                rangeDays = days,
                rangeLabel = "$days days",
                totalWatchTimeSeconds = totalWatchTimeSeconds,
                totalPlayCount = totalPlayCount,
                mostActiveUser = enrichedUsersStats.maxByOrNull { it.totalWatchTimeSeconds },
                mostWatchedTitle = media.maxWithOrNull(compareBy<JellyfinMediaWatchStats> { it.totalWatchTimeSeconds }.thenBy { it.playCount }),
                users = enrichedUsersStats,
                media = media,
                trend = trend,
                recentActivity = recentActivity,
                message = null,
            )
        }.getOrElse { fallback }
    }

    private fun parsePlaybackReportingUsers(
        array: JSONArray,
        userImages: Map<String, String?>,
    ): List<JellyfinUserWatchStats> =
        (0 until array.length())
            .mapNotNull { index -> array.optJSONObject(index) }
            .map { row ->
                val userIdText = row.optString("user_id").takeIf { it.isNotBlank() }
                val userId = userIdText?.let { raw ->
                    runCatching { java.util.UUID.fromString(raw) }.getOrNull()
                        ?: runCatching {
                            val d = raw.lowercase(Locale.US)
                            java.util.UUID.fromString(
                                "${d.substring(0,8)}-${d.substring(8,12)}-${d.substring(12,16)}-${d.substring(16,20)}-${d.substring(20)}"
                            )
                        }.getOrNull()
                }
                val seconds = row.optLong("total_time", -1L).takeIf { it >= 0L }
                    ?: parsePlaybackReportingDuration(row.optString("total_play_time"))
                JellyfinUserWatchStats(
                    userId = userId,
                    displayName = row.optString("user_name").ifBlank { "Unknown user" },
                    avatarUrl = userIdText?.lowercase(Locale.US)?.let { userImages[it] }
                        ?: row.optString("user_name").takeIf { it.isNotBlank() }?.lowercase(Locale.US)?.let { userImages[it] },
                    totalWatchTimeSeconds = seconds,
                    playCount = row.optInt("total_count", 0),
                    lastWatchedAt = row.optString("latest_date").takeIf { it.isNotBlank() },
                    lastWatchedTitle = row.optString("item_name").takeIf { it.isNotBlank() },
                    lastClient = row.optString("client_name").takeIf { it.isNotBlank() },
                    rank = 0,
                )
            }
            .sortedWith(compareByDescending<JellyfinUserWatchStats> { it.totalWatchTimeSeconds }.thenByDescending { it.playCount })
            .mapIndexed { index, stat -> stat.copy(rank = index + 1) }

    private fun enrichUserContentBreakdown(
        session: JellyfinSession,
        users: List<JellyfinUserWatchStats>,
        days: Int,
        endDate: String,
        timezoneOffset: String,
    ): List<JellyfinUserWatchStats> {
        if (users.isEmpty()) return users
        val typeFilters = listOf("Movie" to "moviesCount", "Episode" to "episodesCount", "Audio" to "audioCount")
        val countsByUserAndType = mutableMapOf<String, MutableMap<String, Int>>()
        for ((typeName, _) in typeFilters) {
            val encodedType = URLEncoder.encode(typeName, Charsets.UTF_8.name())
            runCatching {
                val rows = playbackReportingJsonArray(
                    session = session,
                    pathAndQuery = "user_usage_stats/PlayActivity?filter=$encodedType&days=$days&endDate=$endDate&dataType=count&timezoneOffset=$timezoneOffset",
                )
                for (i in 0 until rows.length()) {
                    val row = rows.optJSONObject(i) ?: continue
                    val rawUserId = row.optString("user_id").takeIf { it.isNotBlank() } ?: continue
                    val normalizedUserId = runCatching { java.util.UUID.fromString(rawUserId) }.getOrNull()?.toString()
                        ?: runCatching {
                            val d = rawUserId.lowercase(Locale.US)
                            java.util.UUID.fromString(
                                "${d.substring(0,8)}-${d.substring(8,12)}-${d.substring(12,16)}-${d.substring(16,20)}-${d.substring(20)}"
                            ).toString()
                        }.getOrNull() ?: rawUserId
                    val usage = row.optJSONObject("user_usage")
                    val count = if (usage != null) {
                        var total = 0
                        usage.keys().forEach { date -> total += usage.optInt(date, 0) }
                        total
                    } else {
                        row.optInt("total_count", row.optInt("count", 0))
                    }
                    if (count > 0) {
                        countsByUserAndType.getOrPut(normalizedUserId) { mutableMapOf() }[typeName] =
                            (countsByUserAndType[normalizedUserId]?.get(typeName) ?: 0) + count
                    }
                }
            }
        }
        if (countsByUserAndType.isEmpty()) return users
        return users.map { user ->
            val key = user.userId?.toString() ?: return@map user
            val counts = countsByUserAndType[key] ?: return@map user
            user.copy(
                moviesCount = counts["Movie"] ?: 0,
                episodesCount = counts["Episode"] ?: 0,
                audioCount = counts["Audio"] ?: 0,
            )
        }
    }

    private fun loadPlaybackReportingTrend(
        session: JellyfinSession,
        days: Int,
        endDate: String,
        timezoneOffset: String,
    ): List<JellyfinWatchTimeBucket> =
        runCatching {
            val filters = runCatching {
                playbackReportingJsonArray(session, "user_usage_stats/type_filter_list")
                    .let { array -> (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) } }
                    .joinToString(",")
            }.getOrDefault("Movie,Episode,Audio")
            val encodedFilters = URLEncoder.encode(filters, Charsets.UTF_8.name())
            val rows = playbackReportingJsonArray(
                session = session,
                pathAndQuery = "user_usage_stats/PlayActivity?filter=$encodedFilters&days=$days&endDate=$endDate&dataType=time&timezoneOffset=$timezoneOffset",
            )
            val totalsByDate = linkedMapOf<String, Long>()
            for (rowIndex in 0 until rows.length()) {
                val usage = rows.optJSONObject(rowIndex)?.optJSONObject("user_usage") ?: continue
                usage.keys().forEach { date ->
                    totalsByDate[date] = (totalsByDate[date] ?: 0L) + usage.optLong(date, 0L)
                }
            }
            totalsByDate.entries
                .sortedBy { it.key }
                .takeLast(14)
                .map { (date, seconds) ->
                    JellyfinWatchTimeBucket(
                        label = date.substringAfterLast("-").ifBlank { date },
                        watchTimeSeconds = seconds,
                        playCount = 0,
                    )
                }
        }.getOrDefault(emptyList())

    private suspend fun loadPlaybackReportingMediaBreakdown(
        api: ApiClient,
        session: JellyfinSession,
        days: Int,
        endDate: String,
        timezoneOffset: String,
    ): List<JellyfinMediaWatchStats> =
        runCatching {
            val rows = playbackReportingJsonArray(
                session = session,
                pathAndQuery = "user_usage_stats/ItemName/BreakdownReport?days=$days&endDate=$endDate&timezoneOffset=$timezoneOffset",
            )
            val stats = (0 until rows.length())
                .mapNotNull { index -> rows.optJSONObject(index) }
                .mapNotNull { row ->
                    val title = row.firstString("label", "name", "item_name", "ItemName", "Name").takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    val itemId = row.playbackReportingItemId()
                    JellyfinMediaWatchStats(
                        itemId = itemId,
                        title = title,
                        type = row.firstString("type", "item_type", "Type").takeIf { it.isNotBlank() },
                        posterUrl = null,
                        logoUrl = null,
                        playCount = row.firstInt("count", "total_count", "TotalCount", "plays", "Value"),
                        totalWatchTimeSeconds = row.firstLong("total_time", "duration", "Duration", "TotalTime", "time", "Time"),
                        uniqueUsers = row.firstIntOrNull("users", "unique_users", "UniqueUsers"),
                        lastWatchedAt = row.firstString("latest_date", "LastSeen", "last_seen").takeIf { it.isNotBlank() },
                    )
                }
                .sortedWith(compareByDescending<JellyfinMediaWatchStats> { it.totalWatchTimeSeconds }.thenByDescending { it.playCount })
                .take(12)
            val artworkById = loadPlaybackReportingArtwork(api, session, stats.mapNotNull { it.itemId })
            val missingArtwork = stats.filter { item -> item.itemId?.let { artworkById[it]?.first } == null }
            val artworkByTitle = loadPlaybackReportingArtworkByTitle(api, session, missingArtwork)
            stats.map { item ->
                val art = item.itemId?.let { artworkById[it] }
                val titleArt = artworkByTitle[item.title.normalizedPlaybackReportingTitle()]
                item.copy(
                    posterUrl = art?.first ?: titleArt?.first,
                    logoUrl = art?.second ?: titleArt?.second,
                )
            }
        }.getOrDefault(emptyList())

    private suspend fun loadPlaybackReportingArtwork(
        api: ApiClient,
        session: JellyfinSession,
        itemIds: List<java.util.UUID>,
    ): Map<java.util.UUID, Pair<String?, String?>> {
        val ids = itemIds.distinct().take(36)
        if (ids.isEmpty()) return emptyMap()
        return runCatching {
            val result by api.itemsApi.getItems(
                GetItemsRequest(
                    userId = session.user.id,
                    ids = ids,
                    fields = itemFields,
                    imageTypeLimit = 2,
                    enableImageTypes = itemImageTypes,
                    enableImages = true,
                    enableUserData = false,
                    enableTotalRecordCount = false,
                ),
            )
            val items = result.items
            val missingAlbumIds = items
                .filter { it.primaryImageUrl(api, 260) == null }
                .mapNotNull { it.albumId ?: it.parentId }
                .distinct()

            val albumArtById = if (missingAlbumIds.isNotEmpty()) {
                runCatching {
                    val albumResult by api.itemsApi.getItems(
                        GetItemsRequest(
                            userId = session.user.id,
                            ids = missingAlbumIds,
                            fields = listOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO),
                            imageTypeLimit = 1,
                            enableImageTypes = listOf(ImageType.PRIMARY, ImageType.THUMB),
                            enableImages = true,
                            enableUserData = false,
                            enableTotalRecordCount = false,
                        ),
                    )
                    albumResult.items.associate { albumItem ->
                        albumItem.id to (albumItem.primaryImageUrl(api, 260) ?: albumItem.thumbImageUrl(api, 360))
                    }
                }.getOrDefault(emptyMap())
            } else {
                emptyMap()
            }

            items.associate { item ->
                val logoTag = item.imageTags?.get(ImageType.LOGO)?.takeIf { it.isNotBlank() }
                val logoUrl = logoTag?.let { itemImageUrl(api, item.id, ImageType.LOGO, it, maxWidth = 420) }
                val directPosterUrl: String? = item.primaryImageUrl(api, 260) ?: item.thumbImageUrl(api, 360) ?: item.backdropImageUrl(api, 360)
                val fallbackAlbumPosterUrl: String? = if (directPosterUrl == null) {
                    val aId = item.albumId ?: item.parentId
                    aId?.let { albumArtById[it] }
                } else null
                val posterUrl = directPosterUrl ?: fallbackAlbumPosterUrl
                item.id to (posterUrl to logoUrl)
            }
        }.getOrDefault(emptyMap())
    }

    private suspend fun loadPlaybackReportingArtworkByTitle(
        api: ApiClient,
        session: JellyfinSession,
        items: List<JellyfinMediaWatchStats>,
    ): Map<String, Pair<String?, String?>> {
        if (items.isEmpty()) return emptyMap()
        val albumIdByTitle = mutableMapOf<String, java.util.UUID>()
        val results = items.take(16).mapNotNull { stats ->
            runCatching {
                val candidates = stats.title.playbackReportingSearchCandidates()
                val isAudio = stats.type?.lowercase(Locale.US) in setOf("audio", "song", "track", "music")
                val normalizedTitle = stats.title.normalizedPlaybackReportingTitle()

                var foundPosterUrl: String? = null
                var foundLogoUrl: String? = null
                var candidateAlbumId: java.util.UUID? = null

                for (query in candidates) {
                    if (foundPosterUrl != null) break

                    val itemsResponse = runCatching {
                        val response by api.itemsApi.getItems(
                            GetItemsRequest(
                                userId = session.user.id,
                                searchTerm = query,
                                includeItemTypes = listOf(
                                    BaseItemKind.AUDIO,
                                    BaseItemKind.MUSIC_ALBUM,
                                    BaseItemKind.MOVIE,
                                    BaseItemKind.SERIES,
                                    BaseItemKind.EPISODE,
                                ),
                                recursive = true,
                                limit = 8,
                                fields = itemFields,
                                enableImageTypes = itemImageTypes,
                                enableImages = true,
                                enableUserData = false,
                                enableTotalRecordCount = false,
                            ),
                        )
                        response.items
                    }.getOrDefault(emptyList())

                    val matchedItem = itemsResponse.firstOrNull { it.name?.normalizedPlaybackReportingTitle() == normalizedTitle }
                        ?: if (isAudio) itemsResponse.firstOrNull { it.type == BaseItemKind.AUDIO || it.type == BaseItemKind.MUSIC_ALBUM }
                        else itemsResponse.firstOrNull()

                    if (matchedItem != null) {
                        foundLogoUrl = matchedItem.logoImageUrl(api, 420)
                        val directArt = matchedItem.primaryImageUrl(api, 260) ?: matchedItem.thumbImageUrl(api, 360) ?: matchedItem.backdropImageUrl(api, 360)
                        if (directArt != null) {
                            foundPosterUrl = directArt
                            break
                        }
                        if (matchedItem.albumId != null) {
                            candidateAlbumId = matchedItem.albumId
                        }
                    }

                    if (foundPosterUrl == null) {
                        val hintsResponse = runCatching {
                            val response by api.searchApi.getSearchHints(
                                GetSearchHintsRequest(
                                    userId = session.user.id,
                                    searchTerm = query,
                                    includeItemTypes = listOf(
                                        BaseItemKind.MOVIE,
                                        BaseItemKind.SERIES,
                                        BaseItemKind.EPISODE,
                                        BaseItemKind.AUDIO,
                                        BaseItemKind.MUSIC_ALBUM,
                                    ),
                                    limit = 8,
                                    mediaTypes = emptyList(),
                                ),
                            )
                            response.searchHints
                        }.getOrDefault(emptyList())

                        val matchedHint = hintsResponse.firstOrNull { it.name?.normalizedPlaybackReportingTitle() == normalizedTitle }
                            ?: if (isAudio) hintsResponse.firstOrNull { it.albumId != null || it.primaryImageTag != null }
                            else hintsResponse.firstOrNull()

                        if (matchedHint != null) {
                            val hintItemId = matchedHint.itemId ?: matchedHint.id
                            val hintPrimaryTag = matchedHint.primaryImageTag
                            val hintThumbTag = matchedHint.thumbImageTag
                            val hintBackdropTag = matchedHint.backdropImageTag

                            val hintPoster: String? = hintItemId?.let { hId ->
                                hintPrimaryTag?.takeIf { it.isNotBlank() }?.let {
                                    itemImageUrl(api, hId, ImageType.PRIMARY, it, maxWidth = 260)
                                } ?: hintThumbTag?.takeIf { it.isNotBlank() }?.let {
                                    itemImageUrl(api, hId, ImageType.THUMB, it, maxWidth = 360)
                                } ?: hintBackdropTag?.takeIf { it.isNotBlank() }?.let {
                                    itemImageUrl(api, hId, ImageType.BACKDROP, it, maxWidth = 360, index = 0)
                                }
                            }
                            if (hintPoster != null) {
                                foundPosterUrl = hintPoster
                                break
                            }
                            if (matchedHint.albumId != null) {
                                candidateAlbumId = matchedHint.albumId
                            }
                        }
                    }
                }

                if (foundPosterUrl == null && candidateAlbumId != null) {
                    albumIdByTitle[normalizedTitle] = candidateAlbumId
                }
                normalizedTitle to (foundPosterUrl to foundLogoUrl)
            }.getOrNull()
        }.toMap()

        if (albumIdByTitle.isNotEmpty()) {
            val albumArt = loadPlaybackReportingArtwork(api, session, albumIdByTitle.values.distinct().take(36))
            return results.mapValues { (title, pair) ->
                if (pair.first != null) return@mapValues pair
                val albumId = albumIdByTitle[title]
                val artFromAlbum = albumId?.let { albumArt[it]?.first }
                pair.copy(first = artFromAlbum ?: pair.first)
            }
        }
        return results
    }

    private fun playbackReportingJsonArray(session: JellyfinSession, pathAndQuery: String): JSONArray {
        val text = playbackReportingGet(session, pathAndQuery)
        return when {
            text.trim().startsWith("[") -> JSONArray(text)
            else -> JSONObject(text).optJSONArray("items")
                ?: JSONObject(text).optJSONArray("Items")
                ?: JSONArray()
        }
    }

    private fun fetchPluginsFallback(session: JellyfinSession): List<JellyfinAdminPlugin> =
        runCatching {
            val text = authenticatedJsonGet(session, "Plugins")
            val plugins = when {
                text.trim().startsWith("[") -> JSONArray(text)
                else -> JSONObject(text).optJSONArray("Items")
                    ?: JSONObject(text).optJSONArray("items")
                    ?: JSONArray()
            }
            (0 until plugins.length())
                .mapNotNull { index -> plugins.optJSONObject(index) }
                .mapNotNull { plugin ->
                    val id = plugin.firstString("Id", "id")
                        .let { runCatching { java.util.UUID.fromString(it) }.getOrNull() }
                        ?: return@mapNotNull null
                    JellyfinAdminPlugin(
                        id = id,
                        name = plugin.firstString("Name", "name").ifBlank { "Plugin" },
                        version = plugin.firstString("Version", "version").takeIf { it.isNotBlank() },
                        description = plugin.firstString("Description", "description").takeIf { it.isNotBlank() },
                        status = plugin.firstString("Status", "status").takeIf { it.isNotBlank() },
                        hasImage = plugin.optBoolean("HasImage", plugin.optBoolean("hasImage", false)),
                        canUninstall = plugin.optBoolean("CanUninstall", plugin.optBoolean("canUninstall", false)),
                    )
                }
                .sortedWith(compareByDescending<JellyfinAdminPlugin> { it.isEnabled }.thenBy { it.name.lowercase(Locale.US) })
        }.getOrElse { throwable ->
            Log.w("VantafynAdmin", "Plugin fallback failed: ${throwable.javaClass.simpleName}")
            emptyList()
        }

    private fun playbackReportingGet(session: JellyfinSession, pathAndQuery: String): String {
        return authenticatedJsonGet(session, pathAndQuery).also { }
    }

    private fun authenticatedJsonGet(session: JellyfinSession, pathAndQuery: String): String {
        val base = session.server.url.trimEnd('/')
        val connection = URL("$base/$pathAndQuery").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 12_000
        connection.readTimeout = 18_000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("X-Emby-Token", session.accessToken)
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (code !in 200..299) {
            throw IllegalStateException("Jellyfin request failed with HTTP $code")
        }
        return body
    }

    private fun playbackReportingEndDate(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(Calendar.getInstance().time)
    }

    private fun playbackReportingTimezoneOffset(): String {
        val offset = TimeZone.getDefault().getOffset(System.currentTimeMillis()).toFloat() / 3_600_000f
        return if (offset % 1f == 0f) offset.toInt().toString() else String.format(Locale.US, "%.1f", offset)
    }

    private fun parsePlaybackReportingDuration(text: String): Long {
        if (text.isBlank()) return 0L
        val lower = text.lowercase(Locale.US)
        if (lower.contains("<") && lower.contains("minute")) return 30L
        val pattern = Regex("""(\d+)\s*(day|hour|minute|min|second|sec)s?""")
        return pattern.findAll(lower).sumOf { match ->
            val value = match.groupValues[1].toLongOrNull() ?: 0L
            when (match.groupValues[2]) {
                "day" -> value * 86_400L
                "hour" -> value * 3_600L
                "minute", "min" -> value * 60L
                else -> value
            }
        }
    }

    override suspend fun getUserDetail(
        session: JellyfinSession,
        userId: java.util.UUID,
    ): JellyfinResult<JellyfinAdminUserDetail> =
        withContext(ioDispatcher) {
            if (!session.user.isAdministrator) {
                return@withContext JellyfinResult.Failure("Admin access is not available for this user")
            }
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val user by api.userApi.getUserById(userId)
                JellyfinResult.Success(user.toAdminUserDetail(api))
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun createUser(
        session: JellyfinSession,
        username: String,
        password: String,
    ): JellyfinResult<JellyfinAdminUserDetail> =
        withContext(ioDispatcher) {
            if (!session.user.isAdministrator) {
                return@withContext JellyfinResult.Failure("Admin access is not available for this user")
            }
            val trimmedName = username.trim()
            if (trimmedName.isBlank()) {
                return@withContext JellyfinResult.Failure("Enter a user name")
            }
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val created by api.userApi.createUserByName(CreateUserByName(name = trimmedName, password = password))
                val refreshed by api.userApi.getUserById(created.id)
                JellyfinResult.Success(refreshed.toAdminUserDetail(api))
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun updateUserPolicy(
        session: JellyfinSession,
        userId: java.util.UUID,
        isHidden: Boolean?,
        isDisabled: Boolean?,
        isAdministrator: Boolean?,
        enableAllFolders: Boolean?,
        enabledFolderIds: List<java.util.UUID>?,
    ): JellyfinResult<JellyfinAdminUserDetail> =
        withContext(ioDispatcher) {
            if (!session.user.isAdministrator) {
                return@withContext JellyfinResult.Failure("Admin access is not available for this user")
            }
            if (userId == session.user.id && (isDisabled == true || isAdministrator == false)) {
                return@withContext JellyfinResult.Failure("Vantafyn will not disable your current admin profile or remove its admin role")
            }
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val user by api.userApi.getUserById(userId)
                val currentPolicy = user.policy ?: return@withContext JellyfinResult.Failure("Jellyfin did not return a policy for this user")
                val updatedPolicy = currentPolicy.copy(
                    isAdministrator = isAdministrator ?: currentPolicy.isAdministrator,
                    isHidden = isHidden ?: currentPolicy.isHidden,
                    isDisabled = isDisabled ?: currentPolicy.isDisabled,
                    enableAllFolders = enableAllFolders ?: currentPolicy.enableAllFolders,
                    enabledFolders = enabledFolderIds ?: currentPolicy.enabledFolders,
                )
                api.userApi.updateUserPolicy(userId, updatedPolicy)
                val refreshed by api.userApi.getUserById(userId)
                JellyfinResult.Success(refreshed.toAdminUserDetail(api))
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun resetUserPassword(
        session: JellyfinSession,
        userId: java.util.UUID,
        newPassword: String,
    ): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            if (!session.user.isAdministrator) {
                return@withContext JellyfinResult.Failure("Admin access is not available for this user")
            }
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                api.userApi.updateUserPassword(
                    userId,
                    UpdateUserPassword(
                        currentPassword = null,
                        currentPw = null,
                        newPw = newPassword,
                        resetPassword = true,
                    ),
                )
                JellyfinResult.Success(Unit)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun uploadUserProfileImage(
        session: JellyfinSession,
        userId: java.util.UUID,
        upload: JellyfinProfileImageUpload,
    ): JellyfinResult<JellyfinAdminUserDetail> =
        withContext(ioDispatcher) {
            if (!session.user.isAdministrator) {
                return@withContext JellyfinResult.Failure("You don't have permission to change this profile picture.")
            }
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                api.imageApi.postUserImage(userId, FileInfo(upload.bytes, upload.mimeType))
                val refreshed by api.userApi.getUserById(userId)
                JellyfinResult.Success(refreshed.toAdminUserDetail(api))
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toProfileImageUserMessage(throwable), throwable)
            }
        }

    override suspend fun deleteUserProfileImage(
        session: JellyfinSession,
        userId: java.util.UUID,
    ): JellyfinResult<JellyfinAdminUserDetail> =
        withContext(ioDispatcher) {
            if (!session.user.isAdministrator) {
                return@withContext JellyfinResult.Failure("You don't have permission to change this profile picture.")
            }
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                api.imageApi.deleteUserImage(userId)
                val refreshed by api.userApi.getUserById(userId)
                JellyfinResult.Success(refreshed.toAdminUserDetail(api))
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toProfileImageUserMessage(throwable), throwable)
            }
        }

    override suspend fun setPluginEnabled(
        session: JellyfinSession,
        pluginId: java.util.UUID,
        version: String,
        enabled: Boolean,
    ): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            if (!session.user.isAdministrator) {
                return@withContext JellyfinResult.Failure("Admin access is not available for this user")
            }
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                if (enabled) {
                    api.pluginsApi.enablePlugin(pluginId, version)
                } else {
                    api.pluginsApi.disablePlugin(pluginId, version)
                }
                JellyfinResult.Success(Unit)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun scanLibrary(session: JellyfinSession): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            if (!session.user.isAdministrator) {
                return@withContext JellyfinResult.Failure("Admin access is not available for this user")
            }
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                api.libraryApi.refreshLibrary()
                JellyfinResult.Success(Unit)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun setScheduledTaskRunning(
        session: JellyfinSession,
        taskId: String,
        running: Boolean,
    ): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            if (!session.user.isAdministrator) {
                return@withContext JellyfinResult.Failure("Admin access is not available for this user")
            }
            if (taskId.isBlank()) {
                return@withContext JellyfinResult.Failure("Scheduled task is missing an id")
            }
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                if (running) {
                    api.scheduledTasksApi.startTask(taskId)
                } else {
                    api.scheduledTasksApi.stopTask(taskId)
                }
                JellyfinResult.Success(Unit)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun sendSessionMessage(
        session: JellyfinSession,
        targetSessionId: String,
        header: String?,
        text: String,
        timeoutMs: Long,
    ): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            if (!session.user.isAdministrator) {
                return@withContext JellyfinResult.Failure("Admin access is not available for this user")
            }
            if (targetSessionId.isBlank()) {
                return@withContext JellyfinResult.Failure("This active session cannot receive messages")
            }
            val trimmedText = text.trim()
            if (trimmedText.isBlank()) {
                return@withContext JellyfinResult.Failure("Enter a message")
            }
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                api.sessionApi.sendMessageCommand(
                    targetSessionId,
                    MessageCommand(
                        header = header?.trim()?.takeIf { it.isNotBlank() },
                        text = trimmedText,
                        timeoutMs = timeoutMs.coerceIn(1_500L, 60_000L),
                    ),
                )
                JellyfinResult.Success(Unit)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    private suspend fun countItems(
        api: ApiClient,
        session: JellyfinSession,
        types: List<BaseItemKind>?,
    ): Int? =
        runCatching {
            val response by api.itemsApi.getItems(
                GetItemsRequest(
                    userId = session.user.id,
                    recursive = true,
                    limit = 1,
                    includeItemTypes = types,
                    enableTotalRecordCount = true,
                    enableImages = false,
                ),
            )
            response.totalRecordCount
        }.getOrNull()
}

class SdkJellyfinHomeRepository(
    private val jellyfin: Jellyfin,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinHomeRepository {
    override suspend fun getHome(
        session: JellyfinSession,
        libraries: List<JellyfinLibrary>,
    ): JellyfinResult<JellyfinHome> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val resumeItems = runCatching {
                    val response by api.itemsApi.getResumeItems(
                        GetResumeItemsRequest(
                            userId = session.user.id,
                            limit = 12,
                            fields = homeFields,
                            enableUserData = true,
                            imageTypeLimit = 2,
                            enableImageTypes = homeImageTypes,
                            includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE),
                            enableImages = true,
                        ),
                    )
                    response.items
                }.getOrDefault(emptyList())
                val nextUpItems = runCatching {
                    val response by api.tvShowsApi.getNextUp(
                        GetNextUpRequest(
                            userId = session.user.id,
                            limit = 12,
                            fields = homeFields,
                            enableImages = true,
                            imageTypeLimit = 2,
                            enableImageTypes = homeImageTypes,
                            enableUserData = true,
                            enableTotalRecordCount = false,
                        ),
                    )
                    response.items
                }.getOrDefault(emptyList())
                val latestMovies = latest(api, session.user.id, listOf(BaseItemKind.MOVIE), 18)
                val latestTv = latest(api, session.user.id, listOf(BaseItemKind.EPISODE, BaseItemKind.SERIES), 18)
                val liveTvChannels = fetchLiveTvChannels(api, session, 24)
                val liveTvPrograms = fetchLiveTvPrograms(api, session, 24)
                val smartSections = listOfNotNull(
                    smartRow(api, session.user.id, title = "New in Crime", genres = listOf("Crime")),
                    smartRow(api, session.user.id, title = "New in Thrillers", genres = listOf("Thriller", "Thrillers")),
                    smartRow(api, session.user.id, title = "New in Comedy", genres = listOf("Comedy")),
                    smartRow(api, session.user.id, title = "New in Action", genres = listOf("Action")),
                    smartRow(api, session.user.id, title = "New in Horror", genres = listOf("Horror")),
                    smartRow(api, session.user.id, title = "New in Drama", genres = listOf("Drama")),
                    smartRow(api, session.user.id, title = "Highly Rated", minCommunityRating = 7.5),
                    smartRow(api, session.user.id, title = "Family Friendly", maxOfficialRating = "PG"),
                    smartRow(api, session.user.id, title = "Unwatched Movies", types = listOf(BaseItemKind.MOVIE), isPlayed = false),
                    smartRow(api, session.user.id, title = "Unwatched TV", types = listOf(BaseItemKind.SERIES, BaseItemKind.EPISODE), isPlayed = false),
                    smartRow(api, session.user.id, title = "Recently Released Movies", types = listOf(BaseItemKind.MOVIE), sortBy = listOf(ItemSortBy.PREMIERE_DATE)),
                    smartRow(api, session.user.id, title = "Recently Released TV", types = listOf(BaseItemKind.SERIES), sortBy = listOf(ItemSortBy.PREMIERE_DATE)),
                )
                val libraryCards = libraries.map {
                    JellyfinMediaCard(
                        id = it.id,
                        title = it.name,
                        subtitle = it.collectionType?.replaceFirstChar(Char::titlecase),
                        year = null,
                        itemType = it.collectionType,
                        imageUrl = it.imageUrl,
                        backdropUrl = null,
                        progress = null,
                        shape = JellyfinMediaCardShape.Library,
                    )
                }
                val heroSeed = System.currentTimeMillis()
                val heroItems = (resumeItems + latestMovies + nextUpItems + latestTv)
                    .distinctBy { it.heroDedupeKey() }
                    .shuffled(kotlin.random.Random(heroSeed))
                    .map { it.toHero(api) }
                    .filter { it.backdropUrl != null || it.posterUrl != null }
                    .take(10)
                val sections = buildList {
                    val continueItems = (resumeItems + nextUpItems)
                        .distinctBy { it.id }
                        .map { it.toCard(api, JellyfinMediaCardShape.Wide) }
                    if (continueItems.isNotEmpty()) add(JellyfinHomeSection("Continue Watching & Next Up", continueItems))
                    if (latestMovies.isNotEmpty()) add(JellyfinHomeSection("Recently Added Movies", latestMovies.map { it.toCard(api, JellyfinMediaCardShape.Poster) }))
                    if (latestTv.isNotEmpty()) add(JellyfinHomeSection("Recently Added TV", latestTv.map { it.toCard(api, JellyfinMediaCardShape.Wide) }))
                    if (liveTvChannels.isNotEmpty() || liveTvPrograms.isNotEmpty()) {
                        val channelImagesById = liveTvChannels.associate { it.id to it.imageUrl }
                        val channelCards = liveTvChannels.map { it.toCard() }
                        val programCards = liveTvPrograms.map { it.toCard(channelImagesById[it.channelId]) }
                        add(JellyfinHomeSection("Live TV Channels", (programCards + channelCards).take(24)))
                    }
                    addAll(smartSections)
                    if (libraryCards.isNotEmpty()) add(JellyfinHomeSection("My Media", libraryCards))
                }
                JellyfinResult.Success(
                    JellyfinHome(
                        heroItems = heroItems,
                        sections = sections,
                        liveTvChannels = liveTvChannels,
                        liveTvPrograms = liveTvPrograms,
                    ),
                )
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    private suspend fun latest(
        api: ApiClient,
        userId: java.util.UUID,
        types: List<BaseItemKind>,
        limit: Int,
    ): List<BaseItemDto> =
        runCatching {
            val response by api.userLibraryApi.getLatestMedia(
                GetLatestMediaRequest(
                    userId = userId,
                    fields = homeFields,
                    includeItemTypes = types,
                    enableImages = true,
                    imageTypeLimit = 2,
                    enableImageTypes = homeImageTypes,
                    enableUserData = true,
                    limit = limit,
                    groupItems = false,
                ),
            )
            response
        }.getOrDefault(emptyList())

    override suspend fun getLatestMedia(
        session: JellyfinSession,
        limit: Int,
    ): List<JellyfinMediaItem> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val types = listOf(
                    BaseItemKind.MOVIE,
                    BaseItemKind.SERIES,
                    BaseItemKind.EPISODE,
                    BaseItemKind.MUSIC_ALBUM,
                )
                val response by api.userLibraryApi.getLatestMedia(
                    GetLatestMediaRequest(
                        userId = session.user.id,
                        fields = homeFields,
                        includeItemTypes = types,
                        enableImages = true,
                        imageTypeLimit = 2,
                        enableImageTypes = homeImageTypes,
                        enableUserData = true,
                        limit = maxOf(limit, 25),
                        groupItems = false,
                    ),
                )
                response.map { it.toMediaItem(api, shapeFor(it.type)) }
            } catch (_: Throwable) {
                emptyList()
            }
        }

    private suspend fun smartRow(
        api: ApiClient,
        userId: java.util.UUID,
        title: String,
        genres: List<String>? = null,
        minCommunityRating: Double? = null,
        maxOfficialRating: String? = null,
        isPlayed: Boolean? = null,
        types: List<BaseItemKind> = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
        sortBy: List<ItemSortBy> = listOf(ItemSortBy.DATE_CREATED),
    ): JellyfinHomeSection? =
        runCatching {
            val response by api.itemsApi.getItems(
                GetItemsRequest(
                    userId = userId,
                    recursive = true,
                    limit = 18,
                    minCommunityRating = minCommunityRating,
                    maxOfficialRating = maxOfficialRating,
                    sortBy = sortBy,
                    sortOrder = listOf(SortOrder.DESCENDING),
                    fields = homeFields,
                    includeItemTypes = types,
                    isPlayed = isPlayed,
                    genres = genres,
                    enableUserData = true,
                    imageTypeLimit = 2,
                    enableImageTypes = homeImageTypes,
                    enableImages = true,
                    enableTotalRecordCount = false,
                ),
            )
            response.items
                .map { it.toCard(api, if (it.type == BaseItemKind.MOVIE) JellyfinMediaCardShape.Poster else JellyfinMediaCardShape.Wide) }
                .takeIf { it.isNotEmpty() }
                ?.let { JellyfinHomeSection(title, it) }
        }.getOrNull()

    private companion object {
        val homeFields = listOf(
            ItemFields.OVERVIEW,
            ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
            ItemFields.SERIES_PRIMARY_IMAGE,
            ItemFields.MEDIA_STREAMS,
        )
        val homeImageTypes = listOf(ImageType.PRIMARY, ImageType.BACKDROP, ImageType.THUMB, ImageType.LOGO)
    }
}

class SdkJellyfinQuickConnectRepository(
    private val jellyfin: Jellyfin,
    private val storage: JellyfinSessionStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinQuickConnectRepository {
    override suspend fun initiate(server: JellyfinServerConfig): JellyfinResult<JellyfinQuickConnectSession> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = server.url)
                val enabled by api.quickConnectApi.getQuickConnectEnabled()
                if (!enabled) throw AuthenticationException("Quick Connect is not enabled on this server")
                val result by api.quickConnectApi.initiateQuickConnect()
                val secret = result.secret ?: throw AuthenticationException("Server did not return a Quick Connect secret")
                val code = result.code ?: throw AuthenticationException("Server did not return a Quick Connect code")
                JellyfinResult.Success(JellyfinQuickConnectSession(server = server, secret = secret, code = code))
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun poll(session: JellyfinQuickConnectSession): JellyfinResult<JellyfinSession?> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url)
                val state by api.quickConnectApi.getQuickConnectState(session.secret)
                if (!state.authenticated) return@withContext JellyfinResult.Success(null)
                val auth by api.userApi.authenticateWithQuickConnect(session.secret)
                val accessToken = auth.accessToken ?: throw AuthenticationException("Server did not return an access token")
                val authenticatedUser = auth.user ?: throw AuthenticationException("Server did not return a user")
                val authedApi = jellyfin.createApi(baseUrl = session.server.url, accessToken = accessToken)
                val currentUser by authedApi.userApi.getCurrentUser()
                val systemInfo = getPublicSystemInfo(authedApi, session.server.copy(serverId = auth.serverId))
                val restored = JellyfinSession(
                    server = session.server.copy(
                        name = systemInfo.name ?: session.server.name,
                        version = systemInfo.version ?: session.server.version,
                        serverId = systemInfo.id ?: auth.serverId ?: session.server.serverId,
                    ),
                    user = JellyfinUser(
                        id = currentUser.id,
                        name = currentUser.name ?: authenticatedUser.name.orEmpty(),
                        serverName = currentUser.serverName ?: authenticatedUser.serverName,
                        primaryImageTag = currentUser.primaryImageTag ?: authenticatedUser.primaryImageTag,
                        isAdministrator = currentUser.policy?.isAdministrator == true,
                    ),
                    profileId = profileId(systemInfo.id ?: auth.serverId ?: session.server.url, currentUser.id),
                    accessToken = accessToken,
                )
                storage.write(restored.toStoredSession())
                JellyfinResult.Success(restored)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun authorizeDevice(session: JellyfinSession, code: String): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            try {
                val normalizedCode = code.trim().uppercase(Locale.US)
                if (normalizedCode.isBlank()) throw AuthenticationException("Enter the Quick Connect code shown on the other device")
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val authorized by api.quickConnectApi.authorizeQuickConnect(normalizedCode, session.user.id)
                if (!authorized) throw AuthenticationException("That Quick Connect code could not be authorized")
                JellyfinResult.Success(Unit)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

}

class SdkJellyfinWatchPartyRepository(
    private val jellyfin: Jellyfin,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinWatchPartyRepository {
    override suspend fun getSyncPlayGroups(session: JellyfinSession): JellyfinResult<List<WatchPartySession>> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val groups by api.syncPlayApi.syncPlayGetGroups()
                JellyfinResult.Success(
                    groups.map {
                        WatchPartySession(
                            id = it.groupId,
                            name = it.groupName,
                            serverId = session.server.serverId ?: session.server.localId,
                            serverName = session.server.name,
                            role = WatchPartyRole.Participant,
                            rules = WatchPartyRules(),
                            members = it.participants.map { name ->
                                WatchPartyMember(id = name, displayName = name, role = WatchPartyRole.Participant)
                            },
                        )
                    },
                )
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun createSyncPlayGroup(
        session: JellyfinSession,
        name: String,
        rules: WatchPartyRules,
        mode: WatchPartyMode,
        selectedMedia: WatchPartySelectedMedia?,
    ): JellyfinResult<WatchPartySession> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val groupName = name.ifBlank { "${session.user.name}'s Watch Party" }
                api.syncPlayApi.syncPlayCreateGroup(NewGroupRequestDto(groupName))
                val groups by api.syncPlayApi.syncPlayGetGroups()
                val created = groups.firstOrNull { it.groupName == groupName } ?: groups.firstOrNull()
                    ?: throw IllegalStateException("Jellyfin created the SyncPlay group but did not return it in the group list.")
                JellyfinResult.Success(
                    WatchPartySession(
                        id = created.groupId,
                        name = created.groupName,
                        serverId = session.server.serverId ?: session.server.localId,
                        serverName = session.server.name,
                        role = WatchPartyRole.Host,
                        rules = rules,
                        mode = mode,
                        selectedMedia = selectedMedia,
                        members = created.participants.map { memberName ->
                            WatchPartyMember(
                                id = memberName,
                                displayName = memberName,
                                role = if (memberName == session.user.name) WatchPartyRole.Host else WatchPartyRole.Participant,
                            )
                        },
                    ),
                )
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun joinSyncPlayGroup(
        session: JellyfinSession,
        groupId: java.util.UUID,
        rules: WatchPartyRules,
    ): JellyfinResult<Unit> =
        syncPlay(session) { it.syncPlayApi.syncPlayJoinGroup(JoinGroupRequestDto(groupId)) }

    override suspend fun leaveSyncPlayGroup(session: JellyfinSession): JellyfinResult<Unit> =
        syncPlay(session) { it.syncPlayApi.syncPlayLeaveGroup() }

    override suspend fun sendSyncPlayCommand(session: JellyfinSession, command: SyncPlayCommand): JellyfinResult<Unit> =
        syncPlay(session) { api ->
            when (command) {
                SyncPlayCommand.Pause -> api.syncPlayApi.syncPlayPause()
                SyncPlayCommand.Resume -> api.syncPlayApi.syncPlayUnpause()
                is SyncPlayCommand.Seek -> api.syncPlayApi.syncPlaySeek(SeekRequestDto(command.positionTicks))
                is SyncPlayCommand.StartItem -> api.syncPlayApi.syncPlaySetNewQueue(
                    PlayRequestDto(
                        playingQueue = listOf(command.itemId),
                        playingItemPosition = 0,
                        startPositionTicks = command.startPositionTicks,
                    ),
                )
                SyncPlayCommand.Leave -> api.syncPlayApi.syncPlayLeaveGroup()
            }
        }

    override suspend fun getCandidates(
        session: JellyfinSession,
        rules: WatchPartyRules,
        limit: Int,
    ): JellyfinResult<List<WatchPartyCandidate>> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val poolLimit = (limit * 4).coerceAtLeast(120).coerceAtMost(300)
                val itemTypes = when (rules.mediaScope) {
                    WatchPartyMediaScope.MoviesOnly -> listOf(BaseItemKind.MOVIE)
                    WatchPartyMediaScope.TvShowsOnly -> listOf(BaseItemKind.SERIES)
                    WatchPartyMediaScope.MoviesAndTv,
                    WatchPartyMediaScope.RecentlyAdded -> listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES)
                    WatchPartyMediaScope.ContinueWatchingOnly -> listOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE)
                    WatchPartyMediaScope.MyListOnly -> listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES)
                }
                val response = when (rules.mediaScope) {
                    WatchPartyMediaScope.ContinueWatchingOnly -> {
                        val resume by api.itemsApi.getResumeItems(
                            GetResumeItemsRequest(
                                userId = session.user.id,
                                limit = poolLimit,
                                fields = watchPartyFields,
                                includeItemTypes = itemTypes,
                                enableUserData = true,
                                imageTypeLimit = 2,
                                enableImageTypes = itemImageTypes,
                            ),
                        )
                        resume.items
                    }
                    else -> {
                        val items by api.itemsApi.getItems(
                            GetItemsRequest(
                                userId = session.user.id,
                                recursive = true,
                                limit = poolLimit,
                                isFavorite = true.takeIf { rules.mediaScope == WatchPartyMediaScope.MyListOnly },
                                isPlayed = false.takeIf { rules.unwatchedOnly },
                                maxOfficialRating = "PG".takeIf { rules.kidFriendlyOnly },
                                genres = rules.genres.takeIf { it.isNotEmpty() },
                                sortBy = if (rules.mediaScope == WatchPartyMediaScope.RecentlyAdded) {
                                    listOf(ItemSortBy.DATE_CREATED)
                                } else {
                                    listOf(ItemSortBy.RANDOM)
                                },
                                sortOrder = listOf(SortOrder.DESCENDING),
                                fields = watchPartyFields,
                                includeItemTypes = itemTypes,
                                enableUserData = true,
                                imageTypeLimit = 2,
                                enableImageTypes = itemImageTypes,
                                enableImages = true,
                                enableTotalRecordCount = false,
                            ),
                        )
                        items.items
                    }
                }
                JellyfinResult.Success(
                    response
                        .filter { it.playAccess != PlayAccess.NONE }
                        .filter { item ->
                            rules.runtimeLimit.maxTicks?.let { max -> (item.runTimeTicks ?: 0L) <= max } ?: true
                        }
                        .filterNot { it.type == BaseItemKind.EPISODE && rules.mediaScope != WatchPartyMediaScope.ContinueWatchingOnly }
                        .map { it.toWatchPartyCandidate(api, session.server.serverId ?: session.server.localId) }
                        .distinctBy { it.id }
                        .shuffled(Random(System.nanoTime()))
                        .take(poolLimit)
                )
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun getInviteRecipients(session: JellyfinSession): JellyfinResult<List<WatchPartyInviteRecipient>> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val sessions by api.sessionApi.getSessions()
                JellyfinResult.Success(
                    sessions
                        .filter { it.isActive }
                        .filter { !it.id.isNullOrBlank() }
                        .filterNot { dto ->
                            dto.userId == session.user.id ||
                                dto.userName.equals(session.user.name, ignoreCase = true)
                        }
                        .map { dto ->
                            WatchPartyInviteRecipient(
                                sessionId = dto.id.orEmpty(),
                                userId = dto.userId,
                                displayName = dto.userName ?: dto.deviceName ?: "Active device",
                                client = dto.client,
                                deviceName = dto.deviceName,
                                imageUrl = dto.userId?.let { publicUserImageUrl(api, it, dto.userPrimaryImageTag) },
                                active = dto.isActive,
                                supportsRemoteControl = dto.supportsRemoteControl,
                            )
                        }
                        .distinctBy { recipient ->
                            recipient.userId?.toString()
                                ?: recipient.displayName.trim().lowercase()
                                    .ifBlank { recipient.sessionId }
                        },
                )
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun sendInvite(
        session: JellyfinSession,
        invite: WatchPartyInvite,
        recipientSessionIds: List<String>,
    ): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val modeText = when (invite.mode) {
                    WatchPartyMode.FixedTitle -> "watch ${invite.mediaTitle ?: "a title"}"
                    WatchPartyMode.SwipeToMatch -> "pick something together"
                }
                val payload = buildString {
                    append("VANTAFYN_WATCH_PARTY_INVITE")
                    append("|inviteId=").append(invite.inviteId)
                    append("|partyId=").append(invite.partyId)
                    append("|serverAccountId=").append(invite.serverAccountId.orEmpty())
                    append("|mode=").append(invite.mode.name)
                    invite.mediaItemId?.let { append("|mediaItemId=").append(it) }
                    invite.mediaType?.let { append("|mediaType=").append(it.take(40).invitePayloadValue()) }
                    invite.mediaTitle?.let { append("|mediaTitle=").append(it.take(80).invitePayloadValue()) }
                    invite.mediaArtworkUrl?.let { append("|mediaArtworkUrl=").append(it.take(240).invitePayloadValue()) }
                    append("|hostUserId=").append(invite.hostUserId)
                    append("|host=").append(invite.hostDisplayName.take(60).invitePayloadValue())
                    append("|createdAt=").append(invite.createdAt)
                    append("|expiresAt=").append(invite.expiresAt)
                }
                val command = MessageCommand(
                    header = "Vantafyn Watch Party",
                    text = "${invite.hostDisplayName} invited you to $modeText.\n$payload",
                    timeoutMs = (invite.expiresAt - invite.createdAt).coerceAtLeast(30_000L),
                )
                recipientSessionIds.distinct().forEach { sessionId ->
                    api.sessionApi.sendMessageCommand(sessionId, command)
                }
                JellyfinResult.Success(Unit)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    private suspend fun syncPlay(
        session: JellyfinSession,
        block: suspend (ApiClient) -> Unit,
    ): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            try {
                block(jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken))
                JellyfinResult.Success(Unit)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    private companion object {
        val watchPartyFields = listOf(
            ItemFields.OVERVIEW,
            ItemFields.GENRES,
            ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
            ItemFields.SERIES_PRIMARY_IMAGE,
            ItemFields.MEDIA_SOURCES,
        )
    }
}

private val WatchPartyRuntimeLimit.maxTicks: Long?
    get() = when (this) {
        WatchPartyRuntimeLimit.Under90Minutes -> 90L * 60L * 10_000_000L
        WatchPartyRuntimeLimit.Under2Hours -> 120L * 60L * 10_000_000L
        WatchPartyRuntimeLimit.AnyLength -> null
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
        profileId = profileId,
        serverUrl = server.url,
        localServerUrl = server.localUrl,
        remoteServerUrl = server.remoteUrl,
        serverName = server.name,
        serverVersion = server.version,
        serverId = server.serverId,
        userId = user.id,
        userName = user.name,
        userImageTag = user.primaryImageTag,
        accessToken = accessToken,
        lastUsedAt = System.currentTimeMillis(),
    )

private fun userImageUrl(jellyfin: Jellyfin, stored: StoredJellyfinSession): String? =
    runCatching {
        if (stored.userImageTag.isNullOrBlank()) return null
        jellyfin
            .createApi(baseUrl = stored.serverUrl, accessToken = stored.accessToken)
            .imageApi
            .getUserImageUrl(stored.userId)
            .withCacheTag(stored.userImageTag)
    }.getOrNull()

private fun publicUserImageUrl(api: ApiClient, userId: java.util.UUID, imageTag: String?): String? =
    runCatching {
        if (imageTag.isNullOrBlank()) return null
        api.imageApi.getUserImageUrl(userId).withCacheTag(imageTag)
    }.getOrNull()

private fun JSONObject.firstString(vararg names: String): String =
    names.firstNotNullOfOrNull { name -> optString(name).takeIf { it.isNotBlank() } }.orEmpty()

private fun JSONObject.playbackReportingItemId(): java.util.UUID? =
    firstString(
        "item_id",
        "ItemId",
        "ItemID",
        "Id",
        "id",
        "itemId",
        "ItemGuid",
        "item_guid",
        "Guid",
        "guid",
    ).let { raw ->
        runCatching { java.util.UUID.fromString(raw) }.getOrNull()
    }

private fun JSONObject.firstInt(vararg names: String): Int =
    firstIntOrNull(*names) ?: 0

private fun JSONObject.firstIntOrNull(vararg names: String): Int? =
    names.firstNotNullOfOrNull { name ->
        if (!has(name) || isNull(name)) {
            null
        } else {
            optInt(name)
        }
    }

private fun JSONObject.firstLong(vararg names: String): Long =
    names.firstNotNullOfOrNull { name ->
        if (!has(name) || isNull(name)) {
            null
        } else {
            optLong(name)
        }
    } ?: 0L

private fun String.playbackReportingSearchCandidates(): List<String> = buildList {
    val raw = trim()
    if (raw.isNotBlank()) add(raw)
    val noParentheses = raw.replace(Regex("""\s*[\(\[\{][^\)\]\}]*[\)\]\}]\s*"""), " ").trim()
    if (noParentheses.isNotBlank() && noParentheses != raw) add(noParentheses)
    val noTrackNumber = noParentheses.replace(Regex("""^\d+[\.\-\s_]+"""), "").trim()
    if (noTrackNumber.isNotBlank() && noTrackNumber !in this) add(noTrackNumber)
    if (raw.contains(" - ")) {
        val afterDash = raw.substringAfter(" - ").trim().replace(Regex("""\s*[\(\[\{][^\)\]\}]*[\)\]\}]\s*"""), " ").trim()
        if (afterDash.isNotBlank() && afterDash !in this) add(afterDash)
        val beforeDash = raw.substringBefore(" - ").trim().replace(Regex("""\s*[\(\[\{][^\)\]\}]*[\)\]\}]\s*"""), " ").trim()
        if (beforeDash.isNotBlank() && beforeDash !in this) add(beforeDash)
    }
}

private fun String.playbackReportingSearchTitle(): String =
    replace(Regex("""\s+\([^)]*\)\s*$"""), "")
        .substringBefore(" - ")
        .trim()

private fun String.normalizedPlaybackReportingTitle(): String =
    playbackReportingSearchTitle()
        .lowercase(Locale.US)
        .replace(Regex("""[^a-z0-9]+"""), " ")
        .trim()

private fun String.withCacheTag(tag: String?): String {
    val safeTag = tag?.takeIf { it.isNotBlank() } ?: return this
    val separator = if (contains("?")) "&" else "?"
    return "$this${separator}tag=${URLEncoder.encode(safeTag, Charsets.UTF_8.name())}"
}

private val itemFields = listOf(
    ItemFields.OVERVIEW,
    ItemFields.GENRES,
    ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
    ItemFields.SERIES_PRIMARY_IMAGE,
    ItemFields.MEDIA_STREAMS,
)

private val activeSessionItemFields = listOf(
    ItemFields.MEDIA_SOURCES,
    ItemFields.MEDIA_STREAMS,
)

private val musicItemFields = listOf(
    ItemFields.GENRES,
    ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
    ItemFields.MEDIA_STREAMS,
)

private val itemImageTypes = listOf(ImageType.PRIMARY, ImageType.BACKDROP, ImageType.THUMB, ImageType.LOGO)

private val mediaItemTypes = listOf(
    BaseItemKind.MOVIE,
    BaseItemKind.SERIES,
    BaseItemKind.EPISODE,
    BaseItemKind.BOX_SET,
    BaseItemKind.AUDIO,
    BaseItemKind.MUSIC_ALBUM,
    BaseItemKind.BOOK,
)

private fun includeTypesFor(collectionType: String?): List<BaseItemKind> =
    when (collectionType?.lowercase()) {
        "movies" -> listOf(BaseItemKind.MOVIE)
        "tvshows", "series" -> listOf(BaseItemKind.SERIES)
        "boxsets", "collections" -> listOf(BaseItemKind.BOX_SET)
        "music" -> listOf(BaseItemKind.AUDIO, BaseItemKind.MUSIC_ALBUM)
        "books" -> listOf(BaseItemKind.BOOK)
        else -> listOf(BaseItemKind.SERIES, BaseItemKind.MOVIE, BaseItemKind.BOX_SET, BaseItemKind.MUSIC_ALBUM, BaseItemKind.BOOK)
    }

private fun String?.isMusicCollection(): Boolean =
    this?.lowercase() == "music"

private fun String?.isLiveTvCollection(): Boolean =
    this?.lowercase()?.replace(" ", "") in setOf("livetv", "livetvchannels")

private fun shapeFor(type: BaseItemKind?): JellyfinMediaCardShape =
    when (type) {
        BaseItemKind.EPISODE, BaseItemKind.AUDIO -> JellyfinMediaCardShape.Wide
        else -> JellyfinMediaCardShape.Poster
    }

private fun BaseItemDto.toCard(api: ApiClient, shape: JellyfinMediaCardShape): JellyfinMediaCard =
    JellyfinMediaCard(
        id = id,
        title = displayTitle(),
        subtitle = subtitle(),
        year = productionYear,
        itemType = type?.serialName,
        imageUrl = primaryImageUrl(api, if (shape == JellyfinMediaCardShape.Wide) 540 else 320),
        backdropUrl = backdropImageUrl(api, 640),
        thumbUrl = thumbImageUrl(api, 640),
        logoUrl = logoImageUrl(api, 420),
        progress = userData?.playedPercentage?.toFloat()?.div(100f)?.coerceIn(0f, 1f),
        shape = shape,
        isFavorite = userData?.isFavorite == true,
        isPlayed = userData?.played == true,
        unplayedItemCount = userData?.unplayedItemCount ?: 0,
        overview = overview,
        runtimeMinutes = runTimeTicks?.let { (it / 600_000_000L).toInt() },
        officialRating = officialRating,
        communityRating = communityRating,
        genres = genres.orEmpty(),
    )

private fun BaseItemDto.toMediaItem(api: ApiClient, shape: JellyfinMediaCardShape): JellyfinMediaItem =
    JellyfinMediaItem(
        id = id,
        title = displayTitle(),
        subtitle = subtitle(),
        year = productionYear,
        itemType = type?.serialName,
        imageUrl = primaryImageUrl(api, if (shape == JellyfinMediaCardShape.Wide) 540 else 360),
        backdropUrl = backdropImageUrl(api, 760),
        thumbUrl = thumbImageUrl(api, 760),
        logoUrl = logoImageUrl(api, 460),
        progress = userData.progress(),
        shape = shape,
        isFavorite = userData?.isFavorite == true,
        isPlayed = userData?.played == true,
        unplayedItemCount = userData?.unplayedItemCount ?: 0,
        mediaType = mediaType?.serialName,
        seriesId = seriesId,
        seasonNumber = parentIndexNumber,
        episodeNumber = indexNumber,
    )

private fun BaseItemDto.toWatchPartyCandidate(api: ApiClient, serverId: String?): WatchPartyCandidate =
    WatchPartyCandidate(
        id = id,
        serverId = serverId,
        title = displayTitle(),
        subtitle = subtitle(),
        year = productionYear,
        itemType = type?.serialName,
        overview = overview,
        genres = genres.orEmpty(),
        officialRating = officialRating,
        runtimeMinutes = runTimeTicks?.let { (it / 600_000_000L).toInt() },
        imageUrl = primaryImageUrl(api, if (type == BaseItemKind.SERIES) 540 else 360),
        backdropUrl = backdropImageUrl(api, 900),
        isFavorite = userData?.isFavorite == true,
        isContinueWatching = (userData?.playedPercentage ?: 0.0) > 0.0 && userData?.played != true,
    )

private fun BaseItemDto.toMusicTrack(api: ApiClient, session: JellyfinSession): JellyfinMusicTrack =
    JellyfinMusicTrack(
        id = id,
        title = name ?: "Untitled track",
        artist = artists?.joinToString(", ")?.takeIf { it.isNotBlank() }
            ?: albumArtist
            ?: "Unknown artist",
        album = album,
        albumId = albumId,
        durationMs = runTimeTicks?.let { it / 10_000L },
        artworkUrl = primaryImageUrl(api, 520),
        hasLyrics = hasLyrics == true,
        streamUrl = api.universalAudioApi.getUniversalAudioStreamUrl(
            itemId = id,
            container = listOf("mp3", "aac", "flac", "opus", "vorbis", "m4a"),
            mediaSourceId = null,
            deviceId = null,
            userId = session.user.id,
            audioCodec = "aac,mp3,flac,opus,vorbis",
            maxAudioChannels = 2,
            transcodingAudioChannels = null,
            maxStreamingBitrate = 384_000,
            audioBitRate = null,
            startTimeTicks = null,
            transcodingContainer = "mp3",
            transcodingProtocol = MediaStreamProtocol.HTTP,
            maxAudioSampleRate = null,
            maxAudioBitDepth = null,
            enableRemoteMedia = true,
            breakOnNonKeyFrames = false,
            enableRedirection = true,
            enableAudioVbrEncoding = true,
        ).withAccessToken(session.accessToken),
        playlistItemId = playlistItemId,
        isFavorite = userData?.isFavorite == true,
    )

private fun BaseItemDto.toMusicAlbum(api: ApiClient): JellyfinMusicAlbum =
    JellyfinMusicAlbum(
        id = id,
        title = name ?: "Untitled album",
        artist = albumArtist ?: artists?.joinToString(", "),
        year = productionYear,
        artworkUrl = primaryImageUrl(api, 520),
    )

private fun BaseItemDto.toMusicArtist(api: ApiClient): JellyfinMusicArtist =
    JellyfinMusicArtist(
        id = id,
        name = name ?: "Unknown artist",
        imageUrl = primaryImageUrl(api, 520),
    )

private fun BaseItemDto.toMusicPlaylist(api: ApiClient, classifiedTrackCount: Int? = null, trackImageUrls: List<String> = emptyList()): JellyfinMusicPlaylist =
    JellyfinMusicPlaylist(
        id = id,
        name = name ?: "Playlist",
        imageUrl = primaryImageUrl(api, 520),
        trackCount = classifiedTrackCount ?: childCount ?: recursiveItemCount,
        trackImageUrls = trackImageUrls,
    )

private fun Long.toLyricMillis(): Long =
    if (this > 86_400_000L) this / 10_000L else this

private fun BaseItemDto.toDetail(
    api: ApiClient,
    seasons: List<JellyfinSeason> = emptyList(),
    episodes: List<JellyfinEpisode> = emptyList(),
    related: List<JellyfinMediaItem> = emptyList(),
    themeSongUrl: String? = null,
    collectionItems: List<JellyfinMediaItem> = emptyList(),
): JellyfinMediaDetail =
    JellyfinMediaDetail(
        id = id,
        title = displayTitle(),
        subtitle = subtitle(),
        year = productionYear,
        runtimeMinutes = runTimeTicks?.let { (it / 600_000_000L).toInt() },
        officialRating = officialRating,
        communityRating = communityRating,
        overview = overview,
        genres = genres.orEmpty(),
        itemType = type?.serialName,
        imageUrl = primaryImageUrl(api, 520),
        backdropUrl = backdropImageUrl(api, 1200),
        logoUrl = logoImageUrl(api, 560),
        isFavorite = userData?.isFavorite == true,
        isPlayed = userData?.played == true,
        progress = userData.progress(),
        playbackPositionTicks = userData?.playbackPositionTicks ?: 0L,
        streamInfo = streamInfo(),
        mediaInfo = mediaInfoLines(),
        mediaSources = mediaSources.orEmpty().map { it.toMediaSourceSummary() },
        people = people.orEmpty().take(18).map { it.toPerson(api) },
        seasons = seasons,
        episodes = episodes,
        related = related,
        collectionItems = collectionItems,
        externalLinks = externalUrls.orEmpty().mapNotNull { url ->
            val name = url.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val href = url.url?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            JellyfinExternalLink(name, href)
        },
        themeSongUrl = themeSongUrl,
        seriesId = seriesId,
        seasonId = seasonId,
        seriesName = seriesName,
        seasonIndexNumber = parentIndexNumber,
        episodeIndexNumber = indexNumber,
    )

private fun BaseItemDto.toEpisode(api: ApiClient): JellyfinEpisode =
    JellyfinEpisode(
        id = id,
        title = name ?: "Episode ${indexNumber ?: ""}".trim(),
        subtitle = seasonEpisodeLabel(),
        overview = overview,
        imageUrl = thumbImageUrl(api, 520) ?: backdropImageUrl(api, 520) ?: primaryImageUrl(api, 360),
        progress = userData.progress(),
        playbackPositionTicks = userData?.playbackPositionTicks ?: 0L,
        isPlayed = userData?.played == true,
        runtimeMinutes = runTimeTicks?.let { (it / 600_000_000L).toInt() },
        indexNumber = indexNumber,
        seasonIndexNumber = parentIndexNumber,
        seriesId = seriesId,
        seasonId = seasonId,
        seriesName = seriesName,
    )

private fun BaseItemDto.toUpNextCandidate(api: ApiClient): JellyfinUpNextCandidate =
    JellyfinUpNextCandidate(
        itemId = id,
        seriesId = seriesId,
        seasonId = seasonId,
        title = name ?: "Episode ${indexNumber ?: ""}".trim(),
        seriesName = seriesName,
        seasonNumber = parentIndexNumber,
        episodeNumber = indexNumber,
        runtimeTicks = runTimeTicks,
        imageUrl = thumbImageUrl(api, 640) ?: primaryImageUrl(api, 420),
        backdropUrl = backdropImageUrl(api, 1000) ?: thumbImageUrl(api, 1000),
        overview = overview,
        progress = userData.progress(),
        playbackPositionTicks = userData?.playbackPositionTicks ?: 0L,
    )

private fun BaseItemDto.mediaInfoLines(): List<JellyfinMediaInfoLine> =
    buildList {
        container?.takeIf { it.isNotBlank() }?.let { add(JellyfinMediaInfoLine("Container", it.uppercase())) }
        mediaSources.orEmpty().firstOrNull()?.let { source ->
            source.name?.takeIf { it.isNotBlank() }?.let { add(JellyfinMediaInfoLine("Version", it)) }
            source.size?.takeIf { it > 0L }?.let { add(JellyfinMediaInfoLine("Size", it.fileSizeLabel())) }
            source.bitrate?.takeIf { it > 0 }?.let { add(JellyfinMediaInfoLine("Bitrate", it.bitrateLabel())) }
            source.path?.takeIf { it.isNotBlank() }?.let { add(JellyfinMediaInfoLine("Path", it, adminOnly = true)) }
            val video = source.mediaStreams.orEmpty().firstOrNull { it.type == MediaStreamType.VIDEO }
            val audio = source.mediaStreams.orEmpty().firstOrNull { it.type == MediaStreamType.AUDIO }
            video?.codec?.takeIf { it.isNotBlank() }?.let { add(JellyfinMediaInfoLine("Video", it.uppercase())) }
            video?.resolutionLabel()?.let { add(JellyfinMediaInfoLine("Resolution", it)) }
            video?.videoRangeType?.toString()?.takeIf { it.isNotBlank() }?.let { add(JellyfinMediaInfoLine("Range", it)) }
            audio?.codec?.takeIf { it.isNotBlank() }?.let { add(JellyfinMediaInfoLine("Audio", it.uppercase())) }
            audio?.channels?.takeIf { it > 0 }?.let { add(JellyfinMediaInfoLine("Audio channels", it.toString())) }
        }
        runTimeTicks?.let { add(JellyfinMediaInfoLine("Runtime", "${(it / 600_000_000L).toInt()} min")) }
        providerIds?.get("Imdb")?.takeIf { it.isNotBlank() }?.let { add(JellyfinMediaInfoLine("IMDb", it)) }
        providerIds?.get("Tmdb")?.takeIf { it.isNotBlank() }?.let { add(JellyfinMediaInfoLine("TMDb", it)) }
    }

private fun MediaSourceInfo.toMediaSourceSummary(): JellyfinMediaSourceSummary {
    val video = mediaStreams.orEmpty().firstOrNull { it.type == MediaStreamType.VIDEO }
    val audio = mediaStreams.orEmpty().firstOrNull { it.type == MediaStreamType.AUDIO }
    return JellyfinMediaSourceSummary(
        id = id,
        name = name,
        container = container,
        path = path,
        sizeLabel = size?.takeIf { it > 0L }?.fileSizeLabel(),
        bitrateLabel = bitrate?.takeIf { it > 0 }?.bitrateLabel(),
        videoCodec = video?.codec,
        audioCodec = audio?.codec,
        resolution = video?.resolutionLabel(),
        dynamicRange = video?.videoRangeType?.toString(),
        audioTracks = mediaStreams.orEmpty()
            .filter { it.type == MediaStreamType.AUDIO }
            .map { it.displayTitle ?: it.title ?: it.language?.uppercase() ?: "Audio ${it.index}" },
        subtitleTracks = mediaStreams.orEmpty()
            .filter { it.type == MediaStreamType.SUBTITLE }
            .map { it.displayTitle ?: it.title ?: it.language?.uppercase() ?: "Subtitle ${it.index}" },
    )
}

private fun MediaStream.resolutionLabel(): String? {
    val width = width ?: return null
    val height = height ?: return null
    return "${width}x$height"
}

private fun MediaStream.videoQualityLabel(): String? {
    val width = width ?: 0
    val height = height ?: 0
    return when {
        width >= 3840 || height >= 2160 -> "4K"
        width >= 2560 || height >= 1440 -> "1440p"
        height >= 1080 -> "1080p"
        height >= 720 -> "720p"
        width > 0 || height > 0 -> "SD"
        else -> null
    }
}

private fun Long.fileSizeLabel(): String {
    val gib = this / 1_073_741_824.0
    val mib = this / 1_048_576.0
    return if (gib >= 1.0) "%.1f GB".format(gib) else "%.0f MB".format(mib)
}

private fun Int.bitrateLabel(): String =
    if (this >= 1_000_000) "%.1f Mbps".format(this / 1_000_000.0) else "${this / 1_000} kbps"

private suspend fun fetchLiveTvChannels(
    api: ApiClient,
    session: JellyfinSession,
    limit: Int,
): List<JellyfinLiveTvChannel> =
    runCatching {
        val response by api.liveTvApi.getLiveTvChannels(
            GetLiveTvChannelsRequest(
                type = ChannelType.TV,
                userId = session.user.id,
                startIndex = null,
                isMovie = null,
                isSeries = null,
                isNews = null,
                isKids = null,
                isSports = null,
                limit = limit,
                isFavorite = null,
                isLiked = null,
                isDisliked = null,
                enableImages = true,
                imageTypeLimit = 2,
                enableImageTypes = listOf(ImageType.PRIMARY, ImageType.THUMB, ImageType.BACKDROP),
                fields = itemFields,
                enableUserData = true,
                sortBy = listOf(ItemSortBy.SORT_NAME),
                sortOrder = SortOrder.ASCENDING,
                enableFavoriteSorting = true,
                addCurrentProgram = true,
            ),
        )
        response.items.map { it.toLiveTvChannel(api) }
    }.getOrDefault(emptyList())

private suspend fun fetchLiveTvPrograms(
    api: ApiClient,
    session: JellyfinSession,
    limit: Int,
): List<JellyfinLiveTvProgram> =
    runCatching {
        val response by api.liveTvApi.getRecommendedPrograms(
            GetRecommendedProgramsRequest(
                userId = session.user.id,
                limit = limit,
                isAiring = true,
                hasAired = null,
                isSeries = null,
                isMovie = null,
                isNews = null,
                isKids = null,
                isSports = null,
                enableImages = true,
                imageTypeLimit = 2,
                enableImageTypes = listOf(ImageType.PRIMARY, ImageType.THUMB, ImageType.BACKDROP),
                genreIds = null,
                fields = itemFields,
                enableUserData = true,
                enableTotalRecordCount = false,
            ),
        )
        response.items.map { it.toLiveTvProgram(api) }
    }.getOrDefault(emptyList())

private fun BaseItemDto.toLiveTvChannel(api: ApiClient): JellyfinLiveTvChannel =
    JellyfinLiveTvChannel(
        id = id,
        name = name ?: "Live TV Channel",
        number = channelNumber ?: number,
        imageUrl = primaryImageUrl(api, 420) ?: thumbImageUrl(api, 420) ?: backdropImageUrl(api, 640),
        currentProgramName = currentProgram?.name,
        currentProgramStart = currentProgram?.startDate?.toString(),
        currentProgramEnd = currentProgram?.endDate?.toString(),
    )

private fun BaseItemDto.toLiveTvProgram(api: ApiClient): JellyfinLiveTvProgram =
    JellyfinLiveTvProgram(
        id = id,
        channelId = channelId,
        title = name ?: "Live Program",
        subtitle = channelName ?: overview?.take(80),
        imageUrl = thumbImageUrl(api, 640) ?: backdropImageUrl(api, 640) ?: primaryImageUrl(api, 420),
        startDate = startDate?.toString(),
        endDate = endDate?.toString(),
    )

private fun JellyfinLiveTvChannel.toMediaItem(): JellyfinMediaItem =
    JellyfinMediaItem(
        id = id,
        title = listOfNotNull(number, name).joinToString("  ").ifBlank { name },
        subtitle = currentProgramName ?: "Live TV channel",
        year = null,
        itemType = "LiveTvChannel",
        imageUrl = imageUrl,
        backdropUrl = imageUrl,
        thumbUrl = imageUrl,
        logoUrl = null,
        progress = null,
        shape = JellyfinMediaCardShape.Wide,
    )

private fun JellyfinLiveTvChannel.toCard(): JellyfinMediaCard =
    JellyfinMediaCard(
        id = id,
        title = listOfNotNull(number, name).joinToString("  ").ifBlank { name },
        subtitle = currentProgramName ?: "Live TV channel",
        year = null,
        itemType = "LiveTvChannel",
        imageUrl = imageUrl,
        backdropUrl = imageUrl,
        thumbUrl = imageUrl,
        logoUrl = null,
        progress = null,
        shape = JellyfinMediaCardShape.Wide,
    )

private fun JellyfinLiveTvProgram.toCard(channelImageUrl: String?): JellyfinMediaCard {
    val resolvedImageUrl = imageUrl ?: channelImageUrl
    return JellyfinMediaCard(
        id = id,
        title = title,
        subtitle = subtitle ?: "On now",
        year = null,
        itemType = "LiveTvProgram",
        imageUrl = resolvedImageUrl,
        backdropUrl = resolvedImageUrl,
        thumbUrl = resolvedImageUrl,
        logoUrl = null,
        progress = null,
        shape = JellyfinMediaCardShape.Wide,
    )
}

private fun org.jellyfin.sdk.model.api.BaseItemPerson.toPerson(api: ApiClient): JellyfinPerson =
    JellyfinPerson(
        id = id,
        name = name ?: "Unknown",
        role = role,
        type = type?.serialName,
        imageUrl = primaryImageTag?.takeIf { it.isNotBlank() }?.let {
            itemImageUrl(api, id, ImageType.PRIMARY, it, maxWidth = 260)
        },
    )

private fun SearchHint.toSearchResult(api: ApiClient): JellyfinSearchResult? {
    val resolvedId = itemId ?: id ?: return null
    val type = type
    val primary = primaryImageTag?.takeIf { it.isNotBlank() }?.let {
        itemImageUrl(api, resolvedId, ImageType.PRIMARY, it, maxWidth = 360)
    }
    val backdropId = backdropImageItemId
        ?.let { runCatching { java.util.UUID.fromString(it) }.getOrNull() }
        ?: resolvedId
    val backdrop = backdropImageTag?.takeIf { it.isNotBlank() }?.let {
        itemImageUrl(api, backdropId, ImageType.BACKDROP, it, maxWidth = 760, index = 0)
    }
    return JellyfinSearchResult(
        id = resolvedId,
        title = name ?: "Untitled",
        subtitle = listOfNotNull(series, type?.serialName).firstOrNull(),
        year = productionYear,
        itemType = type?.serialName,
        imageUrl = primary,
        backdropUrl = backdrop,
        shape = shapeFor(type),
    )
}

private fun BaseItemDto.toHero(api: ApiClient): JellyfinHeroMediaItem {
    val targetId = if (type == BaseItemKind.EPISODE && seriesId != null) seriesId!! else id
    return JellyfinHeroMediaItem(
        id = targetId,
        title = displayTitle(),
        subtitle = if (type == BaseItemKind.EPISODE) null else subtitle(),
        overview = overview,
        year = productionYear,
        runtimeMinutes = runTimeTicks?.let { (it / 600_000_000L).toInt() },
        officialRating = officialRating,
        communityRating = communityRating,
        genres = genres.orEmpty().take(3),
        backdropUrl = backdropImageUrl(api, 1920),
        logoUrl = logoImageUrl(api, 800),
        posterUrl = primaryImageUrl(api, 600),
    )
}

private fun String.withAccessToken(accessToken: String): String =
    if (contains("api_key=") || accessToken.isBlank()) {
        this
    } else {
        this + if (contains("?")) "&api_key=$accessToken" else "?api_key=$accessToken"
    }

private fun UserItemDataDto?.progress(): Float? =
    this?.playedPercentage?.toFloat()?.div(100f)?.coerceIn(0f, 1f)

private fun BaseItemDto.displayTitle(): String =
    when {
        type == BaseItemKind.EPISODE && !seriesName.isNullOrBlank() -> seriesName.orEmpty()
        else -> name ?: "Untitled"
    }

private fun BaseItemDto.heroDedupeKey(): String {
    val titleKey = when {
        type == BaseItemKind.EPISODE && !seriesName.isNullOrBlank() -> seriesName.orEmpty()
        type == BaseItemKind.SERIES && !name.isNullOrBlank() -> name.orEmpty()
        else -> displayTitle()
    }
        .lowercase()
        .replace(Regex("\\s+"), " ")
        .trim()
    val kind = when (type) {
        BaseItemKind.EPISODE, BaseItemKind.SERIES -> "series"
        BaseItemKind.MOVIE -> "movie"
        else -> type?.serialName ?: "media"
    }
    val yearPart = if (kind == "series") "" else ":${productionYear ?: 0}"
    return "$kind$yearPart:$titleKey"
}

private fun BaseItemDto.subtitle(): String? =
    when {
        type == BaseItemKind.EPISODE -> listOfNotNull(
            seasonEpisodeLabel(),
            name,
        ).joinToString(" - ").ifBlank { null }
        productionYear != null -> productionYear.toString()
        else -> type?.serialName
    }

private fun BaseItemDto.seasonEpisodeLabel(): String? =
    if (parentIndexNumber != null && indexNumber != null) {
        "S${parentIndexNumber} E${indexNumber}"
    } else {
        null
    }

private fun BaseItemDto.primaryImageUrl(api: ApiClient, maxWidth: Int): String? {
    val directTag = imageTags?.get(ImageType.PRIMARY)
    if (!directTag.isNullOrBlank()) return itemImageUrl(api, id, ImageType.PRIMARY, directTag, maxWidth)
    val parentId = parentPrimaryImageItemId?.let { runCatching { java.util.UUID.fromString(it) }.getOrNull() }
    val parentTag = parentPrimaryImageTag
    if (parentId != null && !parentTag.isNullOrBlank()) return itemImageUrl(api, parentId, ImageType.PRIMARY, parentTag, maxWidth)
    val albumPrimaryTag = albumPrimaryImageTag
    val albumItemId = albumId
    if (albumItemId != null && !albumPrimaryTag.isNullOrBlank()) return itemImageUrl(api, albumItemId, ImageType.PRIMARY, albumPrimaryTag, maxWidth)
    return null
}

private fun BaseItemDto.backdropImageUrl(api: ApiClient, maxWidth: Int): String? {
    val tag = backdropImageTags?.firstOrNull()
    if (!tag.isNullOrBlank()) return itemImageUrl(api, id, ImageType.BACKDROP, tag, maxWidth, index = 0)
    val parentId = parentBackdropItemId
    val parentTag = parentBackdropImageTags?.firstOrNull()
    if (parentId != null && !parentTag.isNullOrBlank()) return itemImageUrl(api, parentId, ImageType.BACKDROP, parentTag, maxWidth, index = 0)
    return null
}

private fun BaseItemDto.logoImageUrl(api: ApiClient, maxWidth: Int): String? {
    val tag = imageTags?.get(ImageType.LOGO)
    if (!tag.isNullOrBlank()) return itemImageUrl(api, id, ImageType.LOGO, tag, maxWidth)
    val parentId = parentLogoItemId
    val parentTag = parentLogoImageTag
    if (parentId != null && !parentTag.isNullOrBlank()) return itemImageUrl(api, parentId, ImageType.LOGO, parentTag, maxWidth)
    return null
}

private fun BaseItemDto.thumbImageUrl(api: ApiClient, maxWidth: Int): String? {
    val tag = imageTags?.get(ImageType.THUMB)
    if (!tag.isNullOrBlank()) return itemImageUrl(api, id, ImageType.THUMB, tag, maxWidth)
    val parentId = parentThumbItemId
    val parentTag = parentThumbImageTag
    if (parentId != null && !parentTag.isNullOrBlank()) return itemImageUrl(api, parentId, ImageType.THUMB, parentTag, maxWidth)
    return null
}

private fun BaseItemDto.streamInfo(): List<String> {
    val streams = mediaSources.orEmpty()
        .firstOrNull { source -> source.mediaStreams.orEmpty().any { it.type == MediaStreamType.VIDEO } }
        ?.mediaStreams
        .orEmpty()
        .ifEmpty { mediaStreams.orEmpty() }
    val video = streams.firstOrNull { it.type == MediaStreamType.VIDEO }
    val audio = streams.firstOrNull { it.type == MediaStreamType.AUDIO }
    val subtitle = streams.firstOrNull { it.type == MediaStreamType.SUBTITLE }
    return listOfNotNull(
        video?.let {
            val quality = it.videoQualityLabel()
            val range = it.videoRange?.serialName ?: it.videoRangeType?.serialName
            listOfNotNull(quality, range).joinToString(" ").takeIf { value -> value.isNotBlank() }
        },
        audio?.let {
            val lang = it.language?.uppercase()
            val channels = it.channels?.let { count -> if (count >= 6) "5.1" else "${count}.0" }
            listOfNotNull(lang, channels, it.codec?.uppercase()).joinToString(" · ").takeIf { value -> value.isNotBlank() }
        },
        subtitle?.let { "Subtitles available" },
    )
}

private fun androidMobileDeviceProfile(maxVideoStreamingBitrate: Int? = null): DeviceProfile =
    DeviceProfile(
        name = "Vantafyn Android Mobile",
        id = null,
        maxStreamingBitrate = maxVideoStreamingBitrate ?: DEFAULT_MAX_VIDEO_STREAMING_BITRATE,
        maxStaticBitrate = maxVideoStreamingBitrate ?: DEFAULT_MAX_VIDEO_STATIC_BITRATE,
        musicStreamingTranscodingBitrate = 384_000,
        maxStaticMusicBitrate = 1_000_000,
        directPlayProfiles = listOf(
            DirectPlayProfile(
                container = "mp4,m4v,mov,mkv,webm",
                audioCodec = "aac,mp3,ac3,eac3,opus,vorbis,flac",
                videoCodec = "h264,hevc,vp8,vp9,av1,mpeg4",
                type = DlnaProfileType.VIDEO,
            ),
            DirectPlayProfile(
                container = "mp3,aac,m4a,flac,webma,webm,ogg",
                audioCodec = "aac,mp3,flac,opus,vorbis",
                videoCodec = null,
                type = DlnaProfileType.AUDIO,
            ),
        ),
        transcodingProfiles = listOf(
            TranscodingProfile(
                container = "ts",
                type = DlnaProfileType.VIDEO,
                videoCodec = "h264,hevc",
                audioCodec = "aac,mp3,ac3,eac3",
                protocol = org.jellyfin.sdk.model.api.MediaStreamProtocol.HLS,
                estimateContentLength = false,
                enableMpegtsM2TsMode = false,
                transcodeSeekInfo = TranscodeSeekInfo.AUTO,
                copyTimestamps = false,
                context = EncodingContext.STREAMING,
                enableSubtitlesInManifest = true,
                maxAudioChannels = "6",
                minSegments = 1,
                segmentLength = 6,
                breakOnNonKeyFrames = true,
                conditions = emptyList(),
                enableAudioVbrEncoding = true,
            ),
            TranscodingProfile(
                container = "ts",
                type = DlnaProfileType.AUDIO,
                videoCodec = "",
                audioCodec = "aac,mp3,ac3,eac3",
                protocol = MediaStreamProtocol.HLS,
                estimateContentLength = false,
                enableMpegtsM2TsMode = false,
                transcodeSeekInfo = TranscodeSeekInfo.AUTO,
                copyTimestamps = false,
                context = EncodingContext.STREAMING,
                enableSubtitlesInManifest = false,
                maxAudioChannels = "8",
                minSegments = 1,
                segmentLength = 6,
                breakOnNonKeyFrames = true,
                conditions = emptyList(),
                enableAudioVbrEncoding = true,
            ),
        ),
        containerProfiles = emptyList(),
        codecProfiles = emptyList(),
        subtitleProfiles = listOf(
            SubtitleProfile("vtt", SubtitleDeliveryMethod.HLS, null, null, null),
            SubtitleProfile("webvtt", SubtitleDeliveryMethod.HLS, null, null, null),
            SubtitleProfile("srt", SubtitleDeliveryMethod.EXTERNAL, null, null, null),
            SubtitleProfile("subrip", SubtitleDeliveryMethod.EXTERNAL, null, null, null),
            SubtitleProfile("ttml", SubtitleDeliveryMethod.EXTERNAL, null, null, null),
            SubtitleProfile("ass", SubtitleDeliveryMethod.ENCODE, null, null, null),
            SubtitleProfile("ssa", SubtitleDeliveryMethod.ENCODE, null, null, null),
            SubtitleProfile("pgs", SubtitleDeliveryMethod.ENCODE, null, null, null),
            SubtitleProfile("pgssub", SubtitleDeliveryMethod.ENCODE, null, null, null),
            SubtitleProfile("dvdsub", SubtitleDeliveryMethod.ENCODE, null, null, null),
        ),
    )

private fun Int?.validVideoStreamingBitrate(): Int? =
    this?.takeIf { it in 1_000_000..DEFAULT_MAX_VIDEO_STREAMING_BITRATE }

private const val DEFAULT_MAX_VIDEO_STREAMING_BITRATE = 120_000_000
private const val DEFAULT_MAX_VIDEO_STATIC_BITRATE = 160_000_000

private fun googleCastDeviceProfile(): DeviceProfile =
    DeviceProfile(
        name = "Vantafyn Google Cast",
        id = null,
        maxStreamingBitrate = 20_000_000,
        maxStaticBitrate = 40_000_000,
        musicStreamingTranscodingBitrate = 320_000,
        maxStaticMusicBitrate = 1_000_000,
        directPlayProfiles = listOf(
            DirectPlayProfile(
                container = "mp4,m4v,webm",
                audioCodec = "aac,mp3,ac3,eac3,opus,vorbis",
                videoCodec = "h264,vp8,vp9",
                type = DlnaProfileType.VIDEO,
            ),
            DirectPlayProfile(
                container = "mp3,aac,m4a,webma,webm,ogg",
                audioCodec = "aac,mp3,opus,vorbis",
                videoCodec = null,
                type = DlnaProfileType.AUDIO,
            ),
        ),
        transcodingProfiles = listOf(
            TranscodingProfile(
                container = "ts",
                type = DlnaProfileType.VIDEO,
                videoCodec = "h264",
                audioCodec = "aac,ac3,mp3",
                protocol = MediaStreamProtocol.HLS,
                estimateContentLength = false,
                enableMpegtsM2TsMode = false,
                transcodeSeekInfo = TranscodeSeekInfo.AUTO,
                copyTimestamps = false,
                context = EncodingContext.STREAMING,
                enableSubtitlesInManifest = true,
                maxAudioChannels = "6",
                minSegments = 2,
                segmentLength = 6,
                breakOnNonKeyFrames = true,
                conditions = emptyList(),
                enableAudioVbrEncoding = true,
            ),
        ),
        containerProfiles = emptyList(),
        codecProfiles = emptyList(),
        subtitleProfiles = listOf(
            SubtitleProfile("vtt", SubtitleDeliveryMethod.HLS, null, null, null),
            SubtitleProfile("webvtt", SubtitleDeliveryMethod.HLS, null, null, null),
            SubtitleProfile("srt", SubtitleDeliveryMethod.EXTERNAL, null, null, null),
            SubtitleProfile("subrip", SubtitleDeliveryMethod.EXTERNAL, null, null, null),
            SubtitleProfile("ass", SubtitleDeliveryMethod.ENCODE, null, null, null),
            SubtitleProfile("ssa", SubtitleDeliveryMethod.ENCODE, null, null, null),
            SubtitleProfile("pgs", SubtitleDeliveryMethod.ENCODE, null, null, null),
            SubtitleProfile("pgssub", SubtitleDeliveryMethod.ENCODE, null, null, null),
            SubtitleProfile("dvdsub", SubtitleDeliveryMethod.ENCODE, null, null, null),
        ),
    )

private fun JellyfinPlaybackMethod.toSdkPlayMethod(): PlayMethod =
    when (this) {
        JellyfinPlaybackMethod.DirectPlay -> PlayMethod.DIRECT_PLAY
        JellyfinPlaybackMethod.DirectStream -> PlayMethod.DIRECT_STREAM
        JellyfinPlaybackMethod.Transcode -> PlayMethod.TRANSCODE
    }

private fun sourceLabel(source: MediaSourceInfo, method: JellyfinPlaybackMethod): String {
    val quality = source.mediaStreams.orEmpty()
        .firstOrNull { it.type == MediaStreamType.VIDEO }
        ?.videoQualityLabel()
    val methodLabel = when (method) {
        JellyfinPlaybackMethod.DirectPlay -> "Direct Play"
        JellyfinPlaybackMethod.DirectStream -> "Direct Stream"
        JellyfinPlaybackMethod.Transcode -> "Transcode"
    }
    return listOfNotNull(methodLabel, quality, source.container?.uppercase()).joinToString(" · ")
}

private fun absoluteServerUrl(serverUrl: String, pathOrUrl: String): String =
    if (pathOrUrl.startsWith("http://", ignoreCase = true) || pathOrUrl.startsWith("https://", ignoreCase = true)) {
        pathOrUrl
    } else {
        "${serverUrl.trimEnd('/')}/${pathOrUrl.trimStart('/')}"
    }

private fun String.urlEncoded(): String =
    URLEncoder.encode(this, Charsets.UTF_8.name())

private fun org.jellyfin.sdk.model.api.UserConfiguration.toPlaybackPreferences(): JellyfinUserPlaybackPreferences =
    JellyfinUserPlaybackPreferences(
        audioLanguagePreference = audioLanguagePreference,
        subtitleLanguagePreference = subtitleLanguagePreference,
        subtitleMode = subtitleMode.serialName,
        playDefaultAudioTrack = playDefaultAudioTrack,
        rememberAudioSelections = rememberAudioSelections,
        rememberSubtitleSelections = rememberSubtitleSelections,
        enableNextEpisodeAutoPlay = enableNextEpisodeAutoPlay,
    )

private fun MediaSegmentDto.toMediaSegment(): JellyfinMediaSegment? {
    if (endTicks <= startTicks) return null
    return JellyfinMediaSegment(
        id = id,
        itemId = itemId,
        type = type.toVantafynSegmentType(),
        startTicks = startTicks,
        endTicks = endTicks,
    )
}

private fun MediaSegmentType.toVantafynSegmentType(): JellyfinMediaSegmentType =
    when (this) {
        MediaSegmentType.UNKNOWN -> JellyfinMediaSegmentType.Unknown
        MediaSegmentType.COMMERCIAL -> JellyfinMediaSegmentType.Commercial
        MediaSegmentType.PREVIEW -> JellyfinMediaSegmentType.Preview
        MediaSegmentType.RECAP -> JellyfinMediaSegmentType.Recap
        MediaSegmentType.OUTRO -> JellyfinMediaSegmentType.Outro
        MediaSegmentType.INTRO -> JellyfinMediaSegmentType.Intro
    }

private fun String?.toSubtitlePlaybackMode(fallback: SubtitlePlaybackMode): SubtitlePlaybackMode =
    SubtitlePlaybackMode.entries.firstOrNull {
        it.serialName.equals(this, ignoreCase = true) || it.name.equals(this, ignoreCase = true)
    } ?: fallback

private fun org.jellyfin.sdk.model.api.UserDto.toAdminUserDetail(api: ApiClient): JellyfinAdminUserDetail {
    val policy = policy
    return JellyfinAdminUserDetail(
        user = JellyfinAdminUser(
            id = id,
            name = name ?: "Unknown",
            imageUrl = publicUserImageUrl(api, id, primaryImageTag),
            isAdministrator = policy?.isAdministrator == true,
            isDisabled = policy?.isDisabled == true,
            isHidden = policy?.isHidden == true,
            lastActivity = lastActivityDate?.toString(),
            lastLogin = lastLoginDate?.toString(),
        ),
        enableAllFolders = policy?.enableAllFolders ?: true,
        enabledFolderIds = policy?.enabledFolders.orEmpty(),
    )
}

private fun itemImageUrl(
    api: ApiClient,
    itemId: java.util.UUID,
    imageType: ImageType,
    tag: String,
    maxWidth: Int,
    index: Int? = null,
): String? =
    runCatching {
        api.imageApi.getItemImageUrl(
            itemId = itemId,
            imageType = imageType,
            maxWidth = maxWidth,
            maxHeight = null,
            width = null,
            height = null,
            quality = 90,
            fillWidth = null,
            fillHeight = null,
            tag = tag,
            format = ImageFormat.WEBP,
            percentPlayed = null,
            unplayedCount = null,
            blur = null,
            backgroundColor = null,
            foregroundLayer = null,
            imageIndex = index,
        )
    }.getOrNull()

private fun profileId(serverUrl: String, userId: java.util.UUID): String =
    "${serverUrl.hashCode().toUInt()}-$userId"

private fun toFavoriteUserMessage(throwable: Throwable): String {
    val className = throwable.javaClass.name
    val message = throwable.message.orEmpty()
    return when {
        className.contains("InvalidStatusException") && message.contains("401") -> "Session expired. Please sign in again."
        className.contains("InvalidStatusException") && message.contains("403") -> "Couldn't update My List. This profile is not allowed to change favorites."
        className.contains("SocketTimeout", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) -> "Couldn't reach Jellyfin. Check your server connection and try again."
        className.contains("UnknownHost", ignoreCase = true) ||
            className.contains("ConnectException", ignoreCase = true) -> "Couldn't reach Jellyfin. Check your server connection and try again."
        else -> "Couldn't update My List. Check your server connection and try again."
    }
}

private fun toProfileImageUserMessage(throwable: Throwable): String {
    val className = throwable.javaClass.name
    val message = throwable.message.orEmpty()
    return when {
        className.contains("InvalidStatusException") && (message.contains("401") || message.contains("403")) ->
            "You don't have permission to change this profile picture."
        className.contains("InvalidStatusException") && message.contains("404") ->
            "This Jellyfin server does not allow changing profile pictures from Vantafyn yet."
        className.contains("InvalidStatusException") && message.contains("415") ->
            "That image type is not supported by this Jellyfin server."
        className.contains("InvalidStatusException") && message.contains("413") ->
            "That image is too large."
        className.contains("SocketTimeout", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) -> "Server unreachable."
        className.contains("UnknownHost", ignoreCase = true) ||
            className.contains("ConnectException", ignoreCase = true) -> "Server unreachable."
        else -> "Couldn't update profile picture."
    }
}

private fun toUserMessage(throwable: Throwable): String {
    val className = throwable.javaClass.name
    val message = throwable.message.orEmpty()
    return when {
        throwable is SessionRestoreException -> throwable.message ?: "No saved Jellyfin session"
        throwable is AuthenticationException -> throwable.message ?: "Unable to authenticate"
        throwable is SecurityException -> throwable.message ?: "That address belongs to a different Jellyfin server"
        className.contains("InvalidStatusException") && message.contains("401") -> "Invalid username or password"
        message.contains("CLEARTEXT", ignoreCase = true) -> "Android blocked cleartext HTTP for this server"
        className.contains("SSL", ignoreCase = true) ||
            className.contains("Cert", ignoreCase = true) ||
            message.contains("certificate", ignoreCase = true) -> "HTTPS certificate problem"
        className.contains("SocketTimeout", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) -> "Connection timed out"
        className.contains("UnknownHost", ignoreCase = true) -> "Could not resolve server address"
        className.contains("ConnectException", ignoreCase = true) -> "Could not reach server"
        className.contains("InvalidStatusException") -> "Server responded but does not look like Jellyfin"
        throwable is PlaybackException -> throwable.message ?: "This item cannot be played yet"
        throwable is IllegalArgumentException -> throwable.message ?: "Invalid server address"
        else -> "Could not reach the Jellyfin server"
    }
}

private fun String.invitePayloadValue(): String =
    URLEncoder.encode(this, Charsets.UTF_8.name())

private fun Throwable.toRestoreFailure(): JellyfinSessionRestoreFailure {
    val className = javaClass.name
    val message = message.orEmpty()
    val reason = when {
        this is kotlinx.coroutines.TimeoutCancellationException -> JellyfinRestoreFailureReason.ServerUnreachable
        this is SessionRestoreException -> JellyfinRestoreFailureReason.AuthExpired
        this is SecurityException -> JellyfinRestoreFailureReason.InvalidServerUrl
        this is IllegalArgumentException -> JellyfinRestoreFailureReason.InvalidServerUrl
        className.contains("InvalidStatusException") && message.contains("401") -> JellyfinRestoreFailureReason.AuthExpired
        className.contains("InvalidStatusException") && message.contains("403") -> JellyfinRestoreFailureReason.Unauthorized
        className.contains("UnknownHost", ignoreCase = true) -> JellyfinRestoreFailureReason.ServerUnreachable
        className.contains("SocketTimeout", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) -> JellyfinRestoreFailureReason.ServerUnreachable
        className.contains("ConnectException", ignoreCase = true) ||
            className.contains("NoRouteToHost", ignoreCase = true) ||
            message.contains("failed to connect", ignoreCase = true) ||
            message.contains("unable to resolve host", ignoreCase = true) -> JellyfinRestoreFailureReason.ServerUnreachable
        className.contains("SSL", ignoreCase = true) ||
            className.contains("Cert", ignoreCase = true) ||
            message.contains("certificate", ignoreCase = true) -> JellyfinRestoreFailureReason.ServerUnreachable
        className.contains("InvalidStatusException") -> JellyfinRestoreFailureReason.ServerError
        else -> JellyfinRestoreFailureReason.UnknownError
    }
    val userMessage = when (reason) {
        JellyfinRestoreFailureReason.ServerUnreachable -> "Could not reach your saved Jellyfin server"
        JellyfinRestoreFailureReason.NetworkUnavailable -> "Network unavailable"
        JellyfinRestoreFailureReason.AuthExpired -> "This profile needs to sign in again"
        JellyfinRestoreFailureReason.Unauthorized -> "This profile is not authorized on this server"
        JellyfinRestoreFailureReason.InvalidServerUrl -> "Saved server address is invalid"
        JellyfinRestoreFailureReason.ServerError -> "The saved server responded with an error"
        JellyfinRestoreFailureReason.UnknownError -> "Could not restore this saved profile"
    }
    return JellyfinSessionRestoreFailure(reason, userMessage, this)
}

private class AuthenticationException(message: String) : RuntimeException(message)
private class SessionRestoreException(message: String) : RuntimeException(message)
private class PlaybackException(message: String) : RuntimeException(message)
