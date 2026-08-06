package dev.vantafyn.core.ombi

import android.content.Context
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
    Disabled,
}

enum class OmbiAccessRequestStatus {
    Pending,
    Seen,
    AccountCreated,
    Linked,
    Dismissed,
}

data class OmbiAccessRequest(
    val jellyfinUserId: String,
    val jellyfinUserName: String,
    val serverName: String?,
    val requestedAt: Long,
    val suggestedOmbiUserName: String,
    val status: OmbiAccessRequestStatus = OmbiAccessRequestStatus.Pending,
    val note: String? = null,
)

data class OmbiUserMapping(
    val jellyfinUserId: String,
    val jellyfinUserName: String,
    val ombiUserName: String?,
    val state: OmbiLinkedAccountState = OmbiLinkedAccountState.NotLinked,
    val updatedAt: Long = System.currentTimeMillis(),
    val note: String? = null,
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
        const val SECRET_API_KEY = "ombi.api_key"
    }
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
    fun accessRequests(): List<OmbiAccessRequest> = store.readAccessRequests()
    fun userMappings(): List<OmbiUserMapping> = store.readMappings()
    fun pendingAccessRequestCount(): Int =
        store.readAccessRequests().count { it.status == OmbiAccessRequestStatus.Pending }

    fun mappingFor(jellyfinUserId: String): OmbiUserMapping? =
        store.readMappings().firstOrNull { it.jellyfinUserId == jellyfinUserId }

    fun requestAccess(
        jellyfinUserId: String,
        jellyfinUserName: String,
        serverName: String?,
        note: String?,
    ): OmbiAccessRequest {
        val requests = store.readAccessRequests()
        val existing = requests.firstOrNull {
            it.jellyfinUserId == jellyfinUserId && it.status != OmbiAccessRequestStatus.Dismissed && it.status != OmbiAccessRequestStatus.Linked
        }
        if (existing != null) return existing
        val request = OmbiAccessRequest(
            jellyfinUserId = jellyfinUserId,
            jellyfinUserName = jellyfinUserName,
            serverName = serverName,
            requestedAt = System.currentTimeMillis(),
            suggestedOmbiUserName = jellyfinUserName.toSuggestedOmbiUserName(),
            note = note?.takeIf { it.isNotBlank() },
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
        val updated = store.readAccessRequests().map {
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
        store.readAccessRequests().firstOrNull { it.jellyfinUserId == jellyfinUserId }?.let {
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

    fun upsertMapping(mapping: OmbiUserMapping) {
        val mappings = store.readMappings().filterNot { it.jellyfinUserId == mapping.jellyfinUserId }
        store.saveMappings(mappings + mapping.copy(updatedAt = System.currentTimeMillis()))
    }

    fun clearMapping(jellyfinUserId: String) {
        store.saveMappings(store.readMappings().filterNot { it.jellyfinUserId == jellyfinUserId })
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

    override suspend fun searchTv(query: String): IntegrationResult<List<MediaRequestSearchResult>> =
        runOmbiCatching {
            getJsonArray("/api/v1/Search/tv/${query.urlPath()}").mapJsonObjects { it.toSearchResult(MediaRequestType.Tv) }
        }

    override suspend fun requestMovie(providerId: String, requestedBy: String?): IntegrationResult<Unit> =
        runOmbiCatching {
            val body = JSONObject().put("theMovieDbId", providerId.toIntOrNull() ?: providerId)
            postJson("/api/v1/Request/movie", body, requestedBy)
        }

    override suspend fun requestTv(providerId: String, requestedBy: String?): IntegrationResult<Unit> =
        runOmbiCatching {
            val body = JSONObject().put("tvDbId", providerId.toIntOrNull() ?: providerId)
            postJson("/api/v1/Request/tv", body, requestedBy)
        }

    override suspend fun getUserRequests(userName: String?): IntegrationResult<List<MediaRequestItem>> =
        runOmbiCatching {
            (getRequests("/api/v1/Request/movie", MediaRequestType.Movie) + getRequests("/api/v1/Request/tv", MediaRequestType.Tv))
                .filter { userName.isNullOrBlank() || it.requestedBy.isNullOrBlank() || it.requestedBy.equals(userName, ignoreCase = true) }
        }

    override suspend fun getAllRequests(): IntegrationResult<List<MediaRequestItem>> =
        runOmbiCatching {
            getRequests("/api/v1/Request/movie", MediaRequestType.Movie) + getRequests("/api/v1/Request/tv", MediaRequestType.Tv)
        }

    override suspend fun approveRequest(requestId: String): IntegrationResult<Unit> =
        IntegrationResult.Failure(IntegrationFailureReason.Unsupported, "Approval endpoint varies by Ombi version and is not wired yet.")

    override suspend fun denyRequest(requestId: String): IntegrationResult<Unit> =
        IntegrationResult.Failure(IntegrationFailureReason.Unsupported, "Deny endpoint varies by Ombi version and is not wired yet.")

    private suspend fun <T> runOmbiCatching(block: suspend () -> T): IntegrationResult<T> =
        withContext(ioDispatcher) {
            try {
                val config = store.read()
                val apiKey = store.readApiKey()
                if (!config.isConfigured || apiKey.isNullOrBlank()) {
                    return@withContext IntegrationResult.Failure(IntegrationFailureReason.NotConfigured, "Ombi is not configured.")
                }
                IntegrationResult.Success(withTimeout(12_000L) { block() })
            } catch (throwable: Throwable) {
                throwable.toIntegrationFailure()
            }
        }

    private fun getRequests(path: String, type: MediaRequestType): List<MediaRequestItem> =
        getJsonArray(path).mapJsonObjects { it.toRequestItem(type) }

    private fun getJsonArray(path: String): JSONArray {
        val value = request(path, "GET", null, null)
        return when (value) {
            is JSONArray -> value
            is JSONObject -> value.optJSONArray("results") ?: value.optJSONArray("items") ?: JSONArray().put(value)
            else -> JSONArray()
        }
    }

    private fun getJsonArrayOrObject(path: String): Any = request(path, "GET", null, null)

    private fun postJson(path: String, body: JSONObject, requestedBy: String?) {
        request(path, "POST", body, requestedBy)
    }

    private fun request(path: String, method: String, body: JSONObject?, requestedBy: String?): Any {
        val config = store.read()
        val apiKey = store.readApiKey().orEmpty()
        val connection = URL("${config.baseUrl.trimEnd('/')}$path").openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 12_000
        connection.readTimeout = 12_000
        connection.setRequestProperty("ApiKey", apiKey)
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "application/json")
        val alias = requestedBy?.takeIf { it.isNotBlank() } ?: config.apiAlias
        alias?.let { connection.setRequestProperty("ApiAlias", it) }
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
}

private class OmbiHttpException(val code: Int, message: String) : RuntimeException(message)

private fun Throwable.toIntegrationFailure(): IntegrationResult.Failure {
    val className = javaClass.name
    val message = message.orEmpty()
    val reason = when {
        this is kotlinx.coroutines.TimeoutCancellationException -> IntegrationFailureReason.NetworkError
        this is OmbiHttpException && code == 401 -> IntegrationFailureReason.Unauthorized
        this is OmbiHttpException && code == 403 -> IntegrationFailureReason.Forbidden
        this is OmbiHttpException && code == 409 -> IntegrationFailureReason.AlreadyExists
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
    val userMessage = when (reason) {
        IntegrationFailureReason.Unauthorized -> "Ombi did not accept this API key."
        IntegrationFailureReason.Forbidden -> "This Ombi account does not have permission to do that."
        IntegrationFailureReason.NotConfigured -> "Ombi is not configured."
        IntegrationFailureReason.NetworkError -> "Could not reach Ombi."
        IntegrationFailureReason.ServerError -> "Ombi responded with an error."
        IntegrationFailureReason.Unsupported -> "This Ombi action is not supported yet."
        IntegrationFailureReason.InvalidConfiguration -> "Check the Ombi server address."
        IntegrationFailureReason.AlreadyExists -> "This request already exists."
        IntegrationFailureReason.Unknown -> "The Ombi request failed."
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

private fun String.parseJsonBody(): Any {
    val trimmed = trim()
    if (trimmed.isBlank()) return JSONObject()
    return if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONObject(trimmed)
}

private fun <T> JSONArray.mapJsonObjects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).mapNotNull { index -> optJSONObject(index)?.let(transform) }

private fun JSONObject.toSearchResult(type: MediaRequestType): MediaRequestSearchResult {
    val providerId = when (type) {
        MediaRequestType.Movie -> optAnyString("theMovieDbId") ?: optAnyString("tmdbId") ?: optAnyString("id")
        MediaRequestType.Tv -> optAnyString("theTvDbId") ?: optAnyString("tvDbId") ?: optAnyString("id")
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

private fun JSONObject.toRequestItem(type: MediaRequestType): MediaRequestItem =
    MediaRequestItem(
        requestId = optAnyString("id") ?: optAnyString("requestId") ?: "",
        providerId = optAnyString(if (type == MediaRequestType.Movie) "theMovieDbId" else "theTvDbId"),
        title = optString("title", optString("name", "Untitled")),
        type = type,
        status = requestStatus(),
        requestedBy = optJSONObject("requestedUser")?.optNullableString("userName")
            ?: optNullableString("requestedUserName")
            ?: optNullableString("requestedBy"),
        posterUrl = optNullableString("posterPath") ?: optNullableString("poster"),
    )

private fun JSONObject.requestStatus(): MediaRequestStatus =
    when {
        optBoolean("available", false) || optBoolean("isAvailable", false) -> MediaRequestStatus.Available
        optBoolean("denied", false) || optBoolean("deniedBy", false) -> MediaRequestStatus.Denied
        optBoolean("approved", false) || optBoolean("approvedBy", false) -> MediaRequestStatus.Approved
        optBoolean("processing", false) || optBoolean("isProcessing", false) -> MediaRequestStatus.Processing
        optBoolean("requested", false) || optBoolean("isRequested", false) -> MediaRequestStatus.Pending
        else -> MediaRequestStatus.NotRequested
    }

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

private fun String.toSuggestedOmbiUserName(): String {
    val sanitized = lowercase()
        .replace(Regex("[^a-z0-9._-]+"), ".")
        .trim('.', '_', '-')
    return sanitized.ifBlank { "vantafyn.user" }.take(48)
}

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
                    .put("note", request.note),
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
