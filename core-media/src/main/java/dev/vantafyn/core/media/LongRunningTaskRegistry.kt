package dev.vantafyn.core.media

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class LongRunningTaskType {
    WebSocket,
    CastReporter,
    PlaybackReporter,
    MusicService,
    WatchPartyPolling,
    OmbiRefresh,
    StatisticsRefresh,
    Other,
}

data class LongRunningTaskSnapshot(
    val id: String,
    val type: LongRunningTaskType,
    val owner: String,
    val startedAtMs: Long,
    val lastTickAtMs: Long,
    val state: String,
    val stopReason: String? = null,
)

data class LongRunningTaskEvent(
    val taskId: String,
    val type: LongRunningTaskType,
    val owner: String,
    val atMs: Long,
    val state: String,
    val reason: String,
)

object LongRunningTaskRegistry {
    private val _tasks = MutableStateFlow<List<LongRunningTaskSnapshot>>(emptyList())
    val tasks: StateFlow<List<LongRunningTaskSnapshot>> = _tasks.asStateFlow()
    private val _recentEvents = MutableStateFlow<List<LongRunningTaskEvent>>(emptyList())
    val recentEvents: StateFlow<List<LongRunningTaskEvent>> = _recentEvents.asStateFlow()

    fun start(id: String, type: LongRunningTaskType, owner: String, state: String = "active") {
        val now = System.currentTimeMillis()
        _tasks.update { current ->
            current.filterNot { it.id == id } + LongRunningTaskSnapshot(
                id = id,
                type = type,
                owner = owner,
                startedAtMs = now,
                lastTickAtMs = now,
                state = state,
            )
        }
        recordEvent(id, type, owner, state, "started")
    }

    fun tick(id: String, state: String? = null) {
        val now = System.currentTimeMillis()
        _tasks.update { current ->
            current.map {
                if (it.id == id) it.copy(lastTickAtMs = now, state = state ?: it.state) else it
            }
        }
    }

    fun stop(id: String, reason: String) {
        _tasks.value.firstOrNull { it.id == id }?.let {
            recordEvent(it.id, it.type, it.owner, it.state, reason)
        }
        _tasks.update { current -> current.filterNot { it.id == id } }
    }

    private fun recordEvent(id: String, type: LongRunningTaskType, owner: String, state: String, reason: String) {
        val event = LongRunningTaskEvent(
            taskId = id,
            type = type,
            owner = owner,
            atMs = System.currentTimeMillis(),
            state = state,
            reason = reason,
        )
        _recentEvents.update { current -> (listOf(event) + current).take(12) }
    }
}
