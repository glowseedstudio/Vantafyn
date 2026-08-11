package dev.vantafyn.core.ombi

import android.content.Context
import android.util.Log
import dev.vantafyn.core.integrations.EncryptedIntegrationAuthStorage
import dev.vantafyn.core.integrations.IntegrationCapability
import dev.vantafyn.core.integrations.IntegrationConnectionState
import dev.vantafyn.core.integrations.IntegrationFailureReason
import dev.vantafyn.core.integrations.IntegrationResult
import dev.vantafyn.core.integrations.IntegrationType
import dev.vantafyn.core.integrations.MediaRequestItem
import dev.vantafyn.core.integrations.MediaRequestProvider
import dev.vantafyn.core.integrations.MediaRequestSearchResult
import dev.vantafyn.core.integrations.MediaRequestStatus
import dev.vantafyn.core.integrations.MediaRequestType
import dev.vantafyn.core.integrations.VantafynIntegration
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

data class OmbiConfig(
    val enabled: Boolean = false,
    val baseUrl: String = "",
    val apiAlias: String? = null,
    val accessMode: OmbiAccessMode = if (enabled) OmbiAccessMode.AllUsers else OmbiAccessMode.Disabled,
    val identityMode: OmbiIdentityMode = OmbiIdentityMode.SharedApiKey,
    val lastSuccessfulConnectionAt: Long? = null,
    val lastFailureMessage: String? = null,
    val version: String? = null,
    val capabilities: OmbiCapabilities = OmbiCapabilities(),
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank()
    val isEnabledForUsers: Boolean get() = isConfigured && accessMode == OmbiAccessMode.AllUsers
    val isEnabledForAdmins: Boolean get() = isConfigured && accessMode != OmbiAccessMode.Disabled
    fun isAvailableFor(isAdmin: Boolean): Boolean =
        isConfigured && (accessMode == OmbiAccessMode.AllUsers || (isAdmin && accessMode == OmbiAccessMode.AdminsOnly))
}

enum class OmbiAccessMode {
    Disabled,
    AdminsOnly,
    AllUsers,
}

enum class OmbiIdentityMode {
    SharedApiKey,
    PerUserAccount,
}

enum class OmbiLinkedAccountState {
    NotLinked,
    AccessRequested,
    AccountCreated,
    Linked,
    TokenExpired,
    Disabled,
}

enum class OmbiAccessRequestStatus {
    Pending,
    Seen,
    AccountCreated,
    Linked,
    Dismissed,
}

data class OmbiAccessRequestKey(
    val serverAccountId: String,
    val profileId: String,
    val integrationId: String,
)

data class OmbiAccessRequest(
    val jellyfinUserId: String,
    val jellyfinUserName: String,
    val serverName: String?,
    val requestedAt: Long,
    val suggestedOmbiUserName: String,
    val status: OmbiAccessRequestStatus = OmbiAccessRequestStatus.Pending,
    val note: String? = null,
    val serverAccountId: String? = null,
    val integrationId: String? = null,
)

data class OmbiUserProfile(
    val id: String?,
    val userName: String,
    val email: String?,
    val displayName: String?,
)

enum class OmbiUserMatchState {
    NotChecked,
    MatchFound,
    NoMatchFound,
    UnknownUnavailable,
}

data class OmbiUserMatch(
    val state: OmbiUserMatchState,
    val user: OmbiUserProfile? = null,
    val confidence: String? = null,
)

data class OmbiUserMapping(
    val jellyfinUserId: String,
    val jellyfinUserName: String,
    val ombiUserName: String?,
    val state: OmbiLinkedAccountState = OmbiLinkedAccountState.NotLinked,
    val updatedAt: Long = System.currentTimeMillis(),
    val note: String? = null,
)

data class OmbiUserSession(
    val jellyfinUserId: String,
    val ombiUserName: String,
    val displayName: String?,
    val ombiUserId: String?,
    val expiresAt: String?,
    val roles: List<String> = emptyList(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val lastValidatedAt: Long? = null,
) {
    val bestName: String get() = displayName?.takeIf { it.isNotBlank() } ?: ombiUserName
}

enum class RequestMediaType {
    Movie,
    Series,
}

enum class RequestState {
    NotRequested,
    PendingApproval,
    Approved,
    Processing,
    PartiallyAvailable,
    Available,
    Declined,
    Failed,
    Unknown,
}

enum class OmbiTvRequestSelection {
    AllSeasons,
    FirstSeason,
    LatestSeason,
}

data class RequestMediaSummary(
    val externalId: String,
    val movieDbId: String?,
    val tvDbId: String?,
    val imdbId: String?,
    val mediaType: RequestMediaType,
    val title: String,
    val originalTitle: String?,
    val year: Int?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val overview: String?,
    val rating: Double?,
    val state: RequestState,
    val requestId: String?,
    val isAvailableInJellyfin: Boolean,
    val availableSeasonCount: Int?,
    val totalSeasonCount: Int?,
    val requestedBy: String? = null,
    val requestedDate: String? = null,
)

data class RequestSeasonSummary(
    val seasonNumber: Int,
    val overview: String?,
    val state: RequestState,
    val available: Boolean,
    val episodes: List<RequestEpisodeSummary> = emptyList(),
)

data class RequestEpisodeSummary(
    val episodeNumber: Int,
    val title: String?,
    val airDate: String?,
    val state: RequestState,
    val available: Boolean,
)

data class RequestMediaDetail(
    val summary: RequestMediaSummary,
    val tagline: String?,
    val runtimeMinutes: Int?,
    val genres: List<String>,
    val certification: String?,
    val network: String?,
    val seasons: List<RequestSeasonSummary>,
    val similar: List<RequestMediaSummary>,
)

enum class OmbiDiscoverRailKind {
    SearchResults,
    PopularMovies,
    NowPlayingMovies,
    UpcomingMovies,
    TopRatedMovies,
    TrendingSeries,
    PopularSeries,
    AnticipatedSeries,
    MostWatchedSeries,
    RecentlyRequested,
    RecentlyAvailable,
    MyRequests,
    FamilyQueue,
}

data class OmbiDiscoverRail(
    val kind: OmbiDiscoverRailKind,
    val title: String,
    val items: List<RequestMediaSummary>,
)

data class OmbiUserCapabilities(
    val canRequestMovies: Boolean,
    val canRequestSeries: Boolean,
    val canRequestSeasons: Boolean,
    val canRequestEpisodes: Boolean,
    val canManageOwnRequests: Boolean,
    val canViewOtherRequests: Boolean,
    val canManageRequests: Boolean,
    val moviesAutoApproved: Boolean,
    val seriesAutoApproved: Boolean,
    val remainingMovieRequests: Int?,
    val remainingSeriesRequests: Int?,
)

data class OmbiCapabilities(
    val movieSearch: Boolean = false,
    val tvSearch: Boolean = false,
    val movieRequest: Boolean = false,
    val tvRequest: Boolean = false,
    val userRequestListing: Boolean = false,
    val adminModeration: Boolean = false,
) {
    val labels: List<String>
        get() = buildList {
            if (movieSearch) add("Movie search")
            if (tvSearch) add("TV search")
            if (movieRequest) add("Movie requests")
            if (tvRequest) add("TV requests")
            if (userRequestListing) add("Request history")
            if (adminModeration) add("Admin moderation")
        }
}

data class OmbiConnectionReport(
    val displayName: String = "Ombi",
    val version: String? = null,
    val capabilities: OmbiCapabilities,
)

class OmbiConfigStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("vantafyn_ombi_config", Context.MODE_PRIVATE)
    private val secrets = EncryptedIntegrationAuthStorage(context)

    fun read(): OmbiConfig =
        OmbiConfig(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            baseUrl = preferences.getString(KEY_BASE_URL, null).orEmpty(),
            apiAlias = preferences.getString(KEY_API_ALIAS, null),
            accessMode = preferences.getString(KEY_ACCESS_MODE, null)
                ?.let { runCatching { OmbiAccessMode.valueOf(it) }.getOrNull() }
                ?: if (preferences.getBoolean(KEY_ENABLED, false)) OmbiAccessMode.AllUsers else OmbiAccessMode.Disabled,
            identityMode = preferences.getString(KEY_IDENTITY_MODE, null)
                ?.let { runCatching { OmbiIdentityMode.valueOf(it) }.getOrNull() }
                ?: OmbiIdentityMode.SharedApiKey,
            lastSuccessfulConnectionAt = preferences.getLong(KEY_LAST_SUCCESS, 0L).takeIf { it > 0L },
            lastFailureMessage = preferences.getString(KEY_LAST_FAILURE, null),
            version = preferences.getString(KEY_VERSION, null),
            capabilities = OmbiCapabilities(
                movieSearch = preferences.getBoolean(KEY_CAP_MOVIE_SEARCH, false),
                tvSearch = preferences.getBoolean(KEY_CAP_TV_SEARCH, false),
                movieRequest = preferences.getBoolean(KEY_CAP_MOVIE_REQUEST, false),
                tvRequest = preferences.getBoolean(KEY_CAP_TV_REQUEST, false),
                userRequestListing = preferences.getBoolean(KEY_CAP_REQUEST_LISTING, false),
                adminModeration = preferences.getBoolean(KEY_CAP_ADMIN_MODERATION, false),
            ),
        )

    fun readApiKey(): String? = secrets.readSecret(SECRET_API_KEY)

    fun save(enabled: Boolean, baseUrl: String, apiKey: String, apiAlias: String?) {
        save(
            accessMode = if (enabled) OmbiAccessMode.AllUsers else OmbiAccessMode.Disabled,
            baseUrl = baseUrl,
            apiKey = apiKey,
            apiAlias = apiAlias,
        )
    }

    fun save(accessMode: OmbiAccessMode, baseUrl: String, apiKey: String, apiAlias: String?) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, accessMode != OmbiAccessMode.Disabled)
            .putString(KEY_ACCESS_MODE, accessMode.name)
            .putString(KEY_BASE_URL, normalizeBaseUrl(baseUrl))
            .putString(KEY_API_ALIAS, apiAlias?.takeIf { it.isNotBlank() })
            .apply()
        if (apiKey.isNotBlank()) secrets.saveSecret(SECRET_API_KEY, apiKey.trim())
    }

    fun setEnabled(enabled: Boolean) {
        setAccessMode(if (enabled) OmbiAccessMode.AllUsers else OmbiAccessMode.Disabled)
    }

    fun setAccessMode(mode: OmbiAccessMode) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, mode != OmbiAccessMode.Disabled)
            .putString(KEY_ACCESS_MODE, mode.name)
            .apply()
    }

    fun setIdentityMode(mode: OmbiIdentityMode) {
        preferences.edit().putString(KEY_IDENTITY_MODE, mode.name).apply()
    }

    fun saveConnectionReport(report: OmbiConnectionReport) {
        preferences.edit()
            .putLong(KEY_LAST_SUCCESS, System.currentTimeMillis())
            .remove(KEY_LAST_FAILURE)
            .putString(KEY_VERSION, report.version)
            .putBoolean(KEY_CAP_MOVIE_SEARCH, report.capabilities.movieSearch)
            .putBoolean(KEY_CAP_TV_SEARCH, report.capabilities.tvSearch)
            .putBoolean(KEY_CAP_MOVIE_REQUEST, report.capabilities.movieRequest)
            .putBoolean(KEY_CAP_TV_REQUEST, report.capabilities.tvRequest)
            .putBoolean(KEY_CAP_REQUEST_LISTING, report.capabilities.userRequestListing)
            .putBoolean(KEY_CAP_ADMIN_MODERATION, report.capabilities.adminModeration)
            .apply()
    }

    fun saveFailure(message: String) {
        preferences.edit().putString(KEY_LAST_FAILURE, message).apply()
    }

    fun clear() {
        readSessions().forEach { session ->
            secrets.removeSecret(session.secretKey)
        }
        preferences.edit().clear().apply()
        secrets.removeSecret(SECRET_API_KEY)
    }

    fun readAccessRequests(): List<OmbiAccessRequest> =
        preferences.getString(KEY_ACCESS_REQUESTS, null)
            ?.let { runCatching { JSONArray(it).toAccessRequests() }.getOrDefault(emptyList()) }
            .orEmpty()

    fun saveAccessRequests(requests: List<OmbiAccessRequest>) {
        preferences.edit().putString(KEY_ACCESS_REQUESTS, requests.toAccessRequestsJson().toString()).apply()
    }

    fun readMappings(): List<OmbiUserMapping> =
        preferences.getString(KEY_USER_MAPPINGS, null)
            ?.let { runCatching { JSONArray(it).toUserMappings() }.getOrDefault(emptyList()) }
            .orEmpty()

    fun saveMappings(mappings: List<OmbiUserMapping>) {
        preferences.edit().putString(KEY_USER_MAPPINGS, mappings.toUserMappingsJson().toString()).apply()
    }

    private fun readSessions(): List<OmbiStoredUserSession> =
        preferences.getString(KEY_USER_SESSIONS, null)
            ?.let { runCatching { JSONArray(it).toStoredUserSessions() }.getOrDefault(emptyList()) }
            .orEmpty()

    fun readSession(jellyfinUserId: String): OmbiUserSession? {
        val stored = readSessions().firstOrNull { it.jellyfinUserId == jellyfinUserId } ?: return null
        if (secrets.readSecret(stored.secretKey).isNullOrBlank()) return null
        return stored.toPublicSession()
    }

    fun readSessionToken(jellyfinUserId: String): String? {
        val stored = readSessions().firstOrNull { it.jellyfinUserId == jellyfinUserId } ?: return null
        return secrets.readSecret(stored.secretKey)?.takeIf { it.isNotBlank() }
    }

    fun saveSession(session: OmbiUserSession, accessToken: String) {
        val secretKey = userTokenSecretKey(session.jellyfinUserId)
        secrets.saveSecret(secretKey, accessToken)
        val updated = readSessions().filterNot { it.jellyfinUserId == session.jellyfinUserId } +
            OmbiStoredUserSession(
                jellyfinUserId = session.jellyfinUserId,
                ombiUserName = session.ombiUserName,
                displayName = session.displayName,
                ombiUserId = session.ombiUserId,
                expiresAt = session.expiresAt,
                roles = session.roles,
                lastLoginAt = session.lastLoginAt,
                lastValidatedAt = session.lastValidatedAt,
                secretKey = secretKey,
            )
        preferences.edit().putString(KEY_USER_SESSIONS, updated.toStoredUserSessionsJson().toString()).apply()
    }

    fun removeSession(jellyfinUserId: String) {
        readSessions().firstOrNull { it.jellyfinUserId == jellyfinUserId }?.let {
            secrets.removeSecret(it.secretKey)
        }
        preferences.edit()
            .putString(KEY_USER_SESSIONS, readSessions().filterNot { it.jellyfinUserId == jellyfinUserId }.toStoredUserSessionsJson().toString())
            .apply()
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_ACCESS_MODE = "access_mode"
        const val KEY_IDENTITY_MODE = "identity_mode"
        const val KEY_BASE_URL = "base_url"
        const val KEY_API_ALIAS = "api_alias"
        const val KEY_LAST_SUCCESS = "last_success"
        const val KEY_LAST_FAILURE = "last_failure"
        const val KEY_VERSION = "version"
        const val KEY_CAP_MOVIE_SEARCH = "cap_movie_search"
        const val KEY_CAP_TV_SEARCH = "cap_tv_search"
        const val KEY_CAP_MOVIE_REQUEST = "cap_movie_request"
        const val KEY_CAP_TV_REQUEST = "cap_tv_request"
        const val KEY_CAP_REQUEST_LISTING = "cap_request_listing"
        const val KEY_CAP_ADMIN_MODERATION = "cap_admin_moderation"
        const val KEY_ACCESS_REQUESTS = "access_requests"
        const val KEY_USER_MAPPINGS = "user_mappings"
        const val KEY_USER_SESSIONS = "user_sessions"
        const val SECRET_API_KEY = "ombi.api_key"
    }
}

