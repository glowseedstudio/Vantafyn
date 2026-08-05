package dev.vantafyn.core.jellyfin

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.authenticateUserByName
import org.jellyfin.sdk.api.client.extensions.authenticateWithQuickConnect
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.api.client.extensions.quickConnectApi
import org.jellyfin.sdk.api.client.extensions.searchApi
import org.jellyfin.sdk.api.client.extensions.sessionApi
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.universalAudioApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ChannelType
import org.jellyfin.sdk.model.api.ImageFormat
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SearchHint
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.SubtitlePlaybackMode
import org.jellyfin.sdk.model.api.UserItemDataDto
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.UpdateUserPassword
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.api.request.GetEpisodesRequest
import org.jellyfin.sdk.model.api.request.GetLatestMediaRequest
import org.jellyfin.sdk.model.api.request.GetLiveTvChannelsRequest
import org.jellyfin.sdk.model.api.request.GetNextUpRequest
import org.jellyfin.sdk.model.api.request.GetRecommendedProgramsRequest
import org.jellyfin.sdk.model.api.request.GetResumeItemsRequest
import org.jellyfin.sdk.model.api.request.GetSearchHintsRequest
import org.jellyfin.sdk.model.api.request.GetSeasonsRequest
import org.jellyfin.sdk.model.api.request.GetSimilarItemsRequest
import org.jellyfin.sdk.model.api.request.GetThemeSongsRequest

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
        SdkJellyfinUserPreferencesRepository(jellyfin, ioDispatcher)

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
                    jellyfinUserId = stored.userId,
                    displayName = stored.userName,
                    userImageTag = stored.userImageTag,
                    imageUrl = userImageUrl(jellyfin, stored),
                    lastUsedAt = stored.lastUsedAt,
                )
            }
        }

    override suspend fun restoreSession(profileId: String): JellyfinResult<JellyfinSession> =
        runCatchingResult {
            val stored = storage.read(profileId) ?: throw SessionRestoreException("Saved profile was not found")
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
                    primaryImageTag = currentUser.primaryImageTag ?: stored.userImageTag,
                    isAdministrator = currentUser.policy?.isAdministrator == true,
                ),
                profileId = stored.profileId,
                accessToken = stored.accessToken,
            )
            storage.write(restored.toStoredSession())
            restored
        }

    override suspend fun testServer(serverUrl: String): JellyfinResult<JellyfinServerConfig> =
        runCatchingResult {
            var lastFailure: Throwable? = null
            JellyfinServerUrlNormalizer.candidates(serverUrl).forEach { normalizedUrl ->
                try {
                    val api = jellyfin.createApi(baseUrl = normalizedUrl)
                    val systemInfo = getPublicSystemInfo(api, JellyfinServerConfig(normalizedUrl))
                    return@runCatchingResult JellyfinServerConfig(
                        url = normalizedUrl,
                        name = systemInfo.name,
                        version = systemInfo.version,
                        serverId = systemInfo.id,
                    )
                } catch (throwable: Throwable) {
                    lastFailure = throwable
                }
            }
            throw lastFailure ?: IllegalArgumentException("Enter a valid server URL")
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
        runCatchingResult {
            val normalizedUrl = JellyfinServerUrlNormalizer.normalize(serverUrl)
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
                    primaryImageTag = currentUser.primaryImageTag ?: authenticatedUser.primaryImageTag,
                    isAdministrator = currentUser.policy?.isAdministrator == true,
                ),
                profileId = profileId(normalizedUrl, currentUser.id),
                accessToken = accessToken,
            )
            storage.write(session.toStoredSession())
            session
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
                val response by api.itemsApi.getItems(
                    GetItemsRequest(
                        userId = session.user.id,
                        parentId = library.id,
                        recursive = true,
                        limit = limit,
                        sortBy = listOf(ItemSortBy.DATE_CREATED),
                        sortOrder = listOf(SortOrder.DESCENDING),
                        fields = itemFields,
                        includeItemTypes = includeTypesFor(library.collectionType),
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
                JellyfinResult.Success(
                    item.toDetail(
                        api = api,
                        seasons = seasons,
                        episodes = episodes,
                        related = related,
                        themeSongUrl = themeSongUrl,
                    ),
                )
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
                val userData by if (isFavorite) {
                    api.userLibraryApi.markFavoriteItem(session.user.id, itemId)
                } else {
                    api.userLibraryApi.unmarkFavoriteItem(session.user.id, itemId)
                }
                JellyfinResult.Success(userData.isFavorite == true)
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
            }
        }

    override suspend fun refreshFavoriteState(
        session: JellyfinSession,
        itemId: java.util.UUID,
    ): JellyfinResult<Boolean> =
        withContext(ioDispatcher) {
            try {
                val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
                val userData by api.itemsApi.getItemUserData(session.user.id, itemId)
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
                    api.playStateApi.markPlayedItem(session.user.id, itemId, null)
                } else {
                    api.playStateApi.markUnplayedItem(session.user.id, itemId)
                }
                JellyfinResult.Success(userData.played == true)
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
                )
            }
        }.getOrDefault(emptyList())

    private suspend fun fetchEpisodes(
        api: ApiClient,
        session: JellyfinSession,
        seriesId: java.util.UUID,
        seasonId: java.util.UUID?,
    ): List<JellyfinEpisode> =
        runCatching {
            val response by api.tvShowsApi.getEpisodes(
                GetEpisodesRequest(
                    seriesId = seriesId,
                    userId = session.user.id,
                    fields = itemFields,
                    seasonId = seasonId,
                    limit = 24,
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

class SdkJellyfinUserPreferencesRepository(
    private val jellyfin: Jellyfin,
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
                    audioLanguagePreference = preferences.audioLanguagePreference?.takeIf { it.isNotBlank() },
                    playDefaultAudioTrack = preferences.playDefaultAudioTrack,
                    subtitleLanguagePreference = preferences.subtitleLanguagePreference?.takeIf { it.isNotBlank() },
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
                val sessions = runCatching {
                    val result by api.sessionApi.getSessions()
                    result.map { dto ->
                        JellyfinAdminSession(
                            id = dto.id ?: dto.deviceId ?: dto.userId?.toString().orEmpty(),
                            userId = dto.userId,
                            userName = dto.userName,
                            userImageUrl = dto.userId?.let { publicUserImageUrl(api, it, dto.userPrimaryImageTag) },
                            client = dto.client,
                            deviceName = dto.deviceName,
                            remoteEndPoint = dto.remoteEndPoint,
                            nowPlayingTitle = dto.nowPlayingItem?.name,
                            isTranscoding = dto.transcodingInfo != null,
                        )
                    }
                }.getOrDefault(emptyList())
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
                JellyfinResult.Success(
                    JellyfinAdminOverview(
                        serverName = system.serverName,
                        serverVersion = system.version,
                        operatingSystem = system.operatingSystemDisplayName ?: system.operatingSystem,
                        activeSessions = sessions,
                        users = users,
                        libraryCount = libraries.size,
                        totalItems = countItems(api, session, null),
                        moviesCount = countItems(api, session, listOf(BaseItemKind.MOVIE)),
                        seriesCount = countItems(api, session, listOf(BaseItemKind.SERIES)),
                        episodesCount = countItems(api, session, listOf(BaseItemKind.EPISODE)),
                        musicCount = countItems(api, session, listOf(BaseItemKind.AUDIO, BaseItemKind.MUSIC_ALBUM)),
                        unavailableStats = listOf(
                            "Watch-time totals require a Jellyfin plugin or external reporting source.",
                            "Detailed historical playback analytics are not exposed by Jellyfin core here.",
                        ),
                    ),
                )
            } catch (throwable: Throwable) {
                JellyfinResult.Failure(toUserMessage(throwable), throwable)
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
                val heroItems = (resumeItems + latestMovies + nextUpItems + latestTv)
                    .distinctBy { it.id }
                    .map { it.toHero(api) }
                    .filter { it.backdropUrl != null || it.posterUrl != null }
                    .take(8)
                val sections = buildList {
                    val continueItems = (resumeItems + nextUpItems)
                        .distinctBy { it.id }
                        .map { it.toCard(api, JellyfinMediaCardShape.Wide) }
                    if (continueItems.isNotEmpty()) add(JellyfinHomeSection("Continue Watching & Next Up", continueItems))
                    if (latestMovies.isNotEmpty()) add(JellyfinHomeSection("Recently Added Movies", latestMovies.map { it.toCard(api, JellyfinMediaCardShape.Poster) }))
                    if (latestTv.isNotEmpty()) add(JellyfinHomeSection("Recently Added TV", latestTv.map { it.toCard(api, JellyfinMediaCardShape.Wide) }))
                    if (liveTvChannels.isNotEmpty() || liveTvPrograms.isNotEmpty()) {
                        val channelCards = liveTvChannels.map { it.toCard() }
                        val programCards = liveTvPrograms.map { it.toCard() }
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
                    profileId = profileId(session.server.url, currentUser.id),
                    accessToken = accessToken,
                )
                storage.write(restored.toStoredSession())
                JellyfinResult.Success(restored)
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
        profileId = profileId,
        serverUrl = server.url,
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
    }.getOrNull()

private fun publicUserImageUrl(api: ApiClient, userId: java.util.UUID, imageTag: String?): String? =
    runCatching {
        if (imageTag.isNullOrBlank()) return null
        api.imageApi.getUserImageUrl(userId)
    }.getOrNull()

private val itemFields = listOf(
    ItemFields.OVERVIEW,
    ItemFields.GENRES,
    ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
    ItemFields.SERIES_PRIMARY_IMAGE,
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
        else -> mediaItemTypes
    }

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
    )

private fun BaseItemDto.toDetail(
    api: ApiClient,
    seasons: List<JellyfinSeason> = emptyList(),
    episodes: List<JellyfinEpisode> = emptyList(),
    related: List<JellyfinMediaItem> = emptyList(),
    themeSongUrl: String? = null,
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
        streamInfo = streamInfo(),
        people = people.orEmpty().take(18).map { it.toPerson(api) },
        seasons = seasons,
        episodes = episodes,
        related = related,
        externalLinks = externalUrls.orEmpty().mapNotNull { url ->
            val name = url.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val href = url.url?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            JellyfinExternalLink(name, href)
        },
        themeSongUrl = themeSongUrl,
    )

private fun BaseItemDto.toEpisode(api: ApiClient): JellyfinEpisode =
    JellyfinEpisode(
        id = id,
        title = name ?: "Episode ${indexNumber ?: ""}".trim(),
        subtitle = seasonEpisodeLabel(),
        overview = overview,
        imageUrl = thumbImageUrl(api, 520) ?: backdropImageUrl(api, 520) ?: primaryImageUrl(api, 360),
        progress = userData.progress(),
        indexNumber = indexNumber,
        seasonIndexNumber = parentIndexNumber,
    )

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

private fun JellyfinLiveTvProgram.toCard(): JellyfinMediaCard =
    JellyfinMediaCard(
        id = id,
        title = title,
        subtitle = subtitle ?: "On now",
        year = null,
        itemType = "LiveTvProgram",
        imageUrl = imageUrl,
        backdropUrl = imageUrl,
        thumbUrl = imageUrl,
        logoUrl = null,
        progress = null,
        shape = JellyfinMediaCardShape.Wide,
    )

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

private fun BaseItemDto.toHero(api: ApiClient): JellyfinHeroMediaItem =
    JellyfinHeroMediaItem(
        id = id,
        title = displayTitle(),
        subtitle = subtitle(),
        overview = overview,
        year = productionYear,
        runtimeMinutes = runTimeTicks?.let { (it / 600_000_000L).toInt() },
        officialRating = officialRating,
        communityRating = communityRating,
        genres = genres.orEmpty().take(3),
        backdropUrl = backdropImageUrl(api, 1100),
        logoUrl = logoImageUrl(api, 520),
        posterUrl = primaryImageUrl(api, 420),
    )

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
    val streams = mediaStreams.orEmpty()
    val video = streams.firstOrNull { it.type == MediaStreamType.VIDEO }
    val audio = streams.firstOrNull { it.type == MediaStreamType.AUDIO }
    val subtitle = streams.firstOrNull { it.type == MediaStreamType.SUBTITLE }
    return listOfNotNull(
        video?.let {
            val quality = when {
                (it.height ?: 0) >= 2160 -> "4K"
                (it.height ?: 0) >= 1080 -> "HD"
                (it.height ?: 0) >= 720 -> "HD"
                else -> null
            }
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

private fun toUserMessage(throwable: Throwable): String {
    val className = throwable.javaClass.name
    val message = throwable.message.orEmpty()
    return when {
        throwable is SessionRestoreException -> throwable.message ?: "No saved Jellyfin session"
        throwable is AuthenticationException -> throwable.message ?: "Unable to authenticate"
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
        throwable is IllegalArgumentException -> throwable.message ?: "Invalid server address"
        else -> "Could not reach the Jellyfin server"
    }
}

private class AuthenticationException(message: String) : RuntimeException(message)
private class SessionRestoreException(message: String) : RuntimeException(message)
