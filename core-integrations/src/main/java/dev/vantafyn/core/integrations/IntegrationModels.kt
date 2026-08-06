package dev.vantafyn.core.integrations

enum class IntegrationType {
    Ombi,
    Jellyseerr,
    Overseerr,
    Tautulli,
}

enum class IntegrationCapability {
    Requests,
    RequestStatus,
    RequestApproval,
    RequestManagement,
    UserRequests,
    SearchExternalMedia,
    AdminDashboard,
}

data class IntegrationConfig(
    val type: IntegrationType,
    val enabled: Boolean,
    val baseUrl: String,
    val displayName: String? = null,
)

data class VantafynIntegration(
    val type: IntegrationType,
    val name: String,
    val capabilities: Set<IntegrationCapability>,
    val enabled: Boolean,
)

sealed interface IntegrationConnectionState {
    data object NotConfigured : IntegrationConnectionState
    data object Testing : IntegrationConnectionState
    data class Connected(val displayName: String? = null) : IntegrationConnectionState
    data class Failed(val message: String) : IntegrationConnectionState
}

sealed interface IntegrationResult<out T> {
    data class Success<T>(val value: T) : IntegrationResult<T>
    data class Failure(val reason: IntegrationFailureReason, val message: String, val cause: Throwable? = null) : IntegrationResult<Nothing>
}

enum class IntegrationFailureReason {
    Unauthorized,
    Forbidden,
    NotConfigured,
    NetworkError,
    ServerError,
    Unsupported,
    InvalidConfiguration,
    AlreadyExists,
    Unknown,
}

enum class MediaRequestType {
    Movie,
    Tv,
}

enum class MediaRequestStatus {
    Available,
    NotRequested,
    Pending,
    Approved,
    Denied,
    Processing,
    Unknown,
}

data class MediaRequestSearchResult(
    val providerId: String,
    val title: String,
    val year: Int?,
    val type: MediaRequestType,
    val overview: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val status: MediaRequestStatus,
)

data class MediaRequestItem(
    val requestId: String,
    val providerId: String?,
    val title: String,
    val type: MediaRequestType,
    val status: MediaRequestStatus,
    val requestedBy: String?,
    val posterUrl: String?,
)

interface MediaRequestProvider {
    val integration: VantafynIntegration
    suspend fun testConnection(): IntegrationResult<IntegrationConnectionState.Connected>
    suspend fun searchMovie(query: String): IntegrationResult<List<MediaRequestSearchResult>>
    suspend fun searchTv(query: String): IntegrationResult<List<MediaRequestSearchResult>>
    suspend fun requestMovie(providerId: String, requestedBy: String?): IntegrationResult<Unit>
    suspend fun requestTv(providerId: String, requestedBy: String?): IntegrationResult<Unit>
    suspend fun getUserRequests(userName: String?): IntegrationResult<List<MediaRequestItem>>
    suspend fun getAllRequests(): IntegrationResult<List<MediaRequestItem>>
    suspend fun approveRequest(requestId: String): IntegrationResult<Unit>
    suspend fun denyRequest(requestId: String): IntegrationResult<Unit>
}

interface IntegrationRepository {
    suspend fun integrations(): List<VantafynIntegration>
    fun hasCapability(capability: IntegrationCapability): Boolean
}

interface IntegrationAuthStorage {
    fun saveSecret(key: String, secret: String)
    fun readSecret(key: String): String?
    fun removeSecret(key: String)
}
