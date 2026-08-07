package dev.vantafyn.core.jellyfin

import java.util.UUID

data class JellyfinServerConfig(
    val url: String,
    val name: String? = null,
    val version: String? = null,
    val serverId: String? = null,
    val localId: String = serverId ?: url,
)

data class JellyfinUser(
    val id: UUID,
    val name: String,
    val serverName: String? = null,
    val primaryImageTag: String? = null,
    val isAdministrator: Boolean = false,
)

data class JellyfinSession(
    val server: JellyfinServerConfig,
    val user: JellyfinUser,
    val profileId: String,
    internal val accessToken: String,
)

data class JellyfinLibrary(
    val id: UUID,
    val name: String,
    val collectionType: String?,
    val primaryImageTag: String? = null,
    val imageUrl: String? = null,
)

enum class JellyfinMediaCardShape {
    Poster,
    Wide,
    Library,
}

data class JellyfinMediaCard(
    val id: UUID,
    val title: String,
    val subtitle: String?,
    val year: Int?,
    val itemType: String?,
    val imageUrl: String?,
    val backdropUrl: String?,
    val thumbUrl: String? = null,
    val logoUrl: String? = null,
    val progress: Float?,
    val shape: JellyfinMediaCardShape,
    val isFavorite: Boolean = false,
)

data class JellyfinHeroMediaItem(
    val id: UUID,
    val title: String,
    val subtitle: String?,
    val overview: String?,
    val year: Int?,
    val runtimeMinutes: Int?,
    val officialRating: String?,
    val communityRating: Float?,
    val genres: List<String> = emptyList(),
    val backdropUrl: String?,
    val logoUrl: String?,
    val posterUrl: String?,
)

data class JellyfinHomeSection(
    val title: String,
    val items: List<JellyfinMediaCard>,
)

data class JellyfinHome(
    val heroItems: List<JellyfinHeroMediaItem>,
    val sections: List<JellyfinHomeSection>,
    val liveTvChannels: List<JellyfinLiveTvChannel> = emptyList(),
    val liveTvPrograms: List<JellyfinLiveTvProgram> = emptyList(),
)

data class JellyfinMediaItem(
    val id: UUID,
    val title: String,
    val subtitle: String?,
    val year: Int?,
    val itemType: String?,
    val imageUrl: String?,
    val backdropUrl: String?,
    val thumbUrl: String? = null,
    val logoUrl: String?,
    val progress: Float?,
    val shape: JellyfinMediaCardShape,
    val isFavorite: Boolean = false,
)

enum class MediaAvailabilityState {
    AvailableInJellyfin,
    NotAvailable,
    Requested,
    Pending,
    Approved,
    PartiallyAvailable,
    Unknown,
}

data class JellyfinAvailabilityMatch(
    val itemId: UUID,
    val title: String,
    val itemType: String?,
    val serverId: String?,
    val serverName: String?,
    val matchedProvider: String,
    val matchedProviderId: String,
)

data class JellyfinAvailabilityIndex(
    val moviesCount: Int,
    val seriesCount: Int,
    val lastBuiltAt: Long,
    val sourceServerId: String?,
    val sourceServerName: String?,
    val failedServers: List<String> = emptyList(),
    val itemsByProviderId: Map<String, JellyfinAvailabilityMatch>,
) {
    fun find(providerIds: Map<String, String>): JellyfinAvailabilityMatch? =
        ProviderIdMatcher.find(providerIds, itemsByProviderId)
}

object ProviderIdMatcher {
    fun key(provider: String, id: String): String =
        "${provider.trim().lowercase()}:${id.trim().lowercase()}"

    fun find(providerIds: Map<String, String>, index: Map<String, JellyfinAvailabilityMatch>): JellyfinAvailabilityMatch? =
        providerIds.asSequence()
            .filter { it.key.isNotBlank() && it.value.isNotBlank() }
            .mapNotNull { (provider, id) -> index[key(provider, id)] }
            .firstOrNull()
}