private data class OmbiStoredUserSession(
    val jellyfinUserId: String,
    val ombiUserName: String,
    val displayName: String?,
    val ombiUserId: String?,
    val expiresAt: String?,
    val roles: List<String>,
    val lastLoginAt: Long,
    val lastValidatedAt: Long?,
    val secretKey: String,
) {
    fun toPublicSession(): OmbiUserSession =
        OmbiUserSession(
            jellyfinUserId = jellyfinUserId,
            ombiUserName = ombiUserName,
            displayName = displayName,
            ombiUserId = ombiUserId,
            expiresAt = expiresAt,
            roles = roles,
            lastLoginAt = lastLoginAt,
            lastValidatedAt = lastValidatedAt,
        )
}

class OmbiRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MediaRequestProvider {
    private val store = OmbiConfigStore(context)

    override val integration: VantafynIntegration
        get() = VantafynIntegration(
            type = IntegrationType.Ombi,
            name = "Ombi",
            capabilities = setOf(
                IntegrationCapability.Requests,
                IntegrationCapability.RequestStatus,
                IntegrationCapability.UserRequests,
                IntegrationCapability.SearchExternalMedia,
            ),
            enabled = store.read().isEnabledForUsers && store.readApiKey()?.isNotBlank() == true,
        )

    fun config(): OmbiConfig = store.read()
    fun hasApiKey(): Boolean = store.readApiKey()?.isNotBlank() == true
    fun saveConfig(enabled: Boolean, baseUrl: String, apiKey: String, apiAlias: String?) = store.save(enabled, baseUrl, apiKey, apiAlias)
    fun saveConfig(accessMode: OmbiAccessMode, baseUrl: String, apiKey: String, apiAlias: String?) = store.save(accessMode, baseUrl, apiKey, apiAlias)
    fun setEnabled(enabled: Boolean) = store.setEnabled(enabled)
    fun setAccessMode(mode: OmbiAccessMode) = store.setAccessMode(mode)
    fun setIdentityMode(mode: OmbiIdentityMode) = store.setIdentityMode(mode)
    fun clearConfig() = store.clear()
    fun accessRequests(): List<OmbiAccessRequest> = cleanupAccessRequests()
    fun userMappings(): List<OmbiUserMapping> = store.readMappings()
    fun userSession(jellyfinUserId: String): OmbiUserSession? = store.readSession(jellyfinUserId)
    fun pendingAccessRequestCount(): Int =
        cleanupAccessRequests().count { it.status == OmbiAccessRequestStatus.Pending }

    fun mappingFor(jellyfinUserId: String): OmbiUserMapping? =
        store.readMappings().firstOrNull { it.jellyfinUserId == jellyfinUserId }

    fun requestAccess(
        jellyfinUserId: String,
        jellyfinUserName: String,
        serverName: String?,
        note: String?,
    ): OmbiAccessRequest {
        val config = store.read()
        val key = accessRequestKey(jellyfinUserId, config)
        val requests = cleanupAccessRequests()
        val existing = requests.firstOrNull {
            it.key(config) == key && it.status != OmbiAccessRequestStatus.Dismissed && it.status != OmbiAccessRequestStatus.Linked
        }
        if (existing != null) return existing
        val request = OmbiAccessRequest(
            jellyfinUserId = jellyfinUserId,
            jellyfinUserName = jellyfinUserName,
            serverName = serverName,
            requestedAt = System.currentTimeMillis(),
            suggestedOmbiUserName = jellyfinUserName.toSuggestedOmbiUserName(),
            note = note?.takeIf { it.isNotBlank() },
            serverAccountId = key.serverAccountId,
            integrationId = key.integrationId,
        )
        store.saveAccessRequests(requests + request)
        upsertMapping(
            OmbiUserMapping(
                jellyfinUserId = jellyfinUserId,
                jellyfinUserName = jellyfinUserName,
                ombiUserName = request.suggestedOmbiUserName,
                state = OmbiLinkedAccountState.AccessRequested,
                note = note?.takeIf { it.isNotBlank() },
            ),
        )
        return request
    }

