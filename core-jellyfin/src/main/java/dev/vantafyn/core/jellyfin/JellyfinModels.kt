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
)

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
    val streamInfo: List<String> = emptyList(),
    val people: List<JellyfinPerson> = emptyList(),
    val seasons: List<JellyfinSeason> = emptyList(),
    val episodes: List<JellyfinEpisode> = emptyList(),
    val related: List<JellyfinMediaItem> = emptyList(),
    val externalLinks: List<JellyfinExternalLink> = emptyList(),
    val themeSongUrl: String? = null,
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
    val indexNumber: Int?,
    val seasonIndexNumber: Int?,
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
)

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
    val users: List<JellyfinAdminUser>,
    val libraryCount: Int,
    val totalItems: Int?,
    val moviesCount: Int?,
    val seriesCount: Int?,
    val episodesCount: Int?,
    val musicCount: Int?,
    val unavailableStats: List<String>,
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
    suspend fun testServer(serverUrl: String): JellyfinResult<JellyfinServerConfig>
    suspend fun publicUsers(server: JellyfinServerConfig): JellyfinResult<List<JellyfinPublicUser>>
    suspend fun login(serverUrl: String, username: String, password: String): JellyfinResult<JellyfinSession>
    suspend fun removeProfile(profileId: String)
    suspend fun logout()
}

interface JellyfinLibraryRepository {
    suspend fun getLibraries(session: JellyfinSession): JellyfinResult<List<JellyfinLibrary>>
    suspend fun getLibraryItems(session: JellyfinSession, library: JellyfinLibrary, limit: Int = 60): JellyfinResult<List<JellyfinMediaItem>>
}

interface JellyfinHomeRepository {
    suspend fun getHome(session: JellyfinSession, libraries: List<JellyfinLibrary>): JellyfinResult<JellyfinHome>
}

interface JellyfinMediaRepository {
    suspend fun getMediaDetail(session: JellyfinSession, itemId: UUID): JellyfinResult<JellyfinMediaDetail>
    suspend fun setFavorite(session: JellyfinSession, itemId: UUID, isFavorite: Boolean): JellyfinResult<Boolean>
    suspend fun addFavorite(session: JellyfinSession, itemId: UUID): JellyfinResult<Boolean> =
        setFavorite(session, itemId, true)
    suspend fun removeFavorite(session: JellyfinSession, itemId: UUID): JellyfinResult<Boolean> =
        setFavorite(session, itemId, false)
    suspend fun refreshFavoriteState(session: JellyfinSession, itemId: UUID): JellyfinResult<Boolean>
    suspend fun setPlayed(session: JellyfinSession, itemId: UUID, isPlayed: Boolean): JellyfinResult<Boolean>
}

interface JellyfinSearchRepository {
    suspend fun search(session: JellyfinSession, query: String, limit: Int = 40): JellyfinResult<List<JellyfinSearchResult>>
}

interface JellyfinFavoritesRepository {
    suspend fun getFavorites(session: JellyfinSession, limit: Int = 80): JellyfinResult<List<JellyfinMediaItem>>
}

interface JellyfinAdminRepository {
    suspend fun getOverview(session: JellyfinSession, libraries: List<JellyfinLibrary>): JellyfinResult<JellyfinAdminOverview>
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