data class JellyfinLibraryPage(
    val items: List<JellyfinMediaItem>,
    val startIndex: Int,
    val pageSize: Int,
    val totalItems: Int,
) {
    val currentPage: Int
        get() = if (pageSize <= 0) 1 else (startIndex / pageSize) + 1
    val totalPages: Int
        get() = if (pageSize <= 0 || totalItems <= 0) 1 else ((totalItems - 1) / pageSize) + 1
    val hasPrevious: Boolean
        get() = startIndex > 0
    val hasNext: Boolean
        get() = startIndex + items.size < totalItems
}

data class JellyfinLiveTvChannel(
    val id: UUID,
    val name: String,
    val number: String?,
    val imageUrl: String?,
    val currentProgramName: String?,
    val currentProgramStart: String? = null,
    val currentProgramEnd: String? = null,
)

data class JellyfinLiveTvProgram(
    val id: UUID,
    val channelId: UUID?,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val startDate: String?,
    val endDate: String?,
)

data class JellyfinMediaDetail(
    val id: UUID,
    val title: String,
    val subtitle: String?,
    val year: Int?,
    val runtimeMinutes: Int?,
    val officialRating: String?,
    val communityRating: Float?,
    val overview: String?,
    val genres: List<String>,
    val itemType: String?,
    val imageUrl: String?,
    val backdropUrl: String?,
    val logoUrl: String?,
    val isFavorite: Boolean,
    val isPlayed: Boolean,
    val progress: Float?,
    val playbackPositionTicks: Long = 0L,
    val streamInfo: List<String> = emptyList(),
    val mediaInfo: List<JellyfinMediaInfoLine> = emptyList(),
    val mediaSources: List<JellyfinMediaSourceSummary> = emptyList(),
    val people: List<JellyfinPerson> = emptyList(),
    val seasons: List<JellyfinSeason> = emptyList(),
    val episodes: List<JellyfinEpisode> = emptyList(),
    val related: List<JellyfinMediaItem> = emptyList(),
    val externalLinks: List<JellyfinExternalLink> = emptyList(),
    val themeSongUrl: String? = null,
    val seriesId: UUID? = null,
    val seasonId: UUID? = null,
    val seriesName: String? = null,
    val seasonIndexNumber: Int? = null,
    val episodeIndexNumber: Int? = null,
)

data class JellyfinMediaInfoLine(
    val label: String,
    val value: String,
    val adminOnly: Boolean = false,
)

data class JellyfinMediaSourceSummary(
    val id: String?,
    val name: String?,
    val container: String?,
    val path: String?,
    val sizeLabel: String?,
    val bitrateLabel: String?,
    val videoCodec: String?,
    val audioCodec: String?,
    val resolution: String?,
    val dynamicRange: String?,
    val audioTracks: List<String>,
    val subtitleTracks: List<String>,
)

enum class JellyfinPlaybackMethod {
    DirectPlay,
    DirectStream,
    Transcode,
}

data class JellyfinPlaybackInfo(
    val itemId: UUID,
    val title: String,
    val subtitle: String?,
    val streamUrl: String,
    val fallbackStreamUrl: String?,
    val playSessionId: String?,
    val mediaSourceId: String?,
    val liveStreamId: String?,
    val method: JellyfinPlaybackMethod,
    val runtimeTicks: Long?,
    val startPositionTicks: Long,
    val audioStreamIndex: Int?,
    val subtitleStreamIndex: Int?,
    val audioTracks: List<JellyfinAudioTrack>,
    val subtitleTracks: List<JellyfinSubtitleTrack>,
    val sourceLabel: String?,
    val isLiveStream: Boolean = false,
)

data class JellyfinAudioTrack(
    val index: Int,
    val label: String,
    val language: String?,
    val codec: String?,
    val channels: Int?,
    val isDefault: Boolean,
)

data class JellyfinSubtitleTrack(
    val index: Int,
    val label: String,
    val language: String?,
    val codec: String?,
    val isExternal: Boolean,
    val isDefault: Boolean,
    val deliveryUrl: String?,
)