    fun updateAccessRequest(jellyfinUserId: String, status: OmbiAccessRequestStatus, note: String? = null) {
        val updated = cleanupAccessRequests().map {
            if (it.jellyfinUserId == jellyfinUserId) it.copy(status = status, note = note ?: it.note) else it
        }
        store.saveAccessRequests(updated)
        val mappingState = when (status) {
            OmbiAccessRequestStatus.Pending -> OmbiLinkedAccountState.AccessRequested
            OmbiAccessRequestStatus.Seen -> OmbiLinkedAccountState.AccessRequested
            OmbiAccessRequestStatus.AccountCreated -> OmbiLinkedAccountState.AccountCreated
            OmbiAccessRequestStatus.Linked -> OmbiLinkedAccountState.Linked
            OmbiAccessRequestStatus.Dismissed -> OmbiLinkedAccountState.NotLinked
        }
        cleanupAccessRequests().firstOrNull { it.jellyfinUserId == jellyfinUserId }?.let {
            upsertMapping(
                OmbiUserMapping(
                    jellyfinUserId = it.jellyfinUserId,
                    jellyfinUserName = it.jellyfinUserName,
                    ombiUserName = it.suggestedOmbiUserName,
                    state = mappingState,
                    note = note ?: it.note,
                ),
            )
        }
    }

    fun cleanupAccessRequests(): List<OmbiAccessRequest> {
        val config = store.read()
        val normalized = store.readAccessRequests().map {
            val key = it.key(config)
            it.copy(serverAccountId = key.serverAccountId, integrationId = key.integrationId)
        }
        val deduped = normalized
            .groupBy { it.key(config) }
            .values
            .map { group -> group.maxWith(accessRequestPriorityComparator) }
            .sortedByDescending { it.requestedAt }
        if (deduped != normalized) store.saveAccessRequests(deduped)
        return deduped
    }

    fun upsertMapping(mapping: OmbiUserMapping) {
        val mappings = store.readMappings().filterNot { it.jellyfinUserId == mapping.jellyfinUserId }
        store.saveMappings(mappings + mapping.copy(updatedAt = System.currentTimeMillis()))
    }

    fun clearMapping(jellyfinUserId: String) {
        store.saveMappings(store.readMappings().filterNot { it.jellyfinUserId == jellyfinUserId })
        store.removeSession(jellyfinUserId)
    }

    fun unlinkUserSession(jellyfinUserId: String) {
        store.removeSession(jellyfinUserId)
        mappingFor(jellyfinUserId)?.let {
            upsertMapping(it.copy(state = OmbiLinkedAccountState.AccountCreated))
        }
    }

    fun cachedUserCapabilities(jellyfinUserId: String?): OmbiUserCapabilities {
        val session = jellyfinUserId?.let(store::readSession)
        val claims = session?.roles.orEmpty().map { it.lowercase() }
        val canManage = claims.any { it.contains("admin") || it.contains("manage") || it.contains("requestadmin") }
        return OmbiUserCapabilities(
            canRequestMovies = claims.none { it.contains("deny") && it.contains("movie") },
            canRequestSeries = claims.none { it.contains("deny") && (it.contains("tv") || it.contains("series")) },
            canRequestSeasons = true,
            canRequestEpisodes = false,
            canManageOwnRequests = true,
            canViewOtherRequests = canManage,
            canManageRequests = canManage,
            moviesAutoApproved = claims.any { it.contains("autoapprove") && it.contains("movie") },
            seriesAutoApproved = claims.any { it.contains("autoapprove") && (it.contains("tv") || it.contains("series")) },
            remainingMovieRequests = null,
            remainingSeriesRequests = null,
        )
    }

    suspend fun loginUser(
        jellyfinUserId: String,
        jellyfinUserName: String,
        username: String,
        password: String,
    ): IntegrationResult<OmbiUserSession> =
        runOmbiCatching(requireApiKey = false) {
            if (username.isBlank() || password.isBlank()) {
                throw IllegalArgumentException("Enter your Ombi username and password.")
            }
            val tokenJson = request(
                path = "/api/v1/Token",
                method = "POST",
                body = JSONObject()
                    .put("username", username.trim())
                    .put("password", password)
                    .put("rememberMe", true)
                    .put("usePlexAdminAccount", false)
                    .put("usePlexOAuth", false),
                requestedBy = null,
                auth = OmbiAuth.None,
            ) as JSONObject
            val token = tokenJson.optNullableString("access_token")
                ?: tokenJson.optNullableString("accessToken")
                ?: throw OmbiHttpException(401, "Ombi did not return an access token.")
            val identity = request("/api/v1/Identity", "GET", null, null, OmbiAuth.Bearer(token)) as JSONObject
            val session = identity.toUserSession(
                jellyfinUserId = jellyfinUserId,
                fallbackUserName = username.trim(),
                expiresAt = tokenJson.optNullableString("expiration") ?: tokenJson.optNullableString("expires"),
                lastLoginAt = System.currentTimeMillis(),
            )
            store.saveSession(session, token)
            upsertMapping(
                OmbiUserMapping(
                    jellyfinUserId = jellyfinUserId,
                    jellyfinUserName = jellyfinUserName,
                    ombiUserName = session.ombiUserName,
                    state = OmbiLinkedAccountState.Linked,
                    note = "Linked from Vantafyn",
                ),
            )
            updateAccessRequest(jellyfinUserId, OmbiAccessRequestStatus.Linked)
            session
        }

    suspend fun validateUserSession(jellyfinUserId: String): IntegrationResult<OmbiUserSession> =
        runOmbiCatching(requireApiKey = false) {
            val token = store.readSessionToken(jellyfinUserId) ?: throw OmbiHttpException(401, "No saved Ombi session.")
            val identity = request("/api/v1/Identity", "GET", null, null, OmbiAuth.Bearer(token)) as JSONObject
            val previous = store.readSession(jellyfinUserId)
            val session = identity.toUserSession(
                jellyfinUserId = jellyfinUserId,
                fallbackUserName = previous?.ombiUserName.orEmpty(),
                expiresAt = previous?.expiresAt,
                lastLoginAt = previous?.lastLoginAt ?: System.currentTimeMillis(),
            ).copy(lastValidatedAt = System.currentTimeMillis())
            store.saveSession(session, token)
            mappingFor(jellyfinUserId)?.let {
                upsertMapping(it.copy(ombiUserName = session.ombiUserName, state = OmbiLinkedAccountState.Linked))
            }
            session
        }.also { result ->
            if (result is IntegrationResult.Failure && result.reason == IntegrationFailureReason.Unauthorized) {
                mappingFor(jellyfinUserId)?.let {
                    upsertMapping(it.copy(state = OmbiLinkedAccountState.TokenExpired))
                }
            }
        }

    suspend fun findOmbiUserMatch(
        jellyfinUserName: String?,
        jellyfinEmail: String? = null,
    ): IntegrationResult<OmbiUserMatch> =
        runOmbiCatching {
            val name = jellyfinUserName?.trim().orEmpty()
            if (name.isBlank() && jellyfinEmail.isNullOrBlank()) {
                return@runOmbiCatching OmbiUserMatch(OmbiUserMatchState.NoMatchFound)
            }
            val users = listOmbiUsers()
            val email = jellyfinEmail?.trim()?.lowercase()
            val exactName = users.firstOrNull { it.userName == name }
            val exactEmail = email?.let { target -> users.firstOrNull { it.email?.lowercase() == target } }
            val caseName = users.firstOrNull { it.userName.equals(name, ignoreCase = true) }
            val normalizedName = name.normalizedUserMatchKey()
            val displayFallback = users.firstOrNull {
                normalizedName.isNotBlank() && it.displayName?.normalizedUserMatchKey() == normalizedName
            }
            when {
                exactName != null -> OmbiUserMatch(OmbiUserMatchState.MatchFound, exactName, "Exact username match")
                exactEmail != null -> OmbiUserMatch(OmbiUserMatchState.MatchFound, exactEmail, "Exact email match")
                caseName != null -> OmbiUserMatch(OmbiUserMatchState.MatchFound, caseName, "Username match")
                displayFallback != null -> OmbiUserMatch(OmbiUserMatchState.MatchFound, displayFallback, "Possible display-name match")
                else -> OmbiUserMatch(OmbiUserMatchState.NoMatchFound)
            }
        }.let { result ->
            when (result) {
                is IntegrationResult.Success -> result
                is IntegrationResult.Failure -> IntegrationResult.Success(OmbiUserMatch(OmbiUserMatchState.UnknownUnavailable))
            }
        }

    override suspend fun testConnection(): IntegrationResult<IntegrationConnectionState.Connected> =
        when (val result = testConnectionReport()) {
            is IntegrationResult.Success -> IntegrationResult.Success(IntegrationConnectionState.Connected(result.value.displayName))
            is IntegrationResult.Failure -> result
        }

    suspend fun testConnectionReport(): IntegrationResult<OmbiConnectionReport> =
        runOmbiCatching {
            val version = runCatching { getJsonArrayOrObject("/api/v1/Status") }
                .getOrNull()
                ?.let { (it as? JSONObject)?.optNullableString("version") ?: (it as? JSONObject)?.optNullableString("ombiVersion") }
            val movieListing = runCatching { getJsonArrayOrObject("/api/v1/Request/movie") }.isSuccess
            val tvListing = runCatching { getJsonArrayOrObject("/api/v1/Request/tv") }.isSuccess
            val movieSearch = runCatching { getJsonArray("/api/v1/Search/movie/test") }.isSuccess
            val tvSearch = runCatching { getJsonArray("/api/v1/Search/tv/test") }.isSuccess
            val report = OmbiConnectionReport(
                version = version,
                capabilities = OmbiCapabilities(
                    movieSearch = movieSearch,
                    tvSearch = tvSearch,
                    movieRequest = movieSearch,
                    tvRequest = tvSearch,
                    userRequestListing = movieListing || tvListing,
                    adminModeration = false,
                ),
            )
            store.saveConnectionReport(report)
            report
        }.also { result ->
            if (result is IntegrationResult.Failure) store.saveFailure(result.message)
        }

    override suspend fun searchMovie(query: String): IntegrationResult<List<MediaRequestSearchResult>> =
        runOmbiCatching {
            getJsonArray("/api/v1/Search/movie/${query.urlPath()}").mapJsonObjects { it.toSearchResult(MediaRequestType.Movie) }
        }

