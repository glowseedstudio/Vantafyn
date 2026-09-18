package dev.vantafyn.core.integrations.push

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Internal message router for incoming UnifiedPush payloads.
 * Exposes a shared flow that future Vantafyn notification handlers (Jellyfin sync, playback controls, social, etc.)
 * can subscribe to without ever dealing directly with the UnifiedPush receiver/service.
 */
object UnifiedPushPayloadDispatcher {
    private const val TAG = "UnifiedPushDispatcher"

    private val _incomingPayloads = MutableSharedFlow<UnifiedPushPayload>(extraBufferCapacity = 64)
    val incomingPayloads: SharedFlow<UnifiedPushPayload> = _incomingPayloads.asSharedFlow()

    fun dispatch(payload: UnifiedPushPayload) {
        // Never log the payload content or private keys; only safe metadata
        Log.i(TAG, "Received push payload: size=${payload.sizeBytes} bytes, isDecrypted=${payload.isDecrypted}, timestamp=${payload.receivedAtMillis}")
        _incomingPayloads.tryEmit(payload)
    }
}