data class JellyfinPerson(
    val id: UUID,
    val name: String,
    val role: String?,
    val type: String?,
    val imageUrl: String?,
)

data class JellyfinSeason(
    val id: UUID,
    val title: String,
    val indexNumber: Int?,
)

data class JellyfinEpisode(
    val id: UUID,
    val title: String,
    val subtitle: String?,
    val overview: String?,
    val imageUrl: String?,
    val progress: Float?,
    val playbackPositionTicks: Long = 0L,
    val isPlayed: Boolean = false,
    val runtimeMinutes: Int? = null,
    val indexNumber: Int?,
    val seasonIndexNumber: Int?,
    val seriesId: UUID? = null,
    val seasonId: UUID? = null,
    val seriesName: String? = null,
)

data class JellyfinUpNextCandidate(
    val itemId: UUID,
    val seriesId: UUID?,
    val seasonId: UUID?,
    val title: String,
    val seriesName: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val runtimeTicks: Long?,
    val imageUrl: String?,
    val backdropUrl: String?,
    val overview: String?,
    val progress: Float?,
    val playbackPositionTicks: Long,
)

data class JellyfinExternalLink(
    val name: String,
    val url: String,
)

data class JellyfinSearchResult(
    val id: UUID,
    val title: String,
    val subtitle: String?,
    val year: Int?,
    val itemType: String?,
    val imageUrl: String?,
    val backdropUrl: String?,
    val shape: JellyfinMediaCardShape,
    val isFavorite: Boolean = false,
)

data class JellyfinMusicLibrary(
    val id: UUID,
    val name: String,
    val imageUrl: String?,
)

data class JellyfinMusicTrack(
    val id: UUID,
    val title: String,
    val artist: String,
    val album: String?,
    val albumId: UUID?,
    val durationMs: Long?,
    val artworkUrl: String?,
    val hasLyrics: Boolean,
    val streamUrl: String,
    val playlistItemId: String? = null,
    val isFavorite: Boolean = false,
)

data class JellyfinMusicAlbum(
    val id: UUID,
    val title: String,
    val artist: String?,
    val year: Int?,
    val artworkUrl: String?,
)

data class JellyfinMusicArtist(
    val id: UUID,
    val name: String,
    val imageUrl: String?,
)

data class JellyfinMusicPlaylist(
    val id: UUID,
    val name: String,
    val imageUrl: String?,
    val trackCount: Int?,
)

data class JellyfinMusicHome(
    val libraries: List<JellyfinMusicLibrary>,
    val recentlyAdded: List<JellyfinMusicTrack>,
    val albums: List<JellyfinMusicAlbum>,
    val artists: List<JellyfinMusicArtist>,
    val playlists: List<JellyfinMusicPlaylist>,
    val songs: List<JellyfinMusicTrack>,
)

data class JellyfinLyricLine(
    val startMs: Long?,
    val text: String,
)

data class JellyfinLyrics(
    val plainText: String,
    val syncedLines: List<JellyfinLyricLine>,
    val source: String,
) {
    val isSynced: Boolean = syncedLines.any { it.startMs != null }
}

data class JellyfinQuickConnectSession(
    val server: JellyfinServerConfig,
    val secret: String,
    val code: String,
)

data class JellyfinPublicUser(
    val server: JellyfinServerConfig,
    val id: UUID,
    val displayName: String,
    val primaryImageTag: String?,
    val imageUrl: String?,
    val hasPassword: Boolean,
    val isAdministrator: Boolean = false,
)