    suspend fun searchDiscovery(query: String, jellyfinUserId: String?): IntegrationResult<List<RequestMediaSummary>> =
        runOmbiCatching(requireApiKey = jellyfinUserId == null || store.readSessionToken(jellyfinUserId).isNullOrBlank()) {
            val auth = userOrApiKeyAuth(jellyfinUserId)
            val movies = runCatching {
                getJsonArray("/api/v1/Search/movie/${query.urlPath()}", auth).mapJsonObjects { it.toRequestMediaSummary(MediaRequestType.Movie) }
            }.getOrDefault(emptyList())
            val series = runCatching {
                getJsonArray("/api/v1/Search/tv/${query.urlPath()}", auth).mapJsonObjects { it.toRequestMediaSummary(MediaRequestType.Tv) }
            }.getOrDefault(emptyList())
            movies + series
        }

    suspend fun searchMovieForUser(jellyfinUserId: String, query: String): IntegrationResult<List<MediaRequestSearchResult>> =
        runOmbiCatching(requireApiKey = false) {
            getJsonArray("/api/v1/Search/movie/${query.urlPath()}", OmbiAuth.User(jellyfinUserId)).mapJsonObjects { it.toSearchResult(MediaRequestType.Movie) }
        }

    override suspend fun searchTv(query: String): IntegrationResult<List<MediaRequestSearchResult>> =
        runOmbiCatching {
            getJsonArray("/api/v1/Search/tv/${query.urlPath()}").mapJsonObjects { it.toSearchResult(MediaRequestType.Tv) }
        }

    suspend fun searchTvForUser(jellyfinUserId: String, query: String): IntegrationResult<List<MediaRequestSearchResult>> =
        runOmbiCatching(requireApiKey = false) {
            getJsonArray("/api/v1/Search/tv/${query.urlPath()}", OmbiAuth.User(jellyfinUserId)).mapJsonObjects { it.toSearchResult(MediaRequestType.Tv) }
        }

    override suspend fun requestMovie(providerId: String, requestedBy: String?): IntegrationResult<Unit> =
        runOmbiCatching {
            val body = JSONObject().put("theMovieDbId", providerId.toIntOrNull() ?: providerId)
            validateRequestEngineResult(postJson("/api/v1/Request/movie", body, requestedBy))
        }

    suspend fun requestMovieForUser(jellyfinUserId: String, providerId: String): IntegrationResult<Unit> =
        runOmbiCatching(requireApiKey = false) {
            val body = JSONObject().put("theMovieDbId", providerId.toIntOrNull() ?: providerId)
            validateRequestEngineResult(request("/api/v1/Request/movie", "POST", body, null, OmbiAuth.User(jellyfinUserId)))
        }

    override suspend fun requestTv(providerId: String, requestedBy: String?): IntegrationResult<Unit> =
        requestTv(providerId, requestedBy, OmbiTvRequestSelection.AllSeasons)

    suspend fun requestTv(providerId: String, requestedBy: String?, selection: OmbiTvRequestSelection): IntegrationResult<Unit> =
        requestTvByIds(movieDbId = providerId, tvDbId = providerId, requestedBy = requestedBy, selection = selection)

    suspend fun requestTvByIds(
        movieDbId: String?,
        tvDbId: String?,
        requestedBy: String?,
        selection: OmbiTvRequestSelection,
    ): IntegrationResult<Unit> =
        runOmbiCatching {
            val tmdbId = movieDbId?.takeIf { it.isNotBlank() }
            val tvdbId = tvDbId?.takeIf { it.isNotBlank() }
            require(!tmdbId.isNullOrBlank() || !tvdbId.isNullOrBlank()) { "Ombi did not return a usable TV identifier." }
            val v2Body = JSONObject()
                .put("theMovieDbId", tmdbId?.toIntOrNull() ?: tmdbId)
                .put("requestAll", selection == OmbiTvRequestSelection.AllSeasons)
                .put("latestSeason", selection == OmbiTvRequestSelection.LatestSeason)
                .put("firstSeason", selection == OmbiTvRequestSelection.FirstSeason)
            runCatching {
                if (tmdbId.isNullOrBlank()) throw IllegalArgumentException("Ombi did not return a TMDb id for the v2 TV request endpoint.")
                request("/api/v2/Requests/tv", "POST", v2Body, requestedBy, OmbiAuth.ApiKey)
            }.getOrElse {
                if (tvdbId.isNullOrBlank()) throw it
                val fallbackBody = JSONObject().put("tvDbId", tvdbId?.toIntOrNull() ?: tvdbId)
                request("/api/v1/Request/tv", "POST", fallbackBody, requestedBy, OmbiAuth.ApiKey)
            }.let(::validateRequestEngineResult)
        }

    suspend fun requestTvForUser(jellyfinUserId: String, providerId: String): IntegrationResult<Unit> =
        requestTvForUser(jellyfinUserId, providerId, OmbiTvRequestSelection.AllSeasons)

    suspend fun requestTvForUser(jellyfinUserId: String, providerId: String, selection: OmbiTvRequestSelection): IntegrationResult<Unit> =
        requestTvForUserByIds(jellyfinUserId = jellyfinUserId, movieDbId = providerId, tvDbId = providerId, selection = selection)

    suspend fun requestTvForUserByIds(
        jellyfinUserId: String,
        movieDbId: String?,
        tvDbId: String?,
        selection: OmbiTvRequestSelection,
    ): IntegrationResult<Unit> =
        runOmbiCatching(requireApiKey = false) {
            val tmdbId = movieDbId?.takeIf { it.isNotBlank() }
            val tvdbId = tvDbId?.takeIf { it.isNotBlank() }
            require(!tmdbId.isNullOrBlank() || !tvdbId.isNullOrBlank()) { "Ombi did not return a usable TV identifier." }
            val v2Body = JSONObject()
                .put("theMovieDbId", tmdbId?.toIntOrNull() ?: tmdbId)
                .put("requestAll", selection == OmbiTvRequestSelection.AllSeasons)
                .put("latestSeason", selection == OmbiTvRequestSelection.LatestSeason)
                .put("firstSeason", selection == OmbiTvRequestSelection.FirstSeason)
            runCatching {
                if (tmdbId.isNullOrBlank()) throw IllegalArgumentException("Ombi did not return a TMDb id for the v2 TV request endpoint.")
                request("/api/v2/Requests/tv", "POST", v2Body, null, OmbiAuth.User(jellyfinUserId))
            }.getOrElse {
                if (tvdbId.isNullOrBlank()) throw it
                val fallbackBody = JSONObject().put("tvDbId", tvdbId?.toIntOrNull() ?: tvdbId)
                request("/api/v1/Request/tv", "POST", fallbackBody, null, OmbiAuth.User(jellyfinUserId))
            }.let(::validateRequestEngineResult)
        }

    override suspend fun getUserRequests(userName: String?): IntegrationResult<List<MediaRequestItem>> =
        runOmbiCatching {
            (getRequests("/api/v1/Request/movie", MediaRequestType.Movie) + getRequests("/api/v1/Request/tv", MediaRequestType.Tv))
                .filter { userName.isNullOrBlank() || it.requestedBy.isNullOrBlank() || it.requestedBy.equals(userName, ignoreCase = true) }
        }

    suspend fun getLinkedUserRequests(jellyfinUserId: String): IntegrationResult<List<MediaRequestItem>> =
        runOmbiCatching(requireApiKey = false) {
            val session = store.readSession(jellyfinUserId)
            val byUserId = session?.ombiUserId?.takeIf { it.isNotBlank() }?.let { ombiUserId ->
                runCatching {
                    getRequests("/api/v2/Requests/movie/50/0/requestedDate/desc?requestedBy=${ombiUserId.urlQuery()}", MediaRequestType.Movie, OmbiAuth.User(jellyfinUserId)) +
                        getRequests("/api/v2/Requests/tv/50/0/requestedDate/desc?requestedBy=${ombiUserId.urlQuery()}", MediaRequestType.Tv, OmbiAuth.User(jellyfinUserId))
                }.getOrNull()
            }
            byUserId ?: (
                getRequests("/api/v1/Request/movie", MediaRequestType.Movie, OmbiAuth.User(jellyfinUserId)) +
                    getRequests("/api/v1/Request/tv", MediaRequestType.Tv, OmbiAuth.User(jellyfinUserId))
                )
        }

    suspend fun getMyRequestSummaries(jellyfinUserId: String?, userName: String?, isAdmin: Boolean): IntegrationResult<List<RequestMediaSummary>> =
        runOmbiCatching(requireApiKey = jellyfinUserId == null || store.readSessionToken(jellyfinUserId).isNullOrBlank()) {
            val auth = userOrApiKeyAuth(jellyfinUserId)
            val session = jellyfinUserId?.let(store::readSession)
            val byUserId = session?.ombiUserId?.takeIf { it.isNotBlank() }?.let { ombiUserId ->
                runCatching {
                    getRequestSummaries("/api/v2/Requests/movie/50/0/requestedDate/desc?requestedBy=${ombiUserId.urlQuery()}", RequestMediaType.Movie, auth) +
                        getRequestSummaries("/api/v2/Requests/tv/50/0/requestedDate/desc?requestedBy=${ombiUserId.urlQuery()}", RequestMediaType.Series, auth)
                }.getOrNull()
            }
            byUserId ?: (
                getRequestSummaries("/api/v1/Request/movie", RequestMediaType.Movie, auth) +
                    getRequestSummaries("/api/v1/Request/tv", RequestMediaType.Series, auth)
                ).filter { isAdmin || userName.isNullOrBlank() || it.requestedBy.isNullOrBlank() || it.requestedBy.equals(userName, ignoreCase = true) }
        }

    suspend fun getDiscoveryRails(jellyfinUserId: String?, userName: String?, isAdmin: Boolean): IntegrationResult<List<OmbiDiscoverRail>> =
        runOmbiCatching(requireApiKey = jellyfinUserId == null || store.readSessionToken(jellyfinUserId).isNullOrBlank()) {
            val auth = userOrApiKeyAuth(jellyfinUserId)
            buildList {
                addDiscoveryRail(OmbiDiscoverRailKind.PopularMovies, "Popular movies") {
                    discoverSummaries("/api/v2/Search/movie/popular/0/20", RequestMediaType.Movie, auth)
                }
                addDiscoveryRail(OmbiDiscoverRailKind.NowPlayingMovies, "Now playing") {
                    discoverSummaries("/api/v2/Search/movie/nowplaying/0/20", RequestMediaType.Movie, auth)
                }
                addDiscoveryRail(OmbiDiscoverRailKind.UpcomingMovies, "Upcoming movies") {
                    discoverSummaries("/api/v2/Search/movie/upcoming/0/20", RequestMediaType.Movie, auth)
                }
                addDiscoveryRail(OmbiDiscoverRailKind.TopRatedMovies, "Top-rated movies") {
                    discoverSummaries("/api/v2/Search/movie/toprated/0/20", RequestMediaType.Movie, auth)
                }
                addDiscoveryRail(OmbiDiscoverRailKind.TrendingSeries, "Trending series") {
                    discoverSummaries("/api/v2/Search/tv/trending/0/20", RequestMediaType.Series, auth)
                }
                addDiscoveryRail(OmbiDiscoverRailKind.PopularSeries, "Popular series") {
                    discoverSummaries("/api/v2/Search/tv/popular/0/20", RequestMediaType.Series, auth)
                }
                addDiscoveryRail(OmbiDiscoverRailKind.AnticipatedSeries, "Anticipated series") {
                    discoverSummaries("/api/v2/Search/tv/anticipated/0/20", RequestMediaType.Series, auth)
                }
                addDiscoveryRail(OmbiDiscoverRailKind.MostWatchedSeries, "Most-watched series") {
                    discoverSummaries("/api/v2/Search/tv/mostwatched/0/20", RequestMediaType.Series, auth)
                }
                runCatching {
                    getRequestSummaries("/api/v2/Requests/recentlyRequested", RequestMediaType.Movie, auth)
                        .take(20)
                }.getOrNull()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { add(OmbiDiscoverRail(OmbiDiscoverRailKind.RecentlyRequested, "Recently requested", it)) }

                val visible = runCatching {
                    getRequestSummaries("/api/v1/Request/movie", RequestMediaType.Movie, auth) +
                        getRequestSummaries("/api/v1/Request/tv", RequestMediaType.Series, auth)
                }.getOrDefault(emptyList())

                val familyQueue = visible
                    .filter { it.state in setOf(RequestState.PendingApproval, RequestState.Approved, RequestState.Processing) }
                    .take(20)
                if ((isAdmin || cachedUserCapabilities(jellyfinUserId).canViewOtherRequests) && familyQueue.isNotEmpty()) {
                    add(OmbiDiscoverRail(OmbiDiscoverRailKind.FamilyQueue, "Family queue", familyQueue))
                }

                val recentlyAvailable = runCatching {
                    getRequestSummaries("/api/v2/Requests/movie/available/20/0/requestedDate/desc", RequestMediaType.Movie, auth) +
                        getRequestSummaries("/api/v2/Requests/tv/available/20/0/requestedDate/desc", RequestMediaType.Series, auth)
                }.getOrDefault(visible.filter { it.state == RequestState.Available }.take(20))
                if (recentlyAvailable.isNotEmpty()) {
                    add(OmbiDiscoverRail(OmbiDiscoverRailKind.RecentlyAvailable, "Recently available", recentlyAvailable.take(20)))
                }

                val mine = runCatching {
                    val session = jellyfinUserId?.let(store::readSession)
                    val byUserId = session?.ombiUserId?.takeIf { it.isNotBlank() }?.let { ombiUserId ->
                        getRequestSummaries("/api/v2/Requests/movie/20/0/requestedDate/desc?requestedBy=${ombiUserId.urlQuery()}", RequestMediaType.Movie, auth) +
                            getRequestSummaries("/api/v2/Requests/tv/20/0/requestedDate/desc?requestedBy=${ombiUserId.urlQuery()}", RequestMediaType.Series, auth)
                    }
                    byUserId ?: visible.filter { userName.isNullOrBlank() || it.requestedBy.isNullOrBlank() || it.requestedBy.equals(userName, ignoreCase = true) }
                }.getOrDefault(emptyList()).take(20)
                if (mine.isNotEmpty()) {
                    add(OmbiDiscoverRail(OmbiDiscoverRailKind.MyRequests, "My requests", mine))
                }
            }
        }

    suspend fun getRequestMediaDetail(item: RequestMediaSummary, jellyfinUserId: String?): IntegrationResult<RequestMediaDetail> =
        runOmbiCatching(requireApiKey = jellyfinUserId == null || store.readSessionToken(jellyfinUserId).isNullOrBlank()) {
            val auth = userOrApiKeyAuth(jellyfinUserId)
            val json = when (item.mediaType) {
                RequestMediaType.Movie -> {
                    val id = item.movieDbId?.takeIf { it.isNotBlank() }
                        ?: throw IllegalArgumentException("Ombi did not return a usable TMDb movie identifier.")
                    request("/api/v2/Search/movie/${id.urlPath()}", "GET", null, null, auth) as JSONObject
                }
                RequestMediaType.Series -> {
                    val tvDbId = item.tvDbId?.takeIf { it.isNotBlank() }
                    val movieDbId = item.movieDbId?.takeIf { it.isNotBlank() }
                    if (!tvDbId.isNullOrBlank()) {
                        runCatching {
                            request("/api/v2/Search/tv/${tvDbId.urlPath()}", "GET", null, null, auth) as JSONObject
                        }.getOrElse {
                            val id = movieDbId ?: throw it
                            request("/api/v2/Search/tv/moviedb/${id.urlPath()}", "GET", null, null, auth) as JSONObject
                        }
                    } else {
                        val id = movieDbId ?: throw IllegalArgumentException("Ombi did not return a usable TV identifier.")
                        request("/api/v2/Search/tv/moviedb/${id.urlPath()}", "GET", null, null, auth) as JSONObject
                    }
                }
            }
            json.toRequestMediaDetail(item)
        }

    override suspend fun getAllRequests(): IntegrationResult<List<MediaRequestItem>> =
        runOmbiCatching {
            getRequests("/api/v1/Request/movie", MediaRequestType.Movie) + getRequests("/api/v1/Request/tv", MediaRequestType.Tv)
        }

    override suspend fun approveRequest(requestId: String): IntegrationResult<Unit> =
        IntegrationResult.Failure(IntegrationFailureReason.Unsupported, "Approval endpoint varies by Ombi version and is not wired yet.")

    override suspend fun denyRequest(requestId: String): IntegrationResult<Unit> =
        IntegrationResult.Failure(IntegrationFailureReason.Unsupported, "Deny endpoint varies by Ombi version and is not wired yet.")

    private suspend fun <T> runOmbiCatching(requireApiKey: Boolean = true, block: suspend () -> T): IntegrationResult<T> =
        withContext(ioDispatcher) {
            try {
                val config = store.read()
                val apiKey = store.readApiKey()
                if (!config.isConfigured || (requireApiKey && apiKey.isNullOrBlank())) {
                    return@withContext IntegrationResult.Failure(IntegrationFailureReason.NotConfigured, "Ombi is not configured.")
                }
                IntegrationResult.Success(withTimeout(12_000L) { block() })
            } catch (throwable: Throwable) {
                throwable.toIntegrationFailure()
            }
        }

    private fun getRequests(path: String, type: MediaRequestType, auth: OmbiAuth = OmbiAuth.ApiKey): List<MediaRequestItem> =
        getJsonArray(path, auth).mapJsonObjects { it.toRequestItem(type) }

    private fun getRequestSummaries(path: String, type: RequestMediaType, auth: OmbiAuth): List<RequestMediaSummary> =
        getJsonArray(path, auth).mapJsonObjects { it.toRequestMediaSummary(type.toLegacyType(), path) }

    private fun discoverSummaries(path: String, type: RequestMediaType, auth: OmbiAuth): List<RequestMediaSummary> =
        getJsonArray(path, auth).mapJsonObjects { it.toRequestMediaSummary(type.toLegacyType(), path) }

    private fun listOmbiUsers(): List<OmbiUserProfile> {
        val endpoints = listOf(
            "/api/v1/Identity/Users",
            "/api/v1/Identity",
            "/api/v1/User",
            "/api/v1/Users",
        )
        endpoints.forEach { endpoint ->
            val users = runCatching {
                when (val value = request(endpoint, "GET", null, null, OmbiAuth.ApiKey)) {
                    is JSONArray -> value.toOmbiUsers()
                    is JSONObject -> {
                        value.optJSONArray("users")?.toOmbiUsers()
                            ?: value.optJSONArray("items")?.toOmbiUsers()
                            ?: value.optJSONArray("results")?.toOmbiUsers()
                            ?: emptyList()
                    }
                    else -> emptyList()
                }
            }.getOrDefault(emptyList())
            if (users.isNotEmpty()) return users
        }
        throw OmbiHttpException(404, "Ombi user lookup is unavailable.")
    }

    private fun getJsonArray(path: String, auth: OmbiAuth = OmbiAuth.ApiKey): JSONArray {
        val value = request(path, "GET", null, null, auth)
        return when (value) {
            is JSONArray -> value
            is JSONObject -> value.optJSONArray("results") ?: value.optJSONArray("items") ?: JSONArray().put(value)
            else -> JSONArray()
        }
    }

    private fun getJsonArrayOrObject(path: String): Any = request(path, "GET", null, null, OmbiAuth.ApiKey)

    private fun postJson(path: String, body: JSONObject, requestedBy: String?): Any =
        request(path, "POST", body, requestedBy, OmbiAuth.ApiKey)