data class JellyfinAdminOverview(
    val serverName: String?,
    val serverVersion: String?,
    val operatingSystem: String?,
    val activeSessions: List<JellyfinAdminSession>,
    val connectedSessionCount: Int,
    val users: List<JellyfinAdminUser>,
    val libraryCount: Int,
    val totalItems: Int?,
    val moviesCount: Int?,
    val seriesCount: Int?,
    val episodesCount: Int?,
    val musicCount: Int?,
    val plugins: List<JellyfinAdminPlugin> = emptyList(),
    val tasks: List<JellyfinAdminTask> = emptyList(),
    val recentActivity: List<JellyfinAdminActivity> = emptyList(),
    val devices: List<JellyfinAdminDevice> = emptyList(),
    val serverLogs: List<JellyfinAdminLogFile> = emptyList(),
    val unavailableStats: List<String>,
)

data class JellyfinAdminPlugin(
    val id: UUID,
    val name: String,
    val version: String?,
    val description: String?,
    val status: String?,
    val hasImage: Boolean,
    val canUninstall: Boolean,
)

data class JellyfinAdminTask(
    val id: String,
    val name: String,
    val category: String?,
    val state: String?,
    val progress: Double?,
    val lastStatus: String?,
    val lastEnded: String?,
)

data class JellyfinAdminActivity(
    val id: Long,
    val name: String,
    val shortOverview: String?,
    val type: String?,
    val date: String?,
    val severity: String?,
)

data class JellyfinAdminDevice(
    val id: String,
    val name: String,
    val appName: String?,
    val appVersion: String?,
    val lastUserName: String?,
    val lastActivity: String?,
    val iconUrl: String?,
)

data class JellyfinAdminLogFile(
    val name: String,
    val sizeBytes: Long,
    val modified: String?,
)

data class JellyfinAdminSession(
    val id: String,
    val userId: UUID?,
    val userName: String?,
    val userImageUrl: String?,
    val client: String?,
    val deviceName: String?,
    val remoteEndPoint: String?,
    val nowPlayingTitle: String?,
    val nowPlayingSubtitle: String?,
    val nowPlayingImageUrl: String?,
    val nowPlayingBackdropUrl: String?,
    val nowPlayingType: String?,
    val playMethod: String?,
    val isPaused: Boolean,
    val positionTicks: Long?,
    val runtimeTicks: Long?,
    val videoCodec: String?,
    val audioCodec: String?,
    val container: String?,
    val bitrate: Int?,
    val transcodeReasons: List<String>,
    val lastPlaybackCheckIn: String?,
    val isTranscoding: Boolean,
)

data class JellyfinAdminUser(
    val id: UUID,
    val name: String,
    val imageUrl: String?,
    val isAdministrator: Boolean,
    val isDisabled: Boolean,
    val isHidden: Boolean,
    val lastActivity: String?,
    val lastLogin: String?,
)

data class JellyfinUserPlaybackPreferences(
    val audioLanguagePreference: String?,
    val subtitleLanguagePreference: String?,
    val subtitleMode: String?,
    val playDefaultAudioTrack: Boolean,
    val rememberAudioSelections: Boolean,
    val rememberSubtitleSelections: Boolean,
    val enableNextEpisodeAutoPlay: Boolean,
)

data class JellyfinAdminUserDetail(
    val user: JellyfinAdminUser,
    val enableAllFolders: Boolean,
    val enabledFolderIds: List<UUID>,
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
    val profileId: String,
    val serverUrl: String,
    val serverName: String?,
    val serverVersion: String?,
    val serverId: String?,
    val userId: UUID,
    val userName: String,
    val userImageTag: String?,
    val accessToken: String,
    val lastUsedAt: Long,
)

data class SavedServer(
    val id: String,
    val name: String?,
    val baseUrl: String,
    val serverId: String?,
    val version: String?,
    val lastConnectedAt: Long,
)

data class SavedProfile(
    val id: String,
    val serverRef: String,
    val serverName: String?,
    val serverUrl: String,
    val jellyfinUserId: UUID,
    val displayName: String,
    val userImageTag: String?,
    val imageUrl: String?,
    val lastUsedAt: Long,
)

enum class JellyfinRestoreFailureReason {
    ServerUnreachable,
    NetworkUnavailable,
    AuthExpired,
    Unauthorized,
    InvalidServerUrl,
    ServerError,
    UnknownError,
}