    private fun request(path: String, method: String, body: JSONObject?, requestedBy: String?, auth: OmbiAuth): Any {
        val config = store.read()
        val connection = URL("${config.baseUrl.trimEnd('/')}$path").openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 12_000
        connection.readTimeout = 12_000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "application/json")
        when (auth) {
            OmbiAuth.ApiKey -> {
                connection.setRequestProperty("ApiKey", store.readApiKey().orEmpty())
                val alias = requestedBy?.takeIf { it.isNotBlank() } ?: config.apiAlias
                alias?.let { connection.setRequestProperty("ApiAlias", it) }
            }
            is OmbiAuth.Bearer -> connection.setRequestProperty("Authorization", "Bearer ${auth.token}")
            OmbiAuth.None -> Unit
            is OmbiAuth.User -> {
                val token = store.readSessionToken(auth.jellyfinUserId) ?: throw OmbiHttpException(401, "No saved Ombi session.")
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
        }
        if (body != null) {
            connection.doOutput = true
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
        }
        val code = connection.responseCode
        val response = runCatching {
            BufferedReader((if (code in 200..299) connection.inputStream else connection.errorStream).reader()).use { it.readText() }
        }.getOrDefault("")
        if (code !in 200..299) throw OmbiHttpException(code, response.take(180))
        return response.parseJsonBody()
    }

    private fun userOrApiKeyAuth(jellyfinUserId: String?): OmbiAuth =
        if (jellyfinUserId != null && !store.readSessionToken(jellyfinUserId).isNullOrBlank()) OmbiAuth.User(jellyfinUserId) else OmbiAuth.ApiKey
}

private class OmbiHttpException(val code: Int, message: String) : RuntimeException(message)

private class OmbiRequestEngineException(
    val errorCode: String?,
    message: String,
) : RuntimeException(message)

private class OmbiUnexpectedContentException(message: String) : RuntimeException(message)

private sealed interface OmbiAuth {
    data object ApiKey : OmbiAuth
    data object None : OmbiAuth
    data class Bearer(val token: String) : OmbiAuth
    data class User(val jellyfinUserId: String) : OmbiAuth
}

private inline fun MutableList<OmbiDiscoverRail>.addDiscoveryRail(
    kind: OmbiDiscoverRailKind,
    title: String,
    block: () -> List<RequestMediaSummary>,
) {
    runCatching { block() }
        .getOrDefault(emptyList())
        .takeIf { it.isNotEmpty() }
        ?.let { add(OmbiDiscoverRail(kind, title, it)) }
}

private fun Throwable.toIntegrationFailure(): IntegrationResult.Failure {
    val className = javaClass.name
    val message = message.orEmpty()
    val reason = when {
        this is kotlinx.coroutines.TimeoutCancellationException -> IntegrationFailureReason.NetworkError
        this is OmbiHttpException && code == 401 -> IntegrationFailureReason.Unauthorized
        this is OmbiHttpException && code == 403 -> IntegrationFailureReason.Forbidden
        this is OmbiHttpException && code == 409 -> IntegrationFailureReason.AlreadyExists
        this is OmbiRequestEngineException && errorCode?.contains("AlreadyRequested", ignoreCase = true) == true -> IntegrationFailureReason.AlreadyExists
        this is OmbiUnexpectedContentException -> IntegrationFailureReason.InvalidConfiguration
        this is OmbiRequestEngineException && errorCode?.contains("Quota", ignoreCase = true) == true -> IntegrationFailureReason.Forbidden
        this is OmbiRequestEngineException && errorCode?.contains("Permission", ignoreCase = true) == true -> IntegrationFailureReason.Forbidden
        this is OmbiHttpException && code in 500..599 -> IntegrationFailureReason.ServerError
        this is OmbiHttpException -> IntegrationFailureReason.ServerError
        this is IllegalArgumentException -> IntegrationFailureReason.InvalidConfiguration
        className.contains("UnknownHost", ignoreCase = true) ||
            className.contains("ConnectException", ignoreCase = true) ||
            className.contains("SocketTimeout", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) -> IntegrationFailureReason.NetworkError
        className.contains("SSL", ignoreCase = true) ||
            message.contains("certificate", ignoreCase = true) -> IntegrationFailureReason.NetworkError
        else -> IntegrationFailureReason.Unknown
    }
    val userMessage = when {
        this is OmbiRequestEngineException && errorCode?.contains("Quota", ignoreCase = true) == true -> "This Ombi account has reached its request limit."
        this is OmbiRequestEngineException && errorCode?.contains("NoPermissions", ignoreCase = true) == true -> "This Ombi account does not have permission to request that."
        this is OmbiRequestEngineException && errorCode?.contains("AlreadyRequested", ignoreCase = true) == true -> "This title has already been requested."
        this is OmbiRequestEngineException && message.isNotBlank() -> message.take(180)
        this is OmbiUnexpectedContentException -> message
        else -> when (reason) {
        IntegrationFailureReason.Unauthorized -> "Ombi rejected this API key."
        IntegrationFailureReason.Forbidden -> "This Ombi account does not have permission to do that."
        IntegrationFailureReason.NotConfigured -> "Ombi is not configured."
        IntegrationFailureReason.NetworkError -> "Could not reach Ombi."
        IntegrationFailureReason.ServerError -> "Ombi responded with an error."
        IntegrationFailureReason.Unsupported -> "This Ombi action is not supported yet."
        IntegrationFailureReason.InvalidConfiguration -> "Check the Ombi server address."
        IntegrationFailureReason.AlreadyExists -> "This request already exists."
        IntegrationFailureReason.Unknown -> "The Ombi request failed."
        }
    }
    return IntegrationResult.Failure(reason, userMessage, this)
}

private fun normalizeBaseUrl(value: String): String {
    val trimmed = value.trim().trimEnd('/')
    val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
    val uri = URI(withScheme)
    require(uri.host?.isNotBlank() == true) { "Enter a valid Ombi server address." }
    return withScheme
}

private fun String.urlPath(): String =
    java.net.URLEncoder.encode(trim(), "UTF-8").replace("+", "%20")

private fun String.urlQuery(): String =
    java.net.URLEncoder.encode(trim(), "UTF-8")

private fun String.parseJsonBody(): Any {
    val trimmed = trim()
    if (trimmed.isBlank()) return JSONObject()
    if (trimmed.startsWith("<")) {
        throw OmbiUnexpectedContentException("This address returned a web page instead of Ombi API data. Check reverse proxy/API access.")
    }
    return if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONObject(trimmed)
}

private fun validateRequestEngineResult(value: Any) {
    val json = value as? JSONObject ?: return
    val result = json.opt("result")
    val isError = json.optBoolean("isError", false)
    val errorCode = json.optNullableString("errorCode")
    val message = json.optNullableString("errorMessage") ?: json.optNullableString("message")
    if (isError || result == false) {
        throw OmbiRequestEngineException(errorCode, message ?: "Ombi rejected this request.")
    }
}

private fun <T> JSONArray.mapJsonObjects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).mapNotNull { index -> optJSONObject(index)?.let(transform) }

private fun JSONObject.toSearchResult(type: MediaRequestType): MediaRequestSearchResult {
    val providerId = when (type) {
        MediaRequestType.Movie -> optAnyString("theMovieDbId") ?: optAnyString("tmdbId") ?: optAnyString("id")
        MediaRequestType.Tv -> optAnyString("theTvDbId") ?: optAnyString("tvDbId") ?: optAnyString("theMovieDbId") ?: optAnyString("tmdbId") ?: optAnyString("id")
    }.orEmpty()
    return MediaRequestSearchResult(
        providerId = providerId,
        title = optString("title", optString("name", "Untitled")),
        year = optIntOrNull("releaseDate") ?: optIntOrNull("firstAired") ?: optIntOrNull("year"),
        type = type,
        overview = optNullableString("overview"),
        posterUrl = optNullableString("posterPath") ?: optNullableString("poster"),
        backdropUrl = optNullableString("backdropPath"),
        status = requestStatus(),
    )
}

private fun JSONObject.toRequestMediaSummary(type: MediaRequestType, sourceEndpoint: String? = null): RequestMediaSummary {
    val requestType = if (type == MediaRequestType.Movie) RequestMediaType.Movie else RequestMediaType.Series
    val movieJson = optJSONObject("movie")
    val tvJson = optJSONObject("tv")
    val movieDbId = optAnyString("theMovieDbId")
        ?: optAnyString("tmdbId")
        ?: optAnyString("movieDbId")
        ?: movieJson?.optAnyString("theMovieDbId")
        ?: movieJson?.optAnyString("tmdbId")
        ?: tvJson?.optAnyString("theMovieDbId")
        ?: tvJson?.optAnyString("tmdbId")
    val tvDbId = optAnyString("theTvDbId")
        ?: optAnyString("tvDbId")
        ?: optAnyString("seriesId")
        ?: tvJson?.optAnyString("theTvDbId")
        ?: tvJson?.optAnyString("tvDbId")
    val imdbId = optAnyString("imdbId")
        ?: optAnyString("imdbID")
        ?: movieJson?.optAnyString("imdbId")
        ?: movieJson?.optAnyString("imdbID")
        ?: tvJson?.optAnyString("imdbId")
        ?: tvJson?.optAnyString("imdbID")
    val externalId = when (type) {
        MediaRequestType.Movie -> movieDbId ?: optAnyString("id")
        MediaRequestType.Tv -> movieDbId ?: tvDbId ?: optAnyString("id")
    }.orEmpty()
    val title = optNullableString("title")
        ?: optNullableString("name")
        ?: optJSONObject("movie")?.optNullableString("title")
        ?: optJSONObject("tv")?.optNullableString("title")
        ?: optJSONObject("childRequests")?.optNullableString("title")
        ?: "Untitled"
    val state = requestState()
    debugStatusResolution(sourceEndpoint, this, requestType, title, state)
    return RequestMediaSummary(
        externalId = externalId,
        movieDbId = movieDbId,
        tvDbId = tvDbId,
        imdbId = imdbId,
        mediaType = requestType,
        title = title,
        originalTitle = optNullableString("originalTitle"),
        year = optIntOrNull("releaseDate")
            ?: optIntOrNull("firstAired")
            ?: optIntOrNull("year")
            ?: optJSONObject("movie")?.optIntOrNull("releaseDate"),
        posterUrl = normalizeOmbiArtworkUrl(optNullableString("posterPath") ?: optNullableString("poster")),
        backdropUrl = normalizeOmbiArtworkUrl(optNullableString("backdropPath") ?: optNullableString("background")),
        overview = optNullableString("overview"),
        rating = optDoubleOrNull("voteAverage") ?: optDoubleOrNull("rating"),
        state = state,
        requestId = requestRecordId(),
        isAvailableInJellyfin = optBoolean("available", false) || optBoolean("fullyAvailable", false) || optBoolean("markedAsAvailable", false),
        availableSeasonCount = optIntOrNull("availableSeasonCount"),
        totalSeasonCount = optIntOrNull("totalSeasons") ?: optIntOrNull("seasonCount"),
        requestedBy = optJSONObject("requestedUser")?.optNullableString("userName")
            ?: optNullableString("requestedByAlias")
            ?: optNullableString("requestedUserName")
            ?: optNullableString("requestedBy"),
        requestedDate = optNullableString("requestedDate") ?: optNullableString("createdDate"),
    )
}