class JellyfinSessionRestoreFailure(
    val reason: JellyfinRestoreFailureReason,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

interface JellyfinSessionStorage {
    suspend fun read(): StoredJellyfinSession?
    suspend fun read(profileId: String): StoredJellyfinSession?
    suspend fun readAll(): List<StoredJellyfinSession>
    suspend fun write(session: StoredJellyfinSession)
    suspend fun remove(profileId: String)
    suspend fun clear()
}

interface JellyfinAuthRepository {
    suspend fun savedProfiles(): List<SavedProfile>
    suspend fun restoreSession(profileId: String): JellyfinResult<JellyfinSession>
    suspend fun updateSavedServerUrl(profileId: String, serverUrl: String): JellyfinResult<JellyfinSession>
    suspend fun testServer(serverUrl: String): JellyfinResult<JellyfinServerConfig>
    suspend fun publicUsers(server: JellyfinServerConfig): JellyfinResult<List<JellyfinPublicUser>>
    suspend fun login(serverUrl: String, username: String, password: String): JellyfinResult<JellyfinSession>
    suspend fun removeProfile(profileId: String)
    suspend fun logout()
}

interface JellyfinLibraryRepository {
    suspend fun getLibraries(session: JellyfinSession): JellyfinResult<List<JellyfinLibrary>>
    suspend fun getLibraryItems(session: JellyfinSession, library: JellyfinLibrary, limit: Int = 60): JellyfinResult<List<JellyfinMediaItem>>
    suspend fun getLibraryItemsPage(
        session: JellyfinSession,
        library: JellyfinLibrary,
        startIndex: Int,
        limit: Int = 60,
    ): JellyfinResult<JellyfinLibraryPage>
    suspend fun buildAvailabilityIndex(session: JellyfinSession): JellyfinResult<JellyfinAvailabilityIndex>
}

interface JellyfinHomeRepository {
    suspend fun getHome(session: JellyfinSession, libraries: List<JellyfinLibrary>): JellyfinResult<JellyfinHome>
}

interface JellyfinMediaRepository {
    suspend fun getMediaDetail(session: JellyfinSession, itemId: UUID): JellyfinResult<JellyfinMediaDetail>
    suspend fun getSeasonEpisodes(session: JellyfinSession, seriesId: UUID, seasonId: UUID?): JellyfinResult<List<JellyfinEpisode>>
    suspend fun getNextEpisode(session: JellyfinSession, currentEpisodeId: UUID): JellyfinResult<JellyfinUpNextCandidate?>
    suspend fun setFavorite(session: JellyfinSession, itemId: UUID, isFavorite: Boolean): JellyfinResult<Boolean>
    suspend fun addFavorite(session: JellyfinSession, itemId: UUID): JellyfinResult<Boolean> =
        setFavorite(session, itemId, true)
    suspend fun removeFavorite(session: JellyfinSession, itemId: UUID): JellyfinResult<Boolean> =
        setFavorite(session, itemId, false)
    suspend fun refreshFavoriteState(session: JellyfinSession, itemId: UUID): JellyfinResult<Boolean>
    suspend fun setPlayed(session: JellyfinSession, itemId: UUID, isPlayed: Boolean): JellyfinResult<Boolean>
}

interface JellyfinPlaybackRepository {
    suspend fun getPlaybackInfo(
        session: JellyfinSession,
        itemId: UUID,
        title: String,
        subtitle: String?,
        startPositionTicks: Long = 0L,
        forceTranscode: Boolean = false,
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null,
        isLiveTv: Boolean = false,
    ): JellyfinResult<JellyfinPlaybackInfo>

    suspend fun reportStarted(session: JellyfinSession, info: JellyfinPlaybackInfo, positionTicks: Long): JellyfinResult<Unit>
    suspend fun reportProgress(session: JellyfinSession, info: JellyfinPlaybackInfo, positionTicks: Long, isPaused: Boolean): JellyfinResult<Unit>
    suspend fun reportStopped(session: JellyfinSession, info: JellyfinPlaybackInfo, positionTicks: Long): JellyfinResult<Unit>
}

interface JellyfinSearchRepository {
    suspend fun search(session: JellyfinSession, query: String, limit: Int = 40): JellyfinResult<List<JellyfinSearchResult>>
}

interface JellyfinFavoritesRepository {
    suspend fun getFavorites(session: JellyfinSession, limit: Int = 80): JellyfinResult<List<JellyfinMediaItem>>
}

interface JellyfinMusicRepository {
    suspend fun getMusicHome(session: JellyfinSession): JellyfinResult<JellyfinMusicHome>
    suspend fun getAlbumTracks(session: JellyfinSession, albumId: UUID): JellyfinResult<List<JellyfinMusicTrack>>
    suspend fun getArtistAlbums(session: JellyfinSession, artistId: UUID): JellyfinResult<List<JellyfinMusicAlbum>>
    suspend fun getPlaylistItems(session: JellyfinSession, playlistId: UUID): JellyfinResult<List<JellyfinMusicTrack>>
    suspend fun searchMusic(session: JellyfinSession, query: String, limit: Int = 50): JellyfinResult<List<JellyfinMusicTrack>>
    suspend fun getLyrics(session: JellyfinSession, trackId: UUID): JellyfinResult<JellyfinLyrics?>
    suspend fun createPlaylist(session: JellyfinSession, name: String, itemIds: List<UUID>): JellyfinResult<UUID>
    suspend fun addToPlaylist(session: JellyfinSession, playlistId: UUID, itemIds: List<UUID>): JellyfinResult<Unit>
    suspend fun removeFromPlaylist(session: JellyfinSession, playlistId: UUID, playlistItemIds: List<String>): JellyfinResult<Unit>
}

interface JellyfinAdminRepository {
    suspend fun getOverview(session: JellyfinSession, libraries: List<JellyfinLibrary>): JellyfinResult<JellyfinAdminOverview>
    suspend fun createUser(session: JellyfinSession, username: String, password: String): JellyfinResult<JellyfinAdminUserDetail>
    suspend fun getUserDetail(session: JellyfinSession, userId: UUID): JellyfinResult<JellyfinAdminUserDetail>
    suspend fun updateUserPolicy(
        session: JellyfinSession,
        userId: UUID,
        isHidden: Boolean? = null,
        isDisabled: Boolean? = null,
        isAdministrator: Boolean? = null,
        enableAllFolders: Boolean? = null,
        enabledFolderIds: List<UUID>? = null,
    ): JellyfinResult<JellyfinAdminUserDetail>
    suspend fun resetUserPassword(session: JellyfinSession, userId: UUID, newPassword: String): JellyfinResult<Unit>
    suspend fun scanLibrary(session: JellyfinSession): JellyfinResult<Unit>
    suspend fun setPluginEnabled(session: JellyfinSession, pluginId: UUID, version: String, enabled: Boolean): JellyfinResult<Unit>
    suspend fun setScheduledTaskRunning(session: JellyfinSession, taskId: String, running: Boolean): JellyfinResult<Unit>
}

interface JellyfinUserPreferencesRepository {
    suspend fun getPlaybackPreferences(session: JellyfinSession): JellyfinResult<JellyfinUserPlaybackPreferences>
    suspend fun updatePlaybackPreferences(
        session: JellyfinSession,
        preferences: JellyfinUserPlaybackPreferences,
    ): JellyfinResult<JellyfinUserPlaybackPreferences>
    suspend fun changePassword(session: JellyfinSession, currentPassword: String, newPassword: String): JellyfinResult<Unit>
}

interface JellyfinQuickConnectRepository {
    suspend fun initiate(server: JellyfinServerConfig): JellyfinResult<JellyfinQuickConnectSession>
    suspend fun poll(session: JellyfinQuickConnectSession): JellyfinResult<JellyfinSession?>
}