private fun JSONObject.toRequestMediaDetail(fallback: RequestMediaSummary): RequestMediaDetail {
    val type = fallback.mediaType
    val summary = toRequestMediaSummary(type.toLegacyType()).let {
        it.copy(
            externalId = it.externalId.ifBlank { fallback.externalId },
            movieDbId = it.movieDbId ?: fallback.movieDbId,
            tvDbId = it.tvDbId ?: fallback.tvDbId,
            imdbId = it.imdbId ?: fallback.imdbId,
            posterUrl = it.posterUrl ?: fallback.posterUrl,
            backdropUrl = it.backdropUrl ?: fallback.backdropUrl,
            overview = it.overview ?: fallback.overview,
            state = if (it.state == RequestState.NotRequested && fallback.state != RequestState.NotRequested) fallback.state else it.state,
        )
    }
    return RequestMediaDetail(
        summary = summary,
        tagline = optNullableString("tagline"),
        runtimeMinutes = optIntOrNull("runtime") ?: optNullableString("runtime")?.filter(Char::isDigit)?.toIntOrNull(),
        genres = (optJSONArray("genres")?.toGenreNames().orEmpty())
            .ifEmpty { optJSONArray("genre")?.toStringList().orEmpty() },
        certification = optNullableString("certification")
            ?: optJSONObject("releaseDates")?.optNullableString("certification"),
        network = optJSONObject("network")?.optNullableString("name") ?: optNullableString("network"),
        seasons = optJSONArray("seasonRequests")?.toSeasonSummaries().orEmpty(),
        similar = optJSONObject("similar")
            ?.optJSONArray("results")
            ?.mapJsonObjects { it.toRequestMediaSummary(type.toLegacyType()) }
            .orEmpty()
            .take(12),
    )
}

private fun JSONArray.toSeasonSummaries(): List<RequestSeasonSummary> =
    mapJsonObjects { season ->
        val episodes = season.optJSONArray("episodes")?.toEpisodeSummaries().orEmpty()
        val available = season.optBoolean("seasonAvailable", false) || episodes.isNotEmpty() && episodes.all { it.available }
        RequestSeasonSummary(
            seasonNumber = season.optInt("seasonNumber", 0),
            overview = season.optNullableString("overview"),
            state = when {
                available -> RequestState.Available
                episodes.any { it.state == RequestState.Processing } -> RequestState.Processing
                episodes.any { it.state == RequestState.Approved } -> RequestState.Approved
                episodes.any { it.state == RequestState.PendingApproval } -> RequestState.PendingApproval
                episodes.any { it.state == RequestState.Declined } -> RequestState.Declined
                else -> season.requestState()
            },
            available = available,
            episodes = episodes,
        )
    }.filter { it.seasonNumber > 0 }

private fun JSONArray.toEpisodeSummaries(): List<RequestEpisodeSummary> =
    mapJsonObjects { episode ->
        RequestEpisodeSummary(
            episodeNumber = episode.optInt("episodeNumber", 0),
            title = episode.optNullableString("title"),
            airDate = episode.optNullableString("airDate") ?: episode.optNullableString("airDateDisplay"),
            state = episode.requestState(),
            available = episode.optBoolean("available", false),
        )
    }.filter { it.episodeNumber > 0 }

private fun JSONObject.toRequestItem(type: MediaRequestType): MediaRequestItem =
    MediaRequestItem(
        requestId = optAnyString("id") ?: optAnyString("requestId") ?: "",
        providerId = optAnyString(if (type == MediaRequestType.Movie) "theMovieDbId" else "theTvDbId"),
        title = optString("title", optString("name", "Untitled")),
        type = type,
        status = requestStatus(hasGenericIdEvidence = true),
        requestedBy = optJSONObject("requestedUser")?.optNullableString("userName")
            ?: optNullableString("requestedUserName")
            ?: optNullableString("requestedBy"),
        posterUrl = optNullableString("posterPath") ?: optNullableString("poster"),
    )

private fun JSONObject.toUserSession(
    jellyfinUserId: String,
    fallbackUserName: String,
    expiresAt: String?,
    lastLoginAt: Long,
): OmbiUserSession {
    val username = optNullableString("userName")
        ?: optNullableString("username")
        ?: optNullableString("emailAddress")
        ?: fallbackUserName
    return OmbiUserSession(
        jellyfinUserId = jellyfinUserId,
        ombiUserName = username,
        displayName = optNullableString("alias") ?: optNullableString("displayName") ?: username,
        ombiUserId = optAnyString("id"),
        expiresAt = expiresAt,
        roles = optJSONArray("claims")?.toClaimLabels().orEmpty(),
        lastLoginAt = lastLoginAt,
        lastValidatedAt = System.currentTimeMillis(),
    )
}

private fun JSONObject.requestStatus(hasGenericIdEvidence: Boolean = false): MediaRequestStatus {
    val statusText = optNullableString("requestStatus")
    val hasRequest = hasRequestRecordEvidence(hasGenericIdEvidence)
    return when {
        optBoolean("available", false) || optBoolean("isAvailable", false) || optBoolean("fullyAvailable", false) || optBoolean("markedAsAvailable", false) -> MediaRequestStatus.Available
        !hasRequest -> MediaRequestStatus.NotRequested
        optBoolean("denied", false) || hasMeaningfulValue("deniedBy") || statusText.isRequestStatus("den") -> MediaRequestStatus.Denied
        statusText.isRequestStatus("fail") -> MediaRequestStatus.Unknown
        optBoolean("processing", false) || optBoolean("isProcessing", false) || statusText.isRequestStatus("process") -> MediaRequestStatus.Processing
        optBoolean("approved", false) || hasMeaningfulValue("approvedBy") || statusText.isRequestStatus("approv") -> MediaRequestStatus.Approved
        optBoolean("requested", false) || optBoolean("isRequested", false) || statusText.isRequestStatus("pending") -> MediaRequestStatus.Pending
        statusText?.isNotBlank() == true -> MediaRequestStatus.Unknown
        else -> MediaRequestStatus.NotRequested
    }
}

private fun JSONObject.requestState(): RequestState {
    val statusText = optNullableString("requestStatus")
    val hasRequest = hasRequestRecordEvidence()
    return when {
        optBoolean("fullyAvailable", false) || optBoolean("available", false) || optBoolean("markedAsAvailable", false) -> RequestState.Available
        optBoolean("partlyAvailable", false) -> RequestState.PartiallyAvailable
        !hasRequest -> RequestState.NotRequested
        optBoolean("denied", false) || hasMeaningfulValue("deniedBy") || statusText.isRequestStatus("den") -> RequestState.Declined
        statusText.isRequestStatus("fail") -> RequestState.Failed
        optBoolean("processing", false) || optBoolean("isProcessing", false) ||
            statusText.isRequestStatus("process") -> RequestState.Processing
        optBoolean("approved", false) || hasMeaningfulValue("approvedBy") || statusText.isRequestStatus("approv") -> RequestState.Approved
        optBoolean("requested", false) || optBoolean("isRequested", false) ||
            requestRecordId() != null ||
            statusText.isRequestStatus("pending") -> RequestState.PendingApproval
        statusText?.isNotBlank() == true -> RequestState.Unknown
        else -> RequestState.NotRequested
    }
}

private fun JSONObject.hasRequestRecordEvidence(hasGenericIdEvidence: Boolean = false): Boolean =
    requestRecordId() != null ||
        hasGenericIdEvidence && optAnyString("id") != null ||
        optBoolean("requested", false) ||
        optBoolean("isRequested", false) ||
        hasMeaningfulValue("requestedBy") ||
        hasMeaningfulValue("requestedByAlias") ||
        hasMeaningfulValue("requestedUserName") ||
        hasMeaningfulValue("requestedDate") ||
        optJSONObject("requestedUser") != null ||
        optJSONObject("request") != null ||
        optNullableString("requestStatus").isKnownRequestStatus()

private fun JSONObject.requestRecordId(): String? =
    optRequestId("requestId")
        ?: optRequestId("mediaRequestId")
        ?: optRequestId("requestGuid")
        ?: optJSONObject("request")?.optRequestId("id")
        ?: optJSONObject("request")?.optRequestId("requestId")

private fun JSONObject.optRequestId(name: String): String? {
    val value = opt(name) ?: return null
    return when (value) {
        JSONObject.NULL -> null
        is Number -> value.toLong().takeIf { it > 0L }?.toString()
        is String -> value.trim().takeIf { it.isNotBlank() && it != "null" && it != "0" }
        else -> value.toString().takeIf { it.isNotBlank() && it != "null" && it != "0" }
    }
}

private fun JSONObject.hasMeaningfulValue(name: String): Boolean =
    opt(name)?.let { value ->
        when (value) {
            JSONObject.NULL -> false
            is Boolean -> value
            is Number -> value.toLong() != 0L
            is String -> value.isNotBlank() && value != "null"
            else -> true
        }
    } ?: false

private fun String?.isRequestStatus(fragment: String): Boolean =
    !isNullOrBlank() &&
        !contains("not requested", ignoreCase = true) &&
        !contains("notrequested", ignoreCase = true) &&
        contains(fragment, ignoreCase = true)

private fun String?.isKnownRequestStatus(): Boolean =
    listOf("pending", "approv", "den", "process", "request", "fail")
        .any { isRequestStatus(it) }

private fun debugStatusResolution(
    sourceEndpoint: String?,
    json: JSONObject,
    mediaType: RequestMediaType,
    title: String,
    state: RequestState,
) {
    if (!Log.isLoggable(OMBI_STATUS_TAG, Log.DEBUG)) return
    Log.d(
        OMBI_STATUS_TAG,
        "source=${sourceEndpoint ?: "detail"} type=$mediaType title=$title " +
            "ids={tmdb=${json.optAnyString("theMovieDbId") ?: json.optAnyString("tmdbId")},tvdb=${json.optAnyString("theTvDbId") ?: json.optAnyString("tvDbId") ?: json.optAnyString("seriesId")},request=${json.requestRecordId()}} " +
            "flags={requested=${json.opt("requested")},approved=${json.opt("approved")},denied=${json.opt("denied")},processing=${json.opt("processing")},available=${json.opt("available")},fully=${json.opt("fullyAvailable")},partial=${json.opt("partlyAvailable")},status=${json.opt("requestStatus")}} final=$state",
    )
}

private const val OMBI_STATUS_TAG = "VantafynOmbiStatus"

private fun JSONObject.optIntOrNull(name: String): Int? {
    val value = opt(name) ?: return null
    return when (value) {
        is Number -> value.toInt()
        is String -> value.take(4).toIntOrNull()
        else -> null
    }
}

private fun JSONObject.optAnyString(name: String): String? {
    val value = opt(name) ?: return null
    return value.toString().takeIf { it.isNotBlank() && it != "null" }
}

private fun JSONObject.optNullableString(name: String): String? =
    optString(name).takeIf { it.isNotBlank() && it != "null" }

private fun JSONObject.optDoubleOrNull(name: String): Double? {
    val value = opt(name) ?: return null
    return when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }
}

private fun normalizeOmbiArtworkUrl(value: String?): String? {
    val trimmed = value?.trim()?.takeIf { it.isNotBlank() && it != "null" } ?: return null
    return when {
        trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        trimmed.startsWith("//") -> "https:$trimmed"
        trimmed.startsWith("/") -> "https://image.tmdb.org/t/p/w500$trimmed"
        else -> trimmed
    }
}

private fun RequestMediaType.toLegacyType(): MediaRequestType =
    if (this == RequestMediaType.Movie) MediaRequestType.Movie else MediaRequestType.Tv

private fun JSONArray.toClaimLabels(): List<String> =
    mapJsonObjects {
        it.optNullableString("value")
            ?: it.optNullableString("type")
            ?: it.optNullableString("claimValue")
            ?: it.optNullableString("claimType")
            ?: it.toString()
    }.filter { it.isNotBlank() }.distinct()

private fun JSONArray.toGenreNames(): List<String> =
    (0 until length()).mapNotNull { index ->
        val value = opt(index) ?: return@mapNotNull null
        when (value) {
            is JSONObject -> value.optNullableString("name")
            is String -> value.takeIf { it.isNotBlank() && it != "null" }
            else -> null
        }
    }

private fun userTokenSecretKey(jellyfinUserId: String): String =
    "ombi.user.${jellyfinUserId.replace(Regex("[^A-Za-z0-9_.-]"), "_")}.token"

private fun String.toSuggestedOmbiUserName(): String {
    val sanitized = lowercase()
        .replace(Regex("[^a-z0-9._-]+"), ".")
        .trim('.', '_', '-')
    return sanitized.ifBlank { "vantafyn.user" }.take(48)
}

private val accessRequestPriorityComparator: Comparator<OmbiAccessRequest> =
    compareBy<OmbiAccessRequest> { it.status.priority }
        .thenBy { it.requestedAt }

private val OmbiAccessRequestStatus.priority: Int
    get() = when (this) {
        OmbiAccessRequestStatus.Linked -> 5
        OmbiAccessRequestStatus.AccountCreated -> 4
        OmbiAccessRequestStatus.Pending -> 3
        OmbiAccessRequestStatus.Seen -> 2
        OmbiAccessRequestStatus.Dismissed -> 1
    }

private fun accessRequestKey(jellyfinUserId: String, config: OmbiConfig): OmbiAccessRequestKey =
    OmbiAccessRequestKey(
        serverAccountId = config.baseUrl.ifBlank { "unknown-server" }.lowercase(),
        profileId = jellyfinUserId,
        integrationId = "ombi:${config.baseUrl.ifBlank { "default" }.lowercase()}",
    )

private fun OmbiAccessRequest.key(config: OmbiConfig): OmbiAccessRequestKey =
    OmbiAccessRequestKey(
        serverAccountId = serverAccountId ?: config.baseUrl.ifBlank { serverName ?: "unknown-server" }.lowercase(),
        profileId = jellyfinUserId,
        integrationId = integrationId ?: "ombi:${config.baseUrl.ifBlank { serverName ?: "default" }.lowercase()}",
    )

private fun String.normalizedUserMatchKey(): String =
    lowercase().replace(Regex("[^a-z0-9]+"), "")

private fun JSONArray.toOmbiUsers(): List<OmbiUserProfile> =
    mapJsonObjects { it.toOmbiUserProfile() }

private fun JSONObject.toOmbiUserProfile(): OmbiUserProfile =
    OmbiUserProfile(
        id = optAnyString("id") ?: optAnyString("userId"),
        userName = optNullableString("userName")
            ?: optNullableString("username")
            ?: optNullableString("name")
            ?: optNullableString("emailAddress")
            ?: optNullableString("email")
            ?: "Ombi user",
        email = optNullableString("emailAddress") ?: optNullableString("email"),
        displayName = optNullableString("alias")
            ?: optNullableString("displayName")
            ?: optNullableString("fullName")
            ?: optNullableString("name"),
    )

private fun JSONArray.toAccessRequests(): List<OmbiAccessRequest> =
    mapJsonObjects {
        OmbiAccessRequest(
            jellyfinUserId = it.optString("jellyfinUserId"),
            jellyfinUserName = it.optString("jellyfinUserName"),
            serverName = it.optNullableString("serverName"),
            requestedAt = it.optLong("requestedAt", 0L),
            suggestedOmbiUserName = it.optString("suggestedOmbiUserName"),
            status = it.optString("status")
                .let { value -> runCatching { OmbiAccessRequestStatus.valueOf(value) }.getOrDefault(OmbiAccessRequestStatus.Pending) },
            note = it.optNullableString("note"),
            serverAccountId = it.optNullableString("serverAccountId"),
            integrationId = it.optNullableString("integrationId"),
        )
    }

private fun List<OmbiAccessRequest>.toAccessRequestsJson(): JSONArray =
    JSONArray().also { array ->
        forEach { request ->
            array.put(
                JSONObject()
                    .put("jellyfinUserId", request.jellyfinUserId)
                    .put("jellyfinUserName", request.jellyfinUserName)
                    .put("serverName", request.serverName)
                    .put("requestedAt", request.requestedAt)
                    .put("suggestedOmbiUserName", request.suggestedOmbiUserName)
                    .put("status", request.status.name)
                    .put("note", request.note)
                    .put("serverAccountId", request.serverAccountId)
                    .put("integrationId", request.integrationId),
            )
        }
    }

private fun JSONArray.toUserMappings(): List<OmbiUserMapping> =
    mapJsonObjects {
        OmbiUserMapping(
            jellyfinUserId = it.optString("jellyfinUserId"),
            jellyfinUserName = it.optString("jellyfinUserName"),
            ombiUserName = it.optNullableString("ombiUserName"),
            state = it.optString("state")
                .let { value -> runCatching { OmbiLinkedAccountState.valueOf(value) }.getOrDefault(OmbiLinkedAccountState.NotLinked) },
            updatedAt = it.optLong("updatedAt", 0L).takeIf { value -> value > 0L } ?: System.currentTimeMillis(),
            note = it.optNullableString("note"),
        )
    }

private fun List<OmbiUserMapping>.toUserMappingsJson(): JSONArray =
    JSONArray().also { array ->
        forEach { mapping ->
            array.put(
                JSONObject()
                    .put("jellyfinUserId", mapping.jellyfinUserId)
                    .put("jellyfinUserName", mapping.jellyfinUserName)
                    .put("ombiUserName", mapping.ombiUserName)
                    .put("state", mapping.state.name)
                    .put("updatedAt", mapping.updatedAt)
                    .put("note", mapping.note),
            )
        }
    }

private fun JSONArray.toStoredUserSessions(): List<OmbiStoredUserSession> =
    mapJsonObjects {
        OmbiStoredUserSession(
            jellyfinUserId = it.optString("jellyfinUserId"),
            ombiUserName = it.optString("ombiUserName"),
            displayName = it.optNullableString("displayName"),
            ombiUserId = it.optAnyString("ombiUserId"),
            expiresAt = it.optNullableString("expiresAt"),
            roles = it.optJSONArray("roles")?.toStringList().orEmpty(),
            lastLoginAt = it.optLong("lastLoginAt", 0L).takeIf { value -> value > 0L } ?: System.currentTimeMillis(),
            lastValidatedAt = it.optLong("lastValidatedAt", 0L).takeIf { value -> value > 0L },
            secretKey = it.optString("secretKey").takeIf { value -> value.isNotBlank() } ?: userTokenSecretKey(it.optString("jellyfinUserId")),
        )
    }

private fun List<OmbiStoredUserSession>.toStoredUserSessionsJson(): JSONArray =
    JSONArray().also { array ->
        forEach { session ->
            array.put(
                JSONObject()
                    .put("jellyfinUserId", session.jellyfinUserId)
                    .put("ombiUserName", session.ombiUserName)
                    .put("displayName", session.displayName)
                    .put("ombiUserId", session.ombiUserId)
                    .put("expiresAt", session.expiresAt)
                    .put("roles", JSONArray(session.roles))
                    .put("lastLoginAt", session.lastLoginAt)
                    .put("lastValidatedAt", session.lastValidatedAt ?: 0L)
                    .put("secretKey", session.secretKey),
            )
        }
    }

private fun JSONArray.toStringList(): List<String> =
    (0 until length()).mapNotNull { index -> optString(index).takeIf { it.isNotBlank() && it != "null" } }
